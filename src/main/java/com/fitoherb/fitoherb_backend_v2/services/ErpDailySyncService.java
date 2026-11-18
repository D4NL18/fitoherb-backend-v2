package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.DailySyncReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.MailReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.DailySyncRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.entities.*;
import com.fitoherb.fitoherb_backend_v2.enums.DailySyncMode;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.infra.clients.AiCatalogRefreshClient;
import com.fitoherb.fitoherb_backend_v2.repositories.*;
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpDailySyncService {

    private final ErpCatalogGateway erpCatalogGateway;
    private final ErpCatalogCacheService erpCatalogCacheService;
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductCategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ErpSyncStateRepository syncStateRepository;
    private final ErpSyncRunRepository syncRunRepository;
    private final ErpIgnoredCatalogCodeRepository ignoredCatalogCodeRepository;
    private final AiCatalogRefreshClient aiCatalogRefreshClient;
    private final MailService mailService;
    private final ProductVariantSanitizer variantSanitizer;
    private final ProductVisibilityCalculator visibilityCalculator;
    private final ErpVariantSuggestionService suggestionService;

    @Value("${app.erp.sync.internal-scheduler-enabled:true}")
    private boolean internalSchedulerEnabled = true;

    @Value("${app.erp.sync.fallback-category-slug:outros}")
    private String fallbackCategorySlug = "outros";

    @Value("${app.erp.sync.fallback-supplier-slug:fitoherb}")
    private String fallbackSupplierSlug = "fitoherb";

    @Value("${app.erp.sync.report-email:}")
    private String reportEmail;

    @Scheduled(cron = "${app.erp.sync.cron:0 0 8 * * *}", zone = "America/Sao_Paulo")
    public void runScheduledDailySync() {
        if (!internalSchedulerEnabled) {
            log.debug("Agendador interno de sincronização diária ERP desabilitado por configuração.");
            return;
        }
        try {
            executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false),
                    "SCHEDULED_INTERNAL",
                    "SYSTEM_SCHEDULED",
                    true
            );
        } catch (Exception e) {
            log.warn("Execução agendada da sincronização diária ERP encerrada com falha controlada: {}", e.getClass().getSimpleName());
        }
    }

    @Transactional(noRollbackFor = Exception.class)
    public DailySyncRes executeSync(
            DailySyncReq req,
            String triggerType,
            String triggeredBy,
            boolean acceptIfRunning
    ) {
        DailySyncMode mode = (req != null && req.getMode() != null) ? req.getMode() : DailySyncMode.INCREMENTAL;
        boolean dryRun = req != null && req.isDryRun();
        LocalDateTime now = LocalDateTime.now();
        String runId = UUID.randomUUID().toString();

        int lockAcquired = syncStateRepository.tryAcquireLock(runId, now, now.minusMinutes(15));
        ErpSyncState state = syncStateRepository.findById((short) 1).orElseGet(() -> {
            ErpSyncState created = ErpSyncState.builder()
                    .id((short) 1)
                    .autoSyncEnabled(true)
                    .inProgress(true)
                    .activeRunId(runId)
                    .lockAcquiredAt(now)
                    .updatedAt(now)
                    .build();
            return syncStateRepository.save(created);
        });

        if (lockAcquired == 0) {
            String activeRunId = state.getActiveRunId() != null ? state.getActiveRunId() : runId;
            if (acceptIfRunning) {
                return DailySyncRes.builder()
                        .runId(activeRunId)
                        .mode(mode)
                        .status("ALREADY_RUNNING")
                        .dryRun(dryRun)
                        .build();
            }
            throw new ResourceAlreadyExistsException(
                    "ERP_SYNC_ALREADY_RUNNING",
                    "Já existe uma sincronização diária do ERP Atrio em andamento.",
                    Map.of("activeRunId", activeRunId)
            );
        }

        ErpSyncRun syncRun = ErpSyncRun.builder()
                .id(runId)
                .triggerType(triggerType != null ? triggerType : "SCHEDULED_INTERNAL")
                .triggeredBy(triggeredBy != null ? triggeredBy : "SYSTEM_SCHEDULED")
                .status("RUNNING")
                .startedAt(now)
                .build();
        syncRunRepository.save(syncRun);

        try {
            List<ErpCatalogItemSnapshot> catalog = erpCatalogGateway.fetchFullCatalog();
            Map<Long, ErpCatalogItemSnapshot> catalogByCode = new LinkedHashMap<>();
            if (catalog != null) {
                for (ErpCatalogItemSnapshot item : catalog) {
                    if (item != null && item.getErpCode() != null) {
                        catalogByCode.put(item.getErpCode(), item);
                    }
                }
            }

            Set<Long> ignoredCodes = Optional.ofNullable(ignoredCatalogCodeRepository.findAllErpCodes())
                    .orElse(Collections.emptySet());
            List<Product> existingProducts = Optional.ofNullable(productRepository.findAllWithSkus())
                    .orElse(Collections.emptyList());

            Set<Long> mappedErpCodes = new HashSet<>();
            int checkedMappedSkus = 0;
            int deactivatedProducts = 0;
            int partiallyDeactivatedProducts = 0;
            int reactivatedProducts = 0;
            int createdPendingProducts = 0;
            int ignoredCodesSkipped = 0;
            List<String> pendingProductSummaries = new ArrayList<>();

            for (Product product : existingProducts) {
                List<ProductSku> skus = product.getSkus();
                if (skus == null || skus.isEmpty()) {
                    continue;
                }
                if (product.getCurationStatus() == ProductCurationStatus.IGNORED) {
                    for (ProductSku s : skus) {
                        if (s.getErpCode() != null) {
                            mappedErpCodes.add(s.getErpCode());
                        }
                    }
                    visibilityCalculator.recalculate(product);
                    continue;
                }

                boolean wasActive = Boolean.TRUE.equals(product.getActive());
                int activeSkusBefore = (int) skus.stream().filter(s -> !Boolean.TRUE.equals(s.getErpInactive())).count();
                boolean anySkuChanged = false;

                for (ProductSku sku : skus) {
                    if (sku.getErpCode() == null) {
                        continue;
                    }
                    mappedErpCodes.add(sku.getErpCode());
                    checkedMappedSkus++;

                    if (ignoredCodes.contains(sku.getErpCode())) {
                        ignoredCodesSkipped++;
                        continue;
                    }

                    ErpCatalogItemSnapshot erpSnap = catalogByCode.get(sku.getErpCode());
                    if (erpSnap != null) {
                        boolean newInactive = erpSnap.isInativoPro();
                        if (!Objects.equals(Boolean.TRUE.equals(sku.getErpInactive()), newInactive)) {
                            sku.setErpInactive(newInactive);
                            anySkuChanged = true;
                        }
                        sku.setErpLastVerifiedAt(now);
                    }
                }

                visibilityCalculator.recalculate(product);
                boolean isNowActive = Boolean.TRUE.equals(product.getActive());
                int activeSkusAfter = (int) skus.stream().filter(s -> !Boolean.TRUE.equals(s.getErpInactive())).count();

                if (wasActive && !isNowActive) {
                    deactivatedProducts++;
                } else if (!wasActive && isNowActive) {
                    reactivatedProducts++;
                } else if (isNowActive && skus.size() > 1 && activeSkusAfter < skus.size() && (anySkuChanged || activeSkusAfter < activeSkusBefore)) {
                    partiallyDeactivatedProducts++;
                }

                if (anySkuChanged && !dryRun) {
                    productRepository.save(product);
                }
            }

            if (mode != DailySyncMode.STATUS_ONLY) {
                for (ErpCatalogItemSnapshot item : catalogByCode.values()) {
                    Long code = item.getErpCode();
                    if (mappedErpCodes.contains(code)) {
                        continue;
                    }
                    if (ignoredCodes.contains(code)) {
                        ignoredCodesSkipped++;
                        continue;
                    }
                    if (item.isInativoPro()) {
                        continue;
                    }

                    Product newProduct = buildPendingProductFromErp(item, now);
                    if (!dryRun) {
                        productRepository.save(newProduct);
                    }
                    mappedErpCodes.add(code);
                    createdPendingProducts++;
                    pendingProductSummaries.add(String.format("[%d] %s (PENDING_IMAGE)", code, item.getDescription()));
                }
            }

            syncRun.setStatus("COMPLETED");
            syncRun.setFinishedAt(LocalDateTime.now());
            syncRun.setDeactivatedCount(deactivatedProducts);
            syncRun.setReactivatedCount(reactivatedProducts);
            syncRun.setPartialVariantUpdatedCount(partiallyDeactivatedProducts);
            syncRun.setCreatedCount(createdPendingProducts);
            syncRun.setSkippedIgnoredCount(ignoredCodesSkipped);
            syncRunRepository.save(syncRun);

            syncRunRepository.deleteOldRunsBefore(now.minusDays(90));

            boolean aiRefreshed = aiCatalogRefreshClient.refreshCatalog();

            DailySyncRes response = DailySyncRes.builder()
                    .runId(runId)
                    .mode(mode)
                    .status("COMPLETED")
                    .dryRun(dryRun)
                    .baselineCutoffAt(state.getBaselineCutoffAt() != null ? state.getBaselineCutoffAt().toString() : null)
                    .checkedMappedSkus(checkedMappedSkus)
                    .deactivatedProducts(deactivatedProducts)
                    .partiallyDeactivatedProducts(partiallyDeactivatedProducts)
                    .reactivatedProducts(reactivatedProducts)
                    .createdPendingProducts(createdPendingProducts)
                    .ignoredCodesSkipped(ignoredCodesSkipped)
                    .aiCatalogRefreshed(aiRefreshed)
                    .build();

            sendEmailReportBestEffort(response, catalogByCode.size(), pendingProductSummaries);
            return response;
        } catch (Exception e) {
            syncRun.setStatus("FAILED");
            syncRun.setFinishedAt(LocalDateTime.now());
            syncRun.setErrorSummary(sanitizeErrorSummary(e));
            syncRunRepository.save(syncRun);
            throw e;
        } finally {
            syncStateRepository.releaseLock(LocalDateTime.now());
        }
    }

    private Product buildPendingProductFromErp(ErpCatalogItemSnapshot item, LocalDateTime now) {
        ProductCategory category = categoryRepository.findBySlug(fallbackCategorySlug)
                .orElseGet(() -> {
                    ProductCategory c = new ProductCategory();
                    c.setName("Outros");
                    c.setSlug(fallbackCategorySlug);
                    return c;
                });

        Supplier supplier = supplierRepository.findBySlug(fallbackSupplierSlug)
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName("Fitoherb");
                    s.setSlug(fallbackSupplierSlug);
                    return s;
                });

        String commercialName = suggestionService.formatCommercialName(item.getDescription());
        String baseSlug = StringUtils.toSlug(commercialName + " " + supplier.getName());
        String uniqueSlug = ensureUniqueSlug(baseSlug, item.getErpCode());

        Product product = new Product();
        product.setName(commercialName);
        product.setSlug(uniqueSlug);
        product.setDescription(item.getDescription());
        product.setImagePath(null);
        product.setActive(true);
        product.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setFlavours(Collections.emptyList());
        product.setPresentation(Collections.emptyList());

        ProductSku sku = new ProductSku();
        sku.setProduct(product);
        sku.setErpCode(item.getErpCode());
        sku.setEan(item.getEan());
        sku.setFlavour(null);
        sku.setPresentation(null);
        sku.setNormalizedVariantKey("__SIMPLE__");
        sku.setSortOrder(0);
        sku.setErpInactive(false);
        sku.setErpInactiveConfirmed(false);
        sku.setErpLastVerifiedAt(now);

        product.setSkus(new ArrayList<>(List.of(sku)));
        return product;
    }

    private String ensureUniqueSlug(String baseSlug, Long erpCode) {
        if (productRepository.findBySlug(baseSlug).isEmpty()) {
            return baseSlug;
        }
        String candidate = baseSlug + "-" + erpCode;
        int suffix = 2;
        while (productRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + erpCode + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private void sendEmailReportBestEffort(
            DailySyncRes res,
            int totalScannedErpItems,
            List<String> pendingProductSummaries
    ) {
        if (reportEmail == null || reportEmail.isBlank()) {
            return;
        }
        try {
            MailReq mailReq = new MailReq();
            mailReq.setEmail(reportEmail.trim());
            mailReq.setSubject("Relatório de Sincronização Diária ERP - Fitoherb");

            StringBuilder body = new StringBuilder();
            body.append("Resumo da Sincronização Diária ERP Atrio -> Fitoherb:\n")
                .append("- Itens escaneados no ERP: ").append(totalScannedErpItems).append("\n")
                .append("- SKUs verificados no site: ").append(res.getCheckedMappedSkus()).append("\n")
                .append("- Novos produtos sem imagem (PENDING_IMAGE): ").append(res.getCreatedPendingProducts()).append("\n")
                .append("- Produtos reativados: ").append(res.getReactivatedProducts()).append("\n")
                .append("- Produtos desativados: ").append(res.getDeactivatedProducts()).append("\n")
                .append("- Produtos Multi-SKU parcialmente atualizados: ").append(res.getPartiallyDeactivatedProducts()).append("\n")
                .append("- Códigos ignorados: ").append(res.getIgnoredCodesSkipped()).append("\n");

            if (!pendingProductSummaries.isEmpty()) {
                body.append("\nProdutos aguardando imagem na Central de Curadoria Rápida (PENDING_IMAGE):\n");
                for (String summary : pendingProductSummaries) {
                    body.append("- ").append(summary).append("\n");
                }
            }

            mailReq.setMessage(body.toString());
            mailService.sendEmail(mailReq);
        } catch (Exception e) {
            log.warn("Não foi possível enviar o relatório por e-mail pós-sincronização ERP: {}", e.getClass().getSimpleName());
        }
    }

    private String sanitizeErrorSummary(Exception e) {
        String raw = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        String sanitized = raw.replaceAll("(?i)jdbc:[^\\s]+", "[REDACTED_JDBC_URL]")
                .replaceAll("(?i)password=[^;&\\s]+", "password=[REDACTED]");
        return sanitized.length() > 480 ? sanitized.substring(0, 480) : sanitized;
    }
}

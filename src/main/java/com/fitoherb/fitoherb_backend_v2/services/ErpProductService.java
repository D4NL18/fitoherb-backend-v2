package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SuggestVariantsReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.*;
import com.fitoherb.fitoherb_backend_v2.entities.ErpSyncState;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import com.fitoherb.fitoherb_backend_v2.repositories.ErpSyncStateRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ErpProductService {

    private final ErpCatalogCacheService erpCatalogCacheService;
    private final ErpVariantSuggestionService erpVariantSuggestionService;
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final ErpSyncStateRepository erpSyncStateRepository;
    private final ProductVariantSanitizer variantSanitizer;

    public ApiEnvelope<List<ErpCatalogItemRes>> search(
            String q,
            String supplierSlug,
            boolean includeInactive,
            String excludeProductSlug,
            int page,
            int size
    ) {
        validateSearchQuery(q, size);
        String cleanQuery = q.trim();
        String normQuery = variantSanitizer.normalizeForComparison(cleanQuery);

        List<ErpCatalogItemSnapshot> allItems = erpCatalogCacheService.getCatalogSnapshot();
        List<ErpCatalogItemSnapshot> filtered = new ArrayList<>();

        for (ErpCatalogItemSnapshot item : allItems) {
            if (!includeInactive && item.isInativoPro()) {
                continue;
            }
            if (matchesSearch(item, cleanQuery, normQuery)) {
                filtered.add(item);
            }
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<ErpCatalogItemSnapshot> pageSlice = filtered.subList(fromIndex, toIndex);
        List<Long> sliceCodes = pageSlice.stream().map(ErpCatalogItemSnapshot::getErpCode).toList();

        Map<Long, ProductSku> mappedByCode = new HashMap<>();
        if (!sliceCodes.isEmpty()) {
            List<ProductSku> existingSkus = productSkuRepository.findByErpCodeIn(sliceCodes);
            if (existingSkus != null) {
                for (ProductSku sku : existingSkus) {
                    if (excludeProductSlug != null && sku.getProduct() != null
                            && excludeProductSlug.equalsIgnoreCase(sku.getProduct().getSlug())) {
                        continue;
                    }
                    mappedByCode.put(sku.getErpCode(), sku);
                }
            }
        }

        List<ErpCatalogItemRes> result = pageSlice.stream()
                .map(snap -> toCatalogItemRes(snap, mappedByCode.get(snap.getErpCode())))
                .toList();

        Instant lastSynced = erpCatalogCacheService.getLastSyncedAt();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("page", safePage);
        meta.put("size", safeSize);
        meta.put("totalElements", totalElements);
        meta.put("totalPages", Math.max(totalPages, 1));
        if (lastSynced != null) {
            meta.put("lastSyncedAt", lastSynced.toString());
        }

        return new ApiEnvelope<>(result, meta);
    }

    public ApiEnvelope<ErpCatalogItemRes> getByErpCode(Long erpCode, String excludeProductSlug) {
        if (erpCode == null || erpCode <= 0) {
            throw new IllegalArgumentException("O código do produto no ERP (Id_Pro) deve ser um número inteiro positivo.");
        }

        ErpCatalogItemSnapshot snap = erpCatalogCacheService.findByErpCode(erpCode)
                .orElseThrow(() -> new ResourceNotFoundException("Código ERP '" + erpCode + "' não encontrado no catálogo do ERP Atrio."));

        ProductSku mapped = productSkuRepository.findByErpCode(erpCode).orElse(null);
        if (mapped != null && excludeProductSlug != null && mapped.getProduct() != null
                && excludeProductSlug.equalsIgnoreCase(mapped.getProduct().getSlug())) {
            mapped = null;
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        Instant lastSynced = erpCatalogCacheService.getLastSyncedAt();
        if (lastSynced != null) {
            meta.put("lastSyncedAt", lastSynced.toString());
        }

        return new ApiEnvelope<>(toCatalogItemRes(snap, mapped), meta);
    }

    public ApiEnvelope<SuggestVariantsRes> suggestVariants(SuggestVariantsReq req) {
        if (req == null || req.getErpCodes() == null || req.getErpCodes().isEmpty() || req.getErpCodes().size() > 100) {
            throw new IllegalArgumentException("A lista de códigos ERP deve conter entre 1 e 100 itens.");
        }

        List<ErpCatalogItemSnapshot> snapshots = new ArrayList<>();
        for (Long code : req.getErpCodes()) {
            if (code == null || code <= 0) {
                throw new IllegalArgumentException("Todos os códigos ERP informados devem ser inteiros positivos.");
            }
            ErpCatalogItemSnapshot snap = erpCatalogCacheService.findByErpCode(code)
                    .orElseThrow(() -> new UnprocessableEntityException(
                            "ERP_CODE_NOT_FOUND",
                            "Código ERP '" + code + "' não encontrado no catálogo do ERP Atrio."
                    ));
            snapshots.add(snap);
        }

        Map<Long, String> conflictsByCode = new HashMap<>();
        List<ProductSku> existingSkus = productSkuRepository.findByErpCodeIn(req.getErpCodes());
        if (existingSkus != null) {
            for (ProductSku sku : existingSkus) {
                Product owner = sku.getProduct();
                if (owner != null && req.getExcludeProductSlug() != null
                        && req.getExcludeProductSlug().equalsIgnoreCase(owner.getSlug())) {
                    continue;
                }
                conflictsByCode.put(sku.getErpCode(), owner != null ? owner.getName() : "Outro Produto");
            }
        }

        SuggestVariantsRes suggestion = erpVariantSuggestionService.suggest(
                snapshots, conflictsByCode, req.getExcludeProductSlug()
        );

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("count", suggestion.getItems() != null ? suggestion.getItems().size() : 0);

        return new ApiEnvelope<>(suggestion, meta);
    }

    @Transactional
    public ApiEnvelope<SyncErpCatalogRes> syncCatalogCache() {
        List<ErpCatalogItemSnapshot> catalog = erpCatalogCacheService.forceRefresh();
        int activeCount = 0;
        int inactiveCount = 0;
        Map<Long, ErpCatalogItemSnapshot> byCode = new HashMap<>();

        for (ErpCatalogItemSnapshot item : catalog) {
            byCode.put(item.getErpCode(), item);
            if (item.isInativoPro()) {
                inactiveCount++;
            } else {
                activeCount++;
            }
        }

        List<Product> products = Optional.ofNullable(productRepository.findAllWithSkus()).orElse(Collections.emptyList());
        int checkedSkus = 0;
        int newlyInactivatedProducts = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Product p : products) {
            if (p.getSkus() == null || p.getSkus().isEmpty()) {
                continue;
            }
            boolean hadInactiveBefore = p.getSkus().stream().anyMatch(s -> Boolean.TRUE.equals(s.getErpInactive()));
            boolean changed = false;
            for (ProductSku sku : p.getSkus()) {
                checkedSkus++;
                ErpCatalogItemSnapshot snap = byCode.get(sku.getErpCode());
                if (snap != null) {
                    if (!Objects.equals(Boolean.TRUE.equals(sku.getErpInactive()), snap.isInativoPro())) {
                        sku.setErpInactive(snap.isInativoPro());
                        changed = true;
                    }
                    sku.setErpLastVerifiedAt(now);
                }
            }
            boolean hasInactiveNow = p.getSkus().stream().anyMatch(s -> Boolean.TRUE.equals(s.getErpInactive()));
            if (!hadInactiveBefore && hasInactiveNow) {
                newlyInactivatedProducts++;
            }
            if (changed) {
                productRepository.save(p);
            }
        }

        SyncErpCatalogRes data = SyncErpCatalogRes.builder()
                .processedCatalogItems(catalog.size())
                .activeCatalogItems(activeCount)
                .inactiveCatalogItems(inactiveCount)
                .checkedSiteSkus(checkedSkus)
                .newlyInactivatedMappedProducts(newlyInactivatedProducts)
                .build();

        String ts = OffsetDateTime.now().toString();
        Map<String, Object> meta = Map.of("timestamp", ts, "syncedAt", ts);
        return new ApiEnvelope<>(data, meta);
    }

    public ApiEnvelope<ErpSyncStatusRes> getSyncStatus() {
        ErpSyncState state = erpSyncStateRepository.findById((short) 1).orElse(new ErpSyncState());
        List<Product> pending = productRepository.findByCurationStatusOrderByCreatedAtDesc(ProductCurationStatus.PENDING_IMAGE);

        ErpSyncStatusRes statusRes = ErpSyncStatusRes.builder()
                .inProgress(Boolean.TRUE.equals(state.getInProgress()))
                .activeRunId(state.getActiveRunId())
                .lastSuccessfulSyncAt(state.getLastSuccessfulSyncAt() != null ? state.getLastSuccessfulSyncAt().toString() : null)
                .baselineCutoffAt(state.getBaselineCutoffAt() != null ? state.getBaselineCutoffAt().toString() : null)
                .autoSyncEnabled(Boolean.TRUE.equals(state.getAutoSyncEnabled()))
                .pendingCurationCount(pending != null ? pending.size() : 0)
                .build();

        return new ApiEnvelope<>(statusRes, Map.of("timestamp", OffsetDateTime.now().toString()));
    }

    public void validateSearchQuery(String q, int size) {
        if (q == null || q.trim().isEmpty()) {
            throw new IllegalArgumentException("O parâmetro de busca 'q' é obrigatório.");
        }
        String trimmed = q.trim();
        boolean isNumeric = trimmed.matches("\\d+");
        if (!isNumeric && trimmed.length() < 2) {
            throw new IllegalArgumentException("O termo de busca textual deve possuir pelo menos 2 caracteres.");
        }
        if (size <= 0 || size > 50) {
            throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e 50.");
        }
    }

    private boolean matchesSearch(ErpCatalogItemSnapshot item, String rawQuery, String normQuery) {
        if (String.valueOf(item.getErpCode()).startsWith(rawQuery)) {
            return true;
        }
        if (item.getEan() != null && item.getEan().contains(rawQuery)) {
            return true;
        }
        String descNorm = variantSanitizer.normalizeForComparison(item.getDescription());
        String brandNorm = variantSanitizer.normalizeForComparison(item.getBrandName());
        String catNorm = variantSanitizer.normalizeForComparison(item.getCategoryLabel());
        String combined = descNorm + " " + brandNorm + " " + catNorm;

        String[] tokens = normQuery.split("\\s+");
        for (String token : tokens) {
            if (!token.isEmpty() && !combined.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private ErpCatalogItemRes toCatalogItemRes(ErpCatalogItemSnapshot snap, ProductSku mappedSku) {
        Product owner = mappedSku != null ? mappedSku.getProduct() : null;
        return ErpCatalogItemRes.builder()
                .erpCode(snap.getErpCode())
                .erpDescription(snap.getDescription())
                .ean(snap.getEan())
                .erpBrand(snap.getBrandName())
                .erpSupplier(snap.getSupplierName() != null ? snap.getSupplierName() : snap.getBrandName())
                .inativoPro(snap.isInativoPro())
                .inExterno01(snap.isInExterno01())
                .alreadyMapped(mappedSku != null)
                .mappedProductSlug(owner != null ? owner.getSlug() : null)
                .mappedProductName(owner != null ? owner.getName() : null)
                .mappedFlavour(mappedSku != null ? mappedSku.getFlavour() : null)
                .mappedPresentation(mappedSku != null ? mappedSku.getPresentation() : null)
                .build();
    }
}

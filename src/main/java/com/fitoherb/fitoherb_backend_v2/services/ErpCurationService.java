package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ApiEnvelope;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.IgnoreCurationProductRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductSkuRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.QuickCurationPendingItemRes;
import com.fitoherb.fitoherb_backend_v2.entities.ErpIgnoredCatalogCode;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.infra.clients.AiCatalogRefreshClient;
import com.fitoherb.fitoherb_backend_v2.repositories.ErpIgnoredCatalogCodeRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ErpCurationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final ProductRepository productRepository;
    private final ErpIgnoredCatalogCodeRepository ignoredCatalogCodeRepository;
    private final AiCatalogRefreshClient aiCatalogRefreshClient;

    @Value("${app.erp.sync.fallback-category-slug:outros}")
    private String fallbackCategorySlug = "outros";

    @Value("${app.erp.sync.fallback-supplier-slug:fitoherb}")
    private String fallbackSupplierSlug = "fitoherb";

    public ApiEnvelope<List<QuickCurationPendingItemRes>> getCurationPending(
            int page,
            int size,
            Boolean onlyFallback,
            String search
    ) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        boolean filterFallback = Boolean.TRUE.equals(onlyFallback);

        Page<Product> pendingPage = productRepository.findCurationPending(filterFallback, search, pageable);
        List<QuickCurationPendingItemRes> items = new ArrayList<>();
        long fallbackCount = 0;

        if (pendingPage != null && pendingPage.getContent() != null) {
            for (Product p : pendingPage.getContent()) {
                QuickCurationPendingItemRes item = toPendingItemRes(p);
                if (item.isFallbackCategoryWarning() || item.isFallbackSupplierWarning()) {
                    fallbackCount++;
                }
                items.add(item);
            }
        }

        long totalElements = pendingPage != null ? pendingPage.getTotalElements() : items.size();
        int totalPages = pendingPage != null ? pendingPage.getTotalPages() : 1;

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("page", page);
        meta.put("size", safeSize);
        meta.put("pendingCount", totalElements);
        meta.put("fallbackItemsCount", fallbackCount);
        meta.put("totalElements", totalElements);
        meta.put("totalPages", totalPages);

        return new ApiEnvelope<>(items, meta);
    }

    @Transactional
    public IgnoreCurationProductRes ignorePendingProduct(String slug, String reason, String actorEmail) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com slug: " + slug));

        product.setActive(false);
        product.setCurationStatus(ProductCurationStatus.IGNORED);

        List<Long> ignoredCodes = new ArrayList<>();
        if (product.getSkus() != null) {
            for (ProductSku sku : product.getSkus()) {
                if (sku.getErpCode() != null) {
                    ignoredCodes.add(sku.getErpCode());
                    ErpIgnoredCatalogCode ignoredEntity = ErpIgnoredCatalogCode.builder()
                            .erpCode(sku.getErpCode())
                            .source("ADMIN_IGNORED")
                            .productNameSnapshot(product.getName())
                            .reason(reason)
                            .ignoredBy(actorEmail != null && !actorEmail.isBlank() ? actorEmail : "SYSTEM")
                            .ignoredAt(LocalDateTime.now())
                            .build();
                    ignoredCatalogCodeRepository.save(ignoredEntity);
                }
            }
        }

        productRepository.save(product);
        aiCatalogRefreshClient.refreshCatalog();

        return IgnoreCurationProductRes.builder()
                .slug(product.getSlug())
                .active(false)
                .curationStatus(ProductCurationStatus.IGNORED)
                .ignoredErpCodes(ignoredCodes)
                .build();
    }

    private QuickCurationPendingItemRes toPendingItemRes(Product product) {
        String catSlug = product.getCategory() != null ? product.getCategory().getSlug() : null;
        String catName = product.getCategory() != null ? product.getCategory().getName() : null;
        String supSlug = product.getSupplier() != null ? product.getSupplier().getSlug() : null;
        String supName = product.getSupplier() != null ? product.getSupplier().getName() : null;

        String expectedFallbackCat = fallbackCategorySlug != null ? fallbackCategorySlug : "outros";
        String expectedFallbackSup = fallbackSupplierSlug != null ? fallbackSupplierSlug : "fitoherb";

        boolean fallbackCatWarn = catSlug != null && catSlug.equalsIgnoreCase(expectedFallbackCat);
        boolean fallbackSupWarn = supSlug != null && supSlug.equalsIgnoreCase(expectedFallbackSup);

        List<ProductSkuRes> skuResList = new ArrayList<>();
        if (product.getSkus() != null) {
            for (ProductSku s : product.getSkus()) {
                skuResList.add(ProductSkuRes.builder()
                        .id(s.getId())
                        .flavour(s.getFlavour())
                        .presentation(s.getPresentation())
                        .erpCode(s.getErpCode())
                        .erpInactive(Boolean.TRUE.equals(s.getErpInactive()))
                        .erpInactiveConfirmed(Boolean.TRUE.equals(s.getErpInactiveConfirmed()))
                        .build());
            }
        }

        return QuickCurationPendingItemRes.builder()
                .slug(product.getSlug())
                .name(product.getName())
                .description(product.getDescription())
                .categorySlug(catSlug)
                .categoryName(catName)
                .supplierSlug(supSlug)
                .supplierName(supName)
                .active(Boolean.TRUE.equals(product.getActive()))
                .curationStatus(product.getCurationStatus())
                .fallbackCategoryWarning(fallbackCatWarn)
                .fallbackSupplierWarning(fallbackSupWarn)
                .imageUrl(null)
                .topology(ProductTopology.SIMPLE)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().format(DATE_FORMATTER) : null)
                .skus(skuResList)
                .build();
    }
}

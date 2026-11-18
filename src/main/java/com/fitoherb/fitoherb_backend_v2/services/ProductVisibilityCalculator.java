package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductVisibilityCalculator {

    private final ProductVariantSanitizer variantSanitizer;

    public void recalculate(Product product) {
        if (product == null) {
            return;
        }
        if (product.getCurationStatus() == ProductCurationStatus.IGNORED) {
            product.setActive(false);
            product.setFlavours(Collections.emptyList());
            product.setPresentation(Collections.emptyList());
            return;
        }

        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty()) {
            product.setActive(true);
            return;
        }

        boolean hasActiveSku = skus.stream().anyMatch(s -> !Boolean.TRUE.equals(s.getErpInactive()));
        product.setActive(hasActiveSku);
        product.setFlavours(variantSanitizer.projectActiveFlavours(skus));
        product.setPresentation(variantSanitizer.projectActivePresentations(skus));
    }
}

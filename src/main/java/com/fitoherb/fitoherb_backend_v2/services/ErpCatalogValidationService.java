package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductSkuReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_ERP_CODE_REQUIRED;

@Service
@RequiredArgsConstructor
public class ErpCatalogValidationService {

    private final ErpCatalogCacheService erpCatalogCacheService;
    private final ProductSkuRepository productSkuRepository;
    private final ProductVariantSanitizer variantSanitizer;
    private final ProductTopologyValidator topologyValidator;

    public Map<Long, ErpCatalogItemSnapshot> validateForProduct(ProductReq req, String currentProductId) {
        if (req == null || req.getSkus() == null || req.getSkus().isEmpty()) {
            topologyValidator.validateAndInfer(req != null ? req.getSkus() : null);
            return Collections.emptyMap();
        }

        List<ProductSkuReq> skus = req.getSkus();
        for (ProductSkuReq sku : skus) {
            if (sku == null || sku.getErpCode() == null || sku.getErpCode() <= 0) {
                throw new IllegalArgumentException(MSG_ERP_CODE_REQUIRED);
            }
            sku.setFlavour(variantSanitizer.sanitizeSingleOrNull(sku.getFlavour()));
            sku.setPresentation(variantSanitizer.sanitizeSingleOrNull(sku.getPresentation()));
        }

        topologyValidator.validateAndInfer(skus);

        Set<Long> seenErpCodes = new LinkedHashSet<>();
        Set<String> seenVariantKeys = new LinkedHashSet<>();
        for (ProductSkuReq sku : skus) {
            if (!seenErpCodes.add(sku.getErpCode())) {
                throw new UnprocessableEntityException(
                        "DUPLICATE_ERP_CODE_IN_PAYLOAD",
                        "O código ERP '" + sku.getErpCode() + "' foi informado mais de uma vez neste produto."
                );
            }
            String variantKey = variantSanitizer.buildNormalizedVariantKey(sku.getFlavour(), sku.getPresentation());
            if (!seenVariantKeys.add(variantKey)) {
                String label = (sku.getFlavour() != null ? sku.getFlavour() : "")
                        + (sku.getPresentation() != null ? " / " + sku.getPresentation() : "");
                throw new UnprocessableEntityException(
                        "DUPLICATE_VARIANT_COMBINATION",
                        "A variação '" + label.trim() + "' está duplicada neste produto."
                );
            }
        }

        Map<Long, ErpCatalogItemSnapshot> snapshots = new LinkedHashMap<>();
        List<Long> inactiveCodes = new ArrayList<>();
        for (Long erpCode : seenErpCodes) {
            ErpCatalogItemSnapshot snapshot = erpCatalogCacheService.findByErpCode(erpCode)
                    .orElseThrow(() -> new UnprocessableEntityException(
                            "ERP_CODE_NOT_FOUND",
                            "Código ERP '" + erpCode + "' não encontrado no catálogo do ERP Atrio."
                    ));
            snapshots.put(erpCode, snapshot);
            if (snapshot.isInativoPro()) {
                inactiveCodes.add(erpCode);
            }
        }

        if (!inactiveCodes.isEmpty() && !Boolean.TRUE.equals(req.getAllowInactiveErp())) {
            throw new UnprocessableEntityException(
                    "ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED",
                    "O código ERP '" + inactiveCodes.get(0) + "' está marcado como inativo no ERP Atrio (inativo_pro = 1). Confirme se deseja vinculá-lo mesmo assim.",
                    Map.of("inactiveErpCodes", inactiveCodes)
            );
        }

        List<ProductSku> conflicts = productSkuRepository.findConflictingSkus(new ArrayList<>(seenErpCodes), currentProductId);
        if (conflicts != null && !conflicts.isEmpty()) {
            ProductSku conflict = conflicts.get(0);
            String ownerName = conflict.getProduct() != null && conflict.getProduct().getName() != null
                    ? conflict.getProduct().getName()
                    : "Outro Produto";
            String ownerSlug = conflict.getProduct() != null && conflict.getProduct().getSlug() != null
                    ? conflict.getProduct().getSlug()
                    : "";
            throw new ResourceAlreadyExistsException(
                    "ERP_CODE_ALREADY_MAPPED",
                    "O código ERP '" + conflict.getErpCode() + "' já está vinculado ao produto '" + ownerName + "'.",
                    Map.of(
                            "erpCode", conflict.getErpCode(),
                            "conflictingProductName", ownerName,
                            "conflictingProductSlug", ownerSlug
                    )
            );
        }

        return snapshots;
    }
}

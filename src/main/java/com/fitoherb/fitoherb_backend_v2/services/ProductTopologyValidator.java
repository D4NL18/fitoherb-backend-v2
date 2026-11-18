package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductSkuReq;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MAX_SKUS_PER_PRODUCT;

@Component
public class ProductTopologyValidator {

    private static final String TOPOLOGY_INVALID_CODE = "PRODUCT_TOPOLOGY_INVALID";
    private static final String TOPOLOGY_INVALID_MSG =
            "Estrutura de variações inconsistente: todas as variações do produto devem seguir o mesmo padrão (Simples, Sabores, Apresentações ou Matriz Sabor × Apresentação).";

    public ProductTopology validateAndInfer(List<ProductSkuReq> skus) {
        if (skus == null || skus.isEmpty() || skus.size() > MAX_SKUS_PER_PRODUCT) {
            throw new UnprocessableEntityException(TOPOLOGY_INVALID_CODE, TOPOLOGY_INVALID_MSG);
        }

        ProductTopology expectedTopology = null;
        for (ProductSkuReq sku : skus) {
            if (sku == null) {
                throw new UnprocessableEntityException(TOPOLOGY_INVALID_CODE, TOPOLOGY_INVALID_MSG);
            }
            ProductTopology current = classifySku(sku);
            if (expectedTopology == null) {
                expectedTopology = current;
            } else if (expectedTopology != current) {
                throw new UnprocessableEntityException(TOPOLOGY_INVALID_CODE, TOPOLOGY_INVALID_MSG);
            }
        }

        if (expectedTopology == ProductTopology.SIMPLE && skus.size() > 1) {
            throw new UnprocessableEntityException(TOPOLOGY_INVALID_CODE, TOPOLOGY_INVALID_MSG);
        }

        return expectedTopology;
    }

    private ProductTopology classifySku(ProductSkuReq sku) {
        boolean hasFlavour = sku.getFlavour() != null && !sku.getFlavour().trim().isEmpty();
        boolean hasPresentation = sku.getPresentation() != null && !sku.getPresentation().trim().isEmpty();

        if (!hasFlavour && !hasPresentation) {
            return ProductTopology.SIMPLE;
        }
        if (hasFlavour && !hasPresentation) {
            return ProductTopology.FLAVOUR_ONLY;
        }
        if (!hasFlavour && hasPresentation) {
            return ProductTopology.PRESENTATION_ONLY;
        }
        return ProductTopology.MATRIX;
    }
}

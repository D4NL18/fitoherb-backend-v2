package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductAdminRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductSkuRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductCategoryMapper.class, SupplierMapper.class, ImageUrlBuilder.class})
public interface ProductMapper {

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toProductPublicUrl")
    ProductRes entityToRes(Product product);

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toProductPublicUrl")
    ProductRes entityToPublicRes(Product product);

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toProductPublicUrl")
    @Mapping(target = "erpStatus", expression = "java(product.getErpMappingStatus())")
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "topology", ignore = true)
    @Mapping(target = "skuCount", ignore = true)
    @Mapping(target = "primaryErpCode", ignore = true)
    ProductAdminRes entityToAdminRes(Product product);

    @Mapping(target = "erpDescription", ignore = true)
    ProductSkuRes skuEntityToRes(ProductSku sku);

    @AfterMapping
    default void enrichAdminRes(Product product, @MappingTarget ProductAdminRes adminRes) {
        if (product == null || adminRes == null) {
            return;
        }
        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty()) {
            adminRes.setSkus(Collections.emptyList());
            adminRes.setSkuCount(0);
            adminRes.setPrimaryErpCode(null);
            adminRes.setTopology(ProductTopology.SIMPLE);
            return;
        }

        List<ProductSku> sorted = skus.stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .toList();

        List<ProductSkuRes> skuResList = sorted.stream()
                .map(this::skuEntityToRes)
                .toList();

        adminRes.setSkus(skuResList);
        adminRes.setSkuCount(skuResList.size());
        adminRes.setPrimaryErpCode(sorted.get(0).getErpCode());
        adminRes.setTopology(inferTopologyFromSkus(sorted));
    }

    default ProductTopology inferTopologyFromSkus(List<ProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return ProductTopology.SIMPLE;
        }
        boolean anyFlavour = skus.stream().anyMatch(s -> s.getFlavour() != null && !s.getFlavour().isBlank());
        boolean anyPresentation = skus.stream().anyMatch(s -> s.getPresentation() != null && !s.getPresentation().isBlank());
        if (anyFlavour && anyPresentation) {
            return ProductTopology.MATRIX;
        }
        if (anyFlavour) {
            return ProductTopology.FLAVOUR_ONLY;
        }
        if (anyPresentation) {
            return ProductTopology.PRESENTATION_ONLY;
        }
        return ProductTopology.SIMPLE;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "erpMappingStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Product reqToEntity(ProductReq productReq);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "curationStatus", ignore = true)
    @Mapping(target = "erpMappingStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntityFromReq(ProductReq productReq, @MappingTarget Product product);
}

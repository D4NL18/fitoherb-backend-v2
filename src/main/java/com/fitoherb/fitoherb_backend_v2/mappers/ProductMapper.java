package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {ProductCategoryMapper.class, SupplierMapper.class})
public interface ProductMapper {

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toProductPublicUrl")
    ProductRes entityToRes(Product product);

    @Named("toProductPublicUrl")
    default String generateUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        return "/uploads/products/" + imagePath;
    }

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    Product reqToEntity(ProductReq productReq);

    void updateEntityFromReq(ProductReq productReq, @MappingTarget Product product);
}

package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductCategoryMapper {

    ProductCategory resToEntity(ProductCategoryRes categoryRes);

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toPublicUrl")
    ProductCategoryRes entityToRes(ProductCategory category);

    @Named("toPublicUrl")
    default String generateUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        return "/uploads/categories/" + imagePath;
    }

    ProductCategory reqToEntity(ProductCategoryReq categoryReq);

    List<ProductCategoryRes> toResList(List<ProductCategory> categories);

    void updateEntityFromReq(ProductCategoryReq categoryReq, @MappingTarget ProductCategory category);
}

package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductCategoryMapper {

    ProductCategory resToEntity(ProductCategoryRes categoryRes);

    ProductCategoryRes entityToRes(ProductCategory category);

    ProductCategory reqToEntity(ProductCategoryReq categoryReq);

    List<ProductCategoryRes> toResList(List<ProductCategory> categories);

    void updateEntityFromReq(ProductCategoryReq categoryReq, @MappingTarget ProductCategory category);
}

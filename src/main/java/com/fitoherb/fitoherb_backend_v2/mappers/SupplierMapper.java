package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SupplierReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toPublicUrl")
    SupplierRes entityToRes(Supplier supplier);

    @Named("toPublicUrl")
    default String generateUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        return "/uploads/suppliers/" + imagePath;
    }

    Supplier reqToEntity(SupplierReq supplierReq);

    List<SupplierRes> toResList(List<Supplier> suppliers);

    void updateEntityFromReq(SupplierReq supplierReq, @MappingTarget Supplier supplier);
}
package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    User registerReqToEntity(RegisterReq registerReq);

    RegisterReq entityToRegisterReq(User user);
}

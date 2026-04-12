package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    User registerReqToEntity(RegisterReq registerReq);

    RegisterReq entityToRegisterReq(User user);
}

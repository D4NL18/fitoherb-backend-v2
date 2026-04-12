package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.UserReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.UserRes;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User resToEntity(UserRes userRes);

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "birthDate", dateFormat = "dd-MM-yyyy")
    UserRes entityToRes(User user);

    @Mapping(target = "email", ignore = true)
    void updateEntityFromUserReq(UserReq userReq, @MappingTarget User user);
}

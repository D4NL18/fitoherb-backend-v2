package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ScheduledRouteReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteSummaryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ScheduledRoute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ScheduledRouteMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "depot", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "optimizationResult", ignore = true)
    @Mapping(target = "totalTimeMinutes", ignore = true)
    @Mapping(target = "totalDistanceKm", ignore = true)
    @Mapping(target = "stopsCount", ignore = true)
    ScheduledRoute reqToEntity(ScheduledRouteReq req);
    
    @Mapping(target = "depot", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "optimizationResult", ignore = true)
    ScheduledRouteRes entityToRes(ScheduledRoute entity);
    
    ScheduledRouteSummaryRes entityToSummaryRes(ScheduledRoute entity);
}

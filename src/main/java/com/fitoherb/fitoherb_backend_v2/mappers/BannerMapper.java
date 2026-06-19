package com.fitoherb.fitoherb_backend_v2.mappers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.BannerReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.BannerRes;
import com.fitoherb.fitoherb_backend_v2.entities.Banner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BannerMapper {

    Banner reqToEntity(BannerReq bannerReq);

    @Mapping(target = "createdAt", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = "toBannerPublicUrl")
    @Mapping(target = "imagePath", source = "imagePath", qualifiedByName = "toBannerPublicUrl")
    BannerRes entityToRes(Banner banner);

    @Named("toBannerPublicUrl")
    default String generateUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            return org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/Banners/")
                    .path(imagePath)
                    .toUriString();
        } catch (Exception e) {
            return "/uploads/Banners/" + imagePath;
        }
    }

    void updateEntityFromReq(BannerReq bannerReq, @MappingTarget Banner banner);
}

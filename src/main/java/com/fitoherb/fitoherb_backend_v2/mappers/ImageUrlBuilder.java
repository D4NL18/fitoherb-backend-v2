package com.fitoherb.fitoherb_backend_v2.mappers;

import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlBuilder {
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${gcp.bucket.name:fitoherb-images-bucket}")
    private String bucketName;

    private String buildUrl(String folder, String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        if ("prod".equals(activeProfile) || activeProfile.contains("prod")) {
            return "https://storage.googleapis.com/" + bucketName + "/" + folder + "/" + imagePath;
        } else {
            return "/uploads/" + folder + "/" + imagePath;
        }
    }

    @Named("toBannerPublicUrl")
    public String buildBannerUrl(String imagePath) { return buildUrl("banners", imagePath); }

    @Named("toCategoryPublicUrl")
    public String buildCategoryUrl(String imagePath) { return buildUrl("categories", imagePath); }

    @Named("toProductPublicUrl")
    public String buildProductUrl(String imagePath) { return buildUrl("products", imagePath); }

    @Named("toSupplierPublicUrl")
    public String buildSupplierUrl(String imagePath) { return buildUrl("suppliers", imagePath); }
}

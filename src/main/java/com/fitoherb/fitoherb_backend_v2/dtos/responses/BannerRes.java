package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object representing a banner with its full details")
public class BannerRes {
    @Schema(description = "Unique identifier of the banner", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Title of the banner", example = "Promoção de Inverno")
    private String title;

    @Schema(description = "Filename of the banner image", example = "banner_inverno.jpg")
    private String imagePath;

    @Schema(description = "Full URL to the banner's main image", example = "/uploads/banners/banner_inverno.jpg")
    private String imageUrl;

    @Schema(description = "Indicates whether the banner is currently active", example = "true")
    private boolean isActive;

    @Schema(description = "Ordering position for the banner on the homepage", example = "0")
    private int position;

    @Schema(description = "Formatted timestamp of banner creation", example = "12-04-2026 15:00:00")
    private String createdAt;

    @Schema(description = "Formatted timestamp of last update", example = "12-04-2026 15:00:00")
    private String updatedAt;
}

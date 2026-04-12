package com.fitoherb.fitoherb_backend_v2.DTOs.Responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Response object representing a product category")
public class ProductCategoryRes {

    @Schema(description = "Unique identifier of the category", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Display name of the category", example = "Chás e Infusões")
    private String name;

    @Schema(description = "SEO-friendly unique identifier used in URLs", example = "chas-e-infusoes")
    private String slug;

    @Schema(description = "Timestamp of when the category was created", example = "2026-04-12T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Full URL to the category's illustrative image", example = "https://api.fitoherb.com/storage/categories/chas.png")
    private String imageUrl;
}
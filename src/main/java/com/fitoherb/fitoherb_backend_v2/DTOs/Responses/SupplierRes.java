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
@Schema(description = "Response object representing a business partner or supplier")
public class SupplierRes {

    @Schema(description = "Official name of the supplier", example = "FitoHerb Matrizes Naturais Ltda")
    private String name;

    @Schema(description = "SEO-friendly unique identifier used in URLs", example = "fitoherb-natural-ltda")
    private String slug;

    @Schema(description = "Timestamp of when the supplier was registered", example = "2026-04-12T10:15:30")
    private LocalDateTime createdAt;

    @Schema(description = "Full URL to the supplier's logo or branding image", example = "https://api.fitoherb.com/storage/suppliers/fitoherb-logo.png")
    private String imageUrl;

    @Schema(description = "Indicates if the supplier is currently featured in the storefront", example = "true")
    private Boolean isHighlighted;
}
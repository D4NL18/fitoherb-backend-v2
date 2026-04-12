package com.fitoherb.fitoherb_backend_v2.DTOs.Responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Response object representing a product with its full details")
public class ProductRes {

    @Schema(description = "Display name of the product", example = "Chá de Camomila Orgânico")
    private String name;

    @Schema(description = "Full URL to the product's main image", example = "https://api.fitoherb.com/storage/products/camomila.png")
    private String imageUrl;

    @Schema(description = "Detailed description of the product and its health benefits", example = "Flores de camomila desidratadas, ideais para infusões relaxantes.")
    private String description;

    @Schema(description = "Details of the category this product belongs to")
    private ProductCategoryRes category;

    @Schema(description = "Details of the manufacturer or vendor of the product")
    private SupplierRes supplier;

    @Schema(description = "Formatted timestamp of product registration", example = "12-04-2026 15:00:00")
    private String createdAt;
}
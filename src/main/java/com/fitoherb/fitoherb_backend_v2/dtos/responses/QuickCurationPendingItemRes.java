package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item de produto ativo aguardando imagem na Central de Curadoria Rápida")
public class QuickCurationPendingItemRes {

    private String slug;
    private String name;
    private String description;
    private String categorySlug;
    private String categoryName;
    private String supplierSlug;
    private String supplierName;
    private boolean active;
    private ProductCurationStatus curationStatus;
    private boolean fallbackCategoryWarning;
    private boolean fallbackSupplierWarning;
    private String imageUrl;
    private ProductTopology topology;
    private String createdAt;
    private List<ProductSkuRes> skus;
}

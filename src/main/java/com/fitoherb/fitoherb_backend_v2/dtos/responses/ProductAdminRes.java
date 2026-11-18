package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.enums.ErpMappingStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO administrativo de produto contendo dados de governança ERP, topologia e SKUs")
public class ProductAdminRes extends ProductRes {

    @Schema(description = "Topologia estrutural das variações do produto", example = "FLAVOUR_ONLY")
    private ProductTopology topology;

    @Schema(description = "Status de governança ERP do produto", example = "COMPLETE")
    private ErpMappingStatus erpStatus;

    @Schema(description = "Quantidade de SKUs vinculados", example = "3")
    private Integer skuCount;

    @Schema(description = "Código ERP principal (primeiro SKU ordenado)", example = "131476")
    private Long primaryErpCode;

    @Schema(description = "Indica se o produto está ativo e visível na vitrine pública", example = "true")
    private Boolean active;

    @Schema(description = "Status de curadoria de imagem e metadados", example = "COMPLETE")
    private ProductCurationStatus curationStatus;

    @Schema(description = "Lista detalhada dos SKUs do produto")
    private List<ProductSkuRes> skus;
}

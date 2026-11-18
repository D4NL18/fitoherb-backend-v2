package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item do catálogo do ERP Atrio consultado em modo somente-leitura")
public class ErpCatalogItemRes {

    @Schema(description = "Código Id_Pro no ERP Atrio", example = "131476")
    private Long erpCode;

    @Schema(description = "Descrição oficial no ERP Atrio", example = "100% PURE WHEY 900G BAUNILHA")
    private String erpDescription;

    @Schema(description = "Código de barras EAN", example = "7896438201234")
    private String ean;

    @Schema(description = "Marca comercial no ERP", example = "PROBIOTICA")
    private String erpBrand;

    @Schema(description = "Fornecedor comercial ou marca", example = "PROBIOTICA")
    private String erpSupplier;

    @Schema(description = "Indica se o registro está inativo no ERP (inativo_pro = 1)", example = "false")
    private boolean inativoPro;

    @Schema(description = "Indica presença na view Externo01.Produto", example = "true")
    private boolean inExterno01;

    @Schema(description = "Indica se já está vinculado a outro produto no site", example = "false")
    private boolean alreadyMapped;

    @Schema(description = "Slug do produto ao qual já está vinculado")
    private String mappedProductSlug;

    @Schema(description = "Nome do produto ao qual já está vinculado")
    private String mappedProductName;

    @Schema(description = "Sabor no produto vinculado")
    private String mappedFlavour;

    @Schema(description = "Apresentação no produto vinculado")
    private String mappedPresentation;
}

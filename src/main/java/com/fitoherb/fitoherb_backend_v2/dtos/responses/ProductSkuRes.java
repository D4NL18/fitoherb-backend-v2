package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representa um SKU de variação vinculado a um produto no painel administrativo")
public class ProductSkuRes {

    @Schema(description = "Identificador único do SKU", example = "a1111111-1111-1111-1111-111111111111")
    private String id;

    @Schema(description = "Sabor da variação", example = "Baunilha")
    private String flavour;

    @Schema(description = "Apresentação ou gramatura da variação", example = "900g")
    private String presentation;

    @Schema(description = "Código Id_Pro vinculado no ERP Atrio", example = "131476")
    private Long erpCode;

    @Schema(description = "Indica se o código encontra-se inativo no ERP Atrio", example = "false")
    private Boolean erpInactive;

    @Schema(description = "Indica se o vínculo inativo foi confirmado explicitamente pelo operador", example = "false")
    private Boolean erpInactiveConfirmed;

    @Schema(description = "Descrição original do item no catálogo ERP Atrio")
    private String erpDescription;
}

package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sugestão determinística de sabor e apresentação para um SKU do ERP Atrio")
public class SuggestedVariantItemRes {

    @Schema(description = "Código Id_Pro no ERP Atrio", example = "131476")
    private Long erpCode;

    @Schema(description = "Descrição original no ERP Atrio", example = "100% PURE WHEY 900G BAUN PROBIOTICA")
    private String erpDescription;

    @Schema(description = "Sabor sugerido após expansão determinística", example = "Baunilha")
    private String suggestedFlavour;

    @Schema(description = "Apresentação/gramatura sugerida", example = "900g")
    private String suggestedPresentation;

    @Schema(description = "Indica se o item está inativo no ERP", example = "false")
    private boolean inativoPro;

    @Schema(description = "Indica presença na view Externo01.Produto", example = "true")
    private boolean inExterno01;

    @Schema(description = "Indica se o código já está vinculado a outro produto", example = "false")
    private boolean alreadyMappedElsewhere;

    @Schema(description = "Nome do produto conflitante quando já vinculado")
    private String conflictingProductName;

    @Schema(description = "Alerta de embalagem coletiva (Display/Caixa) quando detectado")
    private String packageWarning;
}

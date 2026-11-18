package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MAX_VARIANT_LENGTH;
import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_ERP_CODE_REQUIRED;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representa um SKU de variação ou produto simples vinculado ao código Id_Pro do ERP Atrio")
public class ProductSkuReq {

    @Schema(description = "Código oficial Id_Pro no ERP Atrio (> 0)", example = "131476", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = MSG_ERP_CODE_REQUIRED)
    @Positive(message = MSG_ERP_CODE_REQUIRED)
    private Long erpCode;

    @Schema(description = "Sabor da variação (nulo em produto simples ou somente apresentação)", example = "Baunilha")
    @Size(max = MAX_VARIANT_LENGTH)
    private String flavour;

    @Schema(description = "Apresentação ou gramatura da variação (nulo em produto simples ou somente sabor)", example = "900g")
    @Size(max = MAX_VARIANT_LENGTH)
    private String presentation;
}

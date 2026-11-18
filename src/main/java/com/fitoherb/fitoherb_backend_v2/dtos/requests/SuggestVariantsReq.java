package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MAX_SKUS_PER_PRODUCT;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição para sugestão determinística de sabores e apresentações a partir de códigos ERP")
public class SuggestVariantsReq {

    @NotEmpty
    @Size(min = 1, max = MAX_SKUS_PER_PRODUCT)
    @Schema(description = "Lista de códigos Id_Pro selecionados no ERP Atrio", example = "[131476, 131477, 131478]")
    private List<@NotNull @Positive Long> erpCodes;

    @Schema(description = "Slug do produto em edição para excluir da detecção de conflito", example = "100-pure-whey-900g-probiotica")
    private String excludeProductSlug;
}

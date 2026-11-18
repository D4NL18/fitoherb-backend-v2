package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado da sugestão em lote de variações e topologia a partir do ERP Atrio")
public class SuggestVariantsRes {

    @Schema(description = "Topologia sugerida para o conjunto de SKUs", example = "FLAVOUR_ONLY")
    private ProductTopology suggestedTopology;

    @Schema(description = "Indica se há ao menos um item inativo no ERP na seleção", example = "false")
    private boolean hasInactiveItems;

    @Schema(description = "Indica se há ao menos um item já vinculado a outro produto", example = "false")
    private boolean hasConflictingItems;

    @Schema(description = "Itens sugeridos com sabores e apresentações extraídos")
    private List<SuggestedVariantItemRes> items;
}

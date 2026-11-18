package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import com.fitoherb.fitoherb_backend_v2.enums.DailySyncMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parâmetros de execução da sincronização diária com o ERP Atrio")
public class DailySyncReq {

    @Builder.Default
    @Schema(description = "Modo de execução da sincronização", example = "INCREMENTAL")
    private DailySyncMode mode = DailySyncMode.INCREMENTAL;

    @Builder.Default
    @Schema(description = "Execução em modo simulação sem persistir alterações", example = "false")
    private Boolean dryRun = false;

    public boolean isDryRun() {
        return Boolean.TRUE.equals(dryRun);
    }
}

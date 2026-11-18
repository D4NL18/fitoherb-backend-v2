package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.enums.DailySyncMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado da execução da sincronização diária ERP -> Site")
public class DailySyncRes {

    private String runId;
    private DailySyncMode mode;
    private String status;
    private boolean dryRun;
    private String baselineCutoffAt;
    private int checkedMappedSkus;
    private int deactivatedProducts;
    private int partiallyDeactivatedProducts;
    private int reactivatedProducts;
    private int createdPendingProducts;
    private int ignoredCodesSkipped;
    private boolean aiCatalogRefreshed;
}

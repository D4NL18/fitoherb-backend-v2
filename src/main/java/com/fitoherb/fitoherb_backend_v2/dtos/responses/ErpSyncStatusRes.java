package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Status atual da sincronização diária ERP Atrio")
public class ErpSyncStatusRes {

    private boolean inProgress;
    private String activeRunId;
    private String lastSuccessfulSyncAt;
    private String baselineCutoffAt;
    private boolean autoSyncEnabled;
    private long pendingCurationCount;
}

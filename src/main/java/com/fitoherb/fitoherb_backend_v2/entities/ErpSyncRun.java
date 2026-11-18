package com.fitoherb.fitoherb_backend_v2.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "erp_sync_runs")
@Entity(name = "erp_sync_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ErpSyncRun {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "triggered_by", length = 150)
    private String triggeredBy;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Builder.Default
    @Column(name = "deactivated_count", nullable = false)
    private Integer deactivatedCount = 0;

    @Builder.Default
    @Column(name = "reactivated_count", nullable = false)
    private Integer reactivatedCount = 0;

    @Builder.Default
    @Column(name = "partial_variant_updated_count", nullable = false)
    private Integer partialVariantUpdatedCount = 0;

    @Builder.Default
    @Column(name = "created_count", nullable = false)
    private Integer createdCount = 0;

    @Builder.Default
    @Column(name = "skipped_ignored_count", nullable = false)
    private Integer skippedIgnoredCount = 0;

    @Column(name = "error_summary", length = 500)
    private String errorSummary;
}

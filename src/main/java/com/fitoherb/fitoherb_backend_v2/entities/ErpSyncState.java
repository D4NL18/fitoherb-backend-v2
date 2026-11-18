package com.fitoherb.fitoherb_backend_v2.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "erp_sync_state")
@Entity(name = "erp_sync_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ErpSyncState {

    @Id
    @Builder.Default
    @Column(name = "id")
    private Short id = 1;

    @Column(name = "baseline_cutoff_at")
    private LocalDateTime baselineCutoffAt;

    @Builder.Default
    @Column(name = "auto_sync_enabled", nullable = false)
    private Boolean autoSyncEnabled = false;

    @Column(name = "last_successful_sync_at")
    private LocalDateTime lastSuccessfulSyncAt;

    @Builder.Default
    @Column(name = "in_progress", nullable = false)
    private Boolean inProgress = false;

    @Column(name = "lock_acquired_at")
    private LocalDateTime lockAcquiredAt;

    @Column(name = "active_run_id", length = 36)
    private String activeRunId;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}

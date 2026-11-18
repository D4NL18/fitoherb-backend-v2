package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ErpSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ErpSyncStateRepository extends JpaRepository<ErpSyncState, Short> {

    @Modifying
    @Query("UPDATE erp_sync_state s SET s.inProgress = true, s.activeRunId = :runId, s.lockAcquiredAt = :now, s.updatedAt = :now " +
           "WHERE s.id = 1 AND (s.inProgress = false OR s.lockAcquiredAt IS NULL OR s.lockAcquiredAt < :staleCutoff)")
    int tryAcquireLock(
            @Param("runId") String runId,
            @Param("now") LocalDateTime now,
            @Param("staleCutoff") LocalDateTime staleCutoff
    );

    @Modifying
    @Query("UPDATE erp_sync_state s SET s.inProgress = false, s.activeRunId = null, s.lockAcquiredAt = null, s.updatedAt = :now WHERE s.id = 1")
    int releaseLock(@Param("now") LocalDateTime now);
}

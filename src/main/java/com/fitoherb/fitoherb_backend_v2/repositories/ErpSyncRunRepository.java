package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ErpSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ErpSyncRunRepository extends JpaRepository<ErpSyncRun, String> {

    Optional<ErpSyncRun> findTopByOrderByStartedAtDesc();

    @Modifying
    @Query("DELETE FROM erp_sync_runs r WHERE r.startedAt < :cutoff AND r.status <> 'RUNNING'")
    int deleteOldRunsBefore(@Param("cutoff") LocalDateTime cutoff);
}

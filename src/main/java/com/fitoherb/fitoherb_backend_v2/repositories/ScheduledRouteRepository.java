package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ScheduledRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledRouteRepository extends JpaRepository<ScheduledRoute, String> {
    
    List<ScheduledRoute> findAllByUserIdAndRouteDateBetweenOrderByRouteDateAsc(String userId, LocalDate startDate, LocalDate endDate);
    
    Optional<ScheduledRoute> findByUserIdAndRouteDate(String userId, LocalDate routeDate);
    
    List<ScheduledRoute> findAllByUserIdOrderByRouteDateAsc(String userId);
    
    @Modifying
    @Query("DELETE FROM scheduled_routes s WHERE s.routeDate < :cutoffDate")
    int deleteByRouteDateBefore(@Param("cutoffDate") LocalDate cutoffDate);
    
    @Query("SELECT s.routeDate FROM scheduled_routes s WHERE s.user.id = :userId AND s.routeDate BETWEEN :startDate AND :endDate ORDER BY s.routeDate ASC")
    List<LocalDate> findRouteDatesByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

package com.fitoherb.fitoherb_backend_v2.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduledRouteCleanupJob {
    
    private final ScheduledRouteService service;
    
    @Scheduled(cron = "0 0 2 * * *") // Todo dia às 02:00 AM
    @Transactional
    public void cleanupExpiredRoutes() {
        service.cleanupExpiredRoutes();
    }
}

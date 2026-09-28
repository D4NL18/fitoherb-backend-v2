package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ScheduledRouteReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteSummaryRes;
import com.fitoherb.fitoherb_backend_v2.services.ScheduledRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scheduled-routes")
@Tag(name = "Scheduled Routes", description = "Management of scheduled seller routes with calendar integration")
public class ScheduledRouteController {

    private final ScheduledRouteService service;

    @PostMapping
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @Operation(summary = "Save or update a scheduled route for a specific date")
    public ResponseEntity<ScheduledRouteRes> saveRoute(@RequestBody @Valid ScheduledRouteReq req) {
        return ResponseEntity.ok(service.saveRoute(req));
    }

    @GetMapping
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @Operation(summary = "List scheduled routes within the visible window")
    public ResponseEntity<List<ScheduledRouteSummaryRes>> listRoutes() {
        return ResponseEntity.ok(service.getRoutesByDateRange());
    }

    @GetMapping("/dates")
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @Operation(summary = "List dates with scheduled routes within the visible window")
    public ResponseEntity<List<LocalDate>> getRouteDates() {
        return ResponseEntity.ok(service.getRouteDates());
    }

    @GetMapping("/{date}")
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @Operation(summary = "Get a complete scheduled route by date")
    public ResponseEntity<ScheduledRouteRes> getRouteByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.getRouteByDate(date));
    }

    @DeleteMapping("/{date}")
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @Operation(summary = "Delete a scheduled route by date")
    public ResponseEntity<Void> deleteRouteByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.deleteRouteByDate(date);
        return ResponseEntity.noContent().build();
    }
}

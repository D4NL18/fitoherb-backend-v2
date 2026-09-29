package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SavedLocationReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SavedLocationRes;
import com.fitoherb.fitoherb_backend_v2.services.SavedLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/saved-locations")
@Tag(name = "Saved Locations", description = "Management of seller bases and client stop points for commercial routing.")
public class SavedLocationController {

    private final SavedLocationService savedLocationService;

    @Operation(summary = "List current user's saved locations", description = "Retrieves all saved locations (both bases and favorite stops) belonging to the authenticated seller or admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of saved locations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT session invalid or expired"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role")
    })
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @GetMapping
    public ResponseEntity<List<SavedLocationRes>> getMySavedLocations() {
        List<SavedLocationRes> locations = savedLocationService.getMySavedLocations();
        return ResponseEntity.ok(locations);
    }

    @Operation(summary = "Get user's active base location", description = "Retrieves the active starting point (BASE) configured for the current seller.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Base location found"),
            @ApiResponse(responseCode = "204", description = "No base location configured"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @GetMapping("/base")
    public ResponseEntity<SavedLocationRes> getMyBaseLocation() {
        return savedLocationService.getMyBaseLocation()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Get saved location by ID", description = "Retrieves details of a specific saved location.")
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @GetMapping("/{id}")
    public ResponseEntity<SavedLocationRes> getById(
            @Parameter(description = "UUID of the saved location") @PathVariable String id) {
        SavedLocationRes location = savedLocationService.getById(id);
        return ResponseEntity.ok(location);
    }

    @Operation(summary = "Create a new saved location", description = "Registers a new location. If marked as BASE, unsets any existing base for the seller.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Saved location created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload")
    })
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @PostMapping
    public ResponseEntity<SavedLocationRes> create(@Valid @RequestBody SavedLocationReq req) {
        SavedLocationRes res = savedLocationService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Operation(summary = "Update an existing saved location", description = "Modifies an existing saved location.")
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @PutMapping("/{id}")
    public ResponseEntity<SavedLocationRes> update(
            @Parameter(description = "UUID of the saved location") @PathVariable String id,
            @Valid @RequestBody SavedLocationReq req) {
        SavedLocationRes res = savedLocationService.update(id, req);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Delete a saved location", description = "Removes a location from the seller's saved places.")
    @PreAuthorize("@authorizationService.isAdminOrSeller()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the saved location") @PathVariable String id) {
        savedLocationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ApiKeyReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ApiKeyCreatedRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ApiKeyRes;
import com.fitoherb.fitoherb_backend_v2.services.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api-keys")
@Tag(name = "API Keys", description = "Management of admin API keys (JWT admin login required; API keys cannot manage keys).")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Operation(summary = "Create API key", description = "Generates a new admin API key. The secret is returned only in this response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Key created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping
    public ResponseEntity<ApiKeyCreatedRes> create(@RequestBody @Valid ApiKeyReq req) {
        var created = apiKeyService.create(req.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Cache-Control", "no-store")
                .body(new ApiKeyCreatedRes(ApiKeyRes.from(created.entity()), created.rawKey()));
    }

    @Operation(summary = "List API keys", description = "Lists key metadata (never the secret).")
    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping
    public ResponseEntity<List<ApiKeyRes>> list() {
        return ResponseEntity.ok(apiKeyService.findAll().stream().map(ApiKeyRes::from).toList());
    }

    @Operation(summary = "Revoke API key", description = "Revokes a key immediately. Idempotent.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Key revoked"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable String id) {
        apiKeyService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}

package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.entities.ApiKey;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "API key metadata (the secret value is never returned after creation)")
public record ApiKeyRes(
        String id,
        String name,
        String keyPrefix,
        boolean active,
        LocalDateTime lastUsedAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        String createdBy
) {
    public static ApiKeyRes from(ApiKey key) {
        return new ApiKeyRes(key.getId(), key.getName(), key.getKeyPrefix(), key.isActive(),
                key.getLastUsedAt(), key.getRevokedAt(), key.getCreatedAt(), key.getCreatedBy());
    }
}

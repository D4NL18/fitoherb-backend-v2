package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of creating an API key. The secret is shown only once.")
public record ApiKeyCreatedRes(
        ApiKeyRes key,
        @Schema(description = "Secret value to send in the X-API-Key header. Store it now: it cannot be retrieved again.")
        String apiKey
) {
}

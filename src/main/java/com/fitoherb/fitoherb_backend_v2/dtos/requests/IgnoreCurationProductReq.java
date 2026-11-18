package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Motivo opcional para descarte auditável de produto pendente de curadoria")
public class IgnoreCurationProductReq {

    @Size(max = 255)
    @Schema(description = "Motivo do descarte", example = "Brinde interno")
    private String reason;
}

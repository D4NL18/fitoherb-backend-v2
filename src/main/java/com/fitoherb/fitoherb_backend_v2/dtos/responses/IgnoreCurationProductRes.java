package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta do descarte auditável de produto pendente de curadoria")
public class IgnoreCurationProductRes {

    private String slug;
    private boolean active;
    private ProductCurationStatus curationStatus;
    private List<Long> ignoredErpCodes;
}

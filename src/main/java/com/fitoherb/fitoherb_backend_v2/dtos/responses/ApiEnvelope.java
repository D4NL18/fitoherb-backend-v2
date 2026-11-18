package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envelope canônico data/meta para respostas da API")
public class ApiEnvelope<T> {

    @Schema(description = "Payload principal da resposta")
    private T data;

    @Schema(description = "Metadados da operação e paginação")
    private Map<String, Object> meta;
}

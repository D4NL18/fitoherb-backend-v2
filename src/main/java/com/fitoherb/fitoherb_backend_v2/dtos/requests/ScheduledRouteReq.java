package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_REQUIRED_FIELD;

@Getter 
@Setter
@Schema(description = "Request para salvar/atualizar uma rota agendada")
public class ScheduledRouteReq {
    
    @NotNull(message = MSG_REQUIRED_FIELD)
    @Schema(description = "Data da rota", example = "2026-09-28")
    private LocalDate routeDate;
    
    @Schema(description = "Horário de partida", example = "08:00")
    private String departureTime;
    
    @Schema(description = "Se retorna à base ao final")
    private Boolean returnToDepot = true;
    
    @NotNull(message = MSG_REQUIRED_FIELD)
    @Schema(description = "Ponto de partida (depot) como objeto JSON")
    private Object depot;
    
    @NotNull(message = MSG_REQUIRED_FIELD)
    @Schema(description = "Lista de paradas como array JSON")
    private Object stops;
    
    @Schema(description = "Resultado da otimização da IA como objeto JSON")
    private Object optimizationResult;
}

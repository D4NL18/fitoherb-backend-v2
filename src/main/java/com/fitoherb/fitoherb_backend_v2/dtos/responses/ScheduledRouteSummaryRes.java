package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor
public class ScheduledRouteSummaryRes {
    private String id;
    private LocalDate routeDate;
    private String departureTime;
    private Integer stopsCount;
    private Double totalTimeMinutes;
    private Double totalDistanceKm;
}

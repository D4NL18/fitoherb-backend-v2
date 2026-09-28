package com.fitoherb.fitoherb_backend_v2.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "scheduled_routes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "route_date"}))
@Entity(name = "scheduled_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class ScheduledRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Column(name = "departure_time", length = 10)
    private String departureTime;

    @Column(name = "return_to_depot")
    private Boolean returnToDepot = true;

    @Column(name = "depot", columnDefinition = "TEXT", nullable = false)
    private String depot;

    @Column(name = "stops", columnDefinition = "TEXT", nullable = false)
    private String stops;

    @Column(name = "optimization_result", columnDefinition = "TEXT")
    private String optimizationResult;

    @Column(name = "total_time_minutes")
    private Double totalTimeMinutes;

    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "stops_count")
    private Integer stopsCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}

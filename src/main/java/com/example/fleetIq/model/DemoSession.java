package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "demo_sessions")
@Data
@NoArgsConstructor
public class DemoSession {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "viaje_id", nullable = false)
    private String viajeId;

    @Column(name = "imei", nullable = false)
    private String imei;

    @Column(name = "route_points", columnDefinition = "JSON", nullable = false)
    private String routePoints; // JSON string de List<double[]>

    @Column(name = "cursor", nullable = false)
    private Integer cursor = 0;

    @Column(name = "interval_sec", nullable = false)
    private Integer intervalSec = 30;

    @Column(name = "status", nullable = false)
    private String status = "active"; // active | stopped | completed

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

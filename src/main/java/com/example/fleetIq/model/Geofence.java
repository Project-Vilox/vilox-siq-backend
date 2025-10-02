package com.example.fleetIq.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "geofences")
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imei")
    private String imei;

    @Column(name = "name")
    private String name;

    @Column(name = "alarmtype")
    private Integer alarmtype;

    @Column(name = "points", columnDefinition = "JSON")
    private String points; // JSON string, e.g. "[[-8.0, -79.0], [-8.1, -79.1], ...]]"

    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime creationDate;

    // Default constructor
    public Geofence() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAlarmtype() {
        return alarmtype;
    }

    public void setAlarmtype(Integer alarmtype) {
        this.alarmtype = alarmtype;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
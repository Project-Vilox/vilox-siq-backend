package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "duplicate_alarms")
public class DuplicateAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imei")
    private String imei;

    @Column(name = "geofence_id")
    private Long geofenceId;

    @Column(name = "alarm_type")
    private String alarmType;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "track_time")
    private Long trackTime;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "entry_time")
    private Long entryTime;

    @Column(name = "exit_time")
    private Long exitTime;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "error_description")
    private String errorDescription;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "alarms", uniqueConstraints = { // Modificado
        @UniqueConstraint(columnNames = {"imei", "geofence_id", "exit_time"}) // Modificado
}) // Modificado
public class Alarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imei", nullable = false, length = 15)
    private String imei;

    @Column(name = "geofence_id", nullable = false)
    private Long geofenceId;

    @Column(name = "track_time")
    private Long trackTime;

    @Column(name = "alarm_type", length = 50)
    private String alarmType;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "plate_number", length = 50)
    private String plateNumber;

    @Column(name = "entry_time")
    private Long entryTime;

    @Column(name = "exit_time")
    private Long exitTime;

    @Column(name = "duration")
    private Long duration;
}
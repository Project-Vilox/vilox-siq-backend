package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imei", nullable = false, length = 15)
    private String imei;

    @Column(name = "gpstime", nullable = false)
    private Long gpstime;

    @Column(name = "hearttime")
    private Long hearttime;

    @Column(name = "systemtime")
    private Long systemtime;

    @Column(name = "servertime")
    private Long servertime;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "course")
    private Double course;

    @Column(name = "acctime")
    private Long acctime;

    @Column(name = "accstatus")
    private Boolean accstatus;

    @Column(name = "doorstatus")
    private Integer doorstatus;

    @Column(name = "chargestatus")
    private Integer chargestatus;

    @Column(name = "oilpowerstatus")
    private Integer oilpowerstatus;

    @Column(name = "defencestatus")
    private Integer defencestatus;

    @Column(name = "datastatus")
    private Integer datastatus;

    @Column(name = "battery")
    private Double battery;

    @Column(name = "mileage")
    private Long mileage;

    @Column(name = "todaymileage")
    private Long todaymileage;

    @Column(name = "externalpower", length = 10)
    private String externalpower;

    @Column(name = "fuel", length = 10)
    private String fuel;

    @Column(name = "fueltime")
    private Long fueltime;

    @Column(name = "temperature", length = 255)
    private String temperature;

    @Column(name = "temperaturetime")
    private Long temperaturetime;

    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime creationDate;

    @Column(name = "alarm_status")
    private String alarmStatus = "PENDING"; // Supports PENDING, ALARM_REGISTERED, ERROR_DUPLICATE, EVALUATED

    @Column(name = "alarm_error_description", length = 255)
    private String alarmErrorDescription;
}
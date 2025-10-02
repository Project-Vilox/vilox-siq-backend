package com.example.fleetIq.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrackDto {
    private Long id;
    private String imei;
    private Long gpstime;
    private Long hearttime;
    private Long systemtime;
    private Long servertime;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double course;
    private Long acctime;
    private Boolean accstatus;
    private Integer doorstatus;
    private Integer chargestatus;
    private Integer oilpowerstatus;
    private Integer defencestatus;
    private Integer datastatus;
    private Double battery;
    private Long mileage;
    private Long todaymileage;
    private String externalpower;
    private String fuel;
    private Long fueltime;
    private String temperature;
    private Long temperaturetime;
    private LocalDateTime creationDate;
}
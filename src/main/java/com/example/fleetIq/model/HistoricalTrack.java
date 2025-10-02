package com.example.fleetIq.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "historical_track")
public class HistoricalTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imei", nullable = false, length = 15)
    private String imei;

    @Column(name = "time", nullable = false)
    private Long time;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "direction")
    private Double direction;

    @Column(name = "acc_status")
    private Boolean accStatus;

    @Column(name = "voltage")
    private Double voltage;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public Long getTime() { return time; }
    public void setTime(Long time) { this.time = time; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }
    public Double getDirection() { return direction; }
    public void setDirection(Double direction) { this.direction = direction; }
    public Boolean getAccStatus() { return accStatus; }
    public void setAccStatus(Boolean accStatus) { this.accStatus = accStatus; }
    public Double getVoltage() { return voltage; }
    public void setVoltage(Double voltage) { this.voltage = voltage; }
}
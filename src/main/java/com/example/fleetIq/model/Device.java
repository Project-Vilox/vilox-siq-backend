package com.example.fleetIq.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(name = "imei")
    private String imei;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "online_time")
    private Long onlineTime;

    @Column(name = "platform_due_time")
    private Long platformDueTime;

    @Column(name = "sim_card")
    private String simCard;

    @Column(name = "iccid")
    private String iccid;

    @Column(name = "activated_time")
    private Long activatedTime;

    @Column(name = "user_due_time")
    private Long userDueTime;

    // Default constructor
    public Device() {}

    // Getters and Setters
    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public Long getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(Long onlineTime) {
        this.onlineTime = onlineTime;
    }

    public Long getPlatformDueTime() {
        return platformDueTime;
    }

    public void setPlatformDueTime(Long platformDueTime) {
        this.platformDueTime = platformDueTime;
    }

    public String getSimCard() {
        return simCard;
    }

    public void setSimCard(String simCard) {
        this.simCard = simCard;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public Long getActivatedTime() {
        return activatedTime;
    }

    public void setActivatedTime(Long activatedTime) {
        this.activatedTime = activatedTime;
    }

    public Long getUserDueTime() {
        return userDueTime;
    }

    public void setUserDueTime(Long userDueTime) {
        this.userDueTime = userDueTime;
    }
}
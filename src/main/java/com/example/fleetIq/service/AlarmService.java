package com.example.fleetIq.service;

import com.example.fleetIq.model.Alarm;
import com.example.fleetIq.model.Geofence;
import com.example.fleetIq.model.Track;

import java.util.List;

public interface AlarmService {
    void checkAndLogAlarm(Track track) throws Exception;
    List<Alarm> getAlarms();
    List<Alarm> findAlarmsByFilters(String imei, Long startTimestamp, Long endTimestamp);
}
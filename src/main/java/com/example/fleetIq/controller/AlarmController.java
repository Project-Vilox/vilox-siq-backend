package com.example.fleetIq.controller;

import com.example.fleetIq.model.Alarm;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.service.AlarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AlarmController {

    @Autowired
    private AlarmService alarmService;

    @PostMapping("/alarms/check")
    public ResponseEntity<String> checkAndLogAlarm(@RequestBody Track track) {
        try {
            alarmService.checkAndLogAlarm(track);
            return ResponseEntity.ok("Alarm check completed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking alarm: " + e.getMessage());
        }
    }

    @GetMapping("/alarms")
    public List<Alarm> getAlarms(
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {

        Long startTimestamp = startDateTime != null ? startDateTime.atZone(ZoneId.of("America/Lima")).toEpochSecond() : null;
        Long endTimestamp = endDateTime != null ? endDateTime.atZone(ZoneId.of("America/Lima")).toEpochSecond() : null;

        return alarmService.findAlarmsByFilters(imei, startTimestamp, endTimestamp);
    }



}
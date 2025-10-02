package com.example.fleetIq.repository;

import com.example.fleetIq.model.DuplicateAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateAlarmRepository extends JpaRepository<DuplicateAlarm, Long> {
    boolean existsByImeiAndGeofenceIdAndAlarmType(String imei, Long geofenceId, String alarmType);
}
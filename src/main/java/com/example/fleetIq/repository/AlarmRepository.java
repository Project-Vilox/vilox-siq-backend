package com.example.fleetIq.repository;

import com.example.fleetIq.model.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // ⭐ MÉTODO PRINCIPAL: Verificar si existe combinación específica imei + geofence_id + alarm_type
    boolean existsByImeiAndGeofenceIdAndAlarmType(String imei, Long geofenceId, String alarmType);

    // Método para verificar si ya existe una alarma para un IMEI en una geocerca específica
    boolean existsByImeiAndGeofenceId(String imei, Long geofenceId);

    // Método para verificar si existe una entrada activa (sin salida)
    boolean existsByImeiAndGeofenceIdAndExitTimeIsNull(String imei, Long geofenceId);

    // Método para obtener una alarma activa (sin tiempo de salida)
    Alarm findByImeiAndGeofenceIdAndExitTimeIsNull(String imei, Long geofenceId);

    // Método para obtener todas las alarmas activas de un IMEI
    List<Alarm> findByImeiAndExitTimeIsNull(String imei);

    // Método para obtener todas las alarmas activas
    List<Alarm> findByExitTimeIsNull();

    // Método adicional para obtener alarmas por IMEI
    List<Alarm> findByImei(String imei);

    // Método adicional para obtener alarmas por geocerca
    List<Alarm> findByGeofenceId(Long geofenceId);

    // Método para obtener alarmas por tipo específico
    List<Alarm> findByAlarmType(String alarmType);

    // Método para obtener registros específicos de entrada y salida
    List<Alarm> findByImeiAndGeofenceIdAndAlarmType(String imei, Long geofenceId, String alarmType);


    List<Alarm> findByTrackTimeBetween(Long start, Long end);

    List<Alarm> findByImeiAndTrackTimeBetween(String imei, Long start, Long end);

    Alarm findByImeiAndGeofenceIdAndAlarmTypeAndExitTimeIsNull(String imei, Long geofenceId, String alarmType);
}
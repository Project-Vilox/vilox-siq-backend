package com.example.fleetIq.repository;

import com.example.fleetIq.model.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Modificado
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
    List<Alarm> findByImeiAndGeofenceIdAndExitTimeIsNull(String imei, Long geofenceId);

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

    /* Modificado: Query para obtener tramos pendientes con información de geocercas
    @Query(value = "SELECT t.id as tramo_id, " + // Modificado
            "t.hora_llegada_programada, " + // Modificado
            "t.hora_salida_programada, " + // Modificado
            "t.sla_minutos, " + // Modificado
            "t.tipo_actividad, " + // Modificado
            "t.estado " + // Modificado
            "FROM public.tramos t " + // Modificado
            "INNER JOIN vehiculos v ON t.tracto = v.id " +
            "WHERE t.estado in ('pendiente','en_curso') " + // Modificado
            "AND v.imei = :imei", // Modificado
            nativeQuery = true) // Modificado
    List<Object[]> findTramosPendientesByImeiAndGeofence(@Param("imei") String imei, @Param("geofenceId") Long geofenceId); */

    // Query para obtener tramos pendientes con información de geocercas
    @Query(value = "SELECT t.id AS tramo_id, " +
            "t.hora_llegada_programada, " +
            "t.hora_salida_programada, " +
            "e.id as establecimiento_id, " +
            "t.tipo_actividad, " +
            " t.viaje_id, " +
            "t.orden, " +
            "gpe.geocerca_id, " +
            "gpe.tipo, gpe.orden as ordenado " +
            "FROM public.tramos t " +
            "INNER JOIN public.vehiculos v ON t.tracto = v.id " +
            "INNER JOIN public.establecimientos e ON t.establecimiento_origen_id = e.id " +
            "INNER JOIN public.geocercas_por_establecimiento gpe ON e.id = gpe.establecimiento_id " +
            "WHERE t.estado IN ('pendiente', 'en_curso','completado') " +
            "AND v.imei = :imei " +
            "ORDER BY gpe.orden ASC",
            nativeQuery = true)
    List<Object[]> findTramosPendientesByImeiAndGeofence(@Param("imei") String imei);

}
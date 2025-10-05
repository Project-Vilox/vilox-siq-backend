package com.example.fleetIq.service;

import com.example.fleetIq.model.Alarm;
import com.example.fleetIq.model.Device;
import com.example.fleetIq.model.DuplicateAlarm;
import com.example.fleetIq.model.Geofence;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.repository.AlarmRepository;
import com.example.fleetIq.repository.DeviceRepository;
import com.example.fleetIq.repository.DuplicateAlarmRepository;
import com.example.fleetIq.repository.GeofenceRepository;
import com.example.fleetIq.repository.TrackRepository;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class AlarmServiceImpl implements AlarmService {

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DuplicateAlarmRepository duplicateAlarmRepository;

    @Scheduled(fixedRate = 60000)
    public void checkAlarmsAutomatically() {
        try {
            // Capturar hora de inicio
            LocalDateTime startTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<Track> latestPendingTracks = trackRepository.findByAlarmStatus("PENDING")
                    .stream()
                    .sorted(Comparator.comparing(Track::getGpstime, Comparator.reverseOrder()))
                    .toList();

            // Log en consola: Cantidad de registros a evaluar con hora de inicio
            System.out.println("📊 Estamos evaluando " + latestPendingTracks.size() + " registros de la tabla tracks en proceso. Inicio: " + startTime.format(formatter));

            // Contadores para los resultados
            int alarmRegisteredCount = 0;
            int evaluatedCount = 0;
            int duplicateErrorCount = 0;

            // Procesar los tracks y contar en el momento de la evaluación
            for (Track track : latestPendingTracks) {
                checkAndLogAlarm(track);
                // Contar según el estado asignado al track en memoria
                switch (track.getAlarmStatus()) {
                    case "ALARM_REGISTERED":
                        alarmRegisteredCount++;
                        break;
                    case "EVALUATED":
                        evaluatedCount++;
                        break;
                    case "ERROR_DUPLICATE":
                        duplicateErrorCount++;
                        break;
                }
            }

            // Capturar hora de fin
            LocalDateTime endTime = LocalDateTime.now();

            // Log en consola: Resumen de resultados con hora de fin
            System.out.println("📋 Proceso finalizado: Se procesaron y registraron en la tabla alarms " + alarmRegisteredCount +
                    ", fueron evaluados " + evaluatedCount + " y hubo " + duplicateErrorCount + " errores de duplicado. Fin: " + endTime.format(formatter));

        } catch (Exception e) {
            System.err.println("Error durante la verificación automática de alarmas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void checkAndLogAlarm(Track track) throws Exception {
        if (track.getImei() == null || track.getLatitude() == null || track.getLongitude() == null) {
            throw new IllegalArgumentException("Track must have IMEI, latitude, and longitude");
        }

        boolean alarmRegistered = false; // Para rastrear si se generó alguna alarma
        boolean duplicateError = false; // Para rastrear si hubo error de duplicado

        List<Geofence> geofences = geofenceRepository.findAll();
        for (Geofence geofence : geofences) {

            JSONArray pointsArray = new JSONArray(geofence.getPoints());
            double[] x = new double[pointsArray.length()];
            double[] y = new double[pointsArray.length()];
            for (int i = 0; i < pointsArray.length(); i++) {
                JSONArray point = pointsArray.getJSONArray(i);
                y[i] = point.getDouble(0); // lat as y
                x[i] = point.getDouble(1); // lon as x
            }

            boolean isCurrentlyInside = isPointInPolygon(track.getLongitude(), track.getLatitude(), x, y);
            boolean hasActiveEntry = alarmRepository.existsByImeiAndGeofenceIdAndExitTimeIsNull(track.getImei(), geofence.getId());

            if (isCurrentlyInside && !hasActiveEntry) {
                // ENTRADA: Está dentro y no tiene entrada activa
                System.out.println("✅ Está dentro y no tiene entrada activa " + track.getImei() + " entered geofence " + geofence.getId());
                // Verificar si ya existe un duplicado en duplicate_alarms
                if (duplicateAlarmRepository.existsByImeiAndGeofenceIdAndAlarmType(track.getImei(), geofence.getId(), "ENTRY")) {
                    System.err.println("⚠️ Duplicado ya registrado en duplicate_alarms para IMEI " + track.getImei() + " y geocerca " + geofence.getId());
                    duplicateError = true;
                    continue; // Saltar a la siguiente geocerca
                }
                Alarm alarm = new Alarm();
                alarm.setImei(truncateString(track.getImei(), 15));
                alarm.setGeofenceId(geofence.getId());
                alarm.setTrackTime(track.getGpstime());
                alarm.setAlarmType("ENTRY");
                alarm.setLatitude(track.getLatitude());
                alarm.setLongitude(track.getLongitude());
                alarm.setDeviceName(truncateString(deviceRepository.findById(Long.valueOf(track.getImei())).map(Device::getDeviceName).orElse("Unknown"), 255));
                alarm.setPlateNumber(truncateString(deviceRepository.findById(Long.valueOf(track.getImei())).map(Device::getPlateNumber).orElse("Unknown"), 50));
                alarm.setEntryTime(System.currentTimeMillis() / 1000L);
                alarm.setExitTime(null);

                try {
                    alarmRepository.save(alarm);
                    System.out.println("✅ ENTRY: IMEI " + track.getImei() + " entered geofence " + geofence.getId());
                    alarmRegistered = true; // Marca que se registró una alarma
                } catch (DataIntegrityViolationException e) {
                    System.err.println("⚠️ Alarm ENTRY duplicada ignorada para IMEI " + track.getImei() + " y geocerca " + geofence.getId() + ": " + e.getMessage());
                    DuplicateAlarm duplicateAlarm = new DuplicateAlarm();
                    duplicateAlarm.setImei(truncateString(alarm.getImei(), 15));
                    duplicateAlarm.setGeofenceId(alarm.getGeofenceId());
                    duplicateAlarm.setAlarmType(truncateString(alarm.getAlarmType(), 50));
                    duplicateAlarm.setDeviceName(truncateString(alarm.getDeviceName(), 255));
                    duplicateAlarm.setPlateNumber(truncateString(alarm.getPlateNumber(), 50));
                    duplicateAlarm.setTrackTime(alarm.getTrackTime());
                    duplicateAlarm.setLatitude(alarm.getLatitude());
                    duplicateAlarm.setLongitude(alarm.getLongitude());
                    duplicateAlarm.setEntryTime(alarm.getEntryTime());
                    duplicateAlarm.setExitTime(alarm.getExitTime());
                    duplicateAlarm.setDuration(alarm.getDuration());
                    // Truncar el mensaje de error a 255 caracteres
                    String errorMessage = e.getMessage();
                    duplicateAlarm.setErrorDescription(errorMessage != null && errorMessage.length() > 255 ? errorMessage.substring(0, 255) : errorMessage);
                    try {
                        System.out.println("📝 Intentando guardar en duplicate_alarms: imei=" + duplicateAlarm.getImei() +
                                ", geofence_id=" + duplicateAlarm.getGeofenceId() +
                                ", alarm_type=" + duplicateAlarm.getAlarmType() +
                                ", device_name=" + truncateString(duplicateAlarm.getDeviceName(), 50) +
                                ", plate_number=" + truncateString(duplicateAlarm.getPlateNumber(), 50) +
                                ", error_description=" + truncateString(duplicateAlarm.getErrorDescription(), 50));
                        duplicateAlarmRepository.save(duplicateAlarm);
                        duplicateError = true; // Marca que hubo un error de duplicado
                    } catch (Throwable ex) {
                        System.err.println("❌ Error al guardar en duplicate_alarms para IMEI " + track.getImei() + " y geocerca " + geofence.getId() + ": " + ex.getMessage());
                        ex.printStackTrace();
                        duplicateError = true; // Marca como error de duplicado aunque falle el guardado
                    }
                }

            } else if (!isCurrentlyInside && hasActiveEntry) {
                // SALIDA: No está dentro pero tiene entrada activa
                Alarm activeAlarm = alarmRepository.findByImeiAndGeofenceIdAndExitTimeIsNull(track.getImei(), geofence.getId());

                if (activeAlarm != null) {
                    // Cerrar la alarma de entrada existente
                    System.out.println("✅ Cerrar la alarma de entrada existente " + track.getImei() + " entered geofence " + geofence.getId());
                    activeAlarm.setExitTime(System.currentTimeMillis() / 1000L);
                    activeAlarm.setAlarmType("ENTRY");
                    alarmRepository.save(activeAlarm);

                    // Crear un nuevo registro para la salida
                    Alarm exitAlarm = new Alarm();
                    exitAlarm.setImei(truncateString(track.getImei(), 15));
                    exitAlarm.setGeofenceId(geofence.getId());
                    exitAlarm.setTrackTime(track.getGpstime());
                    exitAlarm.setAlarmType("EXIT");
                    exitAlarm.setDeviceName(truncateString(deviceRepository.findById(Long.valueOf(track.getImei())).map(Device::getDeviceName).orElse("Unknown"), 255));
                    exitAlarm.setPlateNumber(truncateString(deviceRepository.findById(Long.valueOf(track.getImei())).map(Device::getPlateNumber).orElse("Unknown"), 50));
                    exitAlarm.setLatitude(track.getLatitude());
                    exitAlarm.setLongitude(track.getLongitude());
                    exitAlarm.setEntryTime(activeAlarm.getEntryTime());
                    exitAlarm.setExitTime(System.currentTimeMillis() / 1000L);

                    try {
                        alarmRepository.save(exitAlarm);
                        long duration = exitAlarm.getExitTime() - exitAlarm.getEntryTime();
                        System.out.println("🚪 EXIT: IMEI " + track.getImei() + " exited geofence " + geofence.getId() + " (Duration: " + duration + " seconds)");
                        alarmRegistered = true; // Marca que se registró una alarma
                    } catch (DataIntegrityViolationException e) {
                        System.err.println("⚠️ Alarm EXIT duplicada ignorada para IMEI " + track.getImei() + " y geocerca " + geofence.getId() + ": " + e.getMessage());
                        DuplicateAlarm duplicateAlarm = new DuplicateAlarm();
                        duplicateAlarm.setImei(truncateString(exitAlarm.getImei(), 15));
                        duplicateAlarm.setGeofenceId(exitAlarm.getGeofenceId());
                        duplicateAlarm.setAlarmType(truncateString(exitAlarm.getAlarmType(), 50));
                        duplicateAlarm.setDeviceName(truncateString(exitAlarm.getDeviceName(), 255));
                        duplicateAlarm.setPlateNumber(truncateString(exitAlarm.getPlateNumber(), 50));
                        duplicateAlarm.setTrackTime(exitAlarm.getTrackTime());
                        duplicateAlarm.setLatitude(exitAlarm.getLatitude());
                        duplicateAlarm.setLongitude(exitAlarm.getLongitude());
                        duplicateAlarm.setEntryTime(exitAlarm.getEntryTime());
                        duplicateAlarm.setExitTime(exitAlarm.getExitTime());
                        duplicateAlarm.setDuration(exitAlarm.getDuration());
                        // Truncar el mensaje de error a 255 caracteres
                        String errorMessage = e.getMessage();
                        duplicateAlarm.setErrorDescription(errorMessage != null && errorMessage.length() > 255 ? errorMessage.substring(0, 255) : errorMessage);
                        try {
                            System.out.println("📝 Intentando guardar en duplicate_alarms: imei=" + duplicateAlarm.getImei() +
                                    ", geofence_id=" + duplicateAlarm.getGeofenceId() +
                                    ", alarm_type=" + duplicateAlarm.getAlarmType() +
                                    ", device_name=" + truncateString(duplicateAlarm.getDeviceName(), 50) +
                                    ", plate_number=" + truncateString(duplicateAlarm.getPlateNumber(), 50) +
                                    ", error_description=" + truncateString(duplicateAlarm.getErrorDescription(), 50));
                            duplicateAlarmRepository.save(duplicateAlarm);
                            duplicateError = true; // Marca que hubo un error de duplicado
                        } catch (Throwable ex) {
                            System.err.println("❌ Error al guardar en duplicate_alarms para IMEI " + track.getImei() + " y geocerca " + geofence.getId() + ": " + ex.getMessage());
                            ex.printStackTrace();
                            duplicateError = true; // Marca como error de duplicado aunque falle el guardado
                        }
                    }
                }
            }
            // No imprimimos mensajes para casos donde no hay cambio de estado
        }

        // Actualizar status del track después de procesar todas las geofences
        if (duplicateError) {
            track.setAlarmStatus("ERROR_DUPLICATE");
            track.setAlarmErrorDescription("Duplicado detectado durante procesamiento");
        } else if (alarmRegistered) {
            track.setAlarmStatus("ALARM_REGISTERED");
            track.setAlarmErrorDescription(null); // Limpiar descripción de error
        } else {
            track.setAlarmStatus("EVALUATED");
            track.setAlarmErrorDescription(null); // Limpiar descripción de error
        }
        trackRepository.save(track);
    }

    @Override
    public List<Alarm> getAlarms() {
        return alarmRepository.findAll();
    }

    public List<Alarm> getActiveAlarms() {
        return alarmRepository.findByExitTimeIsNull();
    }

    public void forceExitAllActive(String imei) {
        List<Alarm> activeAlarms = alarmRepository.findByImeiAndExitTimeIsNull(imei);
        long currentTime = System.currentTimeMillis() / 1000L;

        for (Alarm alarm : activeAlarms) {
            alarm.setExitTime(currentTime);
            alarm.setAlarmType("ENTRY_EXIT");
            alarmRepository.save(alarm);
            System.out.println("🔧 Forced exit for alarm ID: " + alarm.getId());
        }
    }

    private boolean isPointInPolygon(double pointX, double pointY, double[] polygonX, double[] polygonY) {
        int polygonVertices = polygonX.length;
        boolean isIn = false;
        for (int i = 0, j = polygonVertices - 1; i < polygonVertices; j = i++) {
            if ((polygonY[i] > pointY) != (polygonY[j] > pointY) &&
                    (pointX < (polygonX[j] - polygonX[i]) * (pointY - polygonY[i]) / (polygonY[j] - polygonY[i]) + polygonX[i])) {
                isIn = !isIn;
            }
        }
        return isIn;
    }

    private String truncateString(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public List<Alarm> findAlarmsByFilters(String imei, Long startTimestamp, Long endTimestamp) {
        if (imei != null && !imei.isEmpty() && startTimestamp != null && endTimestamp != null) {
            return alarmRepository.findByImeiAndTrackTimeBetween(imei, startTimestamp, endTimestamp);
        } else if (imei != null && !imei.isEmpty()) {
            return alarmRepository.findByImei(imei);
        } else if (startTimestamp != null && endTimestamp != null) {
            return alarmRepository.findByTrackTimeBetween(startTimestamp, endTimestamp);
        } else {
            return alarmRepository.findAll();
        }
    }
}
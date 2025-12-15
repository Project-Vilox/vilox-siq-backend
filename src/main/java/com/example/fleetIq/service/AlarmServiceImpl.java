package com.example.fleetIq.service;

import com.example.fleetIq.model.*;
import com.example.fleetIq.repository.*;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    @Scheduled(fixedRate = 2000)
    public void checkAlarmsAutomatically() {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 🔥 CAMBIO 1: Obtener tracks PENDING agrupados por IMEI
            List<Track> allPendingTracks = trackRepository.findByAlarmStatus("PENDING");

            // Agrupar por IMEI
            Map<String, List<Track>> tracksByImei = allPendingTracks.stream()
                    .collect(Collectors.groupingBy(Track::getImei));

            int totalTracks = allPendingTracks.size();
            int totalImeis = tracksByImei.size();

            System.out.println("📊 Procesando " + totalTracks + " tracks de " + totalImeis +
                    " IMEIs diferentes. Inicio: " + startTime.format(formatter));

            int alarmRegisteredCount = 0;
            int evaluatedCount = 0;
            int duplicateErrorCount = 0;

            // 🔥 CAMBIO 2: Procesar cada IMEI de forma independiente
            for (Map.Entry<String, List<Track>> entry : tracksByImei.entrySet()) {
                String imei = entry.getKey();
                List<Track> tracksForImei = entry.getValue();

                // Ordenar tracks de este IMEI por tiempo (más antiguo primero)
                List<Track> sortedTracks = tracksForImei.stream()
                        .sorted(Comparator.comparing(Track::getGpstime))
                        .collect(Collectors.toList());

                System.out.println("🚗 Procesando " + sortedTracks.size() +
                        " tracks para IMEI: " + imei);

                // Procesar tracks de este IMEI en orden cronológico
                for (Track track : sortedTracks) {
                    try {
                        checkAndLogAlarm(track);

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
                    } catch (Exception e) {
                        System.err.println("❌ Error procesando track ID " + track.getId() +
                                " (IMEI: " + imei + "): " + e.getMessage());
                        track.setAlarmStatus("ERROR");
                        track.setAlarmErrorDescription(e.getMessage());
                        trackRepository.save(track);
                    }
                }
            }

            LocalDateTime endTime = LocalDateTime.now();
            System.out.println("📋 Proceso finalizado: " +
                    "ALARM_REGISTERED=" + alarmRegisteredCount +
                    " | EVALUATED=" + evaluatedCount +
                    " | DUPLICATES=" + duplicateErrorCount +
                    ". Fin: " + endTime.format(formatter));

        } catch (Exception e) {
            System.err.println("❌ Error en verificación automática de alarmas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void checkAndLogAlarm(Track track) throws Exception {
        if (track.getImei() == null || track.getLatitude() == null || track.getLongitude() == null) {
            throw new IllegalArgumentException("Track must have IMEI, latitude, and longitude");
        }

        boolean alarmRegistered = false;
        boolean duplicateError = false;

        // Obtener device info una sola vez
        String deviceName = "Unknown";
        String plateNumber = "Unknown";
        try {
            Device device = deviceRepository.findById(Long.valueOf(track.getImei())).orElse(null);
            if (device != null) {
                deviceName = truncateString(device.getDeviceName(), 255);
                plateNumber = truncateString(device.getPlateNumber(), 50);
            }
        } catch (NumberFormatException e) {
            System.err.println("⚠️ IMEI inválido: " + track.getImei());
        }

        List<Geofence> geofences = geofenceRepository.findAll();
        System.out.println("🔍 Track ID: " + track.getId() +
                " | IMEI: " + track.getImei() +
                " | GPS: " + track.getGpstime() +
                " | Lat: " + track.getLatitude() +
                " | Lon: " + track.getLongitude());

        for (Geofence geofence : geofences) {
            try {
                JSONArray pointsArray = new JSONArray(geofence.getPoints());
                double[] x = new double[pointsArray.length()];
                double[] y = new double[pointsArray.length()];

                for (int i = 0; i < pointsArray.length(); i++) {
                    JSONArray point = pointsArray.getJSONArray(i);
                    y[i] = point.getDouble(0);
                    x[i] = point.getDouble(1);
                }

                boolean isCurrentlyInside = isPointInPolygon(track.getLongitude(), track.getLatitude(), x, y);
                boolean hasActiveEntry = alarmRepository.existsByImeiAndGeofenceIdAndExitTimeIsNull(
                        track.getImei(), geofence.getId());

                // Solo mostrar geocercas relevantes
                if (isCurrentlyInside || hasActiveEntry) {
                    System.out.println("   📍 Geocerca " + geofence.getId() +
                            " | Dentro: " + isCurrentlyInside +
                            " | Activa: " + hasActiveEntry);
                }

                // ========== ENTRY ==========
                if (isCurrentlyInside && !hasActiveEntry) {
                    if (duplicateAlarmRepository.existsByImeiAndGeofenceIdAndAlarmType(
                            track.getImei(), geofence.getId(), "ENTRY")) {
                        System.err.println("   ⚠️ ENTRY duplicado en duplicate_alarms");
                        duplicateError = true;
                        continue;
                    }

                    Alarm alarm = new Alarm();
                    alarm.setImei(truncateString(track.getImei(), 15));
                    alarm.setGeofenceId(geofence.getId());
                    alarm.setTrackTime(track.getGpstime());
                    alarm.setAlarmType("ENTRY");
                    alarm.setLatitude(track.getLatitude());
                    alarm.setLongitude(track.getLongitude());
                    alarm.setDeviceName(deviceName);
                    alarm.setPlateNumber(plateNumber);
                    alarm.setEntryTime(System.currentTimeMillis() / 1000L);
                    alarm.setExitTime(null);

                    try {
                        alarmRepository.save(alarm);
                        System.out.println("   ✅ ENTRY → Geocerca " + geofence.getId());
                        alarmRegistered = true;
                    } catch (DataIntegrityViolationException e) {
                        System.err.println("   ⚠️ ENTRY duplicado: " + e.getMessage());
                        saveDuplicateAlarm(alarm, e.getMessage());
                        duplicateError = true;
                    }

                    // ========== EXIT ==========
                } else if (!isCurrentlyInside && hasActiveEntry) {
                    List<Alarm> activeAlarms = alarmRepository.findByImeiAndGeofenceIdAndExitTimeIsNull(
                            track.getImei(), geofence.getId());

                    if (activeAlarms.isEmpty()) {
                        System.err.println("   ⚠️ hasActiveEntry=true pero no hay alarmas activas");
                        continue;
                    }

                    if (activeAlarms.size() > 1) {
                        System.err.println("   ⚠️ " + activeAlarms.size() + " alarmas activas. Cerrando duplicados...");
                        activeAlarms.sort(Comparator.comparing(Alarm::getEntryTime).reversed());

                        for (int i = 1; i < activeAlarms.size(); i++) {
                            Alarm oldAlarm = activeAlarms.get(i);
                            oldAlarm.setExitTime(System.currentTimeMillis() / 1000L);
                            oldAlarm.setAlarmType("ENTRY_EXIT");
                            alarmRepository.save(oldAlarm);
                        }
                    }

                    Alarm activeAlarm = activeAlarms.get(0);
                    activeAlarm.setExitTime(System.currentTimeMillis() / 1000L);
                    activeAlarm.setAlarmType("ENTRY");
                    alarmRepository.save(activeAlarm);

                    Alarm exitAlarm = new Alarm();
                    exitAlarm.setImei(truncateString(track.getImei(), 15));
                    exitAlarm.setGeofenceId(geofence.getId());
                    exitAlarm.setTrackTime(track.getGpstime());
                    exitAlarm.setAlarmType("EXIT");
                    exitAlarm.setDeviceName(deviceName);
                    exitAlarm.setPlateNumber(plateNumber);
                    exitAlarm.setLatitude(track.getLatitude());
                    exitAlarm.setLongitude(track.getLongitude());
                    exitAlarm.setEntryTime(activeAlarm.getEntryTime());
                    exitAlarm.setExitTime(System.currentTimeMillis() / 1000L);

                    try {
                        alarmRepository.save(exitAlarm);
                        long duration = exitAlarm.getExitTime() - exitAlarm.getEntryTime();
                        System.out.println("   🚪 EXIT ← Geocerca " + geofence.getId() +
                                " (" + duration + " seg)");
                        alarmRegistered = true;
                    } catch (DataIntegrityViolationException e) {
                        System.err.println("   ⚠️ EXIT duplicado: " + e.getMessage());
                        saveDuplicateAlarm(exitAlarm, e.getMessage());
                        duplicateError = true;
                    }
                }
            } catch (Exception e) {
                System.err.println("   ❌ Error en geocerca " + geofence.getId() + ": " + e.getMessage());
            }
        }

        // Actualizar estado
        if (duplicateError) {
            track.setAlarmStatus("ERROR_DUPLICATE");
            track.setAlarmErrorDescription("Duplicate detected");
        } else if (alarmRegistered) {
            track.setAlarmStatus("ALARM_REGISTERED");
            track.setAlarmErrorDescription(null);
        } else {
            track.setAlarmStatus("EVALUATED");
            track.setAlarmErrorDescription(null);
        }

        trackRepository.save(track);
    }

    private void saveDuplicateAlarm(Alarm alarm, String errorMessage) {
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
        duplicateAlarm.setErrorDescription(truncateString(errorMessage, 255));

        try {
            duplicateAlarmRepository.save(duplicateAlarm);
        } catch (Throwable ex) {
            System.err.println("❌ Error guardando duplicate_alarm: " + ex.getMessage());
        }
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
        }
    }

    private boolean isPointInPolygon(double pointX, double pointY, double[] polygonX, double[] polygonY) {
        int polygonVertices = polygonX.length;
        boolean isIn = false;
        for (int i = 0, j = polygonVertices - 1; i < polygonVertices; j = i++) {
            if ((polygonY[i] > pointY) != (polygonY[j] > pointY) &&
                    (pointX < (polygonX[j] - polygonX[i]) * (pointY - polygonY[i]) /
                            (polygonY[j] - polygonY[i]) + polygonX[i])) {
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
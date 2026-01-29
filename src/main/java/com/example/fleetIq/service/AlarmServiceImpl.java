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

    // ⭐ CONFIGURACIÓN DE ANTI-REBOTE (para geocercas adyacentes)
    private static final long DEBOUNCE_SECONDS = 30; // 30 seg para geocercas cercanas
    private static final double MIN_DISTANCE_METERS = 30; // 30 metros mínimo
    private static final double ADJACENT_GEOFENCE_THRESHOLD = 100.0; // Considerar adyacentes si están a <150m

    @Scheduled(fixedRate = 2000)
    public void checkAlarmsAutomatically() {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<Track> allPendingTracks = trackRepository.findByAlarmStatus("PENDING");
            Map<String, List<Track>> tracksByImei = allPendingTracks.stream()
                    .collect(Collectors.groupingBy(Track::getImei));

            int totalTracks = allPendingTracks.size();
            int totalImeis = tracksByImei.size();

            System.out.println("📊 Procesando " + totalTracks + " tracks de " + totalImeis +
                    " IMEIs diferentes. Inicio: " + startTime.format(formatter));

            int alarmRegisteredCount = 0;
            int evaluatedCount = 0;
            int duplicateErrorCount = 0;

            for (Map.Entry<String, List<Track>> entry : tracksByImei.entrySet()) {
                String imei = entry.getKey();
                List<Track> tracksForImei = entry.getValue();

                List<Track> sortedTracks = tracksForImei.stream()
                        .sorted(Comparator.comparing(Track::getGpstime))
                        .collect(Collectors.toList());

                System.out.println("🚗 Procesando " + sortedTracks.size() +
                        " tracks para IMEI: " + imei);

                for (Track track : sortedTracks) {
                    try {
                        checkAndLogAlarmWithDebounce(track);

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
    public void checkAndLogAlarmWithDebounce(Track track) throws Exception {
        if (track.getImei() == null || track.getLatitude() == null || track.getLongitude() == null) {
            throw new IllegalArgumentException("Track must have IMEI, latitude, and longitude");
        }

        boolean alarmRegistered = false;
        boolean duplicateError = false;

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

        // ⭐ PASO 1: Detectar en qué geocercas está el vehículo AHORA
        List<Long> currentGeofences = new ArrayList<>();
        Map<Long, Geofence> geofenceMap = new HashMap<>();

        for (Geofence geofence : geofences) {
            geofenceMap.put(geofence.getId(), geofence);

            try {
                JSONArray pointsArray = new JSONArray(geofence.getPoints());
                double[] x = new double[pointsArray.length()];
                double[] y = new double[pointsArray.length()];

                for (int i = 0; i < pointsArray.length(); i++) {
                    JSONArray point = pointsArray.getJSONArray(i);
                    y[i] = point.getDouble(0);
                    x[i] = point.getDouble(1);
                }

                boolean isInside = isPointInPolygon(track.getLongitude(), track.getLatitude(), x, y);
                if (isInside) {
                    currentGeofences.add(geofence.getId());
                }
            } catch (Exception e) {
                System.err.println("   ❌ Error procesando geocerca " + geofence.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("   🎯 Dentro de geocercas: " + currentGeofences);

        // ⭐ PASO 2: Obtener geocercas activas (con ENTRY sin EXIT)
        List<Alarm> activeAlarms = alarmRepository.findByImeiAndExitTimeIsNull(track.getImei());
        Set<Long> activeGeofenceIds = activeAlarms.stream()
                .map(Alarm::getGeofenceId)
                .collect(Collectors.toSet());

        System.out.println("   🔴 Geocercas activas: " + activeGeofenceIds);

        // ⭐ PASO 3: Procesar ENTRADAS (está dentro pero no tiene alarma activa)
        for (Long geofenceId : currentGeofences) {
            if (activeGeofenceIds.contains(geofenceId)) {
                continue; // Ya tiene entrada activa
            }

            // ⭐ VALIDACIÓN 1: Evitar rebotes de salida reciente
            if (shouldIgnoreRecentExit(track.getImei(), geofenceId, track.getGpstime())) {
                System.out.println("   ⏸️ IGNORANDO ENTRY geocerca " + geofenceId + " - Salida reciente (rebote GPS)");
                continue;
            }

            // ⭐ VALIDACIÓN 2: Para geocercas adyacentes - verificar transición válida
            if (!isValidTransition(track.getImei(), geofenceId, activeGeofenceIds, track.getGpstime(),
                    track.getLatitude(), track.getLongitude())) {
                System.out.println("   ⏸️ IGNORANDO ENTRY geocerca " + geofenceId
                        + " - Transición muy rápida entre geocercas adyacentes");
                continue;
            }

            // Crear ENTRY
            Alarm alarm = new Alarm();
            alarm.setImei(truncateString(track.getImei(), 15));
            alarm.setGeofenceId(geofenceId);
            alarm.setTrackTime(track.getGpstime());
            alarm.setAlarmType("ENTRY");
            alarm.setLatitude(track.getLatitude());
            alarm.setLongitude(track.getLongitude());
            alarm.setDeviceName(deviceName);
            alarm.setPlateNumber(plateNumber);
            alarm.setEntryTime(track.getGpstime());
            alarm.setExitTime(null);

            try {
                alarmRepository.save(alarm);
                System.out.println("   ✅ ENTRY → Geocerca " + geofenceId);
                alarmRegistered = true;
                activeGeofenceIds.add(geofenceId);
            } catch (DataIntegrityViolationException e) {
                System.err.println("   ⚠️ ENTRY duplicado: " + e.getMessage());
                saveDuplicateAlarm(alarm, e.getMessage());
                duplicateError = true;
            }
        }

        // ⭐ PASO 4: Procesar SALIDAS (tiene alarma activa pero ya no está dentro)
        Set<Long> geofencesToExit = new HashSet<>(activeGeofenceIds);
        geofencesToExit.removeAll(currentGeofences);

        for (Long geofenceId : geofencesToExit) {
            List<Alarm> alarmsToClose = alarmRepository.findByImeiAndGeofenceIdAndExitTimeIsNull(
                    track.getImei(), geofenceId);

            if (alarmsToClose.isEmpty()) {
                continue;
            }

            // Ordenar por tiempo de entrada (más reciente primero)
            alarmsToClose.sort(Comparator.comparing(Alarm::getEntryTime).reversed());
            Alarm activeAlarm = alarmsToClose.get(0);

            // ⭐ VALIDACIÓN 1: Tiempo mínimo de permanencia
            long timeSinceEntry = track.getGpstime() - activeAlarm.getEntryTime();
            if (timeSinceEntry < DEBOUNCE_SECONDS) {
                System.out.println("   ⏸️ IGNORANDO EXIT geocerca " + geofenceId + " - Tiempo muy corto ("
                        + timeSinceEntry + "s)");
                continue;
            }

            // ⭐ VALIDACIÓN 2: Distancia mínima recorrida
            double distance = calculateDistance(
                    activeAlarm.getLatitude(), activeAlarm.getLongitude(),
                    track.getLatitude(), track.getLongitude());

            if (distance < MIN_DISTANCE_METERS) {
                System.out.println("   ⏸️ IGNORANDO EXIT geocerca " + geofenceId + " - Distancia muy corta (" +
                        String.format("%.1f", distance) + "m)");
                continue;
            }

            // Cerrar alarmas duplicadas si existen
            if (alarmsToClose.size() > 1) {
                System.err.println("   ⚠️ " + alarmsToClose.size() + " alarmas activas para geocerca " + geofenceId);
                for (int i = 1; i < alarmsToClose.size(); i++) {
                    Alarm oldAlarm = alarmsToClose.get(i);
                    oldAlarm.setExitTime(track.getGpstime());
                    oldAlarm.setAlarmType("ENTRY_EXIT");
                    alarmRepository.save(oldAlarm);
                }
            }

            // Cerrar alarma activa
            activeAlarm.setExitTime(track.getGpstime());
            activeAlarm.setAlarmType("ENTRY");
            alarmRepository.save(activeAlarm);

            // Crear alarma EXIT
            Alarm exitAlarm = new Alarm();
            exitAlarm.setImei(truncateString(track.getImei(), 15));
            exitAlarm.setGeofenceId(geofenceId);
            exitAlarm.setTrackTime(track.getGpstime());
            exitAlarm.setAlarmType("EXIT");
            exitAlarm.setDeviceName(deviceName);
            exitAlarm.setPlateNumber(plateNumber);
            exitAlarm.setLatitude(track.getLatitude());
            exitAlarm.setLongitude(track.getLongitude());
            exitAlarm.setEntryTime(activeAlarm.getEntryTime());
            exitAlarm.setExitTime(track.getGpstime());

            try {
                alarmRepository.save(exitAlarm);
                long duration = exitAlarm.getExitTime() - exitAlarm.getEntryTime();
                System.out.println("   🚪 EXIT ← Geocerca " + geofenceId +
                        " (" + duration + "s | " + String.format("%.1f", distance) + "m)");
                alarmRegistered = true;
                activeGeofenceIds.remove(geofenceId);
            } catch (DataIntegrityViolationException e) {
                System.err.println("   ⚠️ EXIT duplicado: " + e.getMessage());
                saveDuplicateAlarm(exitAlarm, e.getMessage());
                duplicateError = true;
            }
        }

        // Actualizar estado del track
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

    /**
     * ⭐ Valida si la transición entre geocercas es legítima (no rebote)
     */
    private boolean isValidTransition(String imei, Long newGeofenceId, Set<Long> activeGeofenceIds,
            Long currentGpstime, double currentLat, double currentLon) {
        try {
            // Si no hay geocercas activas, siempre es válido
            if (activeGeofenceIds.isEmpty()) {
                return true;
            }

            // Obtener la última entrada en cualquier geocerca
            List<Alarm> recentEntries = alarmRepository.findByImei(imei).stream()
                    .filter(a -> "ENTRY".equals(a.getAlarmType()))
                    .filter(a -> currentGpstime - a.getEntryTime() < DEBOUNCE_SECONDS)
                    .sorted(Comparator.comparing(Alarm::getEntryTime).reversed())
                    .collect(Collectors.toList());

            if (recentEntries.isEmpty()) {
                return true;
            }

            Alarm lastEntry = recentEntries.get(0);
            long timeSinceLastEntry = currentGpstime - lastEntry.getEntryTime();

            // Si la última entrada fue hace muy poco, calcular distancia
            if (timeSinceLastEntry < DEBOUNCE_SECONDS) {
                double distanceFromLastEntry = calculateDistance(
                        lastEntry.getLatitude(), lastEntry.getLongitude(),
                        currentLat, currentLon);

                // Si no se movió significativamente, es rebote
                if (distanceFromLastEntry < MIN_DISTANCE_METERS) {
                    System.out.println("      ⚠️ Distancia desde última entrada: " +
                            String.format("%.1f", distanceFromLastEntry) + "m (muy cerca)");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            System.err.println("⚠️ Error validando transición: " + e.getMessage());
            return true; // En caso de error, permitir la transición
        }
    }

    /**
     * Verifica si existe una salida reciente que debería considerarse un rebote
     */
    private boolean shouldIgnoreRecentExit(String imei, Long geofenceId, Long currentGpstime) {
        try {
            List<Alarm> recentExits = alarmRepository.findByImeiAndGeofenceIdAndAlarmType(
                    imei, geofenceId, "EXIT");

            if (recentExits.isEmpty()) {
                return false;
            }

            recentExits.sort(Comparator.comparing(Alarm::getExitTime).reversed());
            Alarm lastExit = recentExits.get(0);

            long timeSinceExit = currentGpstime - lastExit.getExitTime();

            if (timeSinceExit < DEBOUNCE_SECONDS) {
                System.out.println("      ⚠️ Última salida hace " + timeSinceExit + "s");
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("⚠️ Error verificando salida reciente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula la distancia en metros entre dos puntos GPS (fórmula de Haversine)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // metros

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // ========== MÉTODOS AUXILIARES ==========

    @Transactional
    public void checkAndLogAlarm(Track track) throws Exception {
        checkAndLogAlarmWithDebounce(track);
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

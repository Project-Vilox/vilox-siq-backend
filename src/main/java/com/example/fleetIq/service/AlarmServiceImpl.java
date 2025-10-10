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
            LocalDateTime startTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<Track> latestPendingTracks = trackRepository.findByAlarmStatus("PENDING")
                    .stream()
                    .sorted(Comparator.comparing(Track::getGpstime, Comparator.reverseOrder()))
                    .toList();

            System.out.println("📊 Estamos evaluando " + latestPendingTracks.size() + " registros de la tabla tracks en proceso. Inicio: " + startTime.format(formatter));

            int alarmRegisteredCount = 0;
            int evaluatedCount = 0;
            int duplicateErrorCount = 0;

            for (Track track : latestPendingTracks) {
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
            }

            LocalDateTime endTime = LocalDateTime.now();
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

        boolean alarmRegistered = false;
        boolean duplicateError = false;

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
                if (duplicateAlarmRepository.existsByImeiAndGeofenceIdAndAlarmType(track.getImei(), geofence.getId(), "ENTRY")) {
                    System.err.println("⚠️ Duplicate found in duplicate_alarms for IMEI " + track.getImei() + " and geofence " + geofence.getId());
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
                alarm.setDeviceName(truncateString(deviceRepository.findById(Long.valueOf(track.getImei())).map(Device::getDeviceName).orElse("Unknown"), 255));
                alarm.setPlateNumber(truncateString(deviceRepository.findById(Long.valueOf(track.getImei())).map(Device::getPlateNumber).orElse("Unknown"), 50));
                alarm.setEntryTime(System.currentTimeMillis() / 1000L);
                alarm.setExitTime(null);

                try {
                    alarmRepository.save(alarm);
                    System.out.println("✅ ENTRY: IMEI " + track.getImei() + " entered geofence " + geofence.getId());
                    alarmRegistered = true;
                } catch (DataIntegrityViolationException e) {
                    System.err.println("⚠️ Duplicate ENTRY ignored for IMEI " + track.getImei() + " and geofence " + geofence.getId() + ": " + e.getMessage());
                    saveDuplicateAlarm(alarm, e.getMessage()); // Modificado
                    duplicateError = true;
                }

            } else if (!isCurrentlyInside && hasActiveEntry) {
                List<Alarm> activeAlarms = alarmRepository.findByImeiAndGeofenceIdAndExitTimeIsNull(track.getImei(), geofence.getId()); // Modificado

                if (activeAlarms.size() > 1) { // Modificado
                    System.err.println("⚠️ Multiple active alarms found for IMEI " + track.getImei() + " and geofence " + geofence.getId() + ". Closing older alarms."); // Modificado
                    activeAlarms.sort(Comparator.comparing(Alarm::getEntryTime).reversed()); // Modificado
                    for (int i = 1; i < activeAlarms.size(); i++) { // Modificado
                        Alarm oldAlarm = activeAlarms.get(i); // Modificado
                        oldAlarm.setExitTime(System.currentTimeMillis() / 1000L); // Modificado
                        oldAlarm.setAlarmType("ENTRY_EXIT"); // Modificado
                        alarmRepository.save(oldAlarm); // Modificado
                        System.out.println("🔧 Closed duplicate alarm ID: " + oldAlarm.getId()); // Modificado
                    } // Modificado
                } // Modificado

                Alarm activeAlarm = activeAlarms.get(0); // Modificado
                activeAlarm.setExitTime(System.currentTimeMillis() / 1000L);
                activeAlarm.setAlarmType("ENTRY");
                alarmRepository.save(activeAlarm);

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
                    alarmRegistered = true;
                } catch (DataIntegrityViolationException e) {
                    System.err.println("⚠️ Duplicate EXIT ignored for IMEI " + track.getImei() + " and geofence " + geofence.getId() + ": " + e.getMessage());
                    saveDuplicateAlarm(exitAlarm, e.getMessage()); // Modificado
                    duplicateError = true;
                }
            }
        }

        if (duplicateError) {
            track.setAlarmStatus("ERROR_DUPLICATE");
            track.setAlarmErrorDescription("Duplicate detected during processing");
        } else if (alarmRegistered) {
            track.setAlarmStatus("ALARM_REGISTERED");
            track.setAlarmErrorDescription(null);
        } else {
            track.setAlarmStatus("EVALUATED");
            track.setAlarmErrorDescription(null);
        }
        trackRepository.save(track);
    }

    private void saveDuplicateAlarm(Alarm alarm, String errorMessage) { // Modificado
        DuplicateAlarm duplicateAlarm = new DuplicateAlarm(); // Modificado
        duplicateAlarm.setImei(truncateString(alarm.getImei(), 15)); // Modificado
        duplicateAlarm.setGeofenceId(alarm.getGeofenceId()); // Modificado
        duplicateAlarm.setAlarmType(truncateString(alarm.getAlarmType(), 50)); // Modificado
        duplicateAlarm.setDeviceName(truncateString(alarm.getDeviceName(), 255)); // Modificado
        duplicateAlarm.setPlateNumber(truncateString(alarm.getPlateNumber(), 50)); // Modificado
        duplicateAlarm.setTrackTime(alarm.getTrackTime()); // Modificado
        duplicateAlarm.setLatitude(alarm.getLatitude()); // Modificado
        duplicateAlarm.setLongitude(alarm.getLongitude()); // Modificado
        duplicateAlarm.setEntryTime(alarm.getEntryTime()); // Modificado
        duplicateAlarm.setExitTime(alarm.getExitTime()); // Modificado
        duplicateAlarm.setDuration(alarm.getDuration()); // Modificado
        duplicateAlarm.setErrorDescription(truncateString(errorMessage, 255)); // Modificado

        try { // Modificado
            System.out.println("📝 Saving to duplicate_alarms: imei=" + duplicateAlarm.getImei() + // Modificado
                    ", geofence_id=" + duplicateAlarm.getGeofenceId() + // Modificado
                    ", alarm_type=" + duplicateAlarm.getAlarmType()); // Modificado
            duplicateAlarmRepository.save(duplicateAlarm); // Modificado
        } catch (Throwable ex) { // Modificado
            System.err.println("❌ Error saving to duplicate_alarms for IMEI " + alarm.getImei() + " and geofence " + alarm.getGeofenceId() + ": " + ex.getMessage()); // Modificado
            ex.printStackTrace(); // Modificado
        } // Modificado
    } // Modificado

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
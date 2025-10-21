package com.example.fleetIq.service;

import com.example.fleetIq.model.*;
import com.example.fleetIq.repository.*;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    @Autowired
    private TramoRepository tramoRepository;

    @Autowired
    private SlaPorEstablecimientoRepository slaPorEstablecimientoRepo;

    @Scheduled(fixedRate = 2000)
    public void checkAlarmsAutomatically() {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<Track> latestPendingTracks = trackRepository.findByAlarmStatus("PENDING")
                    .stream()
                    .sorted(Comparator.comparing(Track::getGpstime, Comparator.reverseOrder()))
                    .toList();

            System.out.println("📊 Estamos evaluando Evento de Entrada o Salida de " + latestPendingTracks.size() + " registros de la tabla tracks en proceso. Inicio: " + startTime.format(formatter));

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

            if (isCurrentlyInside) {
                System.err.println("se encuentra dentro de la geocerca : " + geofence.getId() + " : " + isCurrentlyInside);
            }
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

                evaluacionCita(alarm);

                try {
                    alarmRepository.save(alarm);
                    System.out.println("✅ ENTRY: IMEI " + track.getImei() + " entered geofence " + geofence.getId());
                    alarmRegistered = true;
                } catch (DataIntegrityViolationException e) {
                    System.err.println("⚠️ Duplicate ENTRY ignored for IMEI " + track.getImei() + " and geofence " + geofence.getId() + ": " + e.getMessage());
                    saveDuplicateAlarm(alarm, e.getMessage());
                    duplicateError = true;
                }

            } else if (!isCurrentlyInside && hasActiveEntry) {
                List<Alarm> activeAlarms = alarmRepository.findByImeiAndGeofenceIdAndExitTimeIsNull(track.getImei(), geofence.getId());

                if (activeAlarms.size() > 1) {
                    System.err.println("⚠️ Multiple active alarms found for IMEI " + track.getImei() + " and geofence " + geofence.getId() + ". Closing older alarms.");
                    activeAlarms.sort(Comparator.comparing(Alarm::getEntryTime).reversed());
                    for (int i = 1; i < activeAlarms.size(); i++) {
                        Alarm oldAlarm = activeAlarms.get(i);
                        oldAlarm.setExitTime(System.currentTimeMillis() / 1000L);
                        oldAlarm.setAlarmType("ENTRY_EXIT");
                        alarmRepository.save(oldAlarm);
                        System.out.println("🔧 Closed duplicate alarm ID: " + oldAlarm.getId());
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

                    evaluacionCita(exitAlarm);

                    alarmRegistered = true;
                } catch (DataIntegrityViolationException e) {
                    System.err.println("⚠️ Duplicate EXIT ignored for IMEI " + track.getImei() + " and geofence " + geofence.getId() + ": " + e.getMessage());
                    saveDuplicateAlarm(exitAlarm, e.getMessage());
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

    private void evaluacionCita(Alarm alarm) {
        try {
            List<Object[]> results = alarmRepository.findTramosPendientesByImeiAndGeofence(alarm.getImei());

            if (!results.isEmpty()) {
                for (Object[] row : results) {
                    String tramoId = (String) row[0];
                    Object horaLlegadaProgramada = row[1];
                    Object horaSalidaProgramada = row[2];
                    String establecimientoId = (String) row[3];
                    String tipoActividad = (String) row[4];
                    String viajeId = (String) row[5];
                    Integer orden = (Integer) row[6];
                    Long geocercaId = (Long) row[7];
                    String tipoGeocerca = (String) row[8];
                    Integer ordenado = (Integer) row[9];

                    System.out.println("📍 Evaluación de cita para " + alarm.getAlarmType() + ":");
                    System.out.println("   - Tramo ID: " + tramoId);
                    System.out.println("   - IMEI: " + alarm.getImei());
                    System.out.println("   - Tipo Actividad: " + tipoActividad);
                    System.out.println("   - Hora llegada programada: " + horaLlegadaProgramada);
                    System.out.println("   - Hora salida programada: " + horaSalidaProgramada);
                    System.out.println("   - SLA (minutos): " + establecimientoId);
                    System.out.println("   - Entry Time (epoch): " + alarm.getEntryTime());
                    System.out.println("   - Exit Time (epoch): " + alarm.getExitTime());

                    // Buscar el tramo en la base de datos 
                    Tramo tramo = tramoRepository.findById(tramoId).orElse(null);

                    if (ordenado == 1 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY") && tramo.getHoraLlegadaReal() == null) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                        LocalDateTime horaLlegadaReal = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(alarm.getEntryTime()),
                                ZoneId.systemDefault()
                        );
                        // Actualizar hora_llegada_real 
                        tramo.setHoraLlegadaReal(horaLlegadaReal);
                        tramo.setEstado(Tramo.EstadoTramo.en_curso);

                        Duration duracionTardanza = Duration.between(tramo.getHoraLlegadaProgramada(), horaLlegadaReal);
                        long minutosTardanza = duracionTardanza.toMinutes();
                        int tardanzaValue = (int) (minutosTardanza <= 0 ? 0 : minutosTardanza);
                        tramo.setTardanzaCita1(tardanzaValue);

                        // Guardar el tramo actualizado // Modificado
                        tramoRepository.save(tramo); // Modificado
                    }
                    if (ordenado == 1 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT") && tramo.getHoraLlegadaReal() != null) {
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);


                    }
                    if (ordenado == 2 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY") && tramo.getHoraSalidaReal() == null) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);


                    }
                    if (ordenado == 2 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                        LocalDateTime horaSalidaReal = LocalDateTime.ofInstant( // Modificado
                                Instant.ofEpochSecond(alarm.getExitTime()), // Modificado
                                ZoneId.systemDefault() // Modificado
                        ); // Modificado

                        Duration duracionPermanencia = Duration.between(tramo.getHoraLlegadaReal(), horaSalidaReal);
                        long totalMinutesPermanencia = duracionPermanencia.toMinutes();
                        tramo.setTiempoPermanenciaCita1((int) totalMinutesPermanencia);

                        Duration duracionAtencionCita = Duration.between(tramo.getHoraSalidaProgramada(), horaSalidaReal);
                        long minutosduracionAtencionCita = duracionAtencionCita.toMinutes();
                        int tiempoAtencionCita1;

                        if (tramo.getTardanzaCita1() == 0) {
                            tiempoAtencionCita1 = (int) minutosduracionAtencionCita;
                        } else {
                            tiempoAtencionCita1 = tramo.getTiempoPermanenciaCita1();
                        }
                        tramo.setTiempoAtencionCita1(tiempoAtencionCita1);

                        tramoRepository.save(tramo); // Modificado


                        SlaPorEstablecimiento slaPorEstablecimiento = new SlaPorEstablecimiento();
                        slaPorEstablecimiento.setIdEstablecimiento(establecimientoId);
                        slaPorEstablecimiento.setTiempoAtencionMinutos(tiempoAtencionCita1);
                        //slaPorEstablecimiento.setIdCliente();
                        slaPorEstablecimiento.setIdViaje(viajeId);
                        slaPorEstablecimientoRepo.save(slaPorEstablecimiento);

                        System.out.println("✅ Tramo actualizado - Hora salida real: " + horaSalidaReal); // Modificado
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                    }
                    if (ordenado == 3 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                        Object[] tramoData = results.get(4); // Get the 5th element (index 4)
                        Tramo tramo2 = tramoRepository.findById((String) tramoData[0]).orElse(null);

                        LocalDateTime horaIngresoReal = LocalDateTime.now(ZoneId.systemDefault());

                        // Actualizar hora_llegada_real
                        tramo.setHoraSalidaReal(horaIngresoReal);

                        Duration duracionTardanza = Duration.between(tramo2.getHoraLlegadaProgramada(), horaIngresoReal);
                        long minutosTardanza = duracionTardanza.toMinutes();
                        int tardanzaValue = (int) (minutosTardanza <= 0 ? 0 : minutosTardanza);
                        tramo.setTardanzaCita2(tardanzaValue);

                        tramo2.setEstado(Tramo.EstadoTramo.en_curso);
                        tramoRepository.save(tramo2); // Modificado
                        // Guardar el tramo actualizado // Modificado
                        tramoRepository.save(tramo); // Modificado
                    }
                    if (ordenado == 3 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                        LocalDateTime horaSalidaReal = LocalDateTime.now(ZoneId.systemDefault());

                        Duration duracionPermanencia = Duration.between(tramo.getHoraLlegadaReal(), horaSalidaReal);
                        long totalMinutesPermanencia = duracionPermanencia.toMinutes();
                        tramo.setTiempoPermanenciaCita2((int) totalMinutesPermanencia);

                        Duration duracionAtencionCita = Duration.between(tramo.getHoraSalidaProgramada(), horaSalidaReal);
                        long minutosduracionAtencionCita = duracionAtencionCita.toMinutes();
                        int tiempoAtencionCita2;

                        if (tramo.getTardanzaCita2() == 0) {
                            tiempoAtencionCita2 = (int) minutosduracionAtencionCita;
                        } else {
                            tiempoAtencionCita2 = tramo.getTiempoPermanenciaCita2();
                        }
                        tramo.setTiempoAtencionCita2(tiempoAtencionCita2);
                        tramo.setEstado(Tramo.EstadoTramo.completado);
                        tramoRepository.save(tramo); // Modificado

                        SlaPorEstablecimiento slaPorEstablecimiento = new SlaPorEstablecimiento();
                        slaPorEstablecimiento.setIdEstablecimiento(establecimientoId);
                        slaPorEstablecimiento.setTiempoAtencionMinutos(tiempoAtencionCita2);
                        //slaPorEstablecimiento.setIdCliente();
                        slaPorEstablecimiento.setIdViaje(viajeId);
                        slaPorEstablecimientoRepo.save(slaPorEstablecimiento);

                    }
                    if (ordenado == 4 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                    }
                    if (ordenado == 4 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                    }
                    if (ordenado == 5 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                    }
                    if (ordenado == 5 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                        LocalDateTime horaLlegadaReal = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(alarm.getEntryTime()),
                                ZoneId.systemDefault()
                        );
                        // Actualizar hora_llegada_real
                        tramo.setHoraLlegadaReal(horaLlegadaReal);
                        tramo.setEstado(Tramo.EstadoTramo.en_curso);

                        Duration duracionTardanza = Duration.between(tramo.getHoraLlegadaProgramada(), horaLlegadaReal);
                        long minutosTardanza = duracionTardanza.toMinutes();
                        int tardanzaValue = (int) (minutosTardanza <= 0 ? 0 : minutosTardanza);
                        tramo.setTardanzaCita2(tardanzaValue);
                        tramoRepository.save(tramo); // Modificado

                    }
                    if (ordenado == 6 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                        LocalDateTime horaSalidaReal = LocalDateTime.now(ZoneId.systemDefault());

                        Duration duracionPermanencia = Duration.between(tramo.getHoraLlegadaReal(), horaSalidaReal);
                        long totalMinutesPermanencia = duracionPermanencia.toMinutes();
                        tramo.setTiempoPermanenciaCita2((int) totalMinutesPermanencia);

                        Duration duracionAtencionCita = Duration.between(tramo.getHoraSalidaProgramada(), horaSalidaReal);
                        long minutosduracionAtencionCita = duracionAtencionCita.toMinutes();
                        int tiempoAtencionCita2;

                        if (tramo.getTardanzaCita2() == 0) {
                            tiempoAtencionCita2 = (int) minutosduracionAtencionCita;
                        } else {
                            tiempoAtencionCita2 = tramo.getTiempoPermanenciaCita2();
                        }
                        tramo.setTiempoAtencionCita2(tiempoAtencionCita2);
                        tramo.setEstado(Tramo.EstadoTramo.completado);
                        tramoRepository.save(tramo); // Modificado

                        SlaPorEstablecimiento slaPorEstablecimiento = new SlaPorEstablecimiento();
                        slaPorEstablecimiento.setIdEstablecimiento(establecimientoId);
                        slaPorEstablecimiento.setTiempoAtencionMinutos(tiempoAtencionCita2);
                        //slaPorEstablecimiento.setIdCliente();
                        slaPorEstablecimiento.setIdViaje(viajeId);
                        slaPorEstablecimientoRepo.save(slaPorEstablecimiento);
                    }
                    if (ordenado == 6 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                    }
                    if (ordenado == 7 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                    }
                    if (ordenado == 7 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);

                        LocalDateTime horaLlegadaReal = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(alarm.getEntryTime()),
                                ZoneId.systemDefault()
                        );
                        // Actualizar hora_llegada_real
                        tramo.setHoraLlegadaReal(horaLlegadaReal);
                        tramo.setEstado(Tramo.EstadoTramo.en_curso);

                        Duration duracionTardanza = Duration.between(tramo.getHoraLlegadaProgramada(), horaLlegadaReal);
                        long minutosTardanza = duracionTardanza.toMinutes();
                        int tardanzaValue = (int) (minutosTardanza <= 0 ? 0 : minutosTardanza);
                        tramo.setTardanzaCita2(tardanzaValue);
                        tramoRepository.save(tramo); // Modificado

                    }
                    if (ordenado == 8 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("ENTRY")) {
                        // Convertir epoch a LocalDateTime
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                        LocalDateTime horaSalidaReal = LocalDateTime.now(ZoneId.systemDefault());

                        Duration duracionPermanencia = Duration.between(tramo.getHoraLlegadaReal(), horaSalidaReal);
                        long totalMinutesPermanencia = duracionPermanencia.toMinutes();
                        tramo.setTiempoPermanenciaCita2((int) totalMinutesPermanencia);

                        Duration duracionAtencionCita = Duration.between(tramo.getHoraSalidaProgramada(), horaSalidaReal);
                        long minutosduracionAtencionCita = duracionAtencionCita.toMinutes();
                        int tiempoAtencionCita2;

                        if (tramo.getTardanzaCita2() == 0) {
                            tiempoAtencionCita2 = (int) minutosduracionAtencionCita;
                        } else {
                            tiempoAtencionCita2 = tramo.getTiempoPermanenciaCita2();
                        }
                        tramo.setTiempoAtencionCita2(tiempoAtencionCita2);
                        tramo.setEstado(Tramo.EstadoTramo.completado);
                        tramoRepository.save(tramo); // Modificado

                        SlaPorEstablecimiento slaPorEstablecimiento = new SlaPorEstablecimiento();
                        slaPorEstablecimiento.setIdEstablecimiento(establecimientoId);
                        slaPorEstablecimiento.setTiempoAtencionMinutos(tiempoAtencionCita2);
                        //slaPorEstablecimiento.setIdCliente();
                        slaPorEstablecimiento.setIdViaje(viajeId);
                        slaPorEstablecimientoRepo.save(slaPorEstablecimiento);
                    }
                    if (ordenado == 8 && Objects.equals(alarm.getGeofenceId(), geocercaId) && alarm.getAlarmType().equals("EXIT")) {
                        System.out.println(" Entro en condifcion : " + orden + " - De la Geocerca nro: " + geocercaId);
                    }
                }
            } else {
                System.out.println("⚠️ No se encontró tramo pendiente para IMEI " + alarm.getImei() + " y geocerca " + alarm.getGeofenceId());
            }

        } catch (Exception e) {
            System.err.println("❌ Error en evaluación de cita: " + e.getMessage());
            e.printStackTrace();
        }
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
            System.out.println("📝 Saving to duplicate_alarms: imei=" + duplicateAlarm.getImei() +
                    ", geofence_id=" + duplicateAlarm.getGeofenceId() +
                    ", alarm_type=" + duplicateAlarm.getAlarmType());
            duplicateAlarmRepository.save(duplicateAlarm);
        } catch (Throwable ex) {
            System.err.println("❌ Error saving to duplicate_alarms for IMEI " + alarm.getImei() + " and geofence " + alarm.getGeofenceId() + ": " + ex.getMessage());
            ex.printStackTrace();
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
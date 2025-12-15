package com.example.fleetIq.service;

import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.model.Alarm;
import com.example.fleetIq.model.Establecimiento;
import com.example.fleetIq.model.Geofence;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.repository.AlarmRepository;
import com.example.fleetIq.repository.EstablecimientoRepository;
import com.example.fleetIq.repository.GeocercaPorEstablecimientoRepository;
import com.example.fleetIq.repository.GeofenceRepository;
import com.example.fleetIq.repository.TrackRepository;
import com.example.fleetIq.repository.TramoRepository;
import com.example.fleetIq.repository.ViajeRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.OptimisticLockException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TramoServiceImpl implements TramoService {

    @Autowired
    private TramoRepository tramoRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private GeocercaPorEstablecimientoRepository geocercaPorEstablecimientoRepo;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private Long lastProcessedAlarmId;

    @PostConstruct
    public void init() {
        // lastProcessedAlarmId = 14093L;
        lastProcessedAlarmId = alarmRepository.findMaxAlarmId().orElse(0L);
        System.out.println("🚀 TramoService inicializado. Última alarma procesada: " + lastProcessedAlarmId);
        System.out.println("⚠️ Las alarmas anteriores a este ID serán ignoradas");
    }

    @Scheduled(fixedRate = 3000)
    public void procesarNuevasAlarmas() {
        try {
            List<Alarm> nuevasAlarmas = alarmRepository.findNewAlarms(lastProcessedAlarmId);

            if (!nuevasAlarmas.isEmpty()) {
                System.out.println("🔔 Procesando " + nuevasAlarmas.size() + " nuevas alarmas...");

                for (Alarm alarm : nuevasAlarmas) {
                    try {
                        procesarAlarma(alarm);
                        lastProcessedAlarmId = alarm.getId();
                    } catch (Exception e) {
                        System.err.println("❌ Error procesando alarma ID " + alarm.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                System.out.println("✅ Alarmas procesadas hasta ID: " + lastProcessedAlarmId);
            }
        } catch (Exception e) {
            System.err.println("❌ Error en procesamiento automático de alarmas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void procesarAlarma(Alarm alarm) {
        List<Tramo> tramosActivos = tramoRepository.findTramosActivosPorVehiculo(alarm.getImei());

        if (tramosActivos.isEmpty()) {
            System.out.println("⚠️ No hay tramo activo para IMEI: " + alarm.getImei());
            return;
        }

        if (tramosActivos.size() > 1) {
            System.out.println("⚠️ Advertencia: Se encontraron " + tramosActivos.size() +
                    " tramos activos para IMEI " + alarm.getImei());
        }

        Tramo tramo = tramosActivos.get(0);
        // 🔍 AGREGAR ESTOS LOGS DE DEPURACIÓN
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔍 DEBUG - Estado del Tramo:");
        System.out.println("   Tramo ID: " + tramo.getId());
        System.out.println("   Estado: " + tramo.getEstado());
        System.out.println("   Hora Llegada Real: " + tramo.getHoraLlegadaReal());
        System.out.println("   Hora Salida Real: " + tramo.getHoraSalidaReal());
        System.out.println("   Hora Llegada Real Destino: " + tramo.getHoraLlegadaRealDestino());
        System.out.println("   Hora Salida Real Destino: " + tramo.getHoraSalidaRealDestino());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        LocalDateTime timestampAlarma = convertirTimestamp(alarm);
        LocalDateTime inicioViaje = tramo.getViaje().getFechaInicioProgramada();

        if (inicioViaje != null && timestampAlarma.isBefore(inicioViaje.minusHours(24))) {
            System.out.println("⚠️ Alarma muy antigua. Ignorando...");
            return;
        }

        Long geocercaId = alarm.getGeofenceId();
        String tipo = alarm.getAlarmType();
        LocalDateTime timestamp = convertirTimestamp(alarm);

        Establecimiento origen = tramo.getEstablecimientoOrigen();
        Establecimiento destino = tramo.getEstablecimientoDestino();

        System.out.println("📍 Evaluando alarma ID " + alarm.getId() +
                " - IMEI: " + alarm.getImei() +
                " - Tipo: " + tipo +
                " - Geocerca: " + geocercaId +
                " - Timestamp: " + timestamp);

        boolean tramoActualizado = false;

        Long geocercaExternaOrigenId = geocercaPorEstablecimientoRepo
                .findGeocercaExternaId(origen.getId()).orElse(null);
        Long geocercaInternaOrigenId = geocercaPorEstablecimientoRepo
                .findGeocercaInternaId(origen.getId()).orElse(null);
        Long geocercaExternaDestinoId = geocercaPorEstablecimientoRepo
                .findGeocercaExternaId(destino.getId()).orElse(null);
        Long geocercaInternaDestinoId = geocercaPorEstablecimientoRepo
                .findGeocercaInternaId(destino.getId()).orElse(null);

        // 🔍 AGREGAR ESTOS LOGS TAMBIÉN
        System.out.println("🗺️ Geocercas del Tramo:");
        System.out.println("   Origen Externa: " + geocercaExternaOrigenId);
        System.out.println("   Origen Interna: " + geocercaInternaOrigenId);
        System.out.println("   Destino Externa: " + geocercaExternaDestinoId);
        System.out.println("   Destino Interna: " + geocercaInternaDestinoId);
        System.out.println("   Geocerca de Alarma: " + geocercaId);

        // ==================== ORIGEN ====================

        if (geocercaExternaOrigenId != null &&
                geocercaId.equals(geocercaExternaOrigenId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraLlegadaReal() == null) {

            tramo.setHoraLlegadaReal(timestamp);
            tramo.setEstado(Tramo.EstadoTramo.en_curso);
            tramoActualizado = true;
            System.out.println("✅ ORIGEN - Primera llegada registrada: " + timestamp);
            System.out.println("📊 Estado: EN_CURSO");
        }

        if (geocercaInternaOrigenId != null &&
                geocercaId.equals(geocercaInternaOrigenId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraLlegadaReal() != null &&
                tramo.getHoraSalidaReal() == null) {

            System.out.println("🅿️ ORIGEN - Confirmación de estacionamiento en zona interna");
        }
        if (geocercaInternaOrigenId != null &&
                geocercaId.equals(geocercaInternaOrigenId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraLlegadaRealDestino() == null) {

            LocalDateTime salidaAnterior = tramo.getHoraSalidaReal();
            tramo.setHoraSalidaReal(timestamp);
            tramoActualizado = true;

            if (salidaAnterior != null) {
                long minutos = java.time.Duration
                        .between(salidaAnterior, timestamp)
                        .toMinutes();
                System.out.println("🔄 ORIGEN - Salida actualizada (maniobra " + minutos + " min después)");
            } else {
                System.out.println("✅ ORIGEN - Primera salida registrada: " + timestamp);
            }
        }

        if (geocercaExternaOrigenId != null &&
                geocercaId.equals(geocercaExternaOrigenId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraSalidaReal() != null &&
                tramo.getHoraLlegadaRealDestino() == null) {

            long segundos = java.time.Duration
                    .between(tramo.getHoraSalidaReal(), timestamp)
                    .getSeconds();

            System.out.println("🚗 ORIGEN - Confirmación de alejamiento (" + segundos +
                    "s después de salir interna)");
        }

        // ==================== DESTINO ====================

        if (geocercaExternaDestinoId != null &&
                geocercaId.equals(geocercaExternaDestinoId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraLlegadaRealDestino() == null &&
                tramo.getHoraSalidaReal() != null) {

            tramo.setHoraLlegadaRealDestino(timestamp);
            tramoActualizado = true;

            long minutosViaje = java.time.Duration
                    .between(tramo.getHoraSalidaReal(), timestamp)
                    .toMinutes();

            System.out.println("✅ DESTINO - Primera llegada registrada: " + timestamp);
            System.out.println("⏱️ Tiempo de viaje: " + minutosViaje + " minutos");
        }

        if (geocercaInternaDestinoId != null &&
                geocercaId.equals(geocercaInternaDestinoId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraLlegadaRealDestino() != null &&
                tramo.getHoraSalidaRealDestino() == null) {

            System.out.println("🅿️ DESTINO - Confirmación de estacionamiento en zona interna");
        }

        if (geocercaInternaDestinoId != null &&
                geocercaId.equals(geocercaInternaDestinoId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraLlegadaRealDestino() != null) {

            LocalDateTime salidaAnterior = tramo.getHoraSalidaRealDestino();
            tramo.setHoraSalidaRealDestino(timestamp);
            tramoActualizado = true;

            if (salidaAnterior != null) {
                long minutos = java.time.Duration
                        .between(salidaAnterior, timestamp)
                        .toMinutes();
                System.out.println("🔄 DESTINO - Salida actualizada (maniobra " + minutos + " min después)");
            } else {
                System.out.println("✅ DESTINO - Primera salida registrada: " + timestamp);
            }
        }

        if (geocercaExternaDestinoId != null &&
                geocercaId.equals(geocercaExternaDestinoId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraSalidaRealDestino() != null &&
                tramo.getEstado() != Tramo.EstadoTramo.completado) {

            long segundos = java.time.Duration
                    .between(tramo.getHoraSalidaRealDestino(), timestamp)
                    .getSeconds();

            if (segundos >= 30) {
                tramo.setEstado(Tramo.EstadoTramo.completado);
                tramoActualizado = true;
                System.out.println("🎉 TRAMO COMPLETADO - Salió de zona externa del destino");
                System.out.println(
                        "⏱️ Tiempo en destino: " +
                                java.time.Duration
                                        .between(tramo.getHoraLlegadaRealDestino(),
                                                tramo.getHoraSalidaRealDestino())
                                        .toMinutes()
                                + " min");
            } else {
                System.out.println("⚠️ Salida externa muy rápida (" + segundos + "s), posible salto GPS");
            }
        }

        if (tramoActualizado) {
            tramoRepository.save(tramo);
            System.out.println("💾 Tramo " + tramo.getId() + " actualizado - Estado: " + tramo.getEstado());
            imprimirResumenTramo(tramo);
        } else {
            System.out.println("ℹ️ Alarma ignorada - No corresponde a transición válida");
        }
    }

    private void imprimirResumenTramo(Tramo tramo) {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📋 RESUMEN TRAMO #" + tramo.getId());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📍 Origen: " + tramo.getEstablecimientoOrigen().getNombre());
        System.out.println("   ↳ Llegada: " + tramo.getHoraLlegadaReal());
        System.out.println("   ↳ Salida:  " + tramo.getHoraSalidaReal());

        if (tramo.getHoraSalidaReal() != null && tramo.getHoraLlegadaReal() != null) {
            long permanencia = java.time.Duration
                    .between(tramo.getHoraLlegadaReal(), tramo.getHoraSalidaReal())
                    .toMinutes();
            System.out.println("   ⏱️ Permanencia: " + permanencia + " minutos");
        }

        System.out.println("📍 Destino: " + tramo.getEstablecimientoDestino().getNombre());
        System.out.println("   ↳ Llegada: " + tramo.getHoraLlegadaRealDestino());
        System.out.println("   ↳ Salida:  " + tramo.getHoraSalidaRealDestino());

        if (tramo.getHoraSalidaRealDestino() != null && tramo.getHoraLlegadaRealDestino() != null) {
            long permanencia = java.time.Duration
                    .between(tramo.getHoraLlegadaRealDestino(), tramo.getHoraSalidaRealDestino())
                    .toMinutes();
            System.out.println("   ⏱️ Permanencia: " + permanencia + " minutos");
        }

        System.out.println("📊 Estado: " + tramo.getEstado());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private LocalDateTime convertirTimestamp(Alarm alarm) {
        Long timestamp = null;

        // Usar el timestamp correspondiente al tipo de alarma
        if ("ENTRY".equals(alarm.getAlarmType())) {
            timestamp = alarm.getEntryTime();
            if (timestamp == null) {
                timestamp = alarm.getExitTime();
            }
        } else if ("EXIT".equals(alarm.getAlarmType())) {
            timestamp = alarm.getExitTime();
            if (timestamp == null) {
                timestamp = alarm.getEntryTime();
            }
        } else {
            timestamp = alarm.getEntryTime();
            if (timestamp == null) {
                timestamp = alarm.getExitTime();
            }
        }

        if (timestamp == null) {
            return LocalDateTime.now();
        }

        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault());
    }

    @Override
    public List<TramoDto> listarTramosPorViaje(String viajeId) {
        return tramoRepository.findByViajeId(viajeId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void crearTramo(TramoDto tramoDto) {
        try {
            Tramo tramo = new Tramo();
            tramo.setOrden(tramoDto.getOrden());
            tramo.setTipoActividad(Tramo.TipoActividad.valueOf(tramoDto.getTipoActividad()));
            tramo.setDescripcion(tramoDto.getDescripcion());
            tramo.setHoraLlegadaProgramada(tramoDto.getHoraLlegadaProgramada());
            tramo.setHoraSalidaProgramada(tramoDto.getHoraSalidaProgramada());
            tramo.setHoraLlegadaReal(tramoDto.getHoraLlegadaReal());
            tramo.setHoraSalidaReal(tramoDto.getHoraSalidaReal());
            tramo.setEstado(Tramo.EstadoTramo.valueOf(tramoDto.getEstado()));
            tramo.setSlaMinutos(tramoDto.getSlaMinutos());
            tramo.setObservaciones(tramoDto.getObservaciones());
            tramo.setTracto(tramoDto.getTracto());
            tramo.setChasis(tramoDto.getChasis());
            tramo.setConductor(tramoDto.getConductor());
            tramo.setTardanzaCita1(tramoDto.getTardanzaCita1());
            tramo.setTiempoPermanenciaCita1(tramoDto.getTiempoPermanenciaCita1());
            tramo.setTiempoAtencionCita1(tramoDto.getTiempoAtencionCita1());
            tramo.setTardanzaCita2(tramoDto.getTardanzaCita2());
            tramo.setTiempoPermanenciaCita2(tramoDto.getTiempoPermanenciaCita2());
            tramo.setTiempoAtencionCita2(tramoDto.getTiempoAtencionCita2());

            Viaje viaje = viajeRepository.findById(tramoDto.getViajeId())
                    .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado: " + tramoDto.getViajeId()));
            tramo.setViaje(viaje);
            viaje.getTramos().add(tramo);

            Establecimiento establecimientoOrigen = establecimientoRepository
                    .findById(tramoDto.getEstablecimientoOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Establecimiento origen no encontrado: " + tramoDto.getEstablecimientoOrigenId()));
            tramo.setEstablecimientoOrigen(establecimientoOrigen);

            Establecimiento establecimientoDestino = establecimientoRepository
                    .findById(tramoDto.getEstablecimientoDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Establecimiento destino no encontrado: " + tramoDto.getEstablecimientoDestinoId()));
            tramo.setEstablecimientoDestino(establecimientoDestino);

            tramoRepository.save(tramo);
            viajeRepository.save(viaje);
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Error de concurrencia al crear el tramo", e);
        }
    }

    private TramoDto convertToDto(Tramo tramo) {
        TramoDto dto = new TramoDto();
        dto.setId(tramo.getId());
        dto.setViajeId(tramo.getViaje().getId());
        dto.setOrden(tramo.getOrden());
        dto.setEstablecimientoOrigenId(tramo.getEstablecimientoOrigen().getId());
        dto.setEstablecimientoDestinoId(tramo.getEstablecimientoDestino().getId());
        dto.setTipoActividad(tramo.getTipoActividad().name());
        dto.setDescripcion(tramo.getDescripcion());
        dto.setHoraLlegadaProgramada(tramo.getHoraLlegadaProgramada());
        dto.setHoraSalidaProgramada(tramo.getHoraSalidaProgramada());
        dto.setHoraLlegadaReal(tramo.getHoraLlegadaReal());
        dto.setHoraSalidaReal(tramo.getHoraSalidaReal());
        dto.setHoraLlegadaRealDestino(tramo.getHoraLlegadaRealDestino());
        dto.setHoraSalidaRealDestino(tramo.getHoraSalidaRealDestino());
        dto.setEstado(tramo.getEstado().name());
        dto.setSlaMinutos(tramo.getSlaMinutos());
        dto.setObservaciones(tramo.getObservaciones());
        dto.setTracto(tramo.getTracto());
        dto.setChasis(tramo.getChasis());
        dto.setConductor(tramo.getConductor());
        dto.setTardanzaCita1(tramo.getTardanzaCita1());
        dto.setTiempoPermanenciaCita1(tramo.getTiempoPermanenciaCita1());
        dto.setTiempoAtencionCita1(tramo.getTiempoAtencionCita1());
        dto.setTardanzaCita2(tramo.getTardanzaCita2());
        dto.setTiempoPermanenciaCita2(tramo.getTiempoPermanenciaCita2());
        dto.setTiempoAtencionCita2(tramo.getTiempoAtencionCita2());

        calcularEtaYAvance(tramo, dto);

        return dto;
    }

    private CoordenadaDto calcularCentroideGeocerca(String establecimientoId, String tipoGeocerca) {
        try {
            Long geocercaId;

            if ("EXTERNA".equals(tipoGeocerca)) {
                geocercaId = geocercaPorEstablecimientoRepo
                        .findGeocercaExternaId(establecimientoId)
                        .orElse(null);
            } else {
                geocercaId = geocercaPorEstablecimientoRepo
                        .findGeocercaInternaId(establecimientoId)
                        .orElse(null);
            }

            if (geocercaId == null) {
                System.out.println("⚠️ No se encontró geocerca " + tipoGeocerca +
                        " para establecimiento ID " + establecimientoId);
                return null;
            }

            Geofence geofence = geofenceRepository.findById(geocercaId).orElse(null);

            if (geofence == null || geofence.getPoints() == null || geofence.getPoints().isEmpty()) {
                System.out.println("⚠️ Geocerca sin puntos definidos: " + geocercaId);
                return null;
            }

            String pointsJson = geofence.getPoints().trim();

            JSONArray puntosArray;
            try {
                puntosArray = new JSONArray(pointsJson);
            } catch (JSONException e) {
                System.err.println("❌ Error parseando JSON de puntos para geocerca " + geocercaId);
                return null;
            }

            if (puntosArray.length() < 3) {
                System.out.println("⚠️ Geocerca con menos de 3 puntos: " + geocercaId);
                return null;
            }

            double sumaLat = 0.0;
            double sumaLng = 0.0;
            int puntosValidos = 0;

            for (int i = 0; i < puntosArray.length(); i++) {
                try {
                    JSONArray coordenada = puntosArray.getJSONArray(i);

                    if (coordenada.length() == 2) {
                        double lat = coordenada.getDouble(0);
                        double lng = coordenada.getDouble(1);

                        sumaLat += lat;
                        sumaLng += lng;
                        puntosValidos++;
                    }
                } catch (JSONException e) {
                    System.out.println("⚠️ Coordenada inválida ignorada en posición " + i);
                }
            }

            if (puntosValidos == 0) {
                return null;
            }

            double centroideLat = sumaLat / puntosValidos;
            double centroideLng = sumaLng / puntosValidos;

            System.out.println("📍 Centroide calculado para establecimiento " + establecimientoId +
                    " (geocerca " + tipoGeocerca + " ID " + geocercaId + "): " +
                    String.format("%.6f, %.6f", centroideLat, centroideLng) +
                    " (desde " + puntosValidos + " puntos)");

            return new CoordenadaDto(centroideLat, centroideLng);

        } catch (Exception e) {
            System.err.println("❌ Error calculando centroide: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ============================================================================
    // BACKEND - TramoService.java - Versión con Diagnóstico Completo
    // ============================================================================

    private void calcularEtaYAvance(Tramo tramo, TramoDto dto) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🚀 INICIANDO CÁLCULO DE ETA Y AVANCE                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        try {
            // ====================================================================
            // PASO 1: Validar Vehículo y Track
            // ====================================================================

            if (tramo.getViaje() == null) {
                System.out.println("❌ ERROR: El tramo no tiene viaje asociado");
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            if (tramo.getViaje().getVehiculo() == null) {
                System.out.println("❌ ERROR: El viaje no tiene vehículo asociado");
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            String imei = tramo.getViaje().getVehiculo().getImei();
            System.out.println("📱 IMEI del vehículo: " + imei);

            Track trackActual = trackRepository.findLatestTrackByImei(imei);

            if (trackActual == null) {
                System.out.println("⚠️ No hay track actual disponible para IMEI: " + imei);
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            System.out.println("✅ Track encontrado - ID: " + trackActual.getId());
            System.out.println("   📍 Lat: " + trackActual.getLatitude() + ", Lon: " + trackActual.getLongitude());
            System.out.println("   🕐 Timestamp: " + trackActual.getGpstime());

            // ====================================================================
            // PASO 2: Obtener Establecimientos
            // ====================================================================

            Establecimiento origen = tramo.getEstablecimientoOrigen();
            Establecimiento destino = tramo.getEstablecimientoDestino();

            if (origen == null || destino == null) {
                System.out.println("❌ ERROR: Origen o destino no disponibles");
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            System.out.println("📍 Origen: " + origen.getNombre() + " (ID: " + origen.getId() + ")");
            System.out.println("📍 Destino: " + destino.getNombre() + " (ID: " + destino.getId() + ")");

            // ====================================================================
            // PASO 3: Calcular Centroides
            // ====================================================================

            System.out.println("\n🎯 Calculando centroides de geocercas...");
            CoordenadaDto centroideOrigen = calcularCentroideGeocerca(origen.getId(), "EXTERNA");
            CoordenadaDto centroideDestino = calcularCentroideGeocerca(destino.getId(), "EXTERNA");

            if (centroideOrigen == null) {
                System.out.println("❌ ERROR: No se pudo calcular centroide de ORIGEN");
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            if (centroideDestino == null) {
                System.out.println("❌ ERROR: No se pudo calcular centroide de DESTINO");
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            System.out
                    .println("✅ Centroide Origen: [" + centroideOrigen.latitud + ", " + centroideOrigen.longitud + "]");
            System.out.println(
                    "✅ Centroide Destino: [" + centroideDestino.latitud + ", " + centroideDestino.longitud + "]");

            // ====================================================================
            // PASO 4: Calcular Ruta Actual → Destino (para ETA)
            // ====================================================================

            System.out.println("\n🗺️ Calculando ruta: POSICIÓN ACTUAL → DESTINO (para ETA)...");
            DuracionDistanciaResult resultadoActualDestino = obtenerDuracionYDistanciaOSRM(
                    trackActual.getLatitude(),
                    trackActual.getLongitude(),
                    centroideDestino.latitud,
                    centroideDestino.longitud);

            String etaCalculado = null;

            if (resultadoActualDestino != null) {
                LocalDateTime horaActual = LocalDateTime.now();
                LocalDateTime eta = horaActual.plusMinutes((long) resultadoActualDestino.duracionMinutos);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                etaCalculado = eta.format(formatter);

                System.out.println("✅ ETA CALCULADO: " + etaCalculado);
                System.out.println(
                        "   ⏱️ Duración restante: " + Math.round(resultadoActualDestino.duracionMinutos) + " minutos");
                System.out.println("   📏 Distancia restante: "
                        + String.format("%.2f km", resultadoActualDestino.distanciaMetros / 1000));
            } else {
                System.out.println("❌ ERROR: No se pudo calcular ruta Actual → Destino");
                System.out.println("   Posible causa: OSRM no encontró ruta válida entre los puntos");
            }

            // ====================================================================
            // PASO 5: Calcular Ruta Origen → Destino (para Avance)
            // ====================================================================

            System.out.println("\n🗺️ Calculando ruta: ORIGEN → DESTINO (para Avance)...");
            DuracionDistanciaResult resultadoOrigenDestino = obtenerDuracionYDistanciaOSRM(
                    centroideOrigen.latitud,
                    centroideOrigen.longitud,
                    centroideDestino.latitud,
                    centroideDestino.longitud);

            double avanceCalculado = 0.0;

            if (resultadoOrigenDestino != null && resultadoActualDestino != null) {
                double distanciaTotal = resultadoOrigenDestino.distanciaMetros;
                double distanciaRestante = resultadoActualDestino.distanciaMetros;
                double distanciaRecorrida = distanciaTotal - distanciaRestante;

                if (distanciaTotal > 0) {
                    avanceCalculado = (distanciaRecorrida / distanciaTotal) * 100.0;
                    avanceCalculado = Math.max(0.0, Math.min(100.0, avanceCalculado));
                    avanceCalculado = Math.round(avanceCalculado * 10.0) / 10.0;
                }

                System.out.println("✅ AVANCE CALCULADO: " + avanceCalculado + "%");
                System.out.println("   📏 Distancia total: " + String.format("%.2f km", distanciaTotal / 1000));
                System.out.println("   📏 Distancia recorrida: " + String.format("%.2f km", distanciaRecorrida / 1000));
                System.out.println("   📏 Distancia restante: " + String.format("%.2f km", distanciaRestante / 1000));
            } else {
                System.out.println("❌ ERROR: No se pudo calcular el avance");
                if (resultadoOrigenDestino == null) {
                    System.out.println("   ⚠️ Fallo en ruta Origen → Destino");
                }
                if (resultadoActualDestino == null) {
                    System.out.println("   ⚠️ Fallo en ruta Actual → Destino");
                }
            }

            // ====================================================================
            // PASO 6: Asignar valores al DTO
            // ====================================================================

            System.out.println("\n💾 Asignando valores al DTO...");
            System.out.println("   ETA antes de asignar: " + dto.getEta());
            System.out.println("   Avance antes de asignar: " + dto.getAvance());

            dto.setEta(etaCalculado);
            dto.setAvance(avanceCalculado);

            System.out.println("   ✅ ETA asignado: " + dto.getEta());
            System.out.println("   ✅ Avance asignado: " + dto.getAvance());

            // ====================================================================
            // RESUMEN FINAL
            // ====================================================================

            System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║  📊 RESUMEN FINAL - Tramo ID: " + tramo.getId());
            System.out.println("╠════════════════════════════════════════════════════════════════╣");
            System.out.println("║  🚗 Vehículo IMEI: " + imei);
            System.out.println("║  📍 Ruta: " + origen.getNombre() + " → " + destino.getNombre());
            System.out.println("║  ⏰ ETA Final: " + (etaCalculado != null ? etaCalculado : "NO DISPONIBLE"));
            System.out.println("║  📈 Avance Final: " + avanceCalculado + "%");
            System.out.println("║  🎯 Estado: " + (etaCalculado != null ? "✅ ÉXITO" : "❌ ERROR"));
            System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println("\n╔════════════════════════════════════════════════════════════════╗");
            System.err.println("║  ❌ ERROR FATAL EN calcularEtaYAvance                          ║");
            System.err.println("╚════════════════════════════════════════════════════════════════╝");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Tipo: " + e.getClass().getName());
            e.printStackTrace();

            dto.setEta(null);
            dto.setAvance(0.0);
        }
    }

    private DuracionDistanciaResult obtenerDuracionYDistanciaOSRM(
            double origenLat, double origenLon,
            double destinoLat, double destinoLon) {

        try {
            String coordenadasOrigen = String.format(Locale.US, "%.6f,%.6f", origenLon, origenLat);
            String coordenadasDestino = String.format(Locale.US, "%.6f,%.6f", destinoLon, destinoLat);

            String url = "https://router.project-osrm.org/route/v1/driving/" +
                    coordenadasOrigen + ";" + coordenadasDestino +
                    "?overview=false";

            System.out.println("   🌐 Consultando OSRM: " + url);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("   📡 OSRM Status Code: " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("   ❌ OSRM respondió con código: " + response.statusCode());
                return null;
            }

            JSONObject json = new JSONObject(response.body());

            if (!json.has("routes") || json.getJSONArray("routes").length() == 0) {
                System.err.println("   ❌ OSRM no encontró ruta entre los puntos");
                System.err.println("   📄 Respuesta: " + response.body());
                return null;
            }

            JSONObject route = json.getJSONArray("routes").getJSONObject(0);
            double duracionSegundos = route.getDouble("duration");
            double distanciaMetros = route.getDouble("distance");
            double duracionMinutos = duracionSegundos / 60.0;

            System.out.println("   ✅ OSRM - Distancia: " + String.format("%.2f km", distanciaMetros / 1000) +
                    " | Duración: " + String.format("%.1f min", duracionMinutos));

            return new DuracionDistanciaResult(duracionMinutos, distanciaMetros);

        } catch (java.net.http.HttpTimeoutException e) {
            System.err.println("   ⏱️ Timeout conectando a OSRM");
            return null;
        } catch (IOException e) {
            System.err.println("   ❌ Error de red con OSRM: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("   ❌ Petición a OSRM interrumpida");
            Thread.currentThread().interrupt();
            return null;
        } catch (JSONException e) {
            System.err.println("   ❌ Error parseando respuesta de OSRM: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("   ❌ Error inesperado con OSRM: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static class CoordenadaDto {
        double latitud;
        double longitud;

        CoordenadaDto(double latitud, double longitud) {
            this.latitud = latitud;
            this.longitud = longitud;
        }
    }

    private static class DuracionDistanciaResult {
        double duracionMinutos;
        double distanciaMetros;

        DuracionDistanciaResult(double duracionMinutos, double distanciaMetros) {
            this.duracionMinutos = duracionMinutos;
            this.distanciaMetros = distanciaMetros;
        }
    }
}
package com.example.fleetIq.service;

import com.example.fleetIq.dto.EstablecimientoDto;
import com.example.fleetIq.dto.ParadaDetectadaDto;
import com.example.fleetIq.dto.ParadaDetectadaDto.LugarCercano;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TramoServiceImpl implements TramoService {
    // Zona horaria de Perú
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

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
    // @Autowired
    // private DetectorParadasService detectorParadasService;
    @Autowired
    private DetectorParadasMejorado detectorMejorado;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private GeocercaPorEstablecimientoRepository geocercaPorEstablecimientoRepo;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private Long lastProcessedAlarmId;

    @PostConstruct
    public void init() {
        // lastProcessedAlarmId = 17873L;
        lastProcessedAlarmId = alarmRepository.findMaxAlarmId().orElse(0L);
        System.out.println("🚀 TramoService inicializado. Última alarma procesada: " + lastProcessedAlarmId);
        System.out.println("⚠️ Las alarmas anteriores a este ID serán ignoradas");
    }

    // @Scheduled(fixedRate = 3000)
    // @Transactional // 1. Mantiene la sesión abierta para todo el lote (evita N+1
    // y
    // // LazyInitException)
    // public void procesarNuevasAlarmas() {
    // try {
    // List<Alarm> nuevasAlarmas =
    // alarmRepository.findNewAlarms(lastProcessedAlarmId);

    // if (!nuevasAlarmas.isEmpty()) {
    // System.out.println("🔔 Procesando " + nuevasAlarmas.size() + " nuevas
    // alarmas...");

    // for (Alarm alarm : nuevasAlarmas) {
    // try {
    // procesarAlarma(alarm);
    // lastProcessedAlarmId = alarm.getId();
    // } catch (Exception e) {
    // System.err.println("❌ Error procesando alarma ID " + alarm.getId() + ": " +
    // e.getMessage());
    // e.printStackTrace();
    // }
    // }

    // System.out.println("✅ Alarmas procesadas hasta ID: " + lastProcessedAlarmId);
    // }
    // } catch (Exception e) {
    // System.err.println("❌ Error en procesamiento automático de alarmas: " +
    // e.getMessage());
    // e.printStackTrace();
    // }
    // }
    @Scheduled(fixedRate = 3000)
    @Transactional
    public void procesarNuevasAlarmas() {
        try {
            List<Alarm> nuevasAlarmas = alarmRepository.findNewAlarms(lastProcessedAlarmId);

            if (!nuevasAlarmas.isEmpty()) {
                // ✅ ORDENAR POR TIMESTAMP CRONOLÓGICO
                nuevasAlarmas.sort((a1, a2) -> {
                    Long t1 = obtenerTimestampRelevante(a1);
                    Long t2 = obtenerTimestampRelevante(a2);

                    int timeCompare = Long.compare(t1, t2);

                    // Si timestamps son IGUALES, priorizar EXIT antes que ENTRY
                    // (el vehículo primero sale de interna, luego entra a externa)
                    if (timeCompare == 0) {
                        int priority1 = getPrioridad(a1);
                        int priority2 = getPrioridad(a2);
                        return Integer.compare(priority1, priority2);
                    }

                    return timeCompare;
                });

                System.out.println("🔔 Procesando " + nuevasAlarmas.size() + " nuevas alarmas...");
                System.out.println("📋 Orden cronológico:");

                for (int i = 0; i < nuevasAlarmas.size(); i++) {
                    Alarm alarm = nuevasAlarmas.get(i);
                    Long timestamp = obtenerTimestampRelevante(alarm);
                    LocalDateTime fecha = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(timestamp),
                            ZONA_PERU);

                    System.out.println(String.format("   %d. ID=%d | %s | %s | Geocerca=%d | %s",
                            i + 1,
                            alarm.getId(),
                            alarm.getAlarmType(),
                            fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            alarm.getGeofenceId(),
                            alarm.getImei()));
                }

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

    // Obtiene el timestamp más relevante de una alarma

    private Long obtenerTimestampRelevante(Alarm alarm) {
        if ("ENTRY".equals(alarm.getAlarmType())) {
            return alarm.getEntryTime() != null ? alarm.getEntryTime() : alarm.getExitTime();
        } else if ("EXIT".equals(alarm.getAlarmType())) {
            return alarm.getExitTime() != null ? alarm.getExitTime() : alarm.getEntryTime();
        } else {
            // ENTRY_EXIT o desconocido
            return alarm.getEntryTime() != null ? alarm.getEntryTime() : alarm.getExitTime();
        }
    }

    // Asigna prioridad cuando timestamps son iguales
    // Menor número = mayor prioridad

    private int getPrioridad(Alarm alarm) {
        String tipo = alarm.getAlarmType();

        // En caso de timestamps iguales, procesamos en este orden:
        if ("EXIT".equals(tipo)) {
            return 1; // Primera prioridad: salidas (sale de interna primero)
        } else if ("ENTRY".equals(tipo)) {
            return 2; // Segunda prioridad: entradas (entra a externa después)
        } else {
            return 3; // ENTRY_EXIT al final
        }
    }

    @Transactional
    public void procesarAlarma(Alarm alarm) {
        List<Tramo> tramosActivos = tramoRepository.findTramosActivosPorVehiculo(alarm.getImei());

        if (tramosActivos.isEmpty()) {
            System.out.println("⚠️ No hay tramo activo para IMEI: " + alarm.getImei());
            return;
        }

        tramosActivos.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));

        Tramo tramo = null;
        for (Tramo t : tramosActivos) {
            if (t.getEstado() == Tramo.EstadoTramo.en_curso) {
                tramo = t;
                break;
            }
        }

        if (tramo == null) {
            tramo = tramosActivos.stream()
                    .filter(t -> t.getEstado() == Tramo.EstadoTramo.pendiente)
                    .findFirst()
                    .orElse(tramosActivos.get(0));
        }

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔍 DEBUG - Estado del Tramo:");
        System.out.println("   Tramo ID: " + tramo.getId());
        System.out.println("   Orden: " + tramo.getOrden());
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

        System.out.println("🗺️ Geocercas del Tramo:");
        System.out.println("   Origen Externa: " + geocercaExternaOrigenId);
        System.out.println("   Origen Interna: " + geocercaInternaOrigenId);
        System.out.println("   Destino Externa: " + geocercaExternaDestinoId);
        System.out.println("   Destino Interna: " + geocercaInternaDestinoId);
        System.out.println("   Geocerca de Alarma: " + geocercaId);

        // CASO ESPECIAL: HERENCIA DE TRAMO ANTERIOR
        if (tramo.getEstado() == Tramo.EstadoTramo.pendiente &&
                tramo.getHoraLlegadaReal() == null && // ✅ Usar campo legacy
                tramo.getOrden() > 1) {

            boolean esAlarmaOrigen = (geocercaExternaOrigenId != null && geocercaId.equals(geocercaExternaOrigenId)) ||
                    (geocercaInternaOrigenId != null && geocercaId.equals(geocercaInternaOrigenId));

            if (esAlarmaOrigen) {
                Tramo tramoAnterior = obtenerTramoAnterior(tramo);
                if (tramoAnterior != null &&
                        tramoAnterior.getEstado() == Tramo.EstadoTramo.completado &&
                        tramoAnterior.getHoraSalidaGeocercaExternaDestino2() != null) {

                    System.out.println("🔄 ACTIVACIÓN AUTOMÁTICA: Heredando del tramo anterior");

                    // Heredar todos los campos
                    tramo.setHoraEntradaGeocercaExternaOrigen(tramoAnterior.getHoraEntradaGeocercaExternaDestino());
                    tramo.setHoraSalidaGeocercaExternaOrigen1(tramoAnterior.getHoraSalidaGeocercaExternaDestino1());
                    tramo.setHoraEntradaGeocercaInternaOrigen(tramoAnterior.getHoraEntradaGeocercaInternaDestino());
                    tramo.setHoraSalidaGeocercaInternaOrigen(tramoAnterior.getHoraSalidaGeocercaInternaDestino());
                    tramo.setHoraEntradaGeocercaExternaOrigen2(tramoAnterior.getHoraEntradaGeocercaExternaDestino2());
                    tramo.setHoraSalidaGeocercaExternaOrigen2(tramoAnterior.getHoraSalidaGeocercaExternaDestino2());
                    tramo.setTiempoAtencionCita1(tramoAnterior.getTiempoAtencionCita2());

                    tramo.setHoraLlegadaReal(tramoAnterior.getHoraEntradaGeocercaExternaDestino());
                    tramo.setHoraSalidaReal(tramoAnterior.getHoraSalidaGeocercaExternaDestino2());

                    tramo.setEstado(Tramo.EstadoTramo.en_curso);

                    // ✅ GUARDAR Y SALIR
                    tramoRepository.save(tramo);
                    System.out.println("✅ Tramo activado con herencia completa");
                    System.out.println("💾 Cambios guardados en BD");
                    return; // ⚠️ IMPORTANTE: Evita procesar más condiciones
                }
            }
        }
        // ========================================================================
        // ORIGEN - GEOCERCA EXTERNA
        // ========================================================================

        // 1️⃣ PRIMERA ENTRADA a Externa (calle → acceso)
        if (geocercaExternaOrigenId != null &&
                geocercaId.equals(geocercaExternaOrigenId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraEntradaGeocercaExternaOrigen() == null) {

            tramo.setHoraEntradaGeocercaExternaOrigen(timestamp);
            tramo.setHoraLlegadaReal(timestamp);
            tramo.setEstado(Tramo.EstadoTramo.en_curso);
            tramoActualizado = true;

            System.out.println("✅ ORIGEN [1/6] - Entrada Externa (desde calle)");
            System.out.println("   📊 Estado: EN_CURSO");

            // 🆕 PRE-CALCULAR COORDENADAS DE ORIGEN Y DESTINO
            System.out.println("📍 Pre-calculando coordenadas para el tramo...");

            CoordenadaDto coordsOrigen = obtenerCoordenadasEstablecimiento(origen.getId());
            if (coordsOrigen != null) {
                System.out.println("   ✅ Origen: [" + coordsOrigen.latitud + ", " + coordsOrigen.longitud + "]");
            } else {
                System.out.println("   ⚠️ No se pudieron calcular coordenadas del origen");
            }

            CoordenadaDto coordsDestino = obtenerCoordenadasEstablecimiento(destino.getId());
            if (coordsDestino != null) {
                System.out.println("   ✅ Destino: [" + coordsDestino.latitud + ", " + coordsDestino.longitud + "]");
            } else {
                System.out.println("   ⚠️ No se pudieron calcular coordenadas del destino");
            }
        }

        // 2️⃣ PRIMERA SALIDA de Externa (acceso → patio)
        if (geocercaExternaOrigenId != null &&
                geocercaId.equals(geocercaExternaOrigenId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraEntradaGeocercaExternaOrigen() != null &&
                tramo.getHoraSalidaGeocercaExternaOrigen1() == null) {

            tramo.setHoraSalidaGeocercaExternaOrigen1(timestamp);
            tramoActualizado = true;

            System.out.println("✅ ORIGEN [2/6] - Salida Externa #1 (hacia patio)");
        }

        // ========================================================================
        // ORIGEN - GEOCERCA INTERNA
        // ========================================================================

        // 3️⃣ ENTRADA a Interna (inicio atención)
        if (geocercaInternaOrigenId != null &&
                geocercaId.equals(geocercaInternaOrigenId) &&
                ("ENTRY".equals(tipo) || "ENTRY_EXIT".equals(tipo)) &&
                tramo.getHoraEntradaGeocercaInternaOrigen() == null) {

            tramo.setHoraEntradaGeocercaInternaOrigen(timestamp);
            tramoActualizado = true;

            System.out.println("🅿️ ORIGEN [3/6] - Entrada Interna (inicio atención)");
        }

        // 4️⃣ SALIDA de Interna (fin atención)
        if (geocercaInternaOrigenId != null &&
                geocercaId.equals(geocercaInternaOrigenId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraEntradaGeocercaInternaOrigen() != null &&
                tramo.getHoraSalidaGeocercaInternaOrigen() == null) { // ✅ Evitar sobrescribir

            tramo.setHoraSalidaGeocercaInternaOrigen(timestamp);
            // ❌ NO ASIGNAR horaSalidaReal AQUÍ
            tramoActualizado = true;

            // CALCULAR TIEMPO DE ATENCIÓN
            long tiempoAtencionMinutos = java.time.Duration
                    .between(tramo.getHoraEntradaGeocercaInternaOrigen(), timestamp)
                    .toMinutes();

            tramo.setTiempoAtencionCita1((int) tiempoAtencionMinutos);

            System.out.println("✅ ORIGEN [4/6] - Salida Interna (fin atención)");
            System.out.println("   🅿️ Tiempo atención: " + tiempoAtencionMinutos + " min");
        }

        // ========================================================================
        // ORIGEN - GEOCERCA EXTERNA (SEGUNDA VEZ)
        // ========================================================================

        // 5️⃣ SEGUNDA ENTRADA a Externa (patio → acceso)
        if (geocercaExternaOrigenId != null &&
                geocercaId.equals(geocercaExternaOrigenId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraSalidaGeocercaInternaOrigen() != null &&
                tramo.getHoraEntradaGeocercaExternaOrigen2() == null) {

            tramo.setHoraEntradaGeocercaExternaOrigen2(timestamp);
            tramoActualizado = true;

            System.out.println("✅ ORIGEN [5/6] - Re-entrada Externa (hacia salida)");
        }

        // 6️⃣ SEGUNDA SALIDA de Externa (acceso → calle) ✅ AQUÍ SE MARCA SALIDA REAL
        if (geocercaExternaOrigenId != null &&
                geocercaId.equals(geocercaExternaOrigenId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraEntradaGeocercaExternaOrigen2() != null &&
                tramo.getHoraSalidaGeocercaExternaOrigen2() == null) {

            long segundos = java.time.Duration
                    .between(tramo.getHoraEntradaGeocercaExternaOrigen2(), timestamp)
                    .getSeconds();

            if (segundos >= 15) {
                tramo.setHoraSalidaGeocercaExternaOrigen2(timestamp);
                tramo.setHoraSalidaReal(timestamp); // ✅ CORRECTO: Salida FINAL de externa
                tramoActualizado = true;

                // CALCULAR PERMANENCIA TOTAL EN ORIGEN
                if (tramo.getHoraEntradaGeocercaExternaOrigen() != null) {
                    long permanenciaMinutos = java.time.Duration
                            .between(tramo.getHoraEntradaGeocercaExternaOrigen(), timestamp)
                            .toMinutes();

                    tramo.setTiempoPermanenciaCita1((int) permanenciaMinutos);
                }

                System.out.println("✅ ORIGEN [6/6] - Salida Final Externa (a la calle)");
                System.out.println("📊 RESUMEN ORIGEN COMPLETADO:");
                System.out.println("   1. Entrada Externa:     " + tramo.getHoraEntradaGeocercaExternaOrigen());
                System.out.println("   2. Salida Externa #1:   " + tramo.getHoraSalidaGeocercaExternaOrigen1());
                System.out.println("   3. Entrada Interna:     " + tramo.getHoraEntradaGeocercaInternaOrigen());
                System.out.println("   4. Salida Interna:      " + tramo.getHoraSalidaGeocercaInternaOrigen());
                System.out.println("   5. Re-entrada Externa:  " + tramo.getHoraEntradaGeocercaExternaOrigen2());
                System.out.println("   6. Salida Final:        " + timestamp);
                System.out.println("   ⏱️ Permanencia:        " + tramo.getTiempoPermanenciaCita1() + " min");
                System.out.println("   🅿️ Atención:           " + tramo.getTiempoAtencionCita1() + " min");
                System.out.println("   ✅ horaSalidaReal (legacy) = " + timestamp);
            } else {
                System.out.println("⚠️ Salida externa muy rápida (" + segundos + "s)");
            }
        }

        // ========================================================================
        // DESTINO - GEOCERCA EXTERNA
        // ========================================================================

        // 1️⃣ PRIMERA ENTRADA a Externa Destino
        if (geocercaExternaDestinoId != null &&
                geocercaId.equals(geocercaExternaDestinoId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraEntradaGeocercaExternaDestino() == null &&
                tramo.getHoraSalidaGeocercaExternaOrigen2() != null) {

            tramo.setHoraEntradaGeocercaExternaDestino(timestamp);
            tramo.setHoraLlegadaRealDestino(timestamp); // ✅ Compatibilidad legacy
            tramoActualizado = true;

            long minutosViaje = java.time.Duration
                    .between(tramo.getHoraSalidaGeocercaExternaOrigen2(), timestamp)
                    .toMinutes();

            System.out.println("✅ DESTINO [1/6] - Entrada Externa");
            System.out.println("   🚛 Tiempo de viaje: " + minutosViaje + " min");
        }

        // 2️⃣ PRIMERA SALIDA de Externa Destino
        if (geocercaExternaDestinoId != null &&
                geocercaId.equals(geocercaExternaDestinoId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraEntradaGeocercaExternaDestino() != null &&
                tramo.getHoraSalidaGeocercaExternaDestino1() == null) {

            tramo.setHoraSalidaGeocercaExternaDestino1(timestamp);
            tramoActualizado = true;

            System.out.println("✅ DESTINO [2/6] - Salida Externa #1 (hacia patio)");
        }

        // ========================================================================
        // DESTINO - GEOCERCA INTERNA
        // ========================================================================

        // 3️⃣ ENTRADA a Interna Destino
        if (geocercaInternaDestinoId != null &&
                geocercaId.equals(geocercaInternaDestinoId) &&
                ("ENTRY".equals(tipo) || "ENTRY_EXIT".equals(tipo)) &&
                tramo.getHoraEntradaGeocercaInternaDestino() == null) {

            tramo.setHoraEntradaGeocercaInternaDestino(timestamp);
            tramoActualizado = true;

            System.out.println("🅿️ DESTINO [3/6] - Entrada Interna (inicio atención)");
        }

        // 4️⃣ SALIDA de Interna Destino
        if (geocercaInternaDestinoId != null &&
                geocercaId.equals(geocercaInternaDestinoId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraEntradaGeocercaInternaDestino() != null &&
                tramo.getHoraSalidaGeocercaInternaDestino() == null) { // ✅ Evitar sobrescribir

            tramo.setHoraSalidaGeocercaInternaDestino(timestamp);
            // ❌ NO ASIGNAR horaSalidaRealDestino AQUÍ
            tramoActualizado = true;

            long tiempoAtencionMinutos = java.time.Duration
                    .between(tramo.getHoraEntradaGeocercaInternaDestino(), timestamp)
                    .toMinutes();

            tramo.setTiempoAtencionCita2((int) tiempoAtencionMinutos);

            System.out.println("✅ DESTINO [4/6] - Salida Interna (fin atención)");
            System.out.println("   🅿️ Tiempo atención: " + tiempoAtencionMinutos + " min");
        }

        // ========================================================================
        // DESTINO - GEOCERCA EXTERNA (SEGUNDA VEZ)
        // ========================================================================

        // 5️⃣ SEGUNDA ENTRADA a Externa Destino
        if (geocercaExternaDestinoId != null &&
                geocercaId.equals(geocercaExternaDestinoId) &&
                "ENTRY".equals(tipo) &&
                tramo.getHoraSalidaGeocercaInternaDestino() != null &&
                tramo.getHoraEntradaGeocercaExternaDestino2() == null) {

            tramo.setHoraEntradaGeocercaExternaDestino2(timestamp);
            tramoActualizado = true;

            System.out.println("✅ DESTINO [5/6] - Re-entrada Externa (hacia salida)");
        }

        // 6️⃣ SEGUNDA SALIDA de Externa Destino - TRAMO COMPLETADO
        if (geocercaExternaDestinoId != null &&
                geocercaId.equals(geocercaExternaDestinoId) &&
                "EXIT".equals(tipo) &&
                tramo.getHoraEntradaGeocercaExternaDestino2() != null &&
                tramo.getHoraSalidaGeocercaExternaDestino2() == null &&
                tramo.getEstado() != Tramo.EstadoTramo.completado) {

            long segundos = java.time.Duration
                    .between(tramo.getHoraEntradaGeocercaExternaDestino2(), timestamp)
                    .getSeconds();

            if (segundos >= 15) {
                tramo.setHoraSalidaGeocercaExternaDestino2(timestamp);
                tramo.setHoraSalidaRealDestino(timestamp); // ✅ CORRECTO: Salida FINAL
                tramoActualizado = true;

                // CALCULAR PERMANENCIA TOTAL EN DESTINO
                if (tramo.getHoraEntradaGeocercaExternaDestino() != null) {
                    long permanenciaMinutos = java.time.Duration
                            .between(tramo.getHoraEntradaGeocercaExternaDestino(), timestamp)
                            .toMinutes();

                    tramo.setTiempoPermanenciaCita2((int) permanenciaMinutos);
                }

                tramo.setEstado(Tramo.EstadoTramo.completado);

                System.out.println("🎉 TRAMO COMPLETADO");
                System.out.println("📊 RESUMEN DESTINO:");
                System.out.println("   1. Entrada Externa:     " + tramo.getHoraEntradaGeocercaExternaDestino());
                System.out.println("   2. Salida Externa #1:   " + tramo.getHoraSalidaGeocercaExternaDestino1());
                System.out.println("   3. Entrada Interna:     " + tramo.getHoraEntradaGeocercaInternaDestino());
                System.out.println("   4. Salida Interna:      " + tramo.getHoraSalidaGeocercaInternaDestino());
                System.out.println("   5. Re-entrada Externa:  " + tramo.getHoraEntradaGeocercaExternaDestino2());
                System.out.println("   6. Salida Final:        " + timestamp);
                System.out.println("   ⏱️ Permanencia:        " + tramo.getTiempoPermanenciaCita2() + " min");
                System.out.println("   🅿️ Atención:           " + tramo.getTiempoAtencionCita2() + " min");
                System.out.println("   ✅ horaSalidaRealDestino (legacy) = " + timestamp);

                activarSiguienteTramo(tramo);
                verificarCompletitudViaje(tramo.getViaje());
            } else {
                System.out.println("⚠️ Salida externa muy rápida (" + segundos + "s)");
            }
        }

        if (tramoActualizado) {
            tramoRepository.save(tramo);
            System.out.println("💾 Tramo actualizado - Estado: " + tramo.getEstado());
        } else {
            System.out.println("ℹ️ Alarma ignorada - No corresponde a transición válida");
        }
    }

    /**
     * 🆕 OBTIENE EL TRAMO ANTERIOR DEL VIAJE
     */
    private Tramo obtenerTramoAnterior(Tramo tramoActual) {
        Viaje viaje = tramoActual.getViaje();
        int ordenActual = tramoActual.getOrden();

        return viaje.getTramos().stream()
                .filter(t -> t.getOrden() == ordenActual - 1)
                .findFirst()
                .orElse(null);
    }

    private void activarSiguienteTramo(Tramo tramoCompletado) {
        try {
            String viajeId = tramoCompletado.getViaje().getId();
            Viaje viaje = viajeRepository.findByIdWithTramos(viajeId)
                    .orElseThrow(() -> new RuntimeException("Viaje no encontrado: " + viajeId));

            int ordenSiguiente = tramoCompletado.getOrden() + 1;
            List<Tramo> tramos = viaje.getTramos();
            tramos.sort(Comparator.comparingInt(Tramo::getOrden));

            Tramo siguienteTramo = tramos.stream()
                    .filter(t -> t.getOrden() == ordenSiguiente)
                    .findFirst()
                    .orElse(null);

            if (siguienteTramo != null) {
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("🔄 ACTIVANDO TRAMO #" + ordenSiguiente);

                // ✅ HERENCIA PARA GEOCERCAS ADYACENTES
                // El destino del T1 es el origen del T2

                // 1. Primera entrada a Externa del T2 = Primera entrada del Destino T1
                siguienteTramo.setHoraEntradaGeocercaExternaOrigen(
                        tramoCompletado.getHoraEntradaGeocercaExternaDestino());

                // 2. Primera salida de Externa del T2 = Primera salida del Destino T1
                siguienteTramo.setHoraSalidaGeocercaExternaOrigen1(
                        tramoCompletado.getHoraSalidaGeocercaExternaDestino1());

                // 3. Entrada Interna del T2 = Entrada Interna Destino T1
                siguienteTramo.setHoraEntradaGeocercaInternaOrigen(
                        tramoCompletado.getHoraEntradaGeocercaInternaDestino());

                // 4. Salida Interna del T2 = Salida Interna Destino T1
                siguienteTramo.setHoraSalidaGeocercaInternaOrigen(
                        tramoCompletado.getHoraSalidaGeocercaInternaDestino());

                // 5. Segunda entrada Externa del T2 = Segunda entrada Destino T1
                siguienteTramo.setHoraEntradaGeocercaExternaOrigen2(
                        tramoCompletado.getHoraEntradaGeocercaExternaDestino2());

                // 6. Segunda salida Externa del T2 = Segunda salida Destino T1
                siguienteTramo.setHoraSalidaGeocercaExternaOrigen2(
                        tramoCompletado.getHoraSalidaGeocercaExternaDestino2());

                // 7. Heredar métricas
                siguienteTramo.setTiempoAtencionCita1(tramoCompletado.getTiempoAtencionCita2());
                // 🆕 AGREGAR ESTAS LÍNEAS - HERENCIA DE CAMPOS LEGACY
                siguienteTramo.setHoraLlegadaReal(tramoCompletado.getHoraEntradaGeocercaExternaDestino());
                siguienteTramo.setHoraSalidaReal(tramoCompletado.getHoraSalidaGeocercaExternaDestino2());
                // 🆕 PRE-CALCULAR COORDENADAS DEL SIGUIENTE DESTINO
                System.out.println("📍 Pre-calculando coordenadas del siguiente destino...");
                Establecimiento siguienteDestino = siguienteTramo.getEstablecimientoDestino();
                if (siguienteDestino != null) {
                    CoordenadaDto coordsDestino = obtenerCoordenadasEstablecimiento(siguienteDestino.getId());
                    if (coordsDestino != null) {
                        System.out.println("   ✅ Destino #" + ordenSiguiente + ": ["
                                + coordsDestino.latitud + ", " + coordsDestino.longitud + "]");
                    } else {
                        System.out.println("   ⚠️ No se pudieron calcular coordenadas del destino");
                    }
                }
                // 8. Activar
                siguienteTramo.setEstado(Tramo.EstadoTramo.en_curso);

                tramoRepository.save(siguienteTramo);

                System.out.println("✅ Tramo #" + ordenSiguiente + " activado");
                System.out.println("   📍 Origen completado (heredado de Destino T" +
                        tramoCompletado.getOrden() + ")");
                System.out.println("   🚀 Esperando llegada al destino #" + ordenSiguiente);
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
        } catch (Exception e) {
            System.err.println("❌ Error activando siguiente tramo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🆕 VERIFICA SI TODOS LOS TRAMOS DEL VIAJE ESTÁN COMPLETADOS
     */
    private void verificarCompletitudViaje(Viaje viajeParam) {
        try {
            // 🔑 RECARGAR EL VIAJE CON TODOS SUS TRAMOS USANDO EL MÉTODO EXISTENTE
            Viaje viaje = viajeRepository.findByIdWithTramos(viajeParam.getId())
                    .orElseThrow(() -> new RuntimeException("Viaje no encontrado: " + viajeParam.getId()));

            List<Tramo> tramos = viaje.getTramos();

            if (tramos == null || tramos.isEmpty()) {
                System.out.println("⚠️ No hay tramos para verificar en el viaje: " + viaje.getId());
                return;
            }

            boolean todosCompletados = tramos.stream()
                    .allMatch(t -> t.getEstado() == Tramo.EstadoTramo.completado);

            if (todosCompletados) {
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("🎉🎉🎉 VIAJE COMPLETADO 🎉🎉🎉");
                System.out.println("   Viaje ID: " + viaje.getId());
                System.out.println("   Código: " + viaje.getCodigoViaje());
                System.out.println("   Total de tramos: " + tramos.size());
                System.out.println("   Todos los tramos completados exitosamente");

                // Calcular tiempo total del viaje
                LocalDateTime inicio = tramos.stream()
                        .filter(t -> t.getHoraLlegadaReal() != null)
                        .map(Tramo::getHoraLlegadaReal)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                LocalDateTime fin = tramos.stream()
                        .filter(t -> t.getHoraSalidaRealDestino() != null)
                        .map(Tramo::getHoraSalidaRealDestino)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                if (inicio != null && fin != null) {
                    long duracionMinutos = java.time.Duration.between(inicio, fin).toMinutes();
                    long horas = duracionMinutos / 60;
                    long minutos = duracionMinutos % 60;
                    System.out.println("   ⏱️ Duración total: " + horas + "h " + minutos + "min (" + duracionMinutos
                            + " minutos)");
                    System.out.println("   🕐 Inicio: " + inicio);
                    System.out.println("   🕐 Fin: " + fin);
                }

                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                long completados = tramos.stream()
                        .filter(t -> t.getEstado() == Tramo.EstadoTramo.completado)
                        .count();
                System.out
                        .println("ℹ️ Viaje en progreso: " + completados + "/" + tramos.size() + " tramos completados");
            }

        } catch (Exception e) {
            System.err.println("❌ Error verificando completitud del viaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void imprimirResumenTramo(Tramo tramo) {

        // ===================== RESUMEN ORIGINAL =====================
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

        // ===================== RESUMEN COMPLETO =====================
        System.out.println("📋 RESUMEN COMPLETO DEL TRAMO #" + tramo.getOrden());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        System.out.println("🔷 ORIGEN:");
        System.out.println("   Entrada Externa:     " + tramo.getHoraEntradaGeocercaExternaOrigen());
        System.out.println("   Salida Externa #1:   " + tramo.getHoraSalidaGeocercaExternaOrigen1());
        System.out.println("   Entrada Interna:     " + tramo.getHoraEntradaGeocercaInternaOrigen());
        System.out.println("   Salida Interna:      " + tramo.getHoraSalidaGeocercaInternaOrigen());
        System.out.println("   Entrada Externa #2:  " + tramo.getHoraEntradaGeocercaExternaOrigen2());
        System.out.println("   Salida Externa #2:   " + tramo.getHoraSalidaGeocercaExternaOrigen2());
        System.out.println("   ⏱️ Permanencia:      " + tramo.getTiempoPermanenciaCita1() + " min");
        System.out.println("   🅿️ Atención:         " + tramo.getTiempoAtencionCita1() + " min");
        System.out.println();

        System.out.println("🔶 DESTINO:");
        System.out.println("   Entrada Externa:     " + tramo.getHoraEntradaGeocercaExternaDestino());
        System.out.println("   Salida Externa #1:   " + tramo.getHoraSalidaGeocercaExternaDestino1());
        System.out.println("   Entrada Interna:     " + tramo.getHoraEntradaGeocercaInternaDestino());
        System.out.println("   Salida Interna:      " + tramo.getHoraSalidaGeocercaInternaDestino());
        System.out.println("   Entrada Externa #2:  " + tramo.getHoraEntradaGeocercaExternaDestino2());
        System.out.println("   Salida Externa #2:   " + tramo.getHoraSalidaGeocercaExternaDestino2());
        System.out.println("   ⏱️ Permanencia:      " + tramo.getTiempoPermanenciaCita2() + " min");
        System.out.println("   🅿️ Atención:         " + tramo.getTiempoAtencionCita2() + " min");

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

    // @Override
    // public List<TramoDto> listarTramosPorViaje(String viajeId) {
    // return tramoRepository.findByViajeId(viajeId).stream()
    // .map(this::convertToDto)
    // .collect(Collectors.toList());
    // }
    @Override
    public List<TramoDto> listarTramosPorViaje(String viajeId) {
        return tramoRepository.findByViajeId(viajeId).stream()
                .map(tramo -> {
                    TramoDto dto = this.convertToDto(tramo);

                    // 🚦 EL FILTRO INTELIGENTE
                    if (tramo.getEstado() == Tramo.EstadoTramo.en_curso) {
                        // Solo aquí entramos a la lógica pesada (OSRM, GPS, logs extensos)
                        this.enriquecerConEtaYAvance(dto, tramo);
                    } else if (tramo.getEstado() == Tramo.EstadoTramo.completado) {
                        dto.setEta("--:--");
                        dto.setAvance(100.0);
                    } else {
                        dto.setAvance(0.0);
                        // El ETA programado ya lo puso convertToDto
                    }

                    return dto;
                })
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

    /**
     * ✅ MÉTODO COMPLETO: Mapea TODOS los campos de geocercas adyacentes
     */
    @Override
    public TramoDto convertToDto(Tramo tramo) {
        if (tramo == null)
            return null;

        TramoDto dto = new TramoDto();

        // ========================================================================
        // IDENTIFICACIÓN Y RELACIONES
        // ========================================================================
        dto.setId(tramo.getId());

        if (tramo.getViaje() != null) {
            dto.setViajeId(tramo.getViaje().getId());
        }

        // ========================================================================
        // ESTABLECIMIENTOS (CON INFORMACIÓN COMPLETA)
        // ========================================================================
        if (tramo.getEstablecimientoOrigen() != null) {
            dto.setEstablecimientoOrigenId(tramo.getEstablecimientoOrigen().getId());

            EstablecimientoDto origenDto = new EstablecimientoDto();
            origenDto.setId(tramo.getEstablecimientoOrigen().getId());
            origenDto.setNombre(tramo.getEstablecimientoOrigen().getNombre());
            dto.setEstablecimientoOrigen(origenDto);
        }

        if (tramo.getEstablecimientoDestino() != null) {
            dto.setEstablecimientoDestinoId(tramo.getEstablecimientoDestino().getId());

            EstablecimientoDto destinoDto = new EstablecimientoDto();
            destinoDto.setId(tramo.getEstablecimientoDestino().getId());
            destinoDto.setNombre(tramo.getEstablecimientoDestino().getNombre());
            dto.setEstablecimientoDestino(destinoDto);
        }

        // ========================================================================
        // INFORMACIÓN DEL TRAMO
        // ========================================================================
        if (tramo.getTipoActividad() != null) {
            dto.setTipoActividad(tramo.getTipoActividad().name());
        }

        if (tramo.getEstado() != null) {
            dto.setEstado(tramo.getEstado().name());
        }

        dto.setOrden(tramo.getOrden());
        dto.setDescripcion(tramo.getDescripcion());
        dto.setSlaMinutos(tramo.getSlaMinutos());
        dto.setObservaciones(tramo.getObservaciones());

        // ========================================================================
        // HORARIOS PROGRAMADOS
        // ========================================================================
        dto.setHoraLlegadaProgramada(tramo.getHoraLlegadaProgramada());
        dto.setHoraSalidaProgramada(tramo.getHoraSalidaProgramada());

        // ========================================================================
        // HORARIOS REALES LEGACY (Compatibilidad)
        // ========================================================================
        dto.setHoraLlegadaReal(tramo.getHoraLlegadaReal());
        dto.setHoraSalidaReal(tramo.getHoraSalidaReal());
        dto.setHoraLlegadaRealDestino(tramo.getHoraLlegadaRealDestino());
        dto.setHoraSalidaRealDestino(tramo.getHoraSalidaRealDestino());

        // ========================================================================
        // ✅ GEOCERCAS ADYACENTES - ORIGEN (12 CAMPOS NUEVOS)
        // ========================================================================
        dto.setHoraEntradaGeocercaExternaOrigen(tramo.getHoraEntradaGeocercaExternaOrigen());
        dto.setHoraSalidaGeocercaExternaOrigen1(tramo.getHoraSalidaGeocercaExternaOrigen1());
        dto.setHoraEntradaGeocercaInternaOrigen(tramo.getHoraEntradaGeocercaInternaOrigen());
        dto.setHoraSalidaGeocercaInternaOrigen(tramo.getHoraSalidaGeocercaInternaOrigen());
        dto.setHoraEntradaGeocercaExternaOrigen2(tramo.getHoraEntradaGeocercaExternaOrigen2());
        dto.setHoraSalidaGeocercaExternaOrigen2(tramo.getHoraSalidaGeocercaExternaOrigen2());

        // ========================================================================
        // ✅ GEOCERCAS ADYACENTES - DESTINO
        // ========================================================================
        dto.setHoraEntradaGeocercaExternaDestino(tramo.getHoraEntradaGeocercaExternaDestino());
        dto.setHoraSalidaGeocercaExternaDestino1(tramo.getHoraSalidaGeocercaExternaDestino1());
        dto.setHoraEntradaGeocercaInternaDestino(tramo.getHoraEntradaGeocercaInternaDestino());
        dto.setHoraSalidaGeocercaInternaDestino(tramo.getHoraSalidaGeocercaInternaDestino());
        dto.setHoraEntradaGeocercaExternaDestino2(tramo.getHoraEntradaGeocercaExternaDestino2());
        dto.setHoraSalidaGeocercaExternaDestino2(tramo.getHoraSalidaGeocercaExternaDestino2());

        // ========================================================================
        // MÉTRICAS
        // ========================================================================
        dto.setTardanzaCita1(tramo.getTardanzaCita1());
        dto.setTiempoPermanenciaCita1(tramo.getTiempoPermanenciaCita1());
        dto.setTiempoAtencionCita1(tramo.getTiempoAtencionCita1());
        dto.setTardanzaCita2(tramo.getTardanzaCita2());
        dto.setTiempoPermanenciaCita2(tramo.getTiempoPermanenciaCita2());
        dto.setTiempoAtencionCita2(tramo.getTiempoAtencionCita2());

        // ========================================================================
        // RECURSOS ASIGNADOS
        // ========================================================================
        dto.setTracto(tramo.getTracto());
        dto.setChasis(tramo.getChasis());
        dto.setConductor(tramo.getConductor());

        // ========================================================================
        // CALCULAR ETA PROGRAMADO (Para comparación posterior)
        // ========================================================================
        if (tramo.getHoraLlegadaProgramada() != null) {
            ZonedDateTime horaProg = tramo.getHoraLlegadaProgramada().atZone(ZONA_PERU);
            dto.setEtaProgramado(horaProg.format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        // ========================================================================
        // CALCULAR DEMORA EN SALIDA DEL ORIGEN
        // ========================================================================
        if (tramo.getHoraSalidaProgramada() != null && tramo.getHoraSalidaReal() != null) {
            long demoraSalida = Duration.between(
                    tramo.getHoraSalidaProgramada(),
                    tramo.getHoraSalidaReal()).toMinutes();

            if (demoraSalida > 0) {
                dto.setDemoraSalida((int) demoraSalida);
                System.out.println("🕐 Demora en salida detectada: " + demoraSalida + " min");
            }
        }

        // ========================================================================
        // OPCIONAL: Crear objeto de demoras detallado
        // ========================================================================
        if (tramo.getHoraSalidaReal() != null && tramo.getHoraLlegadaRealDestino() != null) {
            TramoDto.DemorasDto demoras = new TramoDto.DemorasDto();

            if (dto.getDemoraSalida() != null) {
                demoras.setDemoraSalidaOrigen(dto.getDemoraSalida());
            }

            long tiempoRealTransito = Duration.between(
                    tramo.getHoraSalidaReal(),
                    tramo.getHoraLlegadaRealDestino()).toMinutes();
            demoras.setTiempoRealTransito((int) tiempoRealTransito);

            dto.setDemoras(demoras);
        }

        // NOTA: El ETA actual, avance y semáforo se calculan después
        // en el método enriquecerConEtaYAvance() solo para tramos EN_CURSO

        return dto;
    }

    /**
     * 🆕 NUEVO MÉTODO: Enriquecer DTOs con ETA/Avance de forma segura
     * Se ejecuta FUERA de la transacción principal
     */
    @Override
    public void enriquecerConEtaYAvance(TramoDto dto, Tramo tramo) {
        try {
            calcularEtaYAvance(tramo, dto);
        } catch (Exception e) {
            // Si falla el cálculo, el DTO mantiene valores por defecto
            System.err.println("⚠️ Error calculando ETA para tramo " + dto.getId() + ": " + e.getMessage());
            dto.setEta(null);
            dto.setAvance(0.0);
        }
    }

    @Transactional
    public CoordenadaDto obtenerCoordenadasEstablecimiento(String establecimientoId) {
        try {
            Establecimiento est = establecimientoRepository
                    .findById(establecimientoId)
                    .orElse(null);

            if (est == null) {
                System.out.println("❌ Establecimiento no encontrado: " + establecimientoId);
                return null;
            }

            // CASO 1: Ya tiene centroide calculado
            if (Boolean.TRUE.equals(est.getCentroideCalculado()) &&
                    est.getLatitud() != null && est.getLongitud() != null) {

                System.out.println("✅ Coordenadas persistidas: " + est.getNombre());
                System.out.println("   📍 [" + est.getLatitud() + ", " + est.getLongitud() + "]");

                return new CoordenadaDto(
                        est.getLatitud().doubleValue(),
                        est.getLongitud().doubleValue());
            }

            // CASO 2: Tiene coordenadas manuales
            if (est.getLatitud() != null && est.getLongitud() != null) {
                System.out.println("ℹ️ Coordenadas manuales: " + est.getNombre());
                return new CoordenadaDto(
                        est.getLatitud().doubleValue(),
                        est.getLongitud().doubleValue());
            }

            // CASO 3: Calcular centroide de geocerca
            System.out.println("🔄 Calculando centroide: " + est.getNombre());

            CoordenadaDto coords = calcularCentroideGeocerca(establecimientoId, "EXTERNA");

            if (coords == null) {
                coords = calcularCentroideGeocerca(establecimientoId, "INTERNA");
            }

            // GUARDAR en BD
            if (coords != null) {
                est.setLatitud(BigDecimal.valueOf(coords.latitud));
                est.setLongitud(BigDecimal.valueOf(coords.longitud));
                est.setCentroideCalculado(true);
                est.setFechaCalculoCentroide(LocalDateTime.now());

                establecimientoRepository.save(est);
                System.out.println("💾 [DB DEBUG] ID: " + est.getId() + " - Lat persistida: " + est.getLatitud());
                System.out.println("💾 Centroide guardado:");
                System.out.println("   📍 [" + coords.latitud + ", " + coords.longitud + "]");

                return coords;
            }

            System.out.println("❌ No se pudieron calcular coordenadas");
            return null;

        } catch (Exception e) {
            System.err.println("❌ ERROR AL GUARDAR EN BD: " + e.getMessage());
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ========================================================================
    // MÉTODO: Calcular centroide de geocerca
    // ========================================================================

    private CoordenadaDto calcularCentroideGeocerca(String establecimientoId, String tipoGeocerca) {
        try {
            Long geocercaId = "EXTERNA".equals(tipoGeocerca)
                    ? geocercaPorEstablecimientoRepo.findGeocercaExternaId(establecimientoId).orElse(null)
                    : geocercaPorEstablecimientoRepo.findGeocercaInternaId(establecimientoId).orElse(null);

            if (geocercaId == null)
                return null;

            Geofence geofence = geofenceRepository.findById(geocercaId).orElse(null);
            if (geofence == null || geofence.getPoints() == null)
                return null;

            JSONArray puntosArray = new JSONArray(geofence.getPoints().trim());
            if (puntosArray.length() < 3)
                return null;

            double sumaLat = 0.0, sumaLng = 0.0;
            int puntosValidos = 0;

            for (int i = 0; i < puntosArray.length(); i++) {
                JSONArray coord = puntosArray.getJSONArray(i);
                if (coord.length() == 2) {
                    sumaLat += coord.getDouble(0);
                    sumaLng += coord.getDouble(1);
                    puntosValidos++;
                }
            }

            if (puntosValidos == 0)
                return null;

            return new CoordenadaDto(sumaLat / puntosValidos, sumaLng / puntosValidos);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el track más cercano al momento de salida del origen
     */
    private Track obtenerTrackCercanoASalida(Tramo tramo, String imei) {
        if (tramo.getHoraSalidaReal() == null) {
            System.out.println("      ⚠️ No hay hora de salida real registrada");
            return null;
        }

        try {
            long timestampSalida = tramo.getHoraSalidaReal()
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();

            // Buscar tracks en ventana de ±10 minutos de la salida
            long ventanaInicio = timestampSalida - 600; // -10 min
            long ventanaFin = timestampSalida + 600; // +10 min

            System.out.println("      🔍 Buscando tracks entre " +
                    LocalDateTime.ofEpochSecond(ventanaInicio, 0, ZoneOffset.UTC).atZone(ZONA_PERU)
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    +
                    " y " +
                    LocalDateTime.ofEpochSecond(ventanaFin, 0, ZoneOffset.UTC).atZone(ZONA_PERU)
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            List<Track> tracksVentana = trackRepository.findTracksByImeiInTimeRange(
                    imei, ventanaInicio, ventanaFin);

            if (tracksVentana.isEmpty()) {
                System.out.println("      ⚠️ No hay tracks en la ventana de tiempo");
                return null;
            }

            System.out.println("      ✅ Encontrados " + tracksVentana.size() + " tracks en ventana");

            // Ordenar por cercanía al timestamp de salida y tomar el más cercano
            Track trackMasCercano = tracksVentana.stream()
                    .min((t1, t2) -> {
                        long diff1 = Math.abs(t1.getGpstime() - timestampSalida);
                        long diff2 = Math.abs(t2.getGpstime() - timestampSalida);
                        return Long.compare(diff1, diff2);
                    })
                    .orElse(null);

            if (trackMasCercano != null) {
                long diffMinutos = Math.abs(trackMasCercano.getGpstime() - timestampSalida) / 60;
                System.out.println("      ✅ Track más cercano: ID " + trackMasCercano.getId() +
                        " (diferencia: " + diffMinutos + " min)");
                System.out.println("      📍 Posición: [" + trackMasCercano.getLatitude() + ", "
                        + trackMasCercano.getLongitude() + "]");
            }

            return trackMasCercano;

        } catch (Exception e) {
            System.err.println("      ❌ Error buscando track de salida: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ========================================================================
    // MÉTODO MEJORADO: Calcular ETA con detección de herencia
    // ========================================================================
    private void calcularEtaYAvance(Tramo tramo, TramoDto dto) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🚀 CÁLCULO ETA Y AVANCE - Tramo ID: " + tramo.getId());
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        try {
            Establecimiento origen = tramo.getEstablecimientoOrigen();
            Establecimiento destino = tramo.getEstablecimientoDestino();

            if (origen == null || destino == null) {
                System.out.println("❌ Origen o destino no disponibles");
                dto.setEta(null);
                dto.setAvance(0.0);
                dto.setSemaforo("GRIS");
                return;
            }

            System.out.println("📍 Ruta: " + origen.getNombre() + " → " + destino.getNombre());

            // CASO 1: COMPLETADO
            if (tramo.getEstado() == Tramo.EstadoTramo.completado) {
                System.out.println("✅ Estado: COMPLETADO");
                dto.setEta("--:--");
                dto.setAvance(100.0);
                dto.setSemaforo("VERDE");
                return;
            }

            // CASO 2: PENDIENTE
            if (tramo.getEstado() == Tramo.EstadoTramo.pendiente) {
                System.out.println("⏳ Estado: PENDIENTE");
                dto.setSemaforo("GRIS");
                if (tramo.getHoraLlegadaProgramada() != null) {
                    ZonedDateTime horaProg = tramo.getHoraLlegadaProgramada().atZone(ZONA_PERU);
                    dto.setEta(horaProg.format(DateTimeFormatter.ofPattern("HH:mm")) + " (prog)");
                    System.out.println("   ETA programado: " + dto.getEta());
                } else {
                    dto.setEta("Pendiente");
                }
                dto.setAvance(0.0);
                return;
            }

            // CASO 3: EN CURSO
            if (tramo.getEstado() == Tramo.EstadoTramo.en_curso) {
                System.out.println("🚗 Estado: EN CURSO");

                // 🆕 DETECTAR SI ES TRAMO CON HERENCIA
                boolean esTramoHeredado = (tramo.getOrden() > 1 &&
                        tramo.getHoraSalidaReal() != null &&
                        tramo.getHoraLlegadaRealDestino() == null);

                if (esTramoHeredado) {
                    System.out.println("🔄 TRAMO HEREDADO DETECTADO - Usando GPS actual como origen");
                }

                // Sub-caso: Vehículo todavía en origen (no ha salido)
                if (tramo.getHoraSalidaReal() == null) {
                    System.out.println("   📍 Vehículo en origen (esperando salida)");
                    dto.setEta("En origen");
                    dto.setAvance(5.0);
                    dto.setSemaforo("AMARILLO");
                    return;
                }

                // Sub-caso: Ya llegó al destino
                if (tramo.getHoraLlegadaRealDestino() != null) {
                    System.out.println("   📍 VEHÍCULO LLEGÓ AL DESTINO");
                    dto.setEta("En destino");
                    dto.setAvance(100.0);
                    dto.setSemaforo("VERDE");
                    return;
                }

                // Validar vehículo
                if (tramo.getViaje() == null || tramo.getViaje().getVehiculo() == null) {
                    System.out.println("❌ No hay vehículo asociado");
                    dto.setEta(null);
                    dto.setAvance(0.0);
                    dto.setSemaforo("GRIS");
                    return;
                }

                String imei = tramo.getViaje().getVehiculo().getImei();
                Track track = obtenerTrackRelevante(tramo, imei);

                if (track == null) {
                    System.out.println("⚠️ Sin señal GPS válida");
                    dto.setEta("Sin señal");
                    dto.setSemaforo("GRIS");
                    return;
                }

                System.out.println("   📍 GPS actual: [" + track.getLatitude() + ", " + track.getLongitude() + "]");

                CoordenadaDto coordsDestino = obtenerCoordenadasEstablecimiento(destino.getId());

                if (coordsDestino == null) {
                    System.out.println("❌ No se pudieron obtener coordenadas del destino");
                    dto.setEta("Calculando...");
                    dto.setSemaforo("GRIS");
                    return;
                }

                // 🆕 DECISIÓN INTELIGENTE: Adaptar según el contexto del tramo
                CoordenadaDto coordsOrigenParaRutaTotal;
                DuracionDistanciaResult rutaTotal;

                if (esTramoHeredado) {
                    // TRAMOS HEREDADOS: Ya completaron el origen, usar GPS actual como base
                    coordsOrigenParaRutaTotal = new CoordenadaDto(track.getLatitude(), track.getLongitude());
                    System.out.println("   📍 Origen (heredado): GPS actual [" + coordsOrigenParaRutaTotal.latitud
                            + ", " + coordsOrigenParaRutaTotal.longitud + "]");

                    rutaTotal = obtenerDuracionYDistanciaOSRM(
                            coordsOrigenParaRutaTotal.latitud, coordsOrigenParaRutaTotal.longitud,
                            coordsDestino.latitud, coordsDestino.longitud);

                } else {
                    // TRAMOS NORMALES: Intentar obtener posición REAL de salida
                    System.out.println("   🔍 Buscando track de salida real...");

                    // Primero, intentar obtener el primer track después de la salida
                    Track trackSalida = obtenerTrackCercanoASalida(tramo, imei);

                    if (trackSalida != null && trackSalida.getId() != track.getId()) {
                        // Tenemos un track de cuando salió, usarlo como origen
                        coordsOrigenParaRutaTotal = new CoordenadaDto(trackSalida.getLatitude(),
                                trackSalida.getLongitude());
                        System.out.println("   ✅ Origen: Track al salir [" + coordsOrigenParaRutaTotal.latitud + ", "
                                + coordsOrigenParaRutaTotal.longitud + "]");
                    } else {
                        // Fallback: usar establecimiento
                        coordsOrigenParaRutaTotal = obtenerCoordenadasEstablecimiento(origen.getId());

                        if (coordsOrigenParaRutaTotal == null) {
                            System.out.println("❌ No se pudieron obtener coordenadas del origen");
                            dto.setEta("Calculando...");
                            dto.setSemaforo("GRIS");
                            return;
                        }

                        System.out.println("   📍 Origen: Establecimiento (fallback) ["
                                + coordsOrigenParaRutaTotal.latitud + ", " + coordsOrigenParaRutaTotal.longitud + "]");
                    }

                    rutaTotal = obtenerDuracionYDistanciaOSRM(
                            coordsOrigenParaRutaTotal.latitud, coordsOrigenParaRutaTotal.longitud,
                            coordsDestino.latitud, coordsDestino.longitud);
                }

                if (rutaTotal == null) {
                    System.out.println("❌ No se pudo calcular ruta total");
                    dto.setEta("Error ruta");
                    dto.setSemaforo("GRIS");
                    return;
                }

                System.out.println(
                        "   📏 Ruta total calculada: " + String.format("%.2f km", rutaTotal.distanciaMetros / 1000));

                // Calcular ruta restante (siempre desde GPS actual)
                System.out.println("   🔍 Calculando ruta restante desde GPS actual...");
                DuracionDistanciaResult rutaRestante = obtenerDuracionYDistanciaOSRM(
                        track.getLatitude(), track.getLongitude(),
                        coordsDestino.latitud, coordsDestino.longitud);

                if (rutaRestante == null) {
                    System.out.println("❌ Error en respuesta OSRM para ruta restante");
                    dto.setEta("Error ruta");
                    dto.setSemaforo("GRIS");
                    return;
                }

                System.out.println(
                        "   📏 Ruta restante: " + String.format("%.2f km", rutaRestante.distanciaMetros / 1000));

                // Calcular ETA
                ZonedDateTime ahoraPeru = ZonedDateTime.now(ZONA_PERU);
                ZonedDateTime etaActual = ahoraPeru.plusMinutes((long) Math.ceil(rutaRestante.duracionMinutos));

                // Calcular ETA programado y demoras
                LocalDateTime horaSalidaReal = tramo.getHoraSalidaReal();
                ZonedDateTime etaProgramado = null;
                String demoraMensaje = "";

                if (horaSalidaReal != null && rutaTotal != null) {
                    etaProgramado = horaSalidaReal
                            .atZone(ZONA_PERU)
                            .plusMinutes((long) Math.ceil(rutaTotal.duracionMinutos));

                    dto.setEtaProgramado(etaProgramado.format(DateTimeFormatter.ofPattern("HH:mm")));

                    if (tramo.getHoraSalidaProgramada() != null) {
                        long minutosDemoraSalida = Duration.between(
                                tramo.getHoraSalidaProgramada(),
                                horaSalidaReal).toMinutes();

                        ZonedDateTime llegadaProgramadaDestino = tramo.getHoraSalidaProgramada().atZone(ZONA_PERU);
                        long minutosRetrasoTotal = Duration.between(
                                llegadaProgramadaDestino,
                                etaActual).toMinutes();

                        if (minutosRetrasoTotal > 5) {
                            demoraMensaje = String.format(" (+%d min)", minutosRetrasoTotal);
                            System.out.println("⚠️ DEMORA DETECTADA:");
                            System.out.println("   Llegada programada destino: " + tramo.getHoraSalidaProgramada());
                            System.out.println("   Salida real origen: " + horaSalidaReal);
                            System.out.println("   Demora en salida: " + minutosDemoraSalida + " min");
                            System.out.println("   Retraso total vs plan: " + minutosRetrasoTotal + " min");
                        }
                    }
                }

                String etaStr = etaActual.format(DateTimeFormatter.ofPattern("HH:mm")) + demoraMensaje;

                // Calcular semáforo
                if (tramo.getHoraLlegadaProgramada() != null) {
                    ZonedDateTime horaProg = tramo.getHoraLlegadaProgramada().atZone(ZONA_PERU);
                    long minutosDif = Duration.between(horaProg, etaActual).toMinutes();

                    if (minutosDif <= 5)
                        dto.setSemaforo("VERDE");
                    else if (minutosDif <= 20)
                        dto.setSemaforo("AMARILLO");
                    else
                        dto.setSemaforo("ROJO");

                    System.out.println("🚦 Semáforo: " + dto.getSemaforo() + " (Diferencia: " + minutosDif + " min)");
                } else {
                    dto.setSemaforo("GRIS");
                }

                // 🆕 CALCULAR AVANCE (ahora con protección contra distancias negativas)
                double avance = calcularAvanceInteligente(
                        rutaTotal.distanciaMetros,
                        rutaRestante.distanciaMetros,
                        tramo,
                        esTramoHeredado);

                dto.setEta(etaStr);
                dto.setAvance(avance);

                System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
                System.out.println("║  📊 RESULTADO FINAL");
                System.out.println("╠════════════════════════════════════════════════════════════════╣");
                System.out.println("║  ⏰ ETA Actual: " + etaStr);
                if (etaProgramado != null) {
                    System.out.println(
                            "║  📅 ETA Programado: " + etaProgramado.format(DateTimeFormatter.ofPattern("HH:mm")));
                }
                System.out.println("║  📈 Avance: " + avance + "%");
                System.out.println("║  🚦 Semáforo: " + dto.getSemaforo());
                System.out.println(
                        "║  📏 Distancia restante: " + String.format("%.2f km", rutaRestante.distanciaMetros / 1000));
                System.out.println("║  ⏱️ Tiempo restante: " + Math.round(rutaRestante.duracionMinutos) + " min");
                System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
            }

        } catch (Exception e) {
            System.err.println("\n❌ ERROR FATAL en calcularEtaYAvance: " + e.getMessage());
            e.printStackTrace();
            dto.setEta("Error");
            dto.setSemaforo("GRIS");
        }
    }

    // ========================================================================
    // MÉTODO MEJORADO: Calcular avance con contexto de herencia
    // ========================================================================
    private double calcularAvanceInteligente(
            double distTotal,
            double distRestante,
            Tramo tramo,
            boolean esTramoHeredado) {

        System.out.println("   📊 Cálculo de avance:");
        System.out.println("      Total: " + String.format("%.2f km", distTotal / 1000));
        System.out.println("      Restante: " + String.format("%.2f km", distRestante / 1000));

        // CASO 1: Distancia restante mayor que total (ruta diferente o error de GPS)
        if (distRestante > distTotal * 1.1) { // 10% de tolerancia
            System.out.println("      ⚠️ Ruta actual difiere de la planificada (+"
                    + String.format("%.2f km", (distRestante - distTotal) / 1000) + ")");

            // Usar tiempo como indicador de avance
            if (tramo.getHoraSalidaReal() != null) {
                LocalDateTime ahora = LocalDateTime.now();
                long minutosTranscurridos = Duration.between(tramo.getHoraSalidaReal(), ahora).toMinutes();

                // Estimar duración total: distancia / velocidad promedio 50 km/h
                double duracionEstimadaMinutos = (distTotal / 1000.0) / 50.0 * 60.0;

                System.out.println("      🕐 Calculando avance por tiempo transcurrido...");
                System.out.println("      ⏱️ Transcurrido: " + minutosTranscurridos + " min");
                System.out.println("      ⏱️ Estimado total: ~" + Math.round(duracionEstimadaMinutos) + " min");

                if (duracionEstimadaMinutos > 0) {
                    double avanceTemporal = (minutosTranscurridos / duracionEstimadaMinutos) * 100.0;
                    avanceTemporal = Math.max(10.0, Math.min(95.0, avanceTemporal));

                    System.out.println("      ✅ Avance temporal calculado: " + String.format("%.1f%%", avanceTemporal));

                    return Math.round(avanceTemporal * 10.0) / 10.0;
                }
            }

            System.out.println("      🔧 Asignando avance mínimo: 10%");
            return 10.0;
        }

        // CASO 2: Cálculo normal por distancia
        double distRecorrida = distTotal - distRestante;
        System.out.println("      Recorrida: " + String.format("%.2f km", distRecorrida / 1000));

        if (esTramoHeredado) {
            System.out.println("      🔄 MODO HEREDADO");
        }

        // Protección contra negativos
        if (distRecorrida < 0) {
            System.out.println("      ⚠️ Distancia recorrida negativa - ajustando a 0");
            distRecorrida = 0;
        }

        if (distTotal > 0) {
            double avance = (distRecorrida / distTotal) * 100.0;

            System.out.println("      📐 Avance bruto: " + String.format("%.2f%%", avance));

            // Ajustes de límites
            if (tramo.getHoraSalidaReal() != null && avance < 10.0) {
                System.out.println("      🔧 Ajuste: Mínimo 10% (vehículo ya salió)");
                avance = 10.0;
            }

            if (tramo.getHoraLlegadaRealDestino() == null && avance > 95.0) {
                System.out.println("      🔧 Ajuste: Máximo 95% (no ha llegado al destino)");
                avance = 95.0;
            }

            avance = Math.max(0.0, Math.min(100.0, avance));
            avance = Math.round(avance * 10.0) / 10.0;

            System.out.println("      ✅ Avance final: " + avance + "%");
            return avance;
        }

        System.out.println("      ❌ Distancia total = 0, retornando 0%");
        return 0.0;
    }
    // ========================================================================
    // MÉTODO: Obtener track relevante para el tramo
    // ========================================================================

    private Track obtenerTrackRelevante(Tramo tramo, String imei) {
        try {
            // 1. Usamos Instant para manejar el tiempo absoluto (Epoch Seconds)
            Instant ahora = Instant.now();

            // Definimos el fin de la ventana (ahora + 1 minuto de margen por desfases)
            long finEpoch = ahora.getEpochSecond() + 60;
            long inicioEpoch;

            // 2. Determinar inicio de ventana convirtiendo LocalDateTime a Epoch Seconds
            // Usamos ZoneId.systemDefault() asumiendo que las fechas en 'tramo' están en
            // hora local
            if (tramo.getHoraSalidaReal() != null) {
                inicioEpoch = tramo.getHoraSalidaReal().atZone(ZoneId.systemDefault()).toEpochSecond();
            } else if (tramo.getHoraLlegadaReal() != null) {
                inicioEpoch = tramo.getHoraLlegadaReal().atZone(ZONA_PERU).minusMinutes(30).toEpochSecond();
            } else if (tramo.getViaje().getFechaInicioProgramada() != null) {
                // Si no ha salido, buscamos desde 2 horas antes de la programación
                inicioEpoch = tramo.getViaje().getFechaInicioProgramada()
                        .atZone(ZoneId.systemDefault())
                        .minusHours(2)
                        .toEpochSecond();
            } else {
                // Default: últimas 24 horas
                inicioEpoch = finEpoch - (24 * 3600);
            }

            // 3. Buscamos en el repositorio usando los valores LONG
            // Esto evita el error de "unix_timestamp" en PostgreSQL
            Track track = trackRepository.findLatestTrackByImeiInTimeRange(imei, inicioEpoch, finEpoch);

            if (track == null) {
                System.out.println(" ⚠️ No se encontró track para IMEI " + imei + " entre Epoch: " + inicioEpoch + " y "
                        + finEpoch);
                return null;
            }

            // 4. Validar antigüedad comparando Epoch vs Epoch
            long segundosDeAntiguedad = ahora.getEpochSecond() - track.getGpstime();
            long minutosDesdeTrack = segundosDeAntiguedad / 60;

            if (minutosDesdeTrack > 60) {
                System.out.println("⚠️ Track detectado es antiguo (" + minutosDesdeTrack + " min)");
            } else {
                System.out.println(" ✅ Track válido encontrado (hace " + minutosDesdeTrack + " min)");
            }

            return track;

        } catch (Exception e) {
            System.err.println(" ❌ Error crítico en obtenerTrackRelevante: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    // ========================================================================
    // MÉTODO: Llamar a OSRM
    // ========================================================================

    private DuracionDistanciaResult obtenerDuracionYDistanciaOSRM(
            double origenLat, double origenLon,
            double destinoLat, double destinoLon) {
        try {
            String url = String.format(Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                    origenLon, origenLat, destinoLon, destinoLat);

            System.out.println("   🌐 Consultando OSRM...");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("   📡 Status: " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("   ❌ OSRM error: código " + response.statusCode());
                return null;
            }

            JSONObject json = new JSONObject(response.body());

            if (!json.has("routes") || json.getJSONArray("routes").length() == 0) {
                System.err.println("   ❌ OSRM: No se encontró ruta");
                return null;
            }

            JSONObject route = json.getJSONArray("routes").getJSONObject(0);
            double duracionSeg = route.getDouble("duration");
            double distanciaM = route.getDouble("distance");
            double duracionMin = duracionSeg / 60.0;

            System.out.println("   ✅ Distancia: " + String.format("%.2f km", distanciaM / 1000) +
                    " | Duración: " + String.format("%.1f min", duracionMin));

            return new DuracionDistanciaResult(duracionMin, distanciaM);

        } catch (java.net.http.HttpTimeoutException e) {
            System.err.println("   ⏱️ Timeout OSRM");
            return null;
        } catch (IOException e) {
            System.err.println("   ❌ Error de red OSRM: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("   ❌ Petición interrumpida");
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            System.err.println("   ❌ Error OSRM: " + e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // CLASES AUXILIARES
    // ========================================================================

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

    public List<ParadaDetectadaDto> analizarParadasDelTramo(String tramoId, List<Track> tracks) {
        try {
            Tramo tramo = tramoRepository.findById(tramoId)
                    .orElseThrow(() -> new RuntimeException("Tramo no encontrado"));

            if (tracks == null || tracks.isEmpty()) {
                System.out.println("⚠️ No hay datos GPS para analizar paradas");
                return Collections.emptyList();
            }

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🔍 ANALIZANDO PARADAS DEL TRAMO #" + tramo.getOrden());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 1. Filtrar tracks del período del tramo
            List<Track> tracksFiltrados = filtrarTracksPorPeriodo(tracks, tramo);
            System.out.println("📊 Tracks a analizar: " + tracksFiltrados.size());

            // 2. Detectar paradas (Aquí se crean los objetos ParadaDetectadaDto)
            List<ParadaDetectadaDto> paradas = detectorMejorado.detectarParadasConMotivo(tracksFiltrados);

            // 3. Procesar y Enriquecer cada parada
            paradas.forEach(parada -> {
                // Esto asigna la severidad y puede cambiar el 'motivo' si detecta movimiento
                parada.calcularSeveridad();

                // Agregar observación si hay lugar relevante (Geocercas, POIs)
                LugarCercano lugarRelevante = parada.getLugarMasRelevante();
                if (lugarRelevante != null) {
                    parada.setObservaciones(
                            "Cerca de: " + lugarRelevante.nombre +
                                    " (" + lugarRelevante.tipo + ") a " +
                                    lugarRelevante.distancia + "m");
                }
            });

            // 4. GENERAR ESTADÍSTICAS (Aquí es donde ocurría el NullPointerException)
            System.out.println("\n📋 RESUMEN DE PARADAS:");
            System.out.println("   Total detectadas: " + paradas.size());

            // Agrupamiento por MOTIVO (Usando el getter correcto: getMotivo)
            Map<String, Long> conteoPorMotivo = paradas.stream()
                    .filter(p -> p.getMotivo() != null) // <-- FIX 1: Evita error si el motivo es null
                    .collect(Collectors.groupingBy(
                            ParadaDetectadaDto::getMotivo, // <-- FIX 2: Nombre correcto del método
                            Collectors.counting()));

            // Agrupamiento por SEVERIDAD
            Map<String, Long> conteoPorSeveridad = paradas.stream()
                    .filter(p -> p.getSeveridad() != null)
                    .collect(Collectors.groupingBy(
                            ParadaDetectadaDto::getSeveridad,
                            Collectors.counting()));

            // 5. Imprimir Desgloses en consola
            System.out.println("\n📂 DESGLOSE POR MOTIVO:");
            conteoPorMotivo.forEach((motivo, cantidad) -> System.out.println("   • " + motivo + ": " + cantidad));

            System.out.println("\n⚖️ DESGLOSE POR SEVERIDAD:");
            conteoPorSeveridad.forEach((sev, cantidad) -> System.out.println("   • " + sev + ": " + cantidad));

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

            return paradas;

        } catch (Exception e) {
            System.err.println("❌ Error analizando paradas: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Filtra tracks por el período del tramo
     */
    private List<Track> filtrarTracksPorPeriodo(List<Track> tracks, Tramo tramo) {
        if (tramo.getHoraSalidaReal() == null) {
            return tracks;
        }

        long timestampInicio = tramo.getHoraSalidaReal()
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();

        long timestampFin = tramo.getHoraLlegadaRealDestino() != null
                ? tramo.getHoraLlegadaRealDestino().atZone(ZoneId.systemDefault()).toEpochSecond()
                : Instant.now().getEpochSecond();

        return tracks.stream()
                .filter(t -> {
                    long timestamp = t.getGpstime();
                    return timestamp >= timestampInicio && timestamp <= timestampFin;
                })
                .collect(Collectors.toList());
    }
}

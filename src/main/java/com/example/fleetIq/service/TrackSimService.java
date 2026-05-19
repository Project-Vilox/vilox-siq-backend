package com.example.fleetIq.service;

import com.example.fleetIq.listener.ViajeUpdateEvent;
import com.example.fleetIq.model.Alarm;
import com.example.fleetIq.model.Geofence;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.model.Tramo.EstadoTramo;
import com.example.fleetIq.model.DemoSession;
import com.example.fleetIq.repository.AlarmRepository;
import com.example.fleetIq.repository.DemoSessionRepository;
import com.example.fleetIq.repository.GeocercaPorEstablecimientoRepository;
import com.example.fleetIq.repository.GeofenceRepository;
import com.example.fleetIq.repository.TrackRepository;
import com.example.fleetIq.repository.TramoRepository;
import com.example.fleetIq.repository.ViajeRepository;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class TrackSimService {

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private TramoRepository tramoRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private GeocercaPorEstablecimientoRepository geocercaPorEstablecimientoRepo;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DemoSessionRepository demoSessionRepository;

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    public record Waypoint(int paso, String nombre, String efecto, double lat, double lon) {}

    // Sesión — datos existentes
    private final Map<String, List<Waypoint>> sessions         = new ConcurrentHashMap<>();
    private final Map<String, Integer>        sessionIndex     = new ConcurrentHashMap<>();
    private final Map<String, String>         sessionImei      = new ConcurrentHashMap<>();
    private final Map<String, Long>           sessionBaseEpoch = new ConcurrentHashMap<>();
    private final Map<String, String>         sessionViajeId   = new ConcurrentHashMap<>();

    // Sesión — ruta OSRM
    private final Map<String, List<double[]>> sessionRoutePoints   = new ConcurrentHashMap<>();
    private final Map<String, int[]>          sessionCutoffs       = new ConcurrentHashMap<>();
    private final Map<String, Integer>        sessionTrackCursor   = new ConcurrentHashMap<>();
    private final Map<String, Long>           sessionDeltaSeconds  = new ConcurrentHashMap<>();

    private final Map<String, int[]>          demoStopRanges       = new ConcurrentHashMap<>(); // [roadStart, destStopStart]

    private final Random rng = new Random();

    // Modo Demo — scheduler por sesión
    private final ScheduledExecutorService    demoScheduler        = Executors.newScheduledThreadPool(8);
    private final Map<String, ScheduledFuture<?>> demoJobs         = new ConcurrentHashMap<>();
    private final Map<String, Integer>        demoRouteIdx         = new ConcurrentHashMap<>(); // cursor punto a punto
    // Sesiones demo activas — múltiples simultáneas
    private final Set<String>                 activeDemoSessions   = ConcurrentHashMap.newKeySet();
    private final Map<String, String>         demoSessionToViaje   = new ConcurrentHashMap<>(); // sessionId → viajeId
    private final Map<String, String>         viajeToActiveSession = new ConcurrentHashMap<>(); // viajeId  → sessionId

    // Span máximo de simulación: 8 horas. Evita que rutas con muchos puntos OSRM
    // generen gpstimes de hace días.
    private static final long MAX_SIM_SECONDS = 8L * 3600L;

    // Demo — fases de parada en establecimientos
    private static final int DEMO_ATENCION_MINUTOS = 3;   // minutes stopped at INTERNA
    private static final int DEMO_APPROACH_STEPS   = 5;   // interpolation steps between ext and int centroids
    private static final int DEMO_DEBOUNCE_TICKS   = 5;   // ticks waiting at extCentroid after return (5×7s=35s > 30s debounce)

    public Map<String, Object> init(String viajeId) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado: " + viajeId));

        if (viaje.getVehiculo() == null) {
            throw new IllegalArgumentException("El viaje no tiene vehículo asignado");
        }

        String imei = viaje.getVehiculo().getImei();
        if (imei == null || imei.isBlank()) {
            throw new IllegalArgumentException("El vehículo del viaje no tiene IMEI asignado");
        }

        List<Tramo> tramos = tramoRepository.findByViajeId(viajeId);

        // Resetear viaje
        viaje.setEstado("pendiente");
        viaje.setFechaInicioReal(null);
        viaje.setFechaFinReal(null);
        viaje.setIsDemo(true);
        viajeRepository.save(viaje);

        // Resetear tramos
        for (Tramo tramo : tramos) {
            tramo.setEstado(EstadoTramo.pendiente);
            tramo.setHoraLlegadaReal(null);
            tramo.setHoraSalidaReal(null);
            tramo.setHoraLlegadaRealDestino(null);
            tramo.setHoraSalidaRealDestino(null);
            tramo.setHoraEntradaGeocercaExternaOrigen(null);
            tramo.setHoraSalidaGeocercaExternaOrigen1(null);
            tramo.setHoraEntradaGeocercaInternaOrigen(null);
            tramo.setHoraSalidaGeocercaInternaOrigen(null);
            tramo.setHoraEntradaGeocercaExternaOrigen2(null);
            tramo.setHoraSalidaGeocercaExternaOrigen2(null);
            tramo.setHoraEntradaGeocercaExternaDestino(null);
            tramo.setHoraSalidaGeocercaExternaDestino1(null);
            tramo.setHoraEntradaGeocercaInternaDestino(null);
            tramo.setHoraSalidaGeocercaInternaDestino(null);
            tramo.setHoraEntradaGeocercaExternaDestino2(null);
            tramo.setHoraSalidaGeocercaExternaDestino2(null);
            tramo.setTiempoAtencionCita1(null);
            tramo.setTiempoPermanenciaCita1(null);
            tramo.setTiempoAtencionCita2(null);
            tramo.setTiempoPermanenciaCita2(null);
            tramoRepository.save(tramo);
        }

        // Borrar tracks de simulaciones anteriores para este IMEI
        trackRepository.deleteByImeiAndAlarmStatus(imei, "SIM_EVALUATED");

        // Cerrar alarmas activas del IMEI
        List<Alarm> alarmasActivas = alarmRepository.findByImeiAndExitTimeIsNull(imei);
        long now = System.currentTimeMillis() / 1000;
        for (Alarm alarm : alarmasActivas) {
            alarm.setExitTime(now);
            alarm.setAlarmType("ENTRY_EXIT");
            alarmRepository.save(alarm);
        }

        List<Waypoint> waypoints = calcularWaypoints(tramos);

        // Calcular centroides de geocercas para OSRM y marcadores del mapa
        double[] origenCoords = null, destinoCoords = null;
        if (!tramos.isEmpty()) {
            String origenId = tramos.get(0).getEstablecimientoOrigen().getId();
            String destinoId = tramos.get(tramos.size() - 1).getEstablecimientoDestino().getId();
            origenCoords = centroide(origenId, "EXTERNA");
            if (origenCoords == null) origenCoords = centroide(origenId, "INTERNA");
            destinoCoords = centroide(destinoId, "EXTERNA");
            if (destinoCoords == null) destinoCoords = centroide(destinoId, "INTERNA");
        }

        // Obtener ruta real por calles (OSRM) o fallback interpolación lineal
        List<double[]> routePoints;
        if (origenCoords != null && destinoCoords != null) {
            routePoints = obtenerRutaOSRM(origenCoords, destinoCoords);
        } else {
            Waypoint first = waypoints.get(0);
            Waypoint last  = waypoints.get(waypoints.size() - 1);
            routePoints = interpolarLineal(first.lat(), first.lon(), last.lat(), last.lon(), 30);
        }

        int[] cutoffs = calcularCutoffs(routePoints.size(), waypoints.size());

        // Delta dinámico: nunca superar MAX_SIM_SECONDS de span total.
        // Para N puntos grandes (rutas largas con muchos puntos OSRM), delta < 60s.
        // Para N pequeños (fallback 30 pts), delta = 60s.
        int  n          = routePoints.size();
        long totalSpan  = Math.min((long) n * 60L, MAX_SIM_SECONDS);
        long delta      = Math.max(1L, totalSpan / n);

        String sessionId = UUID.randomUUID().toString();
        long   baseEpoch = System.currentTimeMillis() / 1000 - totalSpan;

        sessions.put(sessionId, waypoints);
        sessionIndex.put(sessionId, 0);
        sessionImei.put(sessionId, imei);
        sessionBaseEpoch.put(sessionId, baseEpoch);
        sessionViajeId.put(sessionId, viajeId);
        sessionRoutePoints.put(sessionId, routePoints);
        sessionCutoffs.put(sessionId, cutoffs);
        sessionTrackCursor.put(sessionId, 0);
        sessionDeltaSeconds.put(sessionId, delta);

        System.out.println("🕐 SIM init: " + n + " puntos de ruta, delta=" + delta + "s, span=" + (totalSpan/3600) + "h");

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("sessionId", sessionId);
        response.put("imei", imei);
        response.put("totalPasos", waypoints.size());
        response.put("waypoints", waypoints);
        response.put("gpstimeMin", baseEpoch);
        response.put("gpstimeMax", baseEpoch + totalSpan + delta); // +delta de margen
        if (origenCoords != null) {
            response.put("origenLat", origenCoords[0]);
            response.put("origenLon", origenCoords[1]);
        }
        if (destinoCoords != null) {
            response.put("destinoLat", destinoCoords[0]);
            response.put("destinoLon", destinoCoords[1]);
        }
        return response;
    }

    @Transactional
    public Map<String, Object> next(String sessionId) {
        if (!sessions.containsKey(sessionId)) {
            throw new IllegalArgumentException("Sesión no encontrada: " + sessionId);
        }

        List<Waypoint> waypoints = sessions.get(sessionId);
        int idx  = sessionIndex.get(sessionId);
        int size = waypoints.size();

        if (idx >= size) {
            return Map.of(
                    "done",       true,
                    "pasoActual", idx,
                    "totalPasos", size
            );
        }

        Waypoint wp = waypoints.get(idx);
        String imei      = sessionImei.get(sessionId);
        long   baseEpoch = sessionBaseEpoch.get(sessionId);

        List<double[]> routePoints  = sessionRoutePoints.get(sessionId);
        int[]          cutoffs      = sessionCutoffs.get(sessionId);
        int            cursor       = sessionTrackCursor.get(sessionId);
        int            cutoff       = cutoffs[idx];
        long           deltaSeconds = sessionDeltaSeconds.get(sessionId);

        // Insertar batch de tracks desde cursor hasta cutoff (ruta real por calles)
        Track lastSaved = null;
        boolean enRuta = wp.nombre().contains("Ruta");
        for (int i = cursor; i <= cutoff && i < routePoints.size(); i++) {
            double[] pt      = routePoints.get(i);
            long     gpstime = baseEpoch + (long) i * deltaSeconds;

            Track track = new Track();
            track.setImei(imei);
            track.setGpstime(gpstime);
            track.setHearttime(System.currentTimeMillis() / 1000);
            track.setLatitude(pt[0]);
            track.setLongitude(pt[1]);
            track.setSpeed(enRuta ? 80.0 : 5.0);
            track.setCourse(0.0);
            track.setIsDemo(false);
            track.setAlarmStatus("SIM_EVALUATED");
            lastSaved = trackRepository.save(track);
        }

        long gpstime = baseEpoch + (long) cutoff * deltaSeconds;

        // Actualizar el tramo directamente (sin pipeline async)
        String viajeId = sessionViajeId.get(sessionId);
        aplicarEfectoDirecto(viajeId, wp, gpstime);

        sessionIndex.put(sessionId, idx + 1);
        sessionTrackCursor.put(sessionId, cutoff + 1);

        return Map.of(
                "pasoActual", idx + 1,
                "totalPasos", size,
                "waypoint",   wp,
                "trackId",    lastSaved != null ? lastSaved.getId() : -1L,
                "gpstime",    gpstime,
                "done",       idx + 1 >= size
        );
    }

    private void aplicarEfectoDirecto(String viajeId, Waypoint wp, long gpstime) {
        List<Tramo> tramos = tramoRepository.findByViajeId(viajeId);
        if (tramos.isEmpty()) return;

        tramos.sort(Comparator.comparingInt(Tramo::getOrden));

        // Tramo en_curso tiene prioridad; si no hay, tomar el primer pendiente
        Tramo tramo = tramos.stream()
                .filter(t -> t.getEstado() == EstadoTramo.en_curso)
                .findFirst()
                .orElseGet(() -> tramos.stream()
                        .filter(t -> t.getEstado() == EstadoTramo.pendiente)
                        .findFirst()
                        .orElse(null));

        if (tramo == null) return;

        // Usar hora actual para los timestamps del tramo (gpstime es histórico para los tracks)
        LocalDateTime ts     = LocalDateTime.now(ZONA_PERU);
        String        nombre = wp.nombre();
        boolean       actualizado = false;

        // ── ORIGEN ─────────────────────────────────────────────────────────────

        if (nombre.contains("Entrada Ext. Origen") && tramo.getHoraEntradaGeocercaExternaOrigen() == null) {
            tramo.setHoraEntradaGeocercaExternaOrigen(ts);
            tramo.setHoraLlegadaReal(ts);
            tramo.setEstado(EstadoTramo.en_curso);
            actualizado = true;
            System.out.println("🟢 SIM ORIGEN [1/5] horaLlegadaReal = " + ts);

        } else if (nombre.contains("Entrada Int. Origen") && tramo.getHoraEntradaGeocercaInternaOrigen() == null) {
            tramo.setHoraEntradaGeocercaInternaOrigen(ts);
            actualizado = true;
            System.out.println("🟢 SIM ORIGEN [2/5] horaEntradaGeocercaInternaOrigen = " + ts);

        } else if (nombre.contains("Fuera del Origen") && tramo.getHoraSalidaGeocercaExternaOrigen1() == null) {
            tramo.setHoraSalidaGeocercaExternaOrigen1(ts);
            if (tramo.getHoraSalidaGeocercaInternaOrigen() == null) {
                tramo.setHoraSalidaGeocercaInternaOrigen(ts);
            }
            actualizado = true;
            System.out.println("🟢 SIM ORIGEN [3/5] horaSalidaGeocercaExternaOrigen1 = " + ts);

        } else if (nombre.contains("Re-entrada Ext. Origen") && tramo.getHoraEntradaGeocercaExternaOrigen2() == null) {
            tramo.setHoraEntradaGeocercaExternaOrigen2(ts);
            actualizado = true;
            System.out.println("🟢 SIM ORIGEN [4/5] horaEntradaGeocercaExternaOrigen2 = " + ts);

        } else if (nombre.contains("Salida Final Origen") && tramo.getHoraSalidaReal() == null) {
            tramo.setHoraSalidaGeocercaExternaOrigen2(ts);
            tramo.setHoraSalidaReal(ts);
            if (tramo.getHoraLlegadaReal() != null) {
                tramo.setTiempoAtencionCita1((int) java.time.Duration.between(tramo.getHoraLlegadaReal(), ts).toMinutes());
            }
            actualizado = true;
            System.out.println("🟢 SIM ORIGEN [5/5] horaSalidaReal = " + ts);

        // ── DESTINO ────────────────────────────────────────────────────────────

        } else if (nombre.contains("Entrada Ext. Destino") && tramo.getHoraLlegadaRealDestino() == null) {
            tramo.setHoraEntradaGeocercaExternaDestino(ts);
            tramo.setHoraLlegadaRealDestino(ts);
            actualizado = true;
            System.out.println("🟢 SIM DESTINO [1/5] horaLlegadaRealDestino = " + ts);

        } else if (nombre.contains("Entrada Int. Destino") && tramo.getHoraEntradaGeocercaInternaDestino() == null) {
            tramo.setHoraEntradaGeocercaInternaDestino(ts);
            actualizado = true;
            System.out.println("🟢 SIM DESTINO [2/5] horaEntradaGeocercaInternaDestino = " + ts);

        } else if (nombre.contains("Fuera del Destino") && tramo.getHoraSalidaGeocercaExternaDestino1() == null) {
            tramo.setHoraSalidaGeocercaExternaDestino1(ts);
            if (tramo.getHoraSalidaGeocercaInternaDestino() == null) {
                tramo.setHoraSalidaGeocercaInternaDestino(ts);
            }
            actualizado = true;
            System.out.println("🟢 SIM DESTINO [3/5] horaSalidaGeocercaExternaDestino1 = " + ts);

        } else if (nombre.contains("Re-entrada Ext. Destino") && tramo.getHoraEntradaGeocercaExternaDestino2() == null) {
            tramo.setHoraEntradaGeocercaExternaDestino2(ts);
            actualizado = true;
            System.out.println("🟢 SIM DESTINO [4/5] horaEntradaGeocercaExternaDestino2 = " + ts);

        } else if (nombre.contains("Salida Final Destino") && tramo.getHoraSalidaRealDestino() == null) {
            tramo.setHoraSalidaGeocercaExternaDestino2(ts);
            tramo.setHoraSalidaRealDestino(ts);
            tramo.setEstado(EstadoTramo.completado);
            if (tramo.getHoraLlegadaRealDestino() != null) {
                tramo.setTiempoAtencionCita2((int) java.time.Duration.between(tramo.getHoraLlegadaRealDestino(), ts).toMinutes());
            }
            actualizado = true;
            System.out.println("🟢 SIM DESTINO [5/5] horaSalidaRealDestino = " + ts + " → COMPLETADO");
        }

        if (actualizado) {
            tramoRepository.save(tramo);
            eventPublisher.publishEvent(new ViajeUpdateEvent(this, viajeId));
        }
    }

    /**
     * Modo Demo: inserta 1 track cada intervalSeconds segundos reales usando el pipeline real
     * (alarmStatus=PENDING, isDemo=false) para que AlarmServiceImpl lo evalúe.
     * intervalSeconds=30 → tiempo real. intervalSeconds=7 → ~4x acelerado.
     */
    public Map<String, Object> initDemo(String viajeId, int intervalSeconds) {
        if (viajeToActiveSession.containsKey(viajeId)) {
            throw new IllegalStateException("El viaje " + viajeId + " ya tiene una demo activa.");
        }
        Map<String, Object> initResult = init(viajeId);
        String sessionId = (String) initResult.get("sessionId");

        List<double[]> osrmRoute = sessionRoutePoints.get(sessionId);
        String imei      = sessionImei.get(sessionId);
        long   baseEpoch = sessionBaseEpoch.get(sessionId);
        long   delta     = sessionDeltaSeconds.get(sessionId);

        // ── Build enriched route with establishment stop phases ───────────────────
        List<double[]> demoRoute    = osrmRoute;
        int[]          stopRanges   = {0, osrmRoute.size()}; // fallback: no stop phases

        List<Tramo> tramos = tramoRepository.findByViajeId(viajeId);
        if (!tramos.isEmpty()) {
            tramos.sort(Comparator.comparingInt(Tramo::getOrden));
            String origenId  = tramos.get(0).getEstablecimientoOrigen().getId();
            String destinoId = tramos.get(tramos.size() - 1).getEstablecimientoDestino().getId();

            double[] extOrigen  = centroide(origenId,  "EXTERNA");
            double[] intOrigen  = centroide(origenId,  "INTERNA");
            double[] extDestino = centroide(destinoId, "EXTERNA");
            double[] intDestino = centroide(destinoId, "INTERNA");

            if (extOrigen != null && intOrigen != null) {
                int nAtencion   = Math.max(6, (DEMO_ATENCION_MINUTOS * 60) / intervalSeconds);
                List<double[]> originStop = buildEstablishmentStopPhase(extOrigen, intOrigen, nAtencion);
                List<double[]> destStop   = (extDestino != null && intDestino != null)
                        ? buildEstablishmentStopPhase(extDestino, intDestino, nAtencion)
                        : new ArrayList<>();

                demoRoute = new ArrayList<>();
                demoRoute.addAll(originStop);
                demoRoute.addAll(osrmRoute);
                demoRoute.addAll(destStop);

                stopRanges = new int[]{originStop.size(), originStop.size() + osrmRoute.size()};
                System.out.println("🏗️ DEMO ruta enriquecida: " + originStop.size()
                        + " origin-stop + " + osrmRoute.size() + " OSRM + " + destStop.size()
                        + " dest-stop = " + demoRoute.size() + " total");
            }
        }

        sessionRoutePoints.put(sessionId, demoRoute);
        demoStopRanges.put(sessionId, stopRanges);

        demoRouteIdx.put(sessionId, 0);
        activeDemoSessions.add(sessionId);
        demoSessionToViaje.put(sessionId, viajeId);
        viajeToActiveSession.put(viajeId, sessionId);

        // Persist enriched route to BD
        JSONArray routeJson = new JSONArray();
        for (double[] p : demoRoute) {
            routeJson.put(new JSONArray(new double[]{p[0], p[1]}));
        }
        DemoSession ds = new DemoSession();
        ds.setId(sessionId);
        ds.setViajeId(viajeId);
        ds.setImei(imei);
        ds.setRoutePoints(routeJson.toString());
        ds.setCursor(0);
        ds.setIntervalSec(intervalSeconds);
        ds.setStatus("active");
        demoSessionRepository.save(ds);

        final List<double[]> finalRoute    = demoRoute;
        final int[]          finalRanges   = stopRanges;
        final int            destStart     = stopRanges[1]; // start of destination stop phase

        ScheduledFuture<?> job = demoScheduler.scheduleAtFixedRate(() -> {
            try {
                int idx = demoRouteIdx.getOrDefault(sessionId, 0);
                if (idx >= finalRoute.size()) {
                    double[] lastPt = finalRoute.get(finalRoute.size() - 1);
                    completarDestinoDemoDirecto(viajeId, lastPt[0], lastPt[1]);
                    stopDemo(sessionId, false);
                    return;
                }
                if (idx >= destStart) {
                    Viaje v = viajeRepository.findById(viajeId).orElse(null);
                    if (v == null || "completado".equals(v.getEstado())) {
                        stopDemo(sessionId, false);
                        return;
                    }
                }

                double[] pt          = finalRoute.get(idx);
                long     gpstime     = System.currentTimeMillis() / 1000;
                boolean  isStopPhase = idx < finalRanges[0] || idx >= finalRanges[1];
                double   speed       = isStopPhase ? 0.0
                        : calcularVelocidadDemo(idx, finalRanges[0], finalRanges[1]);

                Track track = new Track();
                track.setImei(imei);
                track.setGpstime(gpstime);
                track.setHearttime(gpstime);
                track.setLatitude(pt[0]);
                track.setLongitude(pt[1]);
                track.setSpeed(speed);
                track.setCourse(0.0);
                track.setIsDemo(false);
                track.setAlarmStatus("PENDING");
                trackRepository.save(track);

                // Direct apply tramo state at key stop-phase ticks
                if (isStopPhase) {
                    int originStop0   = 0;
                    int originStop1   = 1;
                    int originStopEnd = finalRanges[0] - 1;
                    int destStop0     = finalRanges[1];
                    int destStop1     = finalRanges[1] + 1;
                    int destStopEnd   = finalRoute.size() - 1;

                    if (idx == originStop0) {
                        aplicarWaypointDireto(viajeId, "Entrada Ext. Origen", pt[0], pt[1]);
                    } else if (idx == originStop1) {
                        aplicarWaypointDireto(viajeId, "Entrada Int. Origen", pt[0], pt[1]);
                    } else if (idx == originStopEnd) {
                        aplicarWaypointDireto(viajeId, "Fuera del Origen",       pt[0], pt[1]);
                        aplicarWaypointDireto(viajeId, "Re-entrada Ext. Origen", pt[0], pt[1]);
                        aplicarWaypointDireto(viajeId, "Salida Final Origen",    pt[0], pt[1]);
                    } else if (idx == destStop0) {
                        aplicarWaypointDireto(viajeId, "Entrada Ext. Destino", pt[0], pt[1]);
                    } else if (idx == destStop1) {
                        aplicarWaypointDireto(viajeId, "Entrada Int. Destino", pt[0], pt[1]);
                    } else if (idx == destStopEnd) {
                        aplicarWaypointDireto(viajeId, "Fuera del Destino",         pt[0], pt[1]);
                        aplicarWaypointDireto(viajeId, "Re-entrada Ext. Destino",   pt[0], pt[1]);
                        aplicarWaypointDireto(viajeId, "Salida Final Destino",      pt[0], pt[1]);
                    }
                }

                int nextIdx;
                if (isStopPhase) {
                    nextIdx = idx + 1;
                } else {
                    nextIdx = calcularSiguienteIdx(idx, finalRoute, intervalSeconds);
                    // No saltar sobre el inicio de la destination stop phase
                    if (nextIdx > destStart) nextIdx = destStart;
                }
                final int finalNextIdx = nextIdx;
                demoRouteIdx.put(sessionId, finalNextIdx);

                demoSessionRepository.findById(sessionId).ifPresent(d -> {
                    d.setCursor(finalNextIdx);
                    demoSessionRepository.save(d);
                });

                System.out.println("🎬 DEMO tick " + nextIdx + "/" + finalRoute.size()
                        + " [" + (isStopPhase ? "STOP" : "ROAD") + "] IMEI=" + imei);
            } catch (Exception e) {
                System.err.println("❌ DEMO tick error: " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);

        demoJobs.put(sessionId, job);
        initResult.put("modo", "demo");
        initResult.put("intervalSeconds", intervalSeconds);
        return initResult;
    }

    public void stopDemo(String sessionId, boolean deleteSession) {
        ScheduledFuture<?> job = demoJobs.remove(sessionId);
        if (job != null) job.cancel(false);
        demoRouteIdx.remove(sessionId);
        demoStopRanges.remove(sessionId);
        activeDemoSessions.remove(sessionId);
        String vid = demoSessionToViaje.remove(sessionId);
        if (vid != null) viajeToActiveSession.remove(vid);
        if (deleteSession) cleanup(sessionId);

        // Actualizar status en BD
        demoSessionRepository.findById(sessionId).ifPresent(d -> {
            d.setStatus(deleteSession ? "stopped" : "completed");
            demoSessionRepository.save(d);
        });

        System.out.println("🛑 DEMO detenido: " + sessionId);
    }

    public boolean isDemoActive(String sessionId) {
        return activeDemoSessions.contains(sessionId);
    }

    public Map<String, Object> getDemoStatusByViaje(String viajeId) {
        String sessionId = viajeToActiveSession.get(viajeId);
        if (sessionId == null) return Map.of("active", false);
        return Map.of("active", true, "sessionId", sessionId);
    }

    public String getActiveDemoSession() {
        return activeDemoSessions.isEmpty() ? null : activeDemoSessions.iterator().next();
    }

    @PostConstruct
    public void restoreActiveSessions() {
        List<DemoSession> activas = demoSessionRepository.findByStatus("active");
        if (activas.isEmpty()) return;

        for (DemoSession ds : activas) {
            try {
                JSONArray arr = new JSONArray(ds.getRoutePoints());
                List<double[]> route = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONArray p = arr.getJSONArray(i);
                    route.add(new double[]{p.getDouble(0), p.getDouble(1)});
                }

                String sessionId    = ds.getId();
                int    startCursor  = ds.getCursor();
                int    intervalSecs = ds.getIntervalSec();
                String imei         = ds.getImei();

                Viaje viajeCheck = viajeRepository.findById(ds.getViajeId()).orElse(null);
                if (viajeCheck == null || "completado".equals(viajeCheck.getEstado())) {
                    ds.setStatus("completed");
                    demoSessionRepository.save(ds);
                    System.out.println("✅ Sesión " + sessionId + " viaje ya completado, no se restaura");
                    continue;
                }

                // Recalculate stop ranges from route size and intervalSec
                int stopPhaseSize = calcStopPhaseSize(intervalSecs);
                int[] stopRangesRestore = (route.size() > 2 * stopPhaseSize)
                        ? new int[]{stopPhaseSize, route.size() - stopPhaseSize}
                        : new int[]{0, route.size()};

                sessionImei.put(sessionId, imei);
                sessionViajeId.put(sessionId, ds.getViajeId());
                sessionRoutePoints.put(sessionId, route);
                demoStopRanges.put(sessionId, stopRangesRestore);
                demoRouteIdx.put(sessionId, startCursor);
                activeDemoSessions.add(sessionId);
                demoSessionToViaje.put(sessionId, ds.getViajeId());
                viajeToActiveSession.put(ds.getViajeId(), sessionId);

                final int[] finalRangesRestore = stopRangesRestore;
                final int   destStartRestore   = stopRangesRestore[1];

                ScheduledFuture<?> job = demoScheduler.scheduleAtFixedRate(() -> {
                    try {
                        int idx = demoRouteIdx.getOrDefault(sessionId, 0);
                        if (idx >= route.size()) {
                            String vid = demoSessionToViaje.get(sessionId);
                            if (vid != null) {
                                double[] lastPt = route.get(route.size() - 1);
                                completarDestinoDemoDirecto(vid, lastPt[0], lastPt[1]);
                            }
                            stopDemo(sessionId, false);
                            return;
                        }
                        if (idx >= destStartRestore) {
                            String vid = demoSessionToViaje.get(sessionId);
                            if (vid != null) {
                                Viaje v = viajeRepository.findById(vid).orElse(null);
                                if (v == null || "completado".equals(v.getEstado())) {
                                    stopDemo(sessionId, false);
                                    return;
                                }
                            }
                        }

                        double[] pt          = route.get(idx);
                        long     gpstime     = System.currentTimeMillis() / 1000;
                        boolean  isStopPhase = idx < finalRangesRestore[0] || idx >= finalRangesRestore[1];
                        double   speed       = isStopPhase ? 0.0
                                : calcularVelocidadDemo(idx, finalRangesRestore[0], finalRangesRestore[1]);

                        Track track = new Track();
                        track.setImei(imei);
                        track.setGpstime(gpstime);
                        track.setHearttime(gpstime);
                        track.setLatitude(pt[0]);
                        track.setLongitude(pt[1]);
                        track.setSpeed(speed);
                        track.setCourse(0.0);
                        track.setIsDemo(false);
                        track.setAlarmStatus("PENDING");
                        trackRepository.save(track);

                        int nextIdx;
                        if (isStopPhase) {
                            nextIdx = idx + 1;
                        } else {
                            nextIdx = calcularSiguienteIdx(idx, route, intervalSecs);
                            // No saltar sobre el inicio de la destination stop phase
                            if (nextIdx > destStartRestore) nextIdx = destStartRestore;
                        }
                        final int finalNextIdx = nextIdx;
                        demoRouteIdx.put(sessionId, finalNextIdx);

                        demoSessionRepository.findById(sessionId).ifPresent(d -> {
                            d.setCursor(finalNextIdx);
                            demoSessionRepository.save(d);
                        });

                        System.out.println("🔄 DEMO restaurado tick " + nextIdx + "/" + route.size()
                                + " [" + (isStopPhase ? "STOP" : "ROAD") + "] IMEI=" + imei);
                    } catch (Exception e) {
                        System.err.println("❌ DEMO restore tick error: " + e.getMessage());
                    }
                }, 0, intervalSecs, TimeUnit.SECONDS);

                demoJobs.put(sessionId, job);
                System.out.println("✅ Demo restaurado desde cursor=" + startCursor + " sessionId=" + sessionId);

            } catch (Exception e) {
                System.err.println("❌ Error restaurando sesión " + ds.getId() + ": " + e.getMessage());
                ds.setStatus("stopped");
                demoSessionRepository.save(ds);
            }
        }
    }

    public void cleanup(String sessionId) {
        sessions.remove(sessionId);
        sessionIndex.remove(sessionId);
        sessionImei.remove(sessionId);
        sessionBaseEpoch.remove(sessionId);
        sessionViajeId.remove(sessionId);
        sessionRoutePoints.remove(sessionId);
        sessionCutoffs.remove(sessionId);
        sessionTrackCursor.remove(sessionId);
        sessionDeltaSeconds.remove(sessionId);
        demoStopRanges.remove(sessionId);
    }

    // ── Completar destino al final de la ruta demo ────────────────────────────

    private void aplicarWaypointDireto(String viajeId, String nombre, double lat, double lon) {
        long gpstime = System.currentTimeMillis() / 1000;
        aplicarEfectoDirecto(viajeId, new Waypoint(0, nombre, "", lat, lon), gpstime);
    }

    /**
     * Dispara los 5 eventos de destino directamente sobre el tramo activo.
     * Cada evento verifica si el campo ya fue seteado por AlarmServiceImpl antes de escribir.
     * Se llama cuando el scheduler de demo agota los puntos de ruta.
     */
    private void completarDestinoDemoDirecto(String viajeId, double lat, double lon) {
        String[] eventos = {
            "Entrada Ext. Destino",
            "Entrada Int. Destino",
            "Fuera del Destino",
            "Re-entrada Ext. Destino",
            "Salida Final Destino"
        };
        long gpstime = System.currentTimeMillis() / 1000;
        for (String nombre : eventos) {
            aplicarEfectoDirecto(viajeId, new Waypoint(0, nombre, "", lat, lon), gpstime);
        }
        System.out.println("🏁 DEMO completado directamente para viaje " + viajeId);
    }

    // ── Avance por distancia real ──────────────────────────────────────────────

    /**
     * Calcula el siguiente índice en la ruta avanzando los puntos necesarios para
     * cubrir la distancia equivalente a 80 km/h durante intervalSeconds segundos.
     * Garantiza avanzar al menos 1 punto y nunca sobrepasar el tamaño de la ruta.
     */
    private int calcularSiguienteIdx(int currentIdx, List<double[]> route, int intervalSeconds) {
        double targetMetros = 80_000.0 / 3600.0 * intervalSeconds; // 80 km/h en m/s × segundos
        double acumulado = 0.0;
        int idx = currentIdx;

        while (idx + 1 < route.size()) {
            double[] a = route.get(idx);
            double[] b = route.get(idx + 1);
            acumulado += haversineMetros(a[0], a[1], b[0], b[1]);
            idx++;
            if (acumulado >= targetMetros) break;
        }

        // Garantizar avance mínimo de 1 — evita quedar pegado en el último punto
        return Math.max(idx, currentIdx + 1);
    }

    private double haversineMetros(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ── Ruta OSRM ─────────────────────────────────────────────────────────────

    private List<double[]> obtenerRutaOSRM(double[] origen, double[] destino) {
        try {
            // OSRM espera lon,lat (no lat,lon)
            String url = String.format(
                    "http://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                    origen[1], origen[0],
                    destino[1], destino[0]
            );

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json   = new JSONObject(response.body());
                JSONArray  coords = json
                        .getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<double[]> points = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray coord = coords.getJSONArray(i);
                    double lon = coord.getDouble(0); // OSRM: [lon, lat]
                    double lat = coord.getDouble(1);
                    points.add(new double[]{lat, lon});
                }

                if (!points.isEmpty()) {
                    System.out.println("🗺️ OSRM: ruta con " + points.size() + " puntos");
                    return points;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ OSRM no disponible, usando interpolación lineal: " + e.getMessage());
        }

        // Fallback: línea recta con 30 puntos intermedios
        return interpolarLineal(origen[0], origen[1], destino[0], destino[1], 30);
    }

    private List<double[]> interpolarLineal(double latOrigen, double lonOrigen,
                                            double latDestino, double lonDestino, int n) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t   = (double) i / (n - 1);
            double lat = latOrigen + t * (latDestino - latOrigen);
            double lon = lonOrigen + t * (lonDestino - lonOrigen);
            points.add(new double[]{lat, lon});
        }
        return points;
    }

    /**
     * Distribuye N puntos de ruta en cutoffs para los 12 eventos de negocio.
     *
     * Zona origen   (eventos 0–4): primer 15% de la ruta
     * Zona tránsito (evento 5):    punto medio de la ruta
     * Zona destino  (eventos 6–11): último 15% de la ruta
     */
    private int[] calcularCutoffs(int n, int numWaypoints) {
        int[] cutoffs = new int[numWaypoints];

        int originEnd  = Math.max(10, (int)(n * 0.15));
        int destStart  = Math.min(n - 10, (int)(n * 0.85));

        // Origen: distribuir 5 eventos en [0, originEnd]
        // cutoffs[0] = 0 → paso 1 coloca el camión exactamente al inicio de la ruta
        cutoffs[0] = 0;
        int originSlice = Math.max(1, originEnd / 5);
        for (int i = 1; i < 5 && i < numWaypoints; i++) {
            cutoffs[i] = Math.min(i * originSlice, n - 1);
        }

        // Tránsito: evento 5 en el punto medio
        if (numWaypoints > 5) {
            cutoffs[5] = n / 2;
        }

        // Destino: distribuir eventos 6–11 en [destStart, n-1]
        int destCount = numWaypoints - 6;
        if (destCount > 0) {
            int destRange = (n - 1) - destStart;
            for (int i = 0; i < destCount; i++) {
                int denom = Math.max(1, destCount - 1);
                cutoffs[6 + i] = destStart + (destRange * i) / denom;
            }
            cutoffs[numWaypoints - 1] = n - 1; // último siempre al final de la ruta
        }

        // Garantizar estrictamente creciente y dentro de [0, n-1]
        for (int i = 1; i < cutoffs.length; i++) {
            if (cutoffs[i] <= cutoffs[i - 1]) {
                cutoffs[i] = cutoffs[i - 1] + 1;
            }
            if (cutoffs[i] >= n) {
                cutoffs[i] = n - 1;
            }
        }

        return cutoffs;
    }

    // ── Waypoints de negocio ───────────────────────────────────────────────────

    private List<Waypoint> calcularWaypoints(List<Tramo> tramos) {
        List<Waypoint> waypoints = new ArrayList<>();
        int paso = 1;

        for (int t = 0; t < tramos.size(); t++) {
            Tramo tramo = tramos.get(t);

            String origenId      = tramo.getEstablecimientoOrigen().getId();
            String destinoId     = tramo.getEstablecimientoDestino().getId();
            String nombreOrigen  = tramo.getEstablecimientoOrigen().getNombre();
            String nombreDestino = tramo.getEstablecimientoDestino().getNombre();

            double[] extOrigen  = centroide(origenId,  "EXTERNA");
            double[] intOrigen  = centroide(origenId,  "INTERNA");
            double[] extDestino = centroide(destinoId, "EXTERNA");
            double[] intDestino = centroide(destinoId, "INTERNA");

            System.out.println("🔍 SIM DEBUG tramo[" + t + "] origenId=" + origenId
                    + " extOrigen=" + (extOrigen != null ? "OK" : "NULL")
                    + " intOrigen=" + (intOrigen != null ? "OK" : "NULL"));
            System.out.println("🔍 SIM DEBUG tramo[" + t + "] destinoId=" + destinoId
                    + " extDestino=" + (extDestino != null ? "OK" : "NULL")
                    + " intDestino=" + (intDestino != null ? "OK" : "NULL"));

            if (extOrigen  == null) extOrigen  = intOrigen;
            if (extDestino == null) extDestino = intDestino;

            // Pasos de ORIGEN — solo para el primer tramo
            if (t == 0 && extOrigen != null) {
                double[] intOrigenEfectivo = (intOrigen != null) ? intOrigen : extOrigen;

                waypoints.add(new Waypoint(paso++,
                        "Entrada Ext. Origen (" + nombreOrigen + ")",
                        "horaLlegadaReal · estado en_curso",
                        extOrigen[0], extOrigen[1]));

                waypoints.add(new Waypoint(paso++,
                        "Entrada Int. Origen (" + nombreOrigen + ")",
                        "horaEntradaGeocercaInternaOrigen",
                        intOrigenEfectivo[0], intOrigenEfectivo[1]));

                waypoints.add(new Waypoint(paso++,
                        "Fuera del Origen",
                        "EXIT ext+int → horaSalidaGeocercaExternaOrigen1",
                        extOrigen[0], extOrigen[1]));

                waypoints.add(new Waypoint(paso++,
                        "Re-entrada Ext. Origen",
                        "horaEntradaGeocercaExternaOrigen2",
                        extOrigen[0], extOrigen[1]));

                waypoints.add(new Waypoint(paso++,
                        "Salida Final Origen",
                        "horaSalidaReal ← viaje en camino",
                        extOrigen[0], extOrigen[1]));
            }

            // Paso EN RUTA
            if (extOrigen != null && extDestino != null) {
                double midLat = (extOrigen[0] + extDestino[0]) / 2.0;
                double midLon = (extOrigen[1] + extDestino[1]) / 2.0;

                waypoints.add(new Waypoint(paso++,
                        "En Ruta → " + nombreDestino,
                        "vehículo en tránsito · ETA visible",
                        midLat, midLon));
            }

            // Pasos de DESTINO
            if (extDestino != null) {
                double[] intDestinoEfectivo = (intDestino != null) ? intDestino : extDestino;

                waypoints.add(new Waypoint(paso++,
                        "Llegada Zona Destino",
                        "punto previo a geocercas",
                        extDestino[0], extDestino[1]));

                waypoints.add(new Waypoint(paso++,
                        "Entrada Ext. Destino (" + nombreDestino + ")",
                        "horaLlegadaRealDestino",
                        extDestino[0], extDestino[1]));

                waypoints.add(new Waypoint(paso++,
                        "Entrada Int. Destino (" + nombreDestino + ")",
                        "horaEntradaGeocercaInternaDestino",
                        intDestinoEfectivo[0], intDestinoEfectivo[1]));

                waypoints.add(new Waypoint(paso++,
                        "Fuera del Destino",
                        "EXIT ext+int destino → horaSalidaGeocercaExternaDestino1",
                        extDestino[0], extDestino[1]));

                waypoints.add(new Waypoint(paso++,
                        "Re-entrada Ext. Destino",
                        "horaEntradaGeocercaExternaDestino2",
                        extDestino[0], extDestino[1]));

                waypoints.add(new Waypoint(paso++,
                        "Salida Final Destino",
                        "tramo completado · viaje completado",
                        extDestino[0], extDestino[1]));
            }
        }

        return waypoints;
    }

    // ── Demo stop phase helpers ───────────────────────────────────────────────

    private List<double[]> interpolatePoints(double[] from, double[] to, int steps) {
        List<double[]> pts = new ArrayList<>();
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            pts.add(new double[]{
                from[0] + t * (to[0] - from[0]),
                from[1] + t * (to[1] - from[1])
            });
        }
        return pts;
    }

    private List<double[]> buildEstablishmentStopPhase(double[] extCentroid, double[] intCentroid, int nAtencion) {
        double[] intr = (intCentroid != null) ? intCentroid : extCentroid;
        List<double[]> seq = new ArrayList<>();
        seq.add(extCentroid.clone());                         // idx 0 — entry ext (direct apply)
        seq.add(intr.clone());                                // idx 1 — entry int (direct apply)
        for (int i = 2; i <= nAtencion; i++) seq.add(intr.clone()); // static attention ticks
        seq.add(intr.clone());                                // idx nAtencion+1 — departure (direct apply)
        return seq;
    }

    private int calcStopPhaseSize(int intervalSecs) {
        int nAtencion = Math.max(6, (DEMO_ATENCION_MINUTOS * 60) / intervalSecs);
        return nAtencion + 2;
    }

    /**
     * Calcula velocidad simulada para un tick de ruta (no stop phase).
     * Varía según posición en la ruta:
     *   - Primer 10%: saliendo del establecimiento (30-45 km/h)
     *   - Medio 80%:  ruta libre (60-75 km/h)
     *   - Último 10%: llegando al establecimiento (25-40 km/h)
     * Agrega ±5 km/h de ruido aleatorio.
     */
    private double calcularVelocidadDemo(int idx, int roadStart, int roadEnd) {
        if (roadEnd <= roadStart) return 65.0;
        double progress = (double)(idx - roadStart) / (roadEnd - roadStart);
        double base;
        if (progress < 0.10) {
            base = 30.0 + progress / 0.10 * 15.0; // 30→45 km/h
        } else if (progress > 0.90) {
            base = 40.0 - (progress - 0.90) / 0.10 * 15.0; // 40→25 km/h
        } else {
            base = 65.0; // 65 km/h crucero
        }
        double noise = (rng.nextDouble() * 10.0) - 5.0; // ±5 km/h
        return Math.max(10.0, base + noise);
    }

    // ── Geocercas ─────────────────────────────────────────────────────────────

    private double[] centroide(String establecimientoId, String tipo) {
        try {
            java.util.Optional<Long> geocercaIdOpt;

            if ("EXTERNA".equals(tipo)) {
                geocercaIdOpt = geocercaPorEstablecimientoRepo.findGeocercaExternaId(establecimientoId);
            } else {
                geocercaIdOpt = geocercaPorEstablecimientoRepo.findGeocercaInternaId(establecimientoId);
            }

            if (geocercaIdOpt.isEmpty()) return null;

            Long geocercaId = geocercaIdOpt.get();
            java.util.Optional<Geofence> gfOpt = geofenceRepository.findById(geocercaId);

            if (gfOpt.isEmpty()) return null;

            Geofence gf = gfOpt.get();
            if (gf.getPoints() == null) return null;

            JSONArray pts = new JSONArray(gf.getPoints());
            int n = pts.length();
            if (n == 0) return null;

            double sumLat = 0.0, sumLon = 0.0;
            for (int i = 0; i < n; i++) {
                JSONArray pt = pts.getJSONArray(i);
                sumLat += pt.getDouble(0);
                sumLon += pt.getDouble(1);
            }

            return new double[]{sumLat / n, sumLon / n};

        } catch (Exception e) {
            System.out.println("🔴 SIM centroide ERROR establecimientoId=" + establecimientoId
                    + " tipo=" + tipo + " → " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    // ── Estado de sesión (consultado por el controller) ───────────────────────

    public List<Map<String, Object>> getReplayPoints(String sessionId) {
        List<double[]> route = sessionRoutePoints.get(sessionId);
        if (route == null) throw new IllegalArgumentException("Sesión no encontrada: " + sessionId);
        long base  = sessionBaseEpoch.get(sessionId);
        long delta = sessionDeltaSeconds.get(sessionId);
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < route.size(); i++) {
            double[] pt = route.get(i);
            points.add(Map.of("lat", pt[0], "lon", pt[1], "gpstime", base + (long) i * delta, "index", i));
        }
        return points;
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public int getPasoActual(String sessionId) {
        return sessionIndex.getOrDefault(sessionId, 0);
    }

    public int getTotalPasos(String sessionId) {
        List<Waypoint> waypoints = sessions.get(sessionId);
        return waypoints != null ? waypoints.size() : 0;
    }
}

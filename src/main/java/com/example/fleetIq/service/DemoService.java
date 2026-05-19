package com.example.fleetIq.service;

import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.repository.TrackRepository;
import com.example.fleetIq.repository.TramoRepository;
import com.example.fleetIq.repository.ViajeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoService.class);
    private static final ZoneId PERU_ZONE = ZoneId.of("America/Lima");
    private static final String OSRM_URL = "http://router.project-osrm.org/route/v1/driving/";
    private static final int TARGET_TOTAL_POINTS = 200; // ~10 min at 3s/point

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TramoRepository tramoRepository;

    @Autowired
    private TramoService tramoService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    // ── Session maps ─────────────────────────────────────────────────────────
    private final Map<String, ScheduledFuture<?>>          sessions                   = new ConcurrentHashMap<>();
    private final Map<String, List<Long>>                  sessionTrackIds            = new ConcurrentHashMap<>();
    private final Map<String, Integer>                     sessionIndex               = new ConcurrentHashMap<>();
    private final Map<String, List<Track>>                 sessionTracks              = new ConcurrentHashMap<>();
    private final Map<String, String>                      sessionVehiculoId          = new ConcurrentHashMap<>();
    private final Map<String, double[]>                    sessionOrigen              = new ConcurrentHashMap<>();
    private final Map<String, double[]>                    sessionDestino             = new ConcurrentHashMap<>();
    // Tramo tracking
    private final Map<String, List<Integer>>               sessionTramoBoundaries     = new ConcurrentHashMap<>();
    private final Map<String, List<Tramo>>                 sessionTramos              = new ConcurrentHashMap<>();
    private final Map<String, String>                      sessionViajeId             = new ConcurrentHashMap<>();
    // Restoration snapshots
    private final Map<String, Map<String, LocalDateTime[]>> sessionOriginalTramoTimes = new ConcurrentHashMap<>();
    private final Map<String, Object[]>                    sessionOriginalViajeState  = new ConcurrentHashMap<>();
    private final Map<String, Boolean>                     sessionCompletedNaturally  = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public String start(String viajeId, int intervaloSegundos) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado: " + viajeId));

        if (viaje.getVehiculo() == null)
            throw new IllegalArgumentException("El viaje no tiene vehículo asignado");

        List<Tramo> tramos = tramoRepository.findByViajeId(viajeId); // ordered by orden ASC
        if (tramos.isEmpty())
            throw new IllegalArgumentException("El viaje no tiene tramos");

        String imei = viaje.getVehiculo().getImei();
        String sessionId = UUID.randomUUID().toString();

        // ── 1. Generate tracks (OSRM first, historical fallback) ─────────────
        List<Integer> boundaries = new ArrayList<>();
        List<Track> tracks = generateSyntheticTracks(tramos, imei, boundaries);

        if (tracks.isEmpty()) {
            logger.warn("🎭 OSRM no disponible, usando tracks históricos");
            tracks = loadHistoricalTracks(viaje);
            boundaries.clear();
            boundaries.add(tracks.size());
        }

        if (tracks.isEmpty())
            throw new IllegalArgumentException(
                    "No hay tracks disponibles (OSRM falló y el vehículo no tiene histórico)");

        // ── 2. Save original state for restoration ────────────────────────────
        Map<String, LocalDateTime[]> origTimes = new HashMap<>();
        for (Tramo t : tramos) {
            origTimes.put(t.getId(), new LocalDateTime[]{
                    t.getHoraSalidaProgramada(),
                    t.getHoraLlegadaProgramada(),
                    t.getHoraSalidaReal(),
                    t.getHoraLlegadaReal(),
                    t.getHoraLlegadaRealDestino()
            });
        }
        sessionOriginalTramoTimes.put(sessionId, origTimes);
        sessionOriginalViajeState.put(sessionId, new Object[]{
                viaje.getFechaInicioReal(),
                viaje.getFechaFinReal(),
                viaje.getEstado()
        });

        // ── 3. Set real times — keep programmed times as configured by user ──
        LocalDateTime now = LocalDateTime.now(PERU_ZONE);

        Tramo primerTramo = tramos.get(0);
        // Only set real times — programmed times stay as the user configured them
        primerTramo.setHoraLlegadaReal(now.minusMinutes(30)); // arrived at origin 30 min ago (loading)
        primerTramo.setHoraSalidaReal(now);
        primerTramo.setHoraLlegadaRealDestino(null); // clear any leftover from a previous demo
        primerTramo.setEstado(Tramo.EstadoTramo.en_curso);
        tramoRepository.save(primerTramo);

        // Subsequent tramos: pendiente
        for (int i = 1; i < tramos.size(); i++) {
            Tramo t = tramos.get(i);
            t.setEstado(Tramo.EstadoTramo.pendiente);
            t.setHoraSalidaReal(null);
            t.setHoraLlegadaReal(null);
            t.setHoraLlegadaRealDestino(null);
            tramoRepository.save(t);
        }

        // Viaje: en_curso — clear fechaFinReal so previous demo's end time doesn't bleed through
        viaje.setEstado("en_curso");
        viaje.setFechaInicioReal(now);
        viaje.setFechaFinReal(null);
        viaje.setIsDemo(true);
        viajeRepository.save(viaje);

        // ── 4. Origen/destino for frontend markers ────────────────────────────
        var origen  = primerTramo.getEstablecimientoOrigen();
        var destino = tramos.get(tramos.size() - 1).getEstablecimientoDestino();
        sessionOrigen.put(sessionId,  new double[]{origen.getLatitud().doubleValue(),  origen.getLongitud().doubleValue()});
        sessionDestino.put(sessionId, new double[]{destino.getLatitud().doubleValue(), destino.getLongitud().doubleValue()});

        // ── 5. Store session data ─────────────────────────────────────────────
        sessionTracks.put(sessionId, tracks);
        sessionIndex.put(sessionId, 0);
        sessionTrackIds.put(sessionId, new ArrayList<>());
        sessionVehiculoId.put(sessionId, viaje.getVehiculo().getId());
        sessionTramoBoundaries.put(sessionId, boundaries);
        sessionTramos.put(sessionId, tramos);
        sessionViajeId.put(sessionId, viajeId);
        sessionCompletedNaturally.put(sessionId, false);

        // ── 6. Start scheduler ────────────────────────────────────────────────
        final String imeiRef = imei;
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                int idx = sessionIndex.getOrDefault(sessionId, 0);
                List<Track> trackList = sessionTracks.get(sessionId);

                if (trackList == null || idx >= trackList.size()) {
                    completeDemoNaturally(sessionId);
                    return;
                }

                // Insert synthetic track
                Track src = trackList.get(idx);
                Track demo = new Track();
                demo.setImei(imeiRef);
                demo.setGpstime(System.currentTimeMillis() / 1000);
                demo.setHearttime(System.currentTimeMillis() / 1000);
                demo.setLatitude(src.getLatitude());
                demo.setLongitude(src.getLongitude());
                demo.setSpeed(src.getSpeed());
                demo.setCourse(src.getCourse() != null ? src.getCourse() : 0.0);
                demo.setIsDemo(true);
                demo.setAlarmStatus("EVALUATED");

                Track saved = trackRepository.save(demo);
                sessionTrackIds.get(sessionId).add(saved.getId());
                sessionIndex.put(sessionId, idx + 1);

                // Check tramo boundaries (idx+1 because we just inserted point idx)
                checkTramoBoundary(sessionId, idx + 1);

                logger.debug("🎭 Demo [{}] punto {}/{} → ({}, {})",
                        sessionId.substring(0, 8), idx + 1, trackList.size(),
                        String.format("%.4f", src.getLatitude()),
                        String.format("%.4f", src.getLongitude()));

            } catch (Exception e) {
                logger.error("🎭 Error en scheduler demo [{}]: {}", sessionId.substring(0, 8), e.getMessage());
            }
        }, 0, intervaloSegundos, TimeUnit.SECONDS);

        sessions.put(sessionId, future);

        long demoMinutes = ((long) tracks.size() * intervaloSegundos) / 60;
        logger.info("🎭 Demo iniciada: session={}, viaje={}, tracks={}, intervalo={}s (~{}min)",
                sessionId.substring(0, 8), viajeId, tracks.size(), intervaloSegundos, demoMinutes);

        return sessionId;
    }

    public Map<String, Object> status(String sessionId) {
        boolean active = sessions.containsKey(sessionId) && !sessions.get(sessionId).isDone();
        int idx   = sessionIndex.getOrDefault(sessionId, 0);
        int total = sessionTracks.getOrDefault(sessionId, List.of()).size();
        return Map.of(
                "active",       active,
                "puntoActual",  idx,
                "totalPuntos",  total,
                "progreso",     total > 0 ? (idx * 100 / total) : 0,
                "completado",   sessionCompletedNaturally.getOrDefault(sessionId, false)
        );
    }

    public void stop(String sessionId) {
        ScheduledFuture<?> future = sessions.remove(sessionId);
        if (future != null) future.cancel(false);

        // Delete demo tracks
        List<Long> ids = sessionTrackIds.remove(sessionId);
        if (ids != null && !ids.isEmpty()) {
            trackRepository.deleteAllById(ids);
            logger.info("🗑️ Demo [{}] detenida. {} tracks eliminados.", sessionId.substring(0, 8), ids.size());
        }

        String viajeId = sessionViajeId.get(sessionId);

        // Always restore — demo viajes are meant to be reused
        Map<String, LocalDateTime[]> origTimes = sessionOriginalTramoTimes.get(sessionId);
        List<Tramo> tramos = sessionTramos.get(sessionId);
        if (origTimes != null && tramos != null) {
            for (Tramo t : tramos) {
                LocalDateTime[] orig = origTimes.get(t.getId());
                if (orig != null) {
                    t.setHoraSalidaProgramada(orig[0]);
                    t.setHoraLlegadaProgramada(orig[1]);
                    t.setHoraSalidaReal(orig[2]);
                    t.setHoraLlegadaReal(orig[3]);
                    t.setHoraLlegadaRealDestino(orig[4]);
                    t.setEstado(Tramo.EstadoTramo.pendiente);
                    tramoRepository.save(t);
                }
            }
        }
        Object[] origViaje = sessionOriginalViajeState.get(sessionId);
        if (origViaje != null && viajeId != null) {
            viajeRepository.findById(viajeId).ifPresent(v -> {
                v.setFechaInicioReal((LocalDateTime) origViaje[0]);
                v.setFechaFinReal((LocalDateTime) origViaje[1]);
                v.setEstado((String) origViaje[2]);
                v.setIsDemo(false);
                viajeRepository.save(v);
            });
        }

        cleanupSession(sessionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GETTERS for controller
    // ─────────────────────────────────────────────────────────────────────────

    public String getVehiculoId(String sessionId) { return sessionVehiculoId.get(sessionId); }
    public double[] getOrigen(String sessionId)   { return sessionOrigen.get(sessionId); }
    public double[] getDestino(String sessionId)  { return sessionDestino.get(sessionId); }

    public void stopByCodigoViaje(String codigoViaje) {
        List<Viaje> viajes = viajeRepository.findByCodigoViaje(codigoViaje);
        if (viajes.isEmpty()) throw new IllegalArgumentException("Viaje no encontrado: " + codigoViaje);
        String viajeId = viajes.get(0).getId();

        // Si la sesión sigue activa en memoria, delegamos al stop normal
        String sessionId = sessionViajeId.entrySet().stream()
                .filter(e -> viajeId.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (sessionId != null) {
            stop(sessionId);
            return;
        }

        // Hard-cleanup: la sesión no existe (server reiniciado, etc.)
        logger.warn("🎭 stopByCodigoViaje [{}]: sin sesión activa, haciendo hard-cleanup en BD", codigoViaje);
        Viaje viaje = viajes.get(0);

        // Eliminar tracks demo del vehículo
        if (viaje.getVehiculo() != null) {
            trackRepository.deleteByImeiAndIsDemoTrue(viaje.getVehiculo().getImei());
            logger.info("🗑️ Tracks demo eliminados para IMEI {}", viaje.getVehiculo().getImei());
        }

        // Resetear tramos a pendiente
        List<Tramo> tramos = tramoRepository.findByViajeId(viajeId);
        for (Tramo t : tramos) {
            t.setEstado(Tramo.EstadoTramo.pendiente);
            t.setHoraSalidaReal(null);
            t.setHoraLlegadaReal(null);
            t.setHoraLlegadaRealDestino(null);
            tramoRepository.save(t);
        }

        // Resetear viaje
        viaje.setEstado("pendiente");
        viaje.setFechaInicioReal(null);
        viaje.setFechaFinReal(null);
        viaje.setIsDemo(false);
        viajeRepository.save(viaje);

        logger.info("🎭 Hard-cleanup completado para viaje {}", codigoViaje);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void checkTramoBoundary(String sessionId, int nextIdx) {
        List<Integer> boundaries = sessionTramoBoundaries.get(sessionId);
        List<Tramo> tramos = sessionTramos.get(sessionId);
        if (boundaries == null || tramos == null) return;

        for (int i = 0; i < boundaries.size() - 1; i++) { // last boundary = end of demo
            if (nextIdx == boundaries.get(i)) {
                completeTramo(tramos.get(i));
                if (i + 1 < tramos.size()) {
                    startTramo(tramos.get(i + 1));
                }
            }
        }
    }

    private void completeTramo(Tramo tramo) {
        LocalDateTime now = LocalDateTime.now(PERU_ZONE);
        tramo.setEstado(Tramo.EstadoTramo.completado);
        tramo.setHoraLlegadaRealDestino(now);
        tramoRepository.save(tramo);
        logger.info("🎭 Tramo {} completado → llegada destino: {}", tramo.getOrden(), now);
    }

    private void startTramo(Tramo tramo) {
        LocalDateTime now = LocalDateTime.now(PERU_ZONE);
        tramo.setEstado(Tramo.EstadoTramo.en_curso);
        tramo.setHoraLlegadaReal(now);
        tramo.setHoraSalidaReal(now);
        tramoRepository.save(tramo);
        logger.info("🎭 Tramo {} iniciado a las {}", tramo.getOrden(), now);
    }

    private void completeDemoNaturally(String sessionId) {
        ScheduledFuture<?> future = sessions.remove(sessionId);
        if (future != null) future.cancel(false);

        sessionCompletedNaturally.put(sessionId, true);

        LocalDateTime now = LocalDateTime.now(PERU_ZONE);

        // Complete last tramo
        List<Tramo> tramos = sessionTramos.get(sessionId);
        if (tramos != null && !tramos.isEmpty()) {
            Tramo ultimo = tramos.get(tramos.size() - 1);
            ultimo.setEstado(Tramo.EstadoTramo.completado);
            ultimo.setHoraLlegadaRealDestino(now);
            tramoRepository.save(ultimo);
        }

        // Complete viaje
        String viajeId = sessionViajeId.get(sessionId);
        if (viajeId != null) {
            viajeRepository.findById(viajeId).ifPresent(v -> {
                v.setEstado("completado");
                v.setFechaFinReal(now);
                viajeRepository.save(v);
            });
        }

        // Keep tracks in DB so frontend can still see the route
        // Only remove from scheduler tracking
        sessionIndex.remove(sessionId);
        sessionTracks.remove(sessionId);

        logger.info("🎭 Demo [{}] completada. Vehículo llegó a destino.", sessionId.substring(0, 8));
    }

    private List<Track> generateSyntheticTracks(List<Tramo> tramos, String imei, List<Integer> boundaries) {
        List<Track> allTracks = new ArrayList<>();
        int targetPerTramo = Math.max(50, TARGET_TOTAL_POINTS / tramos.size());

        for (Tramo tramo : tramos) {
            var origen  = tramo.getEstablecimientoOrigen();
            var destino = tramo.getEstablecimientoDestino();

            // Resolve coordinates: entity field first, then geocerca centroid (auto-calculated and persisted)
            double[] coordsOrigen  = origen.getLatitud()  != null && origen.getLongitud()  != null
                    ? new double[]{origen.getLatitud().doubleValue(),  origen.getLongitud().doubleValue()}
                    : tramoService.resolverCoordenadasEstablecimiento(origen.getId());

            double[] coordsDestino = destino.getLatitud() != null && destino.getLongitud() != null
                    ? new double[]{destino.getLatitud().doubleValue(), destino.getLongitud().doubleValue()}
                    : tramoService.resolverCoordenadasEstablecimiento(destino.getId());

            if (coordsOrigen == null || coordsDestino == null) {
                logger.warn("🎭 Tramo {} sin coordenadas resolvibles (no hay lat/lon ni geocerca), omitiendo OSRM", tramo.getOrden());
                boundaries.add(allTracks.size());
                continue;
            }

            try {
                List<double[]> coords = fetchOsrmRoute(
                        coordsOrigen[0],  coordsOrigen[1],
                        coordsDestino[0], coordsDestino[1]
                );

                if (coords.isEmpty()) {
                    logger.warn("🎭 OSRM sin puntos para tramo {}", tramo.getOrden());
                    boundaries.add(allTracks.size());
                    continue;
                }

                List<double[]> sampled = samplePoints(coords, targetPerTramo);

                for (double[] coord : sampled) {
                    Track t = new Track();
                    t.setImei(imei);
                    t.setLatitude(coord[0]);
                    t.setLongitude(coord[1]);
                    t.setSpeed(65.0 + (Math.random() * 30)); // 65–95 km/h
                    t.setCourse(0.0);
                    t.setGpstime(0L); // set at insertion time
                    t.setIsDemo(true);
                    t.setAlarmStatus("EVALUATED");
                    allTracks.add(t);
                }

                boundaries.add(allTracks.size());
                logger.info("🎭 Tramo {}: {} puntos OSRM → {} muestreados ({}→{})",
                        tramo.getOrden(), coords.size(), sampled.size(),
                        origen.getNombre(), destino.getNombre());

            } catch (Exception e) {
                logger.error("🎭 Error OSRM tramo {}: {}", tramo.getOrden(), e.getMessage());
                boundaries.add(allTracks.size());
            }
        }

        return allTracks;
    }

    private List<double[]> fetchOsrmRoute(double origLat, double origLon, double destLat, double destLon) throws Exception {
        // OSRM uses lon,lat order
        String url = OSRM_URL
                + origLon + "," + origLat + ";"
                + destLon + "," + destLat
                + "?overview=full&geometries=geojson";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = objectMapper.readTree(response.body());
        if (!"Ok".equals(root.path("code").asText())) {
            throw new RuntimeException("OSRM code: " + root.path("code").asText());
        }

        JsonNode coordinates = root.path("routes").get(0).path("geometry").path("coordinates");
        List<double[]> result = new ArrayList<>();
        for (JsonNode coord : coordinates) {
            result.add(new double[]{coord.get(1).asDouble(), coord.get(0).asDouble()}); // [lat, lon]
        }
        return result;
    }

    private List<double[]> samplePoints(List<double[]> points, int target) {
        if (points.size() <= target) return new ArrayList<>(points);
        List<double[]> result = new ArrayList<>(target + 1);
        double step = (double) points.size() / target;
        for (int i = 0; i < target; i++) {
            result.add(points.get((int) (i * step)));
        }
        result.add(points.get(points.size() - 1)); // always include last
        return result;
    }

    private List<Track> loadHistoricalTracks(Viaje viaje) {
        var inicio = viaje.getFechaInicioReal() != null ? viaje.getFechaInicioReal() : viaje.getFechaInicioProgramada();
        var fin    = viaje.getFechaFinReal()    != null ? viaje.getFechaFinReal()    : viaje.getFechaFinProgramada();

        if (inicio == null || fin == null) return List.of();

        Long beginTime = inicio.atZone(PERU_ZONE).toEpochSecond();
        Long endTime   = fin.atZone(PERU_ZONE).toEpochSecond();

        List<Track> pool = trackRepository.findByVehiculoIdAndHearttimeBetween(
                viaje.getVehiculo().getId(), beginTime, endTime);

        if (pool.isEmpty()) {
            String imei = viaje.getVehiculo().getImei();
            pool = trackRepository.findByImei(imei).stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsDemo()))
                    .sorted(Comparator.comparingLong(Track::getGpstime))
                    .collect(Collectors.toList());
        }

        if (pool.isEmpty()) return List.of();

        Track refOrigen = pool.get(0);
        return mejorDiaDeRecorrido(pool, refOrigen);
    }

    private List<Track> mejorDiaDeRecorrido(List<Track> tracks, Track referenciaOrigen) {
        Map<Long, List<Track>> porDia = tracks.stream()
                .collect(Collectors.groupingBy(t -> t.getGpstime() / 86400));

        List<Track> mejorDia = porDia.entrySet().stream()
                .max(Comparator.comparingDouble(e -> {
                    List<Track> dia = e.getValue();
                    Track primero = dia.get(0);
                    Track ultimo  = dia.get(dia.size() - 1);
                    return haversine(primero.getLatitude(), primero.getLongitude(),
                                     ultimo.getLatitude(),  ultimo.getLongitude());
                }))
                .map(Map.Entry::getValue)
                .orElse(tracks);

        Track primero = mejorDia.get(0);
        Track ultimo  = mejorDia.get(mejorDia.size() - 1);
        double distPrimero = haversine(referenciaOrigen.getLatitude(), referenciaOrigen.getLongitude(),
                                       primero.getLatitude(), primero.getLongitude());
        double distUltimo  = haversine(referenciaOrigen.getLatitude(), referenciaOrigen.getLongitude(),
                                       ultimo.getLatitude(),  ultimo.getLongitude());
        if (distUltimo < distPrimero) {
            Collections.reverse(mejorDia);
            logger.info("🎭 Tracks históricos invertidos para coincidir con dirección del viaje");
        }

        return mejorDia;
    }

    private void cleanupSession(String sessionId) {
        sessionIndex.remove(sessionId);
        sessionTracks.remove(sessionId);
        sessionVehiculoId.remove(sessionId);
        sessionOrigen.remove(sessionId);
        sessionDestino.remove(sessionId);
        sessionTramoBoundaries.remove(sessionId);
        sessionTramos.remove(sessionId);
        sessionViajeId.remove(sessionId);
        sessionOriginalTramoTimes.remove(sessionId);
        sessionOriginalViajeState.remove(sessionId);
        sessionCompletedNaturally.remove(sessionId);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

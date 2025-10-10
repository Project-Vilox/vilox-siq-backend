package com.example.fleetIq.service;

import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.model.Establecimiento;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.repository.EstablecimientoRepository;
import com.example.fleetIq.repository.TrackRepository;
import com.example.fleetIq.repository.TramoRepository;
import com.example.fleetIq.repository.ViajeRepository;
import jakarta.persistence.OptimisticLockException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
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

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

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

            Establecimiento establecimientoOrigen = establecimientoRepository.findById(tramoDto.getEstablecimientoOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Establecimiento origen no encontrado: " + tramoDto.getEstablecimientoOrigenId()));
            tramo.setEstablecimientoOrigen(establecimientoOrigen);

            Establecimiento establecimientoDestino = establecimientoRepository.findById(tramoDto.getEstablecimientoDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Establecimiento destino no encontrado: " + tramoDto.getEstablecimientoDestinoId()));
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

        // Calcular ETA y Avance
        calcularEtaYAvance(tramo, dto);

        return dto;
    }

    private void calcularEtaYAvance(Tramo tramo, TramoDto dto) {
        try {
            // 1. Obtener el último Track registrado del vehículo del viaje por IMEI
            String imei = tramo.getViaje().getVehiculo().getImei();
            Track trackActual = trackRepository.findLatestTrackByImei(imei);

            if (trackActual == null) {
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            Establecimiento origen = tramo.getEstablecimientoOrigen();
            Establecimiento destino = tramo.getEstablecimientoDestino();

            // Validar que el establecimiento destino tenga coordenadas
            if (destino.getLatitud() == null || destino.getLongitud() == null) {
                dto.setEta(null);
                dto.setAvance(0.0);
                return;
            }

            // 2. Calcular distancia y duración desde posición actual hasta destino
            DuracionDistanciaResult resultadoActualDestino;
            try {
                resultadoActualDestino = obtenerDuracionYDistanciaGmaps(
                        trackActual.getLatitude(),
                        trackActual.getLongitude(),
                        destino.getLatitud().doubleValue(),
                        destino.getLongitud().doubleValue()
                );
            } catch (Exception e) {
                System.err.println("Falling back to OSRM due to Google Maps error: " + e.getMessage());
                resultadoActualDestino = obtenerDuracionYDistancia(
                        trackActual.getLatitude(),
                        trackActual.getLongitude(),
                        destino.getLatitud().doubleValue(),
                        destino.getLongitud().doubleValue()
                );
            }

            // 3. Calcular ETA = hora actual + tiempo estimado de llegada
            LocalDateTime horaActual = LocalDateTime.now();
            LocalDateTime eta = horaActual.plusMinutes((long) resultadoActualDestino.duracionMinutos);
            // Formatear ETA a HH:mm
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            dto.setEta(eta.format(formatter));

            // 4. Calcular distancia total del viaje (origen -> destino)
            if (origen.getLatitud() == null || origen.getLongitud() == null) {
                // Si no hay coordenadas de origen, solo establecemos ETA sin avance
                dto.setAvance(0.0);
                return;
            }

            DuracionDistanciaResult resultadoTotal;
            try {
                resultadoTotal = obtenerDuracionYDistanciaGmaps(
                        origen.getLatitud().doubleValue(),
                        origen.getLongitud().doubleValue(),
                        destino.getLatitud().doubleValue(),
                        destino.getLongitud().doubleValue()
                );
            } catch (Exception e) {
                System.err.println("Falling back to OSRM due to Google Maps error: " + e.getMessage());
                resultadoTotal = obtenerDuracionYDistancia(
                        origen.getLatitud().doubleValue(),
                        origen.getLongitud().doubleValue(),
                        destino.getLatitud().doubleValue(),
                        destino.getLongitud().doubleValue()
                );
            }

            // 5. Calcular distancia recorrida
            double distanciaTotal = resultadoTotal.distanciaMetros;
            double distanciaRestante = resultadoActualDestino.distanciaMetros;
            double distanciaRecorrida = distanciaTotal - distanciaRestante;

            // 6. Calcular porcentaje de avance
            double avance = 0.0;
            if (distanciaTotal > 0) {
                avance = (distanciaRecorrida / distanciaTotal) * 100.0;
                // Asegurar que el avance esté entre 0 y 100
                avance = Math.max(0.0, Math.min(100.0, avance));
                // Redondear a 1 decimal
                avance = Math.round(avance * 10.0) / 10.0;
            }
            dto.setAvance(avance);

        } catch (Exception e) {
            // En caso de error, setear valores por defecto y loguear el error
            dto.setEta(null);
            dto.setAvance(0.0);
            System.err.println("Error al calcular ETA y avance para tramo " + tramo.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DuracionDistanciaResult obtenerDuracionYDistancia(
            double origenLat, double origenLon,
            double destinoLat, double destinoLon) throws Exception {

        String coordenadasOrigen = String.format(Locale.US, "%.6f,%.6f", origenLon, origenLat);
        String coordenadasDestino = String.format(Locale.US, "%.6f,%.6f", destinoLon, destinoLat);
        // Construir URL de OSRM
        String url =
                "https://router.project-osrm.org/route/v1/driving/" + coordenadasOrigen + ";" + coordenadasDestino + "?overview=false";

        // Crear cliente HTTP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        // Enviar petición y obtener respuesta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());

        if (json.has("routes") && json.getJSONArray("routes").length() > 0) {
            JSONObject route = json.getJSONArray("routes").getJSONObject(0);
            double duracionSegundos = route.getDouble("duration");
            double distanciaMetros = route.getDouble("distance");
            double duracionMinutos = duracionSegundos / 60.0;

            return new DuracionDistanciaResult(duracionMinutos, distanciaMetros);
        } else {
            throw new RuntimeException("No se pudo calcular la ruta entre los puntos especificados");
        }
    }

    private DuracionDistanciaResult obtenerDuracionYDistanciaGmaps(
            double origenLat, double origenLon,
            double destinoLat, double destinoLon) throws Exception {

        // Construir cuerpo JSON para Routes API
        JSONArray requestBody = new JSONArray();
        JSONObject route = new JSONObject();
        route.put("origin", new JSONObject()
                .put("location", new JSONObject()
                        .put("latLng", new JSONObject()
                                .put("latitude", origenLat)
                                .put("longitude", origenLon))));
        route.put("destination", new JSONObject()
                .put("location", new JSONObject()
                        .put("latLng", new JSONObject()
                                .put("latitude", destinoLat)
                                .put("longitude", destinoLon))));
        route.put("travelMode", "DRIVE");
        requestBody.put(route);

        // Construir URL de Google Maps Routes API con campos específicos
        String url = "https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix?fields=distanceMeters,duration&key=" + googleMapsApiKey;

        // Crear cliente HTTP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        // Enviar petición y obtener respuesta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Loguear detalles para depuración
        System.out.println("Request URL: " + url);
        System.out.println("Request Body: " + requestBody.toString());
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());

        // Verificar si la respuesta es un JSON válido
        try {
            JSONObject json = new JSONObject(response.body());

            // Verificar si hay error en la respuesta
            if (json.has("error")) {
                String errorMessage = json.getJSONObject("error").getString("message");
                throw new RuntimeException("Error en la respuesta de Routes API: " + errorMessage + " | Status Code: " + response.statusCode());
            }

            // Extraer distancia y duración
            JSONObject element = json.getJSONArray("rows").getJSONObject(0);
            double distanciaMetros = element.getDouble("distanceMeters");
            String durationStr = element.getString("duration");
            // Convertir duración de formato "600s" a minutos
            double duracionSegundos = Double.parseDouble(durationStr.replace("s", ""));
            double duracionMinutos = duracionSegundos / 60.0;

            return new DuracionDistanciaResult(duracionMinutos, distanciaMetros);
        } catch (JSONException e) {
            // Si falla el parseo JSON, loguear y usar OSRM como fallback
            System.err.println("Invalid JSON response from Routes API: " + response.body());
            throw new RuntimeException("Invalid JSON response from Routes API: " + e.getMessage());
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
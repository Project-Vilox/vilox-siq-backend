package com.example.fleetIq.service;

import com.example.fleetIq.model.Track;
import com.example.fleetIq.repository.TrackRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio para obtener tracks GPS desde el endpoint /api/playback de la API de Protrack.
 */
@Service
public class PlaybackTrackServiceImpl implements PlaybackTrackService {

    private static final String API_BASE_URL = "https://api.protrack365.com";

    @Autowired
    private TokenCache tokenCache;

    @Autowired
    private TrackRepository trackRepository; // Agregado para la persistencia

    /**
     * Obtiene los tracks GPS para un IMEI en un rango de tiempo específico desde el endpoint /api/playback y los persiste si no existen.
     *
     * @param imei      El IMEI del dispositivo.
     * @param beginTime Timestamp Unix de inicio (en segundos).
     * @param endTime   Timestamp Unix de fin (en segundos).
     * @return Lista de objetos Track con los datos GPS.
     * @throws Exception Si ocurre un error al comunicarse con la API o al procesar la respuesta.
     */
    @Override
    public List<Track> getPlaybackTracksByImeiAndTime(String imei, Long beginTime, Long endTime) throws Exception {
        List<Track> tracks = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        String accessToken = tokenCache.getAccessToken();
        boolean hasMore = true;

        while (hasMore) {
            // Construir la URL de la solicitud
            String url = String.format("%s/api/playback?access_token=%s&imei=%s&begintime=%d&endtime=%d",
                    API_BASE_URL, accessToken, imei, beginTime, endTime);
            try {
                // Realizar la solicitud GET
                String response = restTemplate.getForObject(url, String.class);
                if (response == null) {
                    throw new Exception("Respuesta de la API es nula");
                }

                // Parsear la respuesta JSON
                JSONObject json = new JSONObject(response);
                int code = json.getInt("code");
                if (code != 0) {
                    String message = json.optString("message", "Error desconocido de la API");
                    throw new Exception(String.format("Error de la API (código %d): %s", code, message));
                }

                // Obtener el campo record
                String record = json.getString("record");
                if (record.isEmpty()) {
                    hasMore = false;
                    break;
                }

                // Parsear los registros separados por punto y coma
                String[] records = record.split(";");
                for (String data : records) {
                    String[] fields = data.split(",");
                    if (fields.length == 5) {
                        Track track = new Track();
                        track.setImei(imei);
                        track.setLongitude(Double.parseDouble(fields[0]));
                        track.setLatitude(Double.parseDouble(fields[1]));
                        track.setGpstime(Long.parseLong(fields[2]));
                        track.setSpeed(Double.parseDouble(fields[3]));
                        track.setCourse(Double.parseDouble(fields[4]));
                        // Los demás campos se mantienen en null o con valores por defecto según el modelo

                        // Lógica de persistencia: verificar si no existe por gpstime (sustituto de hearttime), latitude y longitude en el día
                        LocalDate trackDay = Instant.ofEpochSecond(track.getGpstime()).atZone(ZoneId.of("America/Lima")).toLocalDate(); // Zona -05:00
                        Long startOfDay = trackDay.atStartOfDay(ZoneId.of("America/Lima")).toInstant().getEpochSecond();
                        Long endOfDay = trackDay.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant().getEpochSecond() - 1;

                        // Verificar si existe un track con el mismo imei, gpstime, latitude y longitude en el día
                        Track existingTrack = trackRepository.findByImeiAndGpstimeAndLatitudeAndLongitudeAndGpstimeBetween(
                                track.getImei(), track.getGpstime(), track.getLatitude(), track.getLongitude(), startOfDay, endOfDay);

                        if (existingTrack == null) {
                            trackRepository.save(track);
                            System.out.println("Track guardado: " + track);
                        } else {
                            System.out.println("Track ya existe para el día " + trackDay + ": " + existingTrack);
                        }

                        tracks.add(track);
                    } else {
                        System.out.println("Advertencia: Registro inválido ignorado: " + data);
                    }
                }

                // Manejar paginación: si se devolvieron 1000 registros, continuar con el siguiente lote
                if (records.length == 1000) {
                    beginTime = Long.parseLong(records[records.length - 1].split(",")[2]) + 1;
                } else {
                    hasMore = false;
                }
            } catch (RestClientException e) {
                throw new Exception("Error HTTP al comunicarse con la API: " + e.getMessage());
            } catch (Exception e) {
                throw new Exception("Error al procesar la respuesta de la API: " + e.getMessage());
            }
        }

        return tracks;
    }
}
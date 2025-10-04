package com.example.fleetIq.service;

import com.example.fleetIq.dto.TrackDto;
import com.example.fleetIq.model.Device;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Vehiculo;
import com.example.fleetIq.repository.DeviceRepository;
import com.example.fleetIq.repository.TrackRepository;
import com.example.fleetIq.repository.VehiculoRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrackServiceImpl implements TrackService {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private TokenCache tokenCache;

    @Override
    public void fetchAndSaveTracks(Long beginTime, Long endTime) throws Exception {
        String accessToken = tokenCache.getAccessToken();
        List<Device> devices = deviceRepository.findAll();
        if (devices.isEmpty()) {
            throw new Exception("No se encontraron dispositivos registrados.");
        }

        StringBuilder imeis = new StringBuilder();
        for (Device device : devices) {
            if (imeis.length() > 0) imeis.append(",");
            imeis.append(device.getImei());
        }

        // Contador para los tracks registrados
        int savedTracksCount = fetchAndSaveForImeis(imeis.toString(), beginTime, endTime, accessToken);

        // Log en consola con la cantidad de registros guardados
        System.out.println("🚪 Se termina proceso automático de extracción GPS Tracks desde api protrack365. Registros guardados en la tabla tracks: " + savedTracksCount);
    }

    //@Scheduled(fixedRate = 30000)
    public void scheduleFetchAndSaveTracks() throws Exception {
        Long beginTime = Instant.now().getEpochSecond() - 24 * 60 * 60;
        Long endTime = Instant.now().getEpochSecond();
        LocalDateTime fechaHoraActual = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String fechaHoraFormateada = fechaHoraActual.format(formato);
        System.out.println("✅ Se inicia proceso automático de extracción GPS Tracks desde api protrack365 ::: " + fechaHoraFormateada);
        fetchAndSaveTracks(beginTime, endTime);
    }

    @Override
    public List<Track> getTracksByImei(String imei, Long beginTime, Long endTime) {
        if (beginTime == null) beginTime = 0L;
        if (endTime == null) endTime = System.currentTimeMillis() / 1000;
        return trackRepository.findByImeiAndTimeBetween(imei, beginTime, endTime);
    }

    @Override
    public List<TrackDto> getTracksByVehiculoIdAndHearttimeBetween(String vehiculoId, Long beginTime, Long endTime) {
        List<Track> tracks = trackRepository.findByVehiculoIdAndHearttimeBetween(vehiculoId, beginTime, endTime);
        return tracks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public TrackDto getLatestTrackByVehiculoId(String vehiculoId) {
        List<Track> tracks = trackRepository.findLatestTrackByVehiculoId(vehiculoId);
        return tracks.isEmpty() ? null : convertToDto(tracks.get(0));
    }

    private int fetchAndSaveForImeis(String imeis, Long beginTime, Long endTime, String accessToken) throws Exception {
        int page = 1;
        int pagesize = 500;
        boolean hasMore = true;
        int savedTracksCount = 0; // Contador para los tracks guardados

        while (hasMore) {
            URL url = new URL("https://api.protrack365.com/api/track?access_token=" + accessToken + "&imeis=" + imeis + "&begin_time=" + beginTime + "&end_time=" + endTime + "&page=" + page + "&pagesize=" + pagesize);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();
                JSONObject json = new JSONObject(response.toString());
                if (json.getInt("code") == 0) {
                    JSONArray records = json.getJSONArray("record");
                    if (records.length() < pagesize) hasMore = false;
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        Track track = new Track();
                        track.setImei(record.getString("imei"));
                        track.setGpstime(record.getLong("gpstime"));
                        track.setHearttime(record.optLong("hearttime", 0L));
                        track.setSystemtime(record.optLong("systemtime", 0L));
                        track.setServertime(record.optLong("servertime", 0L));
                        track.setLatitude(record.getDouble("latitude"));
                        track.setLongitude(record.getDouble("longitude"));
                        track.setSpeed(record.optDouble("speed", 0.0));
                        track.setCourse(record.optDouble("course", 0.0));
                        track.setAcctime(record.optLong("acctime", 0L));
                        track.setAccstatus(record.optBoolean("accstatus", false));
                        track.setDoorstatus(record.optInt("doorstatus", 0));
                        track.setChargestatus(record.optInt("chargestatus", 0));
                        track.setOilpowerstatus(record.optInt("oilpowerstatus", 0));
                        track.setDefencestatus(record.optInt("defencestatus", 0));
                        track.setDatastatus(record.optInt("datastatus", 0));
                        track.setBattery(record.optDouble("battery", 0.0));
                        track.setMileage(record.optLong("mileage", 0L));
                        track.setTodaymileage(record.optLong("todaymileage", 0L));
                        track.setExternalpower(record.optString("externalpower", ""));
                        track.setFuel(record.optString("fuel", ""));
                        track.setFueltime(record.optLong("fueltime", 0L));
                        track.setTemperature(record.optString("temperature", "[]"));
                        track.setTemperaturetime(record.optLong("temperaturetime", 0L));
                        if (trackRepository.findByImeiAndTimeBetween(track.getImei(), track.getGpstime(), track.getGpstime()).isEmpty()) {
                            trackRepository.save(track);
                            savedTracksCount++; // Incrementar el contador
                        }
                    }
                    page++;
                } else {
                    throw new Exception("Error de API: " + json.getString("message"));
                }
            } else {
                throw new Exception("Error HTTP: " + conn.getResponseCode());
            }
        }
        return savedTracksCount; // Devolver el número de tracks guardados
    }

    private TrackDto convertToDto(Track track) {
        TrackDto dto = new TrackDto();
        dto.setId(track.getId());
        dto.setImei(track.getImei());
        dto.setGpstime(track.getGpstime());
        dto.setHearttime(track.getHearttime());
        dto.setSystemtime(track.getSystemtime());
        dto.setServertime(track.getServertime());
        dto.setLatitude(track.getLatitude());
        dto.setLongitude(track.getLongitude());
        dto.setSpeed(track.getSpeed());
        dto.setCourse(track.getCourse());
        dto.setAcctime(track.getAcctime());
        dto.setAccstatus(track.getAccstatus());
        dto.setDoorstatus(track.getDoorstatus());
        dto.setChargestatus(track.getChargestatus());
        dto.setOilpowerstatus(track.getOilpowerstatus());
        dto.setDefencestatus(track.getDefencestatus());
        dto.setDatastatus(track.getDatastatus());
        dto.setBattery(track.getBattery());
        dto.setMileage(track.getMileage());
        dto.setTodaymileage(track.getTodaymileage());
        dto.setExternalpower(track.getExternalpower());
        dto.setFuel(track.getFuel());
        dto.setFueltime(track.getFueltime());
        dto.setTemperature(track.getTemperature());
        dto.setTemperaturetime(track.getTemperaturetime());
        dto.setCreationDate(track.getCreationDate());
        return dto;
    }
}
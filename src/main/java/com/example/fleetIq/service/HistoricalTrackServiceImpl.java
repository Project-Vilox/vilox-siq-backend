package com.example.fleetIq.service;

import com.example.fleetIq.model.HistoricalTrack;
import com.example.fleetIq.repository.DeviceRepository;
import com.example.fleetIq.repository.HistoricalTrackRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@Service
public class HistoricalTrackServiceImpl implements HistoricalTrackService {

    @Autowired
    private HistoricalTrackRepository historicalTrackRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private String getAccessToken() throws Exception {
        long time = Instant.now().getEpochSecond();
        String md5Password = calculateMD5("expert2023");
        String signatureInput = md5Password + time;
        String signature = calculateMD5(signatureInput);

        URL url = new URL("http://api.protrack365.com/api/authorization?time=" + time + "&account=expertsac&signature=" + signature);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            JSONObject json = new JSONObject(response.toString());
            if (json.getInt("code") == 0) {
                return json.getJSONObject("record").getString("access_token");
            } else {
                throw new Exception("Error de API: " + json.getString("message"));
            }
        } else {
            throw new Exception("Error HTTP: " + conn.getResponseCode());
        }
    }

    private String calculateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Fallo en el cálculo de MD5", e);
        }
    }

    @Override
    public void fetchAndSaveHistoricalTracks(Long beginTime, Long endTime) throws Exception {
        String accessToken = getAccessToken();
        List<com.example.fleetIq.model.Device> devices = deviceRepository.findAll();
        if (devices.isEmpty()) {
            throw new Exception("No se encontraron dispositivos registrados.");
        }

        // Recopilar todos los IMEIs en una lista
        StringBuilder imeis = new StringBuilder();
        for (com.example.fleetIq.model.Device device : devices) {
            if (imeis.length() > 0) imeis.append(",");
            imeis.append(device.getImei());
        }

        fetchAndSaveForImeis(imeis.toString(), beginTime, endTime, accessToken);
    }

    private void fetchAndSaveForImeis(String imeis, Long beginTime, Long endTime, String accessToken) throws Exception {
        int page = 1;
        int pagesize = 500; // Máximo permitido por la API
        boolean hasMore = true;

        while (hasMore) {
            URL url = new URL("http://api.protrack365.com/api/track?access_token=" + accessToken + "&imeis=" + imeis + "&begin_time=" + beginTime + "&end_time=" + endTime + "&page=" + page + "&pagesize=" + pagesize);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();
                JSONObject json = new JSONObject(response.toString());
                if (json.getInt("code") == 0) {
                    JSONArray records = json.getJSONArray("record");
                    if (records.length() < pagesize) {
                        hasMore = false;
                    }
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        HistoricalTrack track = new HistoricalTrack();
                        track.setImei(record.getString("imei")); // Obtener imei del registro
                        track.setTime(record.getLong("time"));
                        track.setLatitude(record.getDouble("latitude"));
                        track.setLongitude(record.getDouble("longitude"));
                        track.setSpeed(record.optDouble("speed", 0.0));
                        track.setDirection(record.optDouble("direction", 0.0));
                        track.setAccStatus(record.optBoolean("accstatus", false));
                        track.setVoltage(record.optDouble("voltage", 0.0));
                        // Guardar solo si no existe
                        if (historicalTrackRepository.findByImeiAndTimeBetween(track.getImei(), track.getTime(), track.getTime()).isEmpty()) {
                            historicalTrackRepository.save(track);
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
    }
}
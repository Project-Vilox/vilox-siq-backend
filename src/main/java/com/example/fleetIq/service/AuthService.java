package com.example.fleetIq.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;

@Service
public class AuthService {

    public String getAccessToken() throws Exception {
        long time = Instant.now().getEpochSecond();
        String md5Password = calculateMD5("expert2023"); // Asegúrate de que esta contraseña sea correcta
        String signatureInput = md5Password + time;
        String signature = calculateMD5(signatureInput);

        URL url = new URL("https://api.protrack365.com/api/authorization?time=" + time + "&account=expertsac&signature=" + signature);
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
                throw new Exception("Error de API al obtener token: " + json.getString("message"));
            }
        } else {
            throw new Exception("Error HTTP al obtener token: " + conn.getResponseCode());
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
}
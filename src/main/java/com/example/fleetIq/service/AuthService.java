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
        // Primera solicitud para obtener el timestamp actual del servidor
        long currentTime = getCurrentServerTime();
        
        // Usar el timestamp del servidor con una pequeña ventana de tolerancia
        long time = currentTime - 30; // Usar timestamp del servidor menos 30 segundos
        
        System.out.println("🕒 Timestamp del servidor obtenido: " + currentTime);
        System.out.println("🕒 Timestamp para autenticación: " + time);
        System.out.println("🕒 Fecha correspondiente: " + java.time.Instant.ofEpochSecond(time));
        
        String md5Password = calculateMD5("expert2023");
        String signatureInput = md5Password + time;
        String signature = calculateMD5(signatureInput);

        String urlString = "https://api.protrack365.com/api/authorization?time=" + time + "&account=expertsac&signature=" + signature;
        System.out.println("🔗 URL de autenticación: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        System.out.println("📡 Código de respuesta HTTP: " + responseCode);
        
        if (responseCode == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            System.out.println("📋 Respuesta completa de la API: " + response.toString());
            
            JSONObject json = new JSONObject(response.toString());
            if (json.getInt("code") == 0) {
                String token = json.getJSONObject("record").getString("access_token");
                System.out.println("✅ Token obtenido exitosamente");
                return token;
            } else {
                throw new Exception("Error de API al obtener token: " + json.getString("message"));
            }
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            System.out.println("❌ Respuesta de error: " + errorResponse.toString());
            throw new Exception("Error HTTP al obtener token: " + responseCode + " - " + errorResponse.toString());
        }
    }

    // Método para obtener el timestamp actual del servidor
    private long getCurrentServerTime() throws Exception {
        // Hacer una solicitud dummy para obtener el timestamp del servidor
        long dummyTime = System.currentTimeMillis() / 1000;
        String md5Password = calculateMD5("expert2023");
        String signatureInput = md5Password + dummyTime;
        String signature = calculateMD5(signatureInput);

        String urlString = "https://api.protrack365.com/api/authorization?time=" + dummyTime + "&account=expertsac&signature=" + signature;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            JSONObject json = new JSONObject(response.toString());
            if (json.getInt("code") == 10014) { // Error de tiempo esperado
                String message = json.getString("message");
                // Extraer el timestamp del mensaje: "request time error, current server time is 1759598659 (in unix timestamp)"
                String timestampStr = message.replaceAll(".*current server time is (\\d+).*", "$1");
                return Long.parseLong(timestampStr);
            }
        }
        
        // Si no podemos obtener el timestamp del servidor, usar estimación
        return System.currentTimeMillis() / 1000 + 31535920; // Agregar aproximadamente 1 año
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
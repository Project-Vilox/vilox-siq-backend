package com.example.fleetIq.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String PERU_TIMEZONE = "America/Lima";

    public String getAccessToken() throws Exception {
        logger.info("🔄 Iniciando proceso de autenticación...");

        // Obtener el timestamp del servidor de la API
        long serverTime = getCurrentServerTime();

        // Mostrar información de debug sobre los tiempos
        logTimeInformation(serverTime);

        // Usar el timestamp del servidor directamente
        long authTime = serverTime;

        String md5Password = calculateMD5("expert2023");
        String signatureInput = md5Password + authTime;
        String signature = calculateMD5(signatureInput);

        String urlString = "https://api.protrack365.com/api/authorization?time=" + authTime + "&account=expertsac&signature=" + signature;
        logger.info("🔗 URL de autenticación: {}", urlString);

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        logger.info("📡 Código de respuesta HTTP: {}", responseCode);

        if (responseCode == 200) {
            String response = readResponse(conn.getInputStream());
            logger.info("📋 Respuesta completa de la API: {}", response);

            JSONObject json = new JSONObject(response);
            if (json.getInt("code") == 0) {
                String token = json.getJSONObject("record").getString("access_token");
                logger.info("✅ Token obtenido exitosamente");
                return token;
            } else {
                String errorMessage = json.getString("message");
                logger.error("❌ Error de API al obtener token: {}", errorMessage);
                throw new Exception("Error de API al obtener token: " + errorMessage);
            }
        } else {
            String errorResponse = readResponse(conn.getErrorStream());
            logger.error("❌ Error HTTP {}: {}", responseCode, errorResponse);
            throw new Exception("Error HTTP al obtener token: " + responseCode + " - " + errorResponse);
        }
    }

    /**
     * Obtiene el timestamp actual del servidor de la API haciendo una solicitud inicial
     * que deliberadamente fallará por timestamp incorrecto, pero nos devolverá el tiempo correcto
     */
    private long getCurrentServerTime() throws Exception {
        logger.info("🕐 Obteniendo timestamp del servidor...");

        // Usar timestamp local como punto de partida
        long localTime = System.currentTimeMillis() / 1000;

        try {
            // Hacer solicitud dummy para obtener el tiempo del servidor
            String md5Password = calculateMD5("expert2023");
            String signatureInput = md5Password + localTime;
            String signature = calculateMD5(signatureInput);

            String urlString = "https://api.protrack365.com/api/authorization?time=" + localTime + "&account=expertsac&signature=" + signature;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String response = readResponse(conn.getInputStream());
            JSONObject json = new JSONObject(response);

            // Si el código es 10014, es error de tiempo y contiene el timestamp del servidor
            if (json.getInt("code") == 10014) {
                String message = json.getString("message");
                // Extraer timestamp del mensaje: "request time error, current server time is 1759618794 (in unix timestamp)"
                if (message.contains("current server time is")) {
                    String timestampStr = message.replaceAll(".*current server time is (\\d+).*", "$1");
                    long serverTime = Long.parseLong(timestampStr);
                    logger.info("✅ Timestamp del servidor extraído: {}", serverTime);
                    return serverTime;
                }
            }

            // Si no hay error de tiempo, significa que nuestro tiempo local está bien sincronizado
            if (json.getInt("code") == 0) {
                logger.info("✅ Tiempo local sincronizado con servidor");
                return localTime;
            }

        } catch (Exception e) {
            logger.warn("⚠️  No se pudo obtener timestamp del servidor: {}", e.getMessage());
        }

        // Fallback: usar tiempo local
        logger.info("🕐 Usando timestamp local como fallback: {}", localTime);
        return localTime;
    }

    /**
     * Muestra información detallada sobre los tiempos para debugging
     */
    private void logTimeInformation(long serverTimestamp) {
        try {
            // Tiempo del servidor en UTC
            Instant serverInstant = Instant.ofEpochSecond(serverTimestamp);

            // Tiempo del servidor en zona horaria de Perú
            ZonedDateTime serverTimeInPeru = serverInstant.atZone(ZoneId.of(PERU_TIMEZONE));

            // Tiempo local actual
            ZonedDateTime localTimeInPeru = ZonedDateTime.now(ZoneId.of(PERU_TIMEZONE));

            // Tiempo local en timestamp
            long localTimestamp = System.currentTimeMillis() / 1000;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

            logger.info("🕒 Información de tiempos:");
            logger.info("   📡 Servidor API timestamp: {} ({})", serverTimestamp, serverTimeInPeru.format(formatter));
            logger.info("   🖥️  Local PC timestamp: {} ({})", localTimestamp, localTimeInPeru.format(formatter));
            logger.info("   ⏰ Diferencia: {} segundos", Math.abs(serverTimestamp - localTimestamp));

        } catch (Exception e) {
            logger.warn("⚠️  Error al mostrar información de tiempos: {}", e.getMessage());
        }
    }

    /**
     * Lee la respuesta de un InputStream
     */
    private String readResponse(java.io.InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
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
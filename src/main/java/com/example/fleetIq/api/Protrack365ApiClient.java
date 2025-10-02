package com.example.fleetIq.api;

import com.example.fleetIq.config.ApiConfig;
import com.example.fleetIq.model.AuthResponse;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Protrack365ApiClient {

    public AuthResponse authenticate() throws Exception {
        URL url = new URL(ApiConfig.API_BASE_URI + ApiConfig.LOGIN_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        configureConnection(conn);
        sendRequest(conn);

        String response = readResponse(conn);
        return parseAuthResponse(response);
    }

    private void configureConnection(HttpURLConnection conn) throws Exception {
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
    }

    private void sendRequest(HttpURLConnection conn) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("account", ApiConfig.ACCOUNT);
        payload.put("password", ApiConfig.PASSWORD);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes("UTF-8");
            os.write(input, 0, input.length);
        }
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to get access token. HTTP error code: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private AuthResponse parseAuthResponse(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        String accessToken = jsonObject.getString("access_token");
        int expiresIn = jsonObject.getInt("expires_in");

        System.out.println("Access Token: " + accessToken);
        System.out.println("Expires In: " + expiresIn + " seconds");

        return new AuthResponse(accessToken, expiresIn);
    }
}
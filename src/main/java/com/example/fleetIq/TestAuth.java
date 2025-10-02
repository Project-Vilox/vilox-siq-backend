package com.example.fleetIq;


import com.example.fleetIq.api.Protrack365ApiClient;
import com.example.fleetIq.model.AuthResponse;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class TestAuth {
    private static final String API_BASE_URI = "http://api.protrack365.com";
    private static final String AUTH_ENDPOINT = "/api/authorization";
    private static final String ACCOUNT = "expertsac";
    private static final String PASSWORD = "expert2023";

    // Utility method to calculate MD5 hash
    private static String calculateMD5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String getAccessToken() throws Exception {
        // Generate current Unix timestamp
        long time = System.currentTimeMillis() / 1000L;

        // Calculate signature: md5(md5(password) + time)
        String md5Password = calculateMD5(PASSWORD);
        String signatureInput = md5Password + time;
        String signature = calculateMD5(signatureInput);

        // Construct the URL with query parameters
        String urlString = String.format("%s%s?time=%d&account=%s&signature=%s",
                API_BASE_URI, AUTH_ENDPOINT, time, ACCOUNT, signature);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set the request method and properties
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        // Read the response
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            throw new RuntimeException("HTTP error code: " + responseCode + ", Response: " + response.toString());
        }

        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());
        int code = jsonResponse.getInt("code");
        if (code != 0) {
            String message = jsonResponse.optString("message", "No message provided");
            throw new RuntimeException("API error: code=" + code + ", message=" + message);
        }

        // Extract access token from nested "record" object
        JSONObject record = jsonResponse.getJSONObject("record");
        String accessToken = record.getString("access_token");
        int expiresIn = record.getInt("expires_in");
        System.out.println("Access Token: " + accessToken);
        System.out.println("Expires In: " + expiresIn + " seconds");

        return accessToken;
    }

    public static void main(String[] args) {
        try {
            String token = getAccessToken();
            System.out.println("Successfully retrieved access token: " + token);
        } catch (Exception e) {
            System.err.println("Error retrieving access token: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
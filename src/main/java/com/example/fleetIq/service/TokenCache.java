package com.example.fleetIq.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TokenCache {

    private String accessToken;
    private long expirationTime; // Tiempo de expiración en milisegundos
    private final Object lock = new Object(); // Para sincronización

    private final AuthService authService; // ← Cambiado a AuthService

    public TokenCache(AuthService authService) {
        this.authService = authService;
        this.expirationTime = 0; // Inicialmente expirado
        renewToken(); // Obtener token al iniciar
    }

    public String getAccessToken() {
        synchronized (lock) {
            if (System.currentTimeMillis() >= expirationTime) {
                renewToken();
            }
            return accessToken;
        }
    }

    @Scheduled(fixedRate = 1800000) // 30 minutos = 1,800,000 milisegundos
    public void renewToken() {
        synchronized (lock) {
            try {
                System.out.println("🔄 Intentando renovar token...");
                this.accessToken = authService.getAccessToken();
                this.expirationTime = System.currentTimeMillis() + 1800000; // 30 minutos
                System.out.println("🔑 Token renovado exitosamente a las: " + Instant.now());
            } catch (Exception e) {
                System.err.println("❌ Error al renovar el token: " + e.getMessage());
                e.printStackTrace(); // Imprimir stack trace completo para depuración
                this.expirationTime = System.currentTimeMillis() + 60000; // Reintentar en 1 minuto
            }
        }
    }
}
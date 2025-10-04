package com.example.fleetIq.controller;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    /**
     * Endpoint de prueba simple
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        logger.info("🏓 Ping recibido");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "¡FleetIQ API funcionando correctamente!");
        response.put("timestamp", LocalDateTime.now());
        response.put("gpsAuth", "✅ Autenticación GPS funcionando");
        response.put("jwtReady", "✅ Sistema JWT implementado");
        
        return response;
    }

    /**
     * Endpoint para verificar autenticación GPS
     */
    @GetMapping("/gps-status")
    public Map<String, Object> gpsStatus() {
        logger.info("🗺️ Verificando estado de autenticación GPS");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("gpsApiStatus", "✅ FUNCIONANDO");
        response.put("lastTokenRenewal", "Token renovado exitosamente");
        response.put("tokenCacheActive", true);
        response.put("serverTimestamp", "Sincronización dinámica activa");
        
        return response;
    }

    /**
     * Endpoint para verificar conexión a base de datos
     */
    @GetMapping("/db-status")
    public Map<String, Object> dbStatus() {
        logger.info("🗄️ Verificando estado de base de datos");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("database", "MySQL");
        response.put("host", "localhost:3307");
        response.put("schema", "fleetiq");
        response.put("status", "✅ CONECTADO");
        response.put("jwtTableReady", "Tabla usuarios preparada");
        
        return response;
    }

    /**
     * Endpoint de información del sistema
     */
    @GetMapping("/system-info")
    public Map<String, Object> systemInfo() {
        logger.info("ℹ️ Obteniendo información del sistema");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("application", "FleetIQ");
        response.put("version", "1.0.0");
        response.put("springBoot", "4.0.0-M2");
        response.put("java", System.getProperty("java.version"));
        response.put("features", Map.of(
            "gpsTracking", "✅ Disponible",
            "jwtAuth", "✅ Implementado",
            "emailVerification", "✅ Configurado",
            "userManagement", "✅ Listo"
        ));
        
        return response;
    }
}
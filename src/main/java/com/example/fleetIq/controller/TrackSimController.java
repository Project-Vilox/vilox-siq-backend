package com.example.fleetIq.controller;

import com.example.fleetIq.service.TrackSimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/track/sim")
@CrossOrigin(originPatterns = "*")
public class TrackSimController {

    @Autowired
    private TrackSimService trackSimService;

    @PostMapping("/init/{viajeId}")
    public ResponseEntity<Object> init(@PathVariable String viajeId) {
        try {
            Map<String, Object> result = trackSimService.init(viajeId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/next/{sessionId}")
    public ResponseEntity<Object> next(@PathVariable String sessionId) {
        try {
            Map<String, Object> result = trackSimService.next(sessionId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Object> status(@PathVariable String sessionId) {
        if (!trackSimService.sessionExists(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "exists",      true,
                "pasoActual",  trackSimService.getPasoActual(sessionId),
                "totalPasos",  trackSimService.getTotalPasos(sessionId)
        ));
    }

    @PostMapping("/init-demo/{viajeId}")
    public ResponseEntity<Object> initDemo(
            @PathVariable String viajeId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int intervalSeconds) {
        try {
            return ResponseEntity.ok(trackSimService.initDemo(viajeId, intervalSeconds));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/demo/{sessionId}")
    public ResponseEntity<Object> stopDemo(@PathVariable String sessionId) {
        trackSimService.stopDemo(sessionId, true);
        return ResponseEntity.ok(Map.of("mensaje", "Demo detenido"));
    }

    @GetMapping("/demo/status")
    public ResponseEntity<Object> demoStatus() {
        String active = trackSimService.getActiveDemoSession();
        if (active == null) return ResponseEntity.ok(Map.of("active", false));
        return ResponseEntity.ok(Map.of(
            "active", true,
            "sessionId", active,
            "pasoActual", trackSimService.getPasoActual(active),
            "totalPasos", trackSimService.getTotalPasos(active)
        ));
    }

    @GetMapping("/demo/status/{viajeId}")
    public ResponseEntity<Object> demoStatusByViaje(@PathVariable String viajeId) {
        return ResponseEntity.ok(trackSimService.getDemoStatusByViaje(viajeId));
    }

    @GetMapping("/replay/{sessionId}")
    public ResponseEntity<Object> replay(@PathVariable String sessionId) {
        try {
            return ResponseEntity.ok(trackSimService.getReplayPoints(sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Object> cleanup(@PathVariable String sessionId) {
        try {
            trackSimService.cleanup(sessionId);
            return ResponseEntity.ok(Map.of("mensaje", "Sesión finalizada"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

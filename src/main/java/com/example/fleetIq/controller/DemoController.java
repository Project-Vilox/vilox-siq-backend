package com.example.fleetIq.controller;

import com.example.fleetIq.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @Autowired
    private DemoService demoService;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String, Object> body) {
        try {
            String viajeId = (String) body.get("viajeId");
            int intervalo = body.containsKey("intervalo") ? (int) body.get("intervalo") : 10;
            String sessionId = demoService.start(viajeId, intervalo);
            String vehiculoId = demoService.getVehiculoId(sessionId);
            double[] origen = demoService.getOrigen(sessionId);
            double[] destino = demoService.getDestino(sessionId);
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "vehiculoId", vehiculoId,
                "origenLat", origen[0], "origenLon", origen[1],
                "destinoLat", destino[0], "destinoLon", destino[1]
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<?> status(@PathVariable String sessionId) {
        return ResponseEntity.ok(demoService.status(sessionId));
    }

    @PostMapping("/stop/{sessionId}")
    public ResponseEntity<?> stop(@PathVariable String sessionId) {
        demoService.stop(sessionId);
        return ResponseEntity.ok(Map.of("stopped", true));
    }

    @PostMapping("/stop-by-codigo/{codigoViaje}")
    public ResponseEntity<?> stopByCodigo(@PathVariable String codigoViaje) {
        try {
            demoService.stopByCodigoViaje(codigoViaje);
            return ResponseEntity.ok(Map.of("stopped", true, "codigoViaje", codigoViaje));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

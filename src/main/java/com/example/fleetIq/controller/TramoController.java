package com.example.fleetIq.controller;

import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.service.TramoService;
import com.example.fleetIq.util.ObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tramos")
@CrossOrigin(origins = "https://923cc1cddf51.ngrok-free.app")
public class TramoController {

    @Autowired
    private TramoService tramoService;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is healthy at " + LocalDateTime.now());
    }

    @GetMapping("/viaje/{viajeId}")
    public ResponseEntity<List<TramoDto>> listarTramosPorViaje(@PathVariable String viajeId) {
        try {
            List<TramoDto> tramos = tramoService.listarTramosPorViaje(viajeId);
            return ResponseEntity.ok(tramos);
        } catch (Exception e) {
            System.err.println("Error al listar tramos por viaje: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> crearTramo(@RequestBody TramoDto tramoDto) {
        try {
            tramoService.crearTramo(tramoDto);
            System.out.println("[START] endpoint guardar tramos /api/tramos:{} " + ObjectUtil.objectToJson(tramoDto));

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error al crear tramo: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
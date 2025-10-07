package com.example.fleetIq.controller;

import com.example.fleetIq.dto.ViajeDto;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.service.ViajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/viajes")
@RequiredArgsConstructor
public class ViajeController {

    private static final Logger logger = LoggerFactory.getLogger(ViajeController.class);
    private final ViajeService viajeService;

    @PostMapping
    public ResponseEntity<Map<String, String>> guardarViaje(@RequestBody Viaje viaje) {
        try {
            Viaje savedViaje = viajeService.guardarViaje(viaje);
            return ResponseEntity.ok(Map.of("viajeId", savedViaje.getId()));
        } catch (Exception e) {
            System.err.println("Error al guardar viaje: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error al guardar viaje: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ViajeDto>> listarTodosLosViajes() {
        try {
            List<ViajeDto> viajes = viajeService.listarTodosLosViajes();
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<Viaje>> listarTodosLosViajesPaginado(Pageable pageable) {
        try {
            Page<Viaje> viajes = viajeService.listarTodosLosViajesPaginado(pageable);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/codigo/{codigoViaje}")
    public ResponseEntity<List<ViajeDto>> listarViajesPorCodigo(@PathVariable String codigoViaje) {
        try {
            List<ViajeDto> viajes = viajeService.listarViajesPorCodigo(codigoViaje);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<?> listarViajesPorEmpresa(@PathVariable String empresaId) {
        try {
            logger.info("🔍 Buscando viajes para empresa: {}", empresaId);
            List<ViajeDto> viajes = viajeService.listarViajesPorEmpresa(empresaId);
            logger.info("✅ Encontrados {} viajes para empresa {}", viajes.size(), empresaId);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            logger.error("❌ Error al buscar viajes para empresa {}: {}", empresaId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al buscar viajes para empresa",
                "empresaId", empresaId,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/operador/{operadorId}")
    public ResponseEntity<List<ViajeDto>> listarViajesPorOperador(@PathVariable String operadorId) {
        try {
            List<ViajeDto> viajes = viajeService.listarViajesPorOperador(operadorId);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ViajeDto>> listarViajesPorCliente(@PathVariable String clienteId) {
        try {
            List<ViajeDto> viajes = viajeService.listarViajesPorCliente(clienteId);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{operadorId}")
    public ResponseEntity<List<ViajeDto>> listarViajes(@PathVariable String operadorId) {
        try {
            List<ViajeDto> viajes = viajeService.listarViajesId(operadorId);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
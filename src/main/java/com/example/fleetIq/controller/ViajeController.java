package com.example.fleetIq.controller;

import com.example.fleetIq.dto.ViajeDto;
import com.example.fleetIq.dto.ViajeResumenDto;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.service.ViajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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

    // ⭐ NUEVO ENDPOINT PARA CARGAR EL DETALLE COMPLETO DE UN SOLO VIAJE POR SU ID
    @GetMapping("/{viajeId}")
    public ResponseEntity<?> obtenerViajeCompleto(@PathVariable String viajeId) {
        try {
            logger.info("⏳ Buscando detalles completos del viaje con ID: {}", viajeId);

            // Debes implementar este nuevo método en ViajeService:
            // ViajeDto obtenerViajePorId(String id);
            ViajeDto viajeCompleto = viajeService.obtenerViajePorId(viajeId);

            if (viajeCompleto == null) {
                logger.warn("Viaje no encontrado con ID: {}", viajeId);
                return ResponseEntity.notFound().build();
            }

            logger.info("✅ Detalles completos del viaje {} cargados.", viajeId);
            return ResponseEntity.ok(viajeCompleto);
        } catch (Exception e) {
            logger.error("❌ Error al buscar detalles del viaje {}: {}", viajeId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error al buscar detalles del viaje",
                    "viajeId", viajeId,
                    "message", e.getMessage()));
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
                    "message", e.getMessage()));
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

    @GetMapping("/por-operador/{operadorId}")
    public ResponseEntity<List<ViajeDto>> listarViajes(@PathVariable String operadorId) {
        try {
            List<ViajeDto> viajes = viajeService.listarViajesId(operadorId);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/lite/empresa/{empresaId}")
    public ResponseEntity<?> listarViajesPorEmpresaLite(@PathVariable String empresaId) {
        try {
            logger.info("⚡ Buscando viajes (LIGEROS) para empresa: {}", empresaId);
            // NOTA: Debes implementar listarViajesPorEmpresaLite en ViajeService
            List<ViajeResumenDto> viajesLite = viajeService.listarViajesResumenPorEmpresa(empresaId);
            logger.info("✅ Encontrados {} viajes LIGEROS para empresa {}", viajesLite.size(), empresaId);
            return ResponseEntity.ok(viajesLite);
        } catch (Exception e) {
            logger.error("❌ Error al buscar viajes (LIGEROS) para empresa {}: {}", empresaId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error al buscar viajes ligeros para empresa",
                    "empresaId", empresaId,
                    "message", e.getMessage()));
        }
    }

}
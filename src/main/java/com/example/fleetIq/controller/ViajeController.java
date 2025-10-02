package com.example.fleetIq.controller;

import com.example.fleetIq.dto.ViajeDto;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.service.ViajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/viajes")
@CrossOrigin(origins = "https://923cc1cddf51.ngrok-free.app")
@RequiredArgsConstructor
public class ViajeController {

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
    public ResponseEntity<List<ViajeDto>> listarViajesPorEmpresa(@PathVariable String empresaId) {
        try {
            List<ViajeDto> viajes = viajeService.listarViajesPorEmpresa(empresaId);
            return ResponseEntity.ok(viajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
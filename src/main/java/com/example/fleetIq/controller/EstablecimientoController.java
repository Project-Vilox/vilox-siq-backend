package com.example.fleetIq.controller;

import com.example.fleetIq.dto.EstablecimientoDto;
import com.example.fleetIq.service.EstablecimientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/establecimientos")
public class EstablecimientoController {

    @Autowired
    private EstablecimientoService establecimientoService;

    @GetMapping
    public ResponseEntity<List<EstablecimientoDto>> getAllEstablecimientos() {
        List<EstablecimientoDto> establecimientos = establecimientoService.getAllEstablecimientos();
        return ResponseEntity.ok(establecimientos);
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<EstablecimientoDto>> getEstablecimientosByEmpresa(
            @PathVariable String empresaId) {
        List<EstablecimientoDto> establecimientos = establecimientoService.getEstablecimientosByEmpresa(empresaId);
        return ResponseEntity.ok(establecimientos);
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<EstablecimientoDto>> getEstablecimientosByTipo(
            @PathVariable String tipo) {
        List<EstablecimientoDto> establecimientos = establecimientoService.getEstablecimientosByTipo(tipo);
        return ResponseEntity.ok(establecimientos);
    }

    @GetMapping("/publicos")
    public ResponseEntity<List<EstablecimientoDto>> getEstablecimientosPublicos() {
        List<EstablecimientoDto> establecimientos = establecimientoService.getEstablecimientosPublicos();
        return ResponseEntity.ok(establecimientos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstablecimientoDto> getEstablecimientoById(
            @PathVariable String id) {
        EstablecimientoDto establecimiento = establecimientoService.getEstablecimientoById(id);
        if (establecimiento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(establecimiento);
    }
}
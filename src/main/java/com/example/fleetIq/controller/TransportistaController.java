package com.example.fleetIq.controller;

import com.example.fleetIq.dto.TransportistaInfoResponse;
import com.example.fleetIq.service.TransportistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transportista")
public class TransportistaController {

    @Autowired
    private TransportistaService transportistaService;

    @GetMapping("/{id}")
    public ResponseEntity<TransportistaInfoResponse> getTransportistaInfo(
            @PathVariable String id,
            @RequestParam String tipoEmpresa) {

        // Verify tipoEmpresa is 'transportista'
        if (!"transportista".equals(tipoEmpresa)) {
            return ResponseEntity.badRequest().build();
        }

        TransportistaInfoResponse response = transportistaService.getTransportistaInfo(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }
}
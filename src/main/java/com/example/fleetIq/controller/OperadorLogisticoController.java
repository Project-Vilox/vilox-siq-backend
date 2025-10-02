package com.example.fleetIq.controller;

import com.example.fleetIq.dto.OperadorLogisticoInfoResponse;
import com.example.fleetIq.dto.TransportistaInfoResponse;
import com.example.fleetIq.service.OperadorLogisticoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operador-logistico")
public class OperadorLogisticoController {

    @Autowired
    private OperadorLogisticoService operadorLogisticoService;

    @GetMapping("/{id}")
    public ResponseEntity<OperadorLogisticoInfoResponse> getTransportistaInfo(
            @PathVariable String id,
            @RequestParam String tipoEmpresa) {

         OperadorLogisticoInfoResponse response = operadorLogisticoService.getOperadorLogisticoInfo(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }
}
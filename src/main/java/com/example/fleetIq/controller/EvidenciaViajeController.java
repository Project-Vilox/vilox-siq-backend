package com.example.fleetIq.controller;

import com.example.fleetIq.dto.EliminarEvidenciaRequest;
import com.example.fleetIq.dto.EvidenciaViajeRequest;
import com.example.fleetIq.dto.EvidenciaViajeResponse;
import com.example.fleetIq.dto.GenericResponse;
import com.example.fleetIq.service.EvidenciaViajeService;
import com.example.fleetIq.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evidencias-viajes")
@RequiredArgsConstructor
public class EvidenciaViajeController {

    private final EvidenciaViajeService evidenciaViajeService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crearEvidencia(@RequestBody EvidenciaViajeRequest request) {
        try {
            System.out.println("[START] endpoint guardar tramos /api/tramos:{} " + ObjectUtil.objectToJson(request));
            EvidenciaViajeResponse response = evidenciaViajeService.crearEvidencia(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            GenericResponse errorResponse = new GenericResponse("Error al crear evidencia: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            GenericResponse errorResponse = new GenericResponse("Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/viaje/{viajeId}")
    public ResponseEntity<?> obtenerEvidenciasPorViaje(@PathVariable String viajeId) {
        try {
            List<EvidenciaViajeResponse> evidencias = evidenciaViajeService.obtenerEvidenciasPorViaje(viajeId);
            return ResponseEntity.ok(evidencias);
        } catch (IllegalArgumentException e) {
            GenericResponse errorResponse = new GenericResponse("Evidencias no encontradas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/{evidenciaId}/descargar")
    public ResponseEntity<?> descargarEvidencia(@PathVariable String evidenciaId) {
        try {
            byte[] archivo = evidenciaViajeService.descargarEvidencia(evidenciaId);
            EvidenciaViajeResponse evidencia = evidenciaViajeService.obtenerInfoEvidencia(evidenciaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(evidencia.getContentType()));
            headers.setContentDispositionFormData("attachment", evidencia.getNombreArchivo());
            headers.setContentLength(evidencia.getTamanioArchivo());

            return new ResponseEntity<>(archivo, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            GenericResponse errorResponse = new GenericResponse("Evidencia no encontrada: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminarEvidencia(@RequestBody EliminarEvidenciaRequest request) {
        try {
            evidenciaViajeService.eliminarEvidenciaPorViajeHitoSecuencia(request.getIdViaje(), request.getHito(), request.getSecuencia());
            GenericResponse response = new GenericResponse("Evidencia eliminada correctamente");
            response.addData("idViaje", request.getIdViaje());
            response.addData("hito", request.getHito());
            response.addData("secuencia", request.getSecuencia());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            GenericResponse errorResponse = new GenericResponse("Error: Evidencia no encontrada para idViaje: " +
                    request.getIdViaje() + ", hito: " + request.getHito() + ", secuencia: " + request.getSecuencia());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}
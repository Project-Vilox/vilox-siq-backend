package com.example.fleetIq.service;

import com.example.fleetIq.dto.EvidenciaViajeRequest;
import com.example.fleetIq.dto.EvidenciaViajeResponse;
import com.example.fleetIq.model.EvidenciaViaje;
import com.example.fleetIq.repository.EvidenciaViajeRepository;
import com.example.fleetIq.repository.ViajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EvidenciaViajeServiceImpl implements EvidenciaViajeService {

    @Autowired
    private EvidenciaViajeRepository evidenciaViajeRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Override
    @Transactional
    public EvidenciaViajeResponse crearEvidencia(EvidenciaViajeRequest request) {
        // Validar el viaje
        viajeRepository.findById(request.getViajeId())
                .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado: " + request.getViajeId()));

        // Convertir Base64 a bytes
        byte[] adjuntoBytes;
        try {
            adjuntoBytes = Base64.getDecoder().decode(request.getArchivo());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Archivo Base64 inválido", e);
        }

        // Crear entidad EvidenciaViaje
        EvidenciaViaje evidencia = new EvidenciaViaje();
        evidencia.setIdViaje(request.getViajeId());
        evidencia.setHito(request.getHito());
        evidencia.setSecuencia(request.getSecuencia());
        evidencia.setTipoAdjunto(EvidenciaViaje.TipoAdjunto.valueOf(request.getTipoAdjunto()));
        evidencia.setAdjunto(adjuntoBytes);
        evidencia.setNombreArchivo(request.getNombreArchivo());
        evidencia.setContentType(request.getContentType());
        evidencia.setTamanioArchivo(adjuntoBytes.length);
        evidencia.setFechaCreacion(LocalDateTime.now());
        evidencia.setFechaActualizacion(LocalDateTime.now());

        // Guardar la evidencia
        evidencia = evidenciaViajeRepository.save(evidencia);

        // Convertir a DTO de respuesta
        return convertToResponse(evidencia);
    }

    @Override
    public List<EvidenciaViajeResponse> obtenerEvidenciasPorViaje(String viajeId) {
        return evidenciaViajeRepository.findByIdViaje(viajeId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] descargarEvidencia(String evidenciaId) {
        EvidenciaViaje evidencia = evidenciaViajeRepository.findById(evidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + evidenciaId));
        return evidencia.getAdjunto();
    }

    @Override
    @Transactional
    public void eliminarEvidencia(String evidenciaId) {
        EvidenciaViaje evidencia = evidenciaViajeRepository.findById(evidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + evidenciaId));
        evidenciaViajeRepository.delete(evidencia);
    }

    @Override
    @Transactional
    public void eliminarEvidenciaPorViajeHitoSecuencia(String idViaje, EvidenciaViaje.Hito hito, Integer secuencia) {
        EvidenciaViaje evidencia = evidenciaViajeRepository.findByIdViajeAndHitoAndSecuencia(idViaje, hito, secuencia)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada para idViaje: " + idViaje + ", hito: " + hito + ", secuencia: " + secuencia));
        evidenciaViajeRepository.delete(evidencia);
    }

    @Override
    public EvidenciaViajeResponse obtenerInfoEvidencia(String evidenciaId) {
        EvidenciaViaje evidencia = evidenciaViajeRepository.findById(evidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + evidenciaId));
        return convertToResponse(evidencia);
    }

    private EvidenciaViajeResponse convertToResponse(EvidenciaViaje evidencia) {
        EvidenciaViajeResponse response = new EvidenciaViajeResponse();
        response.setId(evidencia.getId());
        response.setViajeId(evidencia.getIdViaje());
        response.setHito(evidencia.getHito());
        response.setSecuencia(evidencia.getSecuencia());
        response.setTipoAdjunto(evidencia.getTipoAdjunto());
        response.setAdjunto(evidencia.getAdjunto());
        response.setNombreArchivo(evidencia.getNombreArchivo());
        response.setContentType(evidencia.getContentType());
        response.setTamanioArchivo(evidencia.getTamanioArchivo());
        response.setFechaCreacion(evidencia.getFechaCreacion());
        response.setFechaActualizacion(evidencia.getFechaActualizacion());
        return response;
    }
}
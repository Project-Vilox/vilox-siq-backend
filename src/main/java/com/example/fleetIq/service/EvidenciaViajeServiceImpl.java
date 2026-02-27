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

@Service
public class EvidenciaViajeServiceImpl implements EvidenciaViajeService {

    @Autowired
    private EvidenciaViajeRepository evidenciaViajeRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Override
    @Transactional
    public EvidenciaViajeResponse crearEvidencia(EvidenciaViajeRequest request) {
        viajeRepository.findById(request.getViajeId())
                .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado: " + request.getViajeId()));

        byte[] adjuntoBytes;
        try {
            adjuntoBytes = Base64.getDecoder().decode(request.getArchivo());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Archivo Base64 inválido", e);
        }

        EvidenciaViaje evidencia = new EvidenciaViaje();
        evidencia.setIdViaje(request.getViajeId());
        evidencia.setHito(request.getHito());
        evidencia.setSecuencia(request.getSecuencia());
        evidencia.setTipoAdjunto(EvidenciaViaje.TipoAdjunto.valueOf(request.getTipoAdjunto()));
        evidencia.setNombreArchivo(request.getNombreArchivo());
        evidencia.setContentType(request.getContentType());
        evidencia.setTamanioArchivo(Long.valueOf(adjuntoBytes.length));
        evidencia.setFechaCreacion(LocalDateTime.now());
        evidencia.setFechaActualizacion(LocalDateTime.now());

        // El path_archivo lo debe poner Laravel o debemos guardarlo aquí físicamente
        // Por ahora dejamos que se cree la entrada sin el binario en la DB
        evidencia = evidenciaViajeRepository.save(evidencia);

        // ✅ Devolver CON el BLOB para el preview inmediato
        return convertToResponseWithBlob(evidencia);
    }

    // ✅ CAMBIO PRINCIPAL: Usar query sin BLOB
    @Override
    @Transactional(readOnly = true)
    public List<EvidenciaViajeResponse> obtenerEvidenciasPorViaje(String viajeId) {
        return evidenciaViajeRepository.findMetadataByIdViaje(viajeId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] descargarEvidencia(String evidenciaId) {
        EvidenciaViaje evidencia = evidenciaViajeRepository.findById(evidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + evidenciaId));

        if (evidencia.getPathArchivo() != null) {
            try {
                String basePath = "c:/Users/yarle/OneDrive/Documentos/Trabajo/Vilox v3/Vilox v3/vilox.api/storage/app/public/";
                java.nio.file.Path path = java.nio.file.Paths.get(basePath, evidencia.getPathArchivo());
                return java.nio.file.Files.readAllBytes(path);
            } catch (Exception e) {
                throw new RuntimeException("Error al leer el archivo físico: " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("La evidencia no tiene un archivo físico asociado");
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Evidencia no encontrada para idViaje: " + idViaje +
                                ", hito: " + hito + ", secuencia: " + secuencia));
        evidenciaViajeRepository.delete(evidencia);
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenciaViajeResponse obtenerInfoEvidencia(String evidenciaId) {
        EvidenciaViaje evidencia = evidenciaViajeRepository.findById(evidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + evidenciaId));
        return convertToResponseWithBlob(evidencia);
    }

    // ✅ Método privado para incluir BLOB
    private EvidenciaViajeResponse convertToResponseWithBlob(EvidenciaViaje evidencia) {
        EvidenciaViajeResponse response = new EvidenciaViajeResponse();
        response.setId(evidencia.getId());
        response.setViajeId(evidencia.getIdViaje());
        response.setHito(evidencia.getHito());
        response.setSecuencia(evidencia.getSecuencia());
        response.setTipoAdjunto(evidencia.getTipoAdjunto());
        // No hay adjunto directo
        response.setNombreArchivo(evidencia.getNombreArchivo());
        response.setContentType(evidencia.getContentType());
        response.setTamanioArchivo(evidencia.getTamanioArchivo());
        response.setFechaCreacion(evidencia.getFechaCreacion());
        response.setFechaActualizacion(evidencia.getFechaActualizacion());
        return response;
    }
}
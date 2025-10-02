
        package com.example.fleetIq.service;

import com.example.fleetIq.dto.EvidenciaViajeRequest;
import com.example.fleetIq.dto.EvidenciaViajeResponse;
import com.example.fleetIq.model.EvidenciaViaje;

import java.util.List;

public interface EvidenciaViajeService {
    EvidenciaViajeResponse crearEvidencia(EvidenciaViajeRequest request);
    List<EvidenciaViajeResponse> obtenerEvidenciasPorViaje(String viajeId);
    byte[] descargarEvidencia(String evidenciaId);
    void eliminarEvidencia(String evidenciaId);
    void eliminarEvidenciaPorViajeHitoSecuencia(String idViaje, EvidenciaViaje.Hito hito, Integer secuencia);
    EvidenciaViajeResponse obtenerInfoEvidencia(String evidenciaId);
}
package com.example.fleetIq.dto;

import com.example.fleetIq.model.EvidenciaViaje;
import lombok.Data;

@Data
public class EvidenciaViajeRequest {
    private String viajeId;
    private EvidenciaViaje.Hito hito;
    private Integer secuencia;
    private String tipoAdjunto; // "IMAGEN" o "PDF"
    private String archivo; // Base64 encoded file
    private String nombreArchivo;
    private String contentType;
}
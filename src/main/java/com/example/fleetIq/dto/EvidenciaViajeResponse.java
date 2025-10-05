package com.example.fleetIq.dto;

import com.example.fleetIq.model.EvidenciaViaje;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvidenciaViajeResponse {
    private String id;
    private String viajeId;
    private EvidenciaViaje.Hito hito;
    private Integer secuencia;
    private EvidenciaViaje.TipoAdjunto tipoAdjunto;
    private String nombreArchivo;
    private String contentType;
    private long tamanioArchivo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private byte[] adjunto;
}
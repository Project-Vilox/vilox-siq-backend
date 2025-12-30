package com.example.fleetIq.dto;

import com.example.fleetIq.model.EvidenciaViaje;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private byte[] adjunto; // Será null en listados

    // ✅ Constructor para LISTADOS (sin BLOB)
    public EvidenciaViajeResponse(
            String id,
            String viajeId,
            EvidenciaViaje.Hito hito,
            Integer secuencia,
            EvidenciaViaje.TipoAdjunto tipoAdjunto,
            String nombreArchivo,
            String contentType,
            long tamanioArchivo,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion) {
        this.id = id;
        this.viajeId = viajeId;
        this.hito = hito;
        this.secuencia = secuencia;
        this.tipoAdjunto = tipoAdjunto;
        this.nombreArchivo = nombreArchivo;
        this.contentType = contentType;
        this.tamanioArchivo = tamanioArchivo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
        this.adjunto = null; // No cargamos BLOB
    }
}
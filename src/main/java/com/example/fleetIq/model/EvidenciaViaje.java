package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "evidencias_viajes")
public class EvidenciaViaje {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "id_viaje", nullable = false)
    private String idViaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "hito", nullable = false)
    private Hito hito;

    @Column(name = "secuencia", nullable = false)
    private Integer secuencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_adjunto", nullable = false)
    private TipoAdjunto tipoAdjunto;

    @Lob
    @Column(name = "adjunto", nullable = false)
    private byte[] adjunto;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "tamanio_archivo", nullable = false)
    private long tamanioArchivo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum Hito {
        RETIRO_DE_VACIO,LLEGADA_A_PLANTA,ENTREGA_A_PUERTO,PREVIO_SERVICIO,LLEGADA_DEPOT,SALIDA_DEPOT,INGRESO_SALIDA_COCHERA,LLEGADA_PLANTA,TERMINO_CARGA, LLEGADA_PUERTO, SALIDA_PUERTO

    }

    public enum TipoAdjunto {
        IMAGEN, PDF
    }
}
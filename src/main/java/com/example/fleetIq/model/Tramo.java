package com.example.fleetIq.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tramos")
public class Tramo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    @JsonBackReference
    private Viaje viaje;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establecimiento_origen_id", nullable = false)
    private Establecimiento establecimientoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establecimiento_destino_id", nullable = false)
    private Establecimiento establecimientoDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actividad", nullable = false)
    private TipoActividad tipoActividad;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "hora_llegada_programada")
    private LocalDateTime horaLlegadaProgramada;

    @Column(name = "hora_salida_programada")
    private LocalDateTime horaSalidaProgramada;

    @Column(name = "hora_llegada_real")
    private LocalDateTime horaLlegadaReal;

    @Column(name = "hora_salida_real")
    private LocalDateTime horaSalidaReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTramo estado;

    @Column(name = "sla_minutos")
    private Integer slaMinutos;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "tracto")
    private String tracto;

    @Column(name = "chasis")
    private String chasis;

    @Column(name = "conductor")
    private String conductor;

    @Column(name = "tardanza_cita1")
    private Integer tardanzaCita1;

    @Column(name = "tiempo_permanencia_cita1")
    private Integer tiempoPermanenciaCita1;

    @Column(name = "tiempo_atencion_cita1")
    private Integer tiempoAtencionCita1;

    @Column(name = "tardanza_cita2")
    private Integer tardanzaCita2;

    @Column(name = "tiempo_permanencia_cita2")
    private Integer tiempoPermanenciaCita2;

    @Column(name = "tiempo_atencion_cita2")
    private Integer tiempoAtencionCita2;

    public enum TipoActividad {
        recojo, entrega, carga, descarga, inspeccion, despacho_full
    }

    public enum EstadoTramo {
        pendiente, en_curso, completado, retrasado
    }
}
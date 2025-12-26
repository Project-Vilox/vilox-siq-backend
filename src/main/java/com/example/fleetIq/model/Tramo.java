package com.example.fleetIq.model;

import com.example.fleetIq.listener.TramoEntityListener;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tramos")
@EntityListeners(TramoEntityListener.class) // ⭐ AGREGAR ESTA LÍNEA
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

    // ⏱️ HORAS REALES DEL ORIGEN
    @Column(name = "hora_llegada_real")
    private LocalDateTime horaLlegadaReal;

    @Column(name = "hora_salida_real")
    private LocalDateTime horaSalidaReal;

    // ⏱️ HORAS REALES DEL DESTINO (NUEVOS CAMPOS)
    @Column(name = "hora_llegada_real_destino")
    private LocalDateTime horaLlegadaRealDestino;

    @Column(name = "hora_salida_real_destino")
    private LocalDateTime horaSalidaRealDestino;

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
    @Column(name = "hora_entrada_geocerca_interna_origen") // ⬅️ NUEVO CAMPO PERSISTENTE
    private LocalDateTime horaEntradaGeocercaInternaOrigen;

    @Column(name = "hora_entrada_geocerca_interna_destino") // ⬅️ NUEVO CAMPO PERSISTENTE
    private LocalDateTime horaEntradaGeocercaInternaDestino;
    // ========================================================================
    // CAMPOS ADICIONALES NECESARIOS PARA GEOCERCAS ADYACENTES
    // ========================================================================

    // ==================== ORIGEN - GEOCERCA EXTERNA ====================
    @Column(name = "hora_entrada_geocerca_externa_origen")
    private LocalDateTime horaEntradaGeocercaExternaOrigen;

    @Column(name = "hora_salida_geocerca_externa_origen_1")
    private LocalDateTime horaSalidaGeocercaExternaOrigen1; // Primera salida (acceso → patio)

    @Column(name = "hora_entrada_geocerca_externa_origen_2")
    private LocalDateTime horaEntradaGeocercaExternaOrigen2; // Segunda entrada (patio → acceso)

    @Column(name = "hora_salida_geocerca_externa_origen_2")
    private LocalDateTime horaSalidaGeocercaExternaOrigen2; // Segunda salida (acceso → calle)

    @Column(name = "hora_salida_geocerca_interna_origen")
    private LocalDateTime horaSalidaGeocercaInternaOrigen;

    // ==================== DESTINO - GEOCERCA EXTERNA ====================
    @Column(name = "hora_entrada_geocerca_externa_destino")
    private LocalDateTime horaEntradaGeocercaExternaDestino;

    @Column(name = "hora_salida_geocerca_externa_destino_1")
    private LocalDateTime horaSalidaGeocercaExternaDestino1; // Primera salida (acceso → patio)

    @Column(name = "hora_entrada_geocerca_externa_destino_2")
    private LocalDateTime horaEntradaGeocercaExternaDestino2; // Segunda entrada (patio → acceso)

    @Column(name = "hora_salida_geocerca_externa_destino_2")
    private LocalDateTime horaSalidaGeocercaExternaDestino2; // Segunda salida (acceso → calle)

    @Column(name = "hora_salida_geocerca_interna_destino")
    private LocalDateTime horaSalidaGeocercaInternaDestino;

    public enum TipoActividad {
        recojo, entrega, carga, descarga, inspeccion, despacho_full
    }

    public enum EstadoTramo {
        pendiente, en_curso, completado, retrasado
    }
}
package com.example.fleetIq.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TramoDto {

    private String id;
    private String viajeId;
    private Integer orden;
    private String establecimientoOrigenId;
    private String establecimientoDestinoId;
    private EstablecimientoDto establecimientoOrigen;
    private EstablecimientoDto establecimientoDestino;
    private String tipoActividad;
    private String descripcion;
    private String estado;
    private Integer slaMinutos;
    private String observaciones;

    // Horas programadas
    private LocalDateTime horaLlegadaProgramada;
    private LocalDateTime horaSalidaProgramada;

    // Horas reales básicas (deprecadas)
    private LocalDateTime horaLlegadaReal;
    private LocalDateTime horaSalidaReal;
    private LocalDateTime horaLlegadaRealDestino;
    private LocalDateTime horaSalidaRealDestino;

    // ========================================================================
    // GEOCERCAS ADYACENTES - ORIGEN
    // ========================================================================
    private LocalDateTime horaEntradaGeocercaExternaOrigen;
    private LocalDateTime horaSalidaGeocercaExternaOrigen1;
    private LocalDateTime horaEntradaGeocercaInternaOrigen;
    private LocalDateTime horaSalidaGeocercaInternaOrigen;
    private LocalDateTime horaEntradaGeocercaExternaOrigen2;
    private LocalDateTime horaSalidaGeocercaExternaOrigen2;

    // ========================================================================
    // GEOCERCAS ADYACENTES - DESTINO
    // ========================================================================
    private LocalDateTime horaEntradaGeocercaExternaDestino;
    private LocalDateTime horaSalidaGeocercaExternaDestino1;
    private LocalDateTime horaEntradaGeocercaInternaDestino;
    private LocalDateTime horaSalidaGeocercaInternaDestino;
    private LocalDateTime horaEntradaGeocercaExternaDestino2;
    private LocalDateTime horaSalidaGeocercaExternaDestino2;

    // Métricas
    private Integer tardanzaCita1;
    private Integer tiempoPermanenciaCita1;
    private Integer tiempoAtencionCita1;
    private Integer tardanzaCita2;
    private Integer tiempoPermanenciaCita2;
    private Integer tiempoAtencionCita2;

    private String tracto;
    private String chasis;
    private String conductor;
    private String eta;
    private String etaProgramado;
    private Double avance;
    private String semaforo;
    private Integer minutosRetraso;
    private Integer demoraSalida;
    private DemorasDto demoras;

    @Data
    public static class DemorasDto {
        private Integer demoraSalidaOrigen;
        private Integer demoraTransito;
        private Integer demoraParadas;
        private String motivoPrincipal;
        private Integer tiempoEstimadoOriginal;
        private Integer tiempoRealTransito;
    }
}
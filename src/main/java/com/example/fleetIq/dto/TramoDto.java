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
    private String tipoActividad;
    private String descripcion;
    private LocalDateTime horaLlegadaProgramada;
    private LocalDateTime horaSalidaProgramada;
    private LocalDateTime horaLlegadaReal;
    private LocalDateTime horaSalidaReal;
    private String estado;
    private Integer slaMinutos;
    private String observaciones;
    private String eta;
    private double avance;
    private String tracto;
    private String chasis;
    private String conductor;
    private Integer tardanzaCita1;
    private Integer tiempoPermanenciaCita1;
    private Integer tiempoAtencionCita1;
    private Integer tardanzaCita2;
    private Integer tiempoPermanenciaCita2;
    private Integer tiempoAtencionCita2;
}
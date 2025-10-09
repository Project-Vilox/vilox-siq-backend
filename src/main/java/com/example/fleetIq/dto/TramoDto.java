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
}
package com.example.fleetIq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlmaceneraDto {
    private String id;
    private String nombre;
    private String tipoEmpresa;
    private String ruc;
    private String direccion;
    private String telefono;
    private String email;
    private boolean activo;
    private String configuracionAlertas;
    private String configuracionDashboard;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
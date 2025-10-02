package com.example.fleetIq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstablecimientosDto {
    private String id;
    private String empresaId;
    private String nombre;
    private String tipo;
    private String direccion;
    private Double latitud;
    private Double longitud;
    private boolean publico;
    private boolean activo;
    private String configuracionSla;
    private LocalDateTime fechaCreacion;
    private String geofenceId;
}
package com.example.fleetIq.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmpresaDto {
    private String id;
    private String nombre;
    private String tipoEmpresa;
    private String ruc;
    private String direccion;
    private String telefono;
    private String email;
    private Boolean activo;
    private String configuracionAlertas;
    private String configuracionDashboard;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public EmpresaDto(String id, String nombre, String tipoEmpresa, String ruc, String direccion,
                      String telefono, String email, Boolean activo, String configuracionAlertas,
                      String configuracionDashboard, LocalDateTime fechaCreacion,
                      LocalDateTime fechaActualizacion) {
        this.id = id;
        this.nombre = nombre;
        this.tipoEmpresa = tipoEmpresa;
        this.ruc = ruc;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.activo = activo;
        this.configuracionAlertas = configuracionAlertas;
        this.configuracionDashboard = configuracionDashboard;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }
}
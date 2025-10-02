package com.example.fleetIq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EstablecimientoDto {
    private String id;
    private String empresaId;
    private String nombre;
    private String tipo;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private Boolean publico;
    private Boolean activo;

    @JsonProperty("configuracion_sla")
    private Object configuracionSla;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    public EstablecimientoDto(String id, String empresaId, String nombre, String tipo,
                              String direccion, BigDecimal latitud, BigDecimal longitud,
                              Boolean publico, Boolean activo, Object configuracionSla,
                              LocalDateTime fechaCreacion) {
        this.id = id;
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.publico = publico;
        this.activo = activo;
        this.configuracionSla = configuracionSla;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters
    public String getId() { return id; }
    public String getEmpresaId() { return empresaId; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getDireccion() { return direccion; }
    public BigDecimal getLatitud() { return latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public Boolean getPublico() { return publico; }
    public Boolean getActivo() { return activo; }
    public Object getConfiguracionSla() { return configuracionSla; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setEmpresaId(String empresaId) { this.empresaId = empresaId; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }
    public void setPublico(Boolean publico) { this.publico = publico; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setConfiguracionSla(Object configuracionSla) { this.configuracionSla = configuracionSla; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
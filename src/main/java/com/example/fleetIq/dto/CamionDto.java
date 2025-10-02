package com.example.fleetIq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamionDto {
    private String id;
    private String empresaId;
    private String placa;
    private String imei;
    private String marca;
    private String modelo;
    private Integer ano;
    private String tipoVehiculo;
    private BigDecimal capacidadToneladas;
    private String estado;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
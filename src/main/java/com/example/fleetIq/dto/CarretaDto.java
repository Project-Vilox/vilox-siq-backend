package com.example.fleetIq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarretaDto {
    private String id;
    private String empresaId;
    private String placa;
    private String imei;
    private String marca;
    private String modelo;
    private Integer ano;
    private String tipoVehiculo;
    private Double capacidadToneladas; // Changed to Double to match DTO expectation
    private String estado;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
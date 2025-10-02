package com.example.fleetIq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlmaceneraConEstablecimientosDto {
    private AlmaceneraDto almacenera;
    private List<EstablecimientosDto> establecimientos;
}
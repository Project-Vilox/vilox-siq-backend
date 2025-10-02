package com.example.fleetIq.dto;

import lombok.Data;

@Data
public class OperadorLogisticoConClientesDto {
    private OperadorLogisticosDto operadorLogistico;

    public OperadorLogisticoConClientesDto(OperadorLogisticosDto operadorLogistico) {
        this.operadorLogistico = operadorLogistico;
    }
}
package com.example.fleetIq.dto;

import lombok.Data;
import java.util.List;

@Data
public class OperadorLogisticoInfoResponse {
    private EmpresaDto operador;
    private List<EmpresaDto> transportistas;

    public OperadorLogisticoInfoResponse(EmpresaDto operador, List<EmpresaDto> transportistas) {
        this.operador = operador;
        this.transportistas = transportistas;
    }
}
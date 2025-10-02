package com.example.fleetIq.dto;

import lombok.Data;

@Data
public class TransportistaWrapperDto {
    private EmpresaDto empresaTransporte;

    public TransportistaWrapperDto(EmpresaDto empresaTransporte) {
        this.empresaTransporte = empresaTransporte;
    }
}
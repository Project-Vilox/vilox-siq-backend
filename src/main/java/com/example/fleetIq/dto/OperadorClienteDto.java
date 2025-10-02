package com.example.fleetIq.dto;

// OperadorClienteDto (for query)
public class OperadorClienteDto {
    private String operadorNombre;
    private String clienteNombre;

    public OperadorClienteDto(String operadorNombre, String clienteNombre) {
        this.operadorNombre = operadorNombre;
        this.clienteNombre = clienteNombre;
    }

    public String getOperadorNombre() { return operadorNombre; }
    public void setOperadorNombre(String operadorNombre) { this.operadorNombre = operadorNombre; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
}
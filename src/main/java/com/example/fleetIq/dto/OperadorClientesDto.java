package com.example.fleetIq.dto;
import java.util.List;

// OperadorClientesDto
public class OperadorClientesDto {
    private String operador;
    private List<String> clientes;

    public OperadorClientesDto(String operador, List<String> clientes) {
        this.operador = operador;
        this.clientes = clientes;
    }

    // Getters and setters
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public List<String> getClientes() { return clientes; }
    public void setClientes(List<String> clientes) { this.clientes = clientes; }
}
package com.example.fleetIq.dto;

import java.util.Objects;

public class ClienteDto {
    private String clienteId;
    private String clienteNombre;
    private String clienteRuc;

    public ClienteDto() {}

    public ClienteDto(String clienteId, String clienteNombre, String clienteRuc) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteRuc = clienteRuc;
    }

    // Getters y setters
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteRuc() { return clienteRuc; }
    public void setClienteRuc(String clienteRuc) { this.clienteRuc = clienteRuc; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClienteDto that = (ClienteDto) o;
        return Objects.equals(clienteId, that.clienteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clienteId);
    }
}
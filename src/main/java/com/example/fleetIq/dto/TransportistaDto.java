package com.example.fleetIq.dto;

import java.util.List;

public class TransportistaDto {
    private String transportistaId;
    private String transportistaNombre;
    private String ruc;
    private List<CamionDto> camiones;
    private List<ChoferDto> choferes;

    public TransportistaDto() {}

    public TransportistaDto(String transportistaId, String transportistaNombre, String ruc, List<CamionDto> camiones, List<ChoferDto> choferes) {
        this.transportistaId = transportistaId;
        this.transportistaNombre = transportistaNombre;
        this.ruc = ruc;
        this.camiones = camiones;
        this.choferes = choferes;
    }

    // Getters y setters
    public String getTransportistaId() { return transportistaId; }
    public void setTransportistaId(String transportistaId) { this.transportistaId = transportistaId; }

    public String getTransportistaNombre() { return transportistaNombre; }
    public void setTransportistaNombre(String transportistaNombre) { this.transportistaNombre = transportistaNombre; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public List<CamionDto> getCamiones() { return camiones; }
    public void setCamiones(List<CamionDto> camiones) { this.camiones = camiones; }

    public List<ChoferDto> getChoferes() { return choferes; }
    public void setChoferes(List<ChoferDto> choferes) { this.choferes = choferes; }
}
package com.example.fleetIq.dto;

import lombok.Data;

import java.util.List;

@Data
public class TransportistaInfoResponse {
    private String id;
    private String nombre;
    private String tipoEmpresa;
    private String ruc;
    private List<CamionDto> camiones;
    private List<CarretaDto> carretas;
    private List<ChoferDto> choferes;
    private List<EstablecimientosDto> establecimientos;
    private List<NavieraConAlmacenerasDto> navieraConAlmaceneras;
    private List<AlmaceneraConEstablecimientosDto> almaceneraConEstablecimientos;
    private List<OperadorLogisticoConClientesDto> operadorLogisticoConClientes;

    public TransportistaInfoResponse(String id, String nombre, String tipoEmpresa, String ruc,
                                     List<CamionDto> camiones, List<CarretaDto> carretas,
                                     List<ChoferDto> choferes, List<EstablecimientosDto> establecimientos,
                                     List<NavieraConAlmacenerasDto> navieraConAlmaceneras,
                                     List<AlmaceneraConEstablecimientosDto> almaceneraConEstablecimientos,
                                     List<OperadorLogisticoConClientesDto> operadorLogisticoConClientes) {
        this.id = id;
        this.nombre = nombre;
        this.tipoEmpresa = tipoEmpresa;
        this.ruc = ruc;
        this.camiones = camiones;
        this.carretas = carretas;
        this.choferes = choferes;
        this.establecimientos = establecimientos;
        this.navieraConAlmaceneras = navieraConAlmaceneras;
        this.almaceneraConEstablecimientos = almaceneraConEstablecimientos;
        this.operadorLogisticoConClientes = operadorLogisticoConClientes;
    }
}
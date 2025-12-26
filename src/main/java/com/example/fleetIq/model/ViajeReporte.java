package com.example.fleetIq.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "data_dash_viajes")
@Data
public class ViajeReporte {

    @Id
    private String viajeid;
    private String viajetipo;
    private java.time.LocalDate fecha;
    private String naviera;
    private String transportista;
    private String operador;
    private String cliente;
    private String deposito;
    private String planta;
    private String puerto;
    private String tracto;
    private String conductor;
    private Integer tardanzadeposito;
    private Integer tardanzaplanta;
    private Integer tardanzapuerto;
    private Integer tiempodeposito;
    private Integer tiempoplanta;
    private Integer tiempopuerto;
    private Integer kilometraje;
    private String ruta;
}
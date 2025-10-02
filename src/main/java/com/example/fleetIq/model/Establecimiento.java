package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "establecimientos")
@Data
@NoArgsConstructor
public class Establecimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "empresa_id")
    private String empresaId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "latitud", precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 11, scale = 8)
    private BigDecimal longitud;

    @Column(name = "publico")
    private Boolean publico;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "configuracion_sla", columnDefinition = "JSON")
    private String configuracionSla;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
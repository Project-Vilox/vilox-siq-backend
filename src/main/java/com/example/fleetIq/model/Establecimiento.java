package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "latitud", precision = 12, scale = 9)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 12, scale = 9)
    private BigDecimal longitud;

    @Column(name = "publico")
    private Boolean publico;

    @Column(name = "activo")
    private Boolean activo;

    @JdbcTypeCode(SqlTypes.JSON) // 👈 Esto le dice a Hibernate que lo trate como JSON en Postgres
    @Column(name = "configuracion_sla", columnDefinition = "JSON")
    private String configuracionSla;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // 🆕 NUEVO CAMPO para saber si las coordenadas son calculadas
    @Column(name = "centroide_calculado")
    private Boolean centroideCalculado = false;

    @Column(name = "fecha_calculo_centroide")
    private LocalDateTime fechaCalculoCentroide;
}
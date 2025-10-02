package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "almaceneras_de_navieras")
@Data
@NoArgsConstructor
public class AlmacenerasDeNavieras {
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresaID_naviera", nullable = false)
    private Empresa empresaNaviera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresaID_almacenera", nullable = false)
    private Empresa empresaAlmacenera;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
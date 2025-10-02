package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transportistas_de_operadores")
@Data
public class TransportistasDeOperadores {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id = java.util.UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "empresaID_operador", nullable = false)
    private Empresa empresaOperador;

    @ManyToOne
    @JoinColumn(name = "empresaID_transportista", nullable = false)
    private Empresa empresaTransportista;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado = "ACTIVO";

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Entity
    @Table(name = "almaceneras_de_navieras")
    @Data
    @NoArgsConstructor
    public static class AlmacenerasDeNavieras {
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
}
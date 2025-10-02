package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "operadores_de_clientes")
@Data
public class OperadoresDeClientes {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id = java.util.UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "empresaID_cliente", nullable = false)
    private Empresa empresaCliente;

    @ManyToOne
    @JoinColumn(name = "empresaID_operador", nullable = false)
    private Empresa empresaOperador;

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
}
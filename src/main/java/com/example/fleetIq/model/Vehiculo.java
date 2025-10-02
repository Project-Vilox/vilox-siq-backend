package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculos")
@Data
@NoArgsConstructor
public class Vehiculo {
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "placa", unique = true, nullable = false)
    private String placa;

    @Column(name = "imei", unique = true)
    private String imei;

    @Column(name = "marca")
    private String marca;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "año")
    private Integer ano;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false)
    private TipoVehiculo tipoVehiculo; // Enum for 'camion', 'trailer', 'furgon'

    @Column(name = "capacidad_toneladas")
    private BigDecimal capacidadToneladas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoVehiculo estado; // Enum for 'activo', 'mantenimiento', 'reparacion', 'inactivo'

    @Column(name = "activo", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo;

    @Column(name = "fecha_creacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;
}

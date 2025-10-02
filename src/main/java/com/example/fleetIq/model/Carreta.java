package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "carretas")
public class Carreta {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Empresa empresa;

    @Column(name = "placa", unique = true, nullable = false, length = 20)
    private String placa;

    @Column(name = "imei", unique = true, length = 50)
    private String imei;

    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "año")
    private Integer año;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false)
    private TipoVehiculo tipoVehiculo = TipoVehiculo.CARRETA;

    @Column(name = "capacidad_toneladas", precision = 8, scale = 2)
    private BigDecimal capacidadToneladas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoVehiculo estado;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
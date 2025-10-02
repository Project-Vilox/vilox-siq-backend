package com.example.fleetIq.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "viajes")
public class Viaje {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "codigo_viaje")
    private String codigoViaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_transportista_id")
    private Empresa empresaTransportista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_operador_id")
    private Empresa empresaOperador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_cliente_id")
    private Empresa empresaCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carreta_id")
    private Carreta carreta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_naviera_id")
    private Empresa empresaNaviera;

    @Column(name = "tipo_operacion")
    private String tipoOperacion;

    @Column(name = "documento_embarque")
    private String documentoEmbarque;

    @Column(name = "estado")
    private String estado;

    @Column(name = "fecha_inicio_programada")
    private LocalDateTime fechaInicioProgramada;

    @Column(name = "fecha_fin_programada")
    private LocalDateTime fechaFinProgramada;

    @Column(name = "fecha_inicio_real")
    private LocalDateTime fechaInicioReal;

    @Column(name = "fecha_fin_real")
    private LocalDateTime fechaFinReal;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "configuracion_alertas", columnDefinition = "JSON")
    private String configuracionAlertas;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "viaje", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Tramo> tramos = new ArrayList<>();
}
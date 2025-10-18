package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "sla_por_establecimiento")
@Data
public class SlaPorEstablecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_establecimiento", nullable = false, length = 36)
    private String idEstablecimiento;

    @Column(name = "tiempo_atencion_minutos", nullable = false)
    private Integer tiempoAtencionMinutos;

    @Column(name = "id_cliente", length = 36)
    private String idCliente;

    @Column(name = "id_viaje", length = 36)
    private String idViaje;

    @ManyToOne
    @JoinColumn(name = "id_cliente", referencedColumnName = "id", insertable = false, updatable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "id_establecimiento", referencedColumnName = "id", insertable = false, updatable = false)
    private Establecimiento establecimiento;

    @ManyToOne
    @JoinColumn(name = "id_viaje", referencedColumnName = "id", insertable = false, updatable = false)
    private Viaje viaje;
}
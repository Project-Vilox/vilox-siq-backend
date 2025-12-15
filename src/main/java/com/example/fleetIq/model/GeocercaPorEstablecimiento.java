package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "geocercas_por_establecimiento")
@Data
@NoArgsConstructor
public class GeocercaPorEstablecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_uuid", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "establecimiento_id", nullable = false, length = 36)
    private String establecimientoId;

    @Column(name = "geocerca_id", nullable = false)
    private Long geocercaId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // "EXTERNA" o "INTERNA"

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private Integer orden; // 1 para externa, 2 para interna

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}
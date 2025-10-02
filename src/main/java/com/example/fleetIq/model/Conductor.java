package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conductores")
@Data
@NoArgsConstructor
public class Conductor {
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "dni")
    private String dni;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellidos")
    private String apellidos;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "licencia_numero")
    private String licenciaNumero;

    @Column(name = "licencia_categoria")
    private String licenciaCategoria;

    @Column(name = "licencia_vencimiento")
    private LocalDate licenciaVencimiento;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "conductor", fetch = FetchType.LAZY)
    private List<ConductorEmpresas> conductorEmpresas;
}
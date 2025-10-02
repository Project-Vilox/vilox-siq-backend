package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
@ToString
public class Empresa {
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo_empresa")
    private String tipoEmpresa;

    @Column(name = "ruc")
    private String ruc;

    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Carreta> carretas = new HashSet<>();

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "configuracion_alertas", columnDefinition = "JSON")
    private String configuracionAlertas;

    @Column(name = "configuracion_dashboard", columnDefinition = "JSON")
    private String configuracionDashboard;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Vehiculo> vehiculos = new HashSet<>();

    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<ConductorEmpresas> conductorEmpresas = new HashSet<>();

    @OneToMany(mappedBy = "empresaTransportista", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Viaje> viajesComoTransportista = new HashSet<>();

    @OneToMany(mappedBy = "empresaOperador", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Viaje> viajesComoOperador = new HashSet<>();

    @OneToMany(mappedBy = "empresaCliente", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<OperadoresDeClientes> operadoresDeClientesAsCliente = new HashSet<>();

    @OneToMany(mappedBy = "empresaOperador", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<OperadoresDeClientes> operadoresDeClientesAsOperador = new HashSet<>();

    @OneToMany(mappedBy = "empresaTransportista", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<TransportistasDeOperadores> transportistasDeOperadoresAsTransportista = new HashSet<>();

    @OneToMany(mappedBy = "empresaOperador", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<TransportistasDeOperadores> transportistasDeOperadoresAsOperador = new HashSet<>();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((nombre == null) ? 0 : nombre.hashCode());
        result = prime * result + ((tipoEmpresa == null) ? 0 : tipoEmpresa.hashCode());
        result = prime * result + ((ruc == null) ? 0 : ruc.hashCode());
        result = prime * result + ((direccion == null) ? 0 : direccion.hashCode());
        result = prime * result + ((telefono == null) ? 0 : telefono.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((activo == null) ? 0 : activo.hashCode());
        result = prime * result + ((configuracionAlertas == null) ? 0 : configuracionAlertas.hashCode());
        result = prime * result + ((configuracionDashboard == null) ? 0 : configuracionDashboard.hashCode());
        result = prime * result + ((fechaCreacion == null) ? 0 : fechaCreacion.hashCode());
        result = prime * result + ((fechaActualizacion == null) ? 0 : fechaActualizacion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Empresa other = (Empresa) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
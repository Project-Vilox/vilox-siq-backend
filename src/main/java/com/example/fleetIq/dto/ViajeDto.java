package com.example.fleetIq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ViajeDto {
    private String id;
    private String codigoViaje;
    private EmpresaDto empresaTransportista;
    private EmpresaDto empresaOperador;
    private EmpresaDto empresaCliente;
    private EmpresaDto empresaNaviera; // Nueva relación para Naviera
    private VehiculoDto vehiculo;
    private CarretaDto carreta;
    private ConductorDto conductor;
    private String tipoOperacion;
    private String documentoEmbarque;
    private String estado;
    private LocalDateTime fechaInicioProgramada;
    private LocalDateTime fechaFinProgramada;
    private LocalDateTime fechaInicioReal;
    private LocalDateTime fechaFinReal;
    private String observaciones;
    private String configuracionAlertas;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private List<TramoDto> tramos; // Relación existente

    @Data
    public static class EmpresaDto {
        private String id;
        private String nombre;
        private String tipoEmpresa;
        private String ruc;
        private String direccion;
        private String telefono;
        private String email;
        private Boolean activo;
        private String configuracionAlertas;
        private String configuracionDashboard;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaActualizacion;
        private String empresaAdministradoraId;
    }

    @Data
    public static class VehiculoDto {
        private String id;
        private String empresaId;
        private String placa;
        private String imei;
        private String marca;
        private String modelo;
        private Integer ano;
        private String tipoVehiculo;
        private BigDecimal capacidadToneladas;
        private String estado;
        private Boolean activo;
        private LocalDateTime fechaCreacion;
    }

    @Data
    public static class CarretaDto {
        private String id;
        private String empresaId;
        private String placa;
        private String imei;
        private String marca;
        private String modelo;
        private Integer ano;
        private String tipoVehiculo;
        private BigDecimal capacidadToneladas;
        private String estado;
        private Boolean activo;
        private LocalDateTime fechaCreacion;
    }

    @Data
    public static class ConductorDto {
        private String id;
        private String dni;
        private String nombre;
        private String apellidos;
        private String telefono;
        private String email;
        private String licenciaNumero;
        private String licenciaCategoria;
        private LocalDate licenciaVencimiento;
        private Boolean activo;
        private LocalDateTime fechaCreacion;
        private String empresaId;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
    }

    @Data
    public static class TramoDto {
        private String id;
        private String viajeId;
        private Integer orden;
        private EstablecimientoDto establecimientoOrigen;
        private EstablecimientoDto establecimientoDestino;
        private String tipoActividad;
        private String descripcion;
        private LocalDateTime horaLlegadaProgramada;
        private LocalDateTime horaSalidaProgramada;
        private LocalDateTime horaLlegadaReal;
        private LocalDateTime horaSalidaReal;
        private String estado;
        private Integer slaMinutos;
        private String observaciones;
    }

    @Data
    public static class EstablecimientoDto {
        private String id;
        private String empresaId;
        private String nombre;
        private String tipo;
        private String direccion;
        private BigDecimal latitud;
        private BigDecimal longitud;
        private Boolean publico;
        private Boolean activo;
        @JsonProperty("configuracion_sla")
        private Object configuracionSla;
        @JsonProperty("fecha_creacion")
        private LocalDateTime fechaCreacion;
    }
}
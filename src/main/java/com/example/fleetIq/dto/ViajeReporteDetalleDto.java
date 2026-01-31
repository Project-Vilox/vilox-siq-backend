package com.example.fleetIq.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO consolidado con toda la información necesaria para generar el reporte de
 * trazabilidad
 */
@Data
public class ViajeReporteDetalleDto {
    // Información básica del viaje
    private String viajeId;
    private String codigoViaje;
    private String contenedor;
    private String documentoEmbarque;
    private String tipoOperacion;
    private String estado;
    private LocalDateTime fechaInicioProgramada;
    private LocalDateTime fechaFinProgramada;
    private LocalDateTime fechaInicioReal;
    private LocalDateTime fechaFinReal;
    private String observaciones;

    // Actores
    private EmpresaInfo cliente;
    private EmpresaInfo transportista;
    private EmpresaInfo operadorLogistico;
    private EmpresaInfo naviera;

    // Recursos
    private ConductorInfo conductor;
    private VehiculoInfo vehiculo;
    private VehiculoInfo carreta;

    // Ruta
    private List<TramoInfo> tramos;

    // Paradas detectadas
    private List<ParadaInfo> paradasDetectadas;

    // Evidencias
    private List<EvidenciaInfo> evidencias;

    // Métricas calculadas
    private MetricasViaje metricas;

    // Reporte GPS detallado
    private List<TrackGpsInfo> tracksGps;

    @Data
    public static class EmpresaInfo {
        private String nombre;
        private String ruc;
        private String tipoEmpresa;
    }

    @Data
    public static class ConductorInfo {
        private String nombre;
        private String licencia;
        private String telefono;
    }

    @Data
    public static class VehiculoInfo {
        private String placa;
        private String marca;
        private String modelo;
        private String tipoVehiculo;
    }

    @Data
    public static class TramoInfo {
        private Integer numero;
        private String nombreOrigen;
        private String nombreDestino;
        private String tipoOrigen;
        private String tipoDestino;
        private LocalDateTime fechaHoraCita1;
        private LocalDateTime fechaHoraCita2;
        private LocalDateTime horaInicioReal;
        private LocalDateTime horaFinReal;
        private Integer tardanzaCita1; // en minutos
        private Integer tardanzaCita2; // en minutos
        private Integer tiempoAtencionCita1; // en minutos
        private Integer tiempoAtencionCita2; // en minutos
        private Integer tiempoPermanenciaCita1; // en minutos - Tiempo total en el punto destino
        private Integer tiempoPermanenciaCita2; // en minutos - Tiempo total en el punto destino
        private String estado;
    }

    @Data
    public static class ParadaInfo {
        private Double latitud;
        private Double longitud;
        private String direccion;
        private LocalDateTime horaInicio;
        private LocalDateTime horaFin;
        private Integer duracionMinutos;
        private Boolean motorApagado;
        private String categoria; // ALTA, MEDIA, BAJA
        private Boolean justificada;
        private String observaciones;
    }

    @Data
    public static class EvidenciaInfo {
        private String hito; // INICIO_CARGA, FIN_CARGA, etc.
        private Integer secuencia;
        private String tipoAdjunto; // IMAGEN, PDF
        private String nombreArchivo;
        private LocalDateTime fechaUpload;
        private String urlArchivo; // URL pública o base64
        private byte[] imagenData; // Datos de la imagen para embedear en PDF
    }

    @Data
    public static class TrackGpsInfo {
        private LocalDateTime fechaHora;
        private Double latitud;
        private Double longitud;
        private Double velocidad;
        private Boolean motorEncendido;
        private String evento; // "En movimiento", "Detenido", etc.
    }

    @Data
    public static class MetricasViaje {
        private Integer kilometrajeTotal;
        private Integer tiempoTotalMinutos;
        private Integer paradasTotales;
        private Integer paradasJustificadas;
        private Double porcentajeCumplimiento;
        private Integer tardanzaTotalMinutos;
    }
}

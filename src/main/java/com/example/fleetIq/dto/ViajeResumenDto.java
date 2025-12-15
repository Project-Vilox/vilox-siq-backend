package com.example.fleetIq.dto;

import java.time.LocalDateTime;
// Usamos Lombok para simplificar el código, si está disponible.
// Si no usas Lombok, asegúrate de generar todos los Getters y Setters manualmente.
// import lombok.Data; // Descomentar si usas Lombok

// @Data // Descomentar si usas Lombok
public class ViajeResumenDto {
    private String id;
    private String codigoViaje;
    private String estado;
    private LocalDateTime fechaInicioProgramada;
    private LocalDateTime fechaFinProgramada; // Campo solicitado
    private String placaVehiculo;
    private String nombreEmpresaTransportista;
    private String nombreOrigenRuta; // Campo solicitado (Origen)
    private String nombreDestinoRuta; // Campo solicitado (Destino)

    // ⭐ CONSTRUCTOR PARA PROYECCIÓN JPQL (EL ORDEN DEBE COINCIDIR CON LA QUERY)
    public ViajeResumenDto(String id, String codigoViaje, String estado, LocalDateTime fechaInicioProgramada,
            LocalDateTime fechaFinProgramada, String placaVehiculo,
            String nombreEmpresaTransportista, String nombreOrigenRuta, String nombreDestinoRuta) {
        this.id = id;
        this.codigoViaje = codigoViaje;
        this.estado = estado;
        this.fechaInicioProgramada = fechaInicioProgramada;
        this.fechaFinProgramada = fechaFinProgramada;
        this.placaVehiculo = placaVehiculo;
        this.nombreEmpresaTransportista = nombreEmpresaTransportista;
        this.nombreOrigenRuta = nombreOrigenRuta;
        this.nombreDestinoRuta = nombreDestinoRuta;
    }

    // ⭐ GETTERS NECESARIOS (si no usa Lombok)

    public String getId() {
        return id;
    }

    public String getCodigoViaje() {
        return codigoViaje;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFechaInicioProgramada() {
        return fechaInicioProgramada;
    }

    public LocalDateTime getFechaFinProgramada() {
        return fechaFinProgramada;
    }

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public String getNombreEmpresaTransportista() {
        return nombreEmpresaTransportista;
    }

    public String getNombreOrigenRuta() {
        return nombreOrigenRuta;
    }

    public String getNombreDestinoRuta() {
        return nombreDestinoRuta;
    }

    // Opcional: Agregar Setters si es necesario, aunque en un DTO de lectura
    // (resumen) no suelen usarse.
}
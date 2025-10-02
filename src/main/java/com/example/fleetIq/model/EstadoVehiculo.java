package com.example.fleetIq.model;

public enum EstadoVehiculo {
    ACTIVO,
    MANTENIMIENTO,
    REPARACION,
    INACTIVO;

    public static EstadoVehiculo fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Estado no puede ser nulo");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No enum constant com.example.fleetIq.model.EstadoVehiculo." + value);
        }
    }
}
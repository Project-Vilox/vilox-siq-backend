package com.example.fleetIq.service;

import com.example.fleetIq.dto.TramoDto;

import java.util.List;

public interface TramoService {
    List<TramoDto> listarTramosPorViaje(String viajeId);
    void crearTramo(TramoDto tramoDto);
}
package com.example.fleetIq.service;

import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.model.Alarm;
import java.util.List;

public interface TramoService {
    List<TramoDto> listarTramosPorViaje(String viajeId);

    void crearTramo(TramoDto tramoDto);

    void procesarAlarma(Alarm alarm);
}
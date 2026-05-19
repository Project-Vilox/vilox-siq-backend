package com.example.fleetIq.service;

import com.example.fleetIq.dto.ParadaDetectadaDto;
import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.dto.ViajeDto;
import com.example.fleetIq.model.Alarm;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Tramo;

import java.util.List;

// En TramoService.java
public interface TramoService {
    // ✅ Ambos métodos devuelven el mismo tipo de objeto
    List<TramoDto> listarTramosPorViaje(String viajeId);

    TramoDto convertToDto(Tramo tramo);

    void crearTramo(TramoDto tramoDto);

    void procesarAlarma(Alarm alarm);

    void enriquecerConEtaYAvance(TramoDto tramoDto, Tramo tramo);

    List<ParadaDetectadaDto> analizarParadasDelTramo(String tramoId, List<Track> tracks);

    /** Resuelve lat/lon de un establecimiento: usa coordenadas existentes o calcula centroide de geocerca. */
    double[] resolverCoordenadasEstablecimiento(String establecimientoId);

}
package com.example.fleetIq.service;

import com.example.fleetIq.dto.EstablecimientoDto;
import java.util.List;

public interface EstablecimientoService {

    List<EstablecimientoDto> getAllEstablecimientos();
    List<EstablecimientoDto> getEstablecimientosByEmpresa(String empresaId);
    List<EstablecimientoDto> getEstablecimientosByTipo(String tipo);
    List<EstablecimientoDto> getEstablecimientosPublicos();
    EstablecimientoDto getEstablecimientoById(String id);
}

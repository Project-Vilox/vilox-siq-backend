package com.example.fleetIq.service;

import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.dto.ViajeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ViajeService {
    Viaje guardarViaje(Viaje viaje);

    List<ViajeDto> listarTodosLosViajes();

    Page<Viaje> listarTodosLosViajesPaginado(Pageable pageable);

    // [ACTUALIZADO] Interfaz actualizada para devolver DTO
    List<ViajeDto> listarViajesPorCodigo(String codigoViaje);

    List<ViajeDto> listarViajesPorEmpresa(String empresaId);

    List<ViajeDto> listarViajesPorOperador(String operadorId);

    List<ViajeDto> listarViajesPorCliente(String clienteId);

    List<ViajeDto> listarViajesId(String operadorId);
}
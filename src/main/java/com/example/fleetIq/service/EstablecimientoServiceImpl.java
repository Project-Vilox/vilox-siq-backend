package com.example.fleetIq.service;

import com.example.fleetIq.dto.EstablecimientoDto;
import com.example.fleetIq.model.Establecimiento;
import com.example.fleetIq.repository.EstablecimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstablecimientoServiceImpl implements EstablecimientoService {

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoDto> getAllEstablecimientos() {
        List<Establecimiento> establecimientos = establecimientoRepository.findByActivoTrue();
        return establecimientos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoDto> getEstablecimientosByEmpresa(String empresaId) {
        List<Establecimiento> establecimientos = establecimientoRepository.findByEmpresaId(empresaId);
        return establecimientos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoDto> getEstablecimientosByTipo(String tipo) {
        List<Establecimiento> establecimientos = establecimientoRepository.findByTipo(tipo);
        return establecimientos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoDto> getEstablecimientosPublicos() {
        List<Establecimiento> establecimientos = establecimientoRepository.findByPublicoTrueAndActivoTrue();
        return establecimientos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EstablecimientoDto getEstablecimientoById(String id) {
        return establecimientoRepository.findById(id)
                .map(this::convertToDto)
                .orElse(null);
    }

    private EstablecimientoDto convertToDto(Establecimiento establecimiento) {
        return new EstablecimientoDto(
                establecimiento.getId(),
                establecimiento.getEmpresaId(),
                establecimiento.getNombre(),
                establecimiento.getTipo(),
                establecimiento.getDireccion(),
                establecimiento.getLatitud(),
                establecimiento.getLongitud(),
                establecimiento.getPublico(),
                establecimiento.getActivo(),
                establecimiento.getConfiguracionSla(),
                establecimiento.getFechaCreacion()
        );
    }
}
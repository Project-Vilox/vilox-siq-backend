package com.example.fleetIq.service;

import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.model.Establecimiento;
import com.example.fleetIq.repository.TramoRepository;
import com.example.fleetIq.repository.ViajeRepository;
import com.example.fleetIq.repository.EstablecimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.OptimisticLockException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TramoServiceImpl implements TramoService {

    @Autowired
    private TramoRepository tramoRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Override
    public List<TramoDto> listarTramosPorViaje(String viajeId) {
        return tramoRepository.findByViajeId(viajeId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void crearTramo(TramoDto tramoDto) {
        try {
            Tramo tramo = new Tramo();
            // No asignar ID manualmente, dejar que JPA lo genere
            tramo.setOrden(tramoDto.getOrden());
            tramo.setTipoActividad(Tramo.TipoActividad.valueOf(tramoDto.getTipoActividad()));
            tramo.setDescripcion(tramoDto.getDescripcion());
            tramo.setHoraLlegadaProgramada(tramoDto.getHoraLlegadaProgramada());
            tramo.setHoraSalidaProgramada(tramoDto.getHoraSalidaProgramada());
            tramo.setHoraLlegadaReal(tramoDto.getHoraLlegadaReal());
            tramo.setHoraSalidaReal(tramoDto.getHoraSalidaReal());
            tramo.setEstado(Tramo.EstadoTramo.valueOf(tramoDto.getEstado()));
            tramo.setSlaMinutos(tramoDto.getSlaMinutos());
            tramo.setObservaciones(tramoDto.getObservaciones());

            // Asignar relaciones
            Viaje viaje = viajeRepository.findById(tramoDto.getViajeId())
                    .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado: " + tramoDto.getViajeId()));
            tramo.setViaje(viaje);
            viaje.getTramos().add(tramo); // Mantener consistencia bidireccional

            Establecimiento establecimientoOrigen = establecimientoRepository.findById(tramoDto.getEstablecimientoOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Establecimiento origen no encontrado: " + tramoDto.getEstablecimientoOrigenId()));
            tramo.setEstablecimientoOrigen(establecimientoOrigen);

            Establecimiento establecimientoDestino = establecimientoRepository.findById(tramoDto.getEstablecimientoDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Establecimiento destino no encontrado: " + tramoDto.getEstablecimientoDestinoId()));
            tramo.setEstablecimientoDestino(establecimientoDestino);

            tramoRepository.save(tramo);
            viajeRepository.save(viaje); // Guardar el viaje para persistir la relación
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Error de concurrencia al crear el tramo: el registro fue modificado por otra transacción", e);
        }
    }

    private TramoDto convertToDto(Tramo tramo) {
        TramoDto dto = new TramoDto();
        dto.setId(tramo.getId());
        dto.setViajeId(tramo.getViaje().getId());
        dto.setOrden(tramo.getOrden());
        dto.setEstablecimientoOrigenId(tramo.getEstablecimientoOrigen().getId());
        dto.setEstablecimientoDestinoId(tramo.getEstablecimientoDestino().getId());
        dto.setTipoActividad(tramo.getTipoActividad().name());
        dto.setDescripcion(tramo.getDescripcion());
        dto.setHoraLlegadaProgramada(tramo.getHoraLlegadaProgramada());
        dto.setHoraSalidaProgramada(tramo.getHoraSalidaProgramada());
        dto.setHoraLlegadaReal(tramo.getHoraLlegadaReal());
        dto.setHoraSalidaReal(tramo.getHoraSalidaReal());
        dto.setEstado(tramo.getEstado().name());
        dto.setSlaMinutos(tramo.getSlaMinutos());
        dto.setObservaciones(tramo.getObservaciones());
        return dto;
    }
}
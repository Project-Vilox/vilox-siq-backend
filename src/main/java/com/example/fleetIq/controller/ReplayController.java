package com.example.fleetIq.controller;

import com.example.fleetIq.dto.TrackDto;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.repository.ViajeRepository;
import com.example.fleetIq.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private static final Logger logger = LoggerFactory.getLogger(ReplayController.class);
    private static final ZoneId PERU_ZONE = ZoneId.of("America/Lima");

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private TrackService trackService;

    @GetMapping("/{viajeId}")
    public ResponseEntity<List<TrackDto>> getReplayByViaje(@PathVariable String viajeId) {
        logger.info("🎬 Replay solicitado para viajeId: {}", viajeId);

        Optional<Viaje> opt = viajeRepository.findById(viajeId);
        if (opt.isEmpty()) {
            logger.warn("⚠️ Viaje no encontrado: {}", viajeId);
            return ResponseEntity.notFound().build();
        }

        Viaje viaje = opt.get();

        if (viaje.getVehiculo() == null) {
            logger.warn("⚠️ El viaje {} no tiene vehículo asignado", viajeId);
            return ResponseEntity.badRequest().build();
        }

        // Usar fechas reales si existen, sino las programadas
        LocalDateTime inicio = viaje.getFechaInicioReal() != null
                ? viaje.getFechaInicioReal()
                : viaje.getFechaInicioProgramada();

        LocalDateTime fin = viaje.getFechaFinReal() != null
                ? viaje.getFechaFinReal()
                : viaje.getFechaFinProgramada();

        if (inicio == null || fin == null) {
            logger.warn("⚠️ El viaje {} no tiene fechas definidas", viajeId);
            return ResponseEntity.badRequest().build();
        }

        Long beginTime = inicio.atZone(PERU_ZONE).toEpochSecond();
        Long endTime = fin.atZone(PERU_ZONE).toEpochSecond();

        logger.info("📋 Vehículo: {}, Rango: {} → {}", viaje.getVehiculo().getId(), inicio, fin);

        List<TrackDto> tracks = trackService.getTracksByVehiculoIdAndHearttimeBetween(
                viaje.getVehiculo().getId(), beginTime, endTime);

        logger.info("✅ Tracks encontrados para replay: {}", tracks.size());

        return ResponseEntity.ok(tracks);
    }
}

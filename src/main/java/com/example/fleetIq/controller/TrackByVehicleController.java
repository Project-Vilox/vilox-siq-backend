package com.example.fleetIq.controller;

import com.example.fleetIq.dto.TrackDto;
import com.example.fleetIq.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/tracks-by-vehicle")
public class TrackByVehicleController {

    private static final Logger logger = LoggerFactory.getLogger(TrackByVehicleController.class);

    @Autowired
    private TrackService trackService;

    @GetMapping
    public ResponseEntity<List<TrackDto>> getTracksByVehiculoId(
            @RequestParam String vehiculoId,
            @RequestParam String inicio,
            @RequestParam String fin) {
        try {
            logger.info("🔍 ===== PETICIÓN RECIBIDA =====");
            logger.info("📋 VehiculoId: {}", vehiculoId);
            logger.info("📅 Fecha inicio (string): {}", inicio);
            logger.info("📅 Fecha fin (string): {}", fin);

            LocalDateTime startDateTime = LocalDateTime.parse(inicio);
            LocalDateTime endDateTime = LocalDateTime.parse(fin);

            // ✅ CRÍTICO: Usar zona horaria de Perú (UTC-5)
            // El frontend envía fechas en hora local, debemos convertirlas correctamente
            ZoneId peruZone = ZoneId.of("America/Lima");
            Long beginTime = startDateTime.atZone(peruZone).toEpochSecond();
            Long endTime = endDateTime.atZone(peruZone).toEpochSecond();

            // Logs detallados para debugging
            logger.info("🕐 Conversión de timestamps:");
            logger.info("   Inicio: {} → {} (Unix)",
                    startDateTime, beginTime);
            logger.info("   Fin: {} → {} (Unix)",
                    endDateTime, endTime);
            logger.info("   Verificación inicio: {}",
                    Instant.ofEpochSecond(beginTime).atZone(peruZone)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            logger.info("   Verificación fin: {}",
                    Instant.ofEpochSecond(endTime).atZone(peruZone)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Llamar al servicio
            List<TrackDto> tracks = trackService.getTracksByVehiculoIdAndHearttimeBetween(
                    vehiculoId, beginTime, endTime);

            logger.info("✅ Tracks encontrados: {}", tracks.size());

            if (tracks.isEmpty()) {
                logger.warn("⚠️ No se encontraron tracks entre {} y {}",
                        Instant.ofEpochSecond(beginTime).atZone(peruZone),
                        Instant.ofEpochSecond(endTime).atZone(peruZone));
            } else {
                TrackDto first = tracks.get(0);
                TrackDto last = tracks.get(tracks.size() - 1);
                logger.info("📍 Primer track: gpstime={} ({}), lat={}, lng={}",
                        first.getGpstime(),
                        Instant.ofEpochSecond(first.getGpstime()).atZone(peruZone)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        first.getLatitude(),
                        first.getLongitude());
                logger.info("📍 Último track: gpstime={} ({}), lat={}, lng={}",
                        last.getGpstime(),
                        Instant.ofEpochSecond(last.getGpstime()).atZone(peruZone)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        last.getLatitude(),
                        last.getLongitude());
            }

            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<TrackDto> getLatestTrackByVehiculoId(@RequestParam String vehiculoId) {
        try {
            logger.info("🔍 Buscando último track para vehículo: {}", vehiculoId);
            TrackDto track = trackService.getLatestTrackByVehiculoId(vehiculoId);

            if (track != null) {
                ZoneId peruZone = ZoneId.of("America/Lima");
                logger.info("✅ Último track encontrado:");
                logger.info("   gpstime: {} ({})",
                        track.getGpstime(),
                        Instant.ofEpochSecond(track.getGpstime()).atZone(peruZone)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                logger.info("   Posición: lat={}, lng={}",
                        track.getLatitude(), track.getLongitude());
                logger.info("   Velocidad: {} km/h", track.getSpeed());
                return ResponseEntity.ok(track);
            } else {
                logger.warn("⚠️ No se encontró track para vehículo {}", vehiculoId);
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
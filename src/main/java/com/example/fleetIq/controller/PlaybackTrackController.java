package com.example.fleetIq.controller;

import com.example.fleetIq.model.Track;
import com.example.fleetIq.service.PlaybackTrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Controlador para manejar solicitudes de tracks GPS desde el endpoint /api/playback de la API de Protrack.
 */
@RestController
@RequestMapping("/api/playback-tracks")
public class PlaybackTrackController {

    @Autowired
    private PlaybackTrackService playbackTrackService;

    /**
     * Obtiene los tracks GPS para un IMEI y una fecha específica desde el endpoint /api/playback.
     *
     * @param imei El IMEI del dispositivo.
     * @param date La fecha en formato yyyy-MM-dd (ej. 2025-09-27).
     * @return Lista de objetos Track con los datos GPS.
     * @throws RuntimeException Si ocurre un error al obtener los tracks.
     */
    @GetMapping("/by-date")
    public List<Track> getPlaybackTracksByImeiAndDate(
            @RequestParam String imei,
            @RequestParam String date) {
        try {
            // Parsear la fecha (ej. "2025-09-27")
            LocalDate localDate = LocalDate.parse(date);
            // Convertir a timestamps Unix en UTC considerando la zona horaria -05:00
            long beginTime = localDate.atStartOfDay(ZoneOffset.ofHours(-5))
                    .toInstant().getEpochSecond() + 5 * 3600;
            long endTime = localDate.atTime(23, 59, 59).atZone(ZoneOffset.ofHours(-5))
                    .toInstant().getEpochSecond() + 5 * 3600;
            return playbackTrackService.getPlaybackTracksByImeiAndTime(imei, beginTime, endTime);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener los tracks de reproducción: " + e.getMessage());
        }
    }
}
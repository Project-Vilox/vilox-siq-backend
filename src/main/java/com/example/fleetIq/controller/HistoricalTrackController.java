package com.example.fleetIq.controller;

import com.example.fleetIq.service.HistoricalTrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/historical")
public class HistoricalTrackController {

    @Autowired
    private HistoricalTrackService historicalTrackService;

    @GetMapping("/fetch-tracks")
    public String fetchAndSaveHistoricalTracks(
            @RequestParam(required = false) Long beginTime,
            @RequestParam(required = false) Long endTime) {
        try {
            if (beginTime == null) {
                beginTime = Instant.now().getEpochSecond() - 24 * 60 * 60; // Últimas 24 horas por defecto
            }
            if (endTime == null) {
                endTime = Instant.now().getEpochSecond();
            }
            historicalTrackService.fetchAndSaveHistoricalTracks(beginTime, endTime);
            return "Tracks históricos cargados exitosamente en historical_track";
        } catch (Exception e) {
            return "Error al cargar tracks históricos: " + e.getMessage();
        }
    }
}
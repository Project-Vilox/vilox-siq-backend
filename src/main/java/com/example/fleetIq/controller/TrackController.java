package com.example.fleetIq.controller;

import com.example.fleetIq.model.Track;
import com.example.fleetIq.service.TrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    @Autowired
    private TrackService trackService;

    @GetMapping("/fetch")
    public String fetchAndSaveTracks(
            @RequestParam(required = false) Long beginTime,
            @RequestParam(required = false) Long endTime) {
        try {
            if (beginTime == null) {
                beginTime = System.currentTimeMillis() / 1000 - 24 * 60 * 60; // Últimas 24 horas
            }
            if (endTime == null) {
                endTime = System.currentTimeMillis() / 1000; // Hasta ahora
            }
            trackService.fetchAndSaveTracks(beginTime, endTime);
            return "Tracks cargados exitosamente en tracks";
        } catch (Exception e) {
            return "Error al cargar tracks: " + e.getMessage();
        }
    }

    @GetMapping
    public List<Track> getTracks(
            @RequestParam String imei,
            @RequestParam(required = false) Long beginTime,
            @RequestParam(required = false) Long endTime) {
        if (beginTime == null) {
            beginTime = 0L; // Desde el inicio si no se especifica
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis() / 1000; // Hasta ahora
        }
        return trackService.getTracksByImei(imei, beginTime, endTime);
    }
}
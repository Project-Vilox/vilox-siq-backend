package com.example.fleetIq.controller;

import com.example.fleetIq.dto.TrackDto;
import com.example.fleetIq.service.TrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/tracks-by-vehicle")
public class TrackByVehicleController {

    @Autowired
    private TrackService trackService;

    @GetMapping
    public ResponseEntity<List<TrackDto>> getTracksByVehiculoId(
            @RequestParam String vehiculoId,
            @RequestParam String inicio,
            @RequestParam String fin) {
        try {
            // Convert ISO 8601 date-time strings to Unix timestamps (seconds)
            LocalDateTime startDateTime = LocalDateTime.parse(inicio);
            LocalDateTime endDateTime = LocalDateTime.parse(fin);
            Long beginTime = startDateTime.toEpochSecond(ZoneOffset.UTC);
            Long endTime = endDateTime.toEpochSecond(ZoneOffset.UTC);

            // Fetch tracks
            List<TrackDto> tracks = trackService.getTracksByVehiculoIdAndHearttimeBetween(vehiculoId, beginTime, endTime);
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<TrackDto> getLatestTrackByVehiculoId(@RequestParam String vehiculoId) {
        try {
            TrackDto track = trackService.getLatestTrackByVehiculoId(vehiculoId);
            return track != null ? ResponseEntity.ok(track) : ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
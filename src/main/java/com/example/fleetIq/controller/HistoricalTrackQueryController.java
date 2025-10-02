package com.example.fleetIq.controller;

import com.example.fleetIq.model.HistoricalTrack;
import com.example.fleetIq.repository.HistoricalTrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/historical")
public class HistoricalTrackQueryController {

    @Autowired
    private HistoricalTrackRepository historicalTrackRepository;

    @GetMapping("/tracks")
    public List<HistoricalTrack> getHistoricalTracks(
            @RequestParam String imei,
            @RequestParam(required = false) Long beginTime,
            @RequestParam(required = false) Long endTime) {
        if (beginTime == null) {
            beginTime = 0L; // Desde el inicio si no se especifica
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis() / 1000; // Hasta ahora
        }
        return historicalTrackRepository.findByImeiAndTimeBetween(imei, beginTime, endTime);
    }
}
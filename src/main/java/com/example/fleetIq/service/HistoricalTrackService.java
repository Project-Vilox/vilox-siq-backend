package com.example.fleetIq.service;

public interface HistoricalTrackService {
    void fetchAndSaveHistoricalTracks(Long beginTime, Long endTime) throws Exception;
}
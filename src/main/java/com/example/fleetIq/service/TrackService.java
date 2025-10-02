package com.example.fleetIq.service;

import com.example.fleetIq.dto.TrackDto;
import com.example.fleetIq.model.Device;
import com.example.fleetIq.model.Track;

import java.util.List;

public interface TrackService {
    void fetchAndSaveTracks(Long beginTime, Long endTime) throws Exception;
    List<Track> getTracksByImei(String imei, Long beginTime, Long endTime);
    List<TrackDto> getTracksByVehiculoIdAndHearttimeBetween(String vehiculoId, Long beginTime, Long endTime);
    TrackDto getLatestTrackByVehiculoId(String vehiculoId);
}
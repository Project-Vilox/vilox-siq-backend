package com.example.fleetIq.service;

import com.example.fleetIq.model.Track;

import java.util.List;

/**
 * Interfaz del servicio para manejar operaciones de tracks GPS desde el endpoint /api/playback.
 */
public interface PlaybackTrackService {
    /**
     * Obtiene los tracks GPS para un IMEI en un rango de tiempo específico desde el endpoint /api/playback.
     *
     * @param imei      El IMEI del dispositivo.
     * @param beginTime Timestamp Unix de inicio (en segundos).
     * @param endTime   Timestamp Unix de fin (en segundos).
     * @return Lista de objetos Track con los datos GPS.
     * @throws Exception Si ocurre un error al comunicarse con la API.
     */
    List<Track> getPlaybackTracksByImeiAndTime(String imei, Long beginTime, Long endTime) throws Exception;
}
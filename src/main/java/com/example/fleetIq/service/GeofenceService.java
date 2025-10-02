package com.example.fleetIq.service;

import com.example.fleetIq.model.Geofence;

public interface GeofenceService {
    void createGeofence(Geofence geofence) throws Exception;
}
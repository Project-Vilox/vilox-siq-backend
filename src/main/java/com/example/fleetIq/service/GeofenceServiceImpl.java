package com.example.fleetIq.service;

import com.example.fleetIq.model.Geofence;
import com.example.fleetIq.repository.GeofenceRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeofenceServiceImpl implements GeofenceService {

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Override
    public void createGeofence(Geofence geofence) throws Exception {
        // Validate geofence data
        if (geofence.getImei() == null || geofence.getImei().isEmpty()) {
            throw new IllegalArgumentException("IMEI cannot be null or empty");
        }
        if (geofence.getName() == null || geofence.getName().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (geofence.getAlarmtype() == null || geofence.getAlarmtype() < 0 || geofence.getAlarmtype() > 2) {
            throw new IllegalArgumentException("Alarm type must be 0 (Out), 1 (In), or 2 (In/Out)");
        }
        if (geofence.getPoints() == null || geofence.getPoints().isEmpty()) {
            throw new IllegalArgumentException("Points cannot be null or empty");
        }
        // Validate points as JSON array of arrays
        try {
            JSONArray pointsArray = new JSONArray(geofence.getPoints());
            if (pointsArray.length() < 3) {
                throw new IllegalArgumentException("Polygon must have at least 3 points");
            }
            for (int i = 0; i < pointsArray.length(); i++) {
                JSONArray point = pointsArray.getJSONArray(i);
                if (point.length() != 2 || !point.get(0).toString().matches("-?\\d+(\\.\\d+)?") || !point.get(1).toString().matches("-?\\d+(\\.\\d+)?")) {
                    throw new IllegalArgumentException("Each point must be [lat, lon] with numeric values");
                }
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException("Points must be a valid JSON array of [lat, lon] arrays");
        }

        // Save to database
        geofenceRepository.save(geofence);
        System.out.println("Geofence created and saved for IMEI " + geofence.getImei());
    }
}
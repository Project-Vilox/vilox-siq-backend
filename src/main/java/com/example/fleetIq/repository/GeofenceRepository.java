package com.example.fleetIq.repository;

import com.example.fleetIq.model.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
}
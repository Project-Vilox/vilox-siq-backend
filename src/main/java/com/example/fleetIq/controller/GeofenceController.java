package com.example.fleetIq.controller;

import com.example.fleetIq.model.Geofence;
import com.example.fleetIq.service.GeofenceServiceImpl;
import com.example.fleetIq.repository.GeofenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
        "http://localhost:8080",
        "https://446ae7f42f09.ngrok-free.app",
        "https://923cc1cddf51.ngrok-free.app"
}, methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE })
public class GeofenceController {

    @Autowired
    private GeofenceServiceImpl geofenceService;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @PostMapping("/geofences")
    public ResponseEntity<String> createGeofence(@RequestBody Geofence geofence) {
        try {
            geofenceService.createGeofence(geofence);
            return ResponseEntity.ok("Geofence created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating geofence: " + e.getMessage());
        }
    }

    @GetMapping("/geofences")
    public List<Geofence> getGeofences() {
        return geofenceRepository.findAll();
    }

    /**
     * Obtener una geocerca por ID
     */
    @GetMapping("/geofences/{id}")
    public ResponseEntity<?> getGeofenceById(@PathVariable Long id) {
        try {
            Optional<Geofence> geofence = geofenceRepository.findById(id);
            if (geofence.isPresent()) {
                return ResponseEntity.ok(geofence.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching geofence: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Actualizar una geocerca existente
     */
    @PutMapping("/geofences/{id}")
    public ResponseEntity<String> updateGeofence(
            @PathVariable Long id,
            @RequestBody Geofence geofence) {
        try {
            geofenceService.updateGeofence(id, geofence);
            return ResponseEntity.ok("Geofence updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating geofence: " + e.getMessage());
        }
    }

    /**
     * Eliminar una geocerca
     */
    @DeleteMapping("/geofences/{id}")
    public ResponseEntity<String> deleteGeofence(@PathVariable Long id) {
        try {
            if (geofenceRepository.existsById(id)) {
                geofenceRepository.deleteById(id);
                return ResponseEntity.ok("Geofence deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting geofence: " + e.getMessage());
        }
    }
}
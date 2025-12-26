package com.example.fleetIq.repository;

import com.example.fleetIq.model.ViajeReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViajeReporteRepository extends JpaRepository<ViajeReporte, String> {
    // Verifica si ya existe un reporte para ese viaje ID específico
    boolean existsByViajeid(String viajeid);
}
package com.example.fleetIq.repository;

import com.example.fleetIq.model.EvidenciaViaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenciaViajeRepository extends JpaRepository<EvidenciaViaje, String> {
    List<EvidenciaViaje> findByIdViaje(String idViaje);
    Optional<EvidenciaViaje> findByIdViajeAndHitoAndSecuencia(String idViaje, EvidenciaViaje.Hito hito, Integer secuencia);
}
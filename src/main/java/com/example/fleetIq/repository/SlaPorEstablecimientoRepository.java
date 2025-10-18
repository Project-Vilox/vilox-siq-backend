package com.example.fleetIq.repository;

import com.example.fleetIq.model.SlaPorEstablecimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface SlaPorEstablecimientoRepository extends JpaRepository<SlaPorEstablecimiento, UUID> {
}
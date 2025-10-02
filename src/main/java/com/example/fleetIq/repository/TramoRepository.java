package com.example.fleetIq.repository;

import com.example.fleetIq.model.Tramo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TramoRepository extends JpaRepository<Tramo, String> {
    @Query("SELECT t FROM Tramo t JOIN FETCH t.viaje JOIN FETCH t.establecimientoOrigen JOIN FETCH t.establecimientoDestino WHERE t.viaje.id = :viajeId")
    List<Tramo> findByViajeId(@Param("viajeId") String viajeId);
}
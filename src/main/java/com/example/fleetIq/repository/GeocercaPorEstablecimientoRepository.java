package com.example.fleetIq.repository;

import com.example.fleetIq.model.GeocercaPorEstablecimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeocercaPorEstablecimientoRepository extends JpaRepository<GeocercaPorEstablecimiento, Long> {

    // Obtener todas las geocercas de un establecimiento
    List<GeocercaPorEstablecimiento> findByEstablecimientoId(String establecimientoId);

    // Obtener geocerca específica por tipo
    Optional<GeocercaPorEstablecimiento> findByEstablecimientoIdAndTipo(String establecimientoId, String tipo);

    // // Obtener ID de geocerca externa
    // @Query("SELECT g.geocercaId FROM GeocercaPorEstablecimiento g WHERE
    // g.establecimientoId = :establecimientoId AND g.tipo = 'EXTERNA'")
    // Optional<Long> findGeocercaExternaId(@Param("establecimientoId") String
    // establecimientoId);

    // // Obtener ID de geocerca interna
    // @Query("SELECT g.geocercaId FROM GeocercaPorEstablecimiento g WHERE
    // g.establecimientoId = :establecimientoId AND g.tipo = 'INTERNA'")
    // Optional<Long> findGeocercaInternaId(@Param("establecimientoId") String
    // establecimientoId);

    // Obtener ID de geocerca externa
    @Query(value = "SELECT g.geocercaId FROM GeocercaPorEstablecimiento g " +
            "WHERE g.establecimientoId = :establecimientoId AND g.tipo = 'EXTERNA' " +
            "ORDER BY g.orden ASC LIMIT 1") // Añadida cláusula ORDER BY y LIMIT 1
    Optional<Long> findGeocercaExternaId(@Param("establecimientoId") String establecimientoId);

    // Obtener ID de geocerca interna
    @Query(value = "SELECT g.geocercaId FROM GeocercaPorEstablecimiento g " +
            "WHERE g.establecimientoId = :establecimientoId AND g.tipo = 'INTERNA' " +
            "ORDER BY g.orden ASC LIMIT 1") // Añadida cláusula ORDER BY y LIMIT 1
    Optional<Long> findGeocercaInternaId(@Param("establecimientoId") String establecimientoId);

    // Verificar si una geocerca está asociada a un establecimiento
    boolean existsByEstablecimientoIdAndGeocercaId(String establecimientoId, Long geocercaId);
}
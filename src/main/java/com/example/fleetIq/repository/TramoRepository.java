package com.example.fleetIq.repository;

import com.example.fleetIq.model.Tramo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TramoRepository extends JpaRepository<Tramo, String> {

  @Query("SELECT t FROM Tramo t " +
      "JOIN FETCH t.viaje " +
      "JOIN FETCH t.establecimientoOrigen " +
      "JOIN FETCH t.establecimientoDestino " +
      "WHERE t.viaje.id = :viajeId")
  List<Tramo> findByViajeId(@Param("viajeId") String viajeId);

  /**
   * 🆕 Encuentra el tramo activo (pendiente o en_curso) de un vehículo por IMEI
   * Retorna el tramo con menor orden (el primero pendiente)
   */
  @Query("SELECT t FROM Tramo t " +
      "JOIN FETCH t.viaje v " +
      "JOIN FETCH v.vehiculo veh " +
      "JOIN FETCH t.establecimientoOrigen " +
      "JOIN FETCH t.establecimientoDestino " +
      "WHERE veh.imei = :imei " +
      "AND t.estado IN ('pendiente', 'en_curso') " +
      "ORDER BY t.orden ASC")
  List<Tramo> findTramosActivosPorVehiculo(@Param("imei") String imei);
  // ^^^ Cambiado a List<Tramo> y renombrado para mayor claridad
}
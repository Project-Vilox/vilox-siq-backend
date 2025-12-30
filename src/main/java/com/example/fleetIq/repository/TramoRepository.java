package com.example.fleetIq.repository;

import com.example.fleetIq.model.Tramo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TramoRepository extends JpaRepository<Tramo, String> {

  @Query("SELECT t FROM Tramo t " +
      "JOIN FETCH t.viaje " +
      "JOIN FETCH t.establecimientoOrigen " +
      "JOIN FETCH t.establecimientoDestino " +
      "WHERE t.viaje.id = :viajeId " +
      "ORDER BY t.orden ASC")
  List<Tramo> findByViajeId(@Param("viajeId") String viajeId);

  /**
   * Encuentra los tramos activos de un vehículo por IMEI
   * Solo busca tramos de viajes activos (pendiente o en_curso)
   * Ordena por fecha de inicio del viaje (más reciente primero) y orden del tramo
   */
  @Query("SELECT t FROM Tramo t " + // ⬅️ Simplemente quita el DISTINCT
      "JOIN FETCH t.viaje v " +
      "JOIN FETCH v.vehiculo veh " +
      "JOIN FETCH t.establecimientoOrigen " +
      "JOIN FETCH t.establecimientoDestino " +
      "WHERE veh.imei = :imei " +
      "AND v.estado IN ('pendiente', 'en_curso') " +
      "AND t.estado IN ('pendiente', 'en_curso') " +
      "ORDER BY v.fechaInicioProgramada DESC, t.orden ASC")
  List<Tramo> findTramosActivosPorVehiculo(@Param("imei") String imei);

  @Query("SELECT t FROM Tramo t " +
           "LEFT JOIN FETCH t.viaje v " +
           "LEFT JOIN FETCH v.vehiculo " +
           "WHERE t.id = :tramoId")
    Optional<Tramo> findByIdWithRelations(@Param("tramoId") String tramoId);
}

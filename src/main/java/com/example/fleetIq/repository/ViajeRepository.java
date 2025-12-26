package com.example.fleetIq.repository;

import java.util.Optional;

import com.example.fleetIq.dto.ViajeResumenDto;
import com.example.fleetIq.model.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ViajeRepository extends JpaRepository<Viaje, String> {
    List<Viaje> findByCodigoViaje(String codigoViaje);

    @Query("SELECT v FROM Viaje v " +
            "JOIN FETCH v.empresaTransportista et " +
            "LEFT JOIN FETCH v.empresaOperador eo " +
            "JOIN FETCH v.empresaCliente ec " +
            "JOIN FETCH v.vehiculo ve " +
            "JOIN FETCH v.carreta ca " +
            "JOIN FETCH v.conductor co " +
            "LEFT JOIN FETCH v.empresaNaviera en " +
            "LEFT JOIN FETCH v.tramos t " +
            "LEFT JOIN FETCH t.establecimientoOrigen " +
            "LEFT JOIN FETCH t.establecimientoDestino " +
            "WHERE v.empresaTransportista.id = :empresaId")
    List<Viaje> findByEmpresaTransportistaId(@Param("empresaId") String empresaId);

    @Query("SELECT v FROM Viaje v " +
            "JOIN FETCH v.tramos t " +
            "JOIN FETCH t.establecimientoOrigen " + // ⬅️ Crucial
            "JOIN FETCH t.establecimientoDestino " + // ⬅️ Crucial
            "WHERE v.id = :id")
    Optional<Viaje> findByIdWithTramos(@Param("id") String id);

    @Query("SELECT v FROM Viaje v " +
            "JOIN FETCH v.empresaTransportista et " +
            "LEFT JOIN FETCH v.empresaOperador eo " +
            "JOIN FETCH v.empresaCliente ec " +
            "JOIN FETCH v.vehiculo ve " +
            "JOIN FETCH v.carreta ca " +
            "JOIN FETCH v.conductor co " +
            "LEFT JOIN FETCH v.empresaNaviera en " +
            "LEFT JOIN FETCH v.tramos t " +
            "LEFT JOIN FETCH t.establecimientoOrigen " +
            "LEFT JOIN FETCH t.establecimientoDestino")
    List<Viaje> findAllWithRelations();

    @Query("SELECT v FROM Viaje v " +
            "JOIN FETCH v.empresaTransportista et " +
            "LEFT JOIN FETCH v.empresaOperador eo " +
            "JOIN FETCH v.empresaCliente ec " +
            "JOIN FETCH v.vehiculo ve " +
            "JOIN FETCH v.carreta ca " +
            "JOIN FETCH v.conductor co " +
            "LEFT JOIN FETCH v.empresaNaviera en " +
            "LEFT JOIN FETCH v.tramos t " +
            "LEFT JOIN FETCH t.establecimientoOrigen " +
            "LEFT JOIN FETCH t.establecimientoDestino " +
            "WHERE v.id = :empresaId")
    List<Viaje> findByEmpresaId(@Param("empresaId") String empresaId);

    @Query("SELECT v FROM Viaje v " +
            "LEFT JOIN FETCH v.empresaTransportista et " +
            "LEFT JOIN FETCH v.empresaOperador eo " +
            "LEFT JOIN FETCH v.empresaCliente ec " +
            "LEFT JOIN FETCH v.vehiculo ve " +
            "LEFT JOIN FETCH v.carreta ca " +
            "LEFT JOIN FETCH v.conductor co " +
            "LEFT JOIN FETCH v.empresaNaviera en " +
            "LEFT JOIN FETCH v.tramos t " +
            "LEFT JOIN FETCH t.establecimientoOrigen " +
            "LEFT JOIN FETCH t.establecimientoDestino " +
            "WHERE v.empresaOperador.id = :operadorId")
    List<Viaje> findByEmpresaOperadorId(@Param("operadorId") String operadorId);

    @Query("SELECT v FROM Viaje v " +
            "JOIN FETCH v.empresaTransportista et " +
            "LEFT JOIN FETCH v.empresaOperador eo " +
            "JOIN FETCH v.empresaCliente ec " +
            "JOIN FETCH v.vehiculo ve " +
            "JOIN FETCH v.carreta ca " +
            "JOIN FETCH v.conductor co " +
            "LEFT JOIN FETCH v.empresaNaviera en " +
            "LEFT JOIN FETCH v.tramos t " +
            "LEFT JOIN FETCH t.establecimientoOrigen " +
            "LEFT JOIN FETCH t.establecimientoDestino " +
            "WHERE v.empresaCliente.id = :clienteId")
    List<Viaje> findByEmpresaClienteId(@Param("clienteId") String clienteId);

    /**
     * Consulta optimizada (SELECT new DTO) para listar viajes resumen.
     * Carga solo los campos necesarios, incluyendo el origen (del primer tramo)
     * y el destino (del último tramo) en la misma query.
     */
    @Query("SELECT new com.example.fleetIq.dto.ViajeResumenDto(" +
            "    v.id, v.codigoViaje, v.estado, v.fechaInicioProgramada, v.fechaFinProgramada, " +
            "    ve.placa, et.nombre, " +
            "    t_origen.establecimientoOrigen.nombre, " + // Origen del Primer Tramo
            "    t_destino.establecimientoDestino.nombre) " + // Destino del Último Tramo
            "FROM Viaje v " +
            "JOIN v.empresaTransportista et " +
            "JOIN v.vehiculo ve " +

            // JOIN al PRIMER TRAMO para obtener el Origen
            "JOIN v.tramos t_origen " +
            "JOIN t_origen.establecimientoOrigen " +

            // JOIN al ÚLTIMO TRAMO para obtener el Destino
            "JOIN v.tramos t_destino " +
            "JOIN t_destino.establecimientoDestino " +

            "WHERE v.empresaTransportista.id = :empresaId " +
            "AND t_origen.orden = 1 " +
            "AND t_destino.orden = (SELECT MAX(t.orden) FROM Tramo t WHERE t.viaje.id = v.id) " +

            "ORDER BY v.fechaInicioProgramada DESC")
    // El método retorna el DTO ligero
    List<ViajeResumenDto> findViajesResumenByEmpresaTransportistaId(@Param("empresaId") String empresaId);

}
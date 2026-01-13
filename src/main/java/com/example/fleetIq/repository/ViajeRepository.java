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

    /**
     * ✅ PASO 1: Carga el viaje con TRAMOS y sus establecimientos
     * SIN DISTINCT para evitar error con columnas JSON
     */
    @Query("""
                SELECT v FROM Viaje v
                LEFT JOIN FETCH v.tramos t
                LEFT JOIN FETCH t.establecimientoOrigen
                LEFT JOIN FETCH t.establecimientoDestino
                LEFT JOIN FETCH v.conductor c
                LEFT JOIN FETCH v.vehiculo veh
                LEFT JOIN FETCH veh.empresa
                LEFT JOIN FETCH v.carreta car
                LEFT JOIN FETCH car.empresa
                LEFT JOIN FETCH v.empresaTransportista
                LEFT JOIN FETCH v.empresaOperador
                LEFT JOIN FETCH v.empresaCliente
                LEFT JOIN FETCH v.empresaNaviera
                WHERE v.id = :id
            """)
    Optional<Viaje> findByIdWithTramos(@Param("id") String id);

    /**
     * ✅ PASO 2: Carga conductorEmpresas en query separado
     */
    @Query("""
                SELECT v FROM Viaje v
                LEFT JOIN FETCH v.conductor c
                LEFT JOIN FETCH c.conductorEmpresas ce
                LEFT JOIN FETCH ce.empresa
                WHERE v.id = :id
            """)
    Optional<Viaje> findByIdWithConductorEmpresas(@Param("id") String id);

    /**
     * 🔥 Query simplificado para carga básica (sin tramos)
     */
    @Query("""
                SELECT v FROM Viaje v
                LEFT JOIN FETCH v.conductor c
                LEFT JOIN FETCH v.vehiculo veh
                LEFT JOIN FETCH veh.empresa
                LEFT JOIN FETCH v.carreta car
                LEFT JOIN FETCH car.empresa
                LEFT JOIN FETCH v.empresaTransportista
                LEFT JOIN FETCH v.empresaOperador
                LEFT JOIN FETCH v.empresaCliente
                LEFT JOIN FETCH v.empresaNaviera
                WHERE v.id = :id
            """)
    Optional<Viaje> findByIdWithBasicRelations(@Param("id") String id);

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

    @Query("SELECT new com.example.fleetIq.dto.ViajeResumenDto(" +
            "    v.id, v.codigoViaje, v.estado, v.fechaInicioProgramada, v.fechaFinProgramada, " +
            "    ve.placa, et.nombre, " +
            "    t_origen.establecimientoOrigen.nombre, " +
            "    t_destino.establecimientoDestino.nombre) " +
            "FROM Viaje v " +
            "JOIN v.empresaTransportista et " +
            "JOIN v.vehiculo ve " +
            "JOIN v.tramos t_origen " +
            "JOIN t_origen.establecimientoOrigen " +
            "JOIN v.tramos t_destino " +
            "JOIN t_destino.establecimientoDestino " +
            "WHERE v.empresaTransportista.id = :empresaId " +
            "AND t_origen.orden = 1 " +
            "AND t_destino.orden = (SELECT MAX(t.orden) FROM Tramo t WHERE t.viaje.id = v.id) " +
            "ORDER BY v.fechaInicioProgramada DESC")
    List<ViajeResumenDto> findViajesResumenByEmpresaTransportistaId(@Param("empresaId") String empresaId);

    @Query("SELECT new com.example.fleetIq.dto.ViajeResumenDto(" +
            "    v.id, v.codigoViaje, v.estado, v.fechaInicioProgramada, v.fechaFinProgramada, " +
            "    ve.placa, et.nombre, " +
            "    t_origen.establecimientoOrigen.nombre, " +
            "    t_destino.establecimientoDestino.nombre) " +
            "FROM Viaje v " +
            "JOIN v.empresaTransportista et " +
            "JOIN v.vehiculo ve " +
            "JOIN v.tramos t_origen " +
            "JOIN t_origen.establecimientoOrigen " +
            "JOIN v.tramos t_destino " +
            "JOIN t_destino.establecimientoDestino " +
            "WHERE v.empresaOperador.id = :operadorId " +
            "AND t_origen.orden = 1 " +
            "AND t_destino.orden = (SELECT MAX(t.orden) FROM Tramo t WHERE t.viaje.id = v.id) " +
            "ORDER BY v.fechaInicioProgramada DESC")
    List<ViajeResumenDto> findViajesResumenByEmpresaOperadorId(@Param("operadorId") String operadorId);

    @Query("SELECT new com.example.fleetIq.dto.ViajeResumenDto(" +
            "    v.id, v.codigoViaje, v.estado, v.fechaInicioProgramada, v.fechaFinProgramada, " +
            "    ve.placa, et.nombre, " +
            "    t_origen.establecimientoOrigen.nombre, " +
            "    t_destino.establecimientoDestino.nombre) " +
            "FROM Viaje v " +
            "JOIN v.empresaTransportista et " +
            "JOIN v.vehiculo ve " +
            "JOIN v.tramos t_origen " +
            "JOIN t_origen.establecimientoOrigen " +
            "JOIN v.tramos t_destino " +
            "JOIN t_destino.establecimientoDestino " +
            "WHERE v.empresaCliente.id = :clienteId " +
            "AND t_origen.orden = 1 " +
            "AND t_destino.orden = (SELECT MAX(t.orden) FROM Tramo t WHERE t.viaje.id = v.id) " +
            "ORDER BY v.fechaInicioProgramada DESC")
    List<ViajeResumenDto> findViajesResumenByEmpresaClienteId(@Param("clienteId") String clienteId);
}
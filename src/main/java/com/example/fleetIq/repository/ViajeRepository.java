package com.example.fleetIq.repository;

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
}
package com.example.fleetIq.repository;

import com.example.fleetIq.model.OperadoresDeClientes;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperadoresDeClientesRepository extends JpaRepository<OperadoresDeClientes, String> {

    @EntityGraph(attributePaths = {"empresaOperador", "empresaCliente"})
    @Query("SELECT oc FROM OperadoresDeClientes oc " +
            "WHERE oc.empresaOperador.id IN (" +
            "  SELECT t.empresaOperador.id FROM TransportistasDeOperadores t " +
            "  WHERE t.empresaTransportista.id = :transportistaId" +
            ") AND oc.estado = 'ACTIVO'")
    List<OperadoresDeClientes> findOperadoresAndClientesByTransportistaId(@Param("transportistaId") String transportistaId);
}
package com.example.fleetIq.repository;

import com.example.fleetIq.model.TransportistasDeOperadores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportistasDeOperadoresRepository extends JpaRepository<TransportistasDeOperadores, String> {

    @Query("SELECT t FROM TransportistasDeOperadores t WHERE t.empresaOperador.id = :operadorId AND t.estado = 'ACTIVO'")
    List<TransportistasDeOperadores> findByEmpresaOperadorId(@Param("operadorId") String operadorId);
}
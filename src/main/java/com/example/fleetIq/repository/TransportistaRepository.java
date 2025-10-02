package com.example.fleetIq.repository;

import com.example.fleetIq.model.Empresa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransportistaRepository extends JpaRepository<Empresa, String> {

    @EntityGraph(attributePaths = {
            "vehiculos",
            "carretas",
            "conductorEmpresas.conductor"
    })
    @Query("SELECT e FROM Empresa e WHERE e.id = :id AND e.tipoEmpresa = :tipoEmpresa")
    Optional<Empresa> findByIdAndTipoEmpresa(@Param("id") String id, @Param("tipoEmpresa") String tipoEmpresa);
}
package com.example.fleetIq.repository;

import com.example.fleetIq.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperadorLogisticoRepository extends JpaRepository<Empresa, String> {

   /* @Query("SELECT e FROM Empresa e " +
            "LEFT JOIN FETCH e.empresasAdministradas " +
            "LEFT JOIN FETCH e.vehiculos " +
            "LEFT JOIN FETCH e.carretas " +  // Agregado para fetch de carretas
            "LEFT JOIN FETCH e.conductorEmpresas ce " +
            "LEFT JOIN FETCH ce.conductor " +
            "WHERE e.id = :id")
    Empresa findOperadorCompletoById(@Param("id") String id);

    @Query("SELECT e FROM Empresa e " +
            "LEFT JOIN FETCH e.empresasAdministradas " +
            "LEFT JOIN FETCH e.vehiculos " +
            "LEFT JOIN FETCH e.carretas " +  // Agregado para fetch de carretas
            "LEFT JOIN FETCH e.conductorEmpresas ce " +
            "LEFT JOIN FETCH ce.conductor " +
            "WHERE e.empresaAdministradora.id = :empresaId AND e.tipoEmpresa = :tipoEmpresa")
    List<Empresa> findOperadoresByAdministradoraId(@Param("empresaId") String empresaId, @Param("tipoEmpresa") String tipoEmpresa); */
}
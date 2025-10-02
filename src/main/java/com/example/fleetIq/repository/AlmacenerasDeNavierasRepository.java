package com.example.fleetIq.repository;

import com.example.fleetIq.model.AlmacenerasDeNavieras;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlmacenerasDeNavierasRepository extends JpaRepository<AlmacenerasDeNavieras, String> {

    @EntityGraph(attributePaths = {"empresaNaviera", "empresaAlmacenera"})
    @Query("SELECT an FROM AlmacenerasDeNavieras an WHERE an.estado = 'ACTIVO'")
    List<AlmacenerasDeNavieras> findAllActive();
}
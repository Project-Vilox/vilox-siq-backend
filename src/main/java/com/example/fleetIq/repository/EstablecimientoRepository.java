package com.example.fleetIq.repository;
import com.example.fleetIq.model.Establecimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EstablecimientoRepository extends JpaRepository<Establecimiento, String> {

    List<Establecimiento> findByActivoTrue();
    List<Establecimiento> findByTipo(String tipo);
    List<Establecimiento> findByEmpresaId(String empresaId);
    List<Establecimiento> findByPublicoTrueAndActivoTrue();
}
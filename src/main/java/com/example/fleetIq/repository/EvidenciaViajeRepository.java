package com.example.fleetIq.repository;

import com.example.fleetIq.dto.EvidenciaViajeResponse;
import com.example.fleetIq.model.EvidenciaViaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvidenciaViajeRepository extends JpaRepository<EvidenciaViaje, String> {

    // ✅ Query para LISTAR (sin BLOB)
    @Query("SELECT new com.example.fleetIq.dto.EvidenciaViajeResponse(" +
            "e.id, e.idViaje, e.hito, e.secuencia, e.tipoAdjunto, " +
            "e.nombreArchivo, e.contentType, e.tamanioArchivo, " +
            "e.fechaCreacion, e.fechaActualizacion) " +
            "FROM EvidenciaViaje e WHERE e.idViaje = :viajeId " +
            "ORDER BY e.secuencia ASC")
    List<EvidenciaViajeResponse> findMetadataByIdViaje(@Param("viajeId") String viajeId);

    // ✅ Query para obtener solo el BLOB
    @Query("SELECT e.adjunto FROM EvidenciaViaje e WHERE e.id = :id")
    byte[] findAdjuntoById(@Param("id") String id);

    Optional<EvidenciaViaje> findByIdViajeAndHitoAndSecuencia(
            String idViaje,
            EvidenciaViaje.Hito hito,
            Integer secuencia);
}
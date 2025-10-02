
        package com.example.fleetIq.dto;

import com.example.fleetIq.model.EvidenciaViaje;
import lombok.Data;

@Data
public class EliminarEvidenciaRequest {
    private String idViaje;
    private EvidenciaViaje.Hito hito;
    private Integer secuencia;
}
package com.example.fleetIq.service;

import com.example.fleetIq.dto.OperadorLogisticoInfoResponse;

public interface OperadorLogisticoService {
    OperadorLogisticoInfoResponse getOperadorLogisticoInfo(String operadorId);
}
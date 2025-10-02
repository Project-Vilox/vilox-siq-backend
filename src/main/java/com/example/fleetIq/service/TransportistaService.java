package com.example.fleetIq.service;

import com.example.fleetIq.dto.TransportistaInfoResponse;

public interface TransportistaService {
    TransportistaInfoResponse getTransportistaInfo(String transportistaId);
}
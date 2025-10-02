package com.example.fleetIq.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class GenericResponse {
    private String message;
    private Map<String, Object> data;

    public GenericResponse() {
        this.data = new HashMap<>();
    }

    public GenericResponse(String message) {
        this.message = message;
        this.data = new HashMap<>();
    }

    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
}
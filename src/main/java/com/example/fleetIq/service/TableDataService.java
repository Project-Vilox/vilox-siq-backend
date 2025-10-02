package com.example.fleetIq.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TableDataService {
    private final JdbcTemplate jdbcTemplate;

    public TableDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getAllTables() {
        return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'fleetiq'", String.class);
    }

    public List<Map<String, Object>> getTableData(String tableName, int page, int size) {
        if (!getAllTables().contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException("Tabla inválida");
        }
        int offset = page * size;
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName + " LIMIT ? OFFSET ?", size, offset);
    }

    public int getTotalRows(String tableName) {
        if (!getAllTables().contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException("Tabla inválida");
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    public List<Map<String, Object>> getAllTableData(String tableName) {
        if (!getAllTables().contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException("Tabla inválida");
        }
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
    }

    // Método para ejecutar query SQL personalizado (usado por vertablas.html y query.html)
    public List<Map<String, Object>> executeQuery(String sql) {
        try {
            // Validación básica: solo permitir SELECT para seguridad (opcional, ajusta según necesidades)
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                throw new IllegalArgumentException("Solo se permiten consultas SELECT por seguridad.");
            }
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error en la consulta: " + e.getMessage());
        }
    }
}
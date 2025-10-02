package com.example.fleetIq.controller;

import com.example.fleetIq.service.TableDataService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "https://923cc1cddf51.ngrok-free.app") // Ajusta si la URL de ngrok cambia
public class TableController {
    private final TableDataService tableDataService;

    public TableController(TableDataService tableDataService) {
        this.tableDataService = tableDataService;
    }

    @GetMapping("/tables")
    public String showTables() {
        return "forward:/web/vertablas.html";
    }

    @GetMapping("/query")
    public String showQueryPage() {
        return "forward:/web/query.html"; // Mantener para la página de queries original si sigue en uso
    }

    @GetMapping("/api/tables/data")
    public Map<String, Object> getTableData(@RequestParam(value = "tableName", required = false) String tableName,
                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "50") int size) {
        Map<String, Object> response = new HashMap<>();
        response.put("tables", tableDataService.getAllTables());
        if (tableName != null && !tableName.isEmpty()) {
            try {
                response.put("data", tableDataService.getTableData(tableName, page, size));
                response.put("tableName", tableName);
                response.put("total", tableDataService.getTotalRows(tableName));
                response.put("page", page);
                response.put("size", size);
            } catch (Exception e) {
                response.put("error", e.getMessage());
            }
        }
        return response;
    }

    // Endpoint para ejecutar query SQL personalizado (usado por vertablas.html y query.html)
    @PostMapping("/api/query/execute")
    public ResponseEntity<Map<String, Object>> executeCustomQuery(@RequestBody Map<String, String> request) {
        String sql = request.get("query");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query requerido"));
        }
        try {
            List<Map<String, Object>> data = tableDataService.executeQuery(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tables/export")
    public void exportTableData(@RequestParam("tableName") String tableName, HttpServletResponse response) throws IOException {
        System.out.println("Iniciando exportación de tabla: " + tableName);
        if (!tableDataService.getAllTables().contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException("Tabla inválida");
        }

        List<Map<String, Object>> data = tableDataService.getAllTableData(tableName);
        System.out.println("Datos recuperados: " + data.size() + " registros");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Datos");

        Row headerRow = sheet.createRow(0);
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            int columnIndex = 0;
            for (String columnName : firstRow.keySet()) {
                Cell cell = headerRow.createCell(columnIndex++);
                cell.setCellValue(columnName);
            }
        }

        int rowIndex = 1;
        for (Map<String, Object> rowData : data) {
            Row dataRow = sheet.createRow(rowIndex++);
            int columnIndex = 0;
            for (Object value : rowData.values()) {
                Cell cell = dataRow.createCell(columnIndex++);
                if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + tableName + "_data.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
        System.out.println("Exportación completada");
    }
}
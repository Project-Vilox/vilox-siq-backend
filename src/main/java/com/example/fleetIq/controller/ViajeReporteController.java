package com.example.fleetIq.controller;

import com.example.fleetIq.service.ViajeReportePdfService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para generación de reportes en PDF
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ViajeReporteController {

    private static final Logger logger = LoggerFactory.getLogger(ViajeReporteController.class);
    private final ViajeReportePdfService pdfService;

    /**
     * Descarga el reporte de trazabilidad de un viaje en formato PDF
     * 
     * @param viajeId ID del viaje
     * @return PDF del reporte
     */
    @GetMapping("/viaje/{viajeId}/pdf")
    public ResponseEntity<byte[]> descargarReportePdf(@PathVariable String viajeId) {
        try {
            logger.info("📥 Solicitando reporte PDF para viaje: {}", viajeId);

            // Generar PDF
            byte[] pdfBytes = pdfService.generarReportePdf(viajeId);

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_viaje_" + viajeId + ".pdf");
            headers.setContentLength(pdfBytes.length);

            logger.info("✅ Reporte PDF generado exitosamente para viaje: {} ({} bytes)", viajeId, pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("❌ Error al generar reporte PDF para viaje {}: {}", viajeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el reporte PDF en formato inline para vista previa en navegador
     * 
     * @param viajeId ID del viaje
     * @return PDF del reporte para vista previa
     */
    @GetMapping("/viaje/{viajeId}/pdf-preview")
    public ResponseEntity<byte[]> previsualizarReportePdf(@PathVariable String viajeId) {
        try {
            logger.info("👁️ Solicitando vista previa PDF para viaje: {}", viajeId);

            // Generar PDF
            byte[] pdfBytes = pdfService.generarReportePdf(viajeId);

            // Preparar headers para visualización inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reporte_viaje_" + viajeId + ".pdf\"");
            headers.setContentLength(pdfBytes.length);

            logger.info("✅ Vista previa PDF generada para viaje: {}", viajeId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("❌ Error al generar vista previa PDF para viaje {}: {}", viajeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

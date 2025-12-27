package com.example.fleetIq.controller;

import com.example.fleetIq.dto.ParadaDetectadaDto;
import com.example.fleetIq.model.Track;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.repository.TrackRepository;
import com.example.fleetIq.repository.TramoRepository;
import com.example.fleetIq.service.TramoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/analisis-paradas")
@CrossOrigin(origins = {
    "http://localhost:8080",
    "https://446ae7f42f09.ngrok-free.app",
    "https://923cc1cddf51.ngrok-free.app"
}, methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE })
public class AnalisisParadasController {

  @Autowired
  private TramoService tramoService;

  @Autowired
  private TramoRepository tramoRepository;

  @Autowired
  private TrackRepository trackRepository;

  /**
   * 🔍 Endpoint: Analizar paradas de un tramo
   * 
   * GET /api/analisis-paradas/tramo/{tramoId}
   */
  @Transactional(readOnly = true)
  @GetMapping("/tramo/{tramoId}")
  public ResponseEntity<?> analizarParadasTramo(@PathVariable String tramoId) {
    try {
      // 1. Obtener el tramo
      Tramo tramo = tramoRepository.findById(tramoId)
          .orElse(null);

      if (tramo == null) {
        return ResponseEntity.notFound().build();
      }

      // 2. Obtener el IMEI del vehículo
      String imei = tramo.getViaje().getVehiculo().getImei();

      if (imei == null) {
        return ResponseEntity.badRequest()
            .body("El tramo no tiene un vehículo asociado");
      }

      // 3. Obtener los tracks del período del tramo
      LocalDateTime inicio = tramo.getHoraLlegadaReal() != null
          ? tramo.getHoraLlegadaReal()
          : tramo.getViaje().getFechaInicioProgramada();

      LocalDateTime fin = tramo.getHoraSalidaRealDestino() != null
          ? tramo.getHoraSalidaRealDestino()
          : LocalDateTime.now();

      long timestampInicio = inicio.atZone(ZoneId.systemDefault()).toEpochSecond();
      long timestampFin = fin.atZone(ZoneId.systemDefault()).toEpochSecond();

      List<Track> tracks = trackRepository.findLatestTracksByImeiInTimeRange(
          imei,
          timestampInicio,
          timestampFin);

      if (tracks.isEmpty()) {
        return ResponseEntity.ok()
            .body(List.of()); // Sin datos GPS
      }

      // 4. Analizar paradas
      List<ParadaDetectadaDto> paradas = tramoService
          .analizarParadasDelTramo(tramoId, tracks);

      return ResponseEntity.ok(paradas);

    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body("Error analizando paradas: " + e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @GetMapping("/tramo/{tramoId}/resumen")
  public ResponseEntity<?> resumenParadasTramo(@PathVariable String tramoId) {
    try {
      Tramo tramo = tramoRepository.findById(tramoId).orElse(null);

      if (tramo == null) {
        return ResponseEntity.notFound().build();
      }

      String imei = tramo.getViaje().getVehiculo().getImei();

      LocalDateTime inicio = tramo.getHoraLlegadaReal() != null
          ? tramo.getHoraLlegadaReal()
          : tramo.getViaje().getFechaInicioProgramada();

      LocalDateTime fin = tramo.getHoraSalidaRealDestino() != null
          ? tramo.getHoraSalidaRealDestino()
          : LocalDateTime.now();

      long timestampInicio = inicio.atZone(ZoneId.systemDefault()).toEpochSecond();
      long timestampFin = fin.atZone(ZoneId.systemDefault()).toEpochSecond();

      List<Track> tracks = trackRepository.findLatestTracksByImeiInTimeRange(
          imei, timestampInicio, timestampFin);

      List<ParadaDetectadaDto> paradas = tramoService
          .analizarParadasDelTramo(tramoId, tracks);

      // Crear resumen
      ResumenParadas resumen = new ResumenParadas();
      resumen.totalParadas = paradas.size();
      resumen.paradasJustificadas = (int) paradas.stream()
          .filter(ParadaDetectadaDto::esJustificada)
          .count();
      resumen.paradasInjustificadas = resumen.totalParadas - resumen.paradasJustificadas;
      resumen.tiempoTotalMinutos = paradas.stream()
          .mapToInt(ParadaDetectadaDto::getDuracionMinutos)
          .sum();

      // Parada más larga
      resumen.paradaMasLarga = paradas.stream()
          .max((a, b) -> Integer.compare(a.getDuracionMinutos(), b.getDuracionMinutos()))
          .map(p -> p.getMotivo() + " (" + p.getDuracionMinutos() + " min)")
          .orElse("N/A");

      // Desglose por categoría
      resumen.porCategoria = paradas.stream()
          .collect(java.util.stream.Collectors.groupingBy(
              ParadaDetectadaDto::getCategoria,
              java.util.stream.Collectors.summingInt(ParadaDetectadaDto::getDuracionMinutos)));

      return ResponseEntity.ok(resumen);

    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body("Error generando resumen: " + e.getMessage());
    }
  }

  // Clase auxiliar para el resumen
  public static class ResumenParadas {
    public int totalParadas;
    public int paradasJustificadas;
    public int paradasInjustificadas;
    public int tiempoTotalMinutos;
    public String paradaMasLarga;
    public java.util.Map<String, Integer> porCategoria;
  }
}

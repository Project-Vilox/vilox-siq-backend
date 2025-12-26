package com.example.fleetIq.service;

import com.example.fleetIq.dto.ParadaDetectadaDto;
import com.example.fleetIq.model.Track;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DetectorParadasMejorado {

  // ========================================================================
  // CONFIGURACIÓN DE DETECCIÓN
  // ========================================================================

  private static final double VELOCIDAD_MAXIMA_KMH = 5.0;
  private static final int DURACION_MINIMA_SEGUNDOS = 120;
  private static final double RADIO_PARADA_METROS = 50.0;
  private static final double DISTANCIA_MINIMA_ENTRE_PARADAS = 200.0;
  private static final int GAP_MAXIMO_DATOS_SEGUNDOS = 600;

  public List<ParadaDetectadaDto> detectarParadasConMotivo(List<Track> tracks) {
    if (tracks == null || tracks.isEmpty()) {
      return Collections.emptyList();
    }

    System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
    System.out.println("║  🔍 DETECTOR DE PARADAS v2.2 (SIN accstatus)");
    System.out.println("╠════════════════════════════════════════════════════════════════╣");
    System.out.println("║  📊 Tracks: " + tracks.size());
    System.out.println("║  ⚙️ Velocidad máx: " + VELOCIDAD_MAXIMA_KMH + " km/h");
    System.out.println("║  ⏱️ Duración mín: " + (DURACION_MINIMA_SEGUNDOS / 60) + " min");
    System.out.println("║  📍 Radio: " + RADIO_PARADA_METROS + " m");
    System.out.println("║  🔧 Criterios: Velocidad + Odómetro + GPS");
    System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

    List<Track> tracksOrdenados = tracks.stream()
        .sorted(Comparator.comparing(Track::getGpstime))
        .collect(Collectors.toList());

    List<ParadaDetectadaDto> paradas = new ArrayList<>();
    ParadaCandidato paradaActual = null;

    for (int i = 0; i < tracksOrdenados.size(); i++) {
      Track track = tracksOrdenados.get(i);
      Track trackAnterior = i > 0 ? tracksOrdenados.get(i - 1) : null;

      // ====================================================================
      // CRITERIOS DE DETECCIÓN (SIN usar accstatus)
      // ====================================================================

      // CRITERIO 1: Velocidad = 0 o muy baja ⭐⭐⭐⭐⭐
      boolean velocidadCero = track.getSpeed() != null && track.getSpeed() <= VELOCIDAD_MAXIMA_KMH;

      // CRITERIO 2: Odómetro sin cambios ⭐⭐⭐⭐⭐
      boolean odometroSinCambio = false;
      if (trackAnterior != null &&
          track.getMileage() != null && trackAnterior.getMileage() != null) {
        long diferenciaOdometro = Math.abs(track.getMileage() - trackAnterior.getMileage());
        odometroSinCambio = diferenciaOdometro < 100; // < 100 metros
      }

      // CRITERIO 3: Kilometraje del día sin cambios ⭐⭐⭐⭐
      boolean kmDiaSinCambio = false;
      if (trackAnterior != null &&
          track.getTodaymileage() != null && trackAnterior.getTodaymileage() != null) {
        long diferenciaKmDia = Math.abs(track.getTodaymileage() - trackAnterior.getTodaymileage());
        kmDiaSinCambio = diferenciaKmDia < 100; // < 100 metros
      }

      // CRITERIO 4: GPS sin movimiento ⭐⭐⭐
      boolean gpsSinMovimiento = false;
      if (trackAnterior != null) {
        double distanciaGPS = calcularDistancia(
            trackAnterior.getLatitude(), trackAnterior.getLongitude(),
            track.getLatitude(), track.getLongitude());
        gpsSinMovimiento = distanciaGPS < 20.0; // < 20 metros
      }

      // ====================================================================
      // LÓGICA DE DECISIÓN
      // ====================================================================

      // Alta confianza: Velocidad baja + Odómetro sin cambio + GPS quieto
      boolean altaConfianza = velocidadCero && odometroSinCambio && gpsSinMovimiento;

      // Media confianza: Velocidad baja + (Odómetro O GPS sin cambio)
      boolean mediaConfianza = velocidadCero && (odometroSinCambio || gpsSinMovimiento);

      // Baja confianza: 3 de 4 criterios
      int criterios = 0;
      if (velocidadCero)
        criterios++;
      if (odometroSinCambio)
        criterios++;
      if (kmDiaSinCambio)
        criterios++;
      if (gpsSinMovimiento)
        criterios++;
      boolean bajaConfianza = criterios >= 3;

      boolean esParada = altaConfianza || mediaConfianza || bajaConfianza;

      // Debug cada 10 tracks
      if (i % 10 == 0) {
        System.out.println("📍 Track #" + i + " | " +
            "Speed:" + String.format("%.1f", track.getSpeed() != null ? track.getSpeed() : 0.0) + " | " +
            "Odómetro:" + (odometroSinCambio ? "SIN_CAMBIO" : "CAMBIÓ") + " | " +
            "GPS:" + (gpsSinMovimiento ? "QUIETO" : "MOVIÓ") + " | " +
            "Parada:" + (esParada ? "SÍ" : "NO"));
      }

      // ====================================================================
      // GESTIÓN DE PARADAS
      // ====================================================================

      if (esParada) {
        if (paradaActual == null) {
          // Iniciar nueva parada
          paradaActual = new ParadaCandidato();
          paradaActual.trackInicio = track;
          paradaActual.timestampInicio = track.getGpstime();
          paradaActual.timestampFin = track.getGpstime();
          paradaActual.latitud = track.getLatitude();
          paradaActual.longitud = track.getLongitude();
          paradaActual.mileageInicio = track.getMileage();
          paradaActual.todayMileageInicio = track.getTodaymileage();

          System.out.println("\n🆕 Nueva parada iniciada:");
          System.out.println("   🕐 " + convertirTimestamp(track.getGpstime()));
          System.out.println("   📍 [" + track.getLatitude() + ", " + track.getLongitude() + "]");
          System.out.println("   🏎️ Speed: " + track.getSpeed() + " km/h");

        } else {
          // Verificar si pertenece a la misma parada
          double distanciaAlInicio = calcularDistancia(
              paradaActual.latitud, paradaActual.longitud,
              track.getLatitude(), track.getLongitude());

          long gapTiempo = track.getGpstime() - paradaActual.timestampFin;

          boolean mismaParadaOdometro = true;
          if (paradaActual.mileageInicio != null && track.getMileage() != null) {
            long cambioOdometro = Math.abs(track.getMileage() - paradaActual.mileageInicio);
            mismaParadaOdometro = cambioOdometro < 200;
          }

          if (distanciaAlInicio <= RADIO_PARADA_METROS &&
              gapTiempo <= GAP_MAXIMO_DATOS_SEGUNDOS &&
              mismaParadaOdometro) {

            // Extender parada actual
            paradaActual.timestampFin = track.getGpstime();
            paradaActual.trackFin = track;
            paradaActual.mileageFin = track.getMileage();
            paradaActual.todayMileageFin = track.getTodaymileage();
            paradaActual.actualizarCentroide(track.getLatitude(), track.getLongitude());

          } else {
            // Cerrar parada actual
            if (distanciaAlInicio > RADIO_PARADA_METROS) {
              System.out.println("   ⚠️ Distancia excedida: " + String.format("%.0f", distanciaAlInicio) + "m");
            }
            if (!mismaParadaOdometro) {
              System.out.println("   ⚠️ Odómetro cambió demasiado");
            }

            cerrarParadaCandidato(paradaActual, paradas);

            // Iniciar nueva
            paradaActual = new ParadaCandidato();
            paradaActual.trackInicio = track;
            paradaActual.timestampInicio = track.getGpstime();
            paradaActual.timestampFin = track.getGpstime();
            paradaActual.latitud = track.getLatitude();
            paradaActual.longitud = track.getLongitude();
            paradaActual.mileageInicio = track.getMileage();
            paradaActual.todayMileageInicio = track.getTodaymileage();
          }
        }

      } else {
        // Movimiento → cerrar parada
        if (paradaActual != null) {
          System.out.println("   🚗 Movimiento detectado - cerrando parada");
          cerrarParadaCandidato(paradaActual, paradas);
          paradaActual = null;
        }
      }
    }

    // Cerrar última parada si existe
    if (paradaActual != null) {
      cerrarParadaCandidato(paradaActual, paradas);
    }

    // Post-procesamiento
    paradas = filtrarParadasInvalidas(paradas);
    paradas = mergearParadasCercanas(paradas);
    imprimirResumenDeteccion(paradas);

    return paradas;
  }

  private void cerrarParadaCandidato(ParadaCandidato candidato, List<ParadaDetectadaDto> paradas) {
    if (candidato.timestampFin == null) {
      candidato.timestampFin = candidato.timestampInicio;
    }

    long duracionSegundos = candidato.timestampFin - candidato.timestampInicio;

    if (duracionSegundos >= DURACION_MINIMA_SEGUNDOS) {
      ParadaDetectadaDto parada = new ParadaDetectadaDto();

      parada.setLatitud(candidato.getLatitudPromedio());
      parada.setLongitud(candidato.getLongitudPromedio());
      parada.setTimestampInicio(candidato.timestampInicio);
      parada.setTimestampFin(candidato.timestampFin);
      parada.setDuracionMinutos((int) (duracionSegundos / 60));

      // ✅ SIEMPRE establecer motorApagado como false para evitar NULL
      parada.setMotorApagado(false);

      parada.setHoraInicio(convertirTimestamp(candidato.timestampInicio));
      parada.setHoraFin(convertirTimestamp(candidato.timestampFin));

      if (candidato.mileageInicio != null && candidato.mileageFin != null) {
        long distanciaOdometro = Math.abs(candidato.mileageFin - candidato.mileageInicio);
        parada.setDistanciaRecorridaMetros((int) distanciaOdometro);
      }

      paradas.add(parada);

      System.out.println("✅ Parada cerrada:");
      System.out.println("   ⏱️ Duración: " + parada.getDuracionMinutos() + " min");
      if (parada.getDistanciaRecorridaMetros() != null) {
        System.out.println("   📏 Odómetro: " + parada.getDistanciaRecorridaMetros() + "m");
      }
      System.out.println("   🕐 " + parada.getHoraInicio() + " → " + parada.getHoraFin());

    } else {
      System.out.println("⏭️ Parada descartada: muy corta (" + (duracionSegundos / 60) + " min)");
    }
  }

  private List<ParadaDetectadaDto> filtrarParadasInvalidas(List<ParadaDetectadaDto> paradas) {
    return paradas.stream()
        .filter(p -> {
          if (p.getLatitud() == null || p.getLongitud() == null) {
            return false;
          }
          if (Math.abs(p.getLatitud()) < 0.001 || Math.abs(p.getLongitud()) < 0.001) {
            System.out.println("⚠️ Parada descartada: coordenadas inválidas");
            return false;
          }
          return true;
        })
        .collect(Collectors.toList());
  }

  private List<ParadaDetectadaDto> mergearParadasCercanas(List<ParadaDetectadaDto> paradas) {
    if (paradas.size() <= 1) {
      return paradas;
    }

    List<ParadaDetectadaDto> paradasMergeadas = new ArrayList<>();
    ParadaDetectadaDto paradaActual = paradas.get(0);

    for (int i = 1; i < paradas.size(); i++) {
      ParadaDetectadaDto paradaSiguiente = paradas.get(i);

      double distancia = calcularDistancia(
          paradaActual.getLatitud(), paradaActual.getLongitud(),
          paradaSiguiente.getLatitud(), paradaSiguiente.getLongitud());

      long tiempoEntrePradas = paradaSiguiente.getTimestampInicio() - paradaActual.getTimestampFin();

      if (distancia < DISTANCIA_MINIMA_ENTRE_PARADAS && tiempoEntrePradas < 300) {
        System.out.println("🔀 Mergeando paradas cercanas (" +
            String.format("%.0f", distancia) + "m, " +
            (tiempoEntrePradas / 60) + " min)");

        paradaActual.setTimestampFin(paradaSiguiente.getTimestampFin());
        paradaActual.setHoraFin(paradaSiguiente.getHoraFin());

        long nuevaDuracion = paradaActual.getTimestampFin() - paradaActual.getTimestampInicio();
        paradaActual.setDuracionMinutos((int) (nuevaDuracion / 60));

      } else {
        paradasMergeadas.add(paradaActual);
        paradaActual = paradaSiguiente;
      }
    }

    paradasMergeadas.add(paradaActual);
    return paradasMergeadas;
  }

  private void imprimirResumenDeteccion(List<ParadaDetectadaDto> paradas) {
    System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
    System.out.println("║  📊 RESUMEN DE DETECCIÓN");
    System.out.println("╠════════════════════════════════════════════════════════════════╣");
    System.out.println("║  Total paradas detectadas: " + paradas.size());

    if (!paradas.isEmpty()) {
      int duracionTotal = paradas.stream()
          .mapToInt(ParadaDetectadaDto::getDuracionMinutos)
          .sum();

      OptionalInt duracionMax = paradas.stream()
          .mapToInt(ParadaDetectadaDto::getDuracionMinutos)
          .max();

      System.out.println("║  Duración total: " + duracionTotal + " min (" +
          (duracionTotal / 60) + "h " + (duracionTotal % 60) + "min)");
      System.out.println("║  Duración promedio: " + (duracionTotal / paradas.size()) + " min");
      System.out.println("║  Parada más larga: " +
          (duracionMax.isPresent() ? duracionMax.getAsInt() + " min" : "N/A"));

      System.out.println("╠════════════════════════════════════════════════════════════════╣");
      System.out.println("║  DETALLE DE PARADAS:");

      for (int i = 0; i < paradas.size(); i++) {
        ParadaDetectadaDto p = paradas.get(i);
        System.out.println("║  " + (i + 1) + ". " +
            p.getHoraInicio().toString() + " | " +
            p.getDuracionMinutos() + " min");
      }
    }

    System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
  }

  private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
    final int EARTH_RADIUS = 6371000;

    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;
  }

  private LocalDateTime convertirTimestamp(Long timestamp) {
    return LocalDateTime.ofInstant(
        Instant.ofEpochSecond(timestamp),
        ZoneId.systemDefault());
  }

  private static class ParadaCandidato {
    Track trackInicio;
    Track trackFin;
    Long timestampInicio;
    Long timestampFin;
    double latitud;
    double longitud;
    Long mileageInicio;
    Long mileageFin;
    Long todayMileageInicio;
    Long todayMileageFin;

    private List<Double> latitudes = new ArrayList<>();
    private List<Double> longitudes = new ArrayList<>();

    ParadaCandidato() {
      latitudes.add(latitud);
      longitudes.add(longitud);
    }

    void actualizarCentroide(double lat, double lon) {
      latitudes.add(lat);
      longitudes.add(lon);
    }

    double getLatitudPromedio() {
      return latitudes.stream()
          .mapToDouble(Double::doubleValue)
          .average()
          .orElse(latitud);
    }

    double getLongitudPromedio() {
      return longitudes.stream()
          .mapToDouble(Double::doubleValue)
          .average()
          .orElse(longitud);
    }
  }
}
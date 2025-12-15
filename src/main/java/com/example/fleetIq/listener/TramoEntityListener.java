package com.example.fleetIq.listener;

import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TramoEntityListener {

  private static ApplicationEventPublisher eventPublisher;

  @Autowired
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    TramoEntityListener.eventPublisher = eventPublisher;
  }

  @PrePersist
  @PreUpdate
  public void calcularMetricas(Tramo tramo) {
    System.out.println("📊 Calculando métricas para Tramo ID: " + tramo.getId() + " (Orden: " + tramo.getOrden() + ")");

    // Calcular métricas del ORIGEN (Cita 1) - Siempre que haya datos
    if (tramo.getHoraLlegadaReal() != null && tramo.getHoraSalidaReal() != null) {
      calcularMetricasOrigen(tramo);
    }

    // Calcular métricas del DESTINO (Cita 2) - Solo si el tramo está completado
    if (tramo.getEstado() == Tramo.EstadoTramo.completado &&
        tramo.getHoraLlegadaRealDestino() != null &&
        tramo.getHoraSalidaRealDestino() != null) {
      calcularMetricasDestino(tramo);
    }
  }

  @PostUpdate
  public void onTramoUpdate(Tramo tramo) {
    System.out.println("🔄 TramoEntityListener: Tramo actualizado ID: " + tramo.getId());
    System.out.println("   Estado: " + tramo.getEstado());

    try {
      Viaje viaje = tramo.getViaje();
      if (viaje == null) {
        System.out.println("⚠️ Viaje no encontrado para tramo: " + tramo.getId());
        return;
      }

      List<Tramo> tramos = viaje.getTramos();
      tramos.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));

      String nuevoEstado = calcularEstadoViaje(tramos);
      String estadoActual = viaje.getEstado();

      System.out.println("🔍 Estado actual viaje: " + estadoActual);
      System.out.println("🔍 Estado calculado: " + nuevoEstado);

      if (!nuevoEstado.equals(estadoActual)) {
        eventPublisher.publishEvent(new ViajeUpdateEvent(this, viaje.getId()));
        System.out.println("📢 EVENTO PUBLICADO para actualizar Viaje ID: " + viaje.getId());
      }

    } catch (Exception e) {
      System.err.println("❌ Error actualizando viaje desde listener: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * 📍 CALCULA MÉTRICAS DEL ESTABLECIMIENTO ORIGEN (CITA 1)
   * Se ejecuta cuando el vehículo sale del origen
   */
  private void calcularMetricasOrigen(Tramo tramo) {
    System.out.println("📍 Calculando métricas del ORIGEN: " +
        (tramo.getEstablecimientoOrigen() != null ? tramo.getEstablecimientoOrigen().getNombre() : "N/A"));

    LocalDateTime llegadaProgramada = tramo.getHoraLlegadaProgramada();
    LocalDateTime llegadaReal = tramo.getHoraLlegadaReal();
    LocalDateTime salidaReal = tramo.getHoraSalidaReal();
    LocalDateTime salidaProgramada = tramo.getHoraSalidaProgramada();

    // 1️⃣ TARDANZA EN ORIGEN
    if (llegadaProgramada != null && llegadaReal != null) {
      long tardanzaMinutos = Duration.between(llegadaProgramada, llegadaReal).toMinutes();
      tramo.setTardanzaCita1((int) Math.max(0, tardanzaMinutos));

      System.out.println("   🕐 Tardanza Origen: " + tramo.getTardanzaCita1() + " min " +
          (tramo.getTardanzaCita1() == 0 ? "(A TIEMPO ✓)" : "(RETRASO ⚠)"));
    }

    // 2️⃣ PERMANENCIA EN ORIGEN
    if (llegadaReal != null && salidaReal != null) {
      long permanenciaMinutos = Duration.between(llegadaReal, salidaReal).toMinutes();
      tramo.setTiempoPermanenciaCita1((int) permanenciaMinutos);

      System.out.println("   ⏱️ Permanencia Origen: " + tramo.getTiempoPermanenciaCita1() + " min");
    }

    // 3️⃣ TIEMPO DE ATENCIÓN EN ORIGEN
    if (llegadaReal != null && salidaReal != null &&
        llegadaProgramada != null && salidaProgramada != null) {

      long permanenciaReal = Duration.between(llegadaReal, salidaReal).toMinutes();
      long permanenciaProgramada = Duration.between(llegadaProgramada, salidaProgramada).toMinutes();
      long tiempoAtencion = Math.abs(permanenciaReal - permanenciaProgramada);

      tramo.setTiempoAtencionCita1((int) tiempoAtencion);

      String evaluacion = permanenciaReal > permanenciaProgramada ? "(TOMÓ MÁS TIEMPO ⚠)" : "(MÁS RÁPIDO ✓)";

      System.out.println("   🎯 Tiempo Atención Origen: " + tramo.getTiempoAtencionCita1() + " min " + evaluacion);
    }
  }

  /**
   * 🎯 CALCULA MÉTRICAS DEL ESTABLECIMIENTO DESTINO (CITA 2)
   * Se ejecuta cuando el tramo se completa (sale del destino)
   */
  private void calcularMetricasDestino(Tramo tramo) {
    System.out.println("🎯 Calculando métricas del DESTINO: " +
        (tramo.getEstablecimientoDestino() != null ? tramo.getEstablecimientoDestino().getNombre() : "N/A"));

    LocalDateTime llegadaProgramadaDestino = tramo.getHoraSalidaProgramada(); // La hora de salida programada del origen
                                                                              // es la llegada esperada al destino
    LocalDateTime llegadaRealDestino = tramo.getHoraLlegadaRealDestino();
    LocalDateTime salidaRealDestino = tramo.getHoraSalidaRealDestino();

    // 1️⃣ TARDANZA EN DESTINO
    if (llegadaProgramadaDestino != null && llegadaRealDestino != null) {
      long tardanzaMinutos = Duration.between(llegadaProgramadaDestino, llegadaRealDestino).toMinutes();
      tramo.setTardanzaCita2((int) Math.max(0, tardanzaMinutos));

      System.out.println("   🕐 Tardanza Destino: " + tramo.getTardanzaCita2() + " min " +
          (tramo.getTardanzaCita2() == 0 ? "(A TIEMPO ✓)" : "(RETRASO ⚠)"));
    }

    // 2️⃣ PERMANENCIA EN DESTINO
    if (llegadaRealDestino != null && salidaRealDestino != null) {
      long permanenciaMinutos = Duration.between(llegadaRealDestino, salidaRealDestino).toMinutes();
      tramo.setTiempoPermanenciaCita2((int) permanenciaMinutos);

      System.out.println("   ⏱️ Permanencia Destino: " + tramo.getTiempoPermanenciaCita2() + " min");
    }

    // 3️⃣ TIEMPO DE ATENCIÓN EN DESTINO
    // Aquí puedes comparar con el SLA o un tiempo esperado de descarga
    if (llegadaRealDestino != null && salidaRealDestino != null && tramo.getSlaMinutos() != null) {
      long permanenciaReal = Duration.between(llegadaRealDestino, salidaRealDestino).toMinutes();
      long tiempoAtencion = Math.abs(permanenciaReal - tramo.getSlaMinutos());

      tramo.setTiempoAtencionCita2((int) tiempoAtencion);

      String evaluacion = permanenciaReal > tramo.getSlaMinutos() ? "(EXCEDIÓ SLA ⚠)" : "(DENTRO DE SLA ✓)";

      System.out.println("   🎯 Tiempo Atención Destino: " + tramo.getTiempoAtencionCita2() + " min " + evaluacion);
      System.out.println("      SLA: " + tramo.getSlaMinutos() + " min | Real: " + permanenciaReal + " min");
    }

    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    System.out.println("📊 RESUMEN COMPLETO DEL TRAMO #" + tramo.getOrden());
    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    System.out.println("📍 ORIGEN ("
        + (tramo.getEstablecimientoOrigen() != null ? tramo.getEstablecimientoOrigen().getNombre() : "N/A") + "):");
    System.out.println("   Tardanza: " + tramo.getTardanzaCita1() + " min");
    System.out.println("   Permanencia: " + tramo.getTiempoPermanenciaCita1() + " min");
    System.out.println("   Atención: " + tramo.getTiempoAtencionCita1() + " min");
    System.out.println("🎯 DESTINO ("
        + (tramo.getEstablecimientoDestino() != null ? tramo.getEstablecimientoDestino().getNombre() : "N/A") + "):");
    System.out.println("   Tardanza: " + tramo.getTardanzaCita2() + " min");
    System.out.println("   Permanencia: " + tramo.getTiempoPermanenciaCita2() + " min");
    System.out.println("   Atención: " + tramo.getTiempoAtencionCita2() + " min");
    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  }

  private String calcularEstadoViaje(List<Tramo> tramos) {
    if (tramos == null || tramos.isEmpty()) {
      return "pendiente";
    }

    long totalTramos = tramos.size();
    long completados = tramos.stream()
        .filter(t -> t.getEstado() != null && t.getEstado().name().equals("completado"))
        .count();
    long enCurso = tramos.stream()
        .filter(t -> t.getEstado() != null && t.getEstado().name().equals("en_curso"))
        .count();
    long retrasados = tramos.stream()
        .filter(t -> t.getEstado() != null && t.getEstado().name().equals("retrasado"))
        .count();

    if (completados == totalTramos) {
      return "completado";
    }
    if (enCurso > 0) {
      return "en_curso";
    }
    if (retrasados == totalTramos) {
      return "retrasado";
    }
    if (completados > 0) {
      return "en_curso";
    }
    return "pendiente";
  }
}
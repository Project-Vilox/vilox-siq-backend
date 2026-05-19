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
    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    System.out.println("📊 Calculando métricas para Tramo #" + tramo.getOrden() +
        " (ID: " + tramo.getId() + ")");
    System.out.println("   Estado actual: " + tramo.getEstado());

    // ⚠️ NO CALCULAR MÉTRICAS SI EL TRAMO ESTÁ PENDIENTE
    if (tramo.getEstado() == Tramo.EstadoTramo.pendiente) {
      System.out.println("⏸️ Tramo pendiente, no se calculan métricas aún");
      System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      return;
    }

    // 📍 CALCULAR MÉTRICAS DEL ORIGEN (Cita 1)
    // Se calcula cuando el tramo está EN_CURSO y tiene datos del origen
    if (tramo.getHoraLlegadaReal() != null) {
      calcularMetricasOrigen(tramo);
    }

    // 🎯 CALCULAR TARDANZA DEL DESTINO (Cita 2) - CUANDO LLEGA
    // ✅ NUEVO: Se calcula tan pronto llegue al destino, sin esperar a completar
    if (tramo.getHoraLlegadaRealDestino() != null) {
      calcularTardanzaDestino(tramo);
    }

    // 🎯 CALCULAR PERMANENCIA Y ATENCIÓN DEL DESTINO - CUANDO SALE
    // Se calcula cuando el tramo está COMPLETADO y tiene salida del destino
    if (tramo.getEstado() == Tramo.EstadoTramo.completado &&
        tramo.getHoraSalidaRealDestino() != null) {
      calcularPermanenciaYAtencionDestino(tramo);
    }

    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  }

  @PostUpdate
  public void onTramoUpdate(Tramo tramo) {
    System.out.println("🔄 TramoEntityListener: Tramo actualizado");
    System.out.println("   Tramo #" + tramo.getOrden() + " - Estado: " + tramo.getEstado());

    try {
      Viaje viaje = tramo.getViaje();
      if (viaje == null) {
        System.out.println("⚠️ Viaje no encontrado para tramo: " + tramo.getId());
        return;
      }

      // ⚠️ SOLO PUBLICAR EVENTO SI HAY CAMBIOS SIGNIFICATIVOS
      if (tramo.getEstado() == Tramo.EstadoTramo.completado ||
          tramo.getEstado() == Tramo.EstadoTramo.en_curso) {

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
      }

    } catch (Exception e) {
      System.err.println("❌ Error actualizando viaje desde listener: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * 📍 CALCULA MÉTRICAS DEL ESTABLECIMIENTO ORIGEN (CITA 1)
   * 
   * IMPORTANTE: En viajes multi-tramo, un mismo establecimiento puede ser:
   * - Destino del Tramo N (métricas Cita2)
   * - Origen del Tramo N+1 (métricas Cita1)
   * 
   * Cada visita se registra independientemente en su tramo correspondiente.
   * 
   * CÁLCULOS:
   * - Tardanza = horaLlegadaReal - horaLlegadaProgramada
   * - Permanencia = horaSalidaReal - horaLlegadaReal
   * - Atención = SE CALCULA EN TramoServiceImpl (desde ENTRY a EXIT de geocerca
   * interna)
   */
  private void calcularMetricasOrigen(Tramo tramo) {
    String nombreOrigen = tramo.getEstablecimientoOrigen() != null
        ? tramo.getEstablecimientoOrigen().getNombre()
        : "N/A";

    System.out.println("┌─────────────────────────────────────────┐");
    System.out.println("│ 📍 MÉTRICAS ORIGEN (Cita 1)             │");
    System.out.println("│ Establecimiento: " + String.format("%-24s", nombreOrigen) + "│");
    System.out.println("└─────────────────────────────────────────┘");

    LocalDateTime llegadaProgramada = tramo.getHoraLlegadaProgramada();
    LocalDateTime llegadaReal = tramo.getHoraLlegadaReal();
    LocalDateTime salidaReal = tramo.getHoraSalidaReal();

    // 1️⃣ TARDANZA EN ORIGEN
    if (llegadaProgramada != null && llegadaReal != null) {
      long tardanzaMinutos = Duration.between(llegadaProgramada, llegadaReal).toMinutes();
      tramo.setTardanzaCita1((int) Math.max(0, tardanzaMinutos));

      String estado = tramo.getTardanzaCita1() == 0 ? "✅ A TIEMPO" : "⚠️ CON RETRASO";
      System.out.println("   🕐 Tardanza: " + tramo.getTardanzaCita1() + " min " + estado);
      System.out.println("      Programada: " + llegadaProgramada);
      System.out.println("      Real: " + llegadaReal);
    }

    // 2️⃣ PERMANENCIA EN ORIGEN (Tiempo total en el establecimiento)
    if (llegadaReal != null && salidaReal != null) {
      long permanenciaMinutos = Duration.between(llegadaReal, salidaReal).toMinutes();
      tramo.setTiempoPermanenciaCita1((int) permanenciaMinutos);

      System.out.println("   ⏱️ Permanencia Total: " + tramo.getTiempoPermanenciaCita1() + " min");
      System.out.println("      Desde: " + llegadaReal + " (entrada externa)");
      System.out.println("      Hasta: " + salidaReal + " (salida externa final)");

      // ✅ VALIDACIÓN INMEDIATA: El tiempo de atención no puede exceder la permanencia
      if (tramo.getTiempoAtencionCita1() != null) {
        if (tramo.getTiempoAtencionCita1() > permanenciaMinutos) {
          System.out.println("   ⚠️ [CORRECCIÓN ORIGEN] Atención (" + tramo.getTiempoAtencionCita1() +
              " min) excedía permanencia (" + permanenciaMinutos + " min). Ajustando...");
          tramo.setTiempoAtencionCita1((int) permanenciaMinutos);
        } else {
          System.out.println("   🅿️ Atención: " + tramo.getTiempoAtencionCita1() + " min (OK)");
        }
      }
    }
  }

  /**
   * 🎯 CALCULA SOLO LA TARDANZA DEL DESTINO (Cita 2)
   * Se ejecuta TAN PRONTO como llega al destino (tramo EN_CURSO)
   * 
   * IMPORTANTE: En viajes multi-tramo, un mismo establecimiento puede ser:
   * - Destino del Tramo N (métricas Cita2) ← Se calcula aquí
   * - Origen del Tramo N+1 (métricas Cita1) ← Se calculará en el siguiente tramo
   * 
   * CÁLCULOS:
   * - Tardanza = horaLlegadaRealDestino - horaSalidaProgramada
   */
  private void calcularTardanzaDestino(Tramo tramo) {
    String nombreDestino = tramo.getEstablecimientoDestino() != null
        ? tramo.getEstablecimientoDestino().getNombre()
        : "N/A";

    System.out.println("┌─────────────────────────────────────────┐");
    System.out.println("│ 🎯 TARDANZA DESTINO (Cita 2)            │");
    System.out.println("│ Establecimiento: " + String.format("%-24s", nombreDestino) + "│");
    System.out.println("└─────────────────────────────────────────┘");

    // fechaFinProgramada del viaje = hora programada de llegada al destino (cita 2)
    LocalDateTime llegadaProgramadaDestino = (tramo.getViaje() != null)
        ? tramo.getViaje().getFechaFinProgramada()
        : null;
    LocalDateTime llegadaRealDestino = tramo.getHoraLlegadaRealDestino();

    // 1️⃣ TARDANZA EN DESTINO
    if (llegadaProgramadaDestino != null && llegadaRealDestino != null) {
      long tardanzaMinutos = Duration.between(llegadaProgramadaDestino, llegadaRealDestino).toMinutes();
      tramo.setTardanzaCita2((int) Math.max(0, tardanzaMinutos));

      String estado = tramo.getTardanzaCita2() == 0 ? "✅ A TIEMPO" : "⚠️ CON RETRASO";
      System.out.println("   🕐 Tardanza Destino: " + tramo.getTardanzaCita2() + " min " + estado);
      System.out.println("      Programada (fechaFinProgramada viaje): " + llegadaProgramadaDestino);
      System.out.println("      Real: " + llegadaRealDestino);
    } else {
      System.out.println("   ⚠️ Sin datos suficientes para calcular tardanza destino");
      System.out.println("      fechaFinProgramada viaje: " + llegadaProgramadaDestino);
      System.out.println("      horaLlegadaRealDestino: " + llegadaRealDestino);
    }
  }

  /**
   * 🎯 CALCULA PERMANENCIA Y ATENCIÓN DEL DESTINO (Cita 2)
   * Se ejecuta cuando el tramo está COMPLETADO (ya salió del destino)
   * 
   * CÁLCULOS:
   * - Permanencia = horaSalidaRealDestino - horaLlegadaRealDestino
   * - Atención = SE CALCULA EN TramoServiceImpl
   */
  private void calcularPermanenciaYAtencionDestino(Tramo tramo) {
    String nombreDestino = tramo.getEstablecimientoDestino() != null
        ? tramo.getEstablecimientoDestino().getNombre()
        : "N/A";

    System.out.println("┌─────────────────────────────────────────┐");
    System.out.println("│ 🎯 PERMANENCIA DESTINO (Cita 2)         │");
    System.out.println("│ Establecimiento: " + String.format("%-24s", nombreDestino) + "│");
    System.out.println("└─────────────────────────────────────────┘");

    LocalDateTime llegadaRealDestino = tramo.getHoraLlegadaRealDestino();
    LocalDateTime salidaRealDestino = tramo.getHoraSalidaRealDestino();

    // 2️⃣ PERMANENCIA EN DESTINO
    if (llegadaRealDestino != null && salidaRealDestino != null) {
      long permanenciaMinutos = Duration.between(llegadaRealDestino, salidaRealDestino).toMinutes();
      tramo.setTiempoPermanenciaCita2((int) permanenciaMinutos);

      System.out.println("   ⏱️ Permanencia Total: " + tramo.getTiempoPermanenciaCita2() + " min");
      System.out.println("      Desde: " + llegadaRealDestino + " (entrada externa)");
      System.out.println("      Hasta: " + salidaRealDestino + " (salida externa final)");

      // ✅ VALIDACIÓN INMEDIATA: El tiempo de atención no puede exceder la permanencia
      if (tramo.getTiempoAtencionCita2() != null) {
        if (tramo.getTiempoAtencionCita2() > permanenciaMinutos) {
          System.out.println("   ⚠️ [CORRECCIÓN DESTINO] Atención (" + tramo.getTiempoAtencionCita2() +
              " min) excedía permanencia (" + permanenciaMinutos + " min). Ajustando...");
          tramo.setTiempoAtencionCita2((int) permanenciaMinutos);
        } else {
          System.out.println("   🅿️ Atención: " + tramo.getTiempoAtencionCita2() + " min (OK)");
        }
      }
    }

    // 📋 RESUMEN COMPLETO DEL TRAMO
    imprimirResumenCompleto(tramo);
  }

  /**
   * 📋 IMPRIME RESUMEN COMPLETO DE TODAS LAS MÉTRICAS DEL TRAMO
   */
  private void imprimirResumenCompleto(Tramo tramo) {
    System.out.println("╔═════════════════════════════════════════════════════════════╗");
    System.out.println("║ 📊 RESUMEN COMPLETO - TRAMO #" + tramo.getOrden());
    System.out.println("╠═════════════════════════════════════════════════════════════╣");

    // ORIGEN
    String nombreOrigen = tramo.getEstablecimientoOrigen() != null
        ? tramo.getEstablecimientoOrigen().getNombre()
        : "N/A";
    System.out.println("║ 📍 ORIGEN: " + nombreOrigen);
    System.out.println("║    Tardanza........: " + formatearMetrica(tramo.getTardanzaCita1()));
    System.out.println("║    Permanencia.....: " + formatearMetrica(tramo.getTiempoPermanenciaCita1()));
    System.out.println("║    Atención........: " + formatearMetrica(tramo.getTiempoAtencionCita1()));
    System.out.println("╠─────────────────────────────────────────────────────────────╣");

    // DESTINO
    String nombreDestino = tramo.getEstablecimientoDestino() != null
        ? tramo.getEstablecimientoDestino().getNombre()
        : "N/A";
    System.out.println("║ 🎯 DESTINO: " + nombreDestino);
    System.out.println("║    Tardanza........: " + formatearMetrica(tramo.getTardanzaCita2()));
    System.out.println("║    Permanencia.....: " + formatearMetrica(tramo.getTiempoPermanenciaCita2()));
    System.out.println("║    Atención........: " + formatearMetrica(tramo.getTiempoAtencionCita2()));
    System.out.println("╠─────────────────────────────────────────────────────────────╣");

    // ESTADO
    System.out.println("║ 📊 ESTADO FINAL: " + tramo.getEstado());
    System.out.println("╚═════════════════════════════════════════════════════════════╝");
  }

  /**
   * Formatea una métrica para visualización
   */
  private String formatearMetrica(Integer minutos) {
    if (minutos == null) {
      return "N/A";
    }
    return minutos + " min";
  }

  /**
   * Calcula el estado del viaje basado en los estados de sus tramos
   */
  private String calcularEstadoViaje(List<Tramo> tramos) {
    if (tramos == null || tramos.isEmpty()) {
      return "pendiente";
    }

    long totalTramos = tramos.size();
    long completados = tramos.stream()
        .filter(t -> t.getEstado() == Tramo.EstadoTramo.completado)
        .count();
    long enCurso = tramos.stream()
        .filter(t -> t.getEstado() == Tramo.EstadoTramo.en_curso)
        .count();

    // 🏁 Todos completados = Viaje completado
    if (completados == totalTramos) {
      return "completado";
    }

    // 🚗 Al menos uno en curso = Viaje en curso
    if (enCurso > 0) {
      return "en_curso";
    }

    // 📊 Al menos uno completado pero ninguno en curso = Viaje en curso
    // (caso de transición entre tramos)
    if (completados > 0) {
      return "en_curso";
    }

    // ⏸️ Por defecto = Pendiente
    return "pendiente";
  }
}
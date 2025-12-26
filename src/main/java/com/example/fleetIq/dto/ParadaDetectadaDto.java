package com.example.fleetIq.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ParadaDetectadaDto {

  // Ubicación
  private Double latitud;
  private Double longitud;

  // Tiempo
  private Long timestampInicio;
  private Long timestampFin;
  private LocalDateTime horaInicio;
  private LocalDateTime horaFin;
  private Integer duracionMinutos;

  // Características de la parada
  private Boolean motorApagado;
  private Integer distanciaRecorridaMetros; // 🆕 Campo nuevo

  // Clasificación
  private String categoria;
  private String motivo;
  private String severidad;
  private Boolean justificada;

  // Lugares cercanos
  private List<LugarCercano> lugaresCercanos;

  // Observaciones
  private String observaciones;

  // ========================================================================
  // GETTERS Y SETTERS
  // ========================================================================

  public Double getLatitud() {
    return latitud;
  }

  public void setLatitud(Double latitud) {
    this.latitud = latitud;
  }

  public Double getLongitud() {
    return longitud;
  }

  public void setLongitud(Double longitud) {
    this.longitud = longitud;
  }

  public Long getTimestampInicio() {
    return timestampInicio;
  }

  public void setTimestampInicio(Long timestampInicio) {
    this.timestampInicio = timestampInicio;
  }

  public Long getTimestampFin() {
    return timestampFin;
  }

  public void setTimestampFin(Long timestampFin) {
    this.timestampFin = timestampFin;
  }

  public LocalDateTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalDateTime horaInicio) {
    this.horaInicio = horaInicio;
  }

  public LocalDateTime getHoraFin() {
    return horaFin;
  }

  public void setHoraFin(LocalDateTime horaFin) {
    this.horaFin = horaFin;
  }

  public Integer getDuracionMinutos() {
    return duracionMinutos;
  }

  public void setDuracionMinutos(Integer duracionMinutos) {
    this.duracionMinutos = duracionMinutos;
  }

  public Boolean isMotorApagado() {
    return motorApagado;
  }

  public void setMotorApagado(Boolean motorApagado) {
    this.motorApagado = motorApagado;
  }

  // 🆕 NUEVO CAMPO
  public Integer getDistanciaRecorridaMetros() {
    return distanciaRecorridaMetros;
  }

  public void setDistanciaRecorridaMetros(Integer distanciaRecorridaMetros) {
    this.distanciaRecorridaMetros = distanciaRecorridaMetros;
  }

  public String getCategoria() {
    return categoria;
  }

  public void setCategoria(String categoria) {
    this.categoria = categoria;
  }

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String motivo) {
    this.motivo = motivo;
  }

  public String getSeveridad() {
    return severidad;
  }

  public void setSeveridad(String severidad) {
    this.severidad = severidad;
  }

  public Boolean esJustificada() {
    return justificada != null ? justificada : false;
  }

  public void setJustificada(Boolean justificada) {
    this.justificada = justificada;
  }

  public List<LugarCercano> getLugaresCercanos() {
    return lugaresCercanos;
  }

  public void setLugaresCercanos(List<LugarCercano> lugaresCercanos) {
    this.lugaresCercanos = lugaresCercanos;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public void setObservaciones(String observaciones) {
    this.observaciones = observaciones;
  }

  // ========================================================================
  // MÉTODOS DE UTILIDAD
  // ========================================================================

  /**
   * Calcula la severidad de la parada basándose en:
   * - Duración
   * - Si está justificada
   * - Distancia recorrida durante la "parada"
   */
  public void calcularSeveridad() {
    if (duracionMinutos == null) {
      severidad = "DESCONOCIDA";
      return;
    }

    // Si el vehículo se movió significativamente, no es una parada real
    if (distanciaRecorridaMetros != null && distanciaRecorridaMetros > 200) {
      severidad = "BAJA";
      justificada = false;
      motivo = "Movimiento detectado (" + distanciaRecorridaMetros + "m)";
      return;
    }

    // Parada justificada (geocerca relevante o motor apagado)
    if (Boolean.TRUE.equals(justificada)) {
      if (duracionMinutos > 60) {
        severidad = "MEDIA"; // Larga pero justificada
      } else {
        severidad = "BAJA"; // Justificada y razonable
      }
      return;
    }

    // Parada no justificada
    if (duracionMinutos >= 30) {
      severidad = "ALTA"; // Parada larga sin justificación
    } else if (duracionMinutos >= 15) {
      severidad = "MEDIA"; // Parada moderada sin justificación
    } else {
      severidad = "BAJA"; // Parada corta
    }
  }

  /**
   * Obtiene el lugar más relevante de los lugares cercanos
   */
  public LugarCercano getLugarMasRelevante() {
    if (lugaresCercanos == null || lugaresCercanos.isEmpty()) {
      return null;
    }

    // Priorizar por tipo y distancia
    return lugaresCercanos.stream()
        .sorted((a, b) -> {
          // Primero por prioridad de tipo
          int prioridadA = getPrioridadTipo(a.tipo);
          int prioridadB = getPrioridadTipo(b.tipo);

          if (prioridadA != prioridadB) {
            return Integer.compare(prioridadA, prioridadB);
          }

          // Luego por distancia
          return Integer.compare(a.distancia, b.distancia);
        })
        .findFirst()
        .orElse(null);
  }

  private int getPrioridadTipo(String tipo) {
    switch (tipo) {
      case "ESTABLECIMIENTO":
        return 1;
      case "GEOCERCA":
        return 2;
      case "POI":
        return 3;
      default:
        return 4;
    }
  }

  // ========================================================================
  // CLASE INTERNA: LugarCercano
  // ========================================================================

  public static class LugarCercano {
    public String nombre;
    public String tipo;
    public int distancia; // metros
    public String descripcion;

    public LugarCercano(String nombre, String tipo, int distancia) {
      this.nombre = nombre;
      this.tipo = tipo;
      this.distancia = distancia;
    }

    public LugarCercano(String nombre, String tipo, int distancia, String descripcion) {
      this.nombre = nombre;
      this.tipo = tipo;
      this.distancia = distancia;
      this.descripcion = descripcion;
    }
  }
}
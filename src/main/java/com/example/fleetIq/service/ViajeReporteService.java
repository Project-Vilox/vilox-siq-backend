package com.example.fleetIq.service;

import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.model.ViajeReporte;
import com.example.fleetIq.repository.ViajeReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViajeReporteService {

  private final ViajeReporteRepository viajeReporteRepository;

  @Transactional
  public void generarYGuardarReporte(Viaje viaje, List<Tramo> tramos) {
    ViajeReporte reporte = new ViajeReporte();

    // 1. Datos Identificadores y Actores (en Mayúsculas)
    reporte.setViajeid(viaje.getId());
    if (viaje.getTipoOperacion() != null)
      reporte.setViajetipo(viaje.getTipoOperacion().toUpperCase());
    reporte.setFecha(viaje.getFechaInicioProgramada().toLocalDate());
    reporte.setNaviera(viaje.getEmpresaNaviera() != null ? viaje.getEmpresaNaviera().getNombre().toUpperCase() : "");
    reporte.setTransportista(
        viaje.getEmpresaTransportista() != null ? viaje.getEmpresaTransportista().getNombre().toUpperCase() : "");
    reporte.setOperador(viaje.getEmpresaOperador() != null ? viaje.getEmpresaOperador().getNombre().toUpperCase() : "");
    reporte.setCliente(viaje.getEmpresaCliente() != null ? viaje.getEmpresaCliente().getNombre().toUpperCase() : "");
    reporte.setTracto(viaje.getVehiculo() != null ? viaje.getVehiculo().getPlaca().toUpperCase() : "");
    reporte.setConductor(viaje.getConductor() != null ? viaje.getConductor().getNombre().toUpperCase() : "");

    // 2. Lógica de Tramos (Origen y Destinos)
    double kmAcumulado = 0;

    for (int i = 0; i < tramos.size(); i++) {
      Tramo t = tramos.get(i);

      // --- CASO ESPECIAL: DEPÓSITO (ORIGEN del primer tramo) ---
      if (i == 0 && t.getEstablecimientoOrigen() != null) {
        String tipoOrigen = t.getEstablecimientoOrigen().getTipo().toLowerCase();
        if (tipoOrigen.contains("almacen") || tipoOrigen.contains("deposito")) {
          reporte.setDeposito(t.getEstablecimientoOrigen().getNombre().toUpperCase());
          // Usamos Cita 1 porque es el ORIGEN
          reporte.setTardanzadeposito(t.getTardanzaCita1() != null ? t.getTardanzaCita1() : 0);
          reporte.setTiempodeposito(t.getTiempoAtencionCita1() != null ? t.getTiempoAtencionCita1() : 0);
        }
      }

      // --- CASOS DESTINO: PLANTA O PUERTO ---
      if (t.getEstablecimientoDestino() != null) {
        String tipoDestino = t.getEstablecimientoDestino().getTipo().toLowerCase();
        // Usamos Cita 2 porque es el DESTINO del tramo
        int tardanzaDestino = t.getTardanzaCita2() != null ? t.getTardanzaCita2() : 0;
        int tiempoAtencionDestino = t.getTiempoAtencionCita2() != null ? t.getTiempoAtencionCita2() : 0;

        if (tipoDestino.contains("planta")) {
          reporte.setPlanta(t.getEstablecimientoDestino().getNombre().toUpperCase());
          reporte.setTardanzaplanta(tardanzaDestino);
          reporte.setTiempoplanta(tiempoAtencionDestino);
        } else if (tipoDestino.contains("puerto")) {
          reporte.setPuerto(t.getEstablecimientoDestino().getNombre().toUpperCase());
          reporte.setTardanzapuerto(tardanzaDestino);
          reporte.setTiempopuerto(tiempoAtencionDestino);
        }
      }
    }

    // 3. Kilometraje y Ruta
    reporte.setKilometraje((int) kmAcumulado);
    String rutaTexto = tramos.stream()
        .map(t -> t.getEstablecimientoDestino().getNombre().toUpperCase())
        .collect(Collectors.joining(" -> ", tramos.get(0).getEstablecimientoOrigen().getNombre().toUpperCase() + " -> ",
            ""));

    if (rutaTexto.length() > 200)
      rutaTexto = rutaTexto.substring(0, 197) + "...";
    reporte.setRuta(rutaTexto);

    // 4. Guardar (con validación de existencia)
    if (!viajeReporteRepository.existsByViajeid(viaje.getId())) {
      viajeReporteRepository.save(reporte);
      System.out.println("💾 Reporte guardado con éxito (Depósito capturado como Origen).");
    }
  }
}
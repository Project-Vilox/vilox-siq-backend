package com.example.fleetIq.service; // Puede estar en com.example.fleetIq.service

import com.example.fleetIq.listener.ViajeUpdateEvent;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ViajeEventHandler {

    private final ViajeRepository viajeRepository;
    private final ViajeReporteService viajeReporteService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleViajeUpdateEvent(ViajeUpdateEvent event) {
        String viajeId = event.getViajeId();

        try {
            Optional<Viaje> viajeOpt = viajeRepository.findByIdWithTramos(viajeId);
            if (viajeOpt.isEmpty())
                return;

            Viaje viaje = viajeOpt.get();
            List<Tramo> tramos = viaje.getTramos();
            tramos.sort(Comparator.comparingInt(Tramo::getOrden));

            String nuevoEstado = calcularEstadoViaje(tramos);
            String estadoAnterior = viaje.getEstado();

            // 1. Cambiar estado del viaje
            if (!nuevoEstado.equals(estadoAnterior)) {
                viaje.setEstado(nuevoEstado);

                // Si el viaje arranca (primer origen), marcamos inicio real
                if ("en_curso".equals(nuevoEstado) && viaje.getFechaInicioReal() == null) {
                    viaje.setFechaInicioReal(tramos.get(0).getHoraLlegadaReal());
                }

                // Si el viaje termina (último destino), marcamos fin real
                if ("completado".equals(nuevoEstado)) {
                    viaje.setFechaFinReal(tramos.get(tramos.size() - 1).getHoraSalidaRealDestino());
                }

                viaje.setFechaActualizacion(LocalDateTime.now());
                viajeRepository.save(viaje); // Guardamos el cambio de estado primero
                System.out.println("🔄 Viaje " + viajeId + " cambió de " + estadoAnterior + " a " + nuevoEstado);
            }

            // 2. DISPARAR REPORTE (Solo si el viaje acaba de llegar a estado completado y no es demo)
            if ("completado".equals(nuevoEstado) && !Boolean.TRUE.equals(viaje.getIsDemo())) {
                System.out.println("📊 Generando reporte final para el viaje: " + viaje.getCodigoViaje());
                viajeReporteService.generarYGuardarReporte(viaje, tramos);
            } else if ("completado".equals(nuevoEstado)) {
                System.out.println("🎭 Viaje demo completado — reporte omitido: " + viaje.getCodigoViaje());
            }

        } catch (Exception e) {
            System.err.println("❌ Error procesando evento de viaje: " + e.getMessage());
        }
    }

    private String calcularEstadoViaje(List<Tramo> tramos) {
        long total = tramos.size();
        long completados = tramos.stream().filter(t -> t.getEstado() == Tramo.EstadoTramo.completado).count();
        long enCurso = tramos.stream().filter(t -> t.getEstado() == Tramo.EstadoTramo.en_curso).count();

        if (completados == total)
            return "completado";
        if (enCurso > 0 || completados > 0)
            return "en_curso";
        return "pendiente";
    }
}
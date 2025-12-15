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
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ViajeEventHandler {

    private final ViajeRepository viajeRepository;

    // ⭐ La clave: REQUIRES_NEW asegura que este método se ejecute en una
    // transacción separada.
    // Esto lo aísla del error de concurrencia del commit anterior.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // ⭐ CAMBIO CLAVE
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleViajeUpdateEvent(ViajeUpdateEvent event) {
        String viajeId = event.getViajeId();
        System.out.println("👂 Handler: Recibido evento para actualizar Viaje ID: " + viajeId);

        try {
            Optional<Viaje> viajeOpt = viajeRepository.findByIdWithTramos(viajeId); // <--- ¡CORREGIDO!

            if (viajeOpt.isEmpty()) {
                System.err.println("⚠️ Handler: Viaje no encontrado para ID: " + viajeId);
                return;
            }

            Viaje viaje = viajeOpt.get();

            // Re-obtener los tramos para recalcular el estado
            // Necesitamos asegurarnos que la lista de tramos esté inicializada y
            // actualizada
            List<Tramo> tramos = viaje.getTramos();
            if (tramos == null || tramos.isEmpty()) {
                // Esto no debería suceder si el tramo existe, pero es una buena práctica.
                return;
            }
            tramos.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));

            // Recalcular estado (Lógica copiada del Listener)
            String nuevoEstado = calcularEstadoViaje(tramos);
            String estadoActual = viaje.getEstado();

            boolean viajeActualizado = false;

            // 1. Actualizar estado
            if (!nuevoEstado.equals(estadoActual)) {
                viaje.setEstado(nuevoEstado);
                viajeActualizado = true;
                System.out.println("✅ Handler: Estado del viaje actualizado a: " + nuevoEstado);
            }

            // 2. Actualizar fecha inicio real
            Tramo primerTramo = tramos.get(0);
            if (viaje.getFechaInicioReal() == null && primerTramo.getHoraLlegadaReal() != null) {
                viaje.setFechaInicioReal(primerTramo.getHoraLlegadaReal());
                viajeActualizado = true;
                System.out.println("✅ Handler: Fecha inicio real actualizada a: " + primerTramo.getHoraLlegadaReal());
            }

            // 3. Actualizar fecha fin real (si está completado)
            if ("completado".equals(nuevoEstado)) {
                Tramo ultimoTramo = tramos.get(tramos.size() - 1);
                if (ultimoTramo.getHoraSalidaRealDestino() != null && viaje.getFechaFinReal() == null) {
                    viaje.setFechaFinReal(ultimoTramo.getHoraSalidaRealDestino());
                    viajeActualizado = true;
                }
            }

            if (viajeActualizado) {
                viaje.setFechaActualizacion(LocalDateTime.now());
                viajeRepository.save(viaje); // Guardar el Viaje en la NUEVA transacción
                System.out.println("💾 Handler: Viaje " + viajeId + " guardado en BD exitosamente.");
            }

        } catch (Exception e) {
            System.err.println("❌ Error en ViajeEventHandler al procesar evento para Viaje ID: " + viajeId + " - "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método auxiliar (Duplicado del Listener, pero necesario para el Handler)
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
        long cancelados = tramos.stream()
                .filter(t -> t.getEstado() != null && t.getEstado().name().equals("retrasado"))
                .count();

        if (completados == totalTramos) {
            return "completado";
        }
        if (enCurso > 0) {
            return "en_curso";
        }
        if (cancelados == totalTramos) {
            return "cancelado";
        }
        if (completados > 0) {
            return "en_curso";
        }
        return "pendiente";
    }
}
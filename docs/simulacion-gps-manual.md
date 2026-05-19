# Simulación GPS Manual — Documentación de Cambios

## Contexto

Se implementó una simulación GPS paso a paso para demostrar el flujo real de seguimiento de viajes sin necesidad de un dispositivo GPS físico. La simulación permite ver cómo se actualizan los tramos, las horas de llegada/salida y el estado del viaje en tiempo real.

---

## ¿Afecta al flujo real?

**No.** El flujo real (`AlarmService` → `TramoService` → `ViajeEventHandler`) no fue tocado en ninguna línea. La simulación opera en una capa completamente separada:

| Aspecto | Flujo real | Simulación |
|---|---|---|
| Inserción de tracks | GPS device → `AlarmService` | `TrackSimController` → `TrackSimService` |
| `alarmStatus` del track | `PENDING` (procesado por `AlarmService`) | `SIM_EVALUATED` (ignorado por `AlarmService`) |
| Actualización del tramo | `TramoService.procesarAlarma()` (async, cada 3s) | `TrackSimService.aplicarEfectoDirecto()` (síncrono, inmediato) |
| Publicación de evento | `TramoService` | `TrackSimService` (mismo `ViajeUpdateEvent`) |
| Generación de PDF | Sí (si `isDemo = false`) | No (viaje marcado `isDemo = true`) |

`AlarmService.checkAlarmsAutomatically()` filtra: `findByAlarmStatus("PENDING")` — jamás toca tracks con `SIM_EVALUATED`.

---

## Archivos creados (nuevos)

### `TrackSimService.java`
**Servicio principal de la simulación.** Responsabilidades:
- `init(viajeId)`: resetea el viaje y todos sus tramos a `pendiente` (timestamps a null), cierra alarmas activas del IMEI, calcula waypoints desde los centroides de geocercas, devuelve la sesión con rango de `gpstime` y coords de origen/destino.
- `next(sessionId)`: inserta un `Track` con `alarmStatus = SIM_EVALUATED` (para visualización en el mapa) y llama a `aplicarEfectoDirecto()` de forma sincrónica.
- `aplicarEfectoDirecto(viajeId, waypoint, gpstime)`: actualiza el tramo directamente en BD según el nombre del waypoint, sin pasar por `AlarmService`/`TramoService`. Publica `ViajeUpdateEvent` para que `ViajeEventHandler` recalcule el estado del viaje.
- `cleanup(sessionId)`: limpia los mapas de sesión en memoria.

**Mapas de sesión en memoria:**
```
sessions         → sessionId → List<Waypoint>
sessionIndex     → sessionId → índice actual
sessionImei      → sessionId → IMEI del vehículo
sessionBaseEpoch → sessionId → epoch base del primer paso
sessionViajeId   → sessionId → ID del viaje
```

**Lógica de `gpstime`:**
```
baseEpoch = now - totalPasos * 60s
gpstime[i] = baseEpoch + i * 60s
```
Todos los pasos quedan en el **pasado reciente**, garantizando que el pipeline de AlarmService (si procesara estos tracks) pasaría los controles de debounce. Evita timestamps futuros confusos en la UI.

**Waypoints generados por tramo:**

| Paso | Nombre | Campo actualizado |
|---|---|---|
| 1 | Entrada Ext. Origen | `horaLlegadaReal` + `horaEntradaGeocercaExternaOrigen` + estado `en_curso` |
| 2 | Entrada Int. Origen | `horaEntradaGeocercaInternaOrigen` |
| 3 | Fuera del Origen | `horaSalidaGeocercaExternaOrigen1` + `horaSalidaGeocercaInternaOrigen` |
| 4 | Re-entrada Ext. Origen | `horaEntradaGeocercaExternaOrigen2` |
| 5 | Salida Final Origen | `horaSalidaReal` + `horaSalidaGeocercaExternaOrigen2` + `tiempoAtencionCita1` |
| 6 | En Ruta → Destino | (solo mueve el vehículo en el mapa, sin campo de tramo) |
| 7 | Llegada Zona Destino | (posición previa, sin campo de tramo) |
| 8 | Entrada Ext. Destino | `horaLlegadaRealDestino` + `horaEntradaGeocercaExternaDestino` |
| 9 | Entrada Int. Destino | `horaEntradaGeocercaInternaDestino` |
| 10 | Fuera del Destino | `horaSalidaGeocercaExternaDestino1` + `horaSalidaGeocercaInternaDestino` |
| 11 | Re-entrada Ext. Destino | `horaEntradaGeocercaExternaDestino2` |
| 12 | Salida Final Destino | `horaSalidaRealDestino` + `horaSalidaGeocercaExternaDestino2` + `tiempoAtencionCita2` + estado `completado` |

**Respuesta de `init()`:**
```json
{
  "sessionId": "uuid",
  "imei": "...",
  "totalPasos": 12,
  "waypoints": [...],
  "gpstimeMin": 1234567890,
  "gpstimeMax": 1234568610,
  "origenLat": -8.12,
  "origenLon": -79.03,
  "destinoLat": -8.55,
  "destinoLon": -79.12
}
```
`gpstimeMin/Max` → el frontend filtra solo los tracks de esta sesión en el mapa.
`origenLat/Lon` y `destinoLat/Lon` → centroides de geocercas, usados como fallback si el establecimiento no tiene `latitud/longitud` en BD.

---

### `TrackSimController.java`
Expone los endpoints de la simulación:
```
POST   /api/track/sim/init/{viajeId}   → inicia sesión y resetea viaje
POST   /api/track/sim/next/{sessionId} → inserta el siguiente paso
DELETE /api/track/sim/{sessionId}      → limpia la sesión
```

---

## Archivos modificados

### `TrackSimController.java`
**Fix CORS:**
```java
// Antes — incompatible con allowCredentials=true en config global
@CrossOrigin(origins = "*")

// Después
@CrossOrigin(originPatterns = "*")
```
`origins = "*"` lanza `IllegalArgumentException` cuando la config global tiene `allowCredentials = true`. `originPatterns = "*"` es equivalente pero compatible.

---

### `SeguimientoViajes.js` (frontend)

**Estado nuevo agregado:**
```javascript
const [simOrigenCoords, setSimOrigenCoords] = useState(null);   // centroide geocerca origen
const [simDestinoCoords, setSimDestinoCoords] = useState(null); // centroide geocerca destino
const [simGpstimeMin, setSimGpstimeMin] = useState(null);       // rango de tracks de esta sesión
const [simGpstimeMax, setSimGpstimeMax] = useState(null);
```

**`initSim()`:** guarda coords y rango de gpstime desde la respuesta del backend.

**`nextSim()`:** eliminado el countdown de 7s. El backend actualiza el tramo de forma sincrónica, entonces el fetch se hace **inmediatamente** después de recibir la respuesta:
```javascript
// Antes
setSimRefreshCountdown(7); // esperar 7s para que el pipeline async procese

// Después
await fetchViajeDetalle(selectedViaje.id, true, true); // inmediato
```

**`stopSim()`:** limpia también `simOrigenCoords`, `simDestinoCoords`, `simGpstimeMin`, `simGpstimeMax`.

**`updateMapWithTracking()` — coordenadas de establecimientos:**
Si `establecimientoOrigen.latitud === 0` (no cargadas en BD), usa los centroides de geocercas del `init()` como fallback para los marcadores I y F del mapa.

**`updateMapWithTracking()` — filtro de tracks:**
Durante simulación, solo muestra tracks dentro del rango `[gpstimeMin, gpstimeMax]` de la sesión actual. Evita que runs anteriores mezclen líneas en el mapa.

**`getValidTrackingData()`:**
Si `simSession` está activo y `horaLlegadaReal` es null (aún no procesó el paso 1), devuelve todos los tracks válidos en lugar de array vacío. Permite visualizar el primer track en el mapa inmediatamente.

---

### `Viajes.js` (frontend)

**Fix "Inicio/Fin Programado" invertidos en detalles:**
El componente de detalles usaba campos de tramo (`horaSalidaProgramada` / `horaLlegadaProgramada`) que para algunos viajes estaban semánticamente invertidos.

```javascript
// Antes — usaba campos del tramo (incorrectos para DEMO-02)
selectedViaje.tramos[0].horaSalidaProgramada        // Inicio
selectedViaje.tramos[last].horaLlegadaProgramada    // Fin

// Después — usa los mismos campos del viaje que usan todos los otros views
selectedViaje.fechaInicioProgramada   // Inicio
selectedViaje.fechaFinProgramada      // Fin
```

---

## Flujo completo de la simulación

```
[Frontend] clic "Iniciar Simulación"
    → POST /api/track/sim/init/{viajeId}
    → Viaje → pendiente, tramos → pendiente, timestamps → null
    → Alarmas activas del IMEI → cerradas
    → Waypoints calculados desde centroides de geocercas
    → Sesión creada en memoria

[Frontend] clic "Insertar Paso N"
    → POST /api/track/sim/next/{sessionId}
    → Track insertado (alarmStatus=SIM_EVALUATED → AlarmService lo ignora)
    → aplicarEfectoDirecto() → campo del tramo actualizado en BD (síncrono)
    → ViajeUpdateEvent publicado → ViajeEventHandler recalcula estado del viaje
    → Response llega al frontend
    → fetchViajeDetalle() inmediato → panel de tramos se actualiza
    → loadVehicleTracking() → vehículo se mueve en el mapa
```

---

## Pendientes / Mejoras futuras

- ETA en tiempo real durante la simulación (requiere integración con Google Maps Distance Matrix o cálculo propio basado en distancia geocercas).
- Soporte multi-tramo en `aplicarEfectoDirecto()` (actualmente asume tramo único o primer tramo activo).
- Reset automático del viaje al iniciar simulación sin necesidad de reset manual desde BD.
- Posibilidad de pausar/retroceder pasos.

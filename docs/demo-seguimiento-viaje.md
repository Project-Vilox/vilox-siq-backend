# DEMO SEGUIMIENTO VIAJE — DOCUMENTACIÓN COMPLETA
**Fecha:** 2026-05-04

---

## QUÉ ES

Modo demo para **SeguimientoViajes** que permite mostrar a un cliente el funcionamiento
completo del sistema sin necesidad de GPS real ni backend productivo activo.

El cliente:
1. Crea un viaje con tramos y horas programadas (form normal)
2. Va a Viajes → Seguimiento → selecciona el viaje → click "🎭 Demo"
3. Ve el vehículo moviéndose en el mapa por rutas reales (OSRM)
4. Ve el panel de tramos actualizarse (en_curso → completado) a medida que avanza
5. Al llegar al destino el viaje queda "completado" automáticamente

---

## FLUJO COMPLETO

```
Usuario selecciona viaje en SeguimientoViajes
  ↓ click "🎭 Demo"
  ↓
POST /api/demo/start { viajeId, intervalo: 3 }
  → DemoService:
      1. Carga tramos del viaje con coordenadas de establecimientos
      2. Llama OSRM para cada tramo (A→B, B→C...) → ruta real por calles
      3. Samplea ~200 puntos de la ruta total
      4. Guarda snapshot de tiempos originales (para restaurar al detener)
      5. Setea tramo 1: en_curso, horaSalidaReal = now, horaLlegadaReal = now-30min
      6. Restantes tramos: pendiente
      7. Viaje: en_curso, fechaInicioReal = now
      8. Arranca scheduler: inserta 1 track demo cada 3s
  → Devuelve { sessionId, vehiculoId, origenLat, origenLon, destinoLat, destinoLon }

Frontend:
  → Mapa cambia de Google Maps → Leaflet (OpenStreetMap, sin API key)
  → Polling cada 5s: refresca tracking GPS
  → Polling cada 30s (6 ciclos): refresca detalle viaje y tramos
  → Al cruzar boundary de tramo: backend marca completado y arranca el siguiente
  → Al terminar todos los puntos: viaje completado automáticamente

click "⏹ Detener Demo" (antes de terminar):
  → DELETE tracks demo de la BD
  → Restaura tiempos originales de tramos
  → Restaura estado original del viaje
  → Mapa vuelve a Google Maps
```

---

## ARCHIVOS MODIFICADOS

### BACKEND — `vilox-siq-backend`

| Archivo | Cambio |
|---------|--------|
| `src/main/java/com/example/fleetIq/service/DemoService.java` | REESCRITO COMPLETAMENTE — ver detalle abajo |
| `src/main/java/com/example/fleetIq/controller/DemoController.java` | Sin cambios (endpoints ya existían) |

#### DemoService.java — cambios respecto a versión anterior

| Feature | Descripción |
|---------|-------------|
| `generateSyntheticTracks()` | NUEVO — llama OSRM para cada tramo, concatena rutas |
| `fetchOsrmRoute()` | NUEVO — HTTP GET a `router.project-osrm.org` con coords de establecimientos |
| `samplePoints()` | NUEVO — reduce N puntos OSRM a ~200 puntos uniformes |
| `checkTramoBoundary()` | NUEVO — detecta cuando el scheduler cruza el límite de un tramo |
| `completeTramo()` | NUEVO — marca tramo completado con horaLlegadaRealDestino = now |
| `startTramo()` | NUEVO — marca tramo en_curso con horaLlegadaReal y horaSalidaReal = now |
| `completeDemoNaturally()` | NUEVO — al agotar todos los puntos, completa viaje y último tramo |
| `sessionTramoBoundaries` | NUEVO — map de sessionId → índices donde termina cada tramo |
| `sessionTramos` | NUEVO — map de sessionId → lista de tramos |
| `sessionOriginalTramoTimes` | NUEVO — snapshot de tiempos antes de demo para restaurar |
| `sessionOriginalViajeState` | NUEVO — snapshot de estado del viaje para restaurar |
| `sessionCompletedNaturally` | NUEVO — distingue fin natural de stop manual |
| `stop()` | MODIFICADO — restaura tiempos originales si fue detenido manualmente |
| `mejorDiaDeRecorrido()` | CONSERVADO — fallback si OSRM no responde |
| `loadHistoricalTracks()` | REFACTORIZADO — extraído de start(), usado solo como fallback |
| Tracks sintéticos | Horas programadas del tramo YA NO SE PISAN — solo se setean horas reales |

### FRONTEND — `vilox-siq-frontend`

| Archivo | Cambio |
|---------|--------|
| `src/components/SeguimientoViajes.js` | MODIFICADO — ver detalle abajo |
| `src/components/DemoMapLeaflet.js` | Sin cambios (ya existía, acepta `{ trackingData, origen, destino }`) |
| `src/components/Viajes.js` | MODIFICADO — fixes de campos invertidos en Cronología y Tramos |

#### SeguimientoViajes.js — cambios agregados

| Elemento | Descripción |
|----------|-------------|
| `import DemoMapLeaflet` | Agregado al inicio del archivo |
| `demoActivo` state | Boolean — controla qué mapa se muestra |
| `demoSession` state | UUID de la sesión demo activa |
| `demoOrigen` state | `{ lat, lng }` para marcador verde en Leaflet |
| `demoDestino` state | `{ lat, lng }` para marcador rojo en Leaflet |
| `demoPollingRef` ref | Referencia al setInterval para poder cancelarlo |
| `startDemo()` | Llama `POST /api/demo/start`, activa Leaflet, arranca polling |
| `stopDemo()` | Llama `POST /api/demo/stop`, restaura Google Maps, limpia estado |
| Botón `🎭 Demo` | Aparece en header cuando hay viaje seleccionado |
| Botón `⏹ Detener Demo` | Reemplaza al botón Demo cuando `demoActivo=true` |
| Swap de mapa | Cuando `demoActivo`: `<DemoMapLeaflet>` en vez del `<div ref={mapContainerRef}>` |
| Overlay "Selecciona un Viaje" | Ahora también chequea `!demoActivo` para no aparecer durante demo |
| Polling separado | GPS cada 5s / detalle viaje cada 30s (contador de ciclos `pollCycle`) |

#### Viajes.js — fixes de campos invertidos

| Ubicación | Bug | Fix |
|-----------|-----|-----|
| Cronología — Inicio Programado (línea ~4406) | Usaba `horaLlegadaProgramada` | Cambiado a `horaSalidaProgramada` |
| Cronología — Fin Programado (línea ~4427) | Usaba `horaSalidaProgramada` | Cambiado a `horaLlegadaProgramada` |
| Detalle tramo — Programado (línea ~4364) | Mostraba llegada → salida (invertido) | Cambiado a salida → llegada |

---

## BASE DE DATOS — CAMBIOS MANUALES EN DEMO-01

| Tabla | Campo | Valor anterior | Valor nuevo | Motivo |
|-------|-------|---------------|-------------|--------|
| `viajes` | `fecha_fin_programada` | `2026-05-05 18:34:00` | `2026-05-04 17:30:00` | Corregir para que fin sea mismo día |
| `tramos` (orden 1) | `establecimiento_destino_id` | HORTIFRUT SALAVERRY (sin coords) | HORTIFRUT PERU SAC (`-6.079, -79.943`) | OSRM necesita coords en destino |
| `tramos` (orden 1) | `hora_salida_programada` | `2026-05-05 18:34:00` | `2026-05-04 12:32:00` | Estaba al día siguiente, invertido |
| `tramos` (orden 1) | `hora_llegada_programada` | `2026-05-04 12:32:00` | `2026-05-04 17:30:00` | Era igual al inicio, corregido a destino |

### Viaje DEMO-01 — estado actual

```
Código:    DEMO-01
Vehículo:  C4B-856
Estado:    pendiente (se resetea a este estado al detener demo)

Tramo 1:
  Origen:             DP WORLD 2 (Paita, Piura) lat -5.086, lon -81.087
  Destino:            HORTIFRUT PERU SAC (Chiclayo, Lambayeque) lat -6.079, lon -79.943
  Salida programada:  2026-05-04 12:32
  Llegada programada: 2026-05-04 17:30
  Ruta OSRM:          ~200 km Panamericana Norte, ~10 min de demo
```

---

## TECNOLOGÍA DE RUTAS: OSRM

- **Servicio:** `http://router.project-osrm.org/route/v1/driving/`
- **Gratuito:** sin API key, sin billing
- **Cobertura:** Perú completo, rutas por calles/autopistas reales
- **Formato:** `lon1,lat1;lon2,lat2?overview=full&geometries=geojson`
- **Fallback:** si OSRM no responde en 10s → usa tracks históricos del vehículo (`mejorDiaDeRecorrido`)
- **Sampleo:** de N puntos OSRM se toman ~200 uniformes (TARGET_TOTAL_POINTS = 200)
- **Duración demo:** 200 puntos × 3s/punto = 600s = ~10 minutos

---

## MAPA EN DEMO MODE

| Modo | Librería | API Key | Uso |
|------|----------|---------|-----|
| Normal | Google Maps | Requerida | Cuando `demoActivo = false` |
| Demo | Leaflet + OpenStreetMap | No requerida | Cuando `demoActivo = true` |

Marcadores Leaflet:
- 🟢 Verde → origen (establecimiento del primer tramo)
- 🔴 Rojo → destino (establecimiento del último tramo)
- 🟣 Púrpura → posición actual del vehículo (último track recibido)

---

## PROBLEMAS ENCONTRADOS Y FIXES

### 1. Horas programadas del tramo mostradas invertidas

**Síntoma:** "Programado: 5:30 PM - 12:32 PM" — llegada antes que salida.

**Causa:** En `Viajes.js`, la sección Cronología y el detalle de tramo usaban `horaLlegadaProgramada` como inicio y `horaSalidaProgramada` como fin — al revés de la semántica correcta.

**Fix:** Swap de los dos campos en ambos lugares de `Viajes.js`.

---

### 2. DemoService pisaba las horas programadas del tramo

**Síntoma:** El seguimiento mostraba "Inicio: 12:01" cuando el viaje tenía programado 12:32.

**Causa:** `DemoService.start()` seteaba `horaSalidaProgramada = now - 2min` para hacer el escenario "coherente", pisando lo que el usuario había configurado.

**Fix:** Eliminado el seteo de horas programadas. Solo se setean las horas **reales** (`horaSalidaReal`, `horaLlegadaReal`). Las horas programadas quedan intactas.

---

### 3. Panel de información se refrescaba cada 5s

**Síntoma:** El panel de tramos/detalle del viaje parpadeaba o re-renderizaba cada 5 segundos.

**Causa:** El polling llamaba `fetchViajeDetalle` (que trae el viaje completo con tramos) cada 5s junto con el tracking GPS.

**Fix:** Polling separado — GPS cada 5s, `fetchViajeDetalle` cada 30s (cada 6 ciclos con contador `pollCycle`).

---

### 4. Destino HORTIFRUT SALAVERRY sin coordenadas

**Síntoma:** OSRM no podía generar ruta porque el establecimiento destino no tenía lat/lon.

**Causa:** HORTIFRUT SALAVERRY tenía lat/lon null en la BD.

**Fix:** Se cambió el destino del tramo a HORTIFRUT PERU SAC que sí tenía coordenadas (`-6.079, -79.943`).

---

### 5. fecha_fin_programada del viaje apuntaba al día siguiente

**Síntoma:** En el listado de SeguimientoViajes se veía "Fin: 05/05/2026 18:34" cuando debería ser el mismo día.

**Causa:** El viaje DEMO-01 se creó con `fecha_fin_programada = 2026-05-05 18:34` (mañana).

**Fix:** UPDATE en BD directo: `fecha_fin_programada = '2026-05-04 17:30:00'`.

---

## LIMITACIONES CONOCIDAS

- Las geocercas (entrada/salida de geofence) NO se registran en demo — el `TramoEntityListener` ignora tracks con `isDemo=true`.
- Solo `horaLlegadaRealDestino` se registra al completar (cuando el scheduler agota los puntos).
- Si OSRM está caído, el fallback usa tracks históricos del vehículo que pueden no coincidir con la ruta real del viaje.
- El sampleo de 200 puntos hace que rutas muy cortas (<5 km) o muy largas (>500 km) puedan verse con saltos o muy densos — ajustar `TARGET_TOTAL_POINTS` en `DemoService.java` si es necesario.

---

## PARA CAMBIAR EL VIAJE DE DEMO

El sistema funciona con **cualquier viaje** que tenga:
1. Vehículo asignado
2. Al menos 1 tramo con coordenadas en origen Y destino

No hay un ID hardcodeado en el frontend (a diferencia de `SeguimientoFlota` que tenía `DEMO_VIAJE_ID`). El usuario selecciona el viaje en la lista y hace click en "🎭 Demo".

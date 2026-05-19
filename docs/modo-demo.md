# Modo Demo — Documentación Completa

## Qué es

El Modo Demo simula en tiempo real el recorrido de un vehículo usando tracks GPS históricos.
En vez de esperar datos en vivo del GPS, el backend inserta un track cada N segundos (por defecto 3s)
tomando coordenadas de un viaje ya completado.

Útil para: demos a clientes, pruebas de la UI, presentaciones sin vehículo real activo.

---

## Arquitectura

```
Frontend (SeguimientoFlota.js)
  └── POST /api/demo/start { viajeId, intervalo }
        └── DemoService.start()
              ├── Carga tracks históricos del viaje (por vehiculo + rango de fechas)
              ├── Crea ScheduledExecutorService (pool de 5 threads)
              ├── Cada N segundos inserta un Track con:
              │     - imei del vehículo del viaje
              │     - gpstime = now()
              │     - lat/lon del track histórico
              │     - isDemo = true
              │     - alarmStatus = "EVALUATED" (no genera alertas)
              └── Devuelve { sessionId, vehiculoId }

Frontend recibe vehiculoId → auto-selecciona el vehículo → activa tiempo real
Polling /api/tracks-by-vehicle?vehiculoId=... cada 30s → recibe los tracks demo
DemoMapLeaflet renderiza la ruta con marcador moviéndose
```

---

## Archivos modificados/creados

### Backend (vilox-siq-backend)

| Archivo | Cambio |
|---------|--------|
| `service/DemoService.java` | NUEVO — lógica de sesión demo, scheduler, inserción/borrado de tracks |
| `controller/DemoController.java` | NUEVO — endpoints start/stop/status |
| `model/Track.java` | Agregado campo `is_demo boolean` |
| `service/AlarmServiceImpl.java` | Filtra tracks con `isDemo=true` para no generar alertas falsas |

### Frontend (vilox-siq-frontend)

| Archivo | Cambio |
|---------|--------|
| `components/DemoMapLeaflet.js` | NUEVO — mapa Leaflet (sin API key) para modo demo |
| `components/SeguimientoFlota.js` | Botón demo, modal, estados, auto-selección de vehículo, render condicional Leaflet/Google Maps |

---

## Endpoints

```
POST /api/demo/start
  Body: { "viajeId": "uuid", "intervalo": 3 }
  Response: { "sessionId": "uuid", "vehiculoId": "uuid" }

GET /api/demo/status/{sessionId}
  Response: { "active": true, "puntoActual": 45, "totalPuntos": 806, "progreso": 5 }

POST /api/demo/stop/{sessionId}
  Response: { "stopped": true }
  Efecto: cancela el scheduler y BORRA todos los tracks demo insertados
```

---

## Mapa: Leaflet vs Google Maps

- **Demo activo** (`demoActivo=true`) → `DemoMapLeaflet` (react-leaflet + OpenStreetMap, sin API key)
- **Normal** (`demoActivo=false`) → Google Maps (requiere `REACT_APP_GOOGLE_MAPS_KEY` válida)

El render condicional está en `SeguimientoFlota.js` línea ~1012:
```jsx
{demoActivo ? (
  <DemoMapLeaflet trackingData={trackingData} />
) : (
  <div ref={mapContainerRef} ... className="google-map-container" />
)}
```

---

## Estado en el frontend

```js
const [demoSession, setDemoSession]     // sessionId activo
const [demoActivo, setDemoActivo]       // boolean — controla qué mapa se muestra
const [showDemoModal, setShowDemoModal] // modal para ingresar viajeId
const [demoViajeId, setDemoViajeId]    // input del UUID del viaje
```

---

## Viaje de prueba (local WSL2)

- **Viaje ID:** `31a7b9e7-f998-4751-88a7-8d5e45c055f2`
- **Código:** BK-031-2025
- **Vehículo:** V4M-797
- **Tracks:** 806 puntos GPS del 05/10/2025
- **Intervalo por defecto:** 3 segundos → ~40 minutos de demo

---

## Bugs encontrados y resueltos

### 1. No se veía nada en el mapa
**Causa:** `loadGoogleMapsAPI()` fallaba (key inválida), y como `fetchVehiculos()` estaba dentro del mismo `try`, nunca se cargaban los vehículos. Al iniciar demo, `vehiculos.find(vehiculoId)` devolvía `undefined` y no se seleccionaba ningún vehículo. Sin `selectedVehiculo`, el polling nunca corría y `trackingData` quedaba vacío.

**Fix:** Mover `fetchVehiculos()` fuera del try/catch de Google Maps para que siempre se ejecute.

```js
// Antes (malo):
try {
  await loadGoogleMapsAPI();
  initializeMap();
  await fetchVehiculos(); // ← nunca llegaba si Maps fallaba
} catch (error) { ... }

// Después (correcto):
try {
  await loadGoogleMapsAPI();
  initializeMap();
} catch (error) {
  console.error("Error initializing Google Maps:", error);
}
await fetchVehiculos(); // ← siempre se ejecuta
```

### 2. Demo no devolvía el vehiculoId
**Causa:** `DemoService.start()` solo retornaba el `sessionId`.

**Fix:** Agregar `Map<String, String> sessionVehiculoId`, guardar el vehiculoId al iniciar, exponerlo con `getVehiculoId(sessionId)`, y el controller lo incluye en la respuesta.

---

## Pendiente

- Agregar `REACT_APP_GOOGLE_MAPS_KEY` válida en `.env` para modo normal
- Evaluar si mostrar barra de progreso de la demo en la UI (ya existe en `/api/demo/status`)
- Considerar WebSocket para actualización en tiempo real en vez de polling cada 30s

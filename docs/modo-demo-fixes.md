# Modo Demo — Fixes y Mejoras

## Fix 1: Pantalla blanca al iniciar demo

**Archivo:** `vilox-siq-frontend/src/components/SeguimientoFlota.js`

**Causa:** El overlay "Selecciona un Vehículo" (`bg-white/90 z-10`) se mostraba siempre que `selectedVehiculo` era null — incluso cuando el demo estaba activo, tapando el mapa Leaflet por completo.

**Fix:**
```jsx
// Antes
{!selectedVehiculo && (

// Después
{!selectedVehiculo && !demoActivo && (
```

---

## Fix 2: `vehiculos.find` fallaba silenciosamente

**Archivo:** `vilox-siq-frontend/src/components/SeguimientoFlota.js`

**Causa:** La comparación `veh.id === data.vehiculoId` fallaba por diferencia de tipos (ej: número vs string UUID). Si `find` devolvía `undefined`, el vehículo nunca se auto-seleccionaba, el polling nunca arrancaba y `trackingData` quedaba vacío.

**Fix:** Comparación robusta con `String()` + logs de diagnóstico:
```js
// Antes
const v = vehiculos.find((veh) => veh.id === data.vehiculoId);

// Después
console.log("🎭 vehiculoId del backend:", data.vehiculoId, typeof data.vehiculoId);
console.log("🎭 vehiculos disponibles:", vehiculos.map(v => ({ id: v.id, tipo: typeof v.id })));
const v = vehiculos.find((veh) => String(veh.id) === String(data.vehiculoId));
if (!v) console.warn("🎭 No se encontró el vehículo con id:", data.vehiculoId);
```

---

## Mejora: Marcador de origen en DemoMapLeaflet

**Archivo:** `vilox-siq-frontend/src/components/DemoMapLeaflet.js`

**Cambio:** Se agregó un marcador verde en el primer punto del recorrido para indicar el origen del viaje.

**Leyenda del mapa:**
| Marcador | Color | Significado |
|----------|-------|-------------|
| Punto verde | `#22C55E` | Origen del viaje (primer track recibido, fijo) |
| Punto púrpura | `#8B5CF6` | Posición actual del vehículo (último track, avanza cada 30s) |
| Línea púrpura | `#8B5CF6` | Ruta recorrida hasta el momento |

**Por qué no hay marcador de destino:** En modo demo el frontend recibe los tracks de a uno (polling cada 30s). No conoce de antemano el punto final del viaje histórico, por lo tanto no puede mostrar un destino fijo.

---

## Cómo funciona el movimiento

El movimiento no es animación fluida — es incremental:

1. El backend inserta **1 track cada 3 segundos** en la BD (`isDemo=true`)
2. El frontend hace **polling cada 30 segundos** a `/api/tracks-by-vehicle`
3. Cada poll devuelve **todos los tracks acumulados** del vehículo hasta ese momento
4. El `Polyline` se redibuja con más puntos → el marcador púrpura salta al último → eso es el "movimiento"

Con 806 puntos totales y polling cada 30s, la demo completa dura ~40 minutos.

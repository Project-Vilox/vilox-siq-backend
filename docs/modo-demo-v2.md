# Modo Demo v2 — Fixes e Implementaciones
**Fecha:** 2026-05-03

---

## Resumen

Sesión de correcciones y mejoras sobre el modo demo existente.
El objetivo es que el cliente vea una simulación realista de un viaje en curso,
sin necesidad de interacción técnica.

---

## 1. Error: "Viaje no encontrado"

**Causa:** El ID de viaje documentado en `modo-demo.md` (`31a7b9e7-...`) no existe
en la BD local. Era un ID de producción o de una sesión anterior con datos distintos.

**Solución:** Se identificaron los viajes válidos para demo en la BD local:

| ID | Código | Vehículo | Tramos | Tracks en rango |
|----|--------|----------|--------|-----------------|
| `43ea68a6-71b0-4037-8a3e-5eb60165f04b` | BK-001-2025 | V4M-797 | 5 | 8.322 |
| `703034d0-4f05-425f-a1ed-93585829fe51` | BK-060-2025 | V4M-797 | 0 | 13.387 |

**ID de demo activo:** `43ea68a6-71b0-4037-8a3e-5eb60165f04b` (BK-001-2025)

---

## 2. Error de compilación: variable no effectively final

**Archivo:** `DemoService.java`

**Causa:** La variable `tracks` se reasignaba dentro del bloque `if` de fallback
y luego se usaba dentro de una lambda (`scheduleAtFixedRate`). Java no permite esto.

**Solución:** Separar en `pool` (mutable) y `final List<Track> tracks` (inmutable):
```java
List<Track> pool = ...; // mutable, se puede reasignar
final List<Track> tracks = mejorDiaDeRecorrido(pool, referenciaOrigen); // final para lambda
```

---

## 3. Error: demo no arrancaba si el viaje no tenía tracks en su rango de fechas

**Causa:** `DemoService.start()` lanzaba excepción inmediata si
`findByVehiculoIdAndHearttimeBetween()` devolvía lista vacía.

**Solución:** Fallback a todos los tracks reales del IMEI (filtrando `isDemo=false`),
luego se aplica `mejorDiaDeRecorrido()` para elegir el mejor segmento:
```java
if (pool.isEmpty()) throw new IllegalArgumentException(...);
// si hay tracks pero fuera del rango → usa todos los del IMEI
```

---

## 4. Mejora: polling cada 5s en demo mode (antes 30s)

**Archivo:** `SeguimientoFlota.js`

**Cambio:**
```js
const intervalo = demoActivo ? 5000 : 30000;
```
`demoActivo` se agregó al array de dependencias del `useEffect` para que
el intervalo se recalcule al activar/desactivar el demo.

---

## 5. Mejora: botón único sin modal (UX para demo a cliente)

**Archivo:** `SeguimientoFlota.js`

**Antes:** Botón "🎭 Modo Demo" abría un modal donde el usuario debía ingresar
un UUID de viaje manualmente.

**Después:** Un solo click inicia la demo con el viaje preconfigurado:
```js
const DEMO_VIAJE_ID = "43ea68a6-71b0-4037-8a3e-5eb60165f04b"; // BK-001-2025 | V4M-797 | 5 tramos
```
El cliente no ve ningún ID ni campo técnico.

---

## 6. Mejora: marcadores de origen (verde) y destino (rojo)

### Backend — `DemoService.java` + `DemoController.java`

Se agregaron mapas en memoria por sesión:
```java
private final Map<String, double[]> sessionOrigen = new ConcurrentHashMap<>();
private final Map<String, double[]> sessionDestino = new ConcurrentHashMap<>();
```

Lógica de resolución (en orden):
1. **Tramos con coordenadas:** usa `establecimientoOrigen` del primer tramo
   y `establecimientoDestino` del último tramo
2. **Fallback GPS:** origen = primer track del mejor día,
   destino = track más lejano del origen (Haversine)

La respuesta de `POST /api/demo/start` ahora incluye:
```json
{
  "sessionId": "...",
  "vehiculoId": "...",
  "origenLat": -9.65,
  "origenLon": -78.26,
  "destinoLat": -4.90,
  "destinoLon": -80.33
}
```

### Frontend — `DemoMapLeaflet.js`

- Punto **verde** `#22C55E` → origen (fijo desde el inicio, aunque no haya tracks aún)
- Punto **rojo** `#EF4444` → destino (fijo desde el inicio)
- Punto **púrpura** `#8B5CF6` → posición actual del vehículo (se mueve cada 5s)

Cuando `trackingData` está vacío (primeros segundos), el mapa ya muestra
origen y destino con zoom country-level para orientar al cliente.

---

## 7. Error: punto destino aparecía en el mismo lugar que el origen

**Causa:** `DemoService` tomaba el **último track** del pool como destino.
Como el viaje tiene múltiples ida y vuelta en un mes, el último track
es cuando el vehículo regresó al punto de partida.

**Solución:** Usar el track **más lejano geográficamente del origen** (Haversine)
como destino, en vez del último track cronológico.

---

## 8. Error: punto púrpura saltaba de un lado a otro (múltiples viajes en un mes)

**Causa:** El viaje BK-001-2025 tiene 8.322 tracks de **un mes entero**
(sept 17 → oct 18). El vehículo realizó múltiples viajes de ida y vuelta.
El demo los reproducía todos en orden, generando movimiento caótico.

**Solución:** `mejorDiaDeRecorrido()` — elige el día con mayor desplazamiento
neto (distancia entre primer y último track del día). Para BK-001-2025
elige el **21 de octubre de 2025** (1.867 tracks, lat -9.66 → -4.90, ~530 km).

```java
private List<Track> mejorDiaDeRecorrido(List<Track> tracks, Track referenciaOrigen) {
    // Agrupa por día UTC (gpstime / 86400)
    // Elige el día con mayor haversine(primero, último)
    // Invierte si el primer track está más lejos del origen real que el último
}
```

---

## 9. Error: inicio y fin invertidos

**Causa:** El mejor día (oct 21) correspondía al **viaje de vuelta** del vehículo
(sur → norte), pero el origen real del viaje está en el norte.
El demo arrancaba desde el sur (destino real) y terminaba en el norte (origen real).

**Solución:** Usar el **primer track del pool completo** como referencia de dirección.
Si el último track del mejor día está más cerca del origen real que el primero,
se invierte la lista:

```java
if (distUltimo < distPrimero) {
    Collections.reverse(mejorDia);
    logger.info("🎭 Tracks invertidos para coincidir con dirección del viaje");
}
```

---

## 10. Error: líneas extrañas en el mapa

**Causa:** El `Polyline` conectaba los últimos 100 tracks (por `slice(-100)`)
que en un viaje de un mes saltaban entre distintas partes de la ruta,
generando líneas que parecían conectar inicio y fin en diagonal.

**Solución:** Se eliminó el `Polyline` del `DemoMapLeaflet`. La demo muestra
solo los tres marcadores (origen, destino, posición actual). Es más limpio
y más representativo de cómo se vería en producción (el cliente ve el ícono
del vehículo moviéndose, no la ruta trazada).

---

## 11. Limpieza al detener la demo

Al presionar "Detener Demo" se limpian todos los estados:
```js
setDemoActivo(false);
setDemoSession(null);
setDemoOrigen(null);
setDemoDestino(null);
demoStartTimeRef.current = null;
setTrackingData([]);
```

---

## Archivos modificados

| Archivo | Cambios |
|---------|---------|
| `vilox-siq-backend/src/.../service/DemoService.java` | Fallback de tracks, `mejorDiaDeRecorrido()`, origen/destino desde tramos o Haversine, inversión de dirección |
| `vilox-siq-backend/src/.../controller/DemoController.java` | Devuelve `origenLat/Lon` y `destinoLat/Lon` en la respuesta de start |
| `vilox-siq-frontend/src/components/SeguimientoFlota.js` | Botón único con `DEMO_VIAJE_ID` hardcodeado, polling 5s en demo, `demoOrigen`/`demoDestino` state, `demoStartTimeRef`, limpieza al detener |
| `vilox-siq-frontend/src/components/DemoMapLeaflet.js` | Marcadores verde/rojo para origen/destino, eliminación del Polyline, mapa inicial con origen/destino aunque no haya tracks |

---

## Estado actual del demo (2026-05-03)

### Funciona:
- Un click inicia la demo sin fricción para el cliente
- El mapa Leaflet muestra origen (verde) y destino (rojo) desde el inicio
- El punto púrpura recorre un viaje limpio de un solo día (~1.867 puntos)
- Se actualiza cada 5 segundos
- Al terminar se detiene automáticamente
- Al detener manualmente limpia todos los tracks demo de la BD

### Limitaciones conocidas:
- En BD local, BK-001-2025 no tiene coordenadas en sus establecimientos →
  usa fallback Haversine para origen/destino. En producción con datos reales
  usará las coordenadas del establecimiento.
- El "mejor día" se calcula sobre tracks UTC, no hora Perú. Puede haber
  una diferencia de ±1 día en el corte si el viaje comenzó cerca de las 19:00 hora Perú.
- El viaje de demo dura ~1.5 horas (1.867 tracks × 3s = 5.600s ≈ 93 min).

### Para cambiar el viaje de demo:
Editar línea 41 de `SeguimientoFlota.js`:
```js
const DEMO_VIAJE_ID = "43ea68a6-71b0-4037-8a3e-5eb60165f04b"; // BK-001-2025 | V4M-797 | 5 tramos
```
El viaje debe existir en la BD y el vehículo debe tener tracks históricos.

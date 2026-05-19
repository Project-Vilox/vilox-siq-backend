# Cambios implementados — Feature Replay de Viaje

## Resumen

Se implementó la funcionalidad de replay GPS de viajes. Permite reproducir visualmente
el recorrido de un vehículo sobre un mapa, punto a punto, con controles de velocidad
y timeline. Usa tracks reales de la tabla `tracks`.

---

## 1. Base de Datos (PostgreSQL WSL2 — siq3)

### Cambios manuales de datos (solo para desarrollo/pruebas)

| Acción | Por qué |
|--------|---------|
| `UPDATE viajes SET fecha_inicio_real, fecha_fin_real` en viajes de V4M-797 | Los viajes completados tenían fechas reales en NULL. Sin fechas el ReplayController devuelve 400. Se asignaron fechas que coinciden con los tracks reales del vehículo. |
| `UPDATE viajes SET empresa_transportista_id` en BK-031-2025 | El viaje pertenecía a "Transportista ABC" pero el usuario de prueba es de "EXPERT TRANSPORT S.A.C.". Se reasignó para que sea visible al usuario adminflota@gmail.com. |

### Diagnóstico realizado

- Los tracks reales del vehículo V4M-797 están entre oct-nov 2025
- El día con recorrido de larga distancia real es **2025-10-05** (806 tracks, lat -6.31 a -5.07, zona Paita → Chiclayo)
- El vehículo con más tracks es `111e1111-e29b-41d4-a716-446655440001` (placa V4M-797): 32k tracks

---

## 2. Backend Java (vilox-siq-backend)

### Archivo nuevo: `controller/ReplayController.java`

**Qué hace:** Endpoint `GET /api/replay/{viajeId}`

**Flujo:**
1. Busca el viaje por ID en la tabla `viajes`
2. Obtiene el vehículo asignado → su ID
3. Usa `fecha_inicio_real` → `fecha_fin_real` como rango (fallback a fechas programadas)
4. Devuelve 400 si no hay vehículo o no hay fechas
5. Llama a `TrackService.getTracksByVehiculoIdAndHearttimeBetween()` con zona horaria Perú (UTC-5)
6. Devuelve `List<TrackDto>` ordenado por gpstime

**Por qué en Java:** Java es el dueño de los tracks GPS. Centralizar la lógica de búsqueda de tracks por viaje evita que el frontend calcule el rango de fechas.

### Cambio en `application-dev.properties`

| Parámetro | Antes | Después | Por qué |
|-----------|-------|---------|---------|
| `spring.datasource.url` | `192.168.192.1:5432/siq3_dev` | `127.0.0.1:5432/siq3` | Unificar BD con Laravel en WSL2 |
| `spring.jpa.hibernate.ddl-auto` | `update` | `none` | Evitar que Java altere el esquema de Laravel |

---

## 3. Backend Laravel (vilox.api)

### Cambio en `app/Http/Controllers/ViajeController.php`

**Qué cambió:** Se agregaron `latitud` y `longitud` a los campos seleccionados de `establecimientoOrigen` y `establecimientoDestino` en todos los métodos que cargan tramos (4 ocurrencias).

**Antes:**
```php
'tramos.establecimientoOrigen:id,nombre,tipo',
'tramos.establecimientoDestino:id,nombre,tipo'
```

**Después:**
```php
'tramos.establecimientoOrigen:id,nombre,tipo,latitud,longitud',
'tramos.establecimientoDestino:id,nombre,tipo,latitud,longitud'
```

**Por qué:** El componente `ReplayViaje.js` necesita las coordenadas reales del origen y destino del viaje para posicionar los marcadores verde (origen) y rojo (destino) en el mapa. Sin esto, los marcadores caían en el primer/último track GPS que no siempre coincide con el establecimiento real.

### Cambio en `config/cors.php`

Se agregaron `login` y `logout` a los paths permitidos por CORS.

**Por qué:** El frontend llamaba a `/login` pero CORS solo cubría `api/*`, causando error de preflight bloqueado.

---

## 4. Frontend React (vilox-siq-frontend)

### Archivo nuevo: `src/components/ReplayViaje.js`

**Qué hace:** Modal de replay GPS con mapa interactivo.

**Tecnología:** Leaflet + react-leaflet + OpenStreetMap (gratuito, sin API key ni billing).
Google Maps fue descartado por requerir billing habilitado.

**Funcionalidades:**
- Carga tracks desde `GET /api/replay/{viajeId}` (Java :8080)
- Marcador verde en coordenadas reales del establecimiento origen (primer tramo)
- Marcador rojo en coordenadas reales del establecimiento destino (último tramo)
- Fallback a primer/último track GPS si el establecimiento no tiene coordenadas
- Polyline gris: ruta completa
- Polyline azul: ruta recorrida hasta el punto actual
- Marcador azul: posición actual del vehículo
- Controles: ▶ Play, ⏸ Pause, ⏮ Reset, velocidades 1x / 5x / 10x / 30x
- Slider de timeline para saltar a cualquier punto
- Stats en tiempo real: velocidad, km recorridos, hora GPS actual
- Al terminar muestra badge "Completado" y detiene el intervalo

### Cambios en `src/components/Viajes.js`

| Cambio | Por qué |
|--------|---------|
| Import de `ReplayViaje` | Necesario para renderizar el modal |
| Estados `showReplay` y `replayViaje` | Controlar apertura del modal y qué viaje reproducir |
| Botón ▶ (morado) en cada fila de la tabla | Punto de entrada al replay desde la lista de viajes |
| Render del modal `<ReplayViaje>` al final del componente | Mostrar el modal sobre la UI cuando se activa |

### Cambio en `public/index.html`

Se removió el script de Google Maps API que se había agregado temporalmente.
**Por qué:** Al migrar a Leaflet ya no es necesario, y la key estaba expuesta en el HTML.

### Archivo nuevo: `.env`

```env
REACT_APP_VILOX_FLOTA_API=http://localhost:8080
REACT_APP_API_URL=http://localhost:8000/api
```

**Por qué:** Sin estas variables, las URLs en `api.js` y `Viajes.js` quedaban como `undefined/endpoint`.

---

## 5. Estado actual del Replay

### Funciona cuando:
- El viaje tiene `fecha_inicio_real` y `fecha_fin_real` definidas
- El vehículo tiene tracks GPS en ese rango de fechas
- El usuario logueado pertenece a la misma empresa transportista del viaje

### Limitaciones conocidas:
- La mayoría de viajes completados tienen `fecha_inicio_real = NULL` — requiere que el sistema registre las fechas reales al completar el viaje
- Los marcadores origen/destino dependen de que los establecimientos tengan `latitud` y `longitud` cargadas en la BD
- El replay muestra tracks del vehículo en el rango del viaje, no tracks vinculados directamente al viaje (no hay FK entre tracks y viajes)

---

## 6. Archivos modificados — resumen

| Archivo | Tipo de cambio |
|---------|---------------|
| `vilox-siq-backend/src/.../ReplayController.java` | Nuevo |
| `vilox-siq-backend/src/.../application-dev.properties` | Modificado |
| `vilox.api/app/Http/Controllers/ViajeController.php` | Modificado |
| `vilox.api/config/cors.php` | Modificado |
| `vilox-siq-frontend/src/components/ReplayViaje.js` | Nuevo |
| `vilox-siq-frontend/src/components/Viajes.js` | Modificado |
| `vilox-siq-frontend/public/index.html` | Modificado |
| `vilox-siq-frontend/.env` | Nuevo |

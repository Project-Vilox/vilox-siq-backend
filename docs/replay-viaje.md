# Feature: Replay de Viaje

## Objetivo

Permitir reproducir visualmente un viaje sobre el mapa, ya sea con tracks GPS reales
(viajes completados) o con una ruta sintética generada (viajes programados a futuro).
Sirve tanto para demo, análisis post-viaje y pruebas de desarrollo.

---

## Cambios por proyecto

### vilox-siq-backend (Java) — cambios nuevos

**¿Por qué acá?**
El backend Java es el dueño de los tracks GPS. Ya tiene TrackRepository y
TrackByVehicleController. El nuevo endpoint de replay es una extensión natural de eso.

#### Archivos nuevos

| Archivo | Qué hace |
|---------|----------|
| `controller/ReplayController.java` | Expone los endpoints de replay |
| `service/ReplayService.java` | Lógica: busca tracks del viaje, corrige gaps, ordena |
| `dto/ReplayTrackDto.java` | Payload de respuesta optimizado para animación |

#### Endpoints nuevos

```
GET  /api/replay/{viajeId}
```
- Recibe el ID del viaje
- Busca el vehículo asignado al viaje (via tabla `viajes`)
- Busca los tracks por IMEI en el rango `fecha_inicio_real` → `fecha_fin_real`
- Devuelve array ordenado por `gpstime` listo para animar
- **Por qué**: el frontend no debería calcular el rango de fechas — el backend conoce el viaje

```
POST /api/replay/simulate   (solo perfil dev)
{
  "viajeId": "uuid",
  "tracks": [ { "imei": "...", "gpstime": 000, "latitude": 0.0, "longitude": 0.0, "speed": 0.0 } ]
}
```
- Inserta tracks sintéticos en la tabla `tracks` con el IMEI del vehículo del viaje
- Permite probar el replay histórico sin tener tracks reales
- Protegido por perfil `dev` — no disponible en `prod`
- **Por qué**: hoy se hace con `INSERT INTO` manual en la BD; esto lo formaliza y automatiza

---

### vilox-siq-frontend (React) — cambios nuevos

**¿Por qué acá?**
La reproducción animada es 100% lógica de UI: avanzar un índice, mover un marcador,
actualizar stats. No tiene sentido en el backend.

#### Archivos nuevos

| Archivo | Qué hace |
|---------|----------|
| `components/ReplayViaje.js` | Componente principal de replay (modal o página) |

#### Archivos modificados

| Archivo | Qué cambia | Por qué |
|---------|------------|---------|
| `components/Viajes.js` | Agrega botón "Replay" por viaje en la lista | Punto de entrada a la feature |
| `components/SeguimientoFlota.js` | Reutiliza lógica de renderizado de mapa | Evitar duplicar código de Google Maps |

#### Lógica del componente ReplayViaje

**Modo histórico** (viaje COMPLETADO):
1. Llama a `GET /api/replay/{viajeId}` (Java)
2. Recibe array de TrackDto ordenados
3. `setInterval` avanza el índice según la velocidad elegida (1x, 5x, 10x)
4. Marcador en el mapa se mueve punto a punto
5. Slider de timeline permite saltar a cualquier momento

**Modo simulación** (viaje PROGRAMADO):
1. Toma origen y destino del viaje (establecimientos con coordenadas)
2. Llama a Google Maps Directions API para obtener la ruta
3. Genera puntos sintéticos cada N metros sobre esa ruta
4. Anima igual que el modo histórico
5. Botón "Guardar tracks" llama a `POST /api/replay/simulate` (solo dev)

#### UI del componente

```
┌─────────────────────────────────────────────────────┐
│  REPLAY DE VIAJE  [código]  Origen → Destino        │
├─────────────────────────────────────────────────────┤
│                                                     │
│              [ MAPA GOOGLE MAPS ]                   │
│   🟢 Origen                          🔴 Destino    │
│        ·····🚛·····················                 │
│                                                     │
├─────────────────────────────────────────────────────┤
│  ⏮  ▶  ⏭    [====●══════════════]   1x  5x  10x  │
│  12:30 hs                           14:45 hs        │
├─────────────────────────────────────────────────────┤
│  📍 Lat / Lon actuales     ⚡ velocidad km/h         │
│  📏 km recorridos          ⏱  tiempo transcurrido   │
└─────────────────────────────────────────────────────┘
```

---

### vilox.api (Laravel) — sin cambios

No requiere modificaciones. Es la fuente de verdad de viajes/tramos/vehículos,
pero los tracks los consulta directamente el backend Java.

---

## Flujo completo

```
[Viajes.js] botón "Replay" en un viaje
        ↓
[ReplayViaje.js] detecta estado del viaje
        ↓
  COMPLETADO → GET /api/replay/{viajeId} (Java)
  PROGRAMADO → Google Maps Directions API (cliente)
        ↓
Array de puntos GPS (reales o sintéticos)
        ↓
setInterval avanza índice → marcador se mueve en mapa
        ↓
Slider + controles de velocidad + stats en tiempo real
```

---

## Orden de implementación recomendado

1. `ReplayController` + `ReplayService` + `ReplayTrackDto` en Java
2. `ReplayViaje.js` en React — modo histórico primero
3. Botón "Replay" en `Viajes.js`
4. Modo simulación (requiere Google Maps API key)
5. `POST /api/replay/simulate` para persistir tracks sintéticos (dev)

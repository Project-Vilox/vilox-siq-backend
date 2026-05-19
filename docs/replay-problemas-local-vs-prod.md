# Replay — Problemas en local vs Producción

## Por qué tuve tantos problemas en local

### 1. La BD local es una copia desincronizada

En producción, los tracks GPS se insertan automáticamente en tiempo real desde
los dispositivos GPS vía ProTrack365. En local, la BD es un dump importado desde
DBeaver que captura un momento en el tiempo.

**Consecuencia:** Los viajes completados tienen `fecha_inicio_real = NULL` porque
en local nunca se ejecutó el flujo real de actualización de estados. Los tracks
existen, pero no están correlacionados con los viajes.

**En producción:** Este problema NO existe. Los viajes se completan con fechas reales
registradas, y los tracks se insertan automáticamente. El replay funciona solo.

---

### 2. Versión de PostgreSQL incompatible

El dump fue exportado desde Postgres 17 (Windows) e importado a Postgres 16 (WSL2).
Hubo errores de `unsupported version` al importar con `psql`, requiriendo exportar
en formato Plain en lugar de Custom.

**En producción:** NO existe este problema. El servidor de producción tiene una
sola instancia de Postgres con versión consistente.

---

### 3. Docker no disponible en WSL2

El proyecto usa Laravel Sail (Docker) para levantar Postgres. En WSL2 sin Docker
Desktop integrado, fue necesario instalar Postgres nativo y configurarlo manualmente.

**En producción:** NO existe. El entorno de producción (Render) tiene su propia BD
gestionada. Localmente se resuelve activando Docker Desktop con integración WSL2.

---

### 4. Dos instancias de Postgres (Windows + WSL2)

DBeaver estaba conectado al Postgres de Windows. Laravel y Java corrían en WSL2.
El `127.0.0.1` apunta a instancias distintas según desde dónde se conecta.

**En producción:** NO existe. Hay una sola BD centralizada en Render.

---

### 5. Java apuntaba a BD diferente (`siq3_dev`)

El `application-dev.properties` tenía `192.168.192.1:5432/siq3_dev` (Postgres Windows).
Laravel usaba `127.0.0.1:5432/siq3` (Postgres WSL2). Dos BDs distintas con datos distintos.

**En producción:** NO existe. El `application-prod.properties` apunta a la BD de Render.
Verificar que `application-prod.properties` tenga la misma URL que el `.env` de Laravel.

---

### 6. `ddl-auto=update` rompía el esquema

Spring Boot con `ddl-auto=update` modificaba tablas al arrancar, causando errores
de constraints y tipos incompatibles con el esquema de Laravel.

**En producción:** RIESGO REAL si `application-prod.properties` tiene `update`.
**Verificar ahora:**

```bash
grep ddl-auto src/main/resources/application-prod.properties
```

Debe ser `none` o `validate`. Si dice `update`, cambiarlo antes de hacer deploy.

---

### 7. Viajes sin `fecha_inicio_real` / `fecha_fin_real`

Los viajes completados en la BD local no tenían fechas reales porque fueron
creados como datos de prueba sin pasar por el flujo completo de la app.

**En producción:** Depende. Si los viajes se completan manualmente sin registrar
las fechas reales, el replay tampoco funcionará. Verificar que el flujo de
completar un viaje actualice `fecha_inicio_real` y `fecha_fin_real`.

---

### 8. Establecimientos sin coordenadas

Los marcadores de origen/destino no aparecían en el lugar correcto porque los
establecimientos no tenían `latitud` y `longitud` cargadas en la BD.

**En producción:** RIESGO REAL. Si los establecimientos no tienen coordenadas,
los marcadores caen en el primer/último track GPS. Verificar que los establecimientos
de producción tengan coordenadas cargadas.

---

### 9. Google Maps requiere billing habilitado

La API key de Google Maps funciona en producción solo si tiene una cuenta de
facturación asociada en Google Cloud Console, aunque el uso sea dentro del free tier.

**Solución aplicada:** Se reemplazó Google Maps por Leaflet + OpenStreetMap en
`ReplayViaje.js`. Gratuito, sin billing, sin key.

**En producción:** Con Leaflet no hay problema. Si en el futuro se quiere usar
Google Maps en otros componentes, habilitar billing en Google Cloud Console.

---

### 10. CORS bloqueaba `/login`

El `config/cors.php` de Laravel solo cubría `api/*`. El frontend llamaba a `/login`
sin el prefijo `/api/`, causando error de preflight bloqueado.

**En producción:** RIESGO REAL si el frontend de producción también llama a `/login`.
Ya está corregido en el código — verificar que el deploy incluya ese cambio.

---

## Resumen: ¿qué problemas tendrás en producción?

| Problema | ¿Ocurre en prod? | Acción requerida |
|----------|-----------------|-----------------|
| BD local desincronizada | ❌ No | — |
| Versión Postgres incompatible | ❌ No | — |
| Docker no disponible | ❌ No | — |
| Dos instancias de Postgres | ❌ No | — |
| Java apuntaba a BD diferente | ❌ No | Verificar application-prod.properties |
| `ddl-auto=update` en prod | ⚠️ Posible | Verificar y cambiar a `none` |
| Viajes sin fechas reales | ⚠️ Posible | Verificar flujo de completar viaje |
| Establecimientos sin coordenadas | ⚠️ Posible | Cargar latitud/longitud en prod |
| Google Maps billing | ❌ Resuelto | Usamos Leaflet |
| CORS bloqueando /login | ⚠️ Posible | Ya corregido, verificar deploy |

---

## Checklist antes de hacer deploy del replay a producción

- [ ] `application-prod.properties` tiene `ddl-auto=none`
- [ ] `application-prod.properties` apunta a la misma BD que Laravel
- [ ] Los establecimientos tienen `latitud` y `longitud` cargadas
- [ ] El flujo de completar viaje registra `fecha_inicio_real` y `fecha_fin_real`
- [ ] CORS incluye `login` y `logout` en paths permitidos
- [ ] `.env` del frontend de producción tiene `REACT_APP_VILOX_FLOTA_API` apuntando al backend Java de prod

# Configuración de Base de Datos

## Ambiente de desarrollo (dev)

### Cambios aplicados

- **Host**: `192.168.192.1` → `127.0.0.1` (Postgres en WSL2 en lugar de Windows)
- **Base de datos**: `siq3_dev` → `siq3` (BD unificada con vilox.api)
- **ddl-auto**: `update` → `validate` (Java ya no modifica el esquema, solo lo valida)

### Motivación

Los tres proyectos del monorepo (`vilox.api`, `vilox-siq-backend`, `vilox-siq-frontend`) deben compartir la misma BD `siq3` corriendo en Postgres 16 (WSL2).

El valor `update` en `ddl-auto` causaba que Spring Boot alterara tablas al arrancar, rompiendo constraints y relaciones que maneja Laravel.

### BD activa

| Parámetro | Valor |
|-----------|-------|
| Host | `127.0.0.1` |
| Puerto | `5432` |
| Base de datos | `siq3` |
| Usuario | `postgres` |
| Servidor | PostgreSQL 16 (WSL2) |

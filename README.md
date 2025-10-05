# FleetIQ - Sistema de Gestión de Fletes

FleetIQ es una aplicación backend desarrollada en Java con Spring Boot 4.0.0-M2, diseñada para el control y gestión integral de fletes, rastreo de vehículos y logística de transporte.

## 🚀 Características principales

- **Gestión de empresas**: Soporte para diferentes tipos de empresas (Transportistas, Operadores Logísticos, Exportadores, Almacenes, Cocheras)
- **Rastreo de vehículos**: Seguimiento en tiempo real de dispositivos GPS y ubicaciones
- **Gestión de viajes y tramos**: Control detallado de rutas y actividades de transporte
- **API RESTful**: Endpoints completos para operaciones CRUD
- **Integración con bases de datos**: Soporte para MySQL y H2 (desarrollo)
- **Exportación de datos**: Generación de archivos Excel con información de tablas
- **Arquitectura modular**: Diseño escalable con separación de responsabilidades

## 🏗️ Arquitectura del proyecto

```
vilox-siq-backend/
├── src/main/java/com/example/fleetIq/
│   ├── FleetIqApplication.java          # Clase principal Spring Boot
│   ├── ServletInitializer.java         # Configuración para WAR
│   ├── TestAuth.java                   # Pruebas de autenticación
│   ├── controller/                     # Controladores REST
│   │   ├── DeviceController.java
│   │   ├── EstablecimientoController.java
│   │   ├── OperadorLogisticoController.java
│   │   ├── TableController.java
│   │   ├── TrackController.java
│   │   ├── TramoController.java
│   │   ├── TransportistaController.java
│   │   └── ViajeController.java
│   ├── dto/                           # Data Transfer Objects
│   ├── model/                         # Entidades JPA
│   │   ├── Empresa.java
│   │   ├── Vehiculo.java
│   │   ├── Device.java
│   │   ├── Track.java
│   │   ├── Viaje.java
│   │   ├── Tramo.java
│   │   ├── Establecimiento.java
│   │   ├── Conductor.java
│   │   └── [otras entidades...]
│   ├── repository/                    # Repositorios JPA
│   ├── service/                       # Servicios de negocio
│   │   ├── impl/                     # Implementaciones
│   │   └── [interfaces...]
│   └── util/                         # Utilidades
│       └── ObjectUtil.java
├── src/main/resources/
│   ├── application.properties        # Configuración de la aplicación
│   ├── static/web/                   # Archivos estáticos web
│   └── templates/
└── src/test/java/                    # Pruebas unitarias
```

## 🛠️ Tecnologías utilizadas

- **Java 21** - Lenguaje de programación
- **Spring Boot 4.0.0-M2** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Spring WebFlux** - Programación reactiva
- **MySQL Connector** - Base de datos principal
- **H2 Database** - Base de datos para desarrollo
- **Lombok** - Reducción de código boilerplate
- **Apache POI** - Generación de archivos Excel
- **JSON Library** - Manejo de datos JSON
- **Maven** - Gestión de dependencias y construcción

## 📋 Requisitos del sistema

- **Java 21** o superior
- **Maven 3.8+**
- **MySQL 8.0+** (para producción)
- **IDE recomendado**: IntelliJ IDEA, Eclipse o VS Code

## ⚙️ Instalación y configuración

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/Project-Vilox/vilox-siq-backend.git
   cd vilox-siq-backend
   ```

2. **Configura la base de datos:**
   
   Edita `src/main/resources/application.properties`:
   ```properties
   # Para MySQL (producción)
   spring.datasource.url=jdbc:mysql://localhost:3307/fleetiq
   spring.datasource.username=root
   spring.datasource.password=rootpassword
   
   # Para H2 (desarrollo) - ya configurado por defecto
   ```

3. **Compila el proyecto:**
   ```bash
   mvn clean install
   ```

4. **Ejecuta la aplicación:**
   ```bash
   mvn spring-boot:run
   ```

5. **Accede a la aplicación:**
   - API: `http://localhost:8080`
   - Consola H2 (desarrollo): `http://localhost:8080/h2-console`

## 🔌 Endpoints principales

### Empresas y Transportistas
- `GET /api/transportista/{id}` - Información de transportista
- `GET /api/operador-logistico/{id}` - Información de operador logístico

### Vehículos y Rastreo
- `GET /api/devices` - Lista de dispositivos
- `GET /api/tracks` - Tracks de ubicación
- `GET /api/tracks/playback` - Reproducción de rutas

### Viajes y Tramos
- `GET /api/viajes` - Lista de viajes
- `POST /api/viajes` - Crear nuevo viaje
- `GET /api/tramos` - Lista de tramos por viaje
- `POST /api/tramos` - Crear nuevo tramo

### Establecimientos
- `GET /api/establecimientos` - Lista de establecimientos
- `GET /api/establecimientos/empresa/{empresaId}` - Por empresa
- `GET /api/establecimientos/publicos` - Establecimientos públicos

### Utilidades
- `GET /tables` - Visualización de tablas
- `GET /query` - Interfaz de consultas
- `POST /export` - Exportar datos a Excel

## 🗃️ Modelo de datos principales

### Entidades clave:
- **Empresa**: Diferentes tipos de empresas del ecosistema logístico
- **Vehiculo**: Vehículos con sus dispositivos de rastreo
- **Device**: Dispositivos GPS instalados en vehículos
- **Track**: Registros de ubicación en tiempo real
- **Viaje**: Viajes planificados con múltiples tramos
- **Tramo**: Segmentos específicos de un viaje (origen-destino)
- **Establecimiento**: Puntos de interés (almacenes, puertos, etc.)
- **Conductor**: Información de conductores

## 🧪 Pruebas

Ejecuta las pruebas unitarias:
```bash
mvn test
```

Ejecuta pruebas con reporte de cobertura:
```bash
mvn test jacoco:report
```

## 🚀 Despliegue

### Construcción para producción:
```bash
mvn clean package -Pprod
```

### Genera archivo WAR:
```bash
mvn clean package
# El archivo .war se genera en target/fleetIq-0.0.1-SNAPSHOT.war
```

## 🔧 Configuración avanzada

### Variables de entorno soportadas:
- `SPRING_PROFILES_ACTIVE` - Perfil activo (dev, prod)
- `DB_URL` - URL de base de datos
- `DB_USERNAME` - Usuario de base de datos
- `DB_PASSWORD` - Contraseña de base de datos

### Perfiles disponibles:
- `dev` - Desarrollo con H2
- `prod` - Producción con MySQL

## 🐛 Solución de problemas

### Error de token de tiempo:
Si aparece "Error al renovar el token: request time error", verifica:
1. Sincronización del reloj del servidor
2. Configuración de zona horaria
3. Conectividad con API externa de rastreo

### Problemas de base de datos:
1. Verifica que MySQL esté ejecutándose
2. Confirma credenciales en `application.properties`
3. Asegúrate de que la base de datos `fleetiq` exista

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Licencia

Este proyecto está bajo la licencia MIT. Ver `LICENSE` para más detalles.

## 👥 Equipo

- **Proyecto Vilox** - Desarrollo y mantenimiento

---

Para más información técnica, consulta la documentación en el código fuente o contacta al equipo de desarrollo.

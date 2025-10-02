# fleetIQ

FleetIQ es una aplicación backend desarrollada en Java con Spring Boot, diseñada para el control y gestión de fletes.

## Características principales

- Gestión de empresas y fletes.
- API RESTful para operaciones CRUD.
- Integración con Maven para gestión de dependencias y construcción.
- Estructura modular y escalable.

## Estructura del proyecto

```
vilox-siq-backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/fleetIq/
│   │   │   ├── FleetIqApplication.java
│   │   │   ├── ServletInitializer.java
│   │   │   └── repository/
│   │   │       └── EmpresaRepository.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/fleetIq/
│           └── FleetIqApplicationTests.java
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── pom.xml
├── .gitignore
└── README.md
```

## Requisitos

- Java 17 o superior
- Maven 3.8+
- (Opcional) IDE como IntelliJ IDEA, Eclipse o VS Code

## Instalación

1. Clona el repositorio:
   ```
   git clone https://github.com/tu-usuario/fleetIQ.git
   cd fleetIQ
   ```

2. Compila el proyecto:
   ```
   mvn clean install
   ```

3. Ejecuta la aplicación:
   ```
   mvn spring-boot:run
   ```

## Configuración

Edita el archivo `src/main/resources/application.properties` para configurar la conexión a la base de datos y otros parámetros.

## Pruebas

Ejecuta las pruebas unitarias con:
```
mvn test
```

## Estructura principal de código

- `FleetIqApplication.java`: Clase principal de arranque de Spring Boot.
- `ServletInitializer.java`: Configuración para despliegue en servlet containers.
- `repository/EmpresaRepository.java`: Acceso a datos de empresas.

## Contribuciones

Las contribuciones son bienvenidas. Por favor, abre un issue o envía un pull request.

## Licencia

Este proyecto está bajo la licencia MIT.

# FleetIQ Backend - Deployment en Render

## 📋 Configuración para Render

### 1. Configuración del Web Service

En Render, configura tu servicio con los siguientes valores:

- **Region**: Oregon (US West)
- **Root Directory**: (dejar vacío)
- **Dockerfile Path**: `./Dockerfile`

### 2. Variables de Entorno Requeridas

Configura estas variables de entorno en el dashboard de Render:

#### Base de Datos (PostgreSQL - ya configurada)
Como ya tienes PostgreSQL configurado en Render, puedes usar las variables existentes o crear nuevas:

```
DATABASE_URL=jdbc:postgresql://dpg-d3gb1015pdvs73ebg360-a.oregon-postgres.render.com:5432/vilox_db_cz40
DATABASE_USERNAME=vilox_db_cz40_user  
DATABASE_PASSWORD=dSjKnkQn8gKLKTe1MThbxrBqDeGsxhNU
```

**Nota**: Si usas las mismas credenciales que ya tienes, no necesitas configurar estas variables ya que están como valores por defecto.

#### JWT Configuration
```
JWT_SECRET=tu_clave_secreta_jwt_super_segura_de_al_menos_32_caracteres
```

#### Email Configuration
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-correo@gmail.com
MAIL_PASSWORD=tu-password-de-aplicacion
```

#### Frontend URL
```
FRONTEND_URL=https://tu-frontend-url.render.com
```

### 3. Comandos de Deployment

Render ejecutará automáticamente:
1. `docker build` usando el Dockerfile
2. El contenedor se ejecutará en el puerto asignado por Render

### 4. Estructura de Archivos para Docker

```
vilox-siq-backend/
├── Dockerfile                          # Configuración de Docker
├── .dockerignore                       # Archivos a ignorar en build
├── src/
│   └── main/
│       └── resources/
│           ├── application.properties          # Config con PostgreSQL
│           └── application-prod.properties     # Config producción
├── pom.xml                             # Dependencias (PostgreSQL incluida)
└── mvnw
```

### 5. Health Check

Una vez desplegado, puedes verificar que el servicio esté funcionando visitando:
```
https://tu-app.onrender.com/actuator/health
```

### 6. Logs y Debugging

Para ver los logs de la aplicación en Render:
1. Ve al dashboard de tu servicio
2. Haz clic en "Logs"
3. Busca mensajes como:
   - ✅ Token obtenido exitosamente
   - 📡 Servidor API timestamp
   - 🚀 Aplicación iniciada

### 7. Base de Datos PostgreSQL

✅ **Ya configurada**: Tu aplicación ya está usando PostgreSQL en Render con la configuración:
- Host: `dpg-d3gb1015pdvs73ebg360-a.oregon-postgres.render.com`
- Puerto: `5432`
- Base de datos: `vilox_db_cz40`

### 8. Troubleshooting

#### Error de conexión a base de datos
- La configuración PostgreSQL ya está establecida
- Si cambias las credenciales, actualiza las variables de entorno

#### Error de autenticación con API externa
- Los logs mostrarán la sincronización de tiempo
- El servicio se auto-sincroniza con el servidor de la API

#### Error de memoria
- El Dockerfile está configurado para usar máximo 512MB
- Render Free tier tiene 512MB de RAM disponible

## 🚀 Pasos para Deploy

1. **Commit y push** todos los archivos a tu repositorio Git
2. **Conecta el repositorio** a Render
3. **Configura las variables de entorno** (opcionales si mantienes las mismas credenciales)
4. **Deploy** automático cuando hagas push a main/master

## 📝 Notas Importantes

- ✅ **PostgreSQL ya configurado** - Usa tu base de datos existente en Render
- ✅ **Aplicación usa perfil `prod`** automáticamente en el contenedor
- ✅ **Logs optimizados** para producción
- ✅ **Sincronización de tiempo** funciona automáticamente
- ✅ **No requiere configuración adicional** de base de datos

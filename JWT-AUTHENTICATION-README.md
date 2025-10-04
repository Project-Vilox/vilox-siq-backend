# 🚛 FleetIQ - Sistema de Autenticación JWT
## Guía Completa de Implementación

### ✅ ¿Qué se ha implementado?

#### 🔐 **Sistema Completo de Autenticación JWT**
- ✅ **Entidad Usuario** con verificación por correo
- ✅ **Repositorio JPA** con consultas personalizadas
- ✅ **Servicio JWT** para generación y validación de tokens
- ✅ **Servicio de Email** para verificación de cuentas
- ✅ **Spring Security** configurado con filtros JWT
- ✅ **API REST** completa para autenticación
- ✅ **DTOs** para peticiones y respuestas

#### 📊 **Estructura de la Base de Datos**

**Tabla: `usuarios`**
```sql
- id (BIGINT, AUTO_INCREMENT, PRIMARY KEY)
- correo (VARCHAR(255), UNIQUE, NOT NULL)
- contrasena (VARCHAR(255), NOT NULL) -- Encriptada con BCrypt
- correo_verificado (BOOLEAN, DEFAULT FALSE)
- token_verificacion (VARCHAR(255))
- fecha_expiracion_token (DATETIME)
- fecha_creacion (DATETIME, NOT NULL)
- fecha_actualizacion (DATETIME)
- activo (BOOLEAN, DEFAULT TRUE)
- rol (ENUM: 'USUARIO', 'ADMIN', DEFAULT 'USUARIO')
```

### 🚀 **Endpoints API Disponibles**

#### **Autenticación Pública**
```
POST /api/auth/registro
POST /api/auth/login
POST /api/auth/verificar?token={token}
POST /api/auth/reenviar-verificacion
POST /api/auth/refresh
GET  /api/auth/validate
```

#### **Endpoints Protegidos**
- Requieren header: `Authorization: Bearer {token}`
- Rutas que empiecen con `/api/` (excepto `/api/auth/` y `/api/public/`)

### 📋 **Pasos para Configurar**

#### **1. Configurar Base de Datos**
Ejecutar el SQL en MySQL:
```sql
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correo VARCHAR(255) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    token_verificacion VARCHAR(255),
    fecha_expiracion_token DATETIME,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    rol ENUM('USUARIO', 'ADMIN') NOT NULL DEFAULT 'USUARIO'
);
```

#### **2. Configurar Email (application.properties)**
```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-correo@gmail.com
spring.mail.password=tu-password-de-aplicacion
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### **3. Configurar JWT (application.properties)**
```properties
# JWT Configuration
jwt.secret=tu_clave_secreta_super_segura_de_al_menos_32_caracteres
jwt.expiration=86400000          # 24 horas
jwt.refresh-expiration=604800000  # 7 días
```

#### **4. Configurar Frontend URL**
```properties
app.frontend.url=http://localhost:3000
```

### 🧪 **Ejemplos de Uso de la API**

#### **1. Registrar Usuario**
```bash
POST /api/auth/registro
Content-Type: application/json

{
    "correo": "usuario@ejemplo.com",
    "contrasena": "miPassword123",
    "confirmarContrasena": "miPassword123"
}
```

**Respuesta:**
```json
{
    "success": true,
    "message": "Usuario registrado exitosamente. Revisa tu correo para verificar tu cuenta.",
    "usuario": {
        "id": 1,
        "correo": "usuario@ejemplo.com",
        "correoVerificado": false
    }
}
```

#### **2. Verificar Cuenta**
```bash
POST /api/auth/verificar?token=uuid-token-from-email
```

#### **3. Iniciar Sesión**
```bash
POST /api/auth/login
Content-Type: application/json

{
    "correo": "usuario@ejemplo.com",
    "contrasena": "miPassword123"
}
```

**Respuesta:**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "usuario": {
        "id": 1,
        "correo": "usuario@ejemplo.com",
        "rol": "USUARIO",
        "correoVerificado": true,
        "activo": true
    }
}
```

#### **4. Usar Token en Peticiones Protegidas**
```bash
GET /api/mi-endpoint-protegido
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 🔧 **Flujo de Autenticación**

#### **Registro de Usuario:**
1. Usuario envía datos de registro
2. Sistema valida datos y crea usuario con `correo_verificado = false`
3. Se genera token de verificación con expiración de 24 horas
4. Se envía correo con enlace de verificación
5. Usuario hace clic en enlace y verifica cuenta

#### **Inicio de Sesión:**
1. Usuario envía credenciales
2. Sistema valida contraseña y estado de verificación
3. Se generan access token (24h) y refresh token (7 días)
4. Cliente usa access token para peticiones autenticadas

#### **Renovación de Token:**
1. Cuando access token expira, cliente usa refresh token
2. Sistema genera nuevo access token
3. Cliente continúa usando la aplicación

### 🔒 **Características de Seguridad**

- ✅ **Contraseñas encriptadas** con BCrypt
- ✅ **Tokens JWT firmados** con HMAC-SHA256
- ✅ **Verificación obligatoria por correo**
- ✅ **Tokens de verificación con expiración**
- ✅ **Roles de usuario** (USUARIO, ADMIN)
- ✅ **CORS configurado** para desarrollo
- ✅ **Filtros de seguridad** en Spring Security
- ✅ **Sesiones stateless** (solo JWT)

### 📧 **Sistema de Emails**

#### **Tipos de correos:**
1. **Verificación de cuenta** - Enviado al registrarse
2. **Bienvenida** - Enviado al verificar cuenta
3. **Restablecimiento** - Para recuperar contraseña (futuro)

#### **Configuración para Gmail:**
1. Activar autenticación de 2 factores
2. Generar contraseña de aplicación
3. Usar la contraseña de aplicación en `spring.mail.password`

### 🗂️ **Archivos Creados/Modificados**

#### **Nuevos Archivos:**
- `src/main/java/com/example/fleetIq/entity/Usuario.java`
- `src/main/java/com/example/fleetIq/repository/UsuarioRepository.java`
- `src/main/java/com/example/fleetIq/service/JwtService.java`
- `src/main/java/com/example/fleetIq/service/EmailService.java`
- `src/main/java/com/example/fleetIq/service/UsuarioService.java`
- `src/main/java/com/example/fleetIq/config/SecurityConfig.java`
- `src/main/java/com/example/fleetIq/config/JwtAuthenticationFilter.java`
- `src/main/java/com/example/fleetIq/controller/AuthController.java`
- `src/main/java/com/example/fleetIq/dto/RegistroRequest.java`
- `src/main/java/com/example/fleetIq/dto/LoginRequest.java`
- `src/main/java/com/example/fleetIq/dto/AuthResponse.java`
- `create-usuarios-table.sql`

#### **Archivos Modificados:**
- `pom.xml` - Agregadas dependencias JWT, Security, Mail
- `src/main/resources/application.properties` - Configuración completa

### 🚨 **Próximos Pasos Recomendados**

1. **Configurar servidor de email** (Gmail, SendGrid, etc.)
2. **Crear la tabla usuarios** en MySQL
3. **Probar endpoints** con Postman o similar
4. **Implementar frontend** para consumir la API
5. **Agregar restablecimiento de contraseña**
6. **Implementar logout** (blacklist de tokens)
7. **Añadir logs de auditoría**

### 💡 **Consejos de Desarrollo**

- Usar **variables de entorno** para secretos en producción
- Implementar **rate limiting** para endpoints de autenticación
- Configurar **HTTPS** en producción
- Usar **refresh tokens** para mejor UX
- Implementar **notificaciones** de dispositivos nuevos
- Agregar **configuración de perfil** de usuario

¡El sistema de autenticación JWT está completamente implementado y listo para usar! 🎉
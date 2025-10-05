# 🚀 Guía de Variables de Entorno para Render

## 📋 Variables de Entorno OBLIGATORIAS

Ve al Dashboard de Render > Tu Web Service > Environment Variables y agrega estas variables:

### 🔒 Variables OBLIGATORIAS

```bash
# Base de Datos PostgreSQL (OBLIGATORIO)
DATABASE_URL=jdbc:postgresql://dpg-d3gb1015pdvs73ebg360-a.oregon-postgres.render.com:5432/vilox_db_cz40
DATABASE_USERNAME=vilox_db_cz40_user
DATABASE_PASSWORD=dSjKnkQn8gKLKTe1MThbxrBqDeGsxhNU

# JWT Secret (OBLIGATORIO - cambia esta clave)
JWT_SECRET=aqui_pon_una_clave_super_segura_de_al_menos_64_caracteres_para_jwt_en_produccion

# Email Configuration (OBLIGATORIO para notificaciones)
MAIL_USERNAME=tu-correo-real@gmail.com
MAIL_PASSWORD=tu-password-de-aplicacion-google

# Frontend URL (OBLIGATORIO)
FRONTEND_URL=https://tu-frontend-app.onrender.com
```

### 🔧 Variables OPCIONALES (tienen valores por defecto)

```bash
# Email Server (OPCIONAL - usa Gmail por defecto)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
```

## 🎯 Cómo agregar en Render:

1. **Ve a tu Dashboard de Render**
2. **Selecciona tu Web Service** (o crea uno nuevo)
3. **Environment Variables** (en el menú lateral)
4. **Add Environment Variable** para cada una:

```
Key: DATABASE_URL
Value: jdbc:postgresql://dpg-d3gb1015pdvs73ebg360-a.oregon-postgres.render.com:5432/vilox_db_cz40

Key: DATABASE_USERNAME  
Value: vilox_db_cz40_user

Key: DATABASE_PASSWORD
Value: dSjKnkQn8gKLKTe1MThbxrBqDeGsxhNU

Key: JWT_SECRET
Value: tu_clave_super_segura_jwt_de_al_menos_64_caracteres_aqui

Key: MAIL_USERNAME  
Value: tu-correo@gmail.com

Key: MAIL_PASSWORD
Value: tu-password-de-aplicacion

Key: FRONTEND_URL
Value: https://tu-frontend.onrender.com
```

## ✅ VENTAJAS de esta configuración:

- 🔒 **Credenciales seguras**: No están hardcodeadas en el código
- 🔄 **Flexibilidad**: Puedes cambiar BD sin tocar código
- 🛡️ **Mejores prácticas**: Cumple estándares de seguridad
- 🚀 **Fácil deployment**: Render maneja todo automáticamente

## ⚠️ IMPORTANTE:

- **JWT_SECRET**: Debe ser diferente al de desarrollo (más de 32 caracteres)
- **MAIL_PASSWORD**: Usa un "App Password" de Google, no tu contraseña normal
- **DATABASE_***: Usa exactamente las credenciales de tu PostgreSQL en Render
- **FRONTEND_URL**: La URL real de tu frontend en Render

## 🔍 Verificación:

Después del deploy, revisa los logs en Render para confirmar:
- ✅ `Started FleetIqApplication` 
- ✅ `Using profile: prod`
- ✅ `Token obtenido exitosamente`
- ✅ `Initialized JPA EntityManagerFactory`

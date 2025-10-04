-- ============================================
-- Script SQL Simplificado para crear tabla de usuarios
-- FleetIQ - Sistema de Autenticación JWT
-- EJECUTAR CADA BLOQUE POR SEPARADO
-- ============================================

-- PASO 1: Crear tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correo VARCHAR(255) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    token_verificacion VARCHAR(255) NULL,
    fecha_expiracion_token DATETIME NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    rol ENUM('USUARIO', 'ADMIN') NOT NULL DEFAULT 'USUARIO'
);

-- PASO 2: Insertar usuario administrador por defecto
-- Contraseña: admin123 (ya encriptada con BCrypt)
INSERT IGNORE INTO usuarios (
    correo, 
    contrasena, 
    correo_verificado, 
    activo, 
    rol, 
    fecha_creacion, 
    fecha_actualizacion
) VALUES (
    'admin@fleetiq.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    TRUE, 
    TRUE, 
    'ADMIN', 
    NOW(), 
    NOW()
);

-- PASO 3: Verificar que la tabla se creó correctamente
SELECT COUNT(*) as 'Total usuarios' FROM usuarios;
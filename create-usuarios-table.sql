-- ============================================
-- Script SQL para crear tabla de usuarios
-- FleetIQ - Sistema de Autenticación JWT
-- ============================================

-- Crear tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
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

-- Crear índices para mejorar rendimiento
CREATE INDEX idx_usuarios_correo ON usuarios(correo);

CREATE INDEX idx_usuarios_token_verificacion ON usuarios(token_verificacion);

CREATE INDEX idx_usuarios_correo_verificado ON usuarios(correo_verificado);

CREATE INDEX idx_usuarios_activo ON usuarios(activo);

-- Insertar usuario administrador por defecto (opcional)
-- Contraseña: admin123 (encriptada con BCrypt)
INSERT INTO usuarios (correo, contrasena, correo_verificado, activo, rol, fecha_creacion, fecha_actualizacion) 
VALUES (
    'admin@fleetiq.com', 
    '$2a$10$rJZjBf6RzCb8yBL1cH7qb.AZs3l2BQBZcFjl.HQoQqYGdK2Jg5m2.',  -- admin123
    TRUE, 
    TRUE, 
    'ADMIN', 
    NOW(), 
    NOW()
) ON DUPLICATE KEY UPDATE 
    correo = VALUES(correo);

-- Mostrar estructura de la tabla
DESCRIBE usuarios;

-- Verificar datos iniciales
SELECT id, correo, correo_verificado, activo, rol, fecha_creacion 
FROM usuarios 
ORDER BY fecha_creacion DESC;
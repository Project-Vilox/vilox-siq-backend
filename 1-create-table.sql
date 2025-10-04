-- Crear SOLO la tabla usuarios
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
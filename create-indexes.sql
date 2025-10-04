-- ============================================
-- Script para crear índices en tabla usuarios
-- Ejecutar DESPUÉS de crear la tabla
-- ============================================

-- Crear índices para mejorar rendimiento
ALTER TABLE usuarios ADD INDEX idx_usuarios_correo (correo);
ALTER TABLE usuarios ADD INDEX idx_usuarios_token_verificacion (token_verificacion);
ALTER TABLE usuarios ADD INDEX idx_usuarios_correo_verificado (correo_verificado);
ALTER TABLE usuarios ADD INDEX idx_usuarios_activo (activo);
ALTER TABLE usuarios ADD INDEX idx_usuarios_rol (rol);

-- Verificar índices creados
SHOW INDEX FROM usuarios;
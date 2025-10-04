-- Insertar SOLO el usuario administrador
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
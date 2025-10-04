-- Verificar los datos
SELECT COUNT(*) as 'Total usuarios' FROM usuarios;
SELECT id, correo, correo_verificado, activo, rol, fecha_creacion FROM usuarios;
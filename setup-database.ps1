# ============================================
# Script para crear tabla de usuarios en MySQL
# Usando PowerShell y .NET MySQL Connector
# ============================================

Write-Host "🗄️ Creando tabla de usuarios en MySQL..." -ForegroundColor Green

# Configuración de conexión
$server = "localhost"
$port = "3307"
$database = "fleetiq"
$username = "root"
$password = "rootpassword"

# SQL para crear tabla
$createTableSQL = @"
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
"@

# SQL para insertar admin
$insertAdminSQL = @"
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
    '`$2a`$10`$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    TRUE, 
    TRUE, 
    'ADMIN', 
    NOW(), 
    NOW()
);
"@

# SQL para verificar
$verifySQL = @"
SELECT COUNT(*) as total_usuarios FROM usuarios;
SELECT id, correo, correo_verificado, activo, rol FROM usuarios;
"@

Write-Host "📋 SQL a ejecutar:" -ForegroundColor Yellow
Write-Host $createTableSQL -ForegroundColor Cyan
Write-Host $insertAdminSQL -ForegroundColor Cyan

Write-Host "`n💡 Para ejecutar manualmente en MySQL:" -ForegroundColor Yellow
Write-Host "1. Conectar a MySQL: mysql -h localhost -P 3307 -u root -p" -ForegroundColor White
Write-Host "2. Seleccionar BD: USE fleetiq;" -ForegroundColor White
Write-Host "3. Ejecutar los comandos SQL mostrados arriba" -ForegroundColor White

Write-Host "`n📝 Guardando SQL en archivo temporal..." -ForegroundColor Yellow
$sqlContent = $createTableSQL + "`n" + $insertAdminSQL + "`n" + $verifySQL
$sqlContent | Out-File -FilePath "temp-create-usuarios.sql" -Encoding UTF8

Write-Host "✅ SQL guardado en: temp-create-usuarios.sql" -ForegroundColor Green
Write-Host "🔧 Puedes ejecutarlo manualmente con tu cliente MySQL preferido" -ForegroundColor Cyan

# Mostrar el contenido del archivo para copiar/pegar
Write-Host "`n📄 Contenido del archivo SQL:" -ForegroundColor Yellow
Write-Host "================================" -ForegroundColor Gray
Get-Content "temp-create-usuarios.sql"
Write-Host "================================" -ForegroundColor Gray
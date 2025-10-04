# Script para descargar dependencias Spring Security y JWT
# Ejecutar desde el directorio raíz del proyecto

Write-Host "🔧 Descargando dependencias Spring Security y JWT..." -ForegroundColor Green

# Crear directorio lib si no existe
if (!(Test-Path "lib")) {
    New-Item -ItemType Directory -Name "lib"
    Write-Host "📁 Directorio lib creado" -ForegroundColor Yellow
}

# URLs de las dependencias
$dependencies = @{
    "spring-security-core-6.2.0.jar" = "https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/6.2.0/spring-security-core-6.2.0.jar"
    "spring-security-web-6.2.0.jar" = "https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/6.2.0/spring-security-web-6.2.0.jar"
    "spring-security-config-6.2.0.jar" = "https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/6.2.0/spring-security-config-6.2.0.jar"
    "spring-security-crypto-6.2.0.jar" = "https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/6.2.0/spring-security-crypto-6.2.0.jar"
    "jjwt-api-0.12.3.jar" = "https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-api/0.12.3/jjwt-api-0.12.3.jar"
    "jjwt-impl-0.12.3.jar" = "https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-impl/0.12.3/jjwt-impl-0.12.3.jar"
    "jjwt-jackson-0.12.3.jar" = "https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-jackson/0.12.3/jjwt-jackson-0.12.3.jar"
}

# Descargar cada dependencia
foreach ($jar in $dependencies.Keys) {
    $url = $dependencies[$jar]
    $outputPath = "lib\$jar"
    
    if (Test-Path $outputPath) {
        Write-Host "✅ $jar ya existe" -ForegroundColor Green
    } else {
        Write-Host "⬇️ Descargando $jar..." -ForegroundColor Yellow
        try {
            Invoke-WebRequest -Uri $url -OutFile $outputPath
            Write-Host "✅ $jar descargado" -ForegroundColor Green
        } catch {
            Write-Host "❌ Error descargando $jar : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "🎉 Descarga completada!" -ForegroundColor Green
Write-Host "📋 Próximos pasos:" -ForegroundColor Cyan
Write-Host "   1. Abre IntelliJ IDEA" -ForegroundColor White
Write-Host "   2. File → Project Structure → Libraries" -ForegroundColor White
Write-Host "   3. Agrega todos los JARs de la carpeta lib/" -ForegroundColor White
Write-Host "   4. Compila el proyecto" -ForegroundColor White
Write-Host ""
Write-Host "🔧 O compila desde línea de comandos:" -ForegroundColor Cyan
Write-Host "   javac -cp `"lib/*;target/classes`" src/main/java/com/example/fleetIq/**/*.java" -ForegroundColor White
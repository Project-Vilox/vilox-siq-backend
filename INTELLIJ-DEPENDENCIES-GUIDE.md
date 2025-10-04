# 🔧 Configuración de Dependencias en IntelliJ IDEA

## 📥 DESCARGAR DEPENDENCIAS MANUALMENTE

### 1️⃣ Spring Security JARs
Descargar estos archivos JAR y agregarlos al proyecto:

```
https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/6.2.0/spring-security-core-6.2.0.jar
https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/6.2.0/spring-security-web-6.2.0.jar
https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/6.2.0/spring-security-config-6.2.0.jar
https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/6.2.0/spring-security-crypto-6.2.0.jar
```

### 2️⃣ JWT Libraries
```
https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-api/0.12.3/jjwt-api-0.12.3.jar
https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-impl/0.12.3/jjwt-impl-0.12.3.jar
https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-jackson/0.12.3/jjwt-jackson-0.12.3.jar
```

## 🛠️ CONFIGURAR EN INTELLIJ IDEA

### Paso 1: Agregar JARs como Libraries
1. Abre **File** → **Project Structure** (Ctrl+Alt+Shift+S)
2. Ve a **Libraries** en el panel izquierdo
3. Haz clic en **+** → **Java**
4. Selecciona todos los JARs descargados
5. Aplica los cambios

### Paso 2: Agregar al Module Path
1. En **Project Structure** → **Modules**
2. Selecciona tu módulo principal
3. Ve a la pestaña **Dependencies**
4. Asegúrate de que las librerías estén listadas
5. Aplica los cambios

### Paso 3: Refresh del Proyecto
1. Haz clic derecho en el proyecto
2. Selecciona **Reload Gradle Project** (si usas Gradle)
3. O **Reimport** para Maven

## 🚀 ALTERNATIVA: Usar Maven Wrapper

Si tu proyecto tiene Maven Wrapper:

```powershell
# En el directorio del proyecto
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

## 📁 UBICACIÓN DE DEPENDENCIAS

Coloca los JARs en:
```
proyecto/
├── lib/
│   ├── spring-security-*.jar
│   ├── jjwt-*.jar
│   └── ...
└── src/
```

Luego agrega al classpath:
```powershell
javac -cp "lib/*;target/classes" src/main/java/com/example/fleetIq/**/*.java
```

## ✅ VERIFICAR INSTALACIÓN

Después de configurar, deberías poder compilar sin errores:
- SecurityConfig.java
- JwtService.java  
- AuthController.java
- UsuarioService.java

## 🎯 NOTAS IMPORTANTES

- Las versiones pueden cambiar, verifica las últimas en Maven Central
- Spring Boot 4.0.0-M2 (milestone) puede tener compatibilidad limitada
- Considera bajar a Spring Boot 3.2.x para mayor estabilidad

---
*Guía para configurar dependencias Spring Security en IntelliJ IDEA*
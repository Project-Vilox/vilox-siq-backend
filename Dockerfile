# Usar imagen con Maven y OpenJDK 21
FROM maven:3.9.4-openjdk-21-slim

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el archivo pom.xml y descargar dependencias (para aprovechar cache de Docker)
COPY pom.xml ./

# Descargar dependencias (esto se cachea si no cambia el pom.xml)
RUN mvn dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Exponer el puerto 8080
EXPOSE 8080

# Variables de entorno para producción
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando para ejecutar la aplicación (forzar perfil prod)
CMD ["java", "-jar", "-Dspring.profiles.active=prod", "target/fleetIq-0.0.1-SNAPSHOT.war"]

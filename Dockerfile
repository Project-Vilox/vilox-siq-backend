# Usar imagen base que sabemos que existe
FROM eclipse-temurin:21-jdk-jammy

# Instalar Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

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

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "-Dspring.profiles.active=prod", "target/fleetIq-0.0.1-SNAPSHOT.war"]

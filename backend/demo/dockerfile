# ==========================================
# STAGE 1: Builder (Compilación)
# Eclipse Temurin 25 (Sin bloqueos) + Maven
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS builder

# Instalar Maven desde los repositorios de Alpine
RUN apk add --no-cache maven

WORKDIR /app

# Copiar el pom.xml y descargar dependencias para aprovechar la caché
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar omitiendo tests
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Runtime (Imagen Final)
# Usamos el JRE ligero de Java 25 para ejecutar el JAR empaquetado
# ==========================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copiar el ejecutable generado directamente desde el Stage 1
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto predeterminado de Spring Boot
EXPOSE 8080

# Punto de entrada para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
# ==========================================
# STAGE 1: Builder (Compilación)
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS builder

RUN apk add --no-cache maven
WORKDIR /app

# Descargar dependencias en caché
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilar la aplicación
COPY src ./src
RUN mvn clean package -DskipTests

# Extraer las capas del JAR usando la herramienta de Spring Boot
RUN java -Djarmode=layertools -jar target/*.jar extract

# ==========================================
# STAGE 2: Runtime (Imagen Final Ligera)
# ==========================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copiar cada capa por separado (las más pesadas cambian con menos frecuencia)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8080

# Ejecutar usando el Launcher de Spring Boot en lugar de "java -jar"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
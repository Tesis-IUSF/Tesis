# ==========================================
# STAGE 1: Builder
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS builder

RUN apk add --no-cache maven
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Extraer las capas correctamente
RUN cd target && jar xf *.jar

# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copiar desde la raíz extraída (sin subcarpeta "extracted")
COPY --from=builder /app/target/BOOT-INF/lib /app/BOOT-INF/lib
COPY --from=builder /app/target/BOOT-INF/classes /app/BOOT-INF/classes
COPY --from=builder /app/target/META-INF /app/META-INF
COPY --from=builder /app/target/org /app/org

EXPOSE 8080

ENTRYPOINT ["java", "-cp", "/app:/app/BOOT-INF/lib/*:/app/BOOT-INF/classes", "com.tesis.DemoApplication"]
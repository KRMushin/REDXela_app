# ── Build stage ─────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache de dependencias: copiar solo pom.xml primero
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar código fuente y compilar (incluye tests)
COPY src ./src
RUN mvn package -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Instalar curl para el healthcheck de Docker Compose
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

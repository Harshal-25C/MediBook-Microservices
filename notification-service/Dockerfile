# ─────────────────────────────────────────────────────────────────
# Multi-stage Dockerfile for MediBook Spring Boot Microservices
# Java 17 · Spring Boot 3.2 · Uses pre-built JAR
# ─────────────────────────────────────────────────────────────────
 
# Stage 1 — Build (Maven)
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
 
# Copy parent POM first (so Docker can cache the dependency layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
 
# Copy actual source
COPY src ./src
RUN mvn clean package -DskipTests -B
 
# Stage 2 — Runtime (lightweight JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
 
# Security: run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
 
# Copy the fat JAR from the build stage
COPY --from=builder /app/target/*.jar app.jar
 
# Spring Boot actuator health-check port (adjust per service)
EXPOSE 8087
 
# Pass env vars at runtime via docker-compose; do NOT bake secrets into image
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=docker", \
  "-jar", "app.jar"]
 
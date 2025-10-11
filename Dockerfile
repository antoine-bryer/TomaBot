# Multi-stage build to optimize size
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /tomabotApp

# Copy Maven configuration files
COPY pom.xml .
COPY src ./src

# Build application (skip tests for Docker build)
RUN mvn clean package -DskipTests

# Final stage: lightweight image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /tomabotApp

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage
COPY --from=build /tomabotApp/target/TomaBot-*.jar app.jar

# Change ownership
RUN chown -R spring:spring /tomabotApp

USER spring:spring

# Default environment variables (can be overridden)
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS=""

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

# Entrypoint with JAVA_OPTS for JVM tuning
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
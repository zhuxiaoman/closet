# Multi-stage build for closet-backend
# Stage 1: build the Spring Boot fat jar with Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN mvn -B -q dependency:go-offline

# Now copy source and build (skip tests; IT tests run separately in dev)
COPY src src
RUN mvn -B -q package -DskipTests

# Stage 2: minimal runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the fat jar built in stage 1
COPY --from=build /app/target/closet-backend-*.jar /app/app.jar

# Spring Boot reads PORT from env; default 8080
EXPOSE 8080
ENV JAVA_OPTS=""

# Healthcheck via the standard Spring Boot actuator (if enabled)
# or just rely on docker-compose healthcheck for postgres/minio.

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

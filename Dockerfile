# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy Maven configuration first
COPY pom.xml .

# Download dependencies separately
RUN mvn -B -ntp dependency:go-offline

# Copy application source
COPY src ./src

# Build application
RUN mvn -B -ntp clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
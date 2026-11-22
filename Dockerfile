
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy Maven mirror configuration
COPY settings.xml /root/.m2/settings.xml

# Copy dependency configuration
COPY pom.xml .

# Download dependencies
RUN mvn -B -ntp -s /root/.m2/settings.xml dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn -B -ntp -s /root/.m2/settings.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

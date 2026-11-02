RUN mvn -B -ntp -DskipTests package

# Stage 2: Run application
FROM eclipse-temurin:17-jre

WORKDIR /app

# Run as non-root user
RUN useradd -r -u 10001 spring

COPY --from=builder /app/target/*.jar /app/app.jar

RUN chown spring:spring /app/app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
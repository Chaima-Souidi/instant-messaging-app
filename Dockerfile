# Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Run
FROM eclipse-temurin:21-alpine AS runner
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/target/*-fat.jar app.jar
RUN mkdir -p /app/db && chown -R appuser:appgroup /app
USER appuser
CMD ["java", "-jar", "app.jar", "-conf", "/conf/config.json"]

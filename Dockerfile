# ============================================
# Smart Expense Tracker - Dockerfile
# Multi-stage build for minimal production image
# ============================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built jar
COPY --from=builder /app/target/smart-expense-tracker-*.jar app.jar

# Create logs directory
RUN mkdir -p logs

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

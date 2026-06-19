# 🛠️ Setup Guide — Smart Expense Tracker System

This guide walks you through setting up the Smart Expense Tracker System for local development, testing, and production deployment.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Environment Configuration](#environment-configuration)
- [Docker Deployment](#docker-deployment)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

Ensure the following tools are installed on your system before proceeding:

| Tool | Required Version | Required? | Download |
|------|-----------------|-----------|----------|
| Java JDK | 21 or higher | ✅ Yes | [Eclipse Adoptium](https://adoptium.net/) |
| Apache Maven | 3.9+ | ✅ Yes | [Maven Downloads](https://maven.apache.org/download.cgi) |
| MySQL Server | 8.0+ | ✅ Yes | [MySQL Downloads](https://dev.mysql.com/downloads/) |
| Node.js | 18+ | ⬜ Optional | [Node.js Downloads](https://nodejs.org/) |
| Docker & Docker Compose | 24+ / 2.20+ | ⬜ Optional | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| Git | 2.x | ✅ Yes | [Git Downloads](https://git-scm.com/downloads) |

### Verify Installations

```bash
java --version          # Should show 21+
mvn --version           # Should show 3.9+
mysql --version         # Should show 8.0+
node --version          # (Optional) Should show 18+
docker --version        # (Optional) Should show 24+
docker compose version  # (Optional) Should show 2.20+
```

---

## Database Setup

### Step 1: Start MySQL Server

```bash
# Linux / macOS
sudo systemctl start mysql

# Windows (if installed as a service)
net start mysql80

# Docker (alternative)
docker run -d --name mysql-expense -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  mysql:8.0
```

### Step 2: Create the Database and User

Connect to MySQL as root:

```bash
mysql -u root -p
```

Execute the following SQL commands:

```sql
-- Create the database
CREATE DATABASE IF NOT EXISTS smart_expense_tracker
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Create a dedicated application user
CREATE USER IF NOT EXISTS 'expense_user'@'localhost'
  IDENTIFIED BY 'Expense@2024#Secure';

-- Grant privileges
GRANT ALL PRIVILEGES ON smart_expense_tracker.* TO 'expense_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify
SHOW DATABASES LIKE 'smart_expense%';
SELECT user, host FROM mysql.user WHERE user = 'expense_user';
```

### Step 3: (Optional) Seed Initial Data

```sql
USE smart_expense_tracker;

-- Default categories
INSERT INTO categories (name, type, icon, user_id) VALUES
  ('Salary', 'INCOME', '💼', NULL),
  ('Freelance', 'INCOME', '💻', NULL),
  ('Food & Dining', 'EXPENSE', '🍕', NULL),
  ('Transportation', 'EXPENSE', '🚗', NULL),
  ('Utilities', 'EXPENSE', '💡', NULL),
  ('Entertainment', 'EXPENSE', '🎬', NULL),
  ('Healthcare', 'EXPENSE', '🏥', NULL),
  ('Shopping', 'EXPENSE', '🛒', NULL);
```

> **Note:** Tables are auto-created by Hibernate on first run (`spring.jpa.hibernate.ddl-auto=update`).

---

## Backend Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/Buvaneshwari26/Smart_Expense_Tracker_System.git
cd Smart_Expense_Tracker_System
```

### Step 2: Configure Application Properties

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_expense_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: expense_user
    password: Expense@2024#Secure
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

app:
  jwt:
    secret: your-256-bit-secret-key-here-make-it-long-and-random
    expiration-ms: 3600000        # 1 hour
    refresh-expiration-ms: 604800000  # 7 days
```

### Step 3: Build the Project

```bash
mvn clean install
```

If you want to skip tests during the initial build:

```bash
mvn clean install -DskipTests
```

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/smart-expense-tracker-0.0.1-SNAPSHOT.jar
```

### Step 5: Verify

Open your browser and navigate to:

| Endpoint | URL |
|----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API Docs (JSON) | http://localhost:8080/v3/api-docs |
| Health Check | http://localhost:8080/actuator/health |

---

## Frontend Setup

### Option A: Direct Browser

Simply open `frontend/index.html` in your web browser.

### Option B: Local HTTP Server

Using Python:

```bash
cd frontend
python -m http.server 5500
```

Using Node.js:

```bash
cd frontend
npx http-server -p 5500 -c-1
```

Using VS Code:

1. Install the **Live Server** extension
2. Right-click `frontend/index.html` → **Open with Live Server**

### Frontend Configuration

Update the API base URL in `frontend/js/config.js`:

```javascript
const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api',
  TIMEOUT: 30000, // 30 seconds
};
```

---

## Environment Configuration

The application supports multiple Spring profiles:

### Local Profile (Default)

```bash
mvn spring-boot:run
```

Uses default `application.yml` settings.

### Development Profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_expense_tracker_dev
    username: expense_user
    password: Expense@2024#Secure
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

logging:
  level:
    com.expense.tracker: DEBUG
    org.springframework.security: DEBUG
```

### Production Profile

```bash
java -jar target/smart-expense-tracker.jar --spring.profiles.active=prod
```

`application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    com.expense.tracker: WARN
    root: WARN
```

### Environment Variables (Production)

```bash
export DB_HOST=your-db-host.com
export DB_PORT=3306
export DB_NAME=smart_expense_tracker
export DB_USERNAME=prod_user
export DB_PASSWORD=super-secure-password
export JWT_SECRET=your-production-jwt-secret-256-bit
```

---

## Docker Deployment

### Step 1: Build the Docker Image

```bash
docker build -t smart-expense-tracker:latest .
```

### Step 2: Run with Docker Compose

```bash
docker-compose up --build -d
```

### `Dockerfile`

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`

```yaml
version: '3.8'

services:
  app:
    build: .
    container_name: expense-tracker-app
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/smart_expense_tracker?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: expense_user
      SPRING_DATASOURCE_PASSWORD: Expense@2024#Secure
      APP_JWT_SECRET: docker-jwt-secret-change-in-production
    networks:
      - expense-network

  db:
    image: mysql:8.0
    container_name: expense-tracker-db
    ports:
      - "3306:3306"
    environment:
      MYSQL_DATABASE: smart_expense_tracker
      MYSQL_USER: expense_user
      MYSQL_PASSWORD: Expense@2024#Secure
      MYSQL_ROOT_PASSWORD: rootpass
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - expense-network

volumes:
  mysql_data:
    driver: local

networks:
  expense-network:
    driver: bridge
```

### Step 3: Verify Docker Deployment

```bash
# Check running containers
docker-compose ps

# View application logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## Troubleshooting

### 1. MySQL Connection Refused

**Error:** `Communications link failure` or `Connection refused`

**Solutions:**
- Verify MySQL is running: `sudo systemctl status mysql`
- Check the port: `netstat -tlnp | grep 3306`
- Ensure `allowPublicKeyRetrieval=true` is in the JDBC URL
- If using Docker, ensure the container is healthy: `docker ps`

### 2. JWT Token Invalid or Expired

**Error:** `401 Unauthorized` on API requests

**Solutions:**
- Ensure you're passing the token in the `Authorization: Bearer <token>` header
- Check token expiration — default is 1 hour
- Use the `/api/auth/refresh-token` endpoint to get a new access token
- Verify the JWT secret in `application.yml` matches across restarts

### 3. Maven Build Failure

**Error:** `BUILD FAILURE` during `mvn clean install`

**Solutions:**
```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Force update dependencies
mvn clean install -U

# Check Java version
java --version  # Must be 21+
```

### 4. Port Already in Use

**Error:** `Port 8080 is already in use`

**Solutions:**
```bash
# Find the process using port 8080
# Linux/macOS
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Or change the port in `application.yml`:

```yaml
server:
  port: 9090
```

### 5. Hibernate DDL Errors

**Error:** Table creation or schema mismatch errors

**Solutions:**
- Set `spring.jpa.hibernate.ddl-auto=create-drop` for a fresh start (⚠️ deletes data)
- Check MySQL character set compatibility
- Ensure entity annotations match the database schema

### 6. Email Notification Failures

**Error:** `AuthenticationFailedException` or `MessagingException`

**Solutions:**
- Enable **2-Step Verification** on your Google account
- Generate an **App Password** at https://myaccount.google.com/apppasswords
- Use the app password (not your account password) in `application.yml`
- Check firewall rules for outbound SMTP (port 587)

### 7. CORS Errors in Browser

**Error:** `Access to XMLHttpRequest has been blocked by CORS policy`

**Solutions:**
- Verify CORS configuration in `SecurityConfig.java` or `WebConfig.java`
- Ensure the frontend URL is whitelisted:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5500", "http://127.0.0.1:5500"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

> **Need Help?** Open an issue on the [GitHub repository](https://github.com/Buvaneshwari26/Smart_Expense_Tracker_System/issues).

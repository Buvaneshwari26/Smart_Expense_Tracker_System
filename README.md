<div align="center">

# 💰 Smart Expense Tracker System

![Build Status](https://github.com/Buvaneshwari26/Smart_Expense_Tracker_System/actions/workflows/build.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**A full-stack personal finance management application built with Spring Boot and modern web technologies.**

Track expenses, manage incomes, set budgets, define savings goals, and gain actionable insights through interactive dashboards and comprehensive reports.

[Getting Started](#-quick-start) •
[API Docs](#-api-documentation) •
[Features](#-features) •
[Contributing](#-contributing)

</div>

---

## 📸 Screenshots

<div align="center">

| Dashboard | Expense Management |
|:-:|:-:|
| ![Dashboard](docs/screenshots/dashboard.png) | ![Expenses](docs/screenshots/expenses.png) |

| Budget Tracking | Reports & Analytics |
|:-:|:-:|
| ![Budgets](docs/screenshots/budgets.png) | ![Reports](docs/screenshots/reports.png) |

</div>

> **Note:** Replace the placeholder paths above with actual screenshot images.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔐 **Authentication & Authorization** | Secure JWT-based authentication with role-based access control and refresh tokens |
| 💸 **Expense Management** | Full CRUD operations for expenses with category tagging, search, and filtering |
| 💵 **Income Tracking** | Record and manage multiple income sources with detailed breakdowns |
| 📊 **Budget Tracking** | Set monthly/yearly budgets per category and receive alerts when limits are approached |
| 🎯 **Savings Goals** | Define savings targets, track progress, and add contributions over time |
| 📈 **Dashboard KPIs** | Real-time key performance indicators — total income, expenses, net savings, and trends |
| 📋 **Reports & Analytics** | Monthly, yearly, and category-wise reports with income-vs-expense comparisons |
| 📤 **Export (PDF / Excel / CSV)** | Export expense and income data in multiple formats for offline analysis |
| 📧 **Email Notifications** | Automated alerts for budget overruns, goal milestones, and periodic summaries |
| 📖 **Swagger API Docs** | Interactive API documentation via Swagger UI for seamless developer onboarding |

---

## 🛠️ Tech Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 25 | Core programming language |
| Spring Boot | 3.x | Application framework |
| Spring Security | 6.x | Authentication & authorization |
| JSON Web Tokens (JWT) | — | Stateless session management |
| Spring Data JPA / Hibernate | — | ORM & database access |
| MySQL | 8.0+ | Relational database |
| Maven | 3.9+ | Build & dependency management |
| Lombok | — | Boilerplate code reduction |

### Frontend

| Technology | Purpose |
|------------|---------|
| HTML5 | Markup & structure |
| CSS3 | Styling & layout |
| JavaScript (ES6+) | Client-side interactivity |
| Bootstrap 5 | Responsive UI components |
| Chart.js | Interactive charts & graphs |

### DevOps & Tooling

| Technology | Purpose |
|------------|---------|
| Docker | Containerization |
| Docker Compose | Multi-container orchestration |
| GitHub Actions | CI/CD pipeline |
| SLF4J / Logback | Logging framework |

---

## 📁 Project Structure

```
Smart_Expense_Tracker_System/
├── src/
│   ├── main/
│   │   ├── java/com/expense/tracker/
│   │   │   ├── config/             # Security, CORS, Swagger configs
│   │   │   ├── controller/         # REST API controllers
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   ├── request/        # Request DTOs
│   │   │   │   └── response/       # Response DTOs
│   │   │   ├── entity/             # JPA entity classes
│   │   │   ├── enums/              # Enumerations
│   │   │   ├── exception/          # Custom exceptions & global handler
│   │   │   ├── repository/         # Spring Data JPA repositories
│   │   │   ├── security/           # JWT filter, provider, entry point
│   │   │   ├── service/            # Business logic services
│   │   │   │   └── impl/           # Service implementations
│   │   │   └── util/               # Utility classes
│   │   └── resources/
│   │       ├── application.yml             # Default configuration
│   │       ├── application-dev.yml         # Dev profile
│   │       ├── application-prod.yml        # Production profile
│   │       └── templates/                  # Email templates
│   └── test/
│       └── java/com/expense/tracker/       # Unit & integration tests
├── frontend/
│   ├── index.html
│   ├── css/
│   ├── js/
│   └── assets/
├── docs/                           # Documentation files
├── postman/                        # Postman collection
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK | 21 or higher |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Docker *(optional)* | 24+ |

### 1. Clone the Repository

```bash
git clone https://github.com/Buvaneshwari26/Smart_Expense_Tracker_System.git
cd Smart_Expense_Tracker_System
```

### 2. Configure MySQL

```sql
CREATE DATABASE smart_expense_tracker;
CREATE USER 'expense_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON smart_expense_tracker.* TO 'expense_user'@'localhost';
FLUSH PRIVILEGES;
```

Update `src/main/resources/application.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_expense_tracker
    username: expense_user
    password: your_password
```

### 3. Run the Backend

```bash
mvn clean install
mvn spring-boot:run
```

The server starts at **http://localhost:8080**.

### 4. Open the Frontend

Open `frontend/index.html` in your browser, or serve it with any static file server:

```bash
# Using Python
python -m http.server 5500 --directory frontend

# Using Node.js (http-server)
npx http-server frontend -p 5500
```

Navigate to **http://localhost:5500**.

---

## 📖 API Documentation

Interactive API documentation is available via **Swagger UI** once the backend is running:

| Resource | URL |
|----------|-----|
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

For a complete offline reference, see the [API Documentation](docs/API_Documentation.md).

---

## ⚙️ Environment Profiles

| Profile | Activation | Database | Logging | Use Case |
|---------|------------|----------|---------|----------|
| `local` | Default | H2 (in-memory) | DEBUG | Local development & quick testing |
| `dev` | `--spring.profiles.active=dev` | MySQL (localhost) | DEBUG | Development with persistent data |
| `prod` | `--spring.profiles.active=prod` | MySQL (remote) | WARN | Production deployment |

```bash
# Run with a specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🐳 Docker

### Using Docker Compose

```bash
docker-compose up --build
```

This starts both the application and a MySQL instance.

### Docker Compose Configuration

```yaml
# docker-compose.yml (excerpt)
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/smart_expense_tracker
      SPRING_DATASOURCE_USERNAME: expense_user
      SPRING_DATASOURCE_PASSWORD: expense_pass

  db:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_DATABASE: smart_expense_tracker
      MYSQL_USER: expense_user
      MYSQL_PASSWORD: expense_pass
      MYSQL_ROOT_PASSWORD: rootpass
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** your changes: `git commit -m "Add amazing feature"`
4. **Push** to the branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

Please ensure your code:
- Follows existing code style and conventions
- Includes appropriate unit/integration tests
- Updates documentation as needed
- Passes all CI checks

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👩‍💻 Author

**Buvaneshwari**

- GitHub: [@Buvaneshwari26](https://github.com/Buvaneshwari26)

---

<div align="center">

⭐ **If you found this project helpful, please give it a star!** ⭐

</div>

# 🏗️ Architecture Diagram — Smart Expense Tracker System

This document presents the High-Level Design (HLD), Low-Level Design (LLD), security architecture, and request lifecycle of the Smart Expense Tracker System.

---

## Table of Contents

- [High-Level Design (HLD)](#high-level-design-hld)
- [Low-Level Design (LLD)](#low-level-design-lld)
- [Security Flow](#security-flow)
- [Request Lifecycle](#request-lifecycle)

---

## High-Level Design (HLD)

### System Architecture Overview

```mermaid
graph TB
    subgraph Client["🖥️ Client Layer"]
        WEB["Web Browser<br/>HTML5 / CSS3 / JS"]
        POSTMAN["Postman / API Client"]
    end

    subgraph Gateway["🔒 API Gateway / Security"]
        CORS["CORS Filter"]
        JWT_FILTER["JWT Authentication Filter"]
        SEC["Spring Security"]
    end

    subgraph Controllers["📡 Controller Layer"]
        AUTH_CTRL["AuthController"]
        CAT_CTRL["CategoryController"]
        INC_CTRL["IncomeController"]
        EXP_CTRL["ExpenseController"]
        BUD_CTRL["BudgetController"]
        GOAL_CTRL["GoalController"]
        DASH_CTRL["DashboardController"]
        RPT_CTRL["ReportController"]
        NOTIF_CTRL["NotificationController"]
    end

    subgraph Services["⚙️ Service Layer"]
        AUTH_SVC["AuthService"]
        CAT_SVC["CategoryService"]
        INC_SVC["IncomeService"]
        EXP_SVC["ExpenseService"]
        BUD_SVC["BudgetService"]
        GOAL_SVC["GoalService"]
        DASH_SVC["DashboardService"]
        RPT_SVC["ReportService"]
        NOTIF_SVC["NotificationService"]
        EXPORT_SVC["ExportService"]
        EMAIL_SVC["EmailService"]
    end

    subgraph Repositories["📦 Repository Layer"]
        USER_REPO["UserRepository"]
        CAT_REPO["CategoryRepository"]
        INC_REPO["IncomeRepository"]
        EXP_REPO["ExpenseRepository"]
        BUD_REPO["BudgetRepository"]
        GOAL_REPO["GoalRepository"]
        NOTIF_REPO["NotificationRepository"]
        TOKEN_REPO["RefreshTokenRepository"]
    end

    subgraph Database["🗄️ Database Layer"]
        MYSQL[("MySQL 8.0<br/>smart_expense_tracker")]
    end

    subgraph External["🌐 External Services"]
        SMTP["SMTP Server<br/>Email Notifications"]
    end

    WEB -->|HTTP/REST| CORS
    POSTMAN -->|HTTP/REST| CORS
    CORS --> JWT_FILTER
    JWT_FILTER --> SEC
    SEC --> Controllers

    AUTH_CTRL --> AUTH_SVC
    CAT_CTRL --> CAT_SVC
    INC_CTRL --> INC_SVC
    EXP_CTRL --> EXP_SVC
    BUD_CTRL --> BUD_SVC
    GOAL_CTRL --> GOAL_SVC
    DASH_CTRL --> DASH_SVC
    RPT_CTRL --> RPT_SVC
    NOTIF_CTRL --> NOTIF_SVC

    INC_SVC --> EXPORT_SVC
    EXP_SVC --> EXPORT_SVC
    BUD_SVC --> NOTIF_SVC
    GOAL_SVC --> NOTIF_SVC
    NOTIF_SVC --> EMAIL_SVC

    AUTH_SVC --> USER_REPO
    AUTH_SVC --> TOKEN_REPO
    CAT_SVC --> CAT_REPO
    INC_SVC --> INC_REPO
    EXP_SVC --> EXP_REPO
    BUD_SVC --> BUD_REPO
    GOAL_SVC --> GOAL_REPO
    NOTIF_SVC --> NOTIF_REPO
    DASH_SVC --> INC_REPO
    DASH_SVC --> EXP_REPO
    RPT_SVC --> INC_REPO
    RPT_SVC --> EXP_REPO

    Repositories -->|JPA / Hibernate| MYSQL
    EMAIL_SVC -->|SMTP| SMTP

    style Client fill:#e3f2fd,stroke:#1565c0
    style Gateway fill:#fff3e0,stroke:#e65100
    style Controllers fill:#e8f5e9,stroke:#2e7d32
    style Services fill:#f3e5f5,stroke:#6a1b9a
    style Repositories fill:#fce4ec,stroke:#b71c1c
    style Database fill:#fff9c4,stroke:#f57f17
    style External fill:#e0f7fa,stroke:#00695c
```

### Layered Architecture Summary

```mermaid
graph LR
    A["Client<br/>(Browser / Postman)"] --> B["Security<br/>(JWT + CORS)"]
    B --> C["Controllers<br/>(REST Endpoints)"]
    C --> D["Services<br/>(Business Logic)"]
    D --> E["Repositories<br/>(Data Access)"]
    E --> F["MySQL<br/>(Database)"]

    style A fill:#bbdefb
    style B fill:#ffe0b2
    style C fill:#c8e6c9
    style D fill:#e1bee7
    style E fill:#f8bbd0
    style F fill:#fff9c4
```

---

## Low-Level Design (LLD)

### Controller Layer — Class Diagram

```mermaid
classDiagram
    class AuthController {
        -AuthService authService
        +register(RegisterRequest) ResponseEntity
        +login(LoginRequest) ResponseEntity
        +refreshToken(RefreshTokenRequest) ResponseEntity
    }

    class CategoryController {
        -CategoryService categoryService
        +getAllCategories() ResponseEntity
        +getCategoryById(Long id) ResponseEntity
        +createCategory(CategoryRequest) ResponseEntity
        +updateCategory(Long id, CategoryRequest) ResponseEntity
        +deleteCategory(Long id) ResponseEntity
    }

    class IncomeController {
        -IncomeService incomeService
        +getAllIncomes(Pageable) ResponseEntity
        +getIncomeById(Long id) ResponseEntity
        +createIncome(IncomeRequest) ResponseEntity
        +updateIncome(Long id, IncomeRequest) ResponseEntity
        +deleteIncome(Long id) ResponseEntity
        +exportPdf() ResponseEntity
        +exportExcel() ResponseEntity
        +exportCsv() ResponseEntity
    }

    class ExpenseController {
        -ExpenseService expenseService
        +getAllExpenses(Pageable) ResponseEntity
        +getExpenseById(Long id) ResponseEntity
        +createExpense(ExpenseRequest) ResponseEntity
        +updateExpense(Long id, ExpenseRequest) ResponseEntity
        +deleteExpense(Long id) ResponseEntity
        +searchExpenses(String keyword, Pageable) ResponseEntity
        +filterExpenses(FilterRequest) ResponseEntity
        +getByCategory(Long categoryId) ResponseEntity
        +exportPdf() ResponseEntity
        +exportExcel() ResponseEntity
        +exportCsv() ResponseEntity
    }

    class BudgetController {
        -BudgetService budgetService
        +getAllBudgets() ResponseEntity
        +getBudgetById(Long id) ResponseEntity
        +createBudget(BudgetRequest) ResponseEntity
        +updateBudget(Long id, BudgetRequest) ResponseEntity
        +deleteBudget(Long id) ResponseEntity
    }

    class GoalController {
        -GoalService goalService
        +getAllGoals() ResponseEntity
        +getGoalById(Long id) ResponseEntity
        +createGoal(GoalRequest) ResponseEntity
        +updateGoal(Long id, GoalRequest) ResponseEntity
        +deleteGoal(Long id) ResponseEntity
        +addSavings(Long id, SavingsRequest) ResponseEntity
    }

    class DashboardController {
        -DashboardService dashboardService
        +getDashboard() ResponseEntity
    }

    class ReportController {
        -ReportService reportService
        +getMonthlyReport(int month, int year) ResponseEntity
        +getYearlyReport(int year) ResponseEntity
        +getCategoryReport(LocalDate start, LocalDate end) ResponseEntity
        +getIncomeVsExpense(LocalDate start, LocalDate end, String granularity) ResponseEntity
    }

    class NotificationController {
        -NotificationService notificationService
        +getAllNotifications(Pageable) ResponseEntity
        +getUnreadCount() ResponseEntity
        +markAsRead(Long id) ResponseEntity
    }
```

### Service Layer — Class Diagram

```mermaid
classDiagram
    class AuthService {
        <<interface>>
        +register(RegisterRequest) UserResponse
        +login(LoginRequest) AuthResponse
        +refreshToken(RefreshTokenRequest) AuthResponse
    }

    class AuthServiceImpl {
        -UserRepository userRepository
        -RefreshTokenRepository tokenRepository
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        +register(RegisterRequest) UserResponse
        +login(LoginRequest) AuthResponse
        +refreshToken(RefreshTokenRequest) AuthResponse
    }

    class CategoryService {
        <<interface>>
        +getAll() List~CategoryResponse~
        +getById(Long) CategoryResponse
        +create(CategoryRequest) CategoryResponse
        +update(Long, CategoryRequest) CategoryResponse
        +delete(Long) void
    }

    class IncomeService {
        <<interface>>
        +getAll(Pageable) Page~IncomeResponse~
        +getById(Long) IncomeResponse
        +create(IncomeRequest) IncomeResponse
        +update(Long, IncomeRequest) IncomeResponse
        +delete(Long) void
        +exportPdf() byte[]
        +exportExcel() byte[]
        +exportCsv() byte[]
    }

    class ExpenseService {
        <<interface>>
        +getAll(Pageable) Page~ExpenseResponse~
        +getById(Long) ExpenseResponse
        +create(ExpenseRequest) ExpenseResponse
        +update(Long, ExpenseRequest) ExpenseResponse
        +delete(Long) void
        +search(String, Pageable) Page~ExpenseResponse~
        +filter(FilterRequest) Page~ExpenseResponse~
        +getByCategory(Long) List~ExpenseResponse~
        +exportPdf() byte[]
        +exportExcel() byte[]
        +exportCsv() byte[]
    }

    class BudgetService {
        <<interface>>
        +getAll() List~BudgetResponse~
        +getById(Long) BudgetResponse
        +create(BudgetRequest) BudgetResponse
        +update(Long, BudgetRequest) BudgetResponse
        +delete(Long) void
    }

    class GoalService {
        <<interface>>
        +getAll() List~GoalResponse~
        +getById(Long) GoalResponse
        +create(GoalRequest) GoalResponse
        +update(Long, GoalRequest) GoalResponse
        +delete(Long) void
        +addSavings(Long, SavingsRequest) GoalResponse
    }

    class DashboardService {
        <<interface>>
        +getDashboardData() DashboardResponse
    }

    class ReportService {
        <<interface>>
        +getMonthlyReport(int, int) MonthlyReport
        +getYearlyReport(int) YearlyReport
        +getCategoryReport(LocalDate, LocalDate) CategoryReport
        +getIncomeVsExpense(LocalDate, LocalDate, String) ComparisonReport
    }

    class NotificationService {
        <<interface>>
        +getAll(Pageable) Page~NotificationResponse~
        +getUnreadCount() Long
        +markAsRead(Long) NotificationResponse
        +createNotification(NotificationRequest) void
    }

    class ExportService {
        -generatePdf(List, String) byte[]
        -generateExcel(List, String) byte[]
        -generateCsv(List, String) byte[]
    }

    class EmailService {
        -JavaMailSender mailSender
        +sendEmail(String to, String subject, String body) void
        +sendBudgetAlert(User, Budget) void
        +sendGoalMilestone(User, SavingsGoal) void
    }

    AuthService <|.. AuthServiceImpl
    AuthServiceImpl --> ExportService : uses
    IncomeService --> ExportService : uses
    ExpenseService --> ExportService : uses
    BudgetService --> NotificationService : triggers
    GoalService --> NotificationService : triggers
    NotificationService --> EmailService : sends
```

### Entity Layer — Class Diagram

```mermaid
classDiagram
    class User {
        -Long id
        -String fullName
        -String email
        -String password
        -Role role
        -Boolean enabled
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -List~Category~ categories
        -List~Income~ incomes
        -List~Expense~ expenses
        -List~Budget~ budgets
        -List~SavingsGoal~ goals
        -List~Notification~ notifications
    }

    class Category {
        -Long id
        -String name
        -CategoryType type
        -String icon
        -User user
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class Income {
        -Long id
        -BigDecimal amount
        -String description
        -LocalDate date
        -Category category
        -User user
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class Expense {
        -Long id
        -BigDecimal amount
        -String description
        -LocalDate date
        -Category category
        -User user
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class Budget {
        -Long id
        -BigDecimal amount
        -Integer month
        -Integer year
        -Category category
        -User user
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class SavingsGoal {
        -Long id
        -String name
        -String description
        -BigDecimal targetAmount
        -BigDecimal currentAmount
        -LocalDate targetDate
        -GoalStatus status
        -User user
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class Notification {
        -Long id
        -NotificationType type
        -String title
        -String message
        -Boolean isRead
        -User user
        -LocalDateTime createdAt
    }

    class RefreshToken {
        -Long id
        -String token
        -LocalDateTime expiryDate
        -User user
        -LocalDateTime createdAt
    }

    class Role {
        <<enumeration>>
        USER
        ADMIN
    }

    class CategoryType {
        <<enumeration>>
        INCOME
        EXPENSE
    }

    class GoalStatus {
        <<enumeration>>
        IN_PROGRESS
        COMPLETED
        CANCELLED
    }

    class NotificationType {
        <<enumeration>>
        BUDGET_WARNING
        BUDGET_EXCEEDED
        GOAL_MILESTONE
        GOAL_COMPLETED
        SYSTEM
    }

    User "1" --> "*" Category
    User "1" --> "*" Income
    User "1" --> "*" Expense
    User "1" --> "*" Budget
    User "1" --> "*" SavingsGoal
    User "1" --> "*" Notification
    User "1" --> "1" RefreshToken
    Category "1" --> "*" Income
    Category "1" --> "*" Expense
    Category "1" --> "*" Budget
```

### Repository Layer — Class Diagram

```mermaid
classDiagram
    class JpaRepository~T, ID~ {
        <<interface>>
        +save(T entity) T
        +findById(ID id) Optional~T~
        +findAll() List~T~
        +deleteById(ID id) void
    }

    class UserRepository {
        <<interface>>
        +findByEmail(String email) Optional~User~
        +existsByEmail(String email) Boolean
    }

    class CategoryRepository {
        <<interface>>
        +findByUserId(Long userId) List~Category~
        +findByUserIdAndType(Long userId, CategoryType type) List~Category~
    }

    class IncomeRepository {
        <<interface>>
        +findByUserId(Long userId, Pageable pageable) Page~Income~
        +findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end) List~Income~
        +sumAmountByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end) BigDecimal
    }

    class ExpenseRepository {
        <<interface>>
        +findByUserId(Long userId, Pageable pageable) Page~Expense~
        +findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end) List~Expense~
        +findByUserIdAndDescriptionContaining(Long userId, String keyword, Pageable pageable) Page~Expense~
        +findByUserIdAndCategoryId(Long userId, Long categoryId) List~Expense~
        +sumAmountByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end) BigDecimal
        +sumAmountByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year) BigDecimal
    }

    class BudgetRepository {
        <<interface>>
        +findByUserId(Long userId) List~Budget~
        +findByUserIdAndMonthAndYear(Long userId, int month, int year) List~Budget~
        +findByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year) Optional~Budget~
    }

    class GoalRepository {
        <<interface>>
        +findByUserId(Long userId) List~SavingsGoal~
        +findByUserIdAndStatus(Long userId, GoalStatus status) List~SavingsGoal~
    }

    class NotificationRepository {
        <<interface>>
        +findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) Page~Notification~
        +countByUserIdAndIsReadFalse(Long userId) Long
    }

    class RefreshTokenRepository {
        <<interface>>
        +findByToken(String token) Optional~RefreshToken~
        +findByUserId(Long userId) Optional~RefreshToken~
        +deleteByUserId(Long userId) void
    }

    JpaRepository <|-- UserRepository
    JpaRepository <|-- CategoryRepository
    JpaRepository <|-- IncomeRepository
    JpaRepository <|-- ExpenseRepository
    JpaRepository <|-- BudgetRepository
    JpaRepository <|-- GoalRepository
    JpaRepository <|-- NotificationRepository
    JpaRepository <|-- RefreshTokenRepository
```

---

## Security Flow

### JWT Authentication Flow

```mermaid
sequenceDiagram
    actor User
    participant Client as Browser / Client
    participant Filter as JwtAuthenticationFilter
    participant Provider as JwtTokenProvider
    participant Security as SecurityContext
    participant Controller as REST Controller
    participant Service as Service Layer
    participant DB as MySQL

    Note over User, DB: === Registration Flow ===
    User->>Client: Fill registration form
    Client->>Controller: POST /api/auth/register
    Controller->>Service: authService.register()
    Service->>DB: Save user (BCrypt password)
    DB-->>Service: User saved
    Service-->>Controller: UserResponse
    Controller-->>Client: 201 Created

    Note over User, DB: === Login Flow ===
    User->>Client: Enter credentials
    Client->>Controller: POST /api/auth/login
    Controller->>Service: authService.login()
    Service->>DB: Find user by email
    DB-->>Service: User entity
    Service->>Service: Verify password (BCrypt)
    Service->>Provider: Generate JWT tokens
    Provider-->>Service: Access + Refresh tokens
    Service->>DB: Save refresh token
    Service-->>Controller: AuthResponse
    Controller-->>Client: 200 OK (tokens)
    Client->>Client: Store tokens locally

    Note over User, DB: === Authenticated Request ===
    User->>Client: Perform action
    Client->>Filter: Request + Authorization header
    Filter->>Filter: Extract JWT from header
    Filter->>Provider: Validate token
    Provider-->>Filter: Token valid + claims
    Filter->>DB: Load UserDetails
    DB-->>Filter: User entity
    Filter->>Security: Set authentication
    Security->>Controller: Authenticated request
    Controller->>Service: Business logic
    Service->>DB: Data operations
    DB-->>Service: Results
    Service-->>Controller: Response DTO
    Controller-->>Client: 200 OK

    Note over User, DB: === Token Refresh ===
    Client->>Controller: POST /api/auth/refresh-token
    Controller->>Service: authService.refreshToken()
    Service->>DB: Find refresh token
    DB-->>Service: RefreshToken entity
    Service->>Service: Validate expiry
    Service->>Provider: Generate new access token
    Provider-->>Service: New tokens
    Service-->>Controller: AuthResponse
    Controller-->>Client: 200 OK (new tokens)
```

### Security Configuration Overview

```mermaid
graph TD
    REQ["Incoming HTTP Request"] --> CORS_FILTER["CORS Filter"]
    CORS_FILTER --> JWT_FILTER["JWT Authentication Filter"]

    JWT_FILTER -->|Token Present| VALIDATE["Validate JWT Token"]
    JWT_FILTER -->|No Token| PUBLIC_CHECK{"Public Endpoint?"}

    VALIDATE -->|Valid| SET_AUTH["Set SecurityContext"]
    VALIDATE -->|Invalid| REJECT_401["401 Unauthorized"]

    PUBLIC_CHECK -->|Yes| ALLOW["Allow Access"]
    PUBLIC_CHECK -->|No| REJECT_401

    SET_AUTH --> AUTHORIZE{"Role Check"}
    AUTHORIZE -->|Authorized| CONTROLLER["Controller"]
    AUTHORIZE -->|Forbidden| REJECT_403["403 Forbidden"]

    ALLOW --> CONTROLLER

    subgraph "Public Endpoints (No Auth Required)"
        PUB1["POST /api/auth/register"]
        PUB2["POST /api/auth/login"]
        PUB3["POST /api/auth/refresh-token"]
        PUB4["GET /swagger-ui/**"]
        PUB5["GET /v3/api-docs/**"]
    end

    style REJECT_401 fill:#ffcdd2,stroke:#b71c1c
    style REJECT_403 fill:#ffcdd2,stroke:#b71c1c
    style ALLOW fill:#c8e6c9,stroke:#2e7d32
    style CONTROLLER fill:#c8e6c9,stroke:#2e7d32
```

---

## Request Lifecycle

### Complete Request Processing Pipeline

```mermaid
graph TD
    A["1. Client sends HTTP Request"] --> B["2. CORS Filter<br/>Validate origin & headers"]
    B --> C["3. JWT Authentication Filter<br/>Extract & validate token"]
    C --> D["4. Spring Security<br/>Authorization check"]
    D --> E["5. DispatcherServlet<br/>Route to controller"]
    E --> F["6. Controller<br/>Parse request, validate input"]
    F --> G["7. Service Layer<br/>Execute business logic"]
    G --> H["8. Repository Layer<br/>JPA/Hibernate queries"]
    H --> I["9. MySQL Database<br/>Execute SQL"]
    I --> J["10. Entity → DTO Mapping<br/>Build response object"]
    J --> K["11. Controller<br/>Wrap in ResponseEntity"]
    K --> L["12. Global Exception Handler<br/>(if error occurred)"]
    L --> M["13. HTTP Response<br/>JSON body + status code"]
    M --> N["14. Client receives response"]

    style A fill:#e3f2fd
    style N fill:#e3f2fd
    style L fill:#fff3e0,stroke:#e65100
    style I fill:#fff9c4,stroke:#f57f17

    G -->|"Budget exceeded?"| G1["Trigger Notification"]
    G1 --> G2["Send Email Alert"]
    G -->|"Goal milestone?"| G3["Trigger Milestone Notification"]
    G3 --> G2
```

### Exception Handling Flow

```mermaid
graph LR
    EX["Exception Thrown"] --> GEH["GlobalExceptionHandler<br/>(@ControllerAdvice)"]

    GEH -->|ResourceNotFoundException| R404["404 Not Found"]
    GEH -->|DuplicateResourceException| R409["409 Conflict"]
    GEH -->|ValidationException| R422["422 Unprocessable Entity"]
    GEH -->|AccessDeniedException| R403["403 Forbidden"]
    GEH -->|AuthenticationException| R401["401 Unauthorized"]
    GEH -->|Exception| R500["500 Internal Server Error"]

    R404 --> RESP["ErrorResponse JSON"]
    R409 --> RESP
    R422 --> RESP
    R403 --> RESP
    R401 --> RESP
    R500 --> RESP

    style R404 fill:#fff3e0
    style R409 fill:#fff3e0
    style R422 fill:#fff3e0
    style R403 fill:#ffcdd2
    style R401 fill:#ffcdd2
    style R500 fill:#ffcdd2
```

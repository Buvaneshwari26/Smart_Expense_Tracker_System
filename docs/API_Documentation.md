# 📖 API Documentation — Smart Expense Tracker System

**Base URL:** `http://localhost:8080/api`

**Authentication:** All endpoints (except Auth) require a Bearer token in the `Authorization` header.

```
Authorization: Bearer <access_token>
```

---

## Table of Contents

- [Authentication](#1-authentication)
- [Categories](#2-categories)
- [Incomes](#3-incomes)
- [Expenses](#4-expenses)
- [Budgets](#5-budgets)
- [Savings Goals](#6-savings-goals)
- [Dashboard](#7-dashboard)
- [Reports](#8-reports)
- [Notifications](#9-notifications)
- [Error Responses](#error-responses)

---

## 1. Authentication

### POST `/api/auth/register`

Register a new user account.

**Request Body:**

```json
{
  "fullName": "Buvaneshwari",
  "email": "buvaneshwari@example.com",
  "password": "SecureP@ss123",
  "confirmPassword": "SecureP@ss123"
}
```

**Response `201 Created`:**

```json
{
  "id": 1,
  "fullName": "Buvaneshwari",
  "email": "buvaneshwari@example.com",
  "message": "User registered successfully"
}
```

**Error `400 Bad Request`:**

```json
{
  "timestamp": "2026-06-19T15:21:08",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists"
}
```

---

### POST `/api/auth/login`

Authenticate and receive access + refresh tokens.

**Request Body:**

```json
{
  "email": "buvaneshwari@example.com",
  "password": "SecureP@ss123"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "fullName": "Buvaneshwari",
    "email": "buvaneshwari@example.com"
  }
}
```

---

### POST `/api/auth/refresh-token`

Refresh an expired access token using a valid refresh token.

**Request Body:**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newtoken...",
  "refreshToken": "bmV3IHJlZnJlc2ggdG9rZW4...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## 2. Categories

### GET `/api/categories`

Retrieve all categories for the authenticated user.

**Response `200 OK`:**

```json
[
  {
    "id": 1,
    "name": "Salary",
    "type": "INCOME",
    "icon": "💼",
    "createdAt": "2026-01-15T10:30:00"
  },
  {
    "id": 2,
    "name": "Food & Dining",
    "type": "EXPENSE",
    "icon": "🍕",
    "createdAt": "2026-01-15T10:30:00"
  }
]
```

---

### GET `/api/categories/{id}`

Retrieve a single category by ID.

**Response `200 OK`:**

```json
{
  "id": 1,
  "name": "Salary",
  "type": "INCOME",
  "icon": "💼",
  "createdAt": "2026-01-15T10:30:00"
}
```

---

### POST `/api/categories`

Create a new category.

**Request Body:**

```json
{
  "name": "Groceries",
  "type": "EXPENSE",
  "icon": "🛒"
}
```

**Response `201 Created`:**

```json
{
  "id": 3,
  "name": "Groceries",
  "type": "EXPENSE",
  "icon": "🛒",
  "createdAt": "2026-06-19T15:30:00"
}
```

---

### PUT `/api/categories/{id}`

Update an existing category.

**Request Body:**

```json
{
  "name": "Groceries & Essentials",
  "type": "EXPENSE",
  "icon": "🛒"
}
```

**Response `200 OK`:**

```json
{
  "id": 3,
  "name": "Groceries & Essentials",
  "type": "EXPENSE",
  "icon": "🛒",
  "createdAt": "2026-06-19T15:30:00"
}
```

---

### DELETE `/api/categories/{id}`

Delete a category. Fails if the category has associated transactions.

**Response `204 No Content`**

---

## 3. Incomes

### GET `/api/incomes`

Retrieve all incomes for the authenticated user.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 10) |
| `sortBy` | string | Sort field (default: `date`) |
| `sortDir` | string | Sort direction: `asc` or `desc` |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "amount": 75000.00,
      "description": "Monthly Salary - June 2026",
      "date": "2026-06-01",
      "category": {
        "id": 1,
        "name": "Salary",
        "type": "INCOME"
      },
      "createdAt": "2026-06-01T09:00:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET `/api/incomes/{id}`

Retrieve a single income by ID.

**Response `200 OK`:**

```json
{
  "id": 1,
  "amount": 75000.00,
  "description": "Monthly Salary - June 2026",
  "date": "2026-06-01",
  "category": {
    "id": 1,
    "name": "Salary",
    "type": "INCOME"
  },
  "createdAt": "2026-06-01T09:00:00"
}
```

---

### POST `/api/incomes`

Create a new income record.

**Request Body:**

```json
{
  "amount": 15000.00,
  "description": "Freelance web development project",
  "date": "2026-06-15",
  "categoryId": 2
}
```

**Response `201 Created`:**

```json
{
  "id": 2,
  "amount": 15000.00,
  "description": "Freelance web development project",
  "date": "2026-06-15",
  "category": {
    "id": 2,
    "name": "Freelance",
    "type": "INCOME"
  },
  "createdAt": "2026-06-15T14:20:00"
}
```

---

### PUT `/api/incomes/{id}`

Update an existing income record.

**Request Body:**

```json
{
  "amount": 18000.00,
  "description": "Freelance web development project (revised)",
  "date": "2026-06-15",
  "categoryId": 2
}
```

**Response `200 OK`:**

```json
{
  "id": 2,
  "amount": 18000.00,
  "description": "Freelance web development project (revised)",
  "date": "2026-06-15",
  "category": {
    "id": 2,
    "name": "Freelance",
    "type": "INCOME"
  },
  "createdAt": "2026-06-15T14:20:00"
}
```

---

### DELETE `/api/incomes/{id}`

Delete an income record.

**Response `204 No Content`**

---

### GET `/api/incomes/export/pdf`

Export all incomes as a PDF file.

**Response:** `application/pdf` binary file download.

---

### GET `/api/incomes/export/excel`

Export all incomes as an Excel file.

**Response:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` binary file download.

---

### GET `/api/incomes/export/csv`

Export all incomes as a CSV file.

**Response:** `text/csv` file download.

---

## 4. Expenses

### GET `/api/expenses`

Retrieve all expenses for the authenticated user.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 10) |
| `sortBy` | string | Sort field (default: `date`) |
| `sortDir` | string | Sort direction: `asc` or `desc` |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "amount": 1250.00,
      "description": "Dinner at Italian restaurant",
      "date": "2026-06-18",
      "category": {
        "id": 3,
        "name": "Food & Dining",
        "type": "EXPENSE"
      },
      "createdAt": "2026-06-18T21:00:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET `/api/expenses/{id}`

Retrieve a single expense by ID.

**Response `200 OK`:**

```json
{
  "id": 1,
  "amount": 1250.00,
  "description": "Dinner at Italian restaurant",
  "date": "2026-06-18",
  "category": {
    "id": 3,
    "name": "Food & Dining",
    "type": "EXPENSE"
  },
  "createdAt": "2026-06-18T21:00:00"
}
```

---

### POST `/api/expenses`

Create a new expense record.

**Request Body:**

```json
{
  "amount": 500.00,
  "description": "Uber ride to office",
  "date": "2026-06-19",
  "categoryId": 4
}
```

**Response `201 Created`:**

```json
{
  "id": 2,
  "amount": 500.00,
  "description": "Uber ride to office",
  "date": "2026-06-19",
  "category": {
    "id": 4,
    "name": "Transportation",
    "type": "EXPENSE"
  },
  "createdAt": "2026-06-19T08:30:00"
}
```

---

### PUT `/api/expenses/{id}`

Update an existing expense record.

**Request Body:**

```json
{
  "amount": 550.00,
  "description": "Uber ride to office (toll included)",
  "date": "2026-06-19",
  "categoryId": 4
}
```

**Response `200 OK`:**

```json
{
  "id": 2,
  "amount": 550.00,
  "description": "Uber ride to office (toll included)",
  "date": "2026-06-19",
  "category": {
    "id": 4,
    "name": "Transportation",
    "type": "EXPENSE"
  },
  "createdAt": "2026-06-19T08:30:00"
}
```

---

### DELETE `/api/expenses/{id}`

Delete an expense record.

**Response `204 No Content`**

---

### GET `/api/expenses/search`

Search expenses by keyword.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `keyword` | string | Search term (matches description) |
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 10) |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "amount": 1250.00,
      "description": "Dinner at Italian restaurant",
      "date": "2026-06-18",
      "category": {
        "id": 3,
        "name": "Food & Dining",
        "type": "EXPENSE"
      }
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET `/api/expenses/filter`

Filter expenses by date range and/or category.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `startDate` | string | Start date (`YYYY-MM-DD`) |
| `endDate` | string | End date (`YYYY-MM-DD`) |
| `categoryId` | long | Category ID (optional) |
| `minAmount` | double | Minimum amount (optional) |
| `maxAmount` | double | Maximum amount (optional) |
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 10) |

**Example:** `GET /api/expenses/filter?startDate=2026-06-01&endDate=2026-06-30&categoryId=3`

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "amount": 1250.00,
      "description": "Dinner at Italian restaurant",
      "date": "2026-06-18",
      "category": {
        "id": 3,
        "name": "Food & Dining",
        "type": "EXPENSE"
      }
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET `/api/expenses/category/{categoryId}`

Get all expenses for a specific category.

**Response `200 OK`:**

```json
[
  {
    "id": 1,
    "amount": 1250.00,
    "description": "Dinner at Italian restaurant",
    "date": "2026-06-18",
    "category": {
      "id": 3,
      "name": "Food & Dining",
      "type": "EXPENSE"
    }
  },
  {
    "id": 5,
    "amount": 800.00,
    "description": "Lunch with colleagues",
    "date": "2026-06-17",
    "category": {
      "id": 3,
      "name": "Food & Dining",
      "type": "EXPENSE"
    }
  }
]
```

---

### GET `/api/expenses/export/pdf`

Export expenses as a PDF file.

**Query Parameters (optional):**

| Parameter | Type | Description |
|-----------|------|-------------|
| `startDate` | string | Start date filter |
| `endDate` | string | End date filter |
| `categoryId` | long | Category filter |

**Response:** `application/pdf` binary file download.

---

### GET `/api/expenses/export/excel`

Export expenses as an Excel spreadsheet.

**Response:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` binary file download.

---

### GET `/api/expenses/export/csv`

Export expenses as a CSV file.

**Response:** `text/csv` file download.

---

## 5. Budgets

### GET `/api/budgets`

Retrieve all budgets for the authenticated user.

**Response `200 OK`:**

```json
[
  {
    "id": 1,
    "category": {
      "id": 3,
      "name": "Food & Dining",
      "type": "EXPENSE"
    },
    "amount": 10000.00,
    "spentAmount": 2050.00,
    "remainingAmount": 7950.00,
    "percentageUsed": 20.5,
    "month": 6,
    "year": 2026,
    "status": "ON_TRACK",
    "createdAt": "2026-06-01T00:00:00"
  }
]
```

---

### GET `/api/budgets/{id}`

Retrieve a single budget by ID.

**Response `200 OK`:**

```json
{
  "id": 1,
  "category": {
    "id": 3,
    "name": "Food & Dining",
    "type": "EXPENSE"
  },
  "amount": 10000.00,
  "spentAmount": 2050.00,
  "remainingAmount": 7950.00,
  "percentageUsed": 20.5,
  "month": 6,
  "year": 2026,
  "status": "ON_TRACK",
  "createdAt": "2026-06-01T00:00:00"
}
```

---

### POST `/api/budgets`

Create a new budget.

**Request Body:**

```json
{
  "categoryId": 4,
  "amount": 5000.00,
  "month": 6,
  "year": 2026
}
```

**Response `201 Created`:**

```json
{
  "id": 2,
  "category": {
    "id": 4,
    "name": "Transportation",
    "type": "EXPENSE"
  },
  "amount": 5000.00,
  "spentAmount": 0.00,
  "remainingAmount": 5000.00,
  "percentageUsed": 0.0,
  "month": 6,
  "year": 2026,
  "status": "ON_TRACK",
  "createdAt": "2026-06-19T10:00:00"
}
```

---

### PUT `/api/budgets/{id}`

Update an existing budget.

**Request Body:**

```json
{
  "categoryId": 4,
  "amount": 6000.00,
  "month": 6,
  "year": 2026
}
```

**Response `200 OK`:**

```json
{
  "id": 2,
  "category": {
    "id": 4,
    "name": "Transportation",
    "type": "EXPENSE"
  },
  "amount": 6000.00,
  "spentAmount": 550.00,
  "remainingAmount": 5450.00,
  "percentageUsed": 9.17,
  "month": 6,
  "year": 2026,
  "status": "ON_TRACK",
  "createdAt": "2026-06-19T10:00:00"
}
```

---

### DELETE `/api/budgets/{id}`

Delete a budget.

**Response `204 No Content`**

---

## 6. Savings Goals

### GET `/api/goals`

Retrieve all savings goals for the authenticated user.

**Response `200 OK`:**

```json
[
  {
    "id": 1,
    "name": "Emergency Fund",
    "description": "6 months of living expenses",
    "targetAmount": 300000.00,
    "currentAmount": 75000.00,
    "progressPercentage": 25.0,
    "targetDate": "2026-12-31",
    "status": "IN_PROGRESS",
    "createdAt": "2026-01-01T00:00:00"
  }
]
```

---

### GET `/api/goals/{id}`

Retrieve a single savings goal by ID.

**Response `200 OK`:**

```json
{
  "id": 1,
  "name": "Emergency Fund",
  "description": "6 months of living expenses",
  "targetAmount": 300000.00,
  "currentAmount": 75000.00,
  "progressPercentage": 25.0,
  "targetDate": "2026-12-31",
  "status": "IN_PROGRESS",
  "createdAt": "2026-01-01T00:00:00"
}
```

---

### POST `/api/goals`

Create a new savings goal.

**Request Body:**

```json
{
  "name": "Vacation Fund",
  "description": "Trip to Europe in summer 2027",
  "targetAmount": 150000.00,
  "targetDate": "2027-05-01"
}
```

**Response `201 Created`:**

```json
{
  "id": 2,
  "name": "Vacation Fund",
  "description": "Trip to Europe in summer 2027",
  "targetAmount": 150000.00,
  "currentAmount": 0.00,
  "progressPercentage": 0.0,
  "targetDate": "2027-05-01",
  "status": "IN_PROGRESS",
  "createdAt": "2026-06-19T12:00:00"
}
```

---

### PUT `/api/goals/{id}`

Update a savings goal.

**Request Body:**

```json
{
  "name": "Vacation Fund",
  "description": "Trip to Japan in spring 2027",
  "targetAmount": 200000.00,
  "targetDate": "2027-04-01"
}
```

**Response `200 OK`:**

```json
{
  "id": 2,
  "name": "Vacation Fund",
  "description": "Trip to Japan in spring 2027",
  "targetAmount": 200000.00,
  "currentAmount": 0.00,
  "progressPercentage": 0.0,
  "targetDate": "2027-04-01",
  "status": "IN_PROGRESS",
  "createdAt": "2026-06-19T12:00:00"
}
```

---

### DELETE `/api/goals/{id}`

Delete a savings goal.

**Response `204 No Content`**

---

### PATCH `/api/goals/{id}/add-savings`

Add savings to a goal.

**Request Body:**

```json
{
  "amount": 10000.00
}
```

**Response `200 OK`:**

```json
{
  "id": 2,
  "name": "Vacation Fund",
  "description": "Trip to Japan in spring 2027",
  "targetAmount": 200000.00,
  "currentAmount": 10000.00,
  "progressPercentage": 5.0,
  "targetDate": "2027-04-01",
  "status": "IN_PROGRESS",
  "createdAt": "2026-06-19T12:00:00"
}
```

---

## 7. Dashboard

### GET `/api/dashboard`

Retrieve dashboard KPIs and summary data.

**Response `200 OK`:**

```json
{
  "totalIncome": 90000.00,
  "totalExpense": 25800.00,
  "netSavings": 64200.00,
  "currentMonthIncome": 75000.00,
  "currentMonthExpense": 12500.00,
  "savingsRate": 71.33,
  "totalBudgets": 5,
  "budgetsOnTrack": 4,
  "budgetsExceeded": 1,
  "activeSavingsGoals": 2,
  "recentTransactions": [
    {
      "id": 2,
      "type": "EXPENSE",
      "amount": 500.00,
      "description": "Uber ride to office",
      "date": "2026-06-19",
      "categoryName": "Transportation"
    },
    {
      "id": 1,
      "type": "EXPENSE",
      "amount": 1250.00,
      "description": "Dinner at Italian restaurant",
      "date": "2026-06-18",
      "categoryName": "Food & Dining"
    }
  ],
  "expensesByCategory": [
    {
      "categoryName": "Food & Dining",
      "totalAmount": 8500.00,
      "percentage": 32.95
    },
    {
      "categoryName": "Transportation",
      "totalAmount": 5500.00,
      "percentage": 21.32
    },
    {
      "categoryName": "Utilities",
      "totalAmount": 3200.00,
      "percentage": 12.40
    }
  ],
  "monthlyTrend": [
    { "month": "Jan", "income": 75000.00, "expense": 22000.00 },
    { "month": "Feb", "income": 75000.00, "expense": 19500.00 },
    { "month": "Mar", "income": 78000.00, "expense": 24000.00 },
    { "month": "Apr", "income": 75000.00, "expense": 21000.00 },
    { "month": "May", "income": 82000.00, "expense": 26500.00 },
    { "month": "Jun", "income": 75000.00, "expense": 12500.00 }
  ]
}
```

---

## 8. Reports

### GET `/api/reports/monthly`

Get a monthly financial report.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `month` | int | Month (1–12) |
| `year` | int | Year (e.g., 2026) |

**Example:** `GET /api/reports/monthly?month=6&year=2026`

**Response `200 OK`:**

```json
{
  "month": 6,
  "year": 2026,
  "totalIncome": 75000.00,
  "totalExpense": 12500.00,
  "netSavings": 62500.00,
  "savingsRate": 83.33,
  "topExpenseCategories": [
    { "category": "Food & Dining", "amount": 5200.00 },
    { "category": "Transportation", "amount": 3800.00 },
    { "category": "Utilities", "amount": 2000.00 },
    { "category": "Entertainment", "amount": 1500.00 }
  ],
  "dailyExpenses": [
    { "date": "2026-06-01", "amount": 350.00 },
    { "date": "2026-06-02", "amount": 0.00 },
    { "date": "2026-06-03", "amount": 1200.00 }
  ]
}
```

---

### GET `/api/reports/yearly`

Get a yearly financial summary.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `year` | int | Year (e.g., 2026) |

**Example:** `GET /api/reports/yearly?year=2026`

**Response `200 OK`:**

```json
{
  "year": 2026,
  "totalIncome": 460000.00,
  "totalExpense": 125500.00,
  "netSavings": 334500.00,
  "savingsRate": 72.72,
  "monthlyBreakdown": [
    { "month": "January", "income": 75000.00, "expense": 22000.00, "savings": 53000.00 },
    { "month": "February", "income": 75000.00, "expense": 19500.00, "savings": 55500.00 },
    { "month": "March", "income": 78000.00, "expense": 24000.00, "savings": 54000.00 },
    { "month": "April", "income": 75000.00, "expense": 21000.00, "savings": 54000.00 },
    { "month": "May", "income": 82000.00, "expense": 26500.00, "savings": 55500.00 },
    { "month": "June", "income": 75000.00, "expense": 12500.00, "savings": 62500.00 }
  ]
}
```

---

### GET `/api/reports/category`

Get expense breakdown by category for a given period.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `startDate` | string | Start date (`YYYY-MM-DD`) |
| `endDate` | string | End date (`YYYY-MM-DD`) |

**Example:** `GET /api/reports/category?startDate=2026-06-01&endDate=2026-06-30`

**Response `200 OK`:**

```json
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "totalExpense": 12500.00,
  "categories": [
    {
      "categoryId": 3,
      "categoryName": "Food & Dining",
      "amount": 5200.00,
      "percentage": 41.60,
      "transactionCount": 15
    },
    {
      "categoryId": 4,
      "categoryName": "Transportation",
      "amount": 3800.00,
      "percentage": 30.40,
      "transactionCount": 22
    },
    {
      "categoryId": 5,
      "categoryName": "Utilities",
      "amount": 2000.00,
      "percentage": 16.00,
      "transactionCount": 3
    },
    {
      "categoryId": 6,
      "categoryName": "Entertainment",
      "amount": 1500.00,
      "percentage": 12.00,
      "transactionCount": 4
    }
  ]
}
```

---

### GET `/api/reports/income-vs-expense`

Compare income vs. expense over a time range.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `startDate` | string | Start date (`YYYY-MM-DD`) |
| `endDate` | string | End date (`YYYY-MM-DD`) |
| `granularity` | string | `DAILY`, `WEEKLY`, or `MONTHLY` |

**Example:** `GET /api/reports/income-vs-expense?startDate=2026-01-01&endDate=2026-06-30&granularity=MONTHLY`

**Response `200 OK`:**

```json
{
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "granularity": "MONTHLY",
  "data": [
    { "period": "2026-01", "income": 75000.00, "expense": 22000.00, "net": 53000.00 },
    { "period": "2026-02", "income": 75000.00, "expense": 19500.00, "net": 55500.00 },
    { "period": "2026-03", "income": 78000.00, "expense": 24000.00, "net": 54000.00 },
    { "period": "2026-04", "income": 75000.00, "expense": 21000.00, "net": 54000.00 },
    { "period": "2026-05", "income": 82000.00, "expense": 26500.00, "net": 55500.00 },
    { "period": "2026-06", "income": 75000.00, "expense": 12500.00, "net": 62500.00 }
  ],
  "summary": {
    "totalIncome": 460000.00,
    "totalExpense": 125500.00,
    "netSavings": 334500.00,
    "averageMonthlyIncome": 76666.67,
    "averageMonthlyExpense": 20916.67
  }
}
```

---

## 9. Notifications

### GET `/api/notifications`

Retrieve all notifications for the authenticated user.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 20) |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "type": "BUDGET_WARNING",
      "title": "Budget Alert: Food & Dining",
      "message": "You've used 80% of your Food & Dining budget for June 2026.",
      "read": false,
      "createdAt": "2026-06-18T22:00:00"
    },
    {
      "id": 2,
      "type": "GOAL_MILESTONE",
      "title": "Savings Milestone Reached!",
      "message": "You've reached 25% of your Emergency Fund goal.",
      "read": true,
      "createdAt": "2026-06-15T10:00:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

### GET `/api/notifications/unread-count`

Get the count of unread notifications.

**Response `200 OK`:**

```json
{
  "unreadCount": 3
}
```

---

### PATCH `/api/notifications/{id}/mark-read`

Mark a notification as read.

**Response `200 OK`:**

```json
{
  "id": 1,
  "type": "BUDGET_WARNING",
  "title": "Budget Alert: Food & Dining",
  "message": "You've used 80% of your Food & Dining budget for June 2026.",
  "read": true,
  "createdAt": "2026-06-18T22:00:00"
}
```

---

## Error Responses

All error responses follow a consistent format:

```json
{
  "timestamp": "2026-06-19T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Expense not found with id: 99",
  "path": "/api/expenses/99"
}
```

### Common HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `200` | OK | Request succeeded |
| `201` | Created | Resource created successfully |
| `204` | No Content | Resource deleted successfully |
| `400` | Bad Request | Invalid request body or parameters |
| `401` | Unauthorized | Missing or invalid authentication token |
| `403` | Forbidden | Insufficient permissions |
| `404` | Not Found | Resource not found |
| `409` | Conflict | Resource already exists (e.g., duplicate budget) |
| `422` | Unprocessable Entity | Validation errors |
| `500` | Internal Server Error | Unexpected server error |

### Validation Error Response

```json
{
  "timestamp": "2026-06-19T15:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "amount",
      "message": "Amount must be greater than zero"
    },
    {
      "field": "date",
      "message": "Date cannot be in the future"
    }
  ]
}
```

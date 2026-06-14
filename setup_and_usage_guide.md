# Smart Expense Tracker System - Setup & Usage Guide

This guide describes how to configure, run, and test your new Spring Boot backend project using **MySQL Workbench**, **IntelliJ IDEA**, and **Postman**.

---

## 1. MySQL Workbench Configuration

Before launching the Spring Boot application, you need to create an empty schema database. Hibernate will automatically generate all the necessary tables, columns, and foreign keys on startup.

### Steps:
1. Open **MySQL Workbench** and connect to your local MySQL server instance.
2. Open a new SQL Query tab and run the following command:
   ```sql
   CREATE DATABASE smart_expense_tracker;
   ```
3. Click the lighting bolt icon to execute. Refresh your schema sidebar; you should see `smart_expense_tracker` listed as an empty schema database.
4. **Important**: Note down your MySQL username and password. By default, the application is configured to connect with username `root` and password `root` (in `src/main/resources/application.properties`). If yours is different (e.g., password is empty or `mysql123`), open `application.properties` in IntelliJ and update these fields:
   ```properties
   spring.datasource.username=YOUR_USER
   spring.datasource.password=YOUR_PASSWORD
   ```

---

## 2. IntelliJ IDEA Configuration

### Steps:
1. Open **IntelliJ IDEA**.
2. Click **Open** (or **File > Open**) and select the root directory of your project: 
   `C:\Users\Buvaneshwari\.gemini\antigravity\scratch\smart-expense-tracker`
3. Click **OK** to open it as a project. If IntelliJ asks, click **Trust Project**.
4. Since it is a Maven project, IntelliJ will automatically detect the `pom.xml` and start downloading dependencies. Let it finish indexing.
5. **Verify Java version**: Go to **File > Project Structure... > Project** and ensure that the **SDK** is set to Java 17 or above.
6. **Enable Lombok (if needed)**: Go to **Settings > Build, Execution, Deployment > Compiler > Annotation Processors** and ensure **Enable annotation processing** is checked.
7. Run the application:
   - Navigate to `src/main/java/com/tracker/SmartExpenseTrackerApplication.java`.
   - Right-click the file and select **Run 'SmartExpenseTrackerApplication.main()'** (or click the green Play icon next to the class declaration).
8. Verify Console Output:
   - You should see logs indicating database connection and Hibernate queries executing (`create table ...`).
   - The final log should say `Started SmartExpenseTrackerApplication in X seconds (JVM running for Y)` on port `8080`.

---

## 3. Postman API Testing Guide

All APIs use JSON as the request/response payload structure. Below is the sequential order to test your endpoints to ensure proper relational mapping (e.g., transactions requiring user and category IDs).

---

### Phase A: Authentication Module

#### 1. Register User
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/auth/register`
* **Body** (`raw` JSON):
  ```json
  {
    "username": "Buvaneshwari",
    "email": "buvana@example.com",
    "password": "Password123"
  }
  ```
* **Expected Response**: `201 Created`
  ```json
  {
    "userId": 1,
    "username": "Buvaneshwari",
    "email": "buvana@example.com",
    "token": "MOCK_JWT_TOKEN_1",
    "message": "User registered successfully"
  }
  ```

#### 2. Login User
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/auth/login`
* **Body** (`raw` JSON):
  ```json
  {
    "email": "buvana@example.com",
    "password": "Password123"
  }
  ```
* **Expected Response**: `200 OK`
  ```json
  {
    "userId": 1,
    "username": "Buvaneshwari",
    "email": "buvana@example.com",
    "token": "MOCK_JWT_TOKEN_1",
    "message": "Login successful"
  }
  ```

---

### Phase B: Category Module

#### 1. Create Income Category
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/categories?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "name": "Salary",
    "type": "INCOME",
    "description": "Monthly job salary"
  }
  ```
* **Expected Response**: `201 Created` with Category `id: 1`

#### 2. Create Expense Category (Food)
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/categories?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "name": "Food & Dining",
    "type": "EXPENSE",
    "description": "Groceries and restaurants"
  }
  ```
* **Expected Response**: `201 Created` with Category `id: 2`

#### 3. Fetch All Categories
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/categories?userId=1`
* **Expected Response**: List of category objects.

---

### Phase C: Budget Module

#### 1. Configure Category Budget Limit
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/budgets?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "amount": 2000.00,
    "startDate": "2026-06-01",
    "endDate": "2026-06-30",
    "categoryId": 2
  }
  ```
* **Expected Response**: `201 Created` (Sets a monthly spending limit of $2,000 for Food).

---

### Phase D: Transaction Logging (Income & Expense)

#### 1. Log Income
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/incomes?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "amount": 5000.00,
    "date": "2026-06-10",
    "description": "June Salary",
    "categoryId": 1
  }
  ```

#### 2. Log Expense (Food)
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/expenses?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "amount": 150.00,
    "date": "2026-06-14",
    "description": "Supermarket Grocery",
    "categoryId": 2
  }
  ```

#### 3. Log Exceeding Expense (To test alerts)
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/expenses?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "amount": 1900.00,
    "date": "2026-06-15",
    "description": "Fancy Dinner Party",
    "categoryId": 2
  }
  ```

#### 4. Filter Expenses (Search & Filtering API)
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/expenses/filter?userId=1&categoryId=2&startDate=2026-06-01&endDate=2026-06-30`
* **Expected Response**: Lists the grocery and dinner expenses.

---

### Phase E: Budget & Goal Auditing

#### 1. Check Budget Status
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/budgets/check?userId=1&categoryId=2`
* **Expected Response**:
  `"EXCEEDED: Spent 2050.00 of 2000.00 (Exceeded by 50.00) for category 'Food & Dining'"`
  *(Since 150 + 1900 = 2050, exceeding the 2000 limit)*

#### 2. Create Savings Goal
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/goals?userId=1`
* **Body** (`raw` JSON):
  ```json
  {
    "title": "Buy Laptop",
    "targetAmount": 1000.00,
    "currentAmount": 200.00,
    "targetDate": "2026-12-31"
  }
  ```
* **Expected Response**: `201 Created`

#### 3. Add to Savings Goal
* **Method**: `PUT`
* **URL**: `http://localhost:8080/api/goals/1/savings?userId=1&amountChange=300.00`
* **Expected Response**: Goal with current amount updated to `500.00`.

---

### Phase F: Analytics & Dashboard

#### 1. Retrieve Dashboard Summary
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/dashboard/summary?userId=1`
* **Expected Response**:
  ```json
  {
    "totalIncome": 5000.00,
    "totalExpense": 2050.00,
    "budgets": [
      {
        "budgetId": 1,
        "categoryName": "Food & Dining",
        "limitAmount": 2000.00,
        "spentAmount": 2050.00,
        "remainingAmount": 0.00,
        "exceeded": true
      }
    ],
    "recentTransactions": [
      {
        "id": 2,
        "type": "EXPENSE",
        "amount": 1900.00,
        "date": "2026-06-15",
        "categoryName": "Food & Dining",
        "description": "Fancy Dinner Party"
      },
      {
        "id": 1,
        "type": "EXPENSE",
        "amount": 150.00,
        "date": "2026-06-14",
        "categoryName": "Food & Dining",
        "description": "Supermarket Grocery"
      },
      {
        "id": 1,
        "type": "INCOME",
        "amount": 5000.00,
        "date": "2026-06-10",
        "categoryName": "Salary",
        "description": "June Salary"
      }
    ],
    "savingsGoals": [
      {
        "goalId": 1,
        "title": "Buy Laptop",
        "targetAmount": 1000.00,
        "currentAmount": 500.00,
        "percentage": 50.00,
        "targetDate": "2026-12-31"
      }
    ]
  }
  ```

#### 2. Get Spending Breakdown Report
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/reports/category-spending?userId=1&startDate=2026-06-01&endDate=2026-06-30`
* **Expected Response**:
  ```json
  [
    {
      "categoryName": "Food & Dining",
      "totalSpent": 2050.00,
      "percentageOfTotal": 100.00
    }
  ]
  ```

#### 3. Get Monthly Trend Report
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/reports/monthly-trend?userId=1&year=2026`
* **Expected Response**: List of 12 monthly objects with total incomes and total expenses (June will have $5000 income and $2050 expenses).

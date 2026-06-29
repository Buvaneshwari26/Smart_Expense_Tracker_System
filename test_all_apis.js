const http = require('http');
const fs = require('fs');

const API_BASE = 'http://localhost:8080/api';

// Helper to make HTTP Requests
function apiRequest(path, method = 'GET', token = null, data = null) {
    return new Promise((resolve) => {
        const url = new URL(API_BASE + path);
        const options = {
            hostname: url.hostname,
            port: url.port,
            path: url.pathname + url.search,
            method: method,
            headers: {}
        };

        if (data) {
            options.headers['Content-Type'] = 'application/json';
        }

        if (token) {
            options.headers['Authorization'] = `Bearer ${token}`;
        }

        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                let parsed = body;
                try {
                    parsed = JSON.parse(body);
                } catch (e) {}
                resolve({
                    status: res.statusCode,
                    body: parsed
                });
            });
        });

        req.on('error', (err) => {
            resolve({
                status: 500,
                body: { message: err.message }
            });
        });

        if (data) {
            req.write(JSON.stringify(data));
        }
        req.end();
    });
}

async function run() {
    const TS = Date.now(); // unique suffix to avoid name collision on re-runs
    console.log("====================================================");
    console.log("    SMART EXPENSE TRACKER - API AUTOMATION SUITE");
    console.log("====================================================");

    const report = [];

    function recordResult(testName, url, method, role, expectedStatus, actualStatus, responseBody, pass) {
        report.push({
            testName,
            url,
            method,
            role,
            expectedStatus,
            actualStatus,
            responseBody,
            pass
        });
        console.log(`[${pass ? 'PASS' : 'FAIL'}] ${method} ${url} | Role: ${role || 'None'}`);
        console.log(`  Expected Status: ${expectedStatus}, Got: ${actualStatus}`);
        if (!pass) {
            console.log(`  Response: ${JSON.stringify(responseBody, null, 2)}`);
        }
    }

    // 1. Bulk Registration
    console.log("\n--- Pre-registering Bulk Users ---");
    const bulkRegRes = await apiRequest('/auth/pre-register-bulk', 'POST');
    const bulkRegData = bulkRegRes.body && bulkRegRes.body.data ? bulkRegRes.body.data : bulkRegRes.body;
    recordResult(
        "Bulk Pre-Registration",
        "/auth/pre-register-bulk",
        "POST",
        "None",
        200,
        bulkRegRes.status,
        bulkRegRes.body,
        bulkRegRes.status === 200
    );

    // 2. Log in and get tokens for each role
    const credentials = {
        ADMIN: { email: "buvaneshwarip6002@gmail.com", password: "Buvana@1712!Secure" },
        USER: { email: "buvanap1712@gmail.com", password: "Buvana@1712!Secure" },
        ANALYST: { email: "buvanesh6421@gmail.com", password: "Buvana@1712!Secure" },
        AUDITOR: { email: "2k23cse021@kiot.ac.in", password: "Buvana@1712!Secure" }
    };

    const tokens = {};
    const userIds = {};

    console.log("\n--- Testing Login / Authentication ---");
    for (const [role, cred] of Object.entries(credentials)) {
        const loginRes = await apiRequest('/auth/login', 'POST', null, cred);
        const loginData = loginRes.body && loginRes.body.data ? loginRes.body.data : loginRes.body;
        const pass = loginRes.status === 200 && loginData.accessToken;
        if (pass) {
            tokens[role] = loginData.accessToken;
            userIds[role] = loginData.userId;
        }
        recordResult(
            `Login as ${role}`,
            "/auth/login",
            "POST",
            role,
            200,
            loginRes.status,
            loginRes.body,
            pass
        );
    }

    if (!tokens.ADMIN || !tokens.USER || !tokens.ANALYST || !tokens.AUDITOR) {
        console.error("Critical Failure: Could not obtain tokens for all roles. Exiting.");
        process.exit(1);
    }

    // 3. Test Refresh Token
    console.log("\n--- Testing Refresh Token ---");
    const testUserCred = credentials.USER;
    const loginResForRefresh = await apiRequest('/auth/login', 'POST', null, testUserCred);
    const loginRefreshData = loginResForRefresh.body && loginResForRefresh.body.data ? loginResForRefresh.body.data : loginResForRefresh.body;
    if (loginResForRefresh.status === 200 && loginRefreshData.refreshToken) {
        const refreshRes = await apiRequest(`/auth/refresh-token?refreshToken=${loginRefreshData.refreshToken}`, 'POST');
        const refreshData = refreshRes.body && refreshRes.body.data ? refreshRes.body.data : refreshRes.body;
        recordResult(
            "Refresh Token Validation",
            "/auth/refresh-token",
            "POST",
            "USER",
            200,
            refreshRes.status,
            refreshRes.body,
            refreshRes.status === 200 && refreshData.accessToken != null
        );
    }

    // 4. Test User Profiles and User Management APIs
    console.log("\n--- Testing User Management (RBAC) ---");
    
    // GET /api/users (ADMIN: 200, AUDITOR: 200, USER: 403, ANALYST: 403)
    const getUsersAdmin = await apiRequest('/users', 'GET', tokens.ADMIN);
    recordResult("Get All Users as ADMIN", "/users", "GET", "ADMIN", 200, getUsersAdmin.status, getUsersAdmin.body, getUsersAdmin.status === 200);

    const getUsersAuditor = await apiRequest('/users', 'GET', tokens.AUDITOR);
    recordResult("Get All Users as AUDITOR", "/users", "GET", "AUDITOR", 200, getUsersAuditor.status, getUsersAuditor.body, getUsersAuditor.status === 200);

    const getUsersUser = await apiRequest('/users', 'GET', tokens.USER);
    recordResult("Get All Users as USER (Denied)", "/users", "GET", "USER", 403, getUsersUser.status, getUsersUser.body, getUsersUser.status === 403);

    // GET /api/users/{id} (Own Profile vs Other Profile)
    const analystUserId = userIds.ANALYST;
    const getUserOwn = await apiRequest(`/users/${analystUserId}`, 'GET', tokens.ANALYST);
    recordResult("Get Own User Info as ANALYST", `/users/${analystUserId}`, "GET", "ANALYST", 200, getUserOwn.status, getUserOwn.body, getUserOwn.status === 200);

    const getUserOther = await apiRequest(`/users/${analystUserId}`, 'GET', tokens.USER);
    recordResult("Get Other User Info as USER (Denied)", `/users/${analystUserId}`, "GET", "USER", 403, getUserOther.status, getUserOther.body, getUserOther.status === 403);

    // GET /api/users/profile (Allauthenticated roles can get their own profile)
    const getProfileRes = await apiRequest('/users/profile', 'GET', tokens.ANALYST);
    recordResult("Get Own Profile Endpoint as ANALYST", '/users/profile', 'GET', "ANALYST", 200, getProfileRes.status, getProfileRes.body, getProfileRes.status === 200);

    // PUT /api/users/{id} (Update Profile - ADMIN or USER only; ANALYST/AUDITOR denied)
    const updateOwnUserRes = await apiRequest(`/users/${userIds.USER}`, 'PUT', tokens.USER, {
        fullName: "Buvana Updated",
        username: "buvanap1712",
        phoneNumber: "9876543210"
    });
    recordResult("Update Own Profile as USER", `/users/${userIds.USER}`, "PUT", "USER", 200, updateOwnUserRes.status, updateOwnUserRes.body, updateOwnUserRes.status === 200);

    const updateAnalystRes = await apiRequest(`/users/${userIds.ANALYST}`, 'PUT', tokens.ANALYST, {
        fullName: "Analyst Updated",
        username: "buvanesh6421"
    });
    recordResult("Update Own Profile as ANALYST (Denied)", `/users/${userIds.ANALYST}`, "PUT", "ANALYST", 403, updateAnalystRes.status, updateAnalystRes.body, updateAnalystRes.status === 403);

    // PATCH /api/users/{id}/role (ADMIN: 200, others: 403)
    const patchRoleUser = await apiRequest(`/users/${userIds.USER}/role?role=USER`, 'PATCH', tokens.USER);
    recordResult("Patch User Role as USER (Denied)", `/users/${userIds.USER}/role?role=USER`, "PATCH", "USER", 403, patchRoleUser.status, patchRoleUser.body, patchRoleUser.status === 403);

    const patchRoleAdmin = await apiRequest(`/users/${userIds.USER}/role?role=USER`, 'PATCH', tokens.ADMIN);
    recordResult("Patch User Role as ADMIN (Allowed)", `/users/${userIds.USER}/role?role=USER`, "PATCH", "ADMIN", 200, patchRoleAdmin.status, patchRoleAdmin.body, patchRoleAdmin.status === 200);


    // 5. Test Categories CRUD (ADMIN / USER write, ANALYST / AUDITOR read-only)
    console.log("\n--- Testing Categories CRUD ---");
    
    // Create
    const createCatUser = await apiRequest('/categories', 'POST', tokens.USER, {
        name: `TestCategoryUser_${TS}`,
        type: "EXPENSE",
        description: "User category"
    });
    const userCatId = createCatUser.body && createCatUser.body.data ? createCatUser.body.data.id : (createCatUser.body ? createCatUser.body.id : null);
    recordResult("Create Category as USER", "/categories", "POST", "USER", 201, createCatUser.status, createCatUser.body, createCatUser.status === 201 && userCatId != null);

    const createCatAnalyst = await apiRequest('/categories', 'POST', tokens.ANALYST, {
        name: "TestCategoryAnalyst",
        type: "EXPENSE",
        description: "Analyst category"
    });
    recordResult("Create Category as ANALYST (Denied)", "/categories", "POST", "ANALYST", 403, createCatAnalyst.status, createCatAnalyst.body, createCatAnalyst.status === 403);

    // Read list
    const getCats = await apiRequest('/categories', 'GET', tokens.ANALYST);
    recordResult("Get Categories List as ANALYST", "/categories", "GET", "ANALYST", 200, getCats.status, getCats.body, getCats.status === 200);

    // Read by ID
    if (userCatId) {
        const getCatById = await apiRequest(`/categories/${userCatId}`, 'GET', tokens.AUDITOR);
        recordResult("Get Category by ID as AUDITOR", `/categories/${userCatId}`, "GET", "AUDITOR", 200, getCatById.status, getCatById.body, getCatById.status === 200);

        // Update
        const updateCat = await apiRequest(`/categories/${userCatId}`, 'PUT', tokens.USER, {
            name: `TestCategoryUser_Updated_${TS}`,
            type: "EXPENSE",
            description: "Updated description"
        });
        recordResult("Update Category as USER", `/categories/${userCatId}`, "PUT", "USER", 200, updateCat.status, updateCat.body, updateCat.status === 200);

        // Delete (ADMIN only)
        const deleteCatUser = await apiRequest(`/categories/${userCatId}`, 'DELETE', tokens.USER);
        recordResult("Delete Category as USER (Denied)", `/categories/${userCatId}`, "DELETE", "USER", 403, deleteCatUser.status, deleteCatUser.body, deleteCatUser.status === 403);

        const deleteCatAdmin = await apiRequest(`/categories/${userCatId}`, 'DELETE', tokens.ADMIN);
        recordResult("Delete Category as ADMIN", `/categories/${userCatId}`, "DELETE", "ADMIN", 204, deleteCatAdmin.status, deleteCatAdmin.body, deleteCatAdmin.status === 204);
    }


    // 6. Test Expenses CRUD, Search, and Exports
    console.log("\n--- Testing Expenses CRUD, Search & Export ---");
    const testCatRes = await apiRequest('/categories', 'POST', tokens.USER, {
        name: `Logistics_${TS}`,
        type: "EXPENSE",
        description: "Business logistics"
    });
    const expCatId = testCatRes.body && testCatRes.body.data ? testCatRes.body.data.id : (testCatRes.body ? testCatRes.body.id : null);

    let expenseId = null;
    if (expCatId) {
        // Create expense as USER
        const createExp = await apiRequest('/expenses', 'POST', tokens.USER, {
            amount: 150.00,
            date: "2026-06-28",
            description: "Office supplies shipment",
            categoryId: expCatId
        });
        expenseId = createExp.body && createExp.body.data ? createExp.body.data.id : (createExp.body ? createExp.body.id : null);
        recordResult("Create Expense as USER", "/expenses", "POST", "USER", 201, createExp.status, createExp.body, createExp.status === 201 && expenseId != null);

        // Create expense as ANALYST (Denied)
        const createExpAnalyst = await apiRequest('/expenses', 'POST', tokens.ANALYST, {
            amount: 200.00,
            date: "2026-06-28",
            description: "Analyst forbidden expense",
            categoryId: expCatId
        });
        recordResult("Create Expense as ANALYST (Denied)", "/expenses", "POST", "ANALYST", 403, createExpAnalyst.status, createExpAnalyst.body, createExpAnalyst.status === 403);

        // Read list
        const getExpenses = await apiRequest('/expenses?page=0&size=5', 'GET', tokens.USER);
        recordResult("Get Expenses List as USER", "/expenses", "GET", "USER", 200, getExpenses.status, getExpenses.body, getExpenses.status === 200);

        // Read by ID
        const getExpById = await apiRequest(`/expenses/${expenseId}`, 'GET', tokens.ANALYST);
        recordResult("Get Expense by ID as ANALYST", `/expenses/${expenseId}`, "GET", "ANALYST", 200, getExpById.status, getExpById.body, getExpById.status === 200);

        // Search Expenses
        const searchExpenses = await apiRequest(`/expenses/search?keyword=shipment&categoryId=${expCatId}&minAmount=100`, 'GET', tokens.USER);
        recordResult("Search Expenses as USER", `/expenses/search?keyword=shipment&categoryId=${expCatId}&minAmount=100`, "GET", "USER", 200, searchExpenses.status, searchExpenses.body, searchExpenses.status === 200);

        // Get by Category
        const getExpByCat = await apiRequest(`/expenses/category/${expCatId}`, 'GET', tokens.USER);
        recordResult("Get Expenses by Category", `/expenses/category/${expCatId}`, "GET", "USER", 200, getExpByCat.status, getExpByCat.body, getExpByCat.status === 200);

        // Update expense as USER
        const updateExp = await apiRequest(`/expenses/${expenseId}`, 'PUT', tokens.USER, {
            amount: 175.00,
            date: "2026-06-28",
            description: "Office supplies shipment updated",
            categoryId: expCatId
        });
        recordResult("Update Expense as USER", `/expenses/${expenseId}`, "PUT", "USER", 200, updateExp.status, updateExp.body, updateExp.status === 200);

        // Export Excel
        const exportExcel = await apiRequest('/expenses/export/excel', 'GET', tokens.USER);
        recordResult("Export Expenses Excel", "/expenses/export/excel", "GET", "USER", 200, exportExcel.status, "Binary data", exportExcel.status === 200);

        // Export CSV
        const exportCsv = await apiRequest('/expenses/export/csv', 'GET', tokens.USER);
        recordResult("Export Expenses CSV", "/expenses/export/csv", "GET", "USER", 200, exportCsv.status, "CSV text data", exportCsv.status === 200);

        // Export PDF
        const exportPdf = await apiRequest('/expenses/export/pdf', 'GET', tokens.USER);
        recordResult("Export Expenses PDF", "/expenses/export/pdf", "GET", "USER", 200, exportPdf.status, "Binary PDF data", exportPdf.status === 200);

        // Delete expense as USER
        const deleteExp = await apiRequest(`/expenses/${expenseId}`, 'DELETE', tokens.USER);
        recordResult("Delete Expense as USER", `/expenses/${expenseId}`, "DELETE", "USER", 204, deleteExp.status, deleteExp.body, deleteExp.status === 204);
    }


    // 7. Test Incomes CRUD, Search, and Exports
    console.log("\n--- Testing Incomes CRUD, Search & Export ---");
    const testIncCatRes = await apiRequest('/categories', 'POST', tokens.USER, {
        name: `Side Hustle_${TS}`,
        type: "INCOME",
        description: "Freelance gigs"
    });
    const incCatId = testIncCatRes.body && testIncCatRes.body.data ? testIncCatRes.body.data.id : (testIncCatRes.body ? testIncCatRes.body.id : null);

    let incomeId = null;
    if (incCatId) {
        // Create income
        const createInc = await apiRequest('/incomes', 'POST', tokens.USER, {
            amount: 800.00,
            date: "2026-06-28",
            source: "Upwork",
            description: "Web development contract",
            categoryId: incCatId
        });
        incomeId = createInc.body && createInc.body.data ? createInc.body.data.id : (createInc.body ? createInc.body.id : null);
        recordResult("Create Income as USER", "/incomes", "POST", "USER", 201, createInc.status, createInc.body, createInc.status === 201 && incomeId != null);

        // Read list
        const getIncomes = await apiRequest('/incomes?page=0&size=5', 'GET', tokens.USER);
        recordResult("Get Incomes List as USER", "/incomes", "GET", "USER", 200, getIncomes.status, getIncomes.body, getIncomes.status === 200);

        // Read by ID
        const getIncById = await apiRequest(`/incomes/${incomeId}`, 'GET', tokens.USER);
        recordResult("Get Income by ID as USER", `/incomes/${incomeId}`, "GET", "USER", 200, getIncById.status, getIncById.body, getIncById.status === 200);

        // Search Incomes
        const searchIncomes = await apiRequest(`/incomes/search?keyword=Upwork&categoryId=${incCatId}`, 'GET', tokens.USER);
        recordResult("Search Incomes as USER", `/incomes/search?keyword=Upwork&categoryId=${incCatId}`, "GET", "USER", 200, searchIncomes.status, searchIncomes.body, searchIncomes.status === 200);

        // Update
        const updateInc = await apiRequest(`/incomes/${incomeId}`, 'PUT', tokens.USER, {
            amount: 900.00,
            date: "2026-06-28",
            source: "Upwork Updated",
            description: "Web development contract updated",
            categoryId: incCatId
        });
        recordResult("Update Income as USER", `/incomes/${incomeId}`, "PUT", "USER", 200, updateInc.status, updateInc.body, updateInc.status === 200);

        // Exports
        const exportExcel = await apiRequest('/incomes/export/excel', 'GET', tokens.USER);
        recordResult("Export Incomes Excel", "/incomes/export/excel", "GET", "USER", 200, exportExcel.status, "Binary data", exportExcel.status === 200);

        const exportCsv = await apiRequest('/incomes/export/csv', 'GET', tokens.USER);
        recordResult("Export Incomes CSV", "/incomes/export/csv", "GET", "USER", 200, exportCsv.status, "CSV data", exportCsv.status === 200);

        // Delete
        const deleteInc = await apiRequest(`/incomes/${incomeId}`, 'DELETE', tokens.USER);
        recordResult("Delete Income as USER", `/incomes/${incomeId}`, "DELETE", "USER", 204, deleteInc.status, deleteInc.body, deleteInc.status === 204);
    }


    // 8. Test Budgets CRUD
    console.log("\n--- Testing Budgets CRUD ---");
    let budgetId = null;
    if (expCatId) {
        // Create budget
        const createBudget = await apiRequest('/budgets', 'POST', tokens.USER, {
            budgetAmount: 600.00,
            month: 6,
            year: 2026,
            categoryId: expCatId
        });
        budgetId = createBudget.body && createBudget.body.data ? createBudget.body.data.id : (createBudget.body ? createBudget.body.id : null);
        recordResult("Create Budget as USER", "/budgets", "POST", "USER", 201, createBudget.status, createBudget.body, createBudget.status === 201 && budgetId != null);

        // Read list
        const getBudgets = await apiRequest('/budgets', 'GET', tokens.USER);
        recordResult("Get Budgets List as USER", "/budgets", "GET", "USER", 200, getBudgets.status, getBudgets.body, getBudgets.status === 200);

        // Read by ID
        const getBudgetById = await apiRequest(`/budgets/${budgetId}`, 'GET', tokens.USER);
        recordResult("Get Budget by ID as USER", `/budgets/${budgetId}`, "GET", "USER", 200, getBudgetById.status, getBudgetById.body, getBudgetById.status === 200);

        // Update
        const updateBudget = await apiRequest(`/budgets/${budgetId}`, 'PUT', tokens.USER, {
            budgetAmount: 800.00,
            month: 6,
            year: 2026,
            categoryId: expCatId
        });
        recordResult("Update Budget as USER", `/budgets/${budgetId}`, "PUT", "USER", 200, updateBudget.status, updateBudget.body, updateBudget.status === 200);

        // Delete
        const deleteBudget = await apiRequest(`/budgets/${budgetId}`, 'DELETE', tokens.USER);
        recordResult("Delete Budget as USER", `/budgets/${budgetId}`, "DELETE", "USER", 204, deleteBudget.status, deleteBudget.body, deleteBudget.status === 204);
    }


    // 9. Test Savings Goals CRUD & Progress
    console.log("\n--- Testing Savings Goals CRUD ---");
    let goalId = null;
    // Create
    const createGoal = await apiRequest('/goals', 'POST', tokens.USER, {
        goalName: "Vacation Savings",
        targetAmount: 5000.00,
        currentAmount: 500.00,
        targetDate: "2027-08-31"
    });
    goalId = createGoal.body && createGoal.body.data ? createGoal.body.data.id : (createGoal.body ? createGoal.body.id : null);
    recordResult("Create Savings Goal as USER", "/goals", "POST", "USER", 201, createGoal.status, createGoal.body, createGoal.status === 201 && goalId != null);

    if (goalId) {
        // Read list
        const getGoals = await apiRequest('/goals?page=0&size=5', 'GET', tokens.USER);
        recordResult("Get Savings Goals List as USER", "/goals", "GET", "USER", 200, getGoals.status, getGoals.body, getGoals.status === 200);

        // Read by ID
        const getGoalById = await apiRequest(`/goals/${goalId}`, 'GET', tokens.USER);
        recordResult("Get Savings Goal by ID as USER", `/goals/${goalId}`, "GET", "USER", 200, getGoalById.status, getGoalById.body, getGoalById.status === 200);

        // Add Savings progress
        const addSavings = await apiRequest(`/goals/${goalId}/add-savings?amount=200.00`, 'PATCH', tokens.USER);
        recordResult("Add Savings to Goal", `/goals/${goalId}/add-savings?amount=200.00`, "PATCH", "USER", 200, addSavings.status, addSavings.body, addSavings.status === 200);

        // Update
        const updateGoal = await apiRequest(`/goals/${goalId}`, 'PUT', tokens.USER, {
            goalName: "Hawaii Vacation Savings",
            targetAmount: 6000.00,
            currentAmount: 700.00,
            targetDate: "2027-08-31"
        });
        recordResult("Update Savings Goal as USER", `/goals/${goalId}`, "PUT", "USER", 200, updateGoal.status, updateGoal.body, updateGoal.status === 200);

        // Delete
        const deleteGoal = await apiRequest(`/goals/${goalId}`, 'DELETE', tokens.USER);
        recordResult("Delete Savings Goal as USER", `/goals/${goalId}`, "DELETE", "USER", 204, deleteGoal.status, deleteGoal.body, deleteGoal.status === 204);
    }


    // 10. Test Recurring Transactions
    console.log("\n--- Testing Recurring Transactions ---");
    let recurringId = null;
    if (expCatId) {
        // Create schedule
        const createRecur = await apiRequest('/recurring', 'POST', tokens.USER, {
            amount: 15.00,
            description: "Spotify Premium",
            categoryId: expCatId,
            frequency: "MONTHLY",
            startDate: "2026-06-01"
        });
        recurringId = createRecur.body && createRecur.body.data ? createRecur.body.data.id : (createRecur.body ? createRecur.body.id : null);
        recordResult("Create Recurring Schedule as USER", "/recurring", "POST", "USER", 200, createRecur.status, createRecur.body, createRecur.status === 200 && recurringId != null);

        if (recurringId) {
            // Read list
            const getRecurs = await apiRequest('/recurring', 'GET', tokens.USER);
            recordResult("Get Recurring List as USER", "/recurring", "GET", "USER", 200, getRecurs.status, getRecurs.body, getRecurs.status === 200);

            // Update
            const updateRecur = await apiRequest(`/recurring/${recurringId}`, 'PUT', tokens.USER, {
                amount: 18.99,
                description: "Spotify Premium Family",
                categoryId: expCatId,
                frequency: "MONTHLY",
                active: true
            });
            recordResult("Update Recurring Schedule as USER", `/recurring/${recurringId}`, "PUT", "USER", 200, updateRecur.status, updateRecur.body, updateRecur.status === 200);

            // Admin manual trigger
            const triggerProcess = await apiRequest('/recurring/trigger-process', 'POST', tokens.ADMIN);
            recordResult("Trigger Recurring Process as ADMIN", "/recurring/trigger-process", "POST", "ADMIN", 200, triggerProcess.status, triggerProcess.body, triggerProcess.status === 200);

            // Delete
            const deleteRecur = await apiRequest(`/recurring/${recurringId}`, 'DELETE', tokens.USER);
            recordResult("Delete Recurring Schedule as USER", `/recurring/${recurringId}`, "DELETE", "USER", 204, deleteRecur.status, deleteRecur.body, deleteRecur.status === 204);
        }
    }


    // 11. Test Notifications
    console.log("\n--- Testing Notifications ---");
    const getNotifs = await apiRequest('/notifications?page=0&size=5', 'GET', tokens.USER);
    recordResult("Get Notifications List", "/notifications?page=0&size=5", "GET", "USER", 200, getNotifs.status, getNotifs.body, getNotifs.status === 200);

    const getUnreadCount = await apiRequest('/notifications/unread-count', 'GET', tokens.USER);
    recordResult("Get Unread Notifications Count", "/notifications/unread-count", "GET", "USER", 200, getUnreadCount.status, getUnreadCount.body, getUnreadCount.status === 200);


    // 12. Dashboard Summary
    console.log("\n--- Testing Dashboard ---");
    const getDashboard = await apiRequest('/dashboard', 'GET', tokens.USER);
    recordResult("Get Dashboard Summary", "/dashboard", "GET", "USER", 200, getDashboard.status, getDashboard.body, getDashboard.status === 200);


    // 13. Reports (All authenticated allowed)
    console.log("\n--- Testing Reports ---");
    const monthlyReport = await apiRequest('/reports/monthly?month=6&year=2026', 'GET', tokens.USER);
    recordResult("Get Monthly Report", "/reports/monthly?month=6&year=2026", "GET", "USER", 200, monthlyReport.status, monthlyReport.body, monthlyReport.status === 200);

    const yearlyReport = await apiRequest('/reports/yearly?year=2026', 'GET', tokens.USER);
    recordResult("Get Yearly Report", "/reports/yearly?year=2026", "GET", "USER", 200, yearlyReport.status, yearlyReport.body, yearlyReport.status === 200);

    const categoryReport = await apiRequest('/reports/category?month=6&year=2026', 'GET', tokens.USER);
    recordResult("Get Category Report", "/reports/category?month=6&year=2026", "GET", "USER", 200, categoryReport.status, categoryReport.body, categoryReport.status === 200);

    const compareReport = await apiRequest('/reports/income-vs-expense?year=2026', 'GET', tokens.USER);
    recordResult("Get Income vs Expense Comparison", "/reports/income-vs-expense?year=2026", "GET", "USER", 200, compareReport.status, compareReport.body, compareReport.status === 200);


    // 14. Admin Backups
    console.log("\n--- Testing Admin Backups ---");
    const backupAdmin = await apiRequest('/admin/backup/export', 'GET', tokens.ADMIN);
    recordResult("Export Backup as ADMIN", "/admin/backup/export", "GET", "ADMIN", 200, backupAdmin.status, "Backup dump JSON", backupAdmin.status === 200);

    const backupAuditor = await apiRequest('/admin/backup/export', 'GET', tokens.AUDITOR);
    recordResult("Export Backup as AUDITOR", "/admin/backup/export", "GET", "AUDITOR", 200, backupAuditor.status, "Backup dump JSON", backupAuditor.status === 200);

    const backupUser = await apiRequest('/admin/backup/export', 'GET', tokens.USER);
    recordResult("Export Backup as USER (Denied)", "/admin/backup/export", "GET", "USER", 403, backupUser.status, backupUser.body, backupUser.status === 403);


    // Clean up temporary category
    if (expCatId) {
        await apiRequest(`/categories/${expCatId}`, 'DELETE', tokens.ADMIN);
    }
    if (incCatId) {
        await apiRequest(`/categories/${incCatId}`, 'DELETE', tokens.ADMIN);
    }


    // 15. Summary
    console.log("\n====================================================");
    console.log("             COMPREHENSIVE TEST SUMMARY             ");
    console.log("====================================================");
    let passed = 0;
    let failed = 0;
    for (const r of report) {
        if (r.pass) passed++;
        else failed++;
    }
    console.log(`TOTAL TESTS RUN : ${report.length}`);
    console.log(`TOTAL PASSED    : ${passed}`);
    console.log(`TOTAL FAILED    : ${failed}`);
    console.log("====================================================");

    // Save final report
    fs.writeFileSync('api_test_results.json', JSON.stringify(report, null, 2));
    console.log("Complete JSON test results written to api_test_results.json");

    if (failed > 0) {
        console.error("Test failed: some API endpoints returned errors.");
        process.exit(1);
    } else {
        console.log("SUCCESS: All API endpoints passed verification!");
        process.exit(0);
    }
}

run();

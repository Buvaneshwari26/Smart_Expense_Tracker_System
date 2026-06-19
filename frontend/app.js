const API_BASE = 'http://localhost:8080/api';

const appState = {
    userId: localStorage.getItem('userId'),
    username: localStorage.getItem('username'),
    categories: [],
    incomes: [],
    expenses: []
};

// --- Initialization & UI Logic ---
document.addEventListener('DOMContentLoaded', () => {
    initAuthForms();
    initSidebar();
    initModals();

    if (appState.userId) {
        showApp();
    } else {
        showAuth();
    }
});

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-info-circle';
    if(type === 'success') icon = 'fa-check-circle';
    if(type === 'error') icon = 'fa-exclamation-circle';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// --- Auth Handling ---
function initAuthForms() {
    const btnLogin = document.getElementById('btn-show-login');
    const btnRegister = document.getElementById('btn-show-register');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');

    btnLogin.addEventListener('click', () => {
        btnLogin.classList.add('active');
        btnRegister.classList.remove('active');
        loginForm.classList.add('active');
        registerForm.classList.remove('active');
    });

    btnRegister.addEventListener('click', () => {
        btnRegister.classList.add('active');
        btnLogin.classList.remove('active');
        registerForm.classList.add('active');
        loginForm.classList.remove('active');
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        
        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ email, password })
            });
            const data = await res.json();
            if (res.ok) {
                appState.userId = data.userId;
                appState.username = data.username;
                localStorage.setItem('userId', data.userId);
                localStorage.setItem('username', data.username);
                showToast('Login successful', 'success');
                showApp();
            } else {
                showToast(data.message || 'Login failed', 'error');
            }
        } catch (err) {
            showToast('Network error', 'error');
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('register-username').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        
        try {
            const res = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ username, email, password })
            });
            const data = await res.json();
            if (res.ok || res.status === 201) {
                showToast('Registration successful. Please login.', 'success');
                btnLogin.click();
            } else {
                showToast(data.message || 'Registration failed', 'error');
            }
        } catch (err) {
            showToast('Network error', 'error');
        }
    });

    document.getElementById('btn-logout').addEventListener('click', () => {
        appState.userId = null;
        appState.username = null;
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        showAuth();
    });
}

function showApp() {
    document.getElementById('auth-view').classList.remove('active');
    document.getElementById('app-view').classList.add('active');
    document.getElementById('display-username').textContent = appState.username || 'User';
    app.loadDashboard();
    app.loadCategories(); // Preload for dropdowns
}

function showAuth() {
    document.getElementById('app-view').classList.remove('active');
    document.getElementById('auth-view').classList.add('active');
}

// --- Navigation ---
function initSidebar() {
    const links = document.querySelectorAll('.nav-links a');
    const sections = document.querySelectorAll('.app-section');

    links.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            links.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            
            const targetId = link.getAttribute('data-target');
            sections.forEach(s => s.classList.remove('active'));
            document.getElementById(targetId).classList.add('active');

            // Load data based on section
            if(targetId === 'dashboard-section') app.loadDashboard();
            if(targetId === 'categories-section') app.loadCategories();
            if(targetId === 'incomes-section') app.loadIncomes();
            if(targetId === 'expenses-section') app.loadExpenses();
            if(targetId === 'budgets-section') app.loadBudgets();
            if(targetId === 'goals-section') app.loadGoals();
            if(targetId === 'reports-section') app.loadReports();
        });
    });
}

// --- Modals ---
function showModal(id) {
    document.getElementById(id).classList.add('active');
}
function closeModal(id) {
    document.getElementById(id).classList.remove('active');
    const form = document.getElementById(id).querySelector('form');
    if(form) form.reset();
}
function initModals() {
    // Category
    document.getElementById('category-form').addEventListener('submit', app.saveCategory);
    // Income
    document.getElementById('income-form').addEventListener('submit', app.saveIncome);
    // Expense
    document.getElementById('expense-form').addEventListener('submit', app.saveExpense);
    // Budget
    document.getElementById('budget-form').addEventListener('submit', app.saveBudget);
    // Goal
    document.getElementById('goal-form').addEventListener('submit', app.saveGoal);
}


// --- API Handlers ---
const api = {
    async get(endpoint) {
        const res = await fetch(`${API_BASE}${endpoint}?userId=${appState.userId}`);
        if(!res.ok) throw new Error('API Error');
        return res.json();
    },
    async post(endpoint, data) {
        const res = await fetch(`${API_BASE}${endpoint}?userId=${appState.userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if(!res.ok) throw new Error('API Error');
        return res.json();
    },
    async delete(endpoint) {
        const res = await fetch(`${API_BASE}${endpoint}?userId=${appState.userId}`, {
            method: 'DELETE'
        });
        if(!res.ok) throw new Error('API Error');
        return res.text();
    }
};

// --- App Logic ---
const app = {
    // Formats currency
    formatCurr(amount) {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
    },

    // --- Dashboard ---
    async loadDashboard() {
        try {
            const data = await api.get('/dashboard/summary');
            const totalBalance = (data.totalIncome || 0) - (data.totalExpense || 0);
            document.getElementById('dash-balance').textContent = this.formatCurr(totalBalance);
            document.getElementById('dash-income').textContent = this.formatCurr(data.totalIncome);
            document.getElementById('dash-expense').textContent = this.formatCurr(data.totalExpense);

            const tbody = document.getElementById('dash-transactions-table').querySelector('tbody');
            tbody.innerHTML = '';
            
            if(data.recentTransactions && data.recentTransactions.length > 0) {
                data.recentTransactions.forEach(t => {
                    const isInc = t.type === 'INCOME';
                    const amountClass = isInc ? 'badge-income' : 'badge-expense';
                    tbody.innerHTML += `
                        <tr>
                            <td>${t.date}</td>
                            <td>${t.description}</td>
                            <td>${t.categoryName || 'General'}</td>
                            <td class="${isInc ? 'style="color: var(--success);"' : 'style="color: var(--danger);"'}">
                                ${isInc ? '+' : '-'}${this.formatCurr(t.amount)}
                            </td>
                            <td><span class="badge ${amountClass}">${t.type}</span></td>
                        </tr>
                    `;
                });
            } else {
                tbody.innerHTML = '<tr><td colspan="5">No recent transactions.</td></tr>';
            }

            // Budget Status
            const budgetList = document.getElementById('dash-budgets-list');
            budgetList.innerHTML = '';
            if(data.budgetStatuses && data.budgetStatuses.length > 0) {
                data.budgetStatuses.forEach(b => {
                    const percent = Math.min((b.spentAmount / b.limitAmount) * 100, 100);
                    let colorClass = '';
                    if(percent > 90) colorClass = 'danger';
                    else if(percent > 75) colorClass = 'warning';

                    budgetList.innerHTML += `
                        <div class="budget-item">
                            <div class="budget-header">
                                <span>${b.categoryName}</span>
                                <span>${this.formatCurr(b.spentAmount)} / ${this.formatCurr(b.limitAmount)}</span>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill ${colorClass}" style="width: ${percent}%"></div>
                            </div>
                        </div>
                    `;
                });
            } else {
                budgetList.innerHTML = '<p class="text-muted">No active budgets.</p>';
            }

        } catch (err) {
            showToast('Failed to load dashboard', 'error');
        }
    },

    // --- Categories ---
    async loadCategories() {
        try {
            appState.categories = await api.get('/categories');
            const tbody = document.getElementById('categories-table').querySelector('tbody');
            tbody.innerHTML = '';
            
            // Populate Dropdowns
            const typeOpts = { EXPENSE: '', INCOME: '' };
            const filterSelect = document.getElementById('expense-filter-category');
            filterSelect.innerHTML = '<option value="">All Categories</option>';

            appState.categories.forEach(c => {
                tbody.innerHTML += `
                    <tr>
                        <td>${c.name}</td>
                        <td><span class="badge ${c.type==='INCOME' ? 'badge-income' : 'badge-expense'}">${c.type}</span></td>
                        <td>${c.description || '-'}</td>
                        <td class="action-btns">
                            <button class="btn-icon" onclick="app.deleteCategory(${c.id})"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `;
                
                const opt = `<option value="${c.id}">${c.name}</option>`;
                if(c.type === 'EXPENSE') {
                    typeOpts.EXPENSE += opt;
                    filterSelect.innerHTML += opt;
                } else {
                    typeOpts.INCOME += opt;
                }
            });

            document.getElementById('expense-category').innerHTML = typeOpts.EXPENSE;
            document.getElementById('budget-category').innerHTML = typeOpts.EXPENSE;
            document.getElementById('income-category').innerHTML = typeOpts.INCOME;

        } catch (err) {
            showToast('Failed to load categories', 'error');
        }
    },

    async saveCategory(e) {
        e.preventDefault();
        const data = {
            name: document.getElementById('category-name').value,
            type: document.getElementById('category-type').value,
            description: document.getElementById('category-desc').value
        };
        try {
            await api.post('/categories', data);
            showToast('Category saved', 'success');
            closeModal('category-modal');
            app.loadCategories();
        } catch (err) { showToast('Error saving category', 'error'); }
    },
    async deleteCategory(id) {
        if(!confirm('Delete this category?')) return;
        try {
            await api.delete(`/categories/${id}`);
            showToast('Category deleted', 'success');
            app.loadCategories();
        } catch (err) { showToast('Error deleting', 'error'); }
    },

    // --- Incomes ---
    async loadIncomes() {
        try {
            appState.incomes = await api.get('/incomes');
            const tbody = document.getElementById('incomes-table').querySelector('tbody');
            tbody.innerHTML = '';
            appState.incomes.forEach(inc => {
                const catName = appState.categories.find(c => c.id === inc.categoryId)?.name || 'Unknown';
                tbody.innerHTML += `
                    <tr>
                        <td>${inc.date}</td>
                        <td>${inc.description || '-'}</td>
                        <td>${catName}</td>
                        <td style="color: var(--success);">${this.formatCurr(inc.amount)}</td>
                        <td><button class="btn-icon" onclick="app.deleteIncome(${inc.id})"><i class="fa-solid fa-trash"></i></button></td>
                    </tr>
                `;
            });
        } catch (err) { showToast('Failed to load incomes', 'error'); }
    },
    async saveIncome(e) {
        e.preventDefault();
        const data = {
            amount: document.getElementById('income-amount').value,
            date: document.getElementById('income-date').value,
            categoryId: document.getElementById('income-category').value,
            description: document.getElementById('income-desc').value
        };
        try {
            await api.post('/incomes', data);
            showToast('Income added', 'success');
            closeModal('income-modal');
            app.loadIncomes();
        } catch (err) { showToast('Error adding income', 'error'); }
    },
    async deleteIncome(id) {
        if(!confirm('Delete this income?')) return;
        try {
            await api.delete(`/incomes/${id}`);
            showToast('Income deleted', 'success');
            app.loadIncomes();
        } catch (err) { showToast('Error deleting', 'error'); }
    },

    // --- Expenses ---
    async loadExpenses() {
        try {
            let url = '/expenses/filter';
            const cat = document.getElementById('expense-filter-category').value;
            const start = document.getElementById('expense-filter-start').value;
            const end = document.getElementById('expense-filter-end').value;
            
            let queryParams = [];
            if(cat) queryParams.push(`categoryId=${cat}`);
            if(start) queryParams.push(`startDate=${start}`);
            if(end) queryParams.push(`endDate=${end}`);
            
            if(queryParams.length > 0) {
                const res = await fetch(`${API_BASE}/expenses/filter?userId=${appState.userId}&${queryParams.join('&')}`);
                appState.expenses = await res.json();
            } else {
                appState.expenses = await api.get('/expenses');
            }

            const tbody = document.getElementById('expenses-table').querySelector('tbody');
            tbody.innerHTML = '';
            appState.expenses.forEach(exp => {
                const catName = appState.categories.find(c => c.id === exp.categoryId)?.name || 'Unknown';
                tbody.innerHTML += `
                    <tr>
                        <td>${exp.date}</td>
                        <td>${exp.description || '-'}</td>
                        <td>${catName}</td>
                        <td style="color: var(--danger);">${this.formatCurr(exp.amount)}</td>
                        <td><button class="btn-icon" onclick="app.deleteExpense(${exp.id})"><i class="fa-solid fa-trash"></i></button></td>
                    </tr>
                `;
            });
        } catch (err) { showToast('Failed to load expenses', 'error'); }
    },
    resetExpenseFilters() {
        document.getElementById('expense-filter-category').value = '';
        document.getElementById('expense-filter-start').value = '';
        document.getElementById('expense-filter-end').value = '';
        this.loadExpenses();
    },
    async saveExpense(e) {
        e.preventDefault();
        const data = {
            amount: document.getElementById('expense-amount').value,
            date: document.getElementById('expense-date').value,
            categoryId: document.getElementById('expense-category').value,
            description: document.getElementById('expense-desc').value
        };
        try {
            await api.post('/expenses', data);
            showToast('Expense added', 'success');
            closeModal('expense-modal');
            app.loadExpenses();
        } catch (err) { showToast('Error adding expense', 'error'); }
    },
    async deleteExpense(id) {
        if(!confirm('Delete this expense?')) return;
        try {
            await api.delete(`/expenses/${id}`);
            showToast('Expense deleted', 'success');
            app.loadExpenses();
        } catch (err) { showToast('Error deleting', 'error'); }
    },

    // --- Budgets ---
    async loadBudgets() {
        try {
            const data = await api.get('/budgets');
            const tbody = document.getElementById('budgets-table').querySelector('tbody');
            tbody.innerHTML = '';
            data.forEach(b => {
                const catName = appState.categories.find(c => c.id === b.categoryId)?.name || 'Unknown';
                tbody.innerHTML += `
                    <tr>
                        <td>${catName}</td>
                        <td>${this.formatCurr(b.limitAmount)}</td>
                        <td>${b.startDate}</td>
                        <td>${b.endDate}</td>
                        <td><button class="btn-icon" onclick="app.deleteBudget(${b.id})"><i class="fa-solid fa-trash"></i></button></td>
                    </tr>
                `;
            });
        } catch (err) { showToast('Failed to load budgets', 'error'); }
    },
    async saveBudget(e) {
        e.preventDefault();
        const data = {
            categoryId: document.getElementById('budget-category').value,
            limitAmount: document.getElementById('budget-amount').value,
            startDate: document.getElementById('budget-start').value,
            endDate: document.getElementById('budget-end').value
        };
        try {
            await api.post('/budgets', data);
            showToast('Budget saved', 'success');
            closeModal('budget-modal');
            app.loadBudgets();
        } catch (err) { showToast('Error saving budget', 'error'); }
    },
    async deleteBudget(id) {
        if(!confirm('Delete this budget?')) return;
        try {
            await api.delete(`/budgets/${id}`);
            showToast('Budget deleted', 'success');
            app.loadBudgets();
        } catch (err) { showToast('Error deleting', 'error'); }
    },

    // --- Goals ---
    async loadGoals() {
        try {
            const data = await api.get('/savings-goals');
            const grid = document.getElementById('goals-grid');
            grid.innerHTML = '';
            data.forEach(g => {
                const percent = Math.min((g.currentAmount / g.targetAmount) * 100, 100);
                grid.innerHTML += `
                    <div class="goal-card glass-panel">
                        <div class="goal-card-header">
                            <h3>${g.title}</h3>
                            <button class="btn-icon" onclick="app.deleteGoal(${g.id})"><i class="fa-solid fa-trash"></i></button>
                        </div>
                        <p class="goal-amount">${this.formatCurr(g.currentAmount)}</p>
                        <div class="progress-bar-bg">
                            <div class="progress-bar-fill" style="width: ${percent}%; background: var(--success)"></div>
                        </div>
                        <div class="goal-meta">
                            <span>Target: ${this.formatCurr(g.targetAmount)}</span>
                            <span>By: ${g.targetDate}</span>
                        </div>
                    </div>
                `;
            });
        } catch (err) { showToast('Failed to load goals', 'error'); }
    },
    async saveGoal(e) {
        e.preventDefault();
        const data = {
            title: document.getElementById('goal-title').value,
            targetAmount: document.getElementById('goal-target-amount').value,
            currentAmount: document.getElementById('goal-current-amount').value,
            targetDate: document.getElementById('goal-target-date').value
        };
        try {
            await api.post('/savings-goals', data);
            showToast('Goal saved', 'success');
            closeModal('goal-modal');
            app.loadGoals();
        } catch (err) { showToast('Error saving goal', 'error'); }
    },
    async deleteGoal(id) {
        if(!confirm('Delete this goal?')) return;
        try {
            await api.delete(`/savings-goals/${id}`);
            showToast('Goal deleted', 'success');
            app.loadGoals();
        } catch (err) { showToast('Error deleting', 'error'); }
    },

    // --- Reports & Charts ---
    chartInstance1: null,
    chartInstance2: null,
    async loadReports() {
        const start = document.getElementById('report-start-date').value;
        const end = document.getElementById('report-end-date').value;
        const year = document.getElementById('report-year').value || new Date().getFullYear();

        try {
            // Expenses by Category Chart
            let catUrl = `${API_BASE}/reports/expenses-by-category?userId=${appState.userId}`;
            if(start) catUrl += `&startDate=${start}`;
            if(end) catUrl += `&endDate=${end}`;
            
            const catRes = await fetch(catUrl);
            const catData = await catRes.json();
            
            this.renderCategoryChart(catData);

            // Monthly Trend Chart
            const trendRes = await fetch(`${API_BASE}/reports/monthly-trend?userId=${appState.userId}&year=${year}`);
            const trendData = await trendRes.json();

            this.renderTrendChart(trendData);

        } catch(err) {
            showToast('Error loading reports', 'error');
        }
    },

    renderCategoryChart(data) {
        const ctx = document.getElementById('categoryChart').getContext('2d');
        if(this.chartInstance1) this.chartInstance1.destroy();

        const labels = Object.keys(data);
        const values = Object.values(data);
        const colors = ['#6366f1', '#ec4899', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#14b8a6'];

        this.chartInstance1 = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right', labels: { color: '#f8fafc' } }
                }
            }
        });
    },

    renderTrendChart(data) {
        const ctx = document.getElementById('trendChart').getContext('2d');
        if(this.chartInstance2) this.chartInstance2.destroy();

        const labels = Object.keys(data).sort((a,b) => parseInt(a)-parseInt(b)); // Sort by month number
        const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
        const displayLabels = labels.map(m => monthNames[parseInt(m)-1]);
        
        const incomeData = labels.map(m => data[m].INCOME || 0);
        const expenseData = labels.map(m => data[m].EXPENSE || 0);

        this.chartInstance2 = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: displayLabels,
                datasets: [
                    {
                        label: 'Income',
                        data: incomeData,
                        backgroundColor: '#10b981',
                        borderRadius: 4
                    },
                    {
                        label: 'Expense',
                        data: expenseData,
                        backgroundColor: '#ef4444',
                        borderRadius: 4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(255,255,255,0.1)' },
                        ticks: { color: '#94a3b8' }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: '#94a3b8' }
                    }
                },
                plugins: {
                    legend: { labels: { color: '#f8fafc' } }
                }
            }
        });
    }
};

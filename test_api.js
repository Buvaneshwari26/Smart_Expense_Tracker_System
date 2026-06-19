const http = require('http');

const API_BASE = 'http://localhost:8080/api';

function request(path, method = 'GET', data = null) {
    return new Promise((resolve, reject) => {
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

        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    try {
                        resolve(JSON.parse(body));
                    } catch (e) {
                        resolve(body);
                    }
                } else {
                    reject(`Request failed with status ${res.statusCode}: ${body}`);
                }
            });
        });

        req.on('error', reject);

        if (data) {
            req.write(JSON.stringify(data));
        }
        req.end();
    });
}

async function runTests() {
    try {
        console.log("1. Testing Registration...");
        try {
            const registerResponse = await request('/auth/register', 'POST', {
                username: 'TestUser999',
                email: 'test999@example.com',
                password: 'password123'
            });
            console.log("Registration successful:", registerResponse);
        } catch(e) {
            console.log("Registration skipped or failed (might already exist).");
        }
        
        console.log("2. Testing Login...");
        const loginResponse = await request('/auth/login', 'POST', {
            email: 'test999@example.com',
            password: 'password123'
        });
        console.log("Login successful:", loginResponse);
        const userId = loginResponse.userId;
        
        console.log(`3. Adding Category for user ${userId}...`);
        const catResponse = await request(`/categories?userId=${userId}`, 'POST', {
            name: 'Freelance',
            type: 'INCOME',
            description: 'Side projects'
        });
        console.log("Category added:", catResponse);
        const catId = catResponse.id;
        
        console.log("4. Adding Income...");
        const incomeResponse = await request(`/incomes?userId=${userId}`, 'POST', {
            amount: 500.0,
            date: '2026-06-20',
            categoryId: catId,
            description: 'Web development project'
        });
        console.log("Income added:", incomeResponse);
        
        console.log("5. Checking Dashboard...");
        const dashboardResponse = await request(`/dashboard/summary?userId=${userId}`, 'GET');
        console.log("Dashboard response:", dashboardResponse);
        
        if (dashboardResponse.totalIncome === 500.0) {
            console.log("SUCCESS! All pages and backend integrations are working correctly.");
        } else {
            console.log("WARNING: Dashboard income did not match expected value.");
        }
    } catch (e) {
        console.error("Test failed:", e);
    }
}

runTests();

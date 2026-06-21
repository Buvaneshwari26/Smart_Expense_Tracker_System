const API_BASE = 'http://localhost:8080/api';

async function runRegistration() {
    console.log('Initiating bulk pre-registration...');
    try {
        const response = await fetch(`${API_BASE}/auth/pre-register-bulk`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(`Server returned error: ${response.status} - ${errText}`);
        }
        
        const result = await response.json();
        console.log('API Status Response:', JSON.stringify(result, null, 2));
    } catch (error) {
        console.error('Registration script error:', error.message);
    }
}

runRegistration();

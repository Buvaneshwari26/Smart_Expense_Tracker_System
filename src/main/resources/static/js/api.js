// Initialize theme before rendering
(function() {
  const theme = localStorage.getItem('theme') || 'dark';
  document.documentElement.setAttribute('data-theme', theme);
})();

const API_BASE = 'http://localhost:8080/api';

const Api = {
  getToken() { return localStorage.getItem('accessToken'); },
  getUserId() { return localStorage.getItem('userId'); },
  
  async request(endpoint, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = this.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    
    const config = { method, headers };
    if (body) config.body = JSON.stringify(body);
    
    const response = await fetch(`${API_BASE}${endpoint}`, config);
    
    if (response.status === 401) {
      // Try refresh token
      const refreshed = await this.refreshToken();
      if (refreshed) return this.request(endpoint, method, body);
      localStorage.clear();
      window.location.href = 'login.html';
      throw new Error('Session expired');
    }
    
    if (response.status === 204) return null;
    
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || data.data?.message || 'Request failed');
    return data.data !== undefined ? data.data : data;
  },
  
  async refreshToken() {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) return false;
      const res = await fetch(`${API_BASE}/auth/refresh-token?refreshToken=${refreshToken}`, { method: 'POST' });
      if (!res.ok) return false;
      const data = await res.json();
      const d = data.data || data;
      localStorage.setItem('accessToken', d.accessToken);
      return true;
    } catch { return false; }
  },
  
  get(endpoint) { return this.request(endpoint, 'GET'); },
  post(endpoint, body) { return this.request(endpoint, 'POST', body); },
  put(endpoint, body) { return this.request(endpoint, 'PUT', body); },
  patch(endpoint, body) { return this.request(endpoint, 'PATCH', body); },
  del(endpoint) { return this.request(endpoint, 'DELETE'); },
  
  download(endpoint, filename) {
    const token = this.getToken();
    return fetch(`${API_BASE}${endpoint}`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    })
    .then(res => res.blob())
    .then(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = filename; a.click();
      URL.revokeObjectURL(url);
    });
  }
};

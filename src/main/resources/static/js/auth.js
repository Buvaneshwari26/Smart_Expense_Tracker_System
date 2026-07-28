const Auth = {
  async login(email, password) {
    const data = await Api.post('/auth/login', { email, password });
    this.saveSession(data);
    return data;
  },
  async register(payload) {
    const data = await Api.post('/auth/register', payload);
    this.saveSession(data);
    return data;
  },
  saveSession(data) {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('userId', data.userId);
    localStorage.setItem('username', data.username);
    localStorage.setItem('fullName', data.fullName || data.username || '');
    localStorage.setItem('email', data.email);
    localStorage.setItem('role', data.role);
  },
  logout() {
    localStorage.clear();
    window.location.href = 'login.html';
  },
  isLoggedIn() {
    return !!localStorage.getItem('accessToken');
  },
  requireAuth() {
    if (!this.isLoggedIn()) window.location.href = 'login.html';
  },
  requireRole(role) {
    if (!this.isLoggedIn()) { window.location.href = 'login.html'; return; }
    const userRole = localStorage.getItem('role');
    if (userRole !== role) { window.location.href = 'dashboard.html'; }
  },
  isAdmin() {
    return localStorage.getItem('role') === 'ADMIN';
  },
  isUser() {
    return localStorage.getItem('role') === 'USER';
  },
  isAnalyst() {
    return localStorage.getItem('role') === 'ANALYST';
  },
  isReadOnly() {
    // ANALYST role is read-only (cannot add/edit/delete)
    return localStorage.getItem('role') === 'ANALYST';
  },
  getUser() {
    return {
      userId: localStorage.getItem('userId'),
      username: localStorage.getItem('username'),
      fullName: localStorage.getItem('fullName'),
      email: localStorage.getItem('email'),
      role: localStorage.getItem('role')
    };
  }
};

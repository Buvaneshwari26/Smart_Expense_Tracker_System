/**
 * Auth — Session management and Role-Based Access Control (RBAC) helpers.
 *
 * Roles stored in localStorage after login:
 *   ADMIN   — Full access
 *   USER    — Own data only
 *   ANALYST — Read-only (dashboard, reports)
 *   AUDITOR — Read-only (all data, audit logs)
 */
const Auth = {

  // ─── Login / Register ────────────────────────────────────────────────────

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

  // ─── Session ─────────────────────────────────────────────────────────────

  saveSession(data) {
    localStorage.setItem('accessToken',  data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('userId',       String(data.userId));
    localStorage.setItem('username',     data.username  || '');
    localStorage.setItem('email',        data.email     || '');
    localStorage.setItem('role',         data.role      || 'USER');
    localStorage.setItem('fullName',     data.fullName  || data.username || '');
  },

  /**
   * Sync profile fields to localStorage after a profile update.
   * Called from profile.html after a successful API update.
   */
  syncProfile(profileDTO) {
    if (profileDTO.username)    localStorage.setItem('username',  profileDTO.username);
    if (profileDTO.email)       localStorage.setItem('email',     profileDTO.email);
    if (profileDTO.fullName)    localStorage.setItem('fullName',  profileDTO.fullName);
    if (profileDTO.phoneNumber) localStorage.setItem('phone',     profileDTO.phoneNumber);
    if (profileDTO.role)        localStorage.setItem('role',      profileDTO.role);

    // Determine the best display name
    const displayName = profileDTO.fullName || profileDTO.username || '';

    // ── 1. Update sidebar footer username ──────────────────────────────────
    const sidebarEl = document.getElementById('sidebar-username');
    if (sidebarEl && displayName) sidebarEl.textContent = displayName;

    // ── 2. Update header greeting h2 (injected by UI.initHeader) ──────────
    const greetingEl = document.getElementById('header-greeting');
    if (greetingEl && displayName) {
      const hours = new Date().getHours();
      const greeting = hours < 12 ? 'Good Morning' : (hours < 17 ? 'Good Afternoon' : 'Good Evening');
      greetingEl.textContent = `${greeting}, ${displayName} \u{1F44B}`;
    }

    // ── 3. Update profile card name (on profile.html) ─────────────────────
    const profileNameEl = document.getElementById('profile-name');
    if (profileNameEl && displayName) profileNameEl.textContent = displayName;

    // ── 4. Update avatar initial ───────────────────────────────────────────
    const avatarEl = document.getElementById('avatar-display');
    if (avatarEl && displayName) avatarEl.textContent = displayName[0].toUpperCase();
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

  getUser() {
    return {
      userId:   localStorage.getItem('userId'),
      username: localStorage.getItem('username'),
      email:    localStorage.getItem('email'),
      role:     localStorage.getItem('role')  || 'USER',
      fullName: localStorage.getItem('fullName') || localStorage.getItem('username') || ''
    };
  },

  // ─── RBAC Helpers ─────────────────────────────────────────────────────────

  getRole() {
    return (localStorage.getItem('role') || 'USER').toUpperCase();
  },

  isAdmin()   { return this.getRole() === 'ADMIN'; },
  isUser()    { return this.getRole() === 'USER'; },
  isAnalyst() { return this.getRole() === 'ANALYST'; },
  isAuditor() { return this.getRole() === 'AUDITOR'; },

  /** Returns true if the current role can create/update/delete records. */
  canWrite() {
    return this.isAdmin() || this.isUser();
  },

  /** Returns true if the current role has read-only access (ANALYST or AUDITOR). */
  isReadOnly() {
    return this.isAnalyst() || this.isAuditor();
  },

  /**
   * Hide all elements matching the selector for read-only roles.
   * Call this on page load to remove write buttons for ANALYST/AUDITOR.
   *
   * @param {string} selector - CSS selector to hide (e.g. '.write-only-btn')
   */
  enforceReadOnly(selector = '.write-only') {
    if (this.isReadOnly()) {
      document.querySelectorAll(selector).forEach(el => el.remove());
    }
  },

  /**
   * Redirect to dashboard if current user does NOT have one of the allowed roles.
   * @param {...string} roles - e.g. 'ADMIN', 'AUDITOR'
   */
  requireRole(...roles) {
    const r = this.getRole();
    if (!roles.includes(r)) {
      UI.showToast('Access denied: insufficient permissions.', 'error');
      setTimeout(() => window.location.href = 'dashboard.html', 1500);
    }
  }
};

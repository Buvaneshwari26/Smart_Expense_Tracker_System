const UI = {
  DEFAULT_AVATAR: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iIzg4OTJhNCI+PGNpcmNsZSBjeD0iMTIiIGN5PSIxMiIgcj0iMTIiIGZpbGw9IiMxZTI5M2IiLz48cGF0aCBkPSJNMTIgMTJjMi4yMSAwIDQtMS43OSA0LTRzLTEuNzktNC00LTQtNCAxLjc5LTQgNCAxLjc5IDQgNCA0em0wIDJjLTIuNjcgMC04IDEuMzQtOCA0djJoMTZ2LTJjMC0yLjY2LTUuMzMtNC04LTR6IiBmaWxsPSIjNjQ3NDhiIi8+PC9zdmc+',

  showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span>${type === 'success' ? '✅' : type === 'warning' ? '⚠️' : '❌'} ${message}</span><button onclick="this.parentElement.remove()">&times;</button>`;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
  },

  showLoading() {
    let overlay = document.querySelector('.loading-overlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.className = 'loading-overlay';
      overlay.innerHTML = '<div class="spinner-border text-light" style="width:3rem;height:3rem"></div>';
      document.body.appendChild(overlay);
    }
    overlay.style.display = 'flex';
  },

  hideLoading() {
    const overlay = document.querySelector('.loading-overlay');
    if (overlay) overlay.style.display = 'none';
  },

  formatCurrency(amount) {
    return '₹' + Number(amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  },

  formatDate(dateStr) {
    if (!dateStr) return '-';
    const parsed = Date.parse(dateStr);
    if (isNaN(parsed)) return dateStr;
    return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  },

  formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const parsed = Date.parse(dateStr);
    if (isNaN(parsed)) return dateStr;
    return new Date(dateStr).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  },

  /**
   * Show a skeleton loading placeholder in a table body.
   */
  showSkeletonTable(tbody, rows = 5, cols = 6) {
    if (!tbody) return;
    let html = '';
    for (let r = 0; r < rows; r++) {
      html += '<tr>';
      for (let c = 0; c < cols; c++) {
        html += `<td><div style="height:16px;border-radius:4px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.08) 50%,rgba(255,255,255,0.03) 75%);background-size:200% 100%;animation:loading-shimmer 1.5s infinite;"></div></td>`;
      }
      html += '</tr>';
    }
    tbody.innerHTML = html;
  },

  /**
   * Show skeleton card placeholders.
   */
  showSkeletonCards(container, count = 3) {
    if (!container) return;
    let html = '';
    for (let i = 0; i < count; i++) {
      html += `
        <div class="col-md-4">
          <div class="glass-card" style="min-height:180px;">
            <div style="height:20px;width:60%;border-radius:4px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.08) 50%,rgba(255,255,255,0.03) 75%);background-size:200% 100%;animation:loading-shimmer 1.5s infinite;margin-bottom:12px;"></div>
            <div style="height:14px;width:80%;border-radius:4px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.08) 50%,rgba(255,255,255,0.03) 75%);background-size:200% 100%;animation:loading-shimmer 1.5s infinite;margin-bottom:8px;"></div>
            <div style="height:14px;width:50%;border-radius:4px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.08) 50%,rgba(255,255,255,0.03) 75%);background-size:200% 100%;animation:loading-shimmer 1.5s infinite;"></div>
          </div>
        </div>`;
    }
    container.innerHTML = html;
  },

  /**
   * Inject and initialize Sticky Top Header, Breadcrumbs, Theme Switcher, and Profile Dropdown
   */
  initHeader(activePage) {
    const user = Auth.getUser();
    if (!user || !user.userId) return;

    // 1. Theme initialization
    const currentTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', currentTheme);

    const mainContent = document.querySelector('.main-content');
    if (!mainContent || document.getElementById('sticky-top-header')) return;

    // Map page titles for breadcrumbs
    const titleMap = {
      'dashboard.html': 'Dashboard',
      'income.html': 'Income Management',
      'expense.html': 'Expense Management',
      'category.html': 'Category Management',
      'budget.html': 'Budget Planner',
      'savings.html': 'Savings Goals',
      'reports.html': 'Reports & Analytics',
      'notifications.html': 'Notifications',
      'profile.html': 'User Profile',
      'admin.html': 'Admin Panel',
      'admin-users.html': 'User Management'
    };

    const currentPageName = titleMap[activePage] || 'Overview';
    const storedPic = localStorage.getItem('profilePicture') || this.DEFAULT_AVATAR;
    const displayName = localStorage.getItem('fullName') || user.username || 'User';
    const roleBadge = user.role === 'ADMIN' ? 'Admin' : (user.role === 'ANALYST' ? 'Analyst' : 'User');

    // Create top header bar
    const topHeader = document.createElement('header');
    topHeader.id = 'sticky-top-header';
    topHeader.className = 'top-header';
    topHeader.innerHTML = `
      <div class="top-header-left">
        <div class="breadcrumb-container">
          <span class="breadcrumb-item"><a href="dashboard.html"><i class="bi bi-house"></i> Home</a></span>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item active">${currentPageName}</span>
        </div>
      </div>
      <div class="top-header-right">
        <div class="global-search-box">
          <i class="bi bi-search"></i>
          <input type="text" id="global-search-input" placeholder="Search page..." oninput="UI.handleGlobalSearch(this.value)">
        </div>

        <button class="header-action-btn" id="theme-toggle-btn" title="Toggle Light/Dark Theme" onclick="UI.toggleTheme()">
          <i class="bi ${currentTheme === 'light' ? 'bi-moon-stars' : 'bi-sun'}"></i>
        </button>

        <a href="notifications.html" class="header-action-btn" title="Notifications">
          <i class="bi bi-bell"></i>
          <span class="notif-badge-pill" id="header-notif-count" style="display:none;">0</span>
        </a>

        <div class="user-profile-menu">
          <div class="user-profile-btn" onclick="UI.toggleUserDropdown(event)">
            <img src="${storedPic}" class="rounded-circle user-avatar-img" style="width:28px;height:28px;object-fit:cover;" onerror="this.src=UI.DEFAULT_AVATAR">
            <span class="small fw-semibold text-truncate" style="max-width:100px;">${displayName}</span>
            <i class="bi bi-chevron-down small opacity-75"></i>
          </div>
          <div class="user-profile-dropdown" id="user-profile-dropdown">
            <div class="px-3 py-2 border-bottom border-secondary border-opacity-10 mb-1">
              <div class="fw-bold small text-truncate">${displayName}</div>
              <div class="text-secondary small" style="font-size:0.75rem;">Role: ${roleBadge}</div>
            </div>
            <a href="profile.html"><i class="bi bi-person me-2"></i>My Profile</a>
            <a href="notifications.html"><i class="bi bi-bell me-2"></i>Notifications</a>
            <div class="dropdown-divider border-secondary border-opacity-10 my-1"></div>
            <button onclick="Auth.logout()"><i class="bi bi-box-arrow-right me-2 text-danger"></i>Logout</button>
          </div>
        </div>
      </div>
    `;

    mainContent.insertBefore(topHeader, mainContent.firstChild);

    // Fetch unread notification count asynchronously
    Api.get(`/notifications?userId=${user.userId}`).then(data => {
      if (data && data.content) {
        const unread = data.content.filter(n => !n.read && !n.isRead).length;
        const countEl = document.getElementById('header-notif-count');
        if (countEl && unread > 0) {
          countEl.textContent = unread > 99 ? '99+' : unread;
          countEl.style.display = 'flex';
        }
      }
    }).catch(() => {});

    // Close user menu when clicking outside
    document.addEventListener('click', (e) => {
      const menu = document.getElementById('user-profile-dropdown');
      if (menu && !e.target.closest('.user-profile-menu')) {
        menu.classList.remove('show');
      }
    });
  },

  toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);

    const btn = document.getElementById('theme-toggle-btn');
    if (btn) {
      btn.innerHTML = `<i class="bi ${newTheme === 'light' ? 'bi-moon-stars' : 'bi-sun'}"></i>`;
    }
  },

  toggleUserDropdown(event) {
    event.stopPropagation();
    const dropdown = document.getElementById('user-profile-dropdown');
    if (dropdown) dropdown.classList.toggle('show');
  },

  handleGlobalSearch(query) {
    const q = (query || '').toLowerCase().trim();
    const tableBody = document.querySelector('tbody');
    if (!tableBody) return;
    
    const rows = tableBody.querySelectorAll('tr');
    rows.forEach(row => {
      const text = row.textContent.toLowerCase();
      row.style.display = text.includes(q) ? '' : 'none';
    });
  },

  renderPagination(container, pageData, onPageChange) {
    container.innerHTML = '';
    if (!pageData || pageData.totalPages <= 1) return;
    const nav = document.createElement('nav');
    nav.innerHTML = '<ul class="pagination pagination-custom"></ul>';
    const ul = nav.querySelector('ul');

    ul.innerHTML += `<li class="page-item ${pageData.first ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number - 1}">&laquo;</a></li>`;

    for (let i = 0; i < pageData.totalPages; i++) {
      ul.innerHTML += `<li class="page-item ${i === pageData.number ? 'active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
    }

    ul.innerHTML += `<li class="page-item ${pageData.last ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number + 1}">&raquo;</a></li>`;

    ul.querySelectorAll('.page-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const page = parseInt(link.dataset.page);
        if (page >= 0 && page < pageData.totalPages) onPageChange(page);
      });
    });
    container.appendChild(nav);
  },

  /**
   * initSidebar(currentPage)
   *
   * Injects role-aware navigation links into #sidebar-nav (if present),
   * then sets the active class and username/avatar.
   * ANALYST role gets a read-only badge.
   */
  initSidebar(activePage) {
    const user = Auth.getUser();
    const currentPage = activePage || window.location.pathname.split('/').pop() || 'dashboard.html';

    // Inject sticky top header
    this.initHeader(currentPage);

    // ── Inject nav links into dynamic sidebar containers ──────────────────
    const navContainer = document.getElementById('sidebar-nav');
    if (navContainer) {
      // Base links available to all roles
      const baseLinks = [
        { href: 'dashboard.html',      icon: 'bi-grid-1x2',     label: 'Dashboard' },
        { href: 'income.html',         icon: 'bi-cash-stack',    label: 'Income' },
        { href: 'expense.html',        icon: 'bi-credit-card',   label: 'Expenses' },
        { href: 'category.html',       icon: 'bi-tags',          label: 'Categories' },
        { href: 'budget.html',         icon: 'bi-pie-chart',     label: 'Budgets' },
        { href: 'savings.html',        icon: 'bi-piggy-bank',    label: 'Savings Goals' },
        { href: 'reports.html',        icon: 'bi-graph-up',      label: 'Reports' },
        { href: 'notifications.html',  icon: 'bi-bell',          label: 'Notifications' },
        { href: 'profile.html',        icon: 'bi-person-circle', label: 'Profile' },
      ];

      // Admin-only links
      const adminLinks = [
        { href: 'admin.html',       icon: 'bi-shield-fill',  label: 'Admin Panel',      adminOnly: true },
        { href: 'admin-users.html', icon: 'bi-people-fill',  label: 'User Management',  adminOnly: true },
      ];

      const allLinks = user.role === 'ADMIN'
        ? [...baseLinks, ...adminLinks]
        : baseLinks;

      navContainer.innerHTML = allLinks.map(link => {
        const isActive = link.href === currentPage ? ' active' : '';
        const adminBadge = link.adminOnly
          ? ' <span style="font-size:0.65rem;background:rgba(99,102,241,0.25);color:#818cf8;padding:1px 5px;border-radius:4px;margin-left:4px;">Admin</span>'
          : '';
        return `<a href="${link.href}" class="nav-link${isActive}">
          <i class="bi ${link.icon}"></i>
          <span>${link.label}${adminBadge}</span>
        </a>`;
      }).join('');

      // Show ANALYST read-only banner if applicable
      if (user.role === 'ANALYST') {
        const banner = document.createElement('div');
        banner.style.cssText = 'margin:8px 12px;padding:8px 12px;background:rgba(245,158,11,0.12);border:1px solid rgba(245,158,11,0.3);border-radius:8px;font-size:0.75rem;color:#fbbf24;display:flex;align-items:center;gap:6px;';
        banner.innerHTML = '<i class="bi bi-eye"></i><span>Read-Only Access</span>';
        navContainer.appendChild(banner);
      }
    }

    // ── Mobile sidebar toggle ─────────────────────────────────────────────
    const toggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggle && sidebar) {
      toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    }

    // ── Activate current nav link (static nav fallback) ───────────────────
    document.querySelectorAll('.nav-link').forEach(link => {
      if (link.getAttribute('href') === currentPage) link.classList.add('active');
    });

    // ── Set username & avatar in sidebar footer ───────────────────────────
    const footer = document.querySelector('.sidebar-footer');
    if (footer && user.userId) {
      const storedPic = localStorage.getItem('profilePicture') || this.DEFAULT_AVATAR;
      const displayName = localStorage.getItem('fullName') || user.username || 'User';
      footer.innerHTML = `
        <div class="d-flex align-items-center gap-2" style="max-width: 140px;">
          <img src="${storedPic}" class="user-avatar-img rounded-circle border border-secondary" style="width:32px;height:32px;object-fit:cover;" onerror="this.src=UI.DEFAULT_AVATAR">
          <span id="sidebar-username" class="text-truncate text-white fw-medium">${displayName}</span>
        </div>
        <button onclick="Auth.logout()" class="btn btn-sm btn-outline-danger border-0 p-1" title="Logout"><i class="bi bi-box-arrow-right"></i></button>
      `;

      // Fetch profile asynchronously to refresh picture and name in real-time
      Api.get(`/users/${user.userId}`).then(data => {
        if (data.profilePicture) {
          localStorage.setItem('profilePicture', data.profilePicture);
          const imgs = document.querySelectorAll('.user-avatar-img');
          imgs.forEach(img => img.src = data.profilePicture);
        } else {
          localStorage.removeItem('profilePicture');
          document.querySelectorAll('.user-avatar-img').forEach(img => img.src = this.DEFAULT_AVATAR);
        }
        const name = data.fullName || data.username;
        if (name) {
          localStorage.setItem('fullName', name);
          localStorage.setItem('username', data.username || name);
          const nameEl = document.getElementById('sidebar-username');
          if (nameEl) nameEl.textContent = name;
        }
      }).catch(err => console.warn('Sidebar profile sync deferred: ' + err.message));
    } else {
      const el = document.getElementById('sidebar-username');
      if (el) el.textContent = user.username || 'User';
    }

    // Automatically enforce read-only UI for ANALYST role
    this.applyReadOnlyMode();
  },

  /**
   * Enforce read-only mode for ANALYST users by hiding edit/delete/add controls
   */
  applyReadOnlyMode() {
    if (!Auth.isReadOnly()) return;
    
    // Add banner at top of main-content if not present
    const main = document.querySelector('.main-content');
    if (main && !document.getElementById('readonly-alert')) {
      const alert = document.createElement('div');
      alert.id = 'readonly-alert';
      alert.className = 'alert alert-warning d-flex align-items-center mb-4';
      alert.style.cssText = 'background: rgba(245, 158, 11, 0.15); border: 1px solid rgba(245, 158, 11, 0.3); color: #fbbf24; border-radius: 10px;';
      alert.innerHTML = '<i class="bi bi-eye-fill me-2 fs-5"></i><span>You are in <strong>ANALYST (Read-Only)</strong> mode. Viewing and exporting are allowed; modifying data is restricted.</span>';
      main.insertBefore(alert, main.firstChild);
    }

    // Hide create/edit/delete/save buttons
    document.querySelectorAll('.btn-glow, [onclick*="openModal"], [onclick*="save"], button[type="submit"]').forEach(el => {
      const text = (el.textContent || '').trim().toLowerCase();
      if (!text.includes('search') && !text.includes('reset') && !text.includes('export') && !text.includes('excel') && !text.includes('csv') && !text.includes('pdf') && !text.includes('logout') && !text.includes('filter')) {
        el.style.display = 'none';
      }
    });
  }
};

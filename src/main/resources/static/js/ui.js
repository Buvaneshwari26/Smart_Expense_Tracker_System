/* ═══════════════════════════════════════════════════════════════
   UI.JS — Enterprise UI Helper Library v3.0
   ═══════════════════════════════════════════════════════════════ */

const UI = {
  DEFAULT_AVATAR: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSI+PGNpcmNsZSBjeD0iMTIiIGN5PSIxMiIgcj0iMTIiIGZpbGw9IiMxZTI5M2IiLz48cGF0aCBkPSJNMTIgMTJjMi4yMSAwIDQtMS43OSA0LTRzLTEuNzktNC00LTQtNCAxLjc5LTQgNCAxLjc5IDQgNCA0em0wIDJjLTIuNjcgMC04IDEuMzQtOCA0djJoMTZ2LTJjMC0yLjY2LTUuMzMtNC04LTR6IiBmaWxsPSIjNDc1NTY5Ii8+PC9zdmc+',

  /* ── Toast Notifications ─────────────────────────────────────── */
  showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <span style="display:flex;align-items:center;gap:10px;">
        <span style="font-size:1rem;">${icons[type] || icons.success}</span>
        <span style="font-size:0.875rem;font-weight:500;">${message}</span>
      </span>
      <button onclick="this.parentElement.remove()" style="flex-shrink:0;">&times;</button>
    `;
    container.appendChild(toast);
    setTimeout(() => { toast.style.animation = 'none'; toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, 4000);
  },

  /* ── Global Loading Overlay ──────────────────────────────────── */
  showLoading() {
    let overlay = document.querySelector('.loading-overlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.className = 'loading-overlay';
      overlay.innerHTML = '<div class="loading-spinner-ring"></div>';
      document.body.appendChild(overlay);
    }
    overlay.style.display = 'flex';
  },

  hideLoading() {
    const overlay = document.querySelector('.loading-overlay');
    if (overlay) overlay.style.display = 'none';
  },

  /* ── Formatters ──────────────────────────────────────────────── */
  formatCurrency(amount) {
    return '₹' + Number(amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  },

  formatDate(dateStr) {
    if (!dateStr) return '—';
    const parsed = Date.parse(dateStr);
    if (isNaN(parsed)) return dateStr;
    return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  },

  formatDateTime(dateStr) {
    if (!dateStr) return '—';
    const parsed = Date.parse(dateStr);
    if (isNaN(parsed)) return dateStr;
    return new Date(dateStr).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  },

  /* ── Skeleton Table ──────────────────────────────────────────── */
  showSkeletonTable(tbody, rows = 5, cols = 6) {
    if (!tbody) return;
    let html = '';
    for (let r = 0; r < rows; r++) {
      html += '<tr>';
      for (let c = 0; c < cols; c++) {
        html += `<td><div style="height:14px;border-radius:6px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.07) 50%,rgba(255,255,255,0.03) 75%);background-size:400% 100%;animation:shimmer 1.6s ease infinite;width:${60+Math.random()*30}%"></div></td>`;
      }
      html += '</tr>';
    }
    tbody.innerHTML = html;
  },

  /* ── Skeleton Cards ──────────────────────────────────────────── */
  showSkeletonCards(container, count = 3) {
    if (!container) return;
    let html = '';
    for (let i = 0; i < count; i++) {
      html += `<div class="col-md-4">
        <div class="glass-card" style="min-height:180px;">
          <div style="height:18px;width:55%;border-radius:6px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.07) 50%,rgba(255,255,255,0.03) 75%);background-size:400% 100%;animation:shimmer 1.6s ease infinite;margin-bottom:14px;"></div>
          <div style="height:13px;width:80%;border-radius:6px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.07) 50%,rgba(255,255,255,0.03) 75%);background-size:400% 100%;animation:shimmer 1.6s ease infinite;margin-bottom:10px;"></div>
          <div style="height:13px;width:50%;border-radius:6px;background:linear-gradient(90deg,rgba(255,255,255,0.03) 25%,rgba(255,255,255,0.07) 50%,rgba(255,255,255,0.03) 75%);background-size:400% 100%;animation:shimmer 1.6s ease infinite;"></div>
        </div>
      </div>`;
    }
    container.innerHTML = html;
  },

  /* ── Sticky Top Header ───────────────────────────────────────── */
  initHeader(activePage) {
    const user = Auth.getUser();
    if (!user || !user.userId) return;

    // Restore saved theme
    const currentTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', currentTheme);

    const mainContent = document.querySelector('.main-content');
    if (!mainContent || document.getElementById('sticky-top-header')) return;

    const titleMap = {
      'dashboard.html': 'Dashboard',
      'income.html': 'Income Management',
      'expense.html': 'Expense Management',
      'category.html': 'Category Management',
      'budget.html': 'Budget Planner',
      'savings.html': 'Savings Goals',
      'reports.html': 'Reports & Analytics',
      'notifications.html': 'Notifications',
      'profile.html': 'My Profile',
      'admin.html': 'Admin Panel',
      'admin-users.html': 'User Management'
    };

    const currentPageName = titleMap[activePage] || 'Overview';
    const storedPic = localStorage.getItem('profilePicture') || this.DEFAULT_AVATAR;
    const displayName = localStorage.getItem('fullName') || user.username || 'User';
    const roleBadgeColor = user.role === 'ADMIN' ? '#818cf8' : (user.role === 'ANALYST' ? '#fcd34d' : '#6ee7b7');
    const roleBadgeLabel = user.role === 'ADMIN' ? 'Admin' : (user.role === 'ANALYST' ? 'Analyst' : 'User');

    const topHeader = document.createElement('header');
    topHeader.id = 'sticky-top-header';
    topHeader.className = 'top-header';
    topHeader.innerHTML = `
      <div class="top-header-left">
        <div class="breadcrumb-container">
          <span class="breadcrumb-item">
            <a href="dashboard.html"><i class="bi bi-house-fill"></i> Home</a>
          </span>
          <span class="breadcrumb-separator"><i class="bi bi-chevron-right"></i></span>
          <span class="breadcrumb-item active">${currentPageName}</span>
        </div>
      </div>

      <div class="top-header-right">
        <div class="global-search-box">
          <i class="bi bi-search"></i>
          <input type="text" id="global-search-input" placeholder="Search on this page…" oninput="UI.handleGlobalSearch(this.value)" autocomplete="off">
        </div>

        <button class="header-action-btn" id="theme-toggle-btn" title="Toggle Theme" onclick="UI.toggleTheme()">
          <i class="bi ${currentTheme === 'light' ? 'bi-moon-stars-fill' : 'bi-sun-fill'}"></i>
        </button>

        <a href="notifications.html" class="header-action-btn" id="notif-btn" title="Notifications" style="text-decoration:none;">
          <i class="bi bi-bell-fill"></i>
          <span class="notif-badge-pill" id="header-notif-count" style="display:none;">0</span>
        </a>

        <div class="user-profile-menu">
          <div class="user-profile-btn" onclick="UI.toggleUserDropdown(event)" id="profile-trigger">
            <img src="${storedPic}" class="rounded-circle user-avatar-img" style="width:28px;height:28px;object-fit:cover;border:2px solid rgba(255,255,255,0.15);" onerror="this.src=UI.DEFAULT_AVATAR">
            <span style="font-size:0.82rem;font-weight:600;max-width:100px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${displayName}</span>
            <span style="font-size:0.65rem;font-weight:700;padding:2px 7px;border-radius:20px;background:rgba(255,255,255,0.08);color:${roleBadgeColor};">${roleBadgeLabel}</span>
            <i class="bi bi-chevron-down" style="font-size:0.7rem;opacity:0.6;"></i>
          </div>
          <div class="user-profile-dropdown" id="user-profile-dropdown">
            <div style="padding:12px 14px 10px;border-bottom:1px solid var(--glass-border);margin-bottom:4px;">
              <div style="font-weight:700;font-size:0.875rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${displayName}</div>
              <div style="font-size:0.75rem;color:var(--text-muted);margin-top:2px;">${user.email || ''}</div>
            </div>
            <a href="profile.html"><i class="bi bi-person-circle"></i> My Profile</a>
            <a href="notifications.html"><i class="bi bi-bell"></i> Notifications</a>
            <div class="dropdown-divider"></div>
            <button onclick="Auth.logout()" style="color:var(--danger)!important;"><i class="bi bi-box-arrow-right"></i> Sign Out</button>
          </div>
        </div>
      </div>
    `;

    mainContent.insertBefore(topHeader, mainContent.firstChild);

    // Fetch unread notification count
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

    // Close dropdown on outside click
    document.addEventListener('click', (e) => {
      const menu = document.getElementById('user-profile-dropdown');
      if (menu && !e.target.closest('.user-profile-menu')) {
        menu.classList.remove('show');
      }
    });
  },

  /* ── Theme Toggle ─────────────────────────────────────────────── */
  toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);

    const btn = document.getElementById('theme-toggle-btn');
    if (btn) {
      btn.innerHTML = `<i class="bi ${newTheme === 'light' ? 'bi-moon-stars-fill' : 'bi-sun-fill'}"></i>`;
    }
  },

  /* ── User Dropdown ────────────────────────────────────────────── */
  toggleUserDropdown(event) {
    event.stopPropagation();
    const dropdown = document.getElementById('user-profile-dropdown');
    if (dropdown) dropdown.classList.toggle('show');
  },

  /* ── Global Search ────────────────────────────────────────────── */
  handleGlobalSearch(query) {
    const q = (query || '').toLowerCase().trim();
    const tableBody = document.querySelector('tbody');
    if (!tableBody) return;
    tableBody.querySelectorAll('tr').forEach(row => {
      row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
  },

  /* ── Pagination ───────────────────────────────────────────────── */
  renderPagination(container, pageData, onPageChange) {
    if (!container) return;
    container.innerHTML = '';
    if (!pageData || pageData.totalPages <= 1) return;
    const nav = document.createElement('nav');
    nav.setAttribute('aria-label', 'Pagination');
    const ul = document.createElement('ul');
    ul.className = 'pagination pagination-custom mb-0';

    const makeItem = (label, page, disabled, active) => {
      const li = document.createElement('li');
      li.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;
      const a = document.createElement('a');
      a.className = 'page-link';
      a.href = '#';
      a.innerHTML = label;
      if (!disabled) {
        a.addEventListener('click', (e) => {
          e.preventDefault();
          if (page >= 0 && page < pageData.totalPages) onPageChange(page);
        });
      }
      li.appendChild(a);
      return li;
    };

    ul.appendChild(makeItem('<i class="bi bi-chevron-left"></i>', pageData.number - 1, pageData.first, false));
    for (let i = 0; i < pageData.totalPages; i++) {
      if (pageData.totalPages > 7 && Math.abs(i - pageData.number) > 2 && i !== 0 && i !== pageData.totalPages - 1) {
        if (i === 1 || i === pageData.totalPages - 2) {
          const li = document.createElement('li');
          li.className = 'page-item disabled';
          li.innerHTML = '<span class="page-link">…</span>';
          ul.appendChild(li);
        }
        continue;
      }
      ul.appendChild(makeItem(i + 1, i, false, i === pageData.number));
    }
    ul.appendChild(makeItem('<i class="bi bi-chevron-right"></i>', pageData.number + 1, pageData.last, false));
    nav.appendChild(ul);
    container.appendChild(nav);
  },

  /* ── Sidebar Initialization ───────────────────────────────────── */
  initSidebar(activePage) {
    const user = Auth.getUser();
    const currentPage = activePage || window.location.pathname.split('/').pop() || 'dashboard.html';

    // Apply theme first
    const currentTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', currentTheme);

    // Inject top sticky header
    this.initHeader(currentPage);

    // ── Populate sidebar nav ────────────────────────────────────
    const navContainer = document.getElementById('sidebar-nav');
    if (navContainer) {
      const baseLinks = [
        { href: 'dashboard.html',     icon: 'bi-grid-1x2-fill',   label: 'Dashboard' },
        { href: 'income.html',        icon: 'bi-cash-stack',       label: 'Income' },
        { href: 'expense.html',       icon: 'bi-credit-card-fill', label: 'Expenses' },
        { href: 'category.html',      icon: 'bi-tags-fill',        label: 'Categories' },
        { href: 'budget.html',        icon: 'bi-pie-chart-fill',   label: 'Budgets' },
        { href: 'savings.html',       icon: 'bi-piggy-bank-fill',  label: 'Savings Goals' },
        { href: 'reports.html',       icon: 'bi-graph-up-arrow',   label: 'Reports' },
        { href: 'notifications.html', icon: 'bi-bell-fill',        label: 'Notifications' },
        { href: 'profile.html',       icon: 'bi-person-circle',    label: 'My Profile' },
      ];

      const adminLinks = [
        { href: 'admin.html',       icon: 'bi-shield-fill-check', label: 'Admin Panel',   adminOnly: true },
        { href: 'admin-users.html', icon: 'bi-people-fill',       label: 'User Management', adminOnly: true },
      ];

      const allLinks = user.role === 'ADMIN' ? [...baseLinks, ...adminLinks] : baseLinks;

      let navHtml = '';
      let inAdminSection = false;
      allLinks.forEach(link => {
        if (link.adminOnly && !inAdminSection) {
          inAdminSection = true;
          navHtml += '<div class="sidebar-section-label">Administration</div>';
        }
        const isActive = link.href === currentPage;
        navHtml += `<a href="${link.href}" class="nav-link${isActive ? ' active' : ''}">
          <i class="bi ${link.icon}"></i>
          <span>${link.label}</span>
        </a>`;
      });

      navContainer.innerHTML = navHtml;

      // ANALYST read-only badge
      if (user.role === 'ANALYST') {
        const badge = document.createElement('div');
        badge.style.cssText = 'margin:10px 8px 4px;padding:8px 12px;background:rgba(245,158,11,0.1);border:1px solid rgba(245,158,11,0.25);border-radius:8px;font-size:0.72rem;color:#fcd34d;display:flex;align-items:center;gap:6px;font-weight:600;';
        badge.innerHTML = '<i class="bi bi-eye-fill"></i><span>Read-Only Access</span>';
        navContainer.appendChild(badge);
      }
    }

    // ── Mobile sidebar toggle ───────────────────────────────────
    const toggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggle && sidebar) {
      toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
      document.addEventListener('click', (e) => {
        if (sidebar.classList.contains('open') && !e.target.closest('.sidebar') && !e.target.closest('.sidebar-toggle')) {
          sidebar.classList.remove('open');
        }
      });
    }

    // ── Sidebar footer ──────────────────────────────────────────
    const footer = document.querySelector('.sidebar-footer');
    if (footer && user.userId) {
      const storedPic = localStorage.getItem('profilePicture') || this.DEFAULT_AVATAR;
      const displayName = localStorage.getItem('fullName') || user.username || 'User';
      footer.innerHTML = `
        <div style="display:flex;align-items:center;gap:10px;min-width:0;flex:1;">
          <img src="${storedPic}" class="user-avatar-img rounded-circle" style="width:34px;height:34px;object-fit:cover;border:2px solid var(--glass-border);flex-shrink:0;" onerror="this.src=UI.DEFAULT_AVATAR">
          <div style="min-width:0;">
            <div id="sidebar-username" style="font-size:0.82rem;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:120px;">${displayName}</div>
            <div style="font-size:0.68rem;color:var(--text-muted);">${user.role}</div>
          </div>
        </div>
        <button onclick="Auth.logout()" style="background:none;border:none;color:var(--text-muted);cursor:pointer;padding:6px;border-radius:8px;transition:all 0.15s;flex-shrink:0;" title="Sign Out" onmouseenter="this.style.color='var(--danger)'" onmouseleave="this.style.color='var(--text-muted)'">
          <i class="bi bi-box-arrow-right" style="font-size:1.1rem;"></i>
        </button>
      `;

      // Refresh profile data
      Api.get(`/users/${user.userId}`).then(data => {
        if (data.profilePicture) {
          localStorage.setItem('profilePicture', data.profilePicture);
          document.querySelectorAll('.user-avatar-img').forEach(img => img.src = data.profilePicture);
        } else {
          localStorage.removeItem('profilePicture');
          document.querySelectorAll('.user-avatar-img').forEach(img => img.src = this.DEFAULT_AVATAR);
        }
        const name = data.fullName || data.username;
        if (name) {
          localStorage.setItem('fullName', name);
          const nameEl = document.getElementById('sidebar-username');
          if (nameEl) nameEl.textContent = name;
        }
      }).catch(() => {});
    }

    // Enforce ANALYST read-only
    this.applyReadOnlyMode();
  },

  /* ── ANALYST Read-Only Mode ───────────────────────────────────── */
  applyReadOnlyMode() {
    if (!Auth.isReadOnly()) return;

    // Insert read-only alert banner
    const main = document.querySelector('.main-content');
    if (main && !document.getElementById('readonly-alert')) {
      const alert = document.createElement('div');
      alert.id = 'readonly-alert';
      alert.innerHTML = '<i class="bi bi-eye-fill" style="font-size:1rem;"></i><span>You are in <strong>ANALYST (Read-Only)</strong> mode — viewing and exporting are allowed, but data modification is restricted.</span>';
      // Insert after the sticky header if it exists
      const header = document.getElementById('sticky-top-header');
      if (header && header.nextSibling) {
        main.insertBefore(alert, header.nextSibling);
      } else {
        main.insertBefore(alert, main.firstChild);
      }
    }

    // Hide all create/edit/delete/save controls
    const actionTexts = ['search', 'reset', 'export', 'excel', 'csv', 'pdf', 'filter', 'logout', 'sign out'];
    const isActionAllowed = (el) => {
      const text = (el.textContent || el.title || '').trim().toLowerCase();
      return actionTexts.some(t => text.includes(t));
    };

    setTimeout(() => {
      document.querySelectorAll('button, a.btn').forEach(el => {
        if (isActionAllowed(el)) return;
        const onclick = (el.getAttribute('onclick') || '');
        if (onclick.includes('openModal') || onclick.includes('save') || onclick.includes('deleteItem') || onclick.includes('editItem')) {
          el.style.display = 'none';
        }
      });

      // Also hide Add buttons by checking btn-glow
      document.querySelectorAll('.btn-glow').forEach(el => {
        if (!isActionAllowed(el)) el.style.display = 'none';
      });
    }, 150);
  }
};

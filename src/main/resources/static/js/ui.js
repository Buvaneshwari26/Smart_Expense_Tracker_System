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
    toast.innerHTML = `<span>${type === 'success' ? '✅' : '❌'} ${message}</span><button onclick="this.parentElement.remove()">&times;</button>`;
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
   * @param {HTMLElement} tbody - The tbody element to fill
   * @param {number} rows - Number of skeleton rows
   * @param {number} cols - Number of columns
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
   * Stub for pages that call UI.initHeader().
   * Currently a no-op — sidebar footer handles user display.
   */
  initHeader() {
    // No-op: sidebar footer already injected by initSidebar()
  },

  renderPagination(container, pageData, onPageChange) {
    container.innerHTML = '';
    if (!pageData || pageData.totalPages <= 1) return;
    const nav = document.createElement('nav');
    nav.innerHTML = '<ul class="pagination pagination-custom"></ul>';
    const ul = nav.querySelector('ul');

    // Previous
    ul.innerHTML += `<li class="page-item ${pageData.first ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number - 1}">&laquo;</a></li>`;

    for (let i = 0; i < pageData.totalPages; i++) {
      ul.innerHTML += `<li class="page-item ${i === pageData.number ? 'active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
    }

    // Next
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
   *
   * @param {string} [activePage] - Override the active page filename.
   */
  initSidebar(activePage) {
    const user = Auth.getUser();
    const currentPage = activePage || window.location.pathname.split('/').pop() || 'dashboard.html';

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
  }
};

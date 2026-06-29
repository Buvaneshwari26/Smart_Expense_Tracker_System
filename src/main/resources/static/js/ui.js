const UI = {
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
    return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  },
  
  renderPagination(container, pageData, onPageChange) {
    container.innerHTML = '';
    if (!pageData || pageData.totalPages <= 1) return;
    const nav = document.createElement('nav');
    nav.innerHTML = '<ul class="pagination pagination-custom m-0"></ul>';
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

  initSidebar() {
    const toggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggle && sidebar) {
      toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    }
    // Collapsible sidebar state load
    const isCollapsed = localStorage.getItem('sidebar_collapsed') === 'true';
    if (isCollapsed && sidebar) {
      sidebar.classList.add('collapsed');
    }

    const user = Auth.getUser();
    const role = Auth.getRole();

    // Dynamically inject the Admin Panel navigation link for ADMIN/AUDITOR roles if not already present
    const navContainer = document.querySelector('.sidebar-nav');
    if (navContainer && (role === 'ADMIN' || role === 'AUDITOR')) {
      const hasAdminLink = Array.from(navContainer.querySelectorAll('.nav-link'))
                               .some(link => link.getAttribute('href') === 'admin.html');
      if (!hasAdminLink) {
        const adminLink = document.createElement('a');
        adminLink.href = 'admin.html';
        adminLink.className = 'nav-link';
        adminLink.innerHTML = '<i class="bi bi-shield-lock-fill"></i><span>Admin Panel</span>';
        navContainer.appendChild(adminLink);
      }
    }

    // ── Role-based sidebar nav visibility ──────────────────────────────────
    const rolePages = {
      ADMIN:   ['dashboard.html','income.html','expense.html','category.html','budget.html','savings.html','reports.html','profile.html','admin.html'],
      USER:    ['dashboard.html','income.html','expense.html','category.html','budget.html','savings.html','reports.html','profile.html'],
      ANALYST: ['dashboard.html','reports.html','profile.html'],
      AUDITOR: ['dashboard.html','reports.html','profile.html','admin.html']
    };
    const allowedPages = rolePages[role] || rolePages['USER'];

    // Hide nav links the current role cannot access
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
      const href = link.getAttribute('href');
      if (!allowedPages.includes(href)) {
        link.style.display = 'none';
      } else {
        link.style.display = 'flex'; // Ensure allowed ones are visible
      }
    });

    // Set active nav link
    const currentPage = window.location.pathname.split('/').pop() || 'dashboard.html';
    document.querySelectorAll('.nav-link').forEach(link => {
      if (link.getAttribute('href') === currentPage) link.classList.add('active');
    });

    // Set username and role badge in sidebar footer
    const el = document.getElementById('sidebar-username');
    if (el) {
      el.textContent = user.fullName || user.username || 'User';
    }

    // Add role badge next to username in sidebar if not already present
    const sidebarFooter = document.querySelector('.sidebar-footer');
    if (sidebarFooter && !document.getElementById('sidebar-role-badge')) {
      const badge = document.createElement('span');
      badge.id = 'sidebar-role-badge';
      badge.className = 'badge ms-2';
      badge.style.cssText = 'background:rgba(78,204,163,0.2);color:var(--accent);font-size:0.65rem;vertical-align:middle;';
      badge.textContent = role;
      const usernameEl = document.getElementById('sidebar-username');
      if (usernameEl) usernameEl.after(badge);
    }

    // Enforce read-only mode dynamically for ANALYST and AUDITOR roles
    this.enforceReadOnly();
  },

  enforceReadOnly() {
    if (!Auth.isReadOnly()) return;

    const isLogoutBtn = (btn) => {
      const onclick = btn.getAttribute('onclick') || '';
      const text = (btn.textContent || btn.value || '').trim().toLowerCase();
      return onclick.includes('logout') || text === 'logout' || btn.classList.contains('logout-btn');
    };

    let observerActive = false;
    let observer = null;

    const enforce = () => {
      // Temporarily disconnect to prevent re-triggering on DOM changes we make
      if (observer) observer.disconnect();

      // 1. Hide buttons/links with write keywords
      const writeKeywords = [/add/i, /save/i, /create/i, /delete/i, /edit/i, /update/i, /remove/i, /new/i, /upload/i];
      document.querySelectorAll('button, a.btn, input[type="submit"], input[type="button"], #empty-state-action').forEach(btn => {
        // Never remove logout / safe UI controls
        if (isLogoutBtn(btn)) return;
        const text = btn.textContent || btn.value || '';
        if (text.toLowerCase().includes('theme') ||
            text.toLowerCase().includes('export') ||
            text.toLowerCase().includes('download') ||
            text.toLowerCase().includes('search') ||
            text.toLowerCase().includes('clear') ||
            text.toLowerCase().includes('filter') ||
            text.toLowerCase().includes('mark all')) {
          return;
        }

        const hasWriteKeyword = writeKeywords.some(regex => regex.test(text));
        const hasWriteIcon = btn.querySelector('.bi-plus-lg, .bi-pencil, .bi-trash, .bi-check-lg, .bi-plus-circle, .bi-pencil-square');
        const hasWriteClass = btn.classList.contains('write-only') ||
                              btn.classList.contains('btn-outline-info') ||
                              btn.classList.contains('btn-danger');
        // btn-outline-danger: only remove if NOT a logout button
        const isDangerWrite = btn.classList.contains('btn-outline-danger') && !isLogoutBtn(btn);

        if (hasWriteKeyword || hasWriteIcon || hasWriteClass || isDangerWrite) {
          btn.remove();
        }
      });

      // 2. Hide edit/delete Actions columns inside tables
      document.querySelectorAll('table').forEach(table => {
        let actionColIndex = -1;
        table.querySelectorAll('thead th').forEach((th, idx) => {
          const text = th.textContent.trim().toLowerCase();
          if (text === 'actions' || text === 'action') {
            actionColIndex = idx;
            th.style.display = 'none';
          }
        });

        if (actionColIndex !== -1) {
          table.querySelectorAll('tbody tr').forEach(tr => {
            const cells = tr.querySelectorAll('td');
            if (cells[actionColIndex]) {
              cells[actionColIndex].style.display = 'none';
            }
          });
        }
      });

      // 3. Remove any elements explicitly marked as write-only
      document.querySelectorAll('.write-only').forEach(el => el.remove());

      // 4. Make form inputs read-only (skip search/filter inputs)
      document.querySelectorAll('input, textarea, select').forEach(el => {
        const id = (el.id || '').toLowerCase();
        const name = (el.name || '').toLowerCase();
        // Skip if part of a search or filter area
        if (id.includes('search') || id.includes('filter') ||
            name.includes('search') || name.includes('filter') ||
            el.type === 'search' ||
            el.closest('.search-area') || el.closest('.filter-area') ||
            el.closest('[id*="search"]') || el.closest('[id*="filter"]')) return;
        el.disabled = true;
        if (el.tagName !== 'SELECT') el.readOnly = true;
      });

      // Reconnect observer after our changes
      if (observerActive && observer) {
        observer.observe(document.body, { childList: true, subtree: true });
      }
    };

    // Run immediately on page load
    enforce();

    // Set up MutationObserver to enforce constraints when data elements are rendered dynamically
    observer = new MutationObserver(() => enforce());
    observerActive = true;
    observer.observe(document.body, { childList: true, subtree: true });
  },


  initHeader() {
    const mainContent = document.querySelector('.main-content');
    if (!mainContent) return;

    // Check if header already exists
    if (document.querySelector('.dashboard-header')) return;

    const user = Auth.getUser();
    const displayName = user.fullName || user.username || 'User';
    
    // Determine greeting
    const hours = new Date().getHours();
    let greeting = 'Good Evening';
    if (hours < 12) greeting = 'Good Morning';
    else if (hours < 17) greeting = 'Good Afternoon';

    // Get avatar image or default
    const savedAvatar = localStorage.getItem('profile_avatar') || '';
    const avatarImg = savedAvatar ? `<img src="${savedAvatar}" class="header-avatar" alt="Avatar"/>` : `<i class="bi bi-person-circle fs-4"></i>`;

    const headerHtml = `
      <header class="dashboard-header animate-in">
        <div class="header-welcome">
          <h2 id="header-greeting">${greeting}, ${displayName} 👋</h2>
          <p id="header-datetime"><i class="bi bi-calendar3 me-2"></i>Loading date &amp; time...</p>
        </div>
        <div class="header-actions">
          <button class="sidebar-collapse-btn d-none d-md-flex" title="Toggle Sidebar">
            <i class="bi bi-layout-sidebar-inset"></i>
          </button>
          <div class="position-relative">
            <button class="notification-bell-btn" id="bell-btn" title="Notifications">
              <i class="bi bi-bell"></i>
              <span class="notification-badge d-none" id="bell-count">0</span>
            </button>
            <div class="notification-dropdown" id="notification-dropdown">
              <div class="notification-header">
                <h6>Notifications</h6>
                <button id="mark-all-read-btn">Mark all as read</button>
              </div>
              <div class="notification-list" id="notification-list">
                <div class="notification-empty">No new alerts.</div>
              </div>
            </div>
          </div>
          <button class="theme-toggle-btn" id="theme-btn" title="Toggle Theme">
            <i class="bi bi-moon-stars"></i>
          </button>
          <a href="profile.html" class="d-flex align-items-center" style="text-decoration:none; color: var(--text-primary);">
            ${avatarImg}
          </a>
        </div>
      </header>
    `;

    mainContent.insertAdjacentHTML('afterbegin', headerHtml);

    // Live Date/Time Clock
    const updateDateTime = () => {
      const el = document.getElementById('header-datetime');
      if (el) {
        const now = new Date();
        const options = { weekday: 'long', day: 'numeric', month: 'short', year: 'numeric' };
        const dateStr = now.toLocaleDateString('en-IN', options);
        const timeStr = now.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        el.innerHTML = `<i class="bi bi-calendar3 me-2"></i>${dateStr} &bull; ${timeStr}`;
      }
    };
    updateDateTime();
    setInterval(updateDateTime, 1000);

    // Sidebar collapse setup
    const collapseBtn = document.querySelector('.sidebar-collapse-btn');
    const sidebar = document.querySelector('.sidebar');
    if (collapseBtn && sidebar) {
      collapseBtn.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
        localStorage.setItem('sidebar_collapsed', sidebar.classList.contains('collapsed'));
      });
    }

    // Theme Toggle setup
    const themeBtn = document.getElementById('theme-btn');
    const currentTheme = localStorage.getItem('theme') || 'dark';
    if (themeBtn) {
      themeBtn.querySelector('i').className = currentTheme === 'light' ? 'bi bi-sun' : 'bi bi-moon-stars';
      themeBtn.addEventListener('click', () => {
        const theme = document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
        themeBtn.querySelector('i').className = theme === 'light' ? 'bi bi-sun' : 'bi bi-moon-stars';
      });
    }

    // Notifications Center logic
    const bellBtn = document.getElementById('bell-btn');
    const dropdown = document.getElementById('notification-dropdown');
    
    if (bellBtn && dropdown) {
      bellBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('show');
        if (dropdown.classList.contains('show')) {
          this.loadNotifications();
        }
      });
      document.addEventListener('click', () => dropdown.classList.remove('show'));
      dropdown.addEventListener('click', (e) => e.stopPropagation());
    }

    const markAllBtn = document.getElementById('mark-all-read-btn');
    if (markAllBtn) {
      markAllBtn.addEventListener('click', async () => {
        const userId = Api.getUserId();
        if (!userId) return;
        try {
          const pageData = await Api.get(`/notifications?userId=${userId}&size=30`);
          const items = pageData.content || pageData;
          if (items && items.length > 0) {
            for (const item of items) {
              if (!item.isRead && !item.read) {
                await Api.patch(`/notifications/${item.id}/mark-read`);
              }
            }
          }
          this.updateUnreadCount();
          this.loadNotifications();
          this.showToast('All notifications marked as read', 'success');
        } catch (err) {
          console.error(err);
        }
      });
    }

    this.updateUnreadCount();
  },

  async updateUnreadCount() {
    const userId = Api.getUserId();
    const badge = document.getElementById('bell-count');
    if (!userId || !badge) return;
    try {
      const count = await Api.get(`/notifications/unread-count?userId=${userId}`);
      if (count > 0) {
        badge.textContent = count;
        badge.classList.remove('d-none');
      } else {
        badge.classList.add('d-none');
      }
    } catch {
      badge.classList.add('d-none');
    }
  },

  async loadNotifications() {
    const userId = Api.getUserId();
    const container = document.getElementById('notification-list');
    if (!userId || !container) return;
    container.innerHTML = '<div class="notification-empty"><div class="spinner-border spinner-border-sm text-light"></div> Loading...</div>';
    try {
      const pageData = await Api.get(`/notifications?userId=${userId}&size=5`);
      const list = pageData.content !== undefined ? pageData.content : pageData;
      container.innerHTML = '';
      if (!list || list.length === 0) {
        container.innerHTML = '<div class="notification-empty">No new alerts.</div>';
        return;
      }
      list.forEach(n => {
        let icon = 'bi-bell-fill';
        let colorClass = 'info';
        if (n.title.toLowerCase().includes('budget')) { icon = 'bi-exclamation-triangle-fill'; colorClass = 'danger'; }
        else if (n.title.toLowerCase().includes('goal')) { icon = 'bi-trophy-fill'; colorClass = 'success'; }
        else if (n.title.toLowerCase().includes('income')) { icon = 'bi-plus-circle-fill'; colorClass = 'success'; }
        else if (n.title.toLowerCase().includes('expense')) { icon = 'bi-dash-circle-fill'; colorClass = 'danger'; }

        const isUnread = !(n.isRead || n.read);
        const itemHtml = `
          <div class="notification-item ${isUnread ? 'unread' : ''}" data-id="${n.id}">
            <div class="notification-item-icon ${colorClass}"><i class="bi ${icon}"></i></div>
            <div class="notification-item-content">
              <div class="notification-item-title">${n.title}</div>
              <div class="notification-item-msg">${n.message}</div>
              <div class="notification-item-time">${this.formatDate(n.createdAt)}</div>
            </div>
          </div>
        `;
        container.insertAdjacentHTML('beforeend', itemHtml);
      });

      container.querySelectorAll('.notification-item').forEach(item => {
        item.addEventListener('click', async () => {
          const id = item.dataset.id;
          if (item.classList.contains('unread')) {
            try {
              await Api.patch(`/notifications/${id}/mark-read`);
              item.classList.remove('unread');
              this.updateUnreadCount();
            } catch (err) {
              console.error(err);
            }
          }
        });
      });
    } catch {
      container.innerHTML = '<div class="notification-empty text-danger">Failed to load alerts.</div>';
    }
  },

  showSkeletonCards(container, count = 3) {
    container.innerHTML = '';
    let cards = '';
    const colSize = Math.floor(12 / count);
    for (let i = 0; i < count; i++) {
      cards += `
        <div class="col-md-${colSize}">
          <div class="glass-card stat-card">
            <div class="skeleton skeleton-circle"></div>
            <div style="flex:1;">
              <div class="skeleton skeleton-text" style="width:50%;"></div>
              <div class="skeleton skeleton-text" style="width:70%;height:24px;"></div>
            </div>
          </div>
        </div>
      `;
    }
    container.innerHTML = cards;
  },

  showSkeletonTable(tbody, rows = 5, cols = 5) {
    tbody.innerHTML = '';
    let trs = '';
    for (let r = 0; r < rows; r++) {
      let tdHtml = '';
      for (let c = 0; c < cols; c++) {
        tdHtml += `<td><div class="skeleton skeleton-text" style="width:${Math.floor(Math.random()*40)+40}%;margin-bottom:0;"></div></td>`;
      }
      trs += `<tr>${tdHtml}</tr>`;
    }
    tbody.innerHTML = trs;
  },

  showSkeletonChart(container) {
    container.innerHTML = `
      <div class="d-flex flex-column justify-content-center align-items-center h-100 py-5">
        <div class="skeleton skeleton-chart mb-3"></div>
      </div>
    `;
  },

  renderEmptyState(container, title, subtitle, iconClass = 'bi-wallet2', actionText = null, actionCallback = null) {
    container.innerHTML = `
      <div class="empty-state-container animate-in">
        <div class="empty-state-icon"><i class="bi ${iconClass}"></i></div>
        <div class="empty-state-title">${title}</div>
        <div class="empty-state-subtitle">${subtitle}</div>
        ${actionText ? `<button class="btn btn-glow btn-sm" id="empty-state-action"><i class="bi bi-plus-lg me-1"></i>${actionText}</button>` : ''}
      </div>
    `;
    if (actionText && actionCallback) {
      const btn = container.querySelector('#empty-state-action');
      if (btn) btn.addEventListener('click', actionCallback);
    }
  }
};

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
   * then sets the active class and username.
   *
   * All pages that use a dynamic sidebar must have:
   *   <nav class="sidebar-nav" id="sidebar-nav"></nav>
   *
   * Pages that still have static nav links (no #sidebar-nav) continue to
   * work as before — this function simply activates the correct link.
   *
   * @param {string} [activePage] - Override the active page filename.
   *   Defaults to the current page filename from window.location.pathname.
   */
  initSidebar(activePage) {
    const user = Auth.getUser();
    const currentPage = activePage || window.location.pathname.split('/').pop() || 'dashboard.html';

    // ── Inject nav links into dynamic sidebar containers ──────────────────
    const navContainer = document.getElementById('sidebar-nav');
    if (navContainer) {
      // Base links available to all roles
      const baseLinks = [
        { href: 'dashboard.html',  icon: 'bi-grid-1x2',    label: 'Dashboard' },
        { href: 'income.html',     icon: 'bi-cash-stack',   label: 'Income' },
        { href: 'expense.html',    icon: 'bi-credit-card',  label: 'Expenses' },
        { href: 'category.html',   icon: 'bi-tags',         label: 'Categories' },
        { href: 'budget.html',     icon: 'bi-pie-chart',    label: 'Budgets' },
        { href: 'savings.html',    icon: 'bi-piggy-bank',   label: 'Savings Goals' },
        { href: 'reports.html',    icon: 'bi-graph-up',     label: 'Reports' },
        { href: 'profile.html',    icon: 'bi-person-circle',label: 'Profile' },
      ];

      // Admin-only links
      const adminLinks = [
        { href: 'admin-users.html', icon: 'bi-people-fill', label: 'User Management', adminOnly: true },
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

    // ── Set username in sidebar footer ────────────────────────────────────
    const el = document.getElementById('sidebar-username');
    if (el) el.textContent = user.username || 'User';
  }
};

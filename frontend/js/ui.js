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

  initSidebar() {
    const toggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggle && sidebar) {
      toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    }
    // Set active nav link
    const currentPage = window.location.pathname.split('/').pop() || 'dashboard.html';
    document.querySelectorAll('.nav-link').forEach(link => {
      if (link.getAttribute('href') === currentPage) link.classList.add('active');
    });
    // Set username in sidebar
    const user = Auth.getUser();
    const el = document.getElementById('sidebar-username');
    if (el) el.textContent = user.username || 'User';
  }
};

function renderSidebar(activePage) {
    const user = Auth.getUser();
    document.getElementById("sidebarMount").innerHTML = `
    <aside class="sidebar">
      <div class="sidebar-brand">
        <h2>🏦 JavaBank</h2>
        <span>Banking System</span>
      </div>
      <nav class="sidebar-nav">
        <a class="nav-item ${activePage==='dashboard'?'active':''}" href="dashboard.html">
          <span class="icon">📊</span> Dashboard
        </a>
        <a class="nav-item ${activePage==='accounts'?'active':''}" href="accounts.html">
          <span class="icon">💳</span> My Accounts
        </a>
        <a class="nav-item ${activePage==='transactions'?'active':''}" href="transactions.html">
          <span class="icon">💸</span> Transactions
        </a>
        <a class="nav-item ${activePage==='transfer'?'active':''}" href="transfer.html">
          <span class="icon">🔄</span> Transfer Money
        </a>
        <a class="nav-item ${activePage==='fraud'?'active':''}" href="fraud.html">
          <span class="icon">🚨</span> Fraud Alerts
        </a>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <strong>${user.fullName || user.username || 'User'}</strong>
          ${user.username || ''}
        </div>
        <button class="btn btn-outline btn-sm" style="width:100%" onclick="logout()">
          Logout
        </button>
      </div>
    </aside>`;
}

async function logout() {
    try { await API.call("/auth/logout", "POST"); } catch(e) {}
    Auth.clear();
    window.location.href = "../index.html";
}

<<<<<<< HEAD
<!-- Shared sidebar nav — included in all app pages -->
<script>
=======
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
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
<<<<<<< HEAD
          <span class="icon">💸</span> Transactions
=======
          <span class="icon">📋</span> Transactions
        </a>
        <a class="nav-item ${activePage==='payments'?'active':''}" href="payments.html">
          <span class="icon">💸</span> Payments
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
        </a>
        <a class="nav-item ${activePage==='transfer'?'active':''}" href="transfer.html">
          <span class="icon">🔄</span> Transfer Money
        </a>
<<<<<<< HEAD
        <a class="nav-item ${activePage==='fraud'?'active':''}" href="fraud.html">
          <span class="icon">🚨</span> Fraud Alerts
        </a>
        <a class="nav-item ${activePage==='pending'?'active':''}" href="pending.html" style="position:relative">
          <span class="icon">⏳</span> Pending Confirm
          <span id="navPendingBadge" style="display:none; position:absolute; right:12px; top:50%; transform:translateY(-50%); background:#ef4444; color:white; font-size:0.65rem; font-weight:700; padding:2px 6px; border-radius:10px; min-width:18px; text-align:center;"></span>
        </a>
=======
        <a class="nav-item ${activePage==='upi'?'active':''}" href="upi.html">
          <span class="icon">📱</span> UPI
        </a>
        <a class="nav-item ${activePage==='fraud'?'active':''}" href="fraud.html">
          <span class="icon">🚨</span> Fraud Alerts
        </a>
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
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
<<<<<<< HEAD
    window.location.href = "/index.html";
}
</script>
=======
    window.location.href = "../index.html";
}
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7

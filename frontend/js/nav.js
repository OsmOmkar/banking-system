function renderSidebar(activePage) {
    const user = Auth.getUser();
    document.getElementById("sidebarMount").innerHTML = `
    <aside class="sidebar" id="mainSidebar">
      <div class="sidebar-brand" style="position:relative">
        <h2>🏦 JavaBank</h2>
        <span>Banking System</span>
        <button class="sidebar-close-btn" onclick="closeMobileSidebar()" title="Close">&#x2715;</button>
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
        <a class="nav-item ${activePage==='upi'?'active':''}" href="upi.html">
          <span class="icon">📱</span> UPI
        </a>
        <a class="nav-item ${activePage==='fraud'?'active':''}" href="fraud.html">
          <span class="icon">🚨</span> Fraud Alerts
        </a>
        <a class="nav-item ${activePage==='pending'?'active':''}" href="pending.html" style="position:relative">
          <span class="icon">⏳</span> Pending Confirm
          <span id="navPendingBadge" style="display:none; position:absolute; right:12px; top:50%; transform:translateY(-50%); background:#ef4444; color:white; font-size:0.65rem; font-weight:700; padding:2px 6px; border-radius:10px; min-width:18px; text-align:center;"></span>
        </a>
        <a class="nav-item ${activePage==='profile'?'active':''}" href="profile.html">
          <span class="icon">👤</span> My Profile
        </a>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <strong>${user.fullName || user.username || 'User'}</strong>
          ${user.username ? '@' + user.username : ''}
        </div>
        <button class="btn btn-outline btn-sm" style="width:100%" onclick="logout()">
          Logout
        </button>
      </div>
    </aside>
    <div class="sidebar-backdrop" id="sidebarBackdrop" onclick="closeMobileSidebar()"></div>`;

    // Inject hamburger button once (only visible on mobile via CSS)
    if (!document.getElementById('jbHamburger')) {
        const ham = document.createElement('button');
        ham.id = 'jbHamburger';
        ham.className = 'hamburger-btn';
        ham.innerHTML = '&#9776;';
        ham.setAttribute('aria-label', 'Open navigation menu');
        ham.onclick = openMobileSidebar;
        document.body.appendChild(ham);
    }

    // Inject iPhone-style notification container (only once)
    if (!document.getElementById('jb-notif-container')) {
        injectNotificationStyles();
        const el = document.createElement('div');
        el.id = 'jb-notif-container';
        document.body.appendChild(el);
    }

    // Start polling for incoming payments
    startPaymentNotificationPolling();
}

function openMobileSidebar() {
    const sidebar  = document.getElementById('mainSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    if (sidebar)  sidebar.classList.add('mob-open');
    if (backdrop) backdrop.classList.add('visible');
}

function closeMobileSidebar() {
    const sidebar  = document.getElementById('mainSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    if (sidebar)  sidebar.classList.remove('mob-open');
    if (backdrop) backdrop.classList.remove('visible');
}

async function logout() {
    try { await API.call("/auth/logout", "POST"); } catch(e) {}
    Auth.clear();
    window.location.href = "/index.html";
}

// ─── iPhone-style payment notification system ───────────────────────────────

function injectNotificationStyles() {
    if (document.getElementById('jb-notif-styles')) return;
    const style = document.createElement('style');
    style.id = 'jb-notif-styles';
    style.textContent = `
        #jb-notif-container {
            position: fixed;
            top: 16px;
            right: 16px;
            z-index: 99999;
            display: flex;
            flex-direction: column;
            gap: 10px;
            pointer-events: none;
        }
        .jb-notif {
            pointer-events: all;
            display: flex;
            align-items: center;
            gap: 12px;
            background: rgba(15,23,42,0.95);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid rgba(255,255,255,0.12);
            border-radius: 20px;
            padding: 12px 16px;
            min-width: 300px;
            max-width: 360px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.05);
            cursor: pointer;
            transform: translateY(-120%) scale(0.9);
            opacity: 0;
            transition: all 0.4s cubic-bezier(0.34,1.56,0.64,1);
        }
        .jb-notif.show {
            transform: translateY(0) scale(1);
            opacity: 1;
        }
        .jb-notif.hide {
            transform: translateY(-120%) scale(0.9);
            opacity: 0;
            transition: all 0.3s ease-in;
        }
        .jb-notif-icon {
            width: 44px; height: 44px;
            background: linear-gradient(135deg, #22c55e, #16a34a);
            border-radius: 12px;
            display: flex; align-items: center; justify-content: center;
            font-size: 22px;
            flex-shrink: 0;
        }
        .jb-notif-app {
            font-size: 0.65rem;
            color: #64748b;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            font-weight: 600;
        }
        .jb-notif-title {
            font-size: 0.95rem;
            font-weight: 700;
            color: #f1f5f9;
            line-height: 1.2;
        }
        .jb-notif-sub {
            font-size: 0.82rem;
            color: #94a3b8;
            line-height: 1.3;
        }
        .jb-notif-time {
            font-size: 0.7rem;
            color: #475569;
            flex-shrink: 0;
            align-self: flex-start;
            padding-top: 2px;
        }
        .jb-notif-progress {
            position: absolute;
            bottom: 0; left: 0;
            height: 3px;
            background: linear-gradient(90deg, #22c55e, #16a34a);
            border-radius: 0 0 20px 20px;
            animation: jbProgress 6s linear forwards;
        }
        @keyframes jbProgress {
            from { width: 100%; }
            to   { width: 0%; }
        }
    `;
    document.head.appendChild(style);
}

function showPaymentNotification(tx) {
    const container = document.getElementById('jb-notif-container');
    if (!container) return;

    // Parse sender name from description
    // "[UPI] From: SenderName (upiId) | note"
    // "Credit: Transfer from ACCXXXXXXXX | note"
    let senderName = 'Someone';
    let amountStr  = '₹' + (tx.amount ? parseFloat(tx.amount).toFixed(2) : '0.00');
    let txId       = tx.id;

    const desc = tx.description || '';
    if (desc.startsWith('[UPI] From:')) {
        const inner = desc.replace('[UPI] From:', '').split('|')[0].trim();
        // inner = "SenderName (upiId)"
        senderName = inner.replace(/\(.*\)$/, '').trim() || senderName;
    } else if (desc.startsWith('Credit: Transfer from')) {
        const accPart = desc.replace('Credit: Transfer from', '').split('|')[0].trim();
        senderName = 'Account ' + accPart;
    }

    const isUpi = desc.startsWith('[UPI]');
    const now = new Date();
    const timeStr = now.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });

    const notif = document.createElement('div');
    notif.className = 'jb-notif';
    notif.style.position = 'relative';
    notif.innerHTML = `
        <div class="jb-notif-icon">${isUpi ? '📱' : '💳'}</div>
        <div style="flex:1;min-width:0;">
            <div class="jb-notif-app">JavaBank${isUpi ? ' · UPI' : ''}</div>
            <div class="jb-notif-title">Money Received  ${amountStr}</div>
            <div class="jb-notif-sub">From: ${senderName}</div>
        </div>
        <div class="jb-notif-time">${timeStr}</div>
        <div class="jb-notif-progress"></div>
    `;

    // Click → go to UPI page with that tx highlighted
    notif.addEventListener('click', () => {
        dismissNotif(notif);
        if (isUpi) {
            window.location.href = 'upi.html?highlightTx=' + txId;
        }
    });

    container.appendChild(notif);
    // Trigger animation
    requestAnimationFrame(() => requestAnimationFrame(() => notif.classList.add('show')));

    // Auto-dismiss after 7s
    setTimeout(() => dismissNotif(notif), 7000);
}

function dismissNotif(notif) {
    notif.classList.add('hide');
    setTimeout(() => notif.remove(), 400);
}

// ─── Polling for new incoming payments ──────────────────────────────────────

let _pollInterval = null;

function startPaymentNotificationPolling() {
    if (!Auth.getToken()) return;
    if (_pollInterval) return; // already running

    // Initial check after 8s (let page settle first)
    setTimeout(pollForNewPayments, 8000);
    // Then every 30s — reduced from 15s to ease DB connection pressure
    _pollInterval = setInterval(pollForNewPayments, 30000);
}

async function pollForNewPayments() {
    if (!Auth.getToken()) return;
    try {
        const txs = await API.call('/upi/transactions');
        if (!txs || !Array.isArray(txs)) return;

        // Only look at received payments (DEPOSIT type)
        const received = txs.filter(t => t.type === 'DEPOSIT');

        if (received.length === 0) {
            // Track max ID so future polls can detect new deposits
            if (txs.length > 0) {
                const maxId = Math.max(...txs.map(t => t.id));
                if (!localStorage.getItem('jb_lastUpiReceiveId'))
                    localStorage.setItem('jb_lastUpiReceiveId', String(maxId));
            }
            return;
        }

        const lastSeenId = parseInt(localStorage.getItem('jb_lastUpiReceiveId') || '0');
        const newOnes    = received.filter(t => t.id > lastSeenId);

        // Always update the high-water mark
        const maxId = Math.max(...received.map(t => t.id));
        localStorage.setItem('jb_lastUpiReceiveId', String(maxId));

        // Show notification only after first baseline poll
        if (lastSeenId > 0 && newOnes.length > 0) {
            newOnes.slice(0, 2).forEach(tx => showPaymentNotification(tx));
        }

    } catch (e) {
        // Silent — user may not have UPI enabled
    }
}

async function pollNonUpiCredits() {
    try {
        const accounts = await API.call('/accounts');
        if (!accounts || !Array.isArray(accounts)) return;

        for (const acc of accounts) {
            const allTxs = await API.call('/transactions?accountId=' + acc.id);
            if (!allTxs || !Array.isArray(allTxs)) continue;

            const deposits = allTxs.filter(t =>
                t.type === 'DEPOSIT' &&
                t.description && t.description.startsWith('Credit: Transfer from')
            );
            if (deposits.length === 0) continue;

            const key = 'jb_lastBankReceiveId_' + acc.id;
            const lastSeenId = parseInt(localStorage.getItem(key) || '0');
            const newOnes = deposits.filter(t => t.id > lastSeenId);

            const maxId = Math.max(...deposits.map(t => t.id));
            localStorage.setItem(key, String(maxId));

            if (lastSeenId > 0 && newOnes.length > 0) {
                newOnes.slice(0, 1).forEach(tx => showPaymentNotification(tx));
            }
        }
    } catch (e) { /* silent */ }
}
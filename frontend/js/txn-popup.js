// ======================================================
// JavaBank — Transaction Result Popup (txn-popup.js)
// Shared across all transaction pages.
// Usage: showTxnPopup({ type, title, amount, amountLabel, details, onOk, showPendingBtn })
//   type: 'success' | 'held' | 'failed'
//   title: string
//   amount: string (e.g. "₹5,000.00")
//   amountLabel: string (e.g. "Debited" / "Credited" / "On Hold")
//   details: [ { label, value }, ... ]
//   onOk: optional callback
//   showPendingBtn: boolean — show "Go to Pending Confirm" button (for held)
// ======================================================

(function () {
  // Inject CSS only once
  if (!document.getElementById('txn-popup-style')) {
    const s = document.createElement('style');
    s.id = 'txn-popup-style';
    s.textContent = `
      /* ── TXN Popup Overlay ── */
      #txnPopupOverlay {
        position: fixed; inset: 0;
        background: rgba(0,0,0,0.75);
        backdrop-filter: blur(4px);
        z-index: 99999;
        display: flex; align-items: center; justify-content: center;
        padding: 1rem;
        animation: txnFadeIn 0.2s ease;
      }
      @keyframes txnFadeIn {
        from { opacity: 0; }
        to   { opacity: 1; }
      }

      /* ── Popup Card ── */
      .txn-popup-card {
        background: #0f172a;
        border-radius: 24px;
        padding: 2.5rem 2rem 2rem;
        max-width: 400px;
        width: 100%;
        text-align: center;
        box-shadow: 0 32px 80px rgba(0,0,0,0.6);
        border: 1px solid rgba(255,255,255,0.06);
        animation: txnCardIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
      }
      @keyframes txnCardIn {
        from { transform: scale(0.85) translateY(20px); opacity: 0; }
        to   { transform: none; opacity: 1; }
      }

      /* ── Animated Icons ── */
      .txn-icon-wrap {
        width: 80px; height: 80px;
        margin: 0 auto 1.25rem;
        position: relative;
      }

      /* SUCCESS circle + checkmark */
      .txn-icon-wrap.success .txn-circle {
        stroke: #22c55e;
        fill: rgba(34,197,94,0.1);
        stroke-dasharray: 230;
        stroke-dashoffset: 230;
        animation: txnCircleDraw 0.6s ease forwards;
      }
      .txn-icon-wrap.success .txn-check {
        stroke: #22c55e;
        stroke-dasharray: 80;
        stroke-dashoffset: 80;
        animation: txnCheckDraw 0.4s ease 0.5s forwards;
      }

      /* HELD circle + pause */
      .txn-icon-wrap.held .txn-circle {
        stroke: #f59e0b;
        fill: rgba(245,158,11,0.1);
        stroke-dasharray: 230;
        stroke-dashoffset: 230;
        animation: txnCircleDraw 0.6s ease forwards, txnPulseStroke 2s 0.7s ease-in-out infinite;
      }
      .txn-icon-wrap.held .txn-pause {
        fill: #f59e0b;
        opacity: 0;
        animation: txnFadeInEl 0.3s ease 0.5s forwards;
      }

      /* FAILED circle + X */
      .txn-icon-wrap.failed .txn-circle {
        stroke: #ef4444;
        fill: rgba(239,68,68,0.1);
        stroke-dasharray: 230;
        stroke-dashoffset: 230;
        animation: txnCircleDraw 0.5s ease forwards;
      }
      .txn-icon-wrap.failed .txn-x-line {
        stroke: #ef4444;
        stroke-dasharray: 60;
        stroke-dashoffset: 60;
        animation: txnXDraw 0.3s ease 0.45s forwards,
                   txnShake 0.4s ease 0.75s;
      }

      @keyframes txnCircleDraw {
        to { stroke-dashoffset: 0; }
      }
      @keyframes txnCheckDraw {
        to { stroke-dashoffset: 0; }
      }
      @keyframes txnXDraw {
        to { stroke-dashoffset: 0; }
      }
      @keyframes txnFadeInEl {
        to { opacity: 1; }
      }
      @keyframes txnPulseStroke {
        0%,100% { opacity: 1; }
        50%      { opacity: 0.5; }
      }
      @keyframes txnShake {
        0%,100% { transform: translateX(0); }
        20%     { transform: translateX(-4px); }
        40%     { transform: translateX(4px); }
        60%     { transform: translateX(-3px); }
        80%     { transform: translateX(3px); }
      }

      /* ── Title ── */
      .txn-popup-title {
        font-size: 1.25rem;
        font-weight: 800;
        margin: 0 0 0.5rem;
        color: #f1f5f9;
        font-family: 'Inter', 'Segoe UI', sans-serif;
      }
      .txn-popup-title.success { color: #22c55e; }
      .txn-popup-title.held    { color: #f59e0b; }
      .txn-popup-title.failed  { color: #ef4444; }

      /* ── Amount pill ── */
      .txn-popup-amount {
        display: inline-block;
        font-size: 1.6rem;
        font-weight: 800;
        padding: 0.35rem 1.25rem;
        border-radius: 50px;
        margin: 0.6rem 0 1.25rem;
        font-family: 'Inter', 'Segoe UI', sans-serif;
        letter-spacing: -0.5px;
      }
      .txn-popup-amount.success { background: rgba(34,197,94,0.12); color: #22c55e; }
      .txn-popup-amount.held    { background: rgba(245,158,11,0.12); color: #f59e0b; }
      .txn-popup-amount.failed  { background: rgba(239,68,68,0.12);  color: #ef4444; }

      .txn-popup-amt-label {
        font-size: 0.72rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        margin-bottom: 4px;
        color: #64748b;
      }

      /* ── Details grid ── */
      .txn-popup-details {
        background: #1e293b;
        border-radius: 14px;
        padding: 0.9rem 1.1rem;
        margin-bottom: 1.25rem;
        text-align: left;
      }
      .txn-popup-detail-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
        padding: 0.35rem 0;
        border-bottom: 1px solid rgba(255,255,255,0.04);
        font-family: 'Inter', 'Segoe UI', sans-serif;
      }
      .txn-popup-detail-row:last-child { border-bottom: none; }
      .txn-popup-detail-label {
        font-size: 0.72rem;
        color: #64748b;
        font-weight: 500;
        white-space: nowrap;
      }
      .txn-popup-detail-value {
        font-size: 0.78rem;
        color: #e2e8f0;
        font-weight: 600;
        text-align: right;
        word-break: break-all;
      }

      /* ── Note (for held) ── */
      .txn-popup-note {
        font-size: 0.8rem;
        color: #94a3b8;
        line-height: 1.6;
        margin-bottom: 1.25rem;
        background: rgba(245,158,11,0.07);
        border: 1px solid rgba(245,158,11,0.2);
        border-radius: 10px;
        padding: 0.65rem 0.85rem;
        text-align: left;
      }
      .txn-popup-note.failed {
        background: rgba(239,68,68,0.07);
        border-color: rgba(239,68,68,0.2);
      }

      /* ── Buttons ── */
      .txn-popup-btns {
        display: flex;
        flex-direction: column;
        gap: 0.6rem;
      }
      .txn-popup-btn-ok {
        width: 100%;
        padding: 0.8rem;
        border: none;
        border-radius: 12px;
        font-size: 0.95rem;
        font-weight: 700;
        cursor: pointer;
        font-family: 'Inter', 'Segoe UI', sans-serif;
        transition: all 0.2s;
        color: white;
      }
      .txn-popup-btn-ok.success { background: linear-gradient(135deg,#22c55e,#16a34a); }
      .txn-popup-btn-ok.held    { background: linear-gradient(135deg,#f59e0b,#d97706); }
      .txn-popup-btn-ok.failed  { background: linear-gradient(135deg,#ef4444,#dc2626); }
      .txn-popup-btn-ok:hover   { filter: brightness(1.1); transform: translateY(-1px); }

      .txn-popup-btn-pending {
        width: 100%;
        padding: 0.75rem;
        background: rgba(245,158,11,0.12);
        border: 1.5px solid rgba(245,158,11,0.35);
        border-radius: 12px;
        font-size: 0.88rem;
        font-weight: 600;
        cursor: pointer;
        font-family: 'Inter', 'Segoe UI', sans-serif;
        color: #f59e0b;
        transition: all 0.2s;
      }
      .txn-popup-btn-pending:hover {
        background: rgba(245,158,11,0.2);
        transform: translateY(-1px);
      }
    `;
    document.head.appendChild(s);
  }

  // ── SVG icon generators ──────────────────────────────────
  function svgSuccess() {
    return `<svg viewBox="0 0 80 80" width="80" height="80">
      <circle class="txn-circle" cx="40" cy="40" r="36"
        fill="rgba(34,197,94,0.1)" stroke="#22c55e" stroke-width="3"
        stroke-linecap="round" transform="rotate(-90 40 40)"/>
      <polyline class="txn-check" points="24,42 35,53 56,28"
        fill="none" stroke="#22c55e" stroke-width="4"
        stroke-linecap="round" stroke-linejoin="round"/>
    </svg>`;
  }

  function svgHeld() {
    return `<svg viewBox="0 0 80 80" width="80" height="80">
      <circle class="txn-circle" cx="40" cy="40" r="36"
        fill="rgba(245,158,11,0.1)" stroke="#f59e0b" stroke-width="3"
        stroke-linecap="round" transform="rotate(-90 40 40)"/>
      <rect class="txn-pause" x="28" y="27" width="9" height="26"
        rx="3" fill="#f59e0b" opacity="0"/>
      <rect class="txn-pause" x="43" y="27" width="9" height="26"
        rx="3" fill="#f59e0b" opacity="0"/>
    </svg>`;
  }

  function svgFailed() {
    return `<svg viewBox="0 0 80 80" width="80" height="80">
      <circle class="txn-circle" cx="40" cy="40" r="36"
        fill="rgba(239,68,68,0.1)" stroke="#ef4444" stroke-width="3"
        stroke-linecap="round" transform="rotate(-90 40 40)"/>
      <line class="txn-x-line" x1="27" y1="27" x2="53" y2="53"
        stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
      <line class="txn-x-line" x1="53" y1="27" x2="27" y2="53"
        stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
    </svg>`;
  }

  // ── Main function ──────────────────────────────────────────
  window.showTxnPopup = function (cfg) {
    // cfg = {
    //   type: 'success'|'held'|'failed',
    //   title: string,
    //   amount: string,          // e.g. '₹5,000.00'
    //   amountLabel: string,     // e.g. 'Amount Credited'
    //   details: [{label,value},...],
    //   note: string (optional)  // shown below details
    //   onOk: fn (optional),
    //   showPendingBtn: bool
    // }
    const type = cfg.type || 'success';

    // Remove any existing popup
    const old = document.getElementById('txnPopupOverlay');
    if (old) old.remove();

    // Build icon
    const iconHtml = type === 'success' ? svgSuccess()
                   : type === 'held'    ? svgHeld()
                   : svgFailed();

    // Build details rows
    const detailsHtml = (cfg.details || []).map(d =>
      `<div class="txn-popup-detail-row">
         <span class="txn-popup-detail-label">${d.label}</span>
         <span class="txn-popup-detail-value">${d.value}</span>
       </div>`
    ).join('');

    // Note section
    const noteHtml = cfg.note
      ? `<div class="txn-popup-note ${type === 'failed' ? 'failed' : ''}">${cfg.note}</div>`
      : '';

    // Pending button
    const pendingBtnHtml = cfg.showPendingBtn
      ? `<button class="txn-popup-btn-pending" onclick="window.location.href='/pages/pending.html'">
           ⏳ Go to Pending Confirmations
         </button>`
      : '';

    // OK label
    const okLabel = type === 'success' ? '✓ Done'
                  : type === 'held'    ? '⏳ I\'ll check later'
                  : '✕ Close';

    const overlay = document.createElement('div');
    overlay.id = 'txnPopupOverlay';
    overlay.innerHTML = `
      <div class="txn-popup-card">
        <div class="txn-icon-wrap ${type}">${iconHtml}</div>
        <div class="txn-popup-title ${type}">${cfg.title || 'Transaction Update'}</div>
        ${cfg.amountLabel ? `<div class="txn-popup-amt-label">${cfg.amountLabel}</div>` : ''}
        ${cfg.amount ? `<div class="txn-popup-amount ${type}">${cfg.amount}</div>` : ''}
        ${detailsHtml ? `<div class="txn-popup-details">${detailsHtml}</div>` : ''}
        ${noteHtml}
        <div class="txn-popup-btns">
          ${pendingBtnHtml}
          <button class="txn-popup-btn-ok ${type}" onclick="_txnPopupClose()">
            ${okLabel}
          </button>
        </div>
      </div>`;

    document.body.appendChild(overlay);

    // Close on overlay click (outside card)
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) _txnPopupClose();
    });

    window._txnPopupOnOk = cfg.onOk || null;
  };

  window._txnPopupClose = function () {
    const overlay = document.getElementById('txnPopupOverlay');
    if (overlay) overlay.remove();
    if (window._txnPopupOnOk) {
      window._txnPopupOnOk();
      window._txnPopupOnOk = null;
    }
  };

  // ── Quick helpers ──────────────────────────────────────────
  function nowStr() {
    return new Date().toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }

  // Build a standard details array for a completed transaction
  window.buildTxnDetails = function (tx, extra) {
    const rows = [];
    if (tx.id)          rows.push({ label: 'Transaction ID', value: 'TXN-' + tx.id });
    if (tx.date)        rows.push({ label: 'Date & Time', value: new Date(tx.date).toLocaleString('en-IN') });
    else                rows.push({ label: 'Date & Time', value: nowStr() });
    if (tx.balanceAfter !== undefined)
      rows.push({ label: 'Balance After', value: '₹' + Number(tx.balanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2 }) });
    if (tx.description) rows.push({ label: 'Description', value: tx.description });
    if (extra)          rows.push(...extra);
    return rows;
  };
})();

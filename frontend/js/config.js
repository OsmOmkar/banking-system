// ============================================================
// JavaBank — API Configuration
// Auto-detects local development vs production Railway URL
// ============================================================

const CONFIG = {
    // If running on localhost, use local backend on port 8081
    // Otherwise use the deployed Railway backend
    API_BASE: (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
        ? 'http://localhost:8081/api'
        : 'https://banking-system-production-7af6.up.railway.app/api'
};

const Auth = {
    getToken: () => localStorage.getItem('token'),
    getUser:  () => JSON.parse(localStorage.getItem('user') || '{}'),
    setAuth:  (token, user) => {
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
    },
    clear: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },
    isLoggedIn: () => !!localStorage.getItem('token'),
    requireAuth: () => {
        if (!localStorage.getItem('token')) {
            window.location.href = '/';
        }
    }
};

const API = {
    call: async (endpoint, method = 'GET', body = null) => {
        const opts = {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${Auth.getToken()}`
            }
        };
        if (body) opts.body = JSON.stringify(body);
        const res = await fetch(CONFIG.API_BASE + endpoint, opts);

        // Handle non-JSON response gracefully
        let data;
        try { data = await res.json(); }
        catch (e) { throw new Error('Server returned non-JSON response. Is the backend running?'); }

        // Special case: 202 = HELD transaction (not an error)
        if (res.status === 202 && data.held) {
            throw Object.assign(new Error(data.error || 'Transaction held'), {
                held: true,
                pendingId: data.pendingId
            });
        }

        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Request failed (status ' + res.status + ')');
        }
        return data.data;
    }
};

const fmt     = (n) => '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 });
const fmtDate = (d) => d ? new Date(d).toLocaleString('en-IN') : '—';

// Log which environment we're running in
console.log('[JavaBank] API Base:', CONFIG.API_BASE);

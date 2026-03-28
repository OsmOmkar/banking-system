// ============================================================
// CONFIGURE THIS: Replace with your Railway backend URL
// after deploying the Java backend to Railway
// ============================================================
const CONFIG = {
    API_BASE: "https://YOUR-APP.up.railway.app/api",
    // For local testing:
    // API_BASE: "http://localhost:8080/api"
};

// Auth helpers
const Auth = {
    getToken: () => localStorage.getItem("token"),
    getUser:  () => JSON.parse(localStorage.getItem("user") || "{}"),
    setAuth:  (token, user) => {
        localStorage.setItem("token", token);
        localStorage.setItem("user", JSON.stringify(user));
    },
    clear: () => {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    },
    isLoggedIn: () => !!localStorage.getItem("token"),
    requireAuth: () => {
        if (!localStorage.getItem("token")) {
            window.location.href = "/index.html";
        }
    }
};

// API helper
const API = {
    call: async (endpoint, method = "GET", body = null) => {
        const opts = {
            method,
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${Auth.getToken()}`
            }
        };
        if (body) opts.body = JSON.stringify(body);
        const res = await fetch(CONFIG.API_BASE + endpoint, opts);
        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || "Request failed");
        }
        return data.data;
    }
};

// Format currency
const fmt = (n) => "₹" + Number(n).toLocaleString("en-IN", { minimumFractionDigits: 2 });
const fmtDate = (d) => d ? new Date(d).toLocaleString("en-IN") : "-";

/**
 * Token Monitor - Frontend Logic
 * Fetches API usage data and updates the widget cards.
 */

const RING_CIRCUMFERENCE = 2 * Math.PI * 52; // ≈ 326.73
const REFRESH_INTERVAL = 60_000; // 60 seconds

let refreshTimer = null;
let isRefreshing = false;

// ==================== Initialization ====================

document.addEventListener('DOMContentLoaded', () => {
    // Register service worker
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/static/sw.js').catch(() => {});
    }

    // Initial fetch
    refreshAll();

    // Auto-refresh
    refreshTimer = setInterval(refreshAll, REFRESH_INTERVAL);
});

// Visibility API: refresh when tab becomes visible
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        refreshAll();
    }
});

// ==================== Data Fetching ====================

async function refreshAll() {
    if (isRefreshing) return;
    isRefreshing = true;

    const btn = document.querySelector('.btn-refresh');
    btn.classList.add('spinning');

    try {
        const resp = await fetch('/api/all');
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);

        const data = await resp.json();
        updateCard('anthropic', data.anthropic);
        updateCard('openai', data.openai);
        updateTimestamp(data.timestamp);

    } catch (err) {
        console.error('Refresh failed:', err);
        setError('anthropic');
        setError('openai');
        updateTimestamp(null, err.message);
    } finally {
        isRefreshing = false;
        btn.classList.remove('spinning');
    }
}

// ==================== UI Updates ====================

function updateCard(provider, data) {
    if (!data || data.error) {
        setError(provider, data?.error);
        return;
    }

    const used = data.total_used || 0;
    const limit = data.monthly_limit || 1;
    const remaining = data.remaining || 0;
    const percent = data.usage_percent || 0;

    // Status dot
    const statusEl = document.getElementById(`${provider}Status`);
    if (statusEl) {
        statusEl.className = 'card-status connected';
        statusEl.title = '已连接';
    }

    // Ring circle
    const circle = document.getElementById(`${provider}Circle`);
    if (circle) {
        const clampedPercent = Math.min(percent, 100);
        const offset = RING_CIRCUMFERENCE * (1 - clampedPercent / 100);
        circle.setAttribute('stroke-dasharray', `${RING_CIRCUMFERENCE} ${RING_CIRCUMFERENCE}`);
        circle.setAttribute('stroke-dashoffset', offset);

        // Color classes
        circle.classList.remove('danger', 'warning');
        if (percent >= 90) circle.classList.add('danger');
        else if (percent >= 75) circle.classList.add('warning');
    }

    // Percent text
    const pctEl = document.getElementById(`${provider}Percent`);
    if (pctEl) {
        const remainingPct = Math.max(0, 100 - percent);
        pctEl.textContent = `${Math.round(remainingPct)}%`;
        pctEl.style.color = getPercentColor(remainingPct);
    }

    // Stats
    setStatValue(`${provider}Used`, formatNumber(used));
    setStatValue(`${provider}Limit`, formatNumber(limit));
    setStatValue(`${provider}Cost`, data.estimated_cost_usd != null ? `$${data.estimated_cost_usd.toFixed(2)}` : '--');

    // Progress bar
    const bar = document.getElementById(`${provider}Bar`);
    if (bar) {
        bar.style.width = `${Math.min(percent, 100)}%`;
    }

    // Store data for potential offline use
    try {
        localStorage.setItem(`token_monitor_${provider}`, JSON.stringify({
            ...data,
            cachedAt: Date.now(),
        }));
    } catch (_) {}
}

function setStatValue(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function setError(provider, msg) {
    const statusEl = document.getElementById(`${provider}Status`);
    if (statusEl) {
        statusEl.className = 'card-status error';
        statusEl.title = msg || '连接错误';
    }

    // Try loading cached data
    try {
        const cached = localStorage.getItem(`token_monitor_${provider}`);
        if (cached) {
            const data = JSON.parse(cached);
            const age = Date.now() - data.cachedAt;
            if (age < 24 * 60 * 60 * 1000) { // 24h cache
                updateCard(provider, data);
                return;
            }
        }
    } catch (_) {}

    const pctEl = document.getElementById(`${provider}Percent`);
    if (pctEl) { pctEl.textContent = '--'; pctEl.style.color = ''; }

    setStatValue(`${provider}Used`, '--');
    setStatValue(`${provider}Limit`, '--');
    setStatValue(`${provider}Cost`, '--');
}

function updateTimestamp(isoString, errorMsg) {
    const dot = document.getElementById('statusDot');
    const text = document.getElementById('lastUpdated');

    if (errorMsg) {
        dot.className = 'dot error';
        text.textContent = `错误: ${errorMsg}`;
    } else if (isoString) {
        dot.className = 'dot connected';
        const d = new Date(isoString);
        text.textContent = `上次更新: ${d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`;
    } else {
        dot.className = 'dot';
        text.textContent = '加载中...';
    }
}

// ==================== Helpers ====================

function formatNumber(n) {
    if (n == null || isNaN(n)) return '--';
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
    return n.toLocaleString();
}

function getPercentColor(pct) {
    if (pct <= 10) return '#ef4444'; // danger
    if (pct <= 25) return '#f59e0b'; // warning
    return '#22c55e'; // success
}

// ==================== Export for inline onclick ====================
window.refreshAll = refreshAll;

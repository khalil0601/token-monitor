/**
 * Token Monitor - Service Worker
 * Cache-first strategy for static assets, network-first for API data.
 */

const CACHE_NAME = 'token-monitor-v1';
const STATIC_ASSETS = [
    '/',
    '/static/index.html',
    '/static/style.css',
    '/static/app.js',
    '/static/manifest.json',
];

// ===== Install =====
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(STATIC_ASSETS).catch(() => {});
        })
    );
    self.skipWaiting();
});

// ===== Activate =====
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
            );
        })
    );
    self.clients.claim();
});

// ===== Fetch =====
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // API requests: network first, no cache
    if (url.pathname.startsWith('/api/')) {
        return; // Don't intercept API calls - let them go to network
    }

    // Static assets: cache first, network fallback
    event.respondWith(
        caches.match(event.request).then((cached) => {
            const fetchPromise = fetch(event.request).then((response) => {
                if (response && response.ok) {
                    const clone = response.clone();
                    caches.open(CACHE_NAME).then((cache) => {
                        cache.put(event.request, clone);
                    });
                }
                return response;
            }).catch(() => cached);

            return cached || fetchPromise;
        })
    );
});

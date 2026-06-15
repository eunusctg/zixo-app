/**
 * Zixo PWA Service Worker
 * Handles offline caching, background sync, and push notifications
 */

const CACHE_NAME = 'zixo-v2';
const STATIC_CACHE = 'zixo-static-v2';
const DYNAMIC_CACHE = 'zixo-dynamic-v2';

// Static assets to cache on install
const STATIC_ASSETS = [
  '/',
  '/manifest.json',
  '/icon-512.png',
  '/icon-192.png',
  '/icon-180.png',
  '/icon-152.png',
  '/icon-144.png',
  '/icon-96.png',
  '/icon-72.png',
  '/icon-48.png',
  '/icon-32.png',
  '/icon-16.png',
];

// Network-first routes (API calls, real-time data)
const NETWORK_FIRST_ROUTES = [
  '/api/',
  'firebaseio.com',
  'googleapis.com',
  'firebase.google.com',
];

// Cache-first routes (static assets, fonts, images)
const CACHE_FIRST_ROUTES = [
  'fonts.googleapis.com',
  'fonts.gstatic.com',
  '.png',
  '.jpg',
  '.jpeg',
  '.svg',
  '.webp',
  '.woff2',
  '.woff',
  '.ttf',
];

// ==================== INSTALL EVENT ====================
self.addEventListener('install', (event) => {
  console.log('[Zixo SW] Installing service worker...');
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => {
      console.log('[Zixo SW] Pre-caching static assets');
      return cache.addAll(STATIC_ASSETS).catch((err) => {
        console.warn('[Zixo SW] Some static assets failed to cache:', err);
      });
    })
  );
  // Skip waiting and activate immediately
  self.skipWaiting();
});

// ==================== ACTIVATE EVENT ====================
self.addEventListener('activate', (event) => {
  console.log('[Zixo SW] Activating service worker...');
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => name !== STATIC_CACHE && name !== DYNAMIC_CACHE)
          .map((name) => {
            console.log('[Zixo SW] Deleting old cache:', name);
            return caches.delete(name);
          })
      );
    })
  );
  // Claim all clients immediately
  self.clients.claim();
});

// ==================== FETCH EVENT ====================
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests
  if (request.method !== 'GET') return;

  // Skip chrome-extension and other non-http requests
  if (!url.protocol.startsWith('http')) return;

  // Skip Firebase Realtime Database and Firestore (they handle their own offline)
  if (url.hostname.includes('firebaseio.com') || 
      url.hostname.includes('firestore.googleapis.com') ||
      url.hostname.includes('firebase.googleapis.com')) {
    return;
  }

  // Network-first for API routes
  if (NETWORK_FIRST_ROUTES.some(route => url.pathname.startsWith(route) || url.hostname.includes(route))) {
    event.respondWith(networkFirst(request));
    return;
  }

  // Cache-first for static assets (fonts, images, etc.)
  if (CACHE_FIRST_ROUTES.some(route => url.href.includes(route))) {
    event.respondWith(cacheFirst(request));
    return;
  }

  // Stale-while-revalidate for navigation and other requests
  if (request.mode === 'navigate' || url.origin === self.location.origin) {
    event.respondWith(staleWhileRevalidate(request));
    return;
  }

  // Default: network with cache fallback
  event.respondWith(networkWithCacheFallback(request));
});

// ==================== CACHING STRATEGIES ====================

/**
 * Network-first: Try network, fall back to cache
 */
async function networkFirst(request) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(DYNAMIC_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    const cached = await caches.match(request);
    if (cached) return cached;
    return new Response('Network error', { status: 503, statusText: 'Service Unavailable' });
  }
}

/**
 * Cache-first: Try cache, fall back to network
 */
async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;

  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(DYNAMIC_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    // Return offline placeholder for images
    if (request.destination === 'image') {
      return new Response(
        '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect fill="#1a1a2e" width="200" height="200"/><text fill="#666" font-size="14" x="50%" y="50%" text-anchor="middle" dy=".3em">Offline</text></svg>',
        { headers: { 'Content-Type': 'image/svg+xml' } }
      );
    }
    return new Response('', { status: 503 });
  }
}

/**
 * Stale-while-revalidate: Return cache immediately, update in background
 */
async function staleWhileRevalidate(request) {
  const cache = await caches.open(DYNAMIC_CACHE);
  const cached = await cache.match(request);

  const fetchPromise = fetch(request).then((response) => {
    if (response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  }).catch(() => cached);

  return cached || fetchPromise;
}

/**
 * Network with cache fallback
 */
async function networkWithCacheFallback(request) {
  try {
    const response = await fetch(request);
    return response;
  } catch (error) {
    const cached = await caches.match(request);
    return cached || new Response('', { status: 503 });
  }
}

// ==================== FIREBASE MESSAGING ====================

// Import Firebase scripts for push notifications
importScripts('https://www.gstatic.com/firebasejs/12.14.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.14.0/firebase-messaging-compat.js');

// Initialize Firebase in the service worker
firebase.initializeApp({
  apiKey: "AIzaSyBgNhIaIG5jcRkQ7frreFjo1Cz8F3_JfPk",
  authDomain: "zixo-call.firebaseapp.com",
  databaseURL: "https://zixo-call-default-rtdb.firebaseio.com",
  projectId: "zixo-call",
  storageBucket: "zixo-call.firebasestorage.app",
  messagingSenderId: "809372450511",
  appId: "1:809372450511:web:7910b1a9b8836c7666c1ba",
  measurementId: "G-L792VKMNTT"
});

// Retrieve Firebase Messaging instance
const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
  console.log('[Zixo SW] Background message received:', payload);

  const { title, body, icon } = payload.notification || {};
  const { type, chatId, callerId, callerName } = payload.data || {};

  // Customize notification based on type
  let notificationTitle = title || 'Zixo';
  let notificationOptions = {
    body: body || '',
    icon: '/icon-192.png',
    badge: '/icon-72.png',
    tag: chatId || 'zixo-notification',
    data: {
      type: type || 'message',
      chatId: chatId || '',
      callerId: callerId || '',
      click_action: chatId ? `/chat/${chatId}` : '/'
    },
    vibrate: [200, 100, 200],
    android: {
      channelId: 'zixo-messages',
      priority: 'high'
    } as any,
  };

  // Handle incoming call notifications specially
  if (type === 'call' || type === 'incoming-call') {
    notificationTitle = `\uD83D\uDCDE ${callerName || 'Someone'} is calling`;
    notificationOptions.body = 'Tap to answer';
    notificationOptions.tag = 'zixo-call';
    notificationOptions.requireInteraction = true;
    notificationOptions.actions = [
      { action: 'answer', title: 'Answer' },
      { action: 'decline', title: 'Decline' }
    ];
    notificationOptions.vibrate = [200, 100, 200, 100, 200, 100, 200];
  }

  // Handle message notifications
  if (type === 'message' || type === 'new-message') {
    notificationOptions.actions = [
      { action: 'reply', title: 'Reply' },
      { action: 'mark_read', title: 'Mark as Read' }
    ];
  }

  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// ==================== NOTIFICATION CLICK ====================

self.addEventListener('notificationclick', (event) => {
  console.log('[Zixo SW] Notification click:', event);

  event.notification.close();

  const data = event.notification.data || {};
  const action = event.action;

  // Handle action buttons
  if (action === 'decline' || action === 'mark_read') {
    return;
  }

  // Open the app and navigate to the relevant screen
  const urlToOpen = data.click_action || '/';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // If there's already a window open, focus it
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          return client.focus();
        }
      }
      // Otherwise open a new window
      return self.clients.openWindow(urlToOpen);
    })
  );
});

// ==================== PUSH EVENT ====================

self.addEventListener('push', (event) => {
  console.log('[Zixo SW] Push event received');
  if (!event.data) return;

  const data = event.data.json();
  const { notification, data: payloadData } = data;

  const title = notification?.title || 'Zixo';
  const body = notification?.body || '';
  const type = payloadData?.type || 'message';
  const chatId = payloadData?.chatId || '';
  const callerName = payloadData?.callerName || '';

  const options: NotificationOptions = {
    body,
    icon: '/icon-192.png',
    badge: '/icon-72.png',
    tag: chatId || 'zixo-notification',
    data: payloadData || {},
    vibrate: [200, 100, 200],
  };

  if (type === 'call' || type === 'incoming-call') {
    options.requireInteraction = true;
    options.vibrate = [200, 100, 200, 100, 200, 100, 200];
  }

  event.waitUntil(
    self.registration.showNotification(title, options)
  );
});

// ==================== BACKGROUND SYNC ====================

self.addEventListener('sync', (event) => {
  console.log('[Zixo SW] Background sync:', event.tag);
  
  if (event.tag === 'send-pending-messages') {
    event.waitUntil(sendPendingMessages());
  }
});

async function sendPendingMessages() {
  // Retrieve pending messages from IndexedDB and send them
  // This will be implemented when offline messaging is added
  console.log('[Zixo SW] Processing pending messages...');
}

console.log('[Zixo SW] Service Worker loaded successfully');

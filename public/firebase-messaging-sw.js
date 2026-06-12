/**
 * Firebase Cloud Messaging Service Worker
 * Handles background push notifications for the Zixo app
 */

// Import Firebase scripts for the service worker
importScripts('https://www.gstatic.com/firebasejs/12.14.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.14.0/firebase-messaging-compat.js');

// Initialize Firebase in the service worker
firebase.initializeApp({
  apiKey: "AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA",
  authDomain: "zixo.pages.dev",
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
    icon: '/logo.svg',
    badge: '/logo.svg',
    tag: chatId || 'zixo-notification',
    data: {
      type: type || 'message',
      chatId: chatId || '',
      callerId: callerId || '',
      click_action: chatId ? `/chat/${chatId}` : '/'
    },
    // Android notification channel
    android: {
      channelId: 'zixo-messages',
      priority: 'high'
    }
  };

  // Handle incoming call notifications specially
  if (type === 'call') {
    notificationTitle = `📞 ${callerName || 'Someone'} is calling`;
    notificationOptions.body = 'Tap to answer';
    notificationOptions.tag = 'zixo-call';
    notificationOptions.requireInteraction = true;
    notificationOptions.actions = [
      { action: 'answer', title: 'Answer' },
      { action: 'decline', title: 'Decline' }
    ];
  }

  // Handle message notifications
  if (type === 'message') {
    notificationOptions.actions = [
      { action: 'reply', title: 'Reply' },
      { action: 'mark_read', title: 'Mark as Read' }
    ];
  }

  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click
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

// Handle notification actions (reply, answer, etc.)
self.addEventListener('notificationclick', (event) => {
  if (event.action === 'reply') {
    // Could implement quick reply functionality
    console.log('[Zixo SW] Quick reply action');
  }
}, true);

console.log('[Zixo SW] Service Worker loaded successfully');

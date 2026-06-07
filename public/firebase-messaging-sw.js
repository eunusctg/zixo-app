/**
 * Firebase Cloud Messaging Service Worker
 * Delegates to the main service worker (sw.js) for all functionality.
 * This file exists for FCM compatibility - Firebase Messaging SDK
 * looks for this specific file at the root.
 */

// Import the main service worker which handles both PWA caching and FCM
importScripts('/sw.js');

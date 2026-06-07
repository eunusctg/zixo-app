'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

// Compute initial states outside the component to avoid setState in effects
function getInitialPWAState() {
  if (typeof window === 'undefined') {
    return { isInstalled: false, isOffline: false, wasDismissed: false };
  }
  const isInstalled = window.matchMedia('(display-mode: standalone)').matches;
  const isOffline = !navigator.onLine;
  const dismissed = localStorage.getItem('zixo_install_dismissed');
  let wasDismissed = false;
  if (dismissed) {
    const dismissedTime = parseInt(dismissed, 10);
    const daysSinceDismissed = (Date.now() - dismissedTime) / (1000 * 60 * 60 * 24);
    if (daysSinceDismissed < 7) {
      wasDismissed = true;
    }
  }
  return { isInstalled, isOffline, wasDismissed };
}

/**
 * PWA Install Prompt Component
 * Shows a beautiful install banner when the app is installable.
 * Also registers the service worker and handles offline detection.
 */
export default function PWAInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);
  const [isInstalled, setIsInstalled] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia('(display-mode: standalone)').matches;
  });
  const [isOffline, setIsOffline] = useState(() => {
    if (typeof window === 'undefined') return false;
    return !navigator.onLine;
  });
  const installDismissedRef = useRef(false);

  // Check dismissed state on mount (via ref, no setState needed)
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const dismissed = localStorage.getItem('zixo_install_dismissed');
    if (dismissed) {
      const dismissedTime = parseInt(dismissed, 10);
      const daysSinceDismissed = (Date.now() - dismissedTime) / (1000 * 60 * 60 * 24);
      if (daysSinceDismissed < 7) {
        installDismissedRef.current = true;
      }
    }
  }, []);

  // Register service worker
  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (!('serviceWorker' in navigator)) return;

    navigator.serviceWorker.register('/sw.js', { scope: '/' })
      .then((registration) => {
        console.log('[Zixo PWA] Service Worker registered:', registration.scope);

        // Check for updates every 30 minutes
        setInterval(() => {
          registration.update().catch(() => {});
        }, 30 * 60 * 1000);

        // Handle updates
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'activated') {
                console.log('[Zixo PWA] New service worker activated');
              }
            });
          }
        });
      })
      .catch((err) => {
        console.warn('[Zixo PWA] Service Worker registration failed:', err);
      });
  }, []);

  // Listen for beforeinstallprompt, appinstalled, online/offline
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const handleBeforeInstall = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      // Show prompt after a short delay so it doesn't feel intrusive
      if (!installDismissedRef.current) {
        setTimeout(() => setShowPrompt(true), 3000);
      }
    };

    const handleAppInstalled = () => {
      setIsInstalled(true);
      setShowPrompt(false);
      setDeferredPrompt(null);
      console.log('[Zixo PWA] App installed successfully');
    };

    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);

    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    window.addEventListener('appinstalled', handleAppInstalled);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
      window.removeEventListener('appinstalled', handleAppInstalled);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const handleInstall = useCallback(async () => {
    if (!deferredPrompt) return;

    try {
      await deferredPrompt.prompt();
      const result = await deferredPrompt.userChoice;
      if (result.outcome === 'accepted') {
        console.log('[Zixo PWA] User accepted install prompt');
      }
    } catch (err) {
      console.warn('[Zixo PWA] Install prompt error:', err);
    }

    setDeferredPrompt(null);
    setShowPrompt(false);
  }, [deferredPrompt]);

  const handleDismiss = useCallback(() => {
    setShowPrompt(false);
    installDismissedRef.current = true;
    // Don't show again for 7 days
    if (typeof window !== 'undefined') {
      localStorage.setItem('zixo_install_dismissed', String(Date.now()));
    }
  }, []);

  return (
    <>
      {/* Offline Banner */}
      <AnimatePresence>
        {isOffline && (
          <motion.div
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className="fixed top-0 left-0 right-0 z-[100] bg-zixo-error/90 backdrop-blur-sm text-white text-center py-2 text-xs font-medium safe-area-top"
          >
            <div className="flex items-center justify-center gap-2">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="1" y1="1" x2="23" y2="23" />
                <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55" />
                <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39" />
                <path d="M10.71 5.05A16 16 0 0 1 22.56 9" />
                <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88" />
                <path d="M8.53 16.11a6 6 0 0 1 6.95 0" />
                <line x1="12" y1="20" x2="12.01" y2="20" />
              </svg>
              You&apos;re offline. Some features may be limited.
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Install Prompt */}
      <AnimatePresence>
        {showPrompt && !isInstalled && deferredPrompt && (
          <motion.div
            initial={{ opacity: 0, y: 100 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 100 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            className="fixed bottom-20 left-4 right-4 z-[90] max-w-md mx-auto"
          >
            <div className="bg-[#1F2C34] rounded-2xl p-4 shadow-2xl border border-white/10">
              <div className="flex items-start gap-3">
                <div className="w-12 h-12 rounded-xl gradient-primary flex items-center justify-center shrink-0 shadow-lg">
                  <span className="text-2xl font-extrabold text-white font-heading">Z</span>
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-bold text-zixo-text">Install Zixo</h3>
                  <p className="text-xs text-zixo-text-secondary mt-0.5">
                    Add Zixo to your home screen for faster access and offline support.
                  </p>
                </div>
                <button
                  onClick={handleDismiss}
                  className="w-6 h-6 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors shrink-0"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
              <div className="flex gap-2 mt-3">
                <button
                  onClick={handleDismiss}
                  className="flex-1 py-2.5 rounded-xl bg-zixo-surface-light text-zixo-text-secondary text-xs font-medium hover:text-zixo-text transition-colors"
                >
                  Not now
                </button>
                <button
                  onClick={handleInstall}
                  className="flex-1 py-2.5 rounded-xl gradient-primary text-white text-xs font-semibold glow-primary"
                >
                  Install App
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

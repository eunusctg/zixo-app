import { getToken, onMessage, type MessagePayload } from 'firebase/messaging';
import { messagingPromise, FCM_VAPID_KEY } from './firebase';

// ==================== NOTIFICATION SOUND ====================

let audioContext: AudioContext | null = null;
let notificationSoundEnabled = true;

/**
 * Play a short notification sound using Web Audio API
 * Generates a pleasant two-tone chime without needing an audio file
 */
export function playNotificationSound(): void {
  if (!notificationSoundEnabled) return;
  if (typeof window === 'undefined') return;

  try {
    if (!audioContext) {
      audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }

    const ctx = audioContext;
    const now = ctx.currentTime;

    // First tone - higher pitch
    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(880, now);
    osc1.frequency.exponentialRampToValueAtTime(440, now + 0.15);
    gain1.gain.setValueAtTime(0.3, now);
    gain1.gain.exponentialRampToValueAtTime(0.01, now + 0.2);
    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    osc1.start(now);
    osc1.stop(now + 0.2);

    // Second tone - lower pitch, delayed
    const osc2 = ctx.createOscillator();
    const gain2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(660, now + 0.15);
    osc2.frequency.exponentialRampToValueAtTime(330, now + 0.35);
    gain2.gain.setValueAtTime(0.2, now + 0.15);
    gain2.gain.exponentialRampToValueAtTime(0.01, now + 0.4);
    osc2.connect(gain2);
    gain2.connect(ctx.destination);
    osc2.start(now + 0.15);
    osc2.stop(now + 0.4);
  } catch (err) {
    console.warn('[Zixo] Could not play notification sound:', err);
  }
}

/**
 * Play a ringing sound for incoming calls
 * Generates a more prominent, repeating ring pattern
 * Uses a dedicated audio context so it can be stopped independently
 * of the notification sound.
 */
let ringingAudioCtx: AudioContext | null = null;
let ringingInterval: ReturnType<typeof setInterval> | null = null;

export function playRingingSound(): void {
  if (typeof window === 'undefined') return;

  // Stop any existing ringing sound first
  stopRingingSound();

  try {
    ringingAudioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();

    const playRingCycle = () => {
      if (!ringingAudioCtx) return;
      const ctx = ringingAudioCtx;
      const now = ctx.currentTime;

      // Ring pattern: two tones repeated
      for (let i = 0; i < 3; i++) {
        const offset = i * 0.4;
        // First ring tone
        const osc1 = ctx.createOscillator();
        const gain1 = ctx.createGain();
        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(440, now + offset);
        gain1.gain.setValueAtTime(0.4, now + offset);
        gain1.gain.exponentialRampToValueAtTime(0.01, now + offset + 0.15);
        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.start(now + offset);
        osc1.stop(now + offset + 0.15);

        // Second ring tone (higher)
        const osc2 = ctx.createOscillator();
        const gain2 = ctx.createGain();
        osc2.type = 'sine';
        osc2.frequency.setValueAtTime(520, now + offset + 0.15);
        gain2.gain.setValueAtTime(0.4, now + offset + 0.15);
        gain2.gain.exponentialRampToValueAtTime(0.01, now + offset + 0.3);
        osc2.connect(gain2);
        gain2.connect(ctx.destination);
        osc2.start(now + offset + 0.15);
        osc2.stop(now + offset + 0.3);
      }
    };

    // Play immediately, then repeat every 2 seconds
    playRingCycle();
    ringingInterval = setInterval(playRingCycle, 2000);
  } catch (err) {
    console.warn('[Zixo] Could not play ringing sound:', err);
  }
}

/**
 * Stop the incoming call ringing sound
 */
export function stopRingingSound(): void {
  if (ringingInterval) {
    clearInterval(ringingInterval);
    ringingInterval = null;
  }
  if (ringingAudioCtx) {
    try {
      ringingAudioCtx.close();
    } catch {}
    ringingAudioCtx = null;
  }
}

/**
 * Play an outgoing ring sound (what the caller hears while waiting for the receiver to answer).
 * Uses a repeating dual-tone pattern that sounds like a standard phone ring.
 * Returns a stop function to end the repeating sound when the call is answered or ended.
 */
let outgoingRingInterval: ReturnType<typeof setInterval> | null = null;
let outgoingRingAudioCtx: AudioContext | null = null;

export function playOutgoingRingSound(): () => void {
  if (typeof window === 'undefined') return () => {};

  // Stop any existing outgoing ring
  stopOutgoingRingSound();

  try {
    outgoingRingAudioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();

    const playRingCycle = () => {
      if (!outgoingRingAudioCtx) return;
      const ctx = outgoingRingAudioCtx;
      const now = ctx.currentTime;

      // Standard ring pattern: two tones (440Hz + 480Hz) for 1 second, then 3 seconds silence
      // Play ring tone burst
      for (let i = 0; i < 8; i++) {
        const offset = i * 0.125;

        // First tone (440Hz - North American ring)
        const osc1 = ctx.createOscillator();
        const gain1 = ctx.createGain();
        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(440, now + offset);
        gain1.gain.setValueAtTime(0.15, now + offset);
        gain1.gain.setValueAtTime(0.15, now + offset + 0.1);
        gain1.gain.exponentialRampToValueAtTime(0.01, now + offset + 0.12);
        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.start(now + offset);
        osc1.stop(now + offset + 0.12);

        // Second tone (480Hz)
        const osc2 = ctx.createOscillator();
        const gain2 = ctx.createGain();
        osc2.type = 'sine';
        osc2.frequency.setValueAtTime(480, now + offset);
        gain2.gain.setValueAtTime(0.12, now + offset);
        gain2.gain.setValueAtTime(0.12, now + offset + 0.1);
        gain2.gain.exponentialRampToValueAtTime(0.01, now + offset + 0.12);
        osc2.connect(gain2);
        gain2.connect(ctx.destination);
        osc2.start(now + offset);
        osc2.stop(now + offset + 0.12);
      }
    };

    // Play immediately, then repeat every 4 seconds (1s ring + 3s silence)
    playRingCycle();
    outgoingRingInterval = setInterval(playRingCycle, 4000);

  } catch (err) {
    console.warn('[Zixo] Could not play outgoing ring sound:', err);
  }

  return stopOutgoingRingSound;
}

/**
 * Stop the outgoing ring sound
 */
export function stopOutgoingRingSound(): void {
  if (outgoingRingInterval) {
    clearInterval(outgoingRingInterval);
    outgoingRingInterval = null;
  }
  if (outgoingRingAudioCtx) {
    try {
      outgoingRingAudioCtx.close();
    } catch {}
    outgoingRingAudioCtx = null;
  }
}

export function setNotificationSoundEnabled(enabled: boolean): void {
  notificationSoundEnabled = enabled;
}

export function isNotificationSoundEnabled(): boolean {
  return notificationSoundEnabled;
}

// ==================== MESSAGE BADGE ====================

let unreadCount = 0;
const originalTitle = typeof document !== 'undefined' ? document.title : 'Zixo';

/**
 * Update the document title to show unread count
 */
export function updateMessageBadge(count: number): void {
  unreadCount = count;
  if (typeof document === 'undefined') return;

  if (count > 0) {
    document.title = `(${count}) Zixo`;
  } else {
    document.title = 'Zixo';
  }
}

/**
 * Increment the unread message badge
 */
export function incrementMessageBadge(): void {
  updateMessageBadge(unreadCount + 1);
}

/**
 * Clear the unread message badge
 */
export function clearMessageBadge(): void {
  updateMessageBadge(0);
}

/**
 * Get current unread count
 */
export function getUnreadCount(): number {
  return unreadCount;
}

// ==================== NOTIFICATION BANNER STATE ====================

type BannerCallback = (notification: { id: string; senderName: string; senderAvatar?: string; messagePreview: string; chatId?: string; type: 'message' | 'call' }) => void;
let bannerListeners: BannerCallback[] = [];

/**
 * Register a listener for notification banner events
 */
export function onNotificationBanner(callback: BannerCallback): () => void {
  bannerListeners.push(callback);
  return () => {
    bannerListeners = bannerListeners.filter(cb => cb !== callback);
  };
}

/**
 * Show a notification banner (called from FCM foreground handler)
 */
export function showBannerNotification(data: {
  senderName: string;
  senderAvatar?: string;
  messagePreview: string;
  chatId?: string;
  type?: 'message' | 'call';
}): void {
  const notification = {
    id: `notif-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    senderName: data.senderName,
    senderAvatar: data.senderAvatar,
    messagePreview: data.messagePreview,
    chatId: data.chatId,
    type: data.type || 'message' as const,
  };

  bannerListeners.forEach(cb => cb(notification));
}

// ==================== BACKGROUND NOTIFICATION HANDLER ====================

/**
 * Initialize background notification support
 * Uses the Notification API for when the app is not in focus
 */
export function initBackgroundNotifications(): void {
  if (typeof window === 'undefined') return;

  // Listen for visibility changes to track when app goes to background
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      // App came to foreground - clear badge
      clearMessageBadge();
    }
  });
}

/**
 * Show a browser notification when the app is not in focus
 */
export function showBrowserNotification(title: string, body: string, data?: Record<string, string>): void {
  if (typeof window === 'undefined') return;
  if (document.visibilityState === 'visible') return; // Don't show if app is visible

  try {
    if (Notification.permission !== 'granted') return;

    const notification = new Notification(title, {
      body,
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      tag: data?.chatId || 'zixo-notification',
      data,
      silent: false,
    });

    notification.onclick = () => {
      window.focus();
      notification.close();
      // Navigate to chat if we have a chatId
      if (data?.chatId) {
        // The app will handle navigation via its own routing
        window.dispatchEvent(new CustomEvent('zixo-navigate-chat', { detail: { chatId: data.chatId } }));
      }
    };

    // Auto-close after 10 seconds
    setTimeout(() => notification.close(), 10000);
  } catch (err) {
    console.warn('[Zixo] Could not show browser notification:', err);
  }
}

// ==================== FCM CORE ====================

/**
 * Request notification permission and get FCM token
 */
export async function requestNotificationPermission(): Promise<string | null> {
  try {
    const messaging = await messagingPromise;
    if (!messaging) {
      console.warn('FCM messaging not supported in this browser');
      return null;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      console.warn('Notification permission denied');
      return null;
    }

    const token = await getToken(messaging, { vapidKey: FCM_VAPID_KEY });
    return token;
  } catch (error) {
    console.error('Error getting FCM token:', error);
    return null;
  }
}

/**
 * Listen to foreground messages (when app is in focus)
 * Returns a cleanup function that properly unsubscribes
 */
export function onForegroundMessage(callback: (payload: MessagePayload) => void): () => void {
  let unsubscribe: (() => void) | null = null;
  let cleanedUp = false;

  messagingPromise.then((messaging) => {
    if (messaging && !cleanedUp) {
      unsubscribe = onMessage(messaging, callback);
    }
  });

  return () => {
    cleanedUp = true;
    if (unsubscribe) unsubscribe();
  };
}

/**
 * Save FCM token to user's profile for server-side push
 */
export async function saveFCMToken(uid: string, token: string): Promise<void> {
  const { doc, setDoc } = await import('firebase/firestore');
  const { db } = await import('./firebase');
  await setDoc(doc(db, 'users', uid), { fcmToken: token }, { merge: true });
}

/**
 * Initialize FCM: request permission, get token, save it, listen for messages
 * Enhanced with notification banners, sounds, and badge updates
 */
export async function initFCM(uid: string): Promise<() => void> {
  // Request permission and get token
  const token = await requestNotificationPermission();

  if (token) {
    // Save token to user profile
    await saveFCMToken(uid, token);
  }

  // Initialize background notification support
  initBackgroundNotifications();

  // Listen for foreground messages
  const unsubscribe = onForegroundMessage((payload) => {
    console.log('Foreground message received:', payload);

    // Show in-app notification banner
    if (payload.notification) {
      const { title, body } = payload.notification;
      const chatId = payload.data?.chatId as string | undefined;
      const type = (payload.data?.type as string) === 'incoming-call' ? 'call' as const : 'message' as const;

      showBannerNotification({
        senderName: title || 'Zixo',
        messagePreview: body || '',
        chatId,
        type,
      });

      // Play notification sound
      if (type === 'call') {
        playRingingSound();
      } else {
        playNotificationSound();
      }

      // Increment badge
      incrementMessageBadge();

      // Show browser notification if app is not focused
      showBrowserNotification(title || 'Zixo', body || '', payload.data as Record<string, string> | undefined);
    }
  });

  return unsubscribe;
}

// ==================== CALL SOUNDS ====================

let incomingRingAudioContext: AudioContext | null = null;
let incomingRingOscillators: OscillatorNode[] = [];
let incomingRingInterval: ReturnType<typeof setInterval> | null = null;

/**
 * Play incoming call ring sound (dual-tone ringing pattern)
 */
export function playIncomingRingSound(): void {
  stopIncomingRingSound();

  try {
    incomingRingAudioContext = new AudioContext();

    const playRing = () => {
      if (!incomingRingAudioContext) return;

      const osc1 = incomingRingAudioContext.createOscillator();
      const osc2 = incomingRingAudioContext.createOscillator();
      const gain = incomingRingAudioContext.createGain();

      osc1.frequency.value = 440; // A4
      osc2.frequency.value = 480; // Bb4
      gain.gain.value = 0.15;

      osc1.connect(gain);
      osc2.connect(gain);
      gain.connect(incomingRingAudioContext.destination);

      osc1.start();
      osc2.start();

      // Ring for 1 second, then silence for 2 seconds
      gain.gain.setValueAtTime(0.15, incomingRingAudioContext.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, incomingRingAudioContext.currentTime + 1.0);

      osc1.stop(incomingRingAudioContext.currentTime + 1.0);
      osc2.stop(incomingRingAudioContext.currentTime + 1.0);

      incomingRingOscillators.push(osc1, osc2);
    };

    playRing();
    incomingRingInterval = setInterval(playRing, 3000); // Ring every 3 seconds
  } catch (err) {
    console.warn('[Zixo] Failed to play incoming ring sound:', err);
  }
}

/**
 * Stop incoming call ring sound
 */
export function stopIncomingRingSound(): void {
  if (incomingRingInterval) {
    clearInterval(incomingRingInterval);
    incomingRingInterval = null;
  }

  incomingRingOscillators.forEach((osc) => {
    try { osc.stop(); } catch {}
  });
  incomingRingOscillators = [];

  if (incomingRingAudioContext) {
    try { incomingRingAudioContext.close(); } catch {}
    incomingRingAudioContext = null;
  }
}



/**
 * Send a push notification to a specific user via the server API
 * This is "forceful" - it sends even if the user is online
 */
export async function sendPushNotification(
  uid: string,
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<boolean> {
  try {
    const response = await fetch('/api/zixo', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        action: 'sendPush',
        uid,
        title,
        body,
        data,
      }),
    });
    const result = await response.json();
    return result.success === true;
  } catch (err) {
    console.error('[Zixo] Push notification failed:', err);
    return false;
  }
}

/**
 * Show an in-app notification banner (XSS-safe)
 * @deprecated Use showBannerNotification instead for the React-based banner
 */
function showInAppNotification(title: string, body: string) {
  // This is now handled by the React NotificationBanner component
  // via the showBannerNotification function above
  showBannerNotification({
    senderName: title,
    messagePreview: body,
    type: 'message',
  });
}

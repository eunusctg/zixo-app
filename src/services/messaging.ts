import { getToken, onMessage, type MessagePayload } from 'firebase/messaging';
import { messagingPromise, FCM_VAPID_KEY } from './firebase';

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
 */
export function onForegroundMessage(callback: (payload: MessagePayload) => void): () => void {
  let unsubscribe: (() => void) | null = null;

  messagingPromise.then((messaging) => {
    if (messaging) {
      unsubscribe = onMessage(messaging, callback);
    }
  });

  return () => {
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
 */
export async function initFCM(uid: string): Promise<() => void> {
  // Request permission and get token
  const token = await requestNotificationPermission();

  if (token) {
    // Save token to user profile
    await saveFCMToken(uid, token);
  }

  // Listen for foreground messages
  const unsubscribe = onForegroundMessage((payload) => {
    console.log('Foreground message received:', payload);

    // Show in-app notification
    if (payload.notification) {
      const { title, body } = payload.notification;
      showInAppNotification(title || 'Zixo', body || '');
    }
  });

  return unsubscribe;
}

/**
 * Show an in-app notification banner
 */
function showInAppNotification(title: string, body: string) {
  // Create a custom notification element
  if (typeof document === 'undefined') return;

  const notif = document.createElement('div');
  notif.id = 'zixo-notification';
  notif.style.cssText = `
    position: fixed; top: 20px; right: 20px; z-index: 9999;
    background: rgba(26, 26, 46, 0.95); backdrop-filter: blur(20px);
    border: 1px solid rgba(108, 92, 231, 0.3);
    border-radius: 16px; padding: 16px 20px; max-width: 320px;
    color: white; font-family: 'Inter', sans-serif;
    box-shadow: 0 8px 32px rgba(0,0,0,0.4);
    animation: slideDown 0.3s ease-out;
  `;

  notif.innerHTML = `
    <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
      <div style="width:24px;height:24px;border-radius:8px;background:linear-gradient(135deg,#6C5CE7,#00D2D3);display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:12px;">Z</div>
      <span style="font-weight:600;font-size:14px;">${title}</span>
    </div>
    <p style="font-size:13px;color:#B0B0C3;margin:0;">${body}</p>
  `;

  // Add animation
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideDown { from { transform: translateY(-20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
  `;
  document.head.appendChild(style);
  document.body.appendChild(notif);

  // Auto-remove after 4 seconds
  setTimeout(() => {
    notif.style.opacity = '0';
    notif.style.transition = 'opacity 0.3s';
    setTimeout(() => {
      notif.remove();
      style.remove();
    }, 300);
  }, 4000);
}

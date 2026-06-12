import { initializeApp, getApps } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getDatabase } from 'firebase/database';
import { getMessaging, isSupported } from 'firebase/messaging';
import { getStorage } from 'firebase/storage';
import { getAnalytics, isSupported as isAnalyticsSupported } from 'firebase/analytics';

// ==================== FIREBASE CONFIGURATION ====================
// Production credentials for Zixo app (zixo-call project)
const firebaseConfig = {
  apiKey: "AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA",
  authDomain: "zixo.pages.dev",
  databaseURL: "https://zixo-call-default-rtdb.firebaseio.com",
  projectId: "zixo-call",
  storageBucket: "zixo-call.firebasestorage.app",
  messagingSenderId: "809372450511",
  appId: "1:809372450511:web:7910b1a9b8836c7666c1ba",
  measurementId: "G-L792VKMNTT"
};

// FCM VAPID key for push notifications
export const FCM_VAPID_KEY = "BFnV-bHlDzMT9eszuKzQMpsPhrlXx8ClrLRhXhYrLuu1wQ10GugFPYZL5Brn8xMoHNmb0JxTAEDwfwD9y6zj9xk";

// ==================== INITIALIZE FIREBASE ====================
// Prevent re-initialization on HMR (Hot Module Replacement)
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

// ==================== FIREBASE SERVICES ====================

// Authentication
export const auth = getAuth(app);

// Cloud Firestore (primary database for chats, messages, users)
export const db = getFirestore(app);

// Realtime Database (for presence, typing indicators, call signaling)
export const rtdb = getDatabase(app);

// Cloud Storage (for media files - images, voice notes, documents)
export const storage = getStorage(app);

// Cloud Messaging (push notifications)
export const messagingPromise = typeof window !== 'undefined'
  ? isSupported().then(supported => supported ? getMessaging(app) : null)
  : Promise.resolve(null);

// Analytics (usage tracking)
export const analyticsPromise = typeof window !== 'undefined'
  ? isAnalyticsSupported().then(supported => supported ? getAnalytics(app) : null)
  : Promise.resolve(null);

export default app;

// ==================== ENVIRONMENT VARIABLES ====================
// These are stored in .env.local for security
// - NEXT_PUBLIC_FIREBASE_DATABASE_SECRET: Used for Realtime Database auth (server-side only)
// - FIREBASE_SERVICE_ACCOUNT: Used for admin operations (server-side only)
// - FIREBASE_SERVICE_ACCOUNT_EMAIL: firebase-adminsdk-fbsvc@zixo-call.iam.gserviceaccount.com

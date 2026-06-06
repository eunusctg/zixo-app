import { initializeApp, getApps } from 'firebase/app';
import { getAuth, connectAuthEmulator } from 'firebase/auth';
import { getFirestore, connectFirestoreEmulator } from 'firebase/firestore';
import { getMessaging, isSupported } from 'firebase/messaging';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: "AIzaSyBgNhIaIG5jcRkQ7frreFjo1Cz8F3_JfPk",
  authDomain: "zixo-call.firebaseapp.com",
  projectId: "zixo-call",
  storageBucket: "zixo-call.firebasestorage.app",
  messagingSenderId: "809372450511",
  appId: "1:809372450511:web:7910b1a9b8836c7666c1ba",
  measurementId: "G-L792VKMNTT"
};

// FCM VAPID key for push notifications
export const FCM_VAPID_KEY = "BFnV-bHlDzMT9eszuKzQMpsPhrlXx8ClrLRhXhYrLuu1wQ10GugFPYZL5Brn8xMoHNmb0JxTAEDwfwD9y6zj9xk";

// Initialize Firebase (prevent re-initialization on HMR)
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

// Initialize services
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);

// Initialize messaging (browser only)
export const messagingPromise = typeof window !== 'undefined'
  ? isSupported().then(supported => supported ? getMessaging(app) : null)
  : Promise.resolve(null);

export default app;

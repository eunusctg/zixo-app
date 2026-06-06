import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile,
  onAuthStateChanged,
  setPersistence,
  browserLocalPersistence,
  browserSessionPersistence,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  type User,
  type AuthError,
  type ConfirmationResult,
} from 'firebase/auth';
import { doc, setDoc, getDoc, serverTimestamp } from 'firebase/firestore';
import { auth, db } from './firebase';

const googleProvider = new GoogleAuthProvider();

// Scopes for Google Sign-In
googleProvider.addScope('profile');
googleProvider.addScope('email');

// ==================== AUTH ERROR MESSAGES ====================

/**
 * Map Firebase Auth error codes to user-friendly messages
 */
export function getAuthErrorMessage(error: AuthError | { code?: string }): string {
  const code = (error as AuthError).code || '';
  const errorMessages: Record<string, string> = {
    'auth/email-already-in-use': 'This email is already registered. Try signing in instead.',
    'auth/invalid-email': 'Please enter a valid email address.',
    'auth/operation-not-allowed': 'This sign-in method is not enabled. Please contact support.',
    'auth/weak-password': 'Password is too weak. Use at least 6 characters with a mix of letters and numbers.',
    'auth/user-disabled': 'This account has been disabled. Please contact support.',
    'auth/user-not-found': 'No account found with this email. Please sign up first.',
    'auth/wrong-password': 'Incorrect password. Please try again or reset your password.',
    'auth/invalid-credential': 'Invalid email or password. Please check your credentials and try again.',
    'auth/too-many-requests': 'Too many attempts. Please wait a moment and try again.',
    'auth/network-request-failed': 'Network error. Please check your internet connection and try again.',
    'auth/popup-closed-by-user': 'Sign-in was cancelled.',
    'auth/popup-blocked': 'Pop-up was blocked by your browser. Please allow pop-ups and try again.',
    'auth/cancelled-popup-request': 'Only one sign-in popup is allowed at a time.',
    'auth/credential-already-in-use': 'This credential is already linked to another account.',
    'auth/requires-recent-login': 'This operation requires a recent login. Please sign in again.',
    'auth/unverified-email': 'Please verify your email before signing in.',
    'auth/account-exists-with-different-credential': 'An account already exists with this email using a different sign-in method.',
    'auth/invalid-verification-code': 'The verification code is invalid. Please try again.',
    'auth/invalid-verification-id': 'The verification ID is invalid. Please restart the verification process.',
    'auth/expired-action-code': 'This link has expired. Please request a new one.',
    'auth/invalid-action-code': 'This link is invalid. Please request a new one.',
  };

  return errorMessages[code] || 'An unexpected error occurred. Please try again.';
}

/**
 * Check if error is a rate limiting error
 */
export function isRateLimitError(error: AuthError | { code?: string }): boolean {
  const code = (error as AuthError).code || '';
  return code === 'auth/too-many-requests';
}

// ==================== SESSION PERSISTENCE ====================

/**
 * Configure session persistence
 * @param remember - If true, persists across browser sessions (default). If false, clears on tab close.
 */
export async function setSessionPersistence(remember: boolean): Promise<void> {
  await setPersistence(auth, remember ? browserLocalPersistence : browserSessionPersistence);
}

// Initialize with local persistence (remember user across sessions)
setPersistence(auth, browserLocalPersistence).catch(console.error);

// ==================== EMAIL VERIFICATION ====================

/**
 * Send email verification to the current user
 */
export async function sendVerificationEmail(): Promise<void> {
  if (auth.currentUser && !auth.currentUser.emailVerified) {
    await sendEmailVerification(auth.currentUser);
  }
}

/**
 * Check if the current user's email is verified
 */
export function isEmailVerified(): boolean {
  return auth.currentUser?.emailVerified ?? false;
}

/**
 * Reload the current user to check if email has been verified
 */
export async function reloadUser(): Promise<boolean> {
  if (auth.currentUser) {
    await auth.currentUser.reload();
    return auth.currentUser.emailVerified;
  }
  return false;
}

export interface ZixoUserProfile {
  uid: string;
  displayName: string;
  email: string;
  username: string;
  bio: string;
  avatar: string;
  online: boolean;
  lastSeen: number;
  createdAt: number;
  publicKey?: string;
  role?: 'admin' | 'user';
}

/**
 * Check if a user profile has admin role
 */
export function isAdmin(user: ZixoUserProfile | null): boolean {
  return user?.role === 'admin';
}

/**
 * Register a new user with email/password
 */
export async function registerWithEmail(
  email: string,
  password: string,
  displayName: string
): Promise<{ user: User; profile: ZixoUserProfile }> {
  const credential = await createUserWithEmailAndPassword(auth, email, password);
  const user = credential.user;

  // Update Firebase Auth display name
  await updateProfile(user, { displayName });

  // Send email verification
  await sendEmailVerification(user).catch(console.error);

  // Create Firestore user profile
  const username = `@${displayName.toLowerCase().replace(/\s+/g, '')}${Math.floor(Math.random() * 1000)}`;
  const profile: ZixoUserProfile = {
    uid: user.uid,
    displayName,
    email,
    username,
    bio: 'Living free, connecting freely 🌍',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
    createdAt: Date.now(),
  };

  await setDoc(doc(db, 'users', user.uid), {
    ...profile,
    createdAt: serverTimestamp(),
    lastSeen: serverTimestamp(),
  });

  // Also create username mapping for search
  await setDoc(doc(db, 'usernames', username), { uid: user.uid });

  return { user, profile };
}

/**
 * Sign in with email/password
 */
export async function loginWithEmail(
  email: string,
  password: string
): Promise<{ user: User; profile: ZixoUserProfile }> {
  const credential = await signInWithEmailAndPassword(auth, email, password);
  const profile = await getUserProfile(credential.user.uid);

  // Update online status
  await updateOnlineStatus(credential.user.uid, true);

  return { user: credential.user, profile };
}

/**
 * Sign in with Google
 */
export async function loginWithGoogle(): Promise<{ user: User; profile: ZixoUserProfile }> {
  const credential = await signInWithPopup(auth, googleProvider);
  const user = credential.user;

  // Check if user profile exists
  let profile = await getUserProfile(user.uid);

  if (!profile) {
    // First-time Google sign-in: create profile
    const username = `@${(user.displayName || user.email?.split('@')[0] || 'user').toLowerCase().replace(/\s+/g, '')}${Math.floor(Math.random() * 1000)}`;
    profile = {
      uid: user.uid,
      displayName: user.displayName || user.email?.split('@')[0] || 'User',
      email: user.email || '',
      username,
      bio: 'Living free, connecting freely 🌍',
      avatar: user.photoURL || '',
      online: true,
      lastSeen: Date.now(),
      createdAt: Date.now(),
    };

    await setDoc(doc(db, 'users', user.uid), {
      ...profile,
      createdAt: serverTimestamp(),
      lastSeen: serverTimestamp(),
    });
    await setDoc(doc(db, 'usernames', username), { uid: user.uid });
  } else {
    await updateOnlineStatus(user.uid, true);
  }

  return { user, profile };
}

/**
 * Send password reset email
 */
export async function resetPassword(email: string): Promise<void> {
  await sendPasswordResetEmail(auth, email);
}

/**
 * Sign out the current user
 */
export async function logoutUser(): Promise<void> {
  if (auth.currentUser) {
    await updateOnlineStatus(auth.currentUser.uid, false);
  }
  await signOut(auth);
}

/**
 * Get user profile from Firestore
 */
export async function getUserProfile(uid: string): Promise<ZixoUserProfile | null> {
  const docRef = doc(db, 'users', uid);
  const docSnap = await getDoc(docRef);

  if (docSnap.exists()) {
    const data = docSnap.data();
    return {
      uid: data.uid,
      displayName: data.displayName || '',
      email: data.email || '',
      username: data.username || '',
      bio: data.bio || '',
      avatar: data.avatar || '',
      online: data.online || false,
      lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
      createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
      publicKey: data.publicKey,
      role: data.role || 'user',
    };
  }
  return null;
}

/**
 * Update user online status
 */
export async function updateOnlineStatus(uid: string, online: boolean): Promise<void> {
  const docRef = doc(db, 'users', uid);
  await setDoc(docRef, {
    online,
    lastSeen: serverTimestamp(),
  }, { merge: true });
}

/**
 * Update user profile
 */
export async function updateUserProfile(
  uid: string,
  updates: Partial<Pick<ZixoUserProfile, 'displayName' | 'bio' | 'avatar' | 'username'>>
): Promise<void> {
  const docRef = doc(db, 'users', uid);
  await setDoc(docRef, updates, { merge: true });

  // If username changed, update mapping
  if (updates.username) {
    await setDoc(doc(db, 'usernames', updates.username), { uid });
  }
}

/**
 * Listen to auth state changes
 */
export function onAuthChange(callback: (user: User | null) => void) {
  return onAuthStateChanged(auth, callback);
}

/**
 * Search for a user by username (exact match)
 */
export async function searchUserByUsername(username: string): Promise<ZixoUserProfile | null> {
  const { collection, query, where, getDocs } = await import('firebase/firestore');
  const q = query(collection(db, 'users'), where('username', '==', username));
  const snapshot = await getDocs(q);

  if (!snapshot.empty) {
    const data = snapshot.docs[0].data();
    return {
      uid: data.uid,
      displayName: data.displayName || '',
      email: data.email || '',
      username: data.username || '',
      bio: data.bio || '',
      avatar: data.avatar || '',
      online: data.online || false,
      lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
      createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
      role: data.role || 'user',
    };
  }
  return null;
}

/**
 * Search for users by display name or username (prefix match)
 * Returns multiple results sorted by displayName
 */
export async function searchUsers(searchText: string, maxResults: number = 20): Promise<ZixoUserProfile[]> {
  const { collection, query, where, getDocs, limit: firestoreLimit } = await import('firebase/firestore');
  const searchLower = searchText.toLowerCase().replace(/^@/, '');
  const searchUpper = searchLower + '\uf8ff';
  const results: Map<string, ZixoUserProfile> = new Map();

  try {
    // Search by username prefix
    const q1 = query(
      collection(db, 'users'),
      where('username', '>=', `@${searchLower}`),
      where('username', '<', `@${searchUpper}`),
      firestoreLimit(maxResults)
    );
    const snap1 = await getDocs(q1);
    snap1.forEach((docSnap) => {
      const data = docSnap.data();
      if (!results.has(data.uid)) {
        results.set(data.uid, {
          uid: data.uid,
          displayName: data.displayName || '',
          email: data.email || '',
          username: data.username || '',
          bio: data.bio || '',
          avatar: data.avatar || '',
          online: data.online || false,
          lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
          createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
          role: data.role || 'user',
        });
      }
    });
  } catch (err) {
    console.warn('[Zixo] Username prefix search failed:', err);
  }

  try {
    // Search by displayName prefix (case-sensitive, uses lowercase)
    const q2 = query(
      collection(db, 'users'),
      where('displayName', '>=', searchLower.charAt(0).toUpperCase() + searchLower.slice(1)),
      where('displayName', '<', searchLower.charAt(0).toUpperCase() + searchLower.slice(1) + '\uf8ff'),
      firestoreLimit(maxResults)
    );
    const snap2 = await getDocs(q2);
    snap2.forEach((docSnap) => {
      const data = docSnap.data();
      if (!results.has(data.uid)) {
        results.set(data.uid, {
          uid: data.uid,
          displayName: data.displayName || '',
          email: data.email || '',
          username: data.username || '',
          bio: data.bio || '',
          avatar: data.avatar || '',
          online: data.online || false,
          lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
          createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
          role: data.role || 'user',
        });
      }
    });
  } catch (err) {
    console.warn('[Zixo] DisplayName prefix search failed:', err);
  }

  // If no prefix results, try client-side filtering of recent users
  if (results.size === 0) {
    try {
      const q3 = query(collection(db, 'users'), firestoreLimit(50));
      const snap3 = await getDocs(q3);
      snap3.forEach((docSnap) => {
        const data = docSnap.data();
        const nameMatch = (data.displayName || '').toLowerCase().includes(searchLower);
        const unameMatch = (data.username || '').toLowerCase().includes(searchLower);
        if ((nameMatch || unameMatch) && !results.has(data.uid)) {
          results.set(data.uid, {
            uid: data.uid,
            displayName: data.displayName || '',
            email: data.email || '',
            username: data.username || '',
            bio: data.bio || '',
            avatar: data.avatar || '',
            online: data.online || false,
            lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
            createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
            role: data.role || 'user',
          });
        }
      });
    } catch (err) {
      console.warn('[Zixo] Client-side user search failed:', err);
    }
  }

  return Array.from(results.values()).slice(0, maxResults);
}

/**
 * Get all users on Zixo (for browsing/discovery)
 * Returns up to maxResults users, excluding the current user
 */
export async function getAllUsers(currentUid: string, maxResults: number = 50): Promise<ZixoUserProfile[]> {
  const { collection, query, getDocs, limit: firestoreLimit } = await import('firebase/firestore');
  const users: ZixoUserProfile[] = [];

  try {
    const q = query(collection(db, 'users'), firestoreLimit(maxResults + 1));
    const snapshot = await getDocs(q);
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      if (data.uid !== currentUid) {
        users.push({
          uid: data.uid,
          displayName: data.displayName || '',
          email: data.email || '',
          username: data.username || '',
          bio: data.bio || '',
          avatar: data.avatar || '',
          online: data.online || false,
          lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
          createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
          role: data.role || 'user',
        });
      }
    });
  } catch (err) {
    console.warn('[Zixo] Failed to fetch all users:', err);
  }

  return users.slice(0, maxResults);
}

// ==================== PHONE AUTH (OTP) ====================

let recaptchaVerifier: RecaptchaVerifier | null = null;
let confirmationResult: ConfirmationResult | null = null;

/**
 * Initialize the invisible reCAPTCHA verifier for phone auth
 * @param buttonId - The ID of the container element for the reCAPTCHA
 */
export function initRecaptcha(buttonId: string): void {
  if (recaptchaVerifier) return;
  recaptchaVerifier = new RecaptchaVerifier(auth, buttonId, {
    size: 'invisible',
    callback: () => {
      // reCAPTCHA solved
    },
  });
}

/**
 * Send OTP to the given phone number
 * @param phoneNumber - Phone number in +XX format (e.g., +1234567890)
 */
export async function sendOTP(phoneNumber: string): Promise<void> {
  if (!recaptchaVerifier) {
    throw new Error('Recaptcha not initialized. Call initRecaptcha first.');
  }
  confirmationResult = await signInWithPhoneNumber(auth, phoneNumber, recaptchaVerifier);
}

/**
 * Verify the OTP code sent to the user's phone
 * @param code - The 6-digit OTP code
 */
export async function verifyOTP(code: string): Promise<{ user: User; profile: ZixoUserProfile }> {
  if (!confirmationResult) {
    throw new Error('No confirmation result. Send OTP first.');
  }
  const result = await confirmationResult.confirm(code);
  const user = result.user;

  // Get or create profile
  let profile = await getUserProfile(user.uid);
  if (!profile) {
    profile = {
      uid: user.uid,
      displayName: user.displayName || user.phoneNumber || 'User',
      email: user.email || '',
      username: `@user${Math.floor(Math.random() * 10000)}`,
      bio: '',
      avatar: user.photoURL || '',
      online: true,
      lastSeen: Date.now(),
      createdAt: Date.now(),
      role: 'user',
    };
    // Create Firestore profile
    await setDoc(doc(db, 'users', user.uid), {
      ...profile,
      createdAt: serverTimestamp(),
      lastSeen: serverTimestamp(),
    });
  }
  return { user, profile };
}

/**
 * Reset phone auth state
 */
export function resetPhoneAuth(): void {
  if (recaptchaVerifier) {
    recaptchaVerifier.clear();
    recaptchaVerifier = null;
  }
  confirmationResult = null;
}

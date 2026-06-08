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
import { ref, set as rtdbSet, update as rtdbUpdate } from 'firebase/database';
import { auth, db, rtdb } from './firebase';

// ==================== RTDB PROFILE SYNC ====================

/**
 * Sync user profile data to RTDB /users/{uid} for fallback access.
 * This ensures the RTDB fallback in the API route works even when
 * FIREBASE_PRIVATE_KEY is not set (Firestore REST API unavailable).
 * Excludes large fields like publicKey.
 */
async function syncUserProfileToRTDB(uid: string, profile: Record<string, any>, merge: boolean = false): Promise<void> {
  try {
    const { publicKey, ...rtdbProfile } = profile;
    // Convert any Firestore timestamps to numbers for RTDB compatibility
    const cleanProfile: Record<string, any> = {};
    for (const [key, value] of Object.entries(rtdbProfile)) {
      if (value && typeof value === 'object' && typeof value.toMillis === 'function') {
        cleanProfile[key] = value.toMillis();
      } else {
        cleanProfile[key] = value;
      }
    }
    if (merge) {
      await rtdbUpdate(ref(rtdb, `users/${uid}`), cleanProfile);
    } else {
      await rtdbSet(ref(rtdb, `users/${uid}`), cleanProfile);
    }
  } catch (err) {
    console.warn('[Zixo Auth] Failed to sync profile to RTDB:', err);
  }
}

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
    'auth/unauthorized-domain': 'This domain is not authorized for sign-in. Please add it in Firebase Console > Authentication > Settings > Authorized domains, or try accessing the app from an authorized domain.',
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
// Note: Also set in firebase.ts to ensure it's called before auth state listener starts.
// Keeping this here as a safety net in case firebase.ts import is delayed.
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
  phoneNumber?: string;
  zixoNumber?: string;
}

/**
 * Check if a user profile has admin role
 */
export function isAdmin(user: ZixoUserProfile | null): boolean {
  return user?.role === 'admin';
}

/**
 * Generate a unique 8-digit Zixo Number
 * Checks the zixoNumbers Firestore collection to ensure uniqueness
 * Retries up to 10 times if collision occurs
 */
export async function generateUniqueZixoNumber(): Promise<string> {
  const MAX_ATTEMPTS = 10;

  // Null-check: if db is undefined, return a fallback number
  if (!db) {
    console.warn('[Zixo Auth] Firestore db is undefined in generateUniqueZixoNumber, returning fallback');
    return String(Date.now()).slice(-8);
  }

  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    // Generate random 8-digit number (10000000 - 99999999)
    const num = Math.floor(Math.random() * 90000000) + 10000000;
    const zixoNumber = String(num);

    try {
      // Check if it already exists in the zixoNumbers collection
      const docRef = doc(db, 'zixoNumbers', zixoNumber);
      const docSnap = await getDoc(docRef);

      if (!docSnap.exists()) {
        return zixoNumber;
      }
    } catch (err) {
      console.warn('[Zixo Auth] Error checking zixoNumber uniqueness, using generated number:', err);
      return zixoNumber;
    }
  }

  // Extremely unlikely fallback - just return a timestamp-based number
  const fallback = String(Date.now()).slice(-8);
  return fallback;
}

/**
 * Ensure a user profile has a zixoNumber assigned.
 * If missing, generates one and saves it to both the user profile and the zixoNumbers mapping.
 * Always checks Firestore first to avoid creating duplicate mappings.
 */
export async function ensureZixoNumber(uid: string, existingProfile?: Record<string, any>): Promise<string> {
  // Check if the profile already has a zixoNumber
  if (existingProfile?.zixoNumber) {
    return existingProfile.zixoNumber;
  }

  // Null-check: if db is undefined (shouldn't happen but defensive), return a fallback
  if (!db) {
    console.warn('[Zixo Auth] Firestore db is undefined in ensureZixoNumber, returning fallback');
    return String(Date.now()).slice(-8);
  }

  // Double-check Firestore in case the profile data is stale
  try {
    const userDoc = await getDoc(doc(db, 'users', uid));
    if (userDoc.exists()) {
      const data = userDoc.data();
      if (data?.zixoNumber) {
        // Also ensure the mapping exists in zixoNumbers collection
        const mappingRef = doc(db, 'zixoNumbers', data.zixoNumber);
        const mappingSnap = await getDoc(mappingRef);
        if (!mappingSnap.exists()) {
          await setDoc(mappingRef, { uid });
        }
        return data.zixoNumber;
      }
    }
  } catch (err) {
    console.warn('[Zixo Auth] Failed to check Firestore for existing zixoNumber:', err);
  }

  // Generate and assign a new zixoNumber
  let zixoNumber: string;
  try {
    zixoNumber = await generateUniqueZixoNumber();
  } catch (err) {
    console.warn('[Zixo Auth] Failed to generate zixoNumber:', err);
    zixoNumber = String(Date.now()).slice(-8);
  }

  // Save to user profile
  try {
    await setDoc(doc(db, 'users', uid), { zixoNumber }, { merge: true });
    // Create mapping in zixoNumbers collection
    await setDoc(doc(db, 'zixoNumbers', zixoNumber), { uid });
    // Sync zixoNumber to RTDB
    await syncUserProfileToRTDB(uid, { zixoNumber }, true);
  } catch (err) {
    console.warn('[Zixo Auth] Failed to save zixoNumber to Firestore:', err);
  }

  return zixoNumber;
}

/**
 * Search for a user by their Zixo Number
 * Looks up the zixoNumbers collection, then fetches the user profile
 */
export async function searchUserByZixoNumber(zixoNumber: string): Promise<ZixoUserProfile | null> {
  if (!zixoNumber || zixoNumber.trim().length === 0) return null;

  // Look up the mapping in zixoNumbers collection (document ID is the number)
  const mappingRef = doc(db, 'zixoNumbers', zixoNumber.trim());
  const mappingSnap = await getDoc(mappingRef);

  if (!mappingSnap.exists()) {
    return null;
  }

  const mappingData = mappingSnap.data();
  const uid = mappingData?.uid;
  if (!uid) return null;

  // Fetch the user profile from users collection
  return getUserProfile(uid);
}

/**
 * Format a Zixo Number for display: "1234 5678"
 */
export function formatZixoNumber(zixoNumber?: string): string {
  if (!zixoNumber) return '';
  const clean = zixoNumber.replace(/\s/g, '');
  if (clean.length !== 8) return zixoNumber;
  return `${clean.slice(0, 4)} ${clean.slice(4)}`;
}

/**
 * Sanitize user input by stripping HTML tags and dangerous characters.
 * Prevents XSS attacks from user-supplied data stored in Firestore.
 */
export function sanitizeInput(input: string): string {
  if (!input || typeof input !== 'string') return input;
  return input
    // Strip HTML tags
    .replace(/<[^>]*>/g, '')
    // Strip script event handlers (on* attributes that might survive tag stripping)
    .replace(/\bon\w+\s*=\s*["'][^"']*["']/gi, '')
    // Strip javascript: URLs
    .replace(/javascript\s*:/gi, '')
    // Strip data: URLs that could contain XSS
    .replace(/data\s*:\s*text\/html/gi, '')
    // Encode remaining angle brackets as a safety net
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // Trim whitespace
    .trim();
}

/**
 * Register a new user with email/password
 */
export async function registerWithEmail(
  email: string,
  password: string,
  displayName: string
): Promise<{ user: User; profile: ZixoUserProfile }> {
  // Sanitize user inputs
  const safeDisplayName = sanitizeInput(displayName);
  const safeEmail = sanitizeInput(email);

  const credential = await createUserWithEmailAndPassword(auth, safeEmail, password);
  const user = credential.user;

  // Update Firebase Auth display name
  await updateProfile(user, { displayName: safeDisplayName });

  // Send email verification
  await sendEmailVerification(user).catch(console.error);

  // Create Firestore user profile
  const username = `@${safeDisplayName.toLowerCase().replace(/\s+/g, '')}${Math.floor(Math.random() * 1000)}`;
  const zixoNumber = await generateUniqueZixoNumber();
  const profile: ZixoUserProfile = {
    uid: user.uid,
    displayName: safeDisplayName,
    email,
    username,
    bio: 'Living free, connecting freely 🌍',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
    createdAt: Date.now(),
    zixoNumber,
  };

  await setDoc(doc(db, 'users', user.uid), {
    ...profile,
    createdAt: serverTimestamp(),
    lastSeen: serverTimestamp(),
  });

  // Also create username mapping for search
  await setDoc(doc(db, 'usernames', username), { uid: user.uid });

  // Create zixoNumber mapping for fast lookup
  await setDoc(doc(db, 'zixoNumbers', zixoNumber), { uid: user.uid });

  // Sync profile to RTDB for fallback access
  await syncUserProfileToRTDB(user.uid, profile);

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
  let profile = await getUserProfile(credential.user.uid);

  if (!profile) {
    throw new Error('User profile not found. Please contact support.');
  }

  // Update online status
  await updateOnlineStatus(credential.user.uid, true);

  // Lazily assign zixoNumber if missing (for existing users)
  if (!profile.zixoNumber) {
    const newZixoNumber = await ensureZixoNumber(credential.user.uid);
    profile = { ...profile, zixoNumber: newZixoNumber };
  }

  return { user: credential.user, profile };
}

/**
 * Sign in with Google
 */
export async function loginWithGoogle(): Promise<{ user: User; profile: ZixoUserProfile }> {
  let credential;
  try {
    credential = await signInWithPopup(auth, googleProvider);
  } catch (err: any) {
    // Provide a clear error for unauthorized domains (e.g., zixocall.eu.cc not in Firebase authorized domains)
    if (err?.code === 'auth/unauthorized-domain') {
      throw new Error('This domain is not authorized for sign-in. Please add it in Firebase Console > Authentication > Settings > Authorized domains, or try accessing the app from an authorized domain.');
    }
    throw err;
  }
  const user = credential.user;

  // Safely derive display name and username parts with null safety
  const nameStr = user.displayName || (user.email ? user.email.split('@')[0] : null) || 'user';
  const safeUsername = `@${nameStr.toLowerCase().replace(/\s+/g, '')}${Math.floor(Math.random() * 1000)}`;
  const safeDisplayName = user.displayName || (user.email ? user.email.split('@')[0] : null) || 'User';

  // Check if user profile exists
  let profile = await getUserProfile(user.uid);

  if (!profile) {
    // First-time Google sign-in: create profile
    const zixoNumber = await generateUniqueZixoNumber();
    profile = {
      uid: user.uid,
      displayName: safeDisplayName,
      email: user.email || '',
      username: safeUsername,
      bio: 'Living free, connecting freely 🌍',
      avatar: user.photoURL || '',
      online: true,
      lastSeen: Date.now(),
      createdAt: Date.now(),
      zixoNumber,
    };

    await setDoc(doc(db, 'users', user.uid), {
      ...profile,
      createdAt: serverTimestamp(),
      lastSeen: serverTimestamp(),
    });
    await setDoc(doc(db, 'usernames', safeUsername), { uid: user.uid });
    // Create zixoNumber mapping for fast lookup
    await setDoc(doc(db, 'zixoNumbers', zixoNumber), { uid: user.uid });
    // Sync profile to RTDB for fallback access
    await syncUserProfileToRTDB(user.uid, profile);
  } else {
    await updateOnlineStatus(user.uid, true);
    // Lazily assign zixoNumber if missing (for existing users)
    if (!profile.zixoNumber) {
      const newZixoNumber = await ensureZixoNumber(user.uid);
      profile = { ...profile, zixoNumber: newZixoNumber };
    }
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
    const profile: ZixoUserProfile = {
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
      phoneNumber: data.phoneNumber || '',
      zixoNumber: data.zixoNumber || '',
    };

    // Lazily assign zixoNumber if missing (for existing users)
    if (!profile.zixoNumber) {
      const newZixoNumber = await ensureZixoNumber(data.uid, data);
      profile.zixoNumber = newZixoNumber;
    }

    return profile;
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

  // Sync online status to RTDB
  await syncUserProfileToRTDB(uid, { online, lastSeen: Date.now() }, true);
}

/**
 * Update user profile
 */
export async function updateUserProfile(
  uid: string,
  updates: Partial<Pick<ZixoUserProfile, 'displayName' | 'bio' | 'avatar' | 'username'>>
): Promise<void> {
  // Sanitize text inputs to prevent XSS
  const sanitizedUpdates: Record<string, any> = {};
  for (const [key, value] of Object.entries(updates)) {
    if (typeof value === 'string' && key !== 'avatar') {
      sanitizedUpdates[key] = sanitizeInput(value);
    } else {
      sanitizedUpdates[key] = value;
    }
  }

  const docRef = doc(db, 'users', uid);
  await setDoc(docRef, sanitizedUpdates, { merge: true });

  // If username changed, update mapping
  if (updates.username) {
    await setDoc(doc(db, 'usernames', updates.username), { uid });
  }

  // Sync profile updates to RTDB
  await syncUserProfileToRTDB(uid, sanitizedUpdates, true);
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
      zixoNumber: data.zixoNumber || '',
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

  // If the search looks like a Zixo number (8 digits), search by that too
  const isZixoNumber = /^\d{8}$/.test(searchText.replace(/\s/g, ''));
  if (isZixoNumber) {
    try {
      const zixoResult = await searchUserByZixoNumber(searchText.replace(/\s/g, ''));
      if (zixoResult && !results.has(zixoResult.uid)) {
        results.set(zixoResult.uid, zixoResult);
      }
    } catch (err) {
      console.warn('[Zixo] Zixo number search failed:', err);
    }
  }

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
          zixoNumber: data.zixoNumber || '',
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
          zixoNumber: data.zixoNumber || '',
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
            zixoNumber: data.zixoNumber || '',
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
 * Tries Firestore client SDK first, falls back to API endpoint, then RTDB
 */
export async function getAllUsers(currentUid: string, maxResults: number = 50): Promise<ZixoUserProfile[]> {
  const users: ZixoUserProfile[] = [];

  // Method 1: Firestore client SDK
  try {
    const { collection, query, getDocs, limit: firestoreLimit } = await import('firebase/firestore');
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
          zixoNumber: data.zixoNumber || '',
        });
      }
    });
    if (users.length > 0) return users.slice(0, maxResults);
  } catch (err) {
    console.warn('[Zixo] Firestore client: Failed to fetch all users:', err);
  }

  // Method 2: API endpoint (discoverUsers - no admin role required)
  try {
    const res = await fetch('/api/zixo', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'discoverUsers', limit: maxResults }),
    });
    if (res.ok) {
      const data = await res.json();
      if (data.users && data.users.length > 0) {
        return data.users
          .filter((u: any) => u.uid !== currentUid)
          .map((u: any) => ({
            uid: u.uid || u.id,
            displayName: u.displayName || '',
            email: u.email || '',
            username: u.username || '',
            bio: u.bio || '',
            avatar: u.avatar || '',
            online: u.online || false,
            lastSeen: u.lastSeen || Date.now(),
            createdAt: u.createdAt || Date.now(),
            role: u.role || 'user',
            zixoNumber: u.zixoNumber || '',
          }))
          .slice(0, maxResults);
      }
    }
  } catch (err) {
    console.warn('[Zixo] API: Failed to fetch all users:', err);
  }

  return users.slice(0, maxResults);
}

// ==================== PHONE AUTH (OTP) ====================

let recaptchaVerifier: RecaptchaVerifier | null = null;
let confirmationResult: ConfirmationResult | null = null;

/**
 * Initialize the invisible reCAPTCHA verifier for phone auth
 * Uses invisible reCAPTCHA for seamless UX
 * @param containerId - The ID of the container element for the reCAPTCHA
 */
export function initRecaptcha(containerId: string): void {
  // Always clear previous verifier first to avoid stale state
  if (recaptchaVerifier) {
    try {
      recaptchaVerifier.clear();
    } catch (e) {
      // Ignore cleanup errors
    }
    recaptchaVerifier = null;
  }

  // Ensure the container element exists in the DOM
  const container = typeof document !== 'undefined' ? document.getElementById(containerId) : null;
  if (!container) {
    console.warn(`[Zixo Auth] reCAPTCHA container #${containerId} not found in DOM. Creating it.`);
    // Create the container if it doesn't exist
    const div = document.createElement('div');
    div.id = containerId;
    document.body.appendChild(div);
  }

  recaptchaVerifier = new RecaptchaVerifier(auth, containerId, {
    size: 'invisible',
    callback: () => {
      console.log('[Zixo Auth] reCAPTCHA solved');
    },
    'expired-callback': () => {
      console.log('[Zixo Auth] reCAPTCHA expired, resetting...');
      resetPhoneAuth();
    },
  });
}

/**
 * Send OTP to the given phone number
 * @param phoneNumber - Phone number in +XX format (e.g., +8801712345678)
 */
export async function sendOTP(phoneNumber: string): Promise<void> {
  if (!recaptchaVerifier) {
    throw new Error('Recaptcha not initialized. Call initRecaptcha first.');
  }

  // Ensure the reCAPTCHA verifier is ready
  try {
    await recaptchaVerifier.verify();
  } catch (e: any) {
    // If reCAPTCHA verification fails, try resetting and re-initializing
    console.warn('[Zixo Auth] reCAPTCHA verify failed, resetting:', e?.message);
    resetPhoneAuth();
    initRecaptcha('recaptcha-container');
    if (!recaptchaVerifier) {
      throw new Error('Failed to initialize reCAPTCHA. Please refresh the page and try again.');
    }
  }

  try {
    confirmationResult = await signInWithPhoneNumber(auth, phoneNumber, recaptchaVerifier);
  } catch (err: any) {
    // If we get operation-not-allowed, provide clear guidance
    if (err?.code === 'auth/operation-not-allowed') {
      console.error('[Zixo Auth] Phone auth not enabled. Visit: https://console.firebase.google.com/project/zixo-call/authentication/providers');
    }
    // Reset reCAPTCHA on any error so it can be reused
    resetPhoneAuth();
    throw err;
  }
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
    const phoneNumber = user.phoneNumber || '';
    const phoneUsername = phoneNumber ? `@${phoneNumber.replace(/[^0-9]/g, '').slice(-7)}` : `@user${Math.floor(Math.random() * 10000)}`;
    const zixoNumber = await generateUniqueZixoNumber();
    profile = {
      uid: user.uid,
      displayName: user.displayName || phoneNumber || 'User',
      email: user.email || '',
      username: phoneUsername,
      bio: '',
      avatar: user.photoURL || '',
      online: true,
      lastSeen: Date.now(),
      createdAt: Date.now(),
      role: 'user',
      phoneNumber: phoneNumber,
      zixoNumber,
    };
    // Create Firestore profile
    await setDoc(doc(db, 'users', user.uid), {
      ...profile,
      createdAt: serverTimestamp(),
      lastSeen: serverTimestamp(),
    });
    // Create username mapping for search
    await setDoc(doc(db, 'usernames', phoneUsername), { uid: user.uid });
    // Create zixoNumber mapping for fast lookup
    await setDoc(doc(db, 'zixoNumbers', zixoNumber), { uid: user.uid });
    // Sync profile to RTDB for fallback access
    await syncUserProfileToRTDB(user.uid, profile);
  } else {
    // Update online status for existing user
    await updateOnlineStatus(user.uid, true);
    // Lazily assign zixoNumber if missing (for existing users)
    if (!profile.zixoNumber) {
      const newZixoNumber = await ensureZixoNumber(user.uid);
      profile = { ...profile, zixoNumber: newZixoNumber };
    }
  }
  return { user, profile };
}

/**
 * Reset phone auth state
 */
export function resetPhoneAuth(): void {
  if (recaptchaVerifier) {
    try {
      recaptchaVerifier.clear();
    } catch (e) {
      // Ignore cleanup errors
    }
    recaptchaVerifier = null;
  }
  confirmationResult = null;
}

import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  sendPasswordResetEmail,
  updateProfile,
  onAuthStateChanged,
  type User,
} from 'firebase/auth';
import { doc, setDoc, getDoc, serverTimestamp } from 'firebase/firestore';
import { auth, db } from './firebase';

const googleProvider = new GoogleAuthProvider();

// Scopes for Google Sign-In
googleProvider.addScope('profile');
googleProvider.addScope('email');

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
 * Search for a user by username
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
    };
  }
  return null;
}

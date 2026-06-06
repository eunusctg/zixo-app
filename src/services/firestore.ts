import {
  collection,
  doc,
  setDoc,
  getDoc,
  getDocs,
  addDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  query,
  where,
  orderBy,
  limit,
  serverTimestamp,
  Timestamp,
  writeBatch,
  type Unsubscribe,
  type DocumentData,
} from 'firebase/firestore';
import { db } from './firebase';
import type { ZixoUserProfile } from './auth';

// ==================== TYPES ====================

export interface FirestoreMessage {
  id: string;
  chatId: string;
  senderId: string;
  text: string;
  type: 'text' | 'image' | 'voice' | 'file' | 'location' | 'emoji';
  timestamp: Timestamp;
  status: 'sending' | 'sent' | 'delivered' | 'read';
  replyTo?: string;
  starred?: boolean;
  mediaUrl?: string;
  fileName?: string;
  fileSize?: number;
  duration?: number;
}

export interface FirestoreChat {
  id: string;
  participants: string[];
  isGroup: boolean;
  groupName?: string;
  groupAvatar?: string;
  lastMessage?: string;
  lastMessageSender?: string;
  lastMessageTime?: Timestamp;
  unreadCount: Record<string, number>; // { uid: count }
  typing?: string[];
  pinned?: boolean;
  muted?: string[]; // array of uids who muted
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface FirestoreCall {
  id: string;
  callerId: string;
  callerName: string;
  callerAvatar: string;
  receiverId: string;
  receiverName: string;
  receiverAvatar: string;
  type: 'audio' | 'video';
  direction: 'incoming' | 'outgoing' | 'missed';
  duration: number;
  timestamp: Timestamp;
}

// ==================== CHAT OPERATIONS ====================

/**
 * Create or get an existing 1-on-1 chat between two users
 */
export async function createOrGetChat(
  uid1: string,
  uid2: string
): Promise<string> {
  // Check if chat already exists
  const q = query(
    collection(db, 'chats'),
    where('participants', 'array-contains', uid1)
  );
  const snapshot = await getDocs(q);

  for (const docSnap of snapshot.docs) {
    const data = docSnap.data() as FirestoreChat;
    if (data.participants.includes(uid2) && !data.isGroup) {
      return docSnap.id;
    }
  }

  // Create new chat
  const chatRef = await addDoc(collection(db, 'chats'), {
    participants: [uid1, uid2],
    isGroup: false,
    unreadCount: { [uid1]: 0, [uid2]: 0 },
    typing: [],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  return chatRef.id;
}

/**
 * Send a message to a chat
 */
export async function sendMessage(
  chatId: string,
  senderId: string,
  text: string,
  type: FirestoreMessage['type'] = 'text',
  extras?: { mediaUrl?: string; fileName?: string; fileSize?: number; duration?: number; replyTo?: string }
): Promise<string> {
  const messageData: Omit<FirestoreMessage, 'id'> & { timestamp: any } = {
    chatId,
    senderId,
    text,
    type,
    timestamp: serverTimestamp(),
    status: 'sent',
    starred: false,
    ...extras,
  };

  const msgRef = await addDoc(collection(db, 'chats', chatId, 'messages'), messageData);

  // Update chat metadata
  const chatRef = doc(db, 'chats', chatId);
  const chatSnap = await getDoc(chatRef);
  if (chatSnap.exists()) {
    const chatData = chatSnap.data() as FirestoreChat;
    const unreadUpdates: Record<string, number> = {};

    // Increment unread count for all participants except sender
    chatData.participants.forEach((uid) => {
      if (uid !== senderId) {
        unreadUpdates[`unreadCount.${uid}`] = (chatData.unreadCount?.[uid] || 0) + 1;
      }
    });

    await updateDoc(chatRef, {
      lastMessage: text,
      lastMessageSender: senderId,
      lastMessageTime: serverTimestamp(),
      updatedAt: serverTimestamp(),
      ...unreadUpdates,
    });
  }

  return msgRef.id;
}

/**
 * Mark messages as read in a chat
 */
export async function markChatRead(chatId: string, uid: string): Promise<void> {
  const chatRef = doc(db, 'chats', chatId);
  await updateDoc(chatRef, {
    [`unreadCount.${uid}`]: 0,
    updatedAt: serverTimestamp(),
  });
}

/**
 * Update message status to delivered/read
 */
export async function updateMessageStatus(
  chatId: string,
  messageId: string,
  status: FirestoreMessage['status']
): Promise<void> {
  const msgRef = doc(db, 'chats', chatId, 'messages', messageId);
  await updateDoc(msgRef, { status });
}

/**
 * Set typing indicator
 */
export async function setTyping(chatId: string, uid: string, isTyping: boolean): Promise<void> {
  const chatRef = doc(db, 'chats', chatId);
  const chatSnap = await getDoc(chatRef);

  if (chatSnap.exists()) {
    const data = chatSnap.data() as FirestoreChat;
    let typing = data.typing || [];

    if (isTyping && !typing.includes(uid)) {
      typing.push(uid);
    } else if (!isTyping) {
      typing = typing.filter((id) => id !== uid);
    }

    await updateDoc(chatRef, { typing });
  }
}

/**
 * Star/unstar a message
 */
export async function toggleStarMessage(
  chatId: string,
  messageId: string,
  starred: boolean
): Promise<void> {
  const msgRef = doc(db, 'chats', chatId, 'messages', messageId);
  await updateDoc(msgRef, { starred });
}

/**
 * Delete a message
 */
export async function deleteMessage(
  chatId: string,
  messageId: string
): Promise<void> {
  const msgRef = doc(db, 'chats', chatId, 'messages', messageId);
  await deleteDoc(msgRef);
}

// ==================== REAL-TIME LISTENERS ====================

/**
 * Listen to all chats for a user
 */
export function subscribeToUserChats(
  uid: string,
  callback: (chats: FirestoreChat[]) => void
): Unsubscribe {
  const q = query(
    collection(db, 'chats'),
    where('participants', 'array-contains', uid),
    orderBy('updatedAt', 'desc')
  );

  return onSnapshot(q, (snapshot) => {
    const chats: FirestoreChat[] = [];
    snapshot.forEach((docSnap) => {
      chats.push({ id: docSnap.id, ...docSnap.data() } as FirestoreChat);
    });
    callback(chats);
  });
}

/**
 * Listen to messages in a chat
 */
export function subscribeToChatMessages(
  chatId: string,
  messageLimit: number,
  callback: (messages: FirestoreMessage[]) => void
): Unsubscribe {
  const q = query(
    collection(db, 'chats', chatId, 'messages'),
    orderBy('timestamp', 'asc'),
    limit(messageLimit)
  );

  return onSnapshot(q, (snapshot) => {
    const messages: FirestoreMessage[] = [];
    snapshot.forEach((docSnap) => {
      messages.push({ id: docSnap.id, ...docSnap.data() } as FirestoreMessage);
    });
    callback(messages);
  });
}

/**
 * Listen to a single user's profile for online status updates
 */
export function subscribeToUserProfile(
  uid: string,
  callback: (profile: ZixoUserProfile | null) => void
): Unsubscribe {
  const docRef = doc(db, 'users', uid);
  return onSnapshot(docRef, (docSnap) => {
    if (docSnap.exists()) {
      const data = docSnap.data();
      callback({
        uid: data.uid,
        displayName: data.displayName || '',
        email: data.email || '',
        username: data.username || '',
        bio: data.bio || '',
        avatar: data.avatar || '',
        online: data.online || false,
        lastSeen: data.lastSeen?.toMillis?.() || data.lastSeen || Date.now(),
        createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
      });
    } else {
      callback(null);
    }
  });
}

/**
 * Get multiple user profiles at once (for chat list)
 */
export async function getUserProfiles(uids: string[]): Promise<Record<string, ZixoUserProfile>> {
  const profiles: Record<string, ZixoUserProfile> = {};

  // Firestore doesn't support batch get by array of IDs directly,
  // so we fetch them individually (could be optimized with batch reads)
  const promises = uids.map(async (uid) => {
    const docRef = doc(db, 'users', uid);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
      const data = docSnap.data();
      profiles[uid] = {
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
  });

  await Promise.all(promises);
  return profiles;
}

// ==================== CALL LOGS ====================

/**
 * Save a call record
 */
export async function saveCallRecord(call: Omit<FirestoreCall, 'id' | 'timestamp'>): Promise<string> {
  const callRef = await addDoc(collection(db, 'calls'), {
    ...call,
    timestamp: serverTimestamp(),
  });
  return callRef.id;
}

/**
 * Get call history for a user
 */
export async function getCallHistory(uid: string): Promise<FirestoreCall[]> {
  const q1 = query(
    collection(db, 'calls'),
    where('callerId', '==', uid),
    orderBy('timestamp', 'desc'),
    limit(50)
  );
  const q2 = query(
    collection(db, 'calls'),
    where('receiverId', '==', uid),
    orderBy('timestamp', 'desc'),
    limit(50)
  );

  const [snap1, snap2] = await Promise.all([getDocs(q1), getDocs(q2)]);

  const calls: FirestoreCall[] = [];
  snap1.forEach((docSnap) => {
    calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
  });
  snap2.forEach((docSnap) => {
    calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
  });

  // Sort by timestamp descending
  calls.sort((a, b) => {
    const ta = a.timestamp?.toMillis?.() || 0;
    const tb = b.timestamp?.toMillis?.() || 0;
    return tb - ta;
  });

  return calls;
}

/**
 * Delete a call record
 */
export async function deleteCallRecord(callId: string): Promise<void> {
  await deleteDoc(doc(db, 'calls', callId));
}

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
  startAfter,
  endAt,
  serverTimestamp,
  Timestamp,
  writeBatch,
  type Unsubscribe,
  type DocumentData,
  type DocumentSnapshot,
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
  type: 'audio' | 'video' | 'group-audio' | 'group-video';
  direction: 'incoming' | 'outgoing' | 'missed';
  duration: number;
  timestamp: Timestamp;
  participantUids?: string[];
}

// ==================== CHAT OPERATIONS ====================

/**
 * Create or get an existing 1-on-1 chat between two users
 * Handles the case where Firestore indexes may not be deployed yet
 */
export async function createOrGetChat(
  uid1: string,
  uid2: string
): Promise<string> {
  // Strategy 1: Try the indexed query first (participants + updatedAt)
  try {
    const q = query(
      collection(db, 'chats'),
      where('participants', 'array-contains', uid1),
      orderBy('updatedAt', 'desc')
    );
    const snapshot = await getDocs(q);

    for (const docSnap of snapshot.docs) {
      const data = docSnap.data() as FirestoreChat;
      if (data.participants.includes(uid2) && !data.isGroup) {
        return docSnap.id;
      }
    }
  } catch (indexError) {
    // Index may not be deployed yet - fall back to simpler query
    console.warn('[Zixo] Indexed chat query failed, trying fallback query:', indexError);
  }

  // Strategy 2: Fallback query without orderBy (no index needed)
  try {
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
  } catch (fallbackError) {
    console.warn('[Zixo] Fallback chat query also failed:', fallbackError);
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

  // Update chat metadata including last active time
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
      lastActiveAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      ...unreadUpdates,
    });
  }

  // Update sender's last active time in their user profile
  try {
    const userRef = doc(db, 'users', senderId);
    await updateDoc(userRef, { lastActiveAt: serverTimestamp() });
  } catch {
    // Non-critical - don't fail the message send
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

/**
 * Batch delete multiple messages in a chat
 */
export async function batchDeleteMessages(
  chatId: string,
  messageIds: string[]
): Promise<void> {
  if (messageIds.length === 0) return;

  // Firestore batches support up to 500 operations
  const batchSize = 500;
  for (let i = 0; i < messageIds.length; i += batchSize) {
    const batch = writeBatch(db);
    const chunk = messageIds.slice(i, i + batchSize);

    chunk.forEach((msgId) => {
      const msgRef = doc(db, 'chats', chatId, 'messages', msgId);
      batch.delete(msgRef);
    });

    await batch.commit();
  }
}

/**
 * Delete an entire chat and all its messages
 */
export async function deleteChat(chatId: string): Promise<void> {
  // Delete all messages in the chat subcollection first
  const messagesRef = collection(db, 'chats', chatId, 'messages');
  const messagesSnap = await getDocs(messagesRef);

  const batchSize = 500;
  const msgIds = messagesSnap.docs.map((d) => d.id);

  for (let i = 0; i < msgIds.length; i += batchSize) {
    const batch = writeBatch(db);
    const chunk = msgIds.slice(i, i + batchSize);
    chunk.forEach((msgId) => {
      batch.delete(doc(db, 'chats', chatId, 'messages', msgId));
    });
    await batch.commit();
  }

  // Delete the chat document itself
  await deleteDoc(doc(db, 'chats', chatId));
}

// ==================== REAL-TIME LISTENERS ====================

/**
 * Listen to all chats for a user
 * Tries indexed query first, falls back to unindexed query
 */
export function subscribeToUserChats(
  uid: string,
  callback: (chats: FirestoreChat[]) => void
): Unsubscribe {
  // Try the indexed query first (participants + updatedAt)
  const q = query(
    collection(db, 'chats'),
    where('participants', 'array-contains', uid),
    orderBy('updatedAt', 'desc')
  );

  let usingFallback = false;

  const unsub = onSnapshot(q, (snapshot) => {
    const chats: FirestoreChat[] = [];
    snapshot.forEach((docSnap) => {
      chats.push({ id: docSnap.id, ...docSnap.data() } as FirestoreChat);
    });
    // Sort by updatedAt desc in case we're using fallback without orderBy
    chats.sort((a, b) => {
      const ta = a.updatedAt?.toMillis?.() || 0;
      const tb = b.updatedAt?.toMillis?.() || 0;
      return tb - ta;
    });
    callback(chats);
  }, async (error) => {
    // If indexed query fails (missing index), fall back to simple query
    console.warn('[Zixo] Indexed chats query failed, using fallback:', error?.message);
    if (!usingFallback) {
      usingFallback = true;
      try {
        const fallbackQ = query(
          collection(db, 'chats'),
          where('participants', 'array-contains', uid)
        );
        const fallbackUnsub = onSnapshot(fallbackQ, (fallbackSnap) => {
          const chats: FirestoreChat[] = [];
          fallbackSnap.forEach((docSnap) => {
            chats.push({ id: docSnap.id, ...docSnap.data() } as FirestoreChat);
          });
          // Sort client-side since we can't use orderBy without the index
          chats.sort((a, b) => {
            const ta = a.updatedAt?.toMillis?.() || 0;
            const tb = b.updatedAt?.toMillis?.() || 0;
            return tb - ta;
          });
          callback(chats);
        });
        // Return the fallback unsubscribe instead
        // Note: we can't easily replace the outer unsub, but the fallback
        // listener will continue working independently
        return fallbackUnsub;
      } catch (fallbackErr) {
        console.error('[Zixo] Fallback chat query also failed:', fallbackErr);
      }
    }
  });

  return unsub;
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
 * Load older messages for pagination (messages before the given snapshot)
 * Returns the messages and whether there are more to load
 */
export async function loadMoreMessages(
  chatId: string,
  beforeTimestamp: Timestamp,
  pageSize: number = 30
): Promise<{ messages: FirestoreMessage[]; hasMore: boolean }> {
  const q = query(
    collection(db, 'chats', chatId, 'messages'),
    orderBy('timestamp', 'desc'),
    endAt(beforeTimestamp),
    limit(pageSize + 1) // +1 to check if there are more
  );

  const snapshot = await getDocs(q);
  const messages: FirestoreMessage[] = [];

  snapshot.forEach((docSnap) => {
    messages.push({ id: docSnap.id, ...docSnap.data() } as FirestoreMessage);
  });

  const hasMore = messages.length > pageSize;
  if (hasMore) messages.pop(); // Remove the extra message

  // Reverse to get chronological order
  messages.reverse();
  return { messages, hasMore };
}

/**
 * Search messages within a chat by text content
 */
export async function searchMessages(
  chatId: string,
  searchText: string,
  maxResults: number = 50
): Promise<FirestoreMessage[]> {
  // Firestore doesn't support full-text search natively
  // We use a prefix match approach with >= and < operators
  const searchTextLower = searchText.toLowerCase();
  const searchTextUpper = searchTextLower + '\uf8ff'; // Unicode high code point for prefix match

  const q = query(
    collection(db, 'chats', chatId, 'messages'),
    where('text', '>=', searchTextLower),
    where('text', '<', searchTextUpper),
    orderBy('timestamp', 'desc'),
    limit(maxResults)
  );

  try {
    const snapshot = await getDocs(q);
    const messages: FirestoreMessage[] = [];
    snapshot.forEach((docSnap) => {
      messages.push({ id: docSnap.id, ...docSnap.data() } as FirestoreMessage);
    });
    return messages;
  } catch {
    // If the index isn't available, fall back to client-side search
    // by loading recent messages and filtering locally
    const allQ = query(
      collection(db, 'chats', chatId, 'messages'),
      orderBy('timestamp', 'desc'),
      limit(200)
    );
    const snapshot = await getDocs(allQ);
    const messages: FirestoreMessage[] = [];
    snapshot.forEach((docSnap) => {
      const msg = { id: docSnap.id, ...docSnap.data() } as FirestoreMessage;
      if (msg.text && msg.text.toLowerCase().includes(searchTextLower)) {
        messages.push(msg);
      }
    });
    return messages.slice(0, maxResults);
  }
}

/**
 * Search messages across all chats for a user
 */
export async function searchAllMessages(
  uid: string,
  searchText: string,
  maxResults: number = 50
): Promise<Array<{ chatId: string; message: FirestoreMessage }>> {
  // First get all chats for this user
  const chatsQ = query(
    collection(db, 'chats'),
    where('participants', 'array-contains', uid)
  );
  const chatsSnap = await getDocs(chatsQ);

  const results: Array<{ chatId: string; message: FirestoreMessage }> = [];
  const searchLower = searchText.toLowerCase();

  // Search in each chat's messages
  const promises = chatsSnap.docs.map(async (chatDoc) => {
    const chatId = chatDoc.id;
    const msgs = await searchMessages(chatId, searchText, 10);
    msgs.forEach((message) => {
      results.push({ chatId, message });
    });
  });

  await Promise.all(promises);

  // Sort by timestamp descending
  results.sort((a, b) => {
    const ta = a.message.timestamp?.toMillis?.() || 0;
    const tb = b.message.timestamp?.toMillis?.() || 0;
    return tb - ta;
  });

  return results.slice(0, maxResults);
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
        role: data.role || 'user',
        zixoNumber: data.zixoNumber || '',
        phoneNumber: data.phoneNumber || '',
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
        role: data.role || 'user',
        phoneNumber: data.phoneNumber || '',
        zixoNumber: data.zixoNumber || '',
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
 * Uses multiple fallback strategies to handle missing composite indexes
 */
export async function getCallHistory(uid: string): Promise<FirestoreCall[]> {
  const calls: FirestoreCall[] = [];

  // Method 1: Indexed query with orderBy (requires composite index)
  try {
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
    snap1.forEach((docSnap) => {
      calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
    });
    snap2.forEach((docSnap) => {
      if (!calls.find(c => c.id === docSnap.id)) {
        calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
      }
    });

    // If we got results from the indexed query, return them
    if (calls.length > 0) {
      calls.sort((a, b) => {
        const ta = a.timestamp?.toMillis?.() || 0;
        const tb = b.timestamp?.toMillis?.() || 0;
        return tb - ta;
      });
      return calls;
    }
  } catch (err) {
    console.warn('[Zixo] Call history query with orderBy failed, trying fallback:', err);
  }

  // Method 2: Query without orderBy (no composite index needed)
  try {
    const q1 = query(collection(db, 'calls'), where('callerId', '==', uid), limit(50));
    const q2 = query(collection(db, 'calls'), where('receiverId', '==', uid), limit(50));
    const [snap1, snap2] = await Promise.all([getDocs(q1), getDocs(q2)]);
    snap1.forEach((docSnap) => {
      calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
    });
    snap2.forEach((docSnap) => {
      if (!calls.find(c => c.id === docSnap.id)) {
        calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
      }
    });

    if (calls.length > 0) {
      calls.sort((a, b) => {
        const ta = a.timestamp?.toMillis?.() || 0;
        const tb = b.timestamp?.toMillis?.() || 0;
        return tb - ta;
      });
      return calls;
    }
  } catch (fallbackErr) {
    console.warn('[Zixo] Call history fallback query also failed:', fallbackErr);
  }

  // Method 3: Read from RTDB as last resort (calls may have been synced there)
  try {
    const { rtdb } = await import('./firebase');
    const { ref, get } = await import('firebase/database');
    const snapshot = await get(ref(rtdb, `calls/${uid}`));
    if (snapshot.exists()) {
      const data = snapshot.val();
      if (typeof data === 'object' && data !== null) {
        Object.entries(data as Record<string, any>).forEach(([key, val]: [string, any]) => {
          calls.push({
            id: key,
            callerId: val.callerId || '',
            callerName: val.callerName || '',
            callerAvatar: val.callerAvatar || '',
            receiverId: val.receiverId || '',
            receiverName: val.receiverName || '',
            receiverAvatar: val.receiverAvatar || '',
            type: val.type || 'audio',
            direction: val.direction || 'outgoing',
            duration: val.duration || 0,
            timestamp: val.timestamp || { toMillis: () => Date.now() } as any,
          });
        });
      }
    }
  } catch (rtdbErr) {
    console.warn('[Zixo] Call history RTDB fallback also failed:', rtdbErr);
  }

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

// ==================== GROUP CALL LOGS ====================

/**
 * Save a group call record
 */
export async function saveGroupCallRecord(call: {
  callerId: string;
  callerName: string;
  type: 'group-audio' | 'group-video';
  participantUids: string[];
  duration: number;
}): Promise<string> {
  const callRef = await addDoc(collection(db, 'calls'), {
    callerId: call.callerId,
    callerName: call.callerName,
    callerAvatar: '',
    receiverId: 'group',
    receiverName: 'Group Call',
    receiverAvatar: '',
    type: call.type,
    direction: 'outgoing' as const,
    duration: call.duration,
    participantUids: call.participantUids,
    timestamp: serverTimestamp(),
  });
  return callRef.id;
}

/**
 * Get group call history for a user
 */
export async function getGroupCallHistory(uid: string): Promise<FirestoreCall[]> {
  const q1 = query(
    collection(db, 'calls'),
    where('callerId', '==', uid),
    where('type', 'in', ['group-audio', 'group-video']),
    orderBy('timestamp', 'desc'),
    limit(50)
  );
  const q2 = query(
    collection(db, 'calls'),
    where('participantUids', 'array-contains', uid),
    orderBy('timestamp', 'desc'),
    limit(50)
  );

  const [snap1, snap2] = await Promise.allSettled([getDocs(q1), getDocs(q2)]);

  const calls: FirestoreCall[] = [];

  if (snap1.status === 'fulfilled') {
    snap1.value.forEach((docSnap) => {
      calls.push({ id: docSnap.id, ...docSnap.data() } as FirestoreCall);
    });
  }
  if (snap2.status === 'fulfilled') {
    snap2.value.forEach((docSnap) => {
      const data = docSnap.data();
      // Avoid duplicates
      if (!calls.find(c => c.id === docSnap.id)) {
        calls.push({ id: docSnap.id, ...data } as FirestoreCall);
      }
    });
  }

  // Sort by timestamp descending
  calls.sort((a, b) => {
    const ta = a.timestamp?.toMillis?.() || 0;
    const tb = b.timestamp?.toMillis?.() || 0;
    return tb - ta;
  });

  return calls;
}

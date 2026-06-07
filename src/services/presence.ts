/**
 * Presence Service for Zixo
 * Uses Firebase Realtime Database for real-time online status, typing indicators,
 * and call signaling. RTDB is more efficient than Firestore for high-frequency,
 * ephemeral data like presence.
 */

import { ref, set, onDisconnect, onValue, off, update, remove, get, onChildAdded, serverTimestamp as rtdbServerTimestamp } from 'firebase/database';
import { rtdb } from './firebase';

// ==================== PRESENCE (ONLINE STATUS) ====================

/**
 * Set up presence tracking for a user.
 * Automatically sets online/offline state using RTDB onDisconnect.
 */
export function setupPresence(uid: string): () => void {
  const presenceRef = ref(rtdb, `presence/${uid}`);
  const connectedRef = ref(rtdb, '.info/connected');

  const unsubscribe = onValue(connectedRef, (snap) => {
    if (snap.val() === true) {
      // We're connected - set up the onDisconnect hook
      set(presenceRef, {
        online: true,
        lastSeen: rtdbServerTimestamp(),
      });

      // When we disconnect, update the status
      onDisconnect(presenceRef).set({
        online: false,
        lastSeen: rtdbServerTimestamp(),
      });
    }
  });

  return () => {
    off(connectedRef);
    off(presenceRef);
    // Set offline when manually unsubscribing
    set(presenceRef, {
      online: false,
      lastSeen: rtdbServerTimestamp(),
    });
    unsubscribe();
  };
}

/**
 * Listen to a user's online status in real-time
 */
export function subscribeToPresence(
  uid: string,
  callback: (online: boolean, lastSeen: number) => void
): () => void {
  const presenceRef = ref(rtdb, `presence/${uid}`);

  const unsubscribe = onValue(presenceRef, (snap) => {
    if (snap.exists()) {
      const data = snap.val();
      callback(data.online || false, data.lastSeen || 0);
    } else {
      callback(false, 0);
    }
  });

  return () => {
    off(presenceRef);
    unsubscribe();
  };
}

/**
 * Listen to multiple users' presence (for chat list)
 */
export function subscribeToMultiplePresence(
  uids: string[],
  callback: (statuses: Record<string, { online: boolean; lastSeen: number }>) => void
): () => void {
  const unsubs: Array<() => void> = [];
  const statuses: Record<string, { online: boolean; lastSeen: number }> = {};

  uids.forEach((uid) => {
    const unsub = subscribeToPresence(uid, (online, lastSeen) => {
      statuses[uid] = { online, lastSeen };
      callback({ ...statuses });
    });
    unsubs.push(unsub);
  });

  return () => {
    unsubs.forEach((fn) => fn());
  };
}

// ==================== TYPING INDICATORS ====================

/**
 * Set typing indicator for a user in a chat
 */
export function setTypingIndicator(chatId: string, uid: string, isTyping: boolean): void {
  const typingRef = ref(rtdb, `typing/${chatId}/${uid}`);

  if (isTyping) {
    set(typingRef, rtdbServerTimestamp());
    // Auto-clear after 5 seconds (in case user stops without clearing)
    setTimeout(() => {
      remove(typingRef);
    }, 5000);
  } else {
    remove(typingRef);
  }
}

/**
 * Listen to typing indicators in a chat
 */
export function subscribeToTyping(
  chatId: string,
  callback: (typingUids: string[]) => void
): () => void {
  const typingRef = ref(rtdb, `typing/${chatId}`);

  const unsubscribe = onValue(typingRef, (snap) => {
    if (snap.exists()) {
      const data = snap.val();
      const uids = Object.keys(data);
      // Filter out entries older than 5 seconds
      const now = Date.now();
      const activeUids = uids.filter((uid) => {
        const timestamp = data[uid];
        return now - timestamp < 5000;
      });
      callback(activeUids);
    } else {
      callback([]);
    }
  });

  return () => {
    off(typingRef);
    unsubscribe();
  };
}

// ==================== CALL SIGNALING (RTDB) ====================
// Using RTDB for call signaling instead of Firestore for lower latency

export interface RTDBCallSignal {
  callerId: string;
  callerName: string;
  receiverId: string;
  type: 'audio' | 'video';
  status: 'ringing' | 'connected' | 'ended';
  offer?: RTCSessionDescriptionInit;
  answer?: RTCSessionDescriptionInit;
  callerCandidates?: RTCIceCandidateInit[];
  receiverCandidates?: RTCIceCandidateInit[];
  createdAt: number;
}

/**
 * Create a call signal in RTDB
 */
export function createCallSignal(signal: Omit<RTDBCallSignal, 'createdAt'>): string {
  const callId = `call_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const callRef = ref(rtdb, `calls/${callId}`);

  set(callRef, {
    ...signal,
    createdAt: rtdbServerTimestamp(),
    callerCandidates: [],
    receiverCandidates: [],
  });

  return callId;
}

/**
 * Listen for incoming calls for a user
 */
export function subscribeToIncomingCalls(
  uid: string,
  callback: (calls: Array<{ id: string; data: RTDBCallSignal }>) => void
): () => void {
  const callsRef = ref(rtdb, 'calls');

  const unsubscribe = onValue(callsRef, (snap) => {
    if (snap.exists()) {
      const data = snap.val();
      const calls: Array<{ id: string; data: RTDBCallSignal }> = [];

      Object.entries(data).forEach(([id, callData]: [string, any]) => {
        if (callData.receiverId === uid && callData.status === 'ringing') {
          calls.push({ id, data: callData as RTDBCallSignal });
        }
      });

      callback(calls);
    } else {
      callback([]);
    }
  });

  return () => {
    off(callsRef);
    unsubscribe();
  };
}

/**
 * Update a call signal
 */
export function updateCallSignal(callId: string, updates: Partial<RTDBCallSignal>): void {
  const callRef = ref(rtdb, `calls/${callId}`);
  update(callRef, updates);
}

/**
 * End a call (remove signal)
 */
export function endCallSignal(callId: string): void {
  const callRef = ref(rtdb, `calls/${callId}`);
  remove(callRef);
}

/**
 * Add an ICE candidate to the call signal
 */
export function addICECandidate(
  callId: string,
  candidate: RTCIceCandidateInit,
  isCaller: boolean
): void {
  const field = isCaller ? 'callerCandidates' : 'receiverCandidates';
  const callRef = ref(rtdb, `calls/${callId}/${field}`);

  // Use push to add to the array
  const candidateRef = ref(rtdb, `calls/${callId}/${field}/${Date.now()}`);
  set(candidateRef, candidate);
}

// ==================== GROUP CALL SIGNALING (RTDB) ====================

export interface RTDBGroupCallSignal {
  callerId: string;
  callerName: string;
  type: 'group-audio' | 'group-video';
  status: 'ringing' | 'active' | 'ended';
  participants: Record<string, { uid: string; name: string; joinedAt: number }>;
  createdAt: number;
}

/**
 * Create a group call signal in RTDB
 */
export function createGroupCallSignal(signal: Omit<RTDBGroupCallSignal, 'createdAt'>): string {
  const callId = `gcall_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const callRef = ref(rtdb, `groupCalls/${callId}`);

  set(callRef, {
    ...signal,
    createdAt: rtdbServerTimestamp(),
  });

  return callId;
}

/**
 * Subscribe to incoming group calls for a user
 * Listens for group calls where the user's uid is in participants and status is 'ringing'
 */
export function subscribeToGroupCalls(
  uid: string,
  callback: (calls: Array<{ id: string; data: RTDBGroupCallSignal }>) => void
): () => void {
  const callsRef = ref(rtdb, 'groupCalls');

  const unsubscribe = onValue(callsRef, (snap) => {
    if (snap.exists()) {
      const data = snap.val();
      const calls: Array<{ id: string; data: RTDBGroupCallSignal }> = [];

      Object.entries(data).forEach(([id, callData]: [string, any]) => {
        // Check if uid is in participants and call is ringing
        if (
          callData.participants &&
          callData.participants[uid] &&
          callData.status === 'ringing' &&
          callData.callerId !== uid
        ) {
          calls.push({ id, data: callData as RTDBGroupCallSignal });
        }
      });

      callback(calls);
    } else {
      callback([]);
    }
  });

  return () => {
    off(callsRef);
    unsubscribe();
  };
}

/**
 * Join a group call signal (add participant)
 */
export function joinGroupCallSignal(
  callId: string,
  uid: string,
  name: string
): void {
  const participantRef = ref(rtdb, `groupCalls/${callId}/participants/${uid}`);
  set(participantRef, { uid, name, joinedAt: Date.now() });

  // Update status to active
  const statusRef = ref(rtdb, `groupCalls/${callId}/status`);
  set(statusRef, 'active');
}

/**
 * Leave a group call signal (remove participant)
 * If no participants remain, ends the call
 */
export async function leaveGroupCallSignal(
  callId: string,
  uid: string
): Promise<void> {
  const participantRef = ref(rtdb, `groupCalls/${callId}/participants/${uid}`);
  await remove(participantRef);

  // Check remaining participants
  const participantsSnap = await get(ref(rtdb, `groupCalls/${callId}/participants`));
  if (!participantsSnap.exists() || Object.keys(participantsSnap.val()).length === 0) {
    // No one left, end the call
    await remove(ref(rtdb, `groupCalls/${callId}`));
  }
}

/**
 * End a group call signal
 */
export function endGroupCallSignal(callId: string): void {
  const callRef = ref(rtdb, `groupCalls/${callId}`);
  update(callRef, { status: 'ended' });
  // Remove after a short delay to let participants see the 'ended' status
  setTimeout(() => {
    remove(callRef);
  }, 3000);
}

/**
 * Subscribe to group call signaling (offers, answers, candidates)
 * Used internally by ZixoGroupWebRTC
 */
export function subscribeToGroupCallSignaling(
  callId: string,
  callback: (event: { type: string; fromUid: string; data: any }) => void
): () => void {
  const unsubs: Array<() => void> = [];

  // Listen for offers
  const offersRef = ref(rtdb, `groupCalls/${callId}/offers`);
  const unsubOffers = onChildAdded(offersRef, (snap) => {
    if (snap.exists()) {
      const targetUid = snap.key;
      const offers = snap.val();
      Object.entries(offers).forEach(([fromUid, offerData]: [string, any]) => {
        callback({ type: 'offer', fromUid, data: offerData });
      });
    }
  });
  unsubs.push(unsubOffers);

  // Listen for answers
  const answersRef = ref(rtdb, `groupCalls/${callId}/answers`);
  const unsubAnswers = onChildAdded(answersRef, (snap) => {
    if (snap.exists()) {
      const targetUid = snap.key;
      const answers = snap.val();
      Object.entries(answers).forEach(([fromUid, answerData]: [string, any]) => {
        callback({ type: 'answer', fromUid, data: answerData });
      });
    }
  });
  unsubs.push(unsubAnswers);

  return () => {
    unsubs.forEach((fn) => fn());
  };
}

/**
 * Get group call data
 */
export async function getGroupCallData(callId: string): Promise<RTDBGroupCallSignal | null> {
  const callRef = ref(rtdb, `groupCalls/${callId}`);
  const snap = await get(callRef);
  if (snap.exists()) {
    return snap.val() as RTDBGroupCallSignal;
  }
  return null;
}

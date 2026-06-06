'use client';

import { useEffect, useRef, useCallback } from 'react';
import { useZixoStore } from '@/stores/useZixoStore';
import { onAuthChange, getUserProfile, updateOnlineStatus, type ZixoUserProfile } from '@/services/auth';
import { subscribeToUserChats, subscribeToChatMessages, getUserProfiles, getCallHistory } from '@/services/firestore';
import { initFCM } from '@/services/messaging';
import { setupPresence, subscribeToTyping, subscribeToIncomingCalls, subscribeToMultiplePresence, type RTDBCallSignal } from '@/services/presence';

// ==================== RETRY LOGIC ====================

/**
 * Retry a Firestore subscription with exponential backoff
 */
function retrySubscription<T>(
  subscribeFn: () => (() => void) | null,
  maxRetries: number = 3,
  baseDelay: number = 1000,
  onRetry?: (attempt: number) => void
): { unsubscribe: () => void; retryNow: () => void } {
  let attempt = 0;
  let currentUnsub: (() => void) | null = null;
  let retryTimeout: ReturnType<typeof setTimeout> | null = null;
  let disposed = false;

  const trySubscribe = () => {
    if (disposed) return;
    try {
      currentUnsub = subscribeFn();
      if (currentUnsub) {
        attempt = 0; // Reset on success
      } else {
        scheduleRetry();
      }
    } catch (error) {
      console.warn(`[Zixo] Subscription attempt ${attempt + 1} failed:`, error);
      scheduleRetry();
    }
  };

  const scheduleRetry = () => {
    if (disposed || attempt >= maxRetries) return;
    const delay = Math.pow(2, attempt) * baseDelay;
    attempt++;
    onRetry?.(attempt);
    console.log(`[Zixo] Retrying subscription in ${delay}ms (attempt ${attempt}/${maxRetries})`);
    retryTimeout = setTimeout(trySubscribe, delay);
  };

  trySubscribe();

  return {
    unsubscribe: () => {
      disposed = true;
      if (retryTimeout) clearTimeout(retryTimeout);
      if (currentUnsub) currentUnsub();
    },
    retryNow: () => {
      if (retryTimeout) clearTimeout(retryTimeout);
      if (currentUnsub) currentUnsub();
      attempt = 0;
      trySubscribe();
    },
  };
}

/**
 * Hook that manages the Firebase <-> Zustand bridge
 * - Listens to auth state changes
 * - Sets up RTDB presence (online/offline)
 * - Subscribes to real-time chat updates (Firestore)
 * - Subscribes to presence of chat participants
 * - Subscribes to typing indicators (RTDB)
 * - Listens for incoming calls (RTDB) and shows incoming call screen
 * - Loads call history from Firestore
 * - Initializes FCM push notifications
 */
export function useFirebaseBridge() {
  const {
    isAuthenticated,
    currentUser,
    isFirebaseReady,
    activeChatId,
    setFirebaseReady,
    login,
    setChats,
    setMessages,
    setUserProfiles,
    setCallHistory,
    setIncomingCall,
    addUnsub,
  } = useZixoStore();

  const initialized = useRef(false);
  const chatUnsubs = useRef<Record<string, () => void>>({});
  const presenceUnsubs = useRef<Record<string, () => void>>({});
  const callHistoryLoaded = useRef(false);

  // 1. Listen to Firebase auth state changes
  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    const unsub = onAuthChange(async (firebaseUser) => {
      setFirebaseReady();
      console.log('[Zixo Bridge] Auth state changed:', firebaseUser ? `uid=${firebaseUser.uid}` : 'null');

      if (firebaseUser) {
        try {
          const profile = await getUserProfile(firebaseUser.uid);
          if (profile) {
            console.log('[Zixo Bridge] User profile loaded, logging in');
            login(profile);
            initFCM(firebaseUser.uid).catch(console.error);
          } else {
            console.log('[Zixo Bridge] No Firestore profile, creating temp profile');
            // User exists in Auth but not in Firestore yet (e.g. first sign-in)
            const tempProfile: ZixoUserProfile = {
              uid: firebaseUser.uid,
              displayName: firebaseUser.displayName || firebaseUser.email?.split('@')[0] || 'User',
              email: firebaseUser.email || '',
              username: `@${(firebaseUser.displayName || 'user').toLowerCase().replace(/\s+/g, '')}`,
              bio: 'Living free, connecting freely',
              avatar: firebaseUser.photoURL || '',
              online: true,
              lastSeen: Date.now(),
              createdAt: Date.now(),
              role: 'user',
            };
            login(tempProfile);
          }
        } catch (error) {
          console.error('[Zixo Bridge] Error loading user profile:', error);
          // Still log in with a temp profile so user isn't stuck
          const tempProfile: ZixoUserProfile = {
            uid: firebaseUser.uid,
            displayName: firebaseUser.displayName || firebaseUser.email?.split('@')[0] || 'User',
            email: firebaseUser.email || '',
            username: `@${(firebaseUser.displayName || 'user').toLowerCase().replace(/\s+/g, '')}`,
            bio: 'Living free, connecting freely',
            avatar: firebaseUser.photoURL || '',
            online: true,
            lastSeen: Date.now(),
            createdAt: Date.now(),
            role: 'user',
          };
          console.log('[Zixo Bridge] Using temp profile due to error');
          login(tempProfile);

          // Error recovery: retry once after a delay to get real profile
          setTimeout(async () => {
            try {
              const profile = await getUserProfile(firebaseUser.uid);
              if (profile) {
                console.log('[Zixo Bridge] Profile loaded on retry');
                login(profile);
              }
            } catch (retryError) {
              console.error('[Zixo Bridge] Profile load retry also failed:', retryError);
            }
          }, 2000);
        }
      }
    });

    return () => unsub();
  }, [setFirebaseReady, login]);

  // 2. Set up RTDB presence when authenticated
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let presenceUnsub: (() => void) | null = null;

    try {
      presenceUnsub = setupPresence(currentUser.uid);
    } catch (error) {
      console.warn('[Zixo] RTDB presence setup failed, will retry:', error);
      // Retry presence setup after delay
      const retryTimeout = setTimeout(() => {
        try {
          presenceUnsub = setupPresence(currentUser.uid);
        } catch (retryError) {
          console.warn('[Zixo] RTDB presence retry also failed:', retryError);
        }
      }, 3000);

      return () => {
        clearTimeout(retryTimeout);
        if (presenceUnsub) presenceUnsub();
      };
    }

    return () => {
      if (presenceUnsub) presenceUnsub();
    };
  }, [isAuthenticated, currentUser]);

  // 3. Subscribe to real-time chats when authenticated (Firestore)
  //    With retry logic (exponential backoff)
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    const { unsubscribe } = retrySubscription(
      () => {
        const unsub = subscribeToUserChats(currentUser.uid, async (firestoreChats) => {
          if (firestoreChats.length > 0) {
            const allUids = new Set<string>();
            firestoreChats.forEach((c) => c.participants.forEach((uid) => allUids.add(uid)));
            const profiles = await getUserProfiles(Array.from(allUids));
            setUserProfiles(profiles);

            const uiChats = firestoreChats.map((fc) => {
              const myUnread = fc.unreadCount?.[currentUser.uid] || 0;
              return {
                id: fc.id,
                participants: fc.participants,
                participantProfiles: fc.participants.map(
                  (uid) => profiles[uid] || { uid, displayName: 'Unknown', email: '', username: '', bio: '', avatar: '', online: false, lastSeen: Date.now(), createdAt: Date.now(), role: 'user' as const }
                ),
                lastMessage: fc.lastMessage
                  ? {
                      id: `${fc.id}-last`,
                      chatId: fc.id,
                      senderId: fc.lastMessageSender || '',
                      text: fc.lastMessage,
                      type: 'text' as const,
                      timestamp: fc.lastMessageTime?.toMillis?.() || Date.now(),
                      status: 'delivered' as const,
                    }
                  : undefined,
                unreadCount: myUnread,
                isGroup: fc.isGroup,
                groupName: fc.groupName,
                groupAvatar: fc.groupAvatar,
                typing: fc.typing || [],
                pinned: fc.pinned,
                muted: fc.muted?.includes(currentUser.uid),
                createdAt: fc.createdAt?.toMillis?.() || Date.now(),
                updatedAt: fc.updatedAt?.toMillis?.() || Date.now(),
              };
            });

            uiChats.sort((a, b) => b.updatedAt - a.updatedAt);
            setChats(uiChats);
          }
          // No demo data fallback - only show real data
        });

        return unsub;
      },
      3, // max retries
      1000, // base delay
      (attempt) => console.warn(`[Zixo] Chat subscription retry attempt ${attempt}`)
    );

    addUnsub(unsubscribe);
    return () => unsubscribe();
  }, [isAuthenticated, currentUser, setChats, setMessages, setUserProfiles, setCallHistory, addUnsub]);

  // 4. Subscribe to presence of chat participants (RTDB)
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    // Clean up previous presence subscriptions
    Object.values(presenceUnsubs.current).forEach((unsub) => unsub());
    presenceUnsubs.current = {};

    const store = useZixoStore.getState();
    const otherUids = store.chats
      .flatMap((c) => c.participants)
      .filter((uid) => uid !== currentUser.uid);

    if (otherUids.length === 0) return;

    // Subscribe to presence for all chat participants
    const unsub = subscribeToMultiplePresence(otherUids, (statuses) => {
      const currentStore = useZixoStore.getState();
      const updatedProfiles = { ...currentStore.userProfiles };

      Object.entries(statuses).forEach(([uid, status]) => {
        if (updatedProfiles[uid]) {
          updatedProfiles[uid] = {
            ...updatedProfiles[uid],
            online: status.online,
            lastSeen: status.lastSeen,
          };
        }
      });

      currentStore.setUserProfiles(updatedProfiles);

      // Also update chats with the new presence info
      const updatedChats = currentStore.chats.map((chat) => ({
        ...chat,
        participantProfiles: chat.participantProfiles.map((p) =>
          updatedProfiles[p.uid] ? { ...p, ...updatedProfiles[p.uid] } : p
        ),
      }));
      currentStore.setChats(updatedChats);
    });

    presenceUnsubs.current['chat-list'] = unsub;

    return () => {
      Object.values(presenceUnsubs.current).forEach((unsub) => unsub());
      presenceUnsubs.current = {};
    };
  }, [isAuthenticated, currentUser, useZixoStore.getState().chats.length]);

  // 5. Subscribe to typing indicators when active chat changes (RTDB)
  useEffect(() => {
    if (!activeChatId || !isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToTyping(activeChatId, (typingUids) => {
        const store = useZixoStore.getState();
        const updatedChats = store.chats.map((chat) =>
          chat.id === activeChatId ? { ...chat, typing: typingUids } : chat
        );
        store.setChats(updatedChats);
      });
    } catch (error) {
      console.warn('[Zixo] RTDB typing subscription failed:', error);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [activeChatId, isAuthenticated, currentUser]);

  // 6. Subscribe to messages when active chat changes (Firestore)
  //    With retry logic
  useEffect(() => {
    if (!activeChatId || !isAuthenticated) return;

    if (chatUnsubs.current[activeChatId]) {
      chatUnsubs.current[activeChatId]();
    }

    const { unsubscribe } = retrySubscription(
      () => {
        const unsub = subscribeToChatMessages(activeChatId, 100, (firestoreMessages) => {
          if (firestoreMessages.length > 0) {
            const uiMessages = firestoreMessages.map((fm) => ({
              id: fm.id,
              chatId: fm.chatId,
              senderId: fm.senderId,
              text: fm.text,
              type: fm.type,
              timestamp: fm.timestamp?.toMillis?.() || Date.now(),
              status: fm.status,
              replyTo: fm.replyTo,
              starred: fm.starred,
              mediaUrl: fm.mediaUrl,
              fileName: fm.fileName,
              fileSize: fm.fileSize,
              duration: fm.duration,
            }));
            setMessages(activeChatId, uiMessages);
          }
        });

        return unsub;
      },
      3,
      1000,
      (attempt) => console.warn(`[Zixo] Message subscription retry attempt ${attempt}`)
    );

    chatUnsubs.current[activeChatId] = unsubscribe;

    return () => {
      unsubscribe();
    };
  }, [activeChatId, isAuthenticated, setMessages]);

  // 7. Load call history from Firestore on auth
  useEffect(() => {
    if (!isAuthenticated || !currentUser || callHistoryLoaded.current) return;
    callHistoryLoaded.current = true;

    getCallHistory(currentUser.uid)
      .then((calls) => {
        if (calls.length > 0) {
          const uiCalls = calls.map((c) => ({
            id: c.id,
            callerId: c.callerId,
            callerName: c.callerName,
            callerAvatar: c.callerAvatar,
            receiverId: c.receiverId,
            receiverName: c.receiverName,
            receiverAvatar: c.receiverAvatar,
            type: c.type,
            direction: c.direction,
            duration: c.duration,
            timestamp: c.timestamp?.toMillis?.() || Date.now(),
          }));
          setCallHistory(uiCalls);
        }
      })
      .catch((error) => {
        console.warn('[Zixo] Failed to load call history:', error);
      });
  }, [isAuthenticated, currentUser, setCallHistory]);

  // 8. Listen for incoming calls (RTDB) - show incoming call screen
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToIncomingCalls(currentUser.uid, async (calls) => {
        if (calls.length > 0) {
          const call = calls[0];
          console.log('[Zixo] Incoming call:', call);

          // Look up the caller's profile from cache or Firestore
          const store = useZixoStore.getState();
          let callerProfile = store.userProfiles[call.data.callerId];

          if (!callerProfile) {
            try {
              callerProfile = await getUserProfile(call.data.callerId);
            } catch (err) {
              console.warn('[Zixo] Failed to fetch caller profile:', err);
              callerProfile = {
                uid: call.data.callerId,
                displayName: call.data.callerName || 'Unknown',
                email: '',
                username: '',
                bio: '',
                avatar: '',
                online: false,
                lastSeen: Date.now(),
                createdAt: Date.now(),
                role: 'user',
              };
            }
          }

          // Set incoming call state and navigate to incoming-call screen
          if (callerProfile) {
            setIncomingCall({
              callId: call.id,
              callerProfile,
              callType: call.data.type,
              callData: call.data,
            });
            useZixoStore.getState().setScreen('incoming-call');
          }
        }
      });
    } catch (error) {
      console.warn('[Zixo] RTDB incoming call subscription failed:', error);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [isAuthenticated, currentUser, setIncomingCall]);

  // 9. Update online status on visibility change (Firestore)
  useEffect(() => {
    if (!currentUser) return;

    const handleVisibility = () => {
      updateOnlineStatus(
        currentUser.uid,
        document.visibilityState === 'visible'
      ).catch((error) => {
        console.warn('[Zixo] Failed to update online status:', error);
      });
    };

    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [currentUser]);
}

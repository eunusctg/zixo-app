'use client';

import { useEffect, useRef, useCallback, useState } from 'react';
import { useZixoStore } from '@/stores/useZixoStore';
import { onAuthChange, getUserProfile, updateOnlineStatus, type ZixoUserProfile } from '@/services/auth';
import { subscribeToUserChats, subscribeToChatMessages, getUserProfiles, getCallHistory, subscribeToUserProfile } from '@/services/firestore';
import { initFCM, sendPushNotification, onNotificationBanner, updateMessageBadge, playNotificationSound, playRingingSound, stopOutgoingRingSound, showBrowserNotification, showBannerNotification } from '@/services/messaging';
import { setupPresence, subscribeToTyping, subscribeToIncomingCalls, subscribeToCallStatus, subscribeToMultiplePresence, subscribeToGroupCalls, type RTDBCallSignal } from '@/services/presence';
import { getWebRTC } from '@/services/webrtc';
import type { BannerNotification } from '@/components/zixo/NotificationBanner';

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
 * Enhanced with notification banners, sounds, and badge updates
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
    setIncomingGroupCall,
    addUnsub,
  } = useZixoStore();

  // Reactive selector for activeCall.callId
  const activeCallId = useZixoStore((s) => s.activeCall?.callId);
  // Reactive selector for incomingCall.callId — used to detect caller hang-up
  const incomingCallId = useZixoStore((s) => s.incomingCall?.callId);

  const initialized = useRef(false);
  const chatUnsubs = useRef<Record<string, () => void>>({});
  const presenceUnsubs = useRef<Record<string, () => void>>({});
  const callHistoryLoaded = useRef(false);

  // Notification banner state
  const [bannerNotifications, setBannerNotifications] = useState<BannerNotification[]>([]);

  const handleDismissBanner = useCallback((id: string) => {
    setBannerNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  const handleTapBanner = useCallback((notification: BannerNotification) => {
    // Dismiss the banner
    setBannerNotifications(prev => prev.filter(n => n.id !== notification.id));

    // Navigate to the chat if chatId is provided
    if (notification.chatId) {
      const store = useZixoStore.getState();
      store.setActiveChat(notification.chatId);
    }
  }, []);

  // Listen for notification banner events from messaging.ts
  useEffect(() => {
    const unsub = onNotificationBanner((notif) => {
      setBannerNotifications(prev => [notif as BannerNotification, ...prev].slice(0, 5));
    });
    return unsub;
  }, []);

  // 1. Listen to Firebase auth state changes
  // Use useRef for the callback to avoid stale closures and dependency issues
  const authCallbackRef = useRef<(firebaseUser: any) => void>(undefined as any);

  // Debounce timer for null auth state — prevents false logouts during token refresh
  const nullAuthTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  authCallbackRef.current = async (firebaseUser: any) => {
    setFirebaseReady();
    console.log('[Zixo Bridge] Auth state changed:', firebaseUser ? `uid=${firebaseUser.uid}` : 'null');

    // When Firebase reports null (which can happen briefly during token refresh or
    // network interruption), debounce before logging out. This prevents false logouts.
    if (!firebaseUser) {
      const store = useZixoStore.getState();
      // Only trigger logout if we were previously authenticated
      if (store.isAuthenticated && store.currentUser) {
        console.log('[Zixo Bridge] Auth state is null, debouncing before potential logout...');
        // Clear any existing timer
        if (nullAuthTimerRef.current) clearTimeout(nullAuthTimerRef.current);
        // Wait 30 seconds — if Firebase restores auth within this window, no logout
        // We use a generous timeout because token refresh can be slow on mobile
        nullAuthTimerRef.current = setTimeout(async () => {
          const currentStore = useZixoStore.getState();
          // Don't logout if we're no longer in a state that requires it
          if (!currentStore.isAuthenticated || !currentStore.currentUser) return;

          // Check if Firebase still reports null by trying to get the current user
          try {
            const { auth } = await import('@/services/firebase');
            if (!auth.currentUser) {
              // Double-check: try to reload the user (forces a token refresh)
              try {
                console.log('[Zixo Bridge] Attempting user reload before logout...');
                // We can't call reload on null user, so just verify again
                await new Promise(resolve => setTimeout(resolve, 2000));
                if (auth.currentUser) {
                  console.log('[Zixo Bridge] User recovered after additional wait, staying logged in');
                  return;
                }
              } catch {}
              console.log('[Zixo Bridge] Firebase still null after 30s debounce, logging out');
              currentStore.logout();
            } else {
              console.log('[Zixo Bridge] Firebase auth restored during debounce, staying logged in');
            }
          } catch {
            // If we can't check, don't logout — prefer staying in over false logout
            console.log('[Zixo Bridge] Cannot verify auth state, staying logged in (safe default)');
          }
        }, 30000);
      }
      return;
    }

    // Firebase user is valid — clear any pending logout timer
    if (nullAuthTimerRef.current) {
      clearTimeout(nullAuthTimerRef.current);
      nullAuthTimerRef.current = null;
    }

    if (firebaseUser) {
      try {
        const profile = await getUserProfile(firebaseUser.uid);
        if (profile) {
          // Check if user is banned
          if ((profile as any).banned === true) {
            console.log('[Zixo Bridge] User is banned, signing out');
            try {
              const { signOut } = await import('firebase/auth');
              const { auth } = await import('@/services/firebase');
              await signOut(auth);
            } catch (signOutErr) {
              console.error('[Zixo Bridge] Failed to sign out banned user:', signOutErr);
            }
            if (typeof window !== 'undefined') {
              alert('Your account has been suspended. Please contact support if you believe this is an error.');
            }
            return;
          }
          console.log('[Zixo Bridge] User profile loaded, logging in');
          // If Firestore profile has no avatar but Firebase Auth has a photoURL, use it
          if (!profile.avatar && firebaseUser.photoURL) {
            profile.avatar = firebaseUser.photoURL;
            // Also save it to Firestore so it persists
            try {
              const { setDoc: setDocFn } = await import('firebase/firestore');
              const { db } = await import('@/services/firebase');
              const { doc: docFn } = await import('firebase/firestore');
              await setDocFn(docFn(db, 'users', firebaseUser.uid), { avatar: firebaseUser.photoURL }, { merge: true });
            } catch (e) {
              console.warn('[Zixo Bridge] Failed to save Google photoURL to Firestore:', e);
            }
          }
          login(profile);
          initFCM(firebaseUser.uid).catch(console.error);

          // Sync profile to RTDB for fallback access (ensures RTDB /users/{uid} exists)
          try {
            const { rtdb } = await import('@/services/firebase');
            const { ref, set } = await import('firebase/database');
            const { publicKey, ...rtdbProfile } = profile;
            await set(ref(rtdb, `users/${firebaseUser.uid}`), rtdbProfile);
          } catch (rtdbErr) {
            console.warn('[Zixo Bridge] Failed to sync profile to RTDB:', rtdbErr);
          }

          // Ensure admin role for specific accounts
          if (firebaseUser.email === 'eunus527@gmail.com' && profile.role !== 'admin') {
            try {
              const { setDoc: setDocFn } = await import('firebase/firestore');
              const { db } = await import('@/services/firebase');
              const { doc: docFn } = await import('firebase/firestore');
              await setDocFn(docFn(db, 'users', firebaseUser.uid), { role: 'admin' }, { merge: true });
              profile.role = 'admin';
              login({ ...profile, role: 'admin' });
              // Also sync admin role to RTDB
              try {
                const { rtdb } = await import('@/services/firebase');
                const { ref, update } = await import('firebase/database');
                await update(ref(rtdb, `users/${firebaseUser.uid}`), { role: 'admin' });
              } catch {}
              console.log('[Zixo Bridge] Admin role set for', firebaseUser.email);
            } catch (adminErr) {
              console.warn('[Zixo Bridge] Failed to set admin role:', adminErr);
            }
          }

          // Ensure zixoNumber is assigned
          if (!profile.zixoNumber) {
            try {
              const { ensureZixoNumber } = await import('@/services/auth');
              const newZixoNumber = await ensureZixoNumber(firebaseUser.uid);
              profile.zixoNumber = newZixoNumber;
              login({ ...profile, zixoNumber: newZixoNumber });
              console.log('[Zixo Bridge] Zixo number assigned:', newZixoNumber);
            } catch (zixoErr) {
              console.warn('[Zixo Bridge] Failed to assign Zixo number:', zixoErr);
            }
          }
        } else {
          console.log('[Zixo Bridge] No Firestore profile, creating temp profile');
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
            zixoNumber: '',
          };
          login(tempProfile);
        }
      } catch (error) {
        console.error('[Zixo Bridge] Error loading user profile:', error);
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
          zixoNumber: '',
        };
        console.log('[Zixo Bridge] Using temp profile due to error');
        login(tempProfile);

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
  };

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    const unsub = onAuthChange((firebaseUser) => {
      // Use ref to always call the latest callback without stale closures
      authCallbackRef.current?.(firebaseUser);
    });

    return () => {
      unsub();
      // Clean up any pending logout timer
      if (nullAuthTimerRef.current) {
        clearTimeout(nullAuthTimerRef.current);
        nullAuthTimerRef.current = null;
      }
    };
  }, []);

  // 2. Set up RTDB presence when authenticated
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let presenceUnsub: (() => void) | null = null;

    try {
      presenceUnsub = setupPresence(currentUser.uid);
    } catch (error) {
      console.warn('[Zixo] RTDB presence setup failed, will retry:', error);
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

            const latestCurrentUser = useZixoStore.getState().currentUser;

            const uiChats = firestoreChats.map((fc) => {
              const myUnread = fc.unreadCount?.[currentUser.uid] || 0;

              // Send push notification for new messages in non-active chats from other users
              const currentActiveChatId = useZixoStore.getState().activeChatId;
              if (
                myUnread > 0 &&
                fc.id !== currentActiveChatId &&
                fc.lastMessageSender &&
                fc.lastMessageSender !== currentUser.uid &&
                !fc.muted?.includes(currentUser.uid)
              ) {
                const senderName = profiles[fc.lastMessageSender]?.displayName || 'Someone';
                const senderAvatar = profiles[fc.lastMessageSender]?.avatar;
                const messagePreview = fc.lastMessage || 'Sent a message';

                // Show in-app notification banner
                showBannerNotification({
                  senderName,
                  senderAvatar,
                  messagePreview,
                  chatId: fc.id,
                  type: 'message',
                });

                // Play notification sound
                playNotificationSound();

                // Show browser notification if not focused
                showBrowserNotification(senderName, messagePreview, { type: 'new-message', chatId: fc.id, senderId: fc.lastMessageSender });

                // Also send FCM push (for mobile/background)
                sendPushNotification(
                  currentUser.uid,
                  senderName,
                  messagePreview,
                  { type: 'new-message', chatId: fc.id, senderId: fc.lastMessageSender }
                ).catch(() => {});
              }

              return {
                id: fc.id,
                participants: fc.participants,
                participantProfiles: fc.participants.map(
                  (uid) => {
                    if (latestCurrentUser && uid === latestCurrentUser.uid) {
                      return latestCurrentUser;
                    }
                    return profiles[uid] || { uid, displayName: 'Unknown', email: '', username: '', bio: '', avatar: '', online: false, lastSeen: Date.now(), createdAt: Date.now(), role: 'user' as const, zixoNumber: '' };
                  }
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

            // Update message badge with total unread count
            const totalUnread = uiChats.reduce((sum, c) => sum + c.unreadCount, 0);
            updateMessageBadge(totalUnread);
          }
        });

        return unsub;
      },
      3,
      1000,
      (attempt) => console.warn(`[Zixo] Chat subscription retry attempt ${attempt}`)
    );

    addUnsub(unsubscribe);
    return () => unsubscribe();
  }, [isAuthenticated, currentUser, setChats, setMessages, setUserProfiles, setCallHistory, addUnsub]);

  // 4. Subscribe to presence of chat participants (RTDB)
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    Object.values(presenceUnsubs.current).forEach((unsub) => unsub());
    presenceUnsubs.current = {};

    const store = useZixoStore.getState();
    const otherUids = store.chats
      .flatMap((c) => c.participants)
      .filter((uid) => uid !== currentUser.uid);

    if (otherUids.length === 0) return;

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

    getCallHistory(currentUser.uid)
      .then((calls) => {
        // Only mark as loaded on success so it can retry on failure
        callHistoryLoaded.current = true;
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
        console.warn('[Zixo] Failed to load call history, will retry:', error);
        // Don't set callHistoryLoaded so the effect can retry on next render
      });
  }, [isAuthenticated, currentUser, setCallHistory]);

  // 8. Listen for incoming calls (RTDB) - show incoming call screen with enhanced notifications
  // subscribeToIncomingCalls now uses onChildAdded which only fires for NEW calls,
  // not on every data change (ICE candidates, offer updates, etc.)
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToIncomingCalls(currentUser.uid, async (calls) => {
        if (calls.length > 0) {
          const call = calls[0];

          const store = useZixoStore.getState();

          // If we already have this call as our incomingCall, skip re-processing
          if (store.incomingCall?.callId === call.id) {
            return;
          }

          // If we're in an active call, skip incoming call processing
          if (store.activeCall) {
            return;
          }

          // New incoming call - process it
          console.log('[Zixo] New incoming call:', call.id);

          let callerProfile = store.userProfiles[call.data.callerId];

          if (!callerProfile) {
            try {
              const fetched = await getUserProfile(call.data.callerId);
              if (fetched) callerProfile = fetched;
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
                zixoNumber: '',
              };
            }
          }

          // Re-check state after async profile fetch (user may have answered/declined already)
          const currentState = useZixoStore.getState();
          if (currentState.incomingCall || currentState.activeCall) {
            console.log('[Zixo] Skipping incoming call - already in call or already showing incoming');
            return;
          }

          if (callerProfile) {
            setIncomingCall({
              callId: call.id,
              callerProfile,
              callType: call.data.type,
              callData: call.data,
            });
            useZixoStore.getState().setScreen('incoming-call');

            // Show prominent notification banner for incoming call
            showBannerNotification({
              senderName: callerProfile.displayName,
              senderAvatar: callerProfile.avatar,
              messagePreview: `Incoming ${call.data.type} call`,
              type: 'call',
            });

            // Play ringing sound for incoming call
            playRingingSound();

            // Show browser notification for call
            showBrowserNotification(
              `Incoming ${call.data.type} call`,
              `${callerProfile.displayName} is calling you`,
              { type: 'incoming-call', callId: call.id, callType: call.data.type }
            );

            // Also send FCM push
            sendPushNotification(
              currentUser.uid,
              `Incoming ${call.data.type} call`,
              `${callerProfile.displayName} is calling you`,
              { type: 'incoming-call', callId: call.id, callType: call.data.type }
            ).catch(() => {});
          }
        } else {
          // No ringing calls — if we have an incomingCall in state, the caller
          // must have hung up before we answered. Record as missed call.
          const store = useZixoStore.getState();
          if (store.incomingCall && !store.activeCall) {
            console.log('[Zixo] Incoming call was cancelled by caller — recording as missed call');
            const incoming = store.incomingCall;

            // Record the missed call in call history
            const missedCall: any = {
              id: incoming.callId || `call-${Date.now()}`,
              callerId: incoming.callerProfile.uid,
              callerName: incoming.callerProfile.displayName,
              callerAvatar: incoming.callerProfile.avatar || '',
              receiverId: currentUser.uid,
              receiverName: currentUser.displayName,
              receiverAvatar: currentUser.avatar || '',
              type: incoming.callType,
              direction: 'missed' as const,
              duration: 0,
              timestamp: Date.now(),
            };

            useZixoStore.setState((state: any) => ({
              callHistory: [missedCall, ...state.callHistory],
              incomingCall: null,
              currentScreen: state.currentScreen === 'incoming-call' ? 'home' : state.currentScreen,
            }));

            // Save missed call to Firestore
            import('@/services/firestore').then(({ saveCallRecord }) => {
              saveCallRecord({
                callerId: missedCall.callerId,
                callerName: missedCall.callerName,
                callerAvatar: missedCall.callerAvatar,
                receiverId: missedCall.receiverId,
                receiverName: missedCall.receiverName,
                receiverAvatar: missedCall.receiverAvatar,
                type: missedCall.type,
                direction: 'missed',
                duration: 0,
              }).catch(console.error);
            }).catch(console.error);
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

  // 8b. Watch incoming call status — detect when caller hangs up before we answer
  // Records the call as "missed" in call history
  useEffect(() => {
    if (!incomingCallId || !isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToCallStatus(incomingCallId, (callData) => {
        if (!callData) {
          // Caller ended the call before we answered — this is a missed call
          console.log('[Zixo] Incoming call ended by caller (status subscription) — missed call');
          const store = useZixoStore.getState();
          if (store.incomingCall && !store.activeCall) {
            const incoming = store.incomingCall;

            // Record the missed call
            const missedCall: any = {
              id: incoming.callId || `call-${Date.now()}`,
              callerId: incoming.callerProfile.uid,
              callerName: incoming.callerProfile.displayName,
              callerAvatar: incoming.callerProfile.avatar || '',
              receiverId: currentUser.uid,
              receiverName: currentUser.displayName,
              receiverAvatar: currentUser.avatar || '',
              type: incoming.callType,
              direction: 'missed' as const,
              duration: 0,
              timestamp: Date.now(),
            };

            useZixoStore.setState((state: any) => ({
              callHistory: [missedCall, ...state.callHistory],
              incomingCall: null,
              currentScreen: state.currentScreen === 'incoming-call' ? 'home' : state.currentScreen,
            }));

            // Save missed call to Firestore
            import('@/services/firestore').then(({ saveCallRecord }) => {
              saveCallRecord({
                callerId: missedCall.callerId,
                callerName: missedCall.callerName,
                callerAvatar: missedCall.callerAvatar,
                receiverId: missedCall.receiverId,
                receiverName: missedCall.receiverName,
                receiverAvatar: missedCall.receiverAvatar,
                type: missedCall.type,
                direction: 'missed',
                duration: 0,
              }).catch(console.error);
            }).catch(console.error);
          }
        }
      });
    } catch (error) {
      console.warn('[Zixo] RTDB incoming call status subscription failed:', error);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [incomingCallId, isAuthenticated, currentUser, setIncomingCall]);

  // 8c. Watch active call status — detect when the remote party ends the call.
  // Uses a debounce to avoid false triggers from brief RTDB data flickers.
  const activeCallEndDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!activeCallId || !isAuthenticated) return;

    // Clear any pending debounce from a previous call
    if (activeCallEndDebounceRef.current) {
      clearTimeout(activeCallEndDebounceRef.current);
      activeCallEndDebounceRef.current = null;
    }

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToCallStatus(activeCallId, (callData) => {
        if (!callData) {
          // The call data was deleted or status changed to 'ended'.
          // Debounce: wait 1.5 seconds before actually ending the call locally.
          // This prevents false call ends from brief RTDB data flickers
          // (e.g., during ICE restart or signaling updates).
          if (activeCallEndDebounceRef.current) return; // Already debouncing

          activeCallEndDebounceRef.current = setTimeout(() => {
            activeCallEndDebounceRef.current = null;

            // Re-check: if the call is still active, verify RTDB is still null
            const store = useZixoStore.getState();
            if (!store.activeCall || store.activeCall.callId !== activeCallId) {
              return; // Call already ended locally
            }

            // Check if the call status is still indicating ended
            // (the call may have been re-established by ICE restart)
            if (store.activeCall.status === 'connected') {
              // Still connected — don't end. The RTDB data may have been
              // briefly unavailable. Re-subscribe to verify.
              console.log('[Zixo] Active call still connected, ignoring brief RTDB null');
              return;
            }

            console.log('[Zixo] Active call ended by remote party (debounced)');
            if (store.activeCall?.callId === activeCallId) {
              try { getWebRTC().endCall(); } catch {}
              stopOutgoingRingSound();
              const call = store.activeCall;
              if (call?.remoteUser) {
                const currentUser = store.currentUser;
                const actualDuration = call.startedAt
                  ? Math.floor((Date.now() - call.startedAt) / 1000)
                  : call.duration;
                const direction: 'incoming' | 'outgoing' | 'missed' = call.isIncoming
                  ? 'incoming'
                  : 'outgoing';
                const newCall: any = {
                  id: call.callId || `call-${Date.now()}`,
                  callerId: call.isIncoming ? call.remoteUser.uid : (currentUser?.uid || 'user-me'),
                  callerName: call.isIncoming ? call.remoteUser.displayName : (currentUser?.displayName || 'You'),
                  callerAvatar: '',
                  receiverId: call.isIncoming ? (currentUser?.uid || 'user-me') : call.remoteUser.uid,
                  receiverName: call.isIncoming ? (currentUser?.displayName || 'You') : call.remoteUser.displayName,
                  receiverAvatar: '',
                  type: call.type,
                  direction,
                  duration: actualDuration,
                  timestamp: Date.now(),
                };

                // Show 'ended' status briefly so user sees "Call ended" feedback
                useZixoStore.setState({
                  activeCall: store.activeCall ? { ...store.activeCall, status: 'ended' as const } : null,
                  incomingCall: null,
                });

                // Dismiss after a brief delay
                setTimeout(() => {
                  const currentState = useZixoStore.getState();
                  if (currentState.activeCall?.status === 'ended') {
                    useZixoStore.setState((state: any) => ({
                      callHistory: [newCall, ...state.callHistory],
                      activeCall: null,
                      incomingCall: null,
                      currentScreen: 'home',
                    }));
                  }
                }, 1500);

                // Save call record to Firestore for call history persistence
                import('@/services/firestore').then(({ saveCallRecord }) => {
                  saveCallRecord({
                    callerId: newCall.callerId,
                    callerName: newCall.callerName,
                    callerAvatar: newCall.callerAvatar,
                    receiverId: newCall.receiverId,
                    receiverName: newCall.receiverName,
                    receiverAvatar: newCall.receiverAvatar,
                    type: newCall.type,
                    direction: newCall.direction,
                    duration: newCall.duration,
                  }).catch(console.error);
                }).catch(console.error);
              } else {
                useZixoStore.setState({ activeCall: null, incomingCall: null, currentScreen: 'home' });
              }
            }
          }, 1500);
        } else {
          // Call data exists — cancel any pending end-call debounce
          if (activeCallEndDebounceRef.current) {
            clearTimeout(activeCallEndDebounceRef.current);
            activeCallEndDebounceRef.current = null;
          }
        }
      });
    } catch (error) {
      console.warn('[Zixo] RTDB active call status subscription failed:', error);
    }

    return () => {
      if (unsub) unsub();
      if (activeCallEndDebounceRef.current) {
        clearTimeout(activeCallEndDebounceRef.current);
        activeCallEndDebounceRef.current = null;
      }
    };
  }, [activeCallId, isAuthenticated]);

  // 9. Listen for incoming group calls (RTDB)
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToGroupCalls(currentUser.uid, async (calls) => {
        if (calls.length > 0) {
          const call = calls[0];
          console.log('[Zixo] Incoming group call:', call);

          const store = useZixoStore.getState();
          if (store.groupCall || store.incomingGroupCall) return;

          const participantNames = Object.values(call.data.participants || {})
            .filter((p: any) => p.uid !== call.data.callerId && p.uid !== currentUser.uid)
            .map((p: any) => p.name || p.uid)
            .filter((name: string) => name.length > 0);

          setIncomingGroupCall({
            callId: call.id,
            callerName: call.data.callerName || 'Unknown',
            callerId: call.data.callerId,
            callType: call.data.type,
            participantNames,
            callData: call.data,
          });
          useZixoStore.getState().setScreen('incoming-group-call');

          // Show notification banner for group call
          showBannerNotification({
            senderName: call.data.callerName || 'Unknown',
            messagePreview: `Group ${call.data.type === 'group-video' ? 'Video' : 'Audio'} Call`,
            type: 'call',
          });

          // Play ringing sound
          playRingingSound();

          // Show browser notification
          showBrowserNotification(
            `Group ${call.data.type === 'group-video' ? 'Video' : 'Audio'} Call`,
            `${call.data.callerName || 'Unknown'} is calling`,
            { type: 'incoming-group-call', callId: call.id, callType: call.data.type }
          );

          // Also send FCM push
          sendPushNotification(
            currentUser.uid,
            `Group ${call.data.type === 'group-video' ? 'Video' : 'Audio'} Call`,
            `${call.data.callerName || 'Unknown'} is calling`,
            { type: 'incoming-group-call', callId: call.id, callType: call.data.type }
          ).catch(() => {});
        }
      });
    } catch (error) {
      console.warn('[Zixo] RTDB incoming group call subscription failed:', error);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [isAuthenticated, currentUser, setIncomingGroupCall]);

  // 10. Update online status on visibility change (Firestore)
  useEffect(() => {
    if (!currentUser) return;

    const handleVisibility = () => {
      updateOnlineStatus(
        currentUser.uid,
        document.visibilityState === 'visible'
      ).catch((error) => {
        console.warn('[Zixo] Failed to update online status:', error);
      });

      // Clear badge when app comes to foreground
      if (document.visibilityState === 'visible') {
        updateMessageBadge(0);
      }
    };

    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [currentUser]);

  // 11. Listen to current user's Firestore profile for real-time updates
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;
    try {
      unsub = subscribeToUserProfile(currentUser.uid, (profile) => {
        if (profile) {
          const store = useZixoStore.getState();
          const current = store.currentUser;
          if (current && current.uid === profile.uid) {
            // Skip Firestore updates if a local profile edit was made recently
            // (within 5 seconds) to prevent overwriting fresh local changes
            // with stale Firestore data that hasn't propagated yet.
            const timeSinceLocalUpdate = Date.now() - store._profileUpdatedAt;
            if (timeSinceLocalUpdate < 5000) {
              console.log('[Zixo] Skipping Firestore profile update — local update is too recent');
              return;
            }

            // Merge strategy: Firestore profile takes priority for most fields
            // (it's the source of truth for displayName, bio, avatar, online, etc.)
            // but preserve local-only fields that Firestore might not have.
            // Important: if Firestore has a non-empty avatar, use it (it's the latest).
            // If Firestore has empty avatar but local has one, preserve local.
            const merged = {
              ...current,     // Start with all current fields
              ...profile,     // Overlay with Firestore fields (source of truth)
              // Preserve local avatar if Firestore avatar is empty but local has one
              ...(profile.avatar ? {} : current.avatar ? { avatar: current.avatar } : {}),
              // Preserve local zixoNumber if Firestore is missing it
              ...(profile.zixoNumber ? {} : current.zixoNumber ? { zixoNumber: current.zixoNumber } : {}),
            };
            const changed = Object.keys(profile).some(
              (key) => (current as any)[key] !== (merged as any)[key]
            );
            if (changed) {
              useZixoStore.setState({
                currentUser: merged,
                userProfiles: { ...store.userProfiles, [current.uid]: merged },
              });
            }
          }
        }
      });
    } catch (error) {
      console.warn('[Zixo] User profile subscription failed:', error);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [isAuthenticated, currentUser]);

  // Return notification banner state and handlers for the page component
  return {
    bannerNotifications,
    onDismissBanner: handleDismissBanner,
    onTapBanner: handleTapBanner,
  };
}

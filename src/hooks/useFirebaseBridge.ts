'use client';

import { useEffect, useRef } from 'react';
import { useZixoStore, generateDemoChats, generateDemoMessages, generateDemoCalls } from '@/stores/useZixoStore';
import { onAuthChange, getUserProfile, updateOnlineStatus, type ZixoUserProfile } from '@/services/auth';
import { subscribeToUserChats, subscribeToChatMessages, getUserProfiles } from '@/services/firestore';
import { initFCM } from '@/services/messaging';

// Load demo data helper (defined outside hook to avoid hoisting issues)
function loadDemoDataHelper(
  currentUser: ZixoUserProfile,
  setChats: (chats: any[]) => void,
  setMessages: (chatId: string, messages: any[]) => void,
  setUserProfiles: (profiles: Record<string, ZixoUserProfile>) => void,
  setCallHistory: (calls: any[]) => void,
) {
  const demoChats = generateDemoChats(currentUser);
  setChats(demoChats);

  const demoProfiles: Record<string, ZixoUserProfile> = { [currentUser.uid]: currentUser };
  const demoMessagesMap: Record<string, ReturnType<typeof generateDemoMessages>> = {};

  demoChats.forEach((chat) => {
    const otherUser = chat.participantProfiles.find((p) => p.uid !== currentUser.uid);
    if (otherUser) {
      demoProfiles[otherUser.uid] = otherUser;
      demoMessagesMap[chat.id] = generateDemoMessages(chat.id, otherUser.uid);
    }
  });

  setUserProfiles(demoProfiles);
  Object.entries(demoMessagesMap).forEach(([chatId, msgs]) => {
    setMessages(chatId, msgs);
  });

  setCallHistory(generateDemoCalls());
}

/**
 * Hook that manages the Firebase <-> Zustand bridge
 * - Listens to auth state changes
 * - Subscribes to real-time chat updates
 * - Loads demo data as fallback
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
    addUnsub,
  } = useZixoStore();

  const initialized = useRef(false);
  const chatUnsubs = useRef<Record<string, () => void>>({});

  // 1. Listen to Firebase auth state changes
  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    const unsub = onAuthChange(async (firebaseUser) => {
      setFirebaseReady();

      if (firebaseUser) {
        try {
          const profile = await getUserProfile(firebaseUser.uid);
          if (profile) {
            login(profile);
            initFCM(firebaseUser.uid).catch(console.error);
          } else {
            const tempProfile: ZixoUserProfile = {
              uid: firebaseUser.uid,
              displayName: firebaseUser.displayName || firebaseUser.email?.split('@')[0] || 'User',
              email: firebaseUser.email || '',
              username: `@${(firebaseUser.displayName || 'user').toLowerCase().replace(/\s+/g, '')}`,
              bio: 'Living free, connecting freely 🌍',
              avatar: firebaseUser.photoURL || '',
              online: true,
              lastSeen: Date.now(),
              createdAt: Date.now(),
            };
            login(tempProfile);
          }
        } catch (error) {
          console.error('Error loading user profile:', error);
        }
      }
    });

    return () => unsub();
  }, [setFirebaseReady, login]);

  // 2. Subscribe to real-time chats when authenticated
  useEffect(() => {
    if (!isAuthenticated || !currentUser) return;

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToUserChats(currentUser.uid, async (firestoreChats) => {
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
                (uid) => profiles[uid] || { uid, displayName: 'Unknown', email: '', username: '', bio: '', avatar: '', online: false, lastSeen: Date.now(), createdAt: Date.now() }
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
        } else {
          loadDemoDataHelper(currentUser, setChats, setMessages, setUserProfiles, setCallHistory);
        }
      });

      if (unsub) addUnsub(unsub);
    } catch (error) {
      console.warn('Firestore subscription failed, using demo data:', error);
      loadDemoDataHelper(currentUser, setChats, setMessages, setUserProfiles, setCallHistory);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [isAuthenticated, currentUser, setChats, setMessages, setUserProfiles, setCallHistory, addUnsub]);

  // 3. Subscribe to messages when active chat changes
  useEffect(() => {
    if (!activeChatId || !isAuthenticated) return;

    if (chatUnsubs.current[activeChatId]) {
      chatUnsubs.current[activeChatId]();
    }

    let unsub: (() => void) | null = null;

    try {
      unsub = subscribeToChatMessages(activeChatId, 100, (firestoreMessages) => {
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
        } else {
          const demoMsgs = generateDemoMessages(activeChatId, 'demo-1');
          setMessages(activeChatId, demoMsgs);
        }
      });

      if (unsub) {
        chatUnsubs.current[activeChatId] = unsub;
      }
    } catch (error) {
      console.warn('Message subscription failed, using demo data:', error);
      const demoMsgs = generateDemoMessages(activeChatId, 'demo-1');
      setMessages(activeChatId, demoMsgs);
    }

    return () => {
      if (unsub) unsub();
    };
  }, [activeChatId, isAuthenticated, setMessages]);

  // 4. Update online status on visibility change
  useEffect(() => {
    if (!currentUser) return;

    const handleVisibility = () => {
      updateOnlineStatus(
        currentUser.uid,
        document.visibilityState === 'visible'
      ).catch(console.error);
    };

    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [currentUser]);
}

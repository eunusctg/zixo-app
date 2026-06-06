import { create } from 'zustand';
import type { ZixoUserProfile } from '@/services/auth';
import type { FirestoreChat, FirestoreMessage, FirestoreCall } from '@/services/firestore';
import type { UploadProgress } from '@/services/storage';
import { getWebRTC, resetWebRTC } from '@/services/webrtc';
import { endCallSignal, type RTDBCallSignal } from '@/services/presence';

// Types for UI state (compatible with existing components)
export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  text?: string;
  type: 'text' | 'image' | 'voice' | 'file' | 'location' | 'emoji';
  timestamp: number;
  status: 'sending' | 'sent' | 'delivered' | 'read';
  replyTo?: string;
  starred?: boolean;
  mediaUrl?: string;
  fileName?: string;
  fileSize?: number;
  duration?: number;
}

export interface Chat {
  id: string;
  participants: string[];
  participantProfiles: ZixoUserProfile[];
  lastMessage?: Message;
  unreadCount: number;
  isGroup: boolean;
  groupName?: string;
  groupAvatar?: string;
  typing?: string[];
  pinned?: boolean;
  muted?: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface CallRecord {
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
  timestamp: number;
}

export type Screen =
  | 'splash'
  | 'onboarding'
  | 'auth-login'
  | 'auth-signup'
  | 'auth-forgot'
  | 'auth-verify'
  | 'home'
  | 'chat'
  | 'audio-call'
  | 'video-call'
  | 'incoming-call'
  | 'contacts'
  | 'settings'
  | 'profile-edit'
  | 'starred-messages'
  | 'media-gallery'
  | 'new-chat'
  | 'admin-panel';

export type Tab = 'chats' | 'calls' | 'settings';

export type CallStatus = 'idle' | 'ringing' | 'connecting' | 'connected' | 'ended';

export interface SearchMessagesResult {
  chatId: string;
  messageId: string;
  text: string;
  senderId: string;
  timestamp: number;
  highlighted?: boolean;
}

interface ZixoState {
  // Navigation
  currentScreen: Screen;
  previousScreen: Screen | null;
  activeTab: Tab;

  // Auth
  isAuthenticated: boolean;
  isFirebaseReady: boolean;
  currentUser: ZixoUserProfile | null;

  // Chats
  chats: Chat[];
  activeChatId: string | null;
  messages: Record<string, Message[]>;

  // User profiles cache (for chat list display)
  userProfiles: Record<string, ZixoUserProfile>;

  // Calls
  callHistory: CallRecord[];
  activeCall: {
    status: CallStatus;
    type: 'audio' | 'video';
    remoteUser: ZixoUserProfile | null;
    duration: number;
    isMuted: boolean;
    isSpeakerOn: boolean;
    isVideoOn: boolean;
    localStream: MediaStream | null;
    remoteStream: MediaStream | null;
    callId: string | null;
  } | null;
  incomingCall: {
    callId: string;
    callerProfile: ZixoUserProfile;
    callType: 'audio' | 'video';
    callData: RTDBCallSignal;
  } | null;

  // UI
  searchQuery: string;
  isSearching: boolean;
  showFABMenu: boolean;
  authLoading: boolean;
  authError: string | null;

  // Storage uploads
  storageUploads: Record<string, UploadProgress>;

  // Message search
  messageSearchResults: SearchMessagesResult[];
  messageSearchQuery: string;
  isSearchingMessages: boolean;

  // Pagination
  hasMoreMessages: Record<string, boolean>;
  isLoadingMoreMessages: boolean;

  // Firebase unsubscribers
  _unsubs: Array<() => void>;

  // Actions
  setScreen: (screen: Screen) => void;
  setActiveTab: (tab: Tab) => void;
  goBack: () => void;
  login: (user: ZixoUserProfile) => void;
  logout: () => void;
  setFirebaseReady: () => void;
  setActiveChat: (chatId: string) => void;
  setChats: (chats: Chat[]) => void;
  setMessages: (chatId: string, messages: Message[]) => void;
  addMessage: (chatId: string, message: Message) => void;
  updateMessage: (chatId: string, messageId: string, updates: Partial<Message>) => void;
  setUserProfiles: (profiles: Record<string, ZixoUserProfile>) => void;
  setCallHistory: (calls: CallRecord[]) => void;
  setCallStatus: (status: CallStatus) => void;
  startCall: (type: 'audio' | 'video', user: ZixoUserProfile) => void;
  answerCall: (callId: string, callerProfile: ZixoUserProfile, callType: 'audio' | 'video') => void;
  rejectCall: () => void;
  endCall: () => void;
  toggleMute: () => void;
  toggleSpeaker: () => void;
  toggleVideo: () => void;
  setCallRemoteStream: (stream: MediaStream) => void;
  setCallLocalStream: (stream: MediaStream) => void;
  setCallStatus: (status: CallStatus) => void;
  setIncomingCall: (incoming: { callId: string; callerProfile: ZixoUserProfile; callType: 'audio' | 'video'; callData: RTDBCallSignal } | null) => void;
  setSearchQuery: (query: string) => void;
  toggleSearching: () => void;
  toggleFABMenu: () => void;
  markChatRead: (chatId: string) => void;
  setAuthLoading: (loading: boolean) => void;
  setAuthError: (error: string | null) => void;
  addUnsub: (unsub: () => void) => void;
  clearUnsubs: () => void;
  updateStorageUpload: (uploadId: string, progress: UploadProgress) => void;
  removeStorageUpload: (uploadId: string) => void;
  updateUserProfile: (updates: Partial<Pick<ZixoUserProfile, 'displayName' | 'bio' | 'avatar' | 'username'>>) => void;
  deleteChat: (chatId: string) => void;
  searchMessages: (query: string) => void;
  clearMessageSearch: () => void;
  loadMoreMessages: (chatId: string) => void;
  setHasMoreMessages: (chatId: string, hasMore: boolean) => void;
  prependMessages: (chatId: string, messages: Message[]) => void;
}

export const useZixoStore = create<ZixoState>((set, get) => ({
  // Navigation
  currentScreen: 'splash',
  previousScreen: null,
  activeTab: 'chats',

  // Auth
  isAuthenticated: false,
  isFirebaseReady: false,
  currentUser: null,

  // Chats
  chats: [],
  activeChatId: null,
  messages: {},

  // User profiles
  userProfiles: {},

  // Calls
  callHistory: [],
  activeCall: null,
  incomingCall: null,

  // UI
  searchQuery: '',
  isSearching: false,
  showFABMenu: false,
  authLoading: false,
  authError: null,

  // Storage uploads
  storageUploads: {},

  // Message search
  messageSearchResults: [],
  messageSearchQuery: '',
  isSearchingMessages: false,

  // Pagination
  hasMoreMessages: {},
  isLoadingMoreMessages: false,

  // Firebase unsubscribers
  _unsubs: [],

  // Actions
  setScreen: (screen) =>
    set((state) => ({
      previousScreen: state.currentScreen,
      currentScreen: screen,
    })),

  setActiveTab: (tab) => set({ activeTab: tab }),

  goBack: () =>
    set((state) => ({
      currentScreen: state.previousScreen || 'home',
      previousScreen: null,
    })),

  login: (user) =>
    set({
      isAuthenticated: true,
      currentUser: user,
      currentScreen: 'home',
      userProfiles: { [user.uid]: user },
    }),

  logout: () => {
    // Clean up all Firebase listeners
    const unsubs = get()._unsubs;
    unsubs.forEach((fn) => fn());

    set({
      isAuthenticated: false,
      currentUser: null,
      currentScreen: 'auth-login',
      chats: [],
      messages: {},
      callHistory: [],
      activeCall: null,
      activeChatId: null,
      userProfiles: {},
      _unsubs: [],
    });
  },

  setFirebaseReady: () => set({ isFirebaseReady: true }),

  setActiveChat: (chatId) => set({ activeChatId: chatId, currentScreen: 'chat' }),

  setChats: (chats) => set({ chats }),

  setMessages: (chatId, messages) =>
    set((state) => ({
      messages: { ...state.messages, [chatId]: messages },
    })),

  addMessage: (chatId, message) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: [...(state.messages[chatId] || []), message],
      },
    })),

  updateMessage: (chatId, messageId, updates) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: (state.messages[chatId] || []).map((m) =>
          m.id === messageId ? { ...m, ...updates } : m
        ),
      },
    })),

  setUserProfiles: (profiles) =>
    set((state) => ({
      userProfiles: { ...state.userProfiles, ...profiles },
    })),

  setCallHistory: (calls) => set({ callHistory: calls }),

  setCallStatus: (status) =>
    set((state) => ({
      activeCall: state.activeCall ? { ...state.activeCall, status } : null,
    })),

  startCall: (type, user) => {
    const currentUser = get().currentUser;
    if (!currentUser) return;

    // Reset any previous WebRTC instance
    resetWebRTC();
    const webrtc = getWebRTC();

    // Set up callbacks
    webrtc.onRemoteStream = (stream) => {
      useZixoStore.getState().setCallRemoteStream(stream);
      useZixoStore.getState().setCallStatus('connected');
    };

    webrtc.onConnectionStateChange = (state) => {
      if (state === 'disconnected' || state === 'failed') {
        useZixoStore.getState().endCall();
      }
    };

    // Set initial UI state immediately
    set({
      activeCall: {
        status: 'ringing',
        type,
        remoteUser: user,
        duration: 0,
        isMuted: false,
        isSpeakerOn: type === 'audio',
        isVideoOn: type === 'video',
        localStream: null,
        remoteStream: null,
        callId: null,
      },
      currentScreen: type === 'audio' ? 'audio-call' : 'video-call',
    });

    // Initiate WebRTC call asynchronously
    webrtc.startCall(currentUser.uid, currentUser.displayName, user.uid, type)
      .then((callId) => {
        // Update call with callId and local stream
        const localStream = webrtc.getLocalStream();
        set((state) => ({
          activeCall: state.activeCall
            ? { ...state.activeCall, callId, localStream, status: 'connecting' }
            : null,
        }));
      })
      .catch((err) => {
        console.error('[Zixo] Failed to start call:', err);
        // Clean up on failure
        webrtc.endCall();
        set({ activeCall: null, currentScreen: 'home' });
      });
  },

  answerCall: (callId, callerProfile, callType) => {
    const incoming = get().incomingCall;
    if (!incoming) return;

    // Reset any previous WebRTC instance
    resetWebRTC();
    const webrtc = getWebRTC();

    // Set up callbacks
    webrtc.onRemoteStream = (stream) => {
      useZixoStore.getState().setCallRemoteStream(stream);
      useZixoStore.getState().setCallStatus('connected');
    };

    webrtc.onConnectionStateChange = (state) => {
      if (state === 'disconnected' || state === 'failed') {
        useZixoStore.getState().endCall();
      }
    };

    // Set initial UI state
    set({
      activeCall: {
        status: 'connecting',
        type: callType,
        remoteUser: callerProfile,
        duration: 0,
        isMuted: false,
        isSpeakerOn: callType === 'audio',
        isVideoOn: callType === 'video',
        localStream: null,
        remoteStream: null,
        callId,
      },
      incomingCall: null,
      currentScreen: callType === 'audio' ? 'audio-call' : 'video-call',
    });

    // Answer the call via WebRTC
    webrtc.answerCall(callId, incoming.callData)
      .then(() => {
        const localStream = webrtc.getLocalStream();
        set((state) => ({
          activeCall: state.activeCall
            ? { ...state.activeCall, localStream, status: 'connecting' }
            : null,
        }));
      })
      .catch((err) => {
        console.error('[Zixo] Failed to answer call:', err);
        webrtc.endCall();
        set({ activeCall: null, currentScreen: 'home' });
      });
  },

  rejectCall: () => {
    const incoming = get().incomingCall;
    if (incoming) {
      // End the call signal so the caller knows it was rejected
      endCallSignal(incoming.callId);
    }
    set({ incomingCall: null, activeCall: null, currentScreen: 'home' });
  },

  endCall: () => {
    const { activeCall } = get();

    // End WebRTC connection
    try {
      getWebRTC().endCall();
    } catch (e) {
      // WebRTC might not be initialized
    }

    if (activeCall && activeCall.remoteUser) {
      const newCall: CallRecord = {
        id: activeCall.callId || `call-${Date.now()}`,
        callerId: get().currentUser?.uid || 'user-me',
        callerName: get().currentUser?.displayName || 'You',
        callerAvatar: '',
        receiverId: activeCall.remoteUser.uid,
        receiverName: activeCall.remoteUser.displayName,
        receiverAvatar: '',
        type: activeCall.type,
        direction: 'outgoing',
        duration: activeCall.duration,
        timestamp: Date.now(),
      };

      set((state) => ({
        callHistory: [newCall, ...state.callHistory],
        activeCall: null,
        incomingCall: null,
        currentScreen: 'home',
      }));
    } else {
      set({ activeCall: null, incomingCall: null, currentScreen: 'home' });
    }
  },

  toggleMute: () => {
    const { activeCall } = get();
    if (activeCall) {
      const newMuted = !activeCall.isMuted;
      try { getWebRTC().toggleMute(newMuted); } catch {}
      set({ activeCall: { ...activeCall, isMuted: newMuted } });
    }
  },

  toggleSpeaker: () =>
    set((state) => ({
      activeCall: state.activeCall
        ? { ...state.activeCall, isSpeakerOn: !state.activeCall.isSpeakerOn }
        : null,
    })),

  toggleVideo: () => {
    const { activeCall } = get();
    if (activeCall) {
      const newVideoOn = !activeCall.isVideoOn;
      try { getWebRTC().toggleVideo(newVideoOn); } catch {}
      set({ activeCall: { ...activeCall, isVideoOn: newVideoOn } });
    }
  },

  setCallRemoteStream: (stream) =>
    set((state) => ({
      activeCall: state.activeCall
        ? { ...state.activeCall, remoteStream: stream }
        : null,
    })),

  setCallLocalStream: (stream) =>
    set((state) => ({
      activeCall: state.activeCall
        ? { ...state.activeCall, localStream: stream }
        : null,
    })),

  setIncomingCall: (incoming) =>
    set({ incomingCall: incoming }),

  setSearchQuery: (query) => set({ searchQuery: query }),
  toggleSearching: () => set((state) => ({ isSearching: !state.isSearching, searchQuery: '' })),
  toggleFABMenu: () => set((state) => ({ showFABMenu: !state.showFABMenu })),

  markChatRead: (chatId) =>
    set((state) => ({
      chats: state.chats.map((c) =>
        c.id === chatId ? { ...c, unreadCount: 0 } : c
      ),
    })),

  setAuthLoading: (loading) => set({ authLoading: loading }),
  setAuthError: (error) => set({ authError: error }),

  addUnsub: (unsub) =>
    set((state) => ({
      _unsubs: [...state._unsubs, unsub],
    })),

  clearUnsubs: () => {
    const unsubs = get()._unsubs;
    unsubs.forEach((fn) => fn());
    set({ _unsubs: [] });
  },

  // Storage upload tracking
  updateStorageUpload: (uploadId, progress) =>
    set((state) => ({
      storageUploads: { ...state.storageUploads, [uploadId]: progress },
    })),

  removeStorageUpload: (uploadId) =>
    set((state) => {
      const { [uploadId]: _, ...rest } = state.storageUploads;
      return { storageUploads: rest };
    }),

  // Update user profile (syncs with Firestore externally)
  updateUserProfile: (updates) =>
    set((state) => {
      if (!state.currentUser) return {};
      const updatedUser = { ...state.currentUser, ...updates };
      return {
        currentUser: updatedUser,
        userProfiles: { ...state.userProfiles, [updatedUser.uid]: updatedUser },
      };
    }),

  // Delete a chat from local state (Firestore deletion handled separately)
  deleteChat: (chatId) =>
    set((state) => {
      const { [chatId]: _, ...remainingMessages } = state.messages;
      return {
        chats: state.chats.filter((c) => c.id !== chatId),
        messages: remainingMessages,
        activeChatId: state.activeChatId === chatId ? null : state.activeChatId,
        currentScreen: state.activeChatId === chatId ? 'home' : state.currentScreen,
      };
    }),

  // Search messages (results populated externally from Firestore service)
  searchMessages: (query) =>
    set({ messageSearchQuery: query, isSearchingMessages: true }),

  clearMessageSearch: () =>
    set({ messageSearchResults: [], messageSearchQuery: '', isSearchingMessages: false }),

  // Load more messages (pagination trigger)
  loadMoreMessages: (chatId) =>
    set({ isLoadingMoreMessages: true }),

  setHasMoreMessages: (chatId, hasMore) =>
    set((state) => ({
      hasMoreMessages: { ...state.hasMoreMessages, [chatId]: hasMore },
    })),

  prependMessages: (chatId, newMessages) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: [...newMessages, ...(state.messages[chatId] || [])],
      },
      isLoadingMoreMessages: false,
    })),
}));

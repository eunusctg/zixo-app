import { create } from 'zustand';
import type { ZixoUserProfile } from '@/services/auth';
import type { FirestoreChat, FirestoreMessage, FirestoreCall } from '@/services/firestore';
import type { UploadProgress } from '@/services/storage';
import { getWebRTC, resetWebRTC } from '@/services/webrtc';
import { endCallSignal, type RTDBCallSignal } from '@/services/presence';

// Permission types for pre-call permission flow
export type PermissionStatus = 'granted' | 'denied' | 'prompt' | 'unavailable';

export interface PermissionRequest {
  type: 'camera' | 'microphone' | 'location';
  status: 'requesting' | 'granted' | 'denied' | 'error';
  message?: string;
}

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
  | 'auth'
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
    startedAt: number | null; // Track when the call connected for duration
    isIncoming: boolean; // Track if this was an incoming call
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

  // Permissions
  showPermissionModal: boolean;
  permissionRequests: PermissionRequest[];
  permissionCallback: ((granted: boolean) => void) | null;

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

  // Permissions
  showPermissionModal: false,
  permissionRequests: [],
  permissionCallback: null,

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
      currentScreen: 'auth',
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
      activeCall: state.activeCall ? { ...state.activeCall, status, startedAt: status === 'connected' && !state.activeCall.startedAt ? Date.now() : state.activeCall.startedAt } : null,
    })),

  startCall: (type, user) => {
    const currentUser = get().currentUser;
    if (!currentUser) return;

    // Build permission requests list
    const neededPermissions: PermissionRequest[] = [];
    if (type === 'video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });
    neededPermissions.push({ type: 'location', status: 'requesting', message: 'Location sharing for emergency services' });

    // Show permission modal, then proceed with call on grant
    set({
      showPermissionModal: true,
      permissionRequests: neededPermissions,
      permissionCallback: (granted: boolean) => {
        if (!granted) return;

        const state = useZixoStore.getState();
        const currentUser = state.currentUser;
        if (!currentUser) return;

        // Reset any previous WebRTC instance
        resetWebRTC();
        const webrtc = getWebRTC();

        // Set up callbacks
        webrtc.onRemoteStream = (stream) => {
          useZixoStore.getState().setCallRemoteStream(stream);
          useZixoStore.getState().setCallStatus('connected');
        };

        webrtc.onConnectionStateChange = (connState) => {
          if (connState === 'disconnected' || connState === 'failed') {
            useZixoStore.getState().endCall();
          }
        };

        // Set initial UI state immediately
        useZixoStore.setState({
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
            startedAt: null,
            isIncoming: false,
          },
          currentScreen: type === 'audio' ? 'audio-call' : 'video-call',
        });

        // Initiate WebRTC call asynchronously
        webrtc.startCall(currentUser.uid, currentUser.displayName, user.uid, type)
          .then((callId) => {
            const localStream = webrtc.getLocalStream();
            useZixoStore.setState((s: any) => ({
              activeCall: s.activeCall
                ? { ...s.activeCall, callId, localStream, status: 'connecting' }
                : null,
            }));
          })
          .catch((err: any) => {
            console.error('[Zixo] Failed to start call:', err);
            try { webrtc.endCall(); } catch {}
            useZixoStore.setState({ activeCall: null, currentScreen: 'home' });
            if (err?.name === 'NotAllowedError') {
              alert('Camera/Microphone permission denied. Please allow access in your browser settings.');
            } else {
              alert('Failed to start call. Please try again.');
            }
          });
      },
    });
  },

  answerCall: (callId, callerProfile, callType) => {
    const incoming = get().incomingCall;
    if (!incoming) return;

    // Build permission requests list
    const neededPermissions: PermissionRequest[] = [];
    if (callType === 'video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });
    neededPermissions.push({ type: 'location', status: 'requesting', message: 'Location sharing for emergency services' });

    // Show permission modal, then proceed with answering on grant
    set({
      showPermissionModal: true,
      permissionRequests: neededPermissions,
      permissionCallback: (granted: boolean) => {
        if (!granted) {
          // Reject the incoming call if permissions denied
          const s = useZixoStore.getState();
          if (s.incomingCall) {
            endCallSignal(s.incomingCall.callId);
          }
          useZixoStore.setState({ incomingCall: null, activeCall: null });
          return;
        }

        const currentIncoming = useZixoStore.getState().incomingCall;
        if (!currentIncoming) return;

        // Reset any previous WebRTC instance
        resetWebRTC();
        const webrtc = getWebRTC();

        // Set up callbacks
        webrtc.onRemoteStream = (stream) => {
          useZixoStore.getState().setCallRemoteStream(stream);
          useZixoStore.getState().setCallStatus('connected');
        };

        webrtc.onConnectionStateChange = (connState) => {
          if (connState === 'disconnected' || connState === 'failed') {
            useZixoStore.getState().endCall();
          }
        };

        // Set initial UI state
        useZixoStore.setState({
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
            startedAt: null,
            isIncoming: true,
          },
          incomingCall: null,
          currentScreen: callType === 'audio' ? 'audio-call' : 'video-call',
        });

        // Answer the call via WebRTC
        webrtc.answerCall(callId, currentIncoming.callData)
          .then(() => {
            const localStream = webrtc.getLocalStream();
            useZixoStore.setState((s: any) => ({
              activeCall: s.activeCall
                ? { ...s.activeCall, localStream, status: 'connecting', startedAt: Date.now() }
                : null,
            }));
          })
          .catch((err: any) => {
            console.error('[Zixo] Failed to answer call:', err);
            try { webrtc.endCall(); } catch {}
            useZixoStore.setState({ activeCall: null, currentScreen: 'home' });
            if (err?.name === 'NotAllowedError') {
              alert('Camera/Microphone permission denied. Please allow access in your browser settings.');
            } else {
              alert('Failed to answer call. Please try again.');
            }
          });
      },
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
    const { activeCall, currentUser } = get();

    // End WebRTC connection
    try {
      getWebRTC().endCall();
    } catch (e) {
      // WebRTC might not be initialized
    }

    if (activeCall && activeCall.remoteUser) {
      // Calculate actual duration from startedAt
      const actualDuration = activeCall.startedAt
        ? Math.floor((Date.now() - activeCall.startedAt) / 1000)
        : activeCall.duration;

      // Determine direction based on isIncoming flag
      const direction: 'incoming' | 'outgoing' | 'missed' = activeCall.isIncoming
        ? 'incoming'
        : 'outgoing';

      const newCall: CallRecord = {
        id: activeCall.callId || `call-${Date.now()}`,
        callerId: activeCall.isIncoming ? activeCall.remoteUser.uid : (currentUser?.uid || 'user-me'),
        callerName: activeCall.isIncoming ? activeCall.remoteUser.displayName : (currentUser?.displayName || 'You'),
        callerAvatar: '',
        receiverId: activeCall.isIncoming ? (currentUser?.uid || 'user-me') : activeCall.remoteUser.uid,
        receiverName: activeCall.isIncoming ? (currentUser?.displayName || 'You') : activeCall.remoteUser.displayName,
        receiverAvatar: '',
        type: activeCall.type,
        direction,
        duration: actualDuration,
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

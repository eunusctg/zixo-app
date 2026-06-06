import { create } from 'zustand';
import type { ZixoUserProfile } from '@/services/auth';
import type { FirestoreChat, FirestoreMessage, FirestoreCall } from '@/services/firestore';
import type { UploadProgress } from '@/services/storage';

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
  | 'new-chat';

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
  endCall: () => void;
  toggleMute: () => void;
  toggleSpeaker: () => void;
  toggleVideo: () => void;
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

  startCall: (type, user) =>
    set({
      activeCall: {
        status: 'ringing',
        type,
        remoteUser: user,
        duration: 0,
        isMuted: false,
        isSpeakerOn: type === 'audio',
        isVideoOn: type === 'video',
      },
      currentScreen: type === 'audio' ? 'audio-call' : 'video-call',
    }),

  endCall: () => {
    const { activeCall } = get();
    if (activeCall && activeCall.remoteUser) {
      const newCall: CallRecord = {
        id: `call-${Date.now()}`,
        callerId: 'user-me',
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
        currentScreen: 'home',
      }));
    } else {
      set({ activeCall: null, currentScreen: 'home' });
    }
  },

  toggleMute: () =>
    set((state) => ({
      activeCall: state.activeCall
        ? { ...state.activeCall, isMuted: !state.activeCall.isMuted }
        : null,
    })),

  toggleSpeaker: () =>
    set((state) => ({
      activeCall: state.activeCall
        ? { ...state.activeCall, isSpeakerOn: !state.activeCall.isSpeakerOn }
        : null,
    })),

  toggleVideo: () =>
    set((state) => ({
      activeCall: state.activeCall
        ? { ...state.activeCall, isVideoOn: !state.activeCall.isVideoOn }
        : null,
    })),

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

// ==================== DEMO DATA (Fallback when no Firebase connection) ====================
// This is used when Firebase is not yet connected or for demo/preview mode

export const DEMO_USERS: ZixoUserProfile[] = [
  {
    uid: 'demo-1',
    displayName: 'Sarah Chen',
    email: 'sarah@zixo.app',
    username: '@sarahchen',
    bio: 'Designer & dreamer',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
    createdAt: Date.now(),
  },
  {
    uid: 'demo-2',
    displayName: 'Marcus Rivera',
    email: 'marcus@zixo.app',
    username: '@marcusriv',
    bio: 'Code. Coffee. Repeat.',
    avatar: '',
    online: false,
    lastSeen: Date.now() - 3600000,
    createdAt: Date.now(),
  },
  {
    uid: 'demo-3',
    displayName: 'Yuki Tanaka',
    email: 'yuki@zixo.app',
    username: '@yukitan',
    bio: 'Music lover & cat person 🐱',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
    createdAt: Date.now(),
  },
  {
    uid: 'demo-4',
    displayName: 'Priya Sharma',
    email: 'priya@zixo.app',
    username: '@priyash',
    bio: 'Exploring the world one city at a time',
    avatar: '',
    online: false,
    lastSeen: Date.now() - 7200000,
    createdAt: Date.now(),
  },
  {
    uid: 'demo-5',
    displayName: 'Jordan Blake',
    email: 'jordan@zixo.app',
    username: '@jblake',
    bio: 'Photographer | Storyteller',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
    createdAt: Date.now(),
  },
  {
    uid: 'demo-6',
    displayName: 'Emma Wilson',
    email: 'emma@zixo.app',
    username: '@emmaw',
    bio: 'Just here for the calls 😄',
    avatar: '',
    online: false,
    lastSeen: Date.now() - 1800000,
    createdAt: Date.now(),
  },
];

export function generateDemoChats(currentUser: ZixoUserProfile): Chat[] {
  const now = Date.now();
  return DEMO_USERS.map((user, i) => ({
    id: `demo-chat-${i + 1}`,
    participants: [currentUser.uid, user.uid],
    participantProfiles: [currentUser, user],
    lastMessage: {
      id: `demo-msg-last-${i + 1}`,
      chatId: `demo-chat-${i + 1}`,
      senderId: user.uid,
      text: ['Hey! How are you? 👋', 'Check this out!', 'Let me call you later', 'Thanks for sharing!', 'See you soon! ✨', 'Got it, thanks!'][i],
      type: 'text' as const,
      timestamp: now - [300000, 900000, 3600000, 7200000, 14400000, 86400000][i],
      status: 'delivered' as const,
    },
    unreadCount: [3, 1, 0, 0, 2, 0][i],
    isGroup: false,
    typing: i === 0 ? [user.uid] : [],
    createdAt: now - 86400000 * (i + 1),
    updatedAt: now - [300000, 900000, 3600000, 7200000, 14400000, 86400000][i],
  }));
}

export function generateDemoMessages(chatId: string, otherUserId: string): Message[] {
  const now = Date.now();
  return [
    {
      id: `${chatId}-1`,
      chatId,
      senderId: otherUserId,
      text: 'Hey! How are you doing? 👋',
      type: 'text',
      timestamp: now - 86400000,
      status: 'read',
    },
    {
      id: `${chatId}-2`,
      chatId,
      senderId: 'user-me',
      text: "I'm great! Just finished setting up Zixo 🚀",
      type: 'text',
      timestamp: now - 86300000,
      status: 'read',
    },
    {
      id: `${chatId}-3`,
      chatId,
      senderId: otherUserId,
      text: "That's awesome! The app looks so clean and fast",
      type: 'text',
      timestamp: now - 86200000,
      status: 'read',
    },
    {
      id: `${chatId}-4`,
      chatId,
      senderId: 'user-me',
      text: 'Right? No social clutter, just pure connection ✨',
      type: 'text',
      timestamp: now - 86100000,
      status: 'read',
    },
    {
      id: `${chatId}-5`,
      chatId,
      senderId: otherUserId,
      text: 'Want to do a quick video call later?',
      type: 'text',
      timestamp: now - 3600000,
      status: 'read',
    },
    {
      id: `${chatId}-6`,
      chatId,
      senderId: 'user-me',
      text: 'Sure! Let me know when you are free 😊',
      type: 'text',
      timestamp: now - 3500000,
      status: 'delivered',
    },
  ];
}

export function generateDemoCalls(): CallRecord[] {
  const now = Date.now();
  return [
    {
      id: 'demo-call-1',
      callerId: 'demo-1',
      callerName: 'Sarah Chen',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'You',
      receiverAvatar: '',
      type: 'video',
      direction: 'incoming',
      duration: 342,
      timestamp: now - 1800000,
    },
    {
      id: 'demo-call-2',
      callerId: 'user-me',
      callerName: 'You',
      callerAvatar: '',
      receiverId: 'demo-2',
      receiverName: 'Marcus Rivera',
      receiverAvatar: '',
      type: 'audio',
      direction: 'outgoing',
      duration: 145,
      timestamp: now - 5400000,
    },
    {
      id: 'demo-call-3',
      callerId: 'demo-3',
      callerName: 'Yuki Tanaka',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'You',
      receiverAvatar: '',
      type: 'video',
      direction: 'missed',
      duration: 0,
      timestamp: now - 10800000,
    },
    {
      id: 'demo-call-4',
      callerId: 'user-me',
      callerName: 'You',
      callerAvatar: '',
      receiverId: 'demo-5',
      receiverName: 'Jordan Blake',
      receiverAvatar: '',
      type: 'audio',
      direction: 'outgoing',
      duration: 523,
      timestamp: now - 86400000,
    },
    {
      id: 'demo-call-5',
      callerId: 'demo-4',
      callerName: 'Priya Sharma',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'You',
      receiverAvatar: '',
      type: 'video',
      direction: 'incoming',
      duration: 67,
      timestamp: now - 172800000,
    },
    {
      id: 'demo-call-6',
      callerId: 'demo-6',
      callerName: 'Emma Wilson',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'You',
      receiverAvatar: '',
      type: 'audio',
      direction: 'missed',
      duration: 0,
      timestamp: now - 259200000,
    },
  ];
}

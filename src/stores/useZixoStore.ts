import { create } from 'zustand';

// Types
export interface UserProfile {
  uid: string;
  displayName: string;
  email: string;
  username: string;
  bio: string;
  avatar: string;
  online: boolean;
  lastSeen: number;
  publicKey?: string;
}

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
  participantProfiles: UserProfile[];
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

interface ZixoState {
  // Navigation
  currentScreen: Screen;
  previousScreen: Screen | null;
  activeTab: Tab;

  // Auth
  isAuthenticated: boolean;
  currentUser: UserProfile | null;

  // Chats
  chats: Chat[];
  activeChatId: string | null;
  messages: Record<string, Message[]>;

  // Calls
  callHistory: CallRecord[];
  activeCall: {
    status: CallStatus;
    type: 'audio' | 'video';
    remoteUser: UserProfile | null;
    duration: number;
    isMuted: boolean;
    isSpeakerOn: boolean;
    isVideoOn: boolean;
  } | null;

  // UI
  searchQuery: string;
  isSearching: boolean;
  showFABMenu: boolean;

  // Demo data initialized flag
  demoInitialized: boolean;

  // Actions
  setScreen: (screen: Screen) => void;
  setActiveTab: (tab: Tab) => void;
  goBack: () => void;
  login: (user: UserProfile) => void;
  logout: () => void;
  setActiveChat: (chatId: string) => void;
  sendMessage: (chatId: string, text: string, type?: Message['type']) => void;
  setCallStatus: (status: CallStatus) => void;
  startCall: (type: 'audio' | 'video', user: UserProfile) => void;
  endCall: () => void;
  toggleMute: () => void;
  toggleSpeaker: () => void;
  toggleVideo: () => void;
  setSearchQuery: (query: string) => void;
  toggleSearching: () => void;
  toggleFABMenu: () => void;
  initDemoData: () => void;
  markChatRead: (chatId: string) => void;
}

const DEMO_CURRENT_USER: UserProfile = {
  uid: 'user-me',
  displayName: 'Alex Johnson',
  email: 'alex@zixo.app',
  username: '@alexiwe',
  bio: 'Living free, connecting freely 🌍',
  avatar: '',
  online: true,
  lastSeen: Date.now(),
};

const DEMO_USERS: UserProfile[] = [
  {
    uid: 'user-1',
    displayName: 'Sarah Chen',
    email: 'sarah@zixo.app',
    username: '@sarahchen',
    bio: 'Designer & dreamer',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
  },
  {
    uid: 'user-2',
    displayName: 'Marcus Rivera',
    email: 'marcus@zixo.app',
    username: '@marcusriv',
    bio: 'Code. Coffee. Repeat.',
    avatar: '',
    online: false,
    lastSeen: Date.now() - 3600000,
  },
  {
    uid: 'user-3',
    displayName: 'Yuki Tanaka',
    email: 'yuki@zixo.app',
    username: '@yukitan',
    bio: 'Music lover & cat person 🐱',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
  },
  {
    uid: 'user-4',
    displayName: 'Priya Sharma',
    email: 'priya@zixo.app',
    username: '@priyash',
    bio: 'Exploring the world one city at a time',
    avatar: '',
    online: false,
    lastSeen: Date.now() - 7200000,
  },
  {
    uid: 'user-5',
    displayName: 'Jordan Blake',
    email: 'jordan@zixo.app',
    username: '@jblake',
    bio: 'Photographer | Storyteller',
    avatar: '',
    online: true,
    lastSeen: Date.now(),
  },
  {
    uid: 'user-6',
    displayName: 'Emma Wilson',
    email: 'emma@zixo.app',
    username: '@emmaw',
    bio: 'Just here for the calls 😄',
    avatar: '',
    online: false,
    lastSeen: Date.now() - 1800000,
  },
];

const generateMessages = (chatId: string, otherUserId: string): Message[] => {
  const now = Date.now();
  const messages: Message[] = [
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
  return messages;
};

const generateDemoChats = (): Chat[] => {
  const now = Date.now();
  return DEMO_USERS.map((user, i) => ({
    id: `chat-${i + 1}`,
    participants: ['user-me', user.uid],
    participantProfiles: [DEMO_CURRENT_USER, user],
    lastMessage: {
      id: `chat-${i + 1}-last`,
      chatId: `chat-${i + 1}`,
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
};

const generateDemoCalls = (): CallRecord[] => {
  const now = Date.now();
  return [
    {
      id: 'call-1',
      callerId: 'user-1',
      callerName: 'Sarah Chen',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'Alex Johnson',
      receiverAvatar: '',
      type: 'video',
      direction: 'incoming',
      duration: 342,
      timestamp: now - 1800000,
    },
    {
      id: 'call-2',
      callerId: 'user-me',
      callerName: 'Alex Johnson',
      callerAvatar: '',
      receiverId: 'user-2',
      receiverName: 'Marcus Rivera',
      receiverAvatar: '',
      type: 'audio',
      direction: 'outgoing',
      duration: 145,
      timestamp: now - 5400000,
    },
    {
      id: 'call-3',
      callerId: 'user-3',
      callerName: 'Yuki Tanaka',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'Alex Johnson',
      receiverAvatar: '',
      type: 'video',
      direction: 'missed',
      duration: 0,
      timestamp: now - 10800000,
    },
    {
      id: 'call-4',
      callerId: 'user-me',
      callerName: 'Alex Johnson',
      callerAvatar: '',
      receiverId: 'user-5',
      receiverName: 'Jordan Blake',
      receiverAvatar: '',
      type: 'audio',
      direction: 'outgoing',
      duration: 523,
      timestamp: now - 86400000,
    },
    {
      id: 'call-5',
      callerId: 'user-4',
      callerName: 'Priya Sharma',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'Alex Johnson',
      receiverAvatar: '',
      type: 'video',
      direction: 'incoming',
      duration: 67,
      timestamp: now - 172800000,
    },
    {
      id: 'call-6',
      callerId: 'user-6',
      callerName: 'Emma Wilson',
      callerAvatar: '',
      receiverId: 'user-me',
      receiverName: 'Alex Johnson',
      receiverAvatar: '',
      type: 'audio',
      direction: 'missed',
      duration: 0,
      timestamp: now - 259200000,
    },
  ];
};

export const useZixoStore = create<ZixoState>((set, get) => ({
  // Navigation
  currentScreen: 'splash',
  previousScreen: null,
  activeTab: 'chats',

  // Auth
  isAuthenticated: false,
  currentUser: null,

  // Chats
  chats: [],
  activeChatId: null,
  messages: {},

  // Calls
  callHistory: [],
  activeCall: null,

  // UI
  searchQuery: '',
  isSearching: false,
  showFABMenu: false,

  // Demo
  demoInitialized: false,

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
    }),

  logout: () =>
    set({
      isAuthenticated: false,
      currentUser: null,
      currentScreen: 'auth-login',
      chats: [],
      messages: {},
      callHistory: [],
      activeCall: null,
      activeChatId: null,
      demoInitialized: false,
    }),

  setActiveChat: (chatId) => set({ activeChatId: chatId, currentScreen: 'chat' }),

  sendMessage: (chatId, text, type = 'text') => {
    const newMsg: Message = {
      id: `msg-${Date.now()}`,
      chatId,
      senderId: 'user-me',
      text,
      type,
      timestamp: Date.now(),
      status: 'sending',
    };

    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: [...(state.messages[chatId] || []), newMsg],
      },
    }));

    // Simulate message delivery
    setTimeout(() => {
      set((state) => ({
        messages: {
          ...state.messages,
          [chatId]: (state.messages[chatId] || []).map((m) =>
            m.id === newMsg.id ? { ...m, status: 'delivered' as const } : m
          ),
        },
      }));
    }, 1000);

    // Simulate read receipt
    setTimeout(() => {
      set((state) => ({
        messages: {
          ...state.messages,
          [chatId]: (state.messages[chatId] || []).map((m) =>
            m.id === newMsg.id ? { ...m, status: 'read' as const } : m
          ),
        },
      }));
    }, 2500);

    // Simulate reply for demo
    const chat = get().chats.find((c) => c.id === chatId);
    if (chat && !chat.isGroup) {
      const otherUser = chat.participantProfiles.find((p) => p.uid !== 'user-me');
      if (otherUser) {
        // Show typing indicator
        setTimeout(() => {
          set((state) => ({
            chats: state.chats.map((c) =>
              c.id === chatId ? { ...c, typing: [otherUser.uid] } : c
            ),
          }));
        }, 1500);

        // Send reply
        setTimeout(() => {
          const replies = [
            'Sounds great! 😊',
            'I love that idea!',
            'Let me think about it...',
            'Absolutely! Count me in 🙌',
            'Haha, nice one! 😄',
            "I'll get back to you on that",
            'That works for me!',
            'Perfect! Talk soon 🤙',
          ];
          const reply: Message = {
            id: `msg-reply-${Date.now()}`,
            chatId,
            senderId: otherUser.uid,
            text: replies[Math.floor(Math.random() * replies.length)],
            type: 'text',
            timestamp: Date.now(),
            status: 'read',
          };

          set((state) => ({
            messages: {
              ...state.messages,
              [chatId]: [...(state.messages[chatId] || []), reply],
            },
            chats: state.chats.map((c) =>
              c.id === chatId
                ? { ...c, typing: [], lastMessage: reply, updatedAt: Date.now() }
                : c
            ),
          }));
        }, 3500);
      }
    }

    // Update chat last message
    set((state) => ({
      chats: state.chats.map((c) =>
        c.id === chatId ? { ...c, lastMessage: newMsg, updatedAt: Date.now() } : c
      ),
    }));
  },

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

  initDemoData: () => {
    if (get().demoInitialized) return;

    const chats = generateDemoChats();
    const messagesMap: Record<string, Message[]> = {};
    chats.forEach((chat) => {
      const otherUser = chat.participantProfiles.find((p) => p.uid !== 'user-me');
      if (otherUser) {
        messagesMap[chat.id] = generateMessages(chat.id, otherUser.uid);
      }
    });

    set({
      chats,
      messages: messagesMap,
      callHistory: generateDemoCalls(),
      demoInitialized: true,
    });
  },
}));

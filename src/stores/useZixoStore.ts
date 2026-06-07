import { create } from 'zustand';
import type { ZixoUserProfile } from '@/services/auth';
import type { FirestoreChat, FirestoreMessage, FirestoreCall } from '@/services/firestore';
import type { UploadProgress } from '@/services/storage';
import { getWebRTC, resetWebRTC } from '@/services/webrtc';
import { getGroupWebRTC, resetGroupWebRTC } from '@/services/webrtc-group';
import { endCallSignal, type RTDBCallSignal, leaveGroupCallSignal, type RTDBGroupCallSignal } from '@/services/presence';

// ==================== PERMISSION HELPERS ====================

/**
 * Check if call permissions have already been decided (granted or denied).
 * Returns true if all required permissions are already decided (no need to show modal).
 * Returns false if at least one permission is in 'prompt' state (need to show modal).
 */
async function checkCallPermissions(type: 'audio' | 'video'): Promise<boolean> {
  try {
    const micStatus = await navigator.permissions.query({ name: 'microphone' as PermissionName });
    if (micStatus.state === 'granted') return true;
    if (micStatus.state === 'denied') return true; // Already decided, don't ask again
    // micStatus.state === 'prompt' - need to ask
    if (type === 'video') {
      const camStatus = await navigator.permissions.query({ name: 'camera' as PermissionName });
      if (camStatus.state === 'granted' || camStatus.state === 'denied') {
        // Camera is decided, only show modal if mic still needs asking
        return micStatus.state !== 'prompt';
      }
      // Camera is also in 'prompt' state
      return false;
    }
    return false; // Mic is in 'prompt' state, need to show permission modal
  } catch {
    return false; // permissions.query not supported, show modal
  }
}

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
  type: 'audio' | 'video' | 'group-audio' | 'group-video';
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
  | 'group-audio-call'
  | 'group-video-call'
  | 'incoming-group-call'
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

  // Group call state
  groupCall: {
    callId: string | null;
    status: 'idle' | 'ringing' | 'connecting' | 'active' | 'ended';
    type: 'group-audio' | 'group-video';
    participants: Array<{ uid: string; displayName: string; avatar: string; stream: MediaStream | null; isMuted: boolean; isVideoOn: boolean }>;
    localStream: MediaStream | null;
    isMuted: boolean;
    isSpeakerOn: boolean;
    isVideoOn: boolean;
    isIncoming: boolean;
    startedAt: number | null;
  } | null;

  incomingGroupCall: {
    callId: string;
    callerName: string;
    callerId: string;
    callType: 'group-audio' | 'group-video';
    participantNames: string[];
    callData: RTDBGroupCallSignal;
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
  startGroupCall: (type: 'group-audio' | 'group-video', participants: ZixoUserProfile[]) => void;
  answerGroupCall: (callId: string) => void;
  rejectGroupCall: () => void;
  leaveGroupCall: () => void;
  addGroupCallParticipant: (uid: string, displayName: string, stream: MediaStream) => void;
  removeGroupCallParticipant: (uid: string) => void;
  setGroupCallLocalStream: (stream: MediaStream) => void;
  toggleGroupCallMute: () => void;
  toggleGroupCallVideo: () => void;
  toggleGroupCallSpeaker: () => void;
  setIncomingGroupCall: (incoming: {
    callId: string;
    callerName: string;
    callerId: string;
    callType: 'group-audio' | 'group-video';
    participantNames: string[];
    callData: RTDBGroupCallSignal;
  } | null) => void;
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

  // Group calls
  groupCall: null,
  incomingGroupCall: null,

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
    set((state) => {
      const merged = { ...state.userProfiles, ...profiles };
      // Always preserve the current user's profile from currentUser (source of truth)
      // so that stale Firestore reads don't overwrite an in-flight profile update
      if (state.currentUser) {
        merged[state.currentUser.uid] = {
          ...merged[state.currentUser.uid],
          ...state.currentUser,
        };
      }
      return { userProfiles: merged };
    }),

  setCallHistory: (calls) => set({ callHistory: calls }),

  setCallStatus: (status) =>
    set((state) => ({
      activeCall: state.activeCall ? { ...state.activeCall, status, startedAt: status === 'connected' && !state.activeCall.startedAt ? Date.now() : state.activeCall.startedAt } : null,
    })),

  startCall: (type, user) => {
    const currentUser = get().currentUser;
    if (!currentUser) return;

    // Build permission requests list (no 'location' - not needed for calls)
    const neededPermissions: PermissionRequest[] = [];
    if (type === 'video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });

    // Define the callback that proceeds with the call after permissions are granted
    const proceedWithCall = (granted: boolean) => {
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
    };

    // Check if permissions have already been decided - skip modal if so
    checkCallPermissions(type).then((alreadyDecided) => {
      if (alreadyDecided) {
        // Permissions already granted or denied, directly proceed
        proceedWithCall(true);
      } else {
        // Show permission modal
        set({
          showPermissionModal: true,
          permissionRequests: neededPermissions,
          permissionCallback: proceedWithCall,
        });
      }
    });
  },

  answerCall: (callId, callerProfile, callType) => {
    const incoming = get().incomingCall;
    if (!incoming) return;

    // Build permission requests list (no 'location' - not needed for calls)
    const neededPermissions: PermissionRequest[] = [];
    if (callType === 'video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });

    // Define the callback that proceeds with answering after permissions are granted
    const proceedWithAnswer = (granted: boolean) => {
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
    };

    // Check if permissions have already been decided - skip modal if so
    checkCallPermissions(callType).then((alreadyDecided) => {
      if (alreadyDecided) {
        // Permissions already granted or denied, directly proceed
        proceedWithAnswer(true);
      } else {
        // Show permission modal
        set({
          showPermissionModal: true,
          permissionRequests: neededPermissions,
          permissionCallback: proceedWithAnswer,
        });
      }
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

    // Always send endCallSignal FIRST to ensure RTDB data is cleaned up, even if WebRTC throws
    if (activeCall?.callId) {
      try {
        endCallSignal(activeCall.callId);
      } catch (e) {
        // Signal might fail if call was already ended
        console.warn('[Zixo] endCallSignal failed:', e);
      }
    }

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

  // ==================== GROUP CALL ACTIONS ====================

  startGroupCall: (type, participants) => {
    const currentUser = get().currentUser;
    if (!currentUser) return;

    // Build permission requests list
    const neededPermissions: PermissionRequest[] = [];
    if (type === 'group-video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for group video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for group calls' });

    // Show permission modal, then proceed with call on grant
    set({
      showPermissionModal: true,
      permissionRequests: neededPermissions,
      permissionCallback: (granted: boolean) => {
        if (!granted) return;

        const state = useZixoStore.getState();
        const currentUser = state.currentUser;
        if (!currentUser) return;

        // Reset any previous group WebRTC instance
        resetGroupWebRTC();
        const groupWebrtc = getGroupWebRTC();

        // Set up callbacks
        groupWebrtc.onRemoteStream = (uid, stream) => {
          const s = useZixoStore.getState();
          if (s.groupCall) {
            const existing = s.groupCall.participants.find(p => p.uid === uid);
            if (existing) {
              useZixoStore.setState({
                groupCall: {
                  ...s.groupCall,
                  participants: s.groupCall.participants.map(p =>
                    p.uid === uid ? { ...p, stream } : p
                  ),
                },
              });
            }
          }
        };

        groupWebrtc.onRemoteStreamRemoved = (uid) => {
          useZixoStore.getState().removeGroupCallParticipant(uid);
        };

        groupWebrtc.onParticipantJoined = (uid, name) => {
          const s = useZixoStore.getState();
          if (s.groupCall) {
            const existing = s.groupCall.participants.find(p => p.uid === uid);
            if (!existing) {
              useZixoStore.setState({
                groupCall: {
                  ...s.groupCall,
                  participants: [...s.groupCall.participants, {
                    uid,
                    displayName: name,
                    avatar: '',
                    stream: null,
                    isMuted: false,
                    isVideoOn: s.groupCall.type === 'group-video',
                  }],
                },
              });
            }
          }
        };

        groupWebrtc.onParticipantLeft = (uid) => {
          useZixoStore.getState().removeGroupCallParticipant(uid);
        };

        groupWebrtc.onConnectionStateChange = (uid, connState) => {
          if (connState === 'connected') {
            const s = useZixoStore.getState();
            if (s.groupCall && s.groupCall.status !== 'active') {
              useZixoStore.setState({
                groupCall: { ...s.groupCall, status: 'active', startedAt: Date.now() },
              });
            }
          }
        };

        // Set initial UI state
        const participantUids = participants.map(p => p.uid);
        useZixoStore.setState({
          groupCall: {
            callId: null,
            status: 'ringing',
            type,
            participants: participants.map(p => ({
              uid: p.uid,
              displayName: p.displayName,
              avatar: p.avatar || '',
              stream: null,
              isMuted: false,
              isVideoOn: type === 'group-video',
            })),
            localStream: null,
            isMuted: false,
            isSpeakerOn: type === 'group-audio',
            isVideoOn: type === 'group-video',
            isIncoming: false,
            startedAt: null,
          },
          currentScreen: type === 'group-audio' ? 'group-audio-call' : 'group-video-call',
        });

        // Initiate group WebRTC call asynchronously
        groupWebrtc.startGroupCall(currentUser.uid, currentUser.displayName, participantUids, type)
          .then((callId) => {
            const localStream = groupWebrtc.getLocalStream();
            useZixoStore.setState((s: any) => ({
              groupCall: s.groupCall
                ? { ...s.groupCall, callId, localStream, status: 'connecting' }
                : null,
            }));
          })
          .catch((err: any) => {
            console.error('[Zixo] Failed to start group call:', err);
            try { groupWebrtc.leaveGroupCall(); } catch {}
            useZixoStore.setState({ groupCall: null, currentScreen: 'home' });
            if (err?.name === 'NotAllowedError') {
              alert('Camera/Microphone permission denied. Please allow access in your browser settings.');
            } else {
              alert('Failed to start group call. Please try again.');
            }
          });
      },
    });
  },

  answerGroupCall: (callId) => {
    const incoming = get().incomingGroupCall;
    if (!incoming) return;

    const currentUser = get().currentUser;
    if (!currentUser) return;

    // Build permission requests list
    const neededPermissions: PermissionRequest[] = [];
    if (incoming.callType === 'group-video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for group video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for group calls' });

    set({
      showPermissionModal: true,
      permissionRequests: neededPermissions,
      permissionCallback: (granted: boolean) => {
        if (!granted) {
          useZixoStore.setState({ incomingGroupCall: null });
          return;
        }

        const currentIncoming = useZixoStore.getState().incomingGroupCall;
        if (!currentIncoming) return;

        // Reset any previous group WebRTC instance
        resetGroupWebRTC();
        const groupWebrtc = getGroupWebRTC();

        // Set up callbacks
        groupWebrtc.onRemoteStream = (uid, stream) => {
          const s = useZixoStore.getState();
          if (s.groupCall) {
            useZixoStore.setState({
              groupCall: {
                ...s.groupCall,
                participants: s.groupCall.participants.map(p =>
                  p.uid === uid ? { ...p, stream } : p
                ),
              },
            });
          }
        };

        groupWebrtc.onRemoteStreamRemoved = (uid) => {
          useZixoStore.getState().removeGroupCallParticipant(uid);
        };

        groupWebrtc.onParticipantJoined = (uid, name) => {
          const s = useZixoStore.getState();
          if (s.groupCall) {
            const existing = s.groupCall.participants.find(p => p.uid === uid);
            if (!existing) {
              useZixoStore.setState({
                groupCall: {
                  ...s.groupCall,
                  participants: [...s.groupCall.participants, {
                    uid,
                    displayName: name,
                    avatar: '',
                    stream: null,
                    isMuted: false,
                    isVideoOn: s.groupCall.type === 'group-video',
                  }],
                },
              });
            }
          }
        };

        groupWebrtc.onParticipantLeft = (uid) => {
          useZixoStore.getState().removeGroupCallParticipant(uid);
        };

        groupWebrtc.onConnectionStateChange = (uid, connState) => {
          if (connState === 'connected') {
            const s = useZixoStore.getState();
            if (s.groupCall && s.groupCall.status !== 'active') {
              useZixoStore.setState({
                groupCall: { ...s.groupCall, status: 'active', startedAt: Date.now() },
              });
            }
          }
        };

        // Build participant list from callData
        const callData = currentIncoming.callData;
        const participantList = Object.values(callData.participants || {})
          .filter(p => p.joinedAt > 0 && p.uid !== currentUser.uid)
          .map(p => ({
            uid: p.uid,
            displayName: p.name || p.uid,
            avatar: '',
            stream: null,
            isMuted: false,
            isVideoOn: currentIncoming.callType === 'group-video',
          }));

        // Set initial UI state
        useZixoStore.setState({
          groupCall: {
            callId: currentIncoming.callId,
            status: 'connecting',
            type: currentIncoming.callType,
            participants: participantList,
            localStream: null,
            isMuted: false,
            isSpeakerOn: currentIncoming.callType === 'group-audio',
            isVideoOn: currentIncoming.callType === 'group-video',
            isIncoming: true,
            startedAt: null,
          },
          incomingGroupCall: null,
          currentScreen: currentIncoming.callType === 'group-audio' ? 'group-audio-call' : 'group-video-call',
        });

        // Join the group call
        groupWebrtc.joinGroupCall(currentIncoming.callId, currentUser.uid, currentUser.displayName)
          .then(() => {
            const localStream = groupWebrtc.getLocalStream();
            useZixoStore.setState((s: any) => ({
              groupCall: s.groupCall
                ? { ...s.groupCall, localStream, status: 'connecting', startedAt: Date.now() }
                : null,
            }));
          })
          .catch((err: any) => {
            console.error('[Zixo] Failed to join group call:', err);
            try { groupWebrtc.leaveGroupCall(); } catch {}
            useZixoStore.setState({ groupCall: null, currentScreen: 'home' });
            if (err?.name === 'NotAllowedError') {
              alert('Camera/Microphone permission denied.');
            } else {
              alert('Failed to join group call. Please try again.');
            }
          });
      },
    });
  },

  rejectGroupCall: () => {
    const incoming = get().incomingGroupCall;
    if (incoming) {
      // Signal that we rejected
      leaveGroupCallSignal(incoming.callId, incoming.callerId).catch(() => {});
    }
    set({ incomingGroupCall: null, currentScreen: 'home' });
  },

  leaveGroupCall: () => {
    const { groupCall, currentUser } = get();

    // End group WebRTC connection
    try {
      getGroupWebRTC().leaveGroupCall();
    } catch (e) {
      // WebRTC might not be initialized
    }

    if (groupCall) {
      // Save call record
      const actualDuration = groupCall.startedAt
        ? Math.floor((Date.now() - groupCall.startedAt) / 1000)
        : 0;

      // Import and save group call record
      import('@/services/firestore').then(({ saveGroupCallRecord }) => {
        saveGroupCallRecord({
          callerId: groupCall.isIncoming ? (groupCall.participants[0]?.uid || '') : (currentUser?.uid || ''),
          callerName: groupCall.isIncoming ? (groupCall.participants[0]?.displayName || '') : (currentUser?.displayName || ''),
          type: groupCall.type,
          participantUids: groupCall.participants.map(p => p.uid),
          duration: actualDuration,
        }).catch(console.error);
      });

      // Add to local call history
      const newCall: CallRecord = {
        id: groupCall.callId || `gcall-${Date.now()}`,
        callerId: groupCall.isIncoming ? (groupCall.participants[0]?.uid || '') : (currentUser?.uid || ''),
        callerName: groupCall.isIncoming ? (groupCall.participants[0]?.displayName || '') : (currentUser?.displayName || ''),
        callerAvatar: '',
        receiverId: currentUser?.uid || '',
        receiverName: '',
        receiverAvatar: '',
        type: groupCall.type === 'group-audio' ? 'audio' : 'video',
        direction: groupCall.isIncoming ? 'incoming' : 'outgoing',
        duration: actualDuration,
        timestamp: Date.now(),
      };

      set((state) => ({
        callHistory: [newCall, ...state.callHistory],
        groupCall: null,
        incomingGroupCall: null,
        currentScreen: 'home',
      }));
    } else {
      set({ groupCall: null, incomingGroupCall: null, currentScreen: 'home' });
    }
  },

  addGroupCallParticipant: (uid, displayName, stream) =>
    set((state) => {
      if (!state.groupCall) return {};
      const existing = state.groupCall.participants.find(p => p.uid === uid);
      if (existing) {
        return {
          groupCall: {
            ...state.groupCall,
            participants: state.groupCall.participants.map(p =>
              p.uid === uid ? { ...p, stream } : p
            ),
          },
        };
      }
      return {
        groupCall: {
          ...state.groupCall,
          participants: [...state.groupCall.participants, { uid, displayName, avatar: '', stream, isMuted: false, isVideoOn: state.groupCall.type === 'group-video' }],
        },
      };
    }),

  removeGroupCallParticipant: (uid) =>
    set((state) => {
      if (!state.groupCall) return {};
      return {
        groupCall: {
          ...state.groupCall,
          participants: state.groupCall.participants.filter(p => p.uid !== uid),
        },
      };
    }),

  setGroupCallLocalStream: (stream) =>
    set((state) => ({
      groupCall: state.groupCall
        ? { ...state.groupCall, localStream: stream }
        : null,
    })),

  toggleGroupCallMute: () => {
    const { groupCall } = get();
    if (groupCall) {
      const newMuted = !groupCall.isMuted;
      try { getGroupWebRTC().toggleMute(newMuted); } catch {}
      set({ groupCall: { ...groupCall, isMuted: newMuted } });
    }
  },

  toggleGroupCallVideo: () => {
    const { groupCall } = get();
    if (groupCall) {
      const newVideoOn = !groupCall.isVideoOn;
      try { getGroupWebRTC().toggleVideo(newVideoOn); } catch {}
      set({ groupCall: { ...groupCall, isVideoOn: newVideoOn } });
    }
  },

  toggleGroupCallSpeaker: () =>
    set((state) => ({
      groupCall: state.groupCall
        ? { ...state.groupCall, isSpeakerOn: !state.groupCall.isSpeakerOn }
        : null,
    })),

  setIncomingGroupCall: (incoming) =>
    set({ incomingGroupCall: incoming }),

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
  // Also updates participantProfiles in all chats
  updateUserProfile: (updates) =>
    set((state) => {
      if (!state.currentUser) return {};
      const updatedUser = { ...state.currentUser, ...updates };
      // Also update in all chats where this user is a participant
      const updatedChats = state.chats.map(chat => ({
        ...chat,
        participantProfiles: chat.participantProfiles.map(p =>
          p.uid === updatedUser.uid ? { ...p, ...updates } : p
        ),
      }));
      return {
        currentUser: updatedUser,
        userProfiles: { ...state.userProfiles, [updatedUser.uid]: updatedUser },
        chats: updatedChats,
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

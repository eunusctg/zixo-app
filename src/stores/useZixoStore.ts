import { create } from 'zustand';
import type { ZixoUserProfile } from '@/services/auth';
import type { FirestoreChat, FirestoreMessage, FirestoreCall } from '@/services/firestore';
import type { UploadProgress } from '@/services/storage';
import { getWebRTC, resetWebRTC } from '@/services/webrtc';
import { getGroupWebRTC, resetGroupWebRTC } from '@/services/webrtc-group';
import { endCallSignal, type RTDBCallSignal, leaveGroupCallSignal, endGroupCallSignal, type RTDBGroupCallSignal } from '@/services/presence';
import { playOutgoingRingSound, stopOutgoingRingSound, stopRingingSound } from '@/services/messaging';

// ==================== CALL RATE LIMITING ====================

/**
 * Rate limit for call attempts — prevents rapid repeated call attempts.
 * Tracks the timestamp of the last call attempt per user.
 */
const CALL_RATE_LIMIT_MS = 5000; // 5 seconds between call attempts
const lastCallAttemptAt: Record<string, number> = {};

function checkCallRateLimit(userId: string): boolean {
  const now = Date.now();
  const lastAttempt = lastCallAttemptAt[userId];
  if (lastAttempt && now - lastAttempt < CALL_RATE_LIMIT_MS) {
    return false; // Rate limited
  }
  lastCallAttemptAt[userId] = now;
  return true; // Allowed
}

// ==================== CALL TIMEOUT ====================

/**
 * Auto-end a call if it stays in 'ringing' or 'connecting' state for too long.
 * This prevents the user from being stuck on a "Calling..." screen forever
 * when the remote party never answers or the WebRTC connection never establishes.
 */
const CALL_RINGING_TIMEOUT_MS = 45000; // 45 seconds
let callRingingTimeout: ReturnType<typeof setTimeout> | null = null;

function startCallRingingTimeout(): void {
  clearCallRingingTimeout();
  callRingingTimeout = setTimeout(() => {
    const state = useZixoStore.getState();
    if (state.activeCall && (state.activeCall.status === 'ringing' || state.activeCall.status === 'connecting')) {
      console.warn('[Zixo] Call ringing/connecting timeout (45s) — auto-ending call');
      state.endCall();
    }
  }, CALL_RINGING_TIMEOUT_MS);
}

function clearCallRingingTimeout(): void {
  if (callRingingTimeout) {
    clearTimeout(callRingingTimeout);
    callRingingTimeout = null;
  }
}

// ==================== PERMISSION HELPERS ====================

/**
 * Query the actual browser permission status for a given permission name.
 * Returns 'granted' | 'denied' | 'prompt' | 'unsupported'.
 * Falls back gracefully on browsers that don't support the Permissions API
 * for mic/camera (e.g. Firefox).
 */
async function queryBrowserPermission(
  name: 'microphone' | 'camera' | 'geolocation'
): Promise<'granted' | 'denied' | 'prompt' | 'unsupported'> {
  try {
    if (typeof navigator === 'undefined' || !navigator.permissions || !navigator.permissions.query) {
      return 'unsupported';
    }
    const result = await navigator.permissions.query({ name: name as PermissionName });
    return result.state as 'granted' | 'denied' | 'prompt';
  } catch {
    // Firefox doesn't support 'microphone'/'camera' as PermissionName
    // Some browsers throw for certain permission names
    return 'unsupported';
  }
}

/**
 * Check if call permissions have already been granted and are still valid.
 *
 * Strategy:
 *  1. Always verify actual browser permission status via navigator.permissions API
 *  2. If the browser says a previously-cached permission is now revoked (prompt/denied),
 *     invalidate the cache and re-ask the user.
 *  3. If the Permissions API is unsupported (Firefox), fall back to the cache — but
 *     invalidate it if the user hasn't been asked in the current session.
 *  4. Never permanently suppress the permission modal. The "one device, one time"
 *     guarantee comes from the browser itself: once granted, navigator.permissions
 *     returns 'granted' and we skip the modal. If the user revokes the permission
 *     in browser settings, navigator.permissions returns 'prompt' or 'denied', and
 *     we re-ask.
 *
 * Returns true if all required permissions are already granted (skip modal).
 * Returns false if at least one required permission needs user action (show modal).
 */
async function checkCallPermissions(type: 'audio' | 'video'): Promise<boolean> {
  if (typeof window === 'undefined') return false;

  // --- Step 1: Check actual browser permission state via Permissions API ---
  const micStatus = await queryBrowserPermission('microphone');
  const camStatus = type === 'video'
    ? await queryBrowserPermission('camera')
    : 'unsupported' as const; // Don't need camera for audio-only calls

  const isMicGranted = micStatus === 'granted';
  const isCamGranted = camStatus === 'granted';
  const isMicDenied = micStatus === 'denied';
  const isCamDenied = camStatus === 'denied';

  // --- Step 2: If the Permissions API gave us definitive answers, use them ---
  // At least one permission API responded with a real state (not all 'unsupported')
  const anyPermissionSupported = micStatus !== 'unsupported' || (type === 'video' && camStatus !== 'unsupported');

  if (anyPermissionSupported) {
    // Update cache to reflect actual state
    const cachedMic = isMicGranted ? true : isMicDenied ? false : undefined;
    const cachedCam = type === 'video'
      ? (isCamGranted ? true : isCamDenied ? false : undefined)
      : undefined;

    // Read existing cache to preserve values for permissions we didn't check
    let existingCache: Record<string, boolean | undefined> = {};
    try {
      const raw = localStorage.getItem('zixo_call_permissions');
      if (raw) existingCache = JSON.parse(raw);
    } catch {}

    // Write updated cache
    const newCache = {
      micGranted: cachedMic !== undefined ? cachedMic : existingCache.micGranted,
      camGranted: cachedCam !== undefined ? cachedCam : existingCache.camGranted,
    };
    localStorage.setItem('zixo_call_permissions', JSON.stringify(newCache));

    // For audio calls: skip modal only if mic is granted
    if (type === 'audio') {
      if (isMicGranted) return true;   // Mic is granted → skip modal
      if (isMicDenied) return true;     // Mic is permanently denied → skip modal (call will fail gracefully with a message)
      return false;                     // Mic is 'prompt' → show modal
    }

    // For video calls: need both mic and camera granted
    if (type === 'video') {
      // Both granted → skip modal
      if (isMicGranted && isCamGranted) return true;
      // Either one is permanently denied → skip modal (call will fail gracefully)
      if (isMicDenied || isCamDenied) return true;
      // At least one is 'prompt' → show modal so user can grant it
      return false;
    }
  }

  // --- Step 3: Permissions API unsupported (Firefox) — use localStorage cache ---
  // On Firefox, we can't programmatically check mic/camera status.
  // We rely on the cache, but add a session-level flag so we re-ask
  // at least once per browser session if the user hasn't explicitly granted.
  try {
    const permCache = localStorage.getItem('zixo_call_permissions');
    if (permCache) {
      const cached = JSON.parse(permCache);

      // Check if we've already verified permissions this browser session
      const sessionVerified = sessionStorage.getItem('zixo_permissions_verified');

      if (sessionVerified === 'true') {
        // Already verified this session — trust the cache
        if (cached.micGranted) {
          if (type === 'audio') return true;
          if (type === 'video' && cached.camGranted) return true;
          if (type === 'video') return true; // mic granted, camera may or may not be
        }
        if (cached.micGranted === false && cached.camGranted === false) {
          return true; // Both denied previously, skip modal
        }
      } else {
        // Haven't verified this session — try a lightweight getUserMedia probe
        // to confirm the cache is still valid. If it fails, invalidate cache.
        try {
          const probeStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
          probeStream.getTracks().forEach(t => t.stop());
          // Mic is still granted — update cache
          localStorage.setItem('zixo_call_permissions', JSON.stringify({ micGranted: true, camGranted: cached.camGranted || false }));
          sessionStorage.setItem('zixo_permissions_verified', 'true');
          if (type === 'audio') return true;
          // For video, also probe camera if needed
          if (type === 'video' && !cached.camGranted) {
            try {
              const camStream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true });
              camStream.getTracks().forEach(t => t.stop());
              localStorage.setItem('zixo_call_permissions', JSON.stringify({ micGranted: true, camGranted: true }));
              return true;
            } catch {
              // Camera not granted, but mic is — skip modal, video call will be audio-only
              return true;
            }
          }
          return true;
        } catch {
          // Mic access failed — cache is stale, clear it and show modal
          localStorage.removeItem('zixo_call_permissions');
          sessionStorage.removeItem('zixo_permissions_verified');
          return false;
        }
      }
    }
  } catch {
    // Ignore cache errors
  }

  return false; // No cached permission or cache stale → show modal
}

/**
 * Set up permission change listeners to detect when the user revokes
 * mic/camera/location permissions in browser settings. When a revocation
 * is detected, the localStorage cache is cleared so the next call will
 * re-prompt the user.
 */
export function initPermissionChangeListeners(): () => void {
  if (typeof window === 'undefined') return () => {};

  const cleanups: Array<() => void> = [];

  const watchPermission = async (name: 'microphone' | 'camera' | 'geolocation') => {
    try {
      const status = await navigator.permissions.query({ name: name as PermissionName });
      const handler = () => {
        if (status.state === 'prompt' || status.state === 'denied') {
          // Permission was revoked — clear the cache so we re-ask next time
          try {
            const raw = localStorage.getItem('zixo_call_permissions');
            if (raw) {
              const cached = JSON.parse(raw);
              if (name === 'microphone') cached.micGranted = false;
              if (name === 'camera') cached.camGranted = false;
              localStorage.setItem('zixo_call_permissions', JSON.stringify(cached));
            }
          } catch {}
          // Also clear session verification
          try { sessionStorage.removeItem('zixo_permissions_verified'); } catch {}
        } else if (status.state === 'granted') {
          // Permission was granted — update cache
          try {
            const raw = localStorage.getItem('zixo_call_permissions');
            const cached = raw ? JSON.parse(raw) : {};
            if (name === 'microphone') cached.micGranted = true;
            if (name === 'camera') cached.camGranted = true;
            localStorage.setItem('zixo_call_permissions', JSON.stringify(cached));
          } catch {}
          try { sessionStorage.setItem('zixo_permissions_verified', 'true'); } catch {}
        }
      };
      status.addEventListener('change', handler);
      cleanups.push(() => status.removeEventListener('change', handler));
    } catch {
      // Permission not supported on this browser
    }
  };

  watchPermission('microphone');
  watchPermission('camera');
  watchPermission('geolocation');

  return () => cleanups.forEach(fn => fn());
}

/**
 * Save call permission grants to localStorage cache.
 * Call this after user grants microphone/camera permissions.
 */
export function saveCallPermissionCache(micGranted: boolean, camGranted: boolean): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('zixo_call_permissions', JSON.stringify({ micGranted, camGranted }));
    // Mark permissions as verified for this session
    try { sessionStorage.setItem('zixo_permissions_verified', 'true'); } catch {}
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
  _profileUpdatedAt: number; // timestamp of last local profile edit (used to prevent stale Firestore overwrites)

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
  _profileUpdatedAt: 0,

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
    set((state) => ({
      isAuthenticated: true,
      currentUser: user,
      // Only navigate to 'home' on first login (from auth/onboarding).
      // If user is already on a screen (chat, settings, etc.), stay there.
      currentScreen: state.isAuthenticated ? state.currentScreen : 'home',
      userProfiles: { ...state.userProfiles, [user.uid]: user },
    })),

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
    set((state) => {
      // Stop outgoing ring sound when call transitions from ringing/connecting to connected or ended
      if (status === 'connected' || status === 'ended') {
        stopOutgoingRingSound();
        // Clear the ringing timeout — the call has progressed past ringing
        clearCallRingingTimeout();
      }
      return {
        activeCall: state.activeCall ? { ...state.activeCall, status, startedAt: status === 'connected' && !state.activeCall.startedAt ? Date.now() : state.activeCall.startedAt } : null,
      };
    }),

  startCall: (type, user) => {
    const currentUser = get().currentUser;
    if (!currentUser) return;

    // Rate limit check — prevent rapid repeated call attempts
    if (!checkCallRateLimit(currentUser.uid)) {
      console.warn('[Zixo] Call rate limited — too many attempts');
      return;
    }

    // Guard: Prevent starting a new call if already in an active call or incoming call
    const currentActiveCall = get().activeCall;
    const currentIncomingCall = get().incomingCall;
    if (currentActiveCall || currentIncomingCall) {
      console.warn('[Zixo] Cannot start a new call — already in an active or incoming call');
      return;
    }

    // Build permission requests list (no 'location' - not needed for calls)
    const neededPermissions: PermissionRequest[] = [];
    if (type === 'video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });

    // Define the callback that proceeds with the call after permissions are granted
    const proceedWithCall = async (granted: boolean) => {
      if (!granted) return;

      const state = useZixoStore.getState();
      const currentUser = state.currentUser;
      if (!currentUser) return;

      // Double-check: don't start if already in a call (may have changed during permission dialog)
      if (state.activeCall || state.incomingCall) {
        console.warn('[Zixo] Cannot proceed with call — already in an active or incoming call');
        return;
      }

      // Reset any previous WebRTC instance
      await resetWebRTC();
      const webrtc = getWebRTC();

      // Set up callbacks
      webrtc.onRemoteStream = (stream) => {
        useZixoStore.getState().setCallRemoteStream(stream);
        useZixoStore.getState().setCallStatus('connected');
      };

      webrtc.onConnectionStateChange = (connState) => {
        console.log('[Zixo] WebRTC connection state:', connState);
        // Only end call on 'failed' state. 'disconnected' can be temporary
        // during ICE renegotiation and the connection may recover.
        // The subscribeToCallStatus listener handles detecting when the
        // remote party actually ends the call.
        if (connState === 'failed') {
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

      // Play outgoing ring sound (what the caller hears while waiting)
      playOutgoingRingSound();

      // Start ringing timeout — auto-end if still ringing after 45s
      startCallRingingTimeout();

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
          stopOutgoingRingSound();
          useZixoStore.setState({ activeCall: null, currentScreen: 'home' });
          if (err?.name === 'NotAllowedError') {
            // Permission was revoked after our pre-check — invalidate cache
            // so next call will show the permission modal again
            try { localStorage.removeItem('zixo_call_permissions'); } catch {}
            try { sessionStorage.removeItem('zixo_permissions_verified'); } catch {}
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

    // Save callData in the closure so it's always available even if
    // incomingCall is cleared during the permission dialog
    const savedCallData = incoming.callData;
    const savedCallId = incoming.callId;

    // Build permission requests list (no 'location' - not needed for calls)
    const neededPermissions: PermissionRequest[] = [];
    if (callType === 'video') {
      neededPermissions.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    neededPermissions.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });

    // Define the callback that proceeds with answering after permissions are granted
    const proceedWithAnswer = async (granted: boolean) => {
      if (!granted) {
        // Reject the incoming call if permissions denied
        try { endCallSignal(savedCallId); } catch {}
        useZixoStore.setState({ incomingCall: null, activeCall: null });
        return;
      }

      // Use saved callData if incomingCall was cleared during permission dialog
      const currentIncoming = useZixoStore.getState().incomingCall;
      const callData = currentIncoming?.callData || savedCallData;

      if (!currentIncoming && !savedCallData) {
        console.warn('[Zixo] Cannot answer call — no call data available');
        return;
      }

      // Reset any previous WebRTC instance
      await resetWebRTC();
      const webrtc = getWebRTC();

      // Stop incoming ringing sound
      stopRingingSound();

      // Set up callbacks
      webrtc.onRemoteStream = (stream) => {
        useZixoStore.getState().setCallRemoteStream(stream);
        useZixoStore.getState().setCallStatus('connected');
      };

      webrtc.onConnectionStateChange = (connState) => {
        console.log('[Zixo] WebRTC connection state (answer):', connState);
        // Only end call on 'failed' state. 'disconnected' can be temporary
        // during ICE renegotiation and the connection may recover.
        if (connState === 'failed') {
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

      // Answer the call via WebRTC using saved callData as fallback
      try {
        webrtc.answerCall(callId, callData)
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
            // Signal rejection to the caller using saved callId
            try { endCallSignal(savedCallId); } catch {}
            useZixoStore.setState({ activeCall: null, incomingCall: null, currentScreen: 'home' });
            if (err?.name === 'NotAllowedError') {
              try { localStorage.removeItem('zixo_call_permissions'); } catch {}
              try { sessionStorage.removeItem('zixo_permissions_verified'); } catch {}
              alert('Camera/Microphone permission denied. Please allow access in your browser settings.');
            } else {
              const errorMsg = err?.message || 'Unknown error';
              alert(`Failed to answer call: ${errorMsg}. Please try again.`);
            }
          });
      } catch (err: any) {
        console.error('[Zixo] Exception while answering call:', err);
        try { webrtc.endCall(); } catch {}
        try { endCallSignal(savedCallId); } catch {}
        useZixoStore.setState({ activeCall: null, incomingCall: null, currentScreen: 'home' });
        alert('Failed to answer call. Please try again.');
      }
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
    const currentUser = get().currentUser;

    // Stop incoming ringing sound
    stopRingingSound();

    if (incoming) {
      // End the call signal so the caller knows it was rejected
      endCallSignal(incoming.callId);

      // Record as missed call (receiver rejected / declined)
      if (currentUser) {
        const missedCall: CallRecord = {
          id: incoming.callId || `call-${Date.now()}`,
          callerId: incoming.callerProfile.uid,
          callerName: incoming.callerProfile.displayName,
          callerAvatar: incoming.callerProfile.avatar || '',
          receiverId: currentUser.uid,
          receiverName: currentUser.displayName,
          receiverAvatar: currentUser.avatar || '',
          type: incoming.callType,
          direction: 'missed',
          duration: 0,
          timestamp: Date.now(),
        };

        set((state) => ({
          callHistory: [missedCall, ...state.callHistory],
          incomingCall: null,
          activeCall: null,
          currentScreen: 'home',
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

        return;
      }
    }
    set({ incomingCall: null, activeCall: null, currentScreen: 'home' });
  },

  endCall: () => {
    const { activeCall, currentUser, incomingCall } = get();

    // Guard: if there's no active call and no incoming call, nothing to do
    if (!activeCall && !incomingCall) return;

    // Stop outgoing ring sound (in case the call was still in ringing/connecting)
    stopOutgoingRingSound();

    // Clear ringing timeout
    clearCallRingingTimeout();

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

      // Show 'ended' status briefly before dismissing, so user sees "Call ended" feedback
      set((state) => ({
        activeCall: state.activeCall ? { ...state.activeCall, status: 'ended' as const } : null,
        incomingCall: null,
      }));

      // Dismiss after a brief delay
      setTimeout(() => {
        const currentState = useZixoStore.getState();
        // Only dismiss if still in ended state (not re-entered a new call)
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
    set((state) => {
      if (incoming === null && state.currentScreen === 'incoming-call') {
        return { incomingCall: null, currentScreen: 'home' };
      }
      return { incomingCall: incoming };
    }),

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

    // Define the callback that proceeds with the group call after permissions are granted
    const proceedWithGroupCall = (granted: boolean) => {
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
            try { localStorage.removeItem('zixo_call_permissions'); } catch {}
            try { sessionStorage.removeItem('zixo_permissions_verified'); } catch {}
            alert('Camera/Microphone permission denied. Please allow access in your browser settings.');
          } else {
            alert('Failed to start group call. Please try again.');
          }
        });
    };

    // Check if permissions have already been decided - skip modal if so
    const callType = type === 'group-video' ? 'video' : 'audio';
    checkCallPermissions(callType).then((alreadyDecided) => {
      if (alreadyDecided) {
        proceedWithGroupCall(true);
      } else {
        set({
          showPermissionModal: true,
          permissionRequests: neededPermissions,
          permissionCallback: proceedWithGroupCall,
        });
      }
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

    // Define the callback that proceeds with answering after permissions are granted
    const proceedWithGroupAnswer = (granted: boolean) => {
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
            try { localStorage.removeItem('zixo_call_permissions'); } catch {}
            try { sessionStorage.removeItem('zixo_permissions_verified'); } catch {}
            alert('Camera/Microphone permission denied.');
          } else {
            alert('Failed to join group call. Please try again.');
          }
        });
    };

    // Check if permissions have already been decided - skip modal if so
    const callType = incoming.callType === 'group-video' ? 'video' : 'audio';
    checkCallPermissions(callType).then((alreadyDecided) => {
      if (alreadyDecided) {
        proceedWithGroupAnswer(true);
      } else {
        set({
          showPermissionModal: true,
          permissionRequests: neededPermissions,
          permissionCallback: proceedWithGroupAnswer,
        });
      }
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

    // Guard: if there's no group call, nothing to do
    if (!groupCall) {
      set({ groupCall: null, incomingGroupCall: null, currentScreen: 'home' });
      return;
    }

    // Signal departure to other participants via RTDB
    if (groupCall.callId && currentUser) {
      try {
        if (!groupCall.isIncoming) {
          // Creator is leaving — end the entire call for everyone
          console.log('[Zixo] Group call creator leaving, ending call for all');
          endGroupCallSignal(groupCall.callId);
        } else {
          // Participant is leaving — just remove ourselves
          console.log('[Zixo] Group call participant leaving');
          leaveGroupCallSignal(groupCall.callId, currentUser.uid).catch(() => {});
        }
      } catch (e) {
        console.warn('[Zixo] Group call leave signal failed:', e);
      }
    }

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
        _profileUpdatedAt: Date.now(),
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

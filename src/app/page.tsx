'use client';

import React, { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useZixoStore } from '@/stores/useZixoStore';
import { useFirebaseBridge } from '@/hooks/useFirebaseBridge';
import { logoutUser } from '@/services/auth';
import { saveCallPermissionCache, initPermissionChangeListeners } from '@/stores/useZixoStore';
import { sendMessage as firestoreSendMessage, markChatRead as firestoreMarkChatRead, searchMessages as firestoreSearchMessages, createOrGetChat } from '@/services/firestore';
import { searchUserByUsername, searchUsers, getAllUsers, searchUserByZixoNumber } from '@/services/auth';
import { cn, formatDateGroup } from '@/lib/zixo-utils';
import SplashScreen, { OnboardingScreen, AuthScreen } from '@/components/zixo/Onboarding';
import Avatar from '@/components/zixo/Avatar';
import { ChatList } from '@/components/zixo/ChatList';
import { MessageBubble, DateSeparator, ChatInputBar, MessageSearchBar, ScrollToBottomFAB, RecordingOverlay } from '@/components/zixo/ChatScreen';
import { AudioCallScreen, VideoCallScreen, IncomingCallScreen, PermissionModal } from '@/components/zixo/CallScreens';
import { GroupAudioCallScreen, GroupVideoCallScreen, IncomingGroupCallScreen } from '@/components/zixo/GroupCallScreens';
import { CallHistoryList, ContactsScreen } from '@/components/zixo/CallHistory';
import SettingsScreen from '@/components/zixo/SettingsScreen';
import ProfileEditScreen from '@/components/zixo/ProfileEditScreen';
import AdminPanel from '@/components/zixo/AdminPanel';
import { BottomNav, FAB, SearchBar, CallsDialFAB } from '@/components/zixo/Navigation';
import { OnlineStatus, EncryptionBadge } from '@/components/zixo/Common';
import ErrorBoundary from '@/components/zixo/ErrorBoundary';
import NotificationBanner from '@/components/zixo/NotificationBanner';
import PWAInstallPrompt from '@/components/zixo/PWAInstallPrompt';
import type { ZixoUserProfile } from '@/services/auth';

export default function ZixoApp() {
  // Initialize Firebase bridge (auth state, real-time listeners, notification banners)
  const { bannerNotifications, onDismissBanner, onTapBanner } = useFirebaseBridge();

  // Set up permission change listeners to detect when user revokes mic/camera/location
  // in browser settings. When revoked, the cache is invalidated so we re-ask next call.
  useEffect(() => {
    const cleanup = initPermissionChangeListeners();
    return cleanup;
  }, []);

  const {
    currentScreen,
    previousScreen,
    activeTab,
    isAuthenticated,
    isFirebaseReady,
    currentUser,
    chats,
    activeChatId,
    messages,
    callHistory,
    activeCall,
    incomingCall,
    searchQuery,
    isSearching,
    showFABMenu,
    setScreen,
    setActiveTab,
    goBack,
    login,
    logout,
    setActiveChat,
    setChats,
    setMessages,
    addMessage,
    startCall,
    answerCall,
    rejectCall,
    endCall,
    toggleMute,
    toggleSpeaker,
    toggleVideo,
    groupCall,
    incomingGroupCall,
    startGroupCall,
    answerGroupCall,
    rejectGroupCall,
    leaveGroupCall,
    toggleGroupCallMute,
    toggleGroupCallVideo,
    toggleGroupCallSpeaker,
    setSearchQuery,
    toggleSearching,
    toggleFABMenu,
    markChatRead,
    showPermissionModal,
    permissionRequests,
    permissionCallback,
  } = useZixoStore();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const [showMessageSearch, setShowMessageSearch] = useState(false);
  const [isScrolledUp, setIsScrolledUp] = useState(false);

  // Permission modal state
  const [permCurrentIndex, setPermCurrentIndex] = useState(0);
  const [permRequests, setPermRequests] = useState<any[]>([]);

  // When permission modal opens, initialize the request tracking
  useEffect(() => {
    if (showPermissionModal && permissionRequests.length > 0) {
      setPermRequests(permissionRequests);
      setPermCurrentIndex(0);
    }
  }, [showPermissionModal, permissionRequests]);

  const handlePermissionAllow = useCallback(async () => {
    if (permCurrentIndex >= permRequests.length) return;

    const currentReq = permRequests[permCurrentIndex];
    let granted = false;

    try {
      if (currentReq.type === 'camera' || currentReq.type === 'microphone') {
        const constraints: MediaStreamConstraints = {
          audio: currentReq.type === 'microphone' ? true : false,
          video: currentReq.type === 'camera' ? { width: 720, height: 1280, facingMode: 'user' } : false,
        };
        const stream = await navigator.mediaDevices.getUserMedia(constraints);
        stream.getTracks().forEach(track => track.stop());
        granted = true;
      } else if (currentReq.type === 'location') {
        granted = await new Promise<boolean>((resolve) => {
          if (!navigator.geolocation) { resolve(false); return; }
          navigator.geolocation.getCurrentPosition(
            () => resolve(true),
            () => resolve(false),
            { enableHighAccuracy: true, timeout: 10000 }
          );
        });
      }
    } catch (err: any) {
      if (err?.name === 'NotAllowedError') {
        granted = false;
      } else {
        granted = false;
      }
    }

    // Update the request status
    const updatedRequests = [...permRequests];
    updatedRequests[permCurrentIndex] = { ...currentReq, status: granted ? 'granted' : 'denied' };
    setPermRequests(updatedRequests);

    // Move to next permission or complete
    const nextIndex = permCurrentIndex + 1;
    if (nextIndex >= updatedRequests.length) {
      // All permissions processed - check if mic was granted (minimum required)
      const micGranted = updatedRequests.find(r => r.type === 'microphone')?.status === 'granted';
      const camGranted = updatedRequests.find(r => r.type === 'camera')?.status === 'granted';
      // Cache the permission state so we don't ask again next time
      saveCallPermissionCache(micGranted || false, camGranted || false);
      setTimeout(() => {
        useZixoStore.setState({ showPermissionModal: false });
        permissionCallback?.(micGranted || false);
      }, 500);
    } else {
      setPermCurrentIndex(nextIndex);
    }
  }, [permCurrentIndex, permRequests, permissionCallback]);

  const handlePermissionSkip = useCallback(() => {
    if (permCurrentIndex >= permRequests.length) return;

    const updatedRequests = [...permRequests];
    updatedRequests[permCurrentIndex] = { ...permRequests[permCurrentIndex], status: 'denied' };
    setPermRequests(updatedRequests);

    const nextIndex = permCurrentIndex + 1;
    if (nextIndex >= updatedRequests.length) {
      const micGranted = updatedRequests.find(r => r.type === 'microphone')?.status === 'granted';
      const camGranted = updatedRequests.find(r => r.type === 'camera')?.status === 'granted';
      saveCallPermissionCache(micGranted || false, camGranted || false);
      setTimeout(() => {
        useZixoStore.setState({ showPermissionModal: false });
        permissionCallback?.(micGranted || false);
      }, 300);
    } else {
      setPermCurrentIndex(nextIndex);
    }
  }, [permCurrentIndex, permRequests, permissionCallback]);

  const handlePermissionCancel = useCallback(() => {
    useZixoStore.setState({ showPermissionModal: false });
    permissionCallback?.(false);
  }, [permissionCallback]);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (currentScreen === 'chat' && messagesEndRef.current && !isScrolledUp) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, activeChatId, currentScreen, isScrolledUp]);

  // Get active chat data
  const activeChat = useMemo(() => {
    if (!activeChatId) return null;
    return chats.find((c) => c.id === activeChatId) || null;
  }, [activeChatId, chats]);

  const activeChatMessages = useMemo(() => {
    if (!activeChatId) return [];
    return messages[activeChatId] || [];
  }, [activeChatId, messages]);

  const otherUser = useMemo(() => {
    if (!activeChat || !currentUser) return null;
    return activeChat.participantProfiles.find((p) => p.uid !== currentUser.uid) || null;
  }, [activeChat, currentUser]);

  const totalUnread = useMemo(() => {
    return chats.reduce((sum, c) => sum + c.unreadCount, 0);
  }, [chats]);

  // Handle chat click
  const handleChatClick = useCallback((chatId: string) => {
    setActiveChat(chatId);
    markChatRead(chatId);
    // Also mark as read in Firestore
    if (currentUser) {
      firestoreMarkChatRead(chatId, currentUser.uid).catch(console.error);
    }
  }, [setActiveChat, markChatRead, currentUser]);

  // Handle sending a message with media support (tries Firestore, falls back to local)
  const handleSendMessage = useCallback((text: string, type: 'text' | 'image' | 'voice' | 'file' | 'location' = 'text', extras?: { mediaUrl?: string; fileName?: string; fileSize?: number; duration?: number }) => {
    if (!activeChatId || !currentUser) return;

    // Add optimistic local message
    const tempMsg = {
      id: `temp-${Date.now()}`,
      chatId: activeChatId,
      senderId: currentUser.uid,
      text,
      type,
      timestamp: Date.now(),
      status: 'sending' as const,
      ...extras,
    };
    addMessage(activeChatId, tempMsg);

    // Try to send via Firestore
    firestoreSendMessage(activeChatId, currentUser.uid, text, type, extras).then(() => {
      // Update status to sent
      const store = useZixoStore.getState();
      store.updateMessage(activeChatId, tempMsg.id, { status: 'sent' });

      // Simulate delivered after a moment
      setTimeout(() => {
        useZixoStore.getState().updateMessage(activeChatId, tempMsg.id, { status: 'delivered' });
      }, 1000);
    }).catch((err) => {
      console.warn('Firestore send failed, keeping message local:', err);
      // Update to show it's still local
      const store = useZixoStore.getState();
      store.updateMessage(activeChatId, tempMsg.id, { status: 'sent' });
    });
  }, [activeChatId, currentUser, addMessage]);

  // Handle quick call
  const handleQuickCall = useCallback((userId: string) => {
    const user = chats
      .flatMap((c) => c.participantProfiles)
      .find((p) => p.uid === userId);
    if (user) {
      startCall('audio', user);
    }
  }, [chats, startCall]);

  // Handle callback from call history
  const handleCallBack = useCallback((call: any) => {
    const user: ZixoUserProfile = {
      uid: call.callerId === currentUser?.uid ? call.receiverId : call.callerId,
      displayName: call.callerId === currentUser?.uid ? call.receiverName : call.callerName,
      username: '',
      email: '',
      bio: '',
      avatar: '',
      online: true,
      lastSeen: Date.now(),
      createdAt: Date.now(),
      role: 'user',
      zixoNumber: '',
    };
    startCall(call.type, user);
  }, [currentUser, startCall]);

  // Handle file upload from ChatInputBar
  const handleFileUpload = useCallback((file: File, type: 'image' | 'file', downloadUrl?: string) => {
    if (type === 'image') {
      handleSendMessage('📷 Photo', 'image', downloadUrl ? { mediaUrl: downloadUrl } : undefined);
    } else {
      handleSendMessage(`📄 ${file.name}`, 'file', { fileName: file.name, fileSize: file.size, ...(downloadUrl ? { mediaUrl: downloadUrl } : {}) });
    }
  }, [handleSendMessage]);

  // Handle voice recording with MediaRecorder API
  const [isRecording, setIsRecording] = useState(false);
  const [recordingDuration, setRecordingDuration] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const autoStopTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clean up recording timers on unmount
  useEffect(() => {
    return () => {
      if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
      if (autoStopTimeoutRef.current) clearTimeout(autoStopTimeoutRef.current);
    };
  }, []);

  const handleVoiceRecord = useCallback(async () => {
    if (isRecording) {
      // Stop recording — request final data first, then stop
      if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
        try {
          mediaRecorderRef.current.requestData();
        } catch {}
        mediaRecorderRef.current.stop();
      }
      if (recordingTimerRef.current) {
        clearInterval(recordingTimerRef.current);
        recordingTimerRef.current = null;
      }
      if (autoStopTimeoutRef.current) {
        clearTimeout(autoStopTimeoutRef.current);
        autoStopTimeoutRef.current = null;
      }
      setIsRecording(false);
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      // Determine supported MIME type with fallback for Safari/iOS
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : MediaRecorder.isTypeSupported('audio/webm')
          ? 'audio/webm'
          : MediaRecorder.isTypeSupported('audio/mp4')
            ? 'audio/mp4'
            : 'audio/ogg';
      const fileExt = mimeType.startsWith('audio/mp4') ? 'm4a' : mimeType.startsWith('audio/ogg') ? 'ogg' : 'webm';
      const mediaRecorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      const startTime = Date.now();

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = async () => {
        // Stop all audio tracks
        stream.getTracks().forEach(track => track.stop());

        // Request any remaining data that hasn't been delivered yet
        // (Some browsers buffer data until stop — collect it here)

        const audioBlob = new Blob(audioChunksRef.current, { type: mimeType });
        const duration = Math.floor((Date.now() - startTime) / 1000);

        if (audioBlob.size > 0 && duration > 0) {
          // Upload voice note to storage
          try {
            const { uploadChatMedia } = await import('@/services/storage');
            const file = new File([audioBlob], `voice_${Date.now()}.${fileExt}`, { type: mimeType });
            const uploadResult = await uploadChatMedia(activeChatId || '', currentUser?.uid || '', file, 'voice');
            if (uploadResult?.downloadUrl) {
              handleSendMessage('🎤 Voice note', 'voice', { mediaUrl: uploadResult.downloadUrl, duration });
            } else {
              handleSendMessage('🎤 Voice note', 'voice', { duration });
            }
          } catch (err) {
            console.error('[Zixo] Voice upload failed:', err);
            // Fallback: send as voice message without media URL
            handleSendMessage('🎤 Voice note', 'voice', { duration });
          }
        } else if (audioBlob.size > 0) {
          // Duration is 0 but we have data — send anyway with minimum duration
          handleSendMessage('🎤 Voice note', 'voice', { duration: 1 });
        }

        setRecordingDuration(0);
        audioChunksRef.current = [];
        mediaRecorderRef.current = null;
      };

      mediaRecorder.start(1000); // Request data every 1 second for more reliable recording
      setIsRecording(true);
      setRecordingDuration(0);

      // Update duration timer
      recordingTimerRef.current = setInterval(() => {
        setRecordingDuration(prev => prev + 1);
      }, 1000);

      // Auto-stop after 5 minutes
      autoStopTimeoutRef.current = setTimeout(() => {
        if (mediaRecorderRef.current?.state === 'recording') {
          try { mediaRecorderRef.current.requestData(); } catch {}
          mediaRecorderRef.current.stop();
          if (recordingTimerRef.current) {
            clearInterval(recordingTimerRef.current);
            recordingTimerRef.current = null;
          }
          setIsRecording(false);
        }
        autoStopTimeoutRef.current = null;
      }, 300000);

    } catch (err: any) {
      console.error('[Zixo] Voice recording failed:', err);
      if (err?.name === 'NotAllowedError') {
        alert('Microphone permission denied. Please allow microphone access in your browser settings.');
      } else {
        alert('Failed to start voice recording. Please try again.');
      }
    }
  }, [isRecording, activeChatId, currentUser, handleSendMessage]);

  // Handle message search within active chat
  const handleMessageSearch = useCallback(async (query: string) => {
    if (!activeChatId) return;
    try {
      const store = useZixoStore.getState();
      store.searchMessages(query);
      const results = await firestoreSearchMessages(activeChatId, query);
      const searchResults = results.map((msg) => ({
        chatId: msg.chatId,
        messageId: msg.id,
        text: msg.text,
        senderId: msg.senderId,
        timestamp: msg.timestamp?.toMillis?.() || Date.now(),
      }));
      // Store results in state for display
      useZixoStore.setState({ messageSearchResults: searchResults, isSearchingMessages: false });
    } catch (error) {
      console.warn('[Zixo] Message search failed:', error);
      useZixoStore.setState({ isSearchingMessages: false });
    }
  }, [activeChatId]);

  // Handle scroll to detect when user is scrolled up
  const handleMessageScroll = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return;
    const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
    setIsScrolledUp(distanceFromBottom > 150);
  }, []);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    setIsScrolledUp(false);
  }, []);

  // Handle logout with Firebase
  const handleLogout = useCallback(async () => {
    try {
      await logoutUser();
    } catch (err) {
      console.error('Firebase logout error:', err);
    }
    logout(); // Clear local state regardless
  }, [logout]);

  // Handle answering incoming call
  const handleAnswerCall = useCallback(() => {
    // Get fresh state from store (not stale closure)
    const currentIncoming = useZixoStore.getState().incomingCall;
    if (currentIncoming) {
      answerCall(currentIncoming.callId, currentIncoming.callerProfile, currentIncoming.callType);
    }
  }, [answerCall]);

  // Group call participant picker state
  const [showGroupCallPicker, setShowGroupCallPicker] = useState(false);
  const [groupCallType, setGroupCallType] = useState<'group-audio' | 'group-video'>('group-audio');
  const [selectedParticipants, setSelectedParticipants] = useState<Set<string>>(new Set());

  const handleStartGroupCall = useCallback((type: 'group-audio' | 'group-video') => {
    setGroupCallType(type);
    setSelectedParticipants(new Set());
    setShowGroupCallPicker(true);
    toggleFABMenu();
  }, [toggleFABMenu]);

  const handleConfirmGroupCall = useCallback(() => {
    if (!currentUser || selectedParticipants.size === 0) return;

    const participants = chats
      .flatMap((c) => c.participantProfiles)
      .filter((p) => selectedParticipants.has(p.uid) && p.uid !== currentUser.uid);

    // Deduplicate by uid
    const uniqueParticipants = Array.from(
      new Map(participants.map((p) => [p.uid, p])).values()
    );

    if (uniqueParticipants.length > 0) {
      startGroupCall(groupCallType, uniqueParticipants);
    }
    setShowGroupCallPicker(false);
    setSelectedParticipants(new Set());
  }, [currentUser, selectedParticipants, chats, groupCallType, startGroupCall]);

  const toggleParticipantSelection = useCallback((uid: string) => {
    setSelectedParticipants((prev) => {
      const next = new Set(prev);
      if (next.has(uid)) next.delete(uid);
      else if (next.size < 5) next.add(uid); // Max 5 other participants
      return next;
    });
  }, []);

  // Render screens
  const renderScreen = () => {
    switch (currentScreen) {
      case 'splash':
        return <SplashScreen onComplete={() => setScreen('onboarding')} />;

      case 'onboarding':
        return (
          <OnboardingScreen
            onComplete={() => setScreen('auth')}
            onSignIn={() => setScreen('auth')}
            onSignUp={() => setScreen('auth')}
          />
        );

      case 'auth':
        return (
          <AuthScreen
            mode="login"
            onAuth={(data) => {
              // The Firebase bridge (useFirebaseBridge) handles auth state changes
              // via onAuthStateChanged, but there can be a race condition where
              // the bridge hasn't called login() yet when the user clicks Continue
              // on the Zixo Number screen. This callback ensures we navigate to home.
              if (isAuthenticated && currentUser) {
                setScreen('home');
              } else {
                // Bridge hasn't fired yet — check again shortly
                const checkInterval = setInterval(() => {
                  const state = useZixoStore.getState();
                  if (state.isAuthenticated && state.currentUser) {
                    state.setScreen('home');
                    clearInterval(checkInterval);
                  }
                }, 200);
                // Safety: stop checking after 10 seconds
                setTimeout(() => clearInterval(checkInterval), 10000);
              }
            }}
            onSwitchMode={() => {}}
            onBack={() => setScreen('onboarding')}
          />
        );

      case 'incoming-call':
        // If incomingCall is null but screen is still 'incoming-call', navigate home
        // IMMEDIATELY to prevent the black screen. Using direct setState (not setTimeout)
        // avoids the one-frame black flash that setTimeout(fn, 0) causes.
        if (!incomingCall) {
          useZixoStore.setState({ currentScreen: 'home' });
          return null;
        }
        return (
          <IncomingCallScreen
            remoteUser={incomingCall.callerProfile}
            callType={incomingCall.callType}
            onAnswer={handleAnswerCall}
            onDecline={rejectCall}
          />
        );

      case 'audio-call':
      case 'video-call':
        // If activeCall is null but screen is still on a call screen, navigate home
        // IMMEDIATELY to prevent the black screen. Using direct setState (not setTimeout)
        // avoids the one-frame black flash that setTimeout(fn, 0) causes.
        if (!activeCall?.remoteUser) {
          useZixoStore.setState({ currentScreen: 'home' });
          return null;
        }
        if (currentScreen === 'audio-call') {
          return (
            <AudioCallScreen
              remoteUser={activeCall.remoteUser}
              callStatus={activeCall.status as 'ringing' | 'connecting' | 'connected' | 'ended'}
              duration={activeCall.duration}
              isMuted={activeCall.isMuted}
              isSpeakerOn={activeCall.isSpeakerOn}
              onToggleMute={toggleMute}
              onToggleSpeaker={toggleSpeaker}
              onEndCall={endCall}
              remoteStream={activeCall.remoteStream}
            />
          );
        }
        return (
          <VideoCallScreen
            remoteUser={activeCall.remoteUser}
            callStatus={activeCall.status as 'ringing' | 'connecting' | 'connected' | 'ended'}
            duration={activeCall.duration}
            isMuted={activeCall.isMuted}
            isVideoOn={activeCall.isVideoOn}
            onToggleMute={toggleMute}
            onToggleVideo={toggleVideo}
            onFlipCamera={() => {
              try {
                import('@/services/webrtc').then(({ getWebRTC }) => getWebRTC().switchCamera());
              } catch {}
            }}
            onEndCall={endCall}
            localStream={activeCall.localStream}
            remoteStream={activeCall.remoteStream}
          />
        );

      case 'incoming-group-call':
        if (!incomingGroupCall) return null;
        return (
          <IncomingGroupCallScreen
            callerName={incomingGroupCall.callerName}
            callType={incomingGroupCall.callType}
            participantNames={incomingGroupCall.participantNames}
            onAnswer={() => answerGroupCall(incomingGroupCall.callId)}
            onDecline={rejectGroupCall}
          />
        );

      case 'group-audio-call':
        if (!groupCall) return null;
        return (
          <GroupAudioCallScreen
            participants={groupCall.participants}
            status={groupCall.status}
            isMuted={groupCall.isMuted}
            isSpeakerOn={groupCall.isSpeakerOn}
            startedAt={groupCall.startedAt}
            onToggleMute={toggleGroupCallMute}
            onToggleSpeaker={toggleGroupCallSpeaker}
            onLeaveCall={leaveGroupCall}
          />
        );

      case 'group-video-call':
        if (!groupCall) return null;
        return (
          <GroupVideoCallScreen
            participants={groupCall.participants}
            localStream={groupCall.localStream}
            status={groupCall.status}
            isMuted={groupCall.isMuted}
            isVideoOn={groupCall.isVideoOn}
            startedAt={groupCall.startedAt}
            onToggleMute={toggleGroupCallMute}
            onToggleVideo={toggleGroupCallVideo}
            onFlipCamera={() => {
              try {
                import('@/services/webrtc-group').then(({ getGroupWebRTC }) => getGroupWebRTC().switchCamera());
              } catch {}
            }}
            onLeaveCall={leaveGroupCall}
          />
        );

      case 'chat':
        return renderChatScreen();

      case 'contacts':
        return <ErrorBoundary>{renderContactsScreen()}</ErrorBoundary>;

      case 'profile-edit':
        if (!currentUser) return null;
        return (
          <ProfileEditScreen
            user={currentUser}
            onBack={() => setScreen('settings')}
            onSave={async (updates) => {
              // Profile is already updated in Firestore and Zustand by the component
              console.log('[Zixo] Profile saved:', updates);
            }}
          />
        );

      case 'admin-panel':
        return renderAdminPanelScreen();

      case 'settings':
        return renderSettingsScreen();

      case 'home':
      default:
        return renderHomeScreen();
    }
  };

  const renderHomeScreen = () => {
    if (!isAuthenticated || !currentUser) return null;

    return (
      <div className="h-[100dvh] flex flex-col bg-zixo-bg">
        {/* Top Bar */}
        <div className="shrink-0 bg-[#1F2C34]">
          <div className="flex items-center justify-between px-4 py-3 safe-area-top">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl gradient-primary flex items-center justify-center">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M5 4H15L7 12H15L5 20H15" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
                  <circle cx="18" cy="6" r="2" fill="white" fillOpacity="0.8"/>
                  <path d="M19 10C20.1 10 21 10.9 21 12" stroke="white" strokeWidth="1.5" strokeLinecap="round" fill="none" fillOpacity="0.5"/>
                  <path d="M19 14C20.7 14 22 15.3 22 17" stroke="white" strokeWidth="1.5" strokeLinecap="round" fill="none" fillOpacity="0.35"/>
                </svg>
              </div>
              <h1 className="text-xl font-bold font-heading gradient-primary bg-clip-text text-transparent">Zixo</h1>
            </div>
            <div className="flex items-center gap-2">
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={toggleSearching}
                className="w-10 h-10 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-primary hover:bg-zixo-surface-light transition-colors"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
              </motion.button>
              <Avatar key={currentUser.avatar} name={currentUser.displayName} uid={currentUser.uid} avatarUrl={currentUser.avatar} size="sm" online={currentUser.online} />
            </div>
          </div>

          <SearchBar
            value={searchQuery}
            onChange={setSearchQuery}
            isExpanded={isSearching}
            onToggle={toggleSearching}
          />
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto pb-24">
          <AnimatePresence mode="wait">
            {activeTab === 'chats' && (
              <motion.div
                key="chats"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                transition={{ duration: 0.2 }}
              >
                <ChatList
                  chats={chats}
                  currentUserId={currentUser.uid}
                  onChatClick={handleChatClick}
                  onQuickCall={handleQuickCall}
                  searchQuery={searchQuery}
                />
              </motion.div>
            )}

            {activeTab === 'calls' && (
              <motion.div
                key="calls"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                transition={{ duration: 0.2 }}
                className="relative"
              >
                {/* Call History */}
                <CallHistoryList
                  calls={callHistory}
                  currentUserId={currentUser.uid}
                  onCallBack={handleCallBack}
                />

                {/* Separator between call history and contacts */}
                {callHistory.length > 0 && (
                  <div className="mx-4 my-3">
                    <div className="h-px bg-white/5" />
                  </div>
                )}

                {/* Contacts section inside Calls tab */}
                {renderContactsTabContent()}

                {/* 3D Dial FAB */}
                <CallsDialFAB
                  onNewCall={() => setScreen('contacts')}
                  onAddContact={() => setScreen('contacts')}
                />
              </motion.div>
            )}

            {activeTab === 'settings' && (
              <motion.div
                key="settings"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                transition={{ duration: 0.2 }}
              >
                <SettingsScreen
                  user={currentUser}
                  onEditProfile={() => setScreen('profile-edit')}
                  onLogout={handleLogout}
                  onBack={() => setActiveTab('chats')}
                  onAdminPanel={() => setScreen('admin-panel')}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* FAB */}
        {activeTab === 'chats' && (
          <FAB
            isOpen={showFABMenu}
            onToggle={toggleFABMenu}
            onNewChat={() => setScreen('contacts')}
            onNewGroup={() => handleStartGroupCall('group-audio')}
            onQuickCall={() => {
              if (chats.length > 0) {
                const firstChat = chats[0];
                const user = firstChat.participantProfiles.find((p) => p.uid !== currentUser.uid);
                if (user) startCall('audio', user);
              }
            }}
          />
        )}

        {/* Bottom Navigation */}
        <BottomNav
          activeTab={activeTab}
          onTabChange={setActiveTab}
          unreadCount={totalUnread}
        />
      </div>
    );
  };

  const renderChatScreen = () => {
    if (!currentUser) return null;

    // Fallback: if we have an activeChatId but no activeChat yet (just created),
    // show a loading/empty chat screen
    if (activeChatId && !activeChat) {
      return (
        <div className="h-[100dvh] flex flex-col bg-zixo-bg">
          <div className="shrink-0 bg-[#1F2C34] z-10 safe-area-top">
            <div className="flex items-center gap-3 px-3 py-2.5">
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={() => setScreen('home')}
                className="shrink-0 w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text hover:bg-zixo-surface-light transition-colors"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="15 18 9 12 15 6" />
                </svg>
              </motion.button>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-zixo-text">Chat</h3>
                <p className="text-xs text-zixo-text-secondary">Loading...</p>
              </div>
            </div>
          </div>
          <div className="flex-1 flex items-center justify-center">
            <div className="text-zixo-text-secondary text-sm">Starting chat...</div>
          </div>
          <ChatInputBar
            onSend={(text) => handleSendMessage(text)}
            onAttachment={(type) => {
              if (type === 'image') handleSendMessage('📷 Photo shared', 'image');
              else if (type === 'file') handleSendMessage('📄 Document shared', 'file');
              else if (type === 'location') handleSendMessage('📍 Location shared', 'location');
            }}
            onVoiceRecord={handleVoiceRecord}
            isRecording={isRecording}
            recordingDuration={recordingDuration}
            onFileUpload={handleFileUpload}
            chatId={activeChatId || ''}
          />
        </div>
      );
    }

    if (!activeChat || !otherUser) return null;

    // Group messages by date
    const groupedMessages: { date: string; messages: typeof activeChatMessages }[] = [];
    let currentDate = '';

    activeChatMessages.forEach((msg) => {
      const date = formatDateGroup(msg.timestamp);
      if (date !== currentDate) {
        currentDate = date;
        groupedMessages.push({ date, messages: [msg] });
      } else {
        groupedMessages[groupedMessages.length - 1].messages.push(msg);
      }
    });

    return (
      <div className="h-[100dvh] flex flex-col bg-zixo-bg">
        {/* Chat Header */}
        <div className="shrink-0 bg-[#1F2C34] z-10 safe-area-top">
          <div className="flex items-center gap-3 px-3 py-2.5">
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={() => setScreen('home')}
              className="shrink-0 w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text hover:bg-zixo-surface-light transition-colors"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </motion.button>

            <Avatar name={otherUser.displayName} uid={otherUser.uid} avatarUrl={otherUser.avatar} size="sm" online={otherUser.online} />

            <div className="flex-1 min-w-0">
              <h3 className="text-sm font-semibold text-zixo-text truncate">{otherUser.displayName}</h3>
              {activeChat.typing && activeChat.typing.length > 0 ? (
                <p className="text-xs text-zixo-primary">typing...</p>
              ) : (
                <OnlineStatus online={otherUser.online} lastSeen={otherUser.lastSeen} />
              )}
            </div>

            <div className="flex items-center gap-1 shrink-0">
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={() => setShowMessageSearch(!showMessageSearch)}
                className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-primary hover:bg-zixo-surface-light transition-colors"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
              </motion.button>
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={() => startCall('video', otherUser)}
                className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="23 7 16 12 23 17 23 7" />
                  <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                </svg>
              </motion.button>
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={() => startCall('audio', otherUser)}
                className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
              </motion.button>
            </div>
          </div>
        </div>

        {/* Message Search */}
        <AnimatePresence>
          {showMessageSearch && (
            <MessageSearchBar
              onSearch={handleMessageSearch}
              onClose={() => {
                setShowMessageSearch(false);
                useZixoStore.getState().clearMessageSearch();
              }}
              searchQuery={useZixoStore.getState().messageSearchQuery}
              isSearching={useZixoStore.getState().isSearchingMessages}
            />
          )}
        </AnimatePresence>

        {/* Recording Indicator */}
        <AnimatePresence>
          {isRecording && (
            <RecordingOverlay
              duration={recordingDuration}
              onStop={handleVoiceRecord}
            />
          )}
        </AnimatePresence>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto px-4 py-2 relative" ref={messagesContainerRef} onScroll={handleMessageScroll}>
          <div className="flex justify-center mb-4">
            <EncryptionBadge />
          </div>

          {groupedMessages.map((group) => (
            <div key={group.date}>
              <DateSeparator date={group.date} />
              {group.messages.map((msg, i) => {
                const prevMsg = i > 0 ? group.messages[i - 1] : null;
                const isConsecutive = prevMsg?.senderId === msg.senderId;

                return (
                  <MessageBubble
                    key={msg.id}
                    message={msg}
                    isOwn={msg.senderId === currentUser.uid}
                    showAvatar={!isConsecutive || (prevMsg?.senderId !== msg.senderId)}
                    senderName={msg.senderId !== currentUser.uid ? otherUser.displayName : undefined}
                    senderUid={msg.senderId !== currentUser.uid ? otherUser.uid : undefined}
                    isConsecutive={isConsecutive}
                  />
                );
              })}
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* Scroll to Bottom FAB */}
        <AnimatePresence>
          {isScrolledUp && (
            <ScrollToBottomFAB onClick={scrollToBottom} />
          )}
        </AnimatePresence>

        {/* Input Bar */}
        <ChatInputBar
          onSend={(text) => handleSendMessage(text)}
          onAttachment={(type) => {
            if (type === 'image') handleSendMessage('📷 Photo shared', 'image');
            else if (type === 'file') handleSendMessage('📄 Document shared', 'file');
            else if (type === 'location') handleSendMessage('📍 Location shared', 'location');
          }}
          onVoiceRecord={handleVoiceRecord}
          isRecording={isRecording}
          recordingDuration={recordingDuration}
          onFileUpload={handleFileUpload}
          chatId={activeChatId || ''}
        />
      </div>
    );
  };

  const [allUsers, setAllUsers] = useState<ZixoUserProfile[]>([]);
  const [usersLoaded, setUsersLoaded] = useState(false);
  const [usersLoading, setUsersLoading] = useState(false);

  // Load all users for the contacts screen when navigating to it
  useEffect(() => {
    if (currentScreen === 'contacts' && currentUser && !usersLoaded && !usersLoading) {
      setUsersLoading(true);
      getAllUsers(currentUser.uid).then((users) => {
        setAllUsers(users || []);
        setUsersLoaded(true);
        setUsersLoading(false);
      }).catch((err) => {
        console.error('[Zixo] Failed to load users:', err);
        setAllUsers([]);
        setUsersLoaded(true); // Mark as loaded even on error so we don't retry infinitely
        setUsersLoading(false);
      });
    }
    if (currentScreen !== 'contacts') {
      setUsersLoaded(false);
      setUsersLoading(false);
    }
  }, [currentScreen, currentUser, usersLoaded, usersLoading]);

  const renderContactsTabContent = () => {
    if (!currentUser) return null;
    const chatContacts = (chats || [])
      .map((c) => c?.participantProfiles?.find((p) => p?.uid !== currentUser?.uid))
      .filter(Boolean) as ZixoUserProfile[];

    return (
      <div>
        {/* Header with Find People button */}
        <div className="px-4 py-3">
          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={() => setScreen('contacts')}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-zixo-surface border border-zixo-primary/20 hover:border-zixo-primary/40 transition-colors"
          >
            <div className="w-10 h-10 rounded-full gradient-primary flex items-center justify-center">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                <line x1="11" y1="8" x2="11" y2="14" />
                <line x1="8" y1="11" x2="14" y2="11" />
              </svg>
            </div>
            <div className="flex-1 text-left">
              <p className="text-sm font-medium text-zixo-text">Find People</p>
              <p className="text-xs text-zixo-text-secondary">Search by name, Zixo number, or QR code</p>
            </div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-text-secondary">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </motion.button>
        </div>

        {/* Contacts List */}
        {chatContacts.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
            <div className="w-16 h-16 rounded-full bg-zixo-surface flex items-center justify-center mb-4">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="8.5" cy="7" r="4" />
              </svg>
            </div>
            <h3 className="text-base font-semibold text-zixo-text mb-1">No contacts yet</h3>
            <p className="text-sm text-zixo-text-secondary">Start a chat to add contacts</p>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {chatContacts.map((contact, i) => (
              <motion.div
                key={contact.uid}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: Math.min(i * 0.03, 0.3) }}
                className="flex items-center gap-3 px-4 py-3 hover:bg-zixo-surface/50 transition-colors cursor-pointer"
                onClick={() => contact?.uid && handleChatClick(chats.find(c => c.participants.includes(contact.uid))?.id || '')}
              >
                <Avatar name={contact.displayName} uid={contact.uid} avatarUrl={contact.avatar} size="lg" online={contact.online} />
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-medium text-zixo-text truncate">{contact.displayName}</h4>
                  <p className="text-xs text-zixo-text-secondary truncate">{contact.username || contact.bio || ''}</p>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={(e) => { e.stopPropagation(); startCall('audio', contact); }}
                    className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                    </svg>
                  </motion.button>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    );
  };

  const renderContactsScreen = () => {
    if (!currentUser) return null;
    try {
    const allContacts = (chats || [])
      .map((c) => c?.participantProfiles?.find((p) => p?.uid !== currentUser?.uid))
      .filter(Boolean) as ZixoUserProfile[];

    // Show loading state while users are being fetched
    if (usersLoading && allUsers.length === 0) {
      return (
        <div className="h-[100dvh] flex flex-col bg-zixo-bg">
          <div className="shrink-0 flex items-center gap-3 px-4 py-3 bg-[#1F2C34] safe-area-top">
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={() => setScreen('home')}
              className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </motion.button>
            <h2 className="text-lg font-semibold font-heading text-zixo-text">Find People</h2>
          </div>
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center gap-3">
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                className="w-8 h-8 border-2 border-zixo-primary/30 border-t-zixo-primary rounded-full"
              />
              <p className="text-zixo-text-secondary text-sm">Loading people...</p>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="h-[100dvh] flex flex-col bg-zixo-bg">
        <div className="shrink-0 flex items-center gap-3 px-4 py-3 bg-[#1F2C34] safe-area-top">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={() => setScreen('home')}
            className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </motion.button>
          <h2 className="text-lg font-semibold font-heading text-zixo-text">Find People</h2>
        </div>

        <div className="flex-1 overflow-y-auto">
          <ContactsScreen
            contacts={allContacts}
            allUsers={allUsers || []}
            currentUser={currentUser}
            onStartChat={async (userId) => {
              if (!userId || !currentUser) return;
              // Create or get chat in Firestore, then navigate
              try {
                const chatId = await createOrGetChat(currentUser.uid, userId);

                // Check if the chat is already in the store
                const existingChat = (chats || []).find((c) => c?.id === chatId);
                if (!existingChat) {
                  // Optimistically add the chat to the store so the chat screen can render
                  const otherUserProfile = (allUsers || []).find((u) => u?.uid === userId)
                    || (chats || []).flatMap((c) => c?.participantProfiles || []).find((p) => p?.uid === userId);
                  if (otherUserProfile) {
                    const optimisticChat = {
                      id: chatId,
                      participants: [currentUser.uid, userId],
                      participantProfiles: [currentUser, otherUserProfile],
                      lastMessage: undefined,
                      unreadCount: 0,
                      isGroup: false,
                      typing: [],
                      createdAt: Date.now(),
                      updatedAt: Date.now(),
                    };
                    // Add to chats array in store
                    useZixoStore.setState((state: any) => ({
                      chats: [optimisticChat, ...(state.chats || [])],
                    }));
                  }
                }

                handleChatClick(chatId);
              } catch (err) {
                console.error('[Zixo] Failed to create/get chat:', err);
                // Fallback: try finding existing chat in local state
                const chat = (chats || []).find((c) => c?.participants?.includes(userId));
                if (chat) handleChatClick(chat.id);
              }
            }}
            onStartCall={(userId, type) => {
              if (!userId) return;
              // Find user from all loaded profiles or create a minimal profile
              const user = (allUsers || []).find((c) => c?.uid === userId)
                || (chats || []).flatMap((c) => c?.participantProfiles || []).find((p) => p?.uid === userId);
              if (user) startCall(type, user);
            }}
            onSearchUsers={async (query: string) => {
              try {
                return await searchUsers(query);
              } catch (err) {
                console.error('[Zixo] User search failed:', err);
                return [];
              }
            }}
            onSearchByZixoNumber={async (zixoNumber: string) => {
              try {
                return await searchUserByZixoNumber(zixoNumber);
              } catch (err) {
                console.error('[Zixo] Zixo number search failed:', err);
                return null;
              }
            }}
          />
        </div>
      </div>
    );
    } catch (err) {
      console.error('[Zixo] Contacts screen render error:', err);
      return (
        <div className="h-[100dvh] flex flex-col bg-zixo-bg">
          <div className="shrink-0 flex items-center gap-3 px-4 py-3 bg-[#1F2C34]">
            <motion.button whileTap={{ scale: 0.9 }} onClick={() => setScreen('home')} className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="15 18 9 12 15 6" /></svg>
            </motion.button>
            <h2 className="text-lg font-semibold text-zixo-text">Find People</h2>
          </div>
          <div className="flex-1 flex items-center justify-center">
            <div className="text-center px-6">
              <p className="text-zixo-text-secondary text-sm mb-4">Failed to load contacts</p>
              <button onClick={() => setScreen('home')} className="px-4 py-2 rounded-lg gradient-primary text-white text-sm">Go Back</button>
            </div>
          </div>
        </div>
      );
    }
  };

  const renderAdminPanelScreen = () => {
    if (!currentUser || currentUser.role !== 'admin') return null;
    return (
      <AdminPanel
        currentUser={currentUser}
        onBack={() => setScreen('settings')}
      />
    );
  };

  const renderSettingsScreen = () => {
    if (!currentUser) return null;
    return (
      <div className="h-[100dvh] flex flex-col bg-zixo-bg">
        <div className="shrink-0 flex items-center gap-3 px-4 py-3 bg-[#1F2C34] safe-area-top">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={() => setScreen('home')}
            className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </motion.button>
          <h2 className="text-lg font-semibold font-heading text-zixo-text">Settings</h2>
        </div>
        <div className="flex-1 overflow-y-auto pb-20">
          <SettingsScreen user={currentUser} onEditProfile={() => setScreen('profile-edit')} onLogout={handleLogout} onBack={() => setScreen('home')} onAdminPanel={() => setScreen('admin-panel')} />
        </div>
      </div>
    );
  };

  return (
    <ErrorBoundary>
    <div className="min-h-[100dvh] bg-zixo-bg text-zixo-text flex justify-center">
      {/* Notification Banners */}
      <NotificationBanner
        notifications={bannerNotifications}
        onDismiss={onDismissBanner}
        onTap={onTapBanner}
      />

      {/* PWA Install Prompt & Offline Banner */}
      <PWAInstallPrompt />

      <div className="w-full max-w-lg relative page-transition-container">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentScreen}
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.98 }}
            transition={{ duration: 0.2, ease: [0.25, 0.46, 0.45, 0.94] }}
            className="will-change-transform"
          >
            <ErrorBoundary>
              {renderScreen()}
            </ErrorBoundary>
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Permission Modal for camera/mic/location before calls */}
      <PermissionModal
        isOpen={showPermissionModal && permRequests.length > 0}
        requests={permRequests}
        currentIndex={permCurrentIndex}
        onAllow={handlePermissionAllow}
        onSkip={handlePermissionSkip}
        onCancel={handlePermissionCancel}
      />

      {/* Group Call Participant Picker Modal */}
      <AnimatePresence>
        {showGroupCallPicker && currentUser && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[60] flex items-end justify-center"
            onClick={() => setShowGroupCallPicker(false)}
          >
            <div className="absolute inset-0 bg-black/50" />
            <motion.div
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className="relative w-full max-w-lg bg-[#1F2C34] rounded-t-3xl max-h-[80vh] flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="shrink-0 px-5 pt-5 pb-3 border-b border-white/5">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-white">
                    {groupCallType === 'group-video' ? '📹 Group Video Call' : '📞 Group Audio Call'}
                  </h3>
                  <button
                    onClick={() => setShowGroupCallPicker(false)}
                    className="w-8 h-8 rounded-full flex items-center justify-center text-white/60 hover:text-white hover:bg-white/10 transition-colors"
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <line x1="18" y1="6" x2="6" y2="18" />
                      <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                  </button>
                </div>
                <p className="text-xs text-white/40 mt-1">
                  Select up to 5 participants ({selectedParticipants.size}/5 selected)
                </p>

                {/* Call type toggle */}
                <div className="flex gap-2 mt-3">
                  <button
                    onClick={() => setGroupCallType('group-audio')}
                    className={cn(
                      'flex-1 py-2 rounded-xl text-sm font-medium transition-all',
                      groupCallType === 'group-audio'
                        ? 'bg-zixo-primary text-white'
                        : 'bg-white/5 text-white/50 hover:bg-white/10'
                    )}
                  >
                    📞 Audio
                  </button>
                  <button
                    onClick={() => setGroupCallType('group-video')}
                    className={cn(
                      'flex-1 py-2 rounded-xl text-sm font-medium transition-all',
                      groupCallType === 'group-video'
                        ? 'bg-zixo-secondary text-white'
                        : 'bg-white/5 text-white/50 hover:bg-white/10'
                    )}
                  >
                    📹 Video
                  </button>
                </div>
              </div>

              {/* Participant list */}
              <div className="flex-1 overflow-y-auto px-3 py-2 max-h-96">
                {chats
                  .flatMap((c) => c.participantProfiles)
                  .filter((p, i, arr) =>
                    p.uid !== currentUser.uid &&
                    arr.findIndex((x) => x.uid === p.uid) === i
                  )
                  .map((user) => (
                    <button
                      key={user.uid}
                      onClick={() => toggleParticipantSelection(user.uid)}
                      className={cn(
                        'w-full flex items-center gap-3 px-3 py-3 rounded-xl transition-all',
                        selectedParticipants.has(user.uid)
                          ? 'bg-zixo-primary/10 border border-zixo-primary/30'
                          : 'hover:bg-white/5'
                      )}
                    >
                      <Avatar name={user.displayName} uid={user.uid} avatarUrl={user.avatar} size="sm" online={user.online} />
                      <span className="flex-1 text-left text-sm text-white truncate">{user.displayName}</span>
                      {selectedParticipants.has(user.uid) && (
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      )}
                    </button>
                  ))}
                {chats.flatMap((c) => c.participantProfiles).filter((p) => p.uid !== currentUser.uid).length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-sm text-white/40">No contacts available</p>
                    <p className="text-xs text-white/25 mt-1">Start a chat first to see contacts</p>
                  </div>
                )}
              </div>

              {/* Start button */}
              <div className="shrink-0 px-5 py-4 border-t border-white/5">
                <button
                  onClick={handleConfirmGroupCall}
                  disabled={selectedParticipants.size === 0}
                  className={cn(
                    'w-full py-3 rounded-xl text-sm font-semibold transition-all',
                    selectedParticipants.size > 0
                      ? groupCallType === 'group-video'
                        ? 'bg-zixo-secondary text-white hover:opacity-90'
                        : 'bg-zixo-primary text-white hover:opacity-90'
                      : 'bg-white/5 text-white/30 cursor-not-allowed'
                  )}
                >
                  Start Group Call{selectedParticipants.size > 0 ? ` with ${selectedParticipants.size}` : ''}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
    </ErrorBoundary>
  );
}

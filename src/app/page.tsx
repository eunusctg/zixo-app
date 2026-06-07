'use client';

import React, { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useZixoStore } from '@/stores/useZixoStore';
import { useFirebaseBridge } from '@/hooks/useFirebaseBridge';
import { logoutUser } from '@/services/auth';
import { sendMessage as firestoreSendMessage, markChatRead as firestoreMarkChatRead, searchMessages as firestoreSearchMessages, createOrGetChat } from '@/services/firestore';
import { searchUserByUsername, searchUsers, getAllUsers, searchUserByZixoNumber } from '@/services/auth';
import { cn, formatDateGroup } from '@/lib/zixo-utils';
import SplashScreen, { OnboardingScreen, AuthScreen } from '@/components/zixo/Onboarding';
import Avatar from '@/components/zixo/Avatar';
import { ChatList } from '@/components/zixo/ChatList';
import { MessageBubble, DateSeparator, ChatInputBar, MessageSearchBar, ScrollToBottomFAB } from '@/components/zixo/ChatScreen';
import { AudioCallScreen, VideoCallScreen, PermissionModal } from '@/components/zixo/CallScreens';
import { GroupAudioCallScreen, GroupVideoCallScreen, IncomingGroupCallScreen } from '@/components/zixo/GroupCallScreens';
import { CallHistoryList, ContactsScreen } from '@/components/zixo/CallHistory';
import SettingsScreen from '@/components/zixo/SettingsScreen';
import ProfileEditScreen from '@/components/zixo/ProfileEditScreen';
import AdminPanel from '@/components/zixo/AdminPanel';
import { BottomNav, FAB, SearchBar } from '@/components/zixo/Navigation';
import { OnlineStatus, EncryptionBadge } from '@/components/zixo/Common';
import type { ZixoUserProfile } from '@/services/auth';

export default function ZixoApp() {
  // Initialize Firebase bridge (auth state, real-time listeners)
  useFirebaseBridge();

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
  const handleFileUpload = useCallback((file: File, type: 'image' | 'file') => {
    if (type === 'image') {
      handleSendMessage('📷 Photo', 'image');
    } else {
      handleSendMessage(`📄 ${file.name}`, 'file', { fileName: file.name, fileSize: file.size });
    }
  }, [handleSendMessage]);

  // Handle voice recording with MediaRecorder API
  const [isRecording, setIsRecording] = useState(false);
  const [recordingDuration, setRecordingDuration] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  const handleVoiceRecord = useCallback(async () => {
    if (isRecording) {
      // Stop recording
      if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
        mediaRecorderRef.current.stop();
      }
      if (recordingTimerRef.current) {
        clearInterval(recordingTimerRef.current);
      }
      setIsRecording(false);
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' });
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      let startTime = Date.now();

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = async () => {
        // Stop all audio tracks
        stream.getTracks().forEach(track => track.stop());

        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        const duration = Math.floor((Date.now() - startTime) / 1000);

        if (audioBlob.size > 0 && duration > 0) {
          // Upload voice note to storage
          try {
            const { uploadChatMedia } = await import('@/services/storage');
            const file = new File([audioBlob], `voice_${Date.now()}.webm`, { type: 'audio/webm' });
            const uploadResult = await uploadChatMedia(activeChatId || '', currentUser?.uid || '', file, 'voice');
            handleSendMessage('🎤 Voice note', 'voice', { mediaUrl: uploadResult.downloadUrl, duration });
          } catch (err) {
            console.error('[Zixo] Voice upload failed:', err);
            // Fallback: send as text
            handleSendMessage('🎤 Voice note', 'voice', { duration });
          }
        }

        setRecordingDuration(0);
      };

      mediaRecorder.start();
      setIsRecording(true);
      startTime = Date.now();
      setRecordingDuration(0);

      // Update duration timer
      recordingTimerRef.current = setInterval(() => {
        setRecordingDuration(prev => prev + 1);
      }, 1000);

      // Auto-stop after 5 minutes
      setTimeout(() => {
        if (mediaRecorderRef.current?.state === 'recording') {
          mediaRecorderRef.current.stop();
          if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
          setIsRecording(false);
        }
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
    if (incomingCall) {
      answerCall(incomingCall.callId, incomingCall.callerProfile, incomingCall.callType);
    }
  }, [incomingCall, answerCall]);

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
            onAuth={() => {}} // Auth handled by useFirebaseBridge
            onSwitchMode={() => {}}
            onBack={() => setScreen('onboarding')}
          />
        );

      case 'incoming-call':
        if (!incomingCall) return null;
        return (
          <AudioCallScreen
            remoteUser={incomingCall.callerProfile}
            callStatus="ringing"
            duration={0}
            isMuted={false}
            isSpeakerOn={true}
            onToggleMute={() => {}}
            onToggleSpeaker={() => {}}
            onEndCall={rejectCall}
            onAnswer={handleAnswerCall}
            onDecline={rejectCall}
            isIncoming={true}
            remoteStream={null}
          />
        );

      case 'audio-call':
      case 'video-call':
        if (!activeCall?.remoteUser) return null;
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
        return renderContactsScreen();

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
      <div className="h-screen flex flex-col bg-zixo-bg">
        {/* Top Bar */}
        <div className="shrink-0 bg-[#1F2C34]">
          <div className="flex items-center justify-between px-4 py-3 safe-area-top">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl gradient-primary flex items-center justify-center">
                <span className="text-lg font-extrabold text-white font-heading">Z</span>
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
              <Avatar name={currentUser.displayName} uid={currentUser.uid} size="sm" online={currentUser.online} />
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
        <div className="flex-1 overflow-y-auto pb-20">
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
              >
                <CallHistoryList
                  calls={callHistory}
                  currentUserId={currentUser.uid}
                  onCallBack={handleCallBack}
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
    if (!activeChat || !otherUser || !currentUser) return null;

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
      <div className="h-screen flex flex-col bg-zixo-bg">
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

            <Avatar name={otherUser.displayName} uid={otherUser.uid} size="sm" online={otherUser.online} />

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

  // Load all users for the contacts screen when navigating to it
  useEffect(() => {
    if (currentScreen === 'contacts' && currentUser && !usersLoaded) {
      getAllUsers(currentUser.uid).then((users) => {
        setAllUsers(users);
        setUsersLoaded(true);
      }).catch(console.error);
    }
    if (currentScreen !== 'contacts') {
      setUsersLoaded(false);
    }
  }, [currentScreen, currentUser, usersLoaded]);

  const renderContactsScreen = () => {
    if (!currentUser) return null;
    const allContacts = chats
      .map((c) => c.participantProfiles.find((p) => p.uid !== currentUser.uid))
      .filter(Boolean) as ZixoUserProfile[];

    return (
      <div className="h-screen flex flex-col bg-zixo-bg">
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
            allUsers={allUsers}
            onStartChat={async (userId) => {
              // Create or get chat in Firestore, then navigate
              try {
                const chatId = await createOrGetChat(currentUser.uid, userId);
                handleChatClick(chatId);
              } catch (err) {
                console.error('[Zixo] Failed to create/get chat:', err);
                // Fallback: try finding existing chat in local state
                const chat = chats.find((c) => c.participants.includes(userId));
                if (chat) handleChatClick(chat.id);
              }
            }}
            onStartCall={(userId, type) => {
              // Find user from all loaded profiles or create a minimal profile
              const user = allUsers.find((c) => c.uid === userId)
                || chats.flatMap((c) => c.participantProfiles).find((p) => p.uid === userId);
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
      <div className="h-screen flex flex-col bg-zixo-bg">
        <div className="shrink-0 flex items-center gap-3 px-4 py-3 bg-[#1F2C34] safe-area-top">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={() => setActiveTab('settings')}
            className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </motion.button>
          <h2 className="text-lg font-semibold font-heading text-zixo-text">Settings</h2>
        </div>
        <div className="flex-1 overflow-y-auto pb-20">
          <SettingsScreen user={currentUser} onEditProfile={() => setScreen('profile-edit')} onLogout={handleLogout} onBack={() => {}} onAdminPanel={() => setScreen('admin-panel')} />
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-zixo-bg text-zixo-text flex justify-center">
      <div className="w-full max-w-lg relative">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentScreen}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
          >
            {renderScreen()}
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
                      <Avatar name={user.displayName} uid={user.uid} size="sm" online={user.online} />
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
  );
}

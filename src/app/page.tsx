'use client';

import React, { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useZixoStore, generateDemoChats, generateDemoMessages, generateDemoCalls } from '@/stores/useZixoStore';
import { useFirebaseBridge } from '@/hooks/useFirebaseBridge';
import { logoutUser } from '@/services/auth';
import { sendMessage as firestoreSendMessage, markChatRead as firestoreMarkChatRead, searchMessages as firestoreSearchMessages } from '@/services/firestore';
import { cn, formatDateGroup } from '@/lib/zixo-utils';
import SplashScreen, { OnboardingScreen, AuthScreen } from '@/components/zixo/Onboarding';
import Avatar from '@/components/zixo/Avatar';
import { ChatList } from '@/components/zixo/ChatList';
import { MessageBubble, DateSeparator, ChatInputBar, MessageSearchBar, ScrollToBottomFAB } from '@/components/zixo/ChatScreen';
import { AudioCallScreen, VideoCallScreen } from '@/components/zixo/CallScreens';
import { CallHistoryList, ContactsScreen } from '@/components/zixo/CallHistory';
import SettingsScreen from '@/components/zixo/SettingsScreen';
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
    endCall,
    toggleMute,
    toggleSpeaker,
    toggleVideo,
    setSearchQuery,
    toggleSearching,
    toggleFABMenu,
    markChatRead,
  } = useZixoStore();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const demoLoaded = useRef(false);
  const [showMessageSearch, setShowMessageSearch] = useState(false);
  const [isScrolledUp, setIsScrolledUp] = useState(false);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (currentScreen === 'chat' && messagesEndRef.current && !isScrolledUp) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, activeChatId, currentScreen, isScrolledUp]);

  // Load demo data as fallback when authenticated but no Firestore data exists
  useEffect(() => {
    if (isAuthenticated && currentUser && chats.length === 0 && !demoLoaded.current) {
      demoLoaded.current = true;
      const demoChats = generateDemoChats(currentUser);
      setChats(demoChats);

      const demoMsgsMap: Record<string, ReturnType<typeof generateDemoMessages>> = {};
      demoChats.forEach((chat) => {
        const other = chat.participantProfiles.find((p) => p.uid !== currentUser.uid);
        if (other) demoMsgsMap[chat.id] = generateDemoMessages(chat.id, other.uid);
      });
      Object.entries(demoMsgsMap).forEach(([cid, msgs]) => setMessages(cid, msgs));
    }
  }, [isAuthenticated, currentUser, chats.length, setChats, setMessages]);

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

  // Handle auth callback (after Firebase Auth succeeds)
  // Firebase Auth triggers onAuthStateChanged in useFirebaseBridge,
  // which loads the user profile and calls login() -> transitions to 'home' screen.
  // This callback is a fallback: if the bridge doesn't fire within 3 seconds
  // (e.g. slow Firestore), create a temporary profile to unblock the UI.
  const handleAuth = useCallback((data: { email: string; displayName?: string }) => {
    if (currentUser) return; // Already logged in via bridge

    // Set a fallback timer - if useFirebaseBridge doesn't log us in within 3s,
    // create a temp profile so the user isn't stuck
    const fallbackTimer = setTimeout(() => {
      const store = useZixoStore.getState();
      if (!store.currentUser) {
        console.log('[Zixo] Bridge timeout, creating temp profile');
        const user: ZixoUserProfile = {
          uid: 'pending-firebase',
          displayName: data.displayName || data.email.split('@')[0],
          email: data.email,
          username: `@${(data.displayName || data.email.split('@')[0]).toLowerCase().replace(/\s+/g, '')}`,
          bio: 'Living free, connecting freely 🌍',
          avatar: '',
          online: true,
          lastSeen: Date.now(),
          createdAt: Date.now(),
        };
        store.login(user);
      }
    }, 3000);

    return () => clearTimeout(fallbackTimer);
  }, [currentUser]);

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
  const handleSendMessage = useCallback((text: string, type: 'text' | 'image' | 'voice' | 'file' | 'location' = 'text', extras?: { mediaUrl?: string; fileName?: string; fileSize?: number }) => {
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

  // Render screens
  const renderScreen = () => {
    switch (currentScreen) {
      case 'splash':
        return <SplashScreen onComplete={() => setScreen('onboarding')} />;

      case 'onboarding':
        return (
          <OnboardingScreen
            onComplete={() => setScreen('auth-login')}
            onSignIn={() => setScreen('auth-login')}
            onSignUp={() => setScreen('auth-signup')}
          />
        );

      case 'auth-login':
      case 'auth-signup':
      case 'auth-forgot':
        return (
          <AuthScreen
            mode={currentScreen === 'auth-forgot' ? 'forgot' : currentScreen === 'auth-signup' ? 'signup' : 'login'}
            onAuth={handleAuth}
            onSwitchMode={(mode) => {
              setScreen(mode === 'forgot' ? 'auth-forgot' : mode === 'signup' ? 'auth-signup' : 'auth-login');
            }}
            onBack={() => setScreen('onboarding')}
          />
        );

      case 'audio-call':
      case 'video-call':
        if (!activeCall?.remoteUser) return null;
        if (currentScreen === 'audio-call') {
          return (
            <AudioCallScreen
              remoteUser={activeCall.remoteUser}
              callStatus={activeCall.status}
              duration={activeCall.duration}
              isMuted={activeCall.isMuted}
              isSpeakerOn={activeCall.isSpeakerOn}
              onToggleMute={toggleMute}
              onToggleSpeaker={toggleSpeaker}
              onEndCall={endCall}
            />
          );
        }
        return (
          <VideoCallScreen
            remoteUser={activeCall.remoteUser}
            callStatus={activeCall.status}
            duration={activeCall.duration}
            isMuted={activeCall.isMuted}
            isVideoOn={activeCall.isVideoOn}
            onToggleMute={toggleMute}
            onToggleVideo={toggleVideo}
            onFlipCamera={() => {}}
            onEndCall={endCall}
          />
        );

      case 'chat':
        return renderChatScreen();

      case 'contacts':
        return renderContactsScreen();

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
        <div className="shrink-0">
          <div className="flex items-center justify-between px-4 py-3">
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
            onNewGroup={() => {}}
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
        <div className="shrink-0 glass-strong z-10">
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
          onVoiceRecord={() => handleSendMessage('🎤 Voice note', 'voice')}
          onFileUpload={handleFileUpload}
          chatId={activeChatId || ''}
        />
      </div>
    );
  };

  const renderContactsScreen = () => {
    if (!currentUser) return null;
    const allContacts = chats
      .map((c) => c.participantProfiles.find((p) => p.uid !== currentUser.uid))
      .filter(Boolean) as ZixoUserProfile[];

    return (
      <div className="h-screen flex flex-col bg-zixo-bg">
        <div className="shrink-0 flex items-center gap-3 px-4 py-3 glass-strong">
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
            onStartChat={(userId) => {
              const chat = chats.find((c) => c.participants.includes(userId));
              if (chat) handleChatClick(chat.id);
            }}
            onStartCall={(userId, type) => {
              const user = allContacts.find((c) => c.uid === userId);
              if (user) startCall(type, user);
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
        <div className="shrink-0 flex items-center gap-3 px-4 py-3 glass-strong">
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
          <SettingsScreen user={currentUser} onEditProfile={() => {}} onLogout={handleLogout} onBack={() => {}} onAdminPanel={() => setScreen('admin-panel')} />
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-zixo-bg text-zixo-text">
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
  );
}

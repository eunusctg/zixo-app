'use client';

import React, { useState, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn, formatMessageTime, formatDateGroup, getInitials, getAvatarColor } from '@/lib/zixo-utils';
import { useZixoStore, type Message } from '@/stores/useZixoStore';
import { setTypingIndicator } from '@/services/presence';
import { searchMessages as firestoreSearchMessages } from '@/services/firestore';
import { uploadChatMedia, type UploadProgress } from '@/services/storage';

interface MessageBubbleProps {
  message: Message;
  isOwn: boolean;
  showAvatar: boolean;
  senderName?: string;
  senderUid?: string;
  isConsecutive: boolean;
}

export function MessageBubble({
  message,
  isOwn,
  showAvatar,
  senderName,
  senderUid,
  isConsecutive,
}: MessageBubbleProps) {
  const [showActions, setShowActions] = useState(false);

  const statusIcon = () => {
    if (!isOwn) return null;
    switch (message.status) {
      case 'sending':
        return <span className="text-zixo-text-secondary/60 text-[10px]">✓</span>;
      case 'sent':
        return <span className="text-zixo-text-secondary text-[10px]">✓✓</span>;
      case 'delivered':
        return <span className="text-zixo-text-secondary text-[10px]">✓✓</span>;
      case 'read':
        return <span className="text-zixo-primary text-[10px]">✓✓</span>;
      default:
        return null;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 10, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.2 }}
      className={cn(
        'flex gap-2 message-in',
        isOwn ? 'justify-end' : 'justify-start',
        isConsecutive ? 'mt-0.5' : 'mt-3'
      )}
      onClick={() => setShowActions(!showActions)}
    >
      {/* Avatar for received messages */}
      {!isOwn && showAvatar && senderUid && (
        <div className={cn(
          'w-7 h-7 rounded-full bg-gradient-to-br flex items-center justify-center text-[9px] font-semibold text-white shrink-0 mt-auto',
          getAvatarColor(senderUid)
        )}>
          {senderName ? getInitials(senderName) : '?'}
        </div>
      )}
      {!isOwn && !showAvatar && <div className="w-7" />}

      {/* Bubble */}
      <div className={cn('max-w-[75%] group', isOwn ? 'items-end' : 'items-start')}>
        {/* Sender name for groups */}
        {!isOwn && showAvatar && senderName && (
          <p className="text-[11px] text-zixo-primary font-medium mb-0.5 ml-1">{senderName}</p>
        )}

        <div className="relative">
          <div
            className={cn(
              'px-3.5 py-2 rounded-lg text-sm leading-relaxed',
              isOwn
                ? 'bg-[#005C4B] text-[#E9EDEF] rounded-tr-none'
                : 'bg-[#1F2C34] text-[#E9EDEF] border border-white/5 rounded-tl-none'
            )}
          >
            {message.type === 'text' && <p>{message.text}</p>}

            {message.type === 'image' && (
              <div className="relative">
                {message.mediaUrl ? (
                  <img
                    src={message.mediaUrl}
                    alt="Shared image"
                    className="w-48 h-36 rounded-xl object-cover"
                    loading="lazy"
                  />
                ) : (
                  <div className="w-48 h-36 rounded-xl bg-zixo-surface-light flex items-center justify-center text-zixo-text-secondary text-xs">
                    📷 Image
                  </div>
                )}
                {message.text && <p className="mt-1.5">{message.text}</p>}
              </div>
            )}

            {message.type === 'voice' && (
              <div className="flex items-center gap-2 min-w-[140px]">
                <button className="w-7 h-7 rounded-full gradient-primary flex items-center justify-center shrink-0">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="white">
                    <polygon points="5 3 19 12 5 21 5 3" />
                  </svg>
                </button>
                <div className="flex items-center gap-[2px] flex-1 h-5">
                  {[4, 8, 12, 6, 14, 10, 5, 13, 7, 11, 15, 8, 4, 9, 12, 6, 10, 14, 7, 3, 11, 8, 13, 5].map((h, i) => (
                    <div
                      key={i}
                      className="w-[2px] rounded-full bg-current opacity-60"
                      style={{ height: `${h}px` }}
                    />
                  ))}
                </div>
                <span className="text-[10px] opacity-70">
                  {message.duration ? `${Math.floor(message.duration / 60)}:${(message.duration % 60).toString().padStart(2, '0')}` : '0:03'}
                </span>
              </div>
            )}

            {message.type === 'file' && (
              <div className="flex items-center gap-2 bg-zixo-surface-light/50 rounded-xl px-3 py-2">
                <div className="w-8 h-8 rounded-lg bg-zixo-primary/20 flex items-center justify-center text-zixo-primary text-xs">📄</div>
                <div className="min-w-0">
                  <p className="text-xs font-medium truncate">{message.fileName || 'Document'}</p>
                  <p className="text-[10px] text-zixo-text-secondary">
                    {message.fileSize ? `${(message.fileSize / 1024).toFixed(0)} KB` : 'PDF'}
                  </p>
                </div>
              </div>
            )}

            {message.type === 'location' && (
              <div className="rounded-xl overflow-hidden">
                <div className="w-48 h-28 bg-zixo-surface-light flex items-center justify-center text-zixo-text-secondary text-xs">
                  📍 Location shared
                </div>
              </div>
            )}
          </div>

          {/* Timestamp & Status */}
          <div className={cn(
            'flex items-center gap-1 mt-0.5',
            isOwn ? 'justify-end pr-1' : 'justify-start pl-1'
          )}>
            <span className="text-[10px] text-zixo-text-secondary">
              {formatMessageTime(message.timestamp)}
            </span>
            {statusIcon()}
          </div>
        </div>

        {/* Action buttons on long press / click */}
        <AnimatePresence>
          {showActions && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              className={cn(
                'flex gap-1 mt-1',
                isOwn ? 'justify-end' : 'justify-start'
              )}
            >
              {['Reply', 'Copy', 'Forward', 'Star'].map((action) => (
                <button
                  key={action}
                  className="px-2 py-0.5 rounded-full bg-zixo-surface-light text-[10px] text-zixo-text-secondary hover:text-zixo-text hover:bg-zixo-surface transition-colors"
                >
                  {action}
                </button>
              ))}
              {isOwn && (
                <button className="px-2 py-0.5 rounded-full bg-zixo-error/10 text-[10px] text-zixo-error hover:bg-zixo-error/20 transition-colors">
                  Delete
                </button>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

interface DateSeparatorProps {
  date: string;
}

export function DateSeparator({ date }: DateSeparatorProps) {
  return (
    <div className="flex items-center justify-center my-4">
      <span className="px-3 py-1 rounded-full bg-zixo-surface/80 text-[11px] text-zixo-text-secondary">
        {date}
      </span>
    </div>
  );
}

// ==================== MESSAGE SEARCH ====================

interface MessageSearchBarProps {
  onSearch: (query: string) => void;
  onClose: () => void;
  searchQuery: string;
  isSearching: boolean;
}

export function MessageSearchBar({ onSearch, onClose, searchQuery, isSearching }: MessageSearchBarProps) {
  const [text, setText] = useState(searchQuery);

  const handleSearch = () => {
    if (text.trim()) {
      onSearch(text.trim());
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <motion.div
      initial={{ height: 0, opacity: 0 }}
      animate={{ height: 'auto', opacity: 1 }}
      exit={{ height: 0, opacity: 0 }}
      className="overflow-hidden border-b border-white/5"
    >
      <div className="flex items-center gap-2 px-3 py-2">
        <div className="flex-1 relative">
          <input
            type="text"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Search messages..."
            autoFocus
            className="w-full px-3 py-1.5 rounded-lg bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none"
          />
        </div>
        <button
          onClick={handleSearch}
          disabled={isSearching}
          className="px-3 py-1.5 rounded-lg gradient-primary text-white text-xs font-medium disabled:opacity-50"
        >
          {isSearching ? '...' : 'Find'}
        </button>
        <button
          onClick={onClose}
          className="px-2 py-1.5 rounded-lg text-zixo-text-secondary hover:text-zixo-text text-xs"
        >
          ✕
        </button>
      </div>
    </motion.div>
  );
}

// ==================== SCROLL-TO-BOTTOM FAB ====================

interface ScrollToBottomFABProps {
  onClick: () => void;
  unreadCount?: number;
}

export function ScrollToBottomFAB({ onClick, unreadCount }: ScrollToBottomFABProps) {
  return (
    <motion.button
      initial={{ scale: 0, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      exit={{ scale: 0, opacity: 0 }}
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      className="absolute bottom-4 right-4 w-10 h-10 rounded-full bg-zixo-surface shadow-lg border border-white/10 flex items-center justify-center text-zixo-primary hover:bg-zixo-surface-light transition-colors z-10"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="6 9 12 15 18 9" />
      </svg>
      {unreadCount && unreadCount > 0 && (
        <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-zixo-primary text-white text-[10px] font-bold flex items-center justify-center">
          {unreadCount > 9 ? '9+' : unreadCount}
        </span>
      )}
    </motion.button>
  );
}

// ==================== CHAT INPUT BAR ====================

interface ChatInputBarProps {
  onSend: (text: string) => void;
  onAttachment: (type: string) => void;
  onVoiceRecord: () => void;
  onFileUpload: (file: File, type: 'image' | 'file') => void;
  chatId: string;
  isRecording?: boolean;
  recordingDuration?: number;
}

// ==================== RECORDING OVERLAY ====================

interface RecordingOverlayProps {
  duration: number;
  onStop: () => void;
}

export function RecordingOverlay({ duration, onStop }: RecordingOverlayProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className="flex items-center justify-between px-4 py-2.5 bg-zixo-error/15 border-b border-zixo-error/20"
    >
      <div className="flex items-center gap-2.5">
        <div className="relative">
          <div className="w-3 h-3 rounded-full bg-zixo-error" />
          <div className="absolute inset-0 w-3 h-3 rounded-full bg-zixo-error animate-ping opacity-75" />
        </div>
        <span className="text-xs font-medium text-zixo-error">Recording</span>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-sm font-mono tabular-nums text-zixo-error font-semibold">
          {Math.floor(duration / 60)}:{(duration % 60).toString().padStart(2, '0')}
        </span>
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={onStop}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-zixo-error text-white text-xs font-medium"
        >
          <svg width="10" height="10" viewBox="0 0 24 24" fill="white">
            <rect x="6" y="4" width="4" height="16" rx="1" />
            <rect x="14" y="4" width="4" height="16" rx="1" />
          </svg>
          Stop
        </motion.button>
      </div>
    </motion.div>
  );
}

export function ChatInputBar({ onSend, onAttachment, onVoiceRecord, onFileUpload, chatId, isRecording = false, recordingDuration = 0 }: ChatInputBarProps) {
  const [text, setText] = useState('');
  const [showAttachments, setShowAttachments] = useState(false);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const { currentUser, updateStorageUpload, removeStorageUpload } = useZixoStore();

  // Typing indicator handling
  const handleTypingStart = useCallback(() => {
    if (!chatId || !currentUser) return;
    setTypingIndicator(chatId, currentUser.uid, true);
  }, [chatId, currentUser]);

  const handleTypingStop = useCallback(() => {
    if (!chatId || !currentUser) return;
    setTypingIndicator(chatId, currentUser.uid, false);
  }, [chatId, currentUser]);

  const handleTextChange = (value: string) => {
    setText(value);
    if (value.trim()) {
      handleTypingStart();
      // Clear previous timeout
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      // Auto-stop typing after 3 seconds of inactivity
      typingTimeoutRef.current = setTimeout(handleTypingStop, 3000);
    } else {
      handleTypingStop();
    }
  };

  const handleSend = () => {
    if (text.trim()) {
      onSend(text.trim());
      setText('');
      handleTypingStop();
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Handle file upload with progress tracking
  const handleFileSelect = async (files: FileList | null, mediaType: 'image' | 'file') => {
    if (!files || files.length === 0 || !currentUser) return;

    const file = files[0];
    try {
      const result = await uploadChatMedia(chatId, currentUser.uid, file, mediaType, (progress: UploadProgress) => {
        updateStorageUpload(progress.uploadId, progress);
        if (progress.state === 'success' || progress.state === 'error') {
          setTimeout(() => removeStorageUpload(progress.uploadId), 3000);
        }
      });

      onFileUpload(file, mediaType);
    } catch (error) {
      console.error('[Zixo] File upload failed:', error);
    }
  };

  const attachmentTypes = [
    { icon: '📷', label: 'Gallery', type: 'image' },
    { icon: '🎥', label: 'Camera', type: 'camera' },
    { icon: '📄', label: 'File', type: 'file' },
    { icon: '📍', label: 'Location', type: 'location' },
    { icon: '👤', label: 'Contact', type: 'contact' },
  ];

  // Get active uploads for this chat
  const storageUploads = useZixoStore((s) => s.storageUploads);
  const activeUploads = Object.values(storageUploads).filter(
    (u) => u.state === 'running' && u.uploadId.includes(chatId)
  );

  return (
    <div className="bg-[#1F2C34] safe-area-bottom">
      {/* Upload Progress */}
      <AnimatePresence>
        {activeUploads.length > 0 && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="px-4 py-2 space-y-1"
          >
            {activeUploads.map((upload) => (
              <div key={upload.uploadId} className="flex items-center gap-2">
                <span className="text-[11px] text-zixo-text-secondary truncate flex-1">{upload.fileName}</span>
                <div className="w-20 h-1.5 bg-zixo-surface-light rounded-full overflow-hidden">
                  <div
                    className="h-full gradient-primary rounded-full transition-all duration-300"
                    style={{ width: `${upload.progress}%` }}
                  />
                </div>
                <span className="text-[10px] text-zixo-text-secondary">{upload.progress}%</span>
              </div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Attachment Panel */}
      <AnimatePresence>
        {showAttachments && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden"
          >
            <div className="flex gap-4 px-6 py-4">
              {attachmentTypes.map((a) => (
                <motion.button
                  key={a.type}
                  whileTap={{ scale: 0.9 }}
                  onClick={() => {
                    if (a.type === 'image') {
                      imageInputRef.current?.click();
                    } else if (a.type === 'file') {
                      fileInputRef.current?.click();
                    } else {
                      onAttachment(a.type);
                    }
                    setShowAttachments(false);
                  }}
                  className="flex flex-col items-center gap-1.5"
                >
                  <div className="w-12 h-12 rounded-full bg-zixo-surface-light flex items-center justify-center text-xl">
                    {a.icon}
                  </div>
                  <span className="text-[10px] text-zixo-text-secondary">{a.label}</span>
                </motion.button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Hidden file inputs */}
      <input
        ref={imageInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => handleFileSelect(e.target.files, 'image')}
      />
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={(e) => handleFileSelect(e.target.files, 'file')}
      />

      {/* Input Bar */}
      <div className="flex items-end gap-2 px-3 py-2">
        {/* Attachment Toggle */}
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={() => setShowAttachments(!showAttachments)}
          className="shrink-0 w-10 h-10 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-primary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
          </svg>
        </motion.button>

        {/* Text Input */}
        <div className="flex-1 relative">
          <textarea
            value={text}
            onChange={(e) => handleTextChange(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type a message..."
            rows={1}
            className="w-full px-4 py-2.5 rounded-2xl bg-[#2A3942] text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-[#25D366]/30 focus:outline-none resize-none max-h-28 transition-colors"
            style={{ minHeight: '40px' }}
          />
        </div>

        {/* Voice / Send Button / Recording indicator */}
        {isRecording ? (
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            exit={{ scale: 0 }}
            className="flex items-center gap-2"
          >
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-zixo-error/20 border border-zixo-error/30">
              <div className="w-2 h-2 rounded-full bg-zixo-error animate-pulse" />
              <span className="text-xs text-zixo-error font-mono tabular-nums">
                {Math.floor(recordingDuration / 60)}:{(recordingDuration % 60).toString().padStart(2, '0')}
              </span>
            </div>
            <motion.button
              whileTap={{ scale: 0.85 }}
              onClick={onVoiceRecord}
              className="shrink-0 w-10 h-10 rounded-full bg-zixo-error flex items-center justify-center"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="6" y="4" width="4" height="16" rx="1" />
                <rect x="14" y="4" width="4" height="16" rx="1" />
              </svg>
            </motion.button>
          </motion.div>
        ) : (
          <AnimatePresence mode="wait">
            {text.trim() ? (
              <motion.button
                key="send"
                initial={{ scale: 0, rotate: -180 }}
                animate={{ scale: 1, rotate: 0 }}
                exit={{ scale: 0, rotate: 180 }}
                whileTap={{ scale: 0.85 }}
                onClick={handleSend}
                className="shrink-0 w-10 h-10 rounded-full bg-[#25D366] flex items-center justify-center"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
                  <line x1="22" y1="2" x2="11" y2="13" stroke="white" strokeWidth="2" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" fill="white" />
                </svg>
              </motion.button>
            ) : (
              <motion.button
                key="mic"
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                exit={{ scale: 0 }}
                whileTap={{ scale: 0.85 }}
                onClick={onVoiceRecord}
                className="shrink-0 w-10 h-10 rounded-full bg-zixo-surface-light flex items-center justify-center text-zixo-text-secondary hover:text-zixo-primary transition-colors"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                  <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                  <line x1="12" y1="19" x2="12" y2="23" />
                  <line x1="8" y1="23" x2="16" y2="23" />
                </svg>
              </motion.button>
            )}
          </AnimatePresence>
        )}
      </div>
    </div>
  );
}

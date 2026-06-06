'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { cn, formatTime } from '@/lib/zixo-utils';
import Avatar from './Avatar';
import TypingIndicator from './Common';
import type { Chat } from '@/stores/useZixoStore';

interface ChatListItemProps {
  chat: Chat;
  currentUserId: string;
  onClick: (chatId: string) => void;
  onQuickCall: (userId: string) => void;
}

export default function ChatListItem({ chat, currentUserId, onClick, onQuickCall }: ChatListItemProps) {
  const otherUser = chat.participantProfiles.find((p) => p.uid !== currentUserId);
  if (!otherUser) return null;

  const name = chat.isGroup ? chat.groupName || 'Group' : otherUser.displayName;
  const isOnline = otherUser.online;
  const lastMsg = chat.lastMessage;
  const isTyping = chat.typing && chat.typing.length > 0;

  return (
    <motion.div
      whileTap={{ scale: 0.98 }}
      onClick={() => onClick(chat.id)}
      className="flex items-center gap-3 px-4 py-3 hover:bg-zixo-surface/50 active:bg-zixo-surface/80 transition-colors cursor-pointer"
    >
      <Avatar name={name} uid={otherUser.uid} size="lg" online={isOnline} />

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-zixo-text truncate">{name}</h3>
          {lastMsg && (
            <span className={cn(
              'text-[11px] shrink-0',
              chat.unreadCount > 0 ? 'text-[#25D366] font-medium' : 'text-zixo-text-secondary'
            )}>
              {formatTime(lastMsg.timestamp)}
            </span>
          )}
        </div>

        <div className="flex items-center justify-between gap-2 mt-0.5">
          {isTyping ? (
            <TypingIndicator />
          ) : (
            <p className={cn(
              'text-xs truncate',
              chat.unreadCount > 0 ? 'text-zixo-text font-medium' : 'text-zixo-text-secondary'
            )}>
              {lastMsg?.senderId === currentUserId && (
                <span className="text-zixo-text-secondary">
                  {lastMsg.status === 'read' ? '✓✓ ' : lastMsg.status === 'delivered' ? '✓✓ ' : '✓ '}
                </span>
              )}
              {lastMsg?.text || 'No messages yet'}
            </p>
          )}

          {chat.unreadCount > 0 && (
            <div className="shrink-0 min-w-[20px] h-[20px] flex items-center justify-center rounded-full bg-[#25D366] text-white text-[10px] font-bold px-1">
              {chat.unreadCount}
            </div>
          )}
        </div>
      </div>

      {/* Quick audio call button - visible on hover/larger screens */}
      <motion.button
        whileTap={{ scale: 0.85 }}
        onClick={(e) => {
          e.stopPropagation();
          onQuickCall(otherUser.uid);
        }}
        className="shrink-0 w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
        </svg>
      </motion.button>
    </motion.div>
  );
}

interface ChatListProps {
  chats: Chat[];
  currentUserId: string;
  onChatClick: (chatId: string) => void;
  onQuickCall: (userId: string) => void;
  searchQuery: string;
}

export function ChatList({ chats, currentUserId, onChatClick, onQuickCall, searchQuery }: ChatListProps) {
  const filteredChats = searchQuery
    ? chats.filter((chat) => {
        const other = chat.participantProfiles.find((p) => p.uid !== currentUserId);
        const name = chat.isGroup ? chat.groupName : other?.displayName;
        return name?.toLowerCase().includes(searchQuery.toLowerCase());
      })
    : chats;

  const sortedChats = [...filteredChats].sort((a, b) => b.updatedAt - a.updatedAt);

  if (sortedChats.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 px-6 text-center">
        <motion.div
          animate={{ y: [0, -8, 0] }}
          transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
          className="mb-6"
        >
          <div className="w-20 h-20 rounded-full gradient-primary flex items-center justify-center opacity-80">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
          </div>
        </motion.div>
        <h3 className="text-lg font-semibold text-zixo-text mb-2">
          {searchQuery ? 'No results found' : 'Start connecting'}
        </h3>
        <p className="text-sm text-zixo-text-secondary max-w-[260px]">
          {searchQuery
            ? 'Try a different search term'
            : 'Begin a conversation with someone on Zixo. Free calls, free messages.'}
        </p>
      </div>
    );
  }

  return (
    <div className="divide-y divide-white/5">
      {sortedChats.map((chat, i) => (
        <motion.div
          key={chat.id}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: i * 0.03 }}
        >
          <ChatListItem
            chat={chat}
            currentUserId={currentUserId}
            onClick={onChatClick}
            onQuickCall={onQuickCall}
          />
        </motion.div>
      ))}
    </div>
  );
}

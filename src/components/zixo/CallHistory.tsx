'use client';

import React, { useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { cn, formatCallDuration, formatCallTime } from '@/lib/zixo-utils';
import type { CallRecord } from '@/stores/useZixoStore';
import type { ZixoUserProfile } from '@/services/auth';

interface CallHistoryListProps {
  calls: CallRecord[];
  currentUserId: string;
  onCallBack: (call: CallRecord) => void;
}

export function CallHistoryList({ calls, currentUserId, onCallBack }: CallHistoryListProps) {
  const [filter, setFilter] = useState<'all' | 'missed' | 'outgoing' | 'incoming'>('all');

  const filteredCalls = calls.filter((call) => {
    if (filter === 'all') return true;
    return call.direction === filter;
  });

  const filters = [
    { id: 'all' as const, label: 'All' },
    { id: 'missed' as const, label: 'Missed' },
    { id: 'outgoing' as const, label: 'Outgoing' },
    { id: 'incoming' as const, label: 'Incoming' },
  ];

  return (
    <div>
      {/* Filter Tabs */}
      <div className="flex gap-2 px-4 py-3 overflow-x-auto">
        {filters.map((f) => (
          <button
            key={f.id}
            onClick={() => setFilter(f.id)}
            className={cn(
              'px-4 py-1.5 rounded-full text-xs font-medium transition-colors shrink-0',
              filter === f.id
                ? 'gradient-primary text-white'
                : 'bg-zixo-surface-light text-zixo-text-secondary hover:text-zixo-text'
            )}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Call List */}
      {filteredCalls.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 px-6 text-center">
          <motion.div
            animate={{ y: [0, -8, 0] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            className="mb-6"
          >
            <div className="w-20 h-20 rounded-full bg-zixo-surface flex items-center justify-center">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
            </div>
          </motion.div>
          <h3 className="text-lg font-semibold text-zixo-text mb-2">No recent calls</h3>
          <p className="text-sm text-zixo-text-secondary">Your call history will appear here</p>
        </div>
      ) : (
        <div className="divide-y divide-white/5">
          {filteredCalls.map((call, i) => {
            const isIncoming = call.direction === 'incoming';
            const isMissed = call.direction === 'missed';
            const otherName = call.callerId === currentUserId ? call.receiverName : call.callerName;
            const otherUid = call.callerId === currentUserId ? call.receiverId : call.callerId;

            return (
              <motion.div
                key={call.id}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.03 }}
                className="flex items-center gap-3 px-4 py-3 hover:bg-zixo-surface/50 transition-colors"
              >
                <Avatar name={otherName} uid={otherUid} size="lg" />

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h4 className={cn(
                      'text-sm font-medium truncate',
                      isMissed ? 'text-zixo-error' : 'text-zixo-text'
                    )}>
                      {otherName}
                    </h4>
                  </div>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    {/* Direction icon */}
                    {call.direction === 'outgoing' && (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-zixo-success">
                        <line x1="7" y1="17" x2="17" y2="7" />
                        <polyline points="7 7 17 7 17 17" />
                      </svg>
                    )}
                    {call.direction === 'incoming' && (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-zixo-secondary">
                        <line x1="17" y1="7" x2="7" y2="17" />
                        <polyline points="17 17 7 17 7 7" />
                      </svg>
                    )}
                    {call.direction === 'missed' && (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-zixo-error">
                        <line x1="17" y1="7" x2="7" y2="17" />
                        <polyline points="17 17 7 17 7 7" />
                      </svg>
                    )}

                    {/* Call type icon */}
                    {call.type === 'video' ? (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-text-secondary">
                        <polygon points="23 7 16 12 23 17 23 7" />
                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                      </svg>
                    ) : (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-text-secondary">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3" />
                      </svg>
                    )}

                    <span className="text-xs text-zixo-text-secondary">
                      {call.duration > 0 ? formatCallDuration(call.duration) : 'Missed'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="text-[11px] text-zixo-text-secondary">{formatCallTime(call.timestamp)}</span>
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={() => onCallBack(call)}
                    className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
                  >
                    {call.type === 'video' ? (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polygon points="23 7 16 12 23 17 23 7" />
                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                      </svg>
                    ) : (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                      </svg>
                    )}
                  </motion.button>
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </div>
  );
}

interface ContactsScreenProps {
  contacts: ZixoUserProfile[];
  onStartChat: (userId: string) => void;
  onStartCall: (userId: string, type: 'audio' | 'video') => void;
  onSearchUser?: (username: string) => void;
  allUsers?: ZixoUserProfile[];
  onSearchUsers?: (query: string) => Promise<ZixoUserProfile[]>;
}

export function ContactsScreen({ contacts, onStartChat, onStartCall, onSearchUser, allUsers = [], onSearchUsers }: ContactsScreenProps) {
  const [search, setSearch] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<ZixoUserProfile[]>([]);
  const [hasSearched, setHasSearched] = useState(false);

  // Use allUsers as the default list, filter locally if search is short
  const displayUsers = searchResults.length > 0
    ? searchResults
    : search.trim().length > 0 && search.trim().length < 2
      ? allUsers.filter(
          (c) =>
            c.displayName.toLowerCase().includes(search.toLowerCase()) ||
            c.username.toLowerCase().includes(search.toLowerCase())
        )
      : allUsers;

  // Handle search - search Firestore when user types 2+ chars
  const handleSearch = useCallback(async (query: string) => {
    if (!query.trim() || !onSearchUsers) return;
    setIsSearching(true);
    setHasSearched(true);
    try {
      const results = await onSearchUsers(query.trim());
      setSearchResults(results);
    } catch (err) {
      console.error('[Zixo] Search failed:', err);
    } finally {
      setIsSearching(false);
    }
  }, [onSearchUsers]);

  // Debounced search
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);
    if (value.trim().length >= 2 && onSearchUsers) {
      searchTimeoutRef.current = setTimeout(() => handleSearch(value), 500);
    } else if (value.trim().length === 0) {
      setSearchResults([]);
      setHasSearched(false);
    }
  }, [handleSearch, onSearchUsers]);

  // Merge contacts (existing chat partners) with all discovered users
  // Mark which users already have chats
  const contactUids = new Set(contacts.map((c) => c.uid));

  // Separate into "existing chats" and "discover new people"
  const existingChatUsers = displayUsers.filter((u) => contactUids.has(u.uid));
  const newUsers = displayUsers.filter((u) => !contactUids.has(u.uid));

  const renderUserItem = (contact: ZixoUserProfile, i: number) => (
    <motion.div
      key={contact.uid}
      initial={{ opacity: 0, x: -10 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: Math.min(i * 0.03, 0.3) }}
      className="flex items-center gap-3 px-4 py-3 hover:bg-zixo-surface/50 transition-colors cursor-pointer"
      onClick={() => onStartChat(contact.uid)}
    >
      <Avatar name={contact.displayName} uid={contact.uid} size="lg" online={contact.online} />

      <div className="flex-1 min-w-0">
        <h4 className="text-sm font-medium text-zixo-text truncate">{contact.displayName}</h4>
        <p className="text-xs text-zixo-text-secondary truncate">{contact.username}</p>
      </div>

      <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
        <motion.button
          whileTap={{ scale: 0.85 }}
          onClick={() => onStartChat(contact.uid)}
          className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-primary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </motion.button>
        <motion.button
          whileTap={{ scale: 0.85 }}
          onClick={() => onStartCall(contact.uid, 'audio')}
          className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
          </svg>
        </motion.button>
        <motion.button
          whileTap={{ scale: 0.85 }}
          onClick={() => onStartCall(contact.uid, 'video')}
          className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polygon points="23 7 16 12 23 17 23 7" />
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
          </svg>
        </motion.button>
      </div>
    </motion.div>
  );

  return (
    <div className="pb-4">
      {/* Search */}
      <div className="px-4 py-3">
        <div className="relative">
          <svg
            className="absolute left-3 top-1/2 -translate-y-1/2 text-zixo-text-secondary"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by name or @username..."
            className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors"
          />
          {isSearching && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                className="w-4 h-4 border-2 border-zixo-primary/30 border-t-zixo-primary rounded-full"
              />
            </div>
          )}
        </div>
      </div>

      {/* Existing Chat Partners */}
      {existingChatUsers.length > 0 && (
        <>
          <div className="px-4 py-2">
            <h3 className="text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider">
              Your Chats ({existingChatUsers.length})
            </h3>
          </div>
          <div className="divide-y divide-white/5">
            {existingChatUsers.map((contact, i) => renderUserItem(contact, i))}
          </div>
        </>
      )}

      {/* New Users (Discovery) */}
      {newUsers.length > 0 && (
        <>
          <div className="px-4 py-2 mt-2">
            <h3 className="text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider">
              People on Zixo ({newUsers.length})
            </h3>
          </div>
          <div className="divide-y divide-white/5">
            {newUsers.map((contact, i) => renderUserItem(contact, i + existingChatUsers.length))}
          </div>
        </>
      )}

      {/* Searching indicator */}
      {isSearching && displayUsers.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
            className="w-8 h-8 border-2 border-zixo-primary/30 border-t-zixo-primary rounded-full mb-4"
          />
          <p className="text-sm text-zixo-text-secondary">Searching...</p>
        </div>
      )}

      {/* Empty state */}
      {displayUsers.length === 0 && !isSearching && hasSearched && (
        <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
          <div className="w-16 h-16 rounded-full bg-zixo-surface flex items-center justify-center mb-4">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>
          <h3 className="text-base font-semibold text-zixo-text mb-1">No users found</h3>
          <p className="text-sm text-zixo-text-secondary">Try a different name or username</p>
        </div>
      )}

      {displayUsers.length === 0 && !isSearching && !hasSearched && (
        <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
          <div className="w-16 h-16 rounded-full bg-zixo-surface flex items-center justify-center mb-4">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </div>
          <h3 className="text-base font-semibold text-zixo-text mb-1">Discover people</h3>
          <p className="text-sm text-zixo-text-secondary">Search by name or @username to find and chat with people on Zixo</p>
        </div>
      )}
    </div>
  );
}

'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/zixo-utils';
import type { ZixoUserProfile } from '@/services/auth';

// ==================== ADMIN PANEL ====================

interface AdminPanelProps {
  currentUser: ZixoUserProfile;
  onBack: () => void;
}

interface AdminUser {
  id: string;
  uid?: string;
  email?: string;
  displayName?: string;
  username?: string;
  role?: string;
  online?: boolean;
  lastSeen?: any;
  createdAt?: any;
}

export default function AdminPanel({ currentUser, onBack }: AdminPanelProps) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch('/api/zixo', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'listUsers', requesterUid: currentUser.uid }),
      });
      const data = await res.json();
      if (data.success) {
        setUsers(data.users);
      } else {
        setError(data.error || 'Failed to fetch users');
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [currentUser.uid]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleGrantAdmin = async (targetUid: string) => {
    setActionLoading(targetUid);
    try {
      const res = await fetch('/api/zixo', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'grantAdmin', targetUid, requesterUid: currentUser.uid }),
      });
      const data = await res.json();
      if (data.success) {
        setUsers(prev => prev.map(u => u.id === targetUid || u.uid === targetUid ? { ...u, role: 'admin' } : u));
        showToast('Admin role granted successfully');
      } else {
        showToast(data.error || 'Failed to grant admin', 'error');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRevokeAdmin = async (targetUid: string) => {
    setActionLoading(targetUid);
    try {
      const res = await fetch('/api/zixo', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'revokeAdmin', targetUid, requesterUid: currentUser.uid }),
      });
      const data = await res.json();
      if (data.success) {
        setUsers(prev => prev.map(u => u.id === targetUid || u.uid === targetUid ? { ...u, role: 'user' } : u));
        showToast('Admin role revoked');
      } else {
        showToast(data.error || 'Failed to revoke admin', 'error');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeleteUser = async (targetUid: string) => {
    if (!confirm('Are you sure you want to delete this user? This cannot be undone.')) return;
    setActionLoading(targetUid);
    try {
      const res = await fetch('/api/zixo', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'adminDeleteUser', targetUid, requesterUid: currentUser.uid }),
      });
      const data = await res.json();
      if (data.success) {
        setUsers(prev => prev.filter(u => u.id !== targetUid && u.uid !== targetUid));
        showToast('User deleted');
      } else {
        showToast(data.error || 'Failed to delete user', 'error');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const filteredUsers = users.filter(u => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      (u.email || '').toLowerCase().includes(q) ||
      (u.displayName || '').toLowerCase().includes(q) ||
      (u.username || '').toLowerCase().includes(q)
    );
  });

  const getUid = (u: AdminUser) => u.id || u.uid || '';

  return (
    <div className="min-h-screen bg-zixo-bg">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-zixo-bg/80 backdrop-blur-xl border-b border-white/5">
        <div className="flex items-center gap-3 px-4 py-3">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onBack}
            className="w-9 h-9 rounded-full bg-zixo-surface flex items-center justify-center text-zixo-text"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </motion.button>
          <div className="flex-1">
            <h2 className="font-semibold text-zixo-text flex items-center gap-2">
              Admin Panel
              <span className="px-1.5 py-0.5 rounded-full text-[9px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30">ADMIN</span>
            </h2>
            <p className="text-xs text-zixo-text-secondary">{users.length} users registered</p>
          </div>
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={fetchUsers}
            className="w-9 h-9 rounded-full bg-zixo-surface flex items-center justify-center text-zixo-text-secondary"
            disabled={loading}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={cn(loading && 'animate-spin')}>
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
            </svg>
          </motion.button>
        </div>
      </div>

      {/* Search */}
      <div className="px-4 py-3">
        <div className="relative">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="absolute left-3 top-1/2 -translate-y-1/2 text-zixo-text-secondary">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search users by name, email, or username..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-zixo-surface text-sm text-zixo-text placeholder-zixo-text-secondary focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
          />
        </div>
      </div>

      {/* Stats */}
      <div className="px-4 pb-2 grid grid-cols-3 gap-2">
        <div className="bg-zixo-surface rounded-xl p-3 text-center">
          <p className="text-lg font-bold text-zixo-text">{users.length}</p>
          <p className="text-[10px] text-zixo-text-secondary uppercase">Total Users</p>
        </div>
        <div className="bg-zixo-surface rounded-xl p-3 text-center">
          <p className="text-lg font-bold text-amber-400">{users.filter(u => u.role === 'admin').length}</p>
          <p className="text-[10px] text-zixo-text-secondary uppercase">Admins</p>
        </div>
        <div className="bg-zixo-surface rounded-xl p-3 text-center">
          <p className="text-lg font-bold text-green-400">{users.filter(u => u.online).length}</p>
          <p className="text-[10px] text-zixo-text-secondary uppercase">Online</p>
        </div>
      </div>

      {/* User List */}
      <div className="px-4 pb-20">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-12">
            <div className="w-8 h-8 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin mb-3" />
            <p className="text-sm text-zixo-text-secondary">Loading users...</p>
          </div>
        ) : error ? (
          <div className="text-center py-12">
            <p className="text-zixo-error text-sm mb-2">{error}</p>
            <button onClick={fetchUsers} className="text-zixo-primary text-sm">Retry</button>
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-zixo-text-secondary text-sm">No users found</p>
          </div>
        ) : (
          <div className="space-y-1">
            {filteredUsers.map((u) => {
              const uid = getUid(u);
              const isCurrentUser = uid === currentUser.uid;
              const isAdmin = u.role === 'admin';
              const isLoading = actionLoading === uid;

              return (
                <motion.div
                  key={uid}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="bg-zixo-surface rounded-xl p-3"
                >
                  <div className="flex items-center gap-3">
                    {/* Avatar */}
                    <div className="relative shrink-0">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-zixo-primary/30 to-zixo-primary/10 flex items-center justify-center text-sm font-medium text-zixo-text">
                        {(u.displayName || u.email || '?')[0]?.toUpperCase()}
                      </div>
                      {u.online && (
                        <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-green-500 border-2 border-zixo-surface" />
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <p className="text-sm font-medium text-zixo-text truncate">
                          {u.displayName || u.email?.split('@')[0] || 'Unknown'}
                        </p>
                        {isAdmin && (
                          <span className="px-1.5 py-0.5 rounded-full text-[8px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 shrink-0">ADMIN</span>
                        )}
                        {isCurrentUser && (
                          <span className="px-1.5 py-0.5 rounded-full text-[8px] font-bold bg-zixo-primary/20 text-zixo-primary border border-zixo-primary/30 shrink-0">YOU</span>
                        )}
                      </div>
                      <p className="text-xs text-zixo-text-secondary truncate">{u.email || 'No email'}</p>
                      <p className="text-[10px] text-zixo-text-secondary/60 truncate">UID: {uid.slice(0, 12)}...</p>
                    </div>

                    {/* Actions */}
                    {!isCurrentUser && (
                      <div className="flex items-center gap-1.5 shrink-0">
                        {isLoading ? (
                          <div className="w-5 h-5 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <>
                            {isAdmin ? (
                              <motion.button
                                whileTap={{ scale: 0.9 }}
                                onClick={() => handleRevokeAdmin(uid)}
                                className="px-2.5 py-1 rounded-lg text-[10px] font-medium bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 transition-colors"
                              >
                                Revoke Admin
                              </motion.button>
                            ) : (
                              <motion.button
                                whileTap={{ scale: 0.9 }}
                                onClick={() => handleGrantAdmin(uid)}
                                className="px-2.5 py-1 rounded-lg text-[10px] font-medium bg-zixo-primary/10 text-zixo-primary hover:bg-zixo-primary/20 transition-colors"
                              >
                                Make Admin
                              </motion.button>
                            )}
                            <motion.button
                              whileTap={{ scale: 0.9 }}
                              onClick={() => handleDeleteUser(uid)}
                              className="px-2.5 py-1 rounded-lg text-[10px] font-medium bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
                            >
                              Delete
                            </motion.button>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </div>

      {/* Toast */}
      <AnimatePresence>
        {toast && (
          <motion.div
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
            className={cn(
              'fixed bottom-20 left-4 right-4 p-3 rounded-xl text-sm font-medium text-center z-50',
              toast.type === 'success' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
            )}
          >
            {toast.message}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

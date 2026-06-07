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
  banned?: boolean;
  fcmToken?: string;
  zixoNumber?: string;
}

type AdminTab = 'users' | 'settings' | 'notifications' | 'system';

export default function AdminPanel({ currentUser, onBack }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>('users');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  // Settings state
  const [authConfig, setAuthConfig] = useState<{ phoneEnabled: boolean; googleEnabled: boolean }>({
    phoneEnabled: false,
    googleEnabled: true,
  });
  const [smsRegions, setSmsRegions] = useState('');
  const [settingsLoading, setSettingsLoading] = useState(false);

  // Notifications state
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastBody, setBroadcastBody] = useState('');
  const [sendToUid, setSendToUid] = useState('');
  const [sendToTitle, setSendToTitle] = useState('');
  const [sendToBody, setSendToBody] = useState('');
  const [notifLoading, setNotifLoading] = useState(false);
  const [notifHistory, setNotifHistory] = useState<{ title: string; body: string; sentAt: number; recipientCount: number }[]>([]);

  // System state
  const [appStats, setAppStats] = useState<{ totalUsers: number; onlineUsers: number; activeChats: number; callCount: number } | null>(null);
  const [systemLoading, setSystemLoading] = useState(false);
  const [healthStatus, setHealthStatus] = useState<{ status: string; firebase: any } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const apiCall = useCallback(async (action: string, extra: Record<string, any> = {}) => {
    const res = await fetch('/api/zixo', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action, requesterUid: currentUser.uid, ...extra }),
    });
    const data = await res.json();
    if (!data.success && data.error) throw new Error(data.error);
    return data;
  }, [currentUser.uid]);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiCall('listUsers');
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
  }, [apiCall]);

  const fetchAppStats = useCallback(async () => {
    try {
      setSystemLoading(true);
      const data = await apiCall('getAppStats');
      if (data.success) {
        setAppStats(data.stats);
      }
    } catch (err: any) {
      console.error('[Admin] Failed to get stats:', err);
    } finally {
      setSystemLoading(false);
    }
  }, [apiCall]);

  const fetchHealth = useCallback(async () => {
    try {
      const res = await fetch('/api/zixo');
      const data = await res.json();
      setHealthStatus(data);
    } catch (err: any) {
      console.error('[Admin] Health check failed:', err);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
    fetchHealth();
  }, [fetchUsers, fetchHealth]);

  useEffect(() => {
    if (activeTab === 'system') {
      fetchAppStats();
      fetchHealth();
    }
  }, [activeTab, fetchAppStats, fetchHealth]);

  // ==================== USER ACTIONS ====================
  const handleGrantAdmin = async (targetUid: string) => {
    setActionLoading(targetUid);
    try {
      const data = await apiCall('grantAdmin', { targetUid });
      if (data.success) {
        setUsers(prev => prev.map(u => u.id === targetUid || u.uid === targetUid ? { ...u, role: 'admin' } : u));
        showToast('Admin role granted');
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
      const data = await apiCall('revokeAdmin', { targetUid });
      if (data.success) {
        setUsers(prev => prev.map(u => u.id === targetUid || u.uid === targetUid ? { ...u, role: 'user' } : u));
        showToast('Admin role revoked');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleBanUser = async (targetUid: string) => {
    setActionLoading(targetUid);
    try {
      const data = await apiCall('banUser', { targetUid });
      if (data.success) {
        setUsers(prev => prev.map(u => u.id === targetUid || u.uid === targetUid ? { ...u, banned: true } : u));
        showToast('User banned');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnbanUser = async (targetUid: string) => {
    setActionLoading(targetUid);
    try {
      const data = await apiCall('unbanUser', { targetUid });
      if (data.success) {
        setUsers(prev => prev.map(u => u.id === targetUid || u.uid === targetUid ? { ...u, banned: false } : u));
        showToast('User unbanned');
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
      const data = await apiCall('adminDeleteUser', { targetUid });
      if (data.success) {
        setUsers(prev => prev.filter(u => u.id !== targetUid && u.uid !== targetUid));
        showToast('User deleted');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ==================== NOTIFICATION ACTIONS ====================
  const handleBroadcast = async () => {
    if (!broadcastTitle.trim() || !broadcastBody.trim()) {
      showToast('Title and body are required', 'error');
      return;
    }
    setNotifLoading(true);
    try {
      const data = await apiCall('broadcastNotification', { title: broadcastTitle, body: broadcastBody });
      if (data.success) {
        setNotifHistory(prev => [{ title: broadcastTitle, body: broadcastBody, sentAt: Date.now(), recipientCount: data.sentCount || 0 }, ...prev]);
        showToast(`Broadcast sent to ${data.sentCount || 0} users`);
        setBroadcastTitle('');
        setBroadcastBody('');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setNotifLoading(false);
    }
  };

  const handleSendToUser = async () => {
    if (!sendToUid.trim() || !sendToTitle.trim()) {
      showToast('User UID and title are required', 'error');
      return;
    }
    setNotifLoading(true);
    try {
      const data = await apiCall('sendPush', { uid: sendToUid, title: sendToTitle, body: sendToBody });
      if (data.success) {
        showToast('Notification sent');
        setSendToUid('');
        setSendToTitle('');
        setSendToBody('');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setNotifLoading(false);
    }
  };

  // ==================== SETTINGS ACTIONS ====================
  const handleUpdateAuthConfig = async (phoneEnabled: boolean, googleEnabled: boolean) => {
    setSettingsLoading(true);
    try {
      const data = await apiCall('updateAuthConfig', { phoneEnabled, googleEnabled });
      if (data.success) {
        setAuthConfig({ phoneEnabled, googleEnabled });
        showToast('Auth config updated');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setSettingsLoading(false);
    }
  };

  const handleEnablePhoneAuth = async () => {
    setSettingsLoading(true);
    try {
      const data = await apiCall('enablePhoneAuth');
      if (data.success) {
        setAuthConfig(prev => ({ ...prev, phoneEnabled: true }));
        showToast('Phone auth enabled');
      } else {
        showToast(data.error || 'Failed to enable phone auth', 'error');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setSettingsLoading(false);
    }
  };

  // ==================== FILTERING ====================
  const filteredUsers = users.filter(u => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      (u.email || '').toLowerCase().includes(q) ||
      (u.displayName || '').toLowerCase().includes(q) ||
      (u.username || '').toLowerCase().includes(q) ||
      (u.zixoNumber || '').includes(q)
    );
  });

  const getUid = (u: AdminUser) => u.id || u.uid || '';

  const tabs: { id: AdminTab; label: string; icon: React.ReactNode }[] = [
    {
      id: 'users',
      label: 'Users',
      icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>,
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" /></svg>,
    },
    {
      id: 'notifications',
      label: 'Notify',
      icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 0 1-3.46 0" /></svg>,
    },
    {
      id: 'system',
      label: 'System',
      icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" /></svg>,
    },
  ];

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

        {/* Tab Navigation */}
        <div className="flex px-4 gap-1 pb-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={cn(
                'flex-1 flex items-center justify-center gap-1.5 px-2 py-2 rounded-lg text-xs font-medium transition-colors',
                activeTab === tab.id
                  ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                  : 'bg-zixo-surface text-zixo-text-secondary hover:text-zixo-text border border-transparent'
              )}
            >
              {tab.icon}
              <span className="hidden sm:inline">{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      <div className="pb-20">
        <AnimatePresence mode="wait">
          {activeTab === 'users' && (
            <motion.div key="users" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              {renderUsersTab()}
            </motion.div>
          )}
          {activeTab === 'settings' && (
            <motion.div key="settings" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              {renderSettingsTab()}
            </motion.div>
          )}
          {activeTab === 'notifications' && (
            <motion.div key="notifications" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              {renderNotificationsTab()}
            </motion.div>
          )}
          {activeTab === 'system' && (
            <motion.div key="system" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              {renderSystemTab()}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* User Detail Modal */}
      <AnimatePresence>
        {selectedUser && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center p-4"
            onClick={() => setSelectedUser(null)}
          >
            <motion.div
              initial={{ y: 50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 50, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md bg-zixo-surface rounded-2xl p-4 border border-white/10 max-h-[80vh] overflow-y-auto"
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-zixo-text">User Details</h3>
                <button onClick={() => setSelectedUser(null)} className="text-zixo-text-secondary hover:text-zixo-text">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
                </button>
              </div>
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div className="w-14 h-14 rounded-full bg-gradient-to-br from-zixo-primary/30 to-zixo-primary/10 flex items-center justify-center text-xl font-medium text-zixo-text">
                    {(selectedUser.displayName || '?')[0]?.toUpperCase()}
                  </div>
                  <div>
                    <h4 className="font-semibold text-zixo-text">{selectedUser.displayName || 'Unknown'}</h4>
                    <p className="text-xs text-zixo-text-secondary">{selectedUser.email || 'No email'}</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-zixo-bg rounded-lg p-2.5">
                    <p className="text-zixo-text-secondary mb-0.5">Role</p>
                    <p className="font-medium text-zixo-text">{selectedUser.role || 'user'}</p>
                  </div>
                  <div className="bg-zixo-bg rounded-lg p-2.5">
                    <p className="text-zixo-text-secondary mb-0.5">Status</p>
                    <p className={cn("font-medium", selectedUser.banned ? "text-red-400" : selectedUser.online ? "text-green-400" : "text-zixo-text")}>
                      {selectedUser.banned ? 'BANNED' : selectedUser.online ? 'Online' : 'Offline'}
                    </p>
                  </div>
                  <div className="bg-zixo-bg rounded-lg p-2.5">
                    <p className="text-zixo-text-secondary mb-0.5">Zixo Number</p>
                    <p className="font-mono font-medium text-zixo-text">{selectedUser.zixoNumber || 'N/A'}</p>
                  </div>
                  <div className="bg-zixo-bg rounded-lg p-2.5">
                    <p className="text-zixo-text-secondary mb-0.5">FCM Token</p>
                    <p className="font-medium text-zixo-text">{selectedUser.fcmToken ? '✓ Registered' : '✗ None'}</p>
                  </div>
                </div>

                <div className="bg-zixo-bg rounded-lg p-2.5 text-xs">
                  <p className="text-zixo-text-secondary mb-0.5">UID</p>
                  <p className="font-mono text-zixo-text break-all">{getUid(selectedUser)}</p>
                </div>

                <div className="bg-zixo-bg rounded-lg p-2.5 text-xs">
                  <p className="text-zixo-text-secondary mb-0.5">Username</p>
                  <p className="font-medium text-zixo-text">{selectedUser.username || 'N/A'}</p>
                </div>

                <div className="flex gap-2 pt-2">
                  {getUid(selectedUser) !== currentUser.uid && (
                    <>
                      {selectedUser.banned ? (
                        <motion.button
                          whileTap={{ scale: 0.95 }}
                          onClick={() => { handleUnbanUser(getUid(selectedUser)); setSelectedUser(null); }}
                          className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-green-500/10 text-green-400 hover:bg-green-500/20 transition-colors"
                        >
                          Unban User
                        </motion.button>
                      ) : (
                        <motion.button
                          whileTap={{ scale: 0.95 }}
                          onClick={() => { handleBanUser(getUid(selectedUser)); setSelectedUser(null); }}
                          className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
                        >
                          Ban User
                        </motion.button>
                      )}
                      {selectedUser.role === 'admin' ? (
                        <motion.button
                          whileTap={{ scale: 0.95 }}
                          onClick={() => { handleRevokeAdmin(getUid(selectedUser)); setSelectedUser(null); }}
                          className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 transition-colors"
                        >
                          Revoke Admin
                        </motion.button>
                      ) : (
                        <motion.button
                          whileTap={{ scale: 0.95 }}
                          onClick={() => { handleGrantAdmin(getUid(selectedUser)); setSelectedUser(null); }}
                          className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-zixo-primary/10 text-zixo-primary hover:bg-zixo-primary/20 transition-colors"
                        >
                          Make Admin
                        </motion.button>
                      )}
                      <motion.button
                        whileTap={{ scale: 0.95 }}
                        onClick={() => { handleDeleteUser(getUid(selectedUser)); setSelectedUser(null); }}
                        className="py-2.5 px-4 rounded-xl text-xs font-medium bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
                      >
                        Delete
                      </motion.button>
                    </>
                  )}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

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

  // ==================== USERS TAB ====================
  function renderUsersTab() {
    return (
      <>
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
              placeholder="Search users by name, email, username, or Zixo No..."
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-zixo-surface text-sm text-zixo-text placeholder-zixo-text-secondary focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
            />
          </div>
        </div>

        {/* Stats */}
        <div className="px-4 pb-2 grid grid-cols-4 gap-2">
          <div className="bg-zixo-surface rounded-xl p-2.5 text-center">
            <p className="text-base font-bold text-zixo-text">{users.length}</p>
            <p className="text-[9px] text-zixo-text-secondary uppercase">Total</p>
          </div>
          <div className="bg-zixo-surface rounded-xl p-2.5 text-center">
            <p className="text-base font-bold text-green-400">{users.filter(u => u.online).length}</p>
            <p className="text-[9px] text-zixo-text-secondary uppercase">Online</p>
          </div>
          <div className="bg-zixo-surface rounded-xl p-2.5 text-center">
            <p className="text-base font-bold text-amber-400">{users.filter(u => u.role === 'admin').length}</p>
            <p className="text-[9px] text-zixo-text-secondary uppercase">Admins</p>
          </div>
          <div className="bg-zixo-surface rounded-xl p-2.5 text-center">
            <p className="text-base font-bold text-red-400">{users.filter(u => u.banned).length}</p>
            <p className="text-[9px] text-zixo-text-secondary uppercase">Banned</p>
          </div>
        </div>

        {/* User List */}
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
          <div className="px-4 space-y-1">
            {filteredUsers.map((u) => {
              const uid = getUid(u);
              const isCurrentUser = uid === currentUser.uid;
              const isAdmin = u.role === 'admin';
              const isBanned = u.banned;
              const isLoading = actionLoading === uid;

              return (
                <motion.div
                  key={uid}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className={cn(
                    "bg-zixo-surface rounded-xl p-3 cursor-pointer hover:bg-zixo-surface/80 transition-colors",
                    isBanned && "border border-red-500/20 opacity-70"
                  )}
                  onClick={() => setSelectedUser(u)}
                >
                  <div className="flex items-center gap-3">
                    {/* Avatar */}
                    <div className="relative shrink-0">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-zixo-primary/30 to-zixo-primary/10 flex items-center justify-center text-sm font-medium text-zixo-text">
                        {(u.displayName || u.email || '?')[0]?.toUpperCase()}
                      </div>
                      {u.online && !isBanned && (
                        <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-green-500 border-2 border-zixo-surface" />
                      )}
                      {isBanned && (
                        <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-red-500 border-2 border-zixo-surface" />
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <p className={cn("text-sm font-medium truncate", isBanned ? "text-red-400 line-through" : "text-zixo-text")}>
                          {u.displayName || u.email?.split('@')[0] || 'Unknown'}
                        </p>
                        {isAdmin && (
                          <span className="px-1.5 py-0.5 rounded-full text-[8px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 shrink-0">ADMIN</span>
                        )}
                        {isBanned && (
                          <span className="px-1.5 py-0.5 rounded-full text-[8px] font-bold bg-red-500/20 text-red-400 border border-red-500/30 shrink-0">BANNED</span>
                        )}
                        {isCurrentUser && (
                          <span className="px-1.5 py-0.5 rounded-full text-[8px] font-bold bg-zixo-primary/20 text-zixo-primary border border-zixo-primary/30 shrink-0">YOU</span>
                        )}
                      </div>
                      <p className="text-xs text-zixo-text-secondary truncate">{u.email || 'No email'}</p>
                      <div className="flex items-center gap-2">
                        {u.zixoNumber && (
                          <p className="text-[10px] text-zixo-text-secondary/60 font-mono">{u.zixoNumber}</p>
                        )}
                        <p className="text-[10px] text-zixo-text-secondary/60 truncate">UID: {uid.slice(0, 12)}...</p>
                      </div>
                    </div>

                    {/* Quick Actions */}
                    {!isCurrentUser && (
                      <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
                        {isLoading ? (
                          <div className="w-5 h-5 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <>
                            {isBanned ? (
                              <motion.button
                                whileTap={{ scale: 0.9 }}
                                onClick={() => handleUnbanUser(uid)}
                                className="px-2 py-1 rounded-lg text-[10px] font-medium bg-green-500/10 text-green-400 hover:bg-green-500/20 transition-colors"
                              >
                                Unban
                              </motion.button>
                            ) : (
                              <motion.button
                                whileTap={{ scale: 0.9 }}
                                onClick={() => handleBanUser(uid)}
                                className="px-2 py-1 rounded-lg text-[10px] font-medium bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
                              >
                                Ban
                              </motion.button>
                            )}
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
      </>
    );
  }

  // ==================== SETTINGS TAB ====================
  function renderSettingsTab() {
    return (
      <div className="px-4 py-3 space-y-4">
        {/* Auth Configuration */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
            Authentication Providers
          </h3>
          <div className="space-y-3">
            {/* Phone Auth Toggle */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-zixo-text">Phone Auth (OTP)</p>
                <p className="text-[10px] text-zixo-text-secondary">Enable sign-in with phone number via SMS</p>
              </div>
              <button
                onClick={handleEnablePhoneAuth}
                disabled={settingsLoading}
                className={cn(
                  "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                  authConfig.phoneEnabled ? "bg-green-500" : "bg-zixo-surface-light"
                )}
              >
                <span className={cn(
                  "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                  authConfig.phoneEnabled ? "translate-x-6" : "translate-x-1"
                )} />
              </button>
            </div>

            {/* Google Auth Toggle */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-zixo-text">Google Auth</p>
                <p className="text-[10px] text-zixo-text-secondary">Enable sign-in with Google account</p>
              </div>
              <button
                onClick={() => handleUpdateAuthConfig(authConfig.phoneEnabled, !authConfig.googleEnabled)}
                disabled={settingsLoading}
                className={cn(
                  "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                  authConfig.googleEnabled ? "bg-green-500" : "bg-zixo-surface-light"
                )}
              >
                <span className={cn(
                  "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                  authConfig.googleEnabled ? "translate-x-6" : "translate-x-1"
                )} />
              </button>
            </div>

            {/* Email Auth (always on) */}
            <div className="flex items-center justify-between opacity-60">
              <div>
                <p className="text-sm text-zixo-text">Email/Password Auth</p>
                <p className="text-[10px] text-zixo-text-secondary">Always enabled</p>
              </div>
              <div className="relative inline-flex h-6 w-11 items-center rounded-full bg-green-500">
                <span className="inline-block h-4 w-4 transform rounded-full bg-white translate-x-6" />
              </div>
            </div>
          </div>
        </div>

        {/* SMS Region Whitelist */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="2" y1="12" x2="22" y2="12" /><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" /></svg>
            SMS Region Configuration
          </h3>
          <p className="text-[10px] text-zixo-text-secondary mb-2">
            Enter allowed country codes (comma separated). Leave empty for all regions.
          </p>
          <input
            type="text"
            value={smsRegions}
            onChange={(e) => setSmsRegions(e.target.value)}
            placeholder="e.g. +1, +44, +880, +91"
            className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
          />
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => showToast('SMS regions saved')}
            className="mt-2 px-4 py-2 rounded-xl gradient-primary text-white text-xs font-medium"
          >
            Save Regions
          </motion.button>
        </div>
      </div>
    );
  }

  // ==================== NOTIFICATIONS TAB ====================
  function renderNotificationsTab() {
    return (
      <div className="px-4 py-3 space-y-4">
        {/* Broadcast Notification */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>
            Broadcast to All Users
          </h3>
          <div className="space-y-2">
            <input
              type="text"
              value={broadcastTitle}
              onChange={(e) => setBroadcastTitle(e.target.value)}
              placeholder="Notification title"
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
            />
            <textarea
              value={broadcastBody}
              onChange={(e) => setBroadcastBody(e.target.value)}
              placeholder="Notification message"
              rows={3}
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50 resize-none"
            />
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleBroadcast}
              disabled={notifLoading || !broadcastTitle.trim() || !broadcastBody.trim()}
              className={cn(
                "w-full py-2.5 rounded-xl text-sm font-medium transition-all",
                notifLoading || !broadcastTitle.trim() || !broadcastBody.trim()
                  ? "bg-zixo-surface-light text-zixo-text-secondary cursor-not-allowed"
                  : "gradient-primary text-white"
              )}
            >
              {notifLoading ? 'Sending...' : 'Send Broadcast'}
            </motion.button>
          </div>
        </div>

        {/* Send to Specific User */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>
            Send to Specific User
          </h3>
          <div className="space-y-2">
            <input
              type="text"
              value={sendToUid}
              onChange={(e) => setSendToUid(e.target.value)}
              placeholder="User UID"
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50 font-mono text-xs"
            />
            <input
              type="text"
              value={sendToTitle}
              onChange={(e) => setSendToTitle(e.target.value)}
              placeholder="Notification title"
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
            />
            <textarea
              value={sendToBody}
              onChange={(e) => setSendToBody(e.target.value)}
              placeholder="Notification message (optional)"
              rows={2}
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50 resize-none"
            />
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleSendToUser}
              disabled={notifLoading || !sendToUid.trim() || !sendToTitle.trim()}
              className={cn(
                "w-full py-2.5 rounded-xl text-sm font-medium transition-all",
                notifLoading || !sendToUid.trim() || !sendToTitle.trim()
                  ? "bg-zixo-surface-light text-zixo-text-secondary cursor-not-allowed"
                  : "gradient-primary text-white"
              )}
            >
              {notifLoading ? 'Sending...' : 'Send Notification'}
            </motion.button>
          </div>
        </div>

        {/* Notification History */}
        {notifHistory.length > 0 && (
          <div className="bg-zixo-surface rounded-xl p-4">
            <h3 className="text-sm font-semibold text-zixo-text mb-3">Recent Notifications</h3>
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {notifHistory.map((n, i) => (
                <div key={i} className="bg-zixo-bg rounded-lg p-2.5">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-medium text-zixo-text">{n.title}</p>
                    <span className="text-[10px] text-zixo-text-secondary">{new Date(n.sentAt).toLocaleTimeString()}</span>
                  </div>
                  <p className="text-[10px] text-zixo-text-secondary truncate mt-0.5">{n.body}</p>
                  <p className="text-[10px] text-zixo-primary mt-0.5">{n.recipientCount} recipients</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  }

  // ==================== SYSTEM TAB ====================
  function renderSystemTab() {
    return (
      <div className="px-4 py-3 space-y-4">
        {/* App Stats */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 20V10" /><path d="M12 20V4" /><path d="M6 20v-6" /></svg>
            App Statistics
          </h3>
          {systemLoading && !appStats ? (
            <div className="flex justify-center py-6">
              <div className="w-6 h-6 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin" />
            </div>
          ) : appStats ? (
            <div className="grid grid-cols-2 gap-2">
              <div className="bg-zixo-bg rounded-xl p-3 text-center">
                <p className="text-xl font-bold text-zixo-text">{appStats.totalUsers}</p>
                <p className="text-[10px] text-zixo-text-secondary uppercase">Total Users</p>
              </div>
              <div className="bg-zixo-bg rounded-xl p-3 text-center">
                <p className="text-xl font-bold text-green-400">{appStats.onlineUsers}</p>
                <p className="text-[10px] text-zixo-text-secondary uppercase">Online</p>
              </div>
              <div className="bg-zixo-bg rounded-xl p-3 text-center">
                <p className="text-xl font-bold text-amber-400">{appStats.activeChats}</p>
                <p className="text-[10px] text-zixo-text-secondary uppercase">Active Chats</p>
              </div>
              <div className="bg-zixo-bg rounded-xl p-3 text-center">
                <p className="text-xl font-bold text-zixo-primary">{appStats.callCount}</p>
                <p className="text-[10px] text-zixo-text-secondary uppercase">Calls</p>
              </div>
            </div>
          ) : (
            <p className="text-xs text-zixo-text-secondary text-center py-4">Failed to load stats</p>
          )}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={fetchAppStats}
            className="mt-2 w-full py-2 rounded-xl text-xs font-medium bg-zixo-bg text-zixo-text-secondary hover:text-zixo-text transition-colors"
          >
            Refresh Stats
          </motion.button>
        </div>

        {/* Firebase Project Info */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" /></svg>
            Firebase Project Info
          </h3>
          {healthStatus ? (
            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
                <span className="text-zixo-text-secondary">Project ID</span>
                <span className="font-mono text-zixo-text">{healthStatus.firebase?.projectId || 'zixo-call'}</span>
              </div>
              <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
                <span className="text-zixo-text-secondary">Region</span>
                <span className="font-mono text-zixo-text">{healthStatus.firebase?.region || 'us-central1'}</span>
              </div>
              <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
                <span className="text-zixo-text-secondary">Admin API</span>
                <span className={healthStatus.firebase?.adminEnabled ? "text-green-400" : "text-red-400"}>
                  {healthStatus.firebase?.adminEnabled ? '✓ Enabled' : '✗ Disabled'}
                </span>
              </div>
              <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
                <span className="text-zixo-text-secondary">RTDB</span>
                <span className={healthStatus.firebase?.rtdbEnabled ? "text-green-400" : "text-red-400"}>
                  {healthStatus.firebase?.rtdbEnabled ? '✓ Connected' : '✗ Disconnected'}
                </span>
              </div>
              <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
                <span className="text-zixo-text-secondary">App Version</span>
                <span className="font-mono text-zixo-text">{healthStatus.version || '1.0.0'}</span>
              </div>
            </div>
          ) : (
            <div className="flex justify-center py-4">
              <div className="w-6 h-6 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin" />
            </div>
          )}
        </div>

        {/* Server Health */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>
            Server Health Check
          </h3>
          <div className="space-y-2">
            <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
              <span className="text-xs text-zixo-text-secondary">API Endpoint</span>
              <span className={cn("text-xs font-medium", healthStatus?.status === 'ok' ? "text-green-400" : "text-red-400")}>
                {healthStatus?.status === 'ok' ? '● Operational' : '● Down'}
              </span>
            </div>
            <div className="flex items-center justify-between bg-zixo-bg rounded-lg p-2.5">
              <span className="text-xs text-zixo-text-secondary">Active Connections</span>
              <span className="text-xs font-mono text-zixo-text">{appStats?.onlineUsers || 0} users online</span>
            </div>
          </div>
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={fetchHealth}
            className="mt-2 w-full py-2 rounded-xl text-xs font-medium bg-zixo-bg text-zixo-text-secondary hover:text-zixo-text transition-colors"
          >
            Run Health Check
          </motion.button>
        </div>
      </div>
    );
  }
}

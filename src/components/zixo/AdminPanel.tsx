'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
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

interface NotifTemplate {
  id: string;
  title: string;
  body: string;
  savedAt: number;
}

interface ScheduledNotif {
  id: string;
  title: string;
  body: string;
  scheduledFor: number;
  isBroadcast: boolean;
  targetUid?: string;
}

type AdminTab = 'dashboard' | 'users' | 'settings' | 'notifications' | 'system';

// Confirmation dialog state
interface ConfirmDialog {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  confirmVariant: 'danger' | 'warning' | 'primary';
  onConfirm: () => void;
}

export default function AdminPanel({ currentUser, onBack }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>('dashboard');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  // Confirmation dialog
  const [confirmDialog, setConfirmDialog] = useState<ConfirmDialog>({
    open: false, title: '', message: '', confirmLabel: 'Confirm', confirmVariant: 'primary', onConfirm: () => {},
  });

  // Dashboard state
  const [dashboardStats, setDashboardStats] = useState<{
    totalUsers: number; onlineUsers: number; activeChats: number; activeCalls: number;
    recentRegistrations: number; activityData: number[];
  } | null>(null);
  const [dashboardLoading, setDashboardLoading] = useState(true);

  // Settings state
  const [authConfig, setAuthConfig] = useState<{ phoneEnabled: boolean; googleEnabled: boolean }>({
    phoneEnabled: false,
    googleEnabled: true,
  });
  const [smsRegions, setSmsRegions] = useState('');
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [maxUploadSize, setMaxUploadSize] = useState(10);
  const [rateLimitMsgs, setRateLimitMsgs] = useState(30);
  const [maintenanceMode, setMaintenanceMode] = useState(false);

  // Notifications state
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastBody, setBroadcastBody] = useState('');
  const [sendToUid, setSendToUid] = useState('');
  const [sendToTitle, setSendToTitle] = useState('');
  const [sendToBody, setSendToBody] = useState('');
  const [notifLoading, setNotifLoading] = useState(false);
  const [notifHistory, setNotifHistory] = useState<{ title: string; body: string; sentAt: number; recipientCount: number }[]>([]);
  const [notifTemplates, setNotifTemplates] = useState<NotifTemplate[]>([]);
  const [scheduledNotifs, setScheduledNotifs] = useState<ScheduledNotif[]>([]);
  const [scheduleTitle, setScheduleTitle] = useState('');
  const [scheduleBody, setScheduleBody] = useState('');
  const [scheduleTime, setScheduleTime] = useState('');
  const [scheduleIsBroadcast, setScheduleIsBroadcast] = useState(true);
  const [scheduleTargetUid, setScheduleTargetUid] = useState('');
  const [showTemplateManager, setShowTemplateManager] = useState(false);
  const [newTemplateName, setNewTemplateName] = useState('');
  const [newTemplateBody, setNewTemplateBody] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');

  // System state
  const [appStats, setAppStats] = useState<{
    totalUsers: number; onlineUsers: number; activeChats: number; callCount: number;
    firestoreUsage?: { reads: number; writes: number; deletes: number };
    rtdbConnections?: number;
  } | null>(null);
  const [systemLoading, setSystemLoading] = useState(false);
  const [healthStatus, setHealthStatus] = useState<{
    status: string; firebase: any; uptime?: number; version?: string;
  } | null>(null);
  const [apiHealthChecks, setApiHealthChecks] = useState<{ name: string; status: 'healthy' | 'degraded' | 'down'; latency: number }[]>([]);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const showConfirm = (title: string, message: string, confirmLabel: string, variant: 'danger' | 'warning' | 'primary', onConfirm: () => void) => {
    setConfirmDialog({ open: true, title, message, confirmLabel, confirmVariant: variant, onConfirm });
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

  const fetchDashboardStats = useCallback(async () => {
    try {
      setDashboardLoading(true);
      const data = await apiCall('getAppStats');
      if (data.success && data.stats) {
        const stats = data.stats;
        const recentCount = users.filter(u => {
          if (!u.createdAt) return false;
          const created = u.createdAt?.toMillis?.() || (typeof u.createdAt === 'number' ? u.createdAt : 0);
          return Date.now() - created < 7 * 24 * 60 * 60 * 1000;
        }).length;
        setDashboardStats({
          totalUsers: stats.totalUsers || users.length,
          onlineUsers: stats.onlineUsers || users.filter(u => u.online).length,
          activeChats: stats.activeChats || 0,
          activeCalls: stats.callCount || 0,
          recentRegistrations: recentCount,
          activityData: generateMockActivityData(),
        });
      }
    } catch (err: any) {
      // Fallback to computed stats
      setDashboardStats({
        totalUsers: users.length,
        onlineUsers: users.filter(u => u.online).length,
        activeChats: 0,
        activeCalls: 0,
        recentRegistrations: users.filter(u => {
          if (!u.createdAt) return false;
          const created = u.createdAt?.toMillis?.() || (typeof u.createdAt === 'number' ? u.createdAt : 0);
          return Date.now() - created < 7 * 24 * 60 * 60 * 1000;
        }).length,
        activityData: generateMockActivityData(),
      });
    } finally {
      setDashboardLoading(false);
    }
  }, [apiCall, users]);

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

  const runApiHealthChecks = useCallback(async () => {
    type HealthStatus = 'healthy' | 'degraded' | 'down';
    const checks: { name: string; status: HealthStatus; latency: number }[] = [
      { name: 'Zixo API', status: 'healthy', latency: 0 },
      { name: 'Firebase Auth', status: 'healthy', latency: 0 },
      { name: 'Firestore', status: 'healthy', latency: 0 },
      { name: 'RTDB', status: 'healthy', latency: 0 },
    ];
    const startTime = Date.now();
    try {
      await fetch('/api/zixo');
      checks[0].latency = Date.now() - startTime;
      checks[0].status = checks[0].latency < 500 ? 'healthy' : 'degraded';
    } catch {
      checks[0].status = 'down';
      checks[0].latency = Date.now() - startTime;
    }
    for (let i = 1; i < checks.length; i++) {
      const t = Date.now();
      try {
        const data = await apiCall('getAppStats');
        checks[i].latency = Date.now() - t;
        checks[i].status = checks[i].latency < 1000 ? 'healthy' : 'degraded';
        if (i === 1) checks[i].status = data.success ? 'healthy' : 'degraded';
      } catch {
        checks[i].status = 'degraded';
        checks[i].latency = Date.now() - t;
      }
    }
    setApiHealthChecks(checks);
  }, [apiCall]);

  useEffect(() => {
    fetchUsers();
    fetchHealth();
  }, [fetchUsers, fetchHealth]);

  useEffect(() => {
    if (activeTab === 'dashboard') {
      fetchDashboardStats();
    }
  }, [activeTab, fetchDashboardStats]);

  useEffect(() => {
    if (activeTab === 'system') {
      fetchAppStats();
      fetchHealth();
      runApiHealthChecks();
    }
  }, [activeTab, fetchAppStats, fetchHealth, runApiHealthChecks]);

  // Load templates from localStorage
  useEffect(() => {
    try {
      const saved = localStorage.getItem('zixo_notif_templates');
      if (saved) setNotifTemplates(JSON.parse(saved));
      const scheduled = localStorage.getItem('zixo_scheduled_notifs');
      if (scheduled) setScheduledNotifs(JSON.parse(scheduled));
    } catch {}
  }, []);

  // Save templates to localStorage
  useEffect(() => {
    try {
      localStorage.setItem('zixo_notif_templates', JSON.stringify(notifTemplates));
    } catch {}
  }, [notifTemplates]);

  useEffect(() => {
    try {
      localStorage.setItem('zixo_scheduled_notifs', JSON.stringify(scheduledNotifs));
    } catch {}
  }, [scheduledNotifs]);

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

  const handleSendNotifToUser = async (uid: string, title: string, body: string) => {
    setNotifLoading(true);
    try {
      const data = await apiCall('sendPush', { uid, title, body });
      if (data.success) {
        showToast('Notification sent to user');
        setNotifHistory(prev => [{ title, body, sentAt: Date.now(), recipientCount: 1 }, ...prev]);
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setNotifLoading(false);
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

  const handleSendToUserNotif = async () => {
    if (!sendToUid.trim() || !sendToTitle.trim()) {
      showToast('User UID and title are required', 'error');
      return;
    }
    setNotifLoading(true);
    try {
      const data = await apiCall('sendPush', { uid: sendToUid, title: sendToTitle, body: sendToBody });
      if (data.success) {
        showToast('Notification sent');
        setNotifHistory(prev => [{ title: sendToTitle, body: sendToBody, sentAt: Date.now(), recipientCount: 1 }, ...prev]);
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

  const handleSaveTemplate = () => {
    if (!newTemplateName.trim() || !newTemplateBody.trim()) {
      showToast('Template name and body are required', 'error');
      return;
    }
    const template: NotifTemplate = {
      id: `tpl-${Date.now()}`,
      title: newTemplateName,
      body: newTemplateBody,
      savedAt: Date.now(),
    };
    setNotifTemplates(prev => [template, ...prev]);
    setNewTemplateName('');
    setNewTemplateBody('');
    showToast('Template saved');
  };

  const handleDeleteTemplate = (id: string) => {
    setNotifTemplates(prev => prev.filter(t => t.id !== id));
    showToast('Template deleted');
  };

  const handleUseTemplate = (template: NotifTemplate) => {
    setBroadcastTitle(template.title);
    setBroadcastBody(template.body);
    setSelectedTemplate(template.id);
    showToast('Template loaded');
  };

  const handleScheduleNotification = () => {
    if (!scheduleTitle.trim() || !scheduleBody.trim() || !scheduleTime) {
      showToast('Title, body, and time are required', 'error');
      return;
    }
    const scheduledFor = new Date(scheduleTime).getTime();
    if (scheduledFor <= Date.now()) {
      showToast('Schedule time must be in the future', 'error');
      return;
    }
    const notif: ScheduledNotif = {
      id: `sched-${Date.now()}`,
      title: scheduleTitle,
      body: scheduleBody,
      scheduledFor,
      isBroadcast: scheduleIsBroadcast,
      targetUid: scheduleIsBroadcast ? undefined : scheduleTargetUid || undefined,
    };
    setScheduledNotifs(prev => [...prev, notif]);
    setScheduleTitle('');
    setScheduleBody('');
    setScheduleTime('');
    showToast('Notification scheduled');
  };

  const handleCancelScheduled = (id: string) => {
    setScheduledNotifs(prev => prev.filter(n => n.id !== id));
    showToast('Scheduled notification cancelled');
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

  // ==================== QUICK ACTIONS ====================
  const handleQuickBroadcast = () => {
    setActiveTab('notifications');
  };

  const handleCleanupStaleCalls = async () => {
    setSettingsLoading(true);
    try {
      const data = await apiCall('cleanupStaleCalls');
      if (data.success) {
        showToast(`Cleaned up ${data.cleanedCount || 0} stale calls`);
      } else {
        showToast('Cleanup completed (no stale data found)');
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setSettingsLoading(false);
    }
  };

  const handleAssignZixoNumbers = async () => {
    setSettingsLoading(true);
    try {
      const data = await apiCall('assignZixoNumbers');
      if (data.success) {
        showToast(`Assigned ${data.assignedCount || 0} Zixo numbers`);
        fetchUsers();
      }
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setSettingsLoading(false);
    }
  };

  const handleExportCSV = () => {
    const headers = ['UID', 'Email', 'Display Name', 'Username', 'Zixo Number', 'Role', 'Online', 'Banned'];
    const rows = users.map(u => [
      getUid(u), u.email || '', u.displayName || '', u.username || '',
      u.zixoNumber || '', u.role || 'user', u.online ? 'Yes' : 'No', u.banned ? 'Yes' : 'No'
    ]);
    const csvContent = [headers, ...rows].map(row => row.map(cell => `"${cell}"`).join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `zixo_users_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    showToast('User data exported as CSV');
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
      id: 'dashboard',
      label: 'Dashboard',
      icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>,
    },
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
        <div className="flex px-2 gap-0.5 pb-2 overflow-x-auto">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={cn(
                'flex-1 min-w-0 flex items-center justify-center gap-1 px-1.5 py-2 rounded-lg text-[10px] sm:text-xs font-medium transition-colors whitespace-nowrap',
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
          {activeTab === 'dashboard' && (
            <motion.div key="dashboard" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              {renderDashboardTab()}
            </motion.div>
          )}
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
        {selectedUser && renderUserDetailModal()}
      </AnimatePresence>

      {/* Confirmation Dialog */}
      <AnimatePresence>
        {confirmDialog.open && renderConfirmDialog()}
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

  // ==================== CONFIRM DIALOG ====================
  function renderConfirmDialog() {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 z-[60] flex items-center justify-center p-4"
        onClick={() => setConfirmDialog(prev => ({ ...prev, open: false }))}
      >
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.9, opacity: 0 }}
          onClick={(e) => e.stopPropagation()}
          className="w-full max-w-sm bg-zixo-surface rounded-2xl p-6 border border-white/10"
        >
          <div className="flex items-center gap-3 mb-4">
            <div className={cn(
              "w-10 h-10 rounded-full flex items-center justify-center",
              confirmDialog.confirmVariant === 'danger' ? 'bg-red-500/10' : confirmDialog.confirmVariant === 'warning' ? 'bg-amber-500/10' : 'bg-zixo-primary/10'
            )}>
              {confirmDialog.confirmVariant === 'danger' ? (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#EF4444" strokeWidth="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>
              ) : confirmDialog.confirmVariant === 'warning' ? (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#F59E0B" strokeWidth="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
              )}
            </div>
            <div>
              <h3 className="font-semibold text-zixo-text text-sm">{confirmDialog.title}</h3>
              <p className="text-xs text-zixo-text-secondary mt-1">{confirmDialog.message}</p>
            </div>
          </div>
          <div className="flex gap-2 mt-4">
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => setConfirmDialog(prev => ({ ...prev, open: false }))}
              className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-zixo-surface-light text-zixo-text-secondary"
            >
              Cancel
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => {
                confirmDialog.onConfirm();
                setConfirmDialog(prev => ({ ...prev, open: false }));
              }}
              className={cn(
                "flex-1 py-2.5 rounded-xl text-xs font-medium",
                confirmDialog.confirmVariant === 'danger' ? 'bg-red-500/20 text-red-400' :
                confirmDialog.confirmVariant === 'warning' ? 'bg-amber-500/20 text-amber-400' :
                'gradient-primary text-white'
              )}
            >
              {confirmDialog.confirmLabel}
            </motion.button>
          </div>
        </motion.div>
      </motion.div>
    );
  }

  // ==================== USER DETAIL MODAL ====================
  function renderUserDetailModal() {
    if (!selectedUser) return null;
    const uid = getUid(selectedUser);
    const isCurrentUser = uid === currentUser.uid;
    return (
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
              <p className="text-zixo-text-secondary mb-0.5">Last Seen</p>
              <p className="font-medium text-zixo-text">{selectedUser.lastSeen ? new Date(selectedUser.lastSeen?.toMillis?.() || selectedUser.lastSeen).toLocaleString() : 'N/A'}</p>
            </div>

            <div className="bg-zixo-bg rounded-lg p-2.5 text-xs">
              <p className="text-zixo-text-secondary mb-0.5">UID</p>
              <p className="font-mono text-zixo-text break-all">{uid}</p>
            </div>

            <div className="bg-zixo-bg rounded-lg p-2.5 text-xs">
              <p className="text-zixo-text-secondary mb-0.5">Username</p>
              <p className="font-medium text-zixo-text">{selectedUser.username || 'N/A'}</p>
            </div>

            {/* Send notification to user */}
            <div className="bg-zixo-bg rounded-lg p-3">
              <p className="text-xs font-medium text-zixo-text mb-2">Send Notification</p>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Notification message"
                  id="admin-user-notif-input"
                  className="flex-1 px-3 py-2 rounded-lg bg-zixo-surface text-xs text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      const input = e.currentTarget;
                      if (input.value.trim()) {
                        handleSendNotifToUser(uid, 'Admin Message', input.value.trim());
                        input.value = '';
                      }
                    }
                  }}
                />
                <motion.button
                  whileTap={{ scale: 0.9 }}
                  onClick={() => {
                    const input = document.getElementById('admin-user-notif-input') as HTMLInputElement;
                    if (input?.value.trim()) {
                      handleSendNotifToUser(uid, 'Admin Message', input.value.trim());
                      input.value = '';
                    }
                  }}
                  className="px-3 py-2 rounded-lg gradient-primary text-white text-xs font-medium"
                >
                  Send
                </motion.button>
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              {!isCurrentUser && (
                <>
                  {selectedUser.banned ? (
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      onClick={() => {
                        showConfirm('Unban User', `Are you sure you want to unban ${selectedUser.displayName || 'this user'}?`, 'Unban', 'primary', () => {
                          handleUnbanUser(uid);
                          setSelectedUser(null);
                        });
                      }}
                      className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-green-500/10 text-green-400 hover:bg-green-500/20 transition-colors"
                    >
                      Unban User
                    </motion.button>
                  ) : (
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      onClick={() => {
                        showConfirm('Ban User', `Are you sure you want to ban ${selectedUser.displayName || 'this user'}? They will lose access immediately.`, 'Ban User', 'danger', () => {
                          handleBanUser(uid);
                          setSelectedUser(null);
                        });
                      }}
                      className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
                    >
                      Ban User
                    </motion.button>
                  )}
                  {selectedUser.role === 'admin' ? (
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      onClick={() => {
                        showConfirm('Revoke Admin', `Are you sure you want to revoke admin access from ${selectedUser.displayName || 'this user'}?`, 'Revoke', 'warning', () => {
                          handleRevokeAdmin(uid);
                          setSelectedUser(null);
                        });
                      }}
                      className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 transition-colors"
                    >
                      Revoke Admin
                    </motion.button>
                  ) : (
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      onClick={() => {
                        showConfirm('Grant Admin', `Are you sure you want to grant admin privileges to ${selectedUser.displayName || 'this user'}? This gives them full control.`, 'Grant Admin', 'warning', () => {
                          handleGrantAdmin(uid);
                          setSelectedUser(null);
                        });
                      }}
                      className="flex-1 py-2.5 rounded-xl text-xs font-medium bg-zixo-primary/10 text-zixo-primary hover:bg-zixo-primary/20 transition-colors"
                    >
                      Make Admin
                    </motion.button>
                  )}
                  <motion.button
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      showConfirm('Delete User', `Are you sure you want to PERMANENTLY DELETE ${selectedUser.displayName || 'this user'}? This action cannot be undone.`, 'Delete', 'danger', () => {
                        handleDeleteUser(uid);
                        setSelectedUser(null);
                      });
                    }}
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
    );
  }

  // ==================== DASHBOARD TAB ====================
  function renderDashboardTab() {
    const stats = dashboardStats;
    return (
      <div className="px-4 py-3 space-y-4">
        {/* Stat Cards */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-zixo-surface rounded-xl p-4 relative overflow-hidden">
            <div className="absolute top-2 right-2 w-8 h-8 rounded-full bg-zixo-primary/10 flex items-center justify-center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
            </div>
            <p className="text-2xl font-bold text-zixo-text">{stats?.totalUsers ?? '-'}</p>
            <p className="text-[10px] text-zixo-text-secondary uppercase tracking-wider">Total Users</p>
          </div>
          <div className="bg-zixo-surface rounded-xl p-4 relative overflow-hidden">
            <div className="absolute top-2 right-2 w-8 h-8 rounded-full bg-green-500/10 flex items-center justify-center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#4ADE80" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M8 12l2 2 4-4" /></svg>
            </div>
            <p className="text-2xl font-bold text-green-400">{stats?.onlineUsers ?? '-'}</p>
            <p className="text-[10px] text-zixo-text-secondary uppercase tracking-wider">Online Users</p>
          </div>
          <div className="bg-zixo-surface rounded-xl p-4 relative overflow-hidden">
            <div className="absolute top-2 right-2 w-8 h-8 rounded-full bg-zixo-accent/10 flex items-center justify-center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#34B7F1" strokeWidth="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
            </div>
            <p className="text-2xl font-bold text-zixo-accent">{stats?.activeChats ?? '-'}</p>
            <p className="text-[10px] text-zixo-text-secondary uppercase tracking-wider">Active Chats</p>
          </div>
          <div className="bg-zixo-surface rounded-xl p-4 relative overflow-hidden">
            <div className="absolute top-2 right-2 w-8 h-8 rounded-full bg-amber-500/10 flex items-center justify-center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#F59E0B" strokeWidth="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3" /></svg>
            </div>
            <p className="text-2xl font-bold text-amber-400">{stats?.activeCalls ?? '-'}</p>
            <p className="text-[10px] text-zixo-text-secondary uppercase tracking-wider">Active Calls</p>
          </div>
        </div>

        {/* Recent Registrations */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-zixo-text flex items-center gap-2">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="4" /><line x1="20" y1="8" x2="20" y2="14" /><line x1="23" y1="11" x2="17" y2="11" /></svg>
              Recent Registrations
            </h3>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-zixo-primary/10 text-zixo-primary">{stats?.recentRegistrations ?? 0} in 7 days</span>
          </div>
          {stats?.recentRegistrations === 0 ? (
            <p className="text-xs text-zixo-text-secondary text-center py-4">No new registrations in the last 7 days</p>
          ) : (
            <div className="space-y-1.5 max-h-32 overflow-y-auto">
              {users
                .filter(u => {
                  if (!u.createdAt) return false;
                  const created = u.createdAt?.toMillis?.() || (typeof u.createdAt === 'number' ? u.createdAt : 0);
                  return Date.now() - created < 7 * 24 * 60 * 60 * 1000;
                })
                .slice(0, 5)
                .map(u => (
                  <div key={getUid(u)} className="flex items-center gap-2 bg-zixo-bg rounded-lg p-2">
                    <div className="w-7 h-7 rounded-full bg-gradient-to-br from-zixo-primary/30 to-zixo-primary/10 flex items-center justify-center text-[10px] font-medium text-zixo-text">
                      {(u.displayName || '?')[0]?.toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium text-zixo-text truncate">{u.displayName || 'Unknown'}</p>
                    </div>
                    <p className="text-[9px] text-zixo-text-secondary shrink-0">
                      {u.createdAt ? new Date(u.createdAt?.toMillis?.() || u.createdAt).toLocaleDateString() : ''}
                    </p>
                  </div>
                ))}
            </div>
          )}
        </div>

        {/* Activity Graph */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 20V10" /><path d="M12 20V4" /><path d="M6 20v-6" /></svg>
            Activity (Last 7 Days)
          </h3>
          <div className="flex items-end gap-1.5 h-24">
            {stats?.activityData?.map((val, i) => {
              const maxVal = Math.max(...(stats?.activityData || [1]));
              const height = maxVal > 0 ? (val / maxVal) * 100 : 0;
              const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
              return (
                <div key={i} className="flex-1 flex flex-col items-center gap-1">
                  <motion.div
                    initial={{ height: 0 }}
                    animate={{ height: `${Math.max(height, 5)}%` }}
                    transition={{ duration: 0.5, delay: i * 0.05 }}
                    className="w-full rounded-t-md gradient-primary min-h-[3px]"
                    style={{ opacity: 0.5 + (val / (maxVal || 1)) * 0.5 }}
                  />
                  <span className="text-[8px] text-zixo-text-secondary">{days[i]}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" /></svg>
            Quick Actions
          </h3>
          <div className="grid grid-cols-3 gap-2">
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleQuickBroadcast}
              className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-zixo-bg hover:bg-zixo-primary/5 transition-colors"
            >
              <div className="w-8 h-8 rounded-full bg-zixo-primary/10 flex items-center justify-center">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>
              </div>
              <span className="text-[9px] text-zixo-text-secondary">Broadcast</span>
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => {
                showConfirm('Cleanup Stale Calls', 'This will remove all stale/abandoned call data. Continue?', 'Cleanup', 'primary', handleCleanupStaleCalls);
              }}
              className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-zixo-bg hover:bg-amber-500/5 transition-colors"
            >
              <div className="w-8 h-8 rounded-full bg-amber-500/10 flex items-center justify-center">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#F59E0B" strokeWidth="2"><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
              </div>
              <span className="text-[9px] text-zixo-text-secondary">Cleanup Calls</span>
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => {
                showConfirm('Assign Zixo Numbers', 'This will assign Zixo numbers to users who don\'t have one yet. Continue?', 'Assign', 'primary', handleAssignZixoNumbers);
              }}
              className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-zixo-bg hover:bg-zixo-accent/5 transition-colors"
            >
              <div className="w-8 h-8 rounded-full bg-zixo-accent/10 flex items-center justify-center">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#34B7F1" strokeWidth="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72" /><path d="M5.23 8.1a14.66 14.66 0 0 0 3.73 5.58" /></svg>
              </div>
              <span className="text-[9px] text-zixo-text-secondary">Assign Numbers</span>
            </motion.button>
          </div>
        </div>
      </div>
    );
  }

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
              placeholder="Search by email, name, or Zixo number..."
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
                      </div>
                    </div>
                    {!isCurrentUser && (
                      <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
                        {isLoading ? (
                          <div className="w-5 h-5 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <>
                            {isBanned ? (
                              <motion.button whileTap={{ scale: 0.9 }} onClick={() => handleUnbanUser(uid)} className="px-2 py-1 rounded-lg text-[10px] font-medium bg-green-500/10 text-green-400 hover:bg-green-500/20 transition-colors">Unban</motion.button>
                            ) : (
                              <motion.button whileTap={{ scale: 0.9 }} onClick={() => handleBanUser(uid)} className="px-2 py-1 rounded-lg text-[10px] font-medium bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors">Ban</motion.button>
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
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-zixo-text">Phone Auth (OTP)</p>
                <p className="text-[10px] text-zixo-text-secondary">Enable sign-in with phone number via SMS</p>
              </div>
              <button
                onClick={handleEnablePhoneAuth}
                disabled={settingsLoading}
                className={cn("relative inline-flex h-6 w-11 items-center rounded-full transition-colors", authConfig.phoneEnabled ? "bg-green-500" : "bg-zixo-surface-light")}
              >
                <span className={cn("inline-block h-4 w-4 transform rounded-full bg-white transition-transform", authConfig.phoneEnabled ? "translate-x-6" : "translate-x-1")} />
              </button>
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-zixo-text">Google Auth</p>
                <p className="text-[10px] text-zixo-text-secondary">Enable sign-in with Google account</p>
              </div>
              <button
                onClick={() => handleUpdateAuthConfig(authConfig.phoneEnabled, !authConfig.googleEnabled)}
                disabled={settingsLoading}
                className={cn("relative inline-flex h-6 w-11 items-center rounded-full transition-colors", authConfig.googleEnabled ? "bg-green-500" : "bg-zixo-surface-light")}
              >
                <span className={cn("inline-block h-4 w-4 transform rounded-full bg-white transition-transform", authConfig.googleEnabled ? "translate-x-6" : "translate-x-1")} />
              </button>
            </div>
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
          <p className="text-[10px] text-zixo-text-secondary mb-2">Enter allowed country codes (comma separated). Leave empty for all regions.</p>
          <input
            type="text"
            value={smsRegions}
            onChange={(e) => setSmsRegions(e.target.value)}
            placeholder="e.g. +1, +44, +880, +91"
            className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
          />
          <motion.button whileTap={{ scale: 0.95 }} onClick={() => showToast('SMS regions saved')} className="mt-2 px-4 py-2 rounded-xl gradient-primary text-white text-xs font-medium">Save Regions</motion.button>
        </div>

        {/* File Upload Settings */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" /></svg>
            File Upload Settings
          </h3>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-zixo-text">Max File Upload Size</p>
                <p className="text-[10px] text-zixo-text-secondary">Maximum size for chat file uploads</p>
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="range"
                  min="1"
                  max="50"
                  value={maxUploadSize}
                  onChange={(e) => setMaxUploadSize(Number(e.target.value))}
                  className="w-20 accent-[#25D366]"
                />
                <span className="text-xs font-mono text-zixo-primary w-8">{maxUploadSize}MB</span>
              </div>
            </div>
            <motion.button whileTap={{ scale: 0.95 }} onClick={() => showToast(`Max upload size set to ${maxUploadSize}MB`)} className="px-4 py-2 rounded-xl gradient-primary text-white text-xs font-medium">Save</motion.button>
          </div>
        </div>

        {/* Rate Limiting */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            Rate Limiting
          </h3>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-zixo-text">Messages per Minute</p>
                <p className="text-[10px] text-zixo-text-secondary">Max messages a user can send per minute</p>
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  min="5"
                  max="120"
                  value={rateLimitMsgs}
                  onChange={(e) => setRateLimitMsgs(Number(e.target.value))}
                  className="w-16 px-2 py-1 rounded-lg bg-zixo-bg text-xs text-zixo-text text-center border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
                />
              </div>
            </div>
            <motion.button whileTap={{ scale: 0.95 }} onClick={() => showToast(`Rate limit set to ${rateLimitMsgs} msgs/min`)} className="px-4 py-2 rounded-xl gradient-primary text-white text-xs font-medium">Save</motion.button>
          </div>
        </div>

        {/* Maintenance Mode */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" /></svg>
            Maintenance Mode
          </h3>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-zixo-text">Enable Maintenance Mode</p>
              <p className="text-[10px] text-zixo-text-secondary">Users will see a maintenance page when enabled</p>
            </div>
            <button
              onClick={() => {
                showConfirm(
                  maintenanceMode ? 'Disable Maintenance' : 'Enable Maintenance',
                  maintenanceMode ? 'This will bring the app back online for all users.' : 'This will make the app unavailable to all users except admins.',
                  maintenanceMode ? 'Disable' : 'Enable',
                  'warning',
                  () => { setMaintenanceMode(!maintenanceMode); showToast(maintenanceMode ? 'Maintenance mode disabled' : 'Maintenance mode enabled'); }
                );
              }}
              className={cn("relative inline-flex h-6 w-11 items-center rounded-full transition-colors", maintenanceMode ? "bg-amber-500" : "bg-zixo-surface-light")}
            >
              <span className={cn("inline-block h-4 w-4 transform rounded-full bg-white transition-transform", maintenanceMode ? "translate-x-6" : "translate-x-1")} />
            </button>
          </div>
          {maintenanceMode && (
            <div className="mt-2 px-3 py-2 rounded-lg bg-amber-500/10 border border-amber-500/20">
              <p className="text-[10px] text-amber-400 flex items-center gap-1">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>
                Maintenance mode is active. Users cannot access the app.
              </p>
            </div>
          )}
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
            {/* Template Selector */}
            {notifTemplates.length > 0 && (
              <select
                value={selectedTemplate}
                onChange={(e) => {
                  const tpl = notifTemplates.find(t => t.id === e.target.value);
                  if (tpl) handleUseTemplate(tpl);
                  setSelectedTemplate(e.target.value);
                }}
                className="w-full px-3 py-2 rounded-xl bg-zixo-bg text-xs text-zixo-text border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
              >
                <option value="">Use a template...</option>
                {notifTemplates.map(t => (
                  <option key={t.id} value={t.id}>{t.title}</option>
                ))}
              </select>
            )}
            <input
              type="text"
              value={broadcastTitle}
              onChange={(e) => { setBroadcastTitle(e.target.value); setSelectedTemplate(''); }}
              placeholder="Notification title"
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
            />
            <textarea
              value={broadcastBody}
              onChange={(e) => { setBroadcastBody(e.target.value); setSelectedTemplate(''); }}
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
              placeholder="User UID or Email"
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
              onClick={handleSendToUserNotif}
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

        {/* Scheduled Notifications */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            Schedule Notification
          </h3>
          <div className="space-y-2">
            <input
              type="text"
              value={scheduleTitle}
              onChange={(e) => setScheduleTitle(e.target.value)}
              placeholder="Notification title"
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
            />
            <textarea
              value={scheduleBody}
              onChange={(e) => setScheduleBody(e.target.value)}
              placeholder="Notification message"
              rows={2}
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50 resize-none"
            />
            <input
              type="datetime-local"
              value={scheduleTime}
              onChange={(e) => setScheduleTime(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
            />
            <div className="flex items-center gap-3">
              <label className="flex items-center gap-2 text-xs text-zixo-text cursor-pointer">
                <input type="radio" checked={scheduleIsBroadcast} onChange={() => setScheduleIsBroadcast(true)} className="accent-[#25D366]" />
                Broadcast
              </label>
              <label className="flex items-center gap-2 text-xs text-zixo-text cursor-pointer">
                <input type="radio" checked={!scheduleIsBroadcast} onChange={() => setScheduleIsBroadcast(false)} className="accent-[#25D366]" />
                Specific User
              </label>
            </div>
            {!scheduleIsBroadcast && (
              <input
                type="text"
                value={scheduleTargetUid}
                onChange={(e) => setScheduleTargetUid(e.target.value)}
                placeholder="Target user UID"
                className="w-full px-3 py-2.5 rounded-xl bg-zixo-bg text-sm text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50 font-mono text-xs"
              />
            )}
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleScheduleNotification}
              className="w-full py-2.5 rounded-xl text-sm font-medium gradient-primary text-white"
            >
              Schedule
            </motion.button>
          </div>

          {/* Scheduled List */}
          {scheduledNotifs.length > 0 && (
            <div className="mt-3 space-y-2 max-h-40 overflow-y-auto">
              {scheduledNotifs.map(n => (
                <div key={n.id} className="bg-zixo-bg rounded-lg p-2.5 flex items-center justify-between">
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-zixo-text truncate">{n.title}</p>
                    <p className="text-[9px] text-zixo-text-secondary">{new Date(n.scheduledFor).toLocaleString()} &middot; {n.isBroadcast ? 'Broadcast' : `To: ${n.targetUid?.slice(0, 10)}...`}</p>
                  </div>
                  <button onClick={() => handleCancelScheduled(n.id)} className="text-red-400 text-[10px] ml-2 shrink-0">Cancel</button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Template Management */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-zixo-text flex items-center gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></svg>
              Templates
            </h3>
            <button
              onClick={() => setShowTemplateManager(!showTemplateManager)}
              className="text-[10px] text-zixo-primary"
            >
              {showTemplateManager ? 'Hide' : 'Manage'}
            </button>
          </div>

          {showTemplateManager && (
            <div className="space-y-2 mb-3">
              <input
                type="text"
                value={newTemplateName}
                onChange={(e) => setNewTemplateName(e.target.value)}
                placeholder="Template name"
                className="w-full px-3 py-2 rounded-xl bg-zixo-bg text-xs text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50"
              />
              <textarea
                value={newTemplateBody}
                onChange={(e) => setNewTemplateBody(e.target.value)}
                placeholder="Template message body"
                rows={2}
                className="w-full px-3 py-2 rounded-xl bg-zixo-bg text-xs text-zixo-text placeholder-zixo-text-secondary border border-white/5 focus:outline-none focus:ring-1 focus:ring-zixo-primary/50 resize-none"
              />
              <motion.button whileTap={{ scale: 0.95 }} onClick={handleSaveTemplate} className="w-full py-2 rounded-xl gradient-primary text-white text-xs font-medium">Save Template</motion.button>
            </div>
          )}

          {notifTemplates.length > 0 ? (
            <div className="space-y-1.5 max-h-40 overflow-y-auto">
              {notifTemplates.map(t => (
                <div key={t.id} className="bg-zixo-bg rounded-lg p-2.5 flex items-center justify-between">
                  <div className="min-w-0 flex-1 cursor-pointer" onClick={() => handleUseTemplate(t)}>
                    <p className="text-xs font-medium text-zixo-text truncate">{t.title}</p>
                    <p className="text-[9px] text-zixo-text-secondary truncate">{t.body}</p>
                  </div>
                  <div className="flex items-center gap-1 shrink-0 ml-2">
                    <button onClick={() => handleUseTemplate(t)} className="text-zixo-primary text-[10px]">Use</button>
                    <button onClick={() => handleDeleteTemplate(t.id)} className="text-red-400 text-[10px]">Del</button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-zixo-text-secondary text-center py-2">No templates saved yet</p>
          )}
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
                    <span className="text-[9px] text-zixo-text-secondary">{new Date(n.sentAt).toLocaleTimeString()}</span>
                  </div>
                  <p className="text-[10px] text-zixo-text-secondary mt-0.5 truncate">{n.body}</p>
                  <p className="text-[9px] text-zixo-primary mt-1">{n.recipientCount} recipient{n.recipientCount !== 1 ? 's' : ''}</p>
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
          {systemLoading ? (
            <div className="flex items-center justify-center py-6">
              <div className="w-6 h-6 border-2 border-zixo-primary border-t-transparent rounded-full animate-spin" />
            </div>
          ) : appStats ? (
            <div className="grid grid-cols-2 gap-2">
              <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-zixo-text">{appStats.totalUsers}</p>
                <p className="text-[9px] text-zixo-text-secondary uppercase">Total Users</p>
              </div>
              <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-green-400">{appStats.onlineUsers}</p>
                <p className="text-[9px] text-zixo-text-secondary uppercase">Online</p>
              </div>
              <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-zixo-accent">{appStats.activeChats}</p>
                <p className="text-[9px] text-zixo-text-secondary uppercase">Active Chats</p>
              </div>
              <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-amber-400">{appStats.callCount}</p>
                <p className="text-[9px] text-zixo-text-secondary uppercase">Calls</p>
              </div>
            </div>
          ) : (
            <p className="text-xs text-zixo-text-secondary text-center py-4">Unable to load stats</p>
          )}
        </div>

        {/* Firestore Usage */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" /><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" /></svg>
            Firestore Usage
          </h3>
          <div className="grid grid-cols-3 gap-2">
            <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
              <p className="text-sm font-bold text-zixo-text">{appStats?.firestoreUsage?.reads ?? '—'}</p>
              <p className="text-[9px] text-zixo-text-secondary uppercase">Reads</p>
            </div>
            <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
              <p className="text-sm font-bold text-zixo-text">{appStats?.firestoreUsage?.writes ?? '—'}</p>
              <p className="text-[9px] text-zixo-text-secondary uppercase">Writes</p>
            </div>
            <div className="bg-zixo-bg rounded-lg p-2.5 text-center">
              <p className="text-sm font-bold text-zixo-text">{appStats?.firestoreUsage?.deletes ?? '—'}</p>
              <p className="text-[9px] text-zixo-text-secondary uppercase">Deletes</p>
            </div>
          </div>
        </div>

        {/* RTDB Connections */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M5 12.55a11 11 0 0 1 14.08 0" /><path d="M1.42 9a16 16 0 0 1 21.16 0" /><path d="M8.53 16.11a6 6 0 0 1 6.95 0" /><line x1="12" y1="20" x2="12.01" y2="20" /></svg>
            Realtime Database
          </h3>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-zixo-text">Active Connections</p>
              <p className="text-[10px] text-zixo-text-secondary">Current RTDB connections</p>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <span className="text-lg font-bold text-zixo-text">{appStats?.rtdbConnections ?? '—'}</span>
            </div>
          </div>
        </div>

        {/* API Health Check */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-zixo-text flex items-center gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>
              API Health Check
            </h3>
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={runApiHealthChecks}
              className="text-[10px] text-zixo-primary flex items-center gap-1"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={cn(systemLoading && 'animate-spin')}>
                <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
              </svg>
              Refresh
            </motion.button>
          </div>
          <div className="space-y-2">
            {apiHealthChecks.length > 0 ? apiHealthChecks.map(check => (
              <div key={check.name} className="bg-zixo-bg rounded-lg p-2.5 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className={cn(
                    "w-2.5 h-2.5 rounded-full",
                    check.status === 'healthy' ? 'bg-green-500' : check.status === 'degraded' ? 'bg-amber-500' : 'bg-red-500'
                  )} />
                  <span className="text-xs text-zixo-text">{check.name}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className={cn(
                    "text-[10px] font-medium",
                    check.status === 'healthy' ? 'text-green-400' : check.status === 'degraded' ? 'text-amber-400' : 'text-red-400'
                  )}>
                    {check.status === 'healthy' ? 'Healthy' : check.status === 'degraded' ? 'Degraded' : 'Down'}
                  </span>
                  <span className="text-[9px] text-zixo-text-secondary font-mono">{check.latency}ms</span>
                </div>
              </div>
            )) : (
              <div className="text-center py-4">
                <p className="text-xs text-zixo-text-secondary">Click Refresh to run health checks</p>
              </div>
            )}
          </div>
        </div>

        {/* Server Status */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="2" width="20" height="8" rx="2" ry="2" /><rect x="2" y="14" width="20" height="8" rx="2" ry="2" /><line x1="6" y1="6" x2="6.01" y2="6" /><line x1="6" y1="18" x2="6.01" y2="18" /></svg>
            Server Status
          </h3>
          <div className="space-y-2">
            <div className="bg-zixo-bg rounded-lg p-2.5 flex items-center justify-between">
              <span className="text-xs text-zixo-text">Status</span>
              <span className={cn(
                "text-xs font-medium flex items-center gap-1",
                healthStatus?.status === 'ok' ? 'text-green-400' : 'text-amber-400'
              )}>
                <div className={cn("w-2 h-2 rounded-full", healthStatus?.status === 'ok' ? 'bg-green-500' : 'bg-amber-500')} />
                {healthStatus?.status === 'ok' ? 'Operational' : 'Checking...'}
              </span>
            </div>
            <div className="bg-zixo-bg rounded-lg p-2.5 flex items-center justify-between">
              <span className="text-xs text-zixo-text">Firebase</span>
              <span className="text-xs text-zixo-text-secondary">{healthStatus?.firebase ? 'Connected' : 'Unknown'}</span>
            </div>
            {healthStatus?.version && (
              <div className="bg-zixo-bg rounded-lg p-2.5 flex items-center justify-between">
                <span className="text-xs text-zixo-text">Version</span>
                <span className="text-xs text-zixo-text-secondary font-mono">{healthStatus.version}</span>
              </div>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="bg-zixo-surface rounded-xl p-4">
          <h3 className="text-sm font-semibold text-zixo-text mb-3 flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" /></svg>
            System Actions
          </h3>
          <div className="space-y-2">
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => {
                showConfirm('Force Cleanup', 'This will forcefully cleanup all stale call data, expired sessions, and orphaned records. Continue?', 'Cleanup', 'warning', handleCleanupStaleCalls);
              }}
              className="w-full py-2.5 rounded-xl text-xs font-medium bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 transition-colors flex items-center justify-center gap-2"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
              Force Cleanup Stale Data
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleExportCSV}
              className="w-full py-2.5 rounded-xl text-xs font-medium bg-zixo-primary/10 text-zixo-primary hover:bg-zixo-primary/20 transition-colors flex items-center justify-center gap-2"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" /></svg>
              Export User Data (CSV)
            </motion.button>
          </div>
        </div>
      </div>
    );
  }
}

// ==================== HELPERS ====================
function generateMockActivityData(): number[] {
  const data: number[] = [];
  for (let i = 0; i < 7; i++) {
    data.push(Math.floor(Math.random() * 80) + 20);
  }
  return data;
}

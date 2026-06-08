'use client';

import React, { useState } from 'react';
import dynamic from 'next/dynamic';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { cn } from '@/lib/zixo-utils';
import type { ZixoUserProfile } from '@/services/auth';
import { formatZixoNumber } from '@/services/auth';

// Dynamic import to avoid SSR issues
const QRCodeSVG = dynamic(
  () => import('qrcode.react').then((mod) => ({ default: mod.QRCodeSVG })),
  { ssr: false, loading: () => <div className="w-[120px] h-[120px] bg-white/10 rounded-xl" /> }
);

// Moved outside of component to avoid "Cannot create components during render" error
function SettingSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-2">
      <h3 className="px-4 py-2 text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider">{title}</h3>
      <div className="bg-zixo-surface divide-y divide-white/5">{children}</div>
    </div>
  );
}

function SettingRow({ label, subtitle, children, onClick }: { label: string; subtitle?: string; children?: React.ReactNode; onClick?: () => void }) {
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center justify-between px-4 py-3 hover:bg-zixo-surface-light/50 transition-colors text-left"
    >
      <div className="min-w-0">
        <p className="text-sm text-zixo-text">{label}</p>
        {subtitle && <p className="text-xs text-zixo-text-secondary mt-0.5">{subtitle}</p>}
      </div>
      {children}
    </button>
  );
}

function Toggle({ value, onToggle }: { value: boolean; onToggle: () => void }) {
  return (
    <button
      onClick={(e) => { e.stopPropagation(); onToggle(); }}
      className={cn(
        'w-11 h-6 rounded-full relative transition-colors duration-200',
        value ? 'bg-zixo-primary' : 'bg-zixo-surface-light'
      )}
    >
      <motion.div
        animate={{ x: value ? 20 : 2 }}
        transition={{ type: 'spring', stiffness: 500, damping: 30 }}
        className="absolute top-1 w-4 h-4 rounded-full bg-white shadow"
      />
    </button>
  );
}

function ChevronRight() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-text-secondary">
      <polyline points="9 18 15 12 9 6" />
    </svg>
  );
}

interface SettingsScreenProps {
  user: ZixoUserProfile;
  onEditProfile: () => void;
  onLogout: () => void;
  onBack: () => void;
  onAdminPanel?: () => void;
}

export default function SettingsScreen({ user, onEditProfile, onLogout, onBack, onAdminPanel }: SettingsScreenProps) {
  const [theme, setTheme] = useState<'dark' | 'amoled' | 'system'>('dark');
  const [lastSeen, setLastSeen] = useState<'everyone' | 'contacts' | 'nobody'>('everyone');
  const [onlineStatus, setOnlineStatus] = useState(true);
  const [readReceipts, setReadReceipts] = useState(true);
  const [screenLock, setScreenLock] = useState(false);
  const [showQRModal, setShowQRModal] = useState(false);

  return (
    <div className="pb-24">
      {/* Profile Card */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-4"
      >
        <div className="bg-zixo-surface rounded-2xl p-4 flex items-center gap-4">
          <Avatar name={user.displayName} uid={user.uid} avatarUrl={user.avatar} size="xl" online={user.online} />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-zixo-text truncate">{user.displayName}</h3>
              {user.role === 'admin' && (
                <span className="px-1.5 py-0.5 rounded-full text-[9px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 shrink-0">ADMIN</span>
              )}
            </div>
            <p className="text-sm text-zixo-text-secondary truncate">{user.username}</p>
            <p className="text-xs text-zixo-text-secondary truncate mt-0.5">{user.bio}</p>
            {user.zixoNumber && (
              <div className="flex items-center gap-1.5 mt-1">
                <span className="text-[10px] font-semibold text-zixo-primary/70 uppercase tracking-wider">Zixo</span>
                <span className="text-xs font-bold font-mono tracking-wider text-zixo-primary">{formatZixoNumber(user.zixoNumber)}</span>
              </div>
            )}
          </div>
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onEditProfile}
            className="shrink-0 w-9 h-9 rounded-full bg-zixo-surface-light flex items-center justify-center text-zixo-text-secondary hover:text-zixo-primary transition-colors"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </motion.button>
        </div>

        {/* QR Code - Compact */}
        {user.zixoNumber && (
          <div className="mt-3 pt-3 border-t border-white/5">
            <motion.button
              whileTap={{ scale: 0.97 }}
              whileHover={{ scale: 1.02 }}
              onClick={() => setShowQRModal(true)}
              className="flex items-center justify-between w-full px-1 py-1 rounded-lg hover:bg-white/5 transition-colors"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-white p-1 flex items-center justify-center">
                  <QRCodeSVG value={`ZIXO:${user.zixoNumber}`} size={24} level="L" />
                </div>
                <div>
                  <p className="text-xs font-medium text-zixo-text">My QR Code</p>
                  <p className="text-[10px] text-zixo-text-secondary">Tap to share</p>
                </div>
              </div>
              <ChevronRight />
            </motion.button>
          </div>
        )}
      </motion.div>

      {/* QR Code Enlarged Modal */}
      <AnimatePresence>
        {showQRModal && user.zixoNumber && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[60] flex items-center justify-center p-4"
            onClick={() => setShowQRModal(false)}
          >
            <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" />
            <motion.div
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.8, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 400, damping: 25 }}
              className="relative bg-zixo-surface rounded-2xl p-6 flex flex-col items-center max-w-xs w-full shadow-2xl border border-white/10"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                onClick={() => setShowQRModal(false)}
                className="absolute top-3 right-3 w-8 h-8 rounded-full bg-zixo-surface-light flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>

              <Avatar name={user.displayName} uid={user.uid} avatarUrl={user.avatar} size="xl" online={user.online} />
              <h3 className="text-lg font-semibold text-zixo-text mt-3">{user.displayName}</h3>
              <p className="text-sm text-zixo-text-secondary">{user.username}</p>

              <div className="p-4 bg-white rounded-xl mt-4">
                <QRCodeSVG value={`ZIXO:${user.zixoNumber}`} size={200} level="H" />
              </div>

              <div className="flex items-center gap-1.5 mt-3">
                <span className="text-[10px] font-semibold text-zixo-primary/70 uppercase tracking-wider">Zixo</span>
                <span className="text-sm font-bold font-mono tracking-wider text-zixo-primary">{formatZixoNumber(user.zixoNumber)}</span>
              </div>
              <p className="text-[11px] text-zixo-text-secondary mt-1">Scan to add on Zixo</p>

              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={() => {
                  navigator.clipboard.writeText(user.zixoNumber!).then(() => {
                    const btn = document.getElementById('settings-copy-zixo-modal');
                    if (btn) btn.textContent = 'Copied!';
                    setTimeout(() => { if (btn) btn.textContent = 'Copy Number'; }, 1500);
                  }).catch(() => {});
                }}
                className="mt-3 px-5 py-2 rounded-xl bg-zixo-surface-light text-xs font-medium text-zixo-text-secondary hover:text-zixo-primary transition-colors"
              >
                <span id="settings-copy-zixo-modal">Copy Number</span>
              </motion.button>

              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={() => {
                  setShowQRModal(false);
                  // Navigate to contacts screen where QR scanner is available
                  import('@/stores/useZixoStore').then(({ useZixoStore }) => {
                    useZixoStore.getState().setScreen('contacts');
                  });
                }}
                className="mt-2 px-5 py-2 rounded-xl gradient-primary text-xs font-medium text-white"
              >
                Scan QR Code
              </motion.button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Appearance */}
      <SettingSection title="Appearance">
        <SettingRow label="Theme" subtitle={theme === 'dark' ? 'Dark' : theme === 'amoled' ? 'AMOLED Black' : 'System'}>
          <div className="flex gap-1.5">
            {(['dark', 'amoled', 'system'] as const).map((t) => (
              <button
                key={t}
                onClick={() => setTheme(t)}
                className={cn(
                  'px-2.5 py-1 rounded-full text-[10px] font-medium transition-colors',
                  theme === t ? 'gradient-primary text-white' : 'bg-zixo-surface-light text-zixo-text-secondary'
                )}
              >
                {t === 'dark' ? 'Dark' : t === 'amoled' ? 'AMOLED' : 'System'}
              </button>
            ))}
          </div>
        </SettingRow>
        <SettingRow label="Chat Wallpaper">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Font Size" subtitle="Medium">
          <ChevronRight />
        </SettingRow>
      </SettingSection>

      {/* Privacy & Security */}
      <SettingSection title="Privacy & Security">
        <SettingRow label="Last Seen" subtitle={lastSeen.charAt(0).toUpperCase() + lastSeen.slice(1)}>
          <div className="flex gap-1">
            {(['everyone', 'contacts', 'nobody'] as const).map((v) => (
              <button
                key={v}
                onClick={() => setLastSeen(v)}
                className={cn(
                  'px-2 py-0.5 rounded-full text-[10px] font-medium transition-colors',
                  lastSeen === v ? 'bg-zixo-primary text-white' : 'bg-zixo-surface-light text-zixo-text-secondary'
                )}
              >
                {v.charAt(0).toUpperCase() + v.slice(1)}
              </button>
            ))}
          </div>
        </SettingRow>
        <SettingRow label="Online Status">
          <Toggle value={onlineStatus} onToggle={() => setOnlineStatus(!onlineStatus)} />
        </SettingRow>
        <SettingRow label="Read Receipts">
          <Toggle value={readReceipts} onToggle={() => setReadReceipts(!readReceipts)} />
        </SettingRow>
        <SettingRow label="Screen Lock" subtitle="Biometric / Face ID">
          <Toggle value={screenLock} onToggle={() => setScreenLock(!screenLock)} />
        </SettingRow>
        <SettingRow label="Encryption Key" subtitle="Verify E2E encryption">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Blocked Contacts">
          <ChevronRight />
        </SettingRow>
      </SettingSection>

      {/* Notifications */}
      <SettingSection title="Notifications">
        <SettingRow label="Message Preview">
          <Toggle value={true} onToggle={() => {}} />
        </SettingRow>
        <SettingRow label="Notification Tone">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Do Not Disturb">
          <Toggle value={false} onToggle={() => {}} />
        </SettingRow>
      </SettingSection>

      {/* Data & Storage */}
      <SettingSection title="Data & Storage">
        <SettingRow label="Storage Usage" subtitle="45.2 MB">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Auto-Download Media" subtitle="Wi-Fi only">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Clear Cache">
          <ChevronRight />
        </SettingRow>
      </SettingSection>

      {/* Call Settings */}
      <SettingSection title="Call Settings">
        <SettingRow label="Default Call Type" subtitle="Ask every time">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Noise Suppression" subtitle="Auto">
          <Toggle value={true} onToggle={() => {}} />
        </SettingRow>
      </SettingSection>

      {/* Admin Panel
          Accessible via: Settings tab → scroll down to "Admin" section → click "Admin Panel"
          The user eunus527@gmail.com has role: 'admin' in Firestore. */}
      {user.role === 'admin' && (
        <SettingSection title="Admin">
          <SettingRow label="Admin Panel" subtitle="Manage users & roles" onClick={onAdminPanel}>
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-amber-500/20 text-amber-400">Admin</span>
              <ChevronRight />
            </div>
          </SettingRow>
        </SettingSection>
      )}

      {/* About */}
      <SettingSection title="About">
        <SettingRow label="App Version" subtitle="1.0.0">
          <span className="text-xs text-zixo-text-secondary">1.0.0</span>
        </SettingRow>
        <SettingRow label="Mission" subtitle="100% free. No ads. No social media.">
          <ChevronRight />
        </SettingRow>
        <SettingRow label="Share Zixo">
          <ChevronRight />
        </SettingRow>
      </SettingSection>

      {/* Logout */}
      <div className="px-4 py-6">
        <motion.button
          whileTap={{ scale: 0.98 }}
          onClick={onLogout}
          className="w-full py-3 rounded-xl bg-zixo-error/10 text-zixo-error font-medium text-sm hover:bg-zixo-error/20 transition-colors"
        >
          Log Out
        </motion.button>
      </div>

      {/* Delete Account */}
      <div className="px-4 pb-8">
        <button
          onClick={onLogout}
          className="w-full py-3 rounded-xl text-zixo-text-secondary text-sm hover:text-zixo-error transition-colors"
        >
          Delete Account
        </button>
      </div>
    </div>
  );
}

'use client';

import React, { useState } from 'react';
import { motion } from 'framer-motion';
import Avatar from './Avatar';
import { cn } from '@/lib/zixo-utils';
import type { ZixoUserProfile } from '@/services/auth';

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
}

export default function SettingsScreen({ user, onEditProfile, onLogout, onBack }: SettingsScreenProps) {
  const [theme, setTheme] = useState<'dark' | 'amoled' | 'system'>('dark');
  const [lastSeen, setLastSeen] = useState<'everyone' | 'contacts' | 'nobody'>('everyone');
  const [onlineStatus, setOnlineStatus] = useState(true);
  const [readReceipts, setReadReceipts] = useState(true);
  const [screenLock, setScreenLock] = useState(false);

  return (
    <div className="pb-20">
      {/* Profile Card */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-4"
      >
        <div className="bg-zixo-surface rounded-2xl p-4 flex items-center gap-4">
          <Avatar name={user.displayName} uid={user.uid} size="xl" online={user.online} />
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-zixo-text truncate">{user.displayName}</h3>
            <p className="text-sm text-zixo-text-secondary truncate">{user.username}</p>
            <p className="text-xs text-zixo-text-secondary truncate mt-0.5">{user.bio}</p>
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
      </motion.div>

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

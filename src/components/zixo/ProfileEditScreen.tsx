'use client';

import React, { useState, useRef, useCallback } from 'react';
import dynamic from 'next/dynamic';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { cn } from '@/lib/zixo-utils';
import { updateUserProfile, formatZixoNumber } from '@/services/auth';
import { uploadAvatar } from '@/services/storage';
import { useZixoStore } from '@/stores/useZixoStore';
import type { ZixoUserProfile } from '@/services/auth';

// Dynamic import to avoid SSR issues
const QRCodeSVG = dynamic(
  () => import('qrcode.react').then((mod) => ({ default: mod.QRCodeSVG })),
  { ssr: false, loading: () => <div className="w-[80px] h-[80px] bg-white/10 rounded-lg" /> }
);

// ==================== TYPES ====================

interface ProfileEditScreenProps {
  user: ZixoUserProfile;
  onBack: () => void;
  onSave: (updates: Partial<Pick<ZixoUserProfile, 'displayName' | 'bio' | 'avatar' | 'username'>>) => Promise<void>;
}

// ==================== CONSTANTS ====================

const DISPLAY_NAME_MAX = 30;
const BIO_MAX = 100;

// ==================== ICONS ====================

function BackIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="15 18 9 12 15 6" />
    </svg>
  );
}

function CameraIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
      <circle cx="12" cy="13" r="4" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}

function SpinnerIcon() {
  return (
    <svg className="animate-spin" width="20" height="20" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" className="opacity-25" />
      <path d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="3" strokeLinecap="round" className="opacity-75" />
    </svg>
  );
}

function LockIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-zixo-text-secondary/50">
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0110 0v4" />
    </svg>
  );
}

// ==================== COMPONENT ====================

export default function ProfileEditScreen({ user, onBack, onSave }: ProfileEditScreenProps) {
  // Form state
  const [displayName, setDisplayName] = useState(user.displayName);
  const [bio, setBio] = useState(user.bio);
  const [avatarUrl, setAvatarUrl] = useState(user.avatar);
  const [isSaving, setIsSaving] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showQRModal, setShowQRModal] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Derived state
  const displayNameCount = displayName.length;
  const bioCount = bio.length;
  const isDisplayNameValid = displayName.trim().length > 0 && displayName.length <= DISPLAY_NAME_MAX;
  const isBioValid = bio.length <= BIO_MAX;
  const hasChanges =
    displayName !== user.displayName ||
    bio !== user.bio ||
    avatarUrl !== user.avatar;

  const canSave = isDisplayNameValid && isBioValid && hasChanges && !isSaving;

  // Handle avatar file selection
  const handleAvatarChange = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setError('Please select an image file');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be less than 5MB');
      return;
    }

    setError(null);
    setAvatarUploading(true);

    try {
      const url = await uploadAvatar(user.uid, file, (progress) => {
        // Track upload progress silently — could show in UI if desired
        if (progress.state === 'error') {
          setError(progress.error || 'Avatar upload failed');
        }
      });
      setAvatarUrl(url);
    } catch (err) {
      console.error('[Zixo] Avatar upload failed:', err);
      setError('Failed to upload avatar. Please try again.');
    } finally {
      setAvatarUploading(false);
      // Reset file input so the same file can be selected again
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }, [user.uid]);

  // Handle save
  const handleSave = useCallback(async () => {
    if (!canSave) return;

    setIsSaving(true);
    setError(null);

    const updates: Partial<Pick<ZixoUserProfile, 'displayName' | 'bio' | 'avatar' | 'username'>> = {};
    if (displayName !== user.displayName) updates.displayName = displayName.trim();
    if (bio !== user.bio) updates.bio = bio;
    if (avatarUrl !== user.avatar) updates.avatar = avatarUrl;

    try {
      // Update Firestore via auth service
      await updateUserProfile(user.uid, updates);

      // Update local Zustand store — use both mechanisms to guarantee update
      const store = useZixoStore.getState();
      store.updateUserProfile(updates);
      // Also explicitly set currentUser to guarantee the home page reflects changes
      if (store.currentUser) {
        useZixoStore.setState({
          currentUser: { ...store.currentUser, ...updates },
          _profileUpdatedAt: Date.now(),
        });
      }

      // Call the parent onSave
      await onSave(updates);

      // Show success feedback
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
    } catch (err) {
      console.error('[Zixo] Profile update failed:', err);
      setError('Failed to save changes. Please try again.');
    } finally {
      setIsSaving(false);
    }
  }, [canSave, displayName, bio, avatarUrl, user, onSave]);

  return (
    <div className="h-screen flex flex-col bg-zixo-bg">
      {/* Header */}
      <div className="shrink-0 bg-[#1F2C34] safe-area-top">
        <div className="flex items-center gap-3 px-4 py-3">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onBack}
            className="shrink-0 w-9 h-9 rounded-full flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text hover:bg-zixo-surface-light transition-colors"
          >
            <BackIcon />
          </motion.button>
          <h2 className="text-lg font-semibold font-heading text-zixo-text">Edit Profile</h2>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="px-4 py-6"
        >
          {/* Avatar Section */}
          <div className="flex flex-col items-center mb-8">
            <motion.div
              whileTap={{ scale: 0.95 }}
              className="relative group cursor-pointer"
              onClick={() => fileInputRef.current?.click()}
            >
              {/* Avatar with optional image override */}
              <div className="relative">
                {avatarUrl ? (
                  <div className="w-24 h-24 rounded-full overflow-hidden ring-2 ring-zixo-primary/30">
                    <img
                      src={avatarUrl}
                      alt={displayName}
                      className="w-full h-full object-cover"
                    />
                  </div>
                ) : (
                  <Avatar name={displayName || user.displayName} uid={user.uid} size="2xl" />
                )}

                {/* Camera overlay */}
                <div className={cn(
                  'absolute inset-0 rounded-full flex items-center justify-center transition-all duration-200',
                  'bg-black/40 opacity-0 group-hover:opacity-100',
                  avatarUploading && 'opacity-100'
                )}>
                  {avatarUploading ? (
                    <SpinnerIcon />
                  ) : (
                    <div className="flex flex-col items-center gap-0.5">
                      <CameraIcon />
                      <span className="text-[9px] text-white font-medium">Change</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Green ring pulse when not hovered (subtle affordance) */}
              <div className="absolute -inset-1 rounded-full border-2 border-zixo-primary/20 pointer-events-none" />
            </motion.div>

            <p className="text-xs text-zixo-text-secondary mt-3">
              Tap to change profile photo
            </p>

            {/* Zixo Number Display with QR Code */}
            {user.zixoNumber && (
              <motion.div
                initial={{ opacity: 0, y: 5 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="mt-4 w-full max-w-[320px]"
              >
                <div className="bg-zixo-surface rounded-xl p-4 border border-zixo-primary/10 relative">
                  <div className="flex items-center gap-4">
                    <div className="flex-1 text-center">
                      <p className="text-[10px] font-semibold text-zixo-text-secondary uppercase tracking-wider mb-1">Your Zixo Number</p>
                      <p className="text-2xl font-extrabold font-mono tracking-[0.12em] text-zixo-primary">
                        {formatZixoNumber(user.zixoNumber)}
                      </p>
                      <motion.button
                        whileTap={{ scale: 0.9 }}
                        onClick={() => {
                          navigator.clipboard.writeText(user.zixoNumber!).then(() => {
                            const btn = document.getElementById('profile-copy-zixo');
                            if (btn) btn.textContent = 'Copied!';
                            setTimeout(() => { if (btn) btn.textContent = 'Copy'; }, 1500);
                          }).catch(() => {});
                        }}
                        className="mt-2 px-2.5 py-1 rounded-lg bg-zixo-surface-light text-[11px] font-medium text-zixo-text-secondary hover:text-zixo-primary transition-colors"
                      >
                        <span id="profile-copy-zixo">Copy</span>
                      </motion.button>
                    </div>
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      onClick={() => setShowQRModal(true)}
                      className="shrink-0 p-2 bg-white rounded-lg cursor-pointer"
                      title="Tap to enlarge QR code"
                    >
                      <QRCodeSVG value={`ZIXO:${user.zixoNumber}`} size={72} level="M" />
                    </motion.button>
                  </div>
                  <p className="text-[10px] text-zixo-text-secondary/60 text-center mt-2">Tap QR to share</p>
                </div>
              </motion.div>
            )}

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

                    <h3 className="text-lg font-semibold text-zixo-text">{user.displayName}</h3>
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
                          const btn = document.getElementById('profile-copy-zixo-modal');
                          if (btn) btn.textContent = 'Copied!';
                          setTimeout(() => { if (btn) btn.textContent = 'Copy Number'; }, 1500);
                        }).catch(() => {});
                      }}
                      className="mt-3 px-5 py-2 rounded-xl bg-zixo-surface-light text-xs font-medium text-zixo-text-secondary hover:text-zixo-primary transition-colors"
                    >
                      <span id="profile-copy-zixo-modal">Copy Number</span>
                    </motion.button>
                  </motion.div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Hidden file input */}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleAvatarChange}
              className="hidden"
              aria-label="Choose profile photo"
            />
          </div>

          {/* Form Fields */}
          <div className="space-y-5">
            {/* Display Name */}
            <div>
              <label className="block text-xs font-medium text-zixo-text-secondary uppercase tracking-wider mb-2 px-1">
                Display Name <span className="text-zixo-error">*</span>
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => {
                    if (e.target.value.length <= DISPLAY_NAME_MAX) {
                      setDisplayName(e.target.value);
                      setError(null);
                    }
                  }}
                  placeholder="Enter your name"
                  maxLength={DISPLAY_NAME_MAX}
                  className={cn(
                    'w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm',
                    'placeholder-zixo-text-secondary/50 border border-transparent',
                    'focus:border-zixo-primary/30 focus:outline-none transition-all duration-200',
                    'hover:border-white/10',
                    !isDisplayNameValid && displayName.length > 0 && 'border-zixo-error/50 focus:border-zixo-error/70'
                  )}
                  aria-label="Display name"
                  autoFocus
                />
              </div>
              <div className="flex justify-between items-center mt-1.5 px-1">
                <span className={cn(
                  'text-[11px]',
                  displayName.trim().length === 0 && displayName.length > 0
                    ? 'text-zixo-error'
                    : 'text-transparent'
                )}>
                  Name is required
                </span>
                <span className={cn(
                  'text-[11px] tabular-nums',
                  displayNameCount > DISPLAY_NAME_MAX - 5
                    ? 'text-zixo-error'
                    : 'text-zixo-text-secondary'
                )}>
                  {displayNameCount}/{DISPLAY_NAME_MAX}
                </span>
              </div>
            </div>

            {/* Username (read-only) */}
            <div>
              <label className="block text-xs font-medium text-zixo-text-secondary uppercase tracking-wider mb-2 px-1">
                Username
              </label>
              <div className="relative">
                <div className="absolute left-4 top-1/2 -translate-y-1/2">
                  <LockIcon />
                </div>
                <input
                  type="text"
                  value={user.username}
                  readOnly
                  className={cn(
                    'w-full pl-9 pr-4 py-3 rounded-xl bg-zixo-surface-light/60 text-zixo-text-secondary text-sm',
                    'border border-transparent cursor-not-allowed opacity-70'
                  )}
                  aria-label="Username (read-only)"
                />
              </div>
              <p className="text-[11px] text-zixo-text-secondary/60 mt-1.5 px-1">
                Username cannot be changed
              </p>
            </div>

            {/* Phone Number (read-only) */}
            <div>
              <label className="block text-xs font-medium text-zixo-text-secondary uppercase tracking-wider mb-2 px-1">
                Phone Number
              </label>
              <div className="relative">
                <div className="absolute left-4 top-1/2 -translate-y-1/2">
                  <LockIcon />
                </div>
                <input
                  type="text"
                  value={user.phoneNumber || 'Not set'}
                  readOnly
                  className={cn(
                    'w-full pl-9 pr-4 py-3 rounded-xl bg-zixo-surface-light/60 text-zixo-text-secondary text-sm',
                    'border border-transparent cursor-not-allowed opacity-70'
                  )}
                  aria-label="Phone number (read-only)"
                />
              </div>
              <p className="text-[11px] text-zixo-text-secondary/60 mt-1.5 px-1">
                Phone number cannot be changed
              </p>
            </div>

            {/* Bio / Status */}
            <div>
              <label className="block text-xs font-medium text-zixo-text-secondary uppercase tracking-wider mb-2 px-1">
                Bio / Status
              </label>
              <textarea
                value={bio}
                onChange={(e) => {
                  if (e.target.value.length <= BIO_MAX) {
                    setBio(e.target.value);
                    setError(null);
                  }
                }}
                placeholder="Hey there! I'm using Zixo"
                maxLength={BIO_MAX}
                rows={3}
                className={cn(
                  'w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm resize-none',
                  'placeholder-zixo-text-secondary/50 border border-transparent',
                  'focus:border-zixo-primary/30 focus:outline-none transition-all duration-200',
                  'hover:border-white/10',
                  !isBioValid && 'border-zixo-error/50 focus:border-zixo-error/70'
                )}
                aria-label="Bio or status message"
              />
              <div className="flex justify-end mt-1.5 px-1">
                <span className={cn(
                  'text-[11px] tabular-nums',
                  bioCount > BIO_MAX - 15
                    ? 'text-zixo-error'
                    : 'text-zixo-text-secondary'
                )}>
                  {bioCount}/{BIO_MAX}
                </span>
              </div>
            </div>
          </div>

          {/* Error Message */}
          <AnimatePresence>
            {error && (
              <motion.div
                initial={{ opacity: 0, y: -5, height: 0 }}
                animate={{ opacity: 1, y: 0, height: 'auto' }}
                exit={{ opacity: 0, y: -5, height: 0 }}
                className="mt-4"
              >
                <div className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-zixo-error/10 border border-zixo-error/20">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-zixo-error shrink-0">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                  <p className="text-xs text-zixo-error">{error}</p>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Save Button */}
          <div className="mt-8">
            <motion.button
              whileTap={canSave ? { scale: 0.98 } : {}}
              onClick={handleSave}
              disabled={!canSave}
              className={cn(
                'w-full py-3.5 rounded-xl font-semibold text-sm transition-all duration-200 flex items-center justify-center gap-2',
                canSave
                  ? 'gradient-primary text-white shadow-lg shadow-zixo-primary/20 hover:shadow-zixo-primary/30'
                  : 'bg-zixo-surface-light text-zixo-text-secondary cursor-not-allowed'
              )}
            >
              <AnimatePresence mode="wait">
                {isSaving ? (
                  <motion.div
                    key="saving"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="flex items-center gap-2"
                  >
                    <SpinnerIcon />
                    <span>Saving...</span>
                  </motion.div>
                ) : showSuccess ? (
                  <motion.div
                    key="success"
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.8 }}
                    className="flex items-center gap-2"
                  >
                    <CheckIcon />
                    <span>Saved!</span>
                  </motion.div>
                ) : (
                  <motion.span
                    key="save"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                  >
                    Save Changes
                  </motion.span>
                )}
              </AnimatePresence>
            </motion.button>
          </div>

          {/* Success Toast Overlay */}
          <AnimatePresence>
            {showSuccess && (
              <motion.div
                initial={{ opacity: 0, y: 50, scale: 0.9 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 20, scale: 0.95 }}
                transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                className="fixed bottom-24 left-1/2 -translate-x-1/2 z-50"
              >
                <div className="flex items-center gap-2 px-5 py-3 rounded-2xl glass-strong shadow-xl">
                  <div className="w-6 h-6 rounded-full gradient-primary flex items-center justify-center">
                    <CheckIcon />
                  </div>
                  <span className="text-sm font-medium text-zixo-text">Profile updated successfully</span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Bottom spacing for safe area */}
          <div className="h-8" />
        </motion.div>
      </div>
    </div>
  );
}

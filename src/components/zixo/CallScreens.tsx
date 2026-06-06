'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { OnlineStatus, EncryptionBadge } from './Common';
import { cn } from '@/lib/zixo-utils';
import type { ZixoUserProfile } from '@/services/auth';

interface AudioCallScreenProps {
  remoteUser: ZixoUserProfile;
  callStatus: 'ringing' | 'connecting' | 'connected' | 'ended';
  duration: number;
  isMuted: boolean;
  isSpeakerOn: boolean;
  onToggleMute: () => void;
  onToggleSpeaker: () => void;
  onEndCall: () => void;
  onAnswer?: () => void;
  onDecline?: () => void;
  isIncoming?: boolean;
}

export function AudioCallScreen({
  remoteUser,
  callStatus,
  duration,
  isMuted,
  isSpeakerOn,
  onToggleMute,
  onToggleSpeaker,
  onEndCall,
  onAnswer,
  onDecline,
  isIncoming = false,
}: AudioCallScreenProps) {
  const [callDuration, setCallDuration] = useState(duration);

  useEffect(() => {
    if (callStatus === 'connected') {
      const interval = setInterval(() => {
        setCallDuration((d) => d + 1);
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [callStatus]);

  const formatDuration = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const statusText = () => {
    switch (callStatus) {
      case 'ringing': return isIncoming ? 'Incoming call...' : 'Calling...';
      case 'connecting': return 'Connecting...';
      case 'connected': return formatDuration(callDuration);
      case 'ended': return 'Call ended';
      default: return '';
    }
  };

  return (
    <div className="fixed inset-0 z-50 mesh-bg flex flex-col items-center justify-between py-16 px-6">
      {/* Contact Info */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col items-center gap-4"
      >
        <div className="relative">
          {/* Ring animations */}
          <AnimatePresence>
            {(callStatus === 'ringing' || callStatus === 'connecting') && (
              <>
                <motion.div
                  initial={{ scale: 0.8, opacity: 0.6 }}
                  animate={{ scale: 1.6, opacity: 0 }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: 'easeOut' }}
                  className="absolute inset-0 rounded-full border-2 border-zixo-primary"
                />
                <motion.div
                  initial={{ scale: 0.8, opacity: 0.4 }}
                  animate={{ scale: 1.8, opacity: 0 }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: 'easeOut', delay: 0.5 }}
                  className="absolute inset-0 rounded-full border-2 border-zixo-secondary"
                />
              </>
            )}
          </AnimatePresence>

          <Avatar
            name={remoteUser.displayName}
            uid={remoteUser.uid}
            size="2xl"
            className={callStatus === 'connected' ? 'breathe' : ''}
          />
        </div>

        <div className="text-center">
          <h2 className="text-2xl font-bold text-zixo-text">{remoteUser.displayName}</h2>
          <p className="text-sm text-zixo-text-secondary mt-1">
            {callStatus === 'ringing' && !isIncoming && (
              <span className="flex items-center justify-center gap-1">
                Calling<span className="animate-pulse">.</span><span className="animate-pulse delay-100">.</span><span className="animate-pulse delay-200">.</span>
              </span>
            )}
            {callStatus === 'ringing' && isIncoming && 'Incoming call...'}
            {callStatus === 'connecting' && 'Connecting...'}
            {callStatus === 'connected' && (
              <span className="text-zixo-online">{formatDuration(callDuration)}</span>
            )}
            {callStatus === 'ended' && 'Call ended'}
          </p>
        </div>
      </motion.div>

      {/* Waveform Visualization (connected only) */}
      {callStatus === 'connected' && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex items-center gap-[3px] h-12"
        >
          {Array.from({ length: 30 }).map((_, i) => (
            <motion.div
              key={i}
              className="w-[3px] rounded-full gradient-primary"
              animate={{
                height: isMuted ? 4 : [4, Math.random() * 28 + 8, 4],
              }}
              transition={{
                duration: 0.5 + Math.random() * 0.5,
                repeat: Infinity,
                delay: i * 0.03,
                ease: 'easeInOut',
              }}
            />
          ))}
        </motion.div>
      )}

      {/* Incoming Call Buttons */}
      {isIncoming && callStatus === 'ringing' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-12"
        >
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onDecline}
            className="w-16 h-16 rounded-full bg-zixo-error flex items-center justify-center glow-error"
          >
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round">
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72" transform="rotate(135 12 12)" />
            </svg>
          </motion.button>

          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onAnswer}
            className="w-16 h-16 rounded-full bg-zixo-success flex items-center justify-center glow-success"
          >
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
            </svg>
          </motion.button>
        </motion.div>
      )}

      {/* Active Call Controls */}
      {(callStatus === 'connected' || callStatus === 'ringing') && !isIncoming && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass rounded-3xl p-6"
        >
          <div className="flex items-center justify-center gap-6">
            {/* Mute */}
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={onToggleMute}
              className={cn(
                'w-14 h-14 rounded-full flex items-center justify-center transition-colors',
                isMuted
                  ? 'bg-zixo-error/20 text-zixo-error'
                  : 'bg-zixo-surface-light text-zixo-text hover:text-zixo-primary'
              )}
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                {isMuted ? (
                  <>
                    <line x1="1" y1="1" x2="23" y2="23" />
                    <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" />
                    <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2c0 .97-.2 1.9-.56 2.74" />
                    <line x1="12" y1="19" x2="12" y2="23" />
                    <line x1="8" y1="23" x2="16" y2="23" />
                  </>
                ) : (
                  <>
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                    <line x1="12" y1="19" x2="12" y2="23" />
                    <line x1="8" y1="23" x2="16" y2="23" />
                  </>
                )}
              </svg>
            </motion.button>

            {/* Speaker */}
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={onToggleSpeaker}
              className={cn(
                'w-14 h-14 rounded-full flex items-center justify-center transition-colors',
                isSpeakerOn
                  ? 'bg-zixo-secondary/20 text-zixo-secondary'
                  : 'bg-zixo-surface-light text-zixo-text hover:text-zixo-secondary'
              )}
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
                {isSpeakerOn ? (
                  <>
                    <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                    <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
                  </>
                ) : (
                  <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                )}
              </svg>
            </motion.button>

            {/* End Call */}
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={onEndCall}
              className="w-16 h-16 rounded-full bg-zixo-error flex items-center justify-center glow-error"
            >
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" transform="rotate(135 12 12)">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72" />
              </svg>
            </motion.button>
          </div>
        </motion.div>
      )}
    </div>
  );
}

interface VideoCallScreenProps {
  remoteUser: ZixoUserProfile;
  callStatus: 'ringing' | 'connecting' | 'connected' | 'ended';
  duration: number;
  isMuted: boolean;
  isVideoOn: boolean;
  onToggleMute: () => void;
  onToggleVideo: () => void;
  onFlipCamera: () => void;
  onEndCall: () => void;
}

export function VideoCallScreen({
  remoteUser,
  callStatus,
  duration,
  isMuted,
  isVideoOn,
  onToggleMute,
  onToggleVideo,
  onFlipCamera,
  onEndCall,
}: VideoCallScreenProps) {
  const [callDuration, setCallDuration] = useState(duration);
  const [showControls, setShowControls] = useState(true);

  useEffect(() => {
    if (callStatus === 'connected') {
      const interval = setInterval(() => {
        setCallDuration((d) => d + 1);
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [callStatus]);

  useEffect(() => {
    if (showControls && callStatus === 'connected') {
      const timer = setTimeout(() => setShowControls(false), 4000);
      return () => clearTimeout(timer);
    }
  }, [showControls, callStatus]);

  const formatDuration = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black"
      onClick={() => setShowControls(!showControls)}
    >
      {/* Remote Video (Simulated) */}
      <div className="absolute inset-0 bg-gradient-to-br from-zixo-bg via-zixo-surface to-zixo-primary/10 flex items-center justify-center">
        {isVideoOn ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center"
          >
            <Avatar name={remoteUser.displayName} uid={remoteUser.uid} size="2xl" className="mx-auto breathe" />
            <h3 className="text-xl font-semibold text-zixo-text mt-4">{remoteUser.displayName}</h3>
          </motion.div>
        ) : (
          <div className="text-center">
            <div className="w-24 h-24 rounded-full bg-zixo-surface-light flex items-center justify-center mx-auto">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </div>
            <p className="text-sm text-zixo-text-secondary mt-3">Camera off</p>
          </div>
        )}
      </div>

      {/* Self View (PiP) */}
      <motion.div
        drag
        dragConstraints={{ top: 50, left: 20, right: window.innerWidth - 140, bottom: window.innerHeight - 200 }}
        className="absolute top-12 right-4 w-28 h-40 rounded-2xl bg-gradient-to-br from-zixo-primary/30 to-zixo-secondary/30 overflow-hidden border-2 border-white/10 z-10 cursor-grab active:cursor-grabbing"
      >
        <div className="w-full h-full flex items-center justify-center">
          <div className="w-12 h-12 rounded-full bg-zixo-primary flex items-center justify-center text-sm font-semibold text-white">
            You
          </div>
        </div>
      </motion.div>

      {/* Top Bar */}
      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="absolute top-0 left-0 right-0 glass p-4 pt-6 z-20"
          >
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-semibold text-zixo-text">{remoteUser.displayName}</h3>
                <p className="text-xs text-zixo-text-secondary">
                  {callStatus === 'connected' ? formatDuration(callDuration) : 'Connecting...'}
                </p>
              </div>
              {/* Network quality */}
              <div className="flex items-center gap-1">
                <div className="flex gap-[2px]">
                  <div className="w-1 h-3 rounded-sm bg-zixo-success" />
                  <div className="w-1 h-4 rounded-sm bg-zixo-success" />
                  <div className="w-1 h-5 rounded-sm bg-zixo-success" />
                  <div className="w-1 h-6 rounded-sm bg-zixo-success/40" />
                </div>
                <span className="text-xs text-zixo-text-secondary ml-1">Good</span>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Bottom Controls */}
      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className="absolute bottom-0 left-0 right-0 glass p-6 pb-8 z-20"
          >
            <div className="flex items-center justify-center gap-5">
              {/* Mute */}
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={(e) => { e.stopPropagation(); onToggleMute(); }}
                className={cn(
                  'w-14 h-14 rounded-full flex items-center justify-center transition-colors',
                  isMuted ? 'bg-white/20 text-white' : 'bg-white/10 text-white'
                )}
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  {isMuted ? (
                    <>
                      <line x1="1" y1="1" x2="23" y2="23" />
                      <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" />
                      <path d="M17 16.95A7 7 0 0 1 5 12v-2" />
                      <line x1="12" y1="19" x2="12" y2="23" />
                      <line x1="8" y1="23" x2="16" y2="23" />
                    </>
                  ) : (
                    <>
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                      <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                      <line x1="12" y1="19" x2="12" y2="23" />
                      <line x1="8" y1="23" x2="16" y2="23" />
                    </>
                  )}
                </svg>
              </motion.button>

              {/* Toggle Video */}
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={(e) => { e.stopPropagation(); onToggleVideo(); }}
                className={cn(
                  'w-14 h-14 rounded-full flex items-center justify-center transition-colors',
                  !isVideoOn ? 'bg-white/20 text-white' : 'bg-white/10 text-white'
                )}
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="23 7 16 12 23 17 23 7" />
                  <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                  {!isVideoOn && <line x1="1" y1="1" x2="23" y2="23" />}
                </svg>
              </motion.button>

              {/* Flip Camera */}
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={(e) => { e.stopPropagation(); onFlipCamera(); }}
                className="w-14 h-14 rounded-full bg-white/10 text-white flex items-center justify-center"
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="23 4 23 10 17 10" />
                  <polyline points="1 20 1 14 7 14" />
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                </svg>
              </motion.button>

              {/* End Call */}
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={(e) => { e.stopPropagation(); onEndCall(); }}
                className="w-16 h-16 rounded-full bg-zixo-error flex items-center justify-center glow-error"
              >
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" transform="rotate(135 12 12)">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72" />
                </svg>
              </motion.button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

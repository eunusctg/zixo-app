'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { cn } from '@/lib/zixo-utils';

// ==================== SHARED STYLES ====================

const glowGreen = '0 0 40px rgba(37, 211, 102, 0.6), 0 0 80px rgba(37, 211, 102, 0.3), 0 0 120px rgba(37, 211, 102, 0.1)';
const glowRed = '0 0 40px rgba(234, 67, 53, 0.6), 0 0 80px rgba(234, 67, 53, 0.3), 0 0 120px rgba(234, 67, 53, 0.1)';

const PARTICLES = Array.from({ length: 20 }, (_, i) => ({
  id: i,
  x: Math.random() * 100,
  y: Math.random() * 100,
  size: Math.random() * 4 + 1,
  dur: Math.random() * 6 + 4,
  delay: Math.random() * 3,
  opacity: Math.random() * 0.3 + 0.05,
}));

const formatDuration = (secs: number) => {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};

// ==================== PARTICIPANT TILE ====================

interface ParticipantTileProps {
  uid: string;
  displayName: string;
  avatar: string;
  stream: MediaStream | null;
  isMuted: boolean;
  isVideoOn: boolean;
  isLocal?: boolean;
  isActive?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

function ParticipantTile({ uid, displayName, avatar, stream, isMuted, isVideoOn, isLocal = false, isActive = false, size = 'md' }: ParticipantTileProps) {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream;
      videoRef.current.play().catch(() => {});
    }
  }, [stream]);

  const sizeClasses = {
    sm: 'w-full aspect-square',
    md: 'w-full aspect-square',
    lg: 'w-full aspect-video',
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.9 }}
      className={cn(
        'relative rounded-2xl overflow-hidden bg-zixo-surface-light/30',
        sizeClasses[size],
      )}
      style={{
        border: isActive ? '2px solid rgba(37, 211, 102, 0.5)' : '1px solid rgba(255,255,255,0.08)',
        boxShadow: isActive ? '0 0 20px rgba(37, 211, 102, 0.2)' : 'none',
      }}
    >
      {/* Video stream */}
      {isVideoOn && stream ? (
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted={isLocal}
          className="absolute inset-0 w-full h-full object-cover"
        />
      ) : (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2"
          style={{
            background: 'radial-gradient(ellipse at center, rgba(37, 211, 102, 0.08) 0%, rgba(17, 27, 33, 0.9) 70%)',
          }}
        >
          <Avatar name={displayName} uid={uid} size="lg" />
        </div>
      )}

      {/* Name overlay */}
      <div className="absolute bottom-0 left-0 right-0 p-2"
        style={{ background: 'linear-gradient(transparent, rgba(0,0,0,0.7))' }}
      >
        <div className="flex items-center gap-1.5">
          <span className="text-[11px] font-medium text-white truncate">{displayName}</span>
          {isMuted && (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#EA4335" strokeWidth="2.5" strokeLinecap="round">
              <line x1="1" y1="1" x2="23" y2="23" />
              <path d="M9 9v3a3 3 0 0 0 5.12 2.12" />
              <path d="M17 16.95A7 7 0 0 1 5 12v-2" />
            </svg>
          )}
          {isLocal && (
            <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-white/15 text-white/70">You</span>
          )}
        </div>
      </div>

      {/* Video off indicator */}
      {!isVideoOn && stream && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/60">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" opacity={0.5}>
            <polygon points="23 7 16 12 23 17 23 7" />
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
            <line x1="1" y1="1" x2="23" y2="23" />
          </svg>
        </div>
      )}
    </motion.div>
  );
}

// ==================== GROUP AUDIO CALL SCREEN ====================

interface GroupAudioCallScreenProps {
  participants: Array<{ uid: string; displayName: string; avatar: string; stream: MediaStream | null; isMuted: boolean; isVideoOn: boolean }>;
  status: 'idle' | 'ringing' | 'connecting' | 'active' | 'ended';
  isMuted: boolean;
  isSpeakerOn: boolean;
  startedAt: number | null;
  onToggleMute: () => void;
  onToggleSpeaker: () => void;
  onLeaveCall: () => void;
}

export function GroupAudioCallScreen({
  participants,
  status,
  isMuted,
  isSpeakerOn,
  startedAt,
  onToggleMute,
  onToggleSpeaker,
  onLeaveCall,
}: GroupAudioCallScreenProps) {
  const [callDuration, setCallDuration] = useState(0);

  useEffect(() => {
    if (status !== 'active' || !startedAt) return;
    const initial = Math.floor((Date.now() - startedAt) / 1000);
    const interval = setInterval(() => {
      setCallDuration((d) => d === 0 ? initial : d + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [status, startedAt]);

  const participantCount = participants.length + 1; // +1 for self

  // Grid layout: 2 columns
  const gridCols = participantCount <= 2 ? 'grid-cols-1 max-w-xs mx-auto' :
    participantCount <= 4 ? 'grid-cols-2' : 'grid-cols-2 sm:grid-cols-3';

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-between overflow-hidden safe-area-top safe-area-bottom"
      style={{
        background: 'radial-gradient(ellipse at 50% 30%, #0d2818 0%, #0a0f14 50%, #050809 100%)',
      }}
    >
      {/* Animated particles */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {PARTICLES.map((p) => (
          <motion.div
            key={p.id}
            className="absolute rounded-full"
            style={{
              left: `${p.x}%`,
              top: `${p.y}%`,
              width: p.size,
              height: p.size,
              background: `rgba(37, 211, 102, ${p.opacity})`,
              boxShadow: `0 0 ${p.size * 3}px rgba(37, 211, 102, ${p.opacity * 0.5})`,
            }}
            animate={{
              y: [0, -80, -40, -120, 0],
              x: [0, 20, -20, 10, 0],
              opacity: [p.opacity, p.opacity * 2, p.opacity, p.opacity * 1.5, p.opacity],
            }}
            transition={{
              duration: p.dur,
              repeat: Infinity,
              delay: p.delay,
              ease: 'easeInOut',
            }}
          />
        ))}
      </div>

      {/* Top Bar */}
      <div className="relative z-10 w-full px-4 pt-6 sm:pt-10 pb-2">
        <div className="text-center">
          <motion.h2
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-xl font-bold text-white tracking-tight"
          >
            Group Audio Call
          </motion.h2>
          <div className="flex items-center justify-center gap-2 mt-1">
            <span className="text-xs text-zixo-primary/70">
              {participantCount} {participantCount === 1 ? 'participant' : 'participants'}
            </span>
            {status === 'active' && (
              <>
                <span className="text-xs text-white/30">•</span>
                <motion.span
                  className="text-xs font-mono tracking-[0.2em] font-bold"
                  style={{
                    background: 'linear-gradient(90deg, #25D366, #34B7F1, #25D366)',
                    backgroundSize: '200% 100%',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent',
                    animation: 'shimmer 2s linear infinite',
                  }}
                >
                  {formatDuration(callDuration)}
                </motion.span>
              </>
            )}
            {(status === 'ringing' || status === 'connecting') && (
              <>
                <span className="text-xs text-white/30">•</span>
                <span className="text-xs text-zixo-accent animate-pulse">
                  {status === 'ringing' ? 'Calling...' : 'Connecting...'}
                </span>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Participants Grid */}
      <div className="relative z-10 flex-1 w-full px-4 py-4 overflow-y-auto">
        <div className={cn('grid gap-3', gridCols)}>
          {/* Self tile */}
          <div className="flex flex-col items-center">
            <motion.div
              animate={status === 'active' ? {
                boxShadow: [
                  '0 0 15px rgba(37, 211, 102, 0.2)',
                  '0 0 30px rgba(37, 211, 102, 0.4), 0 0 60px rgba(37, 211, 102, 0.15)',
                  '0 0 15px rgba(37, 211, 102, 0.2)',
                ],
              } : {}}
              transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
              className="relative rounded-full p-0.5"
            >
              <Avatar name="You" uid="self" size="xl" />
              {isMuted && (
                <div className="absolute -top-1 -right-1 w-6 h-6 rounded-full bg-zixo-error/20 flex items-center justify-center"
                  style={{ border: '1px solid rgba(234, 67, 53, 0.4)' }}
                >
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#EA4335" strokeWidth="3" strokeLinecap="round">
                    <line x1="1" y1="1" x2="23" y2="23" />
                    <path d="M9 9v3" />
                  </svg>
                </div>
              )}
            </motion.div>
            <span className="text-[11px] text-white/70 mt-2">You</span>
          </div>

          {/* Remote participants */}
          <AnimatePresence>
            {participants.map((p) => (
              <motion.div
                key={p.uid}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                className="flex flex-col items-center"
              >
                <motion.div
                  animate={status === 'active' && !p.isMuted ? {
                    boxShadow: [
                      '0 0 10px rgba(37, 211, 102, 0.1)',
                      '0 0 25px rgba(37, 211, 102, 0.35), 0 0 50px rgba(37, 211, 102, 0.12)',
                      '0 0 10px rgba(37, 211, 102, 0.1)',
                    ],
                  } : {}}
                  transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                  className={cn(
                    'relative rounded-full p-0.5',
                    p.isMuted ? 'opacity-60' : ''
                  )}
                >
                  <Avatar name={p.displayName} uid={p.uid} size="xl" />
                  {p.isMuted && (
                    <div className="absolute -top-1 -right-1 w-6 h-6 rounded-full bg-zixo-error/20 flex items-center justify-center"
                      style={{ border: '1px solid rgba(234, 67, 53, 0.4)' }}
                    >
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#EA4335" strokeWidth="3" strokeLinecap="round">
                        <line x1="1" y1="1" x2="23" y2="23" />
                        <path d="M9 9v3" />
                      </svg>
                    </div>
                  )}
                </motion.div>
                <span className="text-[11px] text-white/70 mt-2 truncate max-w-[80px]">{p.displayName}</span>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>

      {/* Bottom Controls */}
      <div className="relative z-10 w-full pb-6 sm:pb-12 pt-4 safe-area-bottom">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 200, damping: 20 }}
          className="px-8"
        >
          <div className="rounded-3xl p-6 relative overflow-hidden"
            style={{
              background: 'rgba(17, 27, 33, 0.7)',
              backdropFilter: 'blur(24px)',
              WebkitBackdropFilter: 'blur(24px)',
              border: '1px solid rgba(37, 211, 102, 0.15)',
              boxShadow: '0 0 30px rgba(37, 211, 102, 0.05), inset 0 0 30px rgba(37, 211, 102, 0.02)',
            }}
          >
            <div className="flex items-center justify-center gap-5">
              {/* Mute */}
              <div className="flex flex-col items-center gap-2">
                <motion.button
                  whileTap={{ scale: 0.85 }}
                  onClick={onToggleMute}
                  className={cn(
                    'w-14 h-14 rounded-full flex items-center justify-center transition-all',
                    isMuted
                      ? 'border border-zixo-error/30'
                      : 'bg-white/5 border border-white/5'
                  )}
                  style={isMuted ? {
                    background: 'rgba(234, 67, 53, 0.15)',
                    boxShadow: '0 0 20px rgba(234, 67, 53, 0.25)',
                  } : {}}
                >
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={isMuted ? '#EA4335' : '#E9EDEF'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
                <span className={cn(
                  'text-[9px] tracking-wider uppercase',
                  isMuted ? 'text-zixo-error' : 'text-zixo-text-secondary'
                )}>
                  {isMuted ? 'Muted' : 'Mute'}
                </span>
              </div>

              {/* Speaker */}
              <div className="flex flex-col items-center gap-2">
                <motion.button
                  whileTap={{ scale: 0.85 }}
                  onClick={onToggleSpeaker}
                  className={cn(
                    'w-14 h-14 rounded-full flex items-center justify-center transition-all',
                    isSpeakerOn
                      ? 'border border-zixo-secondary/30'
                      : 'bg-white/5 border border-white/5'
                  )}
                  style={isSpeakerOn ? {
                    background: 'rgba(18, 140, 126, 0.15)',
                    boxShadow: '0 0 20px rgba(18, 140, 126, 0.25)',
                  } : {}}
                >
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={isSpeakerOn ? '#128C7E' : '#E9EDEF'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
                <span className={cn(
                  'text-[9px] tracking-wider uppercase',
                  isSpeakerOn ? 'text-zixo-secondary' : 'text-zixo-text-secondary'
                )}>
                  Speaker
                </span>
              </div>

              {/* Leave Call */}
              <div className="flex flex-col items-center gap-2">
                <motion.button
                  whileTap={{ scale: 0.85 }}
                  onClick={onLeaveCall}
                  className="w-16 h-16 rounded-full flex items-center justify-center"
                  style={{
                    background: 'linear-gradient(135deg, #EA4335, #c62828)',
                    boxShadow: glowRed,
                  }}
                >
                  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" transform="rotate(135 12 12)">
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72" />
                  </svg>
                </motion.button>
                <span className="text-[9px] text-zixo-error tracking-wider uppercase">Leave</span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      <style jsx global>{`
        @keyframes shimmer {
          0% { background-position: -200% 0; }
          100% { background-position: 200% 0; }
        }
      `}</style>
    </div>
  );
}

// ==================== GROUP VIDEO CALL SCREEN ====================

interface GroupVideoCallScreenProps {
  participants: Array<{ uid: string; displayName: string; avatar: string; stream: MediaStream | null; isMuted: boolean; isVideoOn: boolean }>;
  localStream: MediaStream | null;
  status: 'idle' | 'ringing' | 'connecting' | 'active' | 'ended';
  isMuted: boolean;
  isVideoOn: boolean;
  startedAt: number | null;
  onToggleMute: () => void;
  onToggleVideo: () => void;
  onFlipCamera: () => void;
  onLeaveCall: () => void;
}

export function GroupVideoCallScreen({
  participants,
  localStream,
  status,
  isMuted,
  isVideoOn,
  startedAt,
  onToggleMute,
  onToggleVideo,
  onFlipCamera,
  onLeaveCall,
}: GroupVideoCallScreenProps) {
  const [callDuration, setCallDuration] = useState(0);
  const [showControls, setShowControls] = useState(true);
  const localVideoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (status !== 'active' || !startedAt) return;
    const initial = Math.floor((Date.now() - startedAt) / 1000);
    const interval = setInterval(() => {
      setCallDuration((d) => d === 0 ? initial : d + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [status, startedAt]);

  useEffect(() => {
    if (showControls && status === 'active') {
      const timer = setTimeout(() => setShowControls(false), 4000);
      return () => clearTimeout(timer);
    }
  }, [showControls, status]);

  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
      localVideoRef.current.play().catch(() => {});
    }
  }, [localStream]);

  const participantCount = participants.length + 1;

  // Grid layout calculation
  const totalTiles = participantCount;
  const gridCols = totalTiles <= 1 ? 'grid-cols-1' :
    totalTiles <= 2 ? 'grid-cols-1 sm:grid-cols-2' :
    totalTiles <= 4 ? 'grid-cols-2' :
    'grid-cols-2 sm:grid-cols-3';

  return (
    <div
      className="fixed inset-0 z-50 bg-black safe-area-top safe-area-bottom"
      onClick={() => setShowControls(!showControls)}
    >
      {/* Video Grid */}
      <div className={cn(
        'h-full w-full grid gap-1 p-1',
        gridCols
      )}>
        {/* Self tile */}
        <ParticipantTile
          uid="self"
          displayName="You"
          avatar=""
          stream={localStream}
          isMuted={isMuted}
          isVideoOn={isVideoOn}
          isLocal={true}
        />

        {/* Remote participants */}
        <AnimatePresence>
          {participants.map((p) => (
            <ParticipantTile
              key={p.uid}
              uid={p.uid}
              displayName={p.displayName}
              avatar={p.avatar}
              stream={p.stream}
              isMuted={p.isMuted}
              isVideoOn={p.isVideoOn}
            />
          ))}
        </AnimatePresence>
      </div>

      {/* Gradient overlays */}
      <AnimatePresence>
        {showControls && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-x-0 top-0 h-28 pointer-events-none z-[5]"
              style={{ background: 'linear-gradient(180deg, rgba(0,0,0,0.8) 0%, transparent 100%)' }}
            />
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-x-0 bottom-0 h-44 pointer-events-none z-[5]"
              style={{ background: 'linear-gradient(0deg, rgba(0,0,0,0.9) 0%, transparent 100%)' }}
            />
          </>
        )}
      </AnimatePresence>

      {/* Top Bar */}
      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="absolute top-0 left-0 right-0 p-4 pt-6 z-20"
          >
            <div className="flex items-center justify-between px-2">
              <div>
                <h3 className="font-semibold text-white text-lg">Group Video Call</h3>
                <p className="text-xs text-white/60 mt-0.5">
                  {participantCount} {participantCount === 1 ? 'participant' : 'participants'}
                  {status === 'active' && (
                    <span className="ml-2" style={{
                      background: 'linear-gradient(90deg, #25D366, #34B7F1)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      fontWeight: 600,
                    }}>
                      {formatDuration(callDuration)}
                    </span>
                  )}
                </p>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Bottom Controls */}
      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 30 }}
            className="absolute bottom-0 left-0 right-0 p-4 sm:p-6 pb-8 sm:pb-10 z-20 safe-area-bottom"
          >
            <div className="flex items-center justify-center gap-5">
              {/* Mute */}
              <motion.button
                whileTap={{ scale: 0.85 }}
                onClick={(e) => { e.stopPropagation(); onToggleMute(); }}
                className={cn(
                  'w-14 h-14 rounded-full flex items-center justify-center transition-all',
                  isMuted ? '' : 'bg-white/10'
                )}
                style={isMuted ? {
                  background: 'rgba(234, 67, 53, 0.2)',
                  border: '1px solid rgba(234, 67, 53, 0.4)',
                  boxShadow: '0 0 20px rgba(234, 67, 53, 0.25)',
                } : { border: '1px solid rgba(255,255,255,0.1)' }}
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={isMuted ? '#EA4335' : 'white'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
                whileTap={{ scale: 0.85 }}
                onClick={(e) => { e.stopPropagation(); onToggleVideo(); }}
                className={cn(
                  'w-14 h-14 rounded-full flex items-center justify-center transition-all',
                  !isVideoOn ? '' : 'bg-white/10'
                )}
                style={!isVideoOn ? {
                  background: 'rgba(234, 67, 53, 0.2)',
                  border: '1px solid rgba(234, 67, 53, 0.4)',
                  boxShadow: '0 0 20px rgba(234, 67, 53, 0.25)',
                } : { border: '1px solid rgba(255,255,255,0.1)' }}
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={!isVideoOn ? '#EA4335' : 'white'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="23 7 16 12 23 17 23 7" />
                  <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                  {!isVideoOn && <line x1="1" y1="1" x2="23" y2="23" />}
                </svg>
              </motion.button>

              {/* Flip Camera */}
              <motion.button
                whileTap={{ scale: 0.85 }}
                onClick={(e) => { e.stopPropagation(); onFlipCamera(); }}
                className="w-14 h-14 rounded-full flex items-center justify-center bg-white/10"
                style={{ border: '1px solid rgba(255,255,255,0.1)' }}
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="23 4 23 10 17 10" />
                  <polyline points="1 20 1 14 7 14" />
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                </svg>
              </motion.button>

              {/* Leave Call */}
              <motion.button
                whileTap={{ scale: 0.85 }}
                onClick={(e) => { e.stopPropagation(); onLeaveCall(); }}
                className="w-16 h-16 rounded-full flex items-center justify-center"
                style={{
                  background: 'linear-gradient(135deg, #EA4335, #c62828)',
                  boxShadow: glowRed,
                }}
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

// ==================== INCOMING GROUP CALL SCREEN ====================

interface IncomingGroupCallScreenProps {
  callerName: string;
  callType: 'group-audio' | 'group-video';
  participantNames: string[];
  onAnswer: () => void;
  onDecline: () => void;
}

export function IncomingGroupCallScreen({
  callerName,
  callType,
  participantNames,
  onAnswer,
  onDecline,
}: IncomingGroupCallScreenProps) {
  const isVideo = callType === 'group-video';

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-between overflow-hidden safe-area-top safe-area-bottom"
      style={{
        background: 'radial-gradient(ellipse at 50% 30%, #0d2818 0%, #0a0f14 50%, #050809 100%)',
      }}
    >
      {/* Animated particles */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {PARTICLES.map((p) => (
          <motion.div
            key={p.id}
            className="absolute rounded-full"
            style={{
              left: `${p.x}%`,
              top: `${p.y}%`,
              width: p.size,
              height: p.size,
              background: `rgba(37, 211, 102, ${p.opacity})`,
            }}
            animate={{
              y: [0, -80, 0],
              opacity: [p.opacity, p.opacity * 2, p.opacity],
            }}
            transition={{
              duration: p.dur,
              repeat: Infinity,
              delay: p.delay,
            }}
          />
        ))}
      </div>

      {/* Top area - Caller info */}
      <div className="relative z-10 flex flex-col items-center pt-16 sm:pt-24 pb-8">
        <motion.div
          initial={{ opacity: 0, y: -30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 150, damping: 20 }}
          className="flex flex-col items-center gap-5"
        >
          {/* Avatar with pulsing rings */}
          <div className="relative">
            <motion.div
              initial={{ scale: 0.8, opacity: 0.6 }}
              animate={{ scale: 2.5, opacity: 0 }}
              transition={{ duration: 2.5, repeat: Infinity, ease: 'easeOut' }}
              className="absolute inset-0 rounded-full"
              style={{ border: '2px solid rgba(37, 211, 102, 0.5)', boxShadow: '0 0 20px rgba(37, 211, 102, 0.3)' }}
            />
            <motion.div
              initial={{ scale: 0.8, opacity: 0.5 }}
              animate={{ scale: 2, opacity: 0 }}
              transition={{ duration: 2.5, repeat: Infinity, ease: 'easeOut', delay: 0.7 }}
              className="absolute inset-0 rounded-full"
              style={{ border: '2px solid rgba(52, 183, 241, 0.4)', boxShadow: '0 0 20px rgba(52, 183, 241, 0.2)' }}
            />
            <Avatar name={callerName} uid={callerName} size="2xl" />
          </div>

          {/* Name and call type */}
          <div className="text-center">
            <h2 className="text-2xl font-bold text-white tracking-tight">{callerName}</h2>
            <p className="text-sm text-zixo-primary/80 mt-1">
              Group {isVideo ? 'Video' : 'Audio'} Call
            </p>
          </div>

          {/* Participant list */}
          {participantNames.length > 0 && (
            <div className="mt-2 px-4">
              <p className="text-xs text-white/40 mb-2 text-center">
                {participantNames.length + 1} {participantNames.length + 1 === 1 ? 'participant' : 'participants'}
              </p>
              <div className="flex flex-wrap justify-center gap-2">
                {participantNames.map((name, i) => (
                  <span
                    key={i}
                    className="text-[11px] px-2.5 py-1 rounded-full text-white/70"
                    style={{
                      background: 'rgba(255,255,255,0.08)',
                      border: '1px solid rgba(255,255,255,0.1)',
                    }}
                  >
                    {name}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Call type icon */}
          <div className="flex items-center gap-1.5 mt-2">
            {isVideo ? (
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#34B7F1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="23 7 16 12 23 17 23 7" />
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
              </svg>
            ) : (
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
            )}
            <span className={cn(
              'text-xs font-semibold animate-pulse',
              isVideo ? 'text-zixo-accent' : 'text-zixo-primary'
            )}>
              Incoming...
            </span>
          </div>
        </motion.div>
      </div>

      {/* Bottom - Answer/Decline */}
      <div className="relative z-10 w-full pb-8 sm:pb-16 pt-4 safe-area-bottom">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 200, damping: 20 }}
          className="flex items-center justify-center gap-16 px-8"
        >
          {/* Decline */}
          <div className="flex flex-col items-center gap-2">
            <motion.button
              whileTap={{ scale: 0.85 }}
              onClick={onDecline}
              className="w-20 h-20 rounded-full flex items-center justify-center relative"
              style={{
                background: 'linear-gradient(135deg, #EA4335, #c62828)',
                boxShadow: glowRed,
              }}
            >
              <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72" transform="rotate(135 12 12)" />
              </svg>
            </motion.button>
            <span className="text-[10px] text-zixo-text-secondary tracking-wider uppercase">Decline</span>
          </div>

          {/* Answer */}
          <div className="flex flex-col items-center gap-2">
            <motion.button
              whileTap={{ scale: 0.85 }}
              onClick={onAnswer}
              className="w-20 h-20 rounded-full flex items-center justify-center relative"
              style={{
                background: 'linear-gradient(135deg, #25D366, #128C7E)',
                boxShadow: glowGreen,
              }}
            >
              <motion.div
                animate={{
                  boxShadow: [
                    '0 0 0 0 rgba(37, 211, 102, 0.5)',
                    '0 0 0 20px rgba(37, 211, 102, 0)',
                  ],
                }}
                transition={{ duration: 1.5, repeat: Infinity }}
                className="absolute inset-0 rounded-full"
              />
              <motion.div
                animate={{
                  boxShadow: [
                    '0 0 0 0 rgba(37, 211, 102, 0.3)',
                    '0 0 0 35px rgba(37, 211, 102, 0)',
                  ],
                }}
                transition={{ duration: 1.5, repeat: Infinity, delay: 0.5 }}
                className="absolute inset-0 rounded-full"
              />
              <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
            </motion.button>
            <span className="text-[10px] text-zixo-primary tracking-wider uppercase font-medium">Answer</span>
          </div>
        </motion.div>
      </div>
    </div>
  );
}

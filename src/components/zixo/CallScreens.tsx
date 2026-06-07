'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { cn } from '@/lib/zixo-utils';
import type { ZixoUserProfile } from '@/services/auth';

// ==================== SHARED STYLES ====================

const glowGreen = '0 0 40px rgba(37, 211, 102, 0.6), 0 0 80px rgba(37, 211, 102, 0.3), 0 0 120px rgba(37, 211, 102, 0.1)';
const glowRed = '0 0 40px rgba(234, 67, 53, 0.6), 0 0 80px rgba(234, 67, 53, 0.3), 0 0 120px rgba(234, 67, 53, 0.1)';
const glowBlue = '0 0 40px rgba(52, 183, 241, 0.6), 0 0 80px rgba(52, 183, 241, 0.3), 0 0 120px rgba(52, 183, 241, 0.1)';

// Floating particles for background
const PARTICLES = Array.from({ length: 30 }, (_, i) => ({
  id: i,
  x: Math.random() * 100,
  y: Math.random() * 100,
  size: Math.random() * 4 + 1,
  dur: Math.random() * 6 + 4,
  delay: Math.random() * 3,
  opacity: Math.random() * 0.3 + 0.05,
}));

// ==================== AUDIO CALL SCREEN ====================

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
  remoteStream?: MediaStream | null;
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
  remoteStream,
}: AudioCallScreenProps) {
  const [callDuration, setCallDuration] = useState(duration);
  const audioRef = useRef<HTMLAudioElement>(null);

  useEffect(() => {
    if (callStatus === 'connected') {
      const interval = setInterval(() => {
        setCallDuration((d) => d + 1);
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [callStatus]);

  useEffect(() => {
    if (audioRef.current && remoteStream) {
      audioRef.current.srcObject = remoteStream;
      audioRef.current.play().catch(() => {});
    }
  }, [remoteStream]);

  const formatDuration = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const isActive = callStatus === 'connected' || (callStatus === 'ringing' && !isIncoming) || callStatus === 'connecting';

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-between overflow-hidden safe-area-top safe-area-bottom"
      style={{
        background: 'radial-gradient(ellipse at 50% 30%, #0d2818 0%, #0a0f14 50%, #050809 100%)',
      }}
    >
      {/* Hidden audio element */}
      <audio ref={audioRef} autoPlay playsInline />

      {/* Animated particle field */}
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

        {/* Large ambient glow orbs */}
        <motion.div
          className="absolute rounded-full"
          style={{
            width: 400,
            height: 400,
            left: '50%',
            top: '30%',
            marginLeft: -200,
            marginTop: -200,
            background: 'radial-gradient(circle, rgba(37, 211, 102, 0.08), transparent 70%)',
          }}
          animate={{
            scale: [1, 1.3, 1],
            opacity: [0.5, 0.8, 0.5],
          }}
          transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
        />
        <motion.div
          className="absolute rounded-full"
          style={{
            width: 300,
            height: 300,
            left: '20%',
            top: '60%',
            background: 'radial-gradient(circle, rgba(52, 183, 241, 0.06), transparent 70%)',
          }}
          animate={{
            scale: [1.2, 1, 1.2],
            opacity: [0.3, 0.6, 0.3],
          }}
          transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut', delay: 2 }}
        />
      </div>

      {/* Top area - Contact Info */}
      <div className="relative z-10 flex flex-col items-center pt-8 sm:pt-16 pb-4 sm:pb-8">
        <motion.div
          initial={{ opacity: 0, y: -30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 150, damping: 20 }}
          className="flex flex-col items-center gap-5"
        >
          {/* Avatar with neon rings */}
          <div className="relative">
            {/* Pulsing neon rings for ringing state */}
            <AnimatePresence>
              {(callStatus === 'ringing' || callStatus === 'connecting') && (
                <>
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
                  <motion.div
                    initial={{ scale: 0.8, opacity: 0.4 }}
                    animate={{ scale: 1.6, opacity: 0 }}
                    transition={{ duration: 2.5, repeat: Infinity, ease: 'easeOut', delay: 1.4 }}
                    className="absolute inset-0 rounded-full"
                    style={{ border: '2px solid rgba(37, 211, 102, 0.3)', boxShadow: '0 0 15px rgba(37, 211, 102, 0.15)' }}
                  />
                </>
              )}
            </AnimatePresence>

            {/* Breathing glow halo when connected */}
            {callStatus === 'connected' && (
              <>
                <motion.div
                  animate={{
                    boxShadow: [
                      '0 0 30px rgba(37, 211, 102, 0.4), 0 0 60px rgba(37, 211, 102, 0.15)',
                      '0 0 60px rgba(37, 211, 102, 0.6), 0 0 120px rgba(37, 211, 102, 0.25), 0 0 180px rgba(37, 211, 102, 0.1)',
                      '0 0 30px rgba(37, 211, 102, 0.4), 0 0 60px rgba(37, 211, 102, 0.15)',
                    ],
                  }}
                  transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                  className="absolute inset-0 rounded-full"
                />
                <motion.div
                  className="absolute -inset-2 rounded-full"
                  animate={{
                    boxShadow: [
                      '0 0 15px rgba(37, 211, 102, 0.2), inset 0 0 15px rgba(37, 211, 102, 0.05)',
                      '0 0 25px rgba(37, 211, 102, 0.35), inset 0 0 25px rgba(37, 211, 102, 0.1)',
                      '0 0 15px rgba(37, 211, 102, 0.2), inset 0 0 15px rgba(37, 211, 102, 0.05)',
                    ],
                  }}
                  transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                  style={{ border: '1px solid rgba(37, 211, 102, 0.15)' }}
                />
              </>
            )}

            <Avatar
              name={remoteUser.displayName}
              uid={remoteUser.uid}
              size="2xl"
              className={callStatus === 'connected' ? 'breathe' : ''}
            />
          </div>

          {/* Name and status */}
          <div className="text-center">
            <motion.h2
              className="text-2xl font-bold text-white tracking-tight"
              animate={callStatus === 'connected' ? {
                textShadow: [
                  '0 0 10px rgba(37, 211, 102, 0)',
                  '0 0 20px rgba(37, 211, 102, 0.3)',
                  '0 0 10px rgba(37, 211, 102, 0)',
                ],
              } : {}}
              transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            >
              {remoteUser.displayName}
            </motion.h2>
            <div className="mt-2 h-7 flex items-center justify-center">
              <AnimatePresence mode="wait">
                {callStatus === 'ringing' && !isIncoming && (
                  <motion.p
                    key="calling"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="text-sm text-zixo-primary/80 flex items-center gap-1"
                  >
                    Calling<span className="animate-pulse">.</span><span className="animate-pulse delay-100">.</span><span className="animate-pulse delay-200">.</span>
                  </motion.p>
                )}
                {callStatus === 'ringing' && isIncoming && (
                  <motion.p
                    key="incoming"
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: [0.5, 1, 0.5], scale: 1 }}
                    transition={{ duration: 1.5, repeat: Infinity }}
                    className="text-sm text-zixo-primary font-semibold"
                    style={{ textShadow: '0 0 10px rgba(37, 211, 102, 0.5)' }}
                  >
                    Incoming call...
                  </motion.p>
                )}
                {callStatus === 'connecting' && (
                  <motion.p
                    key="connecting"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="text-sm text-zixo-accent"
                    style={{ textShadow: '0 0 10px rgba(52, 183, 241, 0.4)' }}
                  >
                    Connecting...
                  </motion.p>
                )}
                {callStatus === 'connected' && (
                  <motion.p
                    key="connected"
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0 }}
                    className="text-sm font-mono tracking-[0.3em] font-bold"
                    style={{
                      background: 'linear-gradient(90deg, #25D366, #34B7F1, #25D366)',
                      backgroundSize: '200% 100%',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      animation: 'shimmer 2s linear infinite',
                    }}
                  >
                    {formatDuration(callDuration)}
                  </motion.p>
                )}
                {callStatus === 'ended' && (
                  <motion.p
                    key="ended"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="text-sm text-zixo-error"
                    style={{ textShadow: '0 0 10px rgba(234, 67, 53, 0.4)' }}
                  >
                    Call ended
                  </motion.p>
                )}
              </AnimatePresence>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Center area - Waveform / Signal */}
      <div className="relative z-10 flex-1 flex items-center justify-center">
        {callStatus === 'connected' && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            className="relative"
          >
            {/* Circular waveform visualization */}
            <div className="flex items-end justify-center gap-[3px] h-20">
              {Array.from({ length: 48 }).map((_, i) => (
                <motion.div
                  key={i}
                  className="w-[3px] rounded-full"
                  style={{
                    background: `linear-gradient(180deg, #25D366, #34B7F1)`,
                    boxShadow: isMuted ? 'none' : `0 0 8px rgba(37, 211, 102, 0.5), 0 0 16px rgba(37, 211, 102, 0.2)`,
                  }}
                  animate={{
                    height: isMuted ? 4 : [4, Math.random() * 48 + 10, 4],
                  }}
                  transition={{
                    duration: 0.3 + Math.random() * 0.7,
                    repeat: Infinity,
                    delay: i * 0.02,
                    ease: 'easeInOut',
                  }}
                />
              ))}
            </div>
            <p className="text-[10px] text-zixo-text-secondary/50 text-center mt-3 tracking-[0.2em] uppercase">
              {isMuted ? 'Microphone Off' : 'Live Audio'}
            </p>
          </motion.div>
        )}

        {(callStatus === 'ringing' || callStatus === 'connecting') && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex flex-col items-center gap-4"
          >
            {/* Animated signal waves with glow */}
            {[0, 1, 2].map((i) => (
              <motion.div
                key={i}
                className="w-20 h-1.5 rounded-full"
                style={{
                  background: 'linear-gradient(90deg, transparent, #25D366, transparent)',
                  boxShadow: '0 0 10px rgba(37, 211, 102, 0.4)',
                }}
                animate={{
                  scaleX: [0.3, 1, 0.3],
                  opacity: [0.2, 0.7, 0.2],
                }}
                transition={{
                  duration: 1.5,
                  repeat: Infinity,
                  delay: i * 0.3,
                  ease: 'easeInOut',
                }}
              />
            ))}
          </motion.div>
        )}
      </div>

      {/* Bottom area - Controls */}
      <div className="relative z-10 w-full pb-6 sm:pb-12 pt-4 safe-area-bottom">
        {/* Incoming Call Buttons */}
        {isIncoming && callStatus === 'ringing' && (
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
        )}

        {/* Active Call Controls */}
        {isActive && !isIncoming && (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ type: 'spring', stiffness: 200, damping: 20 }}
            className="px-8"
          >
            {/* Glass panel with glow border */}
            <div className="rounded-3xl p-6 relative overflow-hidden"
              style={{
                background: 'rgba(17, 27, 33, 0.7)',
                backdropFilter: 'blur(24px)',
                WebkitBackdropFilter: 'blur(24px)',
                border: '1px solid rgba(37, 211, 102, 0.15)',
                boxShadow: '0 0 30px rgba(37, 211, 102, 0.05), inset 0 0 30px rgba(37, 211, 102, 0.02)',
              }}
            >
              {/* Animated border glow */}
              <motion.div
                className="absolute inset-0 rounded-3xl pointer-events-none"
                animate={{
                  boxShadow: [
                    'inset 0 0 20px rgba(37, 211, 102, 0.03), 0 0 20px rgba(37, 211, 102, 0.03)',
                    'inset 0 0 40px rgba(37, 211, 102, 0.08), 0 0 40px rgba(37, 211, 102, 0.08)',
                    'inset 0 0 20px rgba(37, 211, 102, 0.03), 0 0 20px rgba(37, 211, 102, 0.03)',
                  ],
                }}
                transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
              />

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

                {/* End Call */}
                <div className="flex flex-col items-center gap-2">
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={onEndCall}
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
                  <span className="text-[9px] text-zixo-error tracking-wider uppercase">End</span>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </div>

      {/* Shimmer animation keyframes */}
      <style jsx global>{`
        @keyframes shimmer {
          0% { background-position: -200% 0; }
          100% { background-position: 200% 0; }
        }
      `}</style>
    </div>
  );
}

// ==================== VIDEO CALL SCREEN ====================

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
  localStream?: MediaStream | null;
  remoteStream?: MediaStream | null;
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
  localStream,
  remoteStream,
}: VideoCallScreenProps) {
  const [callDuration, setCallDuration] = useState(duration);
  const [showControls, setShowControls] = useState(true);
  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);

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

  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
      localVideoRef.current.play().catch(() => {});
    }
  }, [localStream]);

  useEffect(() => {
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
      remoteVideoRef.current.play().catch(() => {});
    }
  }, [remoteStream]);

  const formatDuration = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black safe-area-top safe-area-bottom"
      onClick={() => setShowControls(!showControls)}
    >
      {/* Remote Video */}
      {remoteStream ? (
        <video
          ref={remoteVideoRef}
          autoPlay
          playsInline
          className="absolute inset-0 w-full h-full object-cover"
        />
      ) : (
        <div className="absolute inset-0 flex items-center justify-center"
          style={{
            background: 'radial-gradient(ellipse at center, #0d2818 0%, #0a0f14 60%, #000 100%)',
          }}
        >
          {/* Animated neon rings for connecting state */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center"
          >
            <div className="relative inline-block">
              {/* Multi-layer pulsing rings */}
              <motion.div
                animate={{ scale: [1, 1.8, 1], opacity: [0.4, 0, 0.4] }}
                transition={{ duration: 2.5, repeat: Infinity }}
                className="absolute inset-0 rounded-full"
                style={{ border: '2px solid rgba(37, 211, 102, 0.5)', boxShadow: '0 0 30px rgba(37, 211, 102, 0.3)' }}
              />
              <motion.div
                animate={{ scale: [1, 1.5, 1], opacity: [0.3, 0, 0.3] }}
                transition={{ duration: 2.5, repeat: Infinity, delay: 0.6 }}
                className="absolute inset-0 rounded-full"
                style={{ border: '2px solid rgba(52, 183, 241, 0.4)', boxShadow: '0 0 25px rgba(52, 183, 241, 0.2)' }}
              />
              <motion.div
                animate={{ scale: [1, 1.3, 1], opacity: [0.2, 0, 0.2] }}
                transition={{ duration: 2.5, repeat: Infinity, delay: 1.2 }}
                className="absolute inset-0 rounded-full"
                style={{ border: '1px solid rgba(37, 211, 102, 0.2)' }}
              />
              <Avatar name={remoteUser.displayName} uid={remoteUser.uid} size="2xl" className="mx-auto breathe" />
            </div>
            <motion.h3
              className="text-xl font-semibold text-white mt-6"
              animate={{
                textShadow: ['0 0 10px rgba(37, 211, 102, 0)', '0 0 20px rgba(37, 211, 102, 0.3)', '0 0 10px rgba(37, 211, 102, 0)'],
              }}
              transition={{ duration: 3, repeat: Infinity }}
            >
              {remoteUser.displayName}
            </motion.h3>
            <p className="text-xs text-zixo-text-secondary mt-2">
              {callStatus === 'ringing' && 'Calling...'}
              {callStatus === 'connecting' && 'Connecting...'}
            </p>
          </motion.div>
        </div>
      )}

      {/* Cinematic gradient overlays */}
      <div className="absolute inset-x-0 top-0 h-32 pointer-events-none z-[5]"
        style={{ background: 'linear-gradient(180deg, rgba(0,0,0,0.8) 0%, transparent 100%)' }}
      />
      <div className="absolute inset-x-0 bottom-0 h-44 pointer-events-none z-[5]"
        style={{ background: 'linear-gradient(0deg, rgba(0,0,0,0.9) 0%, transparent 100%)' }}
      />

      {/* Self View (PiP) with neon border */}
      <motion.div
        drag
        dragConstraints={{ top: 50, left: 10, right: 250, bottom: 400 }}
        className="absolute top-10 right-3 w-24 h-32 sm:top-12 sm:right-4 sm:w-28 sm:h-40 rounded-2xl overflow-hidden z-10 cursor-grab active:cursor-grabbing"
        style={{
          border: '2px solid rgba(37, 211, 102, 0.4)',
          boxShadow: '0 0 25px rgba(37, 211, 102, 0.2), 0 8px 32px rgba(0,0,0,0.5)',
        }}
      >
        {localStream ? (
          <video
            ref={localVideoRef}
            autoPlay
            playsInline
            muted
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center"
            style={{ background: 'linear-gradient(135deg, rgba(37, 211, 102, 0.2), rgba(18, 140, 126, 0.2))' }}
          >
            <div className="w-12 h-12 rounded-full gradient-primary flex items-center justify-center text-sm font-semibold text-white"
              style={{ boxShadow: '0 0 15px rgba(37, 211, 102, 0.3)' }}
            >
              You
            </div>
          </div>
        )}
        {/* Video off overlay */}
        {!isVideoOn && localStream && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/80">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2">
              <polygon points="23 7 16 12 23 17 23 7" />
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </div>
        )}
      </motion.div>

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
                <h3 className="font-semibold text-white text-lg">{remoteUser.displayName}</h3>
                <p className="text-xs text-white/60 mt-0.5">
                  {callStatus === 'connected' ? (
                    <span style={{
                      background: 'linear-gradient(90deg, #25D366, #34B7F1)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      fontWeight: 600,
                    }}>
                      {formatDuration(callDuration)}
                    </span>
                  ) : 'Connecting...'}
                </p>
              </div>
              {/* Network quality indicator */}
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  backdropFilter: 'blur(10px)',
                  boxShadow: '0 0 15px rgba(37, 211, 102, 0.1)',
                }}
              >
                <div className="flex gap-[2px]">
                  <div className="w-1 h-3 rounded-sm bg-zixo-success" style={{ boxShadow: '0 0 4px rgba(37, 211, 102, 0.5)' }} />
                  <div className="w-1 h-4 rounded-sm bg-zixo-success" style={{ boxShadow: '0 0 4px rgba(37, 211, 102, 0.5)' }} />
                  <div className="w-1 h-5 rounded-sm bg-zixo-success" style={{ boxShadow: '0 0 4px rgba(37, 211, 102, 0.5)' }} />
                  <div className="w-1 h-6 rounded-sm bg-zixo-success/40" />
                </div>
                <span className="text-[10px] text-white/60 ml-1">Good</span>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Bottom Controls with glowing design */}
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

              {/* End Call */}
              <motion.button
                whileTap={{ scale: 0.85 }}
                onClick={(e) => { e.stopPropagation(); onEndCall(); }}
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

// ==================== PERMISSION MODAL ====================

interface PermissionModalProps {
  isOpen: boolean;
  requests: Array<{
    type: 'camera' | 'microphone' | 'location';
    status: 'requesting' | 'granted' | 'denied' | 'error';
    message?: string;
  }>;
  currentIndex: number;
  onAllow: () => void;
  onSkip: () => void;
  onCancel: () => void;
}

export function PermissionModal({
  isOpen,
  requests,
  currentIndex,
  onAllow,
  onSkip,
  onCancel,
}: PermissionModalProps) {
  if (!isOpen || currentIndex >= requests.length) return null;

  const current = requests[currentIndex];
  const isProcessing = current.status === 'requesting';

  const getIcon = (type: string) => {
    switch (type) {
      case 'camera':
        return (
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
        );
      case 'microphone':
        return (
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <line x1="12" y1="19" x2="12" y2="23" />
            <line x1="8" y1="23" x2="16" y2="23" />
          </svg>
        );
      case 'location':
        return (
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
        );
      default:
        return null;
    }
  };

  const getTitle = (type: string) => {
    switch (type) {
      case 'camera': return 'Camera Access';
      case 'microphone': return 'Microphone Access';
      case 'location': return 'Location Access';
      default: return 'Permission';
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[100] flex items-center justify-center"
          style={{ background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(10px)' }}
        >
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.8, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            className="w-[340px] rounded-3xl p-6 text-center relative overflow-hidden"
            style={{
              background: 'linear-gradient(180deg, #1a2a22 0%, #111B21 50%, #0a0f14 100%)',
              border: '1px solid rgba(37, 211, 102, 0.2)',
              boxShadow: '0 0 60px rgba(37, 211, 102, 0.1), 0 20px 60px rgba(0,0,0,0.5)',
            }}
          >
            {/* Progress indicator */}
            <div className="flex items-center justify-center gap-2 mb-6">
              {requests.map((req, i) => (
                <div
                  key={i}
                  className={cn(
                    'w-8 h-1 rounded-full transition-all',
                    i < currentIndex ? 'bg-zixo-primary' :
                    i === currentIndex ? 'bg-zixo-primary/50' :
                    'bg-white/10'
                  )}
                />
              ))}
            </div>

            {/* Icon with glow */}
            <motion.div
              className="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4 text-zixo-primary"
              style={{
                background: 'rgba(37, 211, 102, 0.1)',
                border: '1px solid rgba(37, 211, 102, 0.2)',
                boxShadow: '0 0 30px rgba(37, 211, 102, 0.15)',
              }}
              animate={isProcessing ? {
                boxShadow: [
                  '0 0 30px rgba(37, 211, 102, 0.15)',
                  '0 0 50px rgba(37, 211, 102, 0.3)',
                  '0 0 30px rgba(37, 211, 102, 0.15)',
                ],
              } : {}}
              transition={{ duration: 2, repeat: Infinity }}
            >
              {getIcon(current.type)}
            </motion.div>

            {/* Title */}
            <h3 className="text-lg font-bold text-white mb-2">{getTitle(current.type)}</h3>

            {/* Description */}
            <p className="text-sm text-zixo-text-secondary mb-6 leading-relaxed">
              {current.message || `Zixo needs access to your ${current.type} for calls.`}
            </p>

            {/* Status indicator */}
            {current.status === 'granted' && (
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex items-center justify-center gap-2 mb-4"
              >
                <div className="w-5 h-5 rounded-full bg-zixo-success flex items-center justify-center"
                  style={{ boxShadow: '0 0 10px rgba(37, 211, 102, 0.5)' }}>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </div>
                <span className="text-sm text-zixo-success">Granted</span>
              </motion.div>
            )}

            {current.status === 'denied' && (
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex items-center justify-center gap-2 mb-4"
              >
                <div className="w-5 h-5 rounded-full bg-zixo-error/20 flex items-center justify-center">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#EA4335" strokeWidth="3">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </div>
                <span className="text-sm text-zixo-error">Denied</span>
              </motion.div>
            )}

            {/* Action buttons */}
            <div className="flex items-center gap-3">
              <motion.button
                whileTap={{ scale: 0.95 }}
                onClick={onSkip}
                className="flex-1 py-3 rounded-xl text-sm font-medium text-zixo-text-secondary bg-white/5 border border-white/10 hover:bg-white/10 transition-colors"
              >
                Skip
              </motion.button>
              <motion.button
                whileTap={{ scale: 0.95 }}
                onClick={onAllow}
                className="flex-1 py-3 rounded-xl text-sm font-semibold text-white gradient-primary glow-primary"
                style={{
                  boxShadow: '0 0 20px rgba(37, 211, 102, 0.3)',
                }}
              >
                {isProcessing ? 'Allow' : current.status === 'granted' ? 'Next' : 'Allow'}
              </motion.button>
            </div>

            {/* Cancel */}
            <button
              onClick={onCancel}
              className="mt-3 text-xs text-zixo-text-secondary/60 hover:text-zixo-text-secondary transition-colors"
            >
              Cancel call
            </button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

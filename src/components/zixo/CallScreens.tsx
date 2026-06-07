'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
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
  remoteStream?: MediaStream | null;
}

// Floating orb data for background
const ORBS = [
  { x: 20, y: 30, size: 200, color: 'rgba(37, 211, 102, 0.08)', dur: 8 },
  { x: 70, y: 60, size: 280, color: 'rgba(18, 140, 126, 0.06)', dur: 12 },
  { x: 40, y: 80, size: 160, color: 'rgba(52, 183, 241, 0.05)', dur: 10 },
  { x: 80, y: 20, size: 220, color: 'rgba(37, 211, 102, 0.04)', dur: 14 },
];

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
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-between overflow-hidden"
      style={{
        background: 'linear-gradient(180deg, #0a0f14 0%, #111B21 30%, #0d1a14 70%, #0a0f14 100%)',
      }}
    >
      {/* Hidden audio element */}
      <audio ref={audioRef} autoPlay playsInline />

      {/* Animated background orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {ORBS.map((orb, i) => (
          <motion.div
            key={i}
            className="absolute rounded-full"
            style={{
              left: `${orb.x}%`,
              top: `${orb.y}%`,
              width: orb.size,
              height: orb.size,
              background: `radial-gradient(circle, ${orb.color}, transparent 70%)`,
              marginLeft: -orb.size / 2,
              marginTop: -orb.size / 2,
            }}
            animate={{
              x: [0, 30, -20, 10, 0],
              y: [0, -40, 20, -10, 0],
              scale: [1, 1.2, 0.9, 1.1, 1],
            }}
            transition={{
              duration: orb.dur,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
        ))}

        {/* Mesh grid lines for depth */}
        <div className="absolute inset-0 opacity-[0.03]"
          style={{
            backgroundImage: `
              linear-gradient(rgba(37, 211, 102, 0.5) 1px, transparent 1px),
              linear-gradient(90deg, rgba(37, 211, 102, 0.5) 1px, transparent 1px)
            `,
            backgroundSize: '60px 60px',
          }}
        />
      </div>

      {/* Top area - Contact Info */}
      <div className="relative z-10 flex flex-col items-center pt-16 pb-8">
        <motion.div
          initial={{ opacity: 0, y: -30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 150, damping: 20 }}
          className="flex flex-col items-center gap-5"
        >
          {/* Avatar with rings */}
          <div className="relative">
            {/* Outer pulsing rings */}
            <AnimatePresence>
              {(callStatus === 'ringing' || callStatus === 'connecting') && (
                <>
                  <motion.div
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 2.2, opacity: 0 }}
                    transition={{ duration: 2, repeat: Infinity, ease: 'easeOut' }}
                    className="absolute inset-0 rounded-full border border-zixo-primary/40"
                  />
                  <motion.div
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 1.8, opacity: 0 }}
                    transition={{ duration: 2, repeat: Infinity, ease: 'easeOut', delay: 0.6 }}
                    className="absolute inset-0 rounded-full border border-zixo-secondary/30"
                  />
                  <motion.div
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 1.5, opacity: 0 }}
                    transition={{ duration: 2, repeat: Infinity, ease: 'easeOut', delay: 1.2 }}
                    className="absolute inset-0 rounded-full border border-zixo-accent/20"
                  />
                </>
              )}
            </AnimatePresence>

            {/* Glowing halo when connected */}
            {callStatus === 'connected' && (
              <motion.div
                animate={{
                  boxShadow: [
                    '0 0 30px rgba(37, 211, 102, 0.3), 0 0 60px rgba(37, 211, 102, 0.1)',
                    '0 0 50px rgba(37, 211, 102, 0.5), 0 0 100px rgba(37, 211, 102, 0.2)',
                    '0 0 30px rgba(37, 211, 102, 0.3), 0 0 60px rgba(37, 211, 102, 0.1)',
                  ],
                }}
                transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                className="absolute inset-0 rounded-full"
              />
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
            <h2 className="text-2xl font-bold text-white tracking-tight">{remoteUser.displayName}</h2>
            <div className="mt-2 h-7 flex items-center justify-center">
              <AnimatePresence mode="wait">
                {callStatus === 'ringing' && !isIncoming && (
                  <motion.p
                    key="calling"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="text-sm text-zixo-text-secondary flex items-center gap-1"
                  >
                    Calling<span className="animate-pulse">.</span><span className="animate-pulse delay-100">.</span><span className="animate-pulse delay-200">.</span>
                  </motion.p>
                )}
                {callStatus === 'ringing' && isIncoming && (
                  <motion.p
                    key="incoming"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="text-sm text-zixo-primary font-medium"
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
                    className="text-sm font-mono tracking-widest"
                    style={{
                      background: 'linear-gradient(90deg, #25D366, #34B7F1)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
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
                  >
                    Call ended
                  </motion.p>
                )}
              </AnimatePresence>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Center area - Waveform */}
      <div className="relative z-10 flex-1 flex items-center justify-center">
        {callStatus === 'connected' && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            className="relative"
          >
            {/* Circular waveform visualization */}
            <div className="flex items-end justify-center gap-[3px] h-16">
              {Array.from({ length: 40 }).map((_, i) => (
                <motion.div
                  key={i}
                  className="w-[3px] rounded-full"
                  style={{
                    background: `linear-gradient(180deg, #25D366, #34B7F1)`,
                    boxShadow: isMuted ? 'none' : `0 0 6px rgba(37, 211, 102, 0.4)`,
                  }}
                  animate={{
                    height: isMuted ? 4 : [4, Math.random() * 36 + 8, 4],
                  }}
                  transition={{
                    duration: 0.4 + Math.random() * 0.6,
                    repeat: Infinity,
                    delay: i * 0.025,
                    ease: 'easeInOut',
                  }}
                />
              ))}
            </div>
            <p className="text-[10px] text-zixo-text-secondary/50 text-center mt-3 tracking-wider uppercase">
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
            {/* Animated signal waves */}
            {[0, 1, 2].map((i) => (
              <motion.div
                key={i}
                className="w-16 h-1 rounded-full"
                style={{
                  background: 'linear-gradient(90deg, transparent, #25D366, transparent)',
                }}
                animate={{
                  scaleX: [0.3, 1, 0.3],
                  opacity: [0.2, 0.6, 0.2],
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
      <div className="relative z-10 w-full pb-12 pt-4">
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
                  boxShadow: '0 0 30px rgba(234, 67, 53, 0.5), 0 0 60px rgba(234, 67, 53, 0.2)',
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
                  boxShadow: '0 0 30px rgba(37, 211, 102, 0.5), 0 0 60px rgba(37, 211, 102, 0.2)',
                }}
              >
                <motion.div
                  animate={{
                    boxShadow: [
                      '0 0 0 0 rgba(37, 211, 102, 0.4)',
                      '0 0 0 15px rgba(37, 211, 102, 0)',
                    ],
                  }}
                  transition={{ duration: 1.5, repeat: Infinity }}
                  className="absolute inset-0 rounded-full"
                />
                <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
              </motion.button>
              <span className="text-[10px] text-zixo-text-secondary tracking-wider uppercase">Answer</span>
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
            {/* Glass panel */}
            <div className="rounded-3xl p-6"
              style={{
                background: 'rgba(17, 27, 33, 0.7)',
                backdropFilter: 'blur(24px)',
                WebkitBackdropFilter: 'blur(24px)',
                border: '1px solid rgba(37, 211, 102, 0.1)',
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
                      boxShadow: '0 0 20px rgba(234, 67, 53, 0.2)',
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
                      boxShadow: '0 0 20px rgba(18, 140, 126, 0.2)',
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
                      boxShadow: '0 0 30px rgba(234, 67, 53, 0.5), 0 0 60px rgba(234, 67, 53, 0.2)',
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
      className="fixed inset-0 z-50 bg-black"
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
            background: 'radial-gradient(ellipse at center, #1a2a22 0%, #0a0f14 70%, #000 100%)',
          }}
        >
          {/* Animated rings for connecting state */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center"
          >
            <div className="relative inline-block">
              {/* Pulsing rings */}
              <motion.div
                animate={{ scale: [1, 1.6, 1], opacity: [0.3, 0, 0.3] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="absolute inset-0 rounded-full border border-zixo-primary/40"
              />
              <motion.div
                animate={{ scale: [1, 1.4, 1], opacity: [0.2, 0, 0.2] }}
                transition={{ duration: 2, repeat: Infinity, delay: 0.5 }}
                className="absolute inset-0 rounded-full border border-zixo-secondary/30"
              />
              <Avatar name={remoteUser.displayName} uid={remoteUser.uid} size="2xl" className="mx-auto breathe" />
            </div>
            <h3 className="text-xl font-semibold text-white mt-6">{remoteUser.displayName}</h3>
            <p className="text-xs text-zixo-text-secondary mt-2">
              {callStatus === 'ringing' && 'Calling...'}
              {callStatus === 'connecting' && 'Connecting...'}
            </p>
          </motion.div>
        </div>
      )}

      {/* Cinematic gradient overlay at top and bottom */}
      <div className="absolute inset-x-0 top-0 h-28 pointer-events-none z-[5]"
        style={{ background: 'linear-gradient(180deg, rgba(0,0,0,0.7) 0%, transparent 100%)' }}
      />
      <div className="absolute inset-x-0 bottom-0 h-40 pointer-events-none z-[5]"
        style={{ background: 'linear-gradient(0deg, rgba(0,0,0,0.8) 0%, transparent 100%)' }}
      />

      {/* Self View (PiP) */}
      <motion.div
        drag
        dragConstraints={{ top: 50, left: 20, right: window.innerWidth - 140, bottom: window.innerHeight - 200 }}
        className="absolute top-12 right-4 w-28 h-40 rounded-2xl overflow-hidden z-10 cursor-grab active:cursor-grabbing"
        style={{
          border: '2px solid rgba(37, 211, 102, 0.3)',
          boxShadow: '0 0 20px rgba(37, 211, 102, 0.15), 0 8px 32px rgba(0,0,0,0.4)',
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
            <div className="w-12 h-12 rounded-full gradient-primary flex items-center justify-center text-sm font-semibold text-white">
              You
            </div>
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
                    }}>
                      {formatDuration(callDuration)}
                    </span>
                  ) : 'Connecting...'}
                </p>
              </div>
              {/* Network quality */}
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  backdropFilter: 'blur(10px)',
                }}
              >
                <div className="flex gap-[2px]">
                  <div className="w-1 h-3 rounded-sm bg-zixo-success" />
                  <div className="w-1 h-4 rounded-sm bg-zixo-success" />
                  <div className="w-1 h-5 rounded-sm bg-zixo-success" />
                  <div className="w-1 h-6 rounded-sm bg-zixo-success/40" />
                </div>
                <span className="text-[10px] text-white/60 ml-1">Good</span>
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
            className="absolute bottom-0 left-0 right-0 p-6 pb-10 z-20"
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
                  border: '1px solid rgba(234, 67, 53, 0.3)',
                  boxShadow: '0 0 15px rgba(234, 67, 53, 0.2)',
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
                  border: '1px solid rgba(234, 67, 53, 0.3)',
                  boxShadow: '0 0 15px rgba(234, 67, 53, 0.2)',
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
                  boxShadow: '0 0 30px rgba(234, 67, 53, 0.5), 0 0 60px rgba(234, 67, 53, 0.2)',
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

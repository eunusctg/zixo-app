'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/zixo-utils';

interface SplashScreenProps {
  onComplete: () => void;
}

// Pre-computed particle positions to avoid hydration mismatch
const PARTICLES = [
  { x: 120, y: -200, tx: -80, ty: 150, dur: 3.5, delay: 0.3 },
  { x: -150, y: 100, tx: 60, ty: -250, dur: 4.2, delay: 1.1 },
  { x: 200, y: -50, tx: -120, ty: 300, dur: 3.8, delay: 0.6 },
  { x: -80, y: 200, tx: 180, ty: -100, dur: 4.5, delay: 1.5 },
  { x: 50, y: -300, tx: -200, ty: 50, dur: 3.2, delay: 0.9 },
  { x: -180, y: -100, tx: 100, ty: 250, dur: 4.8, delay: 0.2 },
  { x: 160, y: 150, tx: -50, ty: -200, dur: 3.6, delay: 1.8 },
  { x: -100, y: -250, tx: 150, ty: 100, dur: 4.1, delay: 0.5 },
  { x: 80, y: 300, tx: -180, ty: -150, dur: 3.9, delay: 1.3 },
  { x: -200, y: 50, tx: 70, ty: -300, dur: 4.4, delay: 0.8 },
  { x: 140, y: -180, tx: -90, ty: 200, dur: 3.3, delay: 1.6 },
  { x: -60, y: 250, tx: 200, ty: -50, dur: 4.7, delay: 0.4 },
  { x: 190, y: 80, tx: -140, ty: -280, dur: 3.7, delay: 1.0 },
  { x: -170, y: -200, tx: 30, ty: 180, dur: 4.3, delay: 0.7 },
  { x: 30, y: -80, tx: -160, ty: 320, dur: 3.4, delay: 1.4 },
  { x: -130, y: 320, tx: 110, ty: -120, dur: 4.6, delay: 0.1 },
  { x: 100, y: 200, tx: -200, ty: -80, dur: 3.1, delay: 1.9 },
  { x: -40, y: -320, tx: 170, ty: 60, dur: 4.0, delay: 0.6 },
  { x: 180, y: -160, tx: -70, ty: 280, dur: 3.8, delay: 1.2 },
  { x: -190, y: 280, tx: 90, ty: -200, dur: 4.2, delay: 0.3 },
];

export default function SplashScreen({ onComplete }: SplashScreenProps) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const timer = setTimeout(onComplete, 2500);
    const raf = requestAnimationFrame(() => setMounted(true));
    return () => {
      clearTimeout(timer);
      cancelAnimationFrame(raf);
    };
  }, [onComplete]);

  return (
    <div className="fixed inset-0 z-50 bg-zixo-bg flex items-center justify-center">
      {mounted && (
        <div className="absolute inset-0 overflow-hidden">
          {PARTICLES.map((p, i) => (
            <motion.div
              key={i}
              className="absolute w-2 h-2 rounded-full bg-zixo-primary/20"
              initial={{ x: p.x, y: p.y, scale: 0 }}
              animate={{ x: p.tx, y: p.ty, scale: [0, 1, 0], opacity: [0, 0.5, 0] }}
              transition={{ duration: p.dur, repeat: Infinity, delay: p.delay }}
            />
          ))}
        </div>
      )}

      <div className="flex flex-col items-center gap-6">
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ type: 'spring', stiffness: 200, damping: 15, delay: 0.2 }}
          className="relative"
        >
          <motion.div
            animate={{ scale: [1, 1.3, 1], opacity: [0.3, 0, 0.3] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="absolute -inset-4 rounded-3xl border-2 border-zixo-primary"
          />
          <div className="w-24 h-24 rounded-3xl gradient-primary flex items-center justify-center shadow-2xl glow-primary">
            <span className="text-5xl font-extrabold text-white font-heading">Z</span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="text-center"
        >
          <h1 className="text-4xl font-extrabold font-heading">
            <span className="gradient-primary bg-clip-text text-transparent">Zixo</span>
          </h1>
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1 }}
            className="text-sm text-zixo-text-secondary mt-2"
          >
            Free Video and Audio Calling App
          </motion.p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.5 }}
          className="flex gap-1.5 mt-8"
        >
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              className="w-2 h-2 rounded-full bg-zixo-primary"
              animate={{ scale: [1, 1.5, 1], opacity: [0.3, 1, 0.3] }}
              transition={{ duration: 0.8, repeat: Infinity, delay: i * 0.2 }}
            />
          ))}
        </motion.div>
      </div>
    </div>
  );
}

interface OnboardingScreenProps {
  onComplete: () => void;
  onSignIn: () => void;
  onSignUp: () => void;
}

export function OnboardingScreen({ onComplete, onSignIn, onSignUp }: OnboardingScreenProps) {
  const [page, setPage] = useState(0);

  const pages = [
    {
      emoji: '📞',
      title: 'Free Calls, Always',
      description: 'Crystal-clear audio and video calls with anyone, anywhere in the world. No limits, no charges, no social clutter.',
      gradient: 'from-zixo-primary to-zixo-secondary',
    },
    {
      emoji: '🔒',
      title: 'Private & Encrypted',
      description: 'End-to-end encryption on every message and call. Your conversations stay between you and the people you care about.',
      gradient: 'from-zixo-secondary to-zixo-success',
    },
    {
      emoji: '✨',
      title: 'No Social Media. Just Connection.',
      description: 'No feeds, no stories, no algorithms. Just pure, meaningful communication — the way it should be.',
      gradient: 'from-zixo-primary to-zixo-accent',
    },
  ];

  const currentPage = pages[page];

  return (
    <div className="fixed inset-0 z-50 bg-zixo-bg flex flex-col">
      <div className="flex justify-end p-4">
        <button
          onClick={onComplete}
          className="px-4 py-1.5 rounded-full text-xs font-medium text-zixo-text-secondary hover:text-zixo-text hover:bg-zixo-surface-light transition-colors"
        >
          Skip
        </button>
      </div>

      <div className="flex-1 flex flex-col items-center justify-center px-8">
        <AnimatePresence mode="wait">
          <motion.div
            key={page}
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
            transition={{ duration: 0.3 }}
            className="text-center"
          >
            <motion.div
              animate={{ y: [0, -8, 0] }}
              transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
              className="mb-8"
            >
              <div className={cn(
                'w-28 h-28 rounded-3xl bg-gradient-to-br flex items-center justify-center mx-auto shadow-2xl',
                currentPage.gradient
              )}>
                <span className="text-5xl">{currentPage.emoji}</span>
              </div>
            </motion.div>

            <h2 className="text-2xl font-bold font-heading text-zixo-text mb-3">{currentPage.title}</h2>
            <p className="text-sm text-zixo-text-secondary leading-relaxed max-w-[300px] mx-auto">
              {currentPage.description}
            </p>
          </motion.div>
        </AnimatePresence>
      </div>

      <div className="p-6 pb-10">
        <div className="flex items-center justify-center gap-2 mb-8">
          {pages.map((_, i) => (
            <motion.div
              key={i}
              className={cn(
                'rounded-full transition-all duration-300',
                i === page ? 'w-6 h-2 gradient-primary' : 'w-2 h-2 bg-zixo-surface-light'
              )}
              onClick={() => setPage(i)}
              style={{ cursor: 'pointer' }}
            />
          ))}
        </div>

        {page < pages.length - 1 ? (
          <motion.button
            whileTap={{ scale: 0.98 }}
            onClick={() => setPage(page + 1)}
            className="w-full py-3.5 rounded-xl gradient-primary text-white font-semibold text-sm glow-primary"
          >
            Next
          </motion.button>
        ) : (
          <motion.button
            whileTap={{ scale: 0.98 }}
            onClick={onSignIn}
            className="w-full py-3.5 rounded-xl gradient-primary text-white font-semibold text-sm glow-primary"
          >
            Get Started
          </motion.button>
        )}
      </div>
    </div>
  );
}

interface AuthScreenProps {
  mode: 'login' | 'signup' | 'forgot';
  onAuth: (data: { email: string; displayName?: string }) => void;
  onSwitchMode: (mode: 'login' | 'signup' | 'forgot') => void;
  onBack: () => void;
}

export function AuthScreen({ mode, onAuth, onSwitchMode, onBack }: AuthScreenProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Phone OTP state
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [displayName, setDisplayName] = useState('');
  const [showNameInput, setShowNameInput] = useState(false);
  const [pendingPhoneUser, setPendingPhoneUser] = useState<any>(null);

  const getFirebaseErrorMessage = (code: string): string => {
    switch (code) {
      case 'auth/operation-not-allowed':
        return 'Phone sign-in is not enabled yet. Please enable it in Firebase Console > Authentication > Sign-in method > Phone, or try Google Sign-In.';
      case 'auth/invalid-verification-code':
        return 'Invalid verification code. Please try again.';
      case 'auth/invalid-verification-id':
        return 'Verification expired. Please request a new code.';
      case 'auth/too-many-requests':
        return 'Too many attempts. Please try again later.';
      case 'auth/network-request-failed':
        return 'Network error. Check your internet connection.';
      case 'auth/popup-closed-by-user':
        return 'Sign-in was cancelled.';
      case 'auth/unauthorized-domain':
        return 'This domain is not authorized. Please add it in Firebase Console > Authentication > Settings > Authorized domains.';
      case 'auth/invalid-phone-number':
        return 'Invalid phone number. Use format: +1234567890';
      case 'auth/invalid-credential':
        return 'Invalid credentials. Please try again.';
      default:
        return code ? `Error: ${code.replace('auth/', '')}` : 'Something went wrong. Please try again.';
    }
  };

  const handleGoogleSignIn = async () => {
    setError(null);
    setIsLoading(true);
    try {
      const { loginWithGoogle } = await import('@/services/auth');
      const result = await loginWithGoogle();
      onAuth({ email: result.profile.email, displayName: result.profile.displayName });
    } catch (err: any) {
      const errorCode = err?.code || '';
      if (errorCode !== 'auth/popup-closed-by-user') {
        setError(getFirebaseErrorMessage(errorCode));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendOTP = async () => {
    if (!phoneNumber || phoneNumber.length < 10) {
      setError('Please enter a valid phone number with country code (e.g., +8801712345678)');
      return;
    }
    setError(null);
    setOtpLoading(true);
    try {
      const { initRecaptcha, sendOTP, resetPhoneAuth } = await import('@/services/auth');
      // Reset any previous recaptcha
      resetPhoneAuth();
      initRecaptcha('recaptcha-container');
      await sendOTP(phoneNumber);
      setOtpSent(true);
    } catch (err: any) {
      console.error('[Zixo Auth] Send OTP error:', err);
      const errorCode = err?.code || '';
      setError(getFirebaseErrorMessage(errorCode));
      // Reset recaptcha on error so it can be re-initialized
      const { resetPhoneAuth } = await import('@/services/auth');
      resetPhoneAuth();
    } finally {
      setOtpLoading(false);
    }
  };

  const handleVerifyOTP = async () => {
    if (!otpCode || otpCode.length < 6) {
      setError('Please enter the 6-digit verification code');
      return;
    }
    setError(null);
    setOtpLoading(true);
    try {
      const { verifyOTP } = await import('@/services/auth');
      const result = await verifyOTP(otpCode);

      // Check if this is a new user (no display name set)
      const isNewUser = !result.profile.displayName || result.profile.displayName === result.profile.email || result.profile.displayName.startsWith('@user');

      if (isNewUser) {
        // Show name input for new users
        setPendingPhoneUser(result);
        setShowNameInput(true);
      } else {
        onAuth({ email: result.profile.email, displayName: result.profile.displayName });
      }
    } catch (err: any) {
      console.error('[Zixo Auth] Verify OTP error:', err);
      const errorCode = err?.code || '';
      setError(getFirebaseErrorMessage(errorCode));
    } finally {
      setOtpLoading(false);
    }
  };

  const handleCompleteProfile = async () => {
    if (!displayName.trim()) {
      setError('Please enter your name to continue');
      return;
    }
    setError(null);
    setIsLoading(true);
    try {
      if (pendingPhoneUser) {
        const { updateUserProfile } = await import('@/services/auth');
        const username = `@${displayName.toLowerCase().replace(/\s+/g, '')}${Math.floor(Math.random() * 1000)}`;
        await updateUserProfile(pendingPhoneUser.user.uid, {
          displayName: displayName.trim(),
          username,
        });
        // Update the profile locally
        pendingPhoneUser.profile.displayName = displayName.trim();
        pendingPhoneUser.profile.username = username;
        onAuth({ email: pendingPhoneUser.profile.email, displayName: displayName.trim() });
      }
    } catch (err: any) {
      console.error('[Zixo Auth] Profile update error:', err);
      setError('Failed to update profile. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendOTP = async () => {
    setOtpSent(false);
    setOtpCode('');
    setError(null);
    const { resetPhoneAuth } = await import('@/services/auth');
    resetPhoneAuth();
  };

  return (
    <div className="fixed inset-0 z-50 bg-zixo-bg mesh-bg">
      <div className="h-full flex flex-col max-w-lg mx-auto">
        {/* Back Button */}
        <div className="p-4">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onBack}
            className="w-10 h-10 rounded-full bg-zixo-surface-light/50 flex items-center justify-center text-zixo-text-secondary hover:text-zixo-text transition-colors"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </motion.button>
        </div>

        {/* Content */}
        <div className="flex-1 flex flex-col justify-center px-6">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center mb-8"
          >
            {/* Logo */}
            <div className="w-16 h-16 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-4 glow-primary">
              <span className="text-3xl font-extrabold text-white font-heading">Z</span>
            </div>

            <h2 className="text-2xl font-bold font-heading text-zixo-text">
              {showNameInput ? 'Set Up Profile' : 'Welcome to Zixo'}
            </h2>
            <p className="text-sm text-zixo-text-secondary mt-1">
              {showNameInput
                ? 'Choose a display name for your account'
                : 'Sign in with your phone number or Google account'}
            </p>
          </motion.div>

          {/* Profile Setup (for new phone users) */}
          {showNameInput ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="space-y-4"
            >
              <div>
                <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Display Name</label>
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Your name"
                  autoFocus
                  className="w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors"
                />
              </div>

              <motion.button
                whileTap={{ scale: 0.98 }}
                type="button"
                onClick={handleCompleteProfile}
                disabled={isLoading}
                className={cn(
                  'w-full py-3.5 rounded-xl font-semibold text-sm transition-all min-h-[44px]',
                  isLoading
                    ? 'bg-zixo-primary/50 text-white/70'
                    : 'gradient-primary text-white glow-primary'
                )}
              >
                {isLoading ? (
                  <div className="flex items-center justify-center gap-2">
                    <motion.div
                      animate={{ rotate: 360 }}
                      transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                      className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"
                    />
                    Setting up...
                  </div>
                ) : (
                  'Continue'
                )}
              </motion.button>
            </motion.div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="space-y-4"
            >
              {/* Phone Number Input */}
              <div>
                <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Phone Number</label>
                <input
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+880 1XXX XXXXXX"
                  disabled={otpSent}
                  className="w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors disabled:opacity-50"
                />
              </div>

              {!otpSent ? (
                <motion.button
                  whileTap={{ scale: 0.98 }}
                  type="button"
                  onClick={handleSendOTP}
                  disabled={otpLoading}
                  className={cn(
                    'w-full py-3.5 rounded-xl font-semibold text-sm transition-all min-h-[44px]',
                    otpLoading
                      ? 'bg-zixo-primary/50 text-white/70'
                      : 'gradient-primary text-white glow-primary'
                  )}
                >
                  {otpLoading ? (
                    <div className="flex items-center justify-center gap-2">
                      <motion.div
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                        className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"
                      />
                      Sending...
                    </div>
                  ) : (
                    <div className="flex items-center justify-center gap-2">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                      </svg>
                      Send Verification Code
                    </div>
                  )}
                </motion.button>
              ) : (
                <>
                  <div>
                    <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Verification Code</label>
                    <input
                      type="text"
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="123456"
                      maxLength={6}
                      autoFocus
                      className="w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors text-center tracking-[0.5em] font-mono text-lg"
                    />
                  </div>
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    type="button"
                    onClick={handleVerifyOTP}
                    disabled={otpLoading}
                    className={cn(
                      'w-full py-3.5 rounded-xl font-semibold text-sm transition-all min-h-[44px]',
                      otpLoading
                        ? 'bg-zixo-primary/50 text-white/70'
                        : 'gradient-primary text-white glow-primary'
                    )}
                  >
                    {otpLoading ? (
                      <div className="flex items-center justify-center gap-2">
                        <motion.div
                          animate={{ rotate: 360 }}
                          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                          className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"
                        />
                        Verifying...
                      </div>
                    ) : (
                      'Verify & Continue'
                    )}
                  </motion.button>
                  <div className="flex items-center justify-between">
                    <button
                      type="button"
                      onClick={handleResendOTP}
                      className="text-xs text-zixo-primary hover:text-zixo-primary/80 transition-colors min-h-[44px] flex items-center"
                    >
                      Resend Code
                    </button>
                    <button
                      type="button"
                      onClick={() => { setOtpSent(false); setOtpCode(''); setError(null); }}
                      className="text-xs text-zixo-text-secondary hover:text-zixo-primary transition-colors min-h-[44px] flex items-center"
                    >
                      Change Number
                    </button>
                  </div>
                </>
              )}

              {/* Divider */}
              <div className="flex items-center gap-3 my-2">
                <div className="flex-1 h-px bg-white/10" />
                <span className="text-xs text-zixo-text-secondary">or</span>
                <div className="flex-1 h-px bg-white/10" />
              </div>

              {/* Google Sign-In */}
              <motion.button
                whileTap={{ scale: 0.98 }}
                onClick={handleGoogleSignIn}
                disabled={isLoading}
                className={cn(
                  "w-full py-3 rounded-xl bg-white/5 text-zixo-text font-medium text-sm border border-white/10 hover:bg-white/10 transition-colors flex items-center justify-center gap-3 min-h-[44px]",
                  isLoading && "opacity-50 cursor-not-allowed"
                )}
              >
                <svg width="18" height="18" viewBox="0 0 24 24">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                </svg>
                Continue with Google
              </motion.button>
            </motion.div>
          )}

          {/* Error Display */}
          {error && (
            <motion.div
              initial={{ opacity: 0, y: -5 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex items-start gap-2 px-4 py-2.5 rounded-xl bg-zixo-error/10 border border-zixo-error/20 mt-4"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-error shrink-0 mt-0.5">
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
              <p className="text-xs text-zixo-error leading-relaxed">{error}</p>
            </motion.div>
          )}

          {/* Terms */}
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="text-[10px] text-zixo-text-secondary/60 text-center mt-6 leading-relaxed"
          >
            By continuing, you agree to Zixo&apos;s Terms of Service and Privacy Policy.
            Your phone number is used for verification only.
          </motion.p>
        </div>
      </div>
      {/* reCAPTCHA container for phone auth */}
      <div id="recaptcha-container"></div>
    </div>
  );
}

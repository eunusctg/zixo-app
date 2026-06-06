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
    // Use rAF to defer setState outside of the effect body
    const raf = requestAnimationFrame(() => setMounted(true));
    return () => {
      clearTimeout(timer);
      cancelAnimationFrame(raf);
    };
  }, [onComplete]);

  return (
    <div className="fixed inset-0 z-50 bg-zixo-bg flex items-center justify-center">
      {/* Animated background particles - only render on client */}
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
        {/* Logo */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ type: 'spring', stiffness: 200, damping: 15, delay: 0.2 }}
          className="relative"
        >
          {/* Pulsing ring */}
          <motion.div
            animate={{
              scale: [1, 1.3, 1],
              opacity: [0.3, 0, 0.3],
            }}
            transition={{ duration: 2, repeat: Infinity }}
            className="absolute -inset-4 rounded-3xl border-2 border-zixo-primary"
          />
          <div className="w-24 h-24 rounded-3xl gradient-primary flex items-center justify-center shadow-2xl glow-primary">
            <span className="text-5xl font-extrabold text-white font-heading">Z</span>
          </div>
        </motion.div>

        {/* App Name */}
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

        {/* Loading indicator */}
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
      {/* Skip Button */}
      <div className="flex justify-end p-4">
        <button
          onClick={onComplete}
          className="px-4 py-1.5 rounded-full text-xs font-medium text-zixo-text-secondary hover:text-zixo-text hover:bg-zixo-surface-light transition-colors"
        >
          Skip
        </button>
      </div>

      {/* Content */}
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
            {/* Icon */}
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

      {/* Bottom */}
      <div className="p-6 pb-10">
        {/* Page indicators */}
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

        {/* Action Buttons */}
        {page < pages.length - 1 ? (
          <motion.button
            whileTap={{ scale: 0.98 }}
            onClick={() => setPage(page + 1)}
            className="w-full py-3.5 rounded-xl gradient-primary text-white font-semibold text-sm glow-primary"
          >
            Next
          </motion.button>
        ) : (
          <div className="flex flex-col gap-3">
            <motion.button
              whileTap={{ scale: 0.98 }}
              onClick={onSignUp}
              className="w-full py-3.5 rounded-xl gradient-primary text-white font-semibold text-sm glow-primary"
            >
              Create Account
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.98 }}
              onClick={onSignIn}
              className="w-full py-3.5 rounded-xl bg-zixo-surface-light text-zixo-text font-semibold text-sm border border-white/5"
            >
              Sign In
            </motion.button>
          </div>
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
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Phone OTP state
  const [phoneMode, setPhoneMode] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);

  const passwordStrength = () => {
    if (!password) return 0;
    let score = 0;
    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    return score;
  };

  const strengthColors = ['bg-zixo-error', 'bg-orange-500', 'bg-zixo-accent', 'bg-zixo-success'];
  const strengthLabels = ['Weak', 'Fair', 'Good', 'Strong'];

  const getFirebaseErrorMessage = (code: string): string => {
    switch (code) {
      case 'auth/email-already-in-use': return 'This email is already registered. Try signing in instead.';
      case 'invalid-email': 
      case 'auth/invalid-email': return 'Please enter a valid email address.';
      case 'auth/weak-password': return 'Password is too weak. Use at least 6 characters.';
      case 'auth/user-not-found': return 'No account found with this email.';
      case 'auth/wrong-password': return 'Incorrect password. Please try again.';
      case 'auth/invalid-credential': return 'Invalid email or password. Please try again.';
      case 'auth/too-many-requests': return 'Too many attempts. Please try again later.';
      case 'auth/network-request-failed': return 'Network error. Check your internet connection.';
      case 'auth/popup-closed-by-user': return 'Sign-in was cancelled.';
      case 'auth/unauthorized-domain': return 'This domain is not authorized for sign-in. Please contact support.';
      case 'auth/operation-not-allowed': return 'This sign-in method is not enabled. Please contact support.';
      case 'auth/popup-blocked': return 'Popup was blocked by your browser. Please allow popups and try again.';
      default: return code ? `Error: ${code}` : 'Something went wrong. Please try again.';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      if (mode === 'signup') {
        console.log('[Zixo Auth] Signing up with email:', email);
        const { registerWithEmail } = await import('@/services/auth');
        const result = await registerWithEmail(email, password, displayName || email.split('@')[0]);
        console.log('[Zixo Auth] Sign up successful:', result.profile.displayName);
        onAuth({ email, displayName: result.profile.displayName });
      } else if (mode === 'login') {
        console.log('[Zixo Auth] Logging in with email:', email);
        const { loginWithEmail } = await import('@/services/auth');
        const result = await loginWithEmail(email, password);
        console.log('[Zixo Auth] Login successful:', result.profile.displayName);
        onAuth({ email, displayName: result.profile.displayName });
      } else if (mode === 'forgot') {
        console.log('[Zixo Auth] Sending password reset to:', email);
        const { resetPassword } = await import('@/services/auth');
        await resetPassword(email);
        setError(null);
        alert('Password reset email sent! Check your inbox.');
      }
    } catch (err: any) {
      const errorCode = err?.code || '';
      console.error('[Zixo Auth] Error:', errorCode, err?.message);
      setError(getFirebaseErrorMessage(errorCode));
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setError(null);
    setIsLoading(true);
    try {
      console.log('[Zixo Auth] Starting Google sign-in');
      const { loginWithGoogle } = await import('@/services/auth');
      const result = await loginWithGoogle();
      console.log('[Zixo Auth] Google sign-in successful:', result.profile.displayName);
      onAuth({ email: result.profile.email, displayName: result.profile.displayName });
    } catch (err: any) {
      const errorCode = err?.code || '';
      console.error('[Zixo Auth] Google sign-in error:', errorCode, err?.message);
      if (errorCode !== 'auth/popup-closed-by-user') {
        setError(getFirebaseErrorMessage(errorCode));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendOTP = async () => {
    if (!phoneNumber || phoneNumber.length < 10) {
      setError('Please enter a valid phone number with country code (e.g., +1234567890)');
      return;
    }
    setError(null);
    setOtpLoading(true);
    try {
      const { initRecaptcha, sendOTP } = await import('@/services/auth');
      initRecaptcha('recaptcha-container');
      await sendOTP(phoneNumber);
      setOtpSent(true);
    } catch (err: any) {
      console.error('[Zixo Auth] Send OTP error:', err);
      setError(err?.message || 'Failed to send OTP. Please try again.');
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
      console.log('[Zixo Auth] Phone auth successful:', result.profile.displayName);
      onAuth({ email: result.profile.email, displayName: result.profile.displayName });
    } catch (err: any) {
      console.error('[Zixo Auth] Verify OTP error:', err);
      setError(err?.message || 'Invalid code. Please try again.');
    } finally {
      setOtpLoading(false);
    }
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
              {mode === 'login' && 'Welcome Back'}
              {mode === 'signup' && 'Create Account'}
              {mode === 'forgot' && 'Reset Password'}
            </h2>
            <p className="text-sm text-zixo-text-secondary mt-1">
              {mode === 'login' && 'Sign in to continue your conversations'}
              {mode === 'signup' && 'Join Zixo — free calls, free messages'}
              {mode === 'forgot' && 'Enter your email to receive a reset link'}
            </p>
          </motion.div>

          {phoneMode ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="space-y-4"
            >
              <div>
                <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Phone Number</label>
                <input
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+1 234 567 8900"
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
                      : 'gradient-primary text-white'
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
                    'Send Verification Code'
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
                        : 'gradient-primary text-white'
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
                      'Verify & Sign In'
                    )}
                  </motion.button>
                  <button
                    type="button"
                    onClick={() => { setOtpSent(false); setOtpCode(''); }}
                    className="w-full text-center text-xs text-zixo-text-secondary hover:text-zixo-primary transition-colors min-h-[44px]"
                  >
                    Didn&apos;t get a code? Try again
                  </button>
                </>
              )}

              <button
                type="button"
                onClick={() => { setPhoneMode(false); setOtpSent(false); setOtpCode(''); setError(null); }}
                className="w-full text-center text-xs text-zixo-text-secondary hover:text-zixo-primary transition-colors"
              >
                ← Back to email sign in
              </button>

              {/* Error Display */}
              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -5 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-zixo-error/10 border border-zixo-error/20"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-error shrink-0">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                  <p className="text-xs text-zixo-error">{error}</p>
                </motion.div>
              )}
            </motion.div>
          ) : (
          <motion.form
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            onSubmit={handleSubmit}
            className="space-y-4"
          >
            {mode === 'signup' && (
              <div>
                <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Display Name</label>
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Your name"
                  className="w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors"
                />
              </div>
            )}

            <div>
              <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                className="w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors"
              />
            </div>

            {mode !== 'forgot' && (
              <div>
                <label className="text-xs font-medium text-zixo-text-secondary mb-1.5 block">Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    className="w-full px-4 py-3 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors pr-12"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-zixo-text-secondary hover:text-zixo-text"
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      {showPassword ? (
                        <>
                          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                          <line x1="1" y1="1" x2="23" y2="23" />
                        </>
                      ) : (
                        <>
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                          <circle cx="12" cy="12" r="3" />
                        </>
                      )}
                    </svg>
                  </button>
                </div>

                {/* Password Strength (signup only) */}
                {mode === 'signup' && password && (
                  <div className="mt-2">
                    <div className="flex gap-1">
                      {[0, 1, 2, 3].map((i) => (
                        <div
                          key={i}
                          className={cn(
                            'h-1 flex-1 rounded-full transition-colors',
                            i < passwordStrength() ? strengthColors[passwordStrength() - 1] : 'bg-zixo-surface-light'
                          )}
                        />
                      ))}
                    </div>
                    <p className="text-[10px] text-zixo-text-secondary mt-1">
                      {strengthLabels[passwordStrength() - 1] || 'Too weak'}
                    </p>
                  </div>
                )}
              </div>
            )}

            <motion.button
              whileTap={{ scale: 0.98 }}
              type="submit"
              disabled={isLoading}
              className={cn(
                'w-full py-3.5 rounded-xl font-semibold text-sm transition-all',
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
                  Processing...
                </div>
              ) : (
                <>
                  {mode === 'login' && 'Sign In'}
                  {mode === 'signup' && 'Create Account'}
                  {mode === 'forgot' && 'Send Reset Link'}
                </>
              )}
            </motion.button>

            {/* Error Display */}
            {error && (
              <motion.div
                initial={{ opacity: 0, y: -5 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-zixo-error/10 border border-zixo-error/20"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-error shrink-0">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
                <p className="text-xs text-zixo-error">{error}</p>
              </motion.div>
            )}
          </motion.form>
          )}

          {/* Google Sign-In */}
          {mode !== 'forgot' && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="mt-4"
            >
              <div className="flex items-center gap-3 my-4">
                <div className="flex-1 h-px bg-white/10" />
                <span className="text-xs text-zixo-text-secondary">or continue with</span>
                <div className="flex-1 h-px bg-white/10" />
              </div>

              <div className="flex flex-col gap-3">
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
                    <path
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
                      fill="#4285F4"
                    />
                    <path
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                      fill="#34A853"
                    />
                    <path
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                      fill="#FBBC05"
                    />
                    <path
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                      fill="#EA4335"
                    />
                  </svg>
                  Google
                </motion.button>

                <motion.button
                  whileTap={{ scale: 0.98 }}
                  onClick={() => setPhoneMode(true)}
                  disabled={isLoading}
                  className={cn(
                    "w-full py-3 rounded-xl bg-white/5 text-zixo-text font-medium text-sm border border-white/10 hover:bg-white/10 transition-colors flex items-center justify-center gap-3 min-h-[44px]",
                    isLoading && "opacity-50 cursor-not-allowed"
                  )}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                  </svg>
                  Phone Number
                </motion.button>
              </div>
            </motion.div>
          )}

          {/* Footer Links */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="mt-6 text-center"
          >
            {mode === 'login' && (
              <p className="text-sm text-zixo-text-secondary">
                Don&apos;t have an account?{' '}
                <button onClick={() => onSwitchMode('signup')} className="text-zixo-primary font-medium hover:underline">
                  Sign Up
                </button>
              </p>
            )}
            {mode === 'signup' && (
              <p className="text-sm text-zixo-text-secondary">
                Already have an account?{' '}
                <button onClick={() => onSwitchMode('login')} className="text-zixo-primary font-medium hover:underline">
                  Sign In
                </button>
              </p>
            )}
            {mode === 'login' && (
              <button
                onClick={() => onSwitchMode('forgot')}
                className="text-xs text-zixo-text-secondary hover:text-zixo-primary mt-2 block mx-auto"
              >
                Forgot password?
              </button>
            )}
            {mode === 'forgot' && (
              <button
                onClick={() => onSwitchMode('login')}
                className="text-sm text-zixo-primary font-medium hover:underline"
              >
                Back to Sign In
              </button>
            )}
          </motion.div>
        </div>
      </div>
      {/* reCAPTCHA container for phone auth */}
      <div id="recaptcha-container"></div>
    </div>
  );
}

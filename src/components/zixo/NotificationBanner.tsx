'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/zixo-utils';

export interface BannerNotification {
  id: string;
  senderName: string;
  senderAvatar?: string;
  messagePreview: string;
  chatId?: string;
  timestamp: number;
  type: 'message' | 'call';
}

interface NotificationBannerProps {
  notifications: BannerNotification[];
  onDismiss: (id: string) => void;
  onTap: (notification: BannerNotification) => void;
}

export default function NotificationBanner({ notifications, onDismiss, onTap }: NotificationBannerProps) {
  return (
    <div className="fixed top-0 left-0 right-0 z-[100] pointer-events-none">
      <div className="flex flex-col items-center gap-2 pt-2 px-3">
        <AnimatePresence>
          {notifications.slice(0, 3).map((notif) => (
            <NotificationCard
              key={notif.id}
              notification={notif}
              onDismiss={onDismiss}
              onTap={onTap}
            />
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
}

function NotificationCard({
  notification,
  onDismiss,
  onTap,
}: {
  notification: BannerNotification;
  onDismiss: (id: string) => void;
  onTap: (notif: BannerNotification) => void;
}) {
  const [progress, setProgress] = useState(100);

  useEffect(() => {
    const duration = 5000; // 5 seconds
    const interval = 50;
    const steps = duration / interval;
    let current = steps;

    const timer = setInterval(() => {
      current--;
      setProgress((current / steps) * 100);
      if (current <= 0) {
        clearInterval(timer);
        onDismiss(notification.id);
      }
    }, interval);

    return () => clearInterval(timer);
  }, [notification.id, onDismiss]);

  const isCall = notification.type === 'call';

  return (
    <motion.div
      initial={{ y: -80, opacity: 0, scale: 0.95 }}
      animate={{ y: 0, opacity: 1, scale: 1 }}
      exit={{ y: -80, opacity: 0, scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 400, damping: 30 }}
      className="pointer-events-auto w-full max-w-sm"
    >
      <div
        onClick={() => onTap(notification)}
        className={cn(
          "relative overflow-hidden rounded-2xl shadow-2xl cursor-pointer",
          "bg-[#1F2C34]/95 backdrop-blur-xl border",
          isCall
            ? "border-green-500/30 shadow-green-500/10"
            : "border-white/10"
        )}
      >
        {/* Progress bar */}
        <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-white/5">
          <div
            className={cn(
              "h-full transition-all duration-50 ease-linear",
              isCall ? "bg-green-500/60" : "bg-[#25D366]/60"
            )}
            style={{ width: `${progress}%` }}
          />
        </div>

        <div className="p-3 flex items-center gap-3">
          {/* Avatar */}
          <div className="relative shrink-0">
            {notification.senderAvatar ? (
              <img
                src={notification.senderAvatar}
                alt={notification.senderName}
                className="w-10 h-10 rounded-full object-cover"
              />
            ) : (
              <div className={cn(
                "w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold text-white",
                isCall
                  ? "bg-gradient-to-br from-green-500 to-emerald-600"
                  : "bg-gradient-to-br from-[#25D366] to-[#128C7E]"
              )}>
                {notification.senderName[0]?.toUpperCase() || '?'}
              </div>
            )}
            {isCall && (
              <div className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-green-500 flex items-center justify-center">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="white" stroke="none">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
              </div>
            )}
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5">
              <p className="text-sm font-semibold text-[#E9EDEF] truncate">
                {notification.senderName}
              </p>
              <span className="text-[9px] text-[#8696A0] shrink-0">
                now
              </span>
            </div>
            <p className="text-xs text-[#8696A0] truncate mt-0.5">
              {isCall ? '📞 Incoming call' : notification.messagePreview}
            </p>
          </div>

          {/* Dismiss button */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDismiss(notification.id);
            }}
            className="shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-[#8696A0] hover:text-[#E9EDEF] hover:bg-white/10 transition-colors"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      </div>
    </motion.div>
  );
}

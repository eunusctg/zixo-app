'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/zixo-utils';

interface TypingIndicatorProps {
  className?: string;
}

export default function TypingIndicator({ className }: TypingIndicatorProps) {
  return (
    <div className={cn('flex items-center gap-1 px-3 py-2', className)}>
      <div className="flex gap-1">
        <span className="typing-dot-1 w-1.5 h-1.5 rounded-full bg-zixo-primary" />
        <span className="typing-dot-2 w-1.5 h-1.5 rounded-full bg-zixo-primary" />
        <span className="typing-dot-3 w-1.5 h-1.5 rounded-full bg-zixo-primary" />
      </div>
    </div>
  );
}

interface EmptyStateProps {
  title: string;
  subtitle?: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
  className?: string;
}

export function EmptyState({ title, subtitle, icon, action, className }: EmptyStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn(
        'flex flex-col items-center justify-center px-6 py-12 text-center',
        className
      )}
    >
      {icon && <div className="float mb-6 text-zixo-primary opacity-60">{icon}</div>}
      <h3 className="text-lg font-semibold text-zixo-text mb-2">{title}</h3>
      {subtitle && (
        <p className="text-sm text-zixo-text-secondary max-w-[260px]">{subtitle}</p>
      )}
      {action && <div className="mt-6">{action}</div>}
    </motion.div>
  );
}

interface EncryptionBadgeProps {
  className?: string;
}

export function EncryptionBadge({ className }: EncryptionBadgeProps) {
  return (
    <div
      className={cn(
        'flex items-center gap-1.5 px-2 py-1 rounded-full bg-zixo-surface-light/50 text-xs text-zixo-text-secondary',
        className
      )}
    >
      <svg width="10" height="12" viewBox="0 0 10 12" fill="none" className="text-zixo-success">
        <path
          d="M5 0C3.07 0 1.5 1.57 1.5 3.5V5H1C0.45 5 0 5.45 0 6V11C0 11.55 0.45 12 1 12H9C9.55 12 10 11.55 10 11V6C10 5.45 9.55 5 9 5H8.5V3.5C8.5 1.57 6.93 0 5 0ZM5 1.5C6.1 1.5 7 2.4 7 3.5V5H3V3.5C3 2.4 3.9 1.5 5 1.5ZM5 7C5.55 7 6 7.45 6 8V9.5C6 10.05 5.55 10.5 5 10.5C4.45 10.5 4 10.05 4 9.5V8C4 7.45 4.45 7 5 7Z"
          fill="currentColor"
        />
      </svg>
      End-to-end encrypted
    </div>
  );
}

interface OnlineStatusProps {
  online: boolean;
  lastSeen?: number;
  className?: string;
}

export function OnlineStatus({ online, lastSeen, className }: OnlineStatusProps) {
  if (online) {
    return (
      <span className={cn('text-xs text-zixo-online font-medium', className)}>Online</span>
    );
  }

  if (lastSeen) {
    const diff = Date.now() - lastSeen;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);

    let text = 'Offline';
    if (minutes < 60) text = `Last seen ${minutes}m ago`;
    else if (hours < 24) text = `Last seen ${hours}h ago`;
    else text = `Last seen ${new Date(lastSeen).toLocaleDateString()}`;

    return <span className={cn('text-xs text-zixo-text-secondary', className)}>{text}</span>;
  }

  return <span className={cn('text-xs text-zixo-text-secondary', className)}>Offline</span>;
}

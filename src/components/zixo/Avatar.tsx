'use client';

import React, { useState } from 'react';
import { getInitials, getAvatarColor, cn } from '@/lib/zixo-utils';

interface AvatarProps {
  name: string;
  uid: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  online?: boolean;
  avatarUrl?: string;
  className?: string;
}

const sizeMap = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-12 h-12 text-base',
  xl: 'w-16 h-16 text-lg',
  '2xl': 'w-24 h-24 text-2xl',
};

const onlineSizeMap = {
  sm: 'w-2.5 h-2.5 border-[1.5px]',
  md: 'w-3 h-3 border-2',
  lg: 'w-3.5 h-3.5 border-2',
  xl: 'w-4 h-4 border-2',
  '2xl': 'w-5 h-5 border-3',
};

export default function Avatar({ name, uid, size = 'md', online, avatarUrl, className }: AvatarProps) {
  const initials = getInitials(name);
  const gradient = getAvatarColor(uid);
  const [imgError, setImgError] = useState(false);
  const [lastAvatarUrl, setLastAvatarUrl] = useState(avatarUrl);

  // Reset error state when avatarUrl changes (e.g. user uploads new avatar)
  if (avatarUrl !== lastAvatarUrl) {
    setLastAvatarUrl(avatarUrl);
    setImgError(false);
  }

  const showImage = avatarUrl && !imgError;

  return (
    <div className={cn('relative inline-flex shrink-0', className)}>
      <div
        className={cn(
          'rounded-full bg-gradient-to-br flex items-center justify-center font-semibold text-white overflow-hidden',
          gradient,
          sizeMap[size]
        )}
      >
        {showImage ? (
          <img
            src={avatarUrl}
            alt={name}
            className="w-full h-full object-cover"
            onError={() => setImgError(true)}
          />
        ) : (
          initials
        )}
      </div>
      {online !== undefined && (
        <div
          className={cn(
            'absolute bottom-0 right-0 rounded-full border-zixo-bg bg-zixo-online',
            onlineSizeMap[size],
            online && 'pulse-online'
          )}
        />
      )}
    </div>
  );
}

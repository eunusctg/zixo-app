'use client';

import React from 'react';
import { getInitials, getAvatarColor, cn } from '@/lib/zixo-utils';

interface AvatarProps {
  name: string;
  uid: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  online?: boolean;
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

export default function Avatar({ name, uid, size = 'md', online, className }: AvatarProps) {
  const initials = getInitials(name);
  const gradient = getAvatarColor(uid);

  return (
    <div className={cn('relative inline-flex shrink-0', className)}>
      <div
        className={cn(
          'rounded-full bg-gradient-to-br flex items-center justify-center font-semibold text-white overflow-hidden',
          gradient,
          sizeMap[size]
        )}
      >
        {initials}
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

'use client';

import { useEffect } from 'react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[Zixo] Application error:', error);
  }, [error]);

  return (
    <div className="min-h-screen bg-zixo-bg flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-zixo-surface rounded-2xl p-6 text-center">
        <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center mx-auto mb-4">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h2 className="text-lg font-semibold text-zixo-text mb-2">Something went wrong</h2>
        <p className="text-sm text-zixo-text-secondary mb-1">{error.message || 'An unexpected error occurred'}</p>
        {error.digest && (
          <p className="text-xs text-zixo-text-secondary/60 mb-4">Error ID: {error.digest}</p>
        )}
        <button
          onClick={reset}
          className="px-6 py-2.5 rounded-xl gradient-primary text-white text-sm font-medium hover:opacity-90 transition-opacity"
        >
          Try again
        </button>
      </div>
    </div>
  );
}

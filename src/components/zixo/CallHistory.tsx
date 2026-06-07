'use client';

import React, { useState, useCallback, useRef, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import { cn, formatCallDuration, formatCallTime } from '@/lib/zixo-utils';
import type { CallRecord } from '@/stores/useZixoStore';
import type { ZixoUserProfile } from '@/services/auth';

// Dynamic import to avoid SSR issues on Cloudflare Pages / edge runtime
const QRCodeSVG = dynamic(
  () => import('qrcode.react').then((mod) => ({ default: mod.QRCodeSVG })),
  { ssr: false, loading: () => <div className="w-[180px] h-[180px] bg-white/10 rounded-xl" /> }
);

// ==================== HAVERSINE FORMULA ====================
function haversineDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371; // Earth radius in km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
    Math.cos((lat2 * Math.PI) / 180) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

interface NearbyUser {
  uid: string;
  displayName: string;
  zixoNumber: string;
  lat: number;
  lng: number;
  timestamp: number;
  distance: number;
}

interface CallHistoryListProps {
  calls: CallRecord[];
  currentUserId: string;
  onCallBack: (call: CallRecord) => void;
}

export function CallHistoryList({ calls, currentUserId, onCallBack }: CallHistoryListProps) {
  const [filter, setFilter] = useState<'all' | 'missed' | 'outgoing' | 'incoming'>('all');

  const filteredCalls = calls.filter((call) => {
    if (filter === 'all') return true;
    return call.direction === filter;
  });

  const filters = [
    { id: 'all' as const, label: 'All' },
    { id: 'missed' as const, label: 'Missed' },
    { id: 'outgoing' as const, label: 'Outgoing' },
    { id: 'incoming' as const, label: 'Incoming' },
  ];

  return (
    <div>
      {/* Filter Tabs */}
      <div className="flex gap-2 px-4 py-3 overflow-x-auto">
        {filters.map((f) => (
          <button
            key={f.id}
            onClick={() => setFilter(f.id)}
            className={cn(
              'px-4 py-1.5 rounded-full text-xs font-medium transition-colors shrink-0',
              filter === f.id
                ? 'gradient-primary text-white'
                : 'bg-zixo-surface-light text-zixo-text-secondary hover:text-zixo-text'
            )}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Call List */}
      {filteredCalls.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 px-6 text-center">
          <motion.div
            animate={{ y: [0, -8, 0] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            className="mb-6"
          >
            <div className="w-20 h-20 rounded-full bg-zixo-surface flex items-center justify-center">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
            </div>
          </motion.div>
          <h3 className="text-lg font-semibold text-zixo-text mb-2">No recent calls</h3>
          <p className="text-sm text-zixo-text-secondary">Your call history will appear here</p>
        </div>
      ) : (
        <div className="divide-y divide-white/5">
          {filteredCalls.map((call, i) => {
            const isIncoming = call.direction === 'incoming';
            const isMissed = call.direction === 'missed';
            const otherName = call.callerId === currentUserId ? call.receiverName : call.callerName;
            const otherUid = call.callerId === currentUserId ? call.receiverId : call.callerId;

            return (
              <motion.div
                key={call.id}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.03 }}
                className="flex items-center gap-3 px-4 py-3 hover:bg-zixo-surface/50 transition-colors"
              >
                <Avatar name={otherName} uid={otherUid} size="lg" />

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h4 className={cn(
                      'text-sm font-medium truncate',
                      isMissed ? 'text-zixo-error' : 'text-zixo-text'
                    )}>
                      {otherName}
                    </h4>
                  </div>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    {/* Direction icon */}
                    {call.direction === 'outgoing' && (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-zixo-success">
                        <line x1="7" y1="17" x2="17" y2="7" />
                        <polyline points="7 7 17 7 17 17" />
                      </svg>
                    )}
                    {call.direction === 'incoming' && (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-zixo-secondary">
                        <line x1="17" y1="7" x2="7" y2="17" />
                        <polyline points="17 17 7 17 7 7" />
                      </svg>
                    )}
                    {call.direction === 'missed' && (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-zixo-error">
                        <line x1="17" y1="7" x2="7" y2="17" />
                        <polyline points="17 17 7 17 7 7" />
                      </svg>
                    )}

                    {/* Call type icon */}
                    {call.type === 'video' ? (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-text-secondary">
                        <polygon points="23 7 16 12 23 17 23 7" />
                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                      </svg>
                    ) : (
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-zixo-text-secondary">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3" />
                      </svg>
                    )}

                    <span className="text-xs text-zixo-text-secondary">
                      {call.duration > 0 ? formatCallDuration(call.duration) : 'Missed'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="text-[11px] text-zixo-text-secondary">{formatCallTime(call.timestamp)}</span>
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={() => onCallBack(call)}
                    className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
                  >
                    {call.type === 'video' ? (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polygon points="23 7 16 12 23 17 23 7" />
                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                      </svg>
                    ) : (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                      </svg>
                    )}
                  </motion.button>
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </div>
  );
}

interface ContactsScreenProps {
  contacts: ZixoUserProfile[];
  onStartChat: (userId: string) => void;
  onStartCall: (userId: string, type: 'audio' | 'video') => void;
  onSearchUser?: (username: string) => void;
  allUsers?: ZixoUserProfile[];
  onSearchUsers?: (query: string) => Promise<ZixoUserProfile[]>;
  onSearchByZixoNumber?: (zixoNumber: string) => Promise<ZixoUserProfile | null>;
  currentUser?: ZixoUserProfile | null;
}

export function ContactsScreen({ contacts, onStartChat, onStartCall, onSearchUser, allUsers = [], onSearchUsers, onSearchByZixoNumber, currentUser }: ContactsScreenProps) {
  const [search, setSearch] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<ZixoUserProfile[]>([]);
  const [hasSearched, setHasSearched] = useState(false);
  const [showShareDialog, setShowShareDialog] = useState(false);
  const [shareLink, setShareLink] = useState('');
  const [zixoNumberSearch, setZixoNumberSearch] = useState('');
  const [zixoNumberSearching, setZixoNumberSearching] = useState(false);
  const [zixoNumberResult, setZixoNumberResult] = useState<ZixoUserProfile | null>(null);
  const [zixoNumberError, setZixoNumberError] = useState('');

  // QR Code state
  const [showQRCode, setShowQRCode] = useState(false);
  const [showQRScanner, setShowQRScanner] = useState(false);
  const [qrScanResult, setQrScanResult] = useState<ZixoUserProfile | null>(null);
  const [qrScanError, setQrScanError] = useState('');
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const scanAnimFrameRef = useRef<number | null>(null);
  const jsqrRef = useRef<any>(null);

  // Dynamically load jsQR at mount time (avoids SSR issues on Cloudflare Pages)
  useEffect(() => {
    import('jsqr').then((mod) => { jsqrRef.current = mod.default; }).catch(console.error);
  }, []);

  // Nearby state
  const [showNearby, setShowNearby] = useState(false);
  const [nearbyUsers, setNearbyUsers] = useState<NearbyUser[]>([]);
  const [nearbyLoading, setNearbyLoading] = useState(false);
  const [nearbyError, setNearbyError] = useState('');
  const [myLocation, setMyLocation] = useState<{ lat: number; lng: number } | null>(null);
  const nearbyIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const nearbyRtdbUnsubRef = useRef<(() => void) | null>(null);

  // Use allUsers as the default list, filter locally if search is short
  const displayUsers = searchResults.length > 0
    ? searchResults
    : search.trim().length > 0 && search.trim().length < 2
      ? allUsers.filter(
          (c) =>
            c.displayName.toLowerCase().includes(search.toLowerCase()) ||
            c.username.toLowerCase().includes(search.toLowerCase())
        )
      : allUsers;

  // Handle search - search Firestore when user types 2+ chars
  const handleSearch = useCallback(async (query: string) => {
    if (!query.trim() || !onSearchUsers) return;
    setIsSearching(true);
    setHasSearched(true);
    try {
      const results = await onSearchUsers(query.trim());
      setSearchResults(results);
    } catch (err) {
      console.error('[Zixo] Search failed:', err);
    } finally {
      setIsSearching(false);
    }
  }, [onSearchUsers]);

  // Debounced search
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);
    if (value.trim().length >= 2 && onSearchUsers) {
      searchTimeoutRef.current = setTimeout(() => handleSearch(value), 500);
    } else if (value.trim().length === 0) {
      setSearchResults([]);
      setHasSearched(false);
    }
  }, [handleSearch, onSearchUsers]);

  // Merge contacts (existing chat partners) with all discovered users
  // Mark which users already have chats
  const contactUids = new Set(contacts.map((c) => c.uid));

  // Separate into "existing chats" and "discover new people"
  const existingChatUsers = displayUsers.filter((u) => contactUids.has(u.uid));
  const newUsers = displayUsers.filter((u) => !contactUids.has(u.uid));

  // ==================== QR SCANNER LOGIC ====================
  const stopCamera = useCallback(() => {
    if (scanAnimFrameRef.current) {
      cancelAnimationFrame(scanAnimFrameRef.current);
      scanAnimFrameRef.current = null;
    }
    if (videoRef.current && videoRef.current.srcObject) {
      (videoRef.current.srcObject as MediaStream).getTracks().forEach((t) => t.stop());
      videoRef.current.srcObject = null;
    }
  }, []);

  useEffect(() => {
    if (!showQRScanner) return;

    let stream: MediaStream | null = null;
    const startScanner = async () => {
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
        });
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }

        const scanFrame = () => {
          if (!videoRef.current || !canvasRef.current || !showQRScanner) return;
          const video = videoRef.current;
          const canvas = canvasRef.current;
          if (video.readyState !== video.HAVE_ENOUGH_DATA) {
            scanAnimFrameRef.current = requestAnimationFrame(scanFrame);
            return;
          }
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          const ctx = canvas.getContext('2d');
          if (!ctx) return;
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          if (!jsqrRef.current) {
            scanAnimFrameRef.current = requestAnimationFrame(scanFrame);
            return;
          }
          const code = jsqrRef.current(imageData.data, imageData.width, imageData.height, {
            inversionAttempts: 'dontInvert',
          });
          if (code && code.data) {
            // Found QR code
            const data = code.data;
            let zixoNumber = '';
            if (data.startsWith('ZIXO:')) {
              zixoNumber = data.replace('ZIXO:', '');
            } else if (/^\d{8}$/.test(data)) {
              zixoNumber = data;
            }
            if (zixoNumber && onSearchByZixoNumber) {
              stopCamera();
              setShowQRScanner(false);
              onSearchByZixoNumber(zixoNumber).then((result) => {
                if (result) {
                  setQrScanResult(result);
                  setQrScanError('');
                } else {
                  setQrScanError(`No user found with Zixo Number: ${zixoNumber}`);
                }
              }).catch(() => {
                setQrScanError('Failed to look up scanned user');
              });
              return;
            }
          }
          scanAnimFrameRef.current = requestAnimationFrame(scanFrame);
        };
        scanAnimFrameRef.current = requestAnimationFrame(scanFrame);
      } catch (err: any) {
        console.error('[Zixo] Camera access failed:', err);
        setQrScanError('Camera access denied. Please allow camera access.');
        setShowQRScanner(false);
      }
    };
    startScanner();

    return () => {
      if (stream) stream.getTracks().forEach((t) => t.stop());
      if (scanAnimFrameRef.current) cancelAnimationFrame(scanAnimFrameRef.current);
    };
  }, [showQRScanner, onSearchByZixoNumber, stopCamera]);

  // ==================== NEARBY DISCOVERY LOGIC ====================
  const RTDB_BASE = 'https://zixo-call-default-rtdb.firebaseio.com';

  const getTimeAgo = useCallback((timestamp: number) => {
    const diff = Date.now() - timestamp;
    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    return `${Math.floor(diff / 3600000)}h ago`;
  }, []);

  const startNearbyDiscovery = useCallback(async () => {
    if (!currentUser) return;
    setNearbyLoading(true);
    setNearbyError('');

    try {
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error('Geolocation not supported'));
          return;
        }
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 15000,
        });
      });

      const lat = position.coords.latitude;
      const lng = position.coords.longitude;
      setMyLocation({ lat, lng });

      // Store our location in RTDB
      const secret = process.env.NEXT_PUBLIC_FIREBASE_DATABASE_SECRET || '';
      const storeUrl = `${RTDB_BASE}/nearby/${currentUser.uid}.json${secret ? `?auth=${secret}` : ''}`;
      await fetch(storeUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          lat,
          lng,
          timestamp: Date.now(),
          displayName: currentUser.displayName,
          zixoNumber: currentUser.zixoNumber || '',
        }),
      }).catch(() => {});

      // Fetch all nearby entries from RTDB
      const fetchUrl = `${RTDB_BASE}/nearby.json${secret ? `?auth=${secret}` : ''}`;
      const res = await fetch(fetchUrl);
      if (res.ok) {
        const data = await res.json();
        if (data && typeof data === 'object') {
          const thirtyMinAgo = Date.now() - 30 * 60 * 1000;
          const users: NearbyUser[] = [];
          Object.entries(data).forEach(([uid, info]: [string, any]) => {
            if (uid === currentUser.uid) return;
            if (!info || !info.lat || !info.lng) return;
            if (info.timestamp < thirtyMinAgo) return;
            const distance = haversineDistance(lat, lng, info.lat, info.lng);
            if (distance <= 1) {
              users.push({
                uid,
                displayName: info.displayName || 'Unknown',
                zixoNumber: info.zixoNumber || '',
                lat: info.lat,
                lng: info.lng,
                timestamp: info.timestamp,
                distance,
              });
            }
          });
          users.sort((a, b) => a.distance - b.distance);
          setNearbyUsers(users);
        }
      }

      // Refresh every 30 seconds
      nearbyIntervalRef.current = setInterval(async () => {
        // Update our timestamp
        await fetch(storeUrl, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ timestamp: Date.now() }),
        }).catch(() => {});

        // Re-fetch nearby
        const refreshRes = await fetch(fetchUrl);
        if (refreshRes.ok) {
          const refreshData = await refreshRes.json();
          if (refreshData && typeof refreshData === 'object') {
            const thirtyMinAgo = Date.now() - 30 * 60 * 1000;
            const users: NearbyUser[] = [];
            Object.entries(refreshData).forEach(([uid, info]: [string, any]) => {
              if (uid === currentUser.uid) return;
              if (!info || !info.lat || !info.lng) return;
              if (info.timestamp < thirtyMinAgo) return;
              const distance = haversineDistance(lat, lng, info.lat, info.lng);
              if (distance <= 1) {
                users.push({
                  uid,
                  displayName: info.displayName || 'Unknown',
                  zixoNumber: info.zixoNumber || '',
                  lat: info.lat,
                  lng: info.lng,
                  timestamp: info.timestamp,
                  distance,
                });
              }
            });
            users.sort((a, b) => a.distance - b.distance);
            setNearbyUsers(users);
          }
        }
      }, 30000);

    } catch (err: any) {
      console.error('[Zixo] Nearby discovery failed:', err);
      if (err?.code === 1) {
        setNearbyError('Location permission denied. Please allow location access.');
      } else if (err?.code === 2) {
        setNearbyError('Location unavailable. Please check your device settings.');
      } else if (err?.code === 3) {
        setNearbyError('Location request timed out. Please try again.');
      } else {
        setNearbyError('Failed to get location. Please try again.');
      }
    } finally {
      setNearbyLoading(false);
    }
  }, [currentUser]);

  const cleanupNearby = useCallback(() => {
    if (nearbyIntervalRef.current) {
      clearInterval(nearbyIntervalRef.current);
      nearbyIntervalRef.current = null;
    }
    // Remove our entry from RTDB
    if (currentUser) {
      const secret = process.env.NEXT_PUBLIC_FIREBASE_DATABASE_SECRET || '';
      const deleteUrl = `${RTDB_BASE}/nearby/${currentUser.uid}.json${secret ? `?auth=${secret}` : ''}`;
      fetch(deleteUrl, { method: 'DELETE' }).catch(() => {});
    }
    setMyLocation(null);
    setNearbyUsers([]);
  }, [currentUser]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      cleanupNearby();
      stopCamera();
    };
  }, [cleanupNearby, stopCamera]);

  const renderUserItem = (contact: ZixoUserProfile, i: number) => (
    <motion.div
      key={contact.uid}
      initial={{ opacity: 0, x: -10 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: Math.min(i * 0.03, 0.3) }}
      className="flex items-center gap-3 px-4 py-3 hover:bg-zixo-surface/50 transition-colors cursor-pointer"
      onClick={() => onStartChat(contact.uid)}
    >
      <Avatar name={contact.displayName} uid={contact.uid} size="lg" online={contact.online} />

      <div className="flex-1 min-w-0">
        <h4 className="text-sm font-medium text-zixo-text truncate">{contact.displayName}</h4>
        <p className="text-xs text-zixo-text-secondary truncate">{contact.username}</p>
      </div>

      <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
        <motion.button
          whileTap={{ scale: 0.85 }}
          onClick={() => onStartChat(contact.uid)}
          className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-primary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </motion.button>
        <motion.button
          whileTap={{ scale: 0.85 }}
          onClick={() => onStartCall(contact.uid, 'audio')}
          className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
          </svg>
        </motion.button>
        <motion.button
          whileTap={{ scale: 0.85 }}
          onClick={() => onStartCall(contact.uid, 'video')}
          className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polygon points="23 7 16 12 23 17 23 7" />
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
          </svg>
        </motion.button>
      </div>
    </motion.div>
  );

  return (
    <div className="pb-4">
      {/* Quick Add Methods */}
      <div className="px-4 py-3">
        <h3 className="text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider mb-3">Add Friends</h3>
        <div className="flex gap-3">
          {/* Share Link */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => {
              const link = `https://zixo-app-cfy.pages.dev`;
              setShareLink(link);
              setShowShareDialog(true);
              if (navigator.share) {
                navigator.share({
                  title: 'Join me on Zixo!',
                  text: 'Free video and audio calling app. No ads, no social media.',
                  url: link,
                }).catch(() => {});
              }
            }}
            className="flex-1 flex flex-col items-center gap-2 p-3 rounded-xl bg-zixo-surface border border-white/5 hover:border-zixo-primary/20 transition-colors"
          >
            <div className="w-10 h-10 rounded-full bg-zixo-primary/10 flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="18" cy="5" r="3" />
                <circle cx="6" cy="12" r="3" />
                <circle cx="18" cy="19" r="3" />
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
              </svg>
            </div>
            <span className="text-[11px] text-zixo-text-secondary">Share Link</span>
          </motion.button>

          {/* Search by Phone */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => {
              // Focus the search input and add phone prefix
              setSearch('+');
              const input = document.querySelector('input[placeholder*="Search"]') as HTMLInputElement;
              if (input) input.focus();
            }}
            className="flex-1 flex flex-col items-center gap-2 p-3 rounded-xl bg-zixo-surface border border-white/5 hover:border-zixo-primary/20 transition-colors"
          >
            <div className="w-10 h-10 rounded-full bg-zixo-secondary/10 flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#128C7E" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
            </div>
            <span className="text-[11px] text-zixo-text-secondary">By Phone</span>
          </motion.button>

          {/* Search by Zixo Number */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => {
              const input = document.querySelector('input[placeholder*="Zixo Number"]') as HTMLInputElement;
              if (input) input.focus();
            }}
            className="flex-1 flex flex-col items-center gap-2 p-3 rounded-xl bg-zixo-surface border border-white/5 hover:border-zixo-primary/20 transition-colors"
          >
            <div className="w-10 h-10 rounded-full bg-zixo-primary/10 flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#25D366" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="2" y="4" width="20" height="16" rx="2" />
                <path d="M7 15h0M2 9.5h20" />
              </svg>
            </div>
            <span className="text-[11px] text-zixo-text-secondary">Zixo No.</span>
          </motion.button>

          {/* Nearby */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => { setShowNearby(true); setShowQRCode(false); setShowQRScanner(false); }}
            className={cn(
              "flex-1 flex flex-col items-center gap-2 p-3 rounded-xl border transition-colors",
              showNearby ? "bg-zixo-accent/10 border-zixo-accent/30" : "bg-zixo-surface border-white/5 hover:border-zixo-accent/20"
            )}
          >
            <div className="w-10 h-10 rounded-full bg-zixo-accent/10 flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#34B7F1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="3" />
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
              </svg>
            </div>
            <span className="text-[11px] text-zixo-text-secondary">Nearby</span>
          </motion.button>

          {/* QR Code */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => { setShowQRCode(!showQRCode); setShowNearby(false); setShowQRScanner(false); }}
            className={cn(
              "flex-1 flex flex-col items-center gap-2 p-3 rounded-xl border transition-colors",
              showQRCode ? "bg-amber-500/10 border-amber-500/30" : "bg-zixo-surface border-white/5 hover:border-amber-500/20"
            )}
          >
            <div className="w-10 h-10 rounded-full bg-amber-500/10 flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
                <rect x="14" y="14" width="3" height="3" />
                <line x1="21" y1="14" x2="21" y2="14.01" />
                <line x1="21" y1="21" x2="21" y2="21.01" />
                <line x1="17" y1="18" x2="17" y2="18.01" />
              </svg>
            </div>
            <span className="text-[11px] text-zixo-text-secondary">QR Code</span>
          </motion.button>
        </div>
      </div>

      {/* Share Dialog */}
      <AnimatePresence>
        {showShareDialog && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className="mx-4 mb-3 p-4 rounded-xl bg-zixo-surface border border-zixo-primary/20"
          >
            <div className="flex items-center justify-between mb-2">
              <h4 className="text-sm font-semibold text-zixo-text">Share Zixo Link</h4>
              <button onClick={() => setShowShareDialog(false)} className="text-zixo-text-secondary hover:text-zixo-text">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={shareLink}
                readOnly
                className="flex-1 px-3 py-2 rounded-lg bg-zixo-surface-light text-zixo-text text-xs"
              />
              <motion.button
                whileTap={{ scale: 0.95 }}
                onClick={() => {
                  navigator.clipboard.writeText(shareLink).then(() => {
                    alert('Link copied to clipboard!');
                  }).catch(() => {});
                }}
                className="px-3 py-2 rounded-lg gradient-primary text-white text-xs font-medium"
              >
                Copy
              </motion.button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* QR Code Panel */}
      <AnimatePresence>
        {showQRCode && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className="mx-4 mb-3 p-4 rounded-xl bg-zixo-surface border border-amber-500/20"
          >
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-sm font-semibold text-zixo-text">QR Code</h4>
              <button onClick={() => { setShowQRCode(false); setShowQRScanner(false); }} className="text-zixo-text-secondary hover:text-zixo-text">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>

            {/* My QR Code */}
            {currentUser?.zixoNumber && (
              <div className="flex flex-col items-center mb-4">
                <div className="p-4 bg-white rounded-xl mb-2">
                  <QRCodeSVG
                    value={`ZIXO:${currentUser.zixoNumber}`}
                    size={180}
                    level="M"
                    includeMargin={false}
                  />
                </div>
                <p className="text-xs text-zixo-text-secondary text-center mt-1">
                  Your Zixo Number: <span className="font-mono font-semibold text-zixo-text">{currentUser.zixoNumber.slice(0,4)} {currentUser.zixoNumber.slice(4)}</span>
                </p>
                <p className="text-[10px] text-zixo-text-secondary/60 text-center mt-0.5">
                  Let others scan this QR code to find you
                </p>
              </div>
            )}

            {/* Scan QR Code Button */}
            {!showQRScanner ? (
              <motion.button
                whileTap={{ scale: 0.95 }}
                onClick={() => { setShowQRScanner(true); setQrScanResult(null); setQrScanError(''); }}
                className="w-full py-2.5 rounded-xl gradient-primary text-white text-sm font-medium flex items-center justify-center gap-2"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="7" height="7" />
                  <rect x="14" y="3" width="7" height="7" />
                  <rect x="3" y="14" width="7" height="7" />
                  <rect x="14" y="14" width="3" height="3" />
                  <line x1="21" y1="14" x2="21" y2="14.01" />
                  <line x1="21" y1="21" x2="21" y2="21.01" />
                </svg>
                Scan QR Code
              </motion.button>
            ) : (
              <div className="relative rounded-xl overflow-hidden">
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  className="w-full aspect-square object-cover bg-black rounded-xl"
                />
                <canvas ref={canvasRef} className="hidden" />
                {/* Scanning overlay */}
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                  <div className="w-48 h-48 border-2 border-amber-400 rounded-lg" />
                </div>
                {/* Cancel button */}
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    setShowQRScanner(false);
                    stopCamera();
                  }}
                  className="absolute bottom-3 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-black/60 text-white text-xs font-medium backdrop-blur-sm"
                >
                  Cancel Scan
                </motion.button>
              </div>
            )}

            {/* QR Scan Result */}
            {qrScanResult && (
              <motion.div
                initial={{ opacity: 0, y: 5 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-3"
              >
                <div
                  className="flex items-center gap-3 px-4 py-3 rounded-xl bg-zixo-surface-light border border-zixo-primary/10 cursor-pointer hover:bg-zixo-surface/70 transition-colors"
                  onClick={() => { onStartChat(qrScanResult.uid); setShowQRCode(false); }}
                >
                  <Avatar name={qrScanResult.displayName} uid={qrScanResult.uid} size="lg" online={qrScanResult.online} />
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-medium text-zixo-text truncate">{qrScanResult.displayName}</h4>
                    <p className="text-xs text-zixo-text-secondary truncate">{qrScanResult.username}</p>
                  </div>
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={(e) => { e.stopPropagation(); onStartChat(qrScanResult.uid); setShowQRCode(false); }}
                    className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-primary hover:bg-zixo-surface-light transition-colors"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                  </motion.button>
                </div>
              </motion.div>
            )}
            {qrScanError && (
              <p className="text-xs text-zixo-error mt-2 text-center">{qrScanError}</p>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Nearby Panel */}
      <AnimatePresence>
        {showNearby && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className="mx-4 mb-3 p-4 rounded-xl bg-zixo-surface border border-zixo-accent/20"
          >
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-sm font-semibold text-zixo-text">Nearby People</h4>
              <button onClick={() => { setShowNearby(false); cleanupNearby(); }} className="text-zixo-text-secondary hover:text-zixo-text">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>

            {nearbyLoading && (
              <div className="flex flex-col items-center py-6">
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                  className="w-8 h-8 border-2 border-zixo-accent/30 border-t-zixo-accent rounded-full mb-3"
                />
                <p className="text-xs text-zixo-text-secondary">Getting your location...</p>
              </div>
            )}

            {nearbyError && (
              <div className="text-center py-4">
                <p className="text-xs text-zixo-error mb-2">{nearbyError}</p>
                <button
                  onClick={startNearbyDiscovery}
                  className="text-xs text-zixo-accent hover:underline"
                >
                  Retry
                </button>
              </div>
            )}

            {!nearbyLoading && !nearbyError && myLocation && (
              <>
                <div className="flex items-center gap-2 mb-3 text-xs text-zixo-text-secondary">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#34B7F1" strokeWidth="2">
                    <circle cx="12" cy="12" r="3" />
                    <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
                  </svg>
                  <span>Showing users within 1 km</span>
                  <span className="ml-auto text-[10px] opacity-60">
                    {myLocation.lat.toFixed(3)}, {myLocation.lng.toFixed(3)}
                  </span>
                </div>

                {nearbyUsers.length === 0 ? (
                  <div className="text-center py-6">
                    <div className="w-12 h-12 rounded-full bg-zixo-accent/10 flex items-center justify-center mx-auto mb-3">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-accent">
                        <circle cx="12" cy="12" r="3" />
                        <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83" />
                      </svg>
                    </div>
                    <p className="text-sm text-zixo-text-secondary">No one nearby right now</p>
                    <p className="text-[10px] text-zixo-text-secondary/60 mt-1">Ask friends to open Zixo Nearby!</p>
                  </div>
                ) : (
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {nearbyUsers.map((user) => (
                      <motion.div
                        key={user.uid}
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-zixo-surface-light cursor-pointer hover:bg-zixo-surface/70 transition-colors"
                        onClick={() => { onStartChat(user.uid); setShowNearby(false); }}
                      >
                        <Avatar name={user.displayName} uid={user.uid} size="lg" />
                        <div className="flex-1 min-w-0">
                          <h4 className="text-sm font-medium text-zixo-text truncate">{user.displayName}</h4>
                          <p className="text-[10px] text-zixo-text-secondary font-mono">{user.zixoNumber}</p>
                        </div>
                        <div className="text-right shrink-0">
                          <p className="text-xs text-zixo-accent font-medium">
                            {user.distance < 0.1 ? `${Math.round(user.distance * 1000)}m` : `${user.distance.toFixed(1)}km`}
                          </p>
                          <p className="text-[10px] text-zixo-text-secondary/60">
                            {getTimeAgo(user.timestamp)}
                          </p>
                        </div>
                        <motion.button
                          whileTap={{ scale: 0.85 }}
                          onClick={(e) => { e.stopPropagation(); onStartChat(user.uid); setShowNearby(false); }}
                          className="w-8 h-8 rounded-full flex items-center justify-center text-zixo-primary hover:bg-zixo-surface-light transition-colors"
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                          </svg>
                        </motion.button>
                      </motion.div>
                    ))}
                  </div>
                )}
              </>
            )}

            {!nearbyLoading && !nearbyError && !myLocation && (
              <div className="text-center py-4">
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  onClick={startNearbyDiscovery}
                  className="px-4 py-2.5 rounded-xl gradient-primary text-white text-sm font-medium"
                >
                  Enable Location & Find Nearby
                </motion.button>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Search by Zixo Number */}
      {onSearchByZixoNumber && (
        <div className="px-4 py-3">
          <h3 className="text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider mb-3">Search by Zixo Number</h3>
          <div className="flex gap-2">
            <div className="relative flex-1">
              <svg
                className="absolute left-3 top-1/2 -translate-y-1/2 text-zixo-text-secondary"
                width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              >
                <rect x="2" y="4" width="20" height="16" rx="2" />
                <path d="M7 15h0M2 9.5h20" />
              </svg>
              <input
                type="text"
                value={zixoNumberSearch}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '').slice(0, 8);
                  setZixoNumberSearch(val);
                  setZixoNumberResult(null);
                  setZixoNumberError('');
                }}
                placeholder="Enter 8-digit Zixo Number"
                maxLength={8}
                className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors font-mono tracking-wider"
              />
            </div>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={async () => {
                const num = zixoNumberSearch.replace(/\s/g, '');
                if (num.length !== 8) {
                  setZixoNumberError('Please enter an 8-digit number');
                  return;
                }
                setZixoNumberSearching(true);
                setZixoNumberError('');
                setZixoNumberResult(null);
                try {
                  const result = await onSearchByZixoNumber(num);
                  if (result) {
                    setZixoNumberResult(result);
                  } else {
                    setZixoNumberError('No user found with this Zixo Number');
                  }
                } catch (err) {
                  setZixoNumberError('Search failed. Please try again.');
                } finally {
                  setZixoNumberSearching(false);
                }
              }}
              disabled={zixoNumberSearching || zixoNumberSearch.length !== 8}
              className={cn(
                'px-4 py-2.5 rounded-xl font-medium text-sm transition-all min-h-[44px]',
                zixoNumberSearching || zixoNumberSearch.length !== 8
                  ? 'bg-zixo-surface-light text-zixo-text-secondary cursor-not-allowed'
                  : 'gradient-primary text-white glow-primary'
              )}
            >
              {zixoNumberSearching ? (
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                  className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"
                />
              ) : 'Find'}
            </motion.button>
          </div>
          {zixoNumberError && (
            <p className="text-xs text-zixo-error mt-2">{zixoNumberError}</p>
          )}
          {zixoNumberResult && (
            <motion.div
              initial={{ opacity: 0, y: 5 }}
              animate={{ opacity: 1, y: 0 }}
              className="mt-3"
            >
              <div
                className="flex items-center gap-3 px-4 py-3 rounded-xl bg-zixo-surface border border-zixo-primary/10 cursor-pointer hover:bg-zixo-surface/70 transition-colors"
                onClick={() => onStartChat(zixoNumberResult.uid)}
              >
                <Avatar name={zixoNumberResult.displayName} uid={zixoNumberResult.uid} size="lg" online={zixoNumberResult.online} />
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-medium text-zixo-text truncate">{zixoNumberResult.displayName}</h4>
                  <p className="text-xs text-zixo-text-secondary truncate">{zixoNumberResult.username}</p>
                </div>
                <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={() => onStartChat(zixoNumberResult.uid)}
                    className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-primary hover:bg-zixo-surface-light transition-colors"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    onClick={() => onStartCall(zixoNumberResult.uid, 'audio')}
                    className="w-9 h-9 rounded-full flex items-center justify-center text-zixo-secondary hover:bg-zixo-surface-light transition-colors"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z" />
                    </svg>
                  </motion.button>
                </div>
              </div>
            </motion.div>
          )}
        </div>
      )}

      {/* Search */}
      <div className="px-4 py-3">
        <div className="relative">
          <svg
            className="absolute left-3 top-1/2 -translate-y-1/2 text-zixo-text-secondary"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by name or @username..."
            className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-zixo-surface-light text-zixo-text text-sm placeholder-zixo-text-secondary border border-transparent focus:border-zixo-primary/30 focus:outline-none transition-colors"
          />
          {isSearching && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                className="w-4 h-4 border-2 border-zixo-primary/30 border-t-zixo-primary rounded-full"
              />
            </div>
          )}
        </div>
      </div>

      {/* Existing Chat Partners */}
      {existingChatUsers.length > 0 && (
        <>
          <div className="px-4 py-2">
            <h3 className="text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider">
              Your Chats ({existingChatUsers.length})
            </h3>
          </div>
          <div className="divide-y divide-white/5">
            {existingChatUsers.map((contact, i) => renderUserItem(contact, i))}
          </div>
        </>
      )}

      {/* New Users (Discovery) */}
      {newUsers.length > 0 && (
        <>
          <div className="px-4 py-2 mt-2">
            <h3 className="text-xs font-semibold text-zixo-text-secondary uppercase tracking-wider">
              People on Zixo ({newUsers.length})
            </h3>
          </div>
          <div className="divide-y divide-white/5">
            {newUsers.map((contact, i) => renderUserItem(contact, i + existingChatUsers.length))}
          </div>
        </>
      )}

      {/* Searching indicator */}
      {isSearching && displayUsers.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
            className="w-8 h-8 border-2 border-zixo-primary/30 border-t-zixo-primary rounded-full mb-4"
          />
          <p className="text-sm text-zixo-text-secondary">Searching...</p>
        </div>
      )}

      {/* Empty state */}
      {displayUsers.length === 0 && !isSearching && hasSearched && (
        <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
          <div className="w-16 h-16 rounded-full bg-zixo-surface flex items-center justify-center mb-4">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>
          <h3 className="text-base font-semibold text-zixo-text mb-1">No users found</h3>
          <p className="text-sm text-zixo-text-secondary">Try a different name or username</p>
        </div>
      )}

      {displayUsers.length === 0 && !isSearching && !hasSearched && (
        <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
          <div className="w-16 h-16 rounded-full bg-zixo-surface flex items-center justify-center mb-4">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-zixo-text-secondary">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </div>
          <h3 className="text-base font-semibold text-zixo-text mb-1">Discover people</h3>
          <p className="text-sm text-zixo-text-secondary">Search by name or @username to find and chat with people on Zixo</p>
        </div>
      )}
    </div>
  );
}

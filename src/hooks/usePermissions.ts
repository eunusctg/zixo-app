'use client';

import { useState, useCallback, useRef } from 'react';

export type PermissionStatus = 'granted' | 'denied' | 'prompt' | 'unavailable';

export interface PermissionState {
  camera: PermissionStatus;
  microphone: PermissionStatus;
  location: PermissionStatus;
}

export interface PermissionRequest {
  type: 'camera' | 'microphone' | 'location';
  status: 'requesting' | 'granted' | 'denied' | 'error';
  message?: string;
}

/**
 * Hook to manage camera, microphone, and location permissions
 * Provides pre-flight permission checks and request flows
 */
export function usePermissions() {
  const [permissions, setPermissions] = useState<PermissionState>({
    camera: 'prompt',
    microphone: 'prompt',
    location: 'prompt',
  });

  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [permissionRequests, setPermissionRequests] = useState<PermissionRequest[]>([]);
  const [currentRequestIndex, setCurrentRequestIndex] = useState(0);
  const resolveRef = useRef<((granted: boolean) => void) | null>(null);

  // Check current permission status without requesting
  const checkPermissions = useCallback(async (): Promise<PermissionState> => {
    const result: PermissionState = {
      camera: 'prompt',
      microphone: 'prompt',
      location: 'prompt',
    };

    try {
      // Check camera and microphone
      if (navigator.permissions) {
        try {
          const camStatus = await navigator.permissions.query({ name: 'camera' as PermissionName });
          result.camera = camStatus.state as PermissionStatus;
        } catch {
          // 'camera' permission name not supported in all browsers
        }

        try {
          const micStatus = await navigator.permissions.query({ name: 'microphone' as PermissionName });
          result.microphone = micStatus.state as PermissionStatus;
        } catch {
          // 'microphone' permission name not supported in all browsers
        }

        try {
          const geoStatus = await navigator.permissions.query({ name: 'geolocation' });
          result.location = geoStatus.state as PermissionStatus;
        } catch {
          // Geolocation not supported
        }
      }
    } catch (err) {
      console.warn('[Zixo Permissions] Check failed:', err);
    }

    setPermissions(result);
    return result;
  }, []);

  // Request a single permission
  const requestPermission = useCallback(async (type: 'camera' | 'microphone' | 'location'): Promise<boolean> => {
    try {
      if (type === 'camera' || type === 'microphone') {
        // Request camera/mic via getUserMedia
        const constraints: MediaStreamConstraints = {
          audio: type === 'microphone' || undefined,
          video: type === 'camera' ? { width: 720, height: 1280, facingMode: 'user' } : undefined,
        };

        // If we only need one of audio/video, only request that
        if (type === 'camera') {
          constraints.audio = false;
          constraints.video = { width: 720, height: 1280, facingMode: 'user' };
        } else if (type === 'microphone') {
          constraints.audio = true;
          constraints.video = false;
        }

        const stream = await navigator.mediaDevices.getUserMedia(constraints);
        // Immediately stop the tracks - we just needed permission
        stream.getTracks().forEach(track => track.stop());

        setPermissions(prev => ({ ...prev, [type]: 'granted' }));
        return true;
      }

      if (type === 'location') {
        return new Promise<boolean>((resolve) => {
          if (!navigator.geolocation) {
            setPermissions(prev => ({ ...prev, location: 'unavailable' }));
            resolve(false);
            return;
          }

          navigator.geolocation.getCurrentPosition(
            (position) => {
              setPermissions(prev => ({ ...prev, location: 'granted' }));
              resolve(true);
            },
            (error) => {
              if (error.code === error.PERMISSION_DENIED) {
                setPermissions(prev => ({ ...prev, location: 'denied' }));
              }
              resolve(false);
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
          );
        });
      }

      return false;
    } catch (err: any) {
      if (err?.name === 'NotAllowedError' || err?.name === 'PermissionDeniedError') {
        setPermissions(prev => ({ ...prev, [type]: 'denied' }));
        return false;
      }
      if (err?.name === 'NotFoundError') {
        setPermissions(prev => ({ ...prev, [type]: 'unavailable' }));
        return false;
      }
      console.warn(`[Zixo Permissions] ${type} request failed:`, err);
      return false;
    }
  }, []);

  // Request all permissions needed for a call (with beautiful modal UI)
  const requestCallPermissions = useCallback(async (callType: 'audio' | 'video'): Promise<boolean> => {
    // Build the list of permissions we need
    const needed: PermissionRequest[] = [];

    if (callType === 'video') {
      needed.push({ type: 'camera', status: 'requesting', message: 'Camera access is needed for video calls' });
    }
    needed.push({ type: 'microphone', status: 'requesting', message: 'Microphone access is needed for calls' });
    needed.push({ type: 'location', status: 'requesting', message: 'Location sharing for emergency services' });

    // Check which permissions are already granted
    const currentStatus = await checkPermissions();

    const needsRequest = needed.filter(req => {
      const status = currentStatus[req.type];
      return status !== 'granted'; // Need to request if not granted
    });

    // If all permissions are already granted, proceed
    if (needsRequest.length === 0) {
      return true;
    }

    // Show the permission modal
    setPermissionRequests(needsRequest);
    setCurrentRequestIndex(0);
    setShowPermissionModal(true);

    // Return a promise that resolves when the user completes the flow
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve;
    });
  }, [checkPermissions]);

  // Process the next permission in the queue
  const processNextPermission = useCallback(async () => {
    if (currentRequestIndex >= permissionRequests.length) {
      // All permissions processed
      setShowPermissionModal(false);
      const allGranted = permissionRequests.every(req => req.status === 'granted');
      // For calls, we need at least microphone granted (camera is optional for video calls)
      const micGranted = permissionRequests.find(req => req.type === 'microphone')?.status === 'granted';
      resolveRef.current?.(micGranted || false);
      resolveRef.current = null;
      return;
    }

    const currentReq = permissionRequests[currentRequestIndex];
    const granted = await requestPermission(currentReq.type);

    setPermissionRequests(prev => prev.map((req, i) =>
      i === currentRequestIndex
        ? { ...req, status: granted ? 'granted' : 'denied' }
        : req
    ));

    setCurrentRequestIndex(prev => prev + 1);
  }, [currentRequestIndex, permissionRequests, requestPermission]);

  // Skip a permission
  const skipPermission = useCallback(() => {
    setPermissionRequests(prev => prev.map((req, i) =>
      i === currentRequestIndex
        ? { ...req, status: 'denied' }
        : req
    ));
    setCurrentRequestIndex(prev => prev + 1);

    // Process next after a brief delay
    setTimeout(() => {
      if (currentRequestIndex + 1 >= permissionRequests.length) {
        setShowPermissionModal(false);
        const micGranted = permissionRequests.find(req => req.type === 'microphone')?.status === 'granted';
        resolveRef.current?.(micGranted || false);
        resolveRef.current = null;
      }
    }, 100);
  }, [currentRequestIndex, permissionRequests]);

  // Cancel permission flow
  const cancelPermissions = useCallback(() => {
    setShowPermissionModal(false);
    resolveRef.current?.(false);
    resolveRef.current = null;
  }, []);

  return {
    permissions,
    checkPermissions,
    requestPermission,
    requestCallPermissions,
    showPermissionModal,
    permissionRequests,
    currentRequestIndex,
    processNextPermission,
    skipPermission,
    cancelPermissions,
  };
}

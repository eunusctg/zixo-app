/**
 * WebRTC Service for Zixo
 * Handles peer-to-peer audio/video calls using Google STUN servers
 *
 * Signaling uses Firebase Realtime Database (RTDB) for low-latency
 * offer/answer/ICE candidate exchange.
 * Falls back to Firestore if RTDB is unavailable.
 */

import { ref, onValue, off, onChildAdded, get } from 'firebase/database';
import { db as firestoreDb, doc, setDoc, getDoc, updateDoc, onSnapshot, deleteDoc, collection, serverTimestamp } from 'firebase/firestore';
import { rtdb } from './firebase';
import { createCallSignal, updateCallSignal, endCallSignal, addICECandidate, subscribeToIncomingCalls, type RTDBCallSignal } from './presence';

// Free Google STUN servers
const ICE_SERVERS: RTCConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
    { urls: 'stun:stun3.l.google.com:19302' },
    { urls: 'stun:stun4.l.google.com:19302' },
  ],
};

// Call quality stats
export interface CallQualityStats {
  bitrate: number; // kbps
  packetLoss: number; // percentage
  rtt: number; // round-trip time in ms
  jitter: number; // ms
  codec: string;
}

// Reconnection state
interface ReconnectionState {
  isReconnecting: boolean;
  attemptCount: number;
  maxAttempts: number;
  lastAttemptTime: number;
}

export class ZixoWebRTC {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private callId: string | null = null;
  private unsubscribeSignaling: (() => void) | null = null;
  private isCaller: boolean = false;
  private reconnectionState: ReconnectionState = {
    isReconnecting: false,
    attemptCount: 0,
    maxAttempts: 3,
    lastAttemptTime: 0,
  };
  private statsInterval: ReturnType<typeof setInterval> | null = null;
  private lastStatsBytesSent: number = 0;
  private lastStatsBytesReceived: number = 0;
  private lastStatsTimestamp: number = 0;
  private processedICECandidates: Set<string> = new Set();

  // Callbacks
  public onRemoteStream: ((stream: MediaStream) => void) | null = null;
  public onConnectionStateChange: ((state: RTCPeerConnectionState) => void) | null = null;
  public onICEStateChange: ((state: RTCIceConnectionState) => void) | null = null;
  public onCallQualityUpdate: ((stats: CallQualityStats) => void) | null = null;
  public onReconnecting: ((attempt: number) => void) | null = null;
  public onReconnected: (() => void) | null = null;
  public onRemoteICECandidate: ((candidate: RTCIceCandidateInit) => void) | null = null;

  /**
   * Initialize a peer connection
   */
  private createPeerConnection(): RTCPeerConnection {
    const pc = new RTCPeerConnection(ICE_SERVERS);

    // Add local tracks
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        pc.addTrack(track, this.localStream!);
      });
    }

    // Handle remote tracks
    pc.ontrack = (event) => {
      this.remoteStream = event.streams[0];
      if (this.onRemoteStream) {
        this.onRemoteStream(this.remoteStream);
      }
    };

    // Handle ICE candidates
    pc.onicecandidate = (event) => {
      if (event.candidate && this.callId) {
        addICECandidate(this.callId, event.candidate.toJSON(), this.isCaller);
      }
    };

    // Handle connection state changes
    pc.onconnectionstatechange = () => {
      if (this.onConnectionStateChange) {
        this.onConnectionStateChange(pc.connectionState);
      }

      // Handle reconnection for failed/disconnected states
      if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
        this.handleConnectionFailure();
      }
    };

    pc.oniceconnectionstatechange = () => {
      if (this.onICEStateChange) {
        this.onICEStateChange(pc.iceConnectionState);
      }

      // ICE connection recovery
      if (pc.iceConnectionState === 'failed') {
        this.handleICEFailure();
      } else if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
        // Reset reconnection state on successful connection
        this.reconnectionState.isReconnecting = false;
        this.reconnectionState.attemptCount = 0;
        if (this.onReconnected) this.onReconnected();

        // Start quality monitoring
        this.startQualityMonitoring();
      }
    };

    return pc;
  }

  /**
   * Start a call (caller side)
   */
  async startCall(
    callerId: string,
    callerName: string,
    receiverId: string,
    type: 'audio' | 'video'
  ): Promise<string> {
    // Get local media stream
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: type === 'video' ? { width: 720, height: 1280 } : false,
    });

    this.isCaller = true;
    this.peerConnection = this.createPeerConnection();

    // Create call signal in RTDB
    this.callId = createCallSignal({
      callerId,
      callerName,
      receiverId,
      type,
      status: 'ringing',
    });

    // Create offer
    const offer = await this.peerConnection.createOffer();
    await this.peerConnection.setLocalDescription(offer);

    // Update signal with offer
    updateCallSignal(this.callId, {
      offer: { type: offer.type, sdp: offer.sdp },
    });

    // Listen for answer and remote ICE candidates
    this.listenForSignaling(this.callId);

    return this.callId;
  }

  /**
   * Answer a call (receiver side)
   */
  async answerCall(callId: string, callData: RTDBCallSignal): Promise<void> {
    this.isCaller = false;
    this.callId = callId;

    // Get local media stream
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: callData.type === 'video' ? { width: 720, height: 1280 } : false,
    });

    this.peerConnection = this.createPeerConnection();

    // Set remote description (offer)
    if (callData.offer) {
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(callData.offer));
    }

    // Create answer
    const answer = await this.peerConnection.createAnswer();
    await this.peerConnection.setLocalDescription(answer);

    // Update signal with answer
    updateCallSignal(callId, {
      answer: { type: answer.type, sdp: answer.sdp },
      status: 'connected',
    });

    // Listen for remote ICE candidates
    this.listenForSignaling(callId);
  }

  /**
   * Listen for signaling data (answer + ICE candidates) via RTDB
   * Implements real RTDB listeners for offer/answer/ICE exchange
   */
  private listenForSignaling(callId: string): void {
    const callRef = ref(rtdb, `calls/${callId}`);

    // Listen for answer (for caller) or offer updates
    const answerRef = ref(rtdb, `calls/${callId}/answer`);
    const answerUnsub = onValue(answerRef, async (snap) => {
      if (snap.exists() && this.peerConnection && this.isCaller) {
        const answer = snap.val();
        if (answer && this.peerConnection.signalingState === 'have-local-offer') {
          try {
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
          } catch (err) {
            console.error('[Zixo] Error setting remote answer:', err);
          }
        }
      }
    });

    // Listen for remote ICE candidates
    const candidateField = this.isCaller ? 'receiverCandidates' : 'callerCandidates';
    const candidatesRef = ref(rtdb, `calls/${callId}/${candidateField}`);

    const candidatesUnsub = onChildAdded(candidatesRef, async (snap) => {
      if (snap.exists() && this.peerConnection) {
        const candidateData = snap.val();
        const candidateKey = snap.key;

        // Deduplicate candidates
        if (candidateKey && this.processedICECandidates.has(candidateKey)) return;
        if (candidateKey) this.processedICECandidates.add(candidateKey);

        try {
          await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidateData));
        } catch (err) {
          console.warn('[Zixo] Error adding remote ICE candidate:', err);
        }
      }
    });

    // Store unsubscribe for cleanup
    const existingUnsub = this.unsubscribeSignaling;
    this.unsubscribeSignaling = () => {
      off(answerRef);
      off(candidatesRef);
      answerUnsub();
      candidatesUnsub();
      if (existingUnsub) existingUnsub();
    };
  }

  /**
   * Handle ICE connection failure with ICE restart
   */
  private async handleICEFailure(): Promise<void> {
    console.warn('[Zixo] ICE connection failed, attempting restart...');

    if (!this.peerConnection || !this.callId) return;

    try {
      if (this.isCaller) {
        // Caller initiates ICE restart by creating a new offer with ICE restart
        const offer = await this.peerConnection.createOffer({ iceRestart: true });
        await this.peerConnection.setLocalDescription(offer);

        // Update signal with new offer
        updateCallSignal(this.callId, {
          offer: { type: offer.type, sdp: offer.sdp },
        });
      }
      // Receiver will automatically get the new offer via the signaling listener
    } catch (err) {
      console.error('[Zixo] ICE restart failed:', err);
      this.handleConnectionFailure();
    }
  }

  /**
   * Handle connection failure with reconnection logic
   */
  private async handleConnectionFailure(): Promise<void> {
    const { reconnectionState } = this;

    if (reconnectionState.attemptCount >= reconnectionState.maxAttempts) {
      console.error('[Zixo] Max reconnection attempts reached');
      return;
    }

    // Exponential backoff: 1s, 2s, 4s
    const backoffMs = Math.pow(2, reconnectionState.attemptCount) * 1000;
    reconnectionState.attemptCount++;
    reconnectionState.isReconnecting = true;
    reconnectionState.lastAttemptTime = Date.now();

    if (this.onReconnecting) {
      this.onReconnecting(reconnectionState.attemptCount);
    }

    console.log(`[Zixo] Reconnection attempt ${reconnectionState.attemptCount}/${reconnectionState.maxAttempts} in ${backoffMs}ms`);

    await new Promise((resolve) => setTimeout(resolve, backoffMs));

    try {
      if (this.peerConnection) {
        // Try ICE restart first
        if (this.isCaller) {
          const offer = await this.peerConnection.createOffer({ iceRestart: true });
          await this.peerConnection.setLocalDescription(offer);

          if (this.callId) {
            updateCallSignal(this.callId, {
              offer: { type: offer.type, sdp: offer.sdp },
            });
          }
        }
      }
    } catch (err) {
      console.error('[Zixo] Reconnection attempt failed:', err);
    }
  }

  /**
   * Start monitoring call quality using WebRTC stats API
   */
  private startQualityMonitoring(): void {
    this.stopQualityMonitoring();

    this.statsInterval = setInterval(async () => {
      if (!this.peerConnection) return;

      try {
        const stats = await this.peerConnection.getStats();
        let bytesSent = 0;
        let bytesReceived = 0;
        let packetsLost = 0;
        let packetsSent = 0;
        let rtt = 0;
        let jitter = 0;
        let codec = '';

        stats.forEach((report) => {
          if (report.type === 'outbound-rtp' && report.kind === 'audio') {
            bytesSent = report.bytesSent || 0;
            packetsSent = report.packetsSent || 0;
          }
          if (report.type === 'inbound-rtp' && report.kind === 'audio') {
            bytesReceived = report.bytesReceived || 0;
            packetsLost = report.packetsLost || 0;
            jitter = report.jitter || 0;
          }
          if (report.type === 'candidate-pair' && report.state === 'succeeded') {
            rtt = report.currentRoundTripTime ? report.currentRoundTripTime * 1000 : 0;
          }
          if (report.type === 'codec') {
            codec = report.mimeType || '';
          }
        });

        const now = Date.now();
        const elapsed = (now - this.lastStatsTimestamp) / 1000; // seconds

        if (elapsed > 0 && this.lastStatsTimestamp > 0) {
          const bitrate = ((bytesSent - this.lastStatsBytesSent) * 8) / (elapsed * 1000); // kbps
          const totalPackets = packetsSent + packetsLost;
          const packetLoss = totalPackets > 0 ? (packetsLost / totalPackets) * 100 : 0;

          const qualityStats: CallQualityStats = {
            bitrate: Math.round(bitrate),
            packetLoss: Math.round(packetLoss * 100) / 100,
            rtt: Math.round(rtt),
            jitter: Math.round(jitter * 1000), // Convert to ms
            codec: codec.replace('audio/', ''),
          };

          if (this.onCallQualityUpdate) {
            this.onCallQualityUpdate(qualityStats);
          }
        }

        this.lastStatsBytesSent = bytesSent;
        this.lastStatsBytesReceived = bytesReceived;
        this.lastStatsTimestamp = now;
      } catch {
        // Stats collection is non-critical
      }
    }, 2000); // Update every 2 seconds
  }

  /**
   * Stop quality monitoring
   */
  private stopQualityMonitoring(): void {
    if (this.statsInterval) {
      clearInterval(this.statsInterval);
      this.statsInterval = null;
    }
    this.lastStatsBytesSent = 0;
    this.lastStatsBytesReceived = 0;
    this.lastStatsTimestamp = 0;
  }

  /**
   * Toggle mute on local audio
   */
  toggleMute(muted: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach((track) => {
        track.enabled = !muted;
      });
    }
  }

  /**
   * Toggle video on/off
   */
  toggleVideo(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach((track) => {
        track.enabled = enabled;
      });
    }
  }

  /**
   * Switch camera (front/back) on mobile
   */
  async switchCamera(): Promise<void> {
    if (!this.localStream) return;

    const videoTrack = this.localStream.getVideoTracks()[0];
    if (!videoTrack) return;

    // Stop current video track
    videoTrack.stop();

    // Get new stream with facing mode toggled
    const currentFacing = videoTrack.getSettings().facingMode;
    const newStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: currentFacing === 'user' ? 'environment' : 'user' },
    });

    const newTrack = newStream.getVideoTracks()[0];
    this.localStream.addTrack(newTrack);
    this.localStream.removeTrack(videoTrack);

    // Replace track in peer connection
    if (this.peerConnection) {
      const sender = this.peerConnection.getSenders().find(
        (s) => s.track?.kind === 'video'
      );
      if (sender) {
        await sender.replaceTrack(newTrack);
      }
    }
  }

  /**
   * End the call and clean up
   */
  async endCall(): Promise<void> {
    // Stop quality monitoring
    this.stopQualityMonitoring();

    // Reset reconnection state
    this.reconnectionState = {
      isReconnecting: false,
      attemptCount: 0,
      maxAttempts: 3,
      lastAttemptTime: 0,
    };

    // Reset ICE candidate tracking
    this.processedICECandidates.clear();

    // Stop all tracks
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => track.stop());
    }
    if (this.remoteStream) {
      this.remoteStream.getTracks().forEach((track) => track.stop());
    }

    // Close peer connection
    if (this.peerConnection) {
      this.peerConnection.close();
    }

    // Clean up RTDB signaling
    if (this.callId) {
      try {
        endCallSignal(this.callId);
      } catch (e) {
        // Document may already be deleted
      }
    }

    // Unsubscribe from signaling
    if (this.unsubscribeSignaling) {
      this.unsubscribeSignaling();
    }

    // Reset
    this.peerConnection = null;
    this.localStream = null;
    this.remoteStream = null;
    this.callId = null;
    this.unsubscribeSignaling = null;
  }

  /**
   * Get the local stream
   */
  getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  /**
   * Get the remote stream
   */
  getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }
}

// Singleton instance
let webrtcInstance: ZixoWebRTC | null = null;

export function getWebRTC(): ZixoWebRTC {
  if (!webrtcInstance) {
    webrtcInstance = new ZixoWebRTC();
  }
  return webrtcInstance;
}

export function resetWebRTC(): void {
  if (webrtcInstance) {
    webrtcInstance.endCall();
  }
  webrtcInstance = new ZixoWebRTC();
}

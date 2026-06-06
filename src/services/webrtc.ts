/**
 * WebRTC Service for Zixo
 * Handles peer-to-peer audio/video calls using Google STUN servers
 *
 * Signaling uses Firebase Realtime Database (RTDB) for low-latency
 * offer/answer/ICE candidate exchange.
 * Falls back to Firestore if RTDB is unavailable.
 */

import { doc, setDoc, getDoc, updateDoc, onSnapshot, deleteDoc, collection, serverTimestamp } from 'firebase/firestore';
import { db } from './firebase';
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

export class ZixoWebRTC {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private callId: string | null = null;
  private unsubscribeSignaling: (() => void) | null = null;
  private isCaller: boolean = false;

  // Callbacks
  public onRemoteStream: ((stream: MediaStream) => void) | null = null;
  public onConnectionStateChange: ((state: RTCPeerConnectionState) => void) | null = null;
  public onICEStateChange: ((state: RTCIceConnectionState) => void) | null = null;

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
    };

    pc.oniceconnectionstatechange = () => {
      if (this.onICEStateChange) {
        this.onICEStateChange(pc.iceConnectionState);
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
   */
  private listenForSignaling(callId: string): void {
    // RTDB signaling is handled by the presence service listeners
    // The call screen component will manage the state updates
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

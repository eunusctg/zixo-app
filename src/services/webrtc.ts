/**
 * WebRTC Service for Zixo
 * Handles peer-to-peer audio/video calls using Google STUN servers
 * 
 * Signaling is done through Firestore (offer/answer/ICE candidates stored in a call document)
 * This avoids needing a separate WebSocket server for signaling.
 */

import { doc, setDoc, getDoc, updateDoc, onSnapshot, deleteDoc } from 'firebase/firestore';
import { db } from './firebase';

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
        this.sendICECandidate(this.callId, event.candidate, this.isCaller);
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

  // Callbacks
  public onRemoteStream: ((stream: MediaStream) => void) | null = null;
  public onConnectionStateChange: ((state: RTCPeerConnectionState) => void) | null = null;
  public onICEStateChange: ((state: RTCIceConnectionState) => void) | null = null;
  private isCaller: boolean = false;

  /**
   * Start a call (caller side)
   */
  async startCall(
    callerId: string,
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

    // Create call document in Firestore
    const callRef = doc(collection(db, 'calls_signaling'));
    this.callId = callRef.id;

    await setDoc(callRef, {
      callerId,
      receiverId,
      type,
      status: 'ringing',
      createdAt: serverTimestamp(),
      offer: null,
      answer: null,
      callerCandidates: [],
      receiverCandidates: [],
    });

    // Create offer
    const offer = await this.peerConnection.createOffer();
    await this.peerConnection.setLocalDescription(offer);

    // Store offer in Firestore
    await updateDoc(callRef, {
      offer: { type: offer.type, sdp: offer.sdp },
    });

    // Listen for answer and remote ICE candidates
    this.listenForSignaling(this.callId);

    return this.callId;
  }

  /**
   * Answer a call (receiver side)
   */
  async answerCall(callId: string): Promise<void> {
    this.isCaller = false;
    this.callId = callId;

    // Get call signaling data
    const callRef = doc(db, 'calls_signaling', callId);
    const callSnap = await getDoc(callRef);

    if (!callSnap.exists()) throw new Error('Call not found');

    const callData = callSnap.data();
    const type = callData.type as 'audio' | 'video';

    // Get local media stream
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: type === 'video' ? { width: 720, height: 1280 } : false,
    });

    this.peerConnection = this.createPeerConnection();

    // Set remote description (offer)
    const offer = callData.offer;
    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));

    // Create answer
    const answer = await this.peerConnection.createAnswer();
    await this.peerConnection.setLocalDescription(answer);

    // Store answer in Firestore
    await updateDoc(callRef, {
      answer: { type: answer.type, sdp: answer.sdp },
      status: 'connected',
    });

    // Listen for remote ICE candidates
    this.listenForSignaling(callId);
  }

  /**
   * Listen for signaling data (answer + ICE candidates)
   */
  private listenForSignaling(callId: string): void {
    const callRef = doc(db, 'calls_signaling', callId);

    this.unsubscribeSignaling = onSnapshot(callRef, async (snap) => {
      if (!snap.exists() || !this.peerConnection) return;

      const data = snap.data();

      // If we're the caller, wait for answer
      if (this.isCaller && data.answer && !this.peerConnection.currentRemoteDescription) {
        await this.peerConnection.setRemoteDescription(
          new RTCSessionDescription(data.answer)
        );
      }

      // Process ICE candidates
      const candidatesKey = this.isCaller ? 'receiverCandidates' : 'callerCandidates';
      const candidates = data[candidatesKey] || [];

      for (const candidate of candidates) {
        if (candidate && !candidate.processed) {
          try {
            await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
            candidate.processed = true;
          } catch (e) {
            console.warn('Error adding ICE candidate:', e);
          }
        }
      }
    });
  }

  /**
   * Send ICE candidate to Firestore
   */
  private async sendICECandidate(
    callId: string,
    candidate: RTCIceCandidate,
    isCaller: boolean
  ): Promise<void> {
    const callRef = doc(db, 'calls_signaling', callId);
    const field = isCaller ? 'callerCandidates' : 'receiverCandidates';

    const callSnap = await getDoc(callRef);
    if (callSnap.exists()) {
      const data = callSnap.data();
      const existing: any[] = data[field] || [];
      existing.push(candidate.toJSON());

      await updateDoc(callRef, { [field]: existing });
    }
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

    // Clean up signaling document
    if (this.callId) {
      try {
        await deleteDoc(doc(db, 'calls_signaling', this.callId));
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

// Helper imports
import { collection, serverTimestamp } from 'firebase/firestore';

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

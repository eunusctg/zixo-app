/**
 * Group WebRTC Service for Zixo
 * Handles group audio/video calls using a mesh P2P topology.
 * Each participant establishes a direct RTCPeerConnection to every other participant.
 * Practical group size: 4-6 participants.
 *
 * Signaling uses Firebase RTDB under `groupCalls/{callId}/` path.
 */

import { ref, onValue, off, onChildAdded, set, update, remove, get, push, serverTimestamp as rtdbServerTimestamp } from 'firebase/database';
import { rtdb } from './firebase';

// ==================== ICE SERVERS ====================

const ICE_SERVERS: RTCConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
    { urls: 'stun:stun3.l.google.com:19302' },
    { urls: 'stun:stun4.l.google.com:19302' },
  ],
};

// ==================== TYPES ====================

export type GroupCallType = 'group-audio' | 'group-video';

export interface GroupCallParticipant {
  uid: string;
  name: string;
  joinedAt: number;
}

export interface GroupCallSignal {
  callerId: string;
  callerName: string;
  type: GroupCallType;
  status: 'ringing' | 'active' | 'ended';
  participants: Record<string, GroupCallParticipant>;
  createdAt: number;
}

interface PeerEntry {
  uid: string;
  pc: RTCPeerConnection;
  remoteStream: MediaStream | null;
  unsubFns: Array<() => void>;
}

// ==================== GROUP WEBRTC CLASS ====================

export class ZixoGroupWebRTC {
  private callId: string | null = null;
  private localUid: string | null = null;
  private localName: string = '';
  private localStream: MediaStream | null = null;
  private peers: Map<string, PeerEntry> = new Map();
  private unsubCall: Array<() => void> = [];
  private processedOffers: Set<string> = new Set();
  private processedAnswers: Set<string> = new Set();
  private processedCandidates: Set<string> = new Set();

  // Callbacks
  public onRemoteStream: ((uid: string, stream: MediaStream) => void) | null = null;
  public onRemoteStreamRemoved: ((uid: string) => void) | null = null;
  public onParticipantJoined: ((uid: string, name: string) => void) | null = null;
  public onParticipantLeft: ((uid: string) => void) | null = null;
  public onConnectionStateChange: ((uid: string, state: RTCPeerConnectionState) => void) | null = null;

  // ==================== START GROUP CALL (CALLER) ====================

  async startGroupCall(
    callerId: string,
    callerName: string,
    participantUids: string[],
    type: GroupCallType
  ): Promise<string> {
    this.localUid = callerId;
    this.localName = callerName;

    // Get local media
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: type === 'group-video' ? { width: 720, height: 1280 } : false,
    });

    // Create call ID
    this.callId = `gcall_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

    // Build participants map
    const participants: Record<string, GroupCallParticipant> = {
      [callerId]: { uid: callerId, name: callerName, joinedAt: Date.now() },
    };
    participantUids.forEach((uid) => {
      participants[uid] = { uid, name: '', joinedAt: 0 }; // Will be filled when they join
    });

    // Create the group call signal in RTDB
    const callRef = ref(rtdb, `groupCalls/${this.callId}`);
    await set(callRef, {
      callerId,
      callerName,
      type,
      status: 'ringing',
      participants,
      createdAt: rtdbServerTimestamp(),
    });

    // Listen for participants joining and signaling
    this.listenForSignaling();

    return this.callId;
  }

  // ==================== JOIN GROUP CALL (PARTICIPANT) ====================

  async joinGroupCall(callId: string, uid: string, name: string): Promise<void> {
    this.localUid = uid;
    this.localName = name;
    this.callId = callId;

    // Get the call data to know the type
    const callSnap = await get(ref(rtdb, `groupCalls/${callId}`));
    if (!callSnap.exists()) throw new Error('Group call not found');

    const callData = callSnap.val() as GroupCallSignal;

    // Get local media
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: callData.type === 'group-video' ? { width: 720, height: 1280 } : false,
    });

    // Add self to participants
    const participantRef = ref(rtdb, `groupCalls/${callId}/participants/${uid}`);
    await set(participantRef, { uid, name, joinedAt: Date.now() });

    // Update status to active
    const statusRef = ref(rtdb, `groupCalls/${callId}/status`);
    await set(statusRef, 'active');

    // Get existing participants and create peer connections
    const participantsSnap = await get(ref(rtdb, `groupCalls/${callId}/participants`));
    if (participantsSnap.exists()) {
      const participants = participantsSnap.val() as Record<string, GroupCallParticipant>;
      const otherUids = Object.keys(participants).filter((pUid) => pUid !== uid);

      for (const otherUid of otherUids) {
        if (participants[otherUid].joinedAt > 0) {
          // This participant has already joined, create offer to them
          await this.createPeerConnection(otherUid, true);
        }
      }
    }

    // Listen for signaling
    this.listenForSignaling();
  }

  // ==================== LEAVE GROUP CALL ====================

  async leaveGroupCall(): Promise<void> {
    // Clean up all peer connections
    for (const [uid, peer] of this.peers.entries()) {
      peer.pc.close();
      peer.unsubFns.forEach((fn) => fn());
      if (peer.remoteStream) {
        peer.remoteStream.getTracks().forEach((t) => t.stop());
      }
      if (this.onRemoteStreamRemoved) {
        this.onRemoteStreamRemoved(uid);
      }
    }
    this.peers.clear();

    // Stop local stream
    if (this.localStream) {
      this.localStream.getTracks().forEach((t) => t.stop());
      this.localStream = null;
    }

    // Remove self from participants
    if (this.callId && this.localUid) {
      try {
        const participantRef = ref(rtdb, `groupCalls/${this.callId}/participants/${this.localUid}`);
        await remove(participantRef);

        // Clean up our signaling data
        const offersRef = ref(rtdb, `groupCalls/${this.callId}/offers/${this.localUid}`);
        await remove(offersRef);
        const answersRef = ref(rtdb, `groupCalls/${this.callId}/answers/${this.localUid}`);
        await remove(answersRef);
        const candidatesRef = ref(rtdb, `groupCalls/${this.callId}/candidates/${this.localUid}`);
        await remove(candidatesRef);

        // Check if anyone is left
        const participantsSnap = await get(ref(rtdb, `groupCalls/${this.callId}/participants`));
        if (!participantsSnap.exists() || Object.keys(participantsSnap.val()).length === 0) {
          // No one left, end the call
          await remove(ref(rtdb, `groupCalls/${this.callId}`));
        }
      } catch (e) {
        console.warn('[Zixo Group] Error leaving group call:', e);
      }
    }

    // Unsubscribe from signaling
    this.unsubCall.forEach((fn) => fn());
    this.unsubCall = [];

    // Reset state
    this.callId = null;
    this.localUid = null;
    this.localName = '';
    this.processedOffers.clear();
    this.processedAnswers.clear();
    this.processedCandidates.clear();
  }

  // ==================== CREATE PEER CONNECTION ====================

  private async createPeerConnection(remoteUid: string, isInitiator: boolean): Promise<void> {
    // Don't create duplicate connections
    if (this.peers.has(remoteUid)) return;

    const pc = new RTCPeerConnection(ICE_SERVERS);
    const remoteStream = new MediaStream();

    const peerEntry: PeerEntry = {
      uid: remoteUid,
      pc,
      remoteStream: null,
      unsubFns: [],
    };
    this.peers.set(remoteUid, peerEntry);

    // Add local tracks
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        pc.addTrack(track, this.localStream!);
      });
    }

    // Handle remote tracks
    pc.ontrack = (event) => {
      event.streams[0].getTracks().forEach((track) => {
        remoteStream.addTrack(track);
      });
      peerEntry.remoteStream = remoteStream;
      if (this.onRemoteStream) {
        this.onRemoteStream(remoteUid, remoteStream);
      }
    };

    // Handle ICE candidates
    pc.onicecandidate = (event) => {
      if (event.candidate && this.callId && this.localUid) {
        const candidateRef = ref(
          rtdb,
          `groupCalls/${this.callId}/candidates/${this.localUid}/${remoteUid}/${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
        );
        set(candidateRef, event.candidate.toJSON());
      }
    };

    // Handle connection state changes
    pc.onconnectionstatechange = () => {
      if (this.onConnectionStateChange) {
        this.onConnectionStateChange(remoteUid, pc.connectionState);
      }
      if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
        this.removePeer(remoteUid);
      }
    };

    // If initiator, create offer
    if (isInitiator) {
      try {
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);

        // Write offer to RTDB
        if (this.callId && this.localUid) {
          const offerRef = ref(rtdb, `groupCalls/${this.callId}/offers/${remoteUid}/${this.localUid}`);
          await set(offerRef, { sdp: offer.sdp, type: offer.type });

          // Listen for answer from this specific peer
          this.listenForAnswer(remoteUid);
        }
      } catch (err) {
        console.error('[Zixo Group] Error creating offer:', err);
      }
    }

    // Listen for ICE candidates from remote peer
    this.listenForCandidates(remoteUid);
  }

  // ==================== LISTEN FOR ANSWER ====================

  private listenForAnswer(remoteUid: string): void {
    if (!this.callId || !this.localUid) return;

    const answerRef = ref(rtdb, `groupCalls/${this.callId}/answers/${this.localUid}/${remoteUid}`);
    const unsub = onValue(answerRef, async (snap) => {
      if (!snap.exists()) return;
      const answer = snap.val();
      const answerKey = `${remoteUid}_${snap.key}`;
      if (this.processedAnswers.has(answerKey)) return;
      this.processedAnswers.add(answerKey);

      const peer = this.peers.get(remoteUid);
      if (peer && peer.pc.signalingState === 'have-local-offer') {
        try {
          await peer.pc.setRemoteDescription(new RTCSessionDescription(answer));
        } catch (err) {
          console.warn('[Zixo Group] Error setting remote answer:', err);
        }
      }
    });

    const peer = this.peers.get(remoteUid);
    if (peer) {
      peer.unsubFns.push(unsub);
    }
  }

  // ==================== LISTEN FOR ICE CANDIDATES ====================

  private listenForCandidates(remoteUid: string): void {
    if (!this.callId || !this.localUid) return;

    const candidatesRef = ref(rtdb, `groupCalls/${this.callId}/candidates/${remoteUid}/${this.localUid}`);
    const unsub = onChildAdded(candidatesRef, async (snap) => {
      if (!snap.exists()) return;
      const candidateData = snap.val();
      const candidateKey = snap.key;
      if (candidateKey && this.processedCandidates.has(candidateKey)) return;
      if (candidateKey) this.processedCandidates.add(candidateKey);

      const peer = this.peers.get(remoteUid);
      if (peer) {
        try {
          await peer.pc.addIceCandidate(new RTCIceCandidate(candidateData));
        } catch (err) {
          console.warn('[Zixo Group] Error adding ICE candidate:', err);
        }
      }
    });

    const peer = this.peers.get(remoteUid);
    if (peer) {
      peer.unsubFns.push(unsub);
    }
  }

  // ==================== LISTEN FOR SIGNALING ====================

  private listenForSignaling(): void {
    if (!this.callId || !this.localUid) return;

    // Listen for new participants joining
    const participantsRef = ref(rtdb, `groupCalls/${this.callId}/participants`);
    const unsubParticipants = onChildAdded(participantsRef, async (snap) => {
      if (!snap.exists()) return;
      const participant = snap.val() as GroupCallParticipant;
      if (participant.uid === this.localUid) return;
      if (participant.joinedAt === 0) return; // Not yet joined

      // New participant joined
      if (this.onParticipantJoined) {
        this.onParticipantJoined(participant.uid, participant.name);
      }

      // If this participant joined after us and we haven't connected yet, create offer
      if (!this.peers.has(participant.uid)) {
        await this.createPeerConnection(participant.uid, true);
      }
    });
    this.unsubCall.push(unsubParticipants);

    // Listen for participants leaving
    const unsubParticipantRemoved = onValue(participantsRef, async (snap) => {
      if (!snap.exists()) return;
      const currentParticipants = Object.keys(snap.val() as Record<string, GroupCallParticipant>);

      // Check for removed participants
      for (const [uid] of this.peers.entries()) {
        if (!currentParticipants.includes(uid)) {
          this.removePeer(uid);
          if (this.onParticipantLeft) {
            this.onParticipantLeft(uid);
          }
        }
      }
    });
    this.unsubCall.push(unsubParticipantRemoved);

    // Listen for offers directed at us
    const offersRef = ref(rtdb, `groupCalls/${this.callId}/offers/${this.localUid}`);
    const unsubOffers = onChildAdded(offersRef, async (snap) => {
      if (!snap.exists()) return;
      const offer = snap.val();
      const fromUid = snap.key;
      if (!fromUid) return;

      const offerKey = `${fromUid}_${snap.key}`;
      if (this.processedOffers.has(offerKey)) return;
      this.processedOffers.add(offerKey);

      // Create peer connection if not exists (we're the answerer)
      if (!this.peers.has(fromUid)) {
        const pc = new RTCPeerConnection(ICE_SERVERS);
        const remoteStream = new MediaStream();

        const peerEntry: PeerEntry = {
          uid: fromUid,
          pc,
          remoteStream: null,
          unsubFns: [],
        };
        this.peers.set(fromUid, peerEntry);

        // Add local tracks
        if (this.localStream) {
          this.localStream.getTracks().forEach((track) => {
            pc.addTrack(track, this.localStream!);
          });
        }

        // Handle remote tracks
        pc.ontrack = (event) => {
          event.streams[0].getTracks().forEach((track) => {
            remoteStream.addTrack(track);
          });
          peerEntry.remoteStream = remoteStream;
          if (this.onRemoteStream) {
            this.onRemoteStream(fromUid, remoteStream);
          }
        };

        // Handle ICE candidates
        pc.onicecandidate = (event) => {
          if (event.candidate && this.callId && this.localUid) {
            const candidateRef = ref(
              rtdb,
              `groupCalls/${this.callId}/candidates/${this.localUid}/${fromUid}/${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
            );
            set(candidateRef, event.candidate.toJSON());
          }
        };

        pc.onconnectionstatechange = () => {
          if (this.onConnectionStateChange) {
            this.onConnectionStateChange(fromUid, pc.connectionState);
          }
          if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
            this.removePeer(fromUid);
          }
        };

        // Listen for candidates
        this.listenForCandidates(fromUid);
      }

      // Set remote description and create answer
      const peer = this.peers.get(fromUid);
      if (peer) {
        try {
          await peer.pc.setRemoteDescription(new RTCSessionDescription(offer));
          const answer = await peer.pc.createAnswer();
          await peer.pc.setLocalDescription(answer);

          // Write answer to RTDB
          const answerRef = ref(rtdb, `groupCalls/${this.callId}/answers/${fromUid}/${this.localUid}`);
          await set(answerRef, { sdp: answer.sdp, type: answer.type });
        } catch (err) {
          console.error('[Zixo Group] Error answering offer:', err);
        }
      }
    });
    this.unsubCall.push(unsubOffers);

    // Listen for call status changes (ended)
    const statusRef = ref(rtdb, `groupCalls/${this.callId}/status`);
    const unsubStatus = onValue(statusRef, (snap) => {
      if (snap.exists() && snap.val() === 'ended') {
        if (this.onParticipantLeft) {
          // Signal to all that the call ended
        }
      }
    });
    this.unsubCall.push(unsubStatus);
  }

  // ==================== REMOVE PEER ====================

  private removePeer(uid: string): void {
    const peer = this.peers.get(uid);
    if (peer) {
      peer.pc.close();
      peer.unsubFns.forEach((fn) => fn());
      if (peer.remoteStream) {
        peer.remoteStream.getTracks().forEach((t) => t.stop());
      }
      this.peers.delete(uid);
      if (this.onRemoteStreamRemoved) {
        this.onRemoteStreamRemoved(uid);
      }
    }
  }

  // ==================== TOGGLE MUTE ====================

  toggleMute(muted: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach((track) => {
        track.enabled = !muted;
      });
    }
  }

  // ==================== TOGGLE VIDEO ====================

  toggleVideo(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach((track) => {
        track.enabled = enabled;
      });
    }
  }

  // ==================== SWITCH CAMERA ====================

  async switchCamera(): Promise<void> {
    if (!this.localStream) return;

    const videoTrack = this.localStream.getVideoTracks()[0];
    if (!videoTrack) return;

    const currentFacing = videoTrack.getSettings().facingMode;
    videoTrack.stop();

    const newStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: currentFacing === 'user' ? 'environment' : 'user' },
    });

    const newTrack = newStream.getVideoTracks()[0];
    this.localStream.addTrack(newTrack);
    this.localStream.removeTrack(videoTrack);

    // Replace track in all peer connections
    for (const [, peer] of this.peers.entries()) {
      const sender = peer.pc.getSenders().find((s) => s.track?.kind === 'video');
      if (sender) {
        await sender.replaceTrack(newTrack);
      }
    }
  }

  // ==================== GETTERS ====================

  getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  getCallId(): string | null {
    return this.callId;
  }

  getConnectedPeerCount(): number {
    let count = 0;
    for (const [, peer] of this.peers.entries()) {
      if (peer.pc.connectionState === 'connected') count++;
    }
    return count;
  }

  getRemoteStream(uid: string): MediaStream | null {
    return this.peers.get(uid)?.remoteStream || null;
  }

  getAllRemoteStreams(): Map<string, MediaStream> {
    const streams = new Map<string, MediaStream>();
    for (const [uid, peer] of this.peers.entries()) {
      if (peer.remoteStream) {
        streams.set(uid, peer.remoteStream);
      }
    }
    return streams;
  }
}

// ==================== SINGLETON ====================

let groupWebrtcInstance: ZixoGroupWebRTC | null = null;

export function getGroupWebRTC(): ZixoGroupWebRTC {
  if (!groupWebrtcInstance) {
    groupWebrtcInstance = new ZixoGroupWebRTC();
  }
  return groupWebrtcInstance;
}

export function resetGroupWebRTC(): void {
  if (groupWebrtcInstance) {
    groupWebrtcInstance.leaveGroupCall();
  }
  groupWebrtcInstance = new ZixoGroupWebRTC();
}

---
Task ID: 1
Agent: Main Agent
Task: Fix duplicate calls, add missed call logic, add outgoing ring sound

Work Log:
- Read and analyzed all call-related source files: useFirebaseBridge.ts, CallScreens.tsx, useZixoStore.ts, presence.ts, webrtc.ts, messaging.ts
- Identified root cause of duplicate calls: subscribeToIncomingCalls used onValue on the entire `calls` node, firing on every data change (ICE candidates, offer updates, etc.)
- Rewrote subscribeToIncomingCalls to use onChildAdded (fires only for NEW calls) + per-call status listeners (detects caller cancellation)
- Added missed call recording in 3 places: (1) when caller cancels before answer (useFirebaseBridge 8b), (2) when calls array goes empty while incomingCall exists (useFirebaseBridge 8), (3) when receiver rejects the call (useZixoStore rejectCall)
- Added playOutgoingRingSound() and stopOutgoingRingSound() functions to messaging.ts
- Integrated outgoing ring sound: starts when call is initiated (ringing), stops when connected/ended/failed
- Added guards in startCall to prevent starting a new call while already in an active or incoming call
- Added double-check in proceedWithCall callback (after permission dialog)
- Added stopOutgoingRingSound() in endCall, setCallStatus(connected/ended), and call failure handler
- Build succeeded and deployed to Cloudflare Pages

Stage Summary:
- Fixed duplicate call signals by replacing broad onValue listener with targeted onChildAdded + per-call status listeners
- Implemented missed call recording (direction: 'missed') in call history with Firestore persistence
- Added outgoing ring sound (dual-tone 440Hz+480Hz repeating pattern) that plays while waiting for receiver to answer
- Fixed black screen by preventing multiple simultaneous calls that corrupted state
- Deployed to https://625befee.zixo.pages.dev

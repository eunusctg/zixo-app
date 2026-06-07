---
Task ID: 1
Agent: Main Agent
Task: Fix "Failed to list users" error affecting New Chat + Admin Panel

Work Log:
- Diagnosed root cause: User profiles only in Firestore, RTDB fallback empty
- Added syncUserProfileToRTDB() helper in auth.ts that writes profiles to RTDB /users/{uid}
- Called sync after all profile create/update operations (register, Google login, OTP, profile edit, online status, zixo number)
- Added RTDB profile sync in useFirebaseBridge.ts when auth state loads
- Added syncUsersToRTDB API action in /api/zixo/route.ts
- Added "Sync Users to RTDB" button in Admin Panel settings tab
- Ran migration: synced all 6 existing users to RTDB
- Verified admin user has role=admin in RTDB
- Tested discoverUsers and listUsers API endpoints - both working

Stage Summary:
- RTDB now has all 6 user profiles including admin role
- Both discoverUsers (New Chat) and listUsers (Admin Panel) work via Firestore REST API
- RTDB fallback also works as a backup
---
Task ID: 2
Agent: Main Agent
Task: Fix auto-logout (app logs users out automatically)

Work Log:
- Increased null auth debounce from 10s to 30s in useFirebaseBridge.ts
- Added setPersistence(auth, browserLocalPersistence) in firebase.ts immediately after getAuth()
- This ensures Firebase Auth persists across browser sessions

Stage Summary:
- Auto-logout should no longer happen during token refresh or brief network interruptions
- Auth state persists across page reloads and browser restarts
---
Task ID: 3
Agent: Main Agent
Task: Fix profile picture not showing on home screen

Work Log:
- Added cleanAvatarUrl in Avatar.tsx that filters out empty, whitespace, 'undefined', 'null' strings
- Home screen already uses currentUser.avatar correctly via Avatar component
- useFirebaseBridge already syncs Google photoURL to Firestore avatar field

Stage Summary:
- Avatar component now handles invalid avatar URLs gracefully
- Google photoURL is synced to Firestore on first login
---
Task ID: 4
Agent: Main Agent
Task: Fix Google login loop (Continue → back to login)

Work Log:
- Added immediate store.login() call in handleGoogleSignIn (Onboarding.tsx) after loginWithGoogle returns
- Added same fix for handleVerifyOTP
- Simplified "Continue" button handler to always call store.setScreen('home') without waiting for bridge

Stage Summary:
- Google login now immediately updates Zustand store, preventing race conditions
- Continue button always navigates to home screen
---
Task ID: 5
Agent: Main Agent
Task: Fix call history not showing + profile editing + header logo

Work Log:
- Fixed callHistoryLoaded ref in useFirebaseBridge.ts - only set true on success, allowing retries
- Added RTDB fallback method in getCallHistory (firestore.ts)
- Added _profileUpdatedAt timestamp to prevent bridge from overwriting fresh profile edits
- Bridge's profile subscription now skips updates within 5 seconds of local edit
- Replaced header logo SVG with new Zixo-branded design

Stage Summary:
- Call history loading is more robust with retry mechanism
- Profile edits are preserved for 5 seconds to prevent stale Firestore overwrites
- New Zixo header logo with signal/broadcast arcs
---
Task ID: 1
Agent: Main Agent
Task: Fix incoming call black screen and properly implement incoming/outgoing call logic

Work Log:
- Read and analyzed all call-related source files: CallScreens.tsx, useZixoStore.ts, useFirebaseBridge.ts, webrtc.ts, presence.ts, page.tsx
- Identified root cause of incoming call black screen: incoming-call screen always used AudioCallScreen even for video calls, with remoteStream={null}
- Created new IncomingCallScreen component that handles both audio and video incoming calls with proper UI (pulsing rings, avatar, call type indicator, Answer/Decline buttons)
- Fixed WebRTC answerCall to wait for offer from RTDB if not yet available (race condition fix) - added waitForOffer polling method with 10s timeout
- Fixed audio autoplay policy - added retry on user interaction when autoplay is blocked by mobile browsers
- Fixed video call remote stream autoplay - same retry mechanism
- Fixed critical bug: AudioCallScreen active call controls were hidden for incoming calls (condition `isActive && !isIncoming` excluded answered incoming calls from seeing controls)
- Added proper 'ended' state transition - calls now briefly show "Call ended" before dismissing, with a close button
- Updated useFirebaseBridge to show 'ended' status for 1.5s before transitioning to home when remote party ends call
- Deployed to Cloudflare Pages: https://05ccce7a.zixo.pages.dev

Stage Summary:
- Created IncomingCallScreen component (handles audio + video incoming calls)
- Fixed WebRTC answerCall offer race condition
- Fixed audio/video autoplay on mobile browsers
- Fixed active call controls not showing for answered incoming calls
- Added proper call end UX with brief "Call ended" feedback
- All changes deployed to production
---
Task ID: 1
Agent: Main Agent
Task: Fix incoming call duplication and black screen issues

Work Log:
- Read all call-related source files: CallScreens.tsx, webrtc.ts, useFirebaseBridge.ts, useZixoStore.ts, presence.ts, page.tsx
- Diagnosed root cause: subscribeToIncomingCalls uses onValue on entire `calls` RTDB node, which fires on EVERY data change (ICE candidates, offer/answer updates), causing duplicate incoming call notifications
- Diagnosed root cause: onConnectionStateChange calls endCall() on 'disconnected' state, which can be temporary during ICE renegotiation
- Diagnosed root cause: subscribeToCallStatus triggers false call ends from brief RTDB data flickers
- Fixed useFirebaseBridge.ts: Added deduplication for incoming calls (skip if same callId already in state, skip if already in active call, re-check after async profile fetch)
- Fixed useZixoStore.ts: Changed onConnectionStateChange to only end call on 'failed' state, not 'disconnected'
- Fixed useFirebaseBridge.ts: Added 1.5s debounce to subscribeToCallStatus to prevent false call ends from RTDB flickers, with logic to cancel debounce if data reappears
- Fixed useZixoStore.ts: answerCall error handler now properly clears incomingCall and sends endCallSignal
- Built and deployed to Cloudflare Pages successfully
- Pushed to GitHub

Stage Summary:
- Deployed to https://zixo.pages.dev
- Key fixes: call deduplication, no premature call end on 'disconnected', debounced call status monitoring
- Files modified: useFirebaseBridge.ts, useZixoStore.ts, webrtc.ts

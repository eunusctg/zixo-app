# Work Log - Task 1

## Summary of Changes

### Task 1: Fix Admin Panel Not Showing After Sign-in

**Root Cause:** When users sign in, temp profiles created during fallback paths were missing the `role` field. The `handleAuth` fallback in `page.tsx` also created a competing temp profile without `role`.

**Fixes Applied:**
1. **`useFirebaseBridge.ts`** - Added `role: 'user'` to both temp profile creation paths (no Firestore profile found, and error fallback)
2. **`page.tsx`** - Removed the entire `handleAuth` fallback (lines 115-140) since `useFirebaseBridge` properly handles auth state changes. The fallback created race conditions with the bridge. Now `onAuth` is a no-op `() => {}`.

### Task 2: Remove All Dummy/Seed/Demo Users and Demo Data

1. **`useZixoStore.ts`** - Removed:
   - `DEMO_USERS` array (6 demo user profiles)
   - `generateDemoChats()` function
   - `generateDemoMessages()` function
   - `generateDemoCalls()` function
2. **`page.tsx`** - Removed:
   - Import of `generateDemoChats, generateDemoMessages, generateDemoCalls`
   - `demoLoaded` ref
   - Demo data loading `useEffect`
3. **`useFirebaseBridge.ts`** - Removed:
   - Import of `generateDemoChats, generateDemoMessages, generateDemoCalls`
   - `isDevMode` variable
   - `loadDemoDataHelper` function
   - `if (isDevMode)` blocks in chat subscription and call history loading

### Task 3: Wire Up Real WebRTC P2P Audio/Video Calling

1. **`useZixoStore.ts`** - Major changes:
   - Added `localStream`, `remoteStream`, `callId` to `activeCall` type
   - Added `incomingCall` state with `callId`, `callerProfile`, `callType`, `callData`
   - Added `answerCall`, `rejectCall`, `setCallRemoteStream`, `setCallLocalStream`, `setIncomingCall` actions
   - `startCall` now uses `getWebRTC().startCall()` to create real peer connections
   - `answerCall` uses `getWebRTC().answerCall()` to answer incoming calls
   - `rejectCall` sends `endCallSignal()` to RTDB and clears state
   - `endCall` now calls `getWebRTC().endCall()` to clean up peer connections
   - `toggleMute` and `toggleVideo` now also control the actual WebRTC tracks

2. **`useFirebaseBridge.ts`** - Incoming call handling:
   - Section 8 now looks up caller profile from cache/Firestore
   - Sets `incomingCall` state in the store
   - Navigates to `'incoming-call'` screen

3. **`page.tsx`** - Screen rendering:
   - Added `'incoming-call'` screen case with `AudioCallScreen` (isIncoming=true, answer/decline buttons)
   - Pass `remoteStream` to `AudioCallScreen` and `localStream`/`remoteStream` to `VideoCallScreen`
   - Connected `answerCall` and `rejectCall` from the store

4. **`CallScreens.tsx`** - Real media streams:
   - `AudioCallScreen` now has `<audio>` element for remote stream playback
   - `VideoCallScreen` now has `<video>` elements for both local (PiP) and remote streams
   - Both components accept and render `remoteStream`/`localStream` props

### Task 4: Ensure Texting Works Properly

1. **`CallHistory.tsx` (ContactsScreen)** - Added:
   - `onSearchUser` prop for searching by username
   - Search button in input field for Firestore username search
   - "Your Chats" section header
   - Chat button (message icon) alongside call buttons
   - Click-to-chat on contact rows
   - Empty state with "Search by @username" guidance

2. **`page.tsx`** - Contacts screen now:
   - Imports `createOrGetChat` from firestore and `searchUserByUsername` from auth
   - `onStartChat` creates/gets chat in Firestore before navigating
   - `onSearchUser` searches by username and creates chat with found user

### Build Verification
- `npx next build` compiles successfully
- Dev server returns 200 OK
- No TypeScript errors in source files

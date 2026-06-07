# Call Issues Fix - Work Record

## Task: Fix call-related issues in the Zixo app

### Issue 1: Call disconnect not real-time
**Root Cause:** When the caller hangs up before the receiver answers, the `subscribeToIncomingCalls` callback only handled `calls.length > 0`, never clearing the `incomingCall` state when no ringing calls remain. Additionally, there was no `subscribeToCallStatus` subscription for the incoming call screen.

**Fixes Applied:**
1. **`src/hooks/useFirebaseBridge.ts`**: Added `else` branch in `subscribeToIncomingCalls` callback to clear `incomingCall` state and navigate home when no ringing calls remain.
2. **`src/hooks/useFirebaseBridge.ts`**: Added new `incomingCallId` reactive selector and a `subscribeToCallStatus` effect for the incoming call, so the receiver's screen detects when the caller sets the RTDB status to 'ended'.
3. **`src/stores/useZixoStore.ts`**: Added guard in `endCall` to prevent duplicate processing (skip if no activeCall and no incomingCall).

### Issue 2: Voice Recording in chat not working
**Root Cause:** The voice message bubble had no working audio player (play button had no onClick handler). Also, the `ChatInputBar.handleFileSelect` didn't pass the download URL from the upload result to the parent component, so image/file messages lacked their media URLs.

**Fixes Applied:**
1. **`src/components/zixo/ChatScreen.tsx`**: Added full audio player functionality to `MessageBubble` - play/pause toggle, progress bar visualization, duration display, and proper cleanup on unmount.
2. **`src/components/zixo/ChatScreen.tsx`**: Updated `ChatInputBarProps.onFileUpload` signature to include optional `downloadUrl` parameter.
3. **`src/components/zixo/ChatScreen.tsx`**: Updated `handleFileSelect` to pass `result.downloadUrl` to `onFileUpload`.
4. **`src/app/page.tsx`**: Updated `handleFileUpload` callback to accept `downloadUrl` and pass it as `mediaUrl` in the message extras.

### Issue 3: Group Audio & Video Calls
**Root Cause:** The `leaveGroupCall` action didn't send any RTDB signal when leaving, so other participants wouldn't be notified. When the creator left, the call should end for everyone, but it wasn't signaled.

**Fixes Applied:**
1. **`src/stores/useZixoStore.ts`**: Added `endGroupCallSignal` import from presence service.
2. **`src/stores/useZixoStore.ts`**: In `leaveGroupCall`, added RTDB signaling before WebRTC cleanup:
   - Creator leaving → calls `endGroupCallSignal()` to end call for all participants
   - Participant leaving → calls `leaveGroupCallSignal()` to remove themselves
3. Added guard at the start of `leaveGroupCall` to handle the case where `groupCall` is already null.

### Build Verification
- ESLint check: All modified files pass without errors
- `npx next build`: Build succeeds successfully

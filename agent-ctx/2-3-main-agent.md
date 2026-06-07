# Task 2-3: Fix Group Calls & Strengthen Notifications

## Agent: Main Agent
## Date: 2026-03-04

## Summary
Fixed 4 group call bugs and 4 notification issues in the Zixo app.

## Changes Made

### Group Call Fixes (`src/services/webrtc-group.ts`)
1. **Race condition fix**: Added `connectingPeers` Set to prevent duplicate peer connections when `onChildAdded` fires rapidly
2. **Existing participants on join**: Reordered `joinGroupCall` to set up signaling listeners BEFORE processing existing participants
3. **Call end signal**: Changed status listener from no-op to actually calling `this.leaveGroupCall()` when status becomes 'ended'

### Group Call UI Fixes (`src/components/zixo/GroupCallScreens.tsx`)
1. **Audio element for audio calls**: Added hidden `<audio>` element to `ParticipantTile` for non-local streams
2. **Audio elements in GroupAudioCallScreen**: Added hidden `<audio>` for each remote participant with a stream

### Notification Fixes (`src/services/messaging.ts`)
1. **Foreground message cleanup**: Added `cleanedUp` flag to prevent listener registration after cleanup
2. **XSS fix**: Replaced `innerHTML` with `createElement` + `textContent` in `showInAppNotification`
3. **sendPushNotification**: New function that sends forceful push notifications via `/api/zixo` endpoint

### Notification Integration (`src/hooks/useFirebaseBridge.ts`)
1. **Incoming 1:1 calls**: Sends push notification when incoming call detected
2. **Incoming group calls**: Sends push notification for group call invites
3. **New messages in non-active chats**: Sends push notification for unread messages from other users in non-active chats (respects muted chats)

## Deployment
- Built and deployed to https://zixo.pages.dev
- API verified: returns 200 at /api/zixo

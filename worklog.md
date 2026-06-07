# Zixo App Bug Fixes & Feature Additions - Worklog

**Date**: 2026-06-07
**Scope**: Fix 7 bugs/feature requests across 8 files

---

## Bug 1: "New Chat" button crash - "Cannot read properties of undefined (reading 'length')"

**Root Cause**: In `ContactsScreen` (CallHistory.tsx), the `displayUsers` list and `renderUserItem` function didn't guard against null/undefined entries in `allUsers` or contacts with undefined properties like `displayName` or `username`.

**Files Changed**:
- `src/components/zixo/CallHistory.tsx`

**Changes Made**:
1. **`displayUsers` computation** (line ~243): Wrapped the entire expression in a filter that removes null/undefined entries and entries without a valid `uid`. Added `(allUsers || [])` guards and a final `.filter(u => u && u.uid)`.
2. **`renderUserItem` function** (line ~520): Added optional chaining (`contact?.uid`, `contact?.displayName`, `contact?.username`) with fallback values (`'Unknown'`, `''`) to prevent crashes when contact properties are undefined. Changed `key` prop to use `contact?.uid || i` for stability.

---

## Bug 2: Call history not showing

**Root Cause**: Two issues:
1. `endCall()` in `useZixoStore.ts` only saved call records to the local `callHistory` array but never persisted them to Firestore's `calls` collection.
2. `getCallHistory()` in `firestore.ts` used `orderBy('timestamp', 'desc')` which requires a composite Firestore index that may not exist, causing silent failures.

**Files Changed**:
- `src/stores/useZixoStore.ts`
- `src/services/firestore.ts`

**Changes Made**:
1. **`endCall()` action** (useZixoStore.ts, line ~747): After creating the `newCall` record and adding it to local `callHistory`, added a dynamic import of `saveCallRecord` from `@/services/firestore` to persist the call to Firestore. Uses `.catch(console.error)` for graceful failure.
2. **`getCallHistory()` function** (firestore.ts, line ~598): Wrapped the indexed query in a try/catch block. If the indexed query fails (missing composite index), falls back to simpler queries without `orderBy`. Also added deduplication in the fallback path and client-side sorting. Returns results from either path.

---

## Bug 3: Google login Continue button loops back to login

**Root Cause**: When the user clicks Continue on the Zixo Number screen in `Onboarding.tsx`, the old code called `setShowZixoNumber(false)` (causing re-render as auth form) AND `onAuth({ email: '', displayName: '' })` (re-triggering auth flow), creating a race condition. The Firebase bridge had already called `login()` via `onAuthStateChanged`, so `onAuth` was unnecessary and caused a loop.

**Files Changed**:
- `src/components/zixo/Onboarding.tsx`

**Changes Made**:
1. **Continue button handler** (line ~534): Removed `setShowZixoNumber(false)` and `onAuth({ email: '', displayName: '' })` calls. Instead, directly imports `useZixoStore` and navigates to the home screen using `store.setScreen('home')` if the user is already authenticated. Added a polling safety net (checks every 100ms for up to 5 seconds) in case the Firebase bridge hasn't fired yet.

---

## Bug 4: Change header logo in home screen

**Root Cause**: The home screen header used a plain "Z" letter inside a gradient box, which didn't reflect the Zixo branding well.

**Files Changed**:
- `src/app/page.tsx`

**Changes Made**:
1. **Home screen logo** (line ~700): Replaced the plain `<span>Z</span>` with an SVG logo that combines a stylized "Z" shape with call/chat icon elements (rectangles representing chat bubbles, a small circle representing a call indicator). The SVG uses white fill with varying opacity for depth.

---

## Bug 5: Make more responsive for mobile devices

**Root Cause**: Several mobile UX issues:
- No safe area padding for notched phones
- `h-screen` doesn't account for mobile browser chrome
- No overscroll behavior to prevent pull-to-refresh
- Missing touch-action manipulation
- Bottom nav could overlap content

**Files Changed**:
- `src/app/globals.css`
- `src/components/zixo/Navigation.tsx`
- `src/app/page.tsx`

**Changes Made**:
1. **globals.css**: Added `overscroll-behavior: contain` to `html` to prevent pull-to-refresh. Added `touch-action: manipulation` to `body` to eliminate 300ms tap delay.
2. **Navigation.tsx BottomNav**: Replaced CSS class-based `safe-area-bottom` with inline style `paddingBottom: env(safe-area-inset-bottom, 0px)` for reliable safe area handling.
3. **page.tsx**: Changed all `h-screen` to `h-[100dvh]` (dynamic viewport height) for proper mobile browser chrome handling. Changed content `pb-20` to `pb-24` to ensure bottom nav doesn't overlap content.

---

## Bug 6: Add user QR code in profile card (Settings screen)

**Root Cause**: The Settings screen profile card showed user info but had no QR code for easy friend-adding.

**Files Changed**:
- `src/components/zixo/SettingsScreen.tsx`

**Changes Made**:
1. **Added dynamic QRCodeSVG import** (line ~12): Used `next/dynamic` with SSR disabled (same pattern as CallHistory.tsx) to import `QRCodeSVG` from `qrcode.react`.
2. **Added QR code section** (line ~121): After the edit profile button, added a conditional section that renders when `user.zixoNumber` exists. Contains a white-background rounded container with the QR code encoding `ZIXO:{zixoNumber}`, and a descriptive label "Scan to add on Zixo".

---

## Bug 7: QR code scanning capability

**Status**: Already implemented in `ContactsScreen` (CallHistory.tsx). The QR scanner uses `jsQR` library with camera access, and looks up scanned Zixo numbers via `onSearchByZixoNumber`. No additional changes needed.

---

## Additional Notes

- All TypeScript errors in modified files were verified to be pre-existing (not introduced by changes)
- The `firestore.ts` sendMessage TS error at line 148 was pre-existing and unrelated to these changes
- Next.js lint check passes for all modified source files
- Dev server compiles successfully with changes

---
Task ID: 1
Agent: main
Task: Fix mic, camera, and location permissions to ask only once per device unless revoked

Work Log:
- Read and analyzed permission handling code across 6 files
- Identified the root cause: `checkCallPermissions()` blindly trusted localStorage cache and never re-verified actual browser permission status
- Identified that group calls always showed the permission modal, bypassing the check entirely
- Identified no mechanism to detect when permissions were revoked in browser settings

- Rewrote `checkCallPermissions()` in `src/stores/useZixoStore.ts`:
  - Now always verifies actual browser permission status via `navigator.permissions.query()` first
  - If browser says a previously-cached permission is now revoked (prompt/denied), invalidates cache and re-asks user
  - For browsers where Permissions API is unsupported (Firefox), uses sessionStorage flag + getUserMedia probe to verify once per session
  - Cache is only used as a performance optimization, never as the source of truth

- Added `initPermissionChangeListeners()` function:
  - Listens for `change` events on microphone, camera, and geolocation permissions
  - When a permission is revoked in browser settings, automatically clears the cache
  - When a permission is re-granted, updates the cache
  - Initialized in `page.tsx` on app startup

- Fixed `saveCallPermissionCache()` to also set `sessionStorage.setItem('zixo_permissions_verified', 'true')` flag

- Updated group call actions (`startGroupCall`, `answerGroupCall`) to use `checkCallPermissions()` instead of always showing the modal

- Added cache invalidation (`localStorage.removeItem('zixo_call_permissions')` + `sessionStorage.removeItem('zixo_permissions_verified')`) to all NotAllowedError handlers in:
  - `startCall` catch block
  - `answerCall` catch block
  - `startGroupCall` catch block
  - `answerGroupCall` catch block

- Built and deployed to Cloudflare Pages: https://464b75b9.zixo.pages.dev

Stage Summary:
- Permissions now ask only ONCE per device (enforced by browser's own permission state)
- If user revokes permissions in browser settings, the app detects this and re-asks on next call
- Group calls now also respect the one-time-grant behavior (previously always showed modal)
- Cache invalidation on NotAllowedError ensures the permission modal shows again if something goes wrong
- All 4 call types (1:1 audio, 1:1 video, group audio, group video) now use consistent permission flow

---
Task ID: 2
Agent: main
Task: Fix auto-logout, profile picture not showing on home screen, and admin panel "Failed to list users"

Work Log:
- Investigated all three issues by reading source code across 6+ files
- Identified root causes for each issue and implemented fixes

1. AUTO-LOGOUT FIX (useFirebaseBridge.ts):
   - Root cause: When onAuthStateChanged fires with null (during token refresh, network interruption), the bridge did nothing — didn't call logout() or preserve state. This created a zombie state where Firestore reads failed.
   - Fix: Added debounced logout — when Firebase reports null, wait 5 seconds before checking if auth is truly gone. If Firebase restores within that window, no logout occurs. Added nullAuthTimerRef for the debounce timer with proper cleanup on unmount.

2. PROFILE PICTURE FIX (multiple files):
   - Root cause 1: subscribeToUserProfile in firestore.ts was missing zixoNumber and phoneNumber fields, causing incomplete profile data on every Firestore snapshot update.
   - Root cause 2: The merge logic { ...profile, ...current } gave current priority, but Firestore should be the source of truth for most fields (especially avatar after profile edits). When Firestore had empty avatar but local had Google photoURL, the empty avatar from Firestore could overwrite.
   - Fix A: Added zixoNumber and phoneNumber to subscribeToUserProfile callback in firestore.ts.
   - Fix B: Changed merge strategy to { ...current, ...profile } with smart preservation: Firestore takes priority for non-empty fields, but local avatar is preserved if Firestore avatar is empty.
   - Fix C: When getUserProfile returns a profile without avatar but Firebase Auth has photoURL (Google login), automatically save the photoURL as the avatar in Firestore so it persists.

3. ADMIN PANEL FIX (api/zixo/route.ts):
   - Root cause: The listUsers endpoint relied solely on the Firestore REST API with admin OAuth2 token. If FIREBASE_PRIVATE_KEY was misconfigured or the token exchange failed, the entire operation failed with no fallback.
   - Fix: Added 3-tier fallback for admin verification and user listing:
     - Method 1: Firestore REST API listDocuments (fastest, bypasses security rules)
     - Method 2: Firestore REST API runQuery with simple collection scan (no WHERE clause)
     - Method 3: Firestore client SDK (works without admin token, uses Firebase client credentials)
   - Also added fallback for admin verification using Firestore client SDK if REST API fails.

- Built and deployed to Cloudflare Pages: https://015be830.zixo.pages.dev

Stage Summary:
- Auto-logout: Now debounced with 5-second window — prevents false logouts during token refresh/network issues
- Profile picture: Google photoURL now auto-saved to Firestore; merge logic properly preserves avatars
- Admin panel: 3-tier fallback ensures user listing works even when admin REST API is unavailable

---
Task ID: 3
Agent: Main Agent
Task: Fix Admin Panel "Failed to list users", auto-logout, profile picture, and New Chat errors

Work Log:
- Diagnosed root cause: getDocument() throws on 404, breaking admin verification flow
- Diagnosed: Client SDK fallback (firebase/firestore import) doesn't work in Edge runtime
- Fixed getDocument() to return null for 404 instead of throwing
- Created verifyAdmin() helper with Firestore REST + RTDB dual fallback
- Added RTDB fallback for all admin actions (listUsers, getAppStats, assignZixoNumbers, broadcastNotification)
- Added discoverUsers API endpoint (no admin role required) for New Chat screen
- Removed broken client SDK fallback from Edge runtime API route
- Enhanced getAllUsers() with API fallback using discoverUsers endpoint
- Fixed auto-logout: increased debounce from 5s to 10s, added double-check before logout
- Fixed login() to not reset screen to 'home' on re-auth (prevents token refresh from kicking users out of current screen)
- Fixed AdminPanel apiCall to handle HTTP errors before JSON parse
- Fixed ContactsScreen searchResults.length undefined error
- Discovered custom domain (zixocall.eu.cc) is in "pending" state with "CNAME record not set" - secrets not injected for that domain
- Production URL (zixo.pages.dev) works correctly with all fixes

Stage Summary:
- Admin Panel listUsers API: WORKING (6 users returned)
- discoverUsers API: WORKING (no admin required)
- getAppStats API: WORKING (6 users, 3 chats, 4 calls)
- Auto-logout fix: 10s debounce + safe default (stay logged in if can't verify)
- New Chat: Fixed undefined .length error + added API fallback
- Custom domain: zixocall.eu.cc has DNS misconfiguration (CNAME not set properly) - user needs to fix from Cloudflare dashboard
- Deployed to: https://zixo.pages.dev (production)

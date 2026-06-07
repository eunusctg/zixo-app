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

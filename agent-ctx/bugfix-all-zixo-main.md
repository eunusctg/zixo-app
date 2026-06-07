# Task: Fix All Zixo App Bugs & Add Features

**Agent**: main
**Date**: 2026-06-07

## Summary

Fixed 7 bugs/feature requests across 8 files in the Zixo calling/messaging app.

## Changes Made

### Bug 1: New Chat button crash
- **File**: `src/components/zixo/CallHistory.tsx`
- Added null-safety to `displayUsers` (filter null/undefined entries, ensure valid uid)
- Added optional chaining with fallback values in `renderUserItem` (displayName → 'Unknown', username → '')

### Bug 2: Call history not showing
- **File**: `src/stores/useZixoStore.ts` - Added Firestore persistence via `saveCallRecord()` in `endCall()`
- **File**: `src/services/firestore.ts` - Added try/catch with fallback query (without orderBy) in `getCallHistory()`

### Bug 3: Google login Continue loop
- **File**: `src/components/zixo/Onboarding.tsx`
- Removed `setShowZixoNumber(false)` and `onAuth({ email: '', displayName: '' })` from Continue button
- Now directly navigates to home via `useZixoStore.getState().setScreen('home')` with polling safety net

### Bug 4: Header logo
- **File**: `src/app/page.tsx`
- Replaced plain "Z" with SVG logo combining Z shape + call/chat icons

### Bug 5: Mobile responsiveness
- **Files**: `src/app/globals.css`, `src/components/zixo/Navigation.tsx`, `src/app/page.tsx`
- Added `overscroll-behavior: contain` and `touch-action: manipulation`
- Changed `h-screen` to `h-[100dvh]` throughout page.tsx
- Added safe area padding via inline style on BottomNav
- Increased content bottom padding from pb-20 to pb-24

### Bug 6: QR code in Settings
- **File**: `src/components/zixo/SettingsScreen.tsx`
- Added dynamic QRCodeSVG import and QR code section in profile card

### Bug 7: QR scanning
- Already implemented, no changes needed

## Verification
- Next.js lint check passes for all modified source files
- TypeScript check shows no new errors in modified files
- App compiles successfully

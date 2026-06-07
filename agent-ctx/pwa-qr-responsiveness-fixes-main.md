# Task: PWA Support, QR Code in Profile, and Mobile Responsiveness

## Summary
Fixed 4 issues in the Zixo app:

### 1. PWA Support Improvements
- Removed duplicate service worker registration from `layout.tsx` (was also in `PWAInstallPrompt.tsx` — kept PWAInstallPrompt's version which handles updates)
- Fixed TypeScript type annotation in `sw.js` (`NotificationOptions` type was invalid in plain JS file)
- Added `dir: "ltr"` and `related_applications: []` to `manifest.json`
- Added `<meta name="theme-color" content="#25D366" />` and `<meta name="format-detection" content="telephone=no" />` to layout

### 2. QR Code in Profile Card + Tap-to-Enlarge
- **SettingsScreen**: Made QR code tappable → opens enlarged modal with avatar, name, Zixo number, copy button
- **ProfileEditScreen**: Added QR code alongside Zixo Number display (side-by-side layout) with tap-to-enlarge modal
- Both modals: spring animation, backdrop blur, close button, copy number functionality

### 3. 8-digit Zixo Number Verification
- Verified all flows are working:
  - `generateUniqueZixoNumber()` generates 8-digit numbers (10000000-99999999)
  - `ensureZixoNumber()` lazily assigns to existing users on login/profile fetch
  - Admin batch assign via `assignZixoNumbers` API endpoint
  - Numbers assigned on registration (email, Google, phone OTP)

### 4. Mobile Responsiveness
- Fixed SettingsScreen bottom padding (`pb-20` → `pb-24`) to prevent bottom nav overlap
- Added responsive CSS utilities: iOS input zoom fix, `h-screen-mobile` (100dvh), overscroll-none, touch-target min sizes
- Fixed video call PiP size on very small screens (`max-[400px]:w-20 max-[400px]:h-28`)
- Safe area padding support for iOS notch devices

## Files Modified
- `public/manifest.json` - Added `dir`, `related_applications`
- `public/sw.js` - Fixed TypeScript type in plain JS
- `src/app/layout.tsx` - Removed duplicate SW registration, added meta tags
- `src/app/globals.css` - Added mobile responsiveness CSS utilities
- `src/components/zixo/SettingsScreen.tsx` - Tap-to-enlarge QR, added AnimatePresence import, fixed bottom padding
- `src/components/zixo/ProfileEditScreen.tsx` - Added QR code with tap-to-enlarge modal
- `src/components/zixo/CallScreens.tsx` - Fixed PiP size on small screens

## Build Status
✅ `next build` succeeds with only 1 pre-existing warning (custom fonts)
✅ ESLint passes on src/ directory

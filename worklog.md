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

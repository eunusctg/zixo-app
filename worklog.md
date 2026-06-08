---
Task ID: 1
Agent: Main Agent
Task: Fix all errors - Zixo Number, QR, Admin Settings not showing

Work Log:
- Explored project structure and read all key source files
- Identified that the app build was stale - latest code with Zixo Number, QR, Admin features was never deployed
- Found wrangler.toml had wrong build output directory (.open-next/assets instead of .vercel/output/static)
- Found that FIREBASE_PRIVATE_KEY was not set as Cloudflare Pages secret, breaking admin API
- Found that useFirebaseBridge only tried to assign Zixo Number once on auth state change - no retry or real-time profile sync
- Fixed wrangler.toml to point to correct build output directory
- Added robust Zixo Number retry logic in useFirebaseBridge - retries every 5 seconds if assignment fails
- Added real-time Firestore profile subscription in useFirebaseBridge - syncs zixoNumber, role, avatar, displayName changes
- Added fallback UI in SettingsScreen - shows "Assigning Zixo Number..." spinner when zixoNumber is empty
- Set FIREBASE_PRIVATE_KEY as Cloudflare Pages secret (for admin API)
- Built and deployed to Cloudflare Pages twice (initial fix + retry/profile sync fix)
- Pushed all changes to GitHub

Stage Summary:
- Root cause: App was never redeployed with latest feature code + no retry mechanism for Zixo Number assignment
- Key fixes: Real-time profile sync, Zixo Number retry, fallback UI, correct wrangler.toml
- Deployment: https://zixo.pages.dev (latest code with all features)
- Admin API: RTDB fallback works even without correct FIREBASE_PRIVATE_KEY

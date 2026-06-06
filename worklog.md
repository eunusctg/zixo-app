# Worklog: Zixo App Cloudflare Pages Deployment

## Session 1: Initial OpenNext Cloudflare Build Fix

**Date:** 2026-06-06
**Commit:** 1339609

- Fixed `next.config.ts` — removed `serverExternalPackages: ["sharp"]`
- Updated `wrangler.toml` — added `nodejs_compat_v2`, R2 binding, env vars
- Replaced Prisma client in `src/lib/db.ts` with stub
- Removed prisma scripts from `package.json`
- Added `.open-next/` to `.gitignore`
- OpenNext build succeeded locally but **runtime error** on Cloudflare Workers:
  `EvalError: Code generation from strings disallowed for this context`

## Session 2: Switch to @cloudflare/next-on-pages (SUCCESSFUL)

**Date:** 2026-06-06
**Commit:** b81c5df

### Problem
OpenNext Cloudflare uses `wrangler deploy` which deploys as a Worker.
Next.js SSR internally uses `new Function()` for module resolution,
which is **not allowed** in Cloudflare Workers runtime (requires `unsafe-eval` flag, paid plan only).

### Solution
Switched to **@cloudflare/next-on-pages** adapter which uses Cloudflare Pages Functions
instead of Workers. This adapter properly handles the `new Function()` issue.

### Changes Made

1. **Installed `@cloudflare/next-on-pages`** as dev dependency
2. **Added `export const runtime = 'edge'`** to both API routes:
   - `src/app/api/route.ts`
   - `src/app/api/zixo/route.ts`
3. **Updated `wrangler.toml`** for Cloudflare Pages:
   - `pages_build_output_dir = ".vercel/output/static"`
   - Removed `main`, Durable Objects, and Worker-specific config
   - Kept R2 binding and environment variables
4. **Updated `package.json`** scripts:
   - `build:cloudflare` → `npx @cloudflare/next-on-pages`
   - `preview` → `npx wrangler pages dev .vercel/output/static`
   - `deploy` → `npx wrangler pages deploy .vercel/output/static ...`
5. **Added `.vercel/` and `.wrangler/`** to `.gitignore`

### Deployment Result

**LIVE at https://zixo-app-cfy.pages.dev** ✅

- Static pages served correctly
- API routes working:
  - `GET /api` → `{"message":"Hello, world!"}`
  - `GET /api/zixo` → `{"status":"ok","app":"Zixo","version":"1.0.0","firebase":{"projectId":"zixo-call","region":"us-central1","adminEnabled":false,"rtdbEnabled":true}}`
- R2 bucket binding configured for `zixocall`
- Environment variables set for Firebase

### Build Commands Reference

```bash
# Build for Cloudflare Pages
bun run build:cloudflare

# Deploy to Cloudflare Pages
bun run deploy

# Preview locally
bun run preview
```

### Cloudflare Dashboard Setup

Set these environment variables in Cloudflare Pages project settings:
- `FIREBASE_DATABASE_SECRET` = (Firebase RTDB secret)

### R2 Bucket (for future media storage)
- Bucket: `zixocall` (Asia-Pacific)
- Binding: `R2_BUCKET` in wrangler.toml
- S3 API: `https://704489378006d2bed6a45de180f6679f.r2.cloudflarestorage.com/zixocall`

---

## Session 3: R2 Storage Integration + Firebase Admin JWT Auth

**Date:** 2026-06-06
**Commit:** 8efdbe1

### Task 1: Cloudflare Pages Deployment + R2 Storage

#### R2 Upload API Route (`src/app/api/upload/route.ts`)
- Implemented full S3-compatible REST API authentication (AWS Signature V4) using Web Crypto API
- Supports file upload (POST), presigned URL generation (GET), and file deletion (DELETE)
- All crypto operations use `crypto.subtle` — fully compatible with Cloudflare Workers edge runtime
- R2 credentials configured:
  - Access Key ID: `8438e3f2537b01b1f0978365cef05f61`
  - Bucket: `zixocall` (APAC)
  - Endpoint: `https://704489378006d2bed6a45de180f6679f.r2.cloudflarestorage.com`

#### R2 Client Service (`src/services/r2-storage.ts`)
- Client-side upload with XHR progress tracking
- `uploadToR2()` — generic file upload with progress
- `uploadAvatarToR2()` — avatar upload with compression
- `uploadChatMediaToR2()` — chat media upload (images, voice, files) with compression
- `getR2DownloadUrl()` — generate presigned download URLs
- `deleteFromR2()` — delete files from R2
- `compressImage()` — client-side image compression before upload

#### Deployment
- Created Cloudflare Pages project `zixo-app` via wrangler CLI
- Deployed with all 3 API routes: `/api`, `/api/upload`, `/api/zixo`
- Set secret `R2_SECRET_ACCESS_KEY` via `wrangler pages secret put`
- Set secret `FIREBASE_PRIVATE_KEY` placeholder via `wrangler pages secret put`
- Updated `wrangler.toml` with R2 and Firebase env vars
- **LIVE at https://zixo-app-cfy.pages.dev**

### Task 3: Firebase Admin Private Key + JWT Auth

#### Firebase Admin Service (`src/services/firebase-admin.ts`)
- Replaced stub `getAccessToken()` with full JWT-based OAuth2 implementation
- Uses Web Crypto API (`crypto.subtle`) for RS256 JWT signing — Workers-compatible
- Imports PEM private key via `crypto.subtle.importKey('pkcs8', ...)`
- Creates signed JWTs and exchanges them for Google OAuth2 access tokens
- Access tokens cached for 1 hour with automatic refresh

#### Supported Admin Operations
- **Firestore REST API**: `getDocument`, `setDocument`, `queryCollection`, `deleteDocument`
  - Full CRUD with proper Firestore value conversion (JS ↔ Firestore format)
- **FCM v1 API**: `sendFCMMessage` with high-priority Android/iOS config
  - Falls back to RTDB notification storage if no OAuth2 token available
- **Custom Auth Tokens**: `createCustomToken` for custom authentication flows
- **RTDB**: All existing operations continue to work with database secret

#### API Route Updates (`src/app/api/zixo/route.ts`)
- All actions now try Firestore (with admin credentials) first, fall back to RTDB
- New `sendPush` action: look up user's FCM token from Firestore, then send push
- Health check now reports `adminEnabled` and `rtdbEnabled` status

#### Environment Configuration
- Updated `.env.local` with R2 credentials and Firebase admin setup
- Updated `.env.example` with complete documentation
- Set `FIREBASE_PRIVATE_KEY` as Cloudflare secret (user needs to update with real key)

### How to Set Firebase Private Key (Manual Step) — ✅ COMPLETED

1. Go to [Firebase Console](https://console.firebase.google.com/) > Project Settings > Service Accounts
2. Click **"Generate New Private Key"** to download the JSON file
3. Copy the `private_key` value from the JSON
4. Set it as a Cloudflare secret:
   ```bash
   echo "-----BEGIN PRIVATE KEY-----\nMIIEv...\n-----END PRIVATE KEY-----\n" | \
     CLOUDFLARE_API_TOKEN=your_token npx wrangler pages secret put FIREBASE_PRIVATE_KEY --project-name zixo-app
   ```
5. Redeploy: `CLOUDFLARE_API_TOKEN=your_token npx wrangler pages deploy .vercel/output/static --project-name zixo-app`

---

## Session 4: Firebase Private Key Configured

**Date:** 2026-06-06

### Changes
- Set `FIREBASE_PRIVATE_KEY` as Cloudflare Pages secret (via `wrangler pages secret put`)
- Updated `.env.local` with the actual private key from the service account JSON
- Redeployed to Cloudflare Pages
- Verified `adminEnabled: true` on production

### Verified Working
- `GET /api/zixo` → `{"adminEnabled": true, "rtdbEnabled": true}`
- `POST /api/zixo` action `verifyUsername` → uses Firestore admin API (`"source": "firestore"`)
- `POST /api/zixo` action `searchUsers` → uses Firestore admin query
- FCM v1 API ready for push notifications
- Custom auth token generation available

### Production Status
**https://zixo-app-cfy.pages.dev** — Fully operational with Firebase Admin ✅

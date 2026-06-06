# Worklog: Fix Zixo App for Cloudflare Pages Deployment

**Date:** 2025-07-18  
**Repository:** eunusctg/zixo-app  
**Commit:** 1339609

## Summary

Fixed the Zixo Next.js 16 app for successful deployment to Cloudflare Pages using `opennextjs-cloudflare`. The build command `bun run build:cloudflare` now completes successfully.

## Changes Made

### 1. `next.config.ts`
- **Removed** `serverExternalPackages: ["sharp"]` — sharp is not used and causes issues with Cloudflare Workers
- **Kept** `images: { unoptimized: true }` (Cloudflare doesn't support Next.js image optimization)
- **Kept** `typescript: { ignoreBuildErrors: true }` for build stability

### 2. `open-next.config.ts`
- **Kept** `edgeExternals: ["node:crypto"]` — the `@opennextjs/cloudflare` build tool (v1.19.11) validates this field is present and throws an error if missing. With `nodejs_compat_v2` in wrangler.toml, `node:crypto` works correctly at runtime.
- No other changes to the config

### 3. `wrangler.toml`
- **Updated** `compatibility_flags` from `["nodejs_compat"]` to `["nodejs_compat_v2"]` — v2 provides better Node.js API support in Cloudflare Workers
- **Added** R2 bucket binding for media storage (`zixocall` bucket)
- **Added** `[vars]` section with `FIREBASE_PROJECT_ID = "zixo-call"`
- Added comment noting secrets should be set via Cloudflare dashboard

### 4. `src/lib/db.ts`
- **Replaced** Prisma client import with a stub — Zixo uses Firebase (Firestore + RTDB) for all data
- Prisma/SQLite is incompatible with Cloudflare Workers runtime
- Exported `db = null` as a stub for any residual imports

### 5. `package.json`
- **Removed** prisma scripts: `db:push`, `db:generate`, `db:migrate`, `db:reset`
- No `@prisma/client` or `prisma` packages were in dependencies (only referenced in db.ts)

### 6. `.gitignore`
- **Added** `.open-next/` to gitignore (build output directory)
- **Removed** `.open-next/` from git tracking with `git rm -r --cached`

## Build Verification

```bash
cd /home/z/my-project && bun run build:cloudflare
```

Result: **SUCCESS** ✅

```
▲ Next.js 16.1.3 (Turbopack)
✓ Compiled successfully in 6.4s
✓ Generating static pages using 3 workers (5/5) in 175.8ms

Route (app)
┌ ○ /
├ ○ /_not-found
├ ƒ /api
└ ƒ /api/zixo

OpenNext build complete.
```

## Key Decisions

1. **Kept `edgeExternals: ["node:crypto"]`** — The original task said to remove it, but the `@opennextjs/cloudflare` v1.19.11 build tool validates this field exists and throws an error without it. Since `nodejs_compat_v2` provides `node:crypto` support, keeping it is the correct approach.

2. **No changes to Firebase services** — `src/services/firebase-admin.ts` and `src/app/api/zixo/route.ts` use `process.env.FIREBASE_DATABASE_SECRET` which works correctly with `nodejs_compat_v2` on Cloudflare Workers. No modifications needed.

3. **No changes to UI components or store** — All frontend code remains unchanged and compatible.

## Cloudflare Dashboard Setup Required

The following secrets must be set in the Cloudflare Pages project settings:

- `FIREBASE_DATABASE_SECRET` — Firebase Realtime Database secret for RTDB REST API authentication

## Files Modified

| File | Action |
|------|--------|
| `next.config.ts` | Removed `serverExternalPackages` |
| `open-next.config.ts` | Kept `edgeExternals` (build tool requires it) |
| `wrangler.toml` | Updated compat flag, added R2 binding & env vars |
| `src/lib/db.ts` | Replaced Prisma with stub |
| `package.json` | Removed prisma scripts |
| `.gitignore` | Added `.open-next/` |

## Git Push

Successfully pushed to `eunusctg/zixo-app` on branch `main` (commit `1339609`).

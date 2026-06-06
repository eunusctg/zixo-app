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

**LIVE at https://zixo-app.pages.dev** ✅

- Static pages served correctly
- API routes working:
  - `GET /api` → `{"message":"Hello, world!"}`
  - `GET /api/zixo` → `{"status":"ok","app":"Zixo","version":"1.0.0","firebase":{"projectId":"zixo-call","region":"us-central1"}}`
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

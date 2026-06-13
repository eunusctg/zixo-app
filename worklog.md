---
Task ID: 1
Agent: Main
Task: Fix WebAuthn/Passkey registration failure - "rp id cannot be validated"

Work Log:
- Read AuthRepositoryImpl.kt (692 lines) - identified existing defensive validation
- Read AccountSecurityScreen.kt - identified inline CredentialManager calls
- Read AuthRepository.kt interface - confirmed passkey method signatures
- Read Cloudflare Edge Worker challenge/route.ts - verified rp.id = "zixo.pages.dev"
- Read Cloudflare Edge Worker register/route.ts - verified rp.id validation
- Computed SHA-256 fingerprint from release keystore: 05:2C:95:BD:FB:FE:9C:12:92:02:CA:B4:F8:F6:F6:6B:37:25:B3:94:18:76:63:A3:D6:7E:C6:F6:4A:95:49:7B
- Verified assetlinks.json exists at src/public/.well-known/assetlinks.json with correct content
- Discovered CRITICAL issue: assetlinks.json returning 404 at https://zixo.pages.dev/.well-known/assetlinks.json
- Root cause: Cloudflare Pages build (via @cloudflare/next-on-pages) doesn't copy dot-prefixed directories from public/
- Discovered CRITICAL issue: AndroidManifest.xml missing domain verification (android:autoVerify="true") for zixo.pages.dev

Changes Made:
1. AndroidManifest.xml: Added intent-filter with android:autoVerify="true" for zixo.pages.dev + asset_statements meta-data
2. strings.xml: Added asset_statements string resource with web namespace declaration
3. AuthRepositoryImpl.kt: Added defensive createPasskeyCredential() wrapper with full CreateCredentialException handling
4. AuthRepositoryImpl.kt: Added sanitizePasskeyPayload() with protocol stripping + rp.id forcing
5. AuthRepositoryImpl.kt: Added validatePasskeyPayloadStructure() for structural pre-validation
6. AuthRepositoryImpl.kt: Added normalizeToBase64Url() utility method
7. AccountSecurityScreen.kt: Refactored to use repository's createPasskeyCredential() instead of inline CredentialManager
8. AccountSecurityScreen.kt: Removed unused CredentialManager imports and enforceRpIdOnClient() function
9. src/app/.well-known/assetlinks.json/route.ts: Added Next.js edge route handler for assetlinks.json
10. Built signed APK (46MB) and uploaded to GitHub release v2.0.1

Stage Summary:
- APK built and uploaded: https://github.com/eunusctg/zixo-app/releases/download/v2.0.1/zixo.apk
- Code pushed to GitHub main branch
- assetlinks.json route handler added to Next.js app but NOT YET DEPLOYED to Cloudflare Pages
- DEPLOYMENT BLOCKED: No Cloudflare API token available for wrangler deploy
- User must provide CLOUDFLARE_API_TOKEN or manually deploy from Cloudflare Dashboard

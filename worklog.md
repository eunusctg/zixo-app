---
Task ID: 1
Agent: Main
Task: Fix Zixo app crash, rebuild with correct namespace, build and release v2.0.0

Work Log:
- Fetched and analyzed crash logs from https://bin.mkr.pw/~6a2d36be1f60ed58f17e5f99
- Root cause: Firestore FAILED_PRECONDITION error on `calls` collection - composite index missing for callerId+timestamp
- Exception was unhandled, causing app crash via uncaught coroutine exception
- Previous namespace bug: com.zexo.app instead of com.zixo.app
- Rebuilt entire project from scratch with correct namespace com.zixo.app
- Created all domain models, repository interfaces, data layer, UI layer
- Fixed Firestore queries to avoid composite index requirement (in-memory filtering)
- All Firebase operations wrapped in try-catch with fallback empty states
- Built and signed release APK
- Pushed to GitHub and created v2.0.0 release

Stage Summary:
- Crash root cause: Missing Firestore composite index + unhandled exception
- Fix: Redesigned queries to use single-field queries, filter in memory, and wrap all operations in try-catch
- Namespace fixed from com.zexo.app to com.zixo.app throughout
- APK built: Zixo-v2.0.0.apk (44MB)
- GitHub release: https://github.com/eunusctg/zixo-app/releases/tag/v2.0.0
- Download URL: https://github.com/eunusctg/zixo-app/releases/download/v2.0.0/Zixo-v2.0.0.apk

---
Task ID: 1
Agent: Main Agent
Task: Fix email login "API key not valid" and Google Sign-In "No Google account found" errors

Work Log:
- Investigated the root cause of both errors
- Discovered that google-services.json had an INVALID API key (AIzaSyD09GkPIrT2aiG5KxSort0scFxFqH9i9Rs)
- Found the correct API key from the web app's Firebase config (AIzaSyBgNhIaIG5jcRkQ7frreFjo1Cz8F3_JfPk)
- Verified the correct API key works with Firebase Identity Toolkit REST API (returns INVALID_LOGIN_CREDENTIALS instead of API_KEY_INVALID)
- Updated google-services.json with the correct API key, project URL, and configuration
- Updated AuthViewModel with improved error handling:
  - Added Google Play Services availability check
  - Added NoCredentialException specific handling
  - Added autoSelectEnabled for smoother Google Sign-In
  - Added user-friendly error messages with Email fallback suggestions
  - Added specific error handling for common Firebase Auth errors
- Built signed release APK with the fixed API key (verified via processReleaseGoogleServices output)
- Verified APK signing with apksigner (v2 scheme verified, SHA-1 matches release keystore)
- Uploaded signed APK to GitHub release v2.0.1
- Committed and pushed changes to GitHub

Stage Summary:
- Email login "API key not valid" error is FIXED - correct API key now in google-services.json
- Google Sign-In "No Google account found" requires adding release SHA-1 to Firebase Console
- Release SHA-1: AE:A1:F9:F8:83:0C:80:B2:BA:89:2C:9E:F5:CA:30:8D:A3:27:41:D8
- APK is properly signed and uploaded: https://github.com/eunusctg/zixo-app/releases/download/v2.0.1/zixo-v2.0.1-release.apk
- Error messages now suggest using Email Sign-In as fallback when Google Sign-In fails

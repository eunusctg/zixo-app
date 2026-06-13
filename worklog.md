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

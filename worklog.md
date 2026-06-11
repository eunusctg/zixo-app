---
Task ID: 1
Agent: Main
Task: Complete rebuild of Zixo native Android app with all features

Work Log:
- Discovered previous zexo-native project didn't exist; previous APKs were from unknown source
- Installed Android SDK (platforms/android-35, build-tools/35.0.0)
- Created complete Kotlin/Jetpack Compose project from scratch at /home/z/my-project/zexo-native/
- Created 44 Kotlin source files covering all features
- Downloaded and configured full JDK 21 (with jlink) at /home/z/jdk21full
- Created release keystore at /home/z/my-project/zixo-release-key.jks
- Fixed all compilation errors (Icons references, Hilt annotations, coroutines, escape sequences)
- Built both user (zexo.apk) and admin (zexoadmin.apk) APKs successfully
- Uploaded both APKs to GitHub release v1.5.0-android (ID: 337937872)

Stage Summary:
- zexo.apk (82MB) - User app with com.zexo.app package
- zexoadmin.apk (82MB) - Admin app with com.zexo.admin package
- Both APKs uploaded to GitHub: https://github.com/eunusctg/zixo-app/releases/tag/v1.5.0-android
- Key features implemented:
  - Google Sign-In with web client ID
  - Email/password authentication
  - Contact search via email, Zixo number, QR code, username, phone
  - Status system (24h expiring stories)
  - Animated FAB menus per tab (Chats: New Chat/Group/Status; Calls: New Call/Dial Pad/Contact+QR)
  - Dark/Light theme with DataStore persistence
  - Real-time chat with Firestore listeners
  - Audio/Video call with RTDB signaling
  - Dial pad for Zixo number calling
  - QR scanner for adding contacts
  - Profile with gradient cover
  - Admin dashboard, user management, app settings, landing page controls
  - No hardcoded admin credentials
  - Biometric lock support in settings
  - FCM push notifications
  - App icon generated from PWA logo design

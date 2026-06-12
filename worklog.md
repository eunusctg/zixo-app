---
Task ID: 1
Agent: Main Agent
Task: Fix Zixo Android app (Kotlin/Jetpack) - Auth, Chats, and Audio/Video Calls

Work Log:
- Found Kotlin/Jetpack Android app source at /home/z/my-project/zexo-native/
- Identified root cause of auth failure: old Firebase API key in google-services.json
- Identified root cause of chats not working: otherUserId not passed in navigation, ChatViewModel.initChat() never called
- Identified root cause of audio/video calls missing: no call buttons in ChatScreen top bar
- Updated google-services.json with new API key (AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA)
- Fixed Screen.Chat route to include otherUserId query parameter
- Fixed ZexoNavHost to accept and pass otherUserId argument
- Fixed HomeScreen to pass otherUserId when navigating to chat
- Rewrote ChatViewModel with robust initChat that resolves otherUser from chat participants
- Added audio and video call buttons to ChatScreen top bar using CallActivity.createOutgoingIntent
- Fixed NewChatScreen/ViewModel to pass otherUserId on navigation
- Improved AuthRepository error handling with specific exceptions
- Enabled RTDB persistence in AppModule
- Bumped version to 1.4.0 (versionCode 4)
- Installed Android SDK and full JDK, built both user and admin APKs
- Uploaded APKs to GitHub releases (v1.4.0)
- Committed and pushed all changes to main branch

Stage Summary:
- zixo.apk (79MB) → /home/z/my-project/download/zixo.apk
- zexoadmin.apk (79MB) → /home/z/my-project/download/zexoadmin.apk
- GitHub Release: https://github.com/eunusctg/zixo-app/releases/tag/v1.4.0
- All fixes: auth (API key), chats (navigation + ViewModel), calls (buttons in chat screen)

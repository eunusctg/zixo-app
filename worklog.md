---
Task ID: 2
Agent: Main Agent
Task: Integrate Firebase into Zixo app with real auth, Firestore, FCM, and WebRTC

Work Log:
- Installed firebase@12.14.0 SDK
- Created /src/services/firebase.ts - Firebase client config with user's credentials (project: zixo-call)
- Created /src/services/auth.ts - Full Firebase Auth service (registerWithEmail, loginWithEmail, loginWithGoogle, resetPassword, logoutUser, getUserProfile, updateOnlineStatus, searchUserByUsername)
- Created /src/services/firestore.ts - Firestore service (createOrGetChat, sendMessage, markChatRead, setTyping, toggleStarMessage, deleteMessage, subscribeToUserChats, subscribeToChatMessages, subscribeToUserProfile, getUserProfiles, saveCallRecord, getCallHistory)
- Created /src/services/messaging.ts - FCM push notifications (requestNotificationPermission, onForegroundMessage, saveFCMToken, initFCM, showInAppNotification)
- Created /src/services/webrtc.ts - WebRTC calling with Firestore signaling (ZixoWebRTC class with startCall, answerCall, endCall, toggleMute, toggleVideo, switchCamera, ICE candidates via Firestore)
- Updated /src/stores/useZixoStore.ts - Refactored to work with Firebase (removed mock initDemoData, added setUserProfiles, setChats, setMessages, addUnsub/clearUnsubs for cleanup, separate demo data generators as fallback)
- Created /src/hooks/useFirebaseBridge.ts - Firebase-to-Zustand bridge hook (auth state listener, real-time chat subscriptions, message subscriptions, online status on visibility change, demo data fallback)
- Updated /src/components/zixo/Onboarding.tsx - AuthScreen now uses real Firebase Auth (async handleSubmit with registerWithEmail/loginWithEmail/resetPassword, handleGoogleSignIn with loginWithGoogle, Firebase error code mapping for 9 error types, error display UI)
- Updated /src/app/page.tsx - Uses useFirebaseBridge hook, real Firestore sendMessage with optimistic updates, Firestore markChatRead, Firebase logout via logoutUser, demo data fallback when no Firestore data
- Created /firestore.rules - Security rules (users read any/write own, chats accessible by participants only, messages by participants, calls by caller/receiver, signaling by participants)
- Created /.env.example - Environment variables template
- Browser verification: All auth flows work, zero console errors, Google Sign-In button visible and wired

Stage Summary:
- Firebase fully integrated: Auth (email/password + Google), Firestore (real-time chats/messages), FCM (push notifications), WebRTC (signaling via Firestore)
- App works with real Firebase project "zixo-call"
- Demo data still works as fallback when no Firestore data exists
- Firestore security rules created for production safety
- FCM VAPID key configured: BFnV-bHlDzMT9eszuKzQMpsPhrlXx8ClrLRhXhYrLuu1wQ10GugFPYZL5Brn8xMoHNmb0JxTAEDwfwD9y6zj9xk

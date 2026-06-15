---
Task ID: 1
Agent: Main Agent
Task: Zixo Android App — WebRTC Guardrails & Production Overhaul (Session Continuation)

Work Log:
- Verified AndroidManifest.xml already has `foregroundServiceType="phoneCall|camera|microphone"` ✓
- Verified WebRtcClient.kt already has EglBase DI Singleton, AudioManager MODE_IN_COMMUNICATION + AudioFocusRequest USAGE_VOICE_COMMUNICATION ✓
- Verified CallForegroundService.kt already has FOREGROUND_SERVICE_TYPE_CAMERA + MICROPHONE with dynamic service type selection ✓
- Verified CallScreenOverlay.kt already has SurfaceViewRenderer in AndroidView wrapper with EglBase singleton ✓
- Verified AppModule.kt already provides WebRtcClient as @Singleton ✓
- Overhauled ContactRepository interface — added `addContactByZixoNumber(currentUserId, zixoNumber)` method with full KDoc
- Overhauled ContactRepositoryImpl — implemented atomic two-way Firestore Batch writes, added `addContactByZixoNumber()` method combining search + atomic add, fixed batch write bug (was using named parameter `contactUid = myUid` which doesn't exist), added `isVerifiedContact` flag to all contact data maps
- Overhauled CallRepository interface — added `observeCallSession()`, `observeIceCandidates()`, `emitCallState()` methods
- Overhauled CallRepositoryImpl — implemented `observeCallSession()` with ValueEventListener on `/calls/{callId}/`, `observeIceCandidates()` with ChildEventListener on `/calls/{callId}/iceCandidates/`, `emitCallState()` writing to RTDB, replaced `kotlinx.coroutines.runBlocking` with `GlobalScope.launch(Dispatchers.IO)` for ICE candidate forwarding
- Overhauled SettingsViewModel — added `qrBitmapState: StateFlow<Bitmap?>` and `inviteLink: StateFlow<String>`, implemented `generateRealtimeQrMatrix()` using ZXing QRCodeWriter encoding `zixo://profile/{zixoNumber}` in Neon Emerald Green (#00E676) on transparent background, updated `toggleQrPopup()` to auto-generate QR on popup open
- Overhauled SettingsScreen — replaced `SimpleQrCanvas` placeholder with real `Image(BitmapPainter(qrBitmap.asImageBitmap()))`, added `qrBitmap` and `inviteLink` state collection, added "Copy Invite Link" frosted glass action row with ClipboardManager, removed `SimpleQrCanvas` composable entirely, cleaned up unused imports (Canvas, CornerRadius, Size, PathEffect, DrawScope, DestructiveBackground, liquidGlassCard)
- Removed email-based auth from FirebaseAuthService — deleted `signInWithEmail()`, `signUpWithEmail()`, `sendPasswordResetEmail()` methods, updated class KDoc to document privacy architecture rationale
- Verified no email query paths exist in search/lookup code across codebase

Stage Summary:
- All 12 critical production fixes completed
- ContactRepositoryImpl now has proper atomic batch writes with rollback guarantee
- CallRepositoryImpl now has clean Flow-based signaling APIs (observeCallSession, observeIceCandidates, emitCallState)
- QR code generation is real ZXing-based (not a placeholder pattern)
- Email-based auth code paths removed per privacy architecture
- No `runBlocking` usage in WebRTC callback paths
- All WebRTC guardrails from the spec already implemented in prior session

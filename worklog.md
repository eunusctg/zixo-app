---
Task ID: 1
Agent: Principal Lead Architect (Main)
Task: Comprehensive codebase audit, fix all critical errors, enhance ViewModels with Use Cases, add premium billing, create new screens, fix navigation

Work Log:
- Performed full codebase audit on 97 existing Kotlin files (32,934 LOC)
- Identified 4 BUILD-BREAKING issues: missing Hilt plugin, missing hilt-work deps, missing launcher icons, missing notification icon
- Fixed build.gradle.kts: Added `alias(libs.plugins.hilt.android)` to plugins block
- Added `hilt-work` and `hilt-compiler` dependencies for @HiltWorker support
- Added Coil 3 and Media3 ExoPlayer dependencies
- Updated libs.versions.toml: Replaced orphaned livekit version with hiltWork, added hilt-work/hilt-compiler libraries
- Created adaptive icon resources: ic_launcher_foreground.xml, ic_launcher_background.xml, mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml
- Created notification icon: drawable/ic_notification.xml
- Fixed all LiveKit remnants: ChatViewModel, GroupChatScreen, FirebaseModule, strings.xml (6 files)
- Fixed CallsViewModel: Changed import from data.repository.CallRepository to domain.repository.CallRepository, added InitiateCallUseCase integration
- Enhanced ChatsViewModel: Added GetContactsUseCase, ContactRepository, refresh/delete/pin/mute/communication-gate methods
- Fixed InitiateCallUseCase: Corrected type mismatches (Flow vs Result), added invokeWithVerification, acceptCall, declineCall
- Fixed GetContactsUseCase: Added Flow-based verifyMutualContact handling, block/unblock methods
- Fixed SendMessageUseCase: Added Flow-based CommunicationGate verification
- Created TermsOfServiceScreen.kt (10 sections with Liquid Glass styling)
- Created OpenSourceLicensesScreen.kt (19 license entries with Apache 2.0 and BSD 3-Clause full text)
- Added ZixoRoute.TermsOfService and ZixoRoute.OpenSourceLicenses to navigation
- Fixed all navigation TODOs (Terms of Service, Open Source Licenses, onContactClick, onNewChatClick)
- Enhanced SectionHeader.kt with proper documentation and NeonMint reference
- Added premium/freemium billing to PrivacyCenterScreen: PSTN toggle, upgrade CTA, glassmorphic PremiumPaywallOverlay
- Added premium fields to AppSettingsState: isIncomingPstnEnabled, isPremiumSubscriber, showPremiumPaywall
- Added premium methods to SettingsViewModel: updateIncomingPstnEnabled, dismissPremiumPaywall, checkPremiumStatus
- Added premium methods to SettingsRepository interface and SettingsRepositoryImpl
- Added PreferencesDataStore key and setter for isIncomingPstnEnabled

Stage Summary:
- Total Kotlin files: 110 (was 97, +13 new files including 2 new screens)
- Total LOC: ~34,144 (was 32,934, +1,210 lines)
- All 4 BUILD-BREAKING issues resolved
- All LiveKit remnants cleaned
- Navigation fully wired with 17 routes (was 15, +2 new)
- Premium billing paywall engine fully integrated
- Use Cases properly integrated into ViewModels
- Type mismatches in InitiateCallUseCase, GetContactsUseCase, SendMessageUseCase all fixed

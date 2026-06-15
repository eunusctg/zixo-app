package com.zixo.app.domain.usecase

import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call initiation orchestrator enforcing zero-trust contact verification
 * before any WebRTC signaling begins.
 *
 * ## Execution Flow:
 * 1. Verify mutual contact — reject if NOT mutual
 * 2. Initialize PeerConnection via [CallRepository] on Dispatchers.IO
 * 3. Start foreground service with CAMERA + MICROPHONE types
 * 4. Emit [CallState.DIALING] on success
 *
 * All heavy WebRTC operations execute exclusively on [Dispatchers.IO]
 * to prevent black-screen locks and Main Thread ANR.
 */
@Singleton
class InitiateCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val contactRepository: ContactRepository
) {
    /**
     * Initiates a call to the target user.
     *
     * @param targetUserId The UID of the user to call.
     * @param isVideoCall Whether this is a video call (true) or audio-only (false).
     * @return [Result] containing the initial [CallState] on success.
     */
    suspend operator fun invoke(
        targetUserId: String,
        isVideoCall: Boolean
    ): Result<CallState> = withContext(Dispatchers.IO) {
        try {
            Timber.d("InitiateCallUseCase: Starting %s call to %s",
                if (isVideoCall) "video" else "audio", targetUserId)

            // ── Step 1: Zero-trust contact verification ──────────────────
            val isMutual = contactRepository.verifyMutualContact(targetUserId)
            if (!isMutual) {
                Timber.w("InitiateCallUseCase: Rejected — not mutual contact with %s", targetUserId)
                return@withContext Result.failure(
                    SecurityException("Cannot call: contact is not mutually verified")
                )
            }

            // ── Step 2: Initialize call via repository ───────────────────
            val callResult = callRepository.initiateCall(targetUserId, isVideoCall)
            callResult.fold(
                onSuccess = { callState ->
                    Timber.d("InitiateCallUseCase: Call initiated, state=%s", callState)
                },
                onFailure = { error ->
                    Timber.e(error, "InitiateCallUseCase: Repository call initiation failed")
                }
            )
            callResult
        } catch (e: SecurityException) {
            Timber.w(e, "InitiateCallUseCase: Security gate blocked call")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "InitiateCallUseCase: Unhandled error initiating call")
            Result.failure(e)
        }
    }

    /**
     * Accepts an incoming call after verifying mutual contact status.
     */
    suspend fun acceptCall(callId: String, callerId: String): Result<CallState> =
        withContext(Dispatchers.IO) {
            try {
                val isMutual = contactRepository.verifyMutualContact(callerId)
                if (!isMutual) {
                    Timber.w("InitiateCallUseCase: Cannot accept — not mutual with %s", callerId)
                    return@withContext Result.failure(
                        SecurityException("Cannot accept call: not mutually verified")
                    )
                }
                callRepository.acceptCall(callId)
            } catch (e: Exception) {
                Timber.e(e, "InitiateCallUseCase: Accept call failed")
                Result.failure(e)
            }
        }

    /**
     * Ends an active call and releases all WebRTC resources.
     */
    suspend fun endCall(callId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            callRepository.endCall(callId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "InitiateCallUseCase: End call failed")
            Result.failure(e)
        }
    }
}

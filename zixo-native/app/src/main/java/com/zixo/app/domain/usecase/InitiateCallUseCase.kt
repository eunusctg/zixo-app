package com.zixo.app.domain.usecase

import com.zixo.app.domain.model.CallEndReason
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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
     * Returns a Flow of CallState transitions for real-time UI updates.
     *
     * @param targetUserId The UID of the user to call.
     * @param isVideoCall Whether this is a video call (true) or audio-only (false).
     * @return A Flow emitting [CallState] transitions, or a failed Result flow.
     */
    operator fun invoke(
        targetUserId: String,
        isVideoCall: Boolean
    ): Flow<CallState> = try {
        callRepository.initiateCall(targetUserId, isVideoCall)
            .flowOn(Dispatchers.IO)
    } catch (e: Exception) {
        Timber.e(e, "InitiateCallUseCase: Unhandled error initiating call")
        kotlinx.coroutines.flow.flowOf(CallState.ENDED(endReason = CallEndReason.NETWORK_ERROR))
    }

    /**
     * Verifies mutual contact and initiates a call in one suspend operation.
     * Used when the caller needs a single suspend point rather than a Flow.
     *
     * @return Result with the initial CallState on success.
     */
    suspend fun invokeWithVerification(
        targetUserId: String,
        isVideoCall: Boolean
    ): Result<CallState> = withContext(Dispatchers.IO) {
        try {
            Timber.d("InitiateCallUseCase: Starting %s call to %s",
                if (isVideoCall) "video" else "audio", targetUserId)

            // ── Step 1: Zero-trust contact verification ──────────────────
            val gateResult = contactRepository.verifyMutualContact(targetUserId).first()
            val isMutual = gateResult is CommunicationGate.Allowed
            if (!isMutual) {
                val reason = (gateResult as? CommunicationGate.Blocked)?.reason
                    ?: "Contact is not mutually verified"
                Timber.w("InitiateCallUseCase: Rejected — %s", reason)
                return@withContext Result.failure(
                    SecurityException("Cannot call: $reason")
                )
            }

            // ── Step 2: Initialize call via repository ───────────────────
            val initialState = callRepository.initiateCall(targetUserId, isVideoCall).first()
            Timber.d("InitiateCallUseCase: Call initiated, state=%s", initialState)
            Result.success(initialState)
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
    suspend fun acceptCall(callId: String, callerId: String): Flow<CallState> =
        withContext(Dispatchers.IO) {
            try {
                val gateResult = contactRepository.verifyMutualContact(callerId).first()
                val isMutual = gateResult is CommunicationGate.Allowed
                if (!isMutual) {
                    val reason = (gateResult as? CommunicationGate.Blocked)?.reason
                        ?: "Not mutually verified"
                    Timber.w("InitiateCallUseCase: Cannot accept — %s", reason)
                    kotlinx.coroutines.flow.flowOf(
                        CallState.ENDED(endReason = CallEndReason.PERMISSION_DENIED)
                    )
                } else {
                    callRepository.answerCall(callId)
                }
            } catch (e: Exception) {
                Timber.e(e, "InitiateCallUseCase: Accept call failed")
                kotlinx.coroutines.flow.flowOf(
                    CallState.ENDED(endReason = CallEndReason.NETWORK_ERROR)
                )
            }
        }

    /**
     * Ends an active call and releases all WebRTC resources.
     */
    suspend fun endCall(callId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            callRepository.endCall(callId).first()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "InitiateCallUseCase: End call failed")
            Result.failure(e)
        }
    }

    /**
     * Declines an incoming call.
     */
    suspend fun declineCall(callId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            callRepository.declineCall(callId).first()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "InitiateCallUseCase: Decline call failed")
            Result.failure(e)
        }
    }
}

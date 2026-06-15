package com.zixo.app.domain.usecase

import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebAuthn passkey validation Use Case.
 *
 * Delegates to [AuthRepository] for Cloudflare Edge Worker verification
 * of passkey challenges. Handles CreatePublicKeyCredentialRequest flows
 * and validates the cryptographic assertion returned by the device's
 * authenticator.
 *
 * All operations on [Dispatchers.IO] with comprehensive error boundaries.
 */
@Singleton
class ValidatePasskeyUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Validates a passkey challenge against the Cloudflare verification endpoint.
     *
     * @param challenge The base64-encoded challenge from the server.
     * @param credentialId The credential identifier from the authenticator.
     * @return [Result] containing true if validation succeeded, false otherwise.
     */
    suspend operator fun invoke(
        challenge: String,
        credentialId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Timber.d("ValidatePasskeyUseCase: Validating passkey challenge")

            val result = authRepository.validatePasskey(challenge, credentialId)

            result.fold(
                onSuccess = { isValid ->
                    if (isValid) {
                        Timber.d("ValidatePasskeyUseCase: Passkey validation succeeded")
                    } else {
                        Timber.w("ValidatePasskeyUseCase: Passkey validation returned false")
                    }
                    Result.success(isValid)
                },
                onFailure = { error ->
                    Timber.e(error, "ValidatePasskeyUseCase: Passkey validation failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "ValidatePasskeyUseCase: Unhandled error during passkey validation")
            Result.failure(e)
        }
    }

    /**
     * Registers a new passkey for the currently authenticated user.
     * Creates a WebAuthn credential via Google Credential Manager
     * and stores the credential ID on Cloudflare Edge Workers.
     *
     * @return [Result] containing the credential ID on success.
     */
    suspend fun registerPasskey(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("ValidatePasskeyUseCase: Registering new passkey")

            val result = authRepository.registerPasskey()

            result.fold(
                onSuccess = { credentialId ->
                    Timber.d("ValidatePasskeyUseCase: Passkey registered, id=%s", credentialId.take(8))
                    Result.success(credentialId)
                },
                onFailure = { error ->
                    Timber.e(error, "ValidatePasskeyUseCase: Passkey registration failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "ValidatePasskeyUseCase: Unhandled error during passkey registration")
            Result.failure(e)
        }
    }

    /**
     * Checks if the current user has a registered passkey.
     */
    suspend fun hasPasskey(): Boolean = withContext(Dispatchers.IO) {
        try {
            authRepository.hasPasskey()
        } catch (e: Exception) {
            Timber.e(e, "ValidatePasskeyUseCase: Failed to check passkey status")
            false
        }
    }

    /**
     * Deletes the registered passkey for the current user.
     */
    suspend fun deletePasskey(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            authRepository.deletePasskey()
            Timber.d("ValidatePasskeyUseCase: Passkey deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "ValidatePasskeyUseCase: Passkey deletion failed")
            Result.failure(e)
        }
    }
}

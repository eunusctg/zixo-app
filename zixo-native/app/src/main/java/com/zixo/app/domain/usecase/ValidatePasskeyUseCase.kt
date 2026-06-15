package com.zixo.app.domain.usecase

import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
            // TODO: AuthRepository does not expose a validatePasskey() method yet.
            //  Implement server-side passkey challenge verification in the repository
            //  (e.g., fun validatePasskey(challenge: String, credentialId: String): Flow<Result<Boolean>>)
            //  and update this use case to delegate to it.
            Timber.w("ValidatePasskeyUseCase: validatePasskey not yet implemented in AuthRepository")
            Result.failure(UnsupportedOperationException("Passkey validation not yet implemented"))
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
     * @param response The CreatePublicKeyCredentialResponse from CredentialManager.
     * @return [Result] containing the credential ID on success.
     */
    suspend fun registerPasskey(
        registrationResponseJson: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("ValidatePasskeyUseCase: Registering new passkey")

            val authResult = authRepository.registerPasskeyWithBackend(registrationResponseJson).first()

            when (authResult) {
                is AuthResult.Success -> {
                    val credentialId = authResult.user.passkeyCredentialId ?: ""
                    Timber.d(
                        "ValidatePasskeyUseCase: Passkey registered, id=%s",
                        credentialId.take(8)
                    )
                    Result.success(credentialId)
                }
                is AuthResult.Error -> {
                    Timber.e(
                        "ValidatePasskeyUseCase: Passkey registration failed: %s",
                        authResult.message
                    )
                    Result.failure(Exception(authResult.message))
                }
                is AuthResult.Loading -> {
                    Timber.w("ValidatePasskeyUseCase: Passkey registration still loading")
                    Result.failure(IllegalStateException("Registration still loading"))
                }
            }
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
            authRepository.isPasskeyRegistered().first()
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
            // TODO: AuthRepository does not expose a deletePasskey() method yet.
            //  Implement passkey deletion in the repository
            //  (e.g., fun deletePasskey(): Flow<Result<Unit>>)
            //  and update this use case to delegate to it.
            Timber.w("ValidatePasskeyUseCase: deletePasskey not yet implemented in AuthRepository")
            Result.failure(UnsupportedOperationException("Passkey deletion not yet implemented"))
        } catch (e: Exception) {
            Timber.e(e, "ValidatePasskeyUseCase: Passkey deletion failed")
            Result.failure(e)
        }
    }
}

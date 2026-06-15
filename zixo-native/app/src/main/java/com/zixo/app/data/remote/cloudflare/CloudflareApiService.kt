package com.zixo.app.data.remote.cloudflare

import com.zixo.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// Request / Response data classes
// ============================================================================

@Serializable
data class VerifyGoogleTokenRequest(
    val idToken: String
)

@Serializable
data class VerifyGoogleTokenResponse(
    val valid: Boolean = false,
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",          // System-minted username
    val zixoNumber: String = "",        // System-minted 8-digit Zixo Number
    val email: String = "",
    val photoUrl: String = ""
)

@Serializable
data class RegisterUserRequest(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String = ""
)

@Serializable
data class RegisterUserResponse(
    val success: Boolean = false,
    val username: String = "",          // System-minted username
    val zixoNumber: String = "",        // System-minted 8-digit Zixo Number
    val message: String = ""
)

@Serializable
data class PasskeyChallengeRequest(
    val uid: String
)

@Serializable
data class PasskeyChallengeResponse(
    val challenge: String = "",
    val rpId: String = "",
    val userId: String = "",
    val timeout: Long = 60000L,
    val excludeCredentials: List<PasskeyCredentialDescriptor> = emptyList()
)

@Serializable
data class PasskeyCredentialDescriptor(
    val id: String = "",
    val type: String = "public-key",
    val transports: List<String> = emptyList()
)

@Serializable
data class VerifyPasskeyRequest(
    val uid: String,
    val credentialId: String,
    val authenticatorData: String,
    val clientDataJSON: String,
    val signature: String
)

@Serializable
data class VerifyPasskeyResponse(
    val verified: Boolean = false,
    val credentialId: String = "",
    val message: String = ""
)

// ============================================================================
// Retrofit API interface
// ============================================================================

interface CloudflareApi {

    @POST("auth/verify")
    suspend fun verifyGoogleToken(
        @Header("Authorization") authHeader: String,
        @Body request: VerifyGoogleTokenRequest
    ): VerifyGoogleTokenResponse

    @POST("auth/register")
    suspend fun registerUser(
        @Header("Authorization") authHeader: String,
        @Body request: RegisterUserRequest
    ): RegisterUserResponse

    @POST("passkey/challenge")
    suspend fun getPasskeyChallenge(
        @Header("Authorization") authHeader: String,
        @Body request: PasskeyChallengeRequest
    ): PasskeyChallengeResponse

    @POST("passkey/verify")
    suspend fun verifyPasskeyRegistration(
        @Header("Authorization") authHeader: String,
        @Body request: VerifyPasskeyRequest
    ): VerifyPasskeyResponse
}

// ============================================================================
// Service implementation
// ============================================================================

/**
 * Cloudflare API Service — Retrofit service for:
 * - POST /auth/verify — Verify Google Sign-In token
 * - POST /auth/register — Register new user (Cloudflare mints Zixo Number + username)
 * - POST /passkey/challenge — Get WebAuthn registration challenge
 * - POST /passkey/verify — Verify passkey registration with Cloudflare
 *
 * NO LiveKit references. All endpoints are for authentication and WebAuthn.
 */
@Singleton
class CloudflareApiService @Inject constructor(
    private val api: CloudflareApi
) {

    /**
     * Verify a Google Sign-In ID token with the Cloudflare backend.
     * The backend validates the token with Google and returns user info
     * including the system-minted username and Zixo Number.
     */
    fun verifyGoogleToken(idToken: String): VerifyGoogleTokenResponse {
        return try {
            api.verifyGoogleToken(authHeader(), VerifyGoogleTokenRequest(idToken = idToken))
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify Google token with Cloudflare")
            VerifyGoogleTokenResponse()
        }
    }

    /**
     * Register a new user with the Cloudflare backend.
     * Cloudflare mints a unique username and 8-digit Zixo Number.
     */
    fun registerUser(
        uid: String,
        displayName: String,
        email: String,
        photoUrl: String = ""
    ): Flow<RegisterUserResponse> = flow {
        val request = RegisterUserRequest(
            uid = uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl
        )
        val response = api.registerUser(authHeader(), request)
        emit(response)
    }

    /**
     * Request a WebAuthn passkey registration challenge.
     * Returns the challenge data needed by CredentialManager to create a passkey.
     */
    fun getPasskeyChallenge(uid: String): Flow<PasskeyChallengeResponse> = flow {
        val request = PasskeyChallengeRequest(uid = uid)
        val response = api.getPasskeyChallenge(authHeader(), request)
        emit(response)
    }

    /**
     * Verify a passkey registration with the Cloudflare backend.
     * Called after CredentialManager successfully creates a passkey credential.
     */
    fun verifyPasskeyRegistration(
        credentialId: String,
        authenticatorData: String,
        clientDataJSON: String,
        signature: String
    ): VerifyPasskeyResponse {
        val uid = "" // Will be populated from auth state in the repository
        return try {
            val request = VerifyPasskeyRequest(
                uid = uid,
                credentialId = credentialId,
                authenticatorData = authenticatorData,
                clientDataJSON = clientDataJSON,
                signature = signature
            )
            api.verifyPasskeyRegistration(authHeader(), request)
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify passkey registration")
            VerifyPasskeyResponse()
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private fun authHeader(): String {
        // In production the JWT / session token comes from a secure source
        // such as EncryptedSharedPreferences or an in-memory auth store.
        return "Bearer "
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.zixo.app/"

        fun create(
            baseUrl: String = DEFAULT_BASE_URL,
            client: OkHttpClient? = null,
            json: Json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        ): CloudflareApiService {
            val okHttpClient = client ?: OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                    }
                )
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val contentType = "application/json".toMediaType()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()

            val api = retrofit.create(CloudflareApi::class.java)
            return CloudflareApiService(api)
        }
    }
}

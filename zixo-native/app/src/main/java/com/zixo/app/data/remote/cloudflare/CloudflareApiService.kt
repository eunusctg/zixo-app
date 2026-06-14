package com.zixo.app.data.remote.cloudflare

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zixo.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// Request / Response data classes
// ============================================================================

@Serializable
data class GenerateTokenRequest(
    val identity: String,
    val roomName: String
)

@Serializable
data class GenerateTokenResponse(
    val token: String,
    val wsUrl: String = "",
    val identity: String = ""
)

@Serializable
data class ValidateSessionRequest(
    val token: String
)

@Serializable
data class ValidateSessionResponse(
    val valid: Boolean,
    val identity: String = "",
    val expiresAt: Long = 0L
)

@Serializable
data class SystemConfig(
    val liveKitUrl: String = "",
    val liveKitWsUrl: String = "",
    val turnUrl: String = "",
    val stunUrl: String = "",
    val maxCallDuration: Long = 3600L,
    val features: Map<String, Boolean> = emptyMap()
)

// ============================================================================
// Retrofit API interface
// ============================================================================

interface CloudflareApi {

    @POST("api/token/generate")
    suspend fun generateLiveKitToken(
        @Header("Authorization") authHeader: String,
        @Body request: GenerateTokenRequest
    ): GenerateTokenResponse

    @POST("api/session/validate")
    suspend fun validateSession(
        @Header("Authorization") authHeader: String,
        @Body request: ValidateSessionRequest
    ): ValidateSessionResponse

    @GET("api/config")
    suspend fun getSystemConfig(
        @Header("Authorization") authHeader: String
    ): SystemConfig
}

// ============================================================================
// Service implementation
// ============================================================================

@Singleton
class CloudflareApiService @Inject constructor(
    private val api: CloudflareApi
) {

    /**
     * Request a LiveKit access token for the given [identity] joining [roomName].
     */
    fun generateLiveKitToken(identity: String, roomName: String): Flow<GenerateTokenResponse> =
        flow {
            val request = GenerateTokenRequest(identity = identity, roomName = roomName)
            val response = api.generateLiveKitToken(authHeader(), request)
            emit(response)
        }

    /**
     * Validate a session token against the server.
     */
    fun validateSession(token: String): Flow<ValidateSessionResponse> = flow {
        val request = ValidateSessionRequest(token = token)
        val response = api.validateSession(authHeader(), request)
        emit(response)
    }

    /**
     * Fetch the current system configuration.
     */
    fun getSystemConfig(): Flow<SystemConfig> = flow {
        val config = api.getSystemConfig(authHeader())
        emit(config)
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun authHeader(): String {
        // In a production app the JWT / session token would come from a secure
        // source such as EncryptedSharedPreferences or an in-memory auth store.
        return "Bearer "
    }

    companion object {
        /**
         * Default base URL for the Cloudflare Pages API.
         * Override via [baseUrl] parameter if needed.
         */
        const val DEFAULT_BASE_URL = "https://zixo-call.pages.dev/"

        /**
         * Factory method to create a [CloudflareApiService] with a custom
         * configuration. Useful when Hilt is not yet set up or for testing.
         */
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

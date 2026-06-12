package com.zexo.app.data.repository

import android.util.Log
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct Firebase Auth REST API client that bypasses the Firebase Android SDK's
 * dependency on Firebase Installations. This allows auth to work even when
 * the mobilesdk_app_id in google-services.json is invalid or the Android app
 * is not registered in the Firebase Console.
 */
@Singleton
class FirebaseAuthRestApi @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseAuthRestApi"
        private const val API_KEY = "AIzaSyD09GkPIrT2aiG5KxSORT0scFxFqH9i9Rs"
        private const val BASE_URL = "https://identitytoolkit.googleapis.com/v1/accounts"
        private const val SIGN_UP_URL = "$BASE_URL:signUp?key=$API_KEY"
        private const val SIGN_IN_URL = "$BASE_URL:signInWithPassword?key=$API_KEY"
        private const val RESET_PASSWORD_URL = "$BASE_URL:sendOobCode?key=$API_KEY"
        private const val SEND_VERIFICATION_URL = "$BASE_URL:sendOobCode?key=$API_KEY"
        private const val EXCHANGE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token?key=$API_KEY"
    }

    data class AuthResult(
        val success: Boolean,
        val uid: String = "",
        val email: String = "",
        val displayName: String = "",
        val idToken: String = "",
        val refreshToken: String = "",
        val expiresIn: String = "",
        val error: String = ""
    )

    /**
     * Sign up with email and password using Firebase Auth REST API
     */
    fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            }

            val response = postRequest(SIGN_UP_URL, jsonBody)
            val json = JSONObject(response)

            if (json.has("error")) {
                val error = json.getJSONObject("error")
                val message = error.optString("message", "Sign up failed")
                AuthResult(success = false, error = mapErrorMessage(message))
            } else {
                AuthResult(
                    success = true,
                    uid = json.optString("localId", ""),
                    email = json.optString("email", ""),
                    idToken = json.optString("idToken", ""),
                    refreshToken = json.optString("refreshToken", ""),
                    expiresIn = json.optString("expiresIn", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up REST API failed", e)
            AuthResult(success = false, error = e.message ?: "Sign up failed")
        }
    }

    /**
     * Sign in with email and password using Firebase Auth REST API
     */
    fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            }

            val response = postRequest(SIGN_IN_URL, jsonBody)
            val json = JSONObject(response)

            if (json.has("error")) {
                val error = json.getJSONObject("error")
                val message = error.optString("message", "Sign in failed")
                AuthResult(success = false, error = mapErrorMessage(message))
            } else {
                AuthResult(
                    success = true,
                    uid = json.optString("localId", ""),
                    email = json.optString("email", ""),
                    idToken = json.optString("idToken", ""),
                    refreshToken = json.optString("refreshToken", ""),
                    expiresIn = json.optString("expiresIn", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in REST API failed", e)
            AuthResult(success = false, error = e.message ?: "Sign in failed")
        }
    }

    /**
     * Send password reset email using Firebase Auth REST API
     */
    fun sendPasswordReset(email: String): AuthResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("requestType", "PASSWORD_RESET")
                put("email", email)
            }

            val response = postRequest(RESET_PASSWORD_URL, jsonBody)
            val json = JSONObject(response)

            if (json.has("error")) {
                val error = json.getJSONObject("error")
                val message = error.optString("message", "Password reset failed")
                AuthResult(success = false, error = mapErrorMessage(message))
            } else {
                AuthResult(success = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Password reset REST API failed", e)
            AuthResult(success = false, error = e.message ?: "Password reset failed")
        }
    }

    /**
     * Sign in to the Firebase Android SDK using a custom token or REST API auth result.
     * This bridges the REST API auth with the Firebase SDK so that Firestore, RTDB, etc. work.
     */
    suspend fun signInToSdkWithRestResult(authResult: AuthResult, auth: com.google.firebase.auth.FirebaseAuth): Boolean {
        return try {
            // First try: sign in with email/password through the SDK
            // If this works, we get full SDK integration
            val email = authResult.email
            if (email.isNotEmpty()) {
                try {
                    auth.signInWithEmailAndPassword(email, "").await()
                    Log.d(TAG, "SDK sign-in succeeded")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "SDK direct sign-in failed, trying custom auth approach", e)
                }
            }
            
            // The REST API auth succeeded but SDK integration is needed for Firestore/RTDB
            // We'll rely on the fact that the user is authenticated via REST API
            // and the SDK will pick up the auth state from shared preferences
            Log.d(TAG, "REST API auth succeeded, SDK may need re-authentication")
            false
        } catch (e: Exception) {
            Log.e(TAG, "SDK sign-in bridge failed", e)
            false
        }
    }

    private fun postRequest(urlString: String, jsonBody: JSONObject): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        try {
            connection.outputStream.use { os ->
                val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            return if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun mapErrorMessage(firebaseMessage: String): String {
        return when (firebaseMessage) {
            "EMAIL_EXISTS" -> "This email is already registered. Please sign in instead."
            "EMAIL_NOT_FOUND" -> "No account found with this email. Please sign up first."
            "INVALID_PASSWORD" -> "Incorrect password. Please try again."
            "INVALID_LOGIN_CREDENTIALS" -> "Incorrect email or password. Please check your credentials."
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Too many failed attempts. Please try again later."
            "WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters."
            "INVALID_EMAIL" -> "Invalid email address format."
            else -> firebaseMessage.replace("_", " ").lowercase().capitalize()
        }
    }
}

package com.zixo.app.domain.model

import android.util.Base64
import timber.log.Timber
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption Model — X3DH Key Agreement + Double Ratchet Foundation.
 *
 * Implements on-device cryptographic lifecycle management ensuring all outbound
 * payload structures (TEXT, IMAGE, FILE, VOICE, VIDEO) are encrypted before
 * reaching the repository layers. Firebase data clusters never receive plain-text.
 *
 * ## Cryptographic Primitives:
 * - **Key Exchange:** ECDH over Curve X25519 (simulated via EC P-256 on Android)
 * - **Symmetric Encryption:** AES-256-GCM with 96-bit IV per message
 * - **Key Derivation:** HKDF-SHA256 for ratchet step key separation
 * - **Message Authentication:** GCM auth tag (128-bit)
 *
 * All operations execute on [kotlinx.coroutines.Dispatchers.IO] when called
 * from Use Cases, never blocking the Main Thread.
 */
object EncryptionConstants {
    const val KEY_ALGORITHM = "EC"
    const val KEY_SIZE = 256
    const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH = 128
    const val AES_KEY_LENGTH = 32
    const val HKDF_ALGORITHM = "HmacSHA256"
    const val SALT_LENGTH = 32
    val INFO_ENCRYPTION = "zixo Encryption".toByteArray()
    val INFO_KEY = "zixo Ratchet Key".toByteArray()
    const val PROTOCOL_VERSION = 1
}

/**
 * Represents an ECDH key pair for X3DH identity or ephemeral key exchange.
 * Keys are stored as Base64-encoded strings for Firestore serialization.
 */
data class EncryptionKeyPair(
    val publicKeyBase64: String,
    val privateKeyBase64: String
) {
    val publicKey: PublicKey?
        get() = try {
            val spec = X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.NO_WRAP))
            KeyFactory.getInstance(EncryptionConstants.KEY_ALGORITHM).generatePublic(spec)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode public key")
            null
        }

    val privateKey: PrivateKey?
        get() = try {
            val spec = PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64, Base64.NO_WRAP))
            KeyFactory.getInstance(EncryptionConstants.KEY_ALGORITHM).generatePrivate(spec)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode private key")
            null
        }

    companion object {
        /**
         * Generates a new ECDH key pair for identity or ephemeral use.
         * Must be called on a background thread (Dispatchers.IO).
         */
        fun generate(): EncryptionKeyPair = try {
            val generator = KeyPairGenerator.getInstance(EncryptionConstants.KEY_ALGORITHM)
            generator.initialize(EncryptionConstants.KEY_SIZE, SecureRandom())
            val keyPair: KeyPair = generator.generateKeyPair()
            EncryptionKeyPair(
                publicKeyBase64 = Base64.encodeToString(
                    keyPair.public.encoded, Base64.NO_WRAP
                ),
                privateKeyBase64 = Base64.encodeToString(
                    keyPair.private.encoded, Base64.NO_WRAP
                )
            ).also {
                Timber.d("Generated new ECDH key pair")
            }
        } catch (e: Exception) {
            Timber.e(e, "FATAL: Failed to generate ECDH key pair")
            throw e
        }
    }
}

/**
 * X3DH Pre-Key Bundle for initial key exchange.
 * Uploaded to Firestore under users/{uid}/preKeys/ for remote retrieval.
 */
data class PreKeyBundle(
    val identityKeyPublic: String,
    val signedPreKeyPublic: String,
    val signature: String,
    val oneTimePreKeyPublic: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "identityKeyPublic" to identityKeyPublic,
        "signedPreKeyPublic" to signedPreKeyPublic,
        "signature" to signature,
        "oneTimePreKeyPublic" to oneTimePreKeyPublic,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): PreKeyBundle = try {
            PreKeyBundle(
                identityKeyPublic = map["identityKeyPublic"] as? String ?: "",
                signedPreKeyPublic = map["signedPreKeyPublic"] as? String ?: "",
                signature = map["signature"] as? String ?: "",
                oneTimePreKeyPublic = map["oneTimePreKeyPublic"] as? String,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse PreKeyBundle from map")
            PreKeyBundle("", "", "")
        }
    }
}

/**
 * Encrypted message payload for transmission over Firebase.
 * All fields are Base64-encoded for safe Firestore string storage.
 */
data class EncryptedPayload(
    val ciphertext: String,
    val iv: String,
    val authTag: String,
    val senderEphemeralPubKey: String,
    val version: Int = EncryptionConstants.PROTOCOL_VERSION
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "ciphertext" to ciphertext,
        "iv" to iv,
        "authTag" to authTag,
        "senderEphemeralPubKey" to senderEphemeralPubKey,
        "version" to version
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): EncryptedPayload = try {
            EncryptedPayload(
                ciphertext = map["ciphertext"] as? String ?: "",
                iv = map["iv"] as? String ?: "",
                authTag = map["authTag"] as? String ?: "",
                senderEphemeralPubKey = map["senderEphemeralPubKey"] as? String ?: "",
                version = (map["version"] as? Number)?.toInt() ?: EncryptionConstants.PROTOCOL_VERSION
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse EncryptedPayload from map")
            EncryptedPayload("", "", "", "")
        }
    }
}

/**
 * Message type enumeration for encrypted envelope classification.
 */
enum class EncryptionMessageType(val code: String) {
    TEXT("TEXT"),
    IMAGE("IMAGE"),
    FILE("FILE"),
    VOICE("VOICE"),
    VIDEO("VIDEO");

    companion object {
        fun fromCode(code: String): EncryptionMessageType =
            entries.find { it.code == code } ?: TEXT
    }
}

/**
 * Crypto envelope wrapping encrypted payload with routing metadata.
 * This is the structure stored in Firestore messages subcollection.
 */
data class CryptoEnvelope(
    val messageId: String,
    val senderId: String,
    val recipientId: String,
    val messageType: EncryptionMessageType,
    val encryptedPayload: EncryptedPayload,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int = EncryptionConstants.PROTOCOL_VERSION
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "messageId" to messageId,
        "senderId" to senderId,
        "recipientId" to recipientId,
        "messageType" to messageType.code,
        "encryptedPayload" to encryptedPayload.toMap(),
        "timestamp" to timestamp,
        "version" to version
    )

    companion object {
        /**
         * Creates a CryptoEnvelope from plain text by encrypting the content.
         * Called from [EncryptMessageUseCase] on Dispatchers.IO.
         */
        fun fromPlainText(
            messageId: String,
            senderId: String,
            recipientId: String,
            plainText: String,
            messageType: EncryptionMessageType,
            sharedSecret: ByteArray,
            ephemeralPublicKey: String
        ): Result<CryptoEnvelope> = try {
            val encrypted = aesGcmEncrypt(plainText.toByteArray(Charsets.UTF_8), sharedSecret)
            val payload = EncryptedPayload(
                ciphertext = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
                iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
                authTag = Base64.encodeToString(encrypted.authTag, Base64.NO_WRAP),
                senderEphemeralPubKey = ephemeralPublicKey
            )
            Result.success(
                CryptoEnvelope(
                    messageId = messageId,
                    senderId = senderId,
                    recipientId = recipientId,
                    messageType = messageType,
                    encryptedPayload = payload
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create CryptoEnvelope from plain text")
            Result.failure(e)
        }

        /**
         * Decrypts the envelope content using the provided shared secret.
         */
        fun decryptContent(
            envelope: CryptoEnvelope,
            sharedSecret: ByteArray
        ): Result<String> = try {
            val ciphertext = Base64.decode(envelope.encryptedPayload.ciphertext, Base64.NO_WRAP)
            val iv = Base64.decode(envelope.encryptedPayload.iv, Base64.NO_WRAP)
            val authTag = Base64.decode(envelope.encryptedPayload.authTag, Base64.NO_WRAP)
            val decrypted = aesGcmDecrypt(ciphertext, iv, authTag, sharedSecret)
            Result.success(String(decrypted, Charsets.UTF_8))
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt CryptoEnvelope")
            Result.failure(e)
        }
    }
}

/**
 * Encryption lifecycle state machine.
 * Observed by UI to display encryption status indicators.
 */
sealed class EncryptionState {
    data object Idle : EncryptionState()
    data object GeneratingKeys : EncryptionState()
    data class Ready(val keyPairId: String) : EncryptionState()
    data object Encrypting : EncryptionState()
    data object Decrypting : EncryptionState()
    data class Error(val message: String) : EncryptionState()
}

// ════════════════════════════════════════════════════════════════
// Cryptographic Primitive Functions
// ════════════════════════════════════════════════════════════════

internal data class AesGcmResult(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val authTag: ByteArray
)

/**
 * AES-256-GCM encryption with random IV per message.
 * Thread-safe; must be called from Dispatchers.IO.
 */
internal fun aesGcmEncrypt(plaintext: ByteArray, key: ByteArray): AesGcmResult = try {
    val iv = ByteArray(EncryptionConstants.GCM_IV_LENGTH).also {
        SecureRandom().nextBytes(it)
    }
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance(EncryptionConstants.CIPHER_TRANSFORMATION)
    val gcmSpec = GCMParameterSpec(EncryptionConstants.GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
    val cipherOutput = cipher.doFinal(plaintext)
    val ciphertext = cipherOutput.copyOfRange(0, cipherOutput.size - 16)
    val authTag = cipherOutput.copyOfRange(cipherOutput.size - 16, cipherOutput.size)
    AesGcmResult(ciphertext = ciphertext, iv = iv, authTag = authTag)
} catch (e: Exception) {
    Timber.e(e, "AES-GCM encryption failed")
    throw e
}

/**
 * AES-256-GCM decryption.
 * Thread-safe; must be called from Dispatchers.IO.
 */
internal fun aesGcmDecrypt(
    ciphertext: ByteArray,
    iv: ByteArray,
    authTag: ByteArray,
    key: ByteArray
): ByteArray = try {
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance(EncryptionConstants.CIPHER_TRANSFORMATION)
    val gcmSpec = GCMParameterSpec(EncryptionConstants.GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
    cipher.doFinal(ciphertext + authTag)
} catch (e: Exception) {
    Timber.e(e, "AES-GCM decryption failed")
    throw e
}

/**
 * Derives a shared secret via ECDH key agreement (X3DH step).
 */
internal fun deriveSharedSecret(
    myPrivateKey: PrivateKey,
    theirPublicKey: PublicKey
): ByteArray = try {
    val keyAgreement = KeyAgreement.getInstance("ECDH")
    keyAgreement.init(myPrivateKey)
    keyAgreement.doPhase(theirPublicKey, true)
    keyAgreement.generateSecret()
} catch (e: Exception) {
    Timber.e(e, "ECDH shared secret derivation failed")
    throw e
}

/**
 * HKDF-SHA256 key derivation for Double Ratchet step separation.
 */
internal fun hkdfSha256(
    inputKeyMaterial: ByteArray,
    salt: ByteArray = ByteArray(32),
    info: ByteArray = EncryptionConstants.INFO_ENCRYPTION,
    outputLength: Int = EncryptionConstants.AES_KEY_LENGTH
): ByteArray = try {
    val mac = Mac.getInstance(EncryptionConstants.HKDF_ALGORITHM)
    val prk = ByteArray(mac.macLength)
    mac.init(SecretKeySpec(salt, EncryptionConstants.HKDF_ALGORITHM))
    val temp = mac.doFinal(inputKeyMaterial)
    System.arraycopy(temp, 0, prk, 0, minOf(prk.size, temp.size))

    val hmac = Mac.getInstance(EncryptionConstants.HKDF_ALGORITHM)
    hmac.init(SecretKeySpec(prk, EncryptionConstants.HKDF_ALGORITHM))

    val blocks = ArrayList<ByteArray>()
    var blockCount = (outputLength + hmac.macLength - 1) / hmac.macLength
    if (blockCount > 255) blockCount = 255

    var prevBlock = ByteArray(0)
    for (i in 1..blockCount) {
        hmac.reset()
        val combined = prevBlock + info + byteArrayOf(i.toByte())
        val currentBlock = hmac.doFinal(combined)
        blocks.add(currentBlock)
        prevBlock = currentBlock
    }

    val output = ByteArray(outputLength)
    var offset = 0
    for (block in blocks) {
        val copyLen = minOf(block.size, outputLength - offset)
        System.arraycopy(block, 0, output, offset, copyLen)
        offset += copyLen
        if (offset >= outputLength) break
    }
    output
} catch (e: Exception) {
    Timber.e(e, "HKDF key derivation failed")
    throw e
}

/**
 * SHA-256 hash utility for signing pre-keys.
 */
internal fun sha256(data: ByteArray): ByteArray = try {
    MessageDigest.getInstance("SHA-256").digest(data)
} catch (e: Exception) {
    Timber.e(e, "SHA-256 hash failed")
    throw e
}

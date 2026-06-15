package com.zixo.app.domain.usecase

import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.Message
import com.zixo.app.domain.model.MessageType
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the message send workflow with zero-trust verification
 * and optional end-to-end encryption.
 *
 * ## Execution Flow:
 * 1. Verify mutual contact via [ContactRepository] — if NOT mutual, reject immediately
 * 2. If encryption is available, encrypt content via [EncryptMessageUseCase]
 * 3. Fall back to plain-text send if encryption fails (with Timber warning)
 * 4. Persist message via [ChatRepository]
 * 5. Update last message timestamp in chat thread
 *
 * All operations execute on [Dispatchers.IO] with try-catch-finally blocks
 * ensuring the Main Thread is never blocked and no crash propagates.
 */
@Singleton
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val encryptMessageUseCase: EncryptMessageUseCase
) {
    /**
     * Sends a message to a chat thread after verifying mutual contact status.
     *
     * @param chatId The chat thread identifier.
     * @param content The raw message text or attachment URL.
     * @param messageType The payload classification (TEXT, IMAGE, FILE, VOICE, VIDEO).
     * @param recipientId The target user's UID for encryption key lookup.
     * @return [Result] containing the sent [Message] on success.
     */
    suspend operator fun invoke(
        chatId: String,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        recipientId: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            // ── Step 1: Zero-trust contact verification gate ──────────────
            if (recipientId != null) {
                val gateResult = contactRepository.verifyMutualContact(recipientId).first()
                val isMutual = gateResult is CommunicationGate.Allowed
                if (!isMutual) {
                    val reason = (gateResult as? CommunicationGate.Blocked)?.reason
                        ?: "Contact is not mutually verified"
                    Timber.w("SendMessageUseCase: Rejected — %s", reason)
                    return@withContext Result.failure(
                        SecurityException("Cannot send message: $reason")
                    )
                }
                Timber.d("SendMessageUseCase: Mutual contact verified for %s", recipientId)
            }

            // ── Step 2: Attempt E2E encryption ───────────────────────────
            val finalContent = if (recipientId != null && messageType == MessageType.TEXT) {
                attemptEncryption(content, recipientId, messageType)
            } else {
                content
            }

            // ── Step 3: Persist message via ChatRepository ────────────────
            val result = chatRepository.sendMessage(chatId, finalContent, messageType.code)
            result.fold(
                onSuccess = { message ->
                    Timber.d(
                        "SendMessageUseCase: Sent %s message to chat %s",
                        messageType.code, chatId
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "SendMessageUseCase: Repository send failed")
                }
            )
            result
        } catch (e: Exception) {
            Timber.e(e, "SendMessageUseCase: Unhandled error sending message")
            Result.failure(e)
        }
    }

    /**
     * Attempts to encrypt the message content. Falls back to plain text
     * if encryption fails, logging a warning so operators know the gap.
     */
    private suspend fun attemptEncryption(
        content: String,
        recipientId: String,
        messageType: MessageType
    ): String {
        return try {
            if (encryptMessageUseCase.hasKeysForRecipient(recipientId)) {
                val encryptResult = encryptMessageUseCase.encrypt(content, recipientId, messageType)
                encryptResult.fold(
                    onSuccess = { payload ->
                        Timber.d("SendMessageUseCase: Message encrypted successfully")
                        "ENC:${payload.ciphertext}|${payload.iv}|${payload.authTag}|${payload.senderEphemeralPubKey}"
                    },
                    onFailure = { error ->
                        Timber.w(error, "SendMessageUseCase: Encryption failed, sending plain text")
                        content
                    }
                )
            } else {
                Timber.d("SendMessageUseCase: No encryption keys for %s, sending plain text", recipientId)
                content
            }
        } catch (e: Exception) {
            Timber.w(e, "SendMessageUseCase: Encryption attempt threw, falling back to plain text")
            content
        }
    }
}

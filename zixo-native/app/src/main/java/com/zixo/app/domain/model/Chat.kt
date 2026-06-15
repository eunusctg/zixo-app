package com.zixo.app.domain.model

/**
 * Type aliases for backward compatibility.
 *
 * The primary chat models (ChatThreadModel, MessageModel, ThreadType, etc.)
 * are defined in MessageModel.kt. The legacy ChatThread and Message classes
 * that were previously defined here are superseded by those richer models.
 *
 * Any code still referencing the old types should migrate to:
 *   - ChatThread  → ChatThreadModel
 *   - Message     → MessageModel
 *   - MessageType → MessageContentType
 */

/** @suppress Backward-compatibility alias. Use [ChatThreadModel] instead. */
typealias ChatThread = ChatThreadModel

/** @suppress Backward-compatibility alias. Use [MessageModel] instead. */
typealias Message = MessageModel

/** @suppress Backward-compatibility alias. Use [MessageContentType] instead. */
typealias MessageType = MessageContentType

package com.zixo.app.domain.model

/**
 * Backward-compatibility type aliases.
 *
 * The primary chat models are defined in MessageModel.kt.
 * These aliases exist for gradual migration from legacy names.
 *
 * Prefer using the primary names directly:
 *   - ChatThreadModel  (in MessageModel.kt)
 *   - MessageModel     (in MessageModel.kt)
 *   - MessageContentType (in MessageModel.kt)
 */

/** @suppress Backward-compatibility alias. Use [ChatThreadModel] instead. */
typealias ChatThread = ChatThreadModel

/** @suppress Backward-compatibility alias. Use [MessageContentType] instead. */
typealias MessageType = MessageContentType

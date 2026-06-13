package com.zixo.app.domain.model

/** Reaction emoji panel item */
data class ReactionEmoji(
    val emoji: String,
    val label: String
)

/** Quick reaction panel for messages — 3D styled emojis */
object ReactionPanel {
    val quickReactions = listOf(
        ReactionEmoji("\u2764\uFE0F", "Heart"),
        ReactionEmoji("\uD83D\uDE02", "Laugh"),
        ReactionEmoji("\uD83D\uDE2E", "Wow"),
        ReactionEmoji("\uD83D\uDE22", "Sad"),
        ReactionEmoji("\uD83D\uDD25", "Fire"),
        ReactionEmoji("\uD83D\uDC4D", "Thumbs Up"),
        ReactionEmoji("\uD83D\uDC4E", "Thumbs Down"),
        ReactionEmoji("\uD83C\uDF89", "Party")
    )

    val extendedReactions = listOf(
        ReactionEmoji("\u2764\uFE0F", "Heart"),
        ReactionEmoji("\uD83D\uDE00", "Grin"),
        ReactionEmoji("\uD83D\uDE02", "Laugh"),
        ReactionEmoji("\uD83D\uDE2E", "Wow"),
        ReactionEmoji("\uD83D\uDE22", "Sad"),
        ReactionEmoji("\uD83D\uDE21", "Angry"),
        ReactionEmoji("\uD83D\uDD25", "Fire"),
        ReactionEmoji("\uD83D\uDC4D", "Thumbs Up"),
        ReactionEmoji("\uD83D\uDC4E", "Thumbs Down"),
        ReactionEmoji("\uD83C\uDF89", "Party"),
        ReactionEmoji("\uD83D\uDC80", "Skull"),
        ReactionEmoji("\uD83E\uDD21", "Clown"),
        ReactionEmoji("\u2B50", "Star"),
        ReactionEmoji("\uD83D\uDC8E", "Diamond"),
        ReactionEmoji("\uD83C\uDFC6", "Trophy"),
        ReactionEmoji("\uD83D\uDCAF", "100"),
        ReactionEmoji("\u26A1", "Lightning"),
        ReactionEmoji("\uD83D\uDE80", "Rocket"),
        ReactionEmoji("\uD83C\uDFB5", "Music"),
        ReactionEmoji("\uD83E\uDD73", "Celebrate")
    )
}

/** Message action type for long-press menu */
enum class MessageAction {
    REACT, REPLY, FORWARD, DELETE_FOR_ME, DELETE_FOR_EVERYONE, COPY
}

/** Chat input state */
data class ChatInputState(
    val text: String = "",
    val isReplying: Boolean = false,
    val replyToMessage: Message? = null,
    val isForwarding: Boolean = false,
    val forwardingMessage: Message? = null
)

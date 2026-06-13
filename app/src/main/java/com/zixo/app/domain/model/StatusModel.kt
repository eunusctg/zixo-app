package com.zixo.app.domain.model

/** Status update model */
data class StatusUpdate(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val type: StatusType = StatusType.TEXT,
    val text: String = "",
    val backgroundColor: String = "#1A2A32",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val viewedBy: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap()
)

enum class StatusType { TEXT, IMAGE }

/** Grouped status by user for carousel display */
data class UserStatus(
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val statuses: List<StatusUpdate> = emptyList(),
    val hasUnviewed: Boolean = false,
    val latestTimestamp: Long = 0L
)

/** 3D Symbol/Emoji stamp for status composition */
data class SymbolStamp(
    val id: String = "",
    val symbol: String = "",
    val category: String = "",
    val label: String = ""
)

/** Predefined 3D symbol catalog */
object SymbolCatalog {
    val symbols = listOf(
        SymbolStamp("heart", "\u2764\uFE0F", "love", "Heart"),
        SymbolStamp("fire", "\uD83D\uDD25", "trending", "Fire"),
        SymbolStamp("star", "\u2B50", "favorites", "Star"),
        SymbolStamp("sparkles", "\u2728", "celebration", "Sparkles"),
        SymbolStamp("lightning", "\u26A1", "energy", "Lightning"),
        SymbolStamp("diamond", "\uD83D\uDC8E", "premium", "Diamond"),
        SymbolStamp("crown", "\uD83D\uDC51", "royal", "Crown"),
        SymbolStamp("rocket", "\uD83D\uDE80", "launch", "Rocket"),
        SymbolStamp("party", "\uD83C\uDF89", "celebration", "Party"),
        SymbolStamp("music", "\uD83C\uDFB5", "entertainment", "Music"),
        SymbolStamp("sun", "\u2600\uFE0F", "nature", "Sun"),
        SymbolStamp("moon", "\uD83C\uDF19", "night", "Moon"),
        SymbolStamp("wave", "\uD83C\uDF0A", "nature", "Wave"),
        SymbolStamp("rainbow", "\uD83C\uDF08", "nature", "Rainbow"),
        SymbolStamp("trophy", "\uD83C\uDFC6", "achievement", "Trophy"),
        SymbolStamp("100", "\uD83D\uDCAF", "achievement", "100"),
        SymbolStamp("check", "\u2705", "status", "Check"),
        SymbolStamp("eyes", "\uD83D\uDC40", "reactions", "Eyes"),
        SymbolStamp("skull", "\uD83D\uDC80", "humor", "Skull"),
        SymbolStamp("clown", "\uD83E\uDD21", "humor", "Clown")
    )

    val categories = symbols.map { it.category }.distinct()
    fun byCategory(category: String) = symbols.filter { it.category == category }
}

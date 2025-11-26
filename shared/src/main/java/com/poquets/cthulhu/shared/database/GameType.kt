package com.poquets.cthulhu.shared.database

/**
 * Enumeration of supported game types
 */
enum class GameType(val value: String) {
    ARKHAM("ARKHAM"),
    ELDRITCH("ELDRITCH");
    
    companion object {
        fun fromString(value: String): GameType? {
            return values().find { it.value == value }
        }
    }
}


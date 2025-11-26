package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository pattern for card operations
 * Provides a clean interface for game-specific card management
 */
class CardRepository(private val context: Context) {
    
    private val db = UnifiedCardDatabaseHelper.getInstance(context)
    
    /**
     * Get all cards for a game type (async)
     */
    suspend fun getCards(gameType: GameType): List<UnifiedCard> = withContext(Dispatchers.IO) {
        db.getCardsByGameType(gameType)
    }
    
    /**
     * Get cards by expansion (async)
     */
    suspend fun getCardsByExpansion(
        gameType: GameType,
        expansion: String
    ): List<UnifiedCard> = withContext(Dispatchers.IO) {
        db.getCardsByGameTypeAndExpansion(gameType, expansion)
    }
    
    /**
     * Get cards by region (Eldritch only, async)
     */
    suspend fun getCardsByRegion(
        gameType: GameType,
        region: String
    ): List<UnifiedCard> = withContext(Dispatchers.IO) {
        db.getCardsByGameTypeAndRegion(gameType, region)
    }
    
    /**
     * Get a specific card (async)
     */
    suspend fun getCard(
        gameType: GameType,
        cardId: String,
        expansion: String = "BASE"
    ): UnifiedCard? = withContext(Dispatchers.IO) {
        db.getCard(gameType, cardId, expansion)
    }
    
    /**
     * Get unencountered cards (for shuffling, async)
     */
    suspend fun getUnencounteredCards(
        gameType: GameType,
        expansion: String? = null,
        region: String? = null
    ): List<UnifiedCard> = withContext(Dispatchers.IO) {
        db.getUnencounteredCards(gameType, expansion, region)
    }
    
    /**
     * Update card encountered status (async)
     */
    suspend fun updateEncounteredStatus(
        gameType: GameType,
        cardId: String,
        expansion: String,
        encountered: String
    ): Boolean = withContext(Dispatchers.IO) {
        db.updateCardEncountered(gameType, cardId, expansion, encountered)
    }
    
    /**
     * Reset encountered status for reshuffling (async)
     */
    suspend fun resetEncounteredStatus(
        gameType: GameType,
        expansion: String? = null,
        region: String? = null
    ): Int = withContext(Dispatchers.IO) {
        db.resetEncounteredStatus(gameType, expansion, region)
    }
    
    /**
     * Get card count (async)
     */
    suspend fun getCardCount(gameType: GameType? = null): Int = withContext(Dispatchers.IO) {
        if (gameType != null) {
            db.getCardCount(gameType)
        } else {
            db.getCardCount()
        }
    }
    
    /**
     * Check if cards exist (async)
     */
    suspend fun hasCards(gameType: GameType? = null): Boolean = withContext(Dispatchers.IO) {
        if (gameType != null) {
            db.hasCards(gameType)
        } else {
            db.hasCards()
        }
    }
    
    /**
     * Get all expansions for a game type (async)
     * First tries to get from expansions table, falls back to cards if no expansions found
     */
    suspend fun getExpansionsForGameType(gameType: GameType): List<String> = withContext(Dispatchers.IO) {
        // First try to get from expansions table (more reliable)
        val expansionsFromTable = db.getExpansionNamesForGameType(gameType)
        if (expansionsFromTable.isNotEmpty()) {
            return@withContext expansionsFromTable
        }
        
        // Fallback: get from cards (for backward compatibility)
        val cards = db.getCardsByGameType(gameType)
        cards.map { it.expansion }.distinct().sorted()
    }
    
    /**
     * Get all regions for Eldritch (async)
     */
    suspend fun getRegionsForEldritch(): List<String> = withContext(Dispatchers.IO) {
        val cards = db.getCardsByGameType(GameType.ELDRITCH)
        cards.mapNotNull { it.region }.distinct().sorted()
    }
}


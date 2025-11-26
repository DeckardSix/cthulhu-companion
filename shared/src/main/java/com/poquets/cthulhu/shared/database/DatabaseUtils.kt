package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log

/**
 * Utility functions for database operations
 */
object DatabaseUtils {
    private const val TAG = "DatabaseUtils"
    
    /**
     * Get a summary of cards grouped by game type and expansion
     */
    fun getCardSummary(context: Context): Map<String, Any> {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        
        return mapOf(
            "total" to db.getCardCount(),
            "arkham" to db.getCardCount(GameType.ARKHAM),
            "eldritch" to db.getCardCount(GameType.ELDRITCH),
            "has_arkham" to db.hasCards(GameType.ARKHAM),
            "has_eldritch" to db.hasCards(GameType.ELDRITCH),
            "database_path" to db.getDatabasePath()
        )
    }
    
    /**
     * Get list of unique expansions for a game type
     */
    fun getExpansionsForGameType(context: Context, gameType: GameType): List<String> {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        val cards = db.getCardsByGameType(gameType)
        
        return cards.map { it.expansion }
            .distinct()
            .sorted()
    }
    
    /**
     * Get list of unique regions for Eldritch cards
     */
    fun getRegionsForEldritch(context: Context): List<String> {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        val cards = db.getCardsByGameType(GameType.ELDRITCH)
        
        return cards.mapNotNull { it.region }
            .distinct()
            .sorted()
    }
    
    /**
     * Verify database integrity
     */
    fun verifyDatabase(context: Context): Boolean {
        return try {
            val db = UnifiedCardDatabaseHelper.getInstance(context)
            val total = db.getCardCount()
            val arkham = db.getCardCount(GameType.ARKHAM)
            val eldritch = db.getCardCount(GameType.ELDRITCH)
            
            // Basic integrity check: total should equal sum of both games
            val sum = arkham + eldritch
            val isValid = total == sum
            
            if (!isValid) {
                Log.w(TAG, "Database integrity check failed: total=$total, sum=$sum")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying database: ${e.message}", e)
            false
        }
    }
}


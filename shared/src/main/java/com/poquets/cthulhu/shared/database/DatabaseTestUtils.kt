package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log

/**
 * Test utilities for database operations
 * Useful for debugging and development
 */
object DatabaseTestUtils {
    private const val TAG = "DatabaseTestUtils"
    
    /**
     * Create sample test cards for both games
     */
    fun createTestCards(): Pair<List<UnifiedCard>, List<UnifiedCard>> {
        val arkhamCards = listOf(
            UnifiedCard(
                gameType = GameType.ARKHAM,
                cardId = "1",
                expansion = "BASE",
                neighborhoodId = 1,
                cardName = "Test Arkham Card 1"
            ),
            UnifiedCard(
                gameType = GameType.ARKHAM,
                cardId = "2",
                expansion = "BASE",
                neighborhoodId = 1,
                cardName = "Test Arkham Card 2"
            )
        )
        
        val eldritchCards = listOf(
            UnifiedCard(
                gameType = GameType.ELDRITCH,
                cardId = "A1",
                expansion = "BASE",
                region = "AMERICAS",
                topHeader = "New York",
                topEncounter = "You encounter a strange figure...",
                cardName = "Test Eldritch Card 1"
            ),
            UnifiedCard(
                gameType = GameType.ELDRITCH,
                cardId = "A2",
                expansion = "BASE",
                region = "AMERICAS",
                topHeader = "Chicago",
                topEncounter = "The city is in chaos...",
                cardName = "Test Eldritch Card 2"
            )
        )
        
        return Pair(arkhamCards, eldritchCards)
    }
    
    /**
     * Insert test cards into database
     */
    fun insertTestCards(context: Context): Pair<Int, Int> {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        val (arkhamCards, eldritchCards) = createTestCards()
        
        val arkhamInserted = db.insertCards(arkhamCards)
        val eldritchInserted = db.insertCards(eldritchCards)
        
        Log.d(TAG, "Inserted $arkhamInserted Arkham test cards and $eldritchInserted Eldritch test cards")
        return Pair(arkhamInserted, eldritchInserted)
    }
    
    /**
     * Print database statistics to log
     */
    fun logDatabaseStats(context: Context) {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        val summary = DatabaseUtils.getCardSummary(context)
        
        Log.d(TAG, "=== Database Statistics ===")
        Log.d(TAG, "Total Cards: ${summary["total"]}")
        Log.d(TAG, "Arkham Cards: ${summary["arkham"]}")
        Log.d(TAG, "Eldritch Cards: ${summary["eldritch"]}")
        Log.d(TAG, "Database Path: ${summary["database_path"]}")
        Log.d(TAG, "===========================")
    }
    
    /**
     * Test query performance
     */
    fun testQueryPerformance(context: Context, iterations: Int = 100) {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        
        Log.d(TAG, "Testing query performance with $iterations iterations...")
        
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            db.getCardsByGameType(GameType.ARKHAM)
            db.getCardsByGameType(GameType.ELDRITCH)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val avgTime = duration.toDouble() / iterations
        
        Log.d(TAG, "Query Performance Test Results:")
        Log.d(TAG, "  Total time: ${duration}ms")
        Log.d(TAG, "  Average time per query pair: ${String.format("%.2f", avgTime)}ms")
    }
}


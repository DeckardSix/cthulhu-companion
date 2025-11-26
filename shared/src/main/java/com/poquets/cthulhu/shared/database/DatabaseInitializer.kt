package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log

/**
 * Utility class to initialize the unified database and migrate cards from existing databases
 */
object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"
    
    /**
     * Initialize the unified database and migrate cards from both games
     * @param context Application context
     * @param forceReinit If true, clear existing cards before migration
     * @return Pair of (arkhamCount, eldritchCount) cards migrated
     */
    fun initializeDatabase(
        context: Context,
        forceReinit: Boolean = false
    ): Pair<Int, Int> {
        Log.d(TAG, "Initializing unified database (forceReinit=$forceReinit)")
        
        val unifiedDb = UnifiedCardDatabaseHelper.getInstance(context)
        
        // Clear existing cards if requested
        if (forceReinit) {
            Log.d(TAG, "Clearing existing cards...")
            unifiedDb.clearAllCards()
        }
        
        // Check if we need to migrate
        val hasArkham = unifiedDb.hasCards(GameType.ARKHAM)
        val hasEldritch = unifiedDb.hasCards(GameType.ELDRITCH)
        
        if (hasArkham && hasEldritch && !forceReinit) {
            Log.d(TAG, "Database already contains cards from both games")
            return Pair(
                unifiedDb.getCardCount(GameType.ARKHAM),
                unifiedDb.getCardCount(GameType.ELDRITCH)
            )
        }
        
        // Migrate cards - migrate each game independently
        val arkhamCount = if (hasArkham && !forceReinit) {
            Log.d(TAG, "Arkham cards already exist, skipping migration")
            unifiedDb.getCardCount(GameType.ARKHAM)
        } else {
            Log.d(TAG, "Migrating Arkham cards...")
            CardMigration.migrateArkhamCards(context, unifiedDb)
        }
        
        val eldritchCount = if (hasEldritch && !forceReinit) {
            Log.d(TAG, "Eldritch cards already exist, skipping migration")
            unifiedDb.getCardCount(GameType.ELDRITCH)
        } else {
            Log.d(TAG, "Migrating Eldritch cards...")
            CardMigration.migrateEldritchCards(context, unifiedDb)
        }
        
        Log.d(TAG, "Database initialization complete: $arkhamCount Arkham cards, $eldritchCount Eldritch cards")
        
        return Pair(arkhamCount, eldritchCount)
    }
    
    /**
     * Initialize database and return detailed statistics
     */
    fun initializeDatabaseWithStats(
        context: Context,
        forceReinit: Boolean = false
    ): MigrationStats {
        return try {
            Log.d(TAG, "Initializing unified database with stats (forceReinit=$forceReinit)")
            
            val unifiedDb = UnifiedCardDatabaseHelper.getInstance(context)
            
            // Clear existing cards if requested
            if (forceReinit) {
                Log.d(TAG, "Clearing existing cards...")
                unifiedDb.clearAllCards()
            }
            
            // Check if we need to migrate
            val hasArkham = unifiedDb.hasCards(GameType.ARKHAM)
            val hasEldritch = unifiedDb.hasCards(GameType.ELDRITCH)
            
            if (hasArkham && hasEldritch && !forceReinit) {
                Log.d(TAG, "Database already contains cards from both games")
                val arkhamCount = unifiedDb.getCardCount(GameType.ARKHAM)
                val eldritchCount = unifiedDb.getCardCount(GameType.ELDRITCH)
                
                return MigrationStats(
                    arkhamCount = arkhamCount,
                    eldritchCount = eldritchCount,
                    totalCount = arkhamCount + eldritchCount,
                    arkhamExpansions = DatabaseUtils.getExpansionsForGameType(context, GameType.ARKHAM),
                    eldritchExpansions = DatabaseUtils.getExpansionsForGameType(context, GameType.ELDRITCH),
                    eldritchRegions = DatabaseUtils.getRegionsForEldritch(context),
                    success = true
                )
            }
            
            // Migrate cards
            val (arkhamCount, eldritchCount) = CardMigration.migrateAllCards(
                context,
                unifiedDb
            )
            
            MigrationStats(
                arkhamCount = arkhamCount,
                eldritchCount = eldritchCount,
                totalCount = arkhamCount + eldritchCount,
                arkhamExpansions = DatabaseUtils.getExpansionsForGameType(context, GameType.ARKHAM),
                eldritchExpansions = DatabaseUtils.getExpansionsForGameType(context, GameType.ELDRITCH),
                eldritchRegions = DatabaseUtils.getRegionsForEldritch(context),
                success = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database: ${e.message}", e)
            MigrationStats(
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Import only Arkham cards (force reimport if forceReinit is true)
     */
    fun importArkhamCards(
        context: Context,
        forceReinit: Boolean = false
    ): Int {
        Log.d(TAG, "Importing Arkham cards (forceReinit=$forceReinit)")
        val unifiedDb = UnifiedCardDatabaseHelper.getInstance(context)
        
        if (forceReinit) {
            // Clear only Arkham cards
            Log.d(TAG, "Clearing existing Arkham cards...")
            unifiedDb.clearCards(GameType.ARKHAM)
        }
        
        val hasArkham = unifiedDb.hasCards(GameType.ARKHAM)
        if (hasArkham && !forceReinit) {
            val count = unifiedDb.getCardCount(GameType.ARKHAM)
            Log.d(TAG, "Arkham cards already exist ($count cards), skipping migration")
            return count
        }
        
        Log.d(TAG, "Migrating Arkham cards ONLY (not Eldritch)...")
        // Explicitly pass null for eldritchDbPath to ensure Eldritch migration is not triggered
        val count = CardMigration.migrateArkhamCards(context, unifiedDb, null)
        Log.d(TAG, "Imported $count Arkham cards")
        
        // Verify only Arkham cards were imported
        val arkhamCount = unifiedDb.getCardCount(GameType.ARKHAM)
        val eldritchCount = unifiedDb.getCardCount(GameType.ELDRITCH)
        Log.d(TAG, "After import - Arkham: $arkhamCount cards, Eldritch: $eldritchCount cards")
        
        return count
    }
    
    /**
     * Import only Eldritch cards (force reimport if forceReinit is true)
     */
    fun importEldritchCards(
        context: Context,
        forceReinit: Boolean = false
    ): Int {
        Log.d(TAG, "Importing Eldritch cards (forceReinit=$forceReinit)")
        val unifiedDb = UnifiedCardDatabaseHelper.getInstance(context)
        
        if (forceReinit) {
            // Clear only Eldritch cards
            Log.d(TAG, "Clearing existing Eldritch cards...")
            unifiedDb.clearCards(GameType.ELDRITCH)
        }
        
        val hasEldritch = unifiedDb.hasCards(GameType.ELDRITCH)
        if (hasEldritch && !forceReinit) {
            val count = unifiedDb.getCardCount(GameType.ELDRITCH)
            Log.d(TAG, "Eldritch cards already exist ($count cards), skipping migration")
            return count
        }
        
        Log.d(TAG, "Migrating Eldritch cards ONLY (not Arkham)...")
        // Explicitly pass null for arkhamDbPath to ensure Arkham migration is not triggered
        val count = CardMigration.migrateEldritchCards(context, unifiedDb, null)
        Log.d(TAG, "Imported $count Eldritch cards")
        
        // Verify only Eldritch cards were imported
        val arkhamCount = unifiedDb.getCardCount(GameType.ARKHAM)
        val eldritchCount = unifiedDb.getCardCount(GameType.ELDRITCH)
        Log.d(TAG, "After import - Arkham: $arkhamCount cards, Eldritch: $eldritchCount cards")
        
        return count
    }
    
    /**
     * Get database status information
     */
    fun getDatabaseStatus(context: Context): String {
        val unifiedDb = UnifiedCardDatabaseHelper.getInstance(context)
        val total = unifiedDb.getCardCount()
        val arkham = unifiedDb.getCardCount(GameType.ARKHAM)
        val eldritch = unifiedDb.getCardCount(GameType.ELDRITCH)
        val dbPath = unifiedDb.getDatabasePath()
        
        return """
            Database Status:
            Total Cards: $total
            Arkham Cards: $arkham
            Eldritch Cards: $eldritch
            Database Path: $dbPath
        """.trimIndent()
    }
}


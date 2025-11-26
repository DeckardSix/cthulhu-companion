package com.poquets.cthulhu.shared.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Data classes for Arkham-specific entities
 */
data class NeighborhoodData(
    val id: Long,
    val name: String,
    val cardPath: String? = null,
    val buttonPath: String? = null
)

data class LocationData(
    val id: Long,
    val name: String,
    val buttonPath: String? = null,
    val sort: Int = 0
)

data class EncounterData(
    val id: Long,
    val text: String,
    val locationId: Long? = null
)

/**
 * Unified database helper that stores cards from both Arkham and Eldritch games
 * Cards are separated by game_type field
 */
class UnifiedCardDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    
    companion object {
        private const val TAG = "UnifiedCardDB"
        private const val DATABASE_NAME = "cthulhu_companion.db"
        private const val DATABASE_VERSION = 2
        
        // Singleton instance
        @Volatile
        private var INSTANCE: UnifiedCardDatabaseHelper? = null
        private val lock = Any()
        
        fun getInstance(context: Context): UnifiedCardDatabaseHelper {
            return INSTANCE ?: synchronized(lock) {
                // Try to copy pre-populated database from assets before creating instance
                copyDatabaseFromAssets(context.applicationContext)
                INSTANCE ?: UnifiedCardDatabaseHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        /**
         * Copies database from assets to app database directory if it doesn't exist
         * This allows using a pre-populated database instead of creating from scratch
         */
        private fun copyDatabaseFromAssets(context: Context): Boolean {
            try {
                val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath
                val dbFile = File(dbPath)
                
                // If database already exists and has data, don't overwrite
                if (dbFile.exists() && dbFile.length() > 0) {
                    Log.d(TAG, "Database already exists, skipping copy from assets")
                    return true
                }
                
                // Try to copy from assets/databases/cthulhu_companion.db
                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null
                try {
                    inputStream = context.assets.open("databases/$DATABASE_NAME")
                    
                    // Ensure database directory exists
                    val dbDir = dbFile.parentFile
                    if (dbDir != null && !dbDir.exists()) {
                        dbDir.mkdirs()
                    }
                    
                    outputStream = FileOutputStream(dbPath)
                    
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    
                    outputStream.flush()
                    Log.d(TAG, "Successfully copied pre-populated database from assets")
                    return true
                    
                } catch (e: IOException) {
                    // Database file not in assets - this is OK, will create from scratch
                    Log.d(TAG, "Pre-populated database not found in assets (will create from scratch): ${e.message}")
                    return false
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying database from assets: ${e.message}", e)
                return false
            }
        }
    }
    
    private val appContext: Context = context.applicationContext
    
    // Thread-safe read/write operations
    private val rwLock = ReentrantReadWriteLock()
    
    // Table and column names
    // Unified cards table - supports both game types
    private val TABLE_CARDS = "unified_cards"
    private val COLUMN_ID = "_id"
    private val COLUMN_GAME_TYPE = "game_type"  // "ARKHAM" or "ELDRITCH"
    private val COLUMN_CARD_ID = "card_id"      // Can be integer (Arkham) or text (Eldritch)
    
    // Arkham-specific fields (nullable for Eldritch cards)
    private val COLUMN_NEIGHBORHOOD_ID = "neighborhood_id"
    private val COLUMN_LOCATION_ID = "location_id"
    private val COLUMN_ENCOUNTER_ID = "encounter_id"
    
    // Eldritch-specific fields (nullable for Arkham cards)
    private val COLUMN_REGION = "region"
    private val COLUMN_TOP_HEADER = "top_header"
    private val COLUMN_TOP_ENCOUNTER = "top_encounter"
    private val COLUMN_MIDDLE_HEADER = "middle_header"
    private val COLUMN_MIDDLE_ENCOUNTER = "middle_encounter"
    private val COLUMN_BOTTOM_HEADER = "bottom_header"
    private val COLUMN_BOTTOM_ENCOUNTER = "bottom_encounter"
    
    // Common fields
    private val COLUMN_EXPANSION = "expansion"
    private val COLUMN_ENCOUNTERED = "encountered"
    private val COLUMN_CARD_NAME = "card_name"
    private val COLUMN_CARD_DATA = "card_data"  // JSON or additional data
    
    // Expansions table (shared across games)
    private val TABLE_EXPANSIONS = "expansions"
    private val COLUMN_EXP_ID = "exp_id"
    private val COLUMN_EXP_NAME = "exp_name"
    private val COLUMN_EXP_ICON_PATH = "exp_icon_path"
    
    // Arkham-specific tables
    private val TABLE_NEIGHBORHOODS = "neighborhoods"
    private val COLUMN_NEI_ID = "nei_id"
    private val COLUMN_NEI_NAME = "nei_name"
    private val COLUMN_NEI_CARD_PATH = "nei_card_path"
    private val COLUMN_NEI_BUTTON_PATH = "nei_button_path"
    
    private val TABLE_LOCATIONS = "locations"
    private val COLUMN_LOC_ID = "loc_id"
    private val COLUMN_LOC_NAME = "loc_name"
    private val COLUMN_LOC_NEI_ID = "loc_nei_id"  // NULL for other worlds
    private val COLUMN_LOC_BUTTON_PATH = "loc_button_path"
    private val COLUMN_LOC_SORT = "loc_sort"
    
    private val TABLE_ENCOUNTERS = "encounters"
    private val COLUMN_ENC_ID = "enc_id"
    private val COLUMN_ENC_LOC_ID = "enc_loc_id"
    private val COLUMN_ENC_TEXT = "enc_text"
    
    // Junction table for cards to encounters (many-to-many)
    private val TABLE_CARD_TO_ENCOUNTER = "card_to_encounter"
    private val COLUMN_CTE_CARD_ID = "cte_card_id"
    private val COLUMN_CTE_ENC_ID = "cte_enc_id"
    
    // Create tables SQL
    private val CREATE_TABLE_EXPANSIONS = """
        CREATE TABLE $TABLE_EXPANSIONS (
            $COLUMN_EXP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_GAME_TYPE TEXT NOT NULL,
            $COLUMN_EXP_NAME TEXT NOT NULL,
            $COLUMN_EXP_ICON_PATH TEXT,
            UNIQUE($COLUMN_GAME_TYPE, $COLUMN_EXP_NAME)
        )
    """.trimIndent()
    
    private val CREATE_TABLE_NEIGHBORHOODS = """
        CREATE TABLE $TABLE_NEIGHBORHOODS (
            $COLUMN_NEI_ID INTEGER PRIMARY KEY,
            $COLUMN_EXP_ID INTEGER NOT NULL,
            $COLUMN_NEI_NAME TEXT NOT NULL,
            $COLUMN_NEI_CARD_PATH TEXT,
            $COLUMN_NEI_BUTTON_PATH TEXT,
            FOREIGN KEY ($COLUMN_EXP_ID) REFERENCES $TABLE_EXPANSIONS($COLUMN_EXP_ID)
        )
    """.trimIndent()
    
    private val CREATE_TABLE_LOCATIONS = """
        CREATE TABLE $TABLE_LOCATIONS (
            $COLUMN_LOC_ID INTEGER PRIMARY KEY,
            $COLUMN_EXP_ID INTEGER,
            $COLUMN_NEI_ID INTEGER,
            $COLUMN_LOC_NAME TEXT NOT NULL,
            $COLUMN_LOC_BUTTON_PATH TEXT,
            $COLUMN_LOC_SORT INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY ($COLUMN_EXP_ID) REFERENCES $TABLE_EXPANSIONS($COLUMN_EXP_ID),
            FOREIGN KEY ($COLUMN_NEI_ID) REFERENCES $TABLE_NEIGHBORHOODS($COLUMN_NEI_ID)
        )
    """.trimIndent()
    
    private val CREATE_TABLE_ENCOUNTERS = """
        CREATE TABLE $TABLE_ENCOUNTERS (
            $COLUMN_ENC_ID INTEGER PRIMARY KEY,
            $COLUMN_LOC_ID INTEGER NOT NULL,
            $COLUMN_ENC_TEXT TEXT NOT NULL,
            FOREIGN KEY ($COLUMN_LOC_ID) REFERENCES $TABLE_LOCATIONS($COLUMN_LOC_ID)
        )
    """.trimIndent()
    
    private val CREATE_TABLE_CARD_TO_ENCOUNTER = """
        CREATE TABLE $TABLE_CARD_TO_ENCOUNTER (
            $COLUMN_CTE_CARD_ID TEXT NOT NULL,
            $COLUMN_CTE_ENC_ID INTEGER NOT NULL,
            $COLUMN_GAME_TYPE TEXT NOT NULL,
            PRIMARY KEY ($COLUMN_CTE_CARD_ID, $COLUMN_CTE_ENC_ID, $COLUMN_GAME_TYPE),
            FOREIGN KEY ($COLUMN_CTE_ENC_ID) REFERENCES $TABLE_ENCOUNTERS($COLUMN_ENC_ID)
        )
    """.trimIndent()
    
    private val CREATE_TABLE_CARDS = """
        CREATE TABLE $TABLE_CARDS (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_GAME_TYPE TEXT NOT NULL,
            $COLUMN_CARD_ID TEXT NOT NULL,
            $COLUMN_EXPANSION TEXT NOT NULL DEFAULT 'BASE',
            $COLUMN_CARD_NAME TEXT,
            $COLUMN_ENCOUNTERED TEXT DEFAULT 'NONE',
            $COLUMN_CARD_DATA TEXT,
            -- Arkham fields
            $COLUMN_NEIGHBORHOOD_ID INTEGER,
            $COLUMN_LOCATION_ID INTEGER,
            $COLUMN_ENCOUNTER_ID INTEGER,
            -- Eldritch fields
            $COLUMN_REGION TEXT,
            $COLUMN_TOP_HEADER TEXT,
            $COLUMN_TOP_ENCOUNTER TEXT,
            $COLUMN_MIDDLE_HEADER TEXT,
            $COLUMN_MIDDLE_ENCOUNTER TEXT,
            $COLUMN_BOTTOM_HEADER TEXT,
            $COLUMN_BOTTOM_ENCOUNTER TEXT,
            UNIQUE($COLUMN_GAME_TYPE, $COLUMN_CARD_ID, $COLUMN_EXPANSION)
        )
    """.trimIndent()
    
    // Indexes for performance
    private val CREATE_INDEX_GAME_TYPE = """
        CREATE INDEX idx_game_type ON $TABLE_CARDS($COLUMN_GAME_TYPE)
    """.trimIndent()
    
    private val CREATE_INDEX_EXPANSION = """
        CREATE INDEX idx_expansion ON $TABLE_CARDS($COLUMN_EXPANSION)
    """.trimIndent()
    
    private val CREATE_INDEX_REGION = """
        CREATE INDEX idx_region ON $TABLE_CARDS($COLUMN_REGION)
    """.trimIndent()
    
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "Creating unified database tables")
        db.execSQL(CREATE_TABLE_EXPANSIONS)
        db.execSQL(CREATE_TABLE_CARDS)
        db.execSQL(CREATE_TABLE_NEIGHBORHOODS)
        db.execSQL(CREATE_TABLE_LOCATIONS)
        db.execSQL(CREATE_TABLE_ENCOUNTERS)
        db.execSQL(CREATE_TABLE_CARD_TO_ENCOUNTER)
        db.execSQL(CREATE_INDEX_GAME_TYPE)
        db.execSQL(CREATE_INDEX_EXPANSION)
        db.execSQL(CREATE_INDEX_REGION)
        Log.d(TAG, "Database tables created successfully")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading database from version $oldVersion to $newVersion")
        // Handle migrations here when needed
        if (oldVersion < 2) {
            // Add Arkham-specific tables
            Log.d(TAG, "Adding Arkham-specific tables (neighborhoods, locations, encounters)")
            db.execSQL(CREATE_TABLE_NEIGHBORHOODS)
            db.execSQL(CREATE_TABLE_LOCATIONS)
            db.execSQL(CREATE_TABLE_ENCOUNTERS)
            db.execSQL(CREATE_TABLE_CARD_TO_ENCOUNTER)
        }
    }
    
    /**
     * Check if database has any cards
     */
    fun hasCards(): Boolean {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARDS", null)
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            return count > 0
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Check if database has cards for a specific game type
     */
    fun hasCards(gameType: GameType): Boolean {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_CARDS WHERE $COLUMN_GAME_TYPE = ?",
                arrayOf(gameType.value)
            )
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            return count > 0
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get total card count
     */
    fun getCardCount(): Int {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARDS", null)
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            return count
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get card count for a specific game type
     */
    fun getCardCount(gameType: GameType): Int {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_CARDS WHERE $COLUMN_GAME_TYPE = ?",
                arrayOf(gameType.value)
            )
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            return count
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Clear all cards (use with caution)
     */
    fun clearAllCards() {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            db.delete(TABLE_CARDS, null, null)
            Log.d(TAG, "All cards cleared from database")
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Clear cards for a specific game type
     */
    fun clearCards(gameType: GameType) {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            db.delete(TABLE_CARDS, "$COLUMN_GAME_TYPE = ?", arrayOf(gameType.value))
            Log.d(TAG, "Cards cleared for game type: ${gameType.value}")
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Get database file path (for persistence verification)
     */
    fun getDatabasePath(): String {
        return appContext.getDatabasePath(DATABASE_NAME).absolutePath
    }
    
    /**
     * Get or create an expansion and return its ID
     * Used during migration to map original expansion IDs to unified expansion IDs
     */
    fun getOrCreateExpansion(gameType: GameType, originalExpId: String, expName: String? = null, iconPath: String? = null): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            
            // First try to find by original ID stored in exp_name or by name
            val cursor = if (expName != null) {
                db.query(
                    TABLE_EXPANSIONS,
                    arrayOf(COLUMN_EXP_ID),
                    "$COLUMN_GAME_TYPE = ? AND ($COLUMN_EXP_NAME = ? OR $COLUMN_EXP_NAME = ?)",
                    arrayOf(gameType.value, originalExpId, expName),
                    null, null, null, "1"
                )
            } else {
                db.query(
                    TABLE_EXPANSIONS,
                    arrayOf(COLUMN_EXP_ID),
                    "$COLUMN_GAME_TYPE = ? AND $COLUMN_EXP_NAME = ?",
                    arrayOf(gameType.value, originalExpId),
                    null, null, null, "1"
                )
            }
            
            cursor.use {
                if (it.moveToFirst()) {
                    return it.getLong(0)
                }
            }
            
            // Not found, create it
            val values = android.content.ContentValues().apply {
                put(COLUMN_GAME_TYPE, gameType.value)
                put(COLUMN_EXP_NAME, expName ?: originalExpId)
                iconPath?.let { put(COLUMN_EXP_ICON_PATH, it) }
            }
            
            val newId = db.insert(TABLE_EXPANSIONS, null, values)
            if (newId == -1L) {
                Log.e(TAG, "Failed to create expansion: $expName")
                return -1L
            }
            
            return newId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting or creating expansion: ${e.message}", e)
            return -1L
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Insert a unified card into the database
     */
    fun insertCard(card: UnifiedCard): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            return db.insertOrThrow("unified_cards", null, card.toContentValues())
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting card: ${e.message}", e)
            throw e
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Insert multiple cards in a transaction
     * Uses INSERT OR IGNORE to handle duplicates gracefully
     */
    fun insertCards(cards: List<UnifiedCard>): Int {
        if (cards.isEmpty()) {
            Log.w(TAG, "insertCards called with empty list")
            return 0
        }
        
        // Log game type distribution
        val gameTypeCounts = cards.groupingBy { it.gameType }.eachCount()
        Log.d(TAG, "Inserting ${cards.size} cards: ${gameTypeCounts.entries.joinToString { "${it.key}=${it.value}" }}")
        
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            db.beginTransaction()
            var inserted = 0
            var ignored = 0
            var failed = 0
            try {
                for (card in cards) {
                    try {
                        val values = card.toContentValues()
                        // Use INSERT OR IGNORE to handle duplicates gracefully
                        val result = db.insertWithOnConflict(
                            "unified_cards",
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_IGNORE
                        )
                        if (result > 0) {
                            inserted++
                        } else if (result == -1L) {
                            // Row was ignored due to conflict (duplicate)
                            ignored++
                            if (ignored <= 5) {  // Log first 5 ignored cards
                                Log.d(TAG, "Ignored duplicate card ${card.cardId} (gameType=${card.gameType}, expansion=${card.expansion})")
                            }
                        } else {
                            failed++
                            Log.w(TAG, "Failed to insert card ${card.cardId} (gameType=${card.gameType}, expansion=${card.expansion}): insert returned $result")
                        }
                    } catch (e: Exception) {
                        failed++
                        Log.e(TAG, "Failed to insert card ${card.cardId} (gameType=${card.gameType}, expansion=${card.expansion}): ${e.message}", e)
                        // Log the ContentValues for debugging
                        try {
                            val values = card.toContentValues()
                            Log.d(TAG, "Card ContentValues: ${values.keySet().joinToString { "$it=${values.get(it)}" }}")
                        } catch (e2: Exception) {
                            Log.e(TAG, "Error creating ContentValues: ${e2.message}")
                        }
                    }
                }
                db.setTransactionSuccessful()
                if (failed > 0 || ignored > 0) {
                    Log.w(TAG, "Inserted $inserted cards, ignored $ignored duplicates, failed $failed cards out of ${cards.size} total")
                } else {
                    Log.d(TAG, "Successfully inserted all $inserted cards")
                }
            } finally {
                db.endTransaction()
            }
            return inserted
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Update card's encountered status
     */
    fun updateCardEncountered(
        gameType: GameType,
        cardId: String,
        expansion: String,
        encountered: String
    ): Boolean {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_ENCOUNTERED, encountered)
            }
            val rowsAffected = db.update(
                TABLE_CARDS,
                values,
                "$COLUMN_GAME_TYPE = ? AND $COLUMN_CARD_ID = ? AND $COLUMN_EXPANSION = ?",
                arrayOf(gameType.value, cardId, expansion)
            )
            return rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating card encountered status: ${e.message}", e)
            return false
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Get a specific card by ID
     */
    fun getCard(
        gameType: GameType,
        cardId: String,
        expansion: String = "BASE"
    ): UnifiedCard? {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CARDS,
                null,
                "$COLUMN_GAME_TYPE = ? AND $COLUMN_CARD_ID = ? AND $COLUMN_EXPANSION = ?",
                arrayOf(gameType.value, cardId, expansion),
                null,
                null,
                null,
                "1"
            )
            
            return cursor.use {
                if (it.moveToFirst()) {
                    createCardFromCursor(it)
                } else null
            }
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get all cards for a specific game type
     */
    fun getCardsByGameType(gameType: GameType): List<UnifiedCard> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CARDS,
                null,
                "$COLUMN_GAME_TYPE = ?",
                arrayOf(gameType.value),
                null,
                null,
                "$COLUMN_CARD_ID ASC"
            )
            
            val cards = mutableListOf<UnifiedCard>()
            cursor.use {
                while (it.moveToNext()) {
                    cards.add(createCardFromCursor(it))
                }
            }
            return cards
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get cards by game type and expansion
     */
    fun getCardsByGameTypeAndExpansion(
        gameType: GameType,
        expansion: String
    ): List<UnifiedCard> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CARDS,
                null,
                "$COLUMN_GAME_TYPE = ? AND $COLUMN_EXPANSION = ?",
                arrayOf(gameType.value, expansion),
                null,
                null,
                "$COLUMN_CARD_ID ASC"
            )
            
            val cards = mutableListOf<UnifiedCard>()
            cursor.use {
                while (it.moveToNext()) {
                    cards.add(createCardFromCursor(it))
                }
            }
            return cards
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get cards by game type and region (for Eldritch)
     */
    fun getCardsByGameTypeAndRegion(
        gameType: GameType,
        region: String
    ): List<UnifiedCard> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CARDS,
                null,
                "$COLUMN_GAME_TYPE = ? AND $COLUMN_REGION = ?",
                arrayOf(gameType.value, region),
                null,
                null,
                "$COLUMN_CARD_ID ASC"
            )
            
            val cards = mutableListOf<UnifiedCard>()
            cursor.use {
                while (it.moveToNext()) {
                    cards.add(createCardFromCursor(it))
                }
            }
            return cards
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get cards by game type, expansion, and region
     */
    fun getCardsByGameTypeExpansionAndRegion(
        gameType: GameType,
        expansion: String,
        region: String
    ): List<UnifiedCard> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CARDS,
                null,
                "$COLUMN_GAME_TYPE = ? AND $COLUMN_EXPANSION = ? AND $COLUMN_REGION = ?",
                arrayOf(gameType.value, expansion, region),
                null,
                null,
                "$COLUMN_CARD_ID ASC"
            )
            
            val cards = mutableListOf<UnifiedCard>()
            cursor.use {
                while (it.moveToNext()) {
                    cards.add(createCardFromCursor(it))
                }
            }
            return cards
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get cards that haven't been encountered (for shuffling/reshuffling)
     */
    fun getUnencounteredCards(
        gameType: GameType,
        expansion: String? = null,
        region: String? = null
    ): List<UnifiedCard> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val whereClause = StringBuilder("$COLUMN_GAME_TYPE = ? AND ($COLUMN_ENCOUNTERED = 'NONE' OR $COLUMN_ENCOUNTERED IS NULL)")
            val whereArgs = mutableListOf(gameType.value)
            
            if (expansion != null) {
                whereClause.append(" AND $COLUMN_EXPANSION = ?")
                whereArgs.add(expansion)
            }
            
            if (region != null) {
                whereClause.append(" AND $COLUMN_REGION = ?")
                whereArgs.add(region)
            }
            
            val cursor = db.query(
                TABLE_CARDS,
                null,
                whereClause.toString(),
                whereArgs.toTypedArray(),
                null,
                null,
                "$COLUMN_CARD_ID ASC"
            )
            
            val cards = mutableListOf<UnifiedCard>()
            cursor.use {
                while (it.moveToNext()) {
                    cards.add(createCardFromCursor(it))
                }
            }
            return cards
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Reset all encountered statuses for a game type (reshuffle deck)
     */
    fun resetEncounteredStatus(
        gameType: GameType,
        expansion: String? = null,
        region: String? = null
    ): Int {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val whereClause = StringBuilder("$COLUMN_GAME_TYPE = ?")
            val whereArgs = mutableListOf(gameType.value)
            
            if (expansion != null) {
                whereClause.append(" AND $COLUMN_EXPANSION = ?")
                whereArgs.add(expansion)
            }
            
            if (region != null) {
                whereClause.append(" AND $COLUMN_REGION = ?")
                whereArgs.add(region)
            }
            
            val values = android.content.ContentValues().apply {
                put(COLUMN_ENCOUNTERED, "NONE")
            }
            
            return db.update(
                TABLE_CARDS,
                values,
                whereClause.toString(),
                whereArgs.toTypedArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting encountered status: ${e.message}", e)
            return 0
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Insert a neighborhood
     */
    fun insertNeighborhood(neiId: Long, expId: Long, name: String, cardPath: String? = null, buttonPath: String? = null): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_NEI_ID, neiId)
                put(COLUMN_EXP_ID, expId)
                put(COLUMN_NEI_NAME, name)
                cardPath?.let { put(COLUMN_NEI_CARD_PATH, it) }
                buttonPath?.let { put(COLUMN_NEI_BUTTON_PATH, it) }
            }
            return db.insertWithOnConflict(TABLE_NEIGHBORHOODS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting neighborhood: ${e.message}", e)
            return -1
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Get neighborhoods for an expansion
     */
    fun getNeighborhoods(expId: Long): List<NeighborhoodData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NEIGHBORHOODS,
                arrayOf(COLUMN_NEI_ID, COLUMN_NEI_NAME, COLUMN_NEI_CARD_PATH, COLUMN_NEI_BUTTON_PATH),
                "$COLUMN_EXP_ID = ?",
                arrayOf(expId.toString()),
                null, null,
                "$COLUMN_NEI_NAME ASC"
            )
            
            val neighborhoods = mutableListOf<NeighborhoodData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_NEI_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_NEI_NAME)
                val cardPathIndex = it.getColumnIndex(COLUMN_NEI_CARD_PATH)
                val buttonPathIndex = it.getColumnIndex(COLUMN_NEI_BUTTON_PATH)
                
                while (it.moveToNext()) {
                    neighborhoods.add(
                        NeighborhoodData(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            cardPath = it.getStringOrNull(cardPathIndex),
                            buttonPath = it.getStringOrNull(buttonPathIndex)
                        )
                    )
                }
            }
            return neighborhoods
        } catch (e: Exception) {
            Log.e(TAG, "Error getting neighborhoods: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Insert a location
     */
    fun insertLocation(locId: Long, expId: Long?, neiId: Long?, name: String, buttonPath: String? = null, sort: Int = 0): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_LOC_ID, locId)
                expId?.let { put(COLUMN_EXP_ID, it) }
                neiId?.let { put(COLUMN_NEI_ID, it) }
                put(COLUMN_LOC_NAME, name)
                buttonPath?.let { put(COLUMN_LOC_BUTTON_PATH, it) }
                put(COLUMN_LOC_SORT, sort)
            }
            return db.insertWithOnConflict(TABLE_LOCATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting location: ${e.message}", e)
            return -1
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Get locations for a neighborhood
     */
    fun getLocationsByNeighborhood(neiId: Long): List<LocationData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LOCATIONS,
                arrayOf(COLUMN_LOC_ID, COLUMN_LOC_NAME, COLUMN_LOC_BUTTON_PATH, COLUMN_LOC_SORT),
                "$COLUMN_NEI_ID = ?",
                arrayOf(neiId.toString()),
                null, null,
                "$COLUMN_LOC_SORT ASC, $COLUMN_LOC_NAME ASC"
            )
            
            val locations = mutableListOf<LocationData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_LOC_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_LOC_BUTTON_PATH)
                val sortIndex = it.getColumnIndex(COLUMN_LOC_SORT)
                
                while (it.moveToNext()) {
                    locations.add(
                        LocationData(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            buttonPath = it.getStringOrNull(buttonPathIndex),
                            sort = it.getInt(sortIndex)
                        )
                    )
                }
            }
            return locations
        } catch (e: Exception) {
            Log.e(TAG, "Error getting locations: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get other world locations (locations without neighborhood)
     */
    fun getOtherWorldLocations(expId: Long? = null): List<LocationData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val whereClause = "$COLUMN_NEI_ID IS NULL"
            val whereArgs = if (expId != null) arrayOf(expId.toString()) else null
            
            val cursor = db.query(
                TABLE_LOCATIONS,
                arrayOf(COLUMN_LOC_ID, COLUMN_LOC_NAME, COLUMN_LOC_BUTTON_PATH, COLUMN_LOC_SORT),
                if (expId != null) "$whereClause AND $COLUMN_EXP_ID = ?" else whereClause,
                whereArgs,
                null, null,
                "$COLUMN_LOC_SORT ASC, $COLUMN_LOC_NAME ASC"
            )
            
            val locations = mutableListOf<LocationData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_LOC_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_LOC_BUTTON_PATH)
                val sortIndex = it.getColumnIndex(COLUMN_LOC_SORT)
                
                while (it.moveToNext()) {
                    locations.add(
                        LocationData(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            buttonPath = it.getStringOrNull(buttonPathIndex),
                            sort = it.getInt(sortIndex)
                        )
                    )
                }
            }
            return locations
        } catch (e: Exception) {
            Log.e(TAG, "Error getting other world locations: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Insert an encounter
     */
    fun insertEncounter(encId: Long, locId: Long, text: String): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_ENC_ID, encId)
                put(COLUMN_LOC_ID, locId)
                put(COLUMN_ENC_TEXT, text)
            }
            return db.insertWithOnConflict(TABLE_ENCOUNTERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting encounter: ${e.message}", e)
            return -1
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Get encounters for a location
     */
    fun getEncountersByLocation(locId: Long): List<EncounterData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_ENCOUNTERS,
                arrayOf(COLUMN_ENC_ID, COLUMN_ENC_TEXT),
                "$COLUMN_LOC_ID = ?",
                arrayOf(locId.toString()),
                null, null,
                "$COLUMN_ENC_ID ASC"
            )
            
            val encounters = mutableListOf<EncounterData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_ENC_ID)
                val textIndex = it.getColumnIndexOrThrow(COLUMN_ENC_TEXT)
                
                while (it.moveToNext()) {
                    encounters.add(
                        EncounterData(
                            id = it.getLong(idIndex),
                            text = it.getString(textIndex)
                        )
                    )
                }
            }
            return encounters
        } catch (e: Exception) {
            Log.e(TAG, "Error getting encounters: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Link a card to an encounter
     */
    fun linkCardToEncounter(cardId: String, gameType: GameType, encId: Long) {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_CTE_CARD_ID, cardId)
                put(COLUMN_CTE_ENC_ID, encId)
                put(COLUMN_GAME_TYPE, gameType.value)
            }
            db.insertWithOnConflict(TABLE_CARD_TO_ENCOUNTER, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error linking card to encounter: ${e.message}", e)
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Get encounters for a card
     */
    fun getEncountersForCard(cardId: String, gameType: GameType): List<EncounterData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val query = """
                SELECT e.$COLUMN_ENC_ID, e.$COLUMN_ENC_TEXT, e.$COLUMN_LOC_ID
                FROM $TABLE_ENCOUNTERS e
                INNER JOIN $TABLE_CARD_TO_ENCOUNTER cte ON e.$COLUMN_ENC_ID = cte.$COLUMN_CTE_ENC_ID
                WHERE cte.$COLUMN_CTE_CARD_ID = ? AND cte.$COLUMN_GAME_TYPE = ?
                ORDER BY e.$COLUMN_ENC_ID ASC
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(cardId, gameType.value))
            
            val encounters = mutableListOf<EncounterData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_ENC_ID)
                val textIndex = it.getColumnIndexOrThrow(COLUMN_ENC_TEXT)
                val locIdIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                
                while (it.moveToNext()) {
                    encounters.add(
                        EncounterData(
                            id = it.getLong(idIndex),
                            text = it.getString(textIndex),
                            locationId = it.getLong(locIdIndex)
                        )
                    )
                }
            }
            return encounters
        } catch (e: Exception) {
            Log.e(TAG, "Error getting encounters for card: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Create UnifiedCard from cursor
     */
    private fun createCardFromCursor(cursor: android.database.Cursor): UnifiedCard {
        val gameTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_TYPE))
        val gameType = GameType.fromString(gameTypeStr) ?: GameType.ARKHAM
        
        return UnifiedCard(
            gameType = gameType,
            cardId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_ID)),
            expansion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPANSION)),
            cardName = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_CARD_NAME)),
            encountered = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_ENCOUNTERED)) ?: "NONE",
            cardData = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_CARD_DATA)),
            neighborhoodId = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_NEIGHBORHOOD_ID)),
            locationId = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_LOCATION_ID)),
            encounterId = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_ENCOUNTER_ID)),
            region = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_REGION)),
            topHeader = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_TOP_HEADER)),
            topEncounter = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_TOP_ENCOUNTER)),
            middleHeader = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_MIDDLE_HEADER)),
            middleEncounter = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_MIDDLE_ENCOUNTER)),
            bottomHeader = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_BOTTOM_HEADER)),
            bottomEncounter = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_BOTTOM_ENCOUNTER))
        )
    }
    
    /**
     * Get all expansion names for a game type from the expansions table
     */
    fun getExpansionNamesForGameType(gameType: GameType): List<String> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_EXPANSIONS,
                arrayOf(COLUMN_EXP_NAME),
                "$COLUMN_GAME_TYPE = ?",
                arrayOf(gameType.value),
                null, null,
                "$COLUMN_EXP_NAME ASC"
            )
            
            val expansions = mutableListOf<String>()
            cursor.use {
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_EXP_NAME)
                while (it.moveToNext()) {
                    expansions.add(it.getString(nameIndex))
                }
            }
            
            return expansions
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Helper extension functions for cursor
     */
    private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) {
            getString(columnIndex)
        } else null
    }
    
    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) {
            getLong(columnIndex)
        } else null
    }
}

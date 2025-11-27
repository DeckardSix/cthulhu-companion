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
    val sort: Int = 0,
    val neiId: Long? = null,
    val expId: Long? = null
)

data class EncounterData(
    val id: Long,
    val text: String,
    val locationId: Long? = null
)

data class EncounterWithCardData(
    val encounterId: Long,
    val encounterText: String,
    val locationId: Long,
    val cardId: String
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
        // Version 4: add card_to_color table so we can efficiently map Arkham other world cards to colors
        private const val DATABASE_VERSION = 4
        
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
    
    // Arkham Other World Color tables
    private val TABLE_COLORS = "colors"
    private val COLUMN_COLOR_ID = "color_id"
    private val COLUMN_COLOR_NAME = "color_name"
    private val COLUMN_COLOR_BUTTON_PATH = "color_button_path"
    private val COLUMN_COLOR_EXP_ID = "color_exp_id"
    
    private val TABLE_LOCATION_TO_COLOR = "location_to_color"
    private val COLUMN_LTC_LOC_ID = "ltc_loc_id"
    private val COLUMN_LTC_COLOR_ID = "ltc_color_id"

    // Card to Color table (for Other World cards)
    private val TABLE_CARD_TO_COLOR = "card_to_color"
    private val COLUMN_CTC_CARD_ID = "ctc_card_id"
    private val COLUMN_CTC_COLOR_ID = "ctc_color_id"
    
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
    
    private val CREATE_TABLE_COLORS = """
        CREATE TABLE IF NOT EXISTS $TABLE_COLORS (
            $COLUMN_COLOR_ID INTEGER PRIMARY KEY,
            $COLUMN_COLOR_EXP_ID INTEGER NOT NULL,
            $COLUMN_COLOR_NAME TEXT NOT NULL,
            $COLUMN_COLOR_BUTTON_PATH TEXT,
            FOREIGN KEY ($COLUMN_COLOR_EXP_ID) REFERENCES $TABLE_EXPANSIONS($COLUMN_EXP_ID)
        )
    """.trimIndent()
    
    private val CREATE_TABLE_LOCATION_TO_COLOR = """
        CREATE TABLE IF NOT EXISTS $TABLE_LOCATION_TO_COLOR (
            $COLUMN_LTC_LOC_ID INTEGER NOT NULL,
            $COLUMN_LTC_COLOR_ID INTEGER NOT NULL,
            PRIMARY KEY ($COLUMN_LTC_LOC_ID, $COLUMN_LTC_COLOR_ID),
            FOREIGN KEY ($COLUMN_LTC_LOC_ID) REFERENCES $TABLE_LOCATIONS($COLUMN_LOC_ID),
            FOREIGN KEY ($COLUMN_LTC_COLOR_ID) REFERENCES $TABLE_COLORS($COLUMN_COLOR_ID)
        )
    """.trimIndent()

    private val CREATE_TABLE_CARD_TO_COLOR = """
        CREATE TABLE IF NOT EXISTS $TABLE_CARD_TO_COLOR (
            $COLUMN_CTC_CARD_ID TEXT NOT NULL,
            $COLUMN_CTC_COLOR_ID INTEGER NOT NULL,
            PRIMARY KEY ($COLUMN_CTC_CARD_ID, $COLUMN_CTC_COLOR_ID),
            FOREIGN KEY ($COLUMN_CTC_COLOR_ID) REFERENCES $TABLE_COLORS($COLUMN_COLOR_ID)
        )
    """.trimIndent()
    
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "Creating unified database tables")
        db.execSQL(CREATE_TABLE_EXPANSIONS)
        db.execSQL(CREATE_TABLE_CARDS)
        db.execSQL(CREATE_TABLE_NEIGHBORHOODS)
        db.execSQL(CREATE_TABLE_LOCATIONS)
        db.execSQL(CREATE_TABLE_ENCOUNTERS)
        db.execSQL(CREATE_TABLE_CARD_TO_ENCOUNTER)
        db.execSQL(CREATE_TABLE_COLORS)
        db.execSQL(CREATE_TABLE_LOCATION_TO_COLOR)
        db.execSQL(CREATE_TABLE_CARD_TO_COLOR)
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
        if (oldVersion < 3) {
            // Add color tables
            Log.d(TAG, "Adding color tables (colors, location_to_color)")
            db.execSQL(CREATE_TABLE_COLORS)
            db.execSQL(CREATE_TABLE_LOCATION_TO_COLOR)
        }
        if (oldVersion < 4) {
            // Add card_to_color table
            Log.d(TAG, "Adding card_to_color table")
            db.execSQL(CREATE_TABLE_CARD_TO_COLOR)
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
            // Optimize for bulk inserts
            try {
                db.execSQL("PRAGMA synchronous = OFF")
                db.execSQL("PRAGMA journal_mode = MEMORY")
            } catch (e: Exception) {
                // Ignore if pragmas fail
            }
            
            db.beginTransaction()
            var inserted = 0
            var ignored = 0
            var failed = 0
            try {
                // Use prepared statement for better performance
                val insertStmt = db.compileStatement(
                    "INSERT OR IGNORE INTO unified_cards " +
                    "(game_type, card_id, expansion, card_name, encountered, card_data, " +
                    "neighborhood_id, location_id, encounter_id, region, " +
                    "top_header, top_encounter, middle_header, middle_encounter, " +
                    "bottom_header, bottom_encounter) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                
                for (card in cards) {
                    try {
                        insertStmt.clearBindings()
                        insertStmt.bindString(1, card.gameType.value)
                        insertStmt.bindString(2, card.cardId)
                        insertStmt.bindString(3, card.expansion)
                        card.cardName?.let { insertStmt.bindString(4, it) } ?: insertStmt.bindNull(4)
                        insertStmt.bindString(5, card.encountered ?: "NONE")
                        card.cardData?.let { insertStmt.bindString(6, it) } ?: insertStmt.bindNull(6)
                        card.neighborhoodId?.let { insertStmt.bindLong(7, it) } ?: insertStmt.bindNull(7)
                        card.locationId?.let { insertStmt.bindLong(8, it) } ?: insertStmt.bindNull(8)
                        card.encounterId?.let { insertStmt.bindLong(9, it) } ?: insertStmt.bindNull(9)
                        card.region?.let { insertStmt.bindString(10, it) } ?: insertStmt.bindNull(10)
                        card.topHeader?.let { insertStmt.bindString(11, it) } ?: insertStmt.bindNull(11)
                        card.topEncounter?.let { insertStmt.bindString(12, it) } ?: insertStmt.bindNull(12)
                        card.middleHeader?.let { insertStmt.bindString(13, it) } ?: insertStmt.bindNull(13)
                        card.middleEncounter?.let { insertStmt.bindString(14, it) } ?: insertStmt.bindNull(14)
                        card.bottomHeader?.let { insertStmt.bindString(15, it) } ?: insertStmt.bindNull(15)
                        card.bottomEncounter?.let { insertStmt.bindString(16, it) } ?: insertStmt.bindNull(16)
                        
                        val result = insertStmt.executeInsert()
                        if (result > 0) {
                            inserted++
                        } else {
                            // Row was ignored due to conflict (duplicate) or constraint violation
                            ignored++
                            if (ignored <= 5) {  // Log first 5 ignored cards
                                Log.d(TAG, "Ignored duplicate card ${card.cardId} (gameType=${card.gameType}, expansion=${card.expansion})")
                            }
                        }
                    } catch (e: Exception) {
                        failed++
                        if (failed <= 5) {  // Log first 5 failures
                            Log.e(TAG, "Failed to insert card ${card.cardId} (gameType=${card.gameType}, expansion=${card.expansion}): ${e.message}", e)
                        }
                    }
                }
                
                insertStmt.close()
                db.setTransactionSuccessful()
                if (failed > 0 || ignored > 0) {
                    Log.w(TAG, "Inserted $inserted cards, ignored $ignored duplicates, failed $failed cards out of ${cards.size} total")
                } else {
                    Log.d(TAG, "Successfully inserted all $inserted cards")
                }
            } finally {
                db.endTransaction()
                // Restore normal SQLite settings
                try {
                    db.execSQL("PRAGMA synchronous = NORMAL")
                    db.execSQL("PRAGMA journal_mode = DELETE")
                } catch (e: Exception) {
                    // Ignore if pragmas fail
                }
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
            // Use DISTINCT to ensure unique neighborhoods (in case same neighborhood appears multiple times)
            val cursor = db.rawQuery(
                """
                SELECT DISTINCT 
                    $COLUMN_NEI_ID, 
                    $COLUMN_NEI_NAME, 
                    $COLUMN_NEI_CARD_PATH, 
                    $COLUMN_NEI_BUTTON_PATH
                FROM $TABLE_NEIGHBORHOODS
                WHERE $COLUMN_EXP_ID = ?
                ORDER BY $COLUMN_NEI_NAME ASC
                """.trimIndent(),
                arrayOf(expId.toString())
            )
            
            val neighborhoods = mutableListOf<NeighborhoodData>()
            val seenIds = mutableSetOf<Long>() // Track seen neighborhood IDs to prevent duplicates
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_NEI_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_NEI_NAME)
                val cardPathIndex = it.getColumnIndex(COLUMN_NEI_CARD_PATH)
                val buttonPathIndex = it.getColumnIndex(COLUMN_NEI_BUTTON_PATH)
                
                while (it.moveToNext()) {
                    val neiId = it.getLong(idIndex)
                    // Only add if we haven't seen this neighborhood ID before
                    if (seenIds.add(neiId)) {
                        neighborhoods.add(
                            NeighborhoodData(
                                id = neiId,
                                name = it.getString(nameIndex),
                                cardPath = it.getStringOrNull(cardPathIndex),
                                buttonPath = it.getStringOrNull(buttonPathIndex)
                            )
                        )
                    }
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
     * Get neighborhoods for multiple expansions (single query to avoid duplicates)
     * This is more efficient than calling getNeighborhoods() multiple times
     */
    fun getNeighborhoodsForExpansions(expIds: List<Long>): List<NeighborhoodData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            if (expIds.isEmpty()) {
                return emptyList()
            }
            
            // Build query with IN clause for all expansion IDs
            val placeholders = expIds.joinToString(",") { "?" }
            val query = """
                SELECT DISTINCT 
                    $COLUMN_NEI_ID, 
                    $COLUMN_NEI_NAME, 
                    $COLUMN_NEI_CARD_PATH, 
                    $COLUMN_NEI_BUTTON_PATH
                FROM $TABLE_NEIGHBORHOODS
                WHERE $COLUMN_EXP_ID IN ($placeholders) OR $COLUMN_EXP_ID = 1
                ORDER BY $COLUMN_NEI_NAME ASC
            """.trimIndent()
            
            val args = expIds.map { it.toString() }.toTypedArray()
            val cursor = db.rawQuery(query, args)
            
            val neighborhoods = mutableListOf<NeighborhoodData>()
            val seenIds = mutableSetOf<Long>() // Track seen neighborhood IDs to prevent duplicates
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_NEI_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_NEI_NAME)
                val cardPathIndex = it.getColumnIndex(COLUMN_NEI_CARD_PATH)
                val buttonPathIndex = it.getColumnIndex(COLUMN_NEI_BUTTON_PATH)
                
                while (it.moveToNext()) {
                    val neiId = it.getLong(idIndex)
                    // Only add if we haven't seen this neighborhood ID before
                    if (seenIds.add(neiId)) {
                        neighborhoods.add(
                            NeighborhoodData(
                                id = neiId,
                                name = it.getString(nameIndex),
                                cardPath = it.getStringOrNull(cardPathIndex),
                                buttonPath = it.getStringOrNull(buttonPathIndex)
                            )
                        )
                    }
                }
            }
            return neighborhoods
        } catch (e: Exception) {
            Log.e(TAG, "Error getting neighborhoods for expansions: ${e.message}", e)
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
     * Get a location by ID directly (without expansion filtering)
     */
    fun getLocationById(locId: Long): LocationData? {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LOCATIONS,
                arrayOf(COLUMN_LOC_ID, COLUMN_LOC_NAME, COLUMN_LOC_BUTTON_PATH, COLUMN_NEI_ID, COLUMN_EXP_ID, COLUMN_LOC_SORT),
                "$COLUMN_LOC_ID = ?",
                arrayOf(locId.toString()),
                null, null, null, "1"
            )
            
            cursor.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                    val nameIndex = it.getColumnIndexOrThrow(COLUMN_LOC_NAME)
                    val buttonPathIndex = it.getColumnIndex(COLUMN_LOC_BUTTON_PATH)
                    val neiIdIndex = it.getColumnIndex(COLUMN_NEI_ID)
                    val expIdIndex = it.getColumnIndex(COLUMN_EXP_ID)
                    val sortIndex = it.getColumnIndex(COLUMN_LOC_SORT)
                    
                    return LocationData(
                        id = it.getLong(idIndex),
                        name = it.getString(nameIndex),
                        buttonPath = it.getStringOrNull(buttonPathIndex),
                        neiId = it.getLongOrNull(neiIdIndex),
                        expId = it.getLongOrNull(expIdIndex),
                        sort = if (sortIndex >= 0 && !it.isNull(sortIndex)) it.getInt(sortIndex) else 0
                    )
                }
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location by ID: ${e.message}", e)
            return null
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get locations for a neighborhood
     */
    fun getLocationsByNeighborhood(neiId: Long): List<LocationData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            // Use DISTINCT to ensure unique locations (in case same location appears in multiple expansions)
            val cursor = db.rawQuery(
                """
                SELECT DISTINCT 
                    $COLUMN_LOC_ID, 
                    $COLUMN_LOC_NAME, 
                    $COLUMN_LOC_BUTTON_PATH, 
                    $COLUMN_LOC_SORT
                FROM $TABLE_LOCATIONS
                WHERE $COLUMN_NEI_ID = ?
                ORDER BY $COLUMN_LOC_SORT ASC, $COLUMN_LOC_NAME ASC
                """.trimIndent(),
                arrayOf(neiId.toString())
            )
            
            val locations = mutableListOf<LocationData>()
            val seenIds = mutableSetOf<Long>() // Track seen location IDs to prevent duplicates
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_LOC_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_LOC_BUTTON_PATH)
                val sortIndex = it.getColumnIndex(COLUMN_LOC_SORT)
                
                while (it.moveToNext()) {
                    val locId = it.getLong(idIndex)
                    // Only add if we haven't seen this location ID before
                    if (seenIds.add(locId)) {
                        locations.add(
                            LocationData(
                                id = locId,
                                name = it.getString(nameIndex),
                                buttonPath = it.getStringOrNull(buttonPathIndex),
                                sort = it.getInt(sortIndex)
                            )
                        )
                    }
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
     * @param expIds List of expansion IDs to filter by (base game ID 1 is always included)
     * @param excludeIds List of location IDs to exclude (e.g., 499, 500)
     */
    fun getOtherWorldLocations(expIds: List<Long>? = null, excludeIds: List<Long> = listOf(499L, 500L)): List<LocationData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            var whereClause = "$COLUMN_NEI_ID IS NULL"
            val whereArgs = mutableListOf<String>()
            
            // Filter by expansion IDs (base game ID 1 is always included)
            if (expIds != null && expIds.isNotEmpty()) {
                // Remove 1 from the list if present (we'll handle it separately)
                val nonBaseExpIds = expIds.filter { it != 1L }
                if (nonBaseExpIds.isNotEmpty()) {
                    val placeholders = nonBaseExpIds.joinToString(",") { "?" }
                    // Show locations from selected expansions OR base game (ID 1)
                    whereClause += " AND ($COLUMN_EXP_ID IN ($placeholders) OR $COLUMN_EXP_ID = 1)"
                    whereArgs.addAll(nonBaseExpIds.map { it.toString() })
                } else {
                    // Only base game selected
                    whereClause += " AND $COLUMN_EXP_ID = 1"
                }
            } else {
                // If no expansions specified, only show base game
                whereClause += " AND $COLUMN_EXP_ID = 1"
            }
            
            // Exclude specific location IDs (499 and 500 are special placeholders)
            if (excludeIds.isNotEmpty()) {
                val excludePlaceholders = excludeIds.joinToString(",") { "?" }
                whereClause += " AND $COLUMN_LOC_ID NOT IN ($excludePlaceholders)"
                whereArgs.addAll(excludeIds.map { it.toString() })
                Log.d(TAG, "Excluding location IDs: ${excludeIds.joinToString(", ")}")
            }
            
            // Use DISTINCT to ensure unique locations (in case same location appears in multiple expansions)
            val query = """
                SELECT DISTINCT 
                    $COLUMN_LOC_ID, 
                    $COLUMN_LOC_NAME, 
                    $COLUMN_LOC_BUTTON_PATH, 
                    $COLUMN_NEI_ID, 
                    $COLUMN_EXP_ID, 
                    $COLUMN_LOC_SORT
                FROM $TABLE_LOCATIONS
                WHERE $whereClause
                ORDER BY $COLUMN_LOC_SORT ASC, $COLUMN_LOC_NAME ASC
            """.trimIndent()
            
            val cursor = db.rawQuery(
                query,
                if (whereArgs.isNotEmpty()) whereArgs.toTypedArray() else null
            )
            
            val locations = mutableListOf<LocationData>()
            val seenIds = mutableSetOf<Long>() // Track seen location IDs to prevent duplicates
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_LOC_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_LOC_BUTTON_PATH)
                val neiIdIndex = it.getColumnIndex(COLUMN_NEI_ID)
                val expIdIndex = it.getColumnIndex(COLUMN_EXP_ID)
                val sortIndex = it.getColumnIndex(COLUMN_LOC_SORT)
                
                while (it.moveToNext()) {
                    val locId = it.getLong(idIndex)
                    // Only add if we haven't seen this location ID before
                    if (seenIds.add(locId)) {
                        locations.add(
                            LocationData(
                                id = locId,
                                name = it.getString(nameIndex),
                                buttonPath = it.getStringOrNull(buttonPathIndex),
                                neiId = it.getLongOrNull(neiIdIndex),
                                expId = it.getLongOrNull(expIdIndex),
                                sort = if (sortIndex >= 0 && !it.isNull(sortIndex)) it.getInt(sortIndex) else 0
                            )
                        )
                    }
                }
            }
            Log.d(TAG, "getOtherWorldLocations: Found ${locations.size} locations with query: $whereClause, args: ${whereArgs.joinToString(", ")}")
            if (locations.isEmpty() && expIds != null) {
                // Log what locations exist without expansion filter
                val allLocationsCursor = db.query(
                    TABLE_LOCATIONS,
                    arrayOf(COLUMN_LOC_ID, COLUMN_LOC_NAME, COLUMN_EXP_ID),
                    "$COLUMN_NEI_ID IS NULL AND $COLUMN_LOC_ID NOT IN (?, ?)",
                    arrayOf("499", "500"),
                    null, null, null
                )
                val allCount = allLocationsCursor.count
                allLocationsCursor.close()
                Log.w(TAG, "No locations found with expansion filter. Total otherworld locations (excluding 499,500): $allCount")
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
     * Find encounters by location and card colors
     * Returns encounters that:
     * 1. Belong to the specified location
     * 2. Are from cards that have at least one of the specified colors
     * 
     * This is used for other world card selection logic.
     */
    fun findEncountersByLocationAndColors(
        locationId: Long,
        colorIds: List<Long>,
        gameType: GameType
    ): List<EncounterWithCardData> {
        rwLock.readLock().lock()
        try {
            if (colorIds.isEmpty()) {
                return emptyList()
            }
            
            val db = readableDatabase
            val colorIdsStr = colorIds.joinToString(",")
            
            // Debug: Check how many encounters exist for this location (without color filter)
            val debugQuery1 = """
                SELECT COUNT(*) as count
                FROM $TABLE_ENCOUNTERS e
                WHERE e.$COLUMN_LOC_ID = ?
            """.trimIndent()
            val debugCursor1 = db.rawQuery(debugQuery1, arrayOf(locationId.toString()))
            var totalEncountersForLocation = 0
            debugCursor1.use {
                if (it.moveToFirst()) {
                    totalEncountersForLocation = it.getInt(0)
                }
            }
            Log.d(TAG, "DEBUG: Location $locationId has $totalEncountersForLocation total encounters")
            
            // Debug: Check how many cards have the specified colors
            val debugQuery2 = """
                SELECT COUNT(DISTINCT c.$COLUMN_CARD_ID) as count
                FROM $TABLE_CARDS c
                INNER JOIN $TABLE_CARD_TO_COLOR ctc ON c.$COLUMN_CARD_ID = ctc.$COLUMN_CTC_CARD_ID
                WHERE c.$COLUMN_GAME_TYPE = ?
                    AND ctc.$COLUMN_CTC_COLOR_ID IN ($colorIdsStr)
            """.trimIndent()
            val debugCursor2 = db.rawQuery(debugQuery2, arrayOf(gameType.value))
            var totalCardsWithColors = 0
            debugCursor2.use {
                if (it.moveToFirst()) {
                    totalCardsWithColors = it.getInt(0)
                }
            }
            Log.d(TAG, "DEBUG: Found $totalCardsWithColors cards with colors $colorIds")
            
            // Find cards that:
            // 1. Have at least one encounter for the selected location (locID = locationId)
            // 2. Have at least one of the selected colors
            // This is a two-step process:
            // Step 1: Find cards that have encounters for this location
            // Step 2: Filter those cards to only include ones with the selected colors
            val query = """
                SELECT DISTINCT 
                    c.$COLUMN_CARD_ID
                FROM $TABLE_CARDS c
                INNER JOIN $TABLE_CARD_TO_ENCOUNTER cte ON c.$COLUMN_CARD_ID = cte.$COLUMN_CTE_CARD_ID
                INNER JOIN $TABLE_ENCOUNTERS e ON cte.$COLUMN_CTE_ENC_ID = e.$COLUMN_ENC_ID
                INNER JOIN $TABLE_CARD_TO_COLOR ctc ON c.$COLUMN_CARD_ID = ctc.$COLUMN_CTC_CARD_ID
                WHERE e.$COLUMN_LOC_ID = ?
                    AND c.$COLUMN_GAME_TYPE = ?
                    AND ctc.$COLUMN_CTC_COLOR_ID IN ($colorIdsStr)
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(locationId.toString(), gameType.value))
            
            val cardIds = mutableSetOf<String>()
            cursor.use {
                val cardIdIndex = it.getColumnIndexOrThrow(COLUMN_CARD_ID)
                while (it.moveToNext()) {
                    cardIds.add(it.getString(cardIdIndex))
                }
            }
            
            Log.d(TAG, "Found ${cardIds.size} unique cards for location $locationId with colors $colorIds: ${cardIds.take(10).joinToString(", ")}${if (cardIds.size > 10) "..." else ""}")
            
            // Now get all encounters for these cards (not just for the selected location)
            // This is because a card can have encounters for multiple locations
            if (cardIds.isEmpty()) {
                return emptyList()
            }
            
            val cardIdsStr = cardIds.joinToString("','", "'", "'")
            val encounterQuery = """
                SELECT DISTINCT 
                    e.$COLUMN_ENC_ID,
                    e.$COLUMN_ENC_TEXT,
                    e.$COLUMN_LOC_ID,
                    cte.$COLUMN_CTE_CARD_ID
                FROM $TABLE_ENCOUNTERS e
                INNER JOIN $TABLE_CARD_TO_ENCOUNTER cte ON e.$COLUMN_ENC_ID = cte.$COLUMN_CTE_ENC_ID
                WHERE cte.$COLUMN_CTE_CARD_ID IN ($cardIdsStr)
                ORDER BY e.$COLUMN_ENC_ID ASC
            """.trimIndent()
            
            val encounterCursor = db.rawQuery(encounterQuery, null)
            val results = mutableListOf<EncounterWithCardData>()
            encounterCursor.use {
                val encIdIndex = it.getColumnIndexOrThrow(COLUMN_ENC_ID)
                val encTextIndex = it.getColumnIndexOrThrow(COLUMN_ENC_TEXT)
                val locIdIndex = it.getColumnIndexOrThrow(COLUMN_LOC_ID)
                val cardIdIndex = it.getColumnIndexOrThrow(COLUMN_CTE_CARD_ID)
                
                while (it.moveToNext()) {
                    results.add(
                        EncounterWithCardData(
                            encounterId = it.getLong(encIdIndex),
                            encounterText = it.getString(encTextIndex),
                            locationId = it.getLong(locIdIndex),
                            cardId = it.getString(cardIdIndex)
                        )
                    )
                }
            }
            
            Log.d(TAG, "Found ${results.size} total encounters for ${cardIds.size} cards matching location $locationId with colors $colorIds")
            return results
        } catch (e: Exception) {
            Log.e(TAG, "Error finding encounters by location and colors: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
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
     * Link a card to a color (used for Arkham Other World cards)
     */
    fun linkCardToColor(cardId: String, colorId: Long) {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_CTC_CARD_ID, cardId)
                put(COLUMN_CTC_COLOR_ID, colorId)
            }
            db.insertWithOnConflict(TABLE_CARD_TO_COLOR, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error linking card $cardId to color $colorId: ${e.message}", e)
        } finally {
            rwLock.writeLock().unlock()
        }
    }

    /**
     * Get colors for a specific card (Arkham Other World helper)
     */
    fun getColorsForCard(cardId: String): List<OtherWorldColorData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase

            val query = """
                SELECT c.$COLUMN_COLOR_ID, c.$COLUMN_COLOR_NAME, c.$COLUMN_COLOR_BUTTON_PATH, c.$COLUMN_COLOR_EXP_ID
                FROM $TABLE_COLORS c
                INNER JOIN $TABLE_CARD_TO_COLOR ctc ON c.$COLUMN_COLOR_ID = ctc.$COLUMN_CTC_COLOR_ID
                WHERE ctc.$COLUMN_CTC_CARD_ID = ?
                ORDER BY c.$COLUMN_COLOR_ID
            """.trimIndent()

            val cursor = db.rawQuery(query, arrayOf(cardId))
            val colors = mutableListOf<OtherWorldColorData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_COLOR_BUTTON_PATH)
                val expIdIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_EXP_ID)

                while (it.moveToNext()) {
                    colors.add(
                        OtherWorldColorData(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            buttonPath = it.getStringOrNull(buttonPathIndex),
                            expId = it.getLong(expIdIndex)
                        )
                    )
                }
            }
            return colors
        } catch (e: Exception) {
            Log.e(TAG, "Error getting colors for card $cardId: ${e.message}", e)
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
    
    /**
     * Data class for Other World Color
     */
    data class OtherWorldColorData(
        val id: Long,
        val name: String,
        val buttonPath: String? = null,
        val expId: Long
    )
    
    /**
     * Get colors for a specific location
     */
    fun getColorsForLocation(locId: Long): List<OtherWorldColorData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            
            // First check if tables exist
            val tableExists = try {
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$TABLE_COLORS'", null).use { it.count > 0 }
            } catch (e: Exception) {
                false
            }
            
            if (!tableExists) {
                Log.w(TAG, "Colors table does not exist yet for location $locId")
                return emptyList()
            }
            
            val query = """
                SELECT c.$COLUMN_COLOR_ID, c.$COLUMN_COLOR_NAME, c.$COLUMN_COLOR_BUTTON_PATH, c.$COLUMN_COLOR_EXP_ID
                FROM $TABLE_COLORS c
                INNER JOIN $TABLE_LOCATION_TO_COLOR ltc ON c.$COLUMN_COLOR_ID = ltc.$COLUMN_LTC_COLOR_ID
                WHERE ltc.$COLUMN_LTC_LOC_ID = ?
                ORDER BY c.$COLUMN_COLOR_ID
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(locId.toString()))
            val colors = mutableListOf<OtherWorldColorData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_COLOR_BUTTON_PATH)
                val expIdIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_EXP_ID)
                
                while (it.moveToNext()) {
                    colors.add(
                        OtherWorldColorData(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            buttonPath = it.getStringOrNull(buttonPathIndex),
                            expId = it.getLong(expIdIndex)
                        )
                    )
                }
            }
            Log.d(TAG, "Found ${colors.size} colors for location $locId: ${colors.map { "${it.name}(ID=${it.id})" }}")
            return colors
        } catch (e: Exception) {
            Log.e(TAG, "Error getting colors for location $locId: ${e.message}", e)
            e.printStackTrace()
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Get all current colors (for selected expansions, including base game)
     */
    fun getCurrentOtherWorldColors(expIds: List<Long>? = null): List<OtherWorldColorData> {
        rwLock.readLock().lock()
        try {
            val db = readableDatabase
            val expIdList = expIds?.joinToString(",") ?: "1"
            val query = """
                SELECT $COLUMN_COLOR_ID, $COLUMN_COLOR_NAME, $COLUMN_COLOR_BUTTON_PATH, $COLUMN_COLOR_EXP_ID
                FROM $TABLE_COLORS
                WHERE ($COLUMN_COLOR_EXP_ID IN ($expIdList) OR $COLUMN_COLOR_EXP_ID = 1) 
                AND $COLUMN_COLOR_ID <> 0
                ORDER BY $COLUMN_COLOR_ID
            """.trimIndent()
            
            val cursor = db.rawQuery(query, null)
            val colors = mutableListOf<OtherWorldColorData>()
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_NAME)
                val buttonPathIndex = it.getColumnIndex(COLUMN_COLOR_BUTTON_PATH)
                val expIdIndex = it.getColumnIndexOrThrow(COLUMN_COLOR_EXP_ID)
                
                while (it.moveToNext()) {
                    colors.add(
                        OtherWorldColorData(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            buttonPath = it.getStringOrNull(buttonPathIndex),
                            expId = it.getLong(expIdIndex)
                        )
                    )
                }
            }
            return colors
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current colors: ${e.message}", e)
            return emptyList()
        } finally {
            rwLock.readLock().unlock()
        }
    }
    
    /**
     * Insert a color
     */
    fun insertColor(colorId: Long, expId: Long, name: String, buttonPath: String? = null): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_COLOR_ID, colorId)
                put(COLUMN_COLOR_EXP_ID, expId)
                put(COLUMN_COLOR_NAME, name)
                buttonPath?.let { put(COLUMN_COLOR_BUTTON_PATH, it) }
            }
            return db.insertWithOnConflict(TABLE_COLORS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting color: ${e.message}", e)
            return -1
        } finally {
            rwLock.writeLock().unlock()
        }
    }
    
    /**
     * Link a location to a color
     */
    fun linkLocationToColor(locId: Long, colorId: Long): Long {
        rwLock.writeLock().lock()
        try {
            val db = writableDatabase
            val values = android.content.ContentValues().apply {
                put(COLUMN_LTC_LOC_ID, locId)
                put(COLUMN_LTC_COLOR_ID, colorId)
            }
            return db.insertWithOnConflict(TABLE_LOCATION_TO_COLOR, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error linking location to color: ${e.message}", e)
            return -1
        } finally {
            rwLock.writeLock().unlock()
        }
    }
}

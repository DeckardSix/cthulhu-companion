package com.poquets.cthulhu.shared.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Utility class to migrate cards from existing Arkham and Eldritch databases
 * into the unified database
 */
object CardMigration {
    private const val TAG = "CardMigration"
    
    /**
     * Migrate cards from Eldritch database
     * @param context Application context
     * @param unifiedDb The unified database helper
     * @param eldritchDbPath Path to existing Eldritch database (optional, will try default location)
     * @return Number of cards migrated
     */
    fun migrateEldritchCards(
        context: Context,
        unifiedDb: UnifiedCardDatabaseHelper,
        eldritchDbPath: String? = null
    ): Int {
        var migratedCount = 0
        try {
            // Try multiple possible locations
            val possiblePaths = mutableListOf<String>()
            
            if (eldritchDbPath != null) {
                possiblePaths.add(eldritchDbPath)
            }
            
            // Default location
            possiblePaths.add(context.getDatabasePath("eldritch_cards.db").absolutePath)
            
            // Try package-specific location (if Eldritch app is installed)
            try {
                val eldritchContext = context.createPackageContext(
                    "com.poquets.eldritch",
                    Context.CONTEXT_IGNORE_SECURITY
                )
                possiblePaths.add(eldritchContext.getDatabasePath("eldritch_cards.db").absolutePath)
            } catch (e: Exception) {
                // Eldritch app not installed, skip
                Log.d(TAG, "Eldritch app not found, skipping package-specific path")
            }
            
            // Find existing database
            var eldritchDbFile: File? = null
            var dbPath: String? = null
            for (path in possiblePaths) {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    eldritchDbFile = file
                    dbPath = path
                    break
                }
            }
            
            if (eldritchDbFile == null || dbPath == null) {
                Log.w(TAG, "Eldritch database not found in any of the checked locations, trying XML migration")
                // Fallback to XML migration if database not found
                return migrateEldritchCardsFromXML(context, unifiedDb)
            }
            
            Log.d(TAG, "Opening Eldritch database at: $dbPath (size: ${eldritchDbFile.length()} bytes)")
            val eldritchDb = SQLiteDatabase.openDatabase(
                dbPath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            try {
                val cursor = eldritchDb.rawQuery(
                    "SELECT * FROM cards",
                    null
                )
                
                val cards = mutableListOf<UnifiedCard>()
                
                val expansionNames = mutableSetOf<String>()
                
                cursor.use {
                    val cardIdIndex = it.getColumnIndexOrThrow("card_id")
                    val regionIndex = it.getColumnIndexOrThrow("region")
                    val expansionIndex = it.getColumnIndex("expansion")
                    val topHeaderIndex = it.getColumnIndex("top_header")
                    val topEncounterIndex = it.getColumnIndex("top_encounter")
                    val middleHeaderIndex = it.getColumnIndex("middle_header")
                    val middleEncounterIndex = it.getColumnIndex("middle_encounter")
                    val bottomHeaderIndex = it.getColumnIndex("bottom_header")
                    val bottomEncounterIndex = it.getColumnIndex("bottom_encounter")
                    val encounteredIndex = it.getColumnIndex("encountered")
                    val cardNameIndex = it.getColumnIndex("card_name")
                    
                    while (it.moveToNext()) {
                        val expansion = if (expansionIndex >= 0) {
                            it.getString(expansionIndex) ?: "BASE"
                        } else "BASE"
                        expansionNames.add(expansion)
                        
                        val card = UnifiedCard(
                            gameType = GameType.ELDRITCH,
                            cardId = it.getString(cardIdIndex),
                            expansion = expansion,
                            cardName = if (cardNameIndex >= 0) it.getString(cardNameIndex) else null,
                            encountered = if (encounteredIndex >= 0) {
                                it.getString(encounteredIndex) ?: "NONE"
                            } else "NONE",
                            region = it.getString(regionIndex),
                            topHeader = if (topHeaderIndex >= 0) it.getString(topHeaderIndex) else null,
                            topEncounter = if (topEncounterIndex >= 0) it.getString(topEncounterIndex) else null,
                            middleHeader = if (middleHeaderIndex >= 0) it.getString(middleHeaderIndex) else null,
                            middleEncounter = if (middleEncounterIndex >= 0) it.getString(middleEncounterIndex) else null,
                            bottomHeader = if (bottomHeaderIndex >= 0) it.getString(bottomHeaderIndex) else null,
                            bottomEncounter = if (bottomEncounterIndex >= 0) it.getString(bottomEncounterIndex) else null
                        )
                        cards.add(card)
                    }
                }
                
                // Create expansions in the database before inserting cards
                Log.d(TAG, "Creating ${expansionNames.size} Eldritch expansions in database...")
                for (expansionName in expansionNames) {
                    unifiedDb.getOrCreateExpansion(GameType.ELDRITCH, expansionName, expansionName, null)
                }
                Log.d(TAG, "Created Eldritch expansions: ${expansionNames.joinToString(", ")}")
                
                // Insert all cards in a batch
                migratedCount = unifiedDb.insertCards(cards)
                
                Log.d(TAG, "Migrated $migratedCount Eldritch cards")
                
            } finally {
                eldritchDb.close()
            }
            
            return migratedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating Eldritch cards: ${e.message}", e)
            return 0
        }
    }
    
    /**
     * Migrate cards from Arkham database
     * Note: Arkham has a more complex structure, so we'll need to join tables
     * @param context Application context
     * @param arkhamDbPath Path to existing Arkham database (optional, will try default location)
     * @return Number of cards migrated
     */
    fun migrateArkhamCards(
        context: Context,
        unifiedDb: UnifiedCardDatabaseHelper,
        arkhamDbPath: String? = null
    ): Int {
        try {
            // Try multiple possible locations
            val possiblePaths = mutableListOf<String>()
            
            if (arkhamDbPath != null) {
                possiblePaths.add(arkhamDbPath)
            }
            
            // Default location
            possiblePaths.add(context.getDatabasePath("ahDB").absolutePath)
            
            // Try package-specific location (if Arkham app is installed)
            try {
                val arkhamContext = context.createPackageContext(
                    "com.poquets.arkham",
                    Context.CONTEXT_IGNORE_SECURITY
                )
                possiblePaths.add(arkhamContext.getDatabasePath("ahDB").absolutePath)
            } catch (e: Exception) {
                // Arkham app not installed, skip
                Log.d(TAG, "Arkham app not found, skipping package-specific path")
            }
            
            // Find existing database
            var arkhamDbFile: File? = null
            var dbPath: String? = null
            for (path in possiblePaths) {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    arkhamDbFile = file
                    dbPath = path
                    break
                }
            }
            
            if (arkhamDbFile == null || dbPath == null) {
                Log.w(TAG, "Arkham database not found in any of the checked locations, populating from scratch")
                // Fallback to direct population if database not found
                return populateArkhamCardsFromScratch(context, unifiedDb)
            }
            
            Log.d(TAG, "Opening Arkham database at: $dbPath (size: ${arkhamDbFile.length()} bytes)")
            val arkhamDb = SQLiteDatabase.openDatabase(
                dbPath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            var migratedCount = 0
            
            try {
                // First, migrate neighborhoods
                migrateArkhamNeighborhoods(arkhamDb, unifiedDb)
                
                // Then, migrate locations
                migrateArkhamLocations(arkhamDb, unifiedDb)
                
                // Then, migrate encounters
                migrateArkhamEncounters(arkhamDb, unifiedDb)
                
                // Finally, migrate cards
                // Arkham cards are linked to neighborhoods, so we need to join
                val cursor = arkhamDb.rawQuery(
                    """
                    SELECT 
                        c.cardID,
                        c.neiID,
                        GROUP_CONCAT(cte.expID) as expansions
                    FROM Card c
                    LEFT JOIN CardToExpansion cte ON c.cardID = cte.cardID
                    GROUP BY c.cardID, c.neiID
                    """.trimIndent(),
                    null
                )
                
                val cards = mutableListOf<UnifiedCard>()
                
            cursor.use {
                val cardIdIndex = it.getColumnIndexOrThrow("cardID")
                val neiIdIndex = it.getColumnIndex("neiID")
                val expansionsIndex = it.getColumnIndex("expansions")
                
                Log.d(TAG, "Cursor has ${it.count} rows")
                
                while (it.moveToNext()) {
                        val cardId = it.getLong(cardIdIndex).toString()
                        val neiId = if (neiIdIndex >= 0 && !it.isNull(neiIdIndex)) {
                            it.getLong(neiIdIndex)
                        } else null
                        
                        // Get expansion(s) - cards can belong to multiple expansions
                        val expansions = if (expansionsIndex >= 0) {
                            it.getString(expansionsIndex) ?: "1"  // Default to base expansion (ID 1)
                        } else "1"
                        
                        // Create card for each expansion it belongs to
                        val expansionList = expansions.split(",").map { it.trim() }
                        
                        for (expId in expansionList) {
                            // Get expansion name
                            val expCursor = arkhamDb.rawQuery(
                                "SELECT expName FROM Expansion WHERE expID = ?",
                                arrayOf(expId)
                            )
                            val expansionName = if (expCursor.moveToFirst()) {
                                val name = expCursor.getString(0) ?: "Base"
                                // Normalize expansion name (Base -> BASE for consistency)
                                if (name.equals("Base", ignoreCase = true)) "BASE" else name
                            } else "BASE"
                            expCursor.close()
                            
                            val card = UnifiedCard(
                                gameType = GameType.ARKHAM,
                                cardId = cardId,
                                expansion = expansionName,
                                neighborhoodId = neiId
                            )
                            cards.add(card)
                        }
                    }
                }
                
                // Insert all cards in a batch
                migratedCount = unifiedDb.insertCards(cards)
                
                Log.d(TAG, "Migrated $migratedCount Arkham cards")
                
                // Link cards to encounters
                linkArkhamCardsToEncounters(arkhamDb, unifiedDb)
                
            } finally {
                arkhamDb.close()
            }
            
            return migratedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating Arkham cards: ${e.message}", e)
            return 0
        }
    }
    
    /**
     * Migrate neighborhoods from Arkham database
     */
    private fun migrateArkhamNeighborhoods(
        arkhamDb: SQLiteDatabase,
        unifiedDb: UnifiedCardDatabaseHelper
    ) {
        try {
            // First, get expansion info
            val expCursor = arkhamDb.rawQuery(
                "SELECT expID, expName, expIconPath FROM Expansion",
                null
            )
            val expansionMap = mutableMapOf<Long, Pair<String, String?>>()
            expCursor.use {
                val idIndex = it.getColumnIndexOrThrow("expID")
                val nameIndex = it.getColumnIndexOrThrow("expName")
                val iconPathIndex = it.getColumnIndex("expIconPath")
                
                while (it.moveToNext()) {
                    val expId = it.getLong(idIndex)
                    val expName = it.getString(nameIndex)
                    val iconPath = it.getStringOrNull(iconPathIndex)
                    expansionMap[expId] = Pair(expName, iconPath)
                }
            }
            
            val cursor = arkhamDb.rawQuery(
                "SELECT NeighborhoodID, Name, ExpansionID, CardPath, ButtonPath FROM Neighborhood",
                null
            )
            
            var count = 0
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow("NeighborhoodID")
                val nameIndex = it.getColumnIndexOrThrow("Name")
                val expIdIndex = it.getColumnIndexOrThrow("ExpansionID")
                val cardPathIndex = it.getColumnIndex("CardPath")
                val buttonPathIndex = it.getColumnIndex("ButtonPath")
                
                while (it.moveToNext()) {
                    val neiId = it.getLong(idIndex)
                    val name = it.getString(nameIndex)
                    val expId = it.getLong(expIdIndex)
                    val cardPath = it.getStringOrNull(cardPathIndex)
                    val buttonPath = it.getStringOrNull(buttonPathIndex)
                    
                    // Get expansion info
                    val (expName, iconPath) = expansionMap[expId] ?: Pair("Base", null)
                    
                    // Normalize expansion name (Base -> BASE for consistency)
                    val normalizedExpName = if (expName.equals("Base", ignoreCase = true)) "BASE" else expName
                    
                    // Get or create expansion in unified DB
                    val unifiedExpId = unifiedDb.getOrCreateExpansion(GameType.ARKHAM, expId.toString(), normalizedExpName, iconPath)
                    
                    if (unifiedExpId > 0) {
                        unifiedDb.insertNeighborhood(neiId, unifiedExpId, name, cardPath, buttonPath)
                        count++
                    }
                }
            }
            
            Log.d(TAG, "Migrated $count neighborhoods")
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating neighborhoods: ${e.message}", e)
        }
    }
    
    /**
     * Migrate locations from Arkham database
     */
    private fun migrateArkhamLocations(
        arkhamDb: SQLiteDatabase,
        unifiedDb: UnifiedCardDatabaseHelper
    ) {
        try {
            // Get expansion info for mapping
            val expCursor = arkhamDb.rawQuery(
                "SELECT expID, expName, expIconPath FROM Expansion",
                null
            )
            val expansionMap = mutableMapOf<Long, Pair<String, String?>>()
            expCursor.use {
                val idIndex = it.getColumnIndexOrThrow("expID")
                val nameIndex = it.getColumnIndexOrThrow("expName")
                val iconPathIndex = it.getColumnIndex("expIconPath")
                
                while (it.moveToNext()) {
                    val expId = it.getLong(idIndex)
                    val expName = it.getString(nameIndex)
                    val iconPath = it.getStringOrNull(iconPathIndex)
                    expansionMap[expId] = Pair(expName, iconPath)
                }
            }
            
            val cursor = arkhamDb.rawQuery(
                "SELECT locID, locName, neiID, locExpID, locButtonPath, sort FROM Location",
                null
            )
            
            var count = 0
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow("locID")
                val nameIndex = it.getColumnIndexOrThrow("locName")
                val neiIdIndex = it.getColumnIndex("neiID")
                val expIdIndex = it.getColumnIndex("locExpID")
                val buttonPathIndex = it.getColumnIndex("locButtonPath")
                val sortIndex = it.getColumnIndex("sort")
                
                while (it.moveToNext()) {
                    val locId = it.getLong(idIndex)
                    val name = it.getString(nameIndex)
                    val neiId = it.getLongOrNull(neiIdIndex)
                    val expId = it.getLongOrNull(expIdIndex)
                    val buttonPath = it.getStringOrNull(buttonPathIndex)
                    val sort = if (sortIndex >= 0 && !it.isNull(sortIndex)) it.getInt(sortIndex) else 0
                    
                    val unifiedExpId = expId?.let {
                        val (expName, iconPath) = expansionMap[it] ?: Pair("Base", null)
                        // Normalize expansion name (Base -> BASE for consistency)
                        val normalizedExpName = if (expName.equals("Base", ignoreCase = true)) "BASE" else expName
                        unifiedDb.getOrCreateExpansion(GameType.ARKHAM, it.toString(), normalizedExpName, iconPath)
                    }
                    
                    if (unifiedExpId == null || unifiedExpId > 0) {
                        unifiedDb.insertLocation(locId, unifiedExpId, neiId, name, buttonPath, sort)
                        count++
                    }
                }
            }
            
            Log.d(TAG, "Migrated $count locations")
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating locations: ${e.message}", e)
        }
    }
    
    /**
     * Migrate colors and location-to-color mappings from Arkham database
     */
    private fun migrateArkhamColors(
        arkhamDb: SQLiteDatabase,
        unifiedDb: UnifiedCardDatabaseHelper
    ) {
        try {
            // Get expansion info for mapping
            val expCursor = arkhamDb.rawQuery(
                "SELECT expID, expName, expIconPath FROM Expansion",
                null
            )
            val expansionMap = mutableMapOf<Long, Pair<String, String?>>()
            expCursor.use {
                val idIndex = it.getColumnIndexOrThrow("expID")
                val nameIndex = it.getColumnIndexOrThrow("expName")
                val iconPathIndex = it.getColumnIndex("expIconPath")
                
                while (it.moveToNext()) {
                    val expId = it.getLong(idIndex)
                    val expName = it.getString(nameIndex)
                    val iconPath = it.getStringOrNull(iconPathIndex)
                    expansionMap[expId] = Pair(expName, iconPath)
                }
            }
            
            // Migrate colors
            val colorCursor = arkhamDb.rawQuery(
                "SELECT colorID, colorExpID, colorName, colorButtonPath FROM Color",
                null
            )
            
            var colorCount = 0
            colorCursor.use {
                val idIndex = it.getColumnIndexOrThrow("colorID")
                val expIdIndex = it.getColumnIndexOrThrow("colorExpID")
                val nameIndex = it.getColumnIndexOrThrow("colorName")
                val buttonPathIndex = it.getColumnIndex("colorButtonPath")
                
                while (it.moveToNext()) {
                    val colorId = it.getLong(idIndex)
                    val expId = it.getLong(expIdIndex)
                    val name = it.getString(nameIndex)
                    val buttonPath = it.getStringOrNull(buttonPathIndex)
                    
                    val unifiedExpId = expansionMap[expId]?.let { (expName, iconPath) ->
                        val normalizedExpName = if (expName.equals("Base", ignoreCase = true)) "BASE" else expName
                        unifiedDb.getOrCreateExpansion(GameType.ARKHAM, expId.toString(), normalizedExpName, iconPath)
                    } ?: 1L // Default to base game if expansion not found
                    
                    if (unifiedExpId > 0) {
                        unifiedDb.insertColor(colorId, unifiedExpId, name, buttonPath)
                        colorCount++
                    }
                }
            }
            
            Log.d(TAG, "Migrated $colorCount colors")
            
            // Migrate location-to-color mappings
            val locToColorCursor = arkhamDb.rawQuery(
                "SELECT locToColorLocID, locToColorColorID FROM LocationToColor",
                null
            )
            
            var mappingCount = 0
            locToColorCursor.use {
                val locIdIndex = it.getColumnIndexOrThrow("locToColorLocID")
                val colorIdIndex = it.getColumnIndexOrThrow("locToColorColorID")
                
                while (it.moveToNext()) {
                    val locId = it.getLong(locIdIndex)
                    val colorId = it.getLong(colorIdIndex)
                    
                    unifiedDb.linkLocationToColor(locId, colorId)
                    mappingCount++
                }
            }
            
            Log.d(TAG, "Migrated $mappingCount location-to-color mappings")
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating colors: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Migrate encounters from Arkham database
     */
    private fun migrateArkhamEncounters(
        arkhamDb: SQLiteDatabase,
        unifiedDb: UnifiedCardDatabaseHelper
    ) {
        try {
            val cursor = arkhamDb.rawQuery(
                "SELECT encID, locID, encText FROM Encounter",
                null
            )
            
            var count = 0
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow("encID")
                val locIdIndex = it.getColumnIndexOrThrow("locID")
                val textIndex = it.getColumnIndexOrThrow("encText")
                
                while (it.moveToNext()) {
                    val encId = it.getLong(idIndex)
                    val locId = it.getLong(locIdIndex)
                    val text = it.getString(textIndex)
                    
                    unifiedDb.insertEncounter(encId, locId, text)
                    count++
                }
            }
            
            Log.d(TAG, "Migrated $count encounters")
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating encounters: ${e.message}", e)
        }
    }
    
    /**
     * Link cards to encounters using CardToEncounter table
     */
    private fun linkArkhamCardsToEncounters(
        arkhamDb: SQLiteDatabase,
        unifiedDb: UnifiedCardDatabaseHelper
    ) {
        try {
            val cursor = arkhamDb.rawQuery(
                "SELECT cardID, encID FROM CardToEncounter",
                null
            )
            
            var count = 0
            cursor.use {
                val cardIdIndex = it.getColumnIndexOrThrow("cardID")
                val encIdIndex = it.getColumnIndexOrThrow("encID")
                
                while (it.moveToNext()) {
                    val cardId = it.getLong(cardIdIndex).toString()
                    val encId = it.getLong(encIdIndex)
                    
                    unifiedDb.linkCardToEncounter(cardId, GameType.ARKHAM, encId)
                    count++
                }
            }
            
            Log.d(TAG, "Linked $count card-encounter relationships")
        } catch (e: Exception) {
            Log.e(TAG, "Error linking cards to encounters: ${e.message}", e)
        }
    }
    
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
     * Migrate all cards from both games
     * @return Pair of (arkhamCount, eldritchCount)
     */
    fun migrateAllCards(
        context: Context,
        unifiedDb: UnifiedCardDatabaseHelper,
        arkhamDbPath: String? = null,
        eldritchDbPath: String? = null
    ): Pair<Int, Int> {
        Log.d(TAG, "Starting migration of all cards...")
        val arkhamCount = migrateArkhamCards(context, unifiedDb, arkhamDbPath)
        val eldritchCount = migrateEldritchCards(context, unifiedDb, eldritchDbPath)
        Log.d(TAG, "Migration complete: $arkhamCount Arkham cards, $eldritchCount Eldritch cards")
        return Pair(arkhamCount, eldritchCount)
    }
    
    /**
     * Migrate Eldritch cards from XML file (fallback when database not found)
     */
    private fun migrateEldritchCardsFromXML(
        context: Context,
        unifiedDb: UnifiedCardDatabaseHelper
    ): Int {
        var migratedCount = 0
        try {
            Log.d(TAG, "Attempting to migrate Eldritch cards from XML...")
            
            // Try to open cards.xml from assets
            val inputStream: InputStream = try {
                context.assets.open("cards.xml")
            } catch (e: Exception) {
                Log.e(TAG, "Could not open cards.xml from assets: ${e.message}")
                return 0
            }
            
            // Parse XML
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)
            doc.documentElement.normalize()
            
            val cards = mutableListOf<UnifiedCard>()
            
            // Load BASE expansion first
            val baseNode = doc.getElementsByTagName("BASE").item(0)
            if (baseNode != null) {
                cards.addAll(parseEldritchExpansion(baseNode, "BASE"))
            }
            
            // Load other expansions
            val expansionNames = listOf(
                "FORSAKEN_LORE", "MOUNTAINS_OF_MADNESS", "STRANGE_REMNANTS",
                "UNDER_THE_PYRAMIDS", "SIGNS_OF_CARCOSA", "THE_DREAMLANDS",
                "CITIES_IN_RUIN", "MASKS_OF_NYARLATHOTEP"
            )
            
            for (expansionName in expansionNames) {
                val expansionNode = doc.getElementsByTagName(expansionName).item(0)
                if (expansionNode != null) {
                    cards.addAll(parseEldritchExpansion(expansionNode, expansionName))
                }
            }
            
            // Create expansions in the database before inserting cards
            val allExpansionNames = mutableSetOf("BASE")
            allExpansionNames.addAll(expansionNames)
            
            Log.d(TAG, "Creating Eldritch expansions in database...")
            for (expansionName in allExpansionNames) {
                unifiedDb.getOrCreateExpansion(GameType.ELDRITCH, expansionName, expansionName, null)
            }
            Log.d(TAG, "Created ${allExpansionNames.size} Eldritch expansions")
            
            // Insert all cards
            if (cards.isNotEmpty()) {
                migratedCount = unifiedDb.insertCards(cards)
                Log.d(TAG, "Migrated $migratedCount Eldritch cards from XML")
            } else {
                Log.w(TAG, "No cards found in XML file")
            }
            
            inputStream.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating Eldritch cards from XML: ${e.message}", e)
        }
        
        return migratedCount
    }
    
    /**
     * Parse an Eldritch expansion from XML node
     */
    private fun parseEldritchExpansion(expansionNode: Node, expansionName: String): List<UnifiedCard> {
        val cards = mutableListOf<UnifiedCard>()
        
        try {
            val childNodes = expansionNode.childNodes
            
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node.nodeType != Node.ELEMENT_NODE) continue
                
                when (node.nodeName) {
                    "LOCATIONS" -> cards.addAll(parseEldritchLocations(node, expansionName))
                    "GATES" -> cards.addAll(parseEldritchNamedCard(node, "GATE", expansionName))
                    "EXPEDITIONS" -> cards.addAll(parseEldritchNamedCard(node, "EXPEDITION", expansionName))
                    "RESEARCH" -> cards.addAll(parseEldritchResearch(node, expansionName))
                    "MYSTIC_RUINS" -> cards.addAll(parseEldritchNamedCard(node, "MYSTIC_RUINS", expansionName))
                    "DREAM-QUEST" -> cards.addAll(parseEldritchNamedCard(node, "DREAM-QUEST", expansionName))
                    "DISASTER" -> cards.addAll(parseEldritchNamedCardNoHeaders(node, "DISASTER", expansionName))
                    "DEVASTATION" -> cards.addAll(parseEldritchSpecial(node, "DEVASTATION", expansionName))
                    "SPECIAL" -> cards.addAll(parseEldritchSpecial(node, expansionName))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing expansion $expansionName: ${e.message}", e)
        }
        
        return cards
    }
    
    /**
     * Parse location cards from XML
     */
    private fun parseEldritchLocations(locationsNode: Node, expansionName: String): List<UnifiedCard> {
        val cards = mutableListOf<UnifiedCard>()
        
        val locationNodes = getChildElements(locationsNode)
        for (locationNode in locationNodes) {
            val regionName = locationNode.nodeName
            val topHeader = getNodeText(getChildElement(locationNode, "TOP_HEADER"))
            val middleHeader = getNodeText(getChildElement(locationNode, "MIDDLE_HEADER"))
            val bottomHeader = getNodeText(getChildElement(locationNode, "BOTTOM_HEADER"))
            
            val cardNodes = getChildElements(locationNode, "CARD")
            for (cardNode in cardNodes) {
                val cardId = cardNode.attributes.getNamedItem("id")?.nodeValue ?: continue
                val topEncounter = getNodeText(getChildElement(cardNode, "TOP"))
                val middleEncounter = getNodeText(getChildElement(cardNode, "MIDDLE"))
                val bottomEncounter = getNodeText(getChildElement(cardNode, "BOTTOM"))
                
                cards.add(
                    UnifiedCard(
                        gameType = GameType.ELDRITCH,
                        cardId = cardId,
                        expansion = expansionName,
                        region = regionName,
                        topHeader = topHeader,
                        topEncounter = topEncounter,
                        middleHeader = middleHeader,
                        middleEncounter = middleEncounter,
                        bottomHeader = bottomHeader,
                        bottomEncounter = bottomEncounter,
                        encountered = "NONE"
                    )
                )
            }
        }
        
        return cards
    }
    
    /**
     * Parse named card deck (GATES, EXPEDITIONS, etc.)
     */
    private fun parseEldritchNamedCard(node: Node, deckName: String, expansionName: String): List<UnifiedCard> {
        val cards = mutableListOf<UnifiedCard>()
        
        val cardNodes = getChildElements(node, "CARD")
        for (cardNode in cardNodes) {
            val cardId = cardNode.attributes.getNamedItem("id")?.nodeValue ?: continue
            val name = getNodeText(getChildElement(cardNode, "NAME"))
            val topEncounter = getNodeText(getChildElement(cardNode, "TOP"))
            val middleEncounter = getNodeText(getChildElement(cardNode, "MIDDLE"))
            val bottomEncounter = getNodeText(getChildElement(cardNode, "BOTTOM"))
            
            cards.add(
                UnifiedCard(
                    gameType = GameType.ELDRITCH,
                    cardId = cardId,
                    expansion = expansionName,
                    region = deckName,
                    topHeader = name,
                    topEncounter = topEncounter,
                    middleHeader = "PASS",
                    middleEncounter = middleEncounter,
                    bottomHeader = "FAIL",
                    bottomEncounter = bottomEncounter,
                    encountered = "NONE"
                )
            )
        }
        
        return cards
    }
    
    /**
     * Parse named card deck without headers (DISASTER, etc.)
     */
    private fun parseEldritchNamedCardNoHeaders(node: Node, deckName: String, expansionName: String): List<UnifiedCard> {
        val cards = mutableListOf<UnifiedCard>()
        
        val cardNodes = getChildElements(node, "CARD")
        for (cardNode in cardNodes) {
            val cardId = cardNode.attributes.getNamedItem("id")?.nodeValue ?: continue
            val topEncounter = getNodeText(getChildElement(cardNode, "TOP"))
            val middleEncounter = getNodeText(getChildElement(cardNode, "MIDDLE"))
            val bottomEncounter = getNodeText(getChildElement(cardNode, "BOTTOM"))
            
            cards.add(
                UnifiedCard(
                    gameType = GameType.ELDRITCH,
                    cardId = cardId,
                    expansion = expansionName,
                    region = deckName,
                    topHeader = null,
                    topEncounter = topEncounter,
                    middleHeader = null,
                    middleEncounter = middleEncounter,
                    bottomHeader = null,
                    bottomEncounter = bottomEncounter,
                    encountered = "NONE"
                )
            )
        }
        
        return cards
    }
    
    /**
     * Parse research cards
     */
    private fun parseEldritchResearch(node: Node, expansionName: String): List<UnifiedCard> {
        val cards = mutableListOf<UnifiedCard>()
        
        // Research cards are organized by Ancient One, we'll load all of them
        val topHeader = getNodeText(getChildElement(node, "TOP_HEADER"))
        val middleHeader = getNodeText(getChildElement(node, "MIDDLE_HEADER"))
        val bottomHeader = getNodeText(getChildElement(node, "BOTTOM_HEADER"))
        
        val childNodes = getChildElements(node)
        for (ancientOneNode in childNodes) {
            if (ancientOneNode.nodeName == "TOP_HEADER" || ancientOneNode.nodeName == "MIDDLE_HEADER" || 
                ancientOneNode.nodeName == "BOTTOM_HEADER") continue
            
            val cardNodes = getChildElements(ancientOneNode, "CARD")
            for (cardNode in cardNodes) {
                val cardId = cardNode.attributes.getNamedItem("id")?.nodeValue ?: continue
                val topEncounter = getNodeText(getChildElement(cardNode, "TOP"))
                val middleEncounter = getNodeText(getChildElement(cardNode, "MIDDLE"))
                val bottomEncounter = getNodeText(getChildElement(cardNode, "BOTTOM"))
                
                cards.add(
                    UnifiedCard(
                        gameType = GameType.ELDRITCH,
                        cardId = cardId,
                        expansion = expansionName,
                        region = "RESEARCH",
                        topHeader = topHeader,
                        topEncounter = topEncounter,
                        middleHeader = middleHeader,
                        middleEncounter = middleEncounter,
                        bottomHeader = bottomHeader,
                        bottomEncounter = bottomEncounter,
                        encountered = "NONE"
                    )
                )
            }
        }
        
        return cards
    }
    
    /**
     * Parse special cards (SPECIAL, DEVASTATION, etc.)
     */
    private fun parseEldritchSpecial(node: Node, expansionName: String): List<UnifiedCard> {
        return parseEldritchSpecial(node, null, expansionName)
    }
    
    private fun parseEldritchSpecial(node: Node, deckName: String?, expansionName: String): List<UnifiedCard> {
        val cards = mutableListOf<UnifiedCard>()
        
        val childNodes = getChildElements(node)
        for (specialNode in childNodes) {
            val regionName = deckName ?: specialNode.nodeName
            val cardNodes = getChildElements(specialNode, "CARD")
            
            for (cardNode in cardNodes) {
                val cardId = cardNode.attributes.getNamedItem("id")?.nodeValue ?: continue
                val topHeader = getNodeText(getChildElement(cardNode, "TOP_HEADER"))
                val topEncounter = getNodeText(getChildElement(cardNode, "TOP"))
                val middleHeader = getNodeText(getChildElement(cardNode, "MIDDLE_HEADER"))
                val middleEncounter = getNodeText(getChildElement(cardNode, "MIDDLE"))
                val bottomHeader = getNodeText(getChildElement(cardNode, "BOTTOM_HEADER"))
                val bottomEncounter = getNodeText(getChildElement(cardNode, "BOTTOM"))
                
                cards.add(
                    UnifiedCard(
                        gameType = GameType.ELDRITCH,
                        cardId = cardId,
                        expansion = expansionName,
                        region = regionName,
                        topHeader = topHeader,
                        topEncounter = topEncounter,
                        middleHeader = middleHeader,
                        middleEncounter = middleEncounter,
                        bottomHeader = bottomHeader,
                        bottomEncounter = bottomEncounter,
                        encountered = "NONE"
                    )
                )
            }
        }
        
        return cards
    }
    
    /**
     * Helper functions for XML parsing
     */
    private fun getChildElements(node: Node): List<Node> {
        val elements = mutableListOf<Node>()
        val childNodes = node.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                elements.add(child)
            }
        }
        return elements
    }
    
    private fun getChildElements(node: Node, tagName: String): List<Node> {
        return getChildElements(node).filter { it.nodeName == tagName }
    }
    
    private fun getChildElement(node: Node, tagName: String): Node? {
        val childNodes = node.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == tagName) {
                return child
            }
        }
        return null
    }
    
    private fun getNodeText(node: Node?): String? {
        if (node == null) return null
        val textContent = node.textContent
        return if (textContent.isNullOrBlank()) null else textContent.trim()
    }
    
    /**
     * Populate Arkham cards from scratch (when database not found)
     * Creates a temporary database, populates it using ArkhamInit, then migrates to unified DB
     */
    private fun populateArkhamCardsFromScratch(
        context: Context,
        unifiedDb: UnifiedCardDatabaseHelper
    ): Int {
        var migratedCount = 0
        var tempDb: SQLiteDatabase? = null
        
        try {
            Log.d(TAG, "Populating Arkham cards from scratch using ArkhamInit...")
            
            // Delete any existing temporary database first
            val tempDbName = "temp_arkham.db"
            try {
                context.deleteDatabase(tempDbName)
                Log.d(TAG, "Deleted existing temporary database if it existed")
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete existing database: ${e.message}")
            }
            
            // Use context.openOrCreateDatabase which handles file permissions and setup better
            Log.d(TAG, "Creating temporary database: $tempDbName")
            tempDb = context.openOrCreateDatabase(tempDbName, Context.MODE_PRIVATE, null)
            
            // Create the same schema as Arkham's DatabaseHelper first
            Log.d(TAG, "Creating temporary database schema...")
            createTempArkhamSchema(tempDb)
            
            // Disable foreign key checking during population (we'll validate after)
            // This allows us to insert data in any order without constraint violations
            try {
                tempDb.execSQL("PRAGMA foreign_keys = OFF")
                Log.d(TAG, "Foreign keys disabled for data population")
            } catch (e: Exception) {
                Log.w(TAG, "Could not disable foreign keys: ${e.message}")
            }
            
            // Use transactions for all inserts to improve performance and ensure atomicity
            tempDb.beginTransaction()
            try {
                // Use ArkhamInit to populate the temporary database
                Log.d(TAG, "Calling ArkhamInit.FetchExpansion...")
                ArkhamInit.FetchExpansion(tempDb)
                Log.d(TAG, "FetchExpansion completed")
                
                Log.d(TAG, "Calling ArkhamInit.FetchNeighborhoods...")
                ArkhamInit.FetchNeighborhoods(tempDb)
                Log.d(TAG, "FetchNeighborhoods completed")
                
                Log.d(TAG, "Calling ArkhamInit.FetchLocations...")
                ArkhamInit.FetchLocations(tempDb)
                Log.d(TAG, "FetchLocations completed")
                
                Log.d(TAG, "Calling ArkhamInit.FetchColors...")
                ArkhamInit.FetchColors(tempDb)
                Log.d(TAG, "FetchColors completed")
                
                Log.d(TAG, "Calling ArkhamInit.FetchEncounters...")
                ArkhamInit.FetchEncounters(tempDb)
                Log.d(TAG, "FetchEncounters completed")
                
                Log.d(TAG, "Calling ArkhamInit.FetchOtherWorldLocations...")
                ArkhamInit.FetchOtherWorldLocations(tempDb)
                Log.d(TAG, "FetchOtherWorldLocations completed")
                
                Log.d(TAG, "Calling ArkhamInit.FetchOtherWorldEncounter...")
                ArkhamInit.FetchOtherWorldEncounter(tempDb)
                Log.d(TAG, "FetchOtherWorldEncounter completed")
                
                tempDb.setTransactionSuccessful()
                Log.d(TAG, "Transaction committed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during transaction: ${e.message}", e)
                throw e
            } finally {
                tempDb.endTransaction()
            }
            
            // Re-enable foreign keys after data is inserted (for validation)
            try {
                tempDb.execSQL("PRAGMA foreign_keys = ON")
                Log.d(TAG, "Foreign keys re-enabled for validation")
            } catch (e: Exception) {
                Log.w(TAG, "Could not re-enable foreign keys: ${e.message}")
            }
            
            // Verify data was inserted
            val cardCount = tempDb.rawQuery("SELECT COUNT(*) FROM Card", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            Log.d(TAG, "Temporary database contains $cardCount cards")
            
            val expansionCount = tempDb.rawQuery("SELECT COUNT(*) FROM Expansion", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            Log.d(TAG, "Temporary database contains $expansionCount expansions")
            
            val cardToExpCount = tempDb.rawQuery("SELECT COUNT(*) FROM CardToExpansion", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            Log.d(TAG, "Temporary database contains $cardToExpCount CardToExpansion entries")
            
            if (cardCount == 0) {
                Log.e(TAG, "ERROR: Temporary database has no cards! ArkhamInit.FetchEncounters may have failed.")
                return 0
            }
            
            // Now migrate from the temporary database to the unified database
            Log.d(TAG, "Starting migration from temporary database...")
            migratedCount = migrateArkhamCardsFromDatabase(tempDb, unifiedDb)
            
            Log.d(TAG, "Populated $migratedCount Arkham cards from scratch")
            
            // Verify cards were inserted into unified DB
            val unifiedCardCount = unifiedDb.getCardCount(GameType.ARKHAM)
            Log.d(TAG, "Unified database now contains $unifiedCardCount Arkham cards")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error populating Arkham cards from scratch: ${e.message}", e)
            e.printStackTrace()
            // Log more details about the error
            Log.e(TAG, "Exception type: ${e.javaClass.name}")
            if (e.cause != null) {
                Log.e(TAG, "Caused by: ${e.cause?.message}", e.cause)
            }
        } finally {
            // Close database before deleting file
            try {
                tempDb?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing temporary database: ${e.message}")
            }
            
            // Clean up temporary database
            try {
                // Wait a bit to ensure database is fully closed
                Thread.sleep(100)
                val deleted = context.deleteDatabase("temp_arkham.db")
                if (deleted) {
                    Log.d(TAG, "Temporary database deleted successfully")
                } else {
                    Log.w(TAG, "Could not delete temporary database (may not have existed)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete temporary database: ${e.message}")
            }
        }
        
        return migratedCount
    }
    
    /**
     * Create temporary Arkham database schema matching the original
     */
    private fun createTempArkhamSchema(db: SQLiteDatabase) {
        // Expansions
        db.execSQL("CREATE TABLE IF NOT EXISTS Expansion (expID INTEGER PRIMARY KEY, expName TEXT, expIconPath TEXT)")
        
        // Neighborhoods
        db.execSQL("CREATE TABLE IF NOT EXISTS Neighborhood (NeighborhoodID INTEGER PRIMARY KEY, ExpansionID INTEGER NOT NULL, Name TEXT, CardPath TEXT, ButtonPath TEXT, FOREIGN KEY (ExpansionID) REFERENCES Expansion (expID))")
        
        // Locations
        db.execSQL("CREATE TABLE IF NOT EXISTS Location (locID INTEGER PRIMARY KEY, locExpID INTEGER NULL, locName TEXT, neiID INTEGER NULL, locButtonPath TEXT NULL, sort INTEGER NOT NULL, FOREIGN KEY (locExpID) REFERENCES Expansion (expID), FOREIGN KEY (neiID) REFERENCES Neighborhood (NeighborhoodID))")
        
        // Encounters
        db.execSQL("CREATE TABLE IF NOT EXISTS Encounter (encID INTEGER PRIMARY KEY, encText TEXT, locID INTEGER NOT NULL, FOREIGN KEY (locID) REFERENCES Location (locID))")
        
        // Cards
        db.execSQL("CREATE TABLE IF NOT EXISTS Card (cardID INTEGER PRIMARY KEY, neiID INTEGER NULL, FOREIGN KEY (neiID) REFERENCES Neighborhood (NeighborhoodID))")
        
        // CardToExpansion
        db.execSQL("CREATE TABLE IF NOT EXISTS CardToExpansion (cardID INTEGER NOT NULL, expID INTEGER NOT NULL, FOREIGN KEY (cardID) REFERENCES Card (cardID), FOREIGN KEY (expID) REFERENCES Expansion (expID))")
        
        // CardToEncounter
        db.execSQL("CREATE TABLE IF NOT EXISTS CardToEncounter (cardID INTEGER NOT NULL, encID INTEGER NOT NULL, FOREIGN KEY (cardID) REFERENCES Card (cardID), FOREIGN KEY (encID) REFERENCES Encounter (encID))")
        
        // Colors (for Other World)
        db.execSQL("CREATE TABLE IF NOT EXISTS Color (colorID INTEGER PRIMARY KEY, colorExpID INTEGER NOT NULL, colorName TEXT, colorButtonPath TEXT, FOREIGN KEY (colorExpID) REFERENCES Expansion (expID))")
        
        // Paths
        db.execSQL("CREATE TABLE IF NOT EXISTS Path (pathID INTEGER PRIMARY KEY, path TEXT)")
        
        // ColorToPath
        db.execSQL("CREATE TABLE IF NOT EXISTS ColorToPath (colorToPathColorID INTEGER NOT NULL, colorToPathPathID INTEGER NOT NULL, FOREIGN KEY (colorToPathColorID) REFERENCES Color (colorID), FOREIGN KEY (colorToPathPathID) REFERENCES Path (pathID))")
        
        // LocationToColor
        db.execSQL("CREATE TABLE IF NOT EXISTS LocationToColor (locToColorLocID INTEGER NOT NULL, locToColorColorID INTEGER NOT NULL, FOREIGN KEY (locToColorLocID) REFERENCES Location (locID), FOREIGN KEY (locToColorColorID) REFERENCES Color (colorID))")
        
        // CardToColor
        db.execSQL("CREATE TABLE IF NOT EXISTS CardToColor (cardToColorCardID INTEGER NOT NULL, cardToColorColorID INTEGER NOT NULL, FOREIGN KEY (cardToColorCardID) REFERENCES Card (cardID), FOREIGN KEY (cardToColorColorID) REFERENCES Color (colorID))")
    }
    
    /**
     * Migrate from temporary Arkham database to unified database
     * Reuses existing migration functions
     */
    private fun migrateArkhamCardsFromDatabase(
        tempDb: SQLiteDatabase,
        unifiedDb: UnifiedCardDatabaseHelper
    ): Int {
        var migratedCount = 0
        
        try {
            // First, migrate neighborhoods
            migrateArkhamNeighborhoods(tempDb, unifiedDb)
            
            // Then, migrate locations
            migrateArkhamLocations(tempDb, unifiedDb)
            
            // Then, migrate encounters
            migrateArkhamEncounters(tempDb, unifiedDb)
            
            // Migrate colors and location-to-color mappings
            migrateArkhamColors(tempDb, unifiedDb)
            
            // Finally, migrate cards (reuse existing logic)
            Log.d(TAG, "Querying cards from temporary database...")
            val cursor = tempDb.rawQuery(
                """
                SELECT 
                    c.cardID,
                    c.neiID,
                    GROUP_CONCAT(cte.expID) as expansions
                FROM Card c
                LEFT JOIN CardToExpansion cte ON c.cardID = cte.cardID
                GROUP BY c.cardID, c.neiID
                """.trimIndent(),
                null
            )
            
            val cards = mutableListOf<UnifiedCard>()
            
            cursor.use {
                val cardIdIndex = it.getColumnIndexOrThrow("cardID")
                val neiIdIndex = it.getColumnIndex("neiID")
                val expansionsIndex = it.getColumnIndex("expansions")
                
                Log.d(TAG, "Cursor has ${it.count} rows")
                
                while (it.moveToNext()) {
                    val cardId = it.getLong(cardIdIndex).toString()
                    val neiId = if (neiIdIndex >= 0 && !it.isNull(neiIdIndex)) {
                        it.getLong(neiIdIndex)
                    } else null
                    
                    // Get expansion(s) - cards can belong to multiple expansions
                    val expansions = if (expansionsIndex >= 0) {
                        it.getString(expansionsIndex) ?: "1"  // Default to base expansion (ID 1)
                    } else "1"
                    
                    // Create card for each expansion it belongs to
                    val expansionList = expansions.split(",").map { it.trim() }
                    
                    for (expId in expansionList) {
                        // Get expansion name
                        val expCursor = tempDb.rawQuery(
                            "SELECT expName FROM Expansion WHERE expID = ?",
                            arrayOf(expId)
                        )
                        val expansionName = if (expCursor.moveToFirst()) {
                            val name = expCursor.getString(0) ?: "Base"
                            // Normalize expansion name (Base -> BASE for consistency)
                            if (name.equals("Base", ignoreCase = true)) "BASE" else name
                        } else "BASE"
                        expCursor.close()
                        
                        val card = UnifiedCard(
                            gameType = GameType.ARKHAM,
                            cardId = cardId,
                            expansion = expansionName,
                            neighborhoodId = neiId
                        )
                        cards.add(card)
                    }
                }
            }
            
            // Log card creation details
            Log.d(TAG, "Created ${cards.size} UnifiedCard objects from temporary database")
            if (cards.isEmpty()) {
                Log.w(TAG, "WARNING: No cards were created! Checking temporary database...")
                // Check if there are any cards in the temp DB
                val cardCountCheck = tempDb.rawQuery("SELECT COUNT(*) FROM Card", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
                Log.w(TAG, "Temporary database contains $cardCountCheck cards in Card table")
                
                // Check CardToExpansion
                val cardToExpCount = tempDb.rawQuery("SELECT COUNT(*) FROM CardToExpansion", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
                Log.w(TAG, "Temporary database contains $cardToExpCount entries in CardToExpansion table")
            } else {
                // Log first few cards as sample
                cards.take(3).forEach { card ->
                    Log.d(TAG, "Sample card: ID=${card.cardId}, Expansion=${card.expansion}, Neighborhood=${card.neighborhoodId}, GameType=${card.gameType}")
                }
            }
            
            // Insert all cards in a batch
            if (cards.isNotEmpty()) {
                Log.d(TAG, "Attempting to insert ${cards.size} cards into unified database...")
                migratedCount = unifiedDb.insertCards(cards)
                Log.d(TAG, "Successfully inserted $migratedCount out of ${cards.size} cards")
                
                if (migratedCount == 0 && cards.isNotEmpty()) {
                    Log.e(TAG, "ERROR: No cards were inserted despite having ${cards.size} cards to insert!")
                    // Try inserting one card to see the error
                    try {
                        val testCard = cards.first()
                        Log.d(TAG, "Attempting to insert test card: ${testCard.cardId}, ${testCard.gameType}, ${testCard.expansion}")
                        val result = unifiedDb.insertCard(testCard)
                        Log.d(TAG, "Test card insert result: $result")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inserting test card: ${e.message}", e)
                    }
                }
            } else {
                Log.e(TAG, "ERROR: Cannot insert cards - cards list is empty!")
            }
            
            // Link cards to encounters
            linkArkhamCardsToEncounters(tempDb, unifiedDb)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating from temporary database: ${e.message}", e)
            e.printStackTrace()
        }
        
        return migratedCount
    }
}


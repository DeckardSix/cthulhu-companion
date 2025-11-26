package com.poquets.cthulhu.arkham

import android.content.Context
import android.util.Log
import com.poquets.cthulhu.shared.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Compatibility adapter for AHFlyweightFactory singleton
 * Provides methods compatible with existing Arkham AHFlyweightFactory API
 */
class AHFlyweightFactoryAdapter private constructor(context: Context) {
    
    private val appContext: Context = context.applicationContext
    private val repository = CardRepository(appContext)
    private val deckAdapter = ArkhamDeckAdapter(appContext)
    private val gameState = GameStateManager.getInstance(appContext)
    
    // Cache for expansions
    private var expansionMap: ConcurrentHashMap<Long, ExpansionAdapter>? = null
    
    companion object {
        @Volatile
        private var INSTANCE: AHFlyweightFactoryAdapter? = null
        private val lock = Any()
        
        fun getInstance(context: Context): AHFlyweightFactoryAdapter {
            return INSTANCE ?: synchronized(lock) {
                INSTANCE ?: AHFlyweightFactoryAdapter(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    /**
     * Initialize the factory (compatible with AHFlyweightFactory.Init())
     */
    fun Init(context: Context) {
        // Already initialized via getInstance
        Log.d("AHFlyweightFactoryAdapter", "Initialized")
    }
    
    /**
     * Clear the expansion cache to force refresh
     */
    fun clearExpansionCache() {
        expansionMap = null
        Log.d("AHFlyweightFactoryAdapter", "Expansion cache cleared")
    }
    
    /**
     * Get all expansions (compatible with AHFlyweightFactory.getExpansions())
     */
    fun getExpansions(): List<ExpansionAdapter> {
        if (expansionMap == null) {
            expansionMap = ConcurrentHashMap()
            
            // Get expansions from unified database
            val expansions = runBlocking {
                repository.getExpansionsForGameType(GameType.ARKHAM)
            }
            
            // Map expansion names to IDs (matching Init.java exactly)
            // ID 1 = Base
            // ID 2 = Curse of the Dark Pharoah (note: typo "Pharoah" not "Pharaoh")
            // ID 3 = Dunwich Horror
            // ID 4 = The King in Yellow
            // ID 5 = Kingsport Horror
            // ID 6 = Black Goat of the Woods
            // ID 7 = Innsmouth Horror
            // ID 8 = Lurker at the Threshold
            // ID 9 = Curse of the Dark Pharoah Revised
            // ID 10 = Miskatonic Horror
            val expansionIdMap = mapOf(
                "BASE" to 1L,
                "Base" to 1L,
                "Base Game" to 1L,
                "Curse of the Dark Pharoah" to 2L, // Note: typo in original ("Pharoah" not "Pharaoh")
                "Curse of the Dark Pharaoh" to 2L, // Handle both spellings
                "Dunwich Horror" to 3L,
                "The King in Yellow" to 4L,
                "King in Yellow" to 4L,
                "Kingsport Horror" to 5L,
                "Black Goat of the Woods" to 6L,
                "The Black Goat of the Woods" to 6L,
                "Innsmouth Horror" to 7L,
                "Lurker at the Threshold" to 8L,
                "The Lurker at the Threshold" to 8L,
                "Curse of the Dark Pharoah Revised" to 9L,
                "Curse of the Dark Pharaoh Revised" to 9L, // Handle both spellings
                "Miskatonic Horror" to 10L
            )
            
            // Create ExpansionAdapter for each expansion found in database
            for (expansionName in expansions) {
                val expID = expansionIdMap[expansionName]
                if (expID != null) {
                    // Use the original name from database, but map to correct ID
                    expansionMap!![expID] = ExpansionAdapter(expID, expansionName, null)
                } else {
                    // If name doesn't match, try to find by partial match or use a default ID
                    android.util.Log.w("AHFlyweightFactoryAdapter", "Unknown expansion name: $expansionName")
                    // Try to match by partial name
                    val matchedId = expansionIdMap.entries.find { 
                        expansionName.contains(it.key, ignoreCase = true) || 
                        it.key.contains(expansionName, ignoreCase = true)
                    }?.value
                    if (matchedId != null) {
                        expansionMap!![matchedId] = ExpansionAdapter(matchedId, expansionName, null)
                    }
                }
            }
            
            // Always include base game - use "Base" as the display name (matching ArkhamInit)
            if (!expansionMap!!.containsKey(1L)) {
                expansionMap!![1L] = ExpansionAdapter(1L, "Base", null)
            }
            
            // Ensure all 10 expansions are present (even if not in database yet)
            // This matches Init.java which creates all 10 expansions
            val allExpansions = mapOf(
                1L to "Base",
                2L to "Curse of the Dark Pharoah",
                3L to "Dunwich Horror",
                4L to "The King in Yellow",
                5L to "Kingsport Horror",
                6L to "Black Goat of the Woods",
                7L to "Innsmouth Horror",
                8L to "Lurker at the Threshold",
                9L to "Curse of the Dark Pharoah Revised",
                10L to "Miskatonic Horror"
            )
            
            for ((expId, expName) in allExpansions) {
                if (!expansionMap!!.containsKey(expId)) {
                    expansionMap!![expId] = ExpansionAdapter(expId, expName, null)
                    android.util.Log.d("AHFlyweightFactoryAdapter", "Added missing expansion: $expName (ID=$expId)")
                }
            }
        }
        
        val result = expansionMap!!.values.toList().sortedBy { it.getID() }
        android.util.Log.d("AHFlyweightFactoryAdapter", "Returning ${result.size} expansions: ${result.map { "${it.getName()}(ID=${it.getID()})" }}")
        return result
    }
    
    /**
     * Get expansion by ID (compatible with AHFlyweightFactory.getExpansion())
     */
    fun getExpansion(expID: Long): ExpansionAdapter? {
        getExpansions() // Ensure map is populated
        return expansionMap?.get(expID)
    }
    
    /**
     * Get current neighborhoods (compatible with AHFlyweightFactory.getCurrentNeighborhoods())
     * Returns neighborhoods for enabled expansions
     */
    fun getCurrentNeighborhoods(): List<NeighborhoodAdapter> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val enabledExpansions = gameState.getSelectedExpansions(GameType.ARKHAM)
                
                // Get neighborhoods for enabled expansions
                val allNeighborhoods = mutableListOf<NeighborhoodAdapter>()
                for (expansion in enabledExpansions) {
                    // Find expansion ID in unified DB
                    val expId = getExpansionIdByName(expansion)
                    if (expId > 0) {
                        val neighborhoods = db.getNeighborhoods(expId)
                        neighborhoods.forEach { nei ->
                            allNeighborhoods.add(
                                NeighborhoodAdapter(
                                    nei.id,
                                    nei.name,
                                    nei.cardPath,
                                    nei.buttonPath
                                )
                            )
                        }
                    }
                }
                
                allNeighborhoods
            }
        }
    }
    
    /**
     * Get neighborhood by ID (compatible with AHFlyweightFactory.getNeighborhood())
     */
    fun getNeighborhood(neiID: Long): NeighborhoodAdapter? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                // Search all expansions for this neighborhood
                val expansions = getExpansions()
                for (expansion in expansions) {
                    val neighborhoods = db.getNeighborhoods(expansion.getID())
                    neighborhoods.find { it.id == neiID }?.let { nei ->
                        return@withContext NeighborhoodAdapter(
                            nei.id,
                            nei.name,
                            nei.cardPath,
                            nei.buttonPath
                        )
                    }
                }
                null
            }
        }
    }
    
    /**
     * Helper to get expansion ID by name
     */
    private fun getExpansionIdByName(expansionName: String): Long {
        val expansion = getExpansions().find { it.getName() == expansionName }
        return expansion?.getID() ?: 1L // Default to base game
    }
    
    /**
     * Get current neighborhood cards (compatible with AHFlyweightFactory.getCurrentNeighborhoodsCards())
     */
    suspend fun getCurrentNeighborhoodsCards(neiID: Long): List<NeighborhoodCardAdapter> = withContext(Dispatchers.IO) {
        val cards = deckAdapter.getNeighborhoodCards(neiID)
        cards.map { 
            NeighborhoodCardAdapter(
                it.cardId.toLongOrNull() ?: 0L,
                neiID,
                it,
                appContext
            )
        }
    }
    
    /**
     * Get current other world cards (compatible with AHFlyweightFactory.getCurrentOtherWorldCards())
     */
    suspend fun getCurrentOtherWorldCards(): List<OtherWorldCardAdapter> = withContext(Dispatchers.IO) {
        val cards = deckAdapter.getOtherWorldCards()
        cards.map {
            OtherWorldCardAdapter(
                it.cardId.toLongOrNull() ?: 0L,
                it,
                appContext
            )
        }
    }
    
    /**
     * Get location by ID (compatible with AHFlyweightFactory.getLocation())
     */
    fun getLocation(locID: Long): LocationAdapter? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                
                // First, try to find in neighborhood locations
                val neighborhoods = getCurrentNeighborhoods()
                for (neighborhood in neighborhoods) {
                    val locations = db.getLocationsByNeighborhood(neighborhood.getID())
                    locations.find { it.id == locID }?.let { loc ->
                        android.util.Log.d("AHFlyweightFactoryAdapter", "Found location ${loc.id}: ${loc.name} in neighborhood ${neighborhood.getID()}")
                        return@withContext LocationAdapter(
                            loc.id,
                            loc.name,
                            loc.buttonPath,
                            getOtherWorldColorsFunc = null // Neighborhood locations don't have otherworld colors
                        )
                    }
                }
                
                // If not found in neighborhoods, check otherworld locations
                // For otherworld locations, we need to search ALL expansions, not just enabled ones
                // because encounters might reference locations from any expansion
                val allExpansions = getExpansions()
                val allExpansionIds = allExpansions.map { it.getID() }
                
                val otherWorldLocations = db.getOtherWorldLocations(allExpansionIds, listOf(499L, 500L))
                android.util.Log.d("AHFlyweightFactoryAdapter", "Searching for location $locID in ${otherWorldLocations.size} otherworld locations (checked ${allExpansionIds.size} expansions)")
                
                otherWorldLocations.find { it.id == locID }?.let { loc ->
                    android.util.Log.d("AHFlyweightFactoryAdapter", "Found otherworld location ${loc.id}: ${loc.name}")
                    return@withContext LocationAdapter(
                        loc.id,
                        loc.name,
                        loc.buttonPath,
                        getOtherWorldColorsFunc = { getOtherWorldColors(loc.id) }
                    )
                }
                
                // If still not found, try a direct query without expansion filtering
                val directLocation = db.getLocationById(locID)
                if (directLocation != null) {
                    android.util.Log.d("AHFlyweightFactoryAdapter", "Found location ${directLocation.id}: ${directLocation.name} via direct query")
                    return@withContext LocationAdapter(
                        directLocation.id,
                        directLocation.name,
                        directLocation.buttonPath,
                        getOtherWorldColorsFunc = if (directLocation.neiId == null) { 
                            { getOtherWorldColors(directLocation.id) } 
                        } else null
                    )
                }
                
                android.util.Log.w("AHFlyweightFactoryAdapter", "Location $locID not found anywhere")
                null
            }
        }
    }
    
    /**
     * Get current otherworld locations (compatible with AHFlyweightFactory.getCurrentOtherWorldLocations())
     * Returns locations without neighborhoods, filtered by enabled expansions
     */
    fun getCurrentOtherWorldLocations(): List<LocationAdapter> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val enabledExpansions = gameState.getSelectedExpansions(GameType.ARKHAM)
                
                // Map expansion names to IDs
                val expansionIds = mutableListOf<Long>()
                for (expansionName in enabledExpansions) {
                    val expId = getExpansionIdByName(expansionName)
                    if (expId > 0) {
                        expansionIds.add(expId)
                    }
                }
                
                // Always include base game (ID 1)
                if (!expansionIds.contains(1L)) {
                    expansionIds.add(1L)
                }
                
                // Get otherworld locations filtered by enabled expansions
                // Base game (ID 1) is always included, and locations 499 and 500 are excluded
                val locations = db.getOtherWorldLocations(expansionIds, listOf(499L, 500L))
                
                android.util.Log.d("AHFlyweightFactoryAdapter", "Found ${locations.size} otherworld locations for expansions: $expansionIds")
                if (locations.isEmpty()) {
                    // Try without expansion filtering to see if any locations exist
                    val allLocations = db.getOtherWorldLocations(null, listOf(499L, 500L))
                    android.util.Log.w("AHFlyweightFactoryAdapter", "No locations found with expansion filter. Total otherworld locations (excluding 499,500): ${allLocations.size}")
                    if (allLocations.isNotEmpty()) {
                        android.util.Log.d("AHFlyweightFactoryAdapter", "Sample locations: ${allLocations.take(5).map { "${it.name}(ID=${it.id}, expId=${it.expId})" }.joinToString(", ")}")
                    }
                } else {
                    android.util.Log.d("AHFlyweightFactoryAdapter", "Location names: ${locations.map { "${it.name}(ID=${it.id}, expId=${it.expId})" }.joinToString(", ")}")
                }
                
                locations.map { loc ->
                    LocationAdapter(
                        loc.id,
                        loc.name,
                        loc.buttonPath,
                        getOtherWorldColorsFunc = { getOtherWorldColors(loc.id) }
                    )
                }
            }
        }
    }
    
    /**
     * Get otherworld card path for colored card (compatible with AHFlyweightFactory.getOtherWorldCardPathForColoredCard())
     * This finds the card image path based on the card's colors matching a path's colors
     */
    fun getOtherWorldCardPathForColoredCard(cardID: Long): String? {
        // Full color-to-path mapping via Path/ColorToPath tables is not yet implemented in the unified DB.
        // For now, always return a generic encounter card template that exists in this app's assets so
        // other world cards render with a proper frame instead of a plain color background.
        android.util.Log.d("AHFlyweightFactoryAdapter", "Using generic encounter card template for otherworld card $cardID")
        return "encounter/encounter_front_downtown.png"
    }
    
    /**
     * Get otherworld colors for a location (compatible with AHFlyweightFactory.getOtherWorldColors())
     */
    fun getOtherWorldColors(locID: Long): List<OtherWorldColorAdapter> {
        return runBlocking {
            try {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val colors = db.getColorsForLocation(locID)
                colors.map { colorData ->
                    OtherWorldColorAdapter(
                        id = colorData.id,
                        name = colorData.name,
                        buttonPath = colorData.buttonPath,
                        expId = colorData.expId
                    )
                }
            } catch (e: Exception) {
                Log.e("AHFlyweightFactoryAdapter", "Error getting colors for location $locID: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get current otherworld colors (compatible with AHFlyweightFactory.getCurrentOtherWorldColors())
     */
    fun getCurrentOtherWorldColors(): List<OtherWorldColorAdapter> {
        return runBlocking {
            try {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                // Get applied expansion IDs
                val gameState = GameStateAdapter.getInstance(appContext)
                val expIds = gameState.getAppliedExpansions().toList()
                val colors = db.getCurrentOtherWorldColors(if (expIds.isNotEmpty()) expIds else listOf(1L))
                colors.map { colorData ->
                    OtherWorldColorAdapter(
                        id = colorData.id,
                        name = colorData.name,
                        buttonPath = colorData.buttonPath,
                        expId = colorData.expId
                    )
                }
            } catch (e: Exception) {
                Log.e("AHFlyweightFactoryAdapter", "Error getting current colors: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get encounters for a card (compatible with AHFlyweightFactory.getEncountersForCard())
     */
    fun getEncountersForCard(cardID: Long): List<EncounterAdapter> {
        // TODO: Get encounters from unified database
        return emptyList()
    }
    
    /**
     * Get expansion IDs for a card (compatible with AHFlyweightFactory.getExpansionsForCard())
     */
    fun getExpansionsForCard(cardID: Long): List<Long> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val card = db.getCard(GameType.ARKHAM, cardID.toString())
                
                if (card != null) {
                    val expansionName = card.expansion
                    val expansion = getExpansions().find { it.getName() == expansionName }
                    if (expansion != null) {
                        listOf(expansion.getID())
                    } else {
                        listOf(1L) // Default to base game
                    }
                } else {
                    emptyList()
                }
            }
        }
    }
}

/**
 * Compatibility adapter for Location class
 */
class LocationAdapter(
    private val id: Long,
    private val name: String,
    private val buttonPath: String? = null,
    private val getOtherWorldColorsFunc: (() -> List<OtherWorldColorAdapter>)? = null
) {
    fun getID(): Long = id
    fun getLocationName(): String = name
    fun getButtonPath(): String? = buttonPath
    fun getEncounters(): List<EncounterAdapter> = emptyList() // TODO: Implement
    fun getOtherWorldColors(): List<OtherWorldColorAdapter> {
        return getOtherWorldColorsFunc?.invoke() ?: emptyList()
    }
    override fun toString(): String = name
}

/**
 * Compatibility adapter for OtherWorldColor class
 */
class OtherWorldColorAdapter(
    private val id: Long,
    private val name: String,
    private val buttonPath: String? = null,
    private val expId: Long = 1L
) {
    fun getID(): Long = id
    fun getName(): String = name
    fun getButtonPath(): String? = buttonPath
    fun getExpID(): Long = expId
    override fun toString(): String = name
}

/**
 * Compatibility adapter for Encounter class
 */
class EncounterAdapter(
    private val id: Long,
    private val encounterText: String,
    private val location: LocationAdapter? = null
) {
    fun getID(): Long = id
    fun getLocID(): Long = location?.getID() ?: 0L
    fun getEncounterText(): String = encounterText
    fun getLocation(): LocationAdapter? = location
}


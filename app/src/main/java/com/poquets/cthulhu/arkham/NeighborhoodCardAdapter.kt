package com.poquets.cthulhu.arkham

import android.content.Context
import com.poquets.cthulhu.shared.database.GameType
import com.poquets.cthulhu.shared.database.UnifiedCard
import com.poquets.cthulhu.shared.database.UnifiedCardDatabaseHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Compatibility adapter for Arkham NeighborhoodCard class
 */
class NeighborhoodCardAdapter(
    private val cardId: Long,
    private val neighborhoodId: Long,
    private val unifiedCard: UnifiedCard? = null,
    private val context: Context? = null
) {
    
    fun getID(): Long = cardId
    
    fun getNeiID(): Long = neighborhoodId
    
    fun getNeighborhood(): NeighborhoodAdapter? {
        if (context == null) {
            return NeighborhoodAdapter(neighborhoodId, "Neighborhood $neighborhoodId", null, null)
        }
        
        return runBlocking {
            withContext(Dispatchers.IO) {
                val neighborhood = AHFlyweightFactory.INSTANCE.getNeighborhood(neighborhoodId)
                neighborhood ?: NeighborhoodAdapter(neighborhoodId, "Neighborhood $neighborhoodId", null, null)
            }
        }
    }
    
    fun getCardPath(): String? {
        return getNeighborhood()?.getCardPath()
    }
    
    fun getEncounters(): List<EncounterAdapter> {
        if (context == null) {
            return emptyList()
        }
        
        // If unifiedCard is null, try to load it from database
        val cardToUse = unifiedCard ?: runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(context!!)
                // Try to get card without expansion filter first (card_id is unique per game_type)
                // If that fails, try with BASE expansion as fallback
                db.getCardWithoutExpansion(GameType.ARKHAM, cardId.toString())
                    ?: db.getCard(GameType.ARKHAM, cardId.toString(), "BASE")
            }
        }
        
        if (cardToUse == null) {
            return emptyList()
        }
        
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(context!!)
                val encounters = db.getEncountersForCard(cardToUse.cardId, GameType.ARKHAM)
                encounters.map { enc ->
                    EncounterAdapter(
                        enc.id,
                        enc.text,
                        enc.locationId?.let { locId ->
                            AHFlyweightFactory.INSTANCE.getLocation(locId)
                        }
                    )
                }
            }
        }
    }
    
    fun getExpIDs(): List<Long> {
        if (context == null) {
            android.util.Log.d("NeighborhoodCardAdapter", "Context is null for card $cardId, returning base game")
            return listOf(1L) // Default to base game
        }
        
        // Use runBlocking for synchronous call (for compatibility with existing code)
        // But prefer using getExpIDsSuspend() when called from coroutines
        return runBlocking {
            getExpIDsSuspend()
        }
    }
    
    suspend fun getExpIDsSuspend(): List<Long> {
        if (context == null) {
            android.util.Log.d("NeighborhoodCardAdapter", "Context is null for card $cardId, returning base game")
            return listOf(1L) // Default to base game
        }
        
        // Use AHFlyweightFactory.getExpansionsForCard() - same as original ACard.getExpIDs()
        // This gets the expansion from the card itself (CardToExpansion table), not from the neighborhood
        return withContext(Dispatchers.IO) {
            val expIds = AHFlyweightFactory.INSTANCE.getExpansionsForCard(cardId)
            if (expIds.isNotEmpty()) {
                android.util.Log.d("NeighborhoodCardAdapter", "Found ${expIds.size} expansion IDs for card $cardId: $expIds")
                expIds
            } else {
                android.util.Log.w("NeighborhoodCardAdapter", "No expansion IDs found for card $cardId, returning base game")
                listOf(1L) // Default to base game
            }
        }
    }
    
    override fun toString(): String = "CardID: $cardId NeiID: $neighborhoodId"
    
    fun getUnifiedCard(): UnifiedCard? = unifiedCard
}

/**
 * Compatibility adapter for Neighborhood class
 */
class NeighborhoodAdapter(
    private val id: Long,
    private val name: String,
    private val cardPath: String? = null,
    private val buttonPath: String? = null
) {
    fun getID(): Long = id
    fun getNeighborhoodName(): String = name
    fun getCardPath(): String? = cardPath
    fun getNeighborhoodButtonPath(): String? = buttonPath
    override fun toString(): String = name
}


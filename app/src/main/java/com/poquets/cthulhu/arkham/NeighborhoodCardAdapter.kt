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
        
        // Get expansion ID from the neighborhood (neighborhoods have exp_id in the database)
        // This is more reliable than getting it from the card
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(context!!)
                val expId = db.getExpansionIdForNeighborhood(neighborhoodId)
                
                if (expId != null && expId > 0) {
                    android.util.Log.d("NeighborhoodCardAdapter", "Found expansion ID $expId for neighborhood $neighborhoodId (card $cardId)")
                    listOf(expId)
                } else {
                    android.util.Log.w("NeighborhoodCardAdapter", "Could not find expansion ID for neighborhood $neighborhoodId (card $cardId), trying card expansion")
                    
                    // Fallback: try to get expansion from card
                    val cardToUse = unifiedCard ?: db.getCard(GameType.ARKHAM, cardId.toString())
                    if (cardToUse != null) {
                        val expansionName = cardToUse.expansion
                        val expansion = AHFlyweightFactory.INSTANCE.getExpansions().find { it.getName() == expansionName }
                        if (expansion != null) {
                            android.util.Log.d("NeighborhoodCardAdapter", "Found expansion ID ${expansion.getID()} from card for card $cardId")
                            listOf(expansion.getID())
                        } else {
                            android.util.Log.w("NeighborhoodCardAdapter", "Expansion '$expansionName' not found, returning base game")
                            listOf(1L)
                        }
                    } else {
                        android.util.Log.w("NeighborhoodCardAdapter", "Could not load card or neighborhood expansion, returning base game")
                        listOf(1L) // Default to base game
                    }
                }
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


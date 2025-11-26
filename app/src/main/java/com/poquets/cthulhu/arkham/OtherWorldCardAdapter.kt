package com.poquets.cthulhu.arkham

import android.content.Context
import com.poquets.cthulhu.shared.database.GameType
import com.poquets.cthulhu.shared.database.UnifiedCard
import com.poquets.cthulhu.shared.database.UnifiedCardDatabaseHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Compatibility adapter for Arkham OtherWorldCard class
 */
class OtherWorldCardAdapter(
    private val cardId: Long,
    private val unifiedCard: UnifiedCard? = null,
    private val context: Context? = null
) {
    // Cache colors so we don't keep hitting the database for every filter / render
    @Volatile
    private var cachedColors: List<OtherWorldColorAdapter>? = null
    
    fun getID(): Long = cardId
    
    fun getOtherWorldColors(): List<OtherWorldColorAdapter> {
        // Fast path if we've already resolved colors for this card
        cachedColors?.let { return it }

        if (context == null) {
            return emptyList()
        }

        val resolved = runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    val db = UnifiedCardDatabaseHelper.getInstance(context)

                    // Directly use the card->color mapping migrated from the original Arkham DB.
                    // This matches the original OtherWorldCard.getOtherWorldColors() semantics and
                    // avoids walking encounters/locations.
                    val colors = db.getColorsForCard(cardId.toString())
                    colors.map { colorData ->
                        OtherWorldColorAdapter(
                            id = colorData.id,
                            name = colorData.name,
                            buttonPath = colorData.buttonPath,
                            expId = colorData.expId
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OtherWorldCardAdapter", "Error getting other world colors for card $cardId: ${e.message}", e)
                    emptyList()
                }
            }
        }

        cachedColors = resolved
        return resolved
    }
    
    fun getCardPath(): String? {
        if (context == null) {
            return null
        }
        
        // Try to get path from AHFlyweightFactory (uses color-to-path mapping)
        return runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    AHFlyweightFactory.INSTANCE.getOtherWorldCardPathForColoredCard(cardId)
                } catch (e: Exception) {
                    android.util.Log.w("OtherWorldCardAdapter", "Error getting card path: ${e.message}")
                    // Fallback to default otherworld card path
                    "otherworld/otherworld_front_colorless.png"
                }
            }
        }
    }
    
    fun getEncounters(): List<EncounterAdapter> {
        if (context == null) {
            return emptyList()
        }
        
        // If unifiedCard is null, try to load it from database
        val cardToUse = unifiedCard ?: runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(context!!)
                db.getCard(GameType.ARKHAM, cardId.toString())
            }
        }
        
        if (cardToUse == null) {
            return emptyList()
        }
        
        return runBlocking {
            withContext(Dispatchers.IO) {
                val db = UnifiedCardDatabaseHelper.getInstance(context!!)
                val encounters = db.getEncountersForCard(cardToUse.cardId, GameType.ARKHAM)
                android.util.Log.d("OtherWorldCardAdapter", "Found ${encounters.size} encounters for card ${cardToUse.cardId}")
                encounters.map { enc ->
                    val location = enc.locationId?.let { locId ->
                        val loc = AHFlyweightFactory.INSTANCE.getLocation(locId)
                        if (loc == null) {
                            android.util.Log.w("OtherWorldCardAdapter", "Location $locId not found for encounter ${enc.id}")
                        }
                        loc
                    }
                    if (location == null && enc.locationId != null) {
                        android.util.Log.w("OtherWorldCardAdapter", "Failed to get location ${enc.locationId} for encounter ${enc.id}")
                    }
                    EncounterAdapter(
                        enc.id,
                        enc.text,
                        location
                    )
                }
            }
        }
    }
    
    fun getExpIDs(): List<Long> {
        if (unifiedCard == null) {
            return listOf(1L) // Default to base game
        }
        
        // Get expansion name from unified card and map to ID
        val expansionName = unifiedCard.expansion
        val expansion = AHFlyweightFactory.INSTANCE.getExpansions().find { it.getName() == expansionName }
        return if (expansion != null) {
            listOf(expansion.getID())
        } else {
            listOf(1L) // Default to base game
        }
    }
    
    override fun toString(): String = "CardID: $cardId"
    
    fun getUnifiedCard(): UnifiedCard? = unifiedCard
}

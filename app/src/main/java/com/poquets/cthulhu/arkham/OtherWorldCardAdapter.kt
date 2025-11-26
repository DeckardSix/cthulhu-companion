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
    
    fun getID(): Long = cardId
    
    fun getOtherWorldColors(): List<OtherWorldColorAdapter> {
        // TODO: Get colors from unified database
        // For now, return empty list
        return emptyList()
    }
    
    fun getCardPath(): String? {
        // TODO: Get card path from unified database
        return null
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

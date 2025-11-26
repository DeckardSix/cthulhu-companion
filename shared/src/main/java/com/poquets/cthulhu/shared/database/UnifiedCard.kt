package com.poquets.cthulhu.shared.database

import android.content.ContentValues

/**
 * Unified card data model that can represent both Arkham and Eldritch cards
 */
data class UnifiedCard(
    val gameType: GameType,
    val cardId: String,  // Can be integer as string for Arkham, or text ID for Eldritch
    val expansion: String = "BASE",
    val cardName: String? = null,
    val encountered: String = "NONE",
    val cardData: String? = null,  // JSON or additional data
    
    // Arkham-specific fields
    val neighborhoodId: Long? = null,
    val locationId: Long? = null,
    val encounterId: Long? = null,
    
    // Eldritch-specific fields
    val region: String? = null,
    val topHeader: String? = null,
    val topEncounter: String? = null,
    val middleHeader: String? = null,
    val middleEncounter: String? = null,
    val bottomHeader: String? = null,
    val bottomEncounter: String? = null
) {
    /**
     * Convert to ContentValues for database insertion
     */
    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put("game_type", gameType.value)
        values.put("card_id", cardId)
        values.put("expansion", expansion)
        cardName?.let { values.put("card_name", it) }
        values.put("encountered", encountered)
        cardData?.let { values.put("card_data", it) }
        
        // Arkham fields
        neighborhoodId?.let { values.put("neighborhood_id", it) }
        locationId?.let { values.put("location_id", it) }
        encounterId?.let { values.put("encounter_id", it) }
        
        // Eldritch fields
        region?.let { values.put("region", it) }
        topHeader?.let { values.put("top_header", it) }
        topEncounter?.let { values.put("top_encounter", it) }
        middleHeader?.let { values.put("middle_header", it) }
        middleEncounter?.let { values.put("middle_encounter", it) }
        bottomHeader?.let { values.put("bottom_header", it) }
        bottomEncounter?.let { values.put("bottom_encounter", it) }
        
        return values
    }
}


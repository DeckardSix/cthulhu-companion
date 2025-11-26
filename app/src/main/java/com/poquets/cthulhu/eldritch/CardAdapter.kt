package com.poquets.cthulhu.eldritch

import com.poquets.cthulhu.shared.database.UnifiedCard

/**
 * Compatibility adapter to convert UnifiedCard to Eldritch Card format
 * This allows existing Eldritch code to work with the unified database
 */
class CardAdapter(private val unifiedCard: UnifiedCard) {
    
    val ID: String
        get() = unifiedCard.cardId
    
    val bottomEncounter: String?
        get() = unifiedCard.bottomEncounter
    
    val bottomHeader: String?
        get() = unifiedCard.bottomHeader
    
    var encountered: String
        get() = unifiedCard.encountered
        set(value) {
            // Note: This doesn't update the database - use DecksAdapter for that
        }
    
    val middleEncounter: String?
        get() = unifiedCard.middleEncounter
    
    val middleHeader: String?
        get() = unifiedCard.middleHeader
    
    val region: String?
        get() = unifiedCard.region
    
    val topEncounter: String?
        get() = unifiedCard.topEncounter
    
    val topHeader: String?
        get() = unifiedCard.topHeader
    
    val expansion: String
        get() = unifiedCard.expansion
    
    /**
     * Get the underlying UnifiedCard
     */
    fun getUnifiedCard(): UnifiedCard = unifiedCard
}

/**
 * Convert UnifiedCard to CardAdapter
 */
fun UnifiedCard.toCardAdapter(): CardAdapter = CardAdapter(this)

/**
 * Convert list of UnifiedCard to list of CardAdapter
 */
fun List<UnifiedCard>.toCardAdapters(): List<CardAdapter> = map { it.toCardAdapter() }


package com.poquets.cthulhu.shared.database

/**
 * Helper class for Eldritch-specific card operations
 * Converts between UnifiedCard and Eldritch Card structure
 */
object EldritchCardHelper {
    
    /**
     * Convert UnifiedCard to Eldritch Card-like structure
     * Returns a map with Eldritch-specific fields
     */
    fun toEldritchMap(card: UnifiedCard): Map<String, String?> {
        require(card.gameType == GameType.ELDRITCH) {
            "Card must be ELDRITCH type"
        }
        
        return mapOf(
            "ID" to card.cardId,
            "region" to card.region,
            "expansion" to card.expansion,
            "topHeader" to card.topHeader,
            "topEncounter" to card.topEncounter,
            "middleHeader" to card.middleHeader,
            "middleEncounter" to card.middleEncounter,
            "bottomHeader" to card.bottomHeader,
            "bottomEncounter" to card.bottomEncounter,
            "encountered" to card.encountered,
            "cardName" to card.cardName
        )
    }
    
    /**
     * Create UnifiedCard from Eldritch card data
     */
    fun fromEldritchData(
        cardId: String,
        region: String,
        expansion: String = "BASE",
        topHeader: String? = null,
        topEncounter: String? = null,
        middleHeader: String? = null,
        middleEncounter: String? = null,
        bottomHeader: String? = null,
        bottomEncounter: String? = null,
        encountered: String = "NONE",
        cardName: String? = null
    ): UnifiedCard {
        return UnifiedCard(
            gameType = GameType.ELDRITCH,
            cardId = cardId,
            expansion = expansion,
            cardName = cardName,
            encountered = encountered,
            region = region,
            topHeader = topHeader,
            topEncounter = topEncounter,
            middleHeader = middleHeader,
            middleEncounter = middleEncounter,
            bottomHeader = bottomHeader,
            bottomEncounter = bottomEncounter
        )
    }
    
    /**
     * Get encounter text for a specific section
     */
    fun getEncounterText(card: UnifiedCard, section: String): String? {
        require(card.gameType == GameType.ELDRITCH) {
            "Card must be ELDRITCH type"
        }
        
        return when (section.uppercase()) {
            "TOP" -> card.topEncounter
            "MIDDLE" -> card.middleEncounter
            "BOTTOM" -> card.bottomEncounter
            else -> null
        }
    }
    
    /**
     * Get header text for a specific section
     */
    fun getHeaderText(card: UnifiedCard, section: String): String? {
        require(card.gameType == GameType.ELDRITCH) {
            "Card must be ELDRITCH type"
        }
        
        return when (section.uppercase()) {
            "TOP" -> card.topHeader
            "MIDDLE" -> card.middleHeader
            "BOTTOM" -> card.bottomHeader
            else -> null
        }
    }
}


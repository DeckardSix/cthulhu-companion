package com.poquets.cthulhu.shared.database

/**
 * Helper class for Arkham-specific card operations
 * Converts between UnifiedCard and Arkham Card structure
 */
object ArkhamCardHelper {
    
    /**
     * Convert UnifiedCard to Arkham Card-like structure
     * Returns a map with Arkham-specific fields
     */
    fun toArkhamMap(card: UnifiedCard): Map<String, Any?> {
        require(card.gameType == GameType.ARKHAM) {
            "Card must be ARKHAM type"
        }
        
        return mapOf(
            "cardID" to card.cardId.toLongOrNull(),
            "neighborhoodID" to card.neighborhoodId,
            "locationID" to card.locationId,
            "encounterID" to card.encounterId,
            "expansion" to card.expansion,
            "encountered" to card.encountered,
            "cardName" to card.cardName
        )
    }
    
    /**
     * Create UnifiedCard from Arkham card data
     */
    fun fromArkhamData(
        cardId: Long,
        expansion: String = "BASE",
        neighborhoodId: Long? = null,
        locationId: Long? = null,
        encounterId: Long? = null,
        encountered: String = "NONE",
        cardName: String? = null
    ): UnifiedCard {
        return UnifiedCard(
            gameType = GameType.ARKHAM,
            cardId = cardId.toString(),
            expansion = expansion,
            cardName = cardName,
            encountered = encountered,
            neighborhoodId = neighborhoodId,
            locationId = locationId,
            encounterId = encounterId
        )
    }
    
    /**
     * Check if card belongs to a neighborhood
     */
    fun belongsToNeighborhood(card: UnifiedCard, neighborhoodId: Long): Boolean {
        require(card.gameType == GameType.ARKHAM) {
            "Card must be ARKHAM type"
        }
        return card.neighborhoodId == neighborhoodId
    }
    
    /**
     * Check if card belongs to a location
     */
    fun belongsToLocation(card: UnifiedCard, locationId: Long): Boolean {
        require(card.gameType == GameType.ARKHAM) {
            "Card must be ARKHAM type"
        }
        return card.locationId == locationId
    }
}


package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adapter to bridge Arkham game logic with unified database
 * Provides methods compatible with existing Arkham GameState class
 */
class ArkhamDeckAdapter(private val context: Context) {
    
    private val deckManager = DeckManager(context, GameType.ARKHAM)
    private val repository = CardRepository(context)
    private val gameState = GameStateManager.getInstance(context)
    
    /**
     * Initialize decks for selected expansions
     */
    suspend fun initialize(expansions: List<String> = emptyList()) {
        withContext(Dispatchers.IO) {
            deckManager.initializeDecks(expansions)
        }
    }
    
    /**
     * Get cards for a neighborhood
     */
    suspend fun getNeighborhoodCards(neighborhoodId: Long): List<UnifiedCard> = withContext(Dispatchers.IO) {
        val deckName = "neighborhood_$neighborhoodId"
        deckManager.getDeck(deckName)
    }
    
    /**
     * Get other world cards
     */
    suspend fun getOtherWorldCards(): List<UnifiedCard> = withContext(Dispatchers.IO) {
        // Other world cards have null neighborhood_id
        repository.getCards(GameType.ARKHAM)
            .filter { it.neighborhoodId == null }
            .filter { it.encountered == "NONE" }
    }
    
    /**
     * Get cards for a location
     */
    suspend fun getLocationCards(locationId: Long): List<UnifiedCard> = withContext(Dispatchers.IO) {
        repository.getCards(GameType.ARKHAM)
            .filter { it.locationId == locationId }
            .filter { it.encountered == "NONE" }
    }
    
    /**
     * Shuffle neighborhood deck
     */
    fun shuffleNeighborhood(neighborhoodId: Long) {
        val deckName = "neighborhood_$neighborhoodId"
        deckManager.shuffleDeck(deckName)
    }
    
    /**
     * Draw card from neighborhood
     */
    fun drawNeighborhoodCard(neighborhoodId: Long): UnifiedCard? {
        val deckName = "neighborhood_$neighborhoodId"
        return deckManager.drawCard(deckName)
    }
    
    /**
     * Discard card
     */
    suspend fun discardCard(card: UnifiedCard, encountered: String = "DISCARDED") {
        deckManager.discardCard(card, encountered)
    }
    
    /**
     * Get discard pile
     */
    fun getDiscardPile(): List<UnifiedCard> {
        return deckManager.getDiscardPile()
    }
    
    /**
     * Apply expansion (enable/disable)
     */
    suspend fun applyExpansion(expansionName: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            val currentExpansions = gameState.getSelectedExpansions(GameType.ARKHAM).toMutableSet()
            if (enabled) {
                currentExpansions.add(expansionName)
            } else {
                currentExpansions.remove(expansionName)
            }
            gameState.setSelectedExpansions(GameType.ARKHAM, currentExpansions)
            
            // Reinitialize decks with new expansion set
            initialize(currentExpansions.toList())
        }
    }
}


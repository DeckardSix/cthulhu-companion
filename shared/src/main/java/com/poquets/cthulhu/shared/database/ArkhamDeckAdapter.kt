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
     * Queries database directly to ensure cards are available even if decks haven't been initialized
     * Filters by selected expansions: includes cards that belong to at least one selected expansion,
     * but excludes cards that belong to any non-selected expansion
     */
    suspend fun getNeighborhoodCards(neighborhoodId: Long): List<UnifiedCard> = withContext(Dispatchers.IO) {
        // First try to get from initialized deck (for performance)
        val deckName = "neighborhood_$neighborhoodId"
        val deckCards = deckManager.getDeck(deckName)
        
        // If deck is initialized and has cards, return them
        if (deckCards.isNotEmpty()) {
            return@withContext deckCards
        }
        
        // Otherwise, query directly from database with expansion filtering
        // This ensures cards are available even if decks haven't been initialized
        Log.d("ArkhamDeckAdapter", "Deck not initialized for neighborhood $neighborhoodId, querying database directly")
        
        // Get selected expansions (BASE is always included)
        val selectedExpansions = gameState.getSelectedExpansions(GameType.ARKHAM).toMutableSet()
        if (!selectedExpansions.contains("BASE")) {
            selectedExpansions.add("BASE")
        }
        
        Log.d("ArkhamDeckAdapter", "Filtering cards for neighborhood $neighborhoodId with expansions: $selectedExpansions")
        
        // Get all cards for this neighborhood (across all expansions)
        val allNeighborhoodCards = repository.getCards(GameType.ARKHAM)
            .filter { it.neighborhoodId == neighborhoodId }
            .filter { it.encountered == "NONE" }
        
        Log.d("ArkhamDeckAdapter", "Found ${allNeighborhoodCards.size} total cards for neighborhood $neighborhoodId (unencountered)")
        
        // Filter by expansion: match original SQL logic from AHFlyweightFactory.getCurrentNeighborhoodsCards()
        // Include cards that belong to at least one selected expansion
        // BUT exclude cards that belong to any non-selected expansion
        // Since a card can have multiple entries (one per expansion), we need to:
        // 1. Group by card_id to see all expansions a card belongs to
        // 2. Include card if it has at least one selected expansion AND no non-selected expansions
        val cardsByCardId = allNeighborhoodCards.groupBy { it.cardId }
        val filteredCards = cardsByCardId.filter { (cardId, cardEntries) ->
            // Get all expansions this card belongs to
            val cardExpansions = cardEntries.map { it.expansion }.toSet()
            
            // Check if card belongs to at least one selected expansion
            val hasSelectedExpansion = cardExpansions.any { it in selectedExpansions }
            
            // Check if card belongs to any non-selected expansion
            val hasNonSelectedExpansion = cardExpansions.any { it !in selectedExpansions }
            
            // Include if: has at least one selected expansion AND no non-selected expansions
            val include = hasSelectedExpansion && !hasNonSelectedExpansion
            
            if (!include) {
                Log.d("ArkhamDeckAdapter", "Excluding card $cardId: expansions=$cardExpansions, selected=$selectedExpansions, hasSelected=$hasSelectedExpansion, hasNonSelected=$hasNonSelectedExpansion")
            }
            include
        }.values.map { cardEntries ->
            // For cards that pass the filter, prefer BASE entry if available, otherwise use first entry
            cardEntries.find { it.expansion == "BASE" } ?: cardEntries.first()
        }
        
        Log.d("ArkhamDeckAdapter", "Found ${filteredCards.size} cards for neighborhood $neighborhoodId after expansion filtering")
        filteredCards
    }
    
    /**
     * Get other world cards
     * Filters by selected expansions: includes cards that belong to at least one selected expansion,
     * but excludes cards that belong to any non-selected expansion
     */
    suspend fun getOtherWorldCards(): List<UnifiedCard> = withContext(Dispatchers.IO) {
        // Get selected expansions (BASE is always included)
        val selectedExpansions = gameState.getSelectedExpansions(GameType.ARKHAM).toMutableSet()
        if (!selectedExpansions.contains("BASE")) {
            selectedExpansions.add("BASE")
        }
        
        Log.d("ArkhamDeckAdapter", "Filtering otherworld cards with expansions: $selectedExpansions")
        
        // Other world cards have null neighborhood_id
        val allOtherWorldCards = repository.getCards(GameType.ARKHAM)
            .filter { it.neighborhoodId == null }
            .filter { it.encountered == "NONE" }
        
        Log.d("ArkhamDeckAdapter", "Found ${allOtherWorldCards.size} total otherworld cards (unencountered)")
        
        // Filter by expansion: same logic as neighborhood cards
        // Include cards that belong to at least one selected expansion
        // BUT exclude cards that belong to any non-selected expansion
        val cardsByCardId = allOtherWorldCards.groupBy { it.cardId }
        val filteredCards = cardsByCardId.filter { (cardId, cardEntries) ->
            // Get all expansions this card belongs to
            val cardExpansions = cardEntries.map { it.expansion }.toSet()
            
            // Check if card belongs to at least one selected expansion
            val hasSelectedExpansion = cardExpansions.any { it in selectedExpansions }
            
            // Check if card belongs to any non-selected expansion
            val hasNonSelectedExpansion = cardExpansions.any { it !in selectedExpansions }
            
            // Include if: has at least one selected expansion AND no non-selected expansions
            val include = hasSelectedExpansion && !hasNonSelectedExpansion
            
            if (!include) {
                Log.d("ArkhamDeckAdapter", "Excluding otherworld card $cardId: expansions=$cardExpansions, selected=$selectedExpansions, hasSelected=$hasSelectedExpansion, hasNonSelected=$hasNonSelectedExpansion")
            }
            include
        }.values.map { cardEntries ->
            // For cards that pass the filter, prefer BASE entry if available, otherwise use first entry
            cardEntries.find { it.expansion == "BASE" } ?: cardEntries.first()
        }
        
        Log.d("ArkhamDeckAdapter", "Found ${filteredCards.size} otherworld cards after expansion filtering")
        filteredCards
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


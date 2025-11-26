package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Deck manager for handling card decks and discard piles
 * Works with the unified database for both games
 */
class DeckManager(private val context: Context, private val gameType: GameType) {
    
    private val repository = CardRepository(context)
    private val db = UnifiedCardDatabaseHelper.getInstance(context)
    
    // In-memory deck storage (for performance)
    private val decks = mutableMapOf<String, MutableList<UnifiedCard>>()
    private val discardPile = mutableListOf<UnifiedCard>()
    
    /**
     * Initialize decks from database
     */
    suspend fun initializeDecks(expansions: List<String> = emptyList()) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DeckManager", "Initializing decks for ${gameType.value}")
                
                // Clear existing decks
                decks.clear()
                discardPile.clear()
                
                // Load cards based on game type
                val allCards = if (expansions.isEmpty()) {
                    repository.getCards(gameType)
                } else {
                    expansions.flatMap { expansion ->
                        repository.getCardsByExpansion(gameType, expansion)
                    }
                }
                
                // Group cards by region (Eldritch) or neighborhood (Arkham)
                when (gameType) {
                    GameType.ELDRITCH -> {
                        // Group by region
                        allCards.groupBy { it.region ?: "UNKNOWN" }.forEach { (region, cards) ->
                            decks[region] = cards.filter { it.encountered == "NONE" }.toMutableList()
                            Collections.shuffle(decks[region])
                        }
                    }
                    GameType.ARKHAM -> {
                        // Group by neighborhood
                        allCards.groupBy { it.neighborhoodId ?: -1L }.forEach { (neighborhoodId, cards) ->
                            val deckName = "neighborhood_$neighborhoodId"
                            decks[deckName] = cards.filter { it.encountered == "NONE" }.toMutableList()
                            Collections.shuffle(decks[deckName])
                        }
                    }
                }
                
                // Initialize discard pile
                discardPile.addAll(allCards.filter { it.encountered != "NONE" && it.encountered != null })
                
                Log.d("DeckManager", "Initialized ${decks.size} decks with ${allCards.size} total cards")
                
            } catch (e: Exception) {
                Log.e("DeckManager", "Error initializing decks: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get a deck by name
     */
    fun getDeck(deckName: String): List<UnifiedCard> {
        return decks[deckName] ?: emptyList()
    }
    
    /**
     * Check if a deck exists
     */
    fun hasDeck(deckName: String): Boolean {
        return decks.containsKey(deckName) && !decks[deckName].isNullOrEmpty()
    }
    
    /**
     * Shuffle a deck
     */
    fun shuffleDeck(deckName: String) {
        val deck = decks[deckName]
        if (deck != null) {
            if (deck.isEmpty()) {
                refillDeck(deckName)
            }
            Collections.shuffle(deck)
        }
    }
    
    /**
     * Shuffle all decks
     */
    fun shuffleAllDecks() {
        decks.keys.forEach { shuffleDeck(it) }
    }
    
    /**
     * Draw a card from a deck
     */
    fun drawCard(deckName: String): UnifiedCard? {
        val deck = decks[deckName] ?: return null
        
        if (deck.isEmpty()) {
            refillDeck(deckName)
        }
        
        return if (deck.isNotEmpty()) {
            deck.removeAt(0)
        } else null
    }
    
    /**
     * Discard a card
     */
    suspend fun discardCard(card: UnifiedCard, encountered: String = "DISCARDED") {
        withContext(Dispatchers.IO) {
            try {
                // Update card in database
                repository.updateEncounteredStatus(
                    card.gameType,
                    card.cardId,
                    card.expansion,
                    encountered
                )
                
                // Add to discard pile
                val updatedCard = card.copy(encountered = encountered)
                discardPile.add(0, updatedCard)
                
            } catch (e: Exception) {
                Log.e("DeckManager", "Error discarding card: ${e.message}", e)
            }
        }
    }
    
    /**
     * Refill a deck from discard pile
     */
    private fun refillDeck(deckName: String) {
        val deck = decks[deckName] ?: return
        
        // Move cards from discard back to deck
        val cardsToRefill = discardPile.filter { card ->
            when (gameType) {
                GameType.ELDRITCH -> card.region == deckName
                GameType.ARKHAM -> {
                    val neighborhoodId = card.neighborhoodId
                    deckName == "neighborhood_$neighborhoodId"
                }
            }
        }
        
        cardsToRefill.forEach { card ->
            discardPile.remove(card)
            deck.add(card.copy(encountered = "NONE"))
            
            // Update database (async, but we don't wait for it in this non-suspend function)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repository.updateEncounteredStatus(
                    card.gameType,
                    card.cardId,
                    card.expansion,
                    "NONE"
                )
            }
        }
        
        Collections.shuffle(deck)
    }
    
    /**
     * Get discard pile
     */
    fun getDiscardPile(): List<UnifiedCard> {
        return discardPile.toList()
    }
    
    /**
     * Remove card from discard pile (return to deck)
     */
    suspend fun removeFromDiscard(card: UnifiedCard) {
        withContext(Dispatchers.IO) {
            discardPile.remove(card)
            
            // Find appropriate deck
            val deckName = when (gameType) {
                GameType.ELDRITCH -> card.region ?: return@withContext
                GameType.ARKHAM -> "neighborhood_${card.neighborhoodId ?: return@withContext}"
            }
            
            val deck = decks[deckName] ?: return@withContext
            deck.add(card.copy(encountered = "NONE"))
            shuffleDeck(deckName)
            
            // Update database
            repository.updateEncounteredStatus(
                card.gameType,
                card.cardId,
                card.expansion,
                "NONE"
            )
        }
    }
    
    /**
     * Get deck statistics
     */
    fun getDeckStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        decks.forEach { (name, cards) ->
            stats[name] = cards.size
        }
        stats["DISCARD"] = discardPile.size
        return stats
    }
}


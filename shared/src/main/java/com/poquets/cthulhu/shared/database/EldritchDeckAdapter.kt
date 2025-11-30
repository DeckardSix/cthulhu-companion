package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adapter to bridge Eldritch game logic with unified database
 * Provides methods compatible with existing Eldritch Decks class
 */
class EldritchDeckAdapter(private val context: Context) {
    
    private val deckManager = DeckManager(context, GameType.ELDRITCH)
    private val repository = CardRepository(context)
    private val gameState = GameStateManager.getInstance(context)
    
    /**
     * Initialize decks (similar to Eldritch Decks constructor)
     */
    suspend fun initialize(expansions: List<String> = emptyList()) {
        withContext(Dispatchers.IO) {
            deckManager.initializeDecks(expansions)
        }
    }
    
    /**
     * Get deck by region name (compatible with Eldritch Decks.getDeck())
     */
    suspend fun getDeck(region: String): List<UnifiedCard> = withContext(Dispatchers.IO) {
        deckManager.getDeck(region)
    }
    
    /**
     * Check if deck exists (compatible with Eldritch Decks.containsDeck())
     */
    fun containsDeck(region: String): Boolean {
        return deckManager.hasDeck(region)
    }
    
    /**
     * Shuffle deck (compatible with Eldritch Decks.shuffleDeck())
     */
    fun shuffleDeck(region: String) {
        deckManager.shuffleDeck(region)
    }
    
    /**
     * Shuffle full deck (refill and shuffle)
     */
    fun shuffleFullDeck(region: String) {
        // Reset encountered status for this region and reinitialize decks (async)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.resetEncounteredStatus(GameType.ELDRITCH, null, region)
            deckManager.initializeDecks(gameState.getSelectedExpansions(GameType.ELDRITCH).toList())
        }
    }
    
    /**
     * Discard card (compatible with Eldritch Decks.discardCard())
     */
    suspend fun discardCard(region: String, cardId: String, encountered: String) {
        withContext(Dispatchers.IO) {
            val card = repository.getCard(GameType.ELDRITCH, cardId)
            if (card != null) {
                deckManager.discardCard(card, encountered)
            } else {
                Log.w("EldritchDeckAdapter", "Card not found: $cardId")
            }
        }
    }
    
    /**
     * Get expedition location (compatible with Eldritch Decks.getExpeditionLocation())
     */
    suspend fun getExpeditionLocation(): String = withContext(Dispatchers.IO) {
        val expeditionDeck = deckManager.getDeck("EXPEDITION")
        return@withContext if (expeditionDeck.isNotEmpty()) {
            expeditionDeck[0].topHeader ?: expeditionDeck[0].region ?: "EMPTY"
        } else {
            "EMPTY"
        }
    }
    
    /**
     * Get mystic ruins location
     * For MYSTIC_RUINS cards, the location name is stored in topHeader (from the NAME element)
     */
    suspend fun getMysticRuinsLocation(): String = withContext(Dispatchers.IO) {
        val mysticRuinsDeck = deckManager.getDeck("MYSTIC_RUINS")
        return@withContext if (mysticRuinsDeck.isNotEmpty()) {
            // For MYSTIC_RUINS, the location name is in topHeader (e.g., "Stonehenge - London")
            val location = mysticRuinsDeck[0].topHeader
            Log.d("EldritchDeckAdapter", "Mystic Ruins location: $location (from topHeader)")
            location ?: "EMPTY"
        } else {
            Log.w("EldritchDeckAdapter", "Mystic Ruins deck is empty")
            "EMPTY"
        }
    }
    
    /**
     * Get dream quest location
     */
    suspend fun getDreamQuestLocation(): String = withContext(Dispatchers.IO) {
        val dreamQuestDeck = deckManager.getDeck("DREAM_QUEST") // Normalized: hyphens to underscores
        return@withContext if (dreamQuestDeck.isNotEmpty()) {
            dreamQuestDeck[0].topHeader ?: dreamQuestDeck[0].region ?: "EMPTY"
        } else {
            "EMPTY"
        }
    }
    
    /**
     * Remove expeditions from a region
     */
    suspend fun removeExpeditions(region: String) {
        withContext(Dispatchers.IO) {
            val expeditionDeck = deckManager.getDeck("EXPEDITION")
            expeditionDeck.filter { it.topHeader == region }.forEach { card ->
                deckManager.discardCard(card, "removed")
            }
        }
    }
    
    /**
     * Get discard pile
     */
    suspend fun getDiscardPile(): List<UnifiedCard> = withContext(Dispatchers.IO) {
        deckManager.getDiscardPile()
    }
    
    /**
     * Remove card from discard pile (return to deck)
     */
    suspend fun removeFromDiscard(card: UnifiedCard) {
        deckManager.removeFromDiscard(card)
    }
    
    /**
     * Get SPECIAL card name (topHeader) for button display
     * Returns the topHeader from the first card in the SPECIAL deck, or null if deck is empty
     */
    suspend fun getSpecialCardName(deckName: String): String? = withContext(Dispatchers.IO) {
        val deck = deckManager.getDeck(deckName)
        return@withContext if (deck.isNotEmpty()) {
            deck[0].topHeader
        } else {
            null
        }
    }
}


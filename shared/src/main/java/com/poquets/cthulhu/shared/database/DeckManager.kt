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
                
                // Determine which expansions to load
                // For Eldritch, always include BASE expansion as it's required for core game mechanics
                val expansionsToLoad = if (expansions.isEmpty()) {
                    emptyList<String>()
                } else if (gameType == GameType.ELDRITCH && !expansions.contains("BASE")) {
                    expansions + "BASE"
                } else {
                    expansions
                }
                
                // Load cards based on game type
                val allCards = if (expansions.isEmpty()) {
                    repository.getCards(gameType)
                } else {
                    Log.d("DeckManager", "Loading cards for expansions: ${expansionsToLoad.joinToString()}")
                    val allCards = expansionsToLoad.flatMap { expansion ->
                        val cards = repository.getCardsByExpansion(gameType, expansion)
                        Log.d("DeckManager", "Loaded ${cards.size} cards for expansion '$expansion'")
                        
                        // Verify all cards are from the correct expansion
                        val wrongExpansionCards = cards.filter { (it.expansion ?: "BASE") != expansion }
                        if (wrongExpansionCards.isNotEmpty()) {
                            val wrongExpansions = wrongExpansionCards.map { it.expansion ?: "BASE" }.distinct()
                            Log.e("DeckManager", "ERROR: Query for expansion '$expansion' returned ${wrongExpansionCards.size} cards from wrong expansions: ${wrongExpansions.joinToString()}")
                        }
                        
                        // Debug: log region distribution for this expansion
                        val regionGroups = cards.groupBy { it.region }
                        regionGroups.forEach { (region, regionCards) ->
                            Log.d("DeckManager", "  Expansion '$expansion' region '$region': ${regionCards.size} cards")
                        }
                        cards
                    }
                    Log.d("DeckManager", "Total cards loaded: ${allCards.size} from ${expansionsToLoad.size} expansions")
                    
                    // Final verification: check expansion distribution
                    val expansionGroups = allCards.groupBy { it.expansion ?: "BASE" }
                    Log.d("DeckManager", "Cards by expansion: ${expansionGroups.map { "${it.key}=${it.value.size}" }.joinToString()}")
                    allCards
                }
                
                // Group cards by region (Eldritch) or neighborhood (Arkham)
                when (gameType) {
                    GameType.ELDRITCH -> {
                        // Group by region, normalizing hyphens to underscores
                        // This handles both old data (ANTARCTICA-WEST) and new data (ANTARCTICA_WEST)
                        val regionGroups = allCards.groupBy { (it.region ?: "UNKNOWN").replace("-", "_") }
                        regionGroups.forEach { (region, cards) ->
                            // Filter to only include cards from selected expansions (if expansions were specified)
                            val expansionSet = if (expansionsToLoad.isNotEmpty()) {
                                expansionsToLoad.toSet()
                            } else {
                                // If no expansions specified, include all cards (allCards already filtered by repository)
                                null
                            }
                            val filteredCards = if (expansionSet != null) {
                                cards.filter { card ->
                                    val cardExpansion = card.expansion ?: "BASE"
                                    expansionSet.contains(cardExpansion)
                                }
                            } else {
                                cards // No filtering needed if no expansions specified
                            }
                            
                            // Log if we're filtering out cards
                            if (expansionSet != null) {
                                val filteredOut = cards.size - filteredCards.size
                                if (filteredOut > 0) {
                                    val wrongExpansions = cards.filter { card ->
                                        val cardExpansion = card.expansion ?: "BASE"
                                        !expansionSet.contains(cardExpansion)
                                    }.map { it.expansion }.distinct()
                                    Log.w("DeckManager", "Region '$region': Filtered out $filteredOut cards from unselected expansions: ${wrongExpansions.joinToString()}")
                                }
                            }
                            
                            val unencountered = filteredCards.filter { it.encountered == "NONE" }.toMutableList()
                            decks[region] = unencountered
                            Collections.shuffle(decks[region])
                            
                            // Log expansion distribution for all regions
                            val expansionGroups = filteredCards.groupBy { it.expansion ?: "BASE" }
                            Log.d("DeckManager", "Region '$region': ${unencountered.size} unencountered cards from expansions: ${expansionGroups.map { "${it.key}=${it.value.size}" }.joinToString()}")
                            
                            if (region.contains("ANTARCTICA", ignoreCase = true)) {
                                Log.d("DeckManager", "Antarctica deck '$region': ${unencountered.size} unencountered cards out of ${filteredCards.size} total (expansions: ${expansionGroups.keys.joinToString()})")
                            }
                        }
                        Log.d("DeckManager", "Initialized ${decks.size} Eldritch decks. Regions: ${decks.keys.sorted().joinToString()}")
                        
                        // Debug: Check for SPECIAL decks specifically
                        val specialDecks = decks.keys.filter { it.contains("SPECIAL", ignoreCase = true) }
                        if (specialDecks.isNotEmpty()) {
                            specialDecks.forEach { deckName ->
                                Log.d("DeckManager", "SPECIAL deck '$deckName': ${decks[deckName]?.size ?: 0} cards")
                            }
                        } else {
                            Log.w("DeckManager", "No SPECIAL decks found! Available decks: ${decks.keys.sorted().joinToString()}")
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
     * Shuffles the deck each time it's accessed to ensure randomization
     * Normalizes region names (hyphens to underscores) to handle both formats
     */
    fun getDeck(deckName: String): List<UnifiedCard> {
        // Normalize deck name: convert hyphens to underscores
        // This handles both ANTARCTICA-WEST (from XML) and ANTARCTICA_WEST (expected format)
        val normalizedName = deckName.replace("-", "_")
        val deck = decks[normalizedName] ?: return emptyList()
        
        // Shuffle the deck each time it's accessed to ensure random card selection
        if (deck.isNotEmpty()) {
            Collections.shuffle(deck)
            Log.d("DeckManager", "Shuffled deck $normalizedName (${deck.size} cards) for random access")
        }
        
        return deck
    }
    
    /**
     * Check if a deck exists
     * Normalizes region names (hyphens to underscores) to handle both formats
     */
    fun hasDeck(deckName: String): Boolean {
        // Normalize deck name: convert hyphens to underscores
        val normalizedName = deckName.replace("-", "_")
        return decks.containsKey(normalizedName) && !decks[normalizedName].isNullOrEmpty()
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
     * Note: Card is NOT removed from deck, only marked as encountered in database
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
                Log.d("DeckManager", "Card ${card.cardId} marked as encountered: $encountered (discard size: ${discardPile.size})")
                
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


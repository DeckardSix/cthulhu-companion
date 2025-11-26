package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Example usage of the unified database system
 * This shows how to integrate with existing game code
 */
object IntegrationExample {
    
    /**
     * Example: Initialize Eldritch game with unified database
     */
    fun initializeEldritchGame(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Set current game
                val gameState = GameStateManager.getInstance(context)
                gameState.setCurrentGame(GameType.ELDRITCH)
                
                // 2. Set selected expansions
                val expansions = setOf("BASE", "FORSAKEN_LORE")
                gameState.setSelectedExpansions(GameType.ELDRITCH, expansions)
                
                // 3. Initialize deck adapter
                val deckAdapter = EldritchDeckAdapter(context)
                deckAdapter.initialize(expansions.toList())
                
                // 4. Now you can use the deck adapter like the old Decks class
                val americasDeck = deckAdapter.getDeck("AMERICAS")
                Log.d("IntegrationExample", "Americas deck has ${americasDeck.size} cards")
                
            } catch (e: Exception) {
                Log.e("IntegrationExample", "Error initializing Eldritch: ${e.message}", e)
            }
        }
    }
    
    /**
     * Example: Initialize Arkham game with unified database
     */
    fun initializeArkhamGame(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Set current game
                val gameState = GameStateManager.getInstance(context)
                gameState.setCurrentGame(GameType.ARKHAM)
                
                // 2. Set selected expansions
                val expansions = setOf("BASE", "Dunwich Horror")
                gameState.setSelectedExpansions(GameType.ARKHAM, expansions)
                
                // 3. Initialize deck adapter
                val deckAdapter = ArkhamDeckAdapter(context)
                deckAdapter.initialize(expansions.toList())
                
                // 4. Get neighborhood cards
                val neighborhoodCards = deckAdapter.getNeighborhoodCards(1)
                Log.d("IntegrationExample", "Neighborhood 1 has ${neighborhoodCards.size} cards")
                
            } catch (e: Exception) {
                Log.e("IntegrationExample", "Error initializing Arkham: ${e.message}", e)
            }
        }
    }
    
    /**
     * Example: Draw and discard a card (Eldritch)
     */
    fun drawAndDiscardEldritchCard(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val deckAdapter = EldritchDeckAdapter(context)
                
                // Get deck
                val deck = deckAdapter.getDeck("AMERICAS")
                if (deck.isNotEmpty()) {
                    // Draw first card
                    val card = deck[0]
                    
                    // Discard it
                    deckAdapter.discardCard("AMERICAS", card.cardId, "TOP")
                    
                    Log.d("IntegrationExample", "Drew and discarded card: ${card.cardId}")
                }
                
            } catch (e: Exception) {
                Log.e("IntegrationExample", "Error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Example: Query cards with filters
     */
    fun queryCardsExample(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val repository = CardRepository(context)
                
                // Get all unencountered Eldritch cards from BASE expansion
                val cards = repository.getUnencounteredCards(
                    GameType.ELDRITCH,
                    expansion = "BASE"
                )
                
                Log.d("IntegrationExample", "Found ${cards.size} unencountered BASE cards")
                
                // Get cards by region
                val americasCards = repository.getCardsByRegion(
                    GameType.ELDRITCH,
                    "AMERICAS"
                )
                
                Log.d("IntegrationExample", "Americas region has ${americasCards.size} cards")
                
            } catch (e: Exception) {
                Log.e("IntegrationExample", "Error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Example: Using QueryBuilder
     */
    fun queryBuilderExample(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                // Build complex query
                val cards = com.poquets.cthulhu.shared.database.queryCards(GameType.ELDRITCH)
                    .withExpansion("BASE")
                    .withRegion("AMERICAS")
                    .onlyUnencountered()
                    .limit(10)
                    .execute(context)
                
                Log.d("IntegrationExample", "Query returned ${cards.size} cards")
                
            } catch (e: Exception) {
                Log.e("IntegrationExample", "Error: ${e.message}", e)
            }
        }
    }
}


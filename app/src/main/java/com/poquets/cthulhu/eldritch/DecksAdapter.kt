package com.poquets.cthulhu.eldritch

import android.content.Context
import android.util.Log
import com.poquets.cthulhu.shared.database.EldritchDeckAdapter
import com.poquets.cthulhu.shared.database.GameStateManager
import com.poquets.cthulhu.shared.database.GameType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch

/**
 * Compatibility wrapper for Decks.CARDS API
 * This provides backward compatibility with existing Eldritch code
 * while using the unified database underneath
 */
class DecksAdapter private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: DecksAdapter? = null
        private val lock = Any()
        
        @JvmStatic
        var CARDS: DecksAdapter? = null
            private set
        
        @JvmStatic
        fun initialize(context: Context): DecksAdapter {
            return INSTANCE ?: synchronized(lock) {
                INSTANCE ?: DecksAdapter(context.applicationContext).also {
                    INSTANCE = it
                    CARDS = it
                }
            }
        }
        
        @JvmStatic
        fun getInstance(): DecksAdapter? = INSTANCE
    }
    
    private val context: Context = context.applicationContext
    private val deckAdapter: EldritchDeckAdapter = EldritchDeckAdapter(context)
    private val gameState: GameStateManager = GameStateManager.getInstance(context)
    private var initialized = false
    
    /**
     * Initialize decks (blocking call for compatibility)
     */
    fun initialize(expansions: List<String> = emptyList()) {
        if (initialized) return
        
        runBlocking(Dispatchers.IO) {
            try {
                gameState.setCurrentGame(GameType.ELDRITCH)
                if (expansions.isNotEmpty()) {
                    gameState.setSelectedExpansions(GameType.ELDRITCH, expansions.toSet())
                }
                deckAdapter.initialize(expansions)
                initialized = true
                Log.d("DecksAdapter", "Decks initialized with ${expansions.size} expansions")
            } catch (e: Exception) {
                Log.e("DecksAdapter", "Error initializing decks: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get deck by region name (blocking call)
     */
    fun getDeck(deckName: String): List<CardAdapter> {
        if (!initialized) {
            Log.w("DecksAdapter", "Decks not initialized, initializing now...")
            initialize()
        }
        
        return runBlocking(Dispatchers.IO) {
            try {
                deckAdapter.getDeck(deckName).map { it.toCardAdapter() }
            } catch (e: Exception) {
                Log.e("DecksAdapter", "Error getting deck $deckName: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Check if deck exists
     */
    fun containsDeck(deckName: String): Boolean {
        if (!initialized) return false
        return runBlocking(Dispatchers.IO) {
            deckAdapter.containsDeck(deckName)
        }
    }
    
    /**
     * Shuffle deck
     */
    fun shuffleDeck(deckName: String) {
        if (!initialized) return
        deckAdapter.shuffleDeck(deckName)
    }
    
    /**
     * Shuffle full deck (refill and shuffle)
     */
    fun shuffleFullDeck(deckName: String) {
        if (!initialized) return
        deckAdapter.shuffleFullDeck(deckName)
    }
    
    /**
     * Discard card
     */
    fun discardCard(deck: String, cardId: String, encountered: String?) {
        if (!initialized) return
        
        val encounterStatus = encountered ?: "DISCARDED"
        runBlocking(Dispatchers.IO) {
            deckAdapter.discardCard(deck, cardId, encounterStatus)
        }
    }
    
    /**
     * Get expedition location
     */
    fun getExpeditionLocation(): String? {
        if (!initialized) return null
        return runBlocking(Dispatchers.IO) {
            deckAdapter.getExpeditionLocation()
        }
    }
    
    /**
     * Get mystic ruins location
     */
    fun getMysticRuinsLocation(): String? {
        if (!initialized) return null
        return runBlocking(Dispatchers.IO) {
            deckAdapter.getMysticRuinsLocation()
        }
    }
    
    /**
     * Get dream quest location
     */
    fun getDreamQuestLocation(): String? {
        if (!initialized) return null
        return runBlocking(Dispatchers.IO) {
            deckAdapter.getDreamQuestLocation()
        }
    }
    
    /**
     * Remove expeditions from a region
     */
    fun removeExpeditions(region: String) {
        if (!initialized) return
        runBlocking(Dispatchers.IO) {
            deckAdapter.removeExpeditions(region)
        }
    }
    
    /**
     * Remove card from discard pile
     */
    fun removeCardFromDiscard(position: Int) {
        if (!initialized) return
        runBlocking(Dispatchers.IO) {
            val discardPile = deckAdapter.getDiscardPile()
            if (position >= 0 && position < discardPile.size) {
                val card = discardPile[position]
                deckAdapter.removeFromDiscard(card)
            }
        }
    }
    
    /**
     * Get region name for a card (for compatibility)
     */
    fun getRegion(deckName: String, position: Int): String? {
        val deck = getDeck(deckName)
        if (position >= 0 && position < deck.size) {
            return deck[position].region
        }
        return null
    }
    
    /**
     * Print decks (for debugging)
     */
    fun printDecks() {
        if (!initialized) {
            Log.d("DECKS", "Decks not initialized")
            return
        }
        
        Log.d("DECKS", "=============")
        val regions = listOf("AMERICAS", "EUROPE", "ASIA", "AFRICA", "ANTARCTICA_WEST", 
                           "ANTARCTICA_EAST", "EGYPT", "DREAMLANDS", "GENERAL", "GATE", 
                           "RESEARCH", "EXPEDITION", "MYSTIC_RUINS", "DREAM-QUEST", "DISCARD")
        
        regions.forEach { region ->
            val deck = getDeck(region)
            if (deck.isNotEmpty()) {
                Log.d("DECKS", "$region\t\t${deck.size}")
            }
        }
        Log.d("DECKS", "=============")
    }
}


package com.poquets.cthulhu.arkham

import android.content.Context
import android.util.Log
import com.poquets.cthulhu.shared.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Random

/**
 * Compatibility adapter for GameState singleton
 * Provides methods compatible with existing Arkham GameState API
 */
class GameStateAdapter private constructor(context: Context) {
    
    private val appContext: Context = context.applicationContext
    private val gameState = GameStateManager.getInstance(appContext)
    private val deckAdapter = ArkhamDeckAdapter(appContext)
    private val ahFactory = AHFlyweightFactoryAdapter.getInstance(appContext)
    
    // Cache for neighborhood decks
    private val neighborhoodCardsList = mutableMapOf<Long, MutableList<NeighborhoodCardAdapter>>()
    private var otherWorldCards: MutableList<OtherWorldCardAdapter>? = null
    private val rand = Random(System.currentTimeMillis())
    
    // Store selected otherworld colors
    private val currentColors = mutableMapOf<Long, OtherWorldColorAdapter>()
    
    // Store selected otherworld location ID
    private var selectedLocationId: Long? = null
    
    // Card history (last 20 cards)
    private val cardHistory = mutableListOf<CardHistoryEntry>()
    private val MAX_CARD_HISTORY = 20
    
    data class CardHistoryEntry(
        val cardId: Long,
        val isOtherWorld: Boolean,
        val neighborhoodId: Long? = null, // Only for location cards
        var selectedEncounterId: Long? = null // The encounter that was selected when card was opened
    )
    
    companion object {
        @Volatile
        private var INSTANCE: GameStateAdapter? = null
        private val lock = Any()
        
        fun getInstance(context: Context): GameStateAdapter {
            return INSTANCE ?: synchronized(lock) {
                INSTANCE ?: GameStateAdapter(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    init {
        // Load game state
        Load()
    }
    
    private fun Load() {
        // Ensure base game is always in selected expansions
        val currentExpansions = gameState.getSelectedExpansions(GameType.ARKHAM).toMutableSet()
        if (!currentExpansions.contains("BASE")) {
            currentExpansions.add("BASE")
            gameState.setSelectedExpansions(GameType.ARKHAM, currentExpansions)
            Log.i("GameStateAdapter", "Base game (BASE) added to selected expansions on initialization")
        }
        
        // Ensure base game is applied
        val appliedExpansions = getAppliedExpansions()
        if (appliedExpansions.isEmpty() || !appliedExpansions.contains(1L)) {
            applyExpansion(1, true)
            Log.i("GameStateAdapter", "Base game (ID 1) applied by default")
        }
    }
    
    /**
     * Get applied expansion IDs (compatible with GameState.getAppliedExpansions())
     */
    fun getAppliedExpansions(): Set<Long> {
        val expansionNames = gameState.getSelectedExpansions(GameType.ARKHAM)
        
        // Map expansion names to IDs (matching ArkhamInit.java exactly)
        // ID 1 = Base
        // ID 2 = Curse of the Dark Pharoah
        // ID 3 = Dunwich Horror
        // ID 4 = The King in Yellow
        // ID 5 = Kingsport Horror
        // ID 6 = Black Goat of the Woods
        // ID 7 = Innsmouth Horror
        // ID 8 = Lurker at the Threshold
        // ID 9 = Curse of the Dark Pharoah Revised
        // ID 10 = Miskatonic Horror
        val expansionIdMap = mapOf(
            "BASE" to 1L,
            "Base" to 1L,
            "Base Game" to 1L,
            "Curse of the Dark Pharoah" to 2L,
            "Curse of the Dark Pharaoh" to 2L, // Handle both spellings
            "Dunwich Horror" to 3L,
            "The King in Yellow" to 4L,
            "King in Yellow" to 4L,
            "Kingsport Horror" to 5L,
            "Black Goat of the Woods" to 6L,
            "The Black Goat of the Woods" to 6L,
            "Innsmouth Horror" to 7L,
            "Lurker at the Threshold" to 8L,
            "The Lurker at the Threshold" to 8L,
            "Curse of the Dark Pharoah Revised" to 9L,
            "Curse of the Dark Pharaoh Revised" to 9L, // Handle both spellings
            "Miskatonic Horror" to 10L
        )
        
        val expansionIds = expansionNames.mapNotNull { expansionIdMap[it] }.toMutableSet()
        
        // Always include base game
        expansionIds.add(1L)
        
        return expansionIds
    }
    
    /**
     * Apply or remove expansion (compatible with GameState.applyExpansion())
     */
    fun applyExpansion(expID: Long, isChecked: Boolean) {
        // Base game (ID 1) is mandatory and always enabled
        if (expID == 1L && !isChecked) {
            Log.w("GameStateAdapter", "Attempted to remove base game (ID 1) - ignoring request")
            return
        }
        
        // For base game, always apply it
        val shouldApply = if (expID == 1L) true else isChecked
        
        // Map expansion ID to name (matching ArkhamInit.java exactly)
        val expansionNameMap = mapOf(
            1L to "BASE",
            2L to "Curse of the Dark Pharoah",
            3L to "Dunwich Horror",
            4L to "The King in Yellow",
            5L to "Kingsport Horror",
            6L to "Black Goat of the Woods",
            7L to "Innsmouth Horror",
            8L to "Lurker at the Threshold",
            9L to "Curse of the Dark Pharoah Revised",
            10L to "Miskatonic Horror"
        )
        
        val expansionName = expansionNameMap[expID] ?: return
        
        // Update game state
        CoroutineScope(Dispatchers.IO).launch {
            deckAdapter.applyExpansion(expansionName, shouldApply)
        }
        
        // Ensure base game is always in selected expansions
        val currentExpansions = gameState.getSelectedExpansions(GameType.ARKHAM).toMutableSet()
        if (!currentExpansions.contains("BASE")) {
            currentExpansions.add("BASE")
            gameState.setSelectedExpansions(GameType.ARKHAM, currentExpansions)
            Log.i("GameStateAdapter", "Base game (BASE) added to selected expansions")
        }
        
        // Clear cached decks when expansions change
        neighborhoodCardsList.clear()
        otherWorldCards = null
        
        Log.i("GameStateAdapter", if (shouldApply) "Applied expansion ID $expID" else "Removed expansion ID $expID")
    }
    
    /**
     * Get deck by neighborhood (compatible with GameState.getDeckByNeighborhood())
     */
    fun getDeckByNeighborhood(neiID: Long): List<NeighborhoodCardAdapter> {
        if (neighborhoodCardsList.containsKey(neiID)) {
            val cached = neighborhoodCardsList[neiID]
            Log.d("GameStateAdapter", "Returning cached deck with ${cached?.size ?: 0} cards")
            return cached ?: emptyList()
        } else {
            val cards = runBlocking {
                ahFactory.getCurrentNeighborhoodsCards(neiID)
            }
            val mutableCards = cards.toMutableList()
            randomize(mutableCards)
            neighborhoodCardsList[neiID] = mutableCards
            return mutableCards
        }
    }
    
    /**
     * Randomize neighborhood deck (compatible with GameState.randomizeNeighborhood())
     */
    fun randomizeNeighborhood(neiID: Long) {
        if (neighborhoodCardsList.containsKey(neiID)) {
            randomize(neighborhoodCardsList[neiID]!!)
        }
    }
    
    /**
     * Randomize a list of cards
     */
    private fun <T> randomize(cards: MutableList<T>) {
        if (cards.isEmpty()) return
        
        Collections.shuffle(cards, rand)
        
        // Additional shuffle algorithm from original GameState
        for (i in 0 until cards.size - 1) {
            val randIdx = rand.nextInt(cards.size - i) + i
            val swap = cards.removeAt(randIdx)
            cards.add(i, swap)
        }
    }
    
    /**
     * Get all other world deck (compatible with GameState.getAllOtherWorldDeck())
     */
    fun getAllOtherWorldDeck(): List<OtherWorldCardAdapter> {
        if (otherWorldCards == null) {
            otherWorldCards = runBlocking {
                ahFactory.getCurrentOtherWorldCards().toMutableList()
            }
        }
        return otherWorldCards ?: emptyList()
    }
    
    /**
     * Create new game (compatible with GameState.newGame())
     */
    fun newGame() {
        neighborhoodCardsList.clear()
        otherWorldCards = null
        
        // Create new game ID
        val gameId = System.currentTimeMillis()
        gameState.newGame(GameType.ARKHAM)
        
        // Apply base game
        applyExpansion(1, true)
        
        Log.i("GameStateAdapter", "New game created")
    }
    
    /**
     * Add selected other world color (compatible with GameState.addSelectedOtherWorldColor())
     */
    fun addSelectedOtherWorldColor(color: OtherWorldColorAdapter) {
        if (!currentColors.containsKey(color.getID())) {
            currentColors[color.getID()] = color
            Log.d("GameStateAdapter", "Added other world color: ${color.getName()}")
        }
    }
    
    /**
     * Remove selected other world color (compatible with GameState.removeSelectedOtherWorldColor())
     */
    fun removeSelectedOtherWorldColor(color: OtherWorldColorAdapter) {
        if (currentColors.containsKey(color.getID())) {
            currentColors.remove(color.getID())
            Log.d("GameStateAdapter", "Removed other world color: ${color.getName()}")
        }
    }
    
    /**
     * Clear selected other world colors (compatible with GameState.clearSelectedOtherWorldColor())
     */
    fun clearSelectedOtherWorldColor() {
        currentColors.clear()
        Log.d("GameStateAdapter", "Cleared other world colors")
    }
    
    /**
     * Check if other world color is selected (compatible with GameState.isSelectedOtherWorldColor())
     */
    fun isSelectedOtherWorldColor(color: OtherWorldColorAdapter): Boolean {
        return currentColors.containsKey(color.getID())
    }
    
    /**
     * Get all selected other world colors
     */
    fun getSelectedOtherWorldColors(): List<OtherWorldColorAdapter> {
        return currentColors.values.toList()
    }
    
    /**
     * Set selected otherworld location ID
     */
    fun setSelectedOtherWorldLocation(locationId: Long?) {
        selectedLocationId = locationId
        Log.d("GameStateAdapter", "Set selected otherworld location: $locationId")
    }
    
    /**
     * Get selected otherworld location ID
     */
    fun getSelectedOtherWorldLocation(): Long? {
        return selectedLocationId
    }
    
    /**
     * Get filtered other world deck (compatible with GameState.getFilteredOtherWorldDeck())
     * 
     * ORIGINAL LOGIC: Filter cards by color only (not by location)
     * - Get all other world cards
     * - Filter to cards that have at least one matching color
     * - Return the filtered list (the caller will pick one randomly)
     */
    fun getFilteredOtherWorldDeck(): List<OtherWorldCardAdapter> {
        val selectedColors = getSelectedOtherWorldColors()
        
        // If no colors selected, return the full deck (fallback behavior)
        if (selectedColors.isEmpty()) {
            Log.d("GameStateAdapter", "No colors selected, returning full deck")
            return getAllOtherWorldDeck()
        }

        val selectedColorIds = selectedColors.map { it.getID() }
        Log.d("GameStateAdapter", "Filtering other world cards by colors: $selectedColorIds")
        
        // Get all other world cards
        val allCards = getAllOtherWorldDeck()
        Log.d("GameStateAdapter", "Total other world cards: ${allCards.size}")
        
        // Filter cards that have at least one matching color
        val filteredCards = allCards.filter { card ->
            // Special case: "Stars are Right" card (ID 4242) is always included
            if (card.getID() == 4242L) {
                true
            } else {
                val cardColors = card.getOtherWorldColors()
                val hasMatchingColor = cardColors.any { cardColor ->
                    selectedColorIds.contains(cardColor.getID())
                }
                hasMatchingColor
            }
        }
        
        Log.d("GameStateAdapter", "Filtered to ${filteredCards.size} cards with matching colors")
        
        if (filteredCards.isEmpty()) {
            Log.w("GameStateAdapter", "No cards found with colors $selectedColorIds, returning full deck")
            return allCards
        }
        
        // Shuffle the filtered cards to ensure randomness
        val shuffledCards = filteredCards.toMutableList()
        Collections.shuffle(shuffledCards, rand)
        
        // Pick one card at random
        val randomIndex = rand.nextInt(shuffledCards.size)
        val selectedCard = shuffledCards[randomIndex]
        Log.d("GameStateAdapter", "Selected random card ${selectedCard.getID()} (index $randomIndex out of ${shuffledCards.size} filtered cards)")
        
        return listOf(selectedCard)
    }
    
    /**
     * Prepare other world deck (compatible with GameState.prepOtherWorldDeck())
     */
    fun prepOtherWorldDeck() {
        otherWorldCards = runBlocking {
            ahFactory.getCurrentOtherWorldCards().toMutableList()
        }
        randomize(otherWorldCards!!)
    }
    
    /**
     * Check if other world card is selected (compatible with GameState.otherWorldCardSelected())
     */
    fun otherWorldCardSelected(cardId: Long): Boolean {
        // TODO: Implement card selection logic
        return false
    }
    
    /**
     * Add encounter to history (compatible with GameState.AddHistory())
     */
    fun AddHistory(encounter: EncounterAdapter) {
        runBlocking {
            withContext(Dispatchers.IO) {
                var gameId = gameState.getCurrentGameId()
                if (gameId == -1L) {
                    // Create new game if none exists
                    gameId = gameState.newGame(GameType.ARKHAM)
                    Log.d("GameStateAdapter", "Created new game for history: $gameId")
                    if (gameId == -1L) {
                        Log.e("GameStateAdapter", "Failed to create new game for history")
                        return@withContext
                    }
                }
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val hxId = db.addEncounterHistory(gameId, encounter.getID(), GameType.ARKHAM)
                if (hxId > 0) {
                    Log.d("GameStateAdapter", "Added encounter ${encounter.getID()} to history (hx_id=$hxId, game_id=$gameId)")
                } else {
                    Log.e("GameStateAdapter", "Failed to add encounter ${encounter.getID()} to history (returned hx_id=$hxId)")
                }
            }
        }
    }
    
    /**
     * Get encounter history (compatible with GameState.getEncounterHx())
     * Returns list of EncounterAdapter objects in order (newest first)
     */
    fun getEncounterHx(): List<EncounterAdapter> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val gameId = gameState.getCurrentGameId()
                if (gameId == -1L) {
                    return@withContext emptyList()
                }
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val encIds = db.getEncounterHistory(gameId, GameType.ARKHAM)
                
                encIds.mapNotNull { encId ->
                    try {
                        val encounterText = ahFactory.getEncounterText(encId)
                        val locId = ahFactory.getLocationIdForEncounter(encId)
                        val location = locId?.let { ahFactory.getLocation(it) }
                        EncounterAdapter(encId, encounterText, location)
                    } catch (e: Exception) {
                        Log.w("GameStateAdapter", "Could not load encounter $encId from history: ${e.message}")
                        null
                    }
                }
            }
        }
    }
    
    /**
     * Remove encounter from history by position (compatible with GameState.removeHx())
     */
    fun removeHx(position: Int) {
        runBlocking {
            withContext(Dispatchers.IO) {
                val gameId = gameState.getCurrentGameId()
                if (gameId == -1L) {
                    return@withContext
                }
                val db = UnifiedCardDatabaseHelper.getInstance(appContext)
                val removed = db.removeEncounterHistory(gameId, position, GameType.ARKHAM)
                if (removed) {
                    Log.d("GameStateAdapter", "Removed encounter at position $position from history")
                } else {
                    Log.w("GameStateAdapter", "Failed to remove encounter at position $position")
                }
            }
        }
    }
    
    /**
     * Add card to history (stores last 20 cards)
     */
    fun addCardHistory(cardId: Long, isOtherWorld: Boolean, neighborhoodId: Long? = null, selectedEncounterId: Long? = null) {
        synchronized(cardHistory) {
            // Remove if already exists (to move to front)
            cardHistory.removeAll { it.cardId == cardId && it.isOtherWorld == isOtherWorld }
            // Add to front (latest first)
            cardHistory.add(0, CardHistoryEntry(cardId, isOtherWorld, neighborhoodId, selectedEncounterId))
            // Keep only last 20
            if (cardHistory.size > MAX_CARD_HISTORY) {
                cardHistory.removeAt(cardHistory.size - 1)
            }
            Log.d("GameStateAdapter", "Added card to history: cardId=$cardId, isOtherWorld=$isOtherWorld, neighborhoodId=$neighborhoodId, selectedEncounterId=$selectedEncounterId (total: ${cardHistory.size})")
        }
    }
    
    /**
     * Update the selected encounter for the most recently added card in history
     */
    fun updateCardHistorySelectedEncounter(encounterId: Long) {
        synchronized(cardHistory) {
            if (cardHistory.isNotEmpty()) {
                cardHistory[0] = cardHistory[0].copy(selectedEncounterId = encounterId)
                Log.d("GameStateAdapter", "Updated selected encounter for latest card: encounterId=$encounterId")
            }
        }
    }
    
    /**
     * Get card history (latest first)
     */
    fun getCardHistory(): List<CardHistoryEntry> {
        synchronized(cardHistory) {
            return cardHistory.toList()
        }
    }
    
    /**
     * Clear card history
     */
    fun clearCardHistory() {
        synchronized(cardHistory) {
            cardHistory.clear()
            Log.d("GameStateAdapter", "Cleared card history")
        }
    }
}


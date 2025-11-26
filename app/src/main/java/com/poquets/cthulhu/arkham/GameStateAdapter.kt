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
        
        // Map expansion names to IDs
        val expansionIdMap = mapOf(
            "BASE" to 1L,
            "Dunwich Horror" to 2L,
            "Kingsport Horror" to 3L,
            "Innsmouth Horror" to 4L,
            "The Black Goat of the Woods" to 5L,
            "The Lurker at the Threshold" to 6L,
            "Curse of the Dark Pharaoh" to 7L,
            "The King in Yellow" to 8L,
            "Miskatonic Horror" to 9L,
            "Kingsport Horror" to 10L
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
        
        // Map expansion ID to name
        val expansionNameMap = mapOf(
            1L to "BASE",
            2L to "Dunwich Horror",
            3L to "Kingsport Horror",
            4L to "Innsmouth Horror",
            5L to "The Black Goat of the Woods",
            6L to "The Lurker at the Threshold",
            7L to "Curse of the Dark Pharaoh",
            8L to "The King in Yellow",
            9L to "Miskatonic Horror",
            10L to "Kingsport Horror"
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
     * NEW LOGIC: Instead of filtering cards by color, we:
     * 1. Find encounters that match the selected location AND card colors
     * 2. Pick ONE encounter at random
     * 3. Return the card containing that encounter
     */
    fun getFilteredOtherWorldDeck(): List<OtherWorldCardAdapter> {
        val selectedColors = getSelectedOtherWorldColors()
        val locationId = getSelectedOtherWorldLocation()
        
        // If no colors or location selected, return the full deck (fallback behavior)
        if (selectedColors.isEmpty() || locationId == null) {
            Log.d("GameStateAdapter", "No colors or location selected, returning full deck")
            return getAllOtherWorldDeck()
        }

        val selectedColorIds = selectedColors.map { it.getID() }
        
        // Find encounters that match the location and colors
        val db = UnifiedCardDatabaseHelper.getInstance(appContext)
        val matchingEncounters = runBlocking {
            withContext(Dispatchers.IO) {
                db.findEncountersByLocationAndColors(locationId, selectedColorIds, GameType.ARKHAM)
            }
        }
        
        if (matchingEncounters.isEmpty()) {
            Log.w("GameStateAdapter", "No encounters found for location $locationId with colors $selectedColorIds, returning full deck")
            return getAllOtherWorldDeck()
        }
        
        // Pick one encounter at random
        val selectedEncounter = matchingEncounters[rand.nextInt(matchingEncounters.size)]
        Log.d("GameStateAdapter", "Selected random encounter ${selectedEncounter.encounterId} from card ${selectedEncounter.cardId} (out of ${matchingEncounters.size} matches)")
        
        // Find the card containing this encounter
        val allCards = getAllOtherWorldDeck()
        val card = allCards.find { it.getID().toString() == selectedEncounter.cardId }
        
        return if (card != null) {
            listOf(card)
        } else {
            Log.w("GameStateAdapter", "Card ${selectedEncounter.cardId} not found in deck, returning full deck")
            getAllOtherWorldDeck()
        }
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
        // TODO: Store encounter in history
        Log.d("GameStateAdapter", "Added encounter to history: ${encounter.getID()}")
    }
}


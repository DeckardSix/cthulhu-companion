package com.poquets.cthulhu.shared.database

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages game state for both games
 * Stores game configuration, selected expansions, etc.
 */
class GameStateManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "cthulhu_game_state",
        Context.MODE_PRIVATE
    )
    
    companion object {
        @Volatile
        private var INSTANCE: GameStateManager? = null
        private val lock = Any()
        
        fun getInstance(context: Context): GameStateManager {
            return INSTANCE ?: synchronized(lock) {
                INSTANCE ?: GameStateManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        fun getCurrentInstance(): GameStateManager? = INSTANCE
        private const val KEY_CURRENT_GAME = "current_game"
        private const val KEY_ARKHAM_EXPANSIONS = "arkham_expansions"
        private const val KEY_ELDRITCH_EXPANSIONS = "eldritch_expansions"
        private const val KEY_GAME_ID = "game_id"
    }
    
    /**
     * Get current active game type
     */
    fun getCurrentGame(): GameType? {
        val gameStr = prefs.getString(KEY_CURRENT_GAME, null)
        return gameStr?.let { GameType.fromString(it) }
    }
    
    /**
     * Set current active game
     */
    fun setCurrentGame(gameType: GameType) {
        prefs.edit().putString(KEY_CURRENT_GAME, gameType.value).apply()
        Log.d("GameStateManager", "Current game set to: ${gameType.value}")
    }
    
    /**
     * Get selected expansions for a game
     */
    fun getSelectedExpansions(gameType: GameType): Set<String> {
        val key = when (gameType) {
            GameType.ARKHAM -> KEY_ARKHAM_EXPANSIONS
            GameType.ELDRITCH -> KEY_ELDRITCH_EXPANSIONS
        }
        val expansionsStr = prefs.getStringSet(key, emptySet()) ?: emptySet()
        return expansionsStr
    }
    
    /**
     * Set selected expansions for a game
     */
    fun setSelectedExpansions(gameType: GameType, expansions: Set<String>) {
        val key = when (gameType) {
            GameType.ARKHAM -> KEY_ARKHAM_EXPANSIONS
            GameType.ELDRITCH -> KEY_ELDRITCH_EXPANSIONS
        }
        prefs.edit().putStringSet(key, expansions).apply()
        Log.d("GameStateManager", "Expansions set for ${gameType.value}: $expansions")
    }
    
    /**
     * Get current game ID
     */
    fun getCurrentGameId(): Long {
        return prefs.getLong(KEY_GAME_ID, -1L)
    }
    
    /**
     * Set current game ID
     */
    fun setCurrentGameId(gameId: Long) {
        prefs.edit().putLong(KEY_GAME_ID, gameId).apply()
    }
    
    /**
     * Create new game
     */
    fun newGame(gameType: GameType): Long {
        val gameId = System.currentTimeMillis()
        setCurrentGameId(gameId)
        setCurrentGame(gameType)
        Log.d("GameStateManager", "New game created: $gameId for ${gameType.value}")
        return gameId
    }
    
    /**
     * Clear game state
     */
    fun clearGameState() {
        prefs.edit().clear().apply()
        Log.d("GameStateManager", "Game state cleared")
    }
}


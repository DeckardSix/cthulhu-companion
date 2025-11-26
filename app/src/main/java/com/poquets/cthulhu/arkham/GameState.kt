package com.poquets.cthulhu.arkham

import android.content.Context

/**
 * Compatibility class for GameState singleton
 * Provides static getInstance() access matching original Arkham API
 */
object GameState {
    
    @Volatile
    private var adapter: GameStateAdapter? = null
    
    /**
     * Get the singleton instance (compatible with GameState.getInstance())
     */
    fun getInstance(context: Context? = null): GameStateAdapter {
        if (adapter == null) {
            if (context == null) {
                throw IllegalStateException("GameState not initialized. Call getInstance(context) first.")
            }
            adapter = GameStateAdapter.getInstance(context)
        }
        return adapter!!
    }
}


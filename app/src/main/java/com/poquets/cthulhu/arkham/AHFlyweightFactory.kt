package com.poquets.cthulhu.arkham

import android.content.Context

/**
 * Compatibility class for AHFlyweightFactory singleton
 * Provides static INSTANCE access matching original Arkham API
 */
object AHFlyweightFactory {
    
    @Volatile
    private var adapter: AHFlyweightFactoryAdapter? = null
    
    /**
     * Get the singleton instance (compatible with AHFlyweightFactory.INSTANCE)
     */
    val INSTANCE: AHFlyweightFactoryAdapter
        get() {
            if (adapter == null) {
                throw IllegalStateException("AHFlyweightFactory not initialized. Call Init(context) first.")
            }
            return adapter!!
        }
    
    /**
     * Initialize the factory (compatible with AHFlyweightFactory.INSTANCE.Init())
     */
    fun Init(context: Context) {
        if (adapter == null) {
            adapter = AHFlyweightFactoryAdapter.getInstance(context)
        }
    }
}


package com.poquets.cthulhu.arkham

import com.poquets.cthulhu.shared.database.GameStateManager
import com.poquets.cthulhu.shared.database.GameType

/**
 * Compatibility adapter for Arkham Expansion class
 */
class ExpansionAdapter(
    private val id: Long,
    private val name: String,
    private val expIconPath: String? = null
) {
    
    fun getID(): Long = id
    
    fun getName(): String = name
    
    fun getExpansionIconPath(): String? = expIconPath
    
    fun getApplied(context: android.content.Context? = null): Boolean {
        // Base game is always applied
        if (id == 1L) return true
        
        // Check if this expansion is in the selected expansions
        val gameState = if (context != null) {
            GameStateManager.getInstance(context)
        } else {
            GameStateManager.getCurrentInstance()
        }
        
        if (gameState == null) return id == 1L // Base game always applied
        
        val selectedExpansions = gameState.getSelectedExpansions(GameType.ARKHAM)
        // Map expansion ID to name
        val expansionName = when (id) {
            1L -> "BASE"
            2L -> "Dunwich Horror"
            3L -> "Kingsport Horror"
            4L -> "Innsmouth Horror"
            5L -> "The Black Goat of the Woods"
            6L -> "The Lurker at the Threshold"
            7L -> "Curse of the Dark Pharaoh"
            8L -> "The King in Yellow"
            9L -> "Miskatonic Horror"
            10L -> "Kingsport Horror"
            else -> name
        }
        return selectedExpansions.contains(expansionName) || selectedExpansions.contains(name)
    }
    
    override fun toString(): String = name
    
    fun getCheckboxOffPath(): String {
        return when (id.toInt()) {
            1 -> "checkbox/btn_ba_check_off.png"
            2 -> "checkbox/btn_dp_check_off.png"
            3 -> "checkbox/btn_dh_check_off.png"
            4 -> "checkbox/btn_ky_check_off.png"
            5 -> "checkbox/btn_kh_check_off.png"
            6 -> "checkbox/btn_bg_check_off.png"
            7 -> "checkbox/btn_ih_check_off.png"
            8 -> "checkbox/btn_lt_check_off.png"
            9 -> "checkbox/btn_dpr_check_off.png"
            10 -> "checkbox/btn_mh_check_off.png"
            else -> "checkbox/btn_dh_check_off.png"
        }
    }
    
    fun getCheckboxOnPath(): String {
        return when (id.toInt()) {
            1 -> "checkbox/btn_ba_check_on.png"
            2 -> "checkbox/btn_dp_check_on.png"
            3 -> "checkbox/btn_dh_check_on.png"
            4 -> "checkbox/btn_ky_check_on.png"
            5 -> "checkbox/btn_kh_check_on.png"
            6 -> "checkbox/btn_bg_check_on.png"
            7 -> "checkbox/btn_ih_check_on.png"
            8 -> "checkbox/btn_lt_check_on.png"
            9 -> "checkbox/btn_dpr_check_on.png"
            10 -> "checkbox/btn_mh_check_on.png"
            else -> "checkbox/btn_dh_check_on.png"
        }
    }
}


package com.poquets.cthulhu.eldritch.GUI

import android.graphics.Color

/**
 * Utility class for card colors based on deck names
 */
object CardColorUtils {
    
    // Color mapping for different deck types
    private const val AMERICAS_COLOR = "#ff00741e"
    private const val EUROPE_COLOR = "#ffc96900"
    private const val ASIA_COLOR = "#ff5a0e8a"
    private const val ANT_WEST_COLOR = "#ffb897bb"
    private const val ANT_EAST_COLOR = "#a1746100"
    private const val AFRICA_COLOR = "#7b5000"
    private const val EGYPT_COLOR = "#b60101"
    private const val DREAMLANDS_COLOR = "#8caa2f"
    private const val GENERAL_COLOR = "#5e000000"
    private const val GATE_COLOR = "#fe00cd5b"
    private const val RESEARCH_COLOR = "#ff000000"
    private const val ANT_RESEARCH_COLOR = "#d98b4512"
    private const val SPECIAL1_COLOR = "#d9578b84"
    private const val SPECIAL2_COLOR = "#d92c8b5e"
    private const val SPECIAL3_COLOR = "#d9075e35"
    private const val DISASTER_COLOR = "#ff0000ff"
    private const val DEVASTATION_COLOR = "#ffff0000"
    private const val DISCARD_COLOR = "#ff3d4876"
    private const val EXPEDITION_COLOR = "#ff1a0077"
    private const val MYSTIC_RUINS_COLOR = "#a21d00f2"
    private const val DREAM_QUEST_COLOR = "#5e30ad"
    
    /**
     * Get the background color for a specific deck
     */
    fun getDeckBackgroundColor(deckName: String?): Int {
        if (deckName == null) {
            return Color.WHITE
        }
        
        return when (deckName.uppercase()) {
            "AMERICAS" -> Color.parseColor(AMERICAS_COLOR)
            "EUROPE" -> Color.parseColor(EUROPE_COLOR)
            "ASIA" -> Color.parseColor(ASIA_COLOR)
            "ANTARCTICA-WEST", "ANTARCTICA_WEST" -> Color.parseColor(ANT_WEST_COLOR)
            "ANTARCTICA-EAST", "ANTARCTICA_EAST" -> Color.parseColor(ANT_EAST_COLOR)
            "AFRICA" -> Color.parseColor(AFRICA_COLOR)
            "EGYPT" -> Color.parseColor(EGYPT_COLOR)
            "DREAMLANDS" -> Color.parseColor(DREAMLANDS_COLOR)
            "GENERAL" -> Color.parseColor(GENERAL_COLOR)
            "GATE" -> Color.parseColor(GATE_COLOR)
            "RESEARCH" -> Color.parseColor(RESEARCH_COLOR)
            "ANTARCTICA-RESEARCH", "ANTARCTICA_RESEARCH" -> Color.parseColor(ANT_RESEARCH_COLOR)
            "SPECIAL-1" -> Color.parseColor(SPECIAL1_COLOR)
            "SPECIAL-2" -> Color.parseColor(SPECIAL2_COLOR)
            "SPECIAL-3" -> Color.parseColor(SPECIAL3_COLOR)
            "DISASTER" -> Color.parseColor(DISASTER_COLOR)
            "DEVASTATION" -> Color.parseColor(DEVASTATION_COLOR)
            "DISCARD" -> Color.parseColor(DISCARD_COLOR)
            "EXPEDITION" -> Color.parseColor(EXPEDITION_COLOR)
            "MYSTIC_RUINS" -> Color.parseColor(MYSTIC_RUINS_COLOR)
            "DREAM-QUEST" -> Color.parseColor(DREAM_QUEST_COLOR)
            else -> Color.WHITE
        }
    }
    
    /**
     * Calculate if text should be black or white based on background color brightness
     */
    fun getTextColorForBackground(backgroundColor: Int): Int {
        val red = Color.red(backgroundColor)
        val green = Color.green(backgroundColor)
        val blue = Color.blue(backgroundColor)
        
        // Calculate luminance
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        
        // Use white text for dark backgrounds, black text for light backgrounds
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }
    
    /**
     * Get text color for a specific deck
     */
    fun getDeckTextColor(deckName: String?): Int {
        val backgroundColor = getDeckBackgroundColor(deckName)
        return getTextColorForBackground(backgroundColor)
    }
    
    /**
     * Get the colored card image path for a specific deck/region
     * Maps regions to Arkham's colored card backgrounds (yellow, red, green, blue)
     */
    fun getColoredCardPath(deckName: String?): String? {
        if (deckName == null) {
            return null
        }
        
        // Map regions to colored cards (cycling through the 4 colors)
        // Using the same colored cards as Arkham:
        // Yellow: encounter_front_miskatonic.png
        // Red: encounter_front_uptown.png
        // Blue: encounter_front_frenchhill.png
        // Green: encounter_front_merchant.png
        return when (deckName.uppercase()) {
            "AMERICAS" -> "encounter/encounter_front_merchant.png"  // Green
            "EUROPE" -> "encounter/encounter_front_miskatonic.png"  // Yellow
            "ASIA" -> "encounter/encounter_front_uptown.png"        // Red
            "AFRICA" -> "encounter/encounter_front_frenchhill.png"  // Blue
            "EGYPT" -> "encounter/encounter_front_merchant.png"      // Green
            "DREAMLANDS" -> "encounter/encounter_front_miskatonic.png" // Yellow
            "ANTARCTICA-WEST", "ANTARCTICA_WEST" -> "encounter/encounter_front_frenchhill.png" // Blue
            "ANTARCTICA-EAST", "ANTARCTICA_EAST" -> "encounter/encounter_front_uptown.png"     // Red
            "ANTARCTICA-RESEARCH", "ANTARCTICA_RESEARCH" -> "encounter/encounter_front_merchant.png" // Green
            "GENERAL" -> "encounter/encounter_front_miskatonic.png"  // Yellow
            "GATE" -> "encounter/encounter_front_uptown.png"         // Red
            "RESEARCH" -> "encounter/encounter_front_frenchhill.png" // Blue
            "SPECIAL-1" -> "encounter/encounter_front_merchant.png"  // Green
            "SPECIAL-2" -> "encounter/encounter_front_miskatonic.png" // Yellow
            "SPECIAL-3" -> "encounter/encounter_front_uptown.png"    // Red
            "DISASTER" -> "encounter/encounter_front_uptown.png"     // Red
            "DEVASTATION" -> "encounter/encounter_front_uptown.png"  // Red
            "DISCARD" -> "encounter/encounter_front_downtown.png"   // Default colorless
            "EXPEDITION" -> "encounter/encounter_front_frenchhill.png" // Blue
            "MYSTIC_RUINS" -> "encounter/encounter_front_merchant.png" // Green
            "DREAM-QUEST" -> "encounter/encounter_front_miskatonic.png" // Yellow
            else -> "encounter/encounter_front_downtown.png"         // Default colorless
        }
    }
}


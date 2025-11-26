package com.poquets.cthulhu.eldritch

/**
 * Compatibility class for Eldritch Config
 * Maintains the same API as the original Config class
 */
object Config {
    @JvmStatic
    var ANCIENT_ONE: String? = null
    
    @JvmStatic
    var ANTARCTICA: Boolean = false
    
    @JvmStatic
    var BASE: Boolean = true  // Default to true
    
    @JvmStatic
    var CITIES_IN_RUIN: Boolean = false
    
    @JvmStatic
    var COSMIC_ALIGNMENT: Boolean = false
    
    @JvmStatic
    var DREAMLANDS_BOARD: Boolean = false
    
    @JvmStatic
    var EGYPT: Boolean = false
    
    @JvmStatic
    var FORSAKEN_LORE: Boolean = false
    
    @JvmStatic
    var LITANY_OF_SECRETS: Boolean = false
    
    @JvmStatic
    var MASKS_OF_NYARLATHOTEP: Boolean = false
    
    @JvmStatic
    var MOUNTAINS_OF_MADNESS: Boolean = false
    
    @JvmStatic
    var SIGNS_OF_CARCOSA: Boolean = false
    
    @JvmStatic
    var SPECIAL1: String? = null
    
    @JvmStatic
    var SPECIAL2: String? = null
    
    @JvmStatic
    var SPECIAL3: String? = null
    
    @JvmStatic
    var STRANGE_REMNANTS: Boolean = false
    
    @JvmStatic
    var THE_DREAMLANDS: Boolean = false
    
    @JvmStatic
    var UNDER_THE_PYRAMIDS: Boolean = false
    
    /**
     * Get list of enabled expansion names
     */
    fun getEnabledExpansions(): List<String> {
        val expansions = mutableListOf<String>()
        if (BASE) expansions.add("BASE")
        if (FORSAKEN_LORE) expansions.add("FORSAKEN_LORE")
        if (MOUNTAINS_OF_MADNESS) expansions.add("MOUNTAINS_OF_MADNESS")
        if (STRANGE_REMNANTS) expansions.add("STRANGE_REMNANTS")
        if (UNDER_THE_PYRAMIDS) expansions.add("UNDER_THE_PYRAMIDS")
        if (SIGNS_OF_CARCOSA) expansions.add("SIGNS_OF_CARCOSA")
        if (THE_DREAMLANDS) expansions.add("THE_DREAMLANDS")
        if (CITIES_IN_RUIN) expansions.add("CITIES_IN_RUIN")
        if (MASKS_OF_NYARLATHOTEP) expansions.add("MASKS_OF_NYARLATHOTEP")
        return expansions
    }
}


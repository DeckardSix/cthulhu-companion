package com.poquets.cthulhu.eldritch.GUI

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.eldritch.Config
import com.poquets.cthulhu.eldritch.DecksAdapter
import com.poquets.cthulhu.shared.database.UnifiedCard
import com.poquets.cthulhu.shared.ui.DisclaimerHelper
import java.io.File

/**
 * Main game activity for Eldritch Horror
 * Displays region buttons and handles card drawing
 */
class EldritchCompanion : AppCompatActivity() {
    
    private var expeditionButton: Button? = null
    private var mysticRuinsButton: Button? = null
    private var dreamQuestButton: Button? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eldritch_companion)
        
        // Add padding at top to account for status bar
        val rootLayout = findViewById<android.widget.RelativeLayout>(R.id.rootLayout)
        val statusBarHeight = getStatusBarHeight()
        rootLayout?.setPadding(0, statusBarHeight, 0, 0)
        
        // Add back button positioned like question mark in expansion selector
        addBackButton()
        
        // Ensure ActionBar is properly displayed
        supportActionBar?.show()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Ensure ScrollView is visible
        val mainScrollView = findViewById<android.widget.ScrollView>(R.id.mainScrollView)
        mainScrollView?.visibility = View.VISIBLE
        mainScrollView?.isVerticalScrollBarEnabled = false
        
        // Add header icons (question mark and settings)
        addHeaderIcons()
        
        // Get references to special buttons
        expeditionButton = findViewById(R.id.expeditionButton)
        mysticRuinsButton = findViewById(R.id.mysticRuinsButton)
        dreamQuestButton = findViewById(R.id.dreamQuestButton)
        
        // Set click listeners for all region buttons
        setupRegionButtons()
        
        // Set button visibility based on Config
        updateButtonVisibility()
        
        // Set title
        val ancientOne = Config.ANCIENT_ONE?.replace("_", " ")?.replace(".", "'")
        title = ancientOne ?: "Eldritch Companion"
    }
    
    private fun setupRegionButtons() {
        // Base regions
        findViewById<Button>(R.id.americasButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.europeButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.asiaButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.generalButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.gateButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.researchButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.discardButton)?.setOnClickListener { drawCard(it) }
        
        // Expansion-specific regions
        findViewById<Button>(R.id.africaButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.egyptButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.dreamlandsButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.antWestButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.antEastButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.antResearchButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.special1Button)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.special2Button)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.special3Button)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.disasterButton)?.setOnClickListener { drawCard(it) }
        findViewById<Button>(R.id.devastationButton)?.setOnClickListener { drawCard(it) }
        
        // Special decks
        expeditionButton?.setOnClickListener { drawCard(it) }
        mysticRuinsButton?.setOnClickListener { drawCard(it) }
        dreamQuestButton?.setOnClickListener { drawCard(it) }
        
        // Remove expedition button
        findViewById<Button>(R.id.removeButton)?.setOnClickListener { removeExpedition(it) }
    }
    
    private fun updateButtonVisibility() {
        val decks = DecksAdapter.CARDS
        
        // Helper function to hide button and its parent FrameLayout if deck is empty
        fun hideIfEmpty(buttonId: Int, deckName: String) {
            val button = findViewById<View>(buttonId)
            if (button != null) {
                val hasCards = decks?.getDeck(deckName)?.isNotEmpty() == true
                val parent = button.parent as? View
                if (hasCards) {
                    button.visibility = View.VISIBLE
                    parent?.visibility = View.VISIBLE
                } else {
                    button.visibility = View.GONE
                    parent?.visibility = View.GONE
                }
            }
        }
        
        // Base location buttons - check if they have cards
        hideIfEmpty(R.id.americasButton, "AMERICAS")
        hideIfEmpty(R.id.europeButton, "EUROPE")
        hideIfEmpty(R.id.asiaButton, "ASIA")
        hideIfEmpty(R.id.generalButton, "GENERAL")
        hideIfEmpty(R.id.gateButton, "GATE")
        hideIfEmpty(R.id.researchButton, "RESEARCH")
        hideIfEmpty(R.id.discardButton, "DISCARD")
        
        // Expedition button - check if it has cards and base expansion is selected
        if (Config.BASE && decks != null) {
            val hasCards = decks.getDeck("EXPEDITION").isNotEmpty()
            expeditionButton?.visibility = if (hasCards) View.VISIBLE else View.GONE
            // Hide parent FrameLayout if empty
            expeditionButton?.parent?.let { parent ->
                if (parent is View) {
                    parent.visibility = if (hasCards) View.VISIBLE else View.GONE
                }
            }
        } else {
            expeditionButton?.visibility = View.GONE
            expeditionButton?.parent?.let { parent ->
                if (parent is View) {
                    parent.visibility = View.GONE
                }
            }
        }
        
        // Antarctica buttons - check expansion AND if they have cards
        // Antarctica is part of Mountains of Madness expansion
        if (Config.ANTARCTICA || Config.MOUNTAINS_OF_MADNESS || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Rise_of_the_Elder_Things")) {
            hideIfEmpty(R.id.antWestButton, "ANTARCTICA_WEST")
            hideIfEmpty(R.id.antEastButton, "ANTARCTICA_EAST")
            hideIfEmpty(R.id.antResearchButton, "ANTARCTICA_RESEARCH")
        } else {
            findViewById<View>(R.id.antWestButton)?.visibility = View.GONE
            findViewById<View>(R.id.antWestButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
            findViewById<View>(R.id.antEastButton)?.visibility = View.GONE
            findViewById<View>(R.id.antEastButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
            findViewById<View>(R.id.antResearchButton)?.visibility = View.GONE
            findViewById<View>(R.id.antResearchButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
        }
        
        // Egypt buttons - check expansion AND if they have cards
        if (Config.EGYPT || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Nephren-Ka")) {
            hideIfEmpty(R.id.africaButton, "AFRICA")
            hideIfEmpty(R.id.egyptButton, "EGYPT")
        } else {
            findViewById<View>(R.id.africaButton)?.visibility = View.GONE
            findViewById<View>(R.id.africaButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
            findViewById<View>(R.id.egyptButton)?.visibility = View.GONE
            findViewById<View>(R.id.egyptButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
        }
        
        // Dreamlands buttons - check expansion AND if they have cards
        if (Config.DREAMLANDS_BOARD || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Hypnos")) {
            hideIfEmpty(R.id.dreamlandsButton, "DREAMLANDS")
            dreamQuestButton?.let { button ->
                if (decks != null) {
                    val hasCards = decks.getDeck("DREAM_QUEST").isNotEmpty() // Normalized: hyphens to underscores
                    button.visibility = if (hasCards) View.VISIBLE else View.GONE
                    button.parent?.let { parent ->
                        if (parent is View) {
                            parent.visibility = if (hasCards) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        } else {
            findViewById<View>(R.id.dreamlandsButton)?.visibility = View.GONE
            findViewById<View>(R.id.dreamlandsButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
            dreamQuestButton?.visibility = View.GONE
            dreamQuestButton?.parent?.let { if (it is View) it.visibility = View.GONE }
        }
        
        // Mystic Ruins button - check expansion AND if it has cards
        if (Config.COSMIC_ALIGNMENT || (Config.ANCIENT_ONE != null && 
                (Config.ANCIENT_ONE == "Syzygy" || Config.ANCIENT_ONE == "Antediluvium"))) {
            mysticRuinsButton?.let { button ->
                if (decks != null) {
                    val hasCards = decks.getDeck("MYSTIC_RUINS").isNotEmpty()
                    button.visibility = if (hasCards) View.VISIBLE else View.GONE
                    button.parent?.let { parent ->
                        if (parent is View) {
                            parent.visibility = if (hasCards) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        } else {
            mysticRuinsButton?.visibility = View.GONE
            mysticRuinsButton?.parent?.let { if (it is View) it.visibility = View.GONE }
        }
        
        // Special buttons - only visible if decks exist and have cards from selected expansions
        // Note: Deck names are normalized (hyphens to underscores) in the database
        // So "SPECIAL-1" becomes "SPECIAL_1" in the database
        // Also filter by expansion to ensure only cards from selected expansions are shown
        updateSpecialButton(R.id.special1Button, "SPECIAL_1", decks)
        updateSpecialButton(R.id.special2Button, "SPECIAL_2", decks)
        updateSpecialButton(R.id.special3Button, "SPECIAL_3", decks)
        
        // Disaster buttons - check expansion AND if they have cards
        if (Config.CITIES_IN_RUIN) {
            hideIfEmpty(R.id.disasterButton, "DISASTER")
            hideIfEmpty(R.id.devastationButton, "DEVASTATION")
        } else {
            findViewById<View>(R.id.disasterButton)?.visibility = View.GONE
            findViewById<View>(R.id.disasterButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
            findViewById<View>(R.id.devastationButton)?.visibility = View.GONE
            findViewById<View>(R.id.devastationButton)?.parent?.let { if (it is View) it.visibility = View.GONE }
        }
    }
    
    fun drawCard(view: View) {
        // Map button IDs to deck names
        val deckName = when (view.id) {
            R.id.africaButton -> "AFRICA"
            R.id.americasButton -> "AMERICAS"
            R.id.antEastButton -> "ANTARCTICA_EAST"
            R.id.antResearchButton -> "ANTARCTICA_RESEARCH"
            R.id.antWestButton -> "ANTARCTICA_WEST"
            R.id.asiaButton -> "ASIA"
            R.id.devastationButton -> "DEVASTATION"
            R.id.disasterButton -> "DISASTER"
            R.id.discardButton -> "DISCARD"
            R.id.dreamQuestButton -> "DREAM_QUEST" // Normalized: hyphens to underscores
            R.id.dreamlandsButton -> "DREAMLANDS"
            R.id.egyptButton -> "EGYPT"
            R.id.europeButton -> "EUROPE"
            R.id.expeditionButton -> "EXPEDITION"
            R.id.gateButton -> "GATE"
            R.id.generalButton -> "GENERAL"
            R.id.mysticRuinsButton -> "MYSTIC_RUINS"
            R.id.researchButton -> "RESEARCH"
            R.id.special1Button -> "SPECIAL_1" // Normalized: hyphens to underscores
            R.id.special2Button -> "SPECIAL_2" // Normalized: hyphens to underscores
            R.id.special3Button -> "SPECIAL_3"  // Normalized: hyphens to underscores
            else -> null
        }
        
        if (deckName == null) {
            Log.w("EldritchCompanion", "Unknown button clicked: ${view.id}")
            return
        }
        
        if (deckName == "DISCARD") {
            val discardDeck = DecksAdapter.CARDS?.getDeck("DISCARD")
            if (discardDeck == null || discardDeck.isEmpty()) {
                Toast.makeText(applicationContext, "Discard Pile is Empty.", Toast.LENGTH_SHORT).show()
                return
            }
            // Launch DiscardGallery
            val intent = Intent(this, DiscardGallery::class.java)
            intent.putExtra("DECK", "DISCARD")
            startActivity(intent)
            return
        }
        
        // Launch DeckGallery with deck name
        val intent = Intent(this, DeckGallery::class.java)
        intent.putExtra("DECK", deckName)
        startActivity(intent)
    }
    
    fun removeExpedition(view: View) {
        val location = DecksAdapter.CARDS?.getExpeditionLocation()
        if (location == null || location == "EMPTY") {
            Toast.makeText(applicationContext, "Expedition Deck is Empty.", Toast.LENGTH_SHORT).show()
        } else {
            // Launch RemoveExpedition activity
            val intent = Intent(this, RemoveExpedition::class.java)
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Update button text with current locations
        DecksAdapter.CARDS?.let { decks ->
            decks.printDecks()
            
            expeditionButton?.text = "EXPEDITION [${decks.getExpeditionLocation() ?: "EMPTY"}]"
            
            if (mysticRuinsButton != null && 
                (Config.COSMIC_ALIGNMENT || (Config.ANCIENT_ONE != null && 
                    (Config.ANCIENT_ONE == "Syzygy" || Config.ANCIENT_ONE == "Antediluvium")))) {
                mysticRuinsButton?.text = "Mystic Ruins [${decks.getMysticRuinsLocation() ?: "EMPTY"}]"
            }
            
            if (dreamQuestButton != null && 
                (Config.DREAMLANDS_BOARD || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Hypnos"))) {
                dreamQuestButton?.text = "Dream-Quest [${decks.getDreamQuestLocation() ?: "EMPTY"}]"
            }
        }
        
        // Update button visibility based on available decks
        updateButtonVisibility()
        
        // Save game state
        saveGame()
    }
    
    private fun saveGame() {
        // TODO: Implement game state saving
        // For now, just create the marker file
        try {
            File(filesDir, "discard.xml").createNewFile()
        } catch (e: Exception) {
            Log.e("EldritchCompanion", "Unable to create save file: ${e.message}", e)
        }
    }
    
    private fun addHeaderIcons() {
        val questionMarkFrameLayout = findViewById<FrameLayout>(R.id.questionMarkIconFrameLayout)
        val settingsFrameLayout = findViewById<FrameLayout>(R.id.settingsIconFrameLayout)
        
        val iconSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics
        ).toInt()
        val paddingPx = 5
        
        // Question mark icon (help/disclaimer)
        if (questionMarkFrameLayout != null) {
            val questionMarkIcon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_help)
                contentDescription = "Show Disclaimer"
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                
                val totalSize = iconSize + (paddingPx * 2)
                layoutParams = FrameLayout.LayoutParams(totalSize, totalSize)
                setColorFilter(0xFFFFFFFF.toInt()) // White
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    DisclaimerHelper.showDisclaimer(
                        this@EldritchCompanion,
                        showCheckbox = true,
                        backgroundResId = R.drawable.bg_test_splash
                    )
                }
            }
            questionMarkFrameLayout.addView(questionMarkIcon)
        }
        
        // Settings icon (gear) - link to database management
        if (settingsFrameLayout != null) {
            val settingsIcon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_preferences)
                contentDescription = "Database Management"
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                
                val totalSize = iconSize + (paddingPx * 2)
                layoutParams = FrameLayout.LayoutParams(totalSize, totalSize)
                setColorFilter(0xFFFFFFFF.toInt()) // White
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(this@EldritchCompanion, com.poquets.cthulhu.GUI.DatabaseManagementActivity::class.java)
                    startActivity(intent)
                }
            }
            settingsFrameLayout.addView(settingsIcon)
        }
        
        Log.d("EldritchCompanion", "Header icons added to top bar")
    }
    
    private fun addBackButton() {
        val backButtonFrameLayout: FrameLayout? = findViewById(R.id.backButtonFrameLayout)
        
        if (backButtonFrameLayout != null) {
            val paddingPx = 5
            val iconSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32f,
                resources.displayMetrics
            ).toInt()
            
            val backButton = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_revert)
                contentDescription = "Back"
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                
                val totalSize = iconSize + (paddingPx * 2)
                layoutParams = FrameLayout.LayoutParams(totalSize, totalSize).apply {
                    gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                }
                
                setColorFilter(0xFFFFFFFF.toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    finish()
                }
            }
            
            backButtonFrameLayout.addView(backButton)
        }
    }
    
    /**
     * Update SPECIAL button with expansion icon and card name
     */
    private fun updateSpecialButton(buttonId: Int, deckName: String, decks: DecksAdapter?) {
        val specialDeck = decks?.getDeck(deckName) ?: emptyList()
        val specialFiltered = specialDeck.filter { card ->
            val cardExpansion = card.expansion ?: "BASE"
            Config.getEnabledExpansions().contains(cardExpansion)
        }
        
        val button = findViewById<Button>(buttonId)
        if (specialFiltered.isNotEmpty() && button != null) {
            button.visibility = View.VISIBLE
            val card = specialFiltered[0]
            val cardName = card.topHeader ?: deckName
            val expansionName = card.expansion ?: "BASE"
            
            // Get expansion icon path
            val iconPath = getExpansionIconPath(expansionName)
            if (iconPath != null) {
                try {
                    // Load icon from assets
                    val inputStream = assets.open(iconPath)
                    val iconBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    if (iconBitmap != null) {
                        // Scale icon to appropriate size for button text
                        val iconSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
                        ).toInt()
                        val scaledIcon = android.graphics.Bitmap.createScaledBitmap(
                            iconBitmap, iconSize, iconSize, true
                        )
                        
                        // Create ImageSpan with the icon
                        val drawable = BitmapDrawable(resources, scaledIcon)
                        drawable.setBounds(0, 0, iconSize, iconSize)
                        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
                        
                        // Create SpannableString with icon + space + text
                        val spannable = SpannableString("  $cardName")
                        spannable.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        
                        button.text = spannable
                    } else {
                        // Fallback to text only if icon loading fails
                        button.text = cardName
                    }
                } catch (e: Exception) {
                    Log.w("EldritchCompanion", "Could not load expansion icon from $iconPath: ${e.message}")
                    // Fallback to text only
                    button.text = cardName
                }
            } else {
                // No icon path, just use text
                button.text = cardName
            }
            
            button.parent?.let { if (it is View) it.visibility = View.VISIBLE }
        } else {
            button?.visibility = View.GONE
            button?.parent?.let { if (it is View) it.visibility = View.GONE }
        }
    }
    
    /**
     * Get expansion icon path from expansion name
     */
    private fun getExpansionIconPath(expansionName: String): String? {
        return when (expansionName.uppercase()) {
            "BASE" -> "checkbox/btn_ba_check_on.png" // Use Arkham base icon for Eldritch base game
            "FORSAKEN_LORE" -> "expansion/icon_exp_fl.png"
            "MOUNTAINS_OF_MADNESS" -> "expansion/icon_exp_mom.png"
            "ANTARCTICA" -> "expansion/icon_exp_mom.png" // Antarctica uses MoM icon
            "STRANGE_REMNANTS" -> "expansion/icon_exp_sr.png"
            "COSMIC_ALIGNMENT" -> "expansion/icon_exp_sr.png" // Cosmic Alignment uses SR icon
            "UNDER_THE_PYRAMIDS" -> "expansion/icon_exp_utp.png"
            "EGYPT" -> "expansion/icon_exp_utp.png" // Egypt uses UtP icon
            "LITANY_OF_SECRETS" -> "expansion/icon_exp_utp.png" // Litany uses UtP icon
            "SIGNS_OF_CARCOSA" -> "expansion/icon_exp_soc.png"
            "THE_DREAMLANDS" -> "expansion/icon_exp_td.png"
            "DREAMLANDS" -> "expansion/icon_exp_td.png" // Dreamlands uses TD icon
            "CITIES_IN_RUIN" -> "expansion/icon_exp_cir.png"
            "MASKS_OF_NYARLATHOTEP" -> "expansion/icon_exp_mon.png"
            else -> null
        }
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        // If we can't get it from resources, use a default value (typically 24dp on modern devices)
        if (result == 0) {
            result = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                resources.displayMetrics
            ).toInt()
        }
        return result
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Menu can be added later if needed
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


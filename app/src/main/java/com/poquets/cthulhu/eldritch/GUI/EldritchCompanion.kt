package com.poquets.cthulhu.eldritch.GUI

import android.content.Intent
import android.os.Bundle
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
        // Ensure base location buttons are always visible
        findViewById<View>(R.id.americasButton)?.visibility = View.VISIBLE
        findViewById<View>(R.id.europeButton)?.visibility = View.VISIBLE
        findViewById<View>(R.id.asiaButton)?.visibility = View.VISIBLE
        findViewById<View>(R.id.generalButton)?.visibility = View.VISIBLE
        findViewById<View>(R.id.gateButton)?.visibility = View.VISIBLE
        findViewById<View>(R.id.researchButton)?.visibility = View.VISIBLE
        findViewById<View>(R.id.discardButton)?.visibility = View.VISIBLE
        
        // Expedition button - always visible if base expansion is selected
        if (Config.BASE) {
            expeditionButton?.visibility = View.VISIBLE
        }
        
        // Antarctica buttons
        if (!(Config.ANTARCTICA || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Rise_of_the_Elder_Things"))) {
            findViewById<View>(R.id.antWestButton)?.visibility = View.GONE
            findViewById<View>(R.id.antEastButton)?.visibility = View.GONE
            findViewById<View>(R.id.antResearchButton)?.visibility = View.GONE
        }
        
        // Egypt buttons
        if (!(Config.EGYPT || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Nephren-Ka"))) {
            findViewById<View>(R.id.africaButton)?.visibility = View.GONE
            findViewById<View>(R.id.egyptButton)?.visibility = View.GONE
        }
        
        // Dreamlands buttons
        if (!(Config.DREAMLANDS_BOARD || (Config.ANCIENT_ONE != null && Config.ANCIENT_ONE == "Hypnos"))) {
            findViewById<View>(R.id.dreamlandsButton)?.visibility = View.GONE
            dreamQuestButton?.visibility = View.GONE
        }
        
        // Mystic Ruins button
        if (!(Config.COSMIC_ALIGNMENT || (Config.ANCIENT_ONE != null && 
                (Config.ANCIENT_ONE == "Syzygy" || Config.ANCIENT_ONE == "Antediluvium")))) {
            mysticRuinsButton?.visibility = View.GONE
        }
        
        // Special buttons - only visible if decks exist
        if (DecksAdapter.CARDS == null || !DecksAdapter.CARDS!!.containsDeck("SPECIAL-1")) {
            findViewById<View>(R.id.special1Button)?.visibility = View.GONE
        }
        if (DecksAdapter.CARDS == null || !DecksAdapter.CARDS!!.containsDeck("SPECIAL-2")) {
            findViewById<View>(R.id.special2Button)?.visibility = View.GONE
        }
        if (DecksAdapter.CARDS == null || !DecksAdapter.CARDS!!.containsDeck("SPECIAL-3")) {
            findViewById<View>(R.id.special3Button)?.visibility = View.GONE
        }
        
        // Disaster buttons
        if (!Config.CITIES_IN_RUIN) {
            findViewById<View>(R.id.disasterButton)?.visibility = View.GONE
            findViewById<View>(R.id.devastationButton)?.visibility = View.GONE
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
            R.id.dreamQuestButton -> "DREAM-QUEST"
            R.id.dreamlandsButton -> "DREAMLANDS"
            R.id.egyptButton -> "EGYPT"
            R.id.europeButton -> "EUROPE"
            R.id.expeditionButton -> "EXPEDITION"
            R.id.gateButton -> "GATE"
            R.id.generalButton -> "GENERAL"
            R.id.mysticRuinsButton -> "MYSTIC_RUINS"
            R.id.researchButton -> "RESEARCH"
            R.id.special1Button -> "SPECIAL-1"
            R.id.special2Button -> "SPECIAL-2"
            R.id.special3Button -> "SPECIAL-3"
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
        val headerFrameLayout = findViewById<FrameLayout>(R.id.headerIconsFrameLayout)
        
        if (headerFrameLayout != null) {
            val iconSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics
            ).toInt()
            val margin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
            ).toInt()
            val spacing = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics
            ).toInt()
            
            // Question mark icon (help/disclaimer)
            val questionMarkIcon = ImageView(this)
            questionMarkIcon.setImageResource(android.R.drawable.ic_menu_help)
            questionMarkIcon.contentDescription = "Show Disclaimer"
            
            val questionParams = FrameLayout.LayoutParams(iconSize, iconSize)
            questionParams.gravity = android.view.Gravity.TOP or android.view.Gravity.END
            questionParams.setMargins(0, margin, margin, 0)
            questionMarkIcon.layoutParams = questionParams
            questionMarkIcon.setColorFilter(0xFFFFFFFF.toInt()) // White
            questionMarkIcon.isClickable = true
            questionMarkIcon.isFocusable = true
            questionMarkIcon.setOnClickListener {
                DisclaimerHelper.showDisclaimer(
                    this,
                    showCheckbox = true,
                    backgroundResId = R.drawable.bg_test_splash
                )
            }
            
            // Settings icon (gear) - link to database management
            val settingsIcon = ImageView(this)
            settingsIcon.setImageResource(android.R.drawable.ic_menu_preferences)
            settingsIcon.contentDescription = "Database Management"
            
            val settingsParams = FrameLayout.LayoutParams(iconSize, iconSize)
            settingsParams.gravity = android.view.Gravity.TOP or android.view.Gravity.END
            val settingsTopMargin = margin + iconSize + spacing
            settingsParams.setMargins(0, settingsTopMargin, margin, 0)
            settingsIcon.layoutParams = settingsParams
            settingsIcon.setColorFilter(0xFFFFFFFF.toInt()) // White
            settingsIcon.isClickable = true
            settingsIcon.isFocusable = true
            settingsIcon.setOnClickListener {
                val intent = Intent(this, com.poquets.cthulhu.GUI.DatabaseManagementActivity::class.java)
                startActivity(intent)
            }
            
            headerFrameLayout.addView(questionMarkIcon)
            headerFrameLayout.addView(settingsIcon)
            
            Log.d("EldritchCompanion", "Header icons added")
        }
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


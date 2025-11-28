package com.poquets.cthulhu.eldritch.GUI

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.CompoundButtonCompat
import com.poquets.cthulhu.R
import com.poquets.cthulhu.eldritch.Config
import com.poquets.cthulhu.eldritch.DecksAdapter
import com.poquets.cthulhu.shared.database.DatabaseInitializer
import com.poquets.cthulhu.shared.database.GameStateManager
import com.poquets.cthulhu.shared.database.GameType
import com.poquets.cthulhu.shared.ui.DisclaimerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Setup activity for Eldritch Horror
 * Handles expansion selection and game initialization
 */
class Setup : AppCompatActivity() {
    
    private lateinit var baseBox: CheckBox
    private lateinit var forsakenLoreBox: CheckBox
    private lateinit var mountainsOfMadnessBox: CheckBox
    private lateinit var antarcticaBox: CheckBox
    private lateinit var strangeRemnantsBox: CheckBox
    private lateinit var cosmicAlignmentBox: CheckBox
    private lateinit var underThePyramidsBox: CheckBox
    private lateinit var egyptBox: CheckBox
    private lateinit var litanyOfSecretsBox: CheckBox
    private lateinit var signsOfCarcosaBox: CheckBox
    private lateinit var theDreamlandsBox: CheckBox
    private lateinit var dreamlandsBoardBox: CheckBox
    private lateinit var citiesInRuinBox: CheckBox
    private lateinit var masksOfNyarlathotepBox: CheckBox
    
    private lateinit var ancientOneSpinner: Spinner
    private lateinit var continueButton: Button
    private lateinit var startButton: Button
    
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private lateinit var gameState: GameStateManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        
        // Initialize GameStateManager after Activity is ready
        gameState = GameStateManager.getInstance(this)
        
        // Initialize UI components
        initializeViews()
        setupCheckboxes()
        setupSpinner()
        setupButtons()
        
        // Show disclaimer on first launch
        showDisclaimerIfNeeded()
        
        // Initialize database if needed
        initializeDatabase()
    }
    
    private fun initializeViews() {
        baseBox = findViewById(R.id.baseBox)
        forsakenLoreBox = findViewById(R.id.forsakenLoreBox)
        mountainsOfMadnessBox = findViewById(R.id.mountainsOfMadnessBox)
        antarcticaBox = findViewById(R.id.antarcticaBox)
        strangeRemnantsBox = findViewById(R.id.strangeRemnantsBox)
        cosmicAlignmentBox = findViewById(R.id.cosmicAlignmentBox)
        underThePyramidsBox = findViewById(R.id.underThePyramidsBox)
        egyptBox = findViewById(R.id.egyptBox)
        litanyOfSecretsBox = findViewById(R.id.litanyOfSecretsBox)
        signsOfCarcosaBox = findViewById(R.id.signsOfCarcosaBox)
        theDreamlandsBox = findViewById(R.id.theDreamlandsBox)
        dreamlandsBoardBox = findViewById(R.id.dreamlandsBoardBox)
        citiesInRuinBox = findViewById(R.id.citiesInRuinBox)
        masksOfNyarlathotepBox = findViewById(R.id.masksOfNyarlathotepBox)
        
        ancientOneSpinner = findViewById(R.id.ancientOneSpinner)
        continueButton = findViewById(R.id.continueButton)
        startButton = findViewById(R.id.startButton)
        
        // Set font for headers
        val font = Typeface.createFromAsset(assets, "fonts/se-caslon-ant.ttf")
        findViewById<TextView>(R.id.expanHeader)?.typeface = font
        findViewById<TextView>(R.id.ancientHeader)?.typeface = font
    }
    
    private fun setupCheckboxes() {
        val checkboxes = listOf(
            baseBox, forsakenLoreBox, mountainsOfMadnessBox, antarcticaBox,
            strangeRemnantsBox, cosmicAlignmentBox, underThePyramidsBox,
            egyptBox, litanyOfSecretsBox, signsOfCarcosaBox, theDreamlandsBox,
            dreamlandsBoardBox, citiesInRuinBox, masksOfNyarlathotepBox
        )
        
        checkboxes.forEach { checkbox ->
            setCheckboxWhite(checkbox)
        }
        
        // Set expansion icons
        setExpansionIcon(forsakenLoreBox, "icon_exp_fl")
        setExpansionIcon(mountainsOfMadnessBox, "icon_exp_mom")
        setExpansionIcon(antarcticaBox, "icon_exp_mom") // Antarctica uses MoM icon
        setExpansionIcon(strangeRemnantsBox, "icon_exp_sr")
        setExpansionIcon(cosmicAlignmentBox, "icon_exp_sr") // Cosmic Alignment uses SR icon
        setExpansionIcon(underThePyramidsBox, "icon_exp_utp")
        setExpansionIcon(egyptBox, "icon_exp_utp") // Egypt uses UtP icon
        setExpansionIcon(litanyOfSecretsBox, "icon_exp_utp") // Litany uses UtP icon
        setExpansionIcon(signsOfCarcosaBox, "icon_exp_soc")
        setExpansionIcon(theDreamlandsBox, "icon_exp_td")
        setExpansionIcon(dreamlandsBoardBox, "icon_exp_td") // Dreamlands Board uses TD icon
        setExpansionIcon(citiesInRuinBox, "icon_exp_cir")
        setExpansionIcon(masksOfNyarlathotepBox, "icon_exp_mon")
        
        // Default: BASE is checked
        baseBox.isChecked = true
    }
    
    private fun setCheckboxWhite(checkbox: CheckBox) {
        checkbox.setTextColor(android.graphics.Color.WHITE)
        CompoundButtonCompat.setButtonTintList(
            checkbox,
            android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        )
    }
    
    private fun setExpansionIcon(checkbox: CheckBox, iconName: String) {
        try {
            // Try to load from assets first (like Arkham expansion icons)
            val assetPath = "expansion/$iconName.png"
            try {
                val inputStream = assets.open(assetPath)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    val iconSize = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
                    ).toInt()
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
                    val drawable = android.graphics.drawable.BitmapDrawable(resources, scaledBitmap)
                    drawable.setBounds(0, 0, iconSize, iconSize)
                    checkbox.setCompoundDrawables(drawable, null, null, null)
                    checkbox.compoundDrawablePadding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                    ).toInt()
                    Log.d("Setup", "Successfully loaded icon: $iconName from assets")
                    return
                }
            } catch (e: java.io.IOException) {
                Log.d("Setup", "Icon not found in assets: $assetPath, trying drawable resources")
            }
            
            // Fallback to drawable resources
            val iconId = resources.getIdentifier(iconName, "drawable", packageName)
            if (iconId != 0) {
                val icon = resources.getDrawable(iconId, theme)
                val iconSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
                ).toInt()
                icon.setBounds(0, 0, iconSize, iconSize)
                checkbox.setCompoundDrawables(icon, null, null, null)
                checkbox.compoundDrawablePadding = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                ).toInt()
                Log.d("Setup", "Successfully loaded icon: $iconName from drawable resources")
            } else {
                Log.d("Setup", "Icon not found: $iconName (tried assets/expansion/$iconName.png and drawable/$iconName)")
            }
        } catch (e: Exception) {
            Log.w("Setup", "Error setting icon $iconName: ${e.message}", e)
        }
    }
    
    private fun setupSpinner() {
        val ancientOnes = listOf(
            "Random", "Azathoth", "Cthulhu", "Shub-Niggurath", "Yog-Sothoth",
            "Rise of the Elder Things", "Nephren-Ka"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ancientOnes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ancientOneSpinner.adapter = adapter
    }
    
    private fun setupButtons() {
        startButton.setOnClickListener {
            startGame()
        }
        
        continueButton.setOnClickListener {
            continueGame()
        }
    }
    
    private fun showDisclaimerIfNeeded() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val disclaimerShown = prefs.getBoolean("disclaimer_shown", false)
        
        if (!disclaimerShown) {
            DisclaimerHelper.showDisclaimer(
                this,
                showCheckbox = true,
                backgroundResId = R.drawable.bg_test_splash
            )
        }
    }
    
    private fun initializeDatabase() {
        activityScope.launch(Dispatchers.IO) {
            try {
                val (arkhamCount, eldritchCount) = com.poquets.cthulhu.shared.database.DatabaseInitializer.initializeDatabase(
                    this@Setup,
                    forceReinit = false
                )
                Log.d("Setup", "Database initialized: $arkhamCount Arkham, $eldritchCount Eldritch cards")
            } catch (e: Exception) {
                Log.e("Setup", "Error initializing database: ${e.message}", e)
            }
        }
    }
    
    private fun startGame() {
        try {
            // Get selected expansions
            val expansions = getSelectedExpansions()
            if (expansions.isEmpty()) {
                Toast.makeText(this, "Please select at least one expansion", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Get selected Ancient One
            val selectedAncientOne = ancientOneSpinner.selectedItem as String
            var ancientOne = selectedAncientOne
            
            if (ancientOne == "Random") {
                val availableAncientOnes = getAvailableAncientOnes(expansions)
                if (availableAncientOnes.isEmpty()) {
                    Toast.makeText(this, "No Ancient Ones available for selected expansions", Toast.LENGTH_LONG).show()
                    return
                }
                ancientOne = availableAncientOnes.random()
            }
            
            // Set Config values
            Config.ANCIENT_ONE = ancientOne.replace(" ", "_").replace("'", ".")
            Config.BASE = baseBox.isChecked
            Config.FORSAKEN_LORE = forsakenLoreBox.isChecked
            Config.MOUNTAINS_OF_MADNESS = mountainsOfMadnessBox.isChecked
            Config.ANTARCTICA = antarcticaBox.isChecked
            Config.STRANGE_REMNANTS = strangeRemnantsBox.isChecked
            Config.COSMIC_ALIGNMENT = cosmicAlignmentBox.isChecked
            Config.UNDER_THE_PYRAMIDS = underThePyramidsBox.isChecked
            Config.EGYPT = egyptBox.isChecked
            Config.LITANY_OF_SECRETS = litanyOfSecretsBox.isChecked
            Config.SIGNS_OF_CARCOSA = signsOfCarcosaBox.isChecked
            Config.THE_DREAMLANDS = theDreamlandsBox.isChecked
            Config.DREAMLANDS_BOARD = dreamlandsBoardBox.isChecked
            Config.CITIES_IN_RUIN = citiesInRuinBox.isChecked
            Config.MASKS_OF_NYARLATHOTEP = masksOfNyarlathotepBox.isChecked
            
            // Save game state
            gameState.setCurrentGame(GameType.ELDRITCH)
            gameState.setSelectedExpansions(GameType.ELDRITCH, expansions.toSet())
            gameState.newGame(GameType.ELDRITCH)
            
            // Initialize DecksAdapter
            Log.d("Setup", "Initializing DecksAdapter with expansions: $expansions")
            val decksAdapter = DecksAdapter.initialize(this)
            decksAdapter.initialize(expansions)
            
            if (DecksAdapter.CARDS == null) {
                Toast.makeText(this, "Error: Failed to load card decks. Please try again.", Toast.LENGTH_LONG).show()
                Log.e("Setup", "DecksAdapter.CARDS is null after initialization")
                return
            }
            
            // Create save file marker
            try {
                File(filesDir, "discard.xml").createNewFile()
            } catch (e: Exception) {
                Log.e("Setup", "Unable to create save file: ${e.message}", e)
            }
            
            // Launch main game activity
            Log.d("Setup", "Starting EldritchCompanion activity...")
            val intent = Intent(this, EldritchCompanion::class.java)
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e("Setup", "Error in startGame: ${e.message}", e)
            Toast.makeText(this, "Error starting game: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun continueGame() {
        // TODO: Implement continue game functionality
        Toast.makeText(this, "Continue game not yet implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun getSelectedExpansions(): List<String> {
        val expansions = mutableListOf<String>()
        if (baseBox.isChecked) expansions.add("BASE")
        if (forsakenLoreBox.isChecked) expansions.add("FORSAKEN_LORE")
        if (mountainsOfMadnessBox.isChecked) expansions.add("MOUNTAINS_OF_MADNESS")
        if (antarcticaBox.isChecked) expansions.add("ANTARCTICA")
        if (strangeRemnantsBox.isChecked) expansions.add("STRANGE_REMNANTS")
        if (cosmicAlignmentBox.isChecked) expansions.add("COSMIC_ALIGNMENT")
        if (underThePyramidsBox.isChecked) expansions.add("UNDER_THE_PYRAMIDS")
        if (egyptBox.isChecked) expansions.add("EGYPT")
        if (litanyOfSecretsBox.isChecked) expansions.add("LITANY_OF_SECRETS")
        if (signsOfCarcosaBox.isChecked) expansions.add("SIGNS_OF_CARCOSA")
        if (theDreamlandsBox.isChecked) expansions.add("THE_DREAMLANDS")
        if (dreamlandsBoardBox.isChecked) expansions.add("DREAMLANDS_BOARD")
        if (citiesInRuinBox.isChecked) expansions.add("CITIES_IN_RUIN")
        if (masksOfNyarlathotepBox.isChecked) expansions.add("MASKS_OF_NYARLATHOTEP")
        return expansions
    }
    
    private fun getAvailableAncientOnes(expansions: List<String>): List<String> {
        // Base game Ancient Ones
        val ancientOnes = mutableListOf("Azathoth", "Cthulhu", "Shub-Niggurath", "Yog-Sothoth")
        
        // Expansion-specific Ancient Ones
        if (expansions.contains("MOUNTAINS_OF_MADNESS")) {
            ancientOnes.add("Rise of the Elder Things")
        }
        if (expansions.contains("UNDER_THE_PYRAMIDS")) {
            ancientOnes.add("Nephren-Ka")
        }
        
        return ancientOnes
    }
    
    override fun onResume() {
        super.onResume()
        val saveFile = File(filesDir, "discard.xml")
        continueButton.visibility = if (saveFile.exists()) View.VISIBLE else View.GONE
    }
}


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
    
    private lateinit var baseBox: ImageView
    private lateinit var forsakenLoreBox: ImageView
    private lateinit var mountainsOfMadnessBox: ImageView
    private lateinit var antarcticaBox: ImageView
    private lateinit var strangeRemnantsBox: ImageView
    private lateinit var cosmicAlignmentBox: ImageView
    private lateinit var underThePyramidsBox: ImageView
    private lateinit var egyptBox: ImageView
    private lateinit var litanyOfSecretsBox: ImageView
    private lateinit var signsOfCarcosaBox: ImageView
    private lateinit var theDreamlandsBox: ImageView
    private lateinit var dreamlandsBoardBox: ImageView
    private lateinit var citiesInRuinBox: ImageView
    private lateinit var masksOfNyarlathotepBox: ImageView
    
    // Track selection state for each expansion
    private val expansionSelected = mutableMapOf<ImageView, Boolean>()
    
    // Expansion dependencies: child -> parent (lazy initialization after views are ready)
    private val expansionParent: Map<ImageView, ImageView> by lazy {
        mapOf(
            antarcticaBox to mountainsOfMadnessBox,
            cosmicAlignmentBox to strangeRemnantsBox,
            egyptBox to underThePyramidsBox,
            litanyOfSecretsBox to underThePyramidsBox,
            dreamlandsBoardBox to theDreamlandsBox
        )
    }
    
    // Parent -> children mapping (lazy initialization after views are ready)
    private val expansionChildren: Map<ImageView, List<ImageView>> by lazy {
        mapOf(
            mountainsOfMadnessBox to listOf(antarcticaBox),
            strangeRemnantsBox to listOf(cosmicAlignmentBox),
            underThePyramidsBox to listOf(egyptBox, litanyOfSecretsBox),
            theDreamlandsBox to listOf(dreamlandsBoardBox)
        )
    }
    
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
        // Expansion icon mappings: ImageView -> icon base name
        val expansionIcons = mapOf(
            forsakenLoreBox to "icon_exp_fl",
            mountainsOfMadnessBox to "icon_exp_mom",
            antarcticaBox to "icon_exp_mom", // Antarctica uses MoM icon
            strangeRemnantsBox to "icon_exp_sr",
            cosmicAlignmentBox to "icon_exp_sr", // Cosmic Alignment uses SR icon
            underThePyramidsBox to "icon_exp_utp",
            egyptBox to "icon_exp_utp", // Egypt uses UtP icon
            litanyOfSecretsBox to "icon_exp_utp", // Litany uses UtP icon
            signsOfCarcosaBox to "icon_exp_soc",
            theDreamlandsBox to "icon_exp_td",
            dreamlandsBoardBox to "icon_exp_td", // Dreamlands Board uses TD icon
            citiesInRuinBox to "icon_exp_cir",
            masksOfNyarlathotepBox to "icon_exp_mon"
        )
        
        // Initialize all expansions as unselected
        expansionIcons.keys.forEach { imageView ->
            expansionSelected[imageView] = false
        }
        
        // Set up each expansion image view
        expansionIcons.forEach { (imageView, iconName) ->
            setExpansionIcon(imageView, iconName, false)
            
            // Make the entire row clickable (image and text)
            val parentLayout = imageView.parent as? android.view.ViewGroup
            val toggleAction: (View) -> Unit = { _ ->
                toggleExpansion(imageView, iconName)
            }
            
            parentLayout?.setOnClickListener(toggleAction)
            parentLayout?.isClickable = true
            parentLayout?.isFocusable = true
            
            // Make all child views (including TextView) also trigger the toggle
            parentLayout?.let { layout ->
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is TextView || child is ImageView) {
                        child.setOnClickListener(toggleAction)
                        child.isClickable = true
                        child.isFocusable = true
                    }
                }
            }
        }
        
        // Default: BASE is selected (but hidden)
        expansionSelected[baseBox] = true
    }
    
    private fun toggleExpansion(imageView: ImageView, iconName: String) {
        val isSelected = expansionSelected[imageView] ?: false
        val newSelected = !isSelected
        
        // Handle parent-child dependencies
        if (newSelected) {
            // When selecting: if this is a child, also select the parent
            val parent = expansionParent[imageView]
            parent?.let { parentView ->
                if (!(expansionSelected[parentView] ?: false)) {
                    // Find parent's icon name
                    val parentIconName = getIconNameForView(parentView)
                    setExpansionState(parentView, parentIconName, true)
                }
            }
        } else {
            // When unselecting: if this is a parent, also unselect all children
            val children = expansionChildren[imageView]
            children?.forEach { childView ->
                if (expansionSelected[childView] == true) {
                    val childIconName = getIconNameForView(childView)
                    setExpansionState(childView, childIconName, false)
                }
            }
        }
        
        // Toggle the current expansion
        setExpansionState(imageView, iconName, newSelected)
        
        // Update spinner when expansions change
        updateSpinner()
    }
    
    private fun setExpansionState(imageView: ImageView, iconName: String, isSelected: Boolean) {
        expansionSelected[imageView] = isSelected
        setExpansionIcon(imageView, iconName, isSelected)
    }
    
    private fun getIconNameForView(imageView: ImageView): String {
        return when (imageView) {
            forsakenLoreBox -> "icon_exp_fl"
            mountainsOfMadnessBox -> "icon_exp_mom"
            antarcticaBox -> "icon_exp_mom"
            strangeRemnantsBox -> "icon_exp_sr"
            cosmicAlignmentBox -> "icon_exp_sr"
            underThePyramidsBox -> "icon_exp_utp"
            egyptBox -> "icon_exp_utp"
            litanyOfSecretsBox -> "icon_exp_utp"
            signsOfCarcosaBox -> "icon_exp_soc"
            theDreamlandsBox -> "icon_exp_td"
            dreamlandsBoardBox -> "icon_exp_td"
            citiesInRuinBox -> "icon_exp_cir"
            masksOfNyarlathotepBox -> "icon_exp_mon"
            else -> ""
        }
    }
    
    private fun setExpansionIcon(imageView: ImageView, iconName: String, isSelected: Boolean) {
        try {
            // Use _glow.png version when selected, regular version when not selected
            val iconFileName = if (isSelected) "${iconName}_glow.png" else "${iconName}.png"
            val assetPath = "expansion/$iconFileName"
            
            try {
                val inputStream = assets.open(assetPath)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Log.d("Setup", "Successfully loaded icon: $iconFileName from assets")
                    return
                }
            } catch (e: java.io.IOException) {
                Log.d("Setup", "Icon not found in assets: $assetPath")
            }
            
            // Fallback to drawable resources
            val drawableName = if (isSelected) "${iconName}_glow" else iconName
            val iconId = resources.getIdentifier(drawableName, "drawable", packageName)
            if (iconId != 0) {
                imageView.setImageResource(iconId)
                Log.d("Setup", "Successfully loaded icon: $drawableName from drawable resources")
            } else {
                Log.d("Setup", "Icon not found: $drawableName (tried assets/expansion/$iconFileName and drawable/$drawableName)")
            }
        } catch (e: Exception) {
            Log.w("Setup", "Error setting icon $iconName (selected=$isSelected): ${e.message}", e)
        }
    }
    
    private fun setupSpinner() {
        updateSpinner()
    }
    
    private fun updateSpinner() {
        val expansions = getSelectedExpansions()
        val ancientOnes = getAvailableAncientOnes(expansions).toMutableList()
        ancientOnes.add(0, "Random")
        
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ancientOnes) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView?.setTextColor(android.graphics.Color.WHITE)
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView?.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        // Save current selection if it's still valid
        val currentSelection = if (ancientOneSpinner.selectedItemPosition >= 0 && ancientOneSpinner.selectedItemPosition < ancientOneSpinner.count) {
            ancientOneSpinner.selectedItem as? String
        } else null
        
        ancientOneSpinner.adapter = adapter
        
        // Restore selection if it's still available
        if (currentSelection != null && ancientOnes.contains(currentSelection)) {
            val index = ancientOnes.indexOf(currentSelection)
            if (index >= 0) {
                ancientOneSpinner.setSelection(index)
            }
        }
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
            Config.BASE = expansionSelected[baseBox] ?: true
            Config.FORSAKEN_LORE = expansionSelected[forsakenLoreBox] ?: false
            Config.MOUNTAINS_OF_MADNESS = expansionSelected[mountainsOfMadnessBox] ?: false
            Config.ANTARCTICA = expansionSelected[antarcticaBox] ?: false
            Config.STRANGE_REMNANTS = expansionSelected[strangeRemnantsBox] ?: false
            Config.COSMIC_ALIGNMENT = expansionSelected[cosmicAlignmentBox] ?: false
            Config.UNDER_THE_PYRAMIDS = expansionSelected[underThePyramidsBox] ?: false
            Config.EGYPT = expansionSelected[egyptBox] ?: false
            Config.LITANY_OF_SECRETS = expansionSelected[litanyOfSecretsBox] ?: false
            Config.SIGNS_OF_CARCOSA = expansionSelected[signsOfCarcosaBox] ?: false
            Config.THE_DREAMLANDS = expansionSelected[theDreamlandsBox] ?: false
            Config.DREAMLANDS_BOARD = expansionSelected[dreamlandsBoardBox] ?: false
            Config.CITIES_IN_RUIN = expansionSelected[citiesInRuinBox] ?: false
            Config.MASKS_OF_NYARLATHOTEP = expansionSelected[masksOfNyarlathotepBox] ?: false
            
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
        if (expansionSelected[baseBox] == true) expansions.add("BASE")
        if (expansionSelected[forsakenLoreBox] == true) expansions.add("FORSAKEN_LORE")
        if (expansionSelected[mountainsOfMadnessBox] == true) expansions.add("MOUNTAINS_OF_MADNESS")
        if (expansionSelected[antarcticaBox] == true) expansions.add("ANTARCTICA")
        if (expansionSelected[strangeRemnantsBox] == true) expansions.add("STRANGE_REMNANTS")
        if (expansionSelected[cosmicAlignmentBox] == true) expansions.add("COSMIC_ALIGNMENT")
        if (expansionSelected[underThePyramidsBox] == true) expansions.add("UNDER_THE_PYRAMIDS")
        if (expansionSelected[egyptBox] == true) expansions.add("EGYPT")
        if (expansionSelected[litanyOfSecretsBox] == true) expansions.add("LITANY_OF_SECRETS")
        if (expansionSelected[signsOfCarcosaBox] == true) expansions.add("SIGNS_OF_CARCOSA")
        if (expansionSelected[theDreamlandsBox] == true) expansions.add("THE_DREAMLANDS")
        if (expansionSelected[dreamlandsBoardBox] == true) expansions.add("DREAMLANDS_BOARD")
        if (expansionSelected[citiesInRuinBox] == true) expansions.add("CITIES_IN_RUIN")
        if (expansionSelected[masksOfNyarlathotepBox] == true) expansions.add("MASKS_OF_NYARLATHOTEP")
        return expansions
    }
    
    private fun getAvailableAncientOnes(expansions: List<String>): List<String> {
        val ancientOnes = mutableListOf<String>()
        
        // Base game Ancient Ones (always available if BASE is selected)
        if (expansions.contains("BASE")) {
            ancientOnes.addAll(listOf("Azathoth", "Cthulhu", "Shub-Niggurath", "Yog-Sothoth"))
        }
        
        // Expansion-specific Ancient Ones
        if (expansions.contains("FORSAKEN_LORE")) {
            ancientOnes.add("Yig")
        }
        if (expansions.contains("MOUNTAINS_OF_MADNESS")) {
            ancientOnes.add("Rise of the Elder Things")
            ancientOnes.add("Ithaqua")
        }
        if (expansions.contains("STRANGE_REMNANTS")) {
            ancientOnes.add("Syzygy")
        }
        if (expansions.contains("UNDER_THE_PYRAMIDS")) {
            ancientOnes.add("Abhoth")
            ancientOnes.add("Nephren-Ka")
        }
        if (expansions.contains("SIGNS_OF_CARCOSA")) {
            ancientOnes.add("Hastur")
        }
        if (expansions.contains("THE_DREAMLANDS")) {
            ancientOnes.add("Hypnos")
            ancientOnes.add("Atlach-Nacha")
        }
        if (expansions.contains("CITIES_IN_RUIN")) {
            ancientOnes.add("Shudde M'ell")
        }
        if (expansions.contains("MASKS_OF_NYARLATHOTEP")) {
            ancientOnes.add("Nyarlathotep")
            ancientOnes.add("Antediluvium")
        }
        
        return ancientOnes
    }
    
    override fun onResume() {
        super.onResume()
        val saveFile = File(filesDir, "discard.xml")
        continueButton.visibility = if (saveFile.exists()) View.VISIBLE else View.GONE
    }
}


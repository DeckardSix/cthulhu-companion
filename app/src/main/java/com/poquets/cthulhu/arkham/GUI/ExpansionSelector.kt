package com.poquets.cthulhu.arkham.GUI

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.CompoundButtonCompat
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.*
import com.poquets.cthulhu.shared.ui.DisclaimerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Expansion selector activity for Arkham Horror
 * Displays list of expansions with checkboxes
 */
class ExpansionSelector : AppCompatActivity() {
    
    private lateinit var listView: ListView
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.expansion)
        } catch (e: Exception) {
            android.util.Log.e("ExpansionSelector", "Error setting content view: ${e.message}", e)
            // Show error message and finish
            Toast.makeText(this, "Error loading screen: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        try {
            // Check if database needs initialization and migrate if needed
            val db = com.poquets.cthulhu.shared.database.UnifiedCardDatabaseHelper.getInstance(applicationContext)
            val hasCards = kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        db.hasCards(com.poquets.cthulhu.shared.database.GameType.ARKHAM)
                    } catch (e: Exception) {
                        android.util.Log.e("ExpansionSelector", "Error checking for cards: ${e.message}", e)
                        false
                    }
                }
            }
            
            if (!hasCards) {
                android.util.Log.w("ExpansionSelector", "No Arkham cards found, triggering migration...")
                // Show loading message
                Toast.makeText(this, "Initializing Arkham database...", Toast.LENGTH_SHORT).show()
                
                // Trigger migration synchronously
                try {
                    val (arkhamCount, _) = kotlinx.coroutines.runBlocking {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.poquets.cthulhu.shared.database.DatabaseInitializer.initializeDatabase(
                                applicationContext,
                                forceReinit = false
                            )
                        }
                    }
                    android.util.Log.d("ExpansionSelector", "Migration completed: $arkhamCount Arkham cards")
                    
                    if (arkhamCount > 0) {
                        Toast.makeText(this, "Loaded $arkhamCount Arkham cards", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Warning: No Arkham cards were loaded. Check logs for errors.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExpansionSelector", "Error during migration: ${e.message}", e)
                    Toast.makeText(this, "Error during migration: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            // Initialize AHFlyweightFactory
            try {
                AHFlyweightFactory.Init(applicationContext)
                
                // Clear expansion cache if we just migrated to ensure fresh data
                if (!hasCards) {
                    AHFlyweightFactory.INSTANCE.clearExpansionCache()
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpansionSelector", "Error initializing AHFlyweightFactory: ${e.message}", e)
                Toast.makeText(this, "Error initializing factory: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            // Initialize GameState
            try {
                GameState.getInstance(applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("ExpansionSelector", "Error initializing GameState: ${e.message}", e)
                Toast.makeText(this, "Error initializing game state: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            listView = findViewById(R.id.ListView01)
            if (listView == null) {
                android.util.Log.e("ExpansionSelector", "ListView not found in layout!")
                Toast.makeText(this, "Error: ListView not found", Toast.LENGTH_LONG).show()
                return
            }
            
            // Get expansions
            try {
                val expansions = AHFlyweightFactory.INSTANCE.getExpansions()
                android.util.Log.d("ExpansionSelector", "Found ${expansions.size} expansions")
                
                if (expansions.isEmpty()) {
                    android.util.Log.w("ExpansionSelector", "No expansions found! Database may not be initialized.")
                    Toast.makeText(this, "No expansions found. Please initialize the database.", Toast.LENGTH_LONG).show()
                    // Create a default base game expansion
                    val defaultExpansions = listOf(ExpansionAdapter(1L, "Base Game", null))
                    val cursor = ExpansionCursorAdapter(defaultExpansions)
                    val columns = arrayOf("Checked")
                    val to = intArrayOf(R.id.number_entry)
                    val adapter = SimpleCursorAdapter(this, R.layout.checkboxlist, cursor, columns, to, 0)
                    setupAdapter(adapter)
                    listView.adapter = adapter
                } else {
                    val cursor = ExpansionCursorAdapter(expansions)
                    
                    // Create adapter
                    val columns = arrayOf("Checked")
                    val to = intArrayOf(R.id.number_entry)
                    val adapter = SimpleCursorAdapter(this, R.layout.checkboxlist, cursor, columns, to, 0)
                    setupAdapter(adapter)
                    listView.adapter = adapter
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpansionSelector", "Error getting expansions: ${e.message}", e)
                Toast.makeText(this, "Error loading expansions: ${e.message}", Toast.LENGTH_LONG).show()
                // Still show default base game
                val defaultExpansions = listOf(ExpansionAdapter(1L, "Base Game", null))
                val cursor = ExpansionCursorAdapter(defaultExpansions)
                val columns = arrayOf("Checked")
                val to = intArrayOf(R.id.number_entry)
                val adapter = SimpleCursorAdapter(this, R.layout.checkboxlist, cursor, columns, to, 0)
                setupAdapter(adapter)
                listView.adapter = adapter
            }
            
            // Add question mark icon
            try {
                addQuestionMarkIcon()
            } catch (e: Exception) {
                android.util.Log.w("ExpansionSelector", "Error adding question mark icon: ${e.message}")
            }
            
            // Show disclaimer if needed
            try {
                if (DisclaimerHelper.shouldShowDisclaimer(this)) {
                    DisclaimerHelper.showDisclaimer(this, R.drawable.cthulhu_background)
                }
            } catch (e: Exception) {
                android.util.Log.w("ExpansionSelector", "Error showing disclaimer: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpansionSelector", "Error initializing expansion selector: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Error loading expansions: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupAdapter(adapter: SimpleCursorAdapter) {
        adapter.setViewBinder { view, cursor, columnIndex ->
            try {
                // The "Checked" column is at index 2 in the cursor (columns are: _ID, Name, Checked)
                if (columnIndex == 2) {
                    val checkbox = view as CheckBox
                    val expansion = (cursor as ExpansionCursorAdapter).getExpansion()
                    
                    // Remove previous listener to avoid conflicts
                    checkbox.setOnCheckedChangeListener(null)
                    
                    // Base game is always enabled and cannot be disabled
                    val isBaseGame = expansion.getID() == 1L
                    
                    // Set checkbox state - base game is always checked
                    checkbox.isChecked = if (isBaseGame) true else expansion.getApplied(applicationContext)
                    
                    // Disable base game checkbox (cannot be unchecked)
                    checkbox.isEnabled = !isBaseGame
                    
                    // Set text
                    checkbox.text = expansion.getName()
                    
                    // Set font
                    try {
                        val font = Typeface.createFromAsset(assets, "fonts/se-caslon-ant.ttf")
                        checkbox.typeface = font
                    } catch (e: Exception) {
                        android.util.Log.w("ExpansionSelector", "Font not found: ${e.message}")
                    }
                    
                    // Set text color
                    checkbox.setTextColor(android.graphics.Color.WHITE)
                    CompoundButtonCompat.setButtonTintList(
                        checkbox,
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                    )
                    
                    // Set click listener (only for non-base game expansions)
                    if (!isBaseGame) {
                        checkbox.setOnCheckedChangeListener { _, isChecked ->
                            // Apply expansion
                            try {
                                GameState.getInstance(applicationContext).applyExpansion(expansion.getID(), isChecked)
                            } catch (e: Exception) {
                                android.util.Log.e("ExpansionSelector", "Error applying expansion: ${e.message}", e)
                            }
                        }
                    } else {
                        // Ensure base game is always applied
                        try {
                            GameState.getInstance(applicationContext).applyExpansion(1L, true)
                        } catch (e: Exception) {
                            android.util.Log.e("ExpansionSelector", "Error ensuring base game is applied: ${e.message}", e)
                        }
                    }
                    
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpansionSelector", "Error in view binder: ${e.message}", e)
                false
            }
        }
    }
    
    private fun addQuestionMarkIcon() {
        val logoFrameLayout: FrameLayout? = findViewById(R.id.logoFrameLayout)
        
        if (logoFrameLayout != null) {
            val paddingPx = 5
            val iconSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32f,
                resources.displayMetrics
            ).toInt()
            
            val questionMarkIcon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_help)
                contentDescription = "Show Disclaimer"
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                
                val totalSize = iconSize + (paddingPx * 2)
                layoutParams = FrameLayout.LayoutParams(totalSize, totalSize).apply {
                    gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                }
                
                setColorFilter(0xFFFFFFFF.toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    DisclaimerHelper.showDisclaimer(this@ExpansionSelector, R.drawable.cthulhu_background)
                }
            }
            
            logoFrameLayout.addView(questionMarkIcon)
        }
    }
    
    fun openNeighborhood(view: View) {
        val intent = Intent(this, NeighborhoodSelector::class.java)
        startActivity(intent)
    }
    
    fun openOW(view: View) {
        val intent = Intent(this, OtherworldSelector::class.java)
        startActivity(intent)
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.expansion_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_game -> {
                newGame()
                true
            }
            R.id.about_app -> {
                about()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun about() {
        // TODO: Launch AboutActivity
        Toast.makeText(this, "AboutActivity not yet implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun newGame() {
        GameState.getInstance(applicationContext).newGame()
        Toast.makeText(this, "New game started", Toast.LENGTH_SHORT).show()
    }
}


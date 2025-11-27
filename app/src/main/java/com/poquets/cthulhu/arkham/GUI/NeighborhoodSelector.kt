package com.poquets.cthulhu.arkham.GUI

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Neighborhood selector activity for Arkham Horror
 * Displays list of neighborhoods with buttons
 */
class NeighborhoodSelector : AppCompatActivity() {
    
    private lateinit var listView: ListView
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        
        // Add padding at top to account for status bar (like the card screen does with Toolbar)
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
        val statusBarHeight = getStatusBarHeight()
        rootLayout.setPadding(0, statusBarHeight, 0, 0)
        
        // Add back button positioned like question mark in expansion selector
        addBackButton()
        
        // Initialize factories
        AHFlyweightFactory.Init(applicationContext)
        GameState.getInstance(applicationContext)
        
        listView = findViewById(R.id.ListView01)
        
        // Get neighborhoods
        activityScope.launch {
            val neighborhoods = AHFlyweightFactory.INSTANCE.getCurrentNeighborhoods()
            val cursor = NeighborhoodCursorAdapter(neighborhoods)
            
            // Create adapter
            val columns = arrayOf("Left", "Right")
            val to = intArrayOf(R.id.name_entry, R.id.name_entry2)
            val adapter = SimpleCursorAdapter(this@NeighborhoodSelector, R.layout.neighborhood_button, cursor, columns, to, 0)
            
            adapter.setViewBinder { view, cursor, columnIndex ->
                if (columnIndex == 1 || columnIndex == 2) {
                    val button = view as Button
                    val neighborhood = (cursor as NeighborhoodCursorAdapter).getNeighborhood(columnIndex)
                    
                    if (neighborhood == null) {
                        button.visibility = View.INVISIBLE
                    } else {
                        button.visibility = View.VISIBLE
                        button.text = neighborhood.getNeighborhoodName()
                        
                        // Set font
                        try {
                            val font = Typeface.createFromAsset(assets, "fonts/se-caslon-ant.ttf")
                            button.typeface = font
                        } catch (e: Exception) {
                            Log.w("NeighborhoodSelector", "Font not found: ${e.message}")
                        }
                        
                        // Load and set background image from assets (matching original app)
                        val buttonPath = neighborhood.getNeighborhoodButtonPath()
                        Log.d("NeighborhoodSelector", "Loading button for ${neighborhood.getNeighborhoodName()}: path=$buttonPath")
                        
                        if (buttonPath != null && buttonPath.isNotEmpty()) {
                            try {
                                val inputStream = assets.open(buttonPath)
                                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                inputStream.close()
                                
                                if (bitmap != null) {
                                    Log.d("NeighborhoodSelector", "Successfully loaded button image: ${bitmap.width}x${bitmap.height}")
                                    button.background = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                                } else {
                                    Log.d("NeighborhoodSelector", "Bitmap is null after loading from $buttonPath, using default")
                                    // Fallback to default if bitmap couldn't be loaded
                                    button.setBackgroundResource(R.drawable.neighbourhood_overlay)
                                }
                            } catch (e: java.io.FileNotFoundException) {
                                // File not found is expected for expansion neighborhoods without button images
                                // Silently fall back to default (no error logging needed)
                                button.setBackgroundResource(R.drawable.neighbourhood_overlay)
                            } catch (e: Exception) {
                                // Other exceptions (e.g., corrupted file) - log as warning, not error
                                Log.w("NeighborhoodSelector", "Could not load button image from $buttonPath: ${e.message}")
                                // Fallback to default if image not found
                                button.setBackgroundResource(R.drawable.neighbourhood_overlay)
                            }
                        } else {
                            // No button path specified, use default (no logging needed)
                            button.setBackgroundResource(R.drawable.neighbourhood_overlay)
                        }
                        
                        // Set click listener
                        button.setOnClickListener {
                            if (neighborhood == null) {
                                Log.e("NeighborhoodSelector", "Neighborhood is null!")
                                return@setOnClickListener
                            }
                            
                            Log.i("NeighborhoodSelector", "Neighborhood Clicked: ${neighborhood.toString()} (ID: ${neighborhood.getID()})")
                            
                            // Randomize neighborhood deck
                            GameState.getInstance(applicationContext).randomizeNeighborhood(neighborhood.getID())
                            
                            // Launch LocationDeckActivity
                            val intent = Intent(this@NeighborhoodSelector, LocationDeckActivity::class.java)
                            intent.putExtra("neighborhood", neighborhood.getID())
                            startActivity(intent)
                        }
                    }
                    
                    true
                } else {
                    false
                }
            }
            
            listView.adapter = adapter
        }
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
    
    fun openEncHx(view: View) {
        // TODO: Launch LocationHxActivity
        Toast.makeText(this, "LocationHxActivity not yet implemented", Toast.LENGTH_SHORT).show()
    }
    
    fun openOW(view: View) {
        val intent = Intent(this, OtherworldSelector::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
}


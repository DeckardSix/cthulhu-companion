package com.poquets.cthulhu.arkham.GUI

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Otherworld selector activity for Arkham Horror
 * Displays list of otherworld locations
 */
class OtherworldSelector : AppCompatActivity() {
    
    private lateinit var listView: ListView
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otherworld_selector)
        
        // Initialize factories
        AHFlyweightFactory.Init(applicationContext)
        GameState.getInstance(applicationContext)
        
        listView = findViewById(R.id.locationListView)
        
        // Get otherworld locations
        activityScope.launch {
            // TODO: Get otherworld locations from unified database
            // For now, use empty list
            val locations = emptyList<LocationAdapter>()
            val cursor = LocationCursorAdapter(locations)
            
            // Create adapter
            val columns = arrayOf("Left", "Right")
            val to = intArrayOf(R.id.name_entry, R.id.name_entry2)
            val adapter = SimpleCursorAdapter(this@OtherworldSelector, R.layout.location_button, cursor, columns, to, 0)
            
            adapter.setViewBinder { view, cursor, columnIndex ->
                if (columnIndex == 1 || columnIndex == 2) {
                    val button = view as Button
                    val location = (cursor as LocationCursorAdapter).getLocation(columnIndex)
                    
                    if (location == null) {
                        button.visibility = View.INVISIBLE
                    } else {
                        button.visibility = View.VISIBLE
                        button.text = location.getLocationName()
                        
                        // Set font
                        try {
                            val font = Typeface.createFromAsset(assets, "fonts/se-caslon-ant.ttf")
                            button.typeface = font
                        } catch (e: Exception) {
                            Log.w("OtherworldSelector", "Font not found: ${e.message}")
                        }
                        
                        // Set background
                        button.setBackgroundResource(R.drawable.btn_round)
                        
                        // Set click listener
                        button.setOnClickListener {
                            // Clear selected colors
                            GameState.getInstance(applicationContext).clearSelectedOtherWorldColor()
                            
                            // Launch OtherWorldDeckActivity
                            val intent = Intent(this@OtherworldSelector, OtherWorldDeckActivity::class.java)
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
    
    fun openNeighborhood(view: View) {
        val intent = Intent(this, NeighborhoodSelector::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
    
    fun openEncHx(view: View) {
        // TODO: Launch LocationHxActivity
        Toast.makeText(this, "LocationHxActivity not yet implemented", Toast.LENGTH_SHORT).show()
    }
    
    fun openOW(view: View) {
        // TODO: Launch OtherWorldDeckActivity
        Toast.makeText(this, "OtherWorldDeckActivity not yet implemented", Toast.LENGTH_SHORT).show()
    }
}


package com.poquets.cthulhu.eldritch.GUI

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.CompoundButtonCompat
import com.poquets.cthulhu.R
import com.poquets.cthulhu.eldritch.CardAdapter
import com.poquets.cthulhu.eldritch.DecksAdapter
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Activity for removing expeditions from specific regions
 */
class RemoveExpedition : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_expedition)
        
        supportActionBar?.show()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Current location label
        val currentLabel = findViewById<TextView>(R.id.currentLocationLabel)
        val currentLocation = DecksAdapter.CARDS?.getExpeditionLocation() ?: "EMPTY"
        currentLabel.text = "Current: $currentLocation"
        currentLabel.textSize = 20f
        currentLabel.setTextColor(android.graphics.Color.WHITE)
        
        // Radio group for locations
        val group = findViewById<RadioGroup>(R.id.expeditionGroup)
        
        // Get screen size for text sizing
        val dm: DisplayMetrics = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val dens = dm.densityDpi
        val wi = width.toDouble() / dens
        val hi = height.toDouble() / dens
        val x = wi.pow(2.0)
        val y = hi.pow(2.0)
        val screenInches = sqrt(x + y)
        
        // Get locations from expedition deck
        val locations = mutableListOf<String>()
        val expeditionDeck = DecksAdapter.CARDS?.getDeck("EXPEDITION")
        if (expeditionDeck != null) {
            locations.addAll(getLocations(expeditionDeck))
        }
        
        // Create radio buttons for each location
        for (i in locations.indices) {
            val button = RadioButton(this)
            button.text = locations[i]
            button.setTextColor(android.graphics.Color.WHITE)
            CompoundButtonCompat.setButtonTintList(
                button,
                android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            )
            if (screenInches > 4.5) {
                button.textSize = 35f
            }
            button.id = i
            group.addView(button)
        }
        
        // Check current location
        val currentIndex = locations.indexOf(currentLocation)
        if (currentIndex >= 0) {
            group.check(currentIndex)
        }
        
        // Remove button
        val removeButton = findViewById<Button>(R.id.removeExpeditionsButton)
        removeButton.text = "Remove Expeditions"
        removeButton.textSize = 20f
        removeButton.setTextColor(android.graphics.Color.WHITE)
        removeButton.setOnClickListener { removeExpeditions(it) }
    }
    
    private fun getLocations(expeditionDeck: List<CardAdapter>): Set<String> {
        val locations = mutableSetOf<String>()
        for (card in expeditionDeck) {
            card.topHeader?.let { locations.add(it) }
        }
        return locations.toSortedSet()
    }
    
    fun removeExpeditions(view: View) {
        val group = findViewById<RadioGroup>(R.id.expeditionGroup)
        val checkedId = group.checkedRadioButtonId
        if (checkedId != -1) {
            val button = findViewById<RadioButton>(checkedId)
            if (button != null && DecksAdapter.CARDS != null) {
                DecksAdapter.CARDS!!.removeExpeditions(button.text.toString())
            }
        }
        finish()
    }
}


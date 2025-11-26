package com.poquets.cthulhu.arkham.GUI

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Location deck activity for Arkham Horror
 * Displays cards for a selected neighborhood location
 */
class LocationDeckActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private var neighborhoodId: Long = -1L
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.locationdeck)
        
        supportActionBar?.show()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize factories
        AHFlyweightFactory.Init(applicationContext)
        GameState.getInstance(applicationContext)
        
        neighborhoodId = intent.getLongExtra("neighborhood", -1L)
        if (neighborhoodId == -1L) {
            Log.e("LocationDeckActivity", "No neighborhood specified")
            Toast.makeText(this, "Error: No neighborhood specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        viewPager = findViewById(R.id.viewpager)
        
        // Set background color based on neighborhood
        activityScope.launch {
            val neighborhood = AHFlyweightFactory.INSTANCE.getNeighborhood(neighborhoodId)
            if (neighborhood != null) {
                // Try to load button image and extract color
                val buttonPath = neighborhood.getNeighborhoodButtonPath()
                val buttonBitmap = if (buttonPath != null && buttonPath.isNotEmpty()) {
                    try {
                        val inputStream = assets.open(buttonPath)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        bitmap
                    } catch (e: IOException) {
                        Log.w("LocationDeckActivity", "Could not load button image: ${e.message}")
                        null
                    }
                } else {
                    null
                }
                
                val neighborhoodColor = ArkhamColorUtils.getNeighborhoodColor(buttonBitmap, neighborhood.getNeighborhoodName())
                if (neighborhoodColor != android.graphics.Color.TRANSPARENT) {
                    window.decorView.setBackgroundColor(neighborhoodColor)
                }
            }
        }
        
        // Load deck
        activityScope.launch {
            val deck = GameState.getInstance(applicationContext).getDeckByNeighborhood(neighborhoodId)
            
            if (deck.isEmpty()) {
                Log.e("LocationDeckActivity", "Deck is empty for neighborhood $neighborhoodId")
                Toast.makeText(this@LocationDeckActivity, "No cards available for this neighborhood", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            
            Log.d("LocationDeckActivity", "Deck has ${deck.size} cards")
            
            val fragments = deck.map { ArkhamCardFragment.newInstance(it) }
            viewPager.adapter = CardPagerAdapter(this@LocationDeckActivity, fragments)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.deck_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.shuffle -> {
                shuffleDeck()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shuffleDeck() {
        activityScope.launch {
            Log.d("LocationDeckActivity", "Shuffling deck for neighborhood $neighborhoodId")
            GameState.getInstance(applicationContext).randomizeNeighborhood(neighborhoodId)
            
            val deck = GameState.getInstance(applicationContext).getDeckByNeighborhood(neighborhoodId)
            
            if (deck.isEmpty()) {
                Toast.makeText(this@LocationDeckActivity, "No cards to shuffle", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val fragments = deck.map { ArkhamCardFragment.newInstance(it) }
            viewPager.adapter = CardPagerAdapter(this@LocationDeckActivity, fragments)
            viewPager.currentItem = 0
            
            Toast.makeText(this@LocationDeckActivity, "Deck shuffled", Toast.LENGTH_SHORT).show()
            Log.d("LocationDeckActivity", "Deck shuffled, ${deck.size} cards")
        }
    }
    
    private inner class CardPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}


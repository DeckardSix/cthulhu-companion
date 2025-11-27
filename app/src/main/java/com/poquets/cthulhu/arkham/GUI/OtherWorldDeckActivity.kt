package com.poquets.cthulhu.arkham.GUI

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

/**
 * Other world deck activity for Arkham Horror
 * Displays other world cards
 */
class OtherWorldDeckActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.locationdeck)
        
        // Set up toolbar as action bar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize factories
        AHFlyweightFactory.Init(applicationContext)
        GameState.getInstance(applicationContext)
        
        viewPager = findViewById(R.id.viewpager)
        
        // Load deck
        activityScope.launch {
            val deck = GameState.getInstance(applicationContext).getFilteredOtherWorldDeck()
            
            if (deck.isEmpty()) {
                Log.e("OtherWorldDeckActivity", "Deck is empty")
                Toast.makeText(this@OtherWorldDeckActivity, "No cards available", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            
            Log.d("OtherWorldDeckActivity", "Deck has ${deck.size} cards")
            
            val fragments = deck.map { ArkhamCardFragment.newInstance(it) }
            viewPager.adapter = CardPagerAdapter(this@OtherWorldDeckActivity, fragments)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.deck_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button press
                finish()
                true
            }
            R.id.shuffle -> {
                // For other world cards, refresh means close the card
                // The next time a location is selected, a new random encounter will be picked
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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


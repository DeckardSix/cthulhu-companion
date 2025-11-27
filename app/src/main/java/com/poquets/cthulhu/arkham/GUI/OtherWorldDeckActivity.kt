package com.poquets.cthulhu.arkham.GUI

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
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
        
        // Add padding at top to account for status bar (like the card screen does with Toolbar)
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
        val statusBarHeight = getStatusBarHeight()
        rootLayout.setPadding(0, statusBarHeight, 0, 0)
        
        // Set background to cthulhu_background (not a color)
        rootLayout.setBackgroundResource(R.drawable.cthulhu_background)
        
        // Add back button positioned like question mark in expansion selector
        addBackButton()
        
        // Initialize factories
        AHFlyweightFactory.Init(applicationContext)
        GameState.getInstance(applicationContext)
        
        viewPager = findViewById(R.id.viewpager)
        
        // Hide navigation container (only shown in history)
        val navigationContainer = findViewById<LinearLayout>(R.id.navigationContainer)
        navigationContainer?.visibility = View.GONE
        
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
            R.id.shuffle -> {
                // For other world cards, refresh means close the card
                // The next time a location is selected, a new random encounter will be picked
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
    
    private inner class CardPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}


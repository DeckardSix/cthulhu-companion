package com.poquets.cthulhu.eldritch.GUI

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.poquets.cthulhu.R
import com.poquets.cthulhu.eldritch.CardAdapter
import com.poquets.cthulhu.eldritch.DecksAdapter

/**
 * Activity for viewing cards in a deck using ViewPager2
 */
open class DeckGallery : AppCompatActivity() {
    
    protected lateinit var gallery: ViewPager2
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        
        supportActionBar?.show()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        val deckName = intent.getStringExtra("DECK")
        
        // Check if DecksAdapter is initialized
        if (DecksAdapter.CARDS == null) {
            Log.e("DeckGallery", "DecksAdapter.CARDS is null, cannot load deck")
            Toast.makeText(this, "Error: Cards not initialized", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        if (deckName.isNullOrEmpty()) {
            Log.e("DeckGallery", "Deck name is null or empty")
            Toast.makeText(this, "Error: Invalid deck name", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        val cards = DecksAdapter.CARDS!!.getDeck(deckName)
        
        if (cards.isNullOrEmpty()) {
            Log.w("DeckGallery", "Deck '$deckName' is empty")
            Toast.makeText(this, "Deck '$deckName' is empty", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Create fragments for each card
        val fragments = cards.map { card ->
            CardViewFragment.newInstance(card, deckName)
        }
        
        gallery = findViewById(R.id.viewpager)
        gallery.adapter = CardPagerAdapter(this, fragments)
        gallery.setPageTransformer(DepthPageTransformer())
        
        // Create page change callback
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val region = DecksAdapter.CARDS?.getRegion(deckName, position)
                title = region ?: deckName
            }
        }
        
        gallery.registerOnPageChangeCallback(pageChangeCallback!!)
        // Trigger initial page selection
        pageChangeCallback?.onPageSelected(0)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        pageChangeCallback?.let {
            gallery.unregisterOnPageChangeCallback(it)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Add shuffle item
        val shuffleItem = menu.add(Menu.NONE, R.id.action_shuffle_deck, Menu.NONE, "Shuffle")
        shuffleItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        shuffleItem.setIcon(R.drawable.ic_shuffle_actionbar)
        
        // Add discard item
        val discardItem = menu.add(Menu.NONE, R.id.action_discard_card, Menu.NONE, "Discard")
        discardItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        discardItem.setIcon(R.drawable.ic_discard_actionbar)
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val deckName = intent.getStringExtra("DECK")
        
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_shuffle_deck -> {
                if (DecksAdapter.CARDS != null && deckName != null) {
                    DecksAdapter.CARDS!!.shuffleFullDeck(deckName)
                    finish()
                }
                true
            }
            R.id.action_discard_card -> {
                val adapter = gallery.adapter as? CardPagerAdapter
                if (adapter != null) {
                    val cardFragment = adapter.getItem(gallery.currentItem) as? CardViewFragment
                    if (cardFragment != null && DecksAdapter.CARDS != null) {
                        val card = cardFragment.getCard()
                        DecksAdapter.CARDS!!.discardCard(card.region ?: deckName ?: "", card.ID, null)
                    }
                }
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private class CardPagerAdapter(
        fragmentActivity: androidx.fragment.app.FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {
        
        override fun getItemCount(): Int = fragments.size
        
        override fun createFragment(position: Int): Fragment = fragments[position]
        
        fun getItem(position: Int): Fragment = fragments[position]
    }
    
    private class DepthPageTransformer : ViewPager2.PageTransformer {
        private val MIN_SCALE = 0.75f
        
        override fun transformPage(page: View, position: Float) {
            val pageWidth = page.width
            when {
                position < -1f -> {
                    page.alpha = 0f
                }
                position <= 0f -> {
                    page.alpha = 1f
                    page.translationX = 0f
                    page.scaleX = 1f
                    page.scaleY = 1f
                }
                position <= 1f -> {
                    page.alpha = 1f - position
                    page.translationX = pageWidth * -position
                    val scaleFactor = MIN_SCALE + (0.25f * (1f - kotlin.math.abs(position)))
                    page.scaleX = scaleFactor
                    page.scaleY = scaleFactor
                }
                else -> {
                    page.alpha = 0f
                }
            }
        }
    }
}


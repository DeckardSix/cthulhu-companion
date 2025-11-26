package com.poquets.cthulhu.eldritch.GUI

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.poquets.cthulhu.R
import com.poquets.cthulhu.eldritch.DecksAdapter

/**
 * Activity for viewing the discard pile
 * Extends DeckGallery but with a different menu (remove card instead of discard)
 */
class DiscardGallery : DeckGallery() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force deck name to DISCARD
        intent.putExtra("DECK", "DISCARD")
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Add remove card item
        val removeItem = menu.add(Menu.NONE, R.id.action_remove_card, Menu.NONE, "Remove Card")
        removeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        removeItem.setIcon(R.drawable.ic_discard_actionbar)
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_remove_card -> {
                DecksAdapter.CARDS?.removeCardFromDiscard(gallery.currentItem)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


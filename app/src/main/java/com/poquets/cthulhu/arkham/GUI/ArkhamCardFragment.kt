package com.poquets.cthulhu.arkham.GUI

import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.*
import com.poquets.cthulhu.shared.database.UnifiedCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fragment to display an Arkham card with encounters
 */
class ArkhamCardFragment : Fragment() {
    
    private var card: NeighborhoodCardAdapter? = null
    private var isOtherWorld: Boolean = false
    private var otherWorldCard: OtherWorldCardAdapter? = null
    private val fragmentScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val ARG_CARD_ID = "card_id"
        private const val ARG_NEIGHBORHOOD_ID = "neighborhood_id"
        private const val ARG_IS_OTHER_WORLD = "is_other_world"
        
        fun newInstance(card: NeighborhoodCardAdapter): ArkhamCardFragment {
            val fragment = ArkhamCardFragment()
            val args = Bundle()
            args.putLong(ARG_CARD_ID, card.getID())
            args.putLong(ARG_NEIGHBORHOOD_ID, card.getNeiID())
            args.putBoolean(ARG_IS_OTHER_WORLD, false)
            fragment.arguments = args
            // Store card reference directly (fragments are short-lived)
            fragment.card = card
            return fragment
        }
        
        fun newInstance(card: OtherWorldCardAdapter): ArkhamCardFragment {
            val fragment = ArkhamCardFragment()
            val args = Bundle()
            args.putLong(ARG_CARD_ID, card.getID())
            args.putBoolean(ARG_IS_OTHER_WORLD, true)
            fragment.arguments = args
            // Store card reference directly
            fragment.otherWorldCard = card
            fragment.isOtherWorld = true
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isOtherWorld = it.getBoolean(ARG_IS_OTHER_WORLD, false)
            
            // If card wasn't set via newInstance, recreate from arguments
            // Note: ViewPager2 with FragmentStateAdapter should recreate fragments via newInstance,
            // so this path should rarely be taken. When it is, we create a basic adapter.
            // Encounters may not be available until the card is properly loaded.
            if (isOtherWorld && otherWorldCard == null) {
                val cardId = it.getLong(ARG_CARD_ID)
                // Create adapter - will try to load unified card when getEncounters() is called
                otherWorldCard = OtherWorldCardAdapter(cardId, null, requireContext())
            } else if (!isOtherWorld && card == null) {
                val cardId = it.getLong(ARG_CARD_ID)
                val neighborhoodId = it.getLong(ARG_NEIGHBORHOOD_ID)
                // Create adapter - will try to load unified card when getEncounters() is called
                card = NeighborhoodCardAdapter(cardId, neighborhoodId, null, requireContext())
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val scrollView = inflater.inflate(R.layout.cardlistitem, container, false) as ScrollView
        val cardContents = scrollView.findViewById<LinearLayout>(R.id.card_contents)
        
        // Get encounters
        val encounters = if (isOtherWorld) {
            otherWorldCard?.getEncounters() ?: emptyList()
        } else {
            card?.getEncounters() ?: emptyList()
        }
        
        if (encounters.isEmpty()) {
            // Show placeholder if no encounters
            val placeholder = TextView(requireContext()).apply {
                text = "No encounters available for this card"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(30, 30, 30, 30)
            }
            cardContents.addView(placeholder)
            return scrollView
        }
        
        var lastTextView: TextView? = null
        
        // Add encounter headers and text
        for (i in encounters.indices) {
            val encounter = encounters[i]
            
            // Create encounter header
            val header = inflater.inflate(R.layout.encounterheader, cardContents, false) as RelativeLayout
            val title = header.findViewById<TextView>(R.id.titleTV1)
            val button = header.findViewById<Button>(R.id.button1)
            
            // Set location name
            val location = encounter.getLocation()
            title.text = location?.getLocationName() ?: "Unknown Location"
            
            // Set font
            try {
                val font = Typeface.createFromAsset(requireContext().assets, "fonts/se-caslon-ant.ttf")
                title.typeface = font
            } catch (e: Exception) {
                android.util.Log.w("ArkhamCardFragment", "Font not found: ${e.message}")
            }
            
            // Set padding based on screen size
            title.setPadding(
                getIndependentWidth(title.paddingLeft),
                getIndependentHeight(title.paddingTop),
                getIndependentWidth(title.paddingRight),
                getIndependentHeight(title.paddingBottom)
            )
            
            // Set click listener
            val clickListener = View.OnClickListener {
                val gameState = GameState.getInstance(requireContext())
                gameState.AddHistory(encounter)
                
                if (isOtherWorld) {
                    val cardId = otherWorldCard?.getID() ?: 0L
                    val shuffled = gameState.otherWorldCardSelected(cardId)
                    val message = if (shuffled) {
                        getString(R.string.otherword_arrow_clicked_true)
                    } else {
                        getString(R.string.otherword_arrow_clicked_false)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                } else {
                    val neighborhood = card?.getNeighborhood()
                    if (neighborhood != null) {
                        gameState.randomizeNeighborhood(neighborhood.getID())
                    }
                    Toast.makeText(requireContext(), getString(R.string.encounter_arrow_clicked), Toast.LENGTH_SHORT).show()
                }
                
                activity?.finish()
            }
            
            header.setOnClickListener(clickListener)
            button.setOnClickListener(clickListener)
            
            cardContents.addView(header)
            
            // Create encounter text
            val textView = inflater.inflate(R.layout.encountertext, cardContents, false) as TextView
            textView.text = HtmlCompat.fromHtml(
                encounter.getEncounterText(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            textView.setPadding(
                getIndependentWidth(textView.paddingLeft),
                getIndependentHeight(textView.paddingTop),
                getIndependentWidth(textView.paddingRight),
                getIndependentHeight(textView.paddingBottom)
            )
            
            cardContents.addView(textView)
            lastTextView = textView
        }
        
        // Make last text view fill remaining space
        if (lastTextView != null) {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            lastTextView.layoutParams = params
        }
        
        return scrollView
    }
    
    private fun getIndependentWidth(origWidth: Int): Int {
        val dm: DisplayMetrics = resources.displayMetrics
        return Math.ceil((origWidth * dm.widthPixels) / 480.0).toInt()
    }
    
    private fun getIndependentHeight(origHeight: Int): Int {
        val dm: DisplayMetrics = resources.displayMetrics
        return Math.ceil((origHeight * dm.heightPixels) / 800.0).toInt()
    }
}


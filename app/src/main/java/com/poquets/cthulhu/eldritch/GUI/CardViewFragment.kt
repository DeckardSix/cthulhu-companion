package com.poquets.cthulhu.eldritch.GUI

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.poquets.cthulhu.R
import com.poquets.cthulhu.eldritch.CardAdapter
import com.poquets.cthulhu.eldritch.DecksAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Fragment for displaying an individual Eldritch card
 * Simplified version that works with CardAdapter
 */
class CardViewFragment : Fragment(), View.OnClickListener {
    
    private lateinit var card: CardAdapter
    private lateinit var deckName: String
    private val fragmentScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        fun newInstance(cardAdapter: CardAdapter, deckName: String): CardViewFragment {
            val fragment = CardViewFragment()
            val args = Bundle()
            args.putString("REGION", cardAdapter.region)
            args.putString("ID", cardAdapter.ID)
            args.putString("TOP_HEADER", cardAdapter.topHeader)
            args.putString("TOP_ENCOUNTER", cardAdapter.topEncounter)
            args.putString("MIDDLE_HEADER", cardAdapter.middleHeader)
            args.putString("MIDDLE_ENCOUNTER", cardAdapter.middleEncounter)
            args.putString("BOTTOM_HEADER", cardAdapter.bottomHeader)
            args.putString("BOTTOM_ENCOUNTER", cardAdapter.bottomEncounter)
            args.putString("ENCOUNTERED", cardAdapter.encountered)
            args.putString("DECK_NAME", deckName)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: return
        
        // Reconstruct card from arguments
        val region = args.getString("REGION")
        val id = args.getString("ID") ?: ""
        val topHeader = args.getString("TOP_HEADER")
        val topEncounter = args.getString("TOP_ENCOUNTER")
        val middleHeader = args.getString("MIDDLE_HEADER")
        val middleEncounter = args.getString("MIDDLE_ENCOUNTER")
        val bottomHeader = args.getString("BOTTOM_HEADER")
        val bottomEncounter = args.getString("BOTTOM_ENCOUNTER")
        val encountered = args.getString("ENCOUNTERED") ?: "NONE"
        deckName = args.getString("DECK_NAME") ?: ""
        
        // Create a temporary UnifiedCard to wrap in CardAdapter
        // This is a workaround - in a real implementation, we'd pass the UnifiedCard directly
        val unifiedCard = com.poquets.cthulhu.shared.database.UnifiedCard(
            gameType = com.poquets.cthulhu.shared.database.GameType.ELDRITCH,
            cardId = id,
            expansion = "BASE",
            encountered = encountered,
            region = region,
            topHeader = topHeader,
            topEncounter = topEncounter,
            middleHeader = middleHeader,
            middleEncounter = middleEncounter,
            bottomHeader = bottomHeader,
            bottomEncounter = bottomEncounter
        )
        card = CardAdapter(unifiedCard)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create a FrameLayout to layer the colored card background and text content
        val frameLayout = FrameLayout(requireContext())
        frameLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Set base background to cthulhu_background like Arkham screens
        frameLayout.setBackgroundResource(R.drawable.cthulhu_background)
        val textColor = CardColorUtils.getDeckTextColor(deckName)
        
        // Load colored card image asynchronously (like Arkham does)
        // Use card's region if available, otherwise use deckName
        val regionForCard = card.region ?: deckName
        fragmentScope.launch {
            val cardPath = CardColorUtils.getColoredCardPath(regionForCard)
            Log.d("CardViewFragment", "Loading colored card for region '$regionForCard' (deck: '$deckName'): $cardPath")
            
            if (cardPath != null) {
                try {
                    val cardBitmap = withContext(Dispatchers.IO) {
                        try {
                            val inputStream = requireContext().assets.open(cardPath)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()
                            bitmap
                        } catch (e: IOException) {
                            Log.w("CardViewFragment", "Could not load card image from $cardPath: ${e.message}")
                            null
                        }
                    }
                    
                    if (cardBitmap != null) {
                        Log.d("CardViewFragment", "Loaded card bitmap: ${cardBitmap.width}x${cardBitmap.height}")
                        withContext(Dispatchers.Main) {
                            // Display card image behind text content
                            val cardImageView = ImageView(requireContext()).apply {
                                setImageBitmap(cardBitmap)
                                scaleType = ImageView.ScaleType.FIT_XY
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }
                            // Insert at position 0 so it's behind the text layout
                            frameLayout.addView(cardImageView, 0)
                            Log.d("CardViewFragment", "Added colored card image view")
                        }
                    } else {
                        Log.w("CardViewFragment", "Card bitmap is null for path: $cardPath")
                    }
                } catch (e: Exception) {
                    Log.w("CardViewFragment", "Error loading card image: ${e.message}", e)
                }
            } else {
                Log.d("CardViewFragment", "No colored card path for deck '$deckName', using default background")
            }
        }
        
        val mainLayout = LinearLayout(requireContext())
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(100, 100, 100, 20)
        mainLayout.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        // Make background transparent so card image shows through
        mainLayout.background = null
        
        // Try to load font
        val font = try {
            Typeface.createFromAsset(requireContext().assets, "fonts/se-caslon-ant.ttf")
        } catch (e: Exception) {
            null
        }
        
        // ID
        val idView = TextView(requireContext())
        idView.text = card.ID
        idView.textSize = 16f
        idView.setTextColor(textColor)
        mainLayout.addView(idView)
        
        // Top section
        if (!card.topHeader.isNullOrEmpty()) {
            val topHeaderView = TextView(requireContext())
            topHeaderView.text = card.topHeader
            font?.let { topHeaderView.typeface = it }
            topHeaderView.textSize = 36f
            topHeaderView.setTextColor(textColor)
            mainLayout.addView(topHeaderView)
            
            if (!card.topEncounter.isNullOrEmpty()) {
                val topEncounterView = TextView(requireContext())
                topEncounterView.text = formatText(card.topEncounter, textColor)
                topEncounterView.textSize = 14f
                topEncounterView.setTextColor(textColor)
                topEncounterView.setOnClickListener(this)
                mainLayout.addView(topEncounterView)
            }
        }
        
        // Middle section
        if (!card.middleHeader.isNullOrEmpty()) {
            val middleHeaderView = TextView(requireContext())
            middleHeaderView.text = card.middleHeader
            font?.let { middleHeaderView.typeface = it }
            middleHeaderView.textSize = 36f
            middleHeaderView.setTextColor(textColor)
            mainLayout.addView(middleHeaderView)
            
            if (!card.middleEncounter.isNullOrEmpty()) {
                val middleEncounterView = TextView(requireContext())
                middleEncounterView.text = formatText(card.middleEncounter, textColor)
                middleEncounterView.textSize = 14f
                middleEncounterView.setTextColor(textColor)
                middleEncounterView.setOnClickListener(this)
                mainLayout.addView(middleEncounterView)
            }
        }
        
        // Bottom section
        if (!card.bottomHeader.isNullOrEmpty()) {
            val bottomHeaderView = TextView(requireContext())
            bottomHeaderView.text = card.bottomHeader
            font?.let { bottomHeaderView.typeface = it }
            bottomHeaderView.textSize = 36f
            bottomHeaderView.setTextColor(textColor)
            mainLayout.addView(bottomHeaderView)
            
            if (!card.bottomEncounter.isNullOrEmpty()) {
                val bottomEncounterView = TextView(requireContext())
                bottomEncounterView.text = formatText(card.bottomEncounter, textColor)
                bottomEncounterView.textSize = 14f
                bottomEncounterView.setTextColor(textColor)
                bottomEncounterView.setOnClickListener(this)
                mainLayout.addView(bottomEncounterView)
            }
        }
        
        frameLayout.addView(mainLayout)
        return frameLayout
    }
    
    private fun formatText(text: String?, textColor: Int): Spanned {
        if (text == null) return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
        val textColorHex = String.format("#%06X", 0xFFFFFF and textColor)
        val formatted = text.replace("[", "<b><font color='$textColorHex'>[")
            .replace("]", "]</font></b>")
        return HtmlCompat.fromHtml(formatted, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    
    override fun onClick(v: View) {
        if (card.encountered == "NONE") {
            val encounter = when (v.id) {
                // TODO: Add proper IDs for encounter views
                else -> "top"
            }
            DecksAdapter.CARDS?.discardCard(card.region ?: deckName, card.ID, encounter)
            activity?.finish()
        }
    }
    
    fun getCard(): CardAdapter = card
}


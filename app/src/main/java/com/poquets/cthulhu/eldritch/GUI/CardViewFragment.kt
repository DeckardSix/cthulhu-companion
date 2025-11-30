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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
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
            args.putString("EXPANSION", cardAdapter.expansion)
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
        val expansion = args.getString("EXPANSION") ?: "BASE"
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
            expansion = expansion,
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
        // Use black text color for better readability on light card backgrounds
        val textColor = android.graphics.Color.BLACK
        
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
            val topHeaderContainer = RelativeLayout(requireContext())
            topHeaderContainer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            val topHeaderView = TextView(requireContext())
            topHeaderView.id = View.generateViewId()
            topHeaderView.text = card.topHeader
            font?.let { topHeaderView.typeface = it }
            topHeaderView.textSize = 36f
            topHeaderView.setTextColor(textColor)
            topHeaderView.setPadding(0, 10, 5, 0)
            val topHeaderParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            topHeaderView.layoutParams = topHeaderParams
            topHeaderContainer.addView(topHeaderView)
            
            // Add refresh button next to header (same size as Arkham: 48dp)
            val topRefreshButton = Button(requireContext())
            topRefreshButton.id = View.generateViewId()
            topRefreshButton.background = resources.getDrawable(R.drawable.end_right, null)
            val buttonSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
            ).toInt()
            topRefreshButton.layoutParams = RelativeLayout.LayoutParams(buttonSize, buttonSize).apply {
                addRule(RelativeLayout.CENTER_VERTICAL)
                addRule(RelativeLayout.RIGHT_OF, topHeaderView.id)
            }
            topRefreshButton.gravity = android.view.Gravity.CENTER
            topRefreshButton.setPadding(0, 0, 10, 0)
            topRefreshButton.setOnClickListener {
                if (card.encountered == "NONE") {
                    DecksAdapter.CARDS?.discardCard(card.region ?: deckName, card.ID, "top")
                    activity?.finish()
                }
            }
            topHeaderContainer.addView(topRefreshButton)
            mainLayout.addView(topHeaderContainer)
            
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
            val middleHeaderContainer = RelativeLayout(requireContext())
            middleHeaderContainer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            val middleHeaderView = TextView(requireContext())
            middleHeaderView.id = View.generateViewId()
            middleHeaderView.text = card.middleHeader
            font?.let { middleHeaderView.typeface = it }
            middleHeaderView.textSize = 36f
            middleHeaderView.setTextColor(textColor)
            middleHeaderView.setPadding(0, 10, 5, 0)
            val middleHeaderParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            middleHeaderView.layoutParams = middleHeaderParams
            middleHeaderContainer.addView(middleHeaderView)
            
            // Add refresh button next to header (same size as Arkham: 48dp)
            val middleRefreshButton = Button(requireContext())
            middleRefreshButton.id = View.generateViewId()
            middleRefreshButton.background = resources.getDrawable(R.drawable.end_right, null)
            val buttonSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
            ).toInt()
            middleRefreshButton.layoutParams = RelativeLayout.LayoutParams(buttonSize, buttonSize).apply {
                addRule(RelativeLayout.CENTER_VERTICAL)
                addRule(RelativeLayout.RIGHT_OF, middleHeaderView.id)
            }
            middleRefreshButton.gravity = android.view.Gravity.CENTER
            middleRefreshButton.setPadding(0, 0, 10, 0)
            middleRefreshButton.setOnClickListener {
                if (card.encountered == "NONE") {
                    DecksAdapter.CARDS?.discardCard(card.region ?: deckName, card.ID, "middle")
                    activity?.finish()
                }
            }
            middleHeaderContainer.addView(middleRefreshButton)
            mainLayout.addView(middleHeaderContainer)
            
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
            val bottomHeaderContainer = RelativeLayout(requireContext())
            bottomHeaderContainer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            val bottomHeaderView = TextView(requireContext())
            bottomHeaderView.id = View.generateViewId()
            bottomHeaderView.text = card.bottomHeader
            font?.let { bottomHeaderView.typeface = it }
            bottomHeaderView.textSize = 36f
            bottomHeaderView.setTextColor(textColor)
            bottomHeaderView.setPadding(0, 10, 5, 0)
            val bottomHeaderParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            bottomHeaderView.layoutParams = bottomHeaderParams
            bottomHeaderContainer.addView(bottomHeaderView)
            
            // Add refresh button next to header (same size as Arkham: 48dp)
            val bottomRefreshButton = Button(requireContext())
            bottomRefreshButton.id = View.generateViewId()
            bottomRefreshButton.background = resources.getDrawable(R.drawable.end_right, null)
            val buttonSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
            ).toInt()
            bottomRefreshButton.layoutParams = RelativeLayout.LayoutParams(buttonSize, buttonSize).apply {
                addRule(RelativeLayout.CENTER_VERTICAL)
                addRule(RelativeLayout.RIGHT_OF, bottomHeaderView.id)
            }
            bottomRefreshButton.gravity = android.view.Gravity.CENTER
            bottomRefreshButton.setPadding(0, 0, 10, 0)
            bottomRefreshButton.setOnClickListener {
                if (card.encountered == "NONE") {
                    DecksAdapter.CARDS?.discardCard(card.region ?: deckName, card.ID, "bottom")
                    activity?.finish()
                }
            }
            bottomHeaderContainer.addView(bottomRefreshButton)
            mainLayout.addView(bottomHeaderContainer)
            
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
        
        // Add expansion icon at bottom left (like Arkham cards)
        addExpansionIcon(frameLayout)
        
        return frameLayout
    }
    
    /**
     * Get expansion icon path from expansion name
     * Returns the full path (e.g., "expansion/icon_exp_fl.png" or "checkbox/btn_ba_check_on.png")
     */
    private fun getExpansionIconPath(expansionName: String): String? {
        return when (expansionName.uppercase()) {
            "BASE" -> "checkbox/btn_ba_check_on.png" // Use Arkham base icon for Eldritch base game
            "FORSAKEN_LORE" -> "expansion/icon_exp_fl.png"
            "MOUNTAINS_OF_MADNESS" -> "expansion/icon_exp_mom.png"
            "ANTARCTICA" -> "expansion/icon_exp_mom.png" // Antarctica uses MoM icon
            "STRANGE_REMNANTS" -> "expansion/icon_exp_sr.png"
            "COSMIC_ALIGNMENT" -> "expansion/icon_exp_sr.png" // Cosmic Alignment uses SR icon
            "UNDER_THE_PYRAMIDS" -> "expansion/icon_exp_utp.png"
            "EGYPT" -> "expansion/icon_exp_utp.png" // Egypt uses UtP icon
            "LITANY_OF_SECRETS" -> "expansion/icon_exp_utp.png" // Litany uses UtP icon
            "SIGNS_OF_CARCOSA" -> "expansion/icon_exp_soc.png"
            "THE_DREAMLANDS" -> "expansion/icon_exp_td.png"
            "DREAMLANDS" -> "expansion/icon_exp_td.png" // Dreamlands uses TD icon
            "CITIES_IN_RUIN" -> "expansion/icon_exp_cir.png"
            "MASKS_OF_NYARLATHOTEP" -> "expansion/icon_exp_mon.png"
            else -> {
                Log.w("CardViewFragment", "Unknown expansion name: $expansionName")
                null
            }
        }
    }
    
    /**
     * Add expansion icon to the bottom left of the card (like Arkham cards)
     */
    private fun addExpansionIcon(frameLayout: FrameLayout) {
        fragmentScope.launch {
            try {
                // Get expansion name from card
                val expansionName = card.expansion ?: "BASE"
                Log.d("CardViewFragment", "Card expansion: $expansionName")
                
                // Get icon path (returns full path including folder)
                val iconPath = getExpansionIconPath(expansionName)
                if (iconPath == null) {
                    Log.d("CardViewFragment", "No icon path for expansion: $expansionName")
                    return@launch
                }
                
                Log.d("CardViewFragment", "Loading expansion icon from: $iconPath")
                
                val iconBitmap = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = requireContext().assets.open(iconPath)
                        val opts = BitmapFactory.Options().apply {
                            inScaled = true
                            inDensity = 120 // DisplayMetrics.DENSITY_MEDIUM
                            inTargetDensity = requireContext().resources.displayMetrics.densityDpi
                        }
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, opts)
                        inputStream.close()
                        if (bitmap != null) {
                            Log.d("CardViewFragment", "Successfully loaded expansion icon: ${bitmap.width}x${bitmap.height}")
                        }
                        bitmap
                    } catch (e: IOException) {
                        Log.w("CardViewFragment", "Could not load expansion icon from $iconPath: ${e.message}", e)
                        null
                    }
                }
                
                if (iconBitmap != null) {
                    withContext(Dispatchers.Main) {
                        // Create ImageView for expansion icon at bottom left
                        val iconSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            48f, // 48dp icon size (same as Arkham)
                            requireContext().resources.displayMetrics
                        ).toInt()
                        
                        val expansionIcon = ImageView(requireContext()).apply {
                            setImageBitmap(iconBitmap)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            elevation = 20f // High elevation to ensure icon is above content
                            isClickable = false
                            isFocusable = false
                            layoutParams = FrameLayout.LayoutParams(
                                iconSize,
                                iconSize
                            ).apply {
                                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
                                setMargins(
                                    TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        16f,
                                        requireContext().resources.displayMetrics
                                    ).toInt(), // 16dp left margin
                                    0,
                                    0,
                                    TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        16f,
                                        requireContext().resources.displayMetrics
                                    ).toInt() // 16dp bottom margin
                                )
                            }
                        }
                        
                        // Ensure FrameLayout doesn't clip children
                        frameLayout.clipToPadding = false
                        frameLayout.clipChildren = false
                        
                        // Add icon to frame layout (on top of everything)
                        frameLayout.addView(expansionIcon)
                        expansionIcon.bringToFront()
                        frameLayout.requestLayout()
                        frameLayout.invalidate()
                        
                        Log.d("CardViewFragment", "Added expansion icon for $expansionName at bottom left")
                    }
                } else {
                    Log.w("CardViewFragment", "Expansion icon bitmap is null for: $iconPath")
                }
            } catch (e: Exception) {
                Log.w("CardViewFragment", "Error adding expansion icon: ${e.message}", e)
            }
        }
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


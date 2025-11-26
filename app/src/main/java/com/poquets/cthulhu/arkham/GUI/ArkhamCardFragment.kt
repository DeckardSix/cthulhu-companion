package com.poquets.cthulhu.arkham.GUI

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
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
import kotlinx.coroutines.withContext
import java.io.IOException

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
        // Create a FrameLayout to layer the color background and card image
        val frameLayout = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Load color and card image asynchronously
        fragmentScope.launch {
            // Get neighborhood for color extraction (in IO dispatcher since it may block)
            val neighborhood = withContext(Dispatchers.IO) {
                if (!isOtherWorld) {
                    card?.getNeighborhood()
                } else {
                    null
                }
            }
            
            Log.d("ArkhamCardFragment", "Neighborhood: ${neighborhood?.getNeighborhoodName()}, Card: ${card?.getID()}")
            
            // Extract/get neighborhood color or otherworld color
            val backgroundColor = if (isOtherWorld) {
                // For otherworld cards, get color from selected otherworld colors
                val gameState = GameState.getInstance(requireContext())
                val selectedColors = gameState.getSelectedOtherWorldColors()
                Log.d("ArkhamCardFragment", "Selected otherworld colors: ${selectedColors.size}")
                
                // Get the first selected color (or first color of the card if no selection)
                val colorToUse = if (selectedColors.isNotEmpty()) {
                    selectedColors[0]
                } else {
                    // Fallback to card's first color
                    otherWorldCard?.getOtherWorldColors()?.firstOrNull()
                }
                
                if (colorToUse != null) {
                    // Map color ID to actual color value
                    when (colorToUse.getID().toInt()) {
                        1 -> android.graphics.Color.parseColor("#FFD700") // Yellow
                        2 -> android.graphics.Color.parseColor("#DC143C") // Red (Crimson)
                        3 -> android.graphics.Color.parseColor("#4169E1") // Blue (Royal Blue)
                        4 -> android.graphics.Color.parseColor("#228B22") // Green (Forest Green)
                        else -> android.graphics.Color.TRANSPARENT
                    }
                } else {
                    android.graphics.Color.TRANSPARENT
                }
            } else if (neighborhood != null) {
                // Try to load button image and extract color
                val buttonPath: String? = neighborhood.getNeighborhoodButtonPath()
                Log.d("ArkhamCardFragment", "Button path: $buttonPath")
                val buttonBitmap = if (buttonPath != null && buttonPath.isNotEmpty()) {
                    try {
                        val inputStream = requireContext().assets.open(buttonPath)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        Log.d("ArkhamCardFragment", "Loaded button bitmap: ${bitmap != null}")
                        bitmap
                    } catch (e: IOException) {
                        Log.w("ArkhamCardFragment", "Could not load button image: ${e.message}")
                        null
                    }
                } else {
                    Log.w("ArkhamCardFragment", "Button path is null or empty")
                    null
                }
                
                val color = ArkhamColorUtils.getNeighborhoodColor(buttonBitmap, neighborhood.getNeighborhoodName())
                Log.d("ArkhamCardFragment", "Extracted color: ${String.format("#%08X", color)}")
                color
            } else {
                Log.w("ArkhamCardFragment", "Neighborhood is null")
                android.graphics.Color.TRANSPARENT
            }
            
            // Set background color on main thread
            withContext(Dispatchers.Main) {
                if (backgroundColor != android.graphics.Color.TRANSPARENT) {
                    frameLayout.setBackgroundColor(backgroundColor)
                    Log.d("ArkhamCardFragment", "Set background color: ${String.format("#%08X", backgroundColor)}")
                }
            }
            
            // Load and overlay card image
            // Get card path asynchronously (getCardPath() may block)
            val cardPath: String? = if (!isOtherWorld) {
                withContext(Dispatchers.IO) {
                    card?.getCardPath()
                }
            } else {
                withContext(Dispatchers.IO) {
                    otherWorldCard?.getCardPath()
                }
            }
            
            Log.d("ArkhamCardFragment", "Card path: $cardPath")
            
            if (cardPath != null && cardPath.isNotEmpty()) {
                try {
                    val inputStream = requireContext().assets.open(cardPath)
                    val cardBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    if (cardBitmap != null) {
                        Log.d("ArkhamCardFragment", "Loaded card bitmap: ${cardBitmap.width}x${cardBitmap.height}")
                        withContext(Dispatchers.Main) {
                            val cardImageView = ImageView(requireContext()).apply {
                                setImageBitmap(cardBitmap)
                                scaleType = ImageView.ScaleType.FIT_XY
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }
                            // Insert at position 0 so it's behind the scroll view
                            frameLayout.addView(cardImageView, 0)
                            Log.d("ArkhamCardFragment", "Added card image view")
                        }
                    } else {
                        Log.w("ArkhamCardFragment", "Card bitmap is null for path: $cardPath")
                        // Try fallback
                        tryFallbackCardImage(frameLayout)
                    }
                } catch (e: IOException) {
                    Log.w("ArkhamCardFragment", "Could not load card image from $cardPath: ${e.message}")
                    // Try fallback
                    tryFallbackCardImage(frameLayout)
                }
            } else {
                Log.w("ArkhamCardFragment", "Card path is null or empty, trying fallback")
                // Try fallback
                tryFallbackCardImage(frameLayout)
            }
        }
        
        val scrollView = inflater.inflate(R.layout.cardlistitem, frameLayout, false) as ScrollView
        // Remove the default background so card image and color show through
        scrollView.background = null
        val cardContents = scrollView.findViewById<LinearLayout>(R.id.card_contents)
        // Make card contents background transparent too
        cardContents?.background = null
        
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
                setTextColor(android.graphics.Color.BLACK)
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
            
            // Set text color to black (matching original app)
            title.setTextColor(android.graphics.Color.BLACK)
            
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
            // Ensure text color is black (matching original app)
            textView.setTextColor(android.graphics.Color.BLACK)
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
        
        // Add scroll view on top of card image
        frameLayout.addView(scrollView)
        
        return frameLayout
    }
    
    private fun getIndependentWidth(origWidth: Int): Int {
        val dm: DisplayMetrics = resources.displayMetrics
        return Math.ceil((origWidth * dm.widthPixels) / 480.0).toInt()
    }
    
    private fun getIndependentHeight(origHeight: Int): Int {
        val dm: DisplayMetrics = resources.displayMetrics
        return Math.ceil((origHeight * dm.heightPixels) / 800.0).toInt()
    }
    
    /**
     * Try to load a fallback card image
     */
    private fun tryFallbackCardImage(frameLayout: FrameLayout) {
        fragmentScope.launch {
            withContext(Dispatchers.IO) {
                // Try common otherworld card paths
                val fallbackPaths = listOf(
                    // Generic encounter card template (always present in this app's assets)
                    "encounter/encounter_front_downtown.png",
                    // Legacy otherworld paths kept for compatibility if assets are later added
                    "otherworld/otherworld_front_colorless.png",
                    "otherworld/otherworld_front_red.png"
                )
                
                var loaded = false
                for (path in fallbackPaths) {
                    try {
                        val inputStream = requireContext().assets.open(path)
                        val cardBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (cardBitmap != null) {
                            Log.d("ArkhamCardFragment", "Loaded fallback card bitmap from $path: ${cardBitmap.width}x${cardBitmap.height}")
                            withContext(Dispatchers.Main) {
                                val cardImageView = ImageView(requireContext()).apply {
                                    setImageBitmap(cardBitmap)
                                    scaleType = ImageView.ScaleType.FIT_XY
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                }
                                frameLayout.addView(cardImageView, 0)
                                Log.d("ArkhamCardFragment", "Added fallback card image view")
                            }
                            loaded = true
                            break
                        }
                    } catch (e: IOException) {
                        // Try next path
                        continue
                    }
                }
                
                if (!loaded) {
                    Log.w("ArkhamCardFragment", "Could not load any fallback card image")
                }
            }
        }
    }
}


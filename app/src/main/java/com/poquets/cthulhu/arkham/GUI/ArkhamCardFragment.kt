package com.poquets.cthulhu.arkham.GUI

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.LinearGradient
import android.os.Bundle
import android.util.DisplayMetrics
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
                // For otherworld cards, use the CARD's actual colors, not the location's selected colors
                // This ensures that if a red card is selected, it shows red, and if a yellow card is selected, it shows yellow
                val cardColors = otherWorldCard?.getOtherWorldColors() ?: emptyList()
                Log.d("ArkhamCardFragment", "Card ${otherWorldCard?.getID()} has ${cardColors.size} colors: ${cardColors.map { "${it.getName()}(ID=${it.getID()})" }}")
                
                // Get the first color of the card (cards typically have one primary color)
                val colorToUse = cardColors.firstOrNull()
                
                if (colorToUse != null) {
                    // Map color ID to actual color value
                    val colorValue = when (colorToUse.getID().toInt()) {
                        1 -> android.graphics.Color.parseColor("#FFD700") // Yellow
                        2 -> android.graphics.Color.parseColor("#DC143C") // Red (Crimson)
                        3 -> android.graphics.Color.parseColor("#4169E1") // Blue (Royal Blue)
                        4 -> android.graphics.Color.parseColor("#228B22") // Green (Forest Green)
                        else -> android.graphics.Color.TRANSPARENT
                    }
                    Log.d("ArkhamCardFragment", "Using card color: ${colorToUse.getName()} (ID=${colorToUse.getID()}) -> ${String.format("#%08X", colorValue)}")
                    colorValue
                } else {
                    Log.w("ArkhamCardFragment", "Card has no colors, using transparent")
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
            
            // Set background to cthulhu_background image (not just a color)
            withContext(Dispatchers.Main) {
                // Always use cthulhu_background as the base background
                frameLayout.setBackgroundResource(R.drawable.cthulhu_background)
                Log.d("ArkhamCardFragment", "Set background to cthulhu_background")
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
                            // Display card normally without any modifications
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
                            
                            // Add expansion icon at bottom left for both other world and location cards
                            addExpansionIcon(frameLayout)
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
        // Ensure ScrollView doesn't block the expansion icon
        scrollView.elevation = 0f
        // Add bottom padding to leave space for expansion icon (48dp icon + 16dp margin = 64dp)
        val iconSpace = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            64f,
            resources.displayMetrics
        ).toInt()
        scrollView.setPadding(
            scrollView.paddingLeft,
            scrollView.paddingTop,
            scrollView.paddingRight,
            scrollView.paddingBottom + iconSpace
        )
        val cardContents = scrollView.findViewById<LinearLayout>(R.id.card_contents)
        // Make card contents background transparent too
        cardContents?.background = null
        
        // Get encounters
        val encounters = if (isOtherWorld) {
            // For otherworld cards, sort by location name alphabetically (A to Z)
            // "Other" location should always be at the bottom
            val unsortedEncounters = otherWorldCard?.getEncounters() ?: emptyList()
            unsortedEncounters.sortedWith(compareBy<EncounterAdapter> { encounter ->
                val locationName = encounter.getLocation()?.getLocationName() ?: ""
                // "Other" should be sorted last, so use "ZZZ" for it
                if (locationName.equals("Other", ignoreCase = true)) {
                    "ZZZ"
                } else {
                    locationName.uppercase() // Case-insensitive sorting
                }
            }.thenBy { encounter ->
                // Secondary sort by encounter ID for consistency
                encounter.getID()
            })
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
                                // Display card normally without any modifications
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
                                
                                // Add expansion icon at bottom left for both other world and location cards
                                addExpansionIcon(frameLayout)
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
    
    /**
     * Add expansion icon to the bottom left of cards (both other world and location cards)
     */
    private fun addExpansionIcon(frameLayout: FrameLayout) {
        fragmentScope.launch {
            try {
                // Get expansion ID from the card (works for both other world and neighborhood cards)
                val expIds = withContext(Dispatchers.IO) {
                    if (isOtherWorld) {
                        otherWorldCard?.getExpIDs() ?: emptyList()
                    } else {
                        card?.getExpIDs() ?: emptyList()
                    }
                }
                
                Log.d("ArkhamCardFragment", "Found ${expIds.size} expansion IDs: $expIds for ${if (isOtherWorld) "other world" else "location"} card")
                
                if (expIds.isEmpty()) {
                    Log.d("ArkhamCardFragment", "No expansion IDs found for card")
                    return@launch
                }
                
                // Use the first expansion ID (cards typically belong to one primary expansion)
                var expId = expIds.first()
                
                Log.d("ArkhamCardFragment", "Raw expansion ID from getExpIDs(): $expId")
                
                // CRITICAL FIX: Ensure expansion ID is in valid range (1-10)
                // The database might have different IDs, but ExpansionAdapter only supports 1-10
                if (expId < 1 || expId > 10) {
                    Log.w("ArkhamCardFragment", "Expansion ID $expId is out of range (1-10), using base game (ID=1)")
                    expId = 1L
                }
                
                // DIRECT FIX: Use ExpansionAdapter.getCheckboxOnPath() directly with the ID
                // This works for all IDs 1-10 without needing to look up the expansion
                // This is more reliable than looking up the expansion in the map
                val tempExpansion = ExpansionAdapter(expId, "Expansion $expId", null)
                val iconPath = tempExpansion.getCheckboxOnPath()
                
                Log.d("ArkhamCardFragment", "Using expansion icon path: $iconPath for expansion ID $expId")
                Log.d("ArkhamCardFragment", "ExpansionAdapter.getCheckboxOnPath() returned: $iconPath for ID $expId")
                
                // Verify the path is correct
                if (iconPath == "checkbox/btn_dh_check_on.png" && expId != 3L) {
                    Log.w("ArkhamCardFragment", "WARNING: Got default icon path (btn_dh_check_on.png) for non-Dunwich expansion ID $expId")
                }
                
                loadAndAddIcon(frameLayout, iconPath, expId)
            } catch (e: Exception) {
                Log.w("ArkhamCardFragment", "Error adding expansion icon: ${e.message}", e)
            }
        }
    }
    
    /**
     * Helper method to load and add the expansion icon to the frame layout
     */
    private fun loadAndAddIcon(frameLayout: FrameLayout, iconPath: String, expId: Long) {
        fragmentScope.launch {
            try {
                // Load icon bitmap
                val iconBitmap = withContext(Dispatchers.IO) {
                    try {
                        Log.d("ArkhamCardFragment", "Attempting to load icon from assets: $iconPath")
                        val inputStream = requireContext().assets.open(iconPath)
                        val opts = BitmapFactory.Options().apply {
                            inScaled = true
                            inDensity = 120 // DisplayMetrics.DENSITY_MEDIUM
                            inTargetDensity = requireContext().resources.displayMetrics.densityDpi
                        }
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, opts)
                        inputStream.close()
                        if (bitmap != null) {
                            Log.d("ArkhamCardFragment", "Successfully loaded icon bitmap: ${bitmap.width}x${bitmap.height} from $iconPath")
                        } else {
                            Log.w("ArkhamCardFragment", "BitmapFactory returned null for $iconPath")
                        }
                        bitmap
                    } catch (e: IOException) {
                        Log.e("ArkhamCardFragment", "Could not load expansion icon from $iconPath: ${e.message}", e)
                        // List available checkbox files for debugging
                        try {
                            val checkboxFiles = requireContext().assets.list("checkbox")
                            Log.d("ArkhamCardFragment", "Available checkbox files: ${checkboxFiles?.joinToString(", ")}")
                        } catch (e2: Exception) {
                            Log.w("ArkhamCardFragment", "Could not list checkbox directory: ${e2.message}")
                        }
                        null
                    }
                }
                
                if (iconBitmap != null) {
                    withContext(Dispatchers.Main) {
                        // Create ImageView for expansion icon at bottom left
                        val iconSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            48f, // 48dp icon size
                            requireContext().resources.displayMetrics
                        ).toInt()
                        
                        val expansionIcon = ImageView(requireContext()).apply {
                            setImageBitmap(iconBitmap)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            elevation = 20f // High elevation to ensure icon is above ScrollView
                            // Make sure icon is clickable and focusable so it's in the touch hierarchy
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
                        
                        // Add icon to frame layout (on top of everything, after ScrollView)
                        // Add at the end to ensure it's on top
                        frameLayout.addView(expansionIcon)
                        // Bring to front to ensure it's visible above ScrollView
                        expansionIcon.bringToFront()
                        frameLayout.requestLayout() // Request layout update
                        frameLayout.invalidate() // Force redraw
                        
                        // Log frameLayout dimensions and icon position for debugging
                        frameLayout.post {
                            Log.d("ArkhamCardFragment", "Added expansion icon for expansion $expId at bottom left")
                            Log.d("ArkhamCardFragment", "FrameLayout: ${frameLayout.width}x${frameLayout.height}, Icon: ${expansionIcon.width}x${expansionIcon.height}, Position: (${expansionIcon.left}, ${expansionIcon.top})")
                            Log.d("ArkhamCardFragment", "FrameLayout child count: ${frameLayout.childCount}, Icon elevation: ${expansionIcon.elevation}")
                        }
                    }
                } else {
                    Log.w("ArkhamCardFragment", "Icon bitmap is null for path: $iconPath")
                }
            } catch (e: Exception) {
                Log.w("ArkhamCardFragment", "Error loading icon: ${e.message}", e)
            }
        }
    }
    
    /**
     * Create a bitmap with transparent top portion (where the colored band is)
     * to allow the cthulhu_background to show through
     */
    private fun createTopTransparentBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        // Make the top 10-15% transparent (where the colored band typically is)
        val transparentHeight = (height * 0.15f).toInt()
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Draw the original bitmap first
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        // Create a gradient mask that fades from fully transparent at top to fully opaque
        // The gradient should be fully transparent for most of the top portion, then fade to opaque
        val gradient = LinearGradient(
            0f, 0f,
            0f, transparentHeight.toFloat(),
            intArrayOf(
                android.graphics.Color.TRANSPARENT,  // Fully transparent at very top
                android.graphics.Color.TRANSPARENT,  // Still transparent (most of the band)
                android.graphics.Color.TRANSPARENT,  // Still transparent
                android.graphics.Color.BLACK          // Fully opaque (card visible)
            ),
            floatArrayOf(0f, 0.6f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        
        // Apply the gradient mask using DST_IN blend mode
        // This makes pixels transparent where the gradient is transparent
        val paint = Paint().apply {
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        
        // Draw the gradient mask over the top portion
        canvas.drawRect(0f, 0f, width.toFloat(), transparentHeight.toFloat(), paint)
        
        return result
    }
}


package com.poquets.cthulhu.arkham.GUI

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.viewpager2.widget.ViewPager2
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.AHFlyweightFactory
import com.poquets.cthulhu.arkham.EncounterAdapter
import com.poquets.cthulhu.arkham.GameState
import com.poquets.cthulhu.arkham.NeighborhoodCardAdapter
import com.poquets.cthulhu.arkham.OtherWorldCardAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

class LocationHxActivity : AppCompatActivity() {
    
    private var encAdapter: EncounterHxAdapter? = null
    private var noHx = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AHFlyweightFactory.INSTANCE.Init(this.applicationContext)
        
        val encHx = GameState.getInstance(this).getEncounterHx()
        
        if (encHx.isNotEmpty()) {
            noHx = false
            setContentView(R.layout.locationdeck)
            
            // Add padding at top to account for status bar
            val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
            val statusBarHeight = getStatusBarHeight()
            rootLayout?.setPadding(0, statusBarHeight, 0, 0)
            
            // Set background to cthulhu_background (not a color)
            rootLayout?.setBackgroundResource(R.drawable.cthulhu_background)
            
            val viewpager = findViewById<ViewPager2>(R.id.viewpager)
            encAdapter = EncounterHxAdapter(this, encHx.toMutableList())
            viewpager.adapter = encAdapter
            
            // Add back button after ViewPager is set up
            addBackButton()
        } else {
            noHx = true
            setContentView(R.layout.empty_hx)
            
            // Add atmospheric effects for the time-themed Hx screen
            addAtmosphericEffects()
        }
    }
    
    /**
     * Adds atmospheric effects to the Cthulhu-themed Hx screen
     */
    private fun addAtmosphericEffects() {
        // Find the Cthulhu image view
        val timeImage = findViewById<ImageView>(R.id.timeImage)
        if (timeImage != null) {
            // Add a subtle rotation animation to simulate ancient horrors stirring
            val rotation = android.view.animation.RotateAnimation(
                0f, 360f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotation.duration = 30000 // 30 seconds for a full rotation
            rotation.repeatCount = android.view.animation.Animation.INFINITE
            rotation.interpolator = android.view.animation.LinearInterpolator()
            timeImage.startAnimation(rotation)
            
            // Add a subtle fade effect to the atmospheric text
            val atmosphericText = findViewById<TextView>(R.id.atmosphericText)
            if (atmosphericText != null) {
                val fade = android.view.animation.AlphaAnimation(0.6f, 1.0f)
                fade.duration = 2000
                fade.repeatCount = android.view.animation.Animation.INFINITE
                fade.repeatMode = android.view.animation.Animation.REVERSE
                atmosphericText.startAnimation(fade)
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (noHx) {
            return false
        } else {
            val inflater: MenuInflater = menuInflater
            inflater.inflate(R.menu.location_hx_menu, menu)
            return true
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.delete_card -> {
                deleteCard()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun deleteCard() {
        val viewpager = findViewById<ViewPager2>(R.id.viewpager)
        val currentItem = viewpager.currentItem
        encAdapter?.remove(currentItem)
        encAdapter?.notifyDataSetChanged()
        
        if (encAdapter?.itemCount == 0) {
            noHx = true
            setContentView(R.layout.empty_hx)
            
            // Add atmospheric effects when history becomes empty
            addAtmosphericEffects()
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
    
    inner class EncounterHxAdapter(
        private val context: android.content.Context,
        private val encArr: MutableList<EncounterAdapter>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<EncounterHxAdapter.ViewHolder>() {
        
        init {
            Log.w("AHEncounters", "${encArr.size} encounters in Hx.")
        }
        
        override fun getItemCount(): Int = encArr.size
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Create a FrameLayout to hold the card content
            val frameLayout = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            return ViewHolder(frameLayout)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val encounter = encArr[position]
            Log.d("LocationHxActivity", "onBindViewHolder: position=$position, encounter ID=${encounter.getID()}")
            val theCard = AHFlyweightFactory.INSTANCE.getCardByEncID(encounter.getID())
            Log.d("LocationHxActivity", "getCardByEncID returned: ${theCard?.javaClass?.simpleName ?: "null"}")
            val encounters = when (theCard) {
                is NeighborhoodCardAdapter -> {
                    val encs = theCard.getEncounters()
                    Log.d("LocationHxActivity", "NeighborhoodCardAdapter.getEncounters() returned ${encs.size} encounters")
                    encs
                }
                is OtherWorldCardAdapter -> {
                    val encs = theCard.getEncounters()
                    Log.d("LocationHxActivity", "OtherWorldCardAdapter.getEncounters() returned ${encs.size} encounters")
                    encs
                }
                else -> {
                    Log.w("LocationHxActivity", "getCardByEncID returned null or unknown type for encounter ${encounter.getID()}")
                    emptyList()
                }
            }
            
            holder.bind(encounter, theCard, encounters)
        }
        
        override fun getItemId(position: Int): Long {
            return encArr[position].getID()
        }
        
        fun remove(position: Int) {
            GameState.getInstance(context as AppCompatActivity).removeHx(position)
            encArr.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            fun bind(selectedEncounter: EncounterAdapter, theCard: Any?, encounters: List<EncounterAdapter>) {
                val frameLayout = itemView as FrameLayout
                
                // Clear existing views
                frameLayout.removeAllViews()
                
                // Create scroll view for encounters
                val scrollView = LayoutInflater.from(context).inflate(R.layout.cardlistitem, frameLayout, false) as ScrollView
                scrollView.background = null
                val cardContents = scrollView.findViewById<LinearLayout>(R.id.card_contents)
                
                var lastTextView: TextView? = null
                
                // Add encounter headers and text
                for (encounter in encounters) {
                    val encounterText = encounter.getEncounterText()
                    val locationName = encounter.getLocation()?.getLocationName() ?: ""
                    
                    Log.d("LocationHxActivity", "Adding encounter ${encounter.getID()}: location='$locationName', text length=${encounterText.length}")
                    
                    val header = LayoutInflater.from(context).inflate(R.layout.encounterheader, cardContents, false) as RelativeLayout
                    val title = header.findViewById<TextView>(R.id.titleTV1)
                    title.setPadding(
                        getIndependentWidth(title.paddingLeft),
                        getIndependentHeight(title.paddingTop),
                        getIndependentWidth(title.paddingRight),
                        getIndependentHeight(title.paddingBottom)
                    )
                    title.text = locationName
                    // Ensure title text color is visible
                    title.setTextColor(android.graphics.Color.BLACK)
                    
                    // Don't need to select encounters in our Hx
                    val chooseEncounterBtn = header.findViewById<Button>(R.id.button1)
                    header.removeView(chooseEncounterBtn)
                    
                    val text = LayoutInflater.from(context).inflate(R.layout.encountertext, cardContents, false) as TextView
                    // Set text color FIRST before setting HTML content to ensure it's not overridden
                    text.setTextColor(android.graphics.Color.BLACK)
                    // Remove any text appearance that might set white color
                    text.setTextAppearance(android.R.style.TextAppearance)
                    
                    if (encounterText.isNotEmpty()) {
                        // Use FROM_HTML_MODE_COMPACT to avoid color overrides from HTML
                        val spanned = HtmlCompat.fromHtml(encounterText, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        text.text = spanned
                        // Force text color again after HTML is applied (HTML might have color tags)
                        text.setTextColor(android.graphics.Color.BLACK)
                    } else {
                        text.text = "[No encounter text]"
                        Log.w("LocationHxActivity", "Encounter ${encounter.getID()} has empty text!")
                    }
                    // Ensure text is visible by setting a semi-transparent background
                    text.setBackgroundColor(android.graphics.Color.argb(200, 255, 255, 255))
                    text.setPadding(
                        getIndependentWidth(text.paddingLeft),
                        getIndependentHeight(text.paddingTop),
                        getIndependentWidth(text.paddingRight),
                        getIndependentHeight(text.paddingBottom)
                    )
                    
                    // Shade encounters that are not the selected one (compare by ID)
                    if (encounter.getID() != selectedEncounter.getID()) {
                        val shadedColor = getColor(R.color.shaded_hx)
                        header.setBackgroundColor(shadedColor)
                        text.setBackgroundColor(android.graphics.Color.argb(200, 0, 0, 0))
                    }
                    
                    cardContents.addView(header)
                    cardContents.addView(text)
                    lastTextView = text
                }
                
                // Last text fills the rest of the space
                if (lastTextView != null) {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    lastTextView.layoutParams = params
                }
                
                // Load and overlay card image BEFORE adding scrollView to ensure proper layering
                if (theCard != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val cardPath = withContext(Dispatchers.IO) {
                                when (theCard) {
                                    is NeighborhoodCardAdapter -> theCard.getCardPath()
                                    is OtherWorldCardAdapter -> theCard.getCardPath()
                                    else -> null
                                }
                            }
                            
                            if (cardPath != null) {
                                val front = withContext(Dispatchers.IO) {
                                    try {
                                        BitmapFactory.decodeStream(context.assets.open(cardPath))
                                    } catch (e: IOException) {
                                        // Try fallback card image
                                        try {
                                            BitmapFactory.decodeStream(context.assets.open("encounter/encounter_front_downtown.png"))
                                        } catch (e2: IOException) {
                                            null
                                        }
                                    }
                                }
                                
                                if (front != null) {
                                    val result = overlayCard(front, theCard)
                                    scrollView.background = android.graphics.drawable.BitmapDrawable(context.resources, result)
                                    Log.d("LocationHxActivity", "Set card background for encounter ${selectedEncounter.getID()}")
                                } else {
                                    Log.w("LocationHxActivity", "Could not load card image for encounter ${selectedEncounter.getID()}")
                                }
                            } else {
                                Log.w("LocationHxActivity", "No card path for encounter ${selectedEncounter.getID()}")
                            }
                        } catch (e: Exception) {
                            Log.w("LocationHxActivity", "Error loading card image: ${e.message}")
                        }
                    }
                }
                
                // Add scrollView to frameLayout AFTER setting up content
                frameLayout.addView(scrollView)
                Log.d("LocationHxActivity", "Added scrollView with ${encounters.size} encounters to frameLayout")
            }
            
            private fun overlayCard(bmp1: Bitmap, card: Any): Bitmap {
                var retBmp = bmp1
                var totalWidth = 0
                
                val expIds = when (card) {
                    is NeighborhoodCardAdapter -> card.getExpIDs()
                    is OtherWorldCardAdapter -> card.getExpIDs()
                    else -> emptyList()
                }
                
                for (expId in expIds) {
                    val expansion = AHFlyweightFactory.INSTANCE.getExpansion(expId)
                    val path = expansion?.getExpansionIconPath()
                    
                    var expBmp: Bitmap? = null
                    if (path != null) {
                        try {
                            expBmp = BitmapFactory.decodeStream(context.assets.open(path))
                        } catch (e: IOException) {
                            expBmp = null
                        }
                    }
                    
                    retBmp = overlay(retBmp, expBmp, totalWidth + 10)
                    if (expBmp != null) {
                        totalWidth += expBmp.width
                    }
                }
                
                return retBmp
            }
            
            private fun overlay(bmp1: Bitmap, bmp2: Bitmap?, rightMargin: Int): Bitmap {
                if (bmp2 == null) {
                    return bmp1
                }
                
                val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmOverlay)
                canvas.drawBitmap(bmp1, 0f, 0f, null)
                val mtx = Matrix()
                val resizeWidthPercentage = bmp1.width / 305.0f
                val top = bmp1.height - (bmp2.height + 10) * resizeWidthPercentage
                val left = bmp1.width - (bmp2.width + rightMargin) * resizeWidthPercentage
                mtx.setScale(resizeWidthPercentage, resizeWidthPercentage)
                mtx.postTranslate(left, top)
                val paint = Paint()
                paint.isFilterBitmap = true
                canvas.drawBitmap(bmp2, mtx, paint)
                return bmOverlay
            }
            
            private fun getIndependentWidth(origWidth: Int): Int {
                val dm = context.resources.displayMetrics
                return Math.ceil((origWidth * dm.widthPixels) / 480.0).toInt()
            }
            
            private fun getIndependentHeight(origHeight: Int): Int {
                val dm = context.resources.displayMetrics
                return Math.ceil((origHeight * dm.heightPixels) / 800.0).toInt()
            }
        }
    }
}


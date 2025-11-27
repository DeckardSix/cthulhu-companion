package com.poquets.cthulhu.arkham.GUI

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.arkham.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Otherworld selector activity for Arkham Horror
 * Displays list of otherworld locations
 */
class OtherworldSelector : AppCompatActivity() {
    
    private lateinit var listView: ListView
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private val bmpCache = SparseArray<Bitmap>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otherworld_selector)
        
        // Initialize factories
        AHFlyweightFactory.Init(applicationContext)
        GameState.getInstance(applicationContext)
        
        listView = findViewById(R.id.locationListView)
        
        // Get otherworld locations
        activityScope.launch {
            val locations = AHFlyweightFactory.INSTANCE.getCurrentOtherWorldLocations()
            android.util.Log.d("OtherworldSelector", "Found ${locations.size} otherworld locations")
            if (locations.isEmpty()) {
                android.util.Log.w("OtherworldSelector", "WARNING: No otherworld locations found! This might indicate a migration issue.")
            } else {
                android.util.Log.d("OtherworldSelector", "Otherworld location names: ${locations.map { "${it.getLocationName()}(ID=${it.getID()})" }.joinToString(", ")}")
            }
            val cursor = LocationCursorAdapter(locations)
            
            // Create adapter
            val columns = arrayOf("Left", "Right")
            val to = intArrayOf(R.id.name_entry, R.id.name_entry2)
            val adapter = SimpleCursorAdapter(this@OtherworldSelector, R.layout.location_button, cursor, columns, to, 0)
            
            adapter.setViewBinder { view, cursor, columnIndex ->
                if (columnIndex == 1 || columnIndex == 2) {
                    val button = view as Button
                    val location = (cursor as LocationCursorAdapter).getLocation(columnIndex)
                    
                    if (location == null) {
                        button.visibility = View.INVISIBLE
                    } else {
                        button.visibility = View.VISIBLE
                        button.text = location.getLocationName()
                        
                        // Set font
                        try {
                            val font = Typeface.createFromAsset(assets, "fonts/se-caslon-ant.ttf")
                            button.typeface = font
                        } catch (e: Exception) {
                            Log.w("OtherworldSelector", "Font not found: ${e.message}")
                        }
                        
                        // Set background with color overlay (matching original app)
                        button.setBackgroundResource(R.drawable.otherworld_loc_btn_transparent)
                        button.post {
                            try {
                                // Check if colors exist for this location
                                val colors = location.getOtherWorldColors()
                                Log.d("OtherworldSelector", "Location ${location.getLocationName()} (ID=${location.getID()}) has ${colors.size} colors: ${colors.map { "${it.getName()}(ID=${it.getID()})" }}")
                                
                                val bmp = if (bmpCache.get(location.getID().toInt(), null) != null) {
                                    Log.d("OtherworldSelector", "Using cached bitmap for location ${location.getID()}")
                                    bmpCache.get(location.getID().toInt())
                                } else {
                                    val baseBmp = if (bmpCache.get(-1, null) != null) {
                                        bmpCache.get(-1)
                                    } else {
                                        val decoded = BitmapFactory.decodeResource(resources, R.drawable.otherworld_loc_btn)
                                        if (decoded == null) {
                                            Log.e("OtherworldSelector", "Failed to decode otherworld_loc_btn resource")
                                        } else {
                                            Log.d("OtherworldSelector", "Decoded base button image: ${decoded.width}x${decoded.height}")
                                        }
                                        bmpCache.append(-1, decoded)
                                        decoded
                                    }
                                    if (baseBmp != null) {
                                        Log.d("OtherworldSelector", "Calling overlayBtn for location ${location.getID()} with ${colors.size} colors")
                                        overlayBtn(baseBmp, location, button)
                                    } else {
                                        null
                                    }
                                }
                                if (bmp != null) {
                                    Log.d("OtherworldSelector", "Setting button background with bitmap: ${bmp.width}x${bmp.height}")
                                    button.background = BitmapDrawable(resources, bmp)
                                } else {
                                    Log.w("OtherworldSelector", "Bitmap is null, using fallback")
                                    button.setBackgroundResource(R.drawable.otherworld_loc_btn)
                                }
                            } catch (e: Exception) {
                                Log.e("OtherworldSelector", "Error loading button image: ${e.message}", e)
                                e.printStackTrace()
                                button.setBackgroundResource(R.drawable.otherworld_loc_btn)
                            }
                        }
                        
                        // Set click listener
                        button.setOnClickListener {
                            val otherWorldColors = location.getOtherWorldColors()
                            // Clear selected colors
                            GameState.getInstance(applicationContext).clearSelectedOtherWorldColor()
                            
                            // Set selected location ID
                            GameState.getInstance(applicationContext).setSelectedOtherWorldLocation(location.getID())
                            
                            // Set selected colors based on location's colors
                            for (color in otherWorldColors) {
                                GameState.getInstance(applicationContext).addSelectedOtherWorldColor(color)
                            }
                            
                            // Launch OtherWorldDeckActivity
                            val intent = Intent(this@OtherworldSelector, OtherWorldDeckActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    
                    true
                } else {
                    false
                }
            }
            
            listView.adapter = adapter
        }
    }
    
    fun openNeighborhood(view: View) {
        val intent = Intent(this, NeighborhoodSelector::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
    
    fun openEncHx(view: View) {
        // TODO: Launch LocationHxActivity
        Toast.makeText(this, "LocationHxActivity not yet implemented", Toast.LENGTH_SHORT).show()
    }
    
    fun openOW(view: View) {
        // TODO: Launch OtherWorldDeckActivity
        Toast.makeText(this, "OtherWorldDeckActivity not yet implemented", Toast.LENGTH_SHORT).show()
    }
    
    
    /**
     * Overlay color pips on button (matching original app)
     */
    private fun overlayBtn(bmp1: Bitmap, loc: LocationAdapter, but: Button): Bitmap {
        val widthDeform = but.width.toFloat() / bmp1.width
        val heightDeform = but.height.toFloat() / bmp1.height
        
        val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(bmp1, 0f, 0f, null)
        
        var resizeToFitWidth = 166.0f / 225.0f
        var resizeToFitHeight = 166.0f / 225.0f
        
        if (widthDeform > heightDeform) {
            resizeToFitWidth = (resizeToFitWidth / widthDeform) * heightDeform
        } else if (widthDeform < heightDeform) {
            resizeToFitHeight = (resizeToFitHeight / heightDeform) * widthDeform
        }
        
        val paint = Paint()
        paint.isFilterBitmap = true
        
        val owcs = loc.getOtherWorldColors()
        Log.d("OtherworldSelector", "Location ${loc.getLocationName()} has ${owcs.size} colors")
        
        // Position colors in a 2x2 grid in the top-right corner (matching original app)
        for (i in owcs.indices) {
            if (i >= 4) break // Max 4 colors
            
            val mtx = Matrix()
            mtx.setScale(resizeToFitWidth, resizeToFitHeight)
            
            var colorBmp: Bitmap? = null
            val colorId = owcs[i].getID().toInt()
            if (bmpCache.get(colorId + 100, null) != null) {
                colorBmp = bmpCache.get(colorId + 100)
            } else {
                colorBmp = when (colorId) {
                    1 -> BitmapFactory.decodeResource(resources, R.drawable.yellow_on)
                    2 -> BitmapFactory.decodeResource(resources, R.drawable.red_on)
                    3 -> BitmapFactory.decodeResource(resources, R.drawable.blue_on)
                    4 -> BitmapFactory.decodeResource(resources, R.drawable.green_on)
                    else -> null
                }
                if (colorBmp != null) {
                    bmpCache.append(colorId + 100, colorBmp)
                }
            }
            
            if (colorBmp != null) {
                var topMargin = 10
                var rightMargin = 10
                
                // Position in 2x2 grid: i=0 (top-left of grid), i=1 (top-right of grid), 
                // i=2 (bottom-left of grid), i=3 (bottom-right of grid)
                // The grid is positioned in the top-right corner of the button
                if (i / 2 == 1) {
                    // Second row (i=2 or i=3)
                    topMargin = topMargin + colorBmp.height + 5
                }
                if (i % 2 == 1) {
                    // Right column (i=1 or i=3)
                    rightMargin = rightMargin + colorBmp.width + 5
                }
                
                val top = (topMargin * resizeToFitHeight)
                val left = (bmp1.width - (colorBmp.width + rightMargin) * resizeToFitWidth)
                mtx.postTranslate(left, top)
                canvas.drawBitmap(colorBmp, mtx, paint)
                Log.d("OtherworldSelector", "Placed color ${owcs[i].getName()} (ID=${colorId}) at position $i: top=$top, left=$left")
            }
        }
        
        val cachedBmp = bmOverlay.copy(bmOverlay.config ?: Bitmap.Config.ARGB_8888, false)
        bmpCache.append(loc.getID().toInt(), cachedBmp)
        return bmOverlay
    }
}


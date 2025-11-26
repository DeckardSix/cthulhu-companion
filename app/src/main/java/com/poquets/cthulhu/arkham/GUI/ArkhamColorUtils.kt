package com.poquets.cthulhu.arkham.GUI

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

/**
 * Utility class for extracting and mapping colors for Arkham neighborhoods
 */
object ArkhamColorUtils {
    
    private const val TAG = "ArkhamColorUtils"
    
    /**
     * Extract dominant color from a bitmap image
     * Samples pixels from the center region to get the most common color
     */
    fun extractDominantColor(bitmap: Bitmap?): Int {
        if (bitmap == null || bitmap.isRecycled) {
            return Color.TRANSPARENT
        }
        
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            // Sample from center region (avoid edges which might be transparent or borders)
            val startX = width / 4
            val endX = width * 3 / 4
            val startY = height / 4
            val endY = height * 3 / 4
            
            val colorCounts = mutableMapOf<Int, Int>()
            var maxCount = 0
            var dominantColor = Color.TRANSPARENT
            
            // Sample pixels in the center region
            for (x in startX until endX step 5) {
                for (y in startY until endY step 5) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = Color.alpha(pixel)
                    
                    // Skip transparent pixels
                    if (alpha < 128) continue
                    
                    // Round to nearest color (reduce precision to group similar colors)
                    val roundedColor = roundColor(pixel)
                    val count = colorCounts.getOrDefault(roundedColor, 0) + 1
                    colorCounts[roundedColor] = count
                    
                    if (count > maxCount) {
                        maxCount = count
                        dominantColor = roundedColor
                    }
                }
            }
            
            // If we found a color, return it with full opacity
            if (dominantColor != Color.TRANSPARENT) {
                return Color.rgb(
                    Color.red(dominantColor),
                    Color.green(dominantColor),
                    Color.blue(dominantColor)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting color from bitmap: ${e.message}")
        }
        
        return Color.TRANSPARENT
    }
    
    /**
     * Round color to reduce precision and group similar colors
     */
    private fun roundColor(color: Int): Int {
        val r = (Color.red(color) / 16) * 16
        val g = (Color.green(color) / 16) * 16
        val b = (Color.blue(color) / 16) * 16
        return Color.rgb(r, g, b)
    }
    
    /**
     * Get color for a neighborhood by name (fallback if image extraction fails)
     * These colors are based on typical Arkham Horror neighborhood themes
     */
    fun getNeighborhoodColor(neighborhoodName: String?): Int {
        if (neighborhoodName == null) {
            return Color.TRANSPARENT
        }
        
        // Map neighborhood names to colors (based on typical Arkham color schemes)
        return when (neighborhoodName.uppercase()) {
            "DOWNTOWN" -> Color.parseColor("#8B4513") // Brown/Sienna
            "EASTTOWN" -> Color.parseColor("#2F4F4F") // Dark Slate Gray
            "FRENCH HILL" -> Color.parseColor("#556B2F") // Dark Olive Green
            "MERCHANT DISTRICT", "MERCHANT" -> Color.parseColor("#B8860B") // Dark Goldenrod
            "MISKATONIC UNIVERSITY", "MISKATONIC" -> Color.parseColor("#191970") // Midnight Blue
            "NORTHSIDE" -> Color.parseColor("#800080") // Purple
            "RIVERTOWN" -> Color.parseColor("#008B8B") // Dark Cyan
            "SOUTHSIDE" -> Color.parseColor("#8B0000") // Dark Red
            "UPTOWN" -> Color.parseColor("#4B0082") // Indigo
            else -> {
                // Default: try to derive a color from the name hash
                val hash = neighborhoodName.hashCode()
                val r = (hash and 0xFF0000) shr 16
                val g = (hash and 0x00FF00) shr 8
                val b = hash and 0x0000FF
                // Make it darker/more muted
                Color.rgb(r / 2, g / 2, b / 2)
            }
        }
    }
    
    /**
     * Get color for a neighborhood, trying image extraction first, then name mapping
     */
    fun getNeighborhoodColor(buttonBitmap: Bitmap?, neighborhoodName: String?): Int {
        // Try to extract color from button image first
        val extractedColor = extractDominantColor(buttonBitmap)
        if (extractedColor != Color.TRANSPARENT) {
            return extractedColor
        }
        
        // Fallback to name-based mapping
        return getNeighborhoodColor(neighborhoodName)
    }
}


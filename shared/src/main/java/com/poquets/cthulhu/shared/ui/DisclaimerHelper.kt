package com.poquets.cthulhu.shared.ui

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.CompoundButtonCompat

/**
 * Shared disclaimer helper that can be used by both Arkham and Eldritch games
 */
object DisclaimerHelper {
    
    private const val PREFS_NAME = "cthulhu_companion_prefs"
    private const val KEY_DISCLAIMER_SHOWN = "disclaimer_shown"
    
    /**
     * Check if disclaimer should be shown on first launch
     */
    fun shouldShowDisclaimer(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false)
    }
    
    /**
     * Show the disclaimer dialog
     * @param context The activity context
     * @param backgroundResId Drawable resource ID for the background (e.g., R.drawable.cthulhu_background)
     * @param showCheckbox Whether to show the "Don't show again" checkbox
     */
    fun showDisclaimer(
        context: Context,
        backgroundResId: Int,
        showCheckbox: Boolean = true
    ) {
        // Create a scrollable TextView for the message to handle tablets better
        val scrollView = ScrollView(context)
        scrollView.setBackgroundResource(backgroundResId)
        
        // Create a FrameLayout wrapper to add the border
        val borderWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            4f, 
            context.resources.displayMetrics
        ).toInt()
        
        val borderWrapper = FrameLayout(context)
        borderWrapper.setBackgroundColor(0xFFFFFFFF.toInt()) // White border color
        
        // Add scrollView to borderWrapper with margin to create border effect
        val scrollParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        scrollParams.setMargins(borderWidth, borderWidth, borderWidth, borderWidth)
        scrollView.layoutParams = scrollParams
        borderWrapper.addView(scrollView)
        
        // Create a LinearLayout to hold both message and checkbox
        val contentLayout = LinearLayout(context)
        contentLayout.orientation = LinearLayout.VERTICAL
        
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            context.resources.displayMetrics
        ).toInt()
        
        // Add top padding to account for status bar
        val statusBarHeight = getStatusBarHeight(context)
        contentLayout.setPadding(0, statusBarHeight + padding, 0, 0)
        
        // Create back button container at the top
        val backButtonContainer = RelativeLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(padding, padding, padding, padding / 2)
        }
        
        val backButtonFrameLayout = FrameLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(padding, 0, 0, 0)
            }
        }
        
        val paddingPx = 5
        val iconSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            32f,
            context.resources.displayMetrics
        ).toInt()
        
        val backButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            contentDescription = "Back"
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            
            val totalSize = iconSize + (paddingPx * 2)
            layoutParams = FrameLayout.LayoutParams(totalSize, totalSize).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            
            setColorFilter(0xFFFFFFFF.toInt())
            isClickable = true
            isFocusable = true
        }
        
        backButtonFrameLayout.addView(backButton)
        backButtonContainer.addView(backButtonFrameLayout)
        contentLayout.addView(backButtonContainer)
        
        // Create title TextView - centered and large
        val titleView = TextView(context)
        titleView.text = "⚠️ CRITICAL INFORMATION - READ FIRST"
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        titleView.setTextColor(0xFFFFFFFF.toInt()) // White text
        titleView.gravity = Gravity.CENTER
        titleView.setPadding(padding, padding, padding, padding / 2)
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleView.layoutParams = titleParams
        contentLayout.addView(titleView)
        
        // Create message TextView
        val messageView = TextView(context)
        messageView.text = getDisclaimerText()
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        messageView.setTextColor(0xFFFFFFFF.toInt()) // White text
        messageView.setPadding(padding, padding / 2, padding, padding)
        val messageParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        messageView.layoutParams = messageParams
        contentLayout.addView(messageView)
        
        // Create checkbox to save preference (only if showCheckbox is true)
        var dontShowAgainCheckbox: CheckBox? = null
        if (showCheckbox) {
            dontShowAgainCheckbox = CheckBox(context)
            dontShowAgainCheckbox.text = "Don't show this again"
            dontShowAgainCheckbox.setTextColor(0xFFFFFFFF.toInt()) // White text
            dontShowAgainCheckbox.setPadding(padding, padding / 2, padding, padding)
            setCheckboxWhite(dontShowAgainCheckbox)
            
            // Check if the preference was previously saved
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false)
            dontShowAgainCheckbox.isChecked = disclaimerShown
        }
        
        if (dontShowAgainCheckbox != null) {
            val checkboxParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dontShowAgainCheckbox.layoutParams = checkboxParams
            contentLayout.addView(dontShowAgainCheckbox)
        }
        
        scrollView.addView(contentLayout)
        
        // Set maximum height for tablets (60% of screen height)
        val maxHeight = (context.resources.displayMetrics.heightPixels * 0.6).toInt()
        scrollParams.height = maxHeight
        scrollView.layoutParams = scrollParams
        
        val builder = AlertDialog.Builder(context)
        builder.setView(borderWrapper)
        val finalCheckbox = dontShowAgainCheckbox
        builder.setPositiveButton("I Understand") { dialog, _ ->
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Only save preference if checkbox exists and is checked
            if (finalCheckbox != null && finalCheckbox.isChecked) {
                prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, true).apply()
            } else if (finalCheckbox != null && !finalCheckbox.isChecked) {
                // If checkbox is unchecked, clear the preference so disclaimer shows again next time
                prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, false).apply()
            }
            dialog.dismiss()
        }
        builder.setCancelable(true) // Allow back button to dismiss
        
        val dialog = builder.create()
        dialog.show()
        
        // Set back button click listener after dialog is shown
        backButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Set dialog background to match app theme (dark/transparent)
        dialog.window?.let { window ->
            // Make dialog background transparent to show app background
            window.setBackgroundDrawableResource(android.R.color.transparent)
            
            // Ensure dialog doesn't take up entire screen on tablets
            val maxWidth = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            window.setLayout(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }
    
    /**
     * Get the disclaimer text (shared by both games)
     */
    private fun getDisclaimerText(): String {
        return """
            ⚠️ NOT AFFILIATED WITH FANTASY FLIGHT GAMES
            This app is NOT affiliated with Fantasy Flight Games in any way.
            Always refer to official Fantasy Flight Games materials.

            ⚠️ AGE LIMITATION - 14+
            This app is recommended for ages 14+ as per the game publisher.
            Content is based on H.P. Lovecraft Mythos (user discretion advised).

            APP PURPOSE:
            This app is designed to REPLACE physical cards to randomize cards and save table space.

            ⚠️ CRITICAL: In case of ANY doubt, the PHYSICAL CARDS are ALWAYS the truth.
            The app may contain bugs, errors, or inaccuracies.

            KEY INFORMATION:
            • This app is FREE and in PERPETUAL BETA STATUS
            • Cards are stored in a LOCAL DATABASE on your device only
            • The database cannot be changed and requires NO user information
            • The app does NOT save any user information
            • The app does NOT offer any type of communication
            • Works completely OFFLINE - no WiFi or network signal needed
            • Language: English ONLY (regardless of your country/location)
            • The app does NOT use your location

            IMPORTANT:
            ⚠️ You must ALWAYS refer to original physical game materials if there is any doubt.
            ⚠️ Any issues with the app do NOT prevent you from using the real physical cards.
            ⚠️ The developer assumes NO LIABILITY for any issues, errors, or consequences.
            ⚠️ You use this app at your own risk.

            By continuing, you acknowledge that you have read and agree to the full Privacy Policy & Terms of Service.
        """.trimIndent()
    }
    
    /**
     * Helper to style checkbox with white text and tint
     */
    private fun setCheckboxWhite(checkbox: CheckBox) {
        checkbox.setTextColor(android.graphics.Color.WHITE)
        CompoundButtonCompat.setButtonTintList(
            checkbox,
            android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        )
    }
    
    /**
     * Get status bar height
     */
    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        // If we can't get it from resources, use a default value (typically 24dp on modern devices)
        if (result == 0) {
            result = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                context.resources.displayMetrics
            ).toInt()
        }
        return result
    }
}


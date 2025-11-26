package com.poquets.cthulhu.GUI

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.shared.database.DatabaseInitializer
import com.poquets.cthulhu.shared.database.GameType
import com.poquets.cthulhu.shared.database.UnifiedCardDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameSelectionActivity : AppCompatActivity() {
    
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_selection)
        
        setupGameButtons()
        updateDatabaseStatus()
    }
    
    override fun onResume() {
        super.onResume()
        // Update status when returning to this screen (database may have been initialized)
        updateDatabaseStatus()
    }
    
    private fun setupGameButtons() {
        // Arkham Horror button
        findViewById<ImageButton>(R.id.arkhamButton).setOnClickListener {
            launchArkham()
        }
        
        // Eldritch Horror button
        findViewById<ImageButton>(R.id.eldritchButton).setOnClickListener {
            launchEldritch()
        }
        
        // Settings/gear button
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            openDatabaseManagement()
        }
    }
    
    private fun openDatabaseManagement() {
        val intent = android.content.Intent(this, DatabaseManagementActivity::class.java)
        startActivity(intent)
    }
    
    private fun updateDatabaseStatus() {
        activityScope.launch {
            try {
                val db = UnifiedCardDatabaseHelper.getInstance(this@GameSelectionActivity)
                val (arkhamCount, eldritchCount, total) = withContext(Dispatchers.IO) {
                    Triple(
                        db.getCardCount(GameType.ARKHAM),
                        db.getCardCount(GameType.ELDRITCH),
                        db.getCardCount()
                    )
                }
                
                val statusText = "Database: $total cards ($arkhamCount Arkham, $eldritchCount Eldritch)"
                findViewById<TextView>(R.id.databaseStatus)?.text = statusText
                
                android.util.Log.d("GameSelection", "Database status: $statusText")
            } catch (e: Exception) {
                android.util.Log.e("GameSelection", "Error getting database status: ${e.message}", e)
                findViewById<TextView>(R.id.databaseStatus)?.text = "Database: Error loading status"
            }
        }
    }
    
    private fun showGameStatus(gameType: GameType) {
        activityScope.launch {
            try {
                val db = UnifiedCardDatabaseHelper.getInstance(this@GameSelectionActivity)
                val count = withContext(Dispatchers.IO) {
                    db.getCardCount(gameType)
                }
                
                val gameName = when (gameType) {
                    GameType.ARKHAM -> "Arkham Horror"
                    GameType.ELDRITCH -> "Eldritch Horror"
                }
                
                Toast.makeText(
                    this@GameSelectionActivity,
                    "$gameName: $count cards in database",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GameSelectionActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun launchEldritch() {
        val intent = android.content.Intent(this, com.poquets.cthulhu.eldritch.GUI.Setup::class.java)
        startActivity(intent)
    }
    
    private fun launchArkham() {
        val intent = android.content.Intent(this, com.poquets.cthulhu.arkham.GUI.ExpansionSelector::class.java)
        startActivity(intent)
    }
}


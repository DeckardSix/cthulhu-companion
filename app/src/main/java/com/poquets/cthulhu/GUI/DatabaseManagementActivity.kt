package com.poquets.cthulhu.GUI

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.shared.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Data(
    val arkham: Int,
    val eldritch: Int,
    val total: Int,
    val arkhamExps: List<String>,
    val eldritchExps: List<String>
)

class DatabaseManagementActivity : AppCompatActivity() {
    
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_management)
        
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        
        setupButtons()
        refreshStatus()
    }
    
    private fun setupButtons() {
        findViewById<Button>(R.id.btnRefreshStatus).setOnClickListener {
            refreshStatus()
        }
        
        findViewById<Button>(R.id.btnImportArkham).setOnClickListener {
            importArkhamCards()
        }
        
        findViewById<Button>(R.id.btnImportEldritch).setOnClickListener {
            importEldritchCards()
        }
        
        findViewById<Button>(R.id.btnImportLocations).setOnClickListener {
            importLocations()
        }
        
        findViewById<Button>(R.id.btnCheckHealth).setOnClickListener {
            checkDatabaseHealth()
        }
        
        findViewById<Button>(R.id.btnClearDatabase).setOnClickListener {
            showClearDatabaseDialog()
        }
    }
    
    private fun refreshStatus() {
        activityScope.launch {
            try {
                progressBar.visibility = ProgressBar.VISIBLE
                statusText.text = "Loading..."
                
                val db = UnifiedCardDatabaseHelper.getInstance(this@DatabaseManagementActivity)
                val data = withContext(Dispatchers.IO) {
                    val arkham = db.getCardCount(GameType.ARKHAM)
                    val eldritch = db.getCardCount(GameType.ELDRITCH)
                    val totalCount = db.getCardCount()
                    val arkhamExps = db.getExpansionNamesForGameType(GameType.ARKHAM)
                    val eldritchExps = db.getExpansionNamesForGameType(GameType.ELDRITCH)
                    Data(arkham, eldritch, totalCount, arkhamExps, eldritchExps)
                }
                val arkhamCount = data.arkham
                val eldritchCount = data.eldritch
                val total = data.total
                val arkhamExpansions = data.arkhamExps
                val eldritchExpansions = data.eldritchExps
                
                val status = buildString {
                    appendLine("Database Status:")
                    appendLine("Total Cards: $total")
                    appendLine("Arkham Cards: $arkhamCount")
                    appendLine("Eldritch Cards: $eldritchCount")
                    appendLine()
                    appendLine("Arkham Expansions: ${arkhamExpansions.size}")
                    arkhamExpansions.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Eldritch Expansions: ${eldritchExpansions.size}")
                    eldritchExpansions.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Database Path: ${db.getDatabasePath()}")
                }
                
                statusText.text = status
                android.util.Log.d("DatabaseManagement", status)
            } catch (e: Exception) {
                android.util.Log.e("DatabaseManagement", "Error refreshing status: ${e.message}", e)
                statusText.text = "Error: ${e.message}"
            } finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }
    
    private fun importArkhamCards() {
        AlertDialog.Builder(this)
            .setTitle("Import Arkham Cards")
            .setMessage("This will import Arkham Horror cards into the database. This may take a few moments.")
            .setPositiveButton("Import") { _, _ ->
                activityScope.launch {
                    try {
                        progressBar.visibility = ProgressBar.VISIBLE
                        statusText.text = "Importing Arkham cards..."
                        
                        val arkhamCount = withContext(Dispatchers.IO) {
                            DatabaseInitializer.importArkhamCards(
                                this@DatabaseManagementActivity,
                                forceReinit = true  // Force reimport to ensure clean import
                            )
                        }
                        
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Imported $arkhamCount Arkham cards",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        refreshStatus()
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseManagement", "Error importing Arkham cards: ${e.message}", e)
                        e.printStackTrace()
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        statusText.text = "Error: ${e.message}\n\nCheck logcat for details."
                    } finally {
                        progressBar.visibility = ProgressBar.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun importEldritchCards() {
        AlertDialog.Builder(this)
            .setTitle("Import Eldritch Cards")
            .setMessage("This will import Eldritch Horror cards into the database. This may take a few moments.")
            .setPositiveButton("Import") { _, _ ->
                activityScope.launch {
                    try {
                        progressBar.visibility = ProgressBar.VISIBLE
                        statusText.text = "Importing Eldritch cards..."
                        
                        val eldritchCount = withContext(Dispatchers.IO) {
                            DatabaseInitializer.importEldritchCards(
                                this@DatabaseManagementActivity,
                                forceReinit = false
                            )
                        }
                        
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Imported $eldritchCount Eldritch cards",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        refreshStatus()
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseManagement", "Error importing Eldritch cards: ${e.message}", e)
                        e.printStackTrace()
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        statusText.text = "Error: ${e.message}\n\nCheck logcat for details."
                    } finally {
                        progressBar.visibility = ProgressBar.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun importLocations() {
        AlertDialog.Builder(this)
            .setTitle("Import Locations")
            .setMessage("This will import locations for both games. This may take a few moments.")
            .setPositiveButton("Import") { _, _ ->
                activityScope.launch {
                    try {
                        progressBar.visibility = ProgressBar.VISIBLE
                        statusText.text = "Importing locations..."
                        
                        // Import both games to get locations
                        withContext(Dispatchers.IO) {
                            DatabaseInitializer.initializeDatabase(
                                this@DatabaseManagementActivity,
                                forceReinit = false
                            )
                        }
                        
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Locations imported",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        refreshStatus()
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseManagement", "Error importing locations: ${e.message}", e)
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        statusText.text = "Error: ${e.message}"
                    } finally {
                        progressBar.visibility = ProgressBar.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun checkDatabaseHealth() {
        activityScope.launch {
            try {
                progressBar.visibility = ProgressBar.VISIBLE
                statusText.text = "Checking database health..."
                
                val health = withContext(Dispatchers.IO) {
                    val db = UnifiedCardDatabaseHelper.getInstance(this@DatabaseManagementActivity)
                    val arkhamCount = db.getCardCount(GameType.ARKHAM)
                    val eldritchCount = db.getCardCount(GameType.ELDRITCH)
                    val arkhamExpansions = db.getExpansionNamesForGameType(GameType.ARKHAM)
                    val eldritchExpansions = db.getExpansionNamesForGameType(GameType.ELDRITCH)
                    
                    val issues = mutableListOf<String>()
                    
                    if (arkhamCount == 0) {
                        issues.add("No Arkham cards found")
                    }
                    if (eldritchCount == 0) {
                        issues.add("No Eldritch cards found")
                    }
                    if (arkhamExpansions.isEmpty()) {
                        issues.add("No Arkham expansions found")
                    }
                    if (eldritchExpansions.isEmpty()) {
                        issues.add("No Eldritch expansions found")
                    }
                    
                    val healthStatus = if (issues.isEmpty()) {
                        "Database is healthy!\n\n" +
                        "Arkham: $arkhamCount cards, ${arkhamExpansions.size} expansions\n" +
                        "Eldritch: $eldritchCount cards, ${eldritchExpansions.size} expansions"
                    } else {
                        "Database Health Issues:\n\n" + issues.joinToString("\n") + "\n\n" +
                        "Use the import buttons to fix these issues."
                    }
                    
                    healthStatus
                }
                
                AlertDialog.Builder(this@DatabaseManagementActivity)
                    .setTitle("Database Health Check")
                    .setMessage(health)
                    .setPositiveButton("OK", null)
                    .show()
                
                statusText.text = health
            } catch (e: Exception) {
                android.util.Log.e("DatabaseManagement", "Error checking health: ${e.message}", e)
                Toast.makeText(
                    this@DatabaseManagementActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                statusText.text = "Error: ${e.message}"
            } finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }
    
    private fun showClearDatabaseDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Database")
            .setMessage("WARNING: This will delete all cards from the database. This cannot be undone!")
            .setPositiveButton("Clear") { _, _ ->
                activityScope.launch {
                    try {
                        progressBar.visibility = ProgressBar.VISIBLE
                        statusText.text = "Clearing database..."
                        
                        withContext(Dispatchers.IO) {
                            val db = UnifiedCardDatabaseHelper.getInstance(this@DatabaseManagementActivity)
                            db.clearAllCards()
                        }
                        
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Database cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        refreshStatus()
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseManagement", "Error clearing database: ${e.message}", e)
                        Toast.makeText(
                            this@DatabaseManagementActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        statusText.text = "Error: ${e.message}"
                    } finally {
                        progressBar.visibility = ProgressBar.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

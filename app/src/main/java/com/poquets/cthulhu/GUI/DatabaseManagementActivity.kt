package com.poquets.cthulhu.GUI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.poquets.cthulhu.R
import com.poquets.cthulhu.shared.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    
    // Activity result launchers for file picker
    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri ->
        uri?.let { exportDatabaseToUri(it) }
    }
    
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importDatabaseFromUri(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_management)
        
        // Add padding at top to account for status bar (like the card screen does with Toolbar)
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
        val statusBarHeight = getStatusBarHeight()
        rootLayout.setPadding(0, statusBarHeight, 0, 0)
        
        // Add back button positioned like question mark in expansion selector
        addBackButton()
        
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
        
        findViewById<Button>(R.id.btnExportDatabase).setOnClickListener {
            exportDatabase()
        }
        
        findViewById<Button>(R.id.btnImportDatabase).setOnClickListener {
            importDatabase()
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
                                forceReinit = true  // Force reimport to ensure SPECIAL cards are parsed correctly
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
    
    /**
     * Export database to a file chosen by the user
     */
    private fun exportDatabase() {
        try {
            val db = UnifiedCardDatabaseHelper.getInstance(this)
            val dbFile = File(db.getDatabasePath())
            
            if (!dbFile.exists()) {
                Toast.makeText(this, "Database file does not exist", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create a timestamped filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "cthulhu_companion_$timestamp.db"
            
            // Launch file picker to save the database
            exportFileLauncher.launch(filename)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManagement", "Error preparing export: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Export database to the selected URI
     */
    private fun exportDatabaseToUri(uri: Uri) {
        activityScope.launch {
            try {
                progressBar.visibility = ProgressBar.VISIBLE
                statusText.text = "Exporting database..."
                
                val result = withContext(Dispatchers.IO) {
                    val db = UnifiedCardDatabaseHelper.getInstance(this@DatabaseManagementActivity)
                    val sourceFile = File(db.getDatabasePath())
                    
                    if (!sourceFile.exists()) {
                        return@withContext "Database file does not exist"
                    }
                    
                    try {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            FileInputStream(sourceFile).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        
                        val fileSize = sourceFile.length()
                        val sizeMB = String.format(Locale.US, "%.2f", fileSize / (1024.0 * 1024.0))
                        "Database exported successfully!\n\nSize: $sizeMB MB\n\nFile saved to your selected location."
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseManagement", "Error writing to URI: ${e.message}", e)
                        "Error writing file: ${e.message}"
                    }
                }
                
                Toast.makeText(this@DatabaseManagementActivity, result, Toast.LENGTH_LONG).show()
                statusText.text = result
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseManagement", "Error exporting database: ${e.message}", e)
                Toast.makeText(this@DatabaseManagementActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                statusText.text = "Error: ${e.message}"
            } finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }
    
    /**
     * Import database from a file chosen by the user
     */
    private fun importDatabase() {
        AlertDialog.Builder(this)
            .setTitle("Import Database")
            .setMessage("WARNING: This will replace your current database with the imported file. This cannot be undone!\n\nMake sure you have exported your current database first if you want to keep it.")
            .setPositiveButton("Import") { _, _ ->
                // Launch file picker to select database file
                importFileLauncher.launch("*/*")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Import database from the selected URI
     */
    private fun importDatabaseFromUri(uri: Uri) {
        activityScope.launch {
            try {
                progressBar.visibility = ProgressBar.VISIBLE
                statusText.text = "Importing database..."
                
                val result = withContext(Dispatchers.IO) {
                    val db = UnifiedCardDatabaseHelper.getInstance(this@DatabaseManagementActivity)
                    val dbPath = db.getDatabasePath()
                    val dbFile = File(dbPath)
                    
                    try {
                        // Get a readable database to ensure connection is open, then close it
                        val readableDb = db.readableDatabase
                        readableDb.close()
                        
                        // Also close any writable database connections
                        try {
                            val writableDb = db.writableDatabase
                            writableDb.close()
                        } catch (e: Exception) {
                            // Ignore if already closed
                        }
                        
                        // Wait a moment to ensure all connections are closed
                        Thread.sleep(100)
                        
                        // Copy the imported file to the database location
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            // Create backup of current database first
                            if (dbFile.exists()) {
                                val backupFile = File("${dbPath}.backup")
                                if (backupFile.exists()) {
                                    backupFile.delete()
                                }
                                dbFile.copyTo(backupFile)
                            }
                            
                            // Write imported database
                            FileOutputStream(dbFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: return@withContext "Error: Could not open selected file"
                        
                        // Force singleton to reset by clearing instance
                        // Note: This is a workaround since we can't directly reset the singleton
                        // The next getInstance() call will create a new instance
                        
                        // Get a new database instance to verify
                        // We need to use reflection or just verify the file exists
                        val importedFile = File(dbPath)
                        if (!importedFile.exists()) {
                            return@withContext "Error: Imported file was not created"
                        }
                        
                        val fileSize = importedFile.length()
                        val sizeMB = String.format(Locale.US, "%.2f", fileSize / (1024.0 * 1024.0))
                        
                        "Database imported successfully!\n\n" +
                        "File size: $sizeMB MB\n\n" +
                        "Please restart the app to use the imported database.\n\n" +
                        "Note: A backup of your previous database was saved as ${dbPath}.backup"
                        
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseManagement", "Error importing database: ${e.message}", e)
                        // Try to restore backup if import failed
                        try {
                            val backupFile = File("${dbPath}.backup")
                            if (backupFile.exists() && !dbFile.exists()) {
                                backupFile.copyTo(dbFile)
                            }
                        } catch (ex: Exception) {
                            android.util.Log.e("DatabaseManagement", "Error restoring backup: ${ex.message}", ex)
                        }
                        "Error importing database: ${e.message}\n\nCheck logcat for details."
                    }
                }
                
                Toast.makeText(this@DatabaseManagementActivity, result, Toast.LENGTH_LONG).show()
                statusText.text = result
                
                // Refresh status after a short delay to allow database to be ready
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    refreshStatus()
                }, 1000)
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseManagement", "Error importing database: ${e.message}", e)
                Toast.makeText(this@DatabaseManagementActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                statusText.text = "Error: ${e.message}"
            } finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }
}

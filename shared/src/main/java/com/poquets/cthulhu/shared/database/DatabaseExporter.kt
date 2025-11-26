package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting and backing up the unified database
 */
object DatabaseExporter {
    private const val TAG = "DatabaseExporter"
    
    /**
     * Export database to external storage (for backup/sharing)
     * @return Path to exported file, or null if failed
     */
    fun exportToExternalStorage(context: Context): String? {
        return try {
            val db = UnifiedCardDatabaseHelper.getInstance(context)
            val sourcePath = db.getDatabasePath()
            val sourceFile = File(sourcePath)
            
            if (!sourceFile.exists()) {
                Log.e(TAG, "Database file does not exist: $sourcePath")
                return null
            }
            
            // Create export directory
            val exportDir = File(context.getExternalFilesDir(null), "database_exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Create timestamped filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportFile = File(exportDir, "cthulhu_companion_$timestamp.db")
            
            // Copy database file
            copyFile(sourceFile, exportFile)
            
            Log.d(TAG, "Database exported to: ${exportFile.absolutePath}")
            exportFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting database: ${e.message}", e)
            null
        }
    }
    
    /**
     * Export database to a specific location
     */
    fun exportToPath(context: Context, destinationPath: String): Boolean {
        return try {
            val db = UnifiedCardDatabaseHelper.getInstance(context)
            val sourcePath = db.getDatabasePath()
            val sourceFile = File(sourcePath)
            
            if (!sourceFile.exists()) {
                Log.e(TAG, "Database file does not exist: $sourcePath")
                return false
            }
            
            val destinationFile = File(destinationPath)
            destinationFile.parentFile?.mkdirs()
            
            copyFile(sourceFile, destinationFile)
            
            Log.d(TAG, "Database exported to: $destinationPath")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting database: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get database file size in human-readable format
     */
    fun getDatabaseSize(context: Context): String {
        return try {
            val db = UnifiedCardDatabaseHelper.getInstance(context)
            val dbFile = File(db.getDatabasePath())
            
            if (!dbFile.exists()) {
                return "0 B"
            }
            
            val sizeBytes = dbFile.length()
            formatFileSize(sizeBytes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting database size: ${e.message}", e)
            "Unknown"
        }
    }
    
    /**
     * Copy file from source to destination
     */
    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
    
    /**
     * Format file size in human-readable format
     */
    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.2f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }
    
    /**
     * Get list of exported database files
     */
    fun getExportedFiles(context: Context): List<File> {
        return try {
            val exportDir = File(context.getExternalFilesDir(null), "database_exports")
            if (!exportDir.exists()) {
                return emptyList()
            }
            
            exportDir.listFiles { _, name ->
                name.endsWith(".db")
            }?.toList() ?: emptyList()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting exported files: ${e.message}", e)
            emptyList()
        }
    }
}


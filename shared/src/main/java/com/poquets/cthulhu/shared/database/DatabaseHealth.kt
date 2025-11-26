package com.poquets.cthulhu.shared.database

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Database health and diagnostics utility
 */
object DatabaseHealth {
    private const val TAG = "DatabaseHealth"
    
    /**
     * Perform comprehensive health check
     */
    fun performHealthCheck(context: Context): HealthReport {
        val db = UnifiedCardDatabaseHelper.getInstance(context)
        val dbPath = db.getDatabasePath()
        val dbFile = File(dbPath)
        
        val report = HealthReport()
        
        // Check if database file exists
        report.databaseExists = dbFile.exists()
        if (!report.databaseExists) {
            report.issues.add("Database file does not exist")
            return report
        }
        
        // Check file size
        report.databaseSize = dbFile.length()
        if (report.databaseSize == 0L) {
            report.issues.add("Database file is empty")
        }
        
        // Check if readable
        report.databaseReadable = dbFile.canRead()
        if (!report.databaseReadable) {
            report.issues.add("Database file is not readable")
        }
        
        // Check card counts
        try {
            report.totalCards = db.getCardCount()
            report.arkhamCards = db.getCardCount(GameType.ARKHAM)
            report.eldritchCards = db.getCardCount(GameType.ELDRITCH)
            
            // Verify counts match
            if (report.totalCards != report.arkhamCards + report.eldritchCards) {
                report.issues.add("Card count mismatch: total != arkham + eldritch")
            }
            
            // Check if we have cards
            if (report.totalCards == 0) {
                report.issues.add("Database contains no cards")
            }
            
            // Check if we have both game types
            if (report.arkhamCards == 0) {
                report.warnings.add("No Arkham cards found")
            }
            if (report.eldritchCards == 0) {
                report.warnings.add("No Eldritch cards found")
            }
            
        } catch (e: Exception) {
            report.issues.add("Error checking card counts: ${e.message}")
            Log.e(TAG, "Error in health check: ${e.message}", e)
        }
        
        // Verify integrity
        try {
            report.integrityCheck = DatabaseUtils.verifyDatabase(context)
            if (!report.integrityCheck) {
                report.issues.add("Database integrity check failed")
            }
        } catch (e: Exception) {
            report.issues.add("Error verifying integrity: ${e.message}")
        }
        
        // Determine overall health
        report.isHealthy = report.issues.isEmpty() && report.databaseExists && 
                          report.databaseReadable && report.totalCards > 0
        
        return report
    }
    
    /**
     * Health report data class
     */
    data class HealthReport(
        var databaseExists: Boolean = false,
        var databaseReadable: Boolean = false,
        var databaseSize: Long = 0,
        var totalCards: Int = 0,
        var arkhamCards: Int = 0,
        var eldritchCards: Int = 0,
        var integrityCheck: Boolean = false,
        var isHealthy: Boolean = false,
        val issues: MutableList<String> = mutableListOf(),
        val warnings: MutableList<String> = mutableListOf()
    ) {
        fun getSummary(): String {
            return buildString {
                appendLine("Database Health Report")
                appendLine("=====================")
                appendLine("Status: ${if (isHealthy) "HEALTHY" else "ISSUES DETECTED"}")
                appendLine()
                appendLine("Database File:")
                appendLine("  Exists: $databaseExists")
                appendLine("  Readable: $databaseReadable")
                appendLine("  Size: ${formatFileSize(databaseSize)}")
                appendLine()
                appendLine("Card Counts:")
                appendLine("  Total: $totalCards")
                appendLine("  Arkham: $arkhamCards")
                appendLine("  Eldritch: $eldritchCards")
                appendLine()
                appendLine("Integrity: ${if (integrityCheck) "PASSED" else "FAILED"}")
                appendLine()
                
                if (issues.isNotEmpty()) {
                    appendLine("Issues:")
                    issues.forEach { appendLine("  - $it") }
                    appendLine()
                }
                
                if (warnings.isNotEmpty()) {
                    appendLine("Warnings:")
                    warnings.forEach { appendLine("  - $it") }
                }
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format("%.2f KB", kb)
            val mb = kb / 1024.0
            return String.format("%.2f MB", mb)
        }
    }
}


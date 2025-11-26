package com.poquets.cthulhu.shared.database

/**
 * Data class to hold migration statistics
 */
data class MigrationStats(
    val arkhamCount: Int = 0,
    val eldritchCount: Int = 0,
    val totalCount: Int = 0,
    val arkhamExpansions: List<String> = emptyList(),
    val eldritchExpansions: List<String> = emptyList(),
    val eldritchRegions: List<String> = emptyList(),
    val success: Boolean = false,
    val errorMessage: String? = null
) {
    fun getSummary(): String {
        return buildString {
            appendLine("Migration Statistics:")
            appendLine("Total Cards: $totalCount")
            appendLine("Arkham Cards: $arkhamCount")
            appendLine("Eldritch Cards: $eldritchCount")
            appendLine()
            appendLine("Arkham Expansions: ${arkhamExpansions.size}")
            arkhamExpansions.forEach { appendLine("  - $it") }
            appendLine()
            appendLine("Eldritch Expansions: ${eldritchExpansions.size}")
            eldritchExpansions.forEach { appendLine("  - $it") }
            appendLine()
            appendLine("Eldritch Regions: ${eldritchRegions.size}")
            eldritchRegions.forEach { appendLine("  - $it") }
            if (errorMessage != null) {
                appendLine()
                appendLine("Error: $errorMessage")
            }
        }
    }
}


# Shared Module Usage Guide

## Database Operations

### Initialize Database

The database is automatically initialized when the app starts (in `CthulhuApplication`). You can also manually initialize it:

```kotlin
import com.poquets.cthulhu.shared.database.DatabaseInitializer

// Initialize and migrate cards
val (arkhamCount, eldritchCount) = DatabaseInitializer.initializeDatabase(context)

// Force re-initialization (clears existing cards first)
val (arkhamCount, eldritchCount) = DatabaseInitializer.initializeDatabase(context, forceReinit = true)
```

### Access Database

```kotlin
import com.poquets.cthulhu.shared.database.UnifiedCardDatabaseHelper
import com.poquets.cthulhu.shared.database.GameType

val db = UnifiedCardDatabaseHelper.getInstance(context)

// Check if cards exist
val hasCards = db.hasCards()
val hasArkham = db.hasCards(GameType.ARKHAM)
val hasEldritch = db.hasCards(GameType.ELDRITCH)

// Get card counts
val total = db.getCardCount()
val arkhamCount = db.getCardCount(GameType.ARKHAM)
val eldritchCount = db.getCardCount(GameType.ELDRITCH)
```

### Query Cards

```kotlin
// Get all cards for a game type
val arkhamCards = db.getCardsByGameType(GameType.ARKHAM)
val eldritchCards = db.getCardsByGameType(GameType.ELDRITCH)

// Get cards by expansion
val baseCards = db.getCardsByGameTypeAndExpansion(GameType.ELDRITCH, "BASE")

// Get cards by region (Eldritch only)
val americasCards = db.getCardsByGameTypeAndRegion(GameType.ELDRITCH, "AMERICAS")
```

### Insert Cards

```kotlin
import com.poquets.cthulhu.shared.database.UnifiedCard

// Insert single card
val card = UnifiedCard(
    gameType = GameType.ELDRITCH,
    cardId = "A1",
    expansion = "BASE",
    region = "AMERICAS",
    topHeader = "New York",
    topEncounter = "You encounter..."
)
db.insertCard(card)

// Insert multiple cards (batch, more efficient)
val cards = listOf(card1, card2, card3)
val inserted = db.insertCards(cards)
```

### Utility Functions

```kotlin
import com.poquets.cthulhu.shared.database.DatabaseUtils

// Get database summary
val summary = DatabaseUtils.getCardSummary(context)
// Returns: {total, arkham, eldritch, has_arkham, has_eldritch, database_path}

// Get expansions for a game
val arkhamExpansions = DatabaseUtils.getExpansionsForGameType(context, GameType.ARKHAM)
val eldritchExpansions = DatabaseUtils.getExpansionsForGameType(context, GameType.ELDRITCH)

// Get regions (Eldritch only)
val regions = DatabaseUtils.getRegionsForEldritch(context)

// Verify database integrity
val isValid = DatabaseUtils.verifyDatabase(context)
```

## Disclaimer Helper

```kotlin
import com.poquets.cthulhu.shared.ui.DisclaimerHelper

// Check if disclaimer should be shown
if (DisclaimerHelper.shouldShowDisclaimer(this)) {
    DisclaimerHelper.showDisclaimer(
        context = this,
        backgroundResId = R.drawable.your_background,
        showCheckbox = true
    )
}

// Show disclaimer manually (e.g., from "?" icon)
DisclaimerHelper.showDisclaimer(
    context = this,
    backgroundResId = R.drawable.your_background,
    showCheckbox = true
)
```

## Query Builder

For complex queries, use the QueryBuilder:

```kotlin
import com.poquets.cthulhu.shared.database.queryCards

// Build complex query
val cards = queryCards(GameType.ELDRITCH)
    .withExpansion("BASE")
    .withRegion("AMERICAS")
    .onlyUnencountered()
    .limit(10)
    .orderBy("card_id ASC")
    .execute(context)
```

## Database Export

```kotlin
import com.poquets.cthulhu.shared.database.DatabaseExporter

// Export to external storage
val exportPath = DatabaseExporter.exportToExternalStorage(context)

// Export to specific path
val success = DatabaseExporter.exportToPath(context, "/path/to/backup.db")

// Get database size
val size = DatabaseExporter.getDatabaseSize(context)

// List exported files
val exports = DatabaseExporter.getExportedFiles(context)
```

## Database Health Check

```kotlin
import com.poquets.cthulhu.shared.database.DatabaseHealth

// Perform comprehensive health check
val report = DatabaseHealth.performHealthCheck(context)

if (report.isHealthy) {
    println("Database is healthy!")
} else {
    println("Issues found:")
    report.issues.forEach { println("  - $it") }
}

// Get detailed report
val summary = report.getSummary()
```

## Thread Safety

All database operations are thread-safe:
- Read operations use read locks (can happen concurrently)
- Write operations use write locks (exclusive)
- Use coroutines for background operations:

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// In a coroutine
val cards = withContext(Dispatchers.IO) {
    db.getCardsByGameType(GameType.ARKHAM)
}
```

## Database Location

The database is stored persistently at:
```
/data/data/com.poquets.cthulhu/databases/cthulhu_companion.db
```

This location ensures:
- ✅ Data persists across app restarts
- ✅ Data is private to the app
- ✅ Data survives app updates
- ✅ Data is backed up with the app (if backup is enabled)


# Unified Database Documentation

## Overview

The unified database stores cards from both Arkham Horror and Eldritch Horror games in a single persistent SQLite database. Cards are separated by a `game_type` field that distinguishes between "ARKHAM" and "ELDRITCH".

## Database Structure

### Table: `unified_cards`

The main table that stores all cards from both games.

**Columns:**
- `_id` (INTEGER PRIMARY KEY) - Auto-incrementing primary key
- `game_type` (TEXT NOT NULL) - Either "ARKHAM" or "ELDRITCH"
- `card_id` (TEXT NOT NULL) - Card identifier (integer as string for Arkham, text ID for Eldritch)
- `expansion` (TEXT NOT NULL) - Expansion name (default: "BASE")
- `card_name` (TEXT) - Optional card name
- `encountered` (TEXT) - Encounter status (default: "NONE")
- `card_data` (TEXT) - JSON or additional data

**Arkham-specific fields (nullable):**
- `neighborhood_id` (INTEGER) - Reference to neighborhood
- `location_id` (INTEGER) - Reference to location
- `encounter_id` (INTEGER) - Reference to encounter

**Eldritch-specific fields (nullable):**
- `region` (TEXT) - Card region/deck name
- `top_header` (TEXT) - Top section header
- `top_encounter` (TEXT) - Top section encounter text
- `middle_header` (TEXT) - Middle section header
- `middle_encounter` (TEXT) - Middle section encounter text
- `bottom_header` (TEXT) - Bottom section header
- `bottom_encounter` (TEXT) - Bottom section encounter text

**Unique Constraint:** `(game_type, card_id, expansion)`

**Indexes:**
- `idx_game_type` on `game_type`
- `idx_expansion` on `expansion`
- `idx_region` on `region`

### Table: `expansions`

Stores expansion information for both games.

**Columns:**
- `exp_id` (INTEGER PRIMARY KEY) - Auto-incrementing primary key
- `game_type` (TEXT NOT NULL) - Either "ARKHAM" or "ELDRITCH"
- `exp_name` (TEXT NOT NULL) - Expansion name
- `exp_icon_path` (TEXT) - Path to expansion icon

**Unique Constraint:** `(game_type, exp_name)`

## Database Location

The database is stored persistently in the app's private database directory:
```
/data/data/com.poquets.cthulhu/databases/cthulhu_companion.db
```

This location ensures:
- ✅ Data persists across app restarts
- ✅ Data is private to the app
- ✅ Data survives app updates
- ✅ Data is backed up with the app (if backup is enabled)

## Usage

### Initialize Database

```kotlin
import com.poquets.cthulhu.shared.database.DatabaseInitializer

// Initialize and migrate cards from both games
val (arkhamCount, eldritchCount) = DatabaseInitializer.initializeDatabase(context)

// Force re-initialization (clears existing cards)
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

// Get database path
val dbPath = db.getDatabasePath()
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

// Insert multiple cards (batch)
val cards = listOf(card1, card2, card3)
val inserted = db.insertCards(cards)
```

### Migration

The database automatically migrates cards from existing Arkham and Eldritch databases when initialized. Migration looks for databases at:
- Arkham: `/data/data/com.poquets.arkham/databases/ahDB`
- Eldritch: `/data/data/com.poquets.eldritch/databases/eldritch_cards.db`

## Thread Safety

The database helper uses `ReentrantReadWriteLock` to ensure thread-safe operations:
- Read operations (queries) use read locks
- Write operations (inserts, deletes) use write locks
- Multiple reads can happen concurrently
- Writes are exclusive

## Notes

- The database is created automatically on first access
- Cards from both games coexist in the same table, separated by `game_type`
- The database persists across app restarts and updates
- Migration is idempotent - running it multiple times won't duplicate cards (due to unique constraint)


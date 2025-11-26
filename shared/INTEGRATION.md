# Integration Guide

This document explains how to integrate the unified database system with existing Arkham and Eldritch game code.

## Overview

The unified database system provides adapters and managers that bridge the gap between the existing game code and the new unified database. This allows you to gradually migrate game logic while maintaining compatibility.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Game Activities                           │
│  (Arkham/Eldritch UI Components)                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  Game Adapters                               │
│  • EldritchDeckAdapter                                       │
│  • ArkhamDeckAdapter                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  Core Managers                               │
│  • DeckManager (handles deck operations)                     │
│  • GameStateManager (handles game state)                     │
│  • CardRepository (async card operations)                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              UnifiedCardDatabaseHelper                       │
│              (SQLite Database)                               │
└─────────────────────────────────────────────────────────────┘
```

## Integration Steps

### Step 1: Initialize the Database

The database is automatically initialized when the app starts (in `CthulhuApplication`). However, you can manually trigger initialization:

```kotlin
val initializer = DatabaseInitializer(context)
val stats = initializer.initializeDatabase()
```

### Step 2: Use Game Adapters

#### For Eldritch Games

Replace the old `Decks` class usage with `EldritchDeckAdapter`:

```kotlin
// Old way (Java)
Decks decks = new Decks(context);
decks.shuffleDeck("AMERICAS");
Card card = decks.drawCard("AMERICAS");

// New way (Kotlin)
val adapter = EldritchDeckAdapter(context)
lifecycleScope.launch {
    adapter.initialize(expansions)
    adapter.shuffleDeck("AMERICAS")
    val deck = adapter.getDeck("AMERICAS")
    // Use deck...
}
```

#### For Arkham Games

Replace `GameState` and `AHFlyweightFactory` usage with `ArkhamDeckAdapter`:

```kotlin
// Old way (Java)
GameState state = GameState.getInstance();
ArrayList<NeighborhoodCard> cards = state.getDeckByNeighborhood(neiID);

// New way (Kotlin)
val adapter = ArkhamDeckAdapter(context)
lifecycleScope.launch {
    adapter.initialize(expansions)
    val cards = adapter.getNeighborhoodCards(neiID)
    // Use cards...
}
```

### Step 3: Manage Game State

Use `GameStateManager` to track the current game:

```kotlin
val gameState = GameStateManager(context)

// Set current game
gameState.setCurrentGame(GameType.ELDRITCH)

// Set selected expansions
gameState.setSelectedExpansions(GameType.ELDRITCH, setOf("BASE", "FORSAKEN_LORE"))

// Create new game
val gameId = gameState.newGame(GameType.ARKHAM)
```

### Step 4: Query Cards Directly

For advanced queries, use `CardRepository`:

```kotlin
val repository = CardRepository(context)

lifecycleScope.launch {
    // Get all unencountered cards
    val cards = repository.getUnencounteredCards(GameType.ELDRITCH)
    
    // Get cards by expansion
    val baseCards = repository.getCardsByExpansion(GameType.ARKHAM, "BASE")
    
    // Get cards by region
    val americasCards = repository.getCardsByRegion(GameType.ELDRITCH, "AMERICAS")
    
    // Update card status
    repository.updateEncounteredStatus(
        GameType.ELDRITCH,
        cardId = "123",
        expansion = "BASE",
        encountered = "TOP"
    )
}
```

### Step 5: Use QueryBuilder for Complex Queries

For complex filtering, use `QueryBuilder`:

```kotlin
val cards = queryCards(GameType.ELDRITCH)
    .withExpansion("BASE")
    .withRegion("AMERICAS")
    .onlyUnencountered()
    .limit(10)
    .execute(context)
```

## Migration Strategy

### Phase 1: Parallel Operation
- Keep existing database code running
- Add unified database initialization
- Test migration with existing data

### Phase 2: Gradual Replacement
- Replace deck operations with adapters
- Keep old code as fallback
- Test thoroughly

### Phase 3: Full Migration
- Remove old database code
- Use only unified database
- Clean up unused code

## Compatibility Notes

### Eldritch Compatibility

The `EldritchDeckAdapter` provides methods compatible with the old `Decks` class:

- `getDeck(region)` - Returns cards for a region
- `shuffleDeck(region)` - Shuffles a deck
- `discardCard(region, cardId, encountered)` - Discards a card
- `getExpeditionLocation()` - Gets expedition location
- `containsDeck(region)` - Checks if deck exists

### Arkham Compatibility

The `ArkhamDeckAdapter` provides methods compatible with `GameState`:

- `getNeighborhoodCards(neighborhoodId)` - Returns neighborhood cards
- `getOtherWorldCards()` - Returns other world cards
- `shuffleNeighborhood(neighborhoodId)` - Shuffles neighborhood deck
- `drawNeighborhoodCard(neighborhoodId)` - Draws a card
- `applyExpansion(expansionName, enabled)` - Enables/disables expansion

## Example: Complete Game Initialization

```kotlin
class GameActivity : AppCompatActivity() {
    private lateinit var deckAdapter: EldritchDeckAdapter
    private lateinit var gameState: GameStateManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        gameState = GameStateManager(this)
        deckAdapter = EldritchDeckAdapter(this)
        
        // Set game type
        gameState.setCurrentGame(GameType.ELDRITCH)
        
        // Set expansions
        val expansions = setOf("BASE", "FORSAKEN_LORE")
        gameState.setSelectedExpansions(GameType.ELDRITCH, expansions)
        
        // Initialize decks
        lifecycleScope.launch {
            deckAdapter.initialize(expansions.toList())
            
            // Now ready to use
            val americasDeck = deckAdapter.getDeck("AMERICAS")
            // Display cards...
        }
    }
}
```

## Troubleshooting

### Cards Not Loading

1. Check database initialization:
   ```kotlin
   val hasCards = CardRepository(context).hasCards(GameType.ELDRITCH)
   ```

2. Check migration status:
   ```kotlin
   val stats = DatabaseInitializer(context).initializeDatabase()
   ```

3. Use Database Management Activity to verify data

### Performance Issues

- Deck operations are cached in memory
- Database queries run on background threads
- Use `CardRepository` for async operations

### Data Not Persisting

- Ensure database is initialized before use
- Check that `updateEncounteredStatus` is called
- Verify database path in Database Management Activity

## Next Steps

1. Start with one game (Eldritch or Arkham)
2. Replace deck operations gradually
3. Test thoroughly before full migration
4. Update UI to use new adapters
5. Remove old database code once stable


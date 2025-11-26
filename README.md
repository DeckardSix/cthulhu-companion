# Cthulhu Companion

A unified Android companion app for Fantasy Flight Games' Arkham Horror and Eldritch Horror board games.

## Project Structure

This project uses a modular architecture with a shared library:

```
cthulhu-companion/
├── app/                    # Main application module
│   ├── GUI/               # Game selection and launcher
│   └── ...
├── shared/                # Shared library module
│   └── ui/               # Common UI components (disclaimers, etc.)
└── ...
```

## Architecture

### Option 2: Shared Library Approach

The project is structured to:
1. **Extract common code** into the `shared` module
2. **Keep game-specific logic** separate
3. **Share UI components** where possible (disclaimers, themes, etc.)
4. **Maintain independence** of both game modules

## Current Status

- ✅ Project structure created
- ✅ Shared module setup
- ✅ Game selection screen
- ✅ Shared disclaimer helper
- ✅ Unified database with game_type field
- ✅ Database migration from both games
- ✅ Card query and update methods
- ✅ CardRepository for async operations
- ✅ Game-specific card helpers (Arkham/Eldritch)
- ✅ Database management activity
- ⏳ Arkham Horror module (to be integrated)
- ⏳ Eldritch Horror module (to be integrated)

## Next Steps

1. Extract common utilities from both games
2. Integrate Arkham Horror module
3. Integrate Eldritch Horror module
4. Share common resources (themes, backgrounds, etc.)
5. Unified game state management

## Building

```bash
./gradlew build
```

## License

Same as original Arkham and Eldritch Companion apps.


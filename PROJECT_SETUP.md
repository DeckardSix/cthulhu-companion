# Cthulhu Companion - Project Setup

## Overview

This is a unified Android companion app that will eventually combine both Arkham Horror and Eldritch Horror companion apps into a single application using a shared library approach (Option 2).

## Project Structure

```
cthulhu-companion/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── java/com/poquets/cthulhu/
│   │   │   ├── CthulhuApplication.kt
│   │   │   └── GUI/
│   │   │       └── GameSelectionActivity.kt  # Launcher screen
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
│
├── shared/                        # Shared library module
│   ├── src/main/
│   │   ├── java/com/poquets/cthulhu/shared/
│   │   │   └── ui/
│   │   │       └── DisclaimerHelper.kt  # Shared disclaimer dialog
│   │   └── AndroidManifest.xml
│   └── build.gradle
│
├── build.gradle                   # Root build file
├── settings.gradle                # Project settings (includes app and shared)
├── gradle.properties              # Gradle configuration
└── README.md
```

## Current Features

✅ **Project Structure**
- Multi-module Android project
- App module for main application
- Shared library module for common code

✅ **Game Selection Screen**
- Launcher activity to choose between games
- Placeholder buttons for Arkham and Eldritch

✅ **Shared Disclaimer Helper**
- Reusable disclaimer dialog
- Can be used by both games
- Supports custom backgrounds
- "Don't show again" functionality

## Next Steps

### Phase 1: Extract Common Code (Current)
- [x] Create project structure
- [x] Set up shared module
- [x] Create disclaimer helper
- [ ] Extract common utilities
- [ ] Share common resources (themes, colors, etc.)

### Phase 2: Integrate Arkham Horror
- [ ] Copy Arkham codebase to app module
- [ ] Refactor to use shared disclaimer
- [ ] Update package names if needed
- [ ] Test Arkham functionality

### Phase 3: Integrate Eldritch Horror
- [ ] Copy Eldritch codebase to app module
- [ ] Refactor to use shared disclaimer
- [ ] Update package names if needed
- [ ] Test Eldritch functionality

### Phase 4: Polish & Integration
- [ ] Wire up game selection to launch appropriate game
- [ ] Unified navigation
- [ ] Shared resources (backgrounds, icons, etc.)
- [ ] Testing both games in unified app

## Building the Project

1. **Set up local.properties:**
   ```bash
   cp local.properties.template local.properties
   # Edit local.properties and set your SDK path
   ```

2. **Sync Gradle:**
   - Open in Android Studio
   - Let Gradle sync automatically
   - Or run: `./gradlew build`

3. **Run the app:**
   - Currently shows game selection screen
   - Both buttons show "Coming Soon" toast

## Using the Shared Disclaimer

The shared disclaimer can be used in any activity:

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

// Or show it manually (e.g., from a "?" icon click)
DisclaimerHelper.showDisclaimer(
    context = this,
    backgroundResId = R.drawable.your_background,
    showCheckbox = true
)
```

## Dependencies

- **Min SDK:** 24
- **Target SDK:** 35
- **Compile SDK:** 35
- **Java Version:** 21
- **Kotlin Version:** 2.1.0

## Notes

- The project uses Kotlin for new code
- Both original apps use Java, so integration will involve mixed language
- The shared module is written in Kotlin for modern Android development
- Game-specific modules can remain in Java if preferred


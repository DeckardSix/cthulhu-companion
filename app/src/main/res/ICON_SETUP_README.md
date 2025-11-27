# Settings Gear Icon Setup

To use a custom gear icon for the settings button on the game selection screen:

1. Place your icon file `ic_settings_gear.png` in the following directories with the appropriate sizes:

   - `drawable-mdpi/` - 24x24 pixels (1x)
   - `drawable-hdpi/` - 36x36 pixels (1.5x)
   - `drawable-xhdpi/` - 48x48 pixels (2x)
   - `drawable-xxhdpi/` - 72x72 pixels (3x)
   - `drawable-xxxhdpi/` - 96x96 pixels (4x)

2. The icon should be a square image (preferably with transparent background)

3. The layout is configured to:
   - Show the icon in the top right corner
   - Have 10dp padding on top and right
   - Make the entire square area clickable
   - Display at 48dp size

4. After adding the icon files, rebuild the project.


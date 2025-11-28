# Eldritch Expansion Icons

This document lists the expansion icons that need to be added to the project.

## Required Icon Files

You can place the icons in **either** location (assets folder is preferred):

### Option 1: Assets Folder (Recommended)
Place all icon PNG files in: `app/src/main/assets/expansion/`

### Option 2: Drawable Folders
Place the following PNG files in the appropriate drawable density folders:
- `drawable-mdpi/` - 24x24 pixels (1x)
- `drawable-hdpi/` - 36x36 pixels (1.5x)
- `drawable-xhdpi/` - 48x48 pixels (2x)
- `drawable-xxhdpi/` - 72x72 pixels (3x)
- `drawable-xxxhdpi/` - 96x96 pixels (4x)

### Icon Files Needed:

1. **icon_exp_fl.png** - Forsaken Lore (FL)
2. **icon_exp_mom.png** - Mountains of Madness (MoM)
3. **icon_exp_sr.png** - Strange Remnants (SR)
4. **icon_exp_utp.png** - Under the Pyramids (UtP)
5. **icon_exp_soc.png** - Signs of Carcosa (SoC)
6. **icon_exp_td.png** - The Dreamlands (TD)
7. **icon_exp_cir.png** - Cities in Ruin (CiR)
8. **icon_exp_mon.png** - Masks of Nyarlathotep (MoN)

## Icon Usage

- **Antarctica** checkbox uses `icon_exp_mom` (same as Mountains of Madness)
- **Cosmic Alignment** checkbox uses `icon_exp_sr` (same as Strange Remnants)
- **Egypt** checkbox uses `icon_exp_utp` (same as Under the Pyramids)
- **Litany of Secrets** checkbox uses `icon_exp_utp` (same as Under the Pyramids)
- **Dreamlands Board** checkbox uses `icon_exp_td` (same as The Dreamlands)

## Notes

- Icons should be square images with transparent backgrounds
- Icons are displayed at 24dp size next to the checkbox text
- If an icon file is not found, the checkbox will display without an icon (a warning will be logged)


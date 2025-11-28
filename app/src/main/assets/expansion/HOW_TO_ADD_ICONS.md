# How to Add Expansion Icons

## Step-by-Step Instructions

### 1. Prepare Your Images

You have 8 images that need to be saved as PNG files. Here's the mapping:

| Image Description | Save As | Expansion |
|------------------|---------|-----------|
| Open book with ornate swirls | `icon_exp_fl.png` | Forsaken Lore |
| Mountain peak with tentacles | `icon_exp_mom.png` | Mountains of Madness |
| Stylized head in profile | `icon_exp_sr.png` | Strange Remnants |
| Stylized letter "A" | `icon_exp_utp.png` | Under the Pyramids |
| Cat-like creature with tentacles | `icon_exp_soc.png` | Signs of Carcosa |
| Stylized head (alternative) | `icon_exp_td.png` | The Dreamlands |
| Stylized numeral "2" | `icon_exp_cir.png` | Cities in Ruin |
| Masquerade mask | `icon_exp_mon.png` | Masks of Nyarlathotep |

### 2. Image Processing Requirements

For each image:
1. **Remove the dark green background** - Make it transparent
2. **Keep the white outline** - This helps the icon stand out
3. **Save as PNG** - PNG format supports transparency
4. **Recommended size**: 48x48 to 96x96 pixels (or maintain aspect ratio if not square)

### 3. Save Location

Save all 8 PNG files in this exact folder:
```
cthulhu-companion/app/src/main/assets/expansion/
```

### 4. File Names (Must Match Exactly)

- `icon_exp_fl.png`
- `icon_exp_mom.png`
- `icon_exp_sr.png`
- `icon_exp_utp.png`
- `icon_exp_soc.png`
- `icon_exp_td.png`
- `icon_exp_cir.png`
- `icon_exp_mon.png`

### 5. After Adding Files

1. Rebuild the Android project
2. Run the app
3. Go to the Eldritch Setup screen
4. The icons should now appear next to each expansion checkbox

## Quick Reference

The code will automatically:
- Load icons from `assets/expansion/` folder
- Display them at 24dp size next to checkbox text
- Fall back gracefully if an icon is missing (shows checkbox without icon)

## Troubleshooting

If icons don't appear:
1. Check file names match exactly (case-sensitive)
2. Verify files are in `assets/expansion/` folder (not a subfolder)
3. Ensure files are PNG format
4. Check Android Studio shows the files in the assets folder
5. Clean and rebuild the project
6. Check logcat for "Icon not found" messages


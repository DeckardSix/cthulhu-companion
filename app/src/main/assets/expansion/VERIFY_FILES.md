# Verify Icon Files Are Present

Run this command in PowerShell to check if all 8 icon files exist:

```powershell
cd C:\Repository\Android\cthulhu-companion\app\src\main\assets\expansion
Get-ChildItem *.png | Select-Object Name
```

You should see:
- icon_exp_fl.png
- icon_exp_mom.png
- icon_exp_sr.png
- icon_exp_utp.png
- icon_exp_soc.png
- icon_exp_td.png
- icon_exp_cir.png
- icon_exp_mon.png

If any are missing, you need to add them manually.

## Quick Copy Instructions

If you have the images saved somewhere else:

1. Open File Explorer
2. Navigate to: `C:\Repository\Android\cthulhu-companion\app\src\main\assets\expansion\`
3. Copy your 8 PNG files into this folder
4. Make sure the file names match exactly (case-sensitive)
5. Rebuild the project in Android Studio


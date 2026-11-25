# Larva Seconds Lost

A [Scelight](https://sites.google.com/site/scelight/) module for StarCraft II Zerg players. It shows how well you use your larva and injects: for one replay, or averaged over many replays.

**Download and changes:** https://minetonight.github.io/scelight/larva-seconds-lost/

## What it shows

- **Larva timeline** per hatchery: when larva sat at 3 (wasted), inject uptime, and likely missed inject windows.
- **Phase table:** Early, Mid, Late and End game columns with larva generated, larva missed, missed inject value and inject uptime.
- **Benchmarks:** your larva rates ranked against bands from higher-level games.
- **Multi analysis:** pick many replays and get pooled tables for All games, ZvP, ZvT and ZvZ, plus milestones (100 supply, 66 and 80 drones, 200 supply).
- **New replays open automatically** after each game.

## Requirements

- Scelight (tested with 6.5.4) on Windows, Linux or macOS.
- Java 8 or newer to run Scelight. The update check needs TLS 1.2, which older Java versions do not turn on.

## Install (one time)

1. Download `LarvaSecondsLost-<version>.zip` from the [download page](https://minetonight.github.io/scelight/larva-seconds-lost/).
2. Close Scelight.
3. Extract the zip into the folder that **contains** your `Scelight` folder (not into `Scelight` itself). Afterwards this folder must exist:
   `Scelight/mod-x/larva-seconds-lost/<version>/`
4. Start Scelight. Open **Help → About Scelight** (F1), then **Available Modules → Installed Modules**. Enable **Larva Seconds Lost** and restart Scelight.
5. Open the new **Larva** page in the left tree.

## Updates

You install by hand only once. After that:

1. The bottom row of the Larva page shows your version. When a newer version is out, it says so.
2. Click **Install**. The module downloads the new version, checks it against the published checksums, and puts it next to your current version.
3. Restart Scelight. The new version starts, and it removes the old version folder.

The module checks once a day, and when you click **Check now**. It only reads `https://minetonight.github.io/scelight/larva-seconds-lost/module.xml` and sends no data about you or your replays (GitHub sees your IP address, like any web page). Untick **Check for updates automatically** to stop it.

A manual update also works: extract a newer zip the same way as in "Install". Scelight always uses the highest version folder.

## Uninstall

Close Scelight and delete the folder `Scelight/mod-x/larva-seconds-lost/`.

## Troubleshooting

| Problem | What to do |
|---|---|
| No Larva page | Check the module is enabled (Install, step 4). Check the folder level: it must be `Scelight/mod-x/larva-seconds-lost/<version>/`, not `Scelight/Scelight/...`. |
| "Update failed" or "Update check failed" | Nothing was changed. Check your internet connection or firewall, and that Scelight runs on Java 8 or newer. Click **Open download page** and update by hand. |
| The module stopped working after an update | Close Scelight and delete the newest folder in `Scelight/mod-x/larva-seconds-lost/`. Scelight then uses the previous version, if it is still there. Otherwise install the previous zip by hand (old zips stay on the download page folder). |
| Something else | Scelight writes a log file per run (Linux: `~/.config/scelight/logs/`). Lines from this module contain `Larva` or `[larva-`. Report problems on [GitHub issues](https://github.com/minetonight/LarvaSecondsLost-Scelight/issues) or on Discord: `scorpion_rbp`. |

## For developers

- Module source, build and tests: [LarvaSecondsLostExtMod/README.md](LarvaSecondsLostExtMod/README.md)
- How to publish a new version: [RELEASING.md](RELEASING.md)

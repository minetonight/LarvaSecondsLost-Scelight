# Local paths (template)

Copy this file to `knowhow/local-paths.md` (gitignored) and fill in the values for your machine.
The knowhow docs use these variables instead of real paths, so no personal path is committed.

```bash
export REPO="$(git rev-parse --show-toplevel)"          # this repository
export SCELIGHT_HOME="/path/to/Scelight"                 # Scelight install used for development (folder named Scelight)
export SCELIGHT_HOME_2=""                                # optional second install; do not develop against it
export SC2_ACCOUNTS="/path/to/StarCraft II/Accounts"     # Scelight's monitored replay folder
export SC2_REPLAY_DEST="$SC2_ACCOUNTS/<account-id>/<toon>/Replays/Multiplayer"  # where copy tests drop replays
export REPLAY_BACKUP="/path/to/Scelight Replay Backup"   # Scelight's replay backup folder
```

`scelightFolder` in `LarvaSecondsLostExtMod/release/local.properties` should point to the same folder as `$SCELIGHT_HOME`.

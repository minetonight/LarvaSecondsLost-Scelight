# Running Scelight (for developers and AI agents)

Verified on 2026-11-24 on this machine (Ubuntu, KDE Wayland, Scelight 6.5.4, launcher 3.1.4).

## Java version

Scelight is expecting Java 8. The package name `javax.xml.bind.*` is the key clue: old apps commonly depended on that being included by default, while modern OpenJDK releases do not include it unless you add compatibility jars manually.

Use `/usr/lib/jvm/java-8-openjdk-amd64/bin/java` (the default `java` on the PATH is 17 and must not be used to run Scelight).

## Paths

| What | Path |
|---|---|
| Scelight install (used for development) | `/home/aleks/StarCraft II/Scelight/` |
| Installed module | `/home/aleks/StarCraft II/Scelight/mod-x/larva-seconds-lost/<version>/` |
| Start script | `/home/aleks/StarCraft II/Scelight/startScelight.sh` |
| Scelight log (one file per run) | `~/.config/scelight/logs/<yyyy-MM-dd HH_mm_ss>.log`, newest = current run |
| Dev diagnostic dump | `/tmp/larva-latest-dump.txt` (set by `startScelight.sh` via `JAVA_TOOL_OPTIONS`) |
| Monitored replay folder | `/home/aleks/Documents/StarCraft II/Accounts` (with sub-folders) |
| Fixture replays | `benchmark-data/queensMoved/*.SC2Replay` |

A second Scelight install exists at `/home/aleks/Documents/StarCraft II/Scelight/`. Do not use it for development.

## Build and deploy the module

```bash
cd LarvaSecondsLostExtMod
ant INSTALL_DEPLOYMENT
```

This builds the release and replaces `mod-x/larva-seconds-lost/` in the Scelight folder set by `scelightFolder` in `release/release.properties`. **Scelight must be stopped first**, otherwise the running JVM keeps the old jar.

## Start

Check that no Scelight is running, then start it:

```bash
pgrep -f hu.sllauncher.ScelightLauncher && echo "already running - stop it first"

cd "/home/aleks/StarCraft II/Scelight"
export JAVA_TOOL_OPTIONS='-Dlarva.dev.dump.enabled=true -Dlarva.dev.dump.file=/tmp/larva-latest-dump.txt -Dlarva.dev.commands.file=/home/aleks/larva-dev-commands.txt'
/usr/lib/jvm/java-8-openjdk-amd64/bin/java -Xmx1024m -Dfile.encoding=UTF-8 \
  -Dhu.scelight.launched-with=Scelight-linux.sh \
  -cp "mod/launcher/3.1.4/scelight-launcher.sldat:mod/launcher/3.1.4/jl1.0.1.jar:mod/launcher/3.1.4/mp3spi1.9.5.jar:mod/launcher/3.1.4/tritonus_share.jar" \
  hu.sllauncher.ScelightLauncher > "/home/aleks/StarCraft II/Scelight/scelight-stdout.log" 2>&1 &
```

- `larva.dev.commands.file` enables the dev command channel (E17-S00). Until S00 is implemented the property is simply ignored.
- `JAVA_TOOL_OPTIONS` is split on spaces, so every path in it must be free of spaces. That is why the commands file is `/home/aleks/larva-dev-commands.txt` and not inside the Scelight folder.
- Note: `startScelight.sh` redirects stdout to an **unquoted** path with a space, so its output lands in a file named `/home/aleks/StarCraft` and `II/Scelight/...` is passed to Scelight as an argument. Use the command above, or quote the path in the script.

**Startup is ready** when the newest log contains `Larva Seconds Lost module started successfully.` This takes about 20–25 s, because an unregistered start is delayed by 10 s.

```bash
LOG="$(ls -t ~/.config/scelight/logs/*.log | head -1)"
until grep -q "Larva Seconds Lost module started successfully" "$LOG"; do sleep 2; done
```

Always re-read `LOG` after a start: each run writes a new file.

## Read the log

```bash
LOG="$(ls -t ~/.config/scelight/logs/*.log | head -1)"
grep -n "Larva Seconds Lost\|\[larva-dev\]\| E " "$LOG"
```

Format: `yyMMdd HHmmss.SSS <level> <message>`, levels `D` debug, `I` info, `W` warning, `E` error (followed by a stack trace).

Known noise, not caused by the module:
- `W Registration file has expired!`
- `E Uncaught exception ... Replay Counter: null` / `NullPointerException at RepCrawlerJob.jobRun(RepCrawlerJob.java:113)`: caused by empty rows in the Replay Folders page (positions 2 and 4). Harmless.

## Screenshot

Scelight runs on XWayland, so X11 tools see its window:

```bash
WIN=$(xwininfo -root -tree | grep '"Scelight [0-9]' | awk '{print $1}')
import -window "$WIN" -resize 50% /path/to/scratch/scelight.png
```

Then open the PNG with the image-reading tool to check the UI. Do not commit agent screenshots unless a story asks for one under `knowhow/`.

## Drive the module: dev command channel (E17-S00)

Available after E17-S00 is implemented and Scelight was started with `-Dlarva.dev.commands.file=<file>`.

Append one line per command: `<id> <command> [args…]`. Quote arguments with spaces. Use a new id every time.

```bash
CMD="/home/aleks/larva-dev-commands.txt"
echo '1 ping' >> "$CMD"
sleep 2
grep '\[larva-dev\] 1 ' "$LOG"
```

Each command logs `[larva-dev] <id> OK …` or `[larva-dev] <id> FAIL …`; long output follows as `[larva-dev] <id> | …` lines. Lines already in the file when Scelight starts are skipped.

| Command | From story | Effect |
|---|---|---|
| `ping` | S00 | Channel alive check, logs module version |
| `dump-tree` | S00 / S04 | Larva node and sub-pages: label, type, icon, closable, selected |
| `select-larva` | S00 | Select the Larva page |
| `window-info` | S00 | Main frame title and bounds |
| `exit` | S00 | Clean exit through Scelight's normal close path |
| `select <label>`, `close <label>` | S04 | Select or close a sub-page (same path as the X button) |
| `hub-state`, `set-ladder-only <true/false>` | S05 | Hub buttons, checkbox, tooltip |
| `open-replay <path>`, `analyze-latest`, `refresh <label>`, `page-text <label>` | S06 | Single-replay actions without the file chooser; page content as text |
| `auto-open-state` | S07 | Auto-open queue, cap and last decisions |
| `multi <ladderOnly> <file…>`, `multi-cancel <label>`, `multi-state <label>` | S14 | Multi analysis without the chooser |
| `page-text <label>` (multi pages) | S15 | Header, filter summary and all tables as text |

Keep this table in sync when a story adds or changes a command.

The channel bypasses only the file chooser and the mouse. Real clicks, dialogs and look and feel are short manual checks for the user.

## Stop

- **Preferred (after E17-S00):** `echo '<id> exit' >> "$CMD"`. Scelight runs its exit sequence. The log ends with `Larva Seconds Lost destroy sequence completed.` and `Larva Seconds Lost module stopped.`
- **Fallback (before S00, or if Scelight hangs):** `kill -TERM <java pid>` (`pgrep -f hu.sllauncher.ScelightLauncher`). This kills the JVM **without** Scelight's exit sequence: module `destroy()` does not run and Scelight settings changed in that session are not saved. Never use it to test shutdown behaviour.
- If the user is using Scelight, do not stop it without asking.

## Simulate a new replay (FR-17, E17-S07)

The user approved copying test replays into the real monitored folder **and deleting them afterwards**.

```bash
DEST="/home/aleks/Documents/StarCraft II/Accounts/452903306/2-S2-1-7509272/Replays/Multiplayer"
cp "benchmark-data/queensMoved/<fixture>.SC2Replay" "$DEST/zz-larva-test-<fixture>.SC2Replay"
# ... check the log, dump-tree, screenshot ...
rm "$DEST"/zz-larva-test-*.SC2Replay
```

Rules:
- Always prefix copied files with `zz-larva-test-` so they are easy to find and never confused with real games.
- Delete them in the same session, also when the check fails. Before finishing, verify that `ls "$DEST"/zz-larva-test-*` finds nothing.
- Never move, rename or delete any other file in the Accounts folder.
- Scelight's replay-processor cache may keep entries for the deleted files. That is harmless.

## Agent check loop

1. Stop Scelight if running (`exit` command, fallback SIGTERM).
2. `ant INSTALL_DEPLOYMENT`.
3. Start with the command above and wait for `module started successfully`.
4. Run the story's dev commands, grep the log for `[larva-dev]` results and ` E ` errors, take screenshots where the story asks.
5. For FR-17: copy, check, delete the test replays.
6. `exit`, and confirm `Larva Seconds Lost module stopped.` in the log.

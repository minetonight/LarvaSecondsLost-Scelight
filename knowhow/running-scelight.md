# Running Scelight (for developers and AI agents)

Verified on 2026-11-24 on this machine (Ubuntu, KDE Wayland, Scelight 6.5.4, launcher 3.1.4).

## Java version

Scelight is expecting Java 8. The package name `javax.xml.bind.*` is the key clue: old apps commonly depended on that being included by default, while modern OpenJDK releases do not include it unless you add compatibility jars manually.

Use `/usr/lib/jvm/java-8-openjdk-amd64/bin/java` (the default `java` on the PATH is 17 and must not be used to run Scelight).

## Paths

Machine-specific paths are written as shell variables. Their values for this machine are in [local-paths.md](local-paths.md), which is gitignored (copy [local-paths.example.md](local-paths.example.md) on a new machine). Run its `export` block once per shell before using the commands below.

| What | Path |
|---|---|
| Scelight install (used for development) | `$SCELIGHT_HOME/` |
| Installed module | `$SCELIGHT_HOME/mod-x/larva-seconds-lost/<version>/` |
| Start script | `$SCELIGHT_HOME/startScelight.sh` |
| Scelight log (one file per run) | `~/.config/scelight/logs/<yyyy-MM-dd HH_mm_ss>.log`, newest = current run |
| Dev diagnostic dump | `/tmp/larva-latest-dump.txt` (set by `startScelight.sh` via `JAVA_TOOL_OPTIONS`) |
| Monitored replay folder | `$SC2_ACCOUNTS` (with sub-folders) |
| Fixture replays | `benchmark-data/queensMoved/*.SC2Replay` |

`$SCELIGHT_HOME_2/` looks like a second install, but on this machine `$SCELIGHT_HOME`'s parent is a symlink to it (checked 2026-11-25): it is the **same** folder. Log lines show the resolved `Documents/...` path.

## Build and deploy the module

```bash
cd LarvaSecondsLostExtMod
ant INSTALL_DEPLOYMENT
```

This builds the release and replaces `mod-x/larva-seconds-lost/` in the Scelight folder set by `scelightFolder` in `release/local.properties` (gitignored, copy it from `release/local.properties.example`). **Scelight must be stopped first**, otherwise the running JVM keeps the old jar.

## Start

Check that no Scelight is running, then start it:

```bash
pgrep -f hu.sllauncher.ScelightLauncher && echo "already running - stop it first"

cd "$SCELIGHT_HOME"
export JAVA_TOOL_OPTIONS="-Dlarva.dev.dump.enabled=true -Dlarva.dev.dump.file=/tmp/larva-latest-dump.txt -Dlarva.dev.commands.file=$HOME/larva-dev-commands.txt"
/usr/lib/jvm/java-8-openjdk-amd64/bin/java -Xmx1024m -Dfile.encoding=UTF-8 \
  -Dhu.scelight.launched-with=Scelight-linux.sh \
  -cp "mod/launcher/3.1.4/scelight-launcher.sldat:mod/launcher/3.1.4/jl1.0.1.jar:mod/launcher/3.1.4/mp3spi1.9.5.jar:mod/launcher/3.1.4/tritonus_share.jar" \
  hu.sllauncher.ScelightLauncher > "$SCELIGHT_HOME/scelight-stdout.log" 2>&1 &
```

- `larva.dev.commands.file` enables the dev command channel (E17-S00, implemented). Without it the module starts no channel thread and touches no file. Create the file (empty is fine) before starting: `: > $HOME/larva-dev-commands.txt`. If it does not exist, the channel starts at offset 0 and reads it once it appears.
- `pgrep -f hu.sllauncher.ScelightLauncher` also matches the shell running it. Use `pgrep -f '[h]u.sllauncher.ScelightLauncher'`.
- `JAVA_TOOL_OPTIONS` is split on spaces, so every path in it must be free of spaces. That is why the commands file is `$HOME/larva-dev-commands.txt` and not inside the Scelight folder.
- Note: `startScelight.sh` redirects stdout to an **unquoted** path with a space, so its output lands in a file named `$HOME/StarCraft` and `II/Scelight/...` is passed to Scelight as an argument. Use the command above, or quote the path in the script.

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

Implemented (E17-S00). Available when Scelight was started with `-Dlarva.dev.commands.file=<file>`.

Append one line per command: `<id> <command> [args…]`. Quote arguments with spaces. Use a new id every time.

```bash
CMD="$HOME/larva-dev-commands.txt"
echo '1 ping' >> "$CMD"
sleep 2
grep '\[larva-dev\] 1 ' "$LOG"
```

Each command logs `[larva-dev] <id> OK …` or `[larva-dev] <id> FAIL …`; long output follows as `[larva-dev] <id> | …` lines. Lines already in the file when Scelight starts are skipped (the log shows `channel started, offset=<bytes>`). Only complete lines (ending in newline) are executed. Commands are polled every 500 ms. A truncated file restarts at offset 0. Blank lines and `#` comments are ignored; a line with an unterminated quote logs `? FAIL ? bad line`. An exception in a command is logged with stack trace and `FAIL`; the channel keeps running.

| Command | From story | Effect |
|---|---|---|
| `ping` | S00 | Channel alive check, logs module version |
| `dump-tree` | S00 / S04 | Larva node and sub-pages (indented): label, type (`LarvaSubPage(KIND)`), icon, closable, `[SELECTED]`; result line has the selected label and `hostCloseEnabled` (state of the host X button) |
| `select-larva` | S00 | Select the Larva page |
| `window-info` | S00 | Main frame title, bounds, visible, extended state |
| `exit` | S00 | Posts `WINDOW_CLOSING` to the main frame (`services.getMainFrame().asFrame()`, fallback: `Frame.getFrames()` title starts with `Scelight`) via `invokeLater`. Logs `OK exit` and the process ends about 1.5 s later |
| `spike-batch-*` | S02 (removed in S12) | Spike commands, deleted; use the `multi-*` commands below |
| `select <label>`, `close <label>` | S04 | `select` selects the hub (`Larva`) or a sub-page by tree label. `close` selects the page, then clicks the host's own close button (found by tool tip `Close Page (CTRL+F4)`), i.e. the same handler as the X; FAILs with "close button disabled" for the hub. Logs the new selection and `stillOpen` |
| `add-test-page ...` | S04 (removed in S06) | Temporary placeholder-page hook, deleted: real pages exist now, use `open-replay` |
| `hub-state`, `set-ladder-only <true/false>` | S05 | `hub-state`: checkbox state (`ladderOnly=`), button labels, tip text, size. `set-ladder-only` sets the checkbox like a click. Both FAIL with "hub component not created yet" until the hub was shown once (`select Larva`); Scelight creates page content lazily. The tip icon's `getToolTipText()` is null (popup icon shows the tip on hover/click per Scelight settings), so `hub-state` logs the tip text constant instead |
| `open-replay <path>`, `analyze-latest`, `refresh <label>`, `page-text <label>` | S06 | `open-replay` runs the Open Replay path without the chooser (source `Manual replay selection`): result `opened new page '<label>' state=BUSY` or `focused existing page` (dedupe by normalized absolute path, no re-analysis). `analyze-latest` is the Analyze Latest button (source `Latest replay resolver`); it FAILs instead of showing the info dialog when nothing resolves (a modal dialog would block the channel). `refresh <label>` is the page's Refresh button (in place). `page-text <label>` logs state, replay path, the status line and the details text (overview, player totals, phase table, rankings, hatchery breakdown) of a single-replay page; FAILs with "page component not created yet" for a page that was never selected. Paths with spaces or parentheses must be double-quoted, labels too |
| `auto-open-state`, `auto-open-submit <path>` | S07 | `auto-open-state` logs `pending=<n>/5 openSinglePages=<n>/20 ready=` and the counters (`detected opened notZerg skipped droppedPendingLimit precheckFailed`), then the last 20 decisions as `\|` lines. `auto-open-submit` hands a path to the auto-open queue exactly like the replay listener does (dev only; for dedupe, cap, focus-rule and pre-check-failure checks with paths Scelight would not report, e.g. a fixture outside the monitored folder or a truncated file) |
| `multi-run <dir\|listFile\|replay> [true\|false] [noprefilter]`, `multi-cancel`, `multi-state [full]`, `multi-heap [gc]` | S12 | Batch runner without a page. `multi-run` takes a folder (non-recursive `*.SC2Replay`, sorted by name), a text file with one path per line, or one replay; the second argument is `ladderOnly` (default `true`); `noprefilter` runs the full analysis for every file. It returns at once (`started N files ...`); the work runs on the `larva-multi-analysis` worker. `multi-cancel` sets the token. `multi-state` logs `state=RUNNING\|COMPLETED\|CANCELLED\|FAILED k=<k>/<N> failed=<f> elapsedMs=... progressPostsOnEdt=... edtOutcome=...`, for a cancelled run `processedWhenCancelled` and `cancelLatencyMs` (request to the `onCancelled` callback on the EDT), plus the dev monitor: `heapBaselineMb` (after a forced GC at the start), `heapPeakUsedMb(withGarbage)` (raw, includes garbage the GC has not collected, so it can look huge in a 1 GB JVM), `heapPeakLiveAfterGcMb` / `heapPeakLiveGrowthMb` (peak of what survived the last collection; use this one for the 200 MB budget), `edtHeartbeats`, `edtMaxDelayMs`, `edtStallsOver200ms` (a task posted every 50 ms to the EDT; its start delay). When completed it appends the filter summary, game count, skipped count and tracked players; `multi-state full` adds the whole result dump as `\|` lines (same text as the headless dump). `multi-heap [gc]` logs used/total/max MB and, after `gc`, the growth retained over the last run's baseline. Pass paths with spaces in double quotes. Navigate with `select-larva`, `open-replay`, `select "<label>"` while a run is going |
| `multi-open <dir\|listFile\|replay> [true\|false] [confirm=yes\|no]`, `multi-page-state <index> [full]`, `multi-page-cancel <index>`, `multi-page-close <index>`, `multi-chooser-config`, `set-default-locale <lang> [country]` | S14 | Multi analysis page without the chooser. `multi-open` runs everything the Multi Analysis button does after the chooser (`openMultiAnalysisPage`): a **new** multi page every call (never deduped), selected, run started. Above 500 files the confirm step is asked through the injectable `Confirmer`; the command answers with `confirm=` (default `yes`) and logs the dialog text (`confirmAsked=true answer=no dialogText='Analyze 501 replays? Estimated time: ~1 min.'`); `no` creates no page (`declined, no page`). Pages are addressed by their **0-based index among the multi pages in tree order** (labels are not unique while runs go). `multi-page-state` logs label, `state=RUNNING\|DONE\|CANCELLED\|FAILED`, `k=<k>/<N>`, `failed=`, and what the component shows (status line `Analyzing k / N: <file>`, `Failed: f`, progress bar value/max, result header); `full` adds the result text dump as `\|` lines. `multi-page-cancel` asks the run to stop (page ends as `Multi analysis #k (cancelled)`). `multi-page-close` selects the page and clicks the host close button (the X handler): log shows `page disposed, run cancelled` and `late ... callback ignored`. `multi-chooser-config` builds the real Scelight multi chooser **without showing it** and logs multi-select, files-only, filter text and accept results, current directory. `set-default-locale bg BG` changes the JVM default locale at runtime to check the date label (see note below) |
| `multi-page-state <index> [full]` (extended), `multi-page-toggle-skipped <index>`, `multi-page-snapshot <index> <png>`, `set-frame-size <w> <h>` | S15 | For a finished page `multi-page-state` adds `\| layout ...` lines about the results view: header, filter summary, help line and empty-state text; panel size and preferred size; the enclosing page scroll pane (`viewport=`, `horizontalScrollBar=`, `verticalScrollBar=`); per column its `x`/`width` and per table `title`, `x`, `y`, `width`, `height`, `preferredWidth`, `clipped=` (the table reaches beyond its grid cell); `anyTableClipped=`; skipped count, `skippedExpanded=` and the toggle text. `full` appends the view-model text (`header:`, `summary:`, `player n:`, `table:` with the header and rows as `label \| cell \| ...`; per player the tables are Milestones, All, ZvP, ZvT, ZvZ since S17, `skipped (n):`), the cell strings of the golden check. `multi-page-toggle-skipped` clicks the section's toggle in code and logs the entries. `multi-page-snapshot` paints the whole results panel (not only the visible part) into a PNG. `set-frame-size` un-maximizes the main frame and sets its size (the window manager may add or remove a few pixels, check `window-info`); at 1280 px Scelight's page area is about 1000 px wide |
| `chooser-folder-state`, `chooser-folder-confirm <open\|multi> <path>` | S17 | `chooser-folder-state` logs the memory file, `lastDetected`, and per chooser (`open` = Open Replay on hub and single pages, `multi` = Multi Analysis) the remembered folder, whether it still exists, the start folder (remembered if it exists, else parent of the last detected replay, else none) and the `currentDirectory` of a freshly built real chooser (not shown). `chooser-folder-confirm` feeds a confirmed selection of that file through `LarvaReplayFileChooser.rememberSelection`, the method the chooser result uses (the folder of the file is remembered and written to disk); it does not open the chooser and never touches the file itself |
| `show-help [hide]` | S16 | Shows (or hides) the help popup of the hub's help icon, as a click on it would (`IPopupIcon.showPopup` with a synthetic mouse press). Needs the hub component (`select-larva` first). The popup is a lightweight popup inside the main window when it fits (current help: about 750 x 930 px), otherwise a separate `###focusableSwingPopup###` X window cut off by the screen; screenshot the Scelight window (`xwininfo -root -tree`, `import -window <id>`) or that popup window. It stays open until `show-help hide` |
| `update-state` | E18 | Online update status: phase (IDLE, CHECKING, UP_TO_DATE, AVAILABLE, INSTALLING, INSTALLED, FAILED), running version, message, checked URL, automatic checks on/off, the found release, and whether the hub row exists. |
| `update-check` | E18 | Like **Check now**: queues a check and returns at once. Poll `update-state`. |
| `update-install` | E18 | Like **Install**: only in phase AVAILABLE or FAILED. Returns at once. Poll `update-state` for INSTALLED, then `exit` and start Scelight again to load the new version. |

Keep this table in sync when a story adds or changes a command.

**Locale checks (S14):** do not start Scelight with `-Duser.language=bg`. Scelight ties its registration to the system info, a changed language gives `Registration info mismatch` / `Not registered, delaying start by 10 sec...` and the launcher then waits for a click on START (no auto start, an agent cannot click it). Start normally and use `set-default-locale bg BG` / `set-default-locale en US` on the channel: the label uses `Locale.getDefault()` at completion time, on Scelight's own Java 8. (Java 8's bg data gives `dd.MM.yy`; a newer JRE appends `г.`, which the formatter strips.)

The channel bypasses only the file chooser and the mouse. Real clicks, dialogs and look and feel are short manual checks for the user.

## Remembered chooser folders (E17-S17)

`Open Replay...` (hub and single-replay pages share one memory) and `Multi Analysis...` each remember the folder of their last confirmed selection across Scelight restarts.

- **Location:** `~/.config/scelight/mod-x-cache/larva-seconds-lost/chooser-folders.properties`. That is the module cache folder from `IModEnv.getCacheFolder()` (SDK 1.5.2; Scelight keeps it across restarts). UTF-8 properties file with two keys, `openReplay.folder` and `multiAnalysis.folder`, written through a temp file and a move. Delete the file (Scelight stopped) to reset.
- **Why not `IModEnv.initSettingsBean`:** SDK 1.5.2 also offers settings beans (persisted XML in the same cache folder), but a bean's settings appear automatically in Scelight's Settings dialog and need a node, group, skill level and icon; the dialog cannot be checked by an agent and these two values are internal state, not user settings.
- **Start folder order:** remembered folder if it still exists, then the parent of the last detected replay, then Scelight's chooser default. Cancel never writes. Write and read errors are logged (`[larva-hub] could not ...`) and never break the chooser; the value then stays in memory for the session. If the cache folder cannot be resolved the memory is RAM only.
- Check without clicks: `chooser-folder-confirm`, `chooser-folder-state`, `exit`, restart, `chooser-folder-state`.

## Stop

- **Preferred:** `echo '<id> exit' >> "$CMD"`. Scelight runs its exit sequence (observed: no confirm dialog; the process ended within about 2 s). The log ends with `Larva Seconds Lost destroy sequence completed.` and `Larva Seconds Lost module stopped.`
- **Fallback (before S00, or if Scelight hangs):** `kill -TERM <java pid>` (`pgrep -f hu.sllauncher.ScelightLauncher`). This kills the JVM **without** Scelight's exit sequence: module `destroy()` does not run and Scelight settings changed in that session are not saved. Never use it to test shutdown behaviour.
- If a Scelight is already running (also one the user started), stop it: `exit` via the channel if it has one, otherwise `kill -TERM <java pid>`. The user approved this on 2026-11-24.

## Simulate a new replay (FR-17, E17-S07)

> **Precondition — the SC2ReplayStatsUploader module must be disabled before any copy.** The user disabled it on 2026-11-24. Check before every copy test: `grep -o 'sc2replaystats&lt;/folder&gt;&#xA;        &lt;enabled&gt;[a-z]*' ~/.config/scelight/launcher-settings.xml` must end in `false`; if it does not, STOP and ask the user. Background: on 2026-11-24 the S06 and S07 test runs copied 16 replays (pro games plus the user's own) and the uploader picked up every one; one upload failed with HTTP 413, the rest may have been uploaded to the user's sc2replaystats.com account. For pure logic checks, prefer the `auto-open-submit <path>` dev command, which needs no copy.

The user approved copying test replays into the real monitored folder **and deleting them afterwards**. Scelight renames every copy about 1.5 s after it lands and also writes a backup copy into `$REPLAY_BACKUP/` (verified E17-S06/S07), so the cleanup must be exact. Follow these steps literally.

Facts to know first:
- Scelight renames to `<date> <matchup> {..} vs {..} on <map>.SC2Replay` (`zz-larva-test-` prefix gone) and unifies a name that already exists (a backup of the same name gets ` (2)`). Scelight also opens and **selects its own Replay Analyzer page** just before it notifies the module, so the user is never on a `Larva` page when a real new replay arrives (focus rule D17-c then adds without selecting). To test "select" use `auto-open-submit` while on the hub.
- The installed **SC2ReplayStatsUploader** module also sees every copied replay (`SC2ReplayStatsUploader mod: found new replay`) and may upload it to the user's sc2replaystats.com account (a 413 error was logged for one). Tell the user after a test session.
- Prefer fixtures that are not the user's games: `benchmark-data/raw-replays/seeded-sample/*.SC2Replay` (pro games, all contain Zerg). A non-Zerg replay can only come from the user's library (e.g. a `PvT` file under another account folder).

Steps:
1. **Before copying anything**, record the counts and sorted lists (scratchpad, not the repo):
   ```bash
   A="$SC2_ACCOUNTS"; B="$REPLAY_BACKUP"
   DEST="$SC2_REPLAY_DEST"
   find "$A" -name "*.SC2Replay" | wc -l      # expected 8352 on 2026-11-24
   ls "$B" | wc -l                            # expected 4059
   find "$A" -name "*.SC2Replay" | sort > $SP/before-accounts.txt
   ls "$DEST" | sort > $SP/before-multiplayer.txt
   ls "$B" | sort > $SP/before-backup.txt
   ```
2. Copy with plain `cp` (not `cp -p`, the copy must get today's mtime), always named `zz-larva-test-<something>.SC2Replay`. Note which fixtures you used.
3. Do the checks (log, `auto-open-state`, `dump-tree`, screenshots). Then `exit` Scelight first and clean up with Scelight stopped.
4. Delete **only** files that (a) appear in this run's log lines `Renamed new replay: "<from>" to "<to>"` (the `<to>` path) or `New replay saved: <path>` for files you copied, (b) are not in the before-lists (compare the full path with `before-accounts.txt`, the file name with `before-backup.txt`), and (c) have an mtime from today. If a target is in a before-list: stop, delete nothing more and report. Never delete by pattern.
   ```bash
   LOG="$(ls -t ~/.config/scelight/logs/*.log | head -1)"
   grep "Renamed new replay" "$LOG" | sed -E 's/.*" to "(.*)"$/\1/' > $SP/renamed.txt
   grep "New replay saved: " "$LOG" | sed -E 's/.*New replay saved: //' > $SP/saved.txt
   ```
5. **After cleanup** re-run the counts and diff the sorted lists against the before-lists; they must match exactly (`diff` prints nothing). Report before/after counts and the diff result. Scelight's replay-processor cache may keep entries for the deleted files. That is harmless.

Never move, rename or delete any other file in the Accounts folder or the backup folder.

Close behaviour notes: Scelight's main frame (`XFrame`, `DO_NOTHING_ON_CLOSE`) handles `WINDOW_CLOSING` by `MainFrame.close()`, which runs Exit, or only minimizes to tray if the setting "Minimize to Tray on Close" is on. On this machine the setting is off or the tray is unsupported and exit works. If `exit` logs OK but the process stays alive, that setting is probably on: turn it off in Scelight settings.

## Agent check loop

1. Stop Scelight if running (`exit` command, fallback SIGTERM).
2. `ant INSTALL_DEPLOYMENT`.
3. Start with the command above and wait for `module started successfully`.
4. Run the story's dev commands, grep the log for `[larva-dev]` results and ` E ` errors, take screenshots where the story asks.
5. For FR-17: copy, check, then `exit` and delete the test replays (see "Simulate a new replay").
6. `exit`, and confirm `Larva Seconds Lost module stopped.` in the log.

## Test the online update locally (E18)

Verified 2026-11-25 (3.0 to 3.0.1). Point the module at a local `module.xml` instead of GitHub Pages:

1. Temporarily set `version.revision=1` in `LarvaSecondsLostExtMod/release/release.properties`, run `ant BUILD_RELEASE`, copy `release/deployment/LarvaSecondsLost-3.0.1.zip` and `module.xml` to a scratch folder, and set the version back to `0`.
2. In the scratch copy of `module.xml`, change `<urlList>` to `http://127.0.0.1:8765/LarvaSecondsLost-3.0.1.zip`. The URL is not part of any hash.
3. Serve the folder: `python3 -I -m http.server 8765 --bind 127.0.0.1` (in the background).
4. Stop Scelight, run `ant INSTALL_DEPLOYMENT` (installs 3.0), then start Scelight with `-Dlarva.update.moduleXmlUrl=http://127.0.0.1:8765/module.xml` added to `JAVA_TOOL_OPTIONS`.
5. `select-larva`, `update-check`, `update-state` (AVAILABLE), `update-install`, `update-state` (INSTALLED), `exit`. Start again: `ping` must answer `3.0.1`, and the log shows `removed old module folder .../3.0`.
6. Clean up: stop Scelight, stop the server (kill its PID; `pkill -f` also matches your own shell), delete `~/.config/scelight/mod-x-cache/larva-seconds-lost/update.properties`, and run `ant INSTALL_DEPLOYMENT` again.


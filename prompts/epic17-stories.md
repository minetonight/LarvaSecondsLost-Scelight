# Epic 17 — Implementation stories

| | |
|---|---|
| **Epic** | [prompts/epic17-multi-replay-analysis.md](epic17-multi-replay-analysis.md) (FR-*, AC-*, D-*, OQ-*, WP-*, R-*) |
| **Draft** | [prompts/epic17-draft.md](epic17-draft.md) |
| **Created** | 2026-11-24 |
| **Status** | Ready for implementation |
| **Precedence** | 1. OQ decisions in epic §6 (user-edited, binding) → 2. this file → 3. the rest of the epic. Where they conflict, §0.3 lists the resolution. |

---

## 0. Read this first

### 0.1 Scope additions vs. epic

> **SCOPE ADDITION: auto-open new Zerg replays (proposed FR-17). The epic has no FR for this.**
>
> OQ-10 (user decision): *"Keep the listener! It auto-creates a sub-page for a newly detected zerg replay."* The draft also says: *"OQ-10: add the function that auto opens a replay upon new replay detected."* No FR, AC or WP in `epic17-multi-replay-analysis.md` covers it. WP-06 only says "Apply the replay monitor decision (OQ-10)".
>
> - It is implemented in its own story, **E17-S07**. **E17-S06 (WP-06) only removes the old singleton auto-update.** S06 must not build the auto-open feature.
> - The requirement has been added to the epic as FR-17 (§3 C). Text for reference:
>
> **FR-17 Auto-open new Zerg replays.** When Scelight's replay folder monitor reports a new replay (`INewRepListener.newRepDetected`) that contains at least one Zerg player, the module adds a single-replay sub-page for it under the `Larva` hub. The page uses the FR-3 name and icon and the FR-4 content, and its analysis runs in the background.
> - AC-17.1 Given Scelight is running with the module loaded, when a new replay with a Zerg player is saved into a monitored folder, then a child node named after the file (extension stripped) with `larva-replay-icon.png` appears under `Larva`. It shows busy first, then the normal single-replay content.
> - AC-17.2 A new replay with no Zerg player creates no page. "Analyze Latest Replay" can still resolve it.
> - AC-17.3 If a single-replay page for the same file is already open, no second page is created and that replay is not analyzed again (OQ-6).
> - AC-17.4 The new page is selected only under the focus rule (S07 default D17-c). Otherwise it is only added to the tree.
> - AC-17.5 A flood of new replays creates at most the capped number of pages (S07 defaults D17-e). Skipped replays are logged.
> - AC-17.6 No replay parsing happens on the EDT or on Scelight's replay-handler job thread. The listener returns immediately.
> - AC-17.7 Already open pages never change their replay (FR-4).
> - AC-17.8 A replay detected before the hub is installed, or after module `destroy()`, does not create a page and does not throw.

Other clarifications that are in the epic's OQ table but not in its FR or D text:
- **OQ-2 favored player:** Scelight's favored-player setting is **not** used. Tracked players come from D-5/D-6 only (toon identity, name from the most recent eligible game).
- **OQ-14 confirm dialog above 500 files:** E17-S14.
- **OQ-12 strip the CLDR `г.` suffix:** E17-S14 date label helper (`LarvaShortDateFormatter`, used by E17-S15).

### 0.2 Binding decisions (implementers must honor)

| OQ | Decision |
|---|---|
| OQ-1 | Always a 2-column grid. Player 1 left, player 2 right. **One player → left column only**, right column empty (same widths as the two-player case). |
| OQ-2 | Tracked players: D-6 (count ≥ 3 **and** ≥ 20 % for #2; ties by recency, then name). Identity = toon key, display name = name in the most recent eligible game (D-5). **No favored-player lookup** (E17-S10). |
| OQ-3 | The milestone table is its own table per player, **below the 4 larva tables** (All, ZvP, ZvT, ZvZ). Columns All / ZvP / ZvT / ZvZ. Cells `avg m:ss (k/n)`. |
| OQ-4 | Matchup tables with 0 games are shown with title `ZvX — 0 games` and all cells `n/a`. |
| OQ-5 | "4 minutes" = **5 376 loops** (22.4 loops/s × 240). Named constant. Eligible iff `lengthLoops >= 5376`. |
| OQ-6 | Node name = file name **without** `.SC2Replay` (case-insensitive). A button click (Open, Analyze Latest) opens a new page, **unless a page for that replay is already open: then that page is only focused**, no duplicate. Same for auto-open (S07). An open page is never re-rendered with another replay's data (FR-3 AC-3.4, FR-4). |
| OQ-7 | Multi page label while running is `Multi analysis (running…)`, renamed to `from <start> to <end>` on completion **if WP-01 proves renaming works**. Otherwise fixed label `Multi analysis #k` and dates only in the header. This refines FR-7. |
| OQ-8 | `Ladder games only` is not persisted. It is checked every time the hub is created. |
| OQ-9 | File selection only. No folders. |
| OQ-10 | **Keep `INewRepListener`.** It auto-creates a single-replay sub-page for new Zerg replays (FR-17, E17-S07). |
| OQ-11 | Supply used = tracker `PlayerStats.foodUsed` / 4096. Never `foodMade`. |
| OQ-12 | `DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())`. Strip trailing non-digit tokens (e.g. `г.`). |
| OQ-13 | Same four phase columns. `n/a` when no player-game reached the phase. Phase length shows `0/n`. |
| OQ-14 | No hard limit. Confirm dialog above 500 files with an estimated duration. |
| OQ-15 | Parse failure → excluded and listed. Missing phase table for a tracked player → that player-game is excluded and listed with reason `missing-phase-table`. |

D-1 … D-11 (epic §5) are binding unless a story says otherwise.

### 0.3 Conflicts found and how this file resolves them

| # | Epic text | Conflicting decision | Resolution used in stories |
|---|---|---|---|
| C-1 | *(resolved in the epic)* FR-14 now matches OQ-3 | — | Order: heading → All → ZvP → ZvT → ZvZ → Milestones (S15). |
| C-3 | FR-2 / AC-2.1: "Creating one selects it" | OQ-10 / FR-17 auto-open must not steal focus without reason | User-initiated pages are always selected. Auto-opened pages follow the focus rule D17-c (S07). |
| C-4 | §4.1, WP-06: "Decide what the replay monitor does now (OQ-10)"; FR-4 "no auto-latest" | OQ-10: keep the listener, auto-create pages | S06 strips the listener down to recording `lastDetectedReplayPath`. S07 adds auto-open. FR-4 still holds: existing pages never switch replay. |
| C-6 | FR-6 icon name `larva-multy-analysis-icon.png` (also in the draft) | FR-16 and the real file: `icon/larva-multi-analysis-icon.png` | Use `larva-multi-analysis-icon.png`. |
| C-9 | D-8 discards `LarvaAnalysisReport` | D-11 needs that report's `loopToTimeMs` (real-time setting, game speed) | The per-game record stores display-ms values converted at extraction time (S09). |
| C-10 | D-5 "Key = toon (`IToon` string)" | `IToon.toString()` appends the player name when present | The identity key is built from `regionId-programId-realmId-id` only (S09). |
| C-11 | §7 phasing has no slot for FR-17 | OQ-10 | FR-17 goes in Phase 1 after S06 (S07). |

IDs C-2, C-5, C-7 and C-8 were removed after review: FR-3 and OQ-6 describe two different cases (new page vs. focus an already open one), FR-7 is refined by OQ-7, and the favored-player lookup was dropped. FR-17 is now in the epic. Implementers must not edit the epic.

### 0.4 Global implementer rules

**Language and API**
- **Java 7 language and API level.** `build.xml` compiles with `source="1.7" target="1.7"`. No lambdas, method references, streams, `Optional`, `java.time`, `String.join`, `Map.getOrDefault/computeIfAbsent/putIfAbsent`, `List.of`, or default methods. A newer JDK compiles Java 8 API calls without complaint, so **you** must avoid them. Allowed: diamond, try-with-resources, `java.util.concurrent`, `java.util.Objects`, anonymous inner classes.
- SDK 1.5.2 API only (`scelight-ext-mod-api-1.5.2.jar`). Do not modify anything under `scelight/` (reference source only). Reflection is allowed only for **read-only** lookups with a graceful fallback (precedent: `LatestReplayResolver`), never for page or tree management.
- Keep the existing flat package `hu.aleks.larvasecondslostextmod`. Package-private access between module and components is the established pattern. Use clear class-name prefixes (`LarvaHub…`, `LarvaMulti…`, `LarvaAggregation…`).

**Threading (EDT)**
- All Swing and tree work runs on the EDT: `addPage`, `rebuildPageTree`, `selectPage`, hub child-list mutation, every component update.
- Replay parsing and analysis **never** run on the EDT or on Scelight's `NewReplayHandlerJob` thread (the thread that calls `INewRepListener`).
- `guiUtils.runInEDT(r)` is **`invokeAndWait`** when called off the EDT. Never call it while holding a lock. For high-frequency progress posts, prefer `SwingUtilities.invokeLater` and throttle to ≥ 100 ms apart (always post the final state).
- Today `requestReplayAnalysis` calls `pageComp.showBusy(...)` directly on the monitor thread. That is an EDT violation and must disappear with S06.
- No shared mutable state between pages. One `ReplaySummaryService` per worker thread (it holds a non-thread-safe `SimpleDateFormat` and a `LarvaReplayAnalyzer`).

**Contracts that must not change**
- **Epic 13 benchmark contract is frozen.** Do not change `LarvaBenchmarkReplayRecord`, `LarvaBenchmarkPlayerRecord`, `LarvaBenchmarkPhaseRecord`, `LarvaBenchmarkJsonSerializer`, `LarvaBenchmarkRecordFormatter`, `LarvaBenchmarkExtractionService` output, or the existing `LarvaBenchmarkCliExtractor` flags and output bytes. New CLI modes are additive only.
- **Metric definitions are frozen.** Do not change the `LarvaPhaseStats` getters, `LarvaGamePhase` promotion thresholds, `LarvaPhaseBenchmarkRatingFormatter` anchors, or the analyzer's larva/inject logic. Single-page golden output must stay identical (verify with the dev dump: `-Dlarva.dev.dump.enabled=true`, which writes `LarvaTimelineGoldenFormatter` output per analysis).

**Logging**
- Use the module `ILogger` (`module.logger` / service `logger`). Use `debug` for expected events (filter exclusions, dedupe hits, cap skips, cancellation) and `error` with the throwable for unexpected failures, always with the replay path.
- No `System.out` in module code (the CLI may use stderr / `StdErrLogger`). Add no new files under `/tmp`.

**Tests**
- Sources go in `LarvaSecondsLostExtMod/test/hu/aleks/larvasecondslostextmod/…` (same package as production, so package-private members are reachable).
- Jars go in **`LarvaSecondsLostExtMod/test-lib/`**, never `lib/`. `COMPILE` puts `lib/**/*.jar` on the classpath, and `BUILD_RELEASE` copies `lib/**/*.jar` into the release.
- Compile tests to `bin-test/`. Run with `ant TEST` (E17-S03).
- Unit tests use synthetic data only and need no Scelight runtime. Anything that parses replays is a golden, integration or manual check.

**Resources**
- Icons are `icon/larva-module-icon.png` (hub), `icon/larva-replay-icon.png` (single), `icon/larva-multi-analysis-icon.png` (multi), loaded in `loadResources()`. A missing resource must fail init through the existing `buildInitFailureMessage` path (AC-16.2). Ignore the `*-old.png` files.

**Running Scelight as an agent**
- Follow [knowhow/running-scelight.md](../knowhow/running-scelight.md): deploy, start, wait for the startup log line, drive the module through the dev command channel (E17-S00), read the log, take screenshots, stop with the `exit` command.
- Every story that adds a user action (button, page, close, chooser) also adds the matching dev command (see E17-S00 "Commands added by later stories"), so agents can check it without clicking.
- Real clicks, file-chooser dialogs and look-and-feel stay as short manual checks for the user.

**Done means**
- `ant BUILD_RELEASE` passes, `ant TEST` passes, the module loads in Scelight with no init errors, the story's dev-command checks pass in a running Scelight, and the story's manual checks are ticked with notes or screenshots in the PR.

---

## 1. Story index

| ID | Title | Size | Depends on | Phase |
|---|---|---|---|---|
| E17-S00 | Dev command channel for agent-driven checks in a running Scelight | S | — | 0 (first) |
| E17-S01 | Spike: module-owned hub `IPage` with closable, renamable children | S | — | 0 |
| E17-S02 | Spike: batch parse cost, cheap metadata, threading model | S | — | 0 |
| E17-S03 | JUnit test harness and Ant `TEST` target | S | — | 0 |
| E17-S04 | Larva hub page and page navigator | M | S01 | 1 |
| E17-S05 | Hub UI (centered buttons, ladder checkbox, tip icon) | S | S04 | 1 |
| E17-S06 | Single-replay sub-pages (per-page binding, remove singleton auto-update) | L | S04, S05 | 1 |
| E17-S07 | **[FR-17, new scope]** Auto-open sub-page for newly detected Zerg replays | M | S06, S02 | 1 |
| E17-S08 | Milestone extraction (100/200 supply, 66/80 drones) | M | S03 | 2 |
| E17-S09 | Per-game aggregation record and headless extraction path | M | S02, S08 | 2 |
| E17-S10 | Eligibility filter and tracked-player selection | S | S03, S09 | 2 |
| E17-S11 | Aggregation engine (pooled tables, phase length, milestones) | M | S10 | 2 |
| E17-S12 | Batch runner and headless `--multi` dump | M | S09, S11 (CLI part), S02 | 3 |
| E17-S13 | Reusable phase-table renderer | M | S03 | 2 |
| E17-S14 | Multi-analysis page: chooser, creation, progress, label, cancel | M | S01, S05, S12 | 3 |
| E17-S15 | Multi-analysis page: results layout and tables | L | S11, S13, S14 | 3 |
| E17-S16 | Help, knowhow, performance run and release validation | S | all | 4 |

Total ≈ 16–21 dev days (epic estimate 12–16 + FR-17 + split of WP-13).

### 1.1 WP mapping

| WP | Story | | WP | Story |
|---|---|---|---|---|
| WP-01 | S01 | | WP-08 | S09 |
| WP-02 | S02 | | WP-09 | S10 |
| WP-03 | S03 | | WP-10 | S11 |
| WP-04 | S04 | | WP-11 | S12 |
| WP-05 | S05 | | WP-12 | S13 |
| WP-06 | S06 (without FR-17) | | WP-13 | S14 + S15 |
| — (FR-17) | S07 | | WP-14 | S16 |

### 1.2 Dependency and ordering diagram

```
Phase 0 (first)       S00 dev command channel  (every later story adds its commands to it)
Phase 0 (parallel)    S01 spike-pages      S02 spike-batch      S03 test-harness
                        │                    │      │              │
Phase 1 (UI track)    S04 hub+navigator      │      │              │
                        ├──► S05 hub UI      │      │              │
                        │      │             │      │              │
                        └──► S06 single pages◄┘      │              │
                               │                    │              │
                               └──► S07 FR-17 auto-open◄(S02)       │
                                                    │              │
Phase 2 (data track)                                │   S08 milestones◄┘
                                                    └──►S09 per-game record◄─S08
                                                        S10 filter+selector◄─S09
                                                        S11 aggregator◄─S10
                      S13 phase-table renderer◄─S03   (parallel to S08–S11)
Phase 3               S12 batch runner + --multi dump◄─S09 (+S11 for the dump)
                      S14 multi page shell◄─S01,S05,S12
                      S15 multi page results◄─S11,S13,S14
Phase 4               S16 help / perf / release◄─all
```

Critical path: S01 → S04 → S05 → S14 → S15 → S16, joined by S08 → S09 → S10 → S11 → S12. The UI track (S04–S07) and data track (S08–S11, S13) can be staffed in parallel after Phase 0.

**Checkpoints**
- After S01, go or no-go on module-owned `IPage` (affects S04, S06, S07, S14).
- After S06, single-page parity with the pre-epic version (AC-4.1).
- After S11, aggregation numbers are signed off on synthetic data before S15.

---

## 2. Phase 0 — Spikes and harness

### E17-S00 — Dev command channel for agent-driven checks in a running Scelight

| Maps to | Dev tooling (no FR). Enables automated checks for FR-1–FR-17 · user decision 2026-11-24 |
|---|---|
| Size | S |
| Depends on | — (do this first; S01 already uses it) |

**Technical goal.** Let an AI agent drive the module inside a running Scelight without clicking: open, select and close pages, run the same actions as the buttons, dump the page tree, and exit Scelight cleanly. Results go to the Scelight log so the agent can verify them.

**Context.**
- Today an agent can start Scelight (`startScelight.sh`), read `~/.config/scelight/logs/<newest>.log` and take window screenshots (`xwininfo` + `import`). See [knowhow/running-scelight.md](../knowhow/running-scelight.md).
- It **cannot stop Scelight cleanly**: SIGTERM kills the JVM without Scelight's exit sequence, so the module `destroy()` never runs and settings are not saved. `xdotool` / `wmctrl` are not installed (KDE Wayland, Scelight runs on XWayland).
- It cannot click buttons reliably.
- The existing dev switch pattern is `DevDiagnosticDumpWriter` (`-Dlarva.dev.dump.enabled`, `-Dlarva.dev.dump.file`). Follow the same style.

**Scope.**
- New class `LarvaDevCommandChannel`, started in module `init()` **only** when the system property `larva.dev.commands.file` is set. Without the property: no thread, no file access, no behaviour change.
- A daemon thread polls the command file every 500 ms and processes **new lines only** (remember the byte offset; a truncated file restarts at 0). Existing lines at startup are skipped.
- Line format: `<id> <command> [args…]`, for example `7 open-replay /path/to/x.SC2Replay`. Arguments that contain spaces are double-quoted. Blank lines and `#` comments are ignored.
- Every command runs its UI part on the EDT and calls the **same handler methods the buttons use** (no duplicated logic).
- Every command writes exactly one result line to the module log at `info` level:
  `[larva-dev] <id> OK <command> <short result>` or `[larva-dev] <id> FAIL <command> <reason>`.
  Longer output (tree dumps) follows as extra `[larva-dev] <id> | …` lines.
- Commands delivered by this story:

| Command | Effect |
|---|---|
| `ping` | `OK ping <module version>`. Proves the channel is alive. |
| `dump-tree` | Logs the `Larva` node and its children in order: label, page type, icon name, closable, selected marker. Before S04 it logs only the `Larva` page and whether it is selected. |
| `select-larva` | Selects the `Larva` page. |
| `window-info` | Logs the Scelight main frame title, bounds and X window id hint (title) so the agent can screenshot it. |
| `exit` | Closes Scelight's main window through its normal close path (`WINDOW_CLOSING` on the main `JFrame`, found via `Frame.getFrames()`), so Scelight runs its exit sequence and calls module `destroy()`. |

- Commands added by later stories (each story owns its own commands and their tests):

| Story | Commands |
|---|---|
| S04 | `dump-tree` with sub-pages, `select <label>`, `close <label>` (same path as the X button) |
| S05 | `hub-state` (button labels, checkbox state, tooltip text), `set-ladder-only <true/false>` |
| S06 | `open-replay <path>` (bypasses only the file chooser), `analyze-latest`, `refresh <label>`, `page-text <label>` (header + phase table + text details of that page) |
| S07 | `auto-open-state` (queue length, cap, last decisions). FR-17 itself is tested by copying replays into the monitored folder. |
| S14 | `multi <ladderOnly> <file…>` (bypasses only the chooser), `multi-cancel <label>`, `multi-state <label>` (progress, label) |
| S15 | `page-text <label>` for multi pages (header, filter summary, every table as text) |

**Out of scope.** Network or socket listeners. Any command in release builds without the property. Replacing manual checks of real clicks and dialogs.

**Implementation notes.**
- Java 7. Read with `RandomAccessFile` (or a `FileInputStream` skip). UTF-8.
- Unknown command → `FAIL <command> unknown command`. Exceptions in a command are logged with the stack trace and reported as `FAIL`. The channel thread must never die.
- `destroy()` stops the thread (interrupt + flag) before the module's other shutdown steps.
- The main frame lookup: the visible `JFrame` whose title starts with `Scelight`. If Scelight asks for confirmation or minimizes to tray on close, record it in the knowhow file and handle it (for example, a second `exit` or the confirm dialog's default button). Verify on the real app.
- Keep the command set small and documented in the knowhow file's command table.

**Acceptance criteria.**
1. Given Scelight started **without** `larva.dev.commands.file`, then no channel thread exists and the log has no `[larva-dev]` lines.
2. Given the property points to an empty file, when `1 ping` is appended, then within 2 s the log has `[larva-dev] 1 OK ping <version>`.
3. Given `2 dump-tree`, then the log lists the `Larva` page and its selection state.
4. Given `3 foo`, then `[larva-dev] 3 FAIL foo unknown command`, and a following `4 ping` still works.
5. Given `5 exit`, then Scelight exits normally: the log ends with `Larva Seconds Lost destroy sequence completed.` and `Larva Seconds Lost module stopped.`, and the Java process ends within 15 s.
6. Lines present in the file before Scelight started are not executed.

**Tests required.** (Done in S03: JUnit tests `LarvaDevCommandLineTest` and `LarvaDevCommandFileTailTest` exist in `test/`, run with `ant TEST`.) Unit: line parsing (ids, quoted args, comments, blanks) and offset handling with a temp file. Integration: criteria 1–6 in a running Scelight, using the knowhow procedure.

**Definition of done.**
- [x] `LarvaDevCommandChannel` added, off by default.
- [x] Knowhow file updated with the final start command and the command table.
- [x] Criteria 1–6 checked in Scelight by the agent, log excerpts in the PR.

---

### E17-S01 — Spike: module-owned hub `IPage` with closable, renamable children

> **S01 result (2026-11-24): GO for nesting and GO for rename.** All of Q1-Q8 answered in [knowhow/epic17-subpages-spike.md](../knowhow/epic17-subpages-spike.md). S04 builds module-owned `IPage` hub and sub-pages as specified; S14 uses the dated label via `setDisplayName` + `rebuildPageTree(false)` (no `Multi analysis #k` fallback; keep the tree label short, long labels are cut off in the nav tree). Lazy component creation confirmed: S06/S07 keep state in a per-page controller. `rebuildPageTree` re-fires `pageSelected()` on the current page, so it must be idempotent. Sub-pages must be leaves (closing a page with children throws). Spike code (`LarvaSpike*`, `spike-*` commands) lives in the working tree; `LarvaSpikeHubPage` already replaces the `newPage("Larva")` page. X button, real Ctrl+F4 key and mouse history clicks are left as manual checks (list in the knowhow note).

| Maps to | WP-01 · FR-2, FR-7, FR-16 · OQ-6, OQ-7, OQ-10 · R-1, R-2 |
|---|---|
| Size | S (≤ 1 day) |
| Depends on | — |

**Technical goal.** Prove in real Scelight (SDK 1.5.2) that the module can own a non-closable hub page with a mutable child list, and that children can be added, selected, closed (X and `Ctrl+F4`), disposed and relabelled. Record exact host behavior so S04/S06/S07/S14 can be built without guessing.

**Context.**
- Today `LarvaSecondsLostModule.installReplayPage()` adds `guiFactory.newPage("Larva", larvaIcon, false, creator)` via `getMultiPageComp().addPage(...)` and then calls `rebuildPageTree(false)`.
- `IPage` (`scelight/src-launcher/hu/scelightapibase/gui/comp/multipage/IPage.java`) has: `getDisplayName`, `getRicon`, `isCloseable`, `setParent`, `getParent`, `getChildList`, plus `createPageComp` (from `IPageCompCreator`). The API has no `addChild`.
- `IMultiPageComp` has: `addPage`, `rebuildPageTree(boolean)`, `selectPage`, `getSelectedPage`, `clearHistory`.
- Scelight's own pattern is `RepAnalyzersPage.newRepAnalyzerPage(file, select)`: `addChild` → `rebuildMainPageTree()` → optional `selectPage`.

**Questions to answer (each needs a yes/no plus observed detail):**
1. Does `addPage(new LarvaHubPage(...))` with a module-owned `IPage` (mutable `ArrayList` children) render correctly as a tree node, with and without children? Is the hub selectable and does it show its own component when it has children?
2. Does adding a child (`child.setParent(hub)`, `hub.getChildList().add(child)`, `rebuildPageTree(false)`, `selectPage(child)`) show the child indented and selected? Is the hub auto-expanded?
3. Can children be **module-owned `IPage` impls** (closeable = true, own `createPageComp`)? Do `guiFactory.newPage(..., true, ...)` children also work? Which one should we use? (Recommendation: module-owned, needed for rename and for type info.)
4. Is `createPageComp()` called lazily (only on first selection)? Is the component cached across `rebuildPageTree(false)`? What happens to a child **added without selection** (needed for S07 D17-c)?
5. Close: Does X / `Ctrl+F4` on a child call `IPageClosingListener.pageClosing()` and `IPageDisposedListener.pageDisposed()` on the component, remove the child from **our** list, and select the previous page from history or the hub? Is the X disabled on the hub? Confirm `UnsupportedOperationException` if a page with children is closed (the hub must stay non-closable).
6. Rename: Does a mutable `getDisplayName()` plus `rebuildPageTree(false)` update the **tree label** and the **page title bar** (selected and unselected cases)? Does the rebuild re-fire `IPageSelectedListener.pageSelected()`, and does selection stay on the current page?
7. Icons: Does each node show its `IRIcon` in the tree and title bar at 16 px and at larger title font sizes?
8. Is there a race if a child is added while the user is on another page, or when two children are added in quick succession?

**Deliverable.**
- `knowhow/epic17-subpages-spike.md` with the answers, a minimal code sketch of the working pattern, and screenshots `knowhow/epic17-spike-*.png`.
- Throwaway code stays on a spike branch and is **not** merged.

**Go / no-go.**

| Outcome | Downstream change |
|---|---|
| **GO** (Q1, Q2, Q3, Q5 OK) | S04 implements `LarvaHubPage` + `LarvaSubPage` as module-owned `IPage`s as specified. |
| **NO-GO on nesting** | R-1 contingency: S04 adds top-level closable pages named `Larva: <name>` via `addPage`. Close removes from the top-level list (verify the mechanics in the spike). S04, S06, S07 and S14 acceptance criteria that say "under `Larva`" become "top-level, prefixed `Larva:`". The hub stays as the first top-level page. |
| **Rename does not work** (Q6) | S14 uses the OQ-7 fallback `Multi analysis #k` (k = per-session counter). FR-7 date label only in the header. AC-7.x are tested against the header instead. |
| **Lazy component creation confirmed** (Q4) | S06/S07 must keep page state in a controller independent of the component (already the recommended design). |

**Acceptance criteria.**
1. Given the spike build loaded in Scelight, when the questions above are run, then every question has a recorded answer with the observed behavior.
2. The knowhow note states GO or NO-GO for nesting and for rename, with the downstream changes above confirmed or amended.

**Tests required.** Manual only, in Scelight.

**Definition of done.**
- [x] `knowhow/epic17-subpages-spike.md` written with answers to Q1–Q8 and the go/no-go (not committed: no commits requested).
- [x] Screenshots saved (`knowhow/epic17-spike-*.png`, not committed).
- [x] Downstream story impacts noted in the S01 result note above; GO, so no S04/S14 acceptance-criteria edits are needed (§1.2 unchanged).

---

### E17-S02 — Spike: batch parse cost, cheap metadata, threading model

> **S02 result (2026-11-24): GO. Cheap metadata available, 1 worker, pre-filter yes.** Details, command lines and numbers in [knowhow/epic17-batch-spike.md](../knowhow/epic17-batch-spike.md). Full analysis is about 17 ms per replay (median 10, p90 40, max 190; whole library of 8 352 files: mean 10 ms), roughly 90 times under the 1.5 s budget; parsing is 75 % of it. The cache-only `getRepProc` processor exposes format, game mode, date, player races and toons and header loops, **identical** to the full parse in all 8 712 comparisons (0.2 to 0.9 ms versus 8 to 12 ms), but a cache **miss** makes that call a full parse (about 40 ms) that returns events. The pre-filter saves 51 % of the time on the user's library (43 % of files pass). Parser, analyzer and `RepProcCache` are safe with 2 and 4 threads (byte-identical output), but speed does not need it: **S12 uses 1 worker** (`BATCH_WORKER_COUNT = 1`, one service per worker). **`ESTIMATED_MS_PER_REPLAY = 50`** (S14, OQ-14). Progress posts must be throttled (up to 200 files per second). Gate `writePos2jlLog` and `logSpawnLarvaCommands` anyway (no measurable cost, but unbounded `/tmp` and log growth). Re-baselined S16 target: 100 replays ≤ 15 s, 500 replays ≤ 60 s, heap growth ≤ 200 MB. R-3 and R-4 are closed. Spike code: `LarvaSpikeBatchCommands` (`spike-batch-*` commands) and the harness copy in `knowhow/epic17-batch-spike/`.

| Maps to | WP-02 · FR-10, FR-15, FR-17 · §9 performance · R-3, R-4, R-6 |
|---|---|
| Size | S |
| Depends on | — |

**Technical goal.** Measure full per-replay analysis cost and memory, find out which metadata the cheap (cache-only) `repParserEngine.getRepProc(path)` exposes, and decide the threading model for the batch runner (S12) and the auto-open pre-check (S07).

**Context.**
- `ReplaySummaryService.analyzeReplayContext()` calls `getRepProc` and then **reparses** via `parseAndWrapReplay` when tracker or game events are missing (cache-only processor).
- `LarvaBenchmarkCliExtractor` + `StandaloneScelightBootstrap` provide a headless harness (flags `--replay-file`, `--scelight-app-dir`, `--debug`).
- Debug side effects: `LarvaReplayAnalyzer.writePos2jlLog` appends to `/tmp/larva-pos2jl.log`, and `ReplaySummaryService.logSpawnLarvaCommands` logs every inject command.

**Questions to answer.**
1. Wall time per replay for the full path (`getRepProc` + reparse + `LarvaReplayAnalyzer.analyze`) over ≥ 50 replays (`benchmark-data/queensMoved/`, local `benchmark-data/raw-replays/`): mean, p90, max. Is it within ≤ 1.5 s per replay?
2. Does the **cache-only** `IRepProcessor` (no events) expose each of the following, and are the values identical to the full parse?
   - `getFormat()`
   - `getReplay().getAttributesEvents().getGameMode()`
   - `getReplay().getDetails().getTime()`
   - `getReplay().getDetails().getPlayerList()` with each player's `getRace()` and `getToon()`
   - a length value in loops (state which getter)
3. How long does the cheap call take versus the full one?
4. Heap retained by one `LarvaAnalysisReport` / `LarvaReplayPageSummary` (rough, via heap histogram), and peak heap over 100 sequential analyses when only a compact record is kept.
5. Is `LarvaReplayAnalyzer` safe to **reuse sequentially** in one service instance (no state leaks between replays)? Is it safe to run **two service instances on two threads** concurrently (is `IRepParserEngine` thread-safe)? Check the code and test with 2 threads on 50 replays, comparing results byte-for-byte with a sequential run.
6. Cost of the `/tmp/larva-pos2jl.log` writes and of `logSpawnLarvaCommands` per replay (on/off).

**Deliverable.** `knowhow/epic17-batch-spike.md` with:
- the measurement table
- the metadata availability matrix (cheap vs. full)
- the threading decision
- the recommended `ESTIMATED_MS_PER_REPLAY` constant for the OQ-14 dialog

Scratch code may live in the CLI behind a hidden flag on a spike branch. Do not merge.

**Go / no-go and downstream impact.**

| Outcome | Downstream change |
|---|---|
| Cheap metadata **available** (format, mode, date, races, length) | S12 pre-filters before the full reparse (not 1v1 / non-ladder / < 5 376 loops / no Zerg → excluded without a full parse). S07 uses the cheap call for the Zerg check. |
| Cheap metadata **missing or unreliable** | S12 and S07 use the full parse for every file. Filter counts are computed after parsing. §9 target is re-baselined in the knowhow note. |
| Mean ≤ 1.5 s and the parser is **not** safe for 2 threads | S12 uses a single worker thread. |
| Mean > 1.5 s and 2 threads are safe and faster | S12 uses a fixed pool of 2 workers, each with its own `ReplaySummaryService`. Results are merged in selection order. |
| Debug side effects cost > 5 % | S12 must gate `writePos2jlLog` and `logSpawnLarvaCommands` (default: gate anyway, R-6). |

**Acceptance criteria.**
1. Given ≥ 50 replays, when the measurement runs, then the knowhow note lists mean / p90 / max time and peak heap.
2. Every field in Q2 is marked available / missing / differs, with evidence.
3. A single threading decision (1 worker or 2) is recorded with rationale.

**Tests required.** Manual or headless measurement runs. Record the command lines.

**Definition of done.**
- [x] `knowhow/epic17-batch-spike.md` written: measurement table (≥ 50 replays, 460 headless and 8 352 in Scelight), metadata matrix, threading decision, `ESTIMATED_MS_PER_REPLAY`, exact command lines (not committed: no commits requested).
- [x] Every Q2 field marked available / missing / differs with evidence (all available and identical on a cache hit).
- [x] One threading decision recorded (1 worker) with rationale.
- [x] Downstream impacts noted in the S02 result note above. S07, S09, S12, S14, S16 gain concrete numbers; no acceptance criteria had to be changed (S16 target re-baselined in the note).

**Definition of done.**
- [ ] `knowhow/epic17-batch-spike.md` committed.
- [ ] S07, S09 and S12 notes amended if the outcome changes their approach.

---

### E17-S03 — JUnit test harness and Ant `TEST` target

| Maps to | WP-03 · all FRs (enabler) · DoD "release jar contains no test classes" |
|---|---|
| Size | S |
| Depends on | — |

**Technical goal.** Add a unit-test setup that compiles at the Java 7 level, runs with one Ant target, and can never leak into the release.

**Context.**
- `build.xml` targets: `COMPILE` (javac `srcdir=src`, classpath = API jar + `lib/**/*.jar`), `BUILD_RELEASE` (copies `lib/**/*.jar` into the release and jars `bin/`), `INSTALL_DEPLOYMENT`.
- There are no tests and no JUnit today.

**Scope.**
- `LarvaSecondsLostExtMod/test-lib/junit-4.13.2.jar` + `hamcrest-core-1.3.jar` (or JUnit 4.12). Add a `test-lib/README.txt` with versions and source URL.
- `LarvaSecondsLostExtMod/test/hu/aleks/larvasecondslostextmod/` source root.
- Ant targets:
  - `COMPILE_TESTS` (depends `COMPILE`, javac `source/target 1.7`, `srcdir=test`, `destdir=bin-test`, classpath = API jar + `lib` + `test-lib` + `bin`)
  - `TEST` (depends `COMPILE_TESTS`, runs all `*Test` classes, fails the build on any failure)
- Prefer `<java classname="org.junit.runner.JUnitCore" fork="true" failonerror="true">` with an explicit class list built via `<pathconvert>`/`<fileset>`. That avoids needing the `ant-junit` optional task. If `<junit>` is used instead, document the requirement.
- One smoke test, `LarvaPhaseStatsTest`: rate getters against hand-computed values, `null` when the denominator is 0. This doubles as a check of D-1 assumptions.
- `CLEAN` also deletes `bin-test`.

**Out of scope.** Code coverage tooling, CI configuration, tests that need a Scelight runtime.

**Implementation notes.**
- `BUILD_RELEASE` must not depend on `TEST`. Keep release builds independent, and document `ant TEST BUILD_RELEASE` as the release routine in S16.
- Use package-private test helpers (e.g. `LarvaTestFixtures`) under `test/` for synthetic `LarvaPhaseStats`, snapshots and records. Later stories extend it.

**Acceptance criteria.**
1. Given a clean checkout with the SDK present, when `ant TEST` runs, then the smoke test compiles at 1.7 and passes, and the build is green.
2. Given a deliberately failing assertion, when `ant TEST` runs, then the build fails.
3. Given `ant BUILD_RELEASE`, then the produced `${folder}.jar` and release folder contain no class from `test/` and no `junit`/`hamcrest` jar (check with `unzip -l`).
4. A test that uses a lambda fails to compile (proves the 1.7 source level).

**Tests required.** The smoke test itself, plus a manual check of the release jar contents.

**Definition of done.**
- [x] `test-lib/`, `test/`, `COMPILE_TESTS`, `TEST` added. `CLEAN` updated.
- [x] Release jar verified free of test classes and jars.
- [x] Short "How to run tests" section added to `knowhow/testing.md`.

---

## 3. Phase 1 — Navigation (UI track)

### E17-S04 — Larva hub page and page navigator

> **S04 result (2026-11-24): code, unit tests and agent real-app checks done (AC-1..AC-5).** Added `LarvaHubPage`, `LarvaSubPage`, `LarvaSubPageKind`, `LarvaPageNavigator`, dev commands `select`, `close`, `add-test-page` (the AC-2 test hook) in `LarvaDevPageCommands`, and `LarvaDevUiProbe` (trimmed S01 probe, only finds the host close button). S01 spike classes and `spike-*` UI commands deleted (`LarvaSpikeBatchCommands` stays for S12). `build.xml`: `testClasspath` moved into `INIT_PROPERTIES` because the top-level definition did not see `${extModApiJar}` (tests now may use SDK types). Sub-pages return `null` from `getChildList()` (leaf, as in the spike). AC-1 note: the host X is disabled when `Larva` is selected (`dump-tree` `hostCloseEnabled=false`, `close Larva` FAILs "close button disabled"). Startup selects `Replay Folders`, not Larva.

| Maps to | WP-04 · FR-2, FR-16 · AC-2.1–2.4, AC-16.1, AC-16.2 · R-1 |
|---|---|
| Size | M |
| Depends on | S01 (GO/NO-GO shapes this story) |

**User story.** As a Zerg player, I want `Larva` to be a stable hub with child pages I can close individually, so that I can keep several analyses open like Scelight's Replay Analyzers.

**Context (today).**
- `LarvaSecondsLostModule.installReplayPage()` creates one non-closable `newPage("Larva", larvaIcon, false, creator)`. The creator instantiates the singleton `replayPageComp`.
- `loadResources()` loads only `icon/larva-module-icon.png` and `help/larva-module-help.html`.
- The module holds singletons `replayPageComp` and `latestReplaySummary`.

**What must change.** The hub becomes a module-owned `IPage`. All sub-page lifecycle goes through one EDT-confined navigator. All three icons are loaded at init.

**Scope.**
- `LarvaHubPage implements IPage<JComponent>`: name `Larva`, `larvaIcon`, `isCloseable() == false`, `getParent() == null`, mutable `ArrayList<IPage<?>>` children. `createPageComp()` delegates to a supplied creator (S05 provides the real hub component; until then a placeholder panel).
- `LarvaSubPage implements IPage<JComponent>` (closeable = true, mutable display name with a package-private `setDisplayName`, `LarvaSubPageKind {SINGLE_REPLAY, MULTI_ANALYSIS}`, creator delegate, parent = hub).
- `LarvaPageNavigator` (EDT-only):
  - `install()`
  - `isReady()`
  - `addSubPage(LarvaSubPage page, boolean select)`
  - `selectPage(IPage<?>)`
  - `relabel(LarvaSubPage, String)` (calls `rebuildPageTree(false)`)
  - `List<LarvaSubPage> getSubPages(LarvaSubPageKind)`
  - `isLarvaPageSelected()` (hub or any sub-page currently selected; needed by S07)
- `loadResources()` additionally loads `icon/larva-replay-icon.png` and `icon/larva-multi-analysis-icon.png` into `replayIcon` and `multiAnalysisIcon` fields with package-private getters.
- `installReplayPage()` is replaced by `navigator.install()` (still posted via `guiUtils.runInEDT`).

**Out of scope.** Hub buttons (S05), single-page binding (S06), multi page (S14).

**Implementation notes.**
- **Single source of truth:** the hub's child list *is* the registry. Scelight's `removeSelectedPage()` removes closed pages from `parent.getChildList()` itself. Do **not** keep a second list that can drift. Query by iterating the child list (`instanceof LarvaSubPage`).
- `addSubPage`: `page.setParent(hub)`, `hub.getChildList().add(page)` (creation order, AC-2.1), `rebuildPageTree(false)`, then `selectPage(page)` if `select`.
- Disposal: sub-page components implement `IPageDisposedListener` and notify their controller (S06/S14). The navigator only logs at `debug`.
- Guard every public navigator method with an EDT assertion (`SwingUtilities.isEventDispatchThread()`). Log an error and return if it is violated. Do not throw, because the host keeps running.
- `isReady()` becomes true only after `install()` completed on the EDT (S07 relies on it).
- If S01 was NO-GO on nesting, `addSubPage` uses `addPage` with label prefix `Larva: ` instead. Keep that difference inside the navigator.
- `destroy()`: no tree cleanup is required (Scelight disposes pages on exit), but set `ready=false`.

**Acceptance criteria.**
1. Given Scelight starts, when the module loads, then a `Larva` node with `larva-module-icon.png` exists and its X button is disabled (AC-2.2).
2. Given a test hook (temporary debug action or S06), when two sub-pages are added, then they appear under `Larva` in creation order, the last one selected (AC-2.1).
3. Given two sub-pages, when one is closed via X or `Ctrl+F4`, then it disappears, the other stays (AC-2.4), and selection falls back to history or the hub (AC-2.3).
4. Each node type shows its icon in the tree and title bar at 16 px and at a larger title font (AC-16.1).
5. Given the replay or multi icon resource is removed from the jar, when the module initializes, then init fails with the existing "missing packaged resources" message naming phase `loading packaged resources` (AC-16.2).
6. Calling any navigator mutator off the EDT logs an error and does not modify the tree.

**Tests required.**
- Unit: `LarvaHubPage` / `LarvaSubPage` basics (non-closable hub, mutable children, relabel changes `getDisplayName`, kind filter over a child list). No Swing host needed.
- Manual in Scelight: criteria 1–5.

**Definition of done.**
- [x] Hub, sub-page and navigator classes added. `installReplayPage()` replaced.
- [x] Three icons loaded (`requireResource` throws `Missing packaged resource: <path>` in phase `loading packaged resources`).
- [x] The missing-icon failure path is verified once in a running Scelight.
- [x] Unit tests green (`LarvaHubPageTest`, 28 tests in total).
- [x] Agent checks AC-1..AC-5 recorded in a real Scelight (2026-11-24, screenshots `knowhow/epic17-s04-*.png`). Real X click, Ctrl+F4 key and larger title font stay manual for the user.

---

### E17-S05 — Hub UI (centered buttons, ladder checkbox, tip icon)

| Maps to | WP-05 · FR-1 · AC-1.1–1.4 · OQ-8 |
|---|---|
| Size | S |
| Depends on | S04 |

**User story.** As a Zerg player, I want the `Larva` page to show just three clear actions, so that I can open a replay, the latest replay, or a multi analysis without anything loading by itself.

**Context.** Today the `Larva` page *is* `LarvaReplayPageComp`. Its `pageSelected()` and `hierarchyChanged()` call `requestAutomaticLatestReplayLoad`, which auto-analyzes. The hub must do nothing on show.

**Scope.**
- `LarvaHubPageComp extends JPanel`. Horizontally centered, vertically stacked (`GridBagLayout` or `BoxLayout` with glue):
  1. `Open Replay...`
  2. `Analyze Latest Replay`
  3. a row with `?` tip icon (`guiFactory.newTipIcon(IRHtml)`), `Multi Analysis...`, and `Ladder games only` checkbox (checked).
- Tip text (FR-1): *Select multiple replays; you will receive aggregated larva stats for the most common Zerg player(s) across them.*
- `LarvaHubActions` interface (dependency inversion): `openReplay(Component parent)`, `analyzeLatestReplay()`, `startMultiAnalysis(Component parent, boolean ladderOnly)`. The hub component calls only this interface.
- Until S06 and S14 land, the navigator's implementation shows `guiUtils.showInfoMsg("Not available yet")`.
- Keep the existing help icon (`newHelpIcon(module.getHelpContent())`) in the top-right corner, as on the replay page.

**Out of scope.** Behavior of the actions (S06, S14). Persistence of the checkbox (OQ-8: none).

**Implementation notes.**
- The component does **not** implement `IPageSelectedListener` or `HierarchyListener` (AC-1.4).
- The checkbox state lives in the component instance. Because Scelight caches page components, it survives navigation within a session and resets on restart (OQ-8).
- Equal button widths look best. Use one preferred width for the three buttons.

**Acceptance criteria.**
1. Given Scelight starts, when the user selects `Larva`, then only the controls above are visible, centered (AC-1.1).
2. The checkbox is checked on first display (AC-1.2). Given the user unchecks it and clicks `Multi Analysis...`, then `startMultiAnalysis(…, false)` is invoked.
3. Hovering `?` shows the tooltip text (AC-1.3).
4. Selecting the hub, or navigating away and back, triggers no replay analysis. Logs show no analysis start (AC-1.4).
5. After a Scelight restart the checkbox is checked again (OQ-8).

**Tests required.**
- Unit (headless-safe, skip if `GraphicsEnvironment.isHeadless()`): the component invokes the right `LarvaHubActions` method with the checkbox state. Use a fake actions implementation.
- Manual: layout at 1280 px and 1920 px, tooltip, no auto-analysis.

**Definition of done.**
- [x] `LarvaHubPageComp` and `LarvaHubActions` added and wired as the hub's creator.
- [x] Manual layout screenshot `knowhow/epic17-hub.png` (1920 px; 1280 px not checked, see S05 result).

> **S05 result (2026-11-24): code, unit tests and agent real-app checks done.** New `LarvaHubPageComp`, `LarvaHubActions` (implemented by `LarvaPageNavigator` with an info dialog "Not available yet" plus a `[larva-hub]` info log line), `LarvaDevHubCommands`, `help/larva-multi-tip.html` (tip content). `LarvaSecondsLostModule.isLadderOnly()` exposes the checkbox for S14 (true until the hub was shown). Deviations: (1) `LarvaReplayPageComp` is no longer instantiated; the monitor listener still calls `requestReplayAnalysis` for new replays (its page refresh is a no-op now), S06 removes that. (2) The 1280 px layout was not checked: no xdotool/wmctrl to resize the window; the layout is a GridBag block centered in its parent so it does not depend on width. (3) Tip icon tooltip is null by Scelight design, the popup shows on hover/click. (4) No dev command clicks the buttons, since the placeholder dialog is modal and would block the channel. Manual: real clicks on the three buttons (dialog, and `[larva-hub] action startMultiAnalysis ladderOnly=false` after unchecking), tip hover popup, navigate away and back.

---

### E17-S06 — Single-replay sub-pages (per-page binding, remove singleton auto-update)

> **S06 result (2026-11-24): code, unit tests, golden parity and agent real-app checks done.** New: `LarvaReplayPageController` (per-page state BUSY/READY/ERROR, lives independent of the lazily created component, request ids so a superseded or closed page's result is dropped), `LarvaReplayPageView` (what the controller needs from the component), `LarvaReplayAnalysisQueue` (interface) + `LarvaSingleReplayAnalysisQueue` (one daemon thread `larva-replay-loader`, owns the one `ReplaySummaryService`, FIFO, results posted with `invokeLater`, queued work of closed pages skipped, `shutdown()` in `destroy()`), `LarvaReplayAnalysisRunner`, `LarvaSinglePageHost` (module services for the navigator), `LarvaReplayPaths` (`normalize`, `nodeName`), `LarvaReplayFileChooser` (chooser extracted for S14), `LarvaModuleHubActions` (Open / Analyze Latest create or focus; Multi Analysis stays the S05 placeholder), `LarvaDevReplayCommands` (`open-replay`, `analyze-latest`, `refresh`, `page-text`). Navigator gained `openSingleReplay(Path, source, select)` (dedupe by `toAbsolutePath().normalize()`, focus-only on a hit, log "already open"), `findSingleReplayPage`, `getSelectedReplayFile`; the navigator no longer implements `LarvaHubActions`. `LarvaSubPage` carries the controller; `LarvaHubPage.findSingleReplayPage` is the pure lookup. `LarvaReplayPageComp` takes (module, controller, actions), no longer listens to page selection or hierarchy (no auto-latest), implements `IPageDisposedListener` (-> `controller.dispose()`), has an EDT guard in `show*`; visuals and texts unchanged. Module: singletons, `requestReplayAnalysis`, `refreshReplayPage`, `showReplayError`, `getLatestReplaySummary` and the off-EDT `showBusy` are gone; the monitor listener only records `lastDetectedReplayPath` (+ debug log). Dev-dump `recordAnalysisStart/Success/Failure` run inside the queue task; `sourceDescription` strings unchanged. **Decisions:** (1) the temporary `add-test-page` command and its placeholder comp were **removed** (real pages exist; S14 brings its own multi test path). (2) Two pages for different replays with the same file name get the same label (OQ-6 says the label is the file name); the dedupe key is the full path so both can be open, but `select/close/refresh/page-text <label>` act on the first one. (3) A failed analysis with a null message shows "Unknown error" as before. (4) `Error`s from the analysis are logged and shown as error state too, so a page never stays busy forever. **Parity (AC-4.1):** golden snapshot (`LarvaTimelineGoldenFormatter` + analysis report, the dev dump section) for 5 replays (`queensMoved`, `injectsQueued`, 3 `raw-replays/seeded-sample`): headless harness on pre-epic code (commit e629a5e, built in a scratch dir) vs the new classes: byte-identical for all 5. In the real Scelight, the pre-epic module 2.3 (built from e629a5e, fed by the replay-folder monitor) vs the new 3.0 sub-pages (via `open-replay`): the dump sections of 3 replays are byte-identical. (Headless vs in-app differ in two environment lines, `replay load=` and the `replay.players` order of one replay, in old and new alike.) Screenshot `knowhow/epic17-s06-single-page.png` (new) against the pre-epic page in `knowhow/epic17-spike-hub-with-children.png`: same layout. **Not covered by the agent:** real clicks on Open Replay (chooser dialog) and Analyze Latest on hub and page, the "no latest replay" info message (AC-3.2: not exercised, the real latest replay always resolves here; the dev command FAILs instead of showing the dialog), X / Ctrl+F4 key. Surprise: Scelight renames the copied test replay and also writes a backup copy (see running-scelight.md); both were deleted.

| Maps to | WP-06 (**without** the OQ-10 auto-open feature; that is S07) · FR-3, FR-4 · AC-3.1–3.4, AC-4.1–4.3, AC-2.3 · OQ-6 · R-5 |
|---|---|
| Size | L |
| Depends on | S04, S05 |

**User story.** As a Zerg player, I want every replay I open to get its own closable page named after the file, so that I can compare several games and nothing switches under me.

**Context (today).**
- `LarvaReplayPageComp` (singleton) holds:
  - the buttons, `LarvaTimelinePreviewComp` and the details `JTextArea`
  - `pageSelected()` / `hierarchyChanged()` → `requestAutomaticLatestReplayLoad()` (auto-latest)
  - Refresh, which falls back to `module.getLatestReplaySummary()`
- `LarvaSecondsLostModule.requestReplayAnalysis(Path, String)`:
  - starts a raw daemon `Thread("larva-replay-loader")`
  - calls the **shared** `replaySummaryService.analyze(...)`
  - writes `latestReplaySummary`
  - pushes into the singleton via `refreshReplayPage` / `showReplayError`
  - calls `showBusy` **off the EDT**
- Concurrent requests share one non-thread-safe service.
- `installReplayMonitorListener()` sets `lastDetectedReplayPath` and calls `requestReplayAnalysis` for every new replay.

**What must change.**
- Each single-replay page is bound to exactly one `Path` with its own state.
- Analysis requests are serialized through one module-owned queue.
- The singleton fields and the monitor-driven singleton update go away.

**Scope.**
1. `LarvaReplayPageController` (one per sub-page):
   - fields: `replayFile` (absolute, normalized), state `{BUSY, READY, ERROR}`, last `LarvaReplayPageSummary`, error text, `disposed` flag, attached component (nullable)
   - methods: `requestAnalysis(sourceDescription)`, `attach(LarvaReplayPageComp)`, `dispose()`
   - **Holds state even if the component was never created** (lazy `createPageComp`, see S01 Q4). `attach` renders the current state.
2. `LarvaSingleReplayAnalysisQueue`:
   - module-owned single-thread executor (daemon thread `larva-replay-loader`)
   - owns **one** `ReplaySummaryService`, so analyses are FIFO and never concurrent
   - `submit(controller, source)` runs `analyze`, then posts the result to the controller on the EDT
   - results for disposed controllers are dropped with a `debug` log
   - `shutdown()` in `destroy()`
3. `LarvaReplayPageComp` refactor:
   - constructor takes its controller
   - remove `IPageSelectedListener`, `HierarchyListener` and `requestAutomaticLatestReplayLoad` (FR-4, AC-1.4)
   - implement `IPageDisposedListener` → `controller.dispose()`
   - `Open Replay...` and `Analyze Latest Replay` call `LarvaHubActions` (same as the hub)
   - `Refresh Current Replay` calls `controller.requestAnalysis("Larva replay page refresh")` (in place, AC-4.3)
   - visuals, texts, timeline and text area are unchanged
4. Navigator: `openSingleReplay(Path, String source, boolean select)`:
   - normalizes the path
   - **dedupe (OQ-6):** if a `SINGLE_REPLAY` sub-page with an equal path exists, select it and return (no re-analysis)
   - otherwise creates `LarvaSubPage` (name = file name without a trailing `.SC2Replay`, case-insensitive; icon `replayIcon`), adds and selects it, and submits the analysis
5. Hub and page actions:
   - `openReplay`: file chooser (start-folder logic unchanged from `openReplayChooser`: the current page's replay folder, else `lastDetectedReplayPath` parent). Extract it to `LarvaReplayFileChooser` so S14 can reuse it.
   - `analyzeLatestReplay`: `module.resolveLatestReplayPath()`. If `null`, show the existing info message and create no page (AC-3.2).
6. Module cleanup:
   - delete `replayPageComp`, `latestReplaySummary`, `getLatestReplaySummary()`, `refreshReplayPage`, `showReplayError`, `requestReplayAnalysis` (replaced by queue + controller)
   - keep the dev-dump calls (`recordAnalysisStart/Success/Failure`) inside the queue task
7. **Replay monitor listener:** keep it registered. **Reduce it to:** `lastDetectedReplayPath = event.getFile()` plus a `debug` log. **No analysis and no page.** S07 adds auto-open on top. This story must not.

**Out of scope.** FR-17 auto-open (S07). Phase-table renderer extraction (S13). Any change to the replay page's visuals or texts.

**Implementation notes.**
- Name stripping: `name.regionMatches(true, len-10, ".SC2Replay", 0, 10)`. Leave other extensions untouched.
- Path equality: `replayFile.toAbsolutePath().normalize()`. On Windows, `Path.equals` is case-insensitive already. Do not lowercase manually.
- Error handling (AC-3.3): `RuntimeException` from `analyze` sets state ERROR with `e.getMessage()` and logs `error` with the throwable (as today).
- The `sourceDescription` strings stay as today (`Manual replay selection`, `Latest replay resolver`, `Larva replay page refresh`) so dev-dump goldens stay comparable.
- Idle text in `showIdleMessage()` is not reachable any more for bound pages. Keep the method for the null-path case, or remove it if unused.

**Acceptance criteria.**
1. Given the hub, when the user picks `X.SC2Replay`, then a child `X` with the replay icon appears under `Larva`, selected, in busy state, and then shows the overview (AC-3.1).
2. Given `X` is already open, when the user opens `X.SC2Replay` again (via Open or Latest), then no new node is created, the existing `X` is selected, and no analysis is started (OQ-6). The log says "already open".
3. Given no resolvable latest replay, when the user clicks `Analyze Latest Replay`, then the existing info message appears and no page is created (AC-3.2).
4. Given a corrupt replay, then its page stays open with the existing error text, scoped to that page (AC-3.3).
5. Given the same replay, the old `Larva` page (pre-epic build) and the new sub-page produce **identical** `LarvaTimelineGoldenFormatter` output in the dev dump, and visually identical timeline and phase table (AC-4.1).
6. Given pages A and B for different replays, when the user switches between them, then each keeps its content. Refresh on A re-analyzes A only, and B is unchanged (AC-4.2, AC-4.3).
7. Given a new replay arrives in a monitored folder, then **no page changes and no analysis starts** (until S07). `Analyze Latest Replay` resolves the new file.
8. Given a page whose analysis is still queued or running, when the user closes it, then the result is dropped (debug log), and there are no exceptions and no EDT updates to the disposed component (AC-2.3).
9. No component method is called off the EDT (code review plus an EDT assertion in `LarvaReplayPageComp.show*`).

**Tests required.**
- Unit: node-name derivation (`X.SC2Replay`, `x.sc2replay`, `a.b.SC2Replay`, no extension). Dedupe lookup over a synthetic child list. Controller state transitions (BUSY→READY, BUSY→ERROR, result after dispose is ignored), using a fake queue that runs synchronously.
- Golden: dev dump `LarvaTimelineGoldenFormatter` output before and after for ≥ 3 replays (`benchmark-data/queensMoved/*.SC2Replay`). Diff must be empty apart from timestamps and source labels.
- Manual: criteria 1–8 in Scelight. Screenshot old vs. new page.

**Definition of done.**
- [x] Controller, queue, chooser helper and navigator `openSingleReplay` added. Module singletons removed.
- [x] Listener reduced to path recording (S07 will extend it).
- [x] Parity checkpoint passed (criterion 5). Golden diff empty (see S06 result).
- [x] Unit tests green (63 tests). Agent checklist ticked (criteria 1-9, see S06 result); real clicks stay manual for the user.

---

### E17-S07 — [FR-17, new scope] Auto-open sub-page for newly detected Zerg replays

> **S07 result (2026-11-24): code, unit tests and agent real-app checks done.** New: `LarvaAutoOpenPolicy` (pure: `qualifies(List<RaceView>)`, `shouldSelect`, `underCap`, `admit(ready, alreadyOpen, openCount)` -> `Admission` OPEN / SKIP_NOT_READY / SKIP_ALREADY_OPEN / SKIP_CAP; constants `MAX_AUTO_OPEN_SINGLE_PAGES = 20`, `MAX_PENDING_AUTO_REQUESTS = 5`, `SOURCE_MONITOR = "Replay Folder Monitor"`), `LarvaAutoOpenPendingLimit` (AtomicInteger CAS counter), `LarvaAutoOpenOutcome`, `LarvaAutoOpenHost`, `LarvaAutoOpenStats`, `LarvaDevAutoOpenCommands` (`auto-open-state`, plus dev-only `auto-open-submit <path>`). Changed: `LarvaSingleReplayAnalysisQueue.submitAutoOpen(Path)`, `LarvaReplayAnalysisRunner.analyzeForAutoOpen`, `ReplaySummaryService.analyzeForAutoOpen` (loads the processor once, Zerg check on `IDetails` player races, analyzes that same processor; `analyze` now shares `buildPageSummary`/`analyzeLoadedReplay`), `LarvaPageNavigator.checkAutoOpenAdmission` + `autoOpenSingleReplay`, `LarvaReplayPageController.presetResult`, module listener (`handOffToAutoOpen`, logs hand-off time). **Flow:** listener (job thread) records the path, calls `submitAutoOpen` (pending limit, returns in 0.06-0.5 ms) -> worker: EDT admission check (ready, dedupe, cap; before any parse) -> `analyzeForAutoOpen` (one parse) -> EDT: `autoOpenSingleReplay` re-checks admission, creates the page already READY (or ERROR if the analysis failed after a passed Zerg check) and selects it only by the focus rule. **Deviations:** (1) A new page is created already READY, not BUSY first (the story's allowed single-parse option); busy is never visible. (2) `autoOpenSingleReplay` takes the outcome as second argument and the dedupe/cap check is also done on the worker before parsing (saves a parse for duplicates and capped events). (3) Extra dev command `auto-open-submit`. (4) Pending limit counts waiting requests only: a burst of 10 gave 1 running + 5 pending = 6 pages, 4 dropped (logged). **Surprise:** Scelight opens and selects its own Replay Analyzer before notifying listeners, so for real new replays the user is always on a non-Larva page and the page is only added (D17-c). The select branch was verified with `auto-open-submit` (on the hub and on a Larva sub-page). **Side effect:** the SC2ReplayStatsUploader module also picks up copied test replays (see running-scelight.md). **Not covered by the agent:** an event before the hub is installed (unit-tested via the null host / `SKIP_NOT_READY`), the busy state of an auto page, visual check beyond one screenshot, user's own "Minimize to tray" behaviour.

| Maps to | **Proposed FR-17** (§0.1) · OQ-10 · OQ-6 (dedupe) · FR-3/FR-4 (page type) · AC-17.1–17.8 |
|---|---|
| Size | M |
| Depends on | S06 (navigator `openSingleReplay`, queue, controller). S02 (cheap metadata finding; there is a fallback if it is not done) |

> This feature was **not in the epic's FR list** (see §0.1). Every row is now checked by the use and marked confirmed and ready for implementation.

**User story.** As a Zerg player, I want each new Zerg replay that Scelight detects to show up as its own `Larva` sub-page, so that after a game I can look at my larva stats without clicking anything.

**Context.**
- After S06, `installReplayMonitorListener()` only records `lastDetectedReplayPath`.
- Scelight calls `INewRepListener.newRepDetected(INewRepEvent)` from `NewReplayHandlerJob.jobRun()` on a **background job thread**. That happens about 1.5 s after detection, after Scelight's own rename, backup and optional Replay Analyzer open.
- `event.getFile()` is the final path after rename or move. `getOriginalFile()` may not exist any more.
- With Scelight's "only pick up just played replays" setting on, old replays never reach listeners.
- Scelight's own analogue: `RepAnalyzersPage.newRepAnalyzerPage(file, true)` (selects), capped by `Settings.MAX_REP_ANALYZERS_FOR_NEW_REPS` (default 20).

**Defaults for unresolved details**

| # | Topic | Recommended default | Status |
|---|---|---|---|
| D17-a | Trigger condition | The replay has **≥ 1 player whose resolved race is Zerg** (`IPlayer.getRace() == IRace.ZERG`, Random already resolved). Any format, any opponent. | Confirmed |
| D17-b | Ladder-only and < 4 min filters | **Not applied.** They are multi-analysis controls. The hub checkbox does not affect auto-open. Games of any length and mode open.  | Confirmed |
| D17-c | Focus | **Add to the tree without selecting**, **unless** the currently selected page is the `Larva` hub or any `Larva` sub-page. Then select the new page. This mirrors the old "Larva page updates after a game" behavior without stealing focus from Scelight's own Replay Analyzer, which Scelight opens and selects just before notifying listeners. | Confirmed. The replay data like unspent resources, SQ or other is more important than the larva. the user will select the latest tree-child under the larva hub.|
| D17-d | Dedupe (OQ-6) | If a single-replay page for the same normalized path is open, **do nothing**: no new page, no re-analysis, no selection change. Debug log. | Confirmed. (dedupe itself is decided, the "no selection change" part is a default) |
| D17-e | Flood protection | (1) **Cap:** do not auto-create when ≥ **20** single-replay sub-pages are open (manual + auto, constant `MAX_AUTO_OPEN_SINGLE_PAGES = 20`, mirrors Scelight's default). Manual opens are never capped. (2) **Pending limit:** at most **5** auto requests waiting in the queue. Further events are dropped with a debug log naming the file. Each dropped file can still be opened manually. | Confirmed. |
| D17-f | Threading | Listener body: record `lastDetectedReplayPath`, then hand off to `LarvaSingleReplayAnalysisQueue` (`submitAutoOpen(path)`) and **return immediately**. On the queue worker: Zerg pre-check (cheap `getRepProc` if S02 confirms races are available, else full parse) → EDT: create the page in BUSY state → worker: full `analyze` → EDT: show result. One FIFO worker is shared with manual single-replay analyses (S06), so there is no concurrent service use. | Confirmed |
| D17-g | Before hub exists / during module start / after destroy | If `navigator.isReady()` is false when the page would be created (checked on the EDT), **skip with a debug log**. There is no backlog or replay of missed events. `lastDetectedReplayPath` is still updated, so `Analyze Latest Replay` works. After `destroy()`, the listener is removed (exists today) and the queue drops pending auto requests. | Confirmed |
| D17-h | User toggle | **No toggle.** Auto-open is always on. | Confirmed |
| D17-i | Page name, icon, source | Same as manual single pages (FR-3, OQ-6 stripping). `sourceDescription = "Replay Folder Monitor"` (same string as today). | default and Confirmed |
| D17-j | Pre-check parse failure | If the replay cannot be parsed during the pre-check, **create no page** (the user did not ask for it). Debug log. If the pre-check passes but the full analysis fails, the page shows the normal error state (AC-3.3). | Confirmed |

**Scope.**
- `LarvaAutoOpenPolicy` (pure, unit-testable):
  - `boolean qualifies(List<RaceView> players)` (D17-a; races only, so it needs no Scelight runtime)
  - `boolean shouldSelect(boolean larvaPageSelected)` (D17-c)
  - `boolean underCap(int openSinglePages)` (D17-e)
- Listener extension in `LarvaSecondsLostModule.installReplayMonitorListener()`.
- `LarvaSingleReplayAnalysisQueue.submitAutoOpen(Path)` with the pending-limit counter (`AtomicInteger`).
- Navigator method `autoOpenSingleReplay(Path)` (EDT). It runs the ready / dedupe / cap checks, creates the page with `select = policy.shouldSelect(navigator.isLarvaPageSelected())`, and returns the controller or `null`.

**Out of scope.** Any change to Scelight settings. Persistence of auto-opened pages. A toggle (D17-h). Applying multi-analysis filters (D17-b).

**Implementation notes.**
- Order of checks on the EDT (`autoOpenSingleReplay`): ready → dedupe → cap → create. Re-check dedupe on the EDT even if checked earlier, because a manual open may have happened meanwhile.
- The Zerg pre-check must use `IRepProcessor` data only (`getReplay().getDetails().getPlayerList()`). Map to a small `RaceView` list before calling the policy so it stays testable.
- To avoid parsing twice when the cheap processor lacks races, it is acceptable to run the full `analyze`, check Zerg presence from the processor's `IDetails` player races, and create the page directly in READY state. Do not use phase-table map membership for this check; that map is not guaranteed to be Zerg-only (see the race filter in `ReplaySummaryService.buildBenchmarkReplayRecord`). Choose based on S02 and document the choice in code.
- The page component may never be created if the page was not selected (D17-c). The controller holds the result (S06 design). Verify this in the manual check.
- Make sure the replaced `requestReplayAnalysis` path (singleton update, off-EDT `showBusy`) is not resurrected.

**Acceptance criteria.**
1. Given Scelight is running and the user is on the `Larva` hub, when a new 1v1 replay with a Zerg player is saved to a monitored folder, then a child named after the file with the replay icon appears under `Larva`, is selected, shows busy, then shows the overview (AC-17.1, D17-c).
2. Given the user is on Scelight's Replay Analyzer (not a `Larva` page), when a new Zerg replay arrives, then the child is added under `Larva` but **not** selected. Selecting it later shows the finished overview without re-analysis (D17-c, lazy component).
3. Given a new PvT replay, then no page is created, a debug log line names the file, and `Analyze Latest Replay` resolves it (AC-17.2).
4. Given `X` is already open, when `X` is detected again (e.g. re-saved), then no new node appears, there is no re-analysis, and the selection is unchanged (AC-17.3, D17-d).
5. Given 20 single-replay pages are open, when a new Zerg replay arrives, then no page is created and a debug log mentions the cap. A manual `Open Replay...` still works (AC-17.5).
6. Given 10 Zerg replays copied into a monitored folder at once, then at most (5 pending + 1 running) auto pages are produced from that burst, the rest are logged as dropped, and Scelight stays responsive (AC-17.5, AC-17.6).
7. The listener returns within 50 ms (timestamp debug logs around the handoff), and no parsing happens on the EDT (AC-17.6).
8. Existing open pages keep their replay when new replays arrive (AC-17.7).
9. Given an event before `navigator.isReady()`, or after module `destroy()`, then no page is created and nothing is thrown (AC-17.8).
10. Given a detected file that fails to parse in the pre-check, then no page is created (D17-j).

**Tests required.**
- Unit: `LarvaAutoOpenPolicy`:
  - qualifies: Z vs P, P vs T, Z vs Z, 2v2 with one Zerg, empty or null list
  - shouldSelect: both branches
  - underCap: 19 / 20 / 21
  - pending-limit counter behavior (fake queue)
  - dedupe decision over a synthetic child list
- In Scelight, by the agent: criteria 1–10. To simulate new replays, copy fixture replays into the monitored Accounts folder and delete them afterwards (user-approved, procedure and naming in [knowhow/running-scelight.md](../knowhow/running-scelight.md)). If Scelight ignores copied replays because of an "only just played" style setting, record it in the knowhow file.

**Definition of done.**
- [x] Policy, queue extension, navigator method and listener wiring added.
- [x] Unit tests green (79 tests). Agent checklist ticked (criteria 1-10, see S07 result); real-click and visual checks stay manual for the user.
- [ ] Help text mentions auto-open (in S16).

---

## 4. Phase 2 — Analysis core (data track)

### E17-S08 — Milestone extraction (100/200 supply, 66/80 drones)

| Maps to | WP-07 · FR-13 · AC-13.1, AC-13.2 · D-4, D-10 · OQ-11 · R-7 |
|---|---|
| Size | M |
| Depends on | S03 |

**User story.** As a Zerg player, I want to know when I reached 100 and 200 supply and 66 and 80 drones in each game, so that the multi page can show my average macro timings.

**Context.**
- `LarvaReplayAnalyzer`:
  - The `ID_PLAYER_STATS` handler (≈ line 286) stores `LarvaPlayerResourceSnapshot(…, foodUsed, foodMade, workerCountToUse)`.
  - `workerCountToUse` = calculated alive drones from local `aliveDroneCountByPlayerName`, falling back to `workersActiveCount`.
  - `incrementDroneCount` / `decrementDroneCount` (≈ lines 3094–3113) maintain the live count.
- `foodUsed` is fixed-point ×4096 (see `LarvaTimelineModelBuilder` ≈ line 818).
- `LarvaAnalysisReport.getResourceSnapshotsByPlayerName()` exposes snapshots. `loopToTimeMs(int)` converts per Scelight's real-time setting.

**Scope.**
- `LarvaMilestone` enum: `SUPPLY_100`, `DRONES_66`, `DRONES_80`, `SUPPLY_200`. Each has a display label and threshold. Display order: 100 supply, 66 drones, 80 drones, 200 supply.
- `LarvaMilestoneCalculator` (pure): `Map<LarvaMilestone,Integer> compute(List<LarvaPlayerResourceSnapshot> snapshots, Integer exactDrone66Loop, Integer exactDrone80Loop)`.
  - Supply: first snapshot with `foodUsed / 4096 >= 100` (resp. 200). Integer compare: `foodUsed >= 100 * 4096`.
  - Drones: use the exact loop if provided, else the first snapshot with `workers >= 66` (resp. 80).
  - Not reached → absent (no censoring, D-10).
- **Exact drone loop (D-4 preferred upgrade):**
  - Inside the analyzer, after each `incrementDroneCount`, record the **first** loop where the player's count becomes ≥ 66 and ≥ 80 (`firstDroneLoopByPlayerName` maps).
  - Expose them via new `LarvaAnalysisReport` getters `getFirstDroneCountLoop(playerName, threshold)`.
  - Later dips below and re-reaching do not overwrite.

**Out of scope.** Showing milestones on the single page (AC-13.3: it must not). Changing phase promotion (`>` thresholds, `resolvePhasePromotionLoop`). The unused `PHASE_PROMOTION_DWELL_SECONDS`.

**Implementation notes.**
- Use `≥` for milestones and keep `>` for phase promotion. This is intentional (D-4).
- The analyzer change must be additive. Existing outputs (timeline, phase tables, benchmark records) are byte-identical. Verify with the golden dev dump and with `LarvaBenchmarkCliExtractor` output before and after on ≥ 3 replays.
- If the exact-loop change risks regressions, ship the snapshot-only variant and set the exact loops to `null`. AC-13.2's tolerance variant then applies. Note the choice in the PR.
- Time conversion to display ms happens in S09 while the report is still available (C-9).

**Acceptance criteria.**
1. Given synthetic snapshots with `foodUsed` = 99×4096 then 100×4096 at loops 1000 / 1160, when computed, then `SUPPLY_100 = 1160`. `foodUsed = 100×4096 − 1` does not qualify.
2. Given snapshots that never reach 200 supply, then `SUPPLY_200` is absent (AC-13.1 input).
3. Given exact drone loops are provided, then they win over the snapshot scan. Given `null`, the first snapshot with `workers >= 66` is used.
4. Given a fixture replay, the 66-drone loop is within one `PlayerStats` interval of the first loop where the analyzer's alive count reaches 66. With exact capture, it equals that loop (AC-13.2).
5. Given ≥ 3 fixture replays, then the golden dump and the benchmark CLI output are identical before and after this story.
6. The single-replay page shows no milestone data (AC-13.3).

**Tests required.**
- Unit `LarvaMilestoneCalculatorTest`: threshold exactly reached, never reached, empty or null list, fixed-point boundary, drone dip and re-reach (first reach wins), exact-vs-snapshot precedence.
- Golden: before/after diffs (criterion 5).
- Integration (once S12 exists): milestone loops printed by the `--multi` dump for a fixture.

**Definition of done.**
- [x] Enum, calculator, analyzer capture and report getters added.
- [x] Unit tests green (92 tests, 13 new). Golden diffs empty.

> **S08 result (2026-11-24): exact-loop variant shipped.** New: `LarvaMilestone` (enum, display order 100 supply, 66 drones, 80 drones, 200 supply), `LarvaMilestoneCalculator` (pure, `compute(snapshots, exact66, exact80)`, EnumMap in display order, absent = not reached), `LarvaFirstDroneLoopTracker` (package-private, first loop per player and threshold, dips never overwrite). `LarvaReplayAnalyzer` change is 3 lines: create the tracker, call `observe(aliveDroneCountByPlayerName, event.getLoop())` after each tracker event (equivalent to "after each increment", since one event changes the count by at most one and decrements cannot raise it), pass its map to `LarvaAnalysisReport`, which gained one constructor parameter (the analyzer is its only caller) and `getFirstDroneCountLoop(playerName, threshold)` (null if never reached). Supply stays snapshot-based. **Exact loop for drones, snapshot-only for supply.** Evidence (headless harness over `LarvaTimelineGoldenFormatter.format(analyze(...))` plus `LarvaBenchmarkJsonSerializer` output, before vs after the change, `cmp` per file): 6 replays (`benchmark-data/queensMoved/2026-07-03-...Scorpion VS fificent` and the first 5 of `raw-replays/seeded-sample`), 12 files, all byte-identical; the baseline also repeats byte-identically. AC-13.2 on the fixtures: exact 66-drone loops 18710 / 8117 / 8480 / 7902 / 8004 versus snapshot loops 18720 / 8160 / 8640 / 8000 / 8320, always earlier than the snapshot loop, by 10 to 160 loops (one 160-loop `PlayerStats` interval) except one case (8004 vs 8320, 316 loops): the count was at 66 or more between two snapshots, dipped below 66 (drones morphing into buildings) at the 8160 snapshot and was seen at 66 or more again only at 8320. That is exactly why the exact capture (first reach) is preferred; the snapshot scan can be late by more than one interval after a dip. Exact 80-drone loops behave likewise. Non-Zerg players have no tracked exact loop (null): their snapshot worker count falls back to SCV/probe counts, so S09 must only read milestones for Zerg tracked players. No UI change, so the single page shows no milestones (AC-13.3). Not done: the integration check via the `--multi` dump (needs S12); `ant BUILD_RELEASE` was not run (release artifacts are in flux), `ant TEST` is green.

---

### E17-S09 — Per-game aggregation record and headless extraction path

| Maps to | WP-08 · FR-9, FR-10, FR-11 · D-5, D-7, D-8, D-9, D-11 · OQ-5, OQ-15 · R-4 |
|---|---|
| Size | M |
| Depends on | S02 (cheap-metadata outcome), S08 |

**Technical goal.** Produce one small immutable record per replay with everything the filter, selector and aggregator need. The heavy `LarvaAnalysisReport` must be droppable right after extraction.

**Context.**
- `ReplaySummaryService.analyzeReplayContext()` builds `ReplayAnalysisContext(repProc, details, replaySummary, larvaAnalysisReport)`.
- `analyzeBenchmarkReplay()` is the Epic 13 headless path (frozen).
- Helpers `indexPlayersByName`, `findOpponent` (first other player) and `buildMatchupLabel` exist.
- `ReplaySummary.replayEndTime` is a formatted String (do not parse it). `IDetails.getTime()` gives the `Date`.
- `LarvaAnalysisReport.getPlayerPhaseTableByPlayerName()` is **not** guaranteed to be Zerg-only. `buildBenchmarkReplayRecord` filters by `IPlayer.getRace() == IRace.ZERG` from `IDetails`. Do the same, and take race from `IDetails`, never from map membership.

**Scope.**
- `LarvaAggregationGameRecord` (immutable):
  - `Path replayFile`, `Date replayDate` (from `IDetails.getTime()`, FR-9), `IGameMode gameMode` (nullable → treated as non-ladder), `IFormat format`, `int lengthLoops`, `boolean fullParse`
  - `List<LarvaAggregationPlayerRecord> zergPlayers`
  - `List<String> diagnosticFlags`
- `LarvaAggregationPlayerRecord` (immutable):
  - `String identityKey`, `String toonString`, `String name`, `IRace race`, `IRace opponentRace`
  - `EnumMap<LarvaGamePhase, LarvaPhaseStats>` (the existing raw-count objects, reused as is), `EnumMap<LarvaGamePhase, Integer> phaseDurationLoops`, `EnumMap<LarvaGamePhase, Long> phaseDurationDisplayMs`
  - `EnumMap<LarvaMilestone, Long> milestoneDisplayMs` (absent = not reached)
  - `boolean missingPhaseTable`
- `ReplaySummaryService.analyzeForAggregation(Path, String source)`: reuses `analyzeReplayContext` and builds the record via a new `LarvaAggregationRecordBuilder` (separate class to stop `ReplaySummaryService` growing, tech-debt note §10).
- `LarvaAggregationRecordBuilder.buildMetadataOnly(IRepProcessor)` for the S02 pre-filter (format, mode, date, length, races) when cheap metadata is available.

**Out of scope.** Filtering decisions (S10). Aggregation (S11). Any change to `LarvaBenchmark*` classes.

**Implementation notes.**
- **Identity key (D-5, C-10):** `regionId + "-" + programId + "-" + realmId + "-" + id` from `IToon`. **Do not use `IToon.toString()`**, which appends the player name when present. If the toon is null or `isZero()`, use `"name:" + name`. Map toon to name per replay through `IDetails.getPlayerList()`, because the analyzer keys by name.
- **Opponent race (D-7):** `IPlayer.getRace()` of the other player (1v1). For non-1v1 records, `opponentRace` may be null. They are excluded by S10 anyway.
- **Display times (D-11, C-9):** convert loops to ms with `larvaAnalysisReport.loopToTimeMs(loop)` **at build time**. Store loops too (for tests and the dump).
- **Phase duration (D-9):** `LarvaPhaseInterval.getDurationLoops()`. A missing interval or 0 counts as not reached.
- **Missing phase table (OQ-15):** a Zerg player in details without an entry in `getPlayerPhaseTableByPlayerName()` → `missingPhaseTable = true` (the existing `missing-phase-table` diagnostic in `buildPlayerDiagnosticFlagList`).
- **Length:** `LarvaAnalysisReport.getReplayLengthLoops()`. Compare against `LarvaAggregationConstants.MIN_GAME_LENGTH_LOOPS = 5376` (OQ-5) in S10, not here.
- The builder takes plain values (or small adapters), so unit tests can build records without Scelight. Put a `LarvaAggregationGameRecord.Builder` (Java 7 style) in production code for tests and the builder.

**Acceptance criteria.**
1. Given a fixture ZvT replay, when `analyzeForAggregation` runs, then the record has the in-game `Date` (unchanged after `touch`ing the file, AC-9.1), `IFormat.ONE_VS_ONE`, the replay's game mode, length in loops, and one Zerg player with opponent race Terran.
2. The identity key contains no player name for a real Battle.net replay, and the same account across two replays with different clan tags gives the same key.
3. Phase stats in the record equal the single page's `LarvaPlayerPhaseTable` stats for the same replay (field by field).
4. After extraction, no reference to `LarvaAnalysisReport`, `IRepProcessor` or the timeline model is retained by the record (code review, plus a heap check in S12/S16).
5. `LarvaBenchmarkCliExtractor` output is byte-identical before and after this story.

**Tests required.**
- Unit: identity key derivation (toon with name, zero toon, null toon), builder immutability (defensive copies), display-ms carried through.
- Integration (headless via `StandaloneScelightBootstrap`): criterion 1–3 on 2 fixtures. Can run as a CLI dry run until S12's `--multi` exists.

**Definition of done.**
- [x] Record, player record, builder and service entry point added.
- [x] Epic 13 output unchanged (diff attached: `cmp` clean on 5 replays, see result).
- [x] Unit tests green (105 tests, 13 new).

> **S09 result (2026-11-24): done.** New: `LarvaAggregationGameRecord` and `LarvaAggregationPlayerRecord` (final, immutable, `Builder` each, maps and lists unmodifiable copies, `Date` copied in and out; they hold only plain values plus the existing immutable `LarvaPhaseStats`), `LarvaAggregationRecordBuilder` (package-private; plain-value core `buildPlayerRecord(PlayerInput, LoopTimeConverter)` and `buildIdentityKey(...)`; adapters `buildFull(path, details, mode, format, report)` and `buildMetadataOnly(IRepProcessor)`). `ReplaySummaryService` gained only the thin `analyzeForAggregation(Path, String)` and its private `buildReplayDiagnosticFlagList(context)` became a package-private static `(report, details)` so the builder reuses the same replay flags (no duplicate logic; benchmark output unchanged). Decisions: Zerg players are picked from `IDetails` (race and non-empty name); opponent race is the other player's race only when the player list has exactly 2 entries, otherwise null (D-7); the phase-stats map holds every entry of the player's phase table (same objects, so field-by-field equal by construction), while the duration maps list only phases with duration > 0 (D-9; display ms = `report.loopToTimeMs(duration)`); milestones = `LarvaMilestoneCalculator.compute(snapshots, exact66, exact80)` converted with `loopToTimeMs` at build time, only for Zerg players; missing phase table sets `missingPhaseTable` and leaves the maps empty. `fullParse` = `report.isFullReplayParseUsed()`. `buildMetadataOnly` takes the path from `IRepProcessor.getFile()`, reads format, mode, date, header `getElapsedGameLoops()` and players/toons/races, carries no phase or milestone data and sets the diagnostic flag `metadata-only` (`fullParse=false`). **Verification.** Unit (`LarvaAggregationRecordBuilderTest`, 13 tests): identity key (toon fields, no name, same account with different clan tag, zero/null/incomplete toon fallback `name:`), phase data and display ms carried, zero-length phase not reached, missing phase table, milestone exact-vs-snapshot with display ms, unreached absent, unmodifiable maps/lists, builder copies, required fields. Note: `IToon`, `IRace` and friends cannot be loaded without the Scelight runtime (even a `Proxy` fails), so the `IToon` overload of `buildIdentityKey` is a one-line forward checked only in the integration run. Integration (headless harness in the scratchpad, `StandaloneScelightBootstrap` + `ReflectiveScelightRepParserEngine`, 4 replays: the ZvT `Scorpion VS fificent` and the first 3 of `raw-replays/seeded-sample`; replay copied to a temp file, `analyzeForAggregation` run, file mtime set to 1971, run again): (1) date identical before and after the touch and equal to `IDetails.getTime()`, format `1v1`, mode `AutoMM` (queensMoved) or `Private` (seeded sample), lengths 29483 / 26598 / 25867 / 16959 loops equal to `getReplayLengthLoops()`, exactly one Zerg player each, opponent race Terran / Terran / Terran / Protoss; (2) keys like `2-S2-1-5946255`, no name in any key (the toon string in these replays carries no name either); (3) for every player and phase, the record's stats equal `analyze(...)`'s `LarvaPlayerPhaseTable` stats (missed, missed-inject, spawned, hatch/inject eligible and active loops, per-minute rates and uptime) and the duration loops and display ms equal the interval durations and `loopToTimeMs`: 16 phases, 0 mismatches. Milestones in ms for the first fixture: 100 supply 678 888, 66 drones 844 548 (exact loop 18710), 80 drones 890 093, 200 supply 1 040 000. `buildMetadataOnly` compared with the full record (date, mode, format, length, player key, name, races, opponent race, empty phase data): equal on all 4 replays. Benchmark contract: `LarvaBenchmarkCliExtractor` stdout captured before and after the change on 5 replays (the same 5 as S08) with `cmp`: all byte-identical. Not done: a `buildMetadataOnly` check against a real cache-only processor inside Scelight (the headless engine always builds a full processor; per S02 the cache fields are identical), no heap check (criterion 4 is met by construction and left to S12/S16), `ant BUILD_RELEASE` not run. Scelight was not started.

---

### E17-S10 — Eligibility filter and tracked-player selection

| Maps to | WP-09 · FR-8 (summary counts), FR-10, FR-11 · AC-8.2, AC-10.1, AC-10.2, AC-11.1–11.3 · D-5, D-6 · OQ-2, OQ-5, OQ-15 · R-8 |
|---|---|
| Size | S |
| Depends on | S03, S09 (record type only) |

**User story.** As a Zerg player, I want the module to keep only the games that matter and to pick me (and at most one other main account) automatically, so that the averages describe my play and not random opponents.

**Scope.**
- `LarvaEligibilityFilter` (pure). `LarvaEligibility evaluate(LarvaAggregationGameRecord r, boolean ladderOnly)` returns ELIGIBLE or one reason code, checked in this fixed order:
  1. `FAILED_TO_PARSE` (record null or parse error; set by S12)
  2. `NOT_1V1` (`format != IFormat.ONE_VS_ONE`)
  3. `NON_LADDER` (only if `ladderOnly`; `gameMode != IGameMode.AMM`, null or unknown counts as non-ladder)
  4. `TOO_SHORT` (`lengthLoops < 5376`)
  5. `NO_ZERG`
- `LarvaFilterSummary`: counts per reason plus `selected` and `eligible`. Invariant: `selected == eligible + Σ excluded` (AC-8.2). Text form: `selected S · eligible E · excluded: not 1v1 a, non-ladder b, < 4 min c, no Zerg d, failed to parse f`.
- `LarvaTrackedPlayerSelector` (pure). `List<LarvaTrackedPlayer> select(List<LarvaAggregationGameRecord> eligible)`:
  - Count player-games per `identityKey` (the S09 toon key), **only where that identity played Zerg** (AC-11.3). Exclude `missingPhaseTable` player-games from the counts and report them (OQ-15).
  - **D-6:** rank by count desc, then the most recent game date desc, then case-insensitive name. #1 is always tracked. #2 is tracked only if count ≥ 3 **and** count ≥ 20 % of eligible games. Max 2 tracked players.
  - Display name = name in that identity's most recent eligible game (D-5).

**Out of scope.** A manual player picker. Persisting the choice. Scelight's favored-player setting (OQ-2: deliberately not used, no reflective settings lookup).

**Implementation notes.**
- Keep the reason order stable. The summary must count each file exactly once.
- `ladderOnly` comes from the hub checkbox at request time (AC-1.2).

**Acceptance criteria.**
1. Given 3 AMM ≥ 4 min, 2 Private ≥ 4 min, and 1 AMM 3-minute Zerg 1v1 records, when `ladderOnly = true`, then eligible = 3. When `false`, eligible = 5 (AC-10.1).
2. Given lengths 5375 and 5376 loops, then 5375 → `TOO_SHORT` and 5376 → eligible (OQ-5).
3. Given a null game mode with `ladderOnly = true`, then `NON_LADDER`.
4. A failed file counts as `failed to parse` and the batch continues (AC-10.2). The summary counts add up to `S` (AC-8.2).
5. Given 40 games of `Scorpion` (Zerg) vs ZvZ opponents appearing ≤ 2 times each, then the only tracked player is `Scorpion` (AC-11.1).
6. Given 20 games on account A and 15 on account B, then both are tracked and A is first (AC-11.2).
7. Given A played Terran in 5 of its games, then those 5 are not counted for A (AC-11.3).
8. Given equal counts, then the identity with the more recent last game wins. With equal dates, the case-insensitive name decides.
9. Given #2 has 3 games out of 20 eligible (15 %), then #2 is not tracked. 4 of 20 (20 %) → tracked.
10. Given one account played as `OldName` in older games and `NewName` in the most recent one, then it is a single tracked player displayed as `NewName` (D-5).

**Tests required.** Unit tests for all criteria above with synthetic records.

**Definition of done.**
- [x] Filter, summary and selector added.
- [x] ≥ 80 % line coverage of these classes (by inspection of the tests if there is no tool).

> **S10 result (2026-11-24): done.** New (all pure, no Scelight runtime): `LarvaAggregationConstants` (`MIN_GAME_LENGTH_LOOPS = 5376`), `LarvaEligibility` (ELIGIBLE, FAILED_TO_PARSE, NOT_1V1, NON_LADDER, TOO_SHORT, NO_ZERG in check order, with labels), `LarvaEligibilityFilter.evaluate(record, ladderOnly)`, `LarvaFilterSummary` (Builder, `getSelected` = sum of all counts so `selected == eligible + excluded` holds by construction, `toText()` exactly as in the story with the U+00B7 separator), `LarvaTrackedPlayer`, `LarvaTrackedPlayerSelector` (`select`, plus `listMissingPhaseTable` for the OQ-15 report; ranking count desc, latest date desc with undated oldest, name case-insensitive, then identity key as last tie-break; #2 needs count >= 3 and count*100 >= 20*eligible games; max 2; display name from the most recent game, independent of list order). **Seam:** `IFormat`/`IGameMode` constants cannot be loaded without the runtime (even `Proxy` fails), so `LarvaAggregationGameRecord` (S09) got two plain flags, `oneVsOne` and `ladder` (Builder setters, getters `isOneVsOne()`/`isLadder()`; default false, so a missing mode is non-ladder). The SDK comparison lives only in the new package-private `LarvaGameKinds` (`format == ONE_VS_ONE`, `mode == AMM`), called by `LarvaAggregationRecordBuilder` in `buildFull` and `buildMetadataOnly`. The S09 record keeps `format`/`gameMode` too. **Tests:** `LarvaEligibilityFilterTest` (8) and `LarvaTrackedPlayerSelectorTest` (11), total 124 green (105 + 19). AC 1 `ac1_*`, 2 `ac2_*`, 3 `ac3_*`, 4 `ac4_*` (also summary text and invariant), 5 `ac5_*`, 6 `ac6_*`, 7 `ac7_*`, 8 `ac8_*`, 9 `ac9_*`, 10 `ac10_*`; extra: reason order, empty summary, null result, 2-game second player, max 2, missing phase table, empty input, undated games. Coverage by inspection: every branch of the new classes is exercised except `LarvaGameKinds` (SDK-bound, two one-line comparisons, covered only in a real run) and the `LarvaTrackedPlayer` null-key guard. Not done: the real-replay check that the two flags are set correctly (needs the runtime; same one-line comparisons as `LarvaSpikeBatchCommands`), `ant BUILD_RELEASE` not run.

---

### E17-S11 — Aggregation engine (pooled tables, phase length, milestones)

| Maps to | WP-10 · FR-12, FR-13 · AC-12.1–12.3, AC-13.1 · D-1, D-2, D-3, D-7, D-9, D-10, D-11 · OQ-4, OQ-13 |
|---|---|
| Size | M |
| Depends on | S10 |

**User story.** As a Zerg player, I want my larva stats pooled correctly across games per matchup, so that a short end phase in one game cannot distort my averages.

**Scope.**
- `LarvaAggregator` (pure, stateless). `LarvaAggregationResult aggregate(List<LarvaAggregationGameRecord> eligible, List<LarvaTrackedPlayer> tracked, LarvaFilterSummary summary)`.
- Buckets per tracked player: `ALL`, `ZVP`, `ZVT`, `ZVZ` (`LarvaMatchupBucket` enum, matchup from `opponentRace`).
- D-7 mirror: a game where two tracked players faced each other counts in both players' ZvZ and All tables. Header `n` counts it once.
- Per bucket and phase (`LarvaAggregatedPhaseCell`):
  - **Pooled stats (D-1/D-2):** a `new LarvaPhaseStats(phase, null, Σmissed, ΣmissedInject, Σspawned, ΣhatchEligibleLoops, ΣinjectEligibleLoops, ΣinjectActiveLoops)`. The existing getters give the rates. **No formula duplication.**
  - Ranking strings: `LarvaPhaseBenchmarkRatingFormatter.formatSpawnedLarvaRanking/formatMissedLarvaRanking(phase, pooledValue)`. END is `n/a` by design (D-3).
  - **Phase length (D-9):** mean of `phaseDurationDisplayMs` over player-games with duration > 0, plus `k` (reached) and `n` (player-games in the bucket).
- Per bucket and milestone (`LarvaAggregatedMilestoneCell`): mean display-ms over player-games that reached it, plus `k/n` (AC-13.1). No censoring.
- `LarvaAggregationResult`:
  - tracked players (ordered)
  - per player: bucket game counts, phase cells and milestone cells
  - `distinctGameCount` (header `n`: distinct eligible games with ≥ 1 tracked player)
  - `startDate` / `endDate` (min/max `replayDate` over **eligible** games, AC-7.2)
  - filter summary
  - skipped list (`file`, `reason`) including `missing-phase-table` player-games
  - immutable

**Out of scope.** Formatting strings for display (S15 view-model builder). Rendering.

**Implementation notes.**
- Sums are `long`. Assert that the int counts fit when calling the `LarvaPhaseStats` constructor (`Math.toIntExact` is Java 8, so write a manual range check that throws `IllegalStateException` with context).
- A bucket with 0 games (OQ-4) still exists with all cells "no data" (`null` values), so the UI can render `ZvP — 0 games` with `n/a`.
- Every cell whose denominator is 0 yields `null`. The view model turns `null` into `n/a` (D-10, OQ-13).
- Averages of ms values: `long` sum / k, rounded half-up to whole ms.

**Acceptance criteria.**
1. Given synthetic player-games with known counts and loops, then every rate cell equals Σcount × 960 / Σloops (uptime Σactive × 100 / Σeligible) within 0.005 (AC-12.1).
2. The All bucket equals pooling the union of ZvP+ZvT+ZvZ inputs. A test with deliberately unbalanced loop totals proves it differs from the mean of the three bucket rates (AC-12.2).
3. Given no ZvP games, then the ZvP bucket has 0 games and all cells null (AC-12.3, OQ-4).
4. Given 3 of 5 games reach END with durations 60 s / 120 s / 180 s, then END phase length = 120 s with `3/5`. Given no game reached LATE, then LATE length is null with `0/5` (D-9, OQ-13).
5. Given 2 of 4 games reach 200 supply, then the cell = mean of those 2 with `2/4` (AC-13.1).
6. Given tracked A vs tracked B in one ZvZ game, then that game is in A's ZvZ/All and B's ZvZ/All, and `distinctGameCount` counts it once (D-7).
7. END ranking cells are `n/a`. EARLY/MID/LATE ranking strings equal the formatter's output for the pooled value.
8. `startDate`/`endDate` ignore excluded replays.

**Tests required.** Unit tests for all criteria with fixtures in `LarvaTestFixtures`. Include one hand-calculated worked example in the test Javadoc so reviewers can sign off (checkpoint "aggregation numbers signed off").

**Definition of done.**
- [x] Aggregator and result model added, immutable.
- [x] Unit tests green, ≥ 80 % line coverage (by inspection; 143 tests total, 19 new).
- [ ] Sign-off on the worked example recorded in the PR (checkpoint). Worked example is ready: see the Javadoc of `LarvaAggregatorTest`.

> **S11 result (2026-11-24): done, awaiting worked-example sign-off.** New (pure, no Scelight runtime): `LarvaAggregator` (`aggregate(eligible, tracked, summary)` plus an overload with prior skipped entries, e.g. failed files from S12), `LarvaAggregationResult` (with nested `PlayerResult`: `getGameCount/getPhaseCell/getMilestoneCell(bucket, ...)`), `LarvaAggregatedPhaseCell`, `LarvaAggregatedMilestoneCell`, `LarvaMatchupBucket` (ALL, ZVP, ZVT, ZVZ), `LarvaSkippedReplay`. **Seam:** `LarvaAggregationPlayerRecord` got `matchup` (`LarvaMatchupBucket`, null = unknown, Builder `matchup(...)`, `ALL` rejected). It is derived from `opponentRace` once, in `LarvaGameKinds.matchupOf(IRace)` (the only SDK comparison), called by `LarvaAggregationRecordBuilder`. The aggregator reads only `matchup`. "Played Zerg" needs no race check: S09 lists only Zerg players in `zergPlayers`, and the S10 selector relies on that too. Decisions: (1) pooled rates come from `new LarvaPhaseStats(...)` over summed counts; (2) a player-game with unknown matchup counts in ALL only; (3) `missingPhaseTable` player-games of tracked players are not counted (not in `n`, not in `distinctGameCount`) and are listed as `missing-phase-table` (`<file> (<name>)`); (4) `distinctGameCount` = distinct eligible games with at least one counted tracked player-game; (5) ranking cells are null when the rate is null, and END gives the formatter's `n/a`; (6) phase `n` = player-games in the bucket, `k` = player-games with a phase length > 0; (7) int overflow of summed counts throws `IllegalStateException` naming bucket/phase/field. New test helper `LarvaTestFixtures`. Tests: `LarvaAggregatorTest` (AC 1-8, worked example, empty bucket, missing table, rounding, overflow, immutability).

---

### E17-S13 — Reusable phase-table renderer

> **S13 result (2026-11-24): code, unit tests and parity checks done.** New (package-private unless noted): `LarvaTableViewModel` (immutable, defensive copies, `null` cell reads `n/a`, validates shape), `LarvaTablePainter` (`measure`, `paint`; colors, paddings 8, min widths 58 / 220, baselines 14 / 13, header 20, row 18 moved from `drawPhaseTable`; optional title line of 18 px above the table, baseline 13, new but unused by the single page), `LarvaTableComp` (JComponent for S15, preferred size from `measure`), `LarvaPhaseTableViewModels.fromPlayerPhaseTable(table, report)` (today's 7 rows; takes the `LarvaAnalysisReport` or `null` for the plain 16 loops/s fallback of the old `formatLoopTime`), `LarvaPhaseHeaderLabels.resolve`, `LarvaStatFormat` (`formatRate`, `formatPercent`, `formatMinSec`, `NOT_AVAILABLE`). `LarvaTimelinePreviewComp.drawPhaseTable` now builds the model and calls the painter; its vertical constants are unchanged (`PHASE_TABLE_HEIGHT` is now package-private = `HEADER_HEIGHT + ROW_HEIGHT * 7` = 146, a test asserts the painter returns it). `LarvaReplayPageComp.formatPhaseValue/formatPhasePercent` replaced by `LarvaStatFormat`. `LarvaAnalysisReport` has its own copies of `formatDouble/formatPhaseRate/formatPhasePercent` (golden text); left untouched on purpose (no analyzer changes), a later cleanup can point them at `LarvaStatFormat`. **Decisions:** (1) `measure` returns the ink box (+1 px for the right and bottom grid line), `paint` returns the layout height without that pixel (146 for the phase table). (2) `formatMinSec` is new (the old code had no standalone one); `formatRate` keeps the old quirk that `-0.5` prints `0.50` (sign lost for values in (-1, 0)), covered by a test. (3) The `USE_LEGACY_PHASE_TABLE` flag was not needed: the pre-change output was captured as images first (below) and the legacy code was deleted in the same story. **Parity (AC-4.1):** (a) reference PNGs rendered by the pre-S13 code (whole `LarvaTimelinePreviewComp`, synthetic two-player model with a full and an early-only table, fixed `Dialog` 12 font, widths 1000 and 420) stored in `test/resources/`; `LarvaPhaseTablePixelParityTest` compares every pixel: identical, no skip (it skips via assumption if the font fingerprint in `test/resources/font-fingerprint.txt` differs on another machine; regenerate with `-Dlarva.parity.writeDir`). (b) Old formatter outputs captured by reflection before moving them and pinned in `LarvaStatFormatTest` (`1.005` -> `1.00`, `-0.004` -> `0.00`, `null` -> `n/a`, both old classes gave the same strings). (c) Real Scelight: same replay (`queensMoved`, via `open-replay`), maximized window 1920x1152, screenshot of the pre-change 3.0 build against the new build: 0 differing pixels in the page area with timeline and phase table (y 220-1038); 76 scattered pixels in the tree/status header and 1321 in the status bar (memory meter, job counters) differ, as in any two runs. Screenshot: `knowhow/epic17-phase-table-parity.png`. Golden dev dump identical to the pre-change run except `lastUpdated`. **Tests:** 159 (16 new).

| Maps to | WP-12 · FR-4, FR-12, FR-13 (milestone table reuses it) · AC-4.1 · R-5 |
|---|---|
| Size | M |
| Depends on | S03 |

**Technical goal.** Pull the custom-painted phase table out of `LarvaTimelinePreviewComp` into a renderer driven by a string-matrix view model. The single page must look identical. The multi page can render the aggregated tables and the milestone table with the same code.

**Context.**
- `LarvaTimelinePreviewComp.drawPhaseTable(Graphics2D, FontMetrics, LarvaPlayerPhaseTable, left, top, width)` (≈ line 250) hard-codes 7 metric labels (`"Phase span"`, `"Inject uptime [%]"`, …), builds `cellTextMatrix`, sizes columns (`resolveMetricColumnWidth`, `resolvePhaseColumnWidths`), and paints with colors `PHASE_TABLE_HEADER_BG` / `PHASE_TABLE_ALT_BG` / `PHASE_TABLE_GRID_COLOR`.
- Headers come from `resolvePhaseHeaderLabel` (`Early game [<37]`, `Mid game [<67]`, `Late game [<90]`, `End game [90+]`).
- The table height is the fixed `PHASE_TABLE_HEIGHT`.

**Scope.**
- `LarvaTableViewModel` (immutable): optional `title`, `cornerLabel` (today `"Metric"`), `String[] columnHeaders` (any count ≥ 1), `String[] rowLabels`, `String[][] cells` (null → `n/a`).
- `LarvaTablePainter`: `Dimension measure(FontMetrics, model)` and `int paint(Graphics2D, FontMetrics, model, left, top, maxWidth)` (returns the painted height). The colors, paddings, min widths (58 / 220) and baselines are **moved**, not re-invented.
- `LarvaTableComp extends JComponent`: wraps the painter for Swing layouts (preferred size from `measure`). Used by S15.
- `LarvaPhaseTableViewModels.fromPlayerPhaseTable(LarvaPlayerPhaseTable)`: today's 7 rows exactly, used by `LarvaTimelinePreviewComp`.
- `LarvaPhaseHeaderLabels.resolve(phase)`: moved from `resolvePhaseHeaderLabel` and shared.
- `LarvaStatFormat`: shared `formatRate` (2 decimals), `formatPercent`, `formatMinSec(ms)`, `n/a` constant. Moved from the preview component and `LarvaReplayPageComp.formatPhaseValue`. Outputs must be identical.

**Out of scope.** Swing `JTable` migration (tech-debt note). Building the aggregated or milestone view models (S15).

**Implementation notes.**
- Keep `LarvaTimelinePreviewComp`'s vertical layout constants (`PHASE_TABLE_HEIGHT`, gaps) unchanged for the single page. Either assert that the painter's height for the 7-row model equals `PHASE_TABLE_HEIGHT`, or keep using the constant there.
- R-5 contingency: keep the old `drawPhaseTable` behind a private `static final boolean USE_LEGACY_PHASE_TABLE = false` until parity is signed off. Then delete it within this story.

**Acceptance criteria.**
1. Given the same replay, the single page's timeline and phase table are pixel-identical before and after. Check with a screenshot compare at the same window size and font, or by rendering both into a `BufferedImage` in a test and comparing (AC-4.1).
2. `fromPlayerPhaseTable` yields exactly today's row labels and cell strings for a synthetic phase table.
3. A model with 5 columns (Metric + All/ZvP/ZvT/ZvZ) and 4 rows renders with no clipping at the measured size.
4. `LarvaStatFormat.formatRate(1.005)`, `formatRate(-0.004)` and `formatPercent(null)` return the same strings as the old private methods (old values captured in the test).

**Tests required.**
- Unit: view-model content, formatter parity, and painter `measure` monotonic in row count (uses a `BufferedImage` graphics context, which works headless).
- Golden or visual: `BufferedImage` render comparison of old vs. new for a synthetic table, plus a manual screenshot of a real replay.

**Definition of done.**
- [x] Painter, component, view models and shared formatters added. The legacy path removed after parity.
- [x] Screenshot `knowhow/epic17-phase-table-parity.png`.
- [x] Unit tests green (159 tests).

---

## 5. Phase 3 — Multi page

### E17-S12 — Batch runner and headless `--multi` dump

| Maps to | WP-11 · FR-6 (progress), FR-15 · AC-6.2, AC-10.2, AC-15.1, AC-15.2 · §9 NFRs · R-3, R-4, R-6 |
|---|---|
| Size | M |
| Depends on | S09 (runner core), S11 (dump output), S02 (threading decision) |

**Technical goal.** Analyze N selected files in the background with progress, cancellation and per-file error capture, keeping only compact records. Provide a deterministic headless dump for integration testing.

**Scope.**
- `LarvaMultiAnalysisPipeline` (headless, no Swing). `LarvaAggregationResult run(List<Path> files, boolean ladderOnly, LarvaProgressSink sink, LarvaCancellationToken token)`. Per file:
  - optional pre-filter (S02/S09 `buildMetadataOnly`)
  - `analyzeForAggregation`
  - `LarvaEligibilityFilter`
  - then `LarvaTrackedPlayerSelector` + `LarvaAggregator` over the eligible records
- Catch per-file `RuntimeException` → `FAILED_TO_PARSE` plus a skipped entry (file name, message). `logger.error` with the throwable for unexpected errors, `debug` for filter exclusions.
- `LarvaMultiAnalysisRunner`:
  - wraps the pipeline on a worker (`module.factory.newNormalThread(name, runnable)` or a daemon thread; 1 worker or 2 per S02)
  - **one `ReplaySummaryService` per worker thread**
  - `LarvaMultiAnalysisListener` callbacks on the EDT: `onProgress(k, n, fileName, failedCount)` throttled ≥ 100 ms plus a final post, `onCompleted(result)`, `onCancelled()`, `onFailed(Throwable)`
  - `cancel()` sets the token, which is checked before each file (cancel latency ≤ one replay)
- R-6: gate `LarvaReplayAnalyzer.writePos2jlLog` (the `/tmp/larva-pos2jl.log` writes) and `ReplaySummaryService.logSpawnLarvaCommands` behind the dev flag (`DevDiagnosticDumpWriter.PROP_ENABLED` / system property). This is logging-only, and single-page outputs are unchanged.
- **Headless dump (epic §8):** add `--multi <dir>` and `--all-modes` to `LarvaBenchmarkCliExtractor` (or a sibling `LarvaMultiAnalysisCli`, preferred to keep the Epic 13 CLI untouched). It runs the pipeline over `*.SC2Replay` in the dir (non-recursive, sorted by name) and prints a deterministic text dump of `LarvaAggregationResult`: summary, tracked players, raw sums and loops per bucket and phase, formatted cells, milestones in loops and ms, skipped list.

**Out of scope.** UI (S14/S15). Showing partial results while running (R-3 contingency only).

**Implementation notes.**
- Memory: keep `List<LarvaAggregationGameRecord>` only. Null out the context and report references immediately (R-4). If S02 showed records are still too big, switch to streaming accumulation (R-4 contingency) and document it.
- Order of results must be deterministic (selection order) even with 2 workers.
- `onProgress` shows the file *being* analyzed: `k` = 1-based index of the current file.
- Never call `guiUtils.runInEDT` (invokeAndWait) while holding a lock. Throttle in the worker.

**Acceptance criteria.**
1. Given 100 replays, when the runner runs inside Scelight, then the user can navigate to other pages and back, and the progress keeps advancing (AC-15.1). No EDT stall > 200 ms (watch for UI freeze manually, or use a simple EDT heartbeat log in dev mode).
2. Given a run in progress, when `cancel()` is called, then no new file starts, `onCancelled` fires within one replay's time, and the log says "cancelled after k/N", with no exceptions (AC-15.2).
3. Given a corrupt file among valid ones, then it is counted `failed to parse` with its reason and the batch completes (AC-10.2).
4. Given the same folder twice, the `--multi` dump is byte-identical across runs.
5. Peak heap growth over 500 replays is ≤ 200 MB above baseline (§9), measured once and recorded.
6. With the dev flag off, `/tmp/larva-pos2jl.log` is not written during a run.
7. `LarvaBenchmarkCliExtractor` existing flags produce byte-identical output.

**Tests required.**
- Unit: pipeline with a fake `analyzeForAggregation` (interface seam): cancellation between files, failure capture, progress sequence and throttling with an injectable clock, deterministic ordering.
- Integration: `--multi benchmark-data/queensMoved/` dump committed as a golden under `test/golden/epic17-multi-queensMoved.txt`, and diffed in S15/S16.
- Manual / performance: criteria 1, 2, 5.

**Definition of done.**
- [ ] Pipeline, runner, listener, cancellation token, gating and `--multi` CLI added.
- [ ] Golden dump committed. Unit tests green.
- [ ] Performance numbers recorded in `knowhow/epic17-batch-spike.md` (append a section).

---

### E17-S14 — Multi-analysis page: chooser, creation, progress, label, cancel

| Maps to | WP-13 (part 1) · FR-5, FR-6, FR-7, FR-15 · AC-5.1, AC-5.2, AC-6.1, AC-6.2, AC-7.1, AC-7.2, AC-2.3, AC-15.2 · OQ-7, OQ-9, OQ-12, OQ-14 |
|---|---|
| Size | M |
| Depends on | S01 (rename outcome), S05, S12 |

**User story.** As a Zerg player, I want to pick many replays and get a new analysis page that shows its progress and can be closed at any time, so that I can run several analyses without losing earlier ones.

**Scope.**
- `LarvaHubActions.startMultiAnalysis(parent, ladderOnly)`:
  - Scelight file chooser (`guiFactory.newFileChooser()` → `asFileChooser()`): `setMultiSelectionEnabled(true)`, `FILES_ONLY` (OQ-9), filter `*.SC2Replay`, start folder = `lastDetectedReplayPath` parent when known (FR-5). Reuse `LarvaReplayFileChooser` from S06.
  - Cancel → no page (AC-5.2).
  - **> 500 files (OQ-14):** confirm dialog `Analyze N replays? Estimated time: ~M min.` (estimate = N × `ESTIMATED_MS_PER_REPLAY` from S02, divided by the worker count). No → no page.
- `LarvaMultiAnalysisPageController`: holds the runner, the state `{RUNNING, DONE, CANCELLED, FAILED}`, the result, and the progress snapshot. `dispose()` cancels the runner and drops the result (FR-15).
- `LarvaMultiAnalysisPageComp implements IPageDisposedListener`:
  - running view: `Analyzing k / N: <file name>`, an `IProgressBar` (`guiFactory.newProgressBar()`), `Failed: f` (AC-6.2)
  - on DONE it delegates to the S15 results view (until S15, a plain text dump of the result)
- Navigator `openMultiAnalysisPage(files, ladderOnly)`: **always a new** `LarvaSubPage(MULTI_ANALYSIS, multiAnalysisIcon)`, added and selected (AC-6.1). No dedupe for multi pages.
- **Label (OQ-7, per S01):**
  - Rename supported: `Multi analysis (running…)` while running. On DONE, `navigator.relabel(page, "from " + start + " to " + end)`.
  - Rename not supported: fixed `Multi analysis #k` (session counter).
  - On CANCELLED/FAILED, or with zero eligible games: keep the placeholder text without "(running…)", e.g. `Multi analysis #k (cancelled)` if rename works.
- `LarvaShortDateFormatter` (OQ-12):
  - `DateFormat.getDateInstance(DateFormat.SHORT, locale)`
  - strip trailing tokens that contain no digit (`24.11.26 г.` → `24.11.26`)
  - **new instance per call or per thread** (`DateFormat` is not thread-safe)
  - a single-day range renders `from d to d`

**Out of scope.** Results layout and tables (S15).

**Implementation notes.**
- The checkbox value is captured when the button is clicked (AC-1.2). Changing it later does not affect a running page.
- Relabel calls `rebuildPageTree(false)`, which re-fires `pageSelected` on the current page. Page components must tolerate that (they ignore it).
- Closing while running: `pageDisposed` → `controller.dispose()` → `runner.cancel()`. Late EDT callbacks for disposed controllers are ignored.

**Acceptance criteria.**
1. Given the hub, when the user clicks `Multi Analysis...`, then a multi-select chooser filtered to `*.SC2Replay` opens in the last detected replay folder. Ctrl/Shift and select-all work (AC-5.1). Cancel creates no page (AC-5.2).
2. Two consecutive confirmed selections create two sibling nodes with the multi icon. The earlier page is untouched (AC-6.1).
3. While running, the page shows `Analyzing k / N: <file>`, a progress bar and the failed count (AC-6.2).
4. With rename supported: given `bg_BG` default locale, the finished label matches `from dd.MM.yy to dd.MM.yy` with no `г.`. Given `en_US`, it matches `from M/d/yy to M/d/yy` (AC-7.1). Dates are the min/max of eligible games (AC-7.2).
5. With rename unsupported: the label stays `Multi analysis #k`, and the header (S15) carries the dates.
6. Given 501 selected files, then the confirm dialog shows an estimate. Declining creates no page.
7. Given a running page, when the user closes it, then parsing stops within one replay. The log shows cancellation, with no exceptions (AC-15.2, AC-2.3).

**Tests required.**
- Unit: `LarvaShortDateFormatter` for `bg_BG`, `en_US`, a single-day range, and the suffix-stripping regex on `"24.11.26 г."`, `"11/24/26"`, `"24.11.26"`. Estimate text for 501 files. Label state machine (running → done / cancelled / failed, rename supported or not).
- Manual: criteria 1–7. Run Scelight once with `-Duser.language=bg -Duser.country=BG` on the **bundled JRE** (OQ-12, R-9).

**Definition of done.**
- [ ] Chooser action, controller, page comp (progress view), navigator method, label logic and date formatter added.
- [ ] Unit tests green. Manual checks recorded with screenshots `knowhow/epic17-multi-running.png`.

---

### E17-S15 — Multi-analysis page: results layout and tables

| Maps to | WP-13 (part 2) · FR-8, FR-12, FR-13, FR-14, FR-15 · AC-8.1, AC-8.2, AC-12.3, AC-13.3, AC-14.1, AC-15.3 · OQ-1, OQ-3, OQ-4, OQ-13, OQ-15 · D-3 |
|---|---|
| Size | L |
| Depends on | S11, S13, S14 |

**User story.** As a Zerg player, I want my averaged larva tables and macro milestones laid out per player and matchup, so that I can see at a glance where I lose larva against each race.

**Scope.**
- `LarvaMultiAnalysisViewModelBuilder` (pure). It turns `LarvaAggregationResult` into:
  - **Header (FR-8):** `Averaged larva stats for {A, B} for {n} replays from {start} to {end}`, with `n = distinctGameCount` and dates from `LarvaShortDateFormatter`.
  - **Filter summary line:** `LarvaFilterSummary` text (AC-8.2).
  - **Per player:**
    - player-name heading
    - 4 phase tables (`LarvaTableViewModel`) titled `All games — N games`, `ZvP — N games`, `ZvT — N games`, `ZvZ — N games`. Rows:
      1. `Avg phase length [m:ss]` with `m:ss (k/n)`, `n/a (0/n)` when none reached (D-9, OQ-13)
      2. `Inject uptime [%]`
      3. `Larva gen [lrv / htch / min]`
      4. `larva gen ranking`
      5. `Larva missed [lrv / htch / min]`
      6. `larva missed ranking`
      7. `Inject-missed larva [lrv / htch / min]`
    - Columns are the shared phase headers (`LarvaPhaseHeaderLabels`). Values have 2 decimals (`LarvaStatFormat`), `n/a` when null.
    - **Then** the milestone table (OQ-3, **below** the 4 tables): rows `100 supply used`, `66 drones alive`, `80 drones alive`, `200 supply used`. Columns `All / ZvP / ZvT / ZvZ`. Cells `m:ss (k/n)` or `n/a (0/n)`.
  - **Skipped replays:** list of `file name — reason` (parse failures plus `missing-phase-table` player-games, OQ-15).
- `LarvaMultiAnalysisResultsPanel`:
  - header and summary at the top
  - a **2-column grid with equal widths** (`GridLayout(1, 2)` or `GridBagLayout` with equal weights)
  - each column stacks heading, the four `LarvaTableComp`s, and the milestone `LarvaTableComp`
  - **one player → left column filled, right column an empty placeholder of equal width** (OQ-1)
  - a collapsible "Skipped replays (x)" section (toggle button plus a list) at the bottom
  - vertical scrolling inside Scelight's page scroll pane
- **Empty state (AC-15.3):** zero eligible games, or no tracked player → header omitted, filter summary shown, plus the text `No eligible Zerg games in the selection`, no tables.
- A short help line under the header: "Ranking rows compare averages with per-game benchmark bands; averages sit closer to the middle." (D-3 caveat, R-10).

**Out of scope.** Charts, export, a player picker, a single-page milestone display (AC-13.3: none).

**Implementation notes.**
- Build the view model off the EDT (in the runner's completion path) and hand only the immutable view model to the EDT.
- Column alignment: both columns contain the same number of tables with the same row counts (OQ-4 keeps 0-game tables), so rows line up. With one player, the right placeholder keeps widths equal.
- Do not reuse `LarvaTimelinePreviewComp`. Use `LarvaTableComp` from S13.
- The table title is part of `LarvaTableViewModel.title`, painted by the painter, or a `JLabel` above it. Choose one and use it consistently.

**Acceptance criteria.**
1. Given 2 tracked players `A`, `B` and 37 distinct games, then the header reads `Averaged larva stats for A, B for 37 replays from … to …` (AC-8.1). The summary counts add up to `S` (AC-8.2).
2. Each player column shows, top to bottom: heading, All, ZvP, ZvT, ZvZ, Milestones (OQ-3, C-1).
3. Given no ZvP games, then `ZvP — 0 games` is shown with all cells `n/a` and the phase length row `n/a (0/0)` (AC-12.3, OQ-4).
4. Given one tracked player, then the player occupies the left half and the right half is empty, with the same table width as in the two-player case (OQ-1).
5. Given two players at 1280 px width, then both columns are fully visible side by side with no horizontal scroll of a single table (AC-14.1).
6. Given zero eligible games, then the page shows the filter summary and `No eligible Zerg games in the selection` with no tables (AC-15.3).
7. Given 2 corrupt files and 1 missing-phase-table player-game, then "Skipped replays (3)" expands to the 3 entries with reasons.
8. For the `queensMoved` golden folder, the cell strings equal the formatted values in the S12 `--multi` dump.
9. The single-replay page still shows no milestones (AC-13.3).

**Tests required.**
- Unit: view-model builder (header text with 1 and 2 players, 0-game bucket, `k/n` strings, milestone row order, empty-state decision, skipped list) from synthetic `LarvaAggregationResult`s.
- Golden: view-model text dump for the `queensMoved` folder matches the S12 golden dump's formatted section.
- Manual: criteria 2, 4, 5, 6, 7 at 1280 px and 1920 px. Screenshots `knowhow/epic17-multi-2players.png`, `knowhow/epic17-multi-1player.png`, `knowhow/epic17-multi-empty.png`.

**Definition of done.**
- [ ] View-model builder and results panel added. S14's interim text view removed.
- [ ] Unit and golden tests green. Screenshots committed.

---

## 6. Phase 4 — Polish and release

### E17-S16 — Help, knowhow, performance run and release validation

| Maps to | WP-14 · DoD (epic §11) · §9 NFRs · R-3, R-7, R-10 |
|---|---|
| Size | S |
| Depends on | all |

**Technical goal.** Document the new navigation and multi analysis, prove the performance targets, and confirm single-page parity before release.

**Scope.**
- `help/larva-module-help.html`:
  - hub actions
  - sub-pages (close with X or `Ctrl+F4`)
  - auto-open of new Zerg replays (FR-17 with the confirmed defaults)
  - multi analysis: filters, the 4-minute definition, tracked-player rule, pooled averaging, D-3 ranking caveat, milestone cadence caveat (R-7: supply milestones can be ~10 s late)
- `knowhow/epic17-subpages.md`: the hub and sub-page pattern (module-owned `IPage`, child list as registry, lazy components, rename, EDT rules), for reuse by Epic 14.
- Performance run: 100 and 500 replays. Record wall time, peak heap and EDT responsiveness in `knowhow/epic17-batch-spike.md`.
- Full single-page regression: golden dump diff on ≥ 3 replays against the pre-epic build. Manual check of the buttons, Refresh, errors and help.
- Release: `ant CLEAN TEST BUILD_RELEASE` succeeds. Install into Scelight. No init errors. The jar contains no test classes.
- Bump `release/release.properties` version as per the usual release routine. Update `prompts/v3-spec.md` items 6 and 7 status if that is the convention.

**Acceptance criteria.**
1. Help HTML covers each item above and renders in Scelight's help viewer.
2. A 100-replay run completes within the §9 target (≤ 2.5 min at ≤ 1.5 s per replay, or the S02 re-baselined target) without UI freezes. 500-replay peak heap growth is ≤ 200 MB.
3. Golden diffs for single pages are empty. Closing any sub-page releases its work (cancellation and drop logs seen).
4. `BUILD_RELEASE` succeeds against SDK 1.5.2 and the module loads cleanly.
5. Every "default — confirm with user" item (S07 D17-a…j) is confirmed or explicitly listed as an open follow-up in the epic.

**Tests required.** Full `ant TEST`. Golden diffs. Manual release checklist.

**Definition of done.**
- [ ] Help, knowhow notes and screenshots committed.
- [ ] Performance and regression results recorded.
- [ ] Epic §11 DoD items all ticked (the user adds FR-17 to the epic).

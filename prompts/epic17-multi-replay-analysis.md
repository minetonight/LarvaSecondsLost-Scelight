# Epic 17 — Multi-replay larva analysis and Larva hub rework

| | |
|---|---|
| **Status** | Ready for story planning |
| **Created** | 2026-11-24 |
| **Source** | [prompts/epic17-draft.md](epic17-draft.md) (authoritative requirements), [prompts/v3-spec.md](v3-spec.md) items 6 and 7 |
| **Builds on** | Epic 12 phase tables, Epic 13 headless extraction / benchmark bands |
| **Estimated size** | 12–16 dev days across 14 work packages (incl. 2 spikes) |

---

## 1. Executive summary

Today the module has one non-closable `Larva` page that always shows a single replay. Epic 17 makes `Larva` a **hub** (buttons only) with **closable sub-pages** in the Scelight navigation tree, like Scelight's own *Replay Analyzers*. There are two kinds of sub-page:

1. **Single-replay pages**: the current replay overview, one page per opened replay, named after the replay file.
2. **Multi-analysis pages**: the user selects many replays. The module filters them, finds the (up to two) most common Zerg players, and shows **averaged phase tables** (all games plus ZvP / ZvT / ZvZ) and new **milestone timings** (100 / 200 supply used, 66 / 80 drones alive).

### Goals
- Primary: aggregated larva stats per Zerg player over an arbitrary set of replays.
- Secondary: a multi-document navigation model (hub plus closable sub-pages) that later epics (e.g. the Epic 14 player stats window) can reuse.

### Non-goals
- No change to single-replay metric definitions, phase detection thresholds or benchmark anchors.
- No charts over time or trend lines (Epic 14 territory).
- No persistence of open sub-pages across Scelight restarts.
- No folder-recursive or Scelight *Replay List* integration for selection (file chooser only; see OQ-9).
- No team games (2v2+), no non-Zerg player stats.
- No export (CSV/JSON) of aggregated results.

---

## 2. Glossary

| Term | Meaning |
|---|---|
| Hub | The `Larva` top-level tree node. It is non-closable and only shows buttons. |
| Sub-page | A closable child page of the hub in the Scelight left tree. |
| Selected replays | Files picked in the multi-select chooser. |
| Eligible game | A selected replay that passes all filters (FR-10). |
| Tracked player | One of the up to two most common Zerg identities across eligible games (FR-11). |
| Player-game | One tracked player's participation as Zerg in one eligible game. This is the unit of aggregation. |
| Pooled rate | Σ counts / Σ eligible loops across player-games. It uses the same formula as `LarvaPhaseStats`. |
| Milestone | The first loop at which a player reaches a threshold (supply used or alive drones). |

---

## 3. Functional requirements and acceptance criteria

### A. Navigation and hub

**FR-1 Larva hub page.** The `Larva` page shows only controls, horizontally centered and stacked vertically:
1. `Open Replay...`
2. `Analyze Latest Replay`
3. `Multi Analysis...` with a `?` tip icon on one side and a `Ladder games only` checkbox on the other. The checkbox is checked by default.

The tip says: *select multiple replays; you will receive aggregated larva stats for the most common Zerg player(s) across them.*
- [ ] AC-1.1 Given Scelight starts, when the user selects `Larva`, then only the controls above are visible (no timeline, no text area) and they are centered.
- [ ] AC-1.2 The checkbox is checked on first display. Its state is used by the next Multi Analysis request (persistence across restarts: OQ-8).
- [ ] AC-1.3 Hovering the `?` icon shows the tooltip text.
- [ ] AC-1.4 Selecting the hub never triggers replay analysis by itself (today's auto-load-on-show is removed from the hub).

**FR-2 Sub-pages in the tree.** Sub-pages appear as children of `Larva`, in creation order. Creating one selects it. Each sub-page has the X close button in the page title bar (Scelight-native, also `Ctrl+F4`).
- [ ] AC-2.1 After a sub-page is created, the tree shows it indented under `Larva`, and it is selected and visible.
- [ ] AC-2.2 The X button is enabled on sub-pages and disabled on the hub.
- [ ] AC-2.3 Closing a sub-page removes it from the tree and selects the previous page from history, or the hub. Its background work is cancelled and its memory released (FR-15).
- [ ] AC-2.4 Closing one sub-page leaves the others intact. Sub-pages stay until the user closes them or Scelight exits.

**FR-3 Single-replay sub-pages.** `Open Replay...` and `Analyze Latest Replay` (on the hub *and* on any single-replay page) each create a **new** single-replay sub-page. The page is named after the replay file name and uses `larva-replay-icon.png`.
- [ ] AC-3.1 Given the user picks `X.SC2Replay`, a child node labelled `X` (extension stripped, OQ-6) with the replay icon appears and starts in the busy state.
- [ ] AC-3.2 If no latest replay can be resolved, the existing info message is shown and no page is created.
- [ ] AC-3.3 If parsing fails, the page stays open and shows the existing error state (same behavior as today, but scoped to that page).
- [ ] AC-3.4 If a single-replay page for the chosen replay is already open, that page is selected instead of creating a duplicate (OQ-6). An open page is never re-rendered with another replay's data.

**FR-4 Single-replay page content is unchanged.** The current overview (header, the three buttons, timeline preview, phase tables, text details) is kept as is. Only the scope changes:
- `Refresh Current Replay` re-analyzes **this page's** replay in place. It does not create a new page.
- A page is bound to one replay. It never switches to another replay automatically (no auto-latest on `pageSelected` / show).
- [ ] AC-4.1 For the same replay, the old `Larva` page and a new sub-page render identical timeline, phase table and text output (golden compare).
- [ ] AC-4.2 Two open single-replay pages for different replays keep their own content when the user switches between them.
- [ ] AC-4.3 Refresh on page A does not change page B.

### B. Multi analysis

**FR-5 Replay selection.** `Multi Analysis...` opens a Scelight file chooser in multi-selection mode, filtered to `*.SC2Replay`. It starts in the last-detected replay folder when one is known.
- [ ] AC-5.1 The user can select 1..N files, using ctrl/shift selection and select-all.
- [ ] AC-5.2 If the user cancels, no page is created.

**FR-6 One new page per request.** Each confirmed selection creates and selects a **new** multi-analysis sub-page with `larva-multy-analysis-icon.png`. The page shows progress while it runs. Earlier multi pages are never reused or overwritten.
- [ ] AC-6.1 Two consecutive requests produce two sibling nodes.
- [ ] AC-6.2 While running, the page shows `Analyzing k / N: <file name>`, a progress bar and a running count of failed replays.

**FR-7 Tree label.** When analysis completes, the node label is `from <start> to <end>`. Dates use `DateFormat.SHORT` in the user's locale, for example `from 03.11.26 to 24.11.26` (bg) or `from 11/3/26 to 11/24/26` (en_US). While the analysis is running the label is a placeholder (OQ-7).
- [ ] AC-7.1 With the default locale set to `bg_BG` and to `en_US`, the label matches that locale's short date pattern.
- [ ] AC-7.2 `start` and `end` are the earliest and latest replay dates among **eligible** games.

**FR-8 Page header.** The header reads `Averaged larva stats for {names, comma-separated} for {n} replays from {start} to {end}`. Notes:
- `n` is the number of distinct eligible games that include at least one tracked player.
- Dates use the same format as FR-7.

Below the header, a one-line **filter summary** shows: `selected S · eligible E · excluded: not 1v1 a, non-ladder b, < 4 min c, no Zerg d, failed to parse f`.
- [ ] AC-8.1 Given 2 tracked players `A`, `B` and 37 games, the header reads `Averaged larva stats for A, B for 37 replays from … to …`.
- [ ] AC-8.2 The filter summary counts add up to `S`.

**FR-9 Replay dates come from replay content.** Dates come from the replay's own timestamp (`IDetails.getTime()`), never from file mtime.
- [ ] AC-9.1 A replay whose file mtime was changed (`touch`) keeps its in-game date in the header and tree label.

**FR-10 Filtering.** A selected replay is eligible only if all of these hold:
1. It parses (the full event parse succeeds).
2. Format is 1v1 (`IRepProcessor.getFormat() == IFormat.ONE_VS_ONE`).
3. If `Ladder games only` is checked, game mode is AutoMM (`IAttributesEvents.getGameMode() == IGameMode.AMM`). A missing or unknown mode counts as non-ladder. If unchecked, every mode is accepted (AMM, Private, Public, Single player, …).
4. Game length is ≥ 4:00 (threshold definition: OQ-5).
5. At least one player is Zerg.
- [ ] AC-10.1 A mix of 3 AMM, 2 Private and 1 3-minute AMM replay with the checkbox checked gives 3 eligible. Unchecked, it gives 5.
- [ ] AC-10.2 A corrupt file is counted as `failed to parse` and does not abort the batch.

**FR-11 Tracked player identification.** Across eligible games, the module counts player-games per Zerg identity (§5 D-5) and selects at most two tracked players (rule in §5 D-6).
- [ ] AC-11.1 With 40 games of `Scorpion` (Zerg) and ZvZ opponents appearing at most twice each, the only tracked player is `Scorpion`.
- [ ] AC-11.2 With 20 games on account A and 15 on account B, both are tracked and A is listed first.
- [ ] AC-11.3 A game where the tracked identity played Terran or Protoss is not counted for that player.

**FR-12 Aggregated phase tables.** For each tracked player, the page shows, top to bottom:
1. **All games** table
2. **ZvP** table
3. **ZvT** table
4. **ZvZ** table

Every table has the same columns as the existing phase table (Early / Mid / Late / End, same header labels as `LarvaTimelinePreviewComp`). Rows, in order:

| # | Row | Source |
|---|---|---|
| 1 | Avg phase length [m:ss] (`k/n` games reached) | replaces "Phase span" |
| 2 | Inject uptime [%] | pooled |
| 3 | Larva gen [lrv / htch / min] | pooled |
| 4 | larva gen ranking | `LarvaPhaseBenchmarkRatingFormatter` on row 3 |
| 5 | Larva missed [lrv / htch / min] | pooled |
| 6 | larva missed ranking | formatter on row 5 |
| 7 | Inject-missed larva [lrv / htch / min] | pooled |

Each table title shows its game count, e.g. `ZvT — 14 games`. Numbers use two decimals (v3-spec 3.7). Cells with no data show `n/a`.
- [ ] AC-12.1 Given synthetic player-games, each cell equals the pooled formula in D-1/D-2 within 0.005.
- [ ] AC-12.2 The All-games table equals pooling the three matchup tables' inputs together, not averaging the three tables.
- [ ] AC-12.3 A matchup with 0 games is handled as in OQ-4 (default: table shown with title `ZvP — 0 games` and all cells `n/a`).

**FR-13 Milestone stats (multi page only).** For each tracked player, the page shows a milestone table with rows:
- 100 supply used
- 66 drones alive
- 80 drones alive
- 200 supply used

Columns are All / ZvP / ZvT / ZvZ. Each cell shows `avg m:ss (k/n)`, where `k` is the number of player-games that reached the milestone. Supply used means **population actually used**, not supply cap from overlords. Drones alive uses the existing alive-drone counting that drives phase detection.
- [ ] AC-13.1 A game where the player never reaches 200 supply is excluded from that cell's average and counted in `n` but not `k`.
- [ ] AC-13.2 The 66-drone milestone loop for a fixture replay is within one `PlayerStats` interval of the loop where the analyzer's alive-drone count first reaches 66. If the exact-loop variant is chosen, it is equal (D-4).
- [ ] AC-13.3 The single-replay page does not show milestones.

**FR-14 Layout.** Two equal-width columns, one per tracked player, each with a player-name heading. The heading is followed by the All-games table, the three matchup tables (ZvP, ZvT, ZvZ) and, last, the milestone table (OQ-3). With one tracked player, see OQ-1 (default: the player uses the left column and the right column stays empty, so table widths match the two-player case). The page scrolls vertically inside Scelight's page scroll pane.
- [ ] AC-14.1 With two players, both columns are visible side by side at 1280 px width without horizontal scrolling of a single table.

**FR-15 Background execution, progress and errors.**
- Analysis runs off the EDT.
- The UI stays responsive.
- Per-replay failures are collected and listed (file name and reason) in a collapsible "Skipped replays" section.
- Closing the page cancels the run within one replay's processing time.
- [ ] AC-15.1 During a 100-replay run the user can navigate to other pages and back. The progress text keeps advancing.
- [ ] AC-15.2 Closing the page mid-run stops further parsing. Logs show cancellation and there are no exceptions.
- [ ] AC-15.3 Zero eligible games leads to a page showing the filter summary and `No eligible Zerg games in the selection`, with no tables.

**FR-16 Icons.** The hub keeps `icon/larva-module-icon.png`. Single-replay pages use `icon/larva-replay-icon.png`. Multi pages use `icon/larva-multi-analysis-icon.png`. 
- [ ] AC-16.1 Each node type shows its icon in the tree and in the page title bar (at 16 px and at larger title font sizes).
- [ ] AC-16.2 A missing icon resource fails module init with the existing clear "missing packaged resources" message.

### C. Replay monitor

**FR-17 Auto-open new Zerg replays** (added from OQ-10; open details and their defaults are in `epic17-stories.md` E17-S07). When Scelight's replay folder monitor reports a new replay (`INewRepListener.newRepDetected`) that contains at least one Zerg player, the module adds a single-replay sub-page for it under the `Larva` hub. The page uses the FR-3 name and icon and the FR-4 content, and its analysis runs in the background.
- [ ] AC-17.1 When a new replay with a Zerg player is saved into a monitored folder, a child node named after the file (extension stripped) with `larva-replay-icon.png` appears under `Larva`. It shows busy first, then the normal single-replay content.
- [ ] AC-17.2 A new replay with no Zerg player creates no page. `Analyze Latest Replay` can still resolve it.
- [ ] AC-17.3 If a single-replay page for the same file is already open, no second page is created and the replay is not analyzed again (OQ-6).
- [ ] AC-17.4 The new page is selected only under the focus rule (E17-S07 D17-c). Otherwise it is only added to the tree.
- [ ] AC-17.5 A flood of new replays creates at most the capped number of pages (E17-S07 D17-e). Skipped replays are logged.
- [ ] AC-17.6 No replay parsing happens on the EDT or on Scelight's replay-handler job thread. The listener returns immediately.
- [ ] AC-17.7 Already open pages never change their replay (FR-4).
- [ ] AC-17.8 A replay detected before the hub is installed, or after module `destroy()`, creates no page and does not throw.

---

## 4. Codebase grounding (current state → Epic 17 impact)

### 4.1 Module code (`LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/`)

| Class | Today | Impact |
|---|---|---|
| `LarvaSecondsLostModule` | `installReplayPage()` calls `getMultiPageComp().addPage(guiFactory.newPage("Larva", larvaIcon, false, creator))` and then `rebuildPageTree(false)`. It holds a **singleton** `replayPageComp` and `latestReplaySummary`. `requestReplayAnalysis()` starts a raw daemon `Thread` and pushes results into that singleton. The `INewRepListener` eagerly analyzes every new replay. | Replace with a hub page plus a page navigator/registry. Move result routing per page. Keep the replay monitor and auto-open new Zerg replays (OQ-10, FR-17). Load the two new icons in `loadResources()`. |
| `LarvaReplayPageComp` | Implements `IPageSelectedListener` and `HierarchyListener`, both of which auto-load the latest replay. Holds the buttons, a `LarvaTimelinePreviewComp` and the text area. The buttons call `module.requestReplayAnalysis`. | Bind each instance to one replay `Path`. Remove auto-latest. Open/Latest call the navigator (new sub-page). Refresh stays in place. Add `IPageDisposedListener` for cleanup. |
| `LarvaTimelinePreviewComp.drawPhaseTable(...)` (≈ line 250) | The phase table is **custom-painted** with Graphics2D inside the timeline component. Labels are hard-coded (`"Phase span"`, `"Inject uptime [%]"`, …), and it reads `LarvaPlayerPhaseTable` directly. Header labels come from `resolvePhaseHeaderLabel`. | Extract a reusable phase-table renderer or component that takes a **string-matrix view model** (row labels × 4 phases). Single page keeps its visuals; the multi page reuses it with the "Avg phase length" row. |
| `LarvaPlayerPhaseTable`, `LarvaPhaseStats`, `LarvaPhaseInterval`, `LarvaGamePhase` | `LarvaPhaseStats` stores **raw counts and loop denominators** (`missedLarvaCount`, `missedInjectLarvaCount`, `totalSpawnedLarvaCount`, `hatchEligibleLoops`, `injectEligibleLoops`, `injectActiveLoops`) and derives rates using 16 loops/s. Phases are promoted at workers **>** 36 / 66 / 89. | Pooled aggregation can **construct a summed `LarvaPhaseStats`** and reuse its rate getters unchanged (D-1). |
| `LarvaReplayAnalyzer` | The alive-drone count lives in local `aliveDroneCountByPlayerName` (`incrementDroneCount` / `decrementDroneCount`, ≈ lines 3094–3113, fed by UnitBorn/Init/Died/TypeChange). The `PlayerStats` handler (≈ line 286) stores `LarvaPlayerResourceSnapshot(foodUsed, foodMade, workerCountToUse = calculated drone count)`. `resolvePhasePromotionLoop` (≈ line 2157) scans snapshots for `workers > threshold`. `PHASE_PROMOTION_DWELL_SECONDS` is declared but not used by that scan. | Milestones: either scan existing snapshots (zero analyzer change) or record the exact first loop inside the drone counter (D-4). `foodUsed` is **fixed-point ×4096** (see `LarvaTimelineModelBuilder` ≈ line 818). Note: debug code writes `/tmp/larva-pos2jl.log` per snapshot whenever a hatchery tagged `2jl` exists. This costs I/O in batch runs (R-6). |
| `LarvaAnalysisReport` | Exposes `getResourceSnapshotsByPlayerName()`, `getPlayerPhaseTable(name)`, `getReplayLengthLoops()`, `isRealTime()`, `getConverterGameSpeedRelative()`, and the loop→display-ms conversion (≈ line 416). | Source for per-game records and for formatting milestone and phase-length times consistently with the single page. |
| `ReplaySummaryService` | `analyzeReplayContext()`: `getRepProc` → reparse if events are missing → analyzer → `ReplaySummary`. It also has `analyzeBenchmarkReplay()` (headless, no UI model) and helpers such as `indexPlayersByName`, `findOpponent` (first other player, 1v1 assumption), `buildMatchupLabel`, and Zerg-only filtering. It holds a non-thread-safe `SimpleDateFormat`. | Add a headless `analyze…ForAggregation()` path that reuses the context and emits a compact per-game record (D-8). Use **one service instance per worker thread**. |
| `ReplaySummary` | `replayEndTime` is a **formatted String**. `lengthMs` follows the user's real-time setting. | The multi analysis needs a raw `Date` and length in loops. Carry them in the new record, not by parsing strings. |
| `LarvaBenchmarkExtractionService` / `LarvaBenchmarkReplayRecord` / `LarvaBenchmarkPlayerRecord` / `LarvaBenchmarkPhaseRecord` | Epic 13 headless export with raw per-phase counts, matchup and race. It is consumed by the CLI (`LarvaBenchmarkCliExtractor`), the JSON serializer and the Python batch tools. | Good template, but **do not change its schema** (it is the benchmark-tool contract). Add a sibling record type instead (D-8). |
| `LarvaPhaseBenchmarkRatingFormatter` | Anchors exist for EARLY, MID and LATE only. **END returns `n/a`.** They are not matchup-specific and are derived from per-game medians (v3-spec 3.5). | Reuse as is for averaged values (D-3). END ranking stays `n/a`. |
| `LatestReplayResolver` | Resolves the latest replay from the monitor event or a folder scan. | Unchanged. It is used by the hub and the single page "Analyze Latest". |
| `StandaloneScelightBootstrap`, `LarvaBenchmarkCliExtractor` | Headless Scelight bootstrap and per-replay CLI. | Basis for an integration harness / CLI dry run of the multi aggregation. |

**Build constraint:** `build.xml` compiles with `source="1.7" target="1.7"` against `ScelightExtModSDK/Scelight-ext-mod-api/1.5.2/lib/scelight-ext-mod-api-1.5.2.jar`. That means **no lambdas or streams, and avoid `java.time`**. Use `java.text.DateFormat`. There is **no unit-test infrastructure** (no JUnit in `lib/`, no test sources).

### 4.2 Scelight external-module SDK findings

Verified in `scelight/src-launcher`, `scelight/src-app`, `scelight/src-ext-mod-api` and the 1.5.2 javadoc.

| Capability | Finding | Confidence |
|---|---|---|
| Add a top-level page | `IMultiPageComp.addPage(IPage)` plus `rebuildPageTree(boolean)`. This is used today. | Verified |
| Child pages | `IPage.getChildList()` drives tree building (`MultiPageComp.buildPageTree` recurses), and `IPage.setParent/getParent` exist. **The API has no `addChild`.** `IGuiFactory.newPage(...)` returns an anonymous internal `BasePage` whose `childList` is `null`, and `BasePage.addChild` is internal (`hu.sllauncher`), not API. | Verified |
| → Recommended approach | The module **implements `IPage` itself** for the hub (`LarvaHubPage`) with its own mutable `ArrayList` child list. Children are created via `guiFactory.newPage(name, icon, true, creator)` or a module `IPage` impl, with `setParent(hub)`, then `rebuildPageTree(false)` and `selectPage(child)`. This mirrors Scelight's `RepAnalyzersPage.newRepAnalyzerPage`. No Scelight code casts pages to `BasePage` (grep verified). | High; confirm in spike WP-01 |
| Close (X) button | `MultiPageComp` enables the title-bar close button iff `page.isCloseable()`. `removeSelectedPage()` calls `IPageClosingListener.pageClosing()` (veto), removes the page via `parent.getChildList().remove(...)` (**so the list must be mutable**), calls `IPageDisposedListener.pageDisposed()`, then rebuilds. **It throws `UnsupportedOperationException` if the closed page has children**, so the hub must stay non-closable. `Ctrl+F4` is also bound. | Verified |
| Renaming a node after creation | The label comes from `getDisplayName()` when the tree is built, and the title label is set on selection. A **module-owned `IPage` impl with a mutable name** plus `rebuildPageTree(false)` should relabel it. The rebuild re-fires selection, which calls `IPageSelectedListener.pageSelected()` again. `newPage(...)` names are immutable. | Medium; spike WP-01 |
| Multi-file selection | `IGuiFactory.newFileChooser()` → `IFileChooser.asFileChooser()` returns a plain `JFileChooser`, so `setMultiSelectionEnabled(true)` and `getSelectedFiles()` work. | High |
| Icons | `guiFactory.newRIcon(URL)` (used today). The tree and title call `ricon.size(n)`. | Verified |
| Tooltip "?" icon | `IGuiFactory.newTipIcon(IRHtml)` (and `newHelpIcon`, used today). | Verified (API) |
| Progress | `IGuiFactory.newProgressBar()`, `IFactory.newProgressJob(name, ricon, runnable)` / `newNormalThread`. | Verified (API); usage pattern to settle in WP-11 |
| Game mode | `IReplay.getAttributesEvents().getGameMode()` → `IGameMode.AMM / PRIVATE / PUBLIC / SINGLE_PLAYER / UNKNOWN`. | Verified (API) |
| Format | `IRepProcessor.getFormat()` → `IFormat.ONE_VS_ONE`, … | Verified (API) |
| Replay date | `IDetails.getTime()` (`Date`), already read in `ReplaySummaryService`. | Verified |
| Player identity | `IPlayer.getToon()` / `IUser.getToon()` (`IToon`), `IPlayer.getRace()` (resolved race, including Random picks). | Verified (API) |
| Cheap pre-filter | `repParserEngine.getRepProc(path)` may return a **cache-only** processor without events. The module then reparses. Whether the cached processor carries attributes (game mode), details and length is **unverified**. | Unknown; spike WP-02 |

---

## 5. Aggregation semantics (decisions)

Each item is a default decision with its rationale. Planners may revisit, but stories should implement these unless overruled.

| ID | Topic | Decision | Rationale |
|---|---|---|---|
| D-1 | Rate rows (larva gen, larva missed, inject-missed) | **Pooled**: Σ count × 960 / Σ `hatchEligibleLoops` over the bucket's player-games, per phase. Implement by building a summed `LarvaPhaseStats` and calling its existing getters. | Same definition as the single-game value. Time-weighted, so a 20-second END phase in one game cannot outweigh a 6-minute END elsewhere. Zero code duplication of formulas. |
| D-2 | Inject uptime | Pooled: Σ `injectActiveLoops` / Σ `injectEligibleLoops`. | Same as D-1. |
| D-3 | Ranking rows | Apply `LarvaPhaseBenchmarkRatingFormatter` to the pooled value. END stays `n/a` (no anchors). | Reuses the bands. Caveat (add to help text): bands are per-game medians, so an average sits closer to the middle than single games do. |
| D-4 | Milestone detection | Milestone loop = first `PlayerStats` snapshot where `foodUsed/4096 ≥ 100` (resp. `≥ 200`) or snapshot worker count (calculated alive drones) `≥ 66` (resp. `≥ 80`). **Preferred upgrade:** record the exact loop of the first increment that reaches 66 or 80 inside the analyzer's drone counter. Supply stays snapshot-based. | Snapshots exist today. `PlayerStats` cadence is about 10 s of game time, so up to one interval of lateness. Note `≥` here versus `>` for phase promotion: "66 drones made" means reaching 66. |
| D-5 | Player identity across replays | Key = toon (`IToon` string: region/realm/id). Fall back to name only if the toon is missing. Display name = name in the most recent eligible game. | Names change and clan tags vary. Toons are stable per account. Within a replay, the analyzer keys by name, so map toon→name per replay. |
| D-6 | Choosing up to two tracked players | Rank Zerg identities by player-game count. Ties go to the more recent last game, then case-insensitive name. #1 is always tracked. #2 is tracked only if count ≥ 3 **and** ≥ 20 % of eligible games. | Without a threshold, any random ZvZ opponent seen twice would become "player 2". |
| D-7 | Bucket membership | Matchup comes from the tracked player's view, using the opponent's resolved race (Random resolved). If two tracked players faced each other, the game counts in **both** players' ZvZ and All tables. Header `n` counts that game once. Games where the identity was not Zerg are ignored for that identity. | Each player-game stands alone. |
| D-8 | Per-game record | A new immutable compact record per eligible game contains: path, `Date`, game mode, format, length loops, and per Zerg player: toon, name, race, opponent race, per-phase interval and raw `LarvaPhaseStats` counts, milestone loops. Discard the full `LarvaAnalysisReport` after extraction. | Bounded memory for 100–1000 replays. Keeps the Epic 13 schema untouched. |
| D-9 | Phase length row | Mean of `LarvaPhaseInterval.getDurationLoops()` over player-games where duration > 0, formatted `m:ss`, plus `k/n` reached. | Absent phases (game ended in Mid) must not drag the mean toward 0. |
| D-10 | Absent data | Any cell whose denominator is 0 shows `n/a`. Milestones and phase length never treat "not reached" as game end (no censoring). `k/n` is always shown. | Transparent. Avoids fake precision. |
| D-11 | Time display | Format loops with the same conversion as the single page (`LarvaAnalysisReport` loop→ms, honoring Scelight's real-time setting). | Milestones and phase lengths stay comparable with the single-replay page. |

---

## 6. Ambiguities and open questions (with recommended defaults)

| ID | Question | Recommended default |
|---|---|---|
| OQ-1 | "If the players are two, use a second column, for one player, use only the left half of the window." | Always a 2-column grid. Player 1 goes left, player 2 goes right. With one player, use the **left** column only and leave the right empty. Updated and decided! |
| OQ-2 | Tie-breaking and threshold for "most common Zerg player(s)". | D-6 (≥ 3 games and ≥ 20 %; ties by recency, then name). Identity by toon, display name from the most recent eligible game (D-5). Scelight's "favored player" setting is **not** used (no reflective settings lookup). Decided! |
| OQ-3 | Where the milestone table goes and which columns it has. | Its own table per player, **below** the other 4 larva tables. Columns All / ZvP / ZvT / ZvZ, cells `avg (k/n)`. Decided! |
| OQ-4 | Show or hide matchup tables with 0 games. | Show them with title `— 0 games` and `n/a` cells, so the layout stays stable and the two columns align. Decided! |
| OQ-5 | What "4 minutes" means: SC2 LotV in-game clock (22.4 loops/s → 5 376 loops) or module "gameplay time" (16 loops/s → 3 840 loops)? | **5 376 loops**: matches what players see as game length in the SC2 client and Scelight's real-time display. Implement as a named constant. Decided! |
| OQ-6 | Single-replay node name: full file name or without `.SC2Replay`? And what happens when the same replay is opened twice? | Strip the extension. Re-opening an already open replay **selects the existing page** instead of creating a duplicate. Confirmed and Decided! |
| OQ-7 | Tree label while analysis runs (dates unknown yet). | `Multi analysis (running…)`, renamed on completion (depends on WP-01 rename capability). Fallback if renaming is impossible: keep a fixed label `Multi analysis #k` and show dates only in the header. Confirmed: Depends on API availability. |
| OQ-8 | Persist the `Ladder games only` checkbox across restarts? | No persistence. Always defaults to checked (matches the draft). Decided! |
| OQ-9 | Select folders (recursive) in addition to files? | Files only. A folder option is a follow-up. Decided |
| OQ-10 | Replay folder monitor: today it eagerly analyzes each new replay into the singleton page. | Keep the listener! It auto-creates a sub-page for a newly detected zerg replay. Decision: A story creation is needed! |
| OQ-11 | Is supply "used" taken from tracker `PlayerStats` `foodUsed`? | Yes. `foodUsed` is fixed-point /4096, includes units in production (eggs), and never counts overlord-provided cap (`foodMade`). Cadence caveat in D-4. Decided! |
| OQ-12 | Locale short date format: the JVM default `DateFormat.SHORT` may render `bg` as `24.11.26 г.` on CLDR-based JREs (Java 9+), and the year can be 2-digit. | Use `DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())` as is. If the `г.` suffix appears, strip trailing non-digit tokens. Verify on the JRE bundled with Scelight. Confirmed! |
| OQ-13 | Meaning of "lategame/endgame" columns when the phase never occurs. | Same columns as today (Late = workers > 66, End = workers > 89). The cell shows `n/a` when no player-game in the bucket reached the phase, and phase length shows `0/n`. Confirmed! |
| OQ-14 | Upper limit on selection size. | No hard limit. Show a confirm dialog above 500 files with an estimated duration. Confirmed! |
| OQ-15 | Should eligible-but-unparsable or partially analyzed games (e.g. missing phase table) count in `n`? | Failed parse → excluded and listed. Missing phase table for a tracked player → that player-game is excluded and listed with reason `missing-phase-table` (existing diagnostic flag). Confirmed! |

---

## 7. Candidate stories / work packages

Sizes: S ≤ 1 day, M 1–2 days, L 2–3 days. "FR" = acceptance pointers.

| ID | Title | Purpose | Touched areas | Depends on | FR | Size |
|---|---|---|---|---|---|---|
| WP-01 | **Spike: module-owned hub `IPage` with closable children** | Prove add / select / close (X and Ctrl+F4) / dispose / rename-and-rebuild, plus history behavior, in real Scelight with SDK 1.5.2. Record findings in `knowhow/`. | throwaway branch, `LarvaSecondsLostModule` | — | FR-2, FR-7 | S |
| WP-02 | **Spike: batch parse cost and cheap metadata** | Measure per-replay full analysis time on 50+ replays. Check whether the cache-only `getRepProc` exposes game mode, format, length and date for pre-filtering before the full reparse. Decide the threading model (1 worker vs. a small pool). | `ReplaySummaryService`, CLI harness | — | FR-10, FR-15 | S |
| WP-03 | **Test harness** | Add a JUnit 4 jar under a test-only lib folder, a `test/` source folder and an Ant `TEST` target that is excluded from the release jar. Java 7 language level. | `build.xml`, new `test/` | — | all | S |
| WP-04 | **Larva hub page and page navigator** | `LarvaHubPage implements IPage` (non-closable, mutable children). A navigator service owns create/select/close of sub-pages and loads all three icons. Replace `installReplayPage()`. | `LarvaSecondsLostModule`, new hub/navigator classes | WP-01 | FR-2, FR-16 | M |
| WP-05 | **Hub UI** | Centered button stack, `Ladder games only` checkbox, tip icon. No auto-analysis. | new hub component | WP-04 | FR-1 | S |
| WP-06 | **Single-replay sub-pages** | Bind `LarvaReplayPageComp` to one replay. Move busy/summary/error routing from module singletons to a per-page controller. Remove auto-latest. Open/Latest create sub-pages, Refresh stays in place. Add dispose cleanup and dedupe (OQ-6). Auto-open from the replay monitor is a separate story (FR-17, E17-S07). | `LarvaReplayPageComp`, `LarvaSecondsLostModule` | WP-04 | FR-3, FR-4 | L |
| WP-07 | **Milestone extraction** | Compute 100/200 supply and 66/80 alive-drone loops per player (D-4). Optionally capture the exact drone loop in the analyzer and expose it via `LarvaAnalysisReport`. | `LarvaReplayAnalyzer`, `LarvaAnalysisReport`, new milestone model | WP-03 | FR-13 | M |
| WP-08 | **Per-game aggregation record and headless path** | New compact record (D-8) with `Date`, game mode, format and length loops. Toon-keyed Zerg players. Built from the shared `ReplayAnalysisContext`. Epic 13 records untouched. | `ReplaySummaryService`, new record classes | WP-02, WP-07 | FR-9, FR-10, FR-11 | M |
| WP-09 | **Filtering and tracked-player selection** | Pure functions: eligibility with reason codes, filter summary counts, D-5/D-6 selection. | new `…Filter` / `…PlayerSelector` | WP-08 (record type only) | FR-8, FR-10, FR-11 | S |
| WP-10 | **Aggregation engine** | Buckets All / ZvP / ZvT / ZvZ per tracked player. Pooled `LarvaPhaseStats` (D-1/D-2), phase length (D-9), milestone averages with `k/n` (D-10), D-7 mirror rule. Pure and immutable output model. | new aggregator and result model | WP-09 | FR-12, FR-13 | M |
| WP-11 | **Batch runner** | Background job over the selected files. Progress callbacks on the EDT, cancellation on page close, per-file error capture, one `ReplaySummaryService` per worker, bounded memory. | new runner, `IFactory` job/thread API | WP-08 | FR-6, FR-15 | M |
| WP-12 | **Reusable phase-table renderer** | Extract the Graphics2D table from `LarvaTimelinePreviewComp` into a component driven by a row-label × phase string-matrix view model. The single page must look the same (screenshot / golden check). | `LarvaTimelinePreviewComp`, new renderer | WP-03 | FR-4, FR-12 | M |
| WP-13 | **Multi-analysis page** | Multi-select chooser. Page creation with placeholder then dated label (FR-7, OQ-7). Header, filter summary, 2-column layout (OQ-1), milestone table, All plus three matchup tables, skipped-replays list, empty state. | new multi page component, navigator | WP-05, WP-10, WP-11, WP-12 | FR-5–FR-8, FR-12–FR-15 | L |
| WP-14 | **Help, docs and release validation** | Update `help/larva-module-help.html` (hub, multi page, averaging caveat D-3). Add a `knowhow/` note on the sub-page pattern. Run a 100+ replay performance run and a full regression of the single page. | help HTML, `knowhow/`, `prompts/` | all | DoD | S |

### Phasing and ordering

```
Phase 0  Spikes & harness     WP-01, WP-02, WP-03                 (parallel)
Phase 1  Navigation           WP-04 → WP-05, WP-06                (UI track)
Phase 2  Analysis core        WP-07 → WP-08 → WP-09 → WP-10       (data track, parallel to Phase 1)
                              WP-12                               (parallel)
Phase 3  Multi page           WP-11 → WP-13
Phase 4  Polish & release     WP-14
```
Critical path: WP-01 → WP-04 → (WP-10 ∥ WP-11 ∥ WP-12) → WP-13 → WP-14. The UI track and the data track can be staffed independently after Phase 0.

**Checkpoints:**
- After WP-01, go or no-go on module-owned `IPage`. The fallback is in R-1.
- After WP-06, the single page is at parity with today (AC-4.1).
- After WP-10, the aggregation numbers are signed off on synthetic data before any UI work.

---

## 8. Testing strategy

| Level | What | How |
|---|---|---|
| Unit (WP-03 harness) | D-1/D-2 pooled math, D-9 phase length, D-10 n/a rules, D-6 player selection incl. ties and thresholds, D-7 mirror counting, filter reason codes, OQ-5 length boundary (5 375 / 5 376 loops), milestone scan on synthetic snapshot lists (never reached, reached exactly at threshold, fixed-point /4096), locale date label (`bg_BG`, `en_US`, single-day range). | Synthetic `LarvaPhaseStats` / snapshot / record fixtures, with no Scelight runtime. Target: meaningful coverage of the aggregator, filter, selector and milestone classes (≥ 80 % lines there). |
| Golden / fixture | Single-page parity after WP-06/WP-12. Milestone loops on known replays. | Existing `LarvaTimelineGoldenFormatter` / `LarvaBenchmarkRecordFormatter` outputs before vs. after. Fixtures: `benchmark-data/queensMoved/*.SC2Replay`, the local `benchmark-data/raw-replays/` corpus (git-ignored), and Zerg replays under `parser-research/s2protocol/tests/s2replaystatsdata/` (old builds: use only for parse-robustness, not metric values). |
| Integration (headless) | Run filter → select → aggregate over a folder through `StandaloneScelightBootstrap`. Print a deterministic text dump of the result model. | Extend the CLI (e.g. a `--multi <dir>` mode) so planners and CI can diff dumps. |
| Manual / UI | Hub layout. Create, close and Ctrl+F4 sub-pages. Rename on completion. Two-column layout at 1280 px and 1920 px. Cancel on close. Navigating away during a run. Error list. Icons at several title font sizes. | Checklist in WP-14 with screenshots into `knowhow/epic17-*.png` (repo convention). |
| Performance | 100 and 500 replay runs. | Record wall time, peak heap and EDT responsiveness (UI must not freeze). Targets are in §9. |

---

## 9. Non-functional requirements

- **Responsiveness:** no parsing or analysis on the EDT. UI updates are posted via `guiUtils.runInEDT`. Progress updates are throttled to ≥ 100 ms apart.
- **Performance target** (confirm in WP-02): ≤ 1.5 s average per replay on a typical machine, so 100 replays finish in ≤ 2.5 min. Any pre-filter must skip the full reparse for non-ladder and short games when possible.
- **Memory:** keep only compact per-game records (D-8). Peak heap growth for 500 replays is ≤ 200 MB above baseline. Closing a page releases its model.
- **Robustness:** one bad replay never aborts the batch. All failures are logged with path and reason (`logger.error` for unexpected errors, `debug` for expected filter exclusions).
- **Thread safety:** one `ReplaySummaryService` (and thus one `SimpleDateFormat` and one analyzer) per worker thread. No shared mutable module state between pages.
- **Compatibility:** Java 7 language level, SDK 1.5.2 API only. No reflection into Scelight internals for page management (the existing reflective factory is unaffected).
- **Consistency:** metric formulas and time formatting are identical to the single-replay page (D-1, D-11).

---

## 10. Risks

| ID | Risk | Impact | Prob. | Mitigation | Contingency |
|---|---|---|---|---|---|
| R-1 | A module-owned `IPage` hub with children misbehaves (close, history, rebuild) in the real host | High | Low | WP-01 spike first. The Scelight source shows only interface use, no `BasePage` casts. | Top-level closable pages named `Larva: <name>` via `addPage` (loses nesting but keeps close). |
| R-2 | Node rename after completion is not reflected | Med | Med | Mutable `getDisplayName()` plus `rebuildPageTree(false)`, tested in WP-01. | OQ-7 fallback: fixed label or a pre-pass for dates. |
| R-3 | Batch analysis too slow for 100+ replays (full reparse per file) | High | Med | WP-02 measurement, metadata pre-filter, optional 2-thread pool, cancellation. | Show partial results incrementally, recommend smaller selections. |
| R-4 | Memory blow-up holding `LarvaAnalysisReport`s | High | Med | D-8 compact records. Drop the report right after extraction. | Stream aggregation (accumulate sums, no per-game list). |
| R-5 | Refactoring `LarvaReplayPageComp` / `LarvaTimelinePreviewComp` regresses the single page | Med | Med | Golden output compare, screenshot compare, WP-06 parity checkpoint. | Keep the old painter path behind a flag until parity. |
| R-6 | Debug side effects in the analyzer (`/tmp/larva-pos2jl.log` writes, verbose `logSpawnLarvaCommands` per replay) slow down batches or fill disks | Med | Med | Gate both behind the dev diagnostic flag in WP-11. | Accept and document. |
| R-7 | Snapshot-based milestones are coarse (≈ 10 s) | Low | High | Show the caveat in help. Exact-loop drone capture (D-4 upgrade). | Accept for supply milestones. |
| R-8 | Wrong player picked as "most common" (barcodes, renamed accounts, ZvZ opponents) | Med | Med | Toon identity (D-5), threshold (D-6), list names in the header. | Follow-up: manual player picker. Scelight's favored-player setting is deliberately not used (OQ-2). |
| R-9 | Locale date quirks (CLDR suffixes, 2-digit years) | Low | Med | Unit tests for `bg_BG` and `en_US` on the bundled JRE. | Fixed `dd.MM.yy` / `MM/dd/yy` fallback per language. |
| R-10 | Ranking of averaged values is misread as a per-game percentile | Low | Med | Help text caveat (D-3). | Hide ranking rows on the multi page if the user dislikes it. |

### Technical debt notes
- The phase table is custom-painted. WP-12 should leave a renderer that a future Swing `JTable` migration could replace.
- `ReplaySummaryService` keeps growing (page, benchmark and now aggregation paths). Consider splitting into context-loader plus per-output builders when touching it in WP-08.
- `PHASE_PROMOTION_DWELL_SECONDS` is declared but unused by `resolvePhasePromotionLoop`. This is out of scope, but planners should not assume dwell semantics when reusing the drone scan.

---

## 11. Definition of done

- [ ] All FR acceptance criteria pass. OQ decisions are recorded in this file (§6 updated with the chosen answers).
- [ ] The single-replay page is at parity with the pre-epic version (golden and screenshot checks).
- [ ] Unit tests for filter, selector, aggregator, milestones and date label pass via the Ant `TEST` target. The release jar contains no test classes.
- [ ] A 100-replay run completes within the §9 target without UI freezes. Results are recorded in `knowhow/`.
- [ ] Closing any sub-page releases its resources and cancels its work (verified in logs).
- [ ] Help HTML updated. `knowhow/` note on the hub and sub-page pattern added. Screenshots `knowhow/epic17-*.png` added.
- [ ] `BUILD_RELEASE` succeeds against SDK 1.5.2 and the module loads in Scelight with no init errors.

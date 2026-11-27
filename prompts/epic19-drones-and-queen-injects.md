# Epic 19 — Correct drone count and trustworthy missed injects

Research findings and stories for two problems:

1. **Drone count too high.** In a ZvZ with at most 60 drones, the module reported 90+ drones and switched to End game.
2. **Missed injects flagged wrongly.** Queens are flagged when they are spreading creep tumors, and when they are away from the hatchery.

Written for the maintainer and for the agents that will implement this epic. All numbers come from replays parsed with the current module code (3.1) on 2026-11-25.

## 0. Summary

- **Drones.** The module builds its own drone tally instead of using Scelight's count, and the tally never subtracts killed drones. Fix: use `PlayerStats.workersActiveCount`. That is the value behind Scelight's **Workers Active** chart, so it is the ground truth you asked for.
- **Queens.** The replay never reports where a queen walks. The module therefore assumes a queen stays at the hatchery where she was born until she dies. It also does not see most creep tumors: they come from multi-queen selections, and the module reads only single-queen commands. Fix: rebuild missed injects from evidence the replay does contain (injects, tumors with map positions, damaged-unit positions, command target points), with a per-queen energy model.
- **Leftovers.** Debug code and notes from the previous queen fix are still in the repo (section 3.4).

## 1. How this was measured

The probes are temporary Java `main` classes compiled against `LarvaSecondsLostExtMod/bin`. They use the headless bootstrap and classpath from `knowhow/epic17-batch-spike/env.sh.txt`, and Scelight must not be running. They lived in the session scratchpad; E19-S00 turns them into a repo tool.

| Probe | Measures |
|---|---|
| `DroneProbe` | Per `PlayerStats` sample: the module's drone value, Scelight's `workersActiveCount`, and a tag-based tally. Also a census of drone-related tracker events. |
| `EventDump` | Raw drone and building tracker events of one player, in order. |
| `CorpusProbe` | One line per Zerg player: maximum drones with each method, phase start loops and the 66/80-drone milestones with each method. |
| `QueenProbe` | Queen births, queen position updates, queens in `UnitPositions` events, queen tumors, inject and tumor commands by selection type, red (missed-inject) seconds, and an energy upper bound. |
| `IdlePathProbe` | Which analyzer code path produced each red candidate window (from the module's own diagnostic lines). |
| `AttributionProbe` | How many queen tumors can be matched to a tumor command, and to which selection. |

**Corpus.** 145 replays: 70 ZvZ games from the maintainer's own library (2019–2026) and 75 pro games from `benchmark-data/raw-replays/categorized`. That gives **242 Zerg players**, 222 of whom built queens.

## 2. Findings — drone count

### F-D1 Phases and milestones use a hand-built tally, not Scelight's count

`LarvaReplayAnalyzer.analyze()` (around line 298) stores `aliveDroneCountByPlayerName` in each resource snapshot, and falls back to `workersActiveCount` only for a player who has no tally yet. Three outputs read that tally:

- phase boundaries: `resolvePhasePromotionLoop`, thresholds > 36, > 66 and > 89 in `LarvaGamePhase`;
- the multi-analysis milestones for 66 and 80 drones (`LarvaFirstDroneLoopTracker`, `LarvaMilestoneCalculator`);
- the benchmark bands, which were built from phase boundaries computed this way.

### F-D2 Killed drones are never subtracted

The `UNIT_DIED` branch (around line 420) reads `unitTypeName` from the died event. Tracker `UnitDied` events have no `unitTypeName` field (s2protocol `SUnitDiedEvent`). In every probed replay the field was missing on every drone death. So the decrement never runs, and every drone lost to banelings, harass or fights stays in the count for the rest of the game. ZvZ loses the most drones, which is why it shows the problem most.

### F-D3 Morph handling credits a player called "Unknown player"

`UNIT_TYPE_CHANGE` events have no player id either. `handleDroneMorphs()` resolves the owner from the event, gets `null`, and counts for `"Unknown player"`. In one test ZvZ the final map was `{Unknown player=25, <player A>=55, <player B>=57}`. Burrow, unburrow and other morph paths are therefore wrong too. With the generic fallback, "any type change of an unknown tag is a drone becoming a building", the code cannot be fixed piece by piece.

### F-D4 What the tracker really says (checked event by event)

- Drones from eggs arrive as `UnitBorn Drone` (creator ability `LarvaTrain`). The 12 starting drones are `UnitBorn` at loop 0.
- **Drone becomes a building.** The building gets `UnitInit`. The drone gets no event at that moment. The drone's `UnitDied` (no killer) arrives one loop **after the building's `UnitDone`**, which can be 20 or more seconds later.
- **Cancelled building (extractor trick).** The building's `UnitDied` has the own player as killer. The drone comes back without any event.
- `workersActiveCount` drops at building start and comes back on cancel. It does not count drones in eggs. It is sampled every 160 loops (about 7.1 s real time). After a player leaves, the samples for that player are 0.
- A correct tag-based tally would need to track "drone inside a building under construction", cancels and burrows. Even then it differs from `workersActiveCount` by 1 to 9 at some samples (about half the samples in long games). Rebuilding Scelight's number by hand is not worth it.

### F-D5 Scelight's Workers Active chart is `workersActiveCount`

`scelight/src-app/.../charts/chartfactory/PlayerStatsChartFactory.java:72`:

```java
WORKERS_ACTIVE( "Workers Active", false, PlayerStatsEvent.F_WORKERS_ACTIVE_COUNT )
```

If the module uses the same field from the same `PlayerStats` events, it matches the chart exactly at every sample.

### F-D6 Impact (242 Zerg players)

| Measure | Result |
|---|---|
| Module above `workersActiveCount` at some sample, by ≥ 10 / ≥ 20 drones | 94 players (39%) / 58 players (24%). Worst case 85: a pro TvZ with 85 real drones, module 133. |
| End phase (> 89 drones) | Reached in real counts by 8 players, in the module by 27, so **19 are wrong**. |
| Late phase (> 66 drones) | **22** wrong. Where both methods reach Late, the module starts it 7 s earlier (median), and up to 414 s earlier. |
| 80-drone milestone | **20** wrong (real 29, module 49). 66-drone milestone: 19 wrong. |
| Mid phase | No wrong cases: few drones die before 37. |

Example from the maintainer's own library, a 2019 ZvZ:

- The maintainer's real maximum was 54 drones; the module counted 75. It put them in Late game from 12:00 and reported the 66-drone milestone at 12:00.
- The opponent's real maximum was 66; the module counted 96. It put them in End game from 15:00.

### F-D7 Consequences of the fix

- The benchmark bands in `LarvaPhaseBenchmarkRatingFormatter` were computed on phases from the broken count. The commits "updated … bucket lower bounds with proper counting of drones alive" introduced that count. The bands must be regenerated (E19-S05).
- The golden file `test/golden/epic17-multi-queensMoved.txt` changes. The user reviews the diff before it is accepted.
- `LarvaReplayAnalyzer` has 3,152 lines and no unit tests. The drone and queen logic must move into small classes that can be tested on their own.

## 3. Findings — missed injects (queens)

### 3.1 How red windows are made today

For each queen, `buildIdleInjectTimelines()` produces candidate red windows through several paths. Per hatchery, all candidates from all queens are merged (union).

- **A — single-queen inject command.** The queen is bound to the hatch she injected. Red time is the gaps between that hatch's inject windows, counted after her energy is assumed back. Energy spends are known only from single-queen commands.
- **B — queen born near the hatch.** The queen is bound to that hatch **from birth until death or replay end**, with energy always assumed available.
- **C — born near several hatches.** Rotation by `queenTag % n`.
- **D — "position timeline".** The previous fix ("Solution 1"). It walks the queen's position observations and keeps her bound while she is "seen nearby".

`trySeedObservedIdleCandidateFromNearbyQueen` and `isBuildingTypeLike` are never called (dead code).

### 3.2 Why queens away from the hatchery are flagged

- **Queens get no position updates.** Across 1,103 queens there were **0** tracker events with a position other than birth and death. Path D's "position timeline" therefore holds only the birth point and the death point. The diagnostics confirm it: *"Queen 3xf was observed nearby from 06:01 to 18:33"* means "born at 06:01, died at 18:33". A queen born at the main is treated as standing at the main all game, however far she walks to spread creep or defend.
- **Fake position points.** `addQueenStateUpdateAtInjectExpiry` inserts made-up points with the queen's last known position (birth or death point), not the hatch she injected. That reinforces the error.
- **Example: the `queensMoved` fixture**, main hatch `2jl`. Red windows 08:43–12:52 and 14:53–17:44 come from two queens born at the main, which the module wrongly kept there all game. (An earlier version of this file said "which in reality were away": that was an agent's guess from the folder name, not the maintainer's testimony. Nobody has checked this game in StarCraft II; see §9.1.) Candidate seconds by path in this replay: B 3,061 s, D 2,878 s, A 1,797 s. **Most of the red time comes from "born here".**

### 3.3 Why queens that spread creep are flagged

- Paths B and D never subtract energy. Path A subtracts it only for commands given while that single queen was selected.
- **84% of creep-tumor commands** (1,588 of 1,896) and **60% of inject commands** (2,819 of 4,724) were given with several queens or other units selected. The module ignores them all.
- The tracker records every queen tumor regardless of selection: `UnitInit CreepTumorQueen` with x and y, **1,083** in the corpus. The module does not use them.
- **30%** of all red seconds (25,382 of 83,831) overlap a queen tumor of the same player placed during the window or up to 30 s before it.
- **Impossible results.** Upper bound for a player: total queen energy (25 at birth, plus 0.7875 per second while alive, no 200 cap), minus 25 × inferred injects, minus 25 × queen tumors, divided by 25 = the most injects the player could have missed. For **26 of 222 players the module reports more** (137 more in total). One test ZvZ shows 10 missed injects where the bound is 4. The bound ignores the cap and transfusions, so the real overcount is larger.

### 3.4 Evidence the replay has and the module does not use

| Evidence | Source | Notes |
|---|---|---|
| Queen tumor placed, with position | Tracker `UnitInit CreepTumorQueen` | Always present. The queen was at (x, y) and spent 25 energy. It has no creator tag. |
| Which queen placed it | `ICmdEvent` with id `CreepTumorQueen` and a target point, plus the selection tracker | 223 of 327 tumors (68%) match a command within 1.5 cells and 60 s: 98 with one queen selected, 125 with several (needs the nearest-queen rule). |
| Inject (hatch side) | 3 larva within 8 loops (already used) | Reliable. A queen was at that hatch at inject start. |
| Queen positions while fighting | Tracker `UnitPositions` (every 15 s, damaged units only) | 4,150 queen positions in the corpus. Decode: `firstUnitIndex`, then `items` triples (index delta, x, y). The unit index is `tag >> 18`. Same map scale as other tracker x/y. |
| Where queens were sent | Move/attack/patrol/smart commands with target points, plus the selection | Gives a destination, not a position. Optional. |

**Coordinate pitfall.** `ITargetPoint.getXFloat()` and `getYFloat()` return half the tracker scale (raw / 8192). Use `getX() / 4096.0` and `getY() / 4096.0` to compare with tracker x/y. Tumor `UnitInit` (149, 76) matched command (74.75, 38.25) × 2.

**Small constant error.** Queen energy regen is 0.7875 per real second (0.5625 per Normal-speed second), so 25 energy takes 31.7 s, not the 30 s in `QUEEN_RESTORE_25_ENERGY_SECONDS`. The module thinks queens are ready about 2 s early in every cycle.

### 3.5 Leftover artifacts in the repo

- POS2JL debug code for one hatchery tag text `"2jl"` (the `queensMoved` main), in `LarvaReplayAnalyzer`:
  - `writePos2jlLog`, around line 68, writes `/tmp/larva-pos2jl.log` when dev diagnostics are on;
  - the block around lines 1093–1154 adds a `POS2JL` diagnostic line for every queen position.
- `.claude/SOLUTION1_IMPLEMENTATION_SUMMARY.md`, `SOLUTION1_VALIDATION.md`, `SOLUTION1_VALIDATION_REPORT.md` (untracked). They describe path D and rest on a false assumption: "every tracker event updates queen position".
- `benchmark-data/injectsQueued/hatch-*.log`: per-hatch inject debug logs from the queued-injects fix.
- Dead code: `trySeedObservedIdleCandidateFromNearbyQueen`, `resolveObservedNearbyQueenReadyLoop` (called only from the dead method), `isBuildingTypeLike`.
- `prompts/bugfix-worker-count-spore-hatchery-plan.md` and `prompts/epic11c-queensMissed.md` describe the earlier attempts. Keep them as history; this epic replaces their approach.

## 4. What is needed

**Drones.** Nothing from the user. Every replay has the ground truth. Done means the module's series equals `workersActiveCount` at every `PlayerStats` sample.

**Queens.** The replay has no ground truth for "this was a missed inject". Needed:

1. **Scenario replays from the user (E19-S02).** Short games against the AI that follow a script, so each answer is known in advance:
   - **S-a, creep away.** Two hatcheries, one queen each, injecting for 3 minutes. Then queen B walks 40+ cells away and drops tumors for 2 minutes, then comes back and injects again. Expected: no red on hatch B while she is away.
   - **S-b, creep near.** A third queen stays within 8 cells of the main and drops a tumor every time she reaches 25 energy, while the main's own queen keeps injecting. Expected: no red on the main.
   - **S-c, idle.** A queen stands next to her hatch with full energy for 90 s without injecting, then injects. Expected: about 3 missed injects (90 s / 29 s).
   - **S-d, multi-select.** Like S-a and S-c, but all injects and tumors are given with all queens on one control group. Expected: same results as the single-queen versions.

   Note the in-game times of each step. Two or three real ladder replays where you saw false red, with the hatch and time, help too.
2. **Your answers to the open questions** in section 5.
3. **An automated invariant.** The energy bound from 3.3, run over the corpus by the E19-S00 tool: a player's missed injects must never exceed it.

## 5. Open questions (each has a default)

| # | Question | Default |
|---|---|---|
| OQ-1 | A hatch with no queen at all (inject queen died, or none was made): show it? | Not a missed inject. Optionally a separate "no inject queen" note later, not part of this epic. |
| OQ-2 | A creep queen stands near a hatch with 25+ spare energy and the hatch is not injected: miss? | Yes. She could have injected. The energy model decides: spending on tumors keeps her below 25, so she is not flagged. |
| OQ-3 | Keep the phase thresholds 36 / 66 / 89 on the new count? | Yes. Regenerate the benchmark bands (E19-S05). |
| OQ-4 | Milestones from samples every 7.1 s: is that precise enough? | Yes. Use the sample loop. Finer timing is optional (E19-S04). |
| OQ-5 | How long does a queen count as "at the hatch" after the last evidence, with nothing newer? | 30 s, tuned with the scenario replays. |
| OQ-6 | Release as 3.2, with a note that phases, benchmarks and missed injects changed? | Yes. |

## 6. Stories

Rules for all stories:

- Java 7 source level: no lambdas, no streams.
- New logic goes into small classes with JUnit tests (`ant TEST`), for example in packages `…larvasecondslostextmod.workers` and `…larvasecondslostextmod.queens`. `LarvaReplayAnalyzer` shrinks; it must not grow.
- Never run the headless tools while Scelight is running.

### Phase 0 — Tools and cleanup

#### E19-S00 — Analysis check CLI (corpus regression tool)

**Why:** every later story needs before-and-after numbers. The probes are throwaway code.

**Do:** add a dev-only `main` class, for example `LarvaAnalysisCheckCli` (not shipped in the release zip, or harmless if it is). Input: a list file of replays. Output: CSV per Zerg player with:

- module drones vs `workersActiveCount` (max, max difference, wrong phases, wrong milestones);
- queens born, queen tumors, injects, red seconds, red seconds overlapping a tumor, the energy bound and the module's missed injects.

Commit a corpus list with paths relative to `$REPO` and `$SC2_ACCOUNTS` (no personal paths). Record the baseline numbers from sections 2 and 3 in `knowhow/epic19-baseline.md`.

**Done when:** one command reproduces the tables in F-D6 and 3.3 within ±1.

#### E19-S01 — Remove the leftovers

**Do:**

- Delete the POS2JL code.
- Delete the dead methods from 3.5.
- With the user's OK, delete `.claude/SOLUTION1_*.md` and `benchmark-data/injectsQueued/hatch-*.log` (keep the replay).

**Done when:** `grep -rn "2jl\|POS2JL" LarvaSecondsLostExtMod/src` is empty, `ant TEST` passes and the golden is unchanged.

#### E19-S02 — Scenario replays (user)

**The user plays them** (section 4, S-a to S-d) and puts them in `benchmark-data/queenScenarios/` with a `README.md` that has the scripted times and the expected result per hatch.

**Done when:** at least S-a, S-c and S-d exist. Until then, E19-S10 uses the `queensMoved` fixture and the energy bound.

### Phase 1 — Drones

#### E19-S03 — Worker count from `workersActiveCount`

**Do:**

- New `LarvaWorkerSeries`: ordered `(loop, count)` samples per player from `PlayerStats` events. It answers:
  - `firstLoopAbove(threshold)`, used for phases (> 36, > 66, > 89);
  - `firstLoopAtLeast(threshold)`, used for milestones (≥ 66, ≥ 80);
  - `max()`.
- Ignore the zero samples after a player has left.
- Snapshots store `workersActiveCount` again.
- Delete the tally:
  - `handleDroneMorphs`, `recordBuildingConstructionStart`, `incrementDroneCount`, `decrementDroneCount`;
  - `isBuildingConstructionLike`;
  - `unitCompletedByTag` and `buildingConstructionPlayerNameByTag` for drone purposes;
  - the drone branches in `UNIT_BORN` and `UNIT_DIED`.
- Keep the spawning pool tracking.

**Done when:**

- Unit tests cover thresholds, samples out of order, a player who left, and no samples.
- The CLI shows a difference of 0 between the module and `workersActiveCount` for all 242 players.

#### E19-S04 — Milestones from the same series

**Do:**

- Replace `LarvaFirstDroneLoopTracker` with `LarvaWorkerSeries.firstLoopAtLeast(66 / 80)`.
- Report the sample loop.
- Remove `aliveDroneCountByPlayerName` from `LarvaAnalysisReport`, or replace it with the last real sample.
- Optional: refine the loop between two samples by counting drone `UnitBorn` events after the earlier sample. This is exact when no drone was lost or used in between.

**Done when:** `LarvaMilestoneCalculatorTest` and `LarvaAggregationRecordBuilderTest` are updated. The milestones match the first Workers Active sample ≥ 66 / ≥ 80 in Scelight's chart.

#### E19-S05 — Regenerate the benchmark bands and the golden

**Do:**

- Re-run the benchmark pipeline (`benchmark-data/tools`, see `benchmark-data/README.md`) on the elite corpus with the fixed phases.
- Update the bands in `LarvaPhaseBenchmarkRatingFormatter`.
- Regenerate `test/golden/epic17-multi-queensMoved.txt`. The user reviews the diff.

**Done when:** the new percentile files are committed next to the old ones (old ones kept for comparison), the formatter uses the new numbers, and `ant TEST_GOLDEN_MULTI` passes.

#### E19-S06 — Manual check against Scelight's chart

**Do:** in Scelight, open the 2019 ZvZ from F-D6 and one recent ZvZ. Go to Charts → Player stats → Workers Active, and compare the peak and the times where the line crosses 37, 67 and 90 with the Larva page phase columns.

**Done when:** the phase start times match the chart within one sample (7 s).

### Phase 2 — Missed injects

#### E19-S07 — Queen evidence collector

**Do:** a new class, for example `QueenEvidenceCollector`. It turns tracker and game events into a time-ordered list per player:

- `QueenBorn` (tag, loop, x, y, producing hatch) and `QueenDied`;
- `Inject` (hatch, start loop), from the existing inferred inject windows;
- `InjectCommand` (target hatch, queen tags in the selection);
- `Tumor` (loop, x, y, candidate queen tags from the matched command, or empty);
- `Transfuse` (loop, queen tags in the selection);
- `QueenPosition` (tag, loop, x, y), from `UnitPositions`.

Unit tests for:

- the `UnitPositions` delta decoding;
- the target point scale (`getX() / 4096`);
- tumor-to-command matching (1.5 cells, 60 s).

**Done when:** the CLI reports per replay how many tumors and injects were attributed to one queen, to several candidates, or to none. The rates are about those in 3.4.

#### E19-S08 — Queen presence model ("is this queen at hatch H at loop t?")

A queen is at H only when evidence shows it. Rules (defaults, tuned in E19-S10):

- **Anchors at H:** born from H; an inject on H attributed to her; a tumor placed by her within 12 cells of H; a `UnitPositions` sample within 12 cells of H.
- **Away evidence:** a tumor, position, inject or death more than 12 cells from H.
- **Between two anchors at H** with no away evidence between them, she is at H for the whole gap, however long. This keeps real misses: an inject queen who forgets to inject but stays at her hatch.
- **After the last anchor:** she is at H for at most `T_hold` = 30 s (OQ-5), or until the next away evidence, whichever comes first.
- **Unclear attribution** (several candidate queens): use the candidate whose last anchor is nearest; on a tie, the one with more energy. If no queen can be chosen, the evidence still counts as "a queen of this player was here" for energy purposes, but it does not create presence.

**Done when:** unit tests with synthetic evidence lists pass:

- a queen born at the main, then tumors 30 cells away: presence at the main ends at the first tumor (or after `T_hold`);
- an inject chain with a gap of 120 s and no away evidence: present the whole time;
- the queen dies: presence ends.

#### E19-S09 — Queen energy model

**Do:**

- Per queen: 25 energy at birth, plus 0.7875 per real second converted to loops with the game speed, capped at 200.
- Spends: attributed injects (25), tumors (25), transfusions (50). Ambiguous spends go to the candidate chosen by the S08 rule.
- Replace `QUEEN_RESTORE_25_ENERGY_SECONDS` = 30 with the exact rate.
- A spend that would push a queen below 0 means the attribution was wrong. Log it and move the spend to the next candidate.

**Done when:** unit tests cover regen, the cap, spends and the next-candidate fallback.

#### E19-S10 — New missed-inject rule, replacing paths A to D

**Rule:** a red window on hatch H covers loops where all of these hold:

- H is completed, alive and not injected;
- inject is unlocked (pool done + 36 s, unchanged);
- some queen of the player is at H (S08) with ≥ 25 energy (S09).

The existing 29 s marker counting and the 3 larva per missed inject stay as they are.

**Do:**

- Remove paths A, B, C and D and the synthetic position points.
- Rewrite the diagnostic lines so each window says which evidence made it, for example: "queen 3xf at hatch from inject 12:51 to tumor 13:40 (24 cells away); energy 25+ from 13:21".
- Keep the `HatcheryIdleInjectTimeline` / `HatcheryIdleInjectWindow` output shape, so the timeline and the tables need no changes.

**Done when:**

- Scenario replays S-a, S-b and S-d show no red where none is expected; S-c shows 3 ± 1 missed injects.
- In the `queensMoved` fixture, the main's red windows 08:43–12:52 and 14:53–17:44 are gone, or the diagnostic line names real at-hatch evidence for them.
- The CLI shows 0 players above the energy bound (it was 26).
- Red seconds overlapping a tumor placed by a queen that is at least 12 cells from the hatch drop to 0 (that queen was away).
- The golden is regenerated and the user has reviewed it.

#### E19-S11 — Optional: positions from move commands

Only if E19-S10 still leaves false red that the scenario replays or the user can point to.

**Do:** a move, attack or smart command with a target more than 12 cells from H, given while the queen is selected, counts as away evidence from the moment it is given.

### Phase 3 — Documentation and release

#### E19-S12 — Help, knowhow, release

**Do:**

- Help page (`help/larva-module-help.html`):
  - phases use the same drone count as Scelight's Workers Active chart;
  - a missed inject requires a queen at the hatch with 25+ energy;
  - a queen spreading creep or away from the hatch is not counted.
- New `knowhow/replay-tracker-facts.md` with the facts from F-D4 and 3.4: the drone/building event order, `UnitDied` and `UnitTypeChange` without type or owner, the target point scale, `UnitPositions` decoding.
- Release 3.2 as described in `RELEASING.md`. The Changes line says that phases, benchmarks and missed injects changed and that old numbers are not comparable.

**Done when:** the published landing page shows 3.2 and the in-app update works on one machine.

## 7. Risks

| Risk | Mitigation |
|---|---|
| Fewer reds also hide some real misses. | Scenario S-c and the "bridge between anchors" rule. Tune `T_hold` with real replays. |
| Old personal results are not comparable after the update. | A release note (OQ-6). Pooled multi-analysis is recomputed from the replays anyway. |
| Tumor or inject attribution with several queens selected is wrong. | The energy model catches impossible spends (S09); fall back to "player-level" evidence. |
| Scelight's selection tracker misses some selections (44 matched tumors had no queen in the selection). | Tumor position and energy work without the selection; only queen identity is lost. |

## 8. Definition of done (epic)

- The module's drone count equals Scelight's Workers Active at every sample, and phases and milestones follow from it.
- The benchmark bands are regenerated for the new phases.
- Missed injects come only from a queen being at the hatch with energy. No player exceeds the energy bound, and the scenario replays give the expected results.
- The leftovers are removed. `ant TEST` and `ant TEST_GOLDEN_MULTI` pass. 3.2 is released.

## 9. Status (2026-11-26)

Implemented, then revised with the scenario replay (9.1). Numbers: [knowhow/epic19-baseline.md](../knowhow/epic19-baseline.md). Replay facts: [knowhow/replay-tracker-facts.md](../knowhow/replay-tracker-facts.md). `ant TEST` (377 tests), `ant TEST_GOLDEN_MULTI` and `ant TEST_GOLDEN_SCENARIO` pass. Nothing is committed or released.

| Story | Status | Notes |
|---|---|---|
| S00 corpus CLI | done | `LarvaAnalysisCheckCli` (+ `…CheckRow`, `…CheckSummary`). Committed list `benchmark-data/manifests/epic19-check-corpus.txt` (pro + fixtures); the maintainer's own games are in the gitignored `benchmark-data/tmp/epic19-personal-corpus.txt` (paths hold account ids). |
| S01 leftovers | done | POS2JL, the dead methods and the whole old queen pipeline are gone (`LarvaReplayAnalyzer` 3,152 → about 1,580 lines). The untracked leftovers are deleted (maintainer's OK, 2026-11-26). |
| S02 scenario replays | done (2026-11-26) | One ZvZ against the computer with a written ground truth: `benchmark-data/queens-scenarios` (resumed from replay at about 10:05; the interrupted original is kept too). Golden `test/golden/epic19-scenario-red.txt`, `ant TEST_GOLDEN_SCENARIO`. It showed what the evidence model missed: see 9.1. Synthetic S-a to S-d stay as unit tests (`MissedInjectScenarioTest`). |
| S03 workers | done, simpler than planned | Snapshots store `workersActiveCount` again; the tally and `LarvaFirstDroneLoopTracker` are deleted. Phase boundaries moved to `LarvaPhaseIntervalResolver` (tested). No separate `LarvaWorkerSeries`: the snapshots already are that series. |
| S04 milestones | done | `LarvaMilestoneCalculator.compute( snapshots )`: first sample ≥ 66 / ≥ 80. |
| S05 bands, golden | done, redone 2026-11-26 | League bands by MMR-Ranges, league medians, tier starts halfway (the maintainer's spreadsheet method) as one command: `ant BENCHMARK_BANDS` (tool `benchmark-data/tools/benchmark_bands.py`, dump CLI `LarvaPhaseRateDumpCli`, band file `larva-benchmark-bands.properties`, report `benchmark-data/exports/benchmark-bands.md`). See §9.2. |
| S06 chart check | done | The maintainer compared drone times in Scelight and the module: they match. Scenario: 50 drones by the maintainer's count, Workers Active 51, module 51. |
| S07 evidence | done | `queens.QueenEvidenceCollector` (Scelight API) + `QueenEvidenceDecoding` (pure, tested). |
| S08 presence | done, changed twice | `QueenPresence`. When the player's queen orders can be followed (at least half the injects have a Spawn Larva command with queens selected) a queen stays where she is until an order sends her away; otherwise (computer players) evidence only: 29 s hold, 90 s bridge limit. A fact is near every hatchery within 12 cells (main and macro hatchery), but a queen makes only the nearest red hatchery red at a time. See 9.1. |
| S09 energy | done, extended | `QueenEnergyTimeline`: 0.5625 per game second, cap 200, conflicts counted. Attribution (`QueenSpendAttributor`): command selection if known, else all living queens; then can afford, can have walked there in time, nearest by last known position (orders count, 3 cells is a tie), next own order near the spend, more energy, lower tag. Queued injects (back-to-back windows) are one spend each, cast at the command loop. **Spare energy**: red needs 25 energy that the queen could put on this hatchery without losing a later tumor, transfusion or inject elsewhere (her next inject on this hatchery does not count; nothing counts after she reaches the cap). This is what stops creep queens from being red. Since the scenario only spends within 29 s count against it: a queen hoarding energy at a skipped hatchery before a creep burst minutes later is idle until 29 s before the burst. |
| S10 new rule | done | `MissedInjectCalculator`, adapter `LarvaMissedInjectAnalysis`; output shape unchanged. Diagnostics per window and per queen; the red-window tooltip has a "Why" line. Energy bound: with orders followed and the spare horizon it is no longer strict (hoarded energy counts as idle and can still pay for a later burst): 0 of 104 committed and 6 of 216 personal players above it beyond the counting ratio, by 2 to 8 injects. |
| S11 move commands | done (2026-11-26) | Scelight's selection tracker was the problem, not the replays: it ignores steal hotkeys, keeps selections in the wrong order and misreads long remove masks. `SelectionModel`, `SelectionMaskDecoding`, `ReplayCommandStream` (also `CmdUpdateTarget*`). Injects with a matched command in the scenario: 15 → 40 of 42. Queen orders: moves for every selected queen, cast orders for the caster, stops. |
| S12 docs, release | docs done, release not started | Help page section "What the numbers mean"; `docs/benchmarking/statistical-analysis.md` has an Epic 19 note. Release 3.2 needs the maintainer (RELEASING.md). Suggested Changes line: "3.2: phases and drone milestones use Scelight's Workers Active count; missed injects follow your queens' orders and count a queen at the hatchery with spare energy (creep queens and queens you sent away are no longer flagged, idle queens are); larva are assigned by their hatchery; new benchmark bands. Old numbers are not comparable." |

Open questions answered by default: OQ-1 (no queen evidence = no red), OQ-2 (yes, via spare energy), OQ-3 (thresholds kept), OQ-4 (sample loop), OQ-5 (29 s hold; 90 s bridge limit only without orders), OQ-6 (release as 3.2).

### 9.1 Scenario follow-up (2026-11-26)

What the scenario replay showed, and the change for each (numbers: [knowhow/epic19-baseline.md](../knowhow/epic19-baseline.md), section "Scenario replay"):

- **Queens march away and back (6:08 to 7:08):** the evidence model bridged them (red at all bases). Fix: follow queen orders (S11). No red now.
- **Idle queens without evidence (8:16 to 9:24, third base 10:00 to 10:48):** the hold and bridge limit cut them off. Fix: with orders followed, no limit; the spare-energy horizon (29 s) for the creep burst case.
- **Macro hatchery next to the main (7:17 to 7:51):** points belonged to the nearest hatchery only. Fix: a fact is near every hatchery within 12 cells; a queen makes one hatchery red at a time (the nearest), since her energy keeps up with one inject schedule.
- **Queens standing together after a group move:** the nearest-queen rule was a coin flip and later swapped the natural and third queens. Fix: can-reach check and look-ahead to the queen's next own order.
- **Queued orders after a cast order** (tumor, then shift-move): run when the cast is seen.
- **Missed injects:** larva were assigned to hatcheries by a spatial guess because the creator ability is always empty; inject pops split between the main and the macro hatchery went undetected (scenario main injects at 11:20 and 11:59, `queensMoved` 14:52). Fix: assign by the creator tag (`LarvaAssignmentHeuristic`). Benchmark bands move in the fourth decimal: not regenerated.
- **Decoding stops early** in some replays (Scelight): after the last decoded command the evidence-only limits apply.

Open questions for the maintainer:

Answers (2026-11-26):

- `queensMoved` main 15:21 to 17:44 is red: the four queens of the inject hotkey get no order from 14:40 to 17:43 and their energy reaches the cap. There is no testimony that they were away (the earlier claim was an agent's guess, see §3.2), so the red stands as the model's verdict.
- Missed injects count at any supply, also near 200 (maintainer). Late and end game stats change with it.
- No spot check of red windows in StarCraft II for now (no game on the maintainer's computer).
- The scenario replays may be committed.

### 9.2 Clock and league bands (2026-11-26)

- **Game clock.** Scelight's Faster factor is 26/36 (1.385, the game uses 1.4), so its times were about 1.1 % high (game 7:51 showed 7:56), and with its "use real time" setting off the module showed game seconds. The module now has its own `LarvaGameClock` (loops / 22.4, rounded down) for every time it shows and never uses Scelight's conversion. Game-data durations stay in loops, measured on replays: natural larva every 240 loops, back-to-back injects 641 loops apart, queen build 800 loops. Effects: all times about 1.1 % lower and always `m:ss`; missed larva count one per 240 loops (was 237), inject windows 641 loops (was 642). Scelight's own pages still show Scelight time.
- **Rate unit.** "Per minute" rates were per 960 loops (a Normal-speed minute, 42.9 s on the game clock). The maintainer chose game clock minutes (60 s, 1344 loops): every rate is 1.4 times the old value, the bands were regenerated (also exactly 1.4 times, same tiers), the fixture bands and pixel references updated.
- **One time basis for the project.** `knowhow/time-management.md` is the rule: compute in loops, show with `LarvaGameClock`, game data as measured loop constants (hatchery 1600 loops = 71.4 s). `LarvaTimeBasisGuardTest` fails `ant TEST` when code outside `LarvaGameClock` converts loops or uses Scelight's time API. The old guide (loops / 16 "gameplay time", thresholds × 36/26) was the source of the mix.
- **League bands.** `ant BENCHMARK_BANDS` buckets the MMR corpus by league (player MMR, region column of MMR-Ranges season 69; above Master 1 = GM), takes league medians and puts tier starts halfway between them (`docs/benchmarking/statistical-analysis.md`, "League bands"). Tiers: silver, gold, plat, dia, masters, GM, pro. With medians, early larva generated rises with skill and is rated again. Silver is dropped for Mid and Late larva generated (its median is above gold's). The unit tests use fixed fixture bands (`test/resources/larva-benchmark-bands-fixture.properties`), so a band run needs only `ant GOLDEN_MULTI_REGENERATE` and a review.
- **Adjacent hatcheries.** `MissedInjectCalculatorTest` also covers a macro hatchery touching the main (5 cells between centers; any hatchery within 12 cells counts).

# Epic 19 corpus check: how to run it, baseline and results

Regression numbers for drones and missed injects ([prompts/epic19-drones-and-queen-injects.md](../prompts/epic19-drones-and-queen-injects.md)). Re-run the check after any change to phases, drone counting, inject detection or the queen model, and compare with the tables below.

## Run

Scelight must not be running (single-instance socket), and run one headless JVM at a time: a second one opens a "workspace in use" dialog and waits for ever. A probe that throws also hangs (Scelight's threads keep the JVM alive): stop it. Classpath as in [epic17-batch-spike/env.sh.txt](epic17-batch-spike/env.sh.txt); `REPO` and `SC2_ACCOUNTS` as in `local-paths.md`.

```bash
cd LarvaSecondsLostExtMod && ant COMPILE && cd ..
export REPO SC2_ACCOUNTS   # the list files use ${REPO} and ${SC2_ACCOUNTS}
"$J8/java" -Xmx2g -cp "$M/bin:$CPLIBS" hu.aleks.larvasecondslostextmod.LarvaAnalysisCheckCli \
    --list benchmark-data/manifests/epic19-check-corpus.txt --scelight-app-dir "$S" > /tmp/check.csv
```

- stdout: one CSV line per Zerg player (columns in `LarvaAnalysisCheckCli`); stderr ends with the summary lines used below. About 5 s per 80 replays.
- `benchmark-data/manifests/epic19-check-corpus.txt`: committed list, 75 pro games from the elite benchmark library plus the `queensMoved` and `injectsQueued` fixtures and the two scenario replays (`benchmark-data/queens-scenarios`). The pro replays are gitignored; get them with `benchmark-data/tools`.
- `benchmark-data/tmp/epic19-personal-corpus.txt`: the maintainer's own games (70 ZvZ 2018–2022, 30 ZvP and 30 ZvT 2022–2023, 10 games from 2026). Gitignored, because the paths hold account ids. Rebuild it with `find "$SC2_ACCOUNTS" -name "*.SC2Replay"` and the matchup in the file names.

What the columns compare:

- drones: the module's snapshots (`LarvaPlayerResourceSnapshot.getWorkersActiveCount()`) against the raw `PlayerStats.workersActiveCount`; phase starts (> 36, > 66, > 89) and the 66/80 drone milestones as reached or not;
- queens: red seconds, red seconds that overlap a queen tumor of the same player (during the window or up to 30 s before), and the **energy bound**: all queen energy (25 at birth plus regeneration, no cap) minus 25 per inferred inject and per queen tumor, in 25-energy units. A player's missed injects above the bound are impossible. The module counts one missed inject per 29 s of red while a queen needs 31.7 s per 25 energy, so the summary also counts players above `floor(bound × 712 / 642) + 1`.

## Results

Baseline: module 3.1 (2026-11-25). After: Epic 19 code (drones from `workersActiveCount`, queen evidence model with a 29 s hold and a 90 s bridge limit).

| Corpus | Zerg players | Measure | 3.1 | Epic 19 |
|---|---:|---|---:|---:|
| committed (pro + fixtures) | 104 | module drones differ from Workers Active (≥ 10 / ≥ 20) | 85 (47 / 29) | 0 |
| | | wrong phase reached: Mid / Late / End | 0 / 9 / 16 | 0 / 0 / 0 |
| | | wrong milestone reached: 66 / 80 | 5 / 12 | 0 / 0 |
| | | missed injects, total | 1,315 | 501 |
| | | players above the energy bound (beyond the counting ratio) | 11 | 2 (0) |
| | | red seconds / overlapping a queen tumor | 42,708 / 18,373 | 19,620 / 6,182 |
| personal | 216 | module drones differ from Workers Active (≥ 10 / ≥ 20) | 123 (77 / 51) | 0 |
| | | wrong phase reached: Mid / Late / End | 2 / 19 / 10 | 1 / 1 / 0 |
| | | wrong milestone reached: 66 / 80 | 21 / 14 | 1 / 0 |
| | | missed injects, total | 2,868 | 1,448 |
| | | players above the energy bound (beyond the counting ratio) | 24 | 3 (1) |
| | | red seconds / overlapping a queen tumor | 90,844 / 25,777 | 49,820 / 12,130 |

The remaining personal differences are one 2v2 game where two players have the same name (the check matches players by id, the module by name). Multi analysis skips non-1v1 games.

Red that overlaps a tumor is no longer an error signal on its own: an inject queen can be late at the main while a creep queen spreads tumors elsewhere.

Missed injects per 10 minutes of game time:

| Who | Matchup | Players | 3.1 | Evidence | Orders |
|---|---|---:|---:|---:|---:|
| maintainer | ZvP | 31 | 12.8 | 6.8 | 12.3 |
| maintainer | ZvT | 35 | 20.8 | 7.6 | 17.0 |
| maintainer | ZvZ | 73 | 11.1 | 6.4 | 9.3 |
| maintainer's ZvZ opponents | ZvZ | 73 | 11.0 | 6.6 | 11.0 |
| pro | ZvP | 23 | 8.1 | 3.1 | 4.4 |
| pro | ZvT | 25 | 16.5 | 5.4 | 9.6 |
| pro | ZvZ | 52 | 7.2 | 3.3 | 4.3 |

3.1 to Evidence: ZvT dropped most: it is the matchup with the most creep spread, and 3.1 counted creep queens as idle inject queens. Evidence to Orders: idle queens without evidence now count until an order sends them away (the evidence model cut them off after 29 or 90 s). The largest increases are late-game gaps of 1.5 to 4 minutes between injects with an inject queen at her hatchery and no order, often near 200 supply (one Reynor ZvT: 22 to 63, at 190 to 200 supply from 10:13 to 21:00).

The energy bound is no longer strict: with the spare horizon, energy a queen hoards at a skipped hatchery counts as idle and can still pay for a creep burst more than 29 s later. The players above it are 2 to 8 injects over.

`queensMoved` fixture (main hatchery `2jl`): 3.1 had red 08:43–12:52 and 14:53–17:44. Evidence: 07:23–08:13 (the main queen is late, evidence: injects at 06:54 and 08:13, and she dies at the main at 08:40), 08:50–09:41 (queens seen and placing tumors next to the main), 11:47–12:52 (a new queen born at the main injects it 65 s later) and 14:52–15:53 (inject 14:23, next inject 17:44 by the same queen: counted up to the 90 s bridge limit). Missed injects of the player: 46 → 27.

Orders: 07:23–08:13, 08:46–09:41, 11:47–12:52 as before, and 15:21–17:44 (the larva creator fix finds the queued inject of 14:39, popping at 15:20; the four queens of the inject hotkey get no order from 14:40 to 17:43, and their energy reaches the cap). There is no testimony that those queens were away (an earlier note said so; it was an agent's guess from the folder name), so the red stands. Missed injects of the player: 66.

## Scenario replay (E19-S02)

`benchmark-data/queens-scenarios`: one ZvZ against the computer, written ground truth in `ground-truth.txt` (game clock; Scelight shows about 1 % later), resumed from replay at about 10:05 (`queens-by-scenario-interrupted.SC2Replay` is the original game up to 11:31). Regression: `ant TEST_GOLDEN_SCENARIO` (golden `test/golden/epic19-scenario-red.txt`).

| Ground truth (game clock) | Evidence model | Orders model |
|---|---|---|
| 6:08 to 7:08 queens walk to the watchtower and back | red at all four bases (about 190 s) | no red |
| 7:17 to 7:51 macro hatchery skipped, queen next to it with energy | no red | red 07:19 to 07:48 and 07:52 to 07:55 |
| 8:16 to 9:24 all hatcheries skipped, queens at them | red at the third and fourth only, cut at 90 s | red at main, natural, third, fourth (macro while the main is injected) |
| 9:58 third skipped, its queen idle with energy until creep at 11:00 | no red | red 10:00 to 10:37 (29 s before the creep burst) |
| 12:32 two creep queens, two queens idle at hatcheries | – | red at the main and the natural to the end |
| computer player (no commands in the replay) | 0 missed | 0 missed (evidence only) |
| 50 drones at 8:16 | Workers Active 51 | 51 |

Missed injects of the player: 19 (energy bound 14).

## Tuning knobs

All in `LarvaMissedInjectAnalysis`:

- `HOLD_LOOPS` = one inject (641 loops, 28.6 s): presence after evidence when the next evidence is elsewhere and no order sent her there (or, evidence only, missing). Equal to the inject length, so a queen that injects and leaves adds no red.
- `BRIDGE_LIMIT_SECONDS` = 90 (game clock seconds, `LarvaGameClock.secondsToLoops`): evidence only (orders not followed): presence after evidence when the next evidence is at the same hatchery.
- `ORDER_COVERAGE_MIN` = 0.5: orders are followed for a player when at least half of the injects have a Spawn Larva command with queens selected. Below it (computer players, replays without selections) the evidence-only rules apply.
- `SPARE_HORIZON_LOOPS` = one inject (641 loops): energy spent within this time is not spare; spends further ahead do not count.
- `QUEEN_CELLS_PER_SECOND` = 1.31: arrival estimates after a move (slow on purpose: arrivals count late).
- `RADIUS` = 12 cells, `INJECT_COMMAND_LOOKBACK_SECONDS` = 40. In `QueenSpendAttributor`: `MAX_CELLS_PER_LOOP` (4.5 cells/s), `CAST_REACH_CELLS` = 4, `DISTANCE_TIE_CELLS` = 3, `LOOK_AHEAD_NEAR_CELLS` = 12.

## Benchmark bands

One command since 2026-11-26 (about 45 s for 1,620 replays), method in `docs/benchmarking/statistical-analysis.md` ("League bands"):

```bash
cd LarvaSecondsLostExtMod
ant BENCHMARK_BANDS                     # or -Dmmr.refresh=true to download the current MMR-Ranges table first
ant TEST GOLDEN_MULTI_REGENERATE        # tests use fixture bands; the multi golden shows rankings
```

It writes the band file `src/hu/aleks/larvasecondslostextmod/larva-benchmark-bands.properties`, [benchmark-data/exports/benchmark-bands.md](../benchmark-data/exports/benchmark-bands.md) and [phase-rates.csv](../benchmark-data/exports/phase-rates.csv). Review the diff and commit.

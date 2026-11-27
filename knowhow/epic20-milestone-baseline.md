# Epic 20 milestone analysis: how to run it, and the numbers

The full tables behind [prompts/epic20-milestone-benchmarks.md](../prompts/epic20-milestone-benchmarks.md). Module code: the working tree of 2026-11-27 (3.2, after Epic 19: drones from `workersActiveCount`, game clock).

## Run

Scelight must not be running (single-instance socket). Classpath as in [epic17-batch-spike/env.sh.txt](epic17-batch-spike/env.sh.txt). The probe was a throwaway: since E20-S02/S03 the repo has `LarvaMilestoneDumpCli` and `ant MILESTONE_BANDS` (`benchmark-data/tools/milestone_bands.py`), which write `benchmark-data/exports/milestone-times.csv` in their own format (one column per `LarvaMilestone`, a `role` column, the ZvZ opponents included). The probe's output, which the tables below come from, is kept in `epic20-milestone-spike/`; on all 2,003 player-games the repo CLI gives the same loops.

```bash
# compile the module source and the probe into a scratch folder (leaves bin/ alone)
mkdir -p "$SP/src/hu/aleks/larvasecondslostextmod" "$SP/classes"
cp knowhow/epic20-milestone-spike/MilestoneDumpProbe.java.txt "$SP/src/hu/aleks/larvasecondslostextmod/MilestoneDumpProbe.java"
find "$M/src" "$SP/src" -name "*.java" > "$SP/sources.txt"
"$J8/javac" -nowarn -encoding UTF-8 -d "$SP/classes" -cp "$CPLIBS" @"$SP/sources.txt"

# replay list (league per replay, as the larva bands), then the dump: 38 s for 1,625 replays
python3 benchmark-data/tools/benchmark_bands.py list --output benchmark-data/tmp/bands-replays.tsv
export REPO="$R"
"$J8/java" -Xmx3g -cp "$SP/classes:$M/src:$CPLIBS" hu.aleks.larvasecondslostextmod.MilestoneDumpProbe \
    benchmark-data/tmp/bands-replays.tsv "$S" > knowhow/epic20-milestone-spike/milestone-times-probe.csv

# tables below
python3 -I knowhow/epic20-milestone-spike/milestone_analysis.py.txt "$R" \
    knowhow/epic20-milestone-spike/milestone-times-probe.csv knowhow/epic20-milestone-spike/milestone-times-zvz-opponents-probe.csv
```

The ZvZ opponent list (second Zerg of each ladder ZvZ, league from the opponent's own MMR and region) was built with `benchmark_bands.league_of` from `opponentMmr` and `opponentToon` in `replay-corpus-mmr-buckets-canonical.json`: 386 players, 383 with stats.

## Files

| File | What |
|---|---|
| `knowhow/epic20-milestone-spike/milestone-times-probe.csv` | One line per replay, primary Zerg player: cohort (league), matchup, result, replay length, last live sample, max workers, max supply, first sample loop at or above 50/100/150/200 supply and 30/44/55/66/75/80 drones (empty = not reached). 1,620 lines; 5 replays have no stats for the named player (`#` lines). |
| `knowhow/epic20-milestone-spike/milestone-times-zvz-opponents-probe.csv` | Same for the ZvZ opponents. |
| `knowhow/epic20-milestone-spike/MilestoneDumpProbe.java.txt` | The probe. Same rule as `LarvaMilestoneCalculator` (first `PlayerStats` sample ≥ threshold; supply in fixed point × 4096). |
| `knowhow/epic20-milestone-spike/milestone_analysis.py.txt` | The analysis below. |

**Check against the module.** On the `queensMoved` fixture the probe gives 100 supply 15040, 66 drones 18720, 80 drones not reached, 200 supply 23040 loops: 11:11, 13:55, n/a and 17:08, as in `test/golden/epic17-multi-queensMoved.txt`.

**Method.** As `benchmark_bands.py`: median per league of the games that reached the milestone; a tier starts halfway between its median and the median of the league below; the weakest tier half a step below its own median; a league needs 20 reached games; walking down from pro, a league whose median is not later than the next kept league above it is dropped. Times are loops, shown as m:ss on the game clock (loops / 22.4, rounded down), like `LarvaGameClock`.

# Results (2026-11-27)

## A. Corpus

Primary Zerg player per replay (as the larva bands). League from the player's MMR and region, MMR-Ranges season 69; pro = elite corpus (Reynor, Serral).

| league | ZvP | ZvT | ZvZ | all | median game length | ZvZ opponents (both-player check) |
|---|---:|---:|---:|---:|---:|---:|
| silver | 48 | 45 | 26 | 119 | 11:14 | 28 |
| gold | 36 | 31 | 36 | 103 | 11:26 | 20 |
| plat | 58 | 61 | 59 | 178 | 9:15 | 94 |
| dia | 85 | 93 | 112 | 290 | 9:29 | 103 |
| masters | 82 | 69 | 63 | 214 | 10:08 | 55 |
| GM | 89 | 99 | 98 | 286 | 9:32 | 83 |
| pro | 150 | 150 | 130 | 430 | 11:25 | 0 |

## B. 100 supply

**100 supply — All**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 60/119 (50%) | 8:23 | 7:46–10:16 | 8:57 | dropped |
| gold | 59/103 (57%) | 8:41 | 7:44–10:14 | 8:55 | 8:45 |
| plat | 81/178 (46%) | 8:34 | 7:26–9:31 | 8:53 | 8:37 |
| dia | 149/290 (51%) | 7:30 | 6:47–8:27 | 7:45 | 8:02 |
| masters | 154/214 (72%) | 7:01 | 6:32–7:51 | 7:19 | 7:15 |
| GM | 198/286 (69%) | 6:47 | 6:25–7:30 | 7:00 | 6:54 |
| pro | 384/430 (89%) | 6:11 | 5:57–6:32 | 6:22 | 6:29 |

Dropped: silver (median 8:23 not later than gold 8:41).

**100 supply — ZvP**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 19/48 (40%) | 8:20 | 7:30–9:02 | 8:32 | dropped |
| gold | 30/36 (83%) | 8:34 | 7:37–9:43 | 8:52 | 9:04 |
| plat | 30/58 (52%) | 7:33 | 7:08–8:45 | 8:14 | 8:03 |
| dia | 43/85 (51%) | 7:30 | 6:47–8:12 | 7:47 | 7:31 |
| masters | 67/82 (82%) | 7:01 | 6:32–7:37 | 7:12 | 7:15 |
| GM | 62/89 (70%) | 6:40 | 6:25–7:15 | 6:56 | 6:50 |
| pro | 147/150 (98%) | 6:18 | 6:04–6:32 | 6:24 | 6:29 |

Dropped: silver (19 reached (< 20)).

**100 supply — ZvT**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 32/45 (71%) | 9:20 | 8:12–10:28 | 9:30 | 9:26 |
| gold | 17/31 (55%) | 8:48 | 7:37–10:14 | 8:52 | dropped |
| plat | 42/61 (69%) | 9:10 | 7:58–10:16 | 9:20 | 9:15 |
| dia | 70/93 (75%) | 7:37 | 6:52–8:27 | 7:49 | 8:23 |
| masters | 56/69 (81%) | 7:01 | 6:32–7:28 | 7:06 | 7:19 |
| GM | 82/99 (83%) | 6:47 | 6:25–7:24 | 6:54 | 6:54 |
| pro | 137/150 (91%) | 6:11 | 5:57–6:32 | 6:21 | 6:29 |

Dropped: gold (17 reached (< 20)).

**100 supply — ZvZ**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 9/26 (35%) | 7:44 | 7:19–8:12 | 7:54 | dropped |
| gold | 12/36 (33%) | 9:10 | 7:58–11:04 | 9:08 | dropped |
| plat | 9/59 (15%) | 9:17 | 7:08–9:45 | 8:54 | dropped |
| dia | 36/112 (32%) | 7:22 | 6:47–8:27 | 7:33 | dropped |
| masters | 31/63 (49%) | 7:37 | 6:32–8:48 | 7:56 | 7:53 |
| GM | 54/98 (55%) | 7:05 | 6:32–7:53 | 7:12 | 7:21 |
| pro | 100/130 (77%) | 6:07 | 5:50–6:40 | 6:22 | 6:36 |

Dropped: dia (median 7:22 not later than masters 7:37); plat (9 reached (< 20)); gold (12 reached (< 20)); silver (9 reached (< 20)).

## B. 66 drones

**66 drones — All**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 32/119 (27%) | 10:00 | 8:28–12:35 | 10:37 | dropped |
| gold | 23/103 (22%) | 10:57 | 9:02–12:01 | 10:53 | 11:00 |
| plat | 33/178 (19%) | 10:50 | 9:02–13:55 | 11:24 | 10:53 |
| dia | 71/290 (24%) | 8:12 | 7:15–10:21 | 8:42 | 9:31 |
| masters | 97/214 (45%) | 7:15 | 6:32–8:34 | 7:49 | 7:44 |
| GM | 128/286 (45%) | 6:47 | 6:18–7:51 | 7:17 | 7:01 |
| pro | 297/430 (69%) | 6:18 | 5:57–7:01 | 6:46 | 6:32 |

Dropped: silver (median 10:00 not later than gold 10:57).

**66 drones — ZvP**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 10/48 (21%) | 9:06 | 7:56–10:23 | 9:32 | dropped |
| gold | 13/36 (36%) | 10:28 | 9:02–13:34 | 11:10 | dropped |
| plat | 9/58 (16%) | 9:31 | 8:16–11:15 | 9:57 | dropped |
| dia | 22/85 (26%) | 7:55 | 6:52–9:04 | 8:15 | 8:21 |
| masters | 41/82 (50%) | 7:01 | 6:15–8:16 | 7:24 | 7:28 |
| GM | 43/89 (48%) | 6:40 | 6:11–7:22 | 7:03 | 6:50 |
| pro | 126/150 (84%) | 6:18 | 5:57–6:54 | 6:42 | 6:29 |

Dropped: plat (9 reached (< 20)); gold (13 reached (< 20)); silver (10 reached (< 20)).

**66 drones — ZvT**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 18/45 (40%) | 10:21 | 8:32–12:49 | 10:51 | dropped |
| gold | 9/31 (29%) | 10:57 | 7:15–11:47 | 10:22 | dropped |
| plat | 22/61 (36%) | 11:54 | 9:31–14:18 | 11:59 | 13:37 |
| dia | 45/93 (48%) | 8:27 | 7:19–10:28 | 9:00 | 10:10 |
| masters | 45/69 (65%) | 7:30 | 6:36–8:30 | 7:40 | 7:58 |
| GM | 69/99 (70%) | 7:01 | 6:18–7:51 | 7:22 | 7:15 |
| pro | 123/150 (82%) | 6:18 | 5:57–7:08 | 6:41 | 6:40 |

Dropped: gold (9 reached (< 20)); silver (18 reached (< 20)).

**66 drones — ZvZ**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 4/26 (15%) | 11:18 | 8:09–17:24 | 12:17 | dropped |
| gold | 1/36 (3%) | 11:54 | 11:54–11:54 | 11:54 | dropped |
| plat | 2/59 (3%) | 11:36 | 8:18–14:54 | 11:36 | dropped |
| dia | 4/112 (4%) | 7:08 | 6:06–9:58 | 7:44 | dropped |
| masters | 11/63 (17%) | 9:52 | 7:01–11:40 | 9:57 | dropped |
| GM | 16/98 (16%) | 7:12 | 6:02–8:21 | 7:36 | dropped |
| pro | 48/130 (37%) | 6:43 | 5:58–7:22 | 7:08 | dropped |

Dropped: GM (16 reached (< 20)); masters (11 reached (< 20)); dia (4 reached (< 20)); plat (2 reached (< 20)); gold (1 reached (< 20)); silver (4 reached (< 20)).

## B. 80 drones

**80 drones — All**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 14/119 (12%) | 11:43 | 8:39–15:14 | 12:14 | dropped |
| gold | 5/103 (5%) | 8:48 | 8:34–13:34 | 10:37 | dropped |
| plat | 10/178 (6%) | 13:27 | 11:20–17:38 | 14:44 | dropped |
| dia | 27/290 (9%) | 9:52 | 8:05–11:25 | 9:59 | 10:08 |
| masters | 46/214 (21%) | 9:20 | 7:55–11:11 | 9:50 | 9:36 |
| GM | 56/286 (20%) | 8:34 | 7:38–10:12 | 9:24 | 8:57 |
| pro | 169/430 (39%) | 7:58 | 7:22–9:06 | 8:30 | 8:16 |

Dropped: plat (10 reached (< 20)); gold (5 reached (< 20)); silver (14 reached (< 20)).

**80 drones — ZvP**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 6/48 (12%) | 10:03 | 8:27–12:49 | 10:45 | dropped |
| gold | 1/36 (3%) | 8:48 | 8:48–8:48 | 8:48 | dropped |
| plat | 2/58 (3%) | 10:50 | 9:02–12:37 | 10:50 | dropped |
| dia | 7/85 (8%) | 10:21 | 8:12–11:18 | 9:53 | dropped |
| masters | 20/82 (24%) | 8:52 | 7:22–11:02 | 9:17 | 9:01 |
| GM | 25/89 (28%) | 8:34 | 7:30–10:03 | 9:25 | 8:43 |
| pro | 80/150 (53%) | 7:55 | 7:24–8:55 | 8:34 | 8:14 |

Dropped: dia (7 reached (< 20)); plat (2 reached (< 20)); gold (1 reached (< 20)); silver (6 reached (< 20)).

**80 drones — ZvT**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 7/45 (16%) | 13:34 | 8:12–15:00 | 12:28 | dropped |
| gold | 4/31 (13%) | 9:20 | 8:30–15:21 | 11:04 | dropped |
| plat | 7/61 (11%) | 15:00 | 12:22–22:44 | 16:36 | dropped |
| dia | 18/93 (19%) | 9:35 | 7:55–11:52 | 10:03 | dropped |
| masters | 24/69 (35%) | 9:20 | 8:02–11:22 | 10:11 | 9:49 |
| GM | 30/99 (30%) | 8:23 | 7:47–10:16 | 9:14 | 8:52 |
| pro | 87/150 (58%) | 8:05 | 7:15–9:24 | 8:27 | 8:14 |

Dropped: dia (18 reached (< 20)); plat (7 reached (< 20)); gold (4 reached (< 20)); silver (7 reached (< 20)).

**80 drones — ZvZ**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 1/26 (4%) | 19:31 | 19:31–19:31 | 19:31 | dropped |
| gold | 0/36 (0%) | - | -–- | - | dropped |
| plat | 1/59 (2%) | 9:24 | 9:24–9:24 | 9:24 | dropped |
| dia | 2/112 (2%) | 9:42 | 7:06–12:17 | 9:42 | dropped |
| masters | 2/63 (3%) | 11:04 | 9:49–12:19 | 11:04 | dropped |
| GM | 1/98 (1%) | 14:17 | 14:17–14:17 | 14:17 | dropped |
| pro | 2/130 (2%) | 7:55 | 7:17–8:32 | 7:55 | dropped |

Dropped: pro (2 reached (< 20)); GM (1 reached (< 20)); masters (2 reached (< 20)); dia (2 reached (< 20)); plat (1 reached (< 20)); gold (0 reached (< 20)); silver (1 reached (< 20)).

## B. 200 supply

**200 supply — All**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 15/119 (13%) | 15:00 | 14:31–19:02 | 16:05 | dropped |
| gold | 11/103 (11%) | 13:34 | 12:37–15:14 | 15:07 | dropped |
| plat | 8/178 (4%) | 17:37 | 13:37–23:05 | 18:28 | dropped |
| dia | 16/290 (6%) | 12:30 | 10:58–15:37 | 13:17 | dropped |
| masters | 29/214 (14%) | 12:08 | 10:21–14:52 | 12:50 | 12:12 |
| GM | 46/286 (16%) | 12:01 | 10:39–15:16 | 13:09 | 12:05 |
| pro | 161/430 (37%) | 10:42 | 9:31–12:05 | 11:05 | 11:22 |

Dropped: dia (16 reached (< 20)); plat (8 reached (< 20)); gold (11 reached (< 20)); silver (15 reached (< 20)).

**200 supply — ZvP**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 5/48 (10%) | 14:52 | 13:41–15:00 | 14:27 | dropped |
| gold | 6/36 (17%) | 13:52 | 12:13–16:16 | 14:35 | dropped |
| plat | 0/58 (0%) | - | -–- | - | dropped |
| dia | 6/85 (7%) | 12:15 | 11:16–15:51 | 13:15 | dropped |
| masters | 11/82 (13%) | 12:08 | 9:45–15:00 | 13:15 | dropped |
| GM | 18/89 (20%) | 11:18 | 10:39–16:27 | 13:04 | dropped |
| pro | 65/150 (43%) | 10:50 | 9:45–12:22 | 11:17 | dropped |

Dropped: GM (18 reached (< 20)); masters (11 reached (< 20)); dia (6 reached (< 20)); plat (0 reached (< 20)); gold (6 reached (< 20)); silver (5 reached (< 20)).

**200 supply — ZvT**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 10/45 (22%) | 15:32 | 14:51–19:29 | 16:55 | dropped |
| gold | 5/31 (16%) | 13:34 | 12:26–20:14 | 15:47 | dropped |
| plat | 8/61 (13%) | 17:37 | 13:37–23:05 | 18:28 | dropped |
| dia | 10/93 (11%) | 12:44 | 10:28–16:07 | 13:19 | dropped |
| masters | 12/69 (17%) | 12:15 | 11:40–15:25 | 13:12 | dropped |
| GM | 21/99 (21%) | 12:44 | 10:35–15:10 | 13:11 | 13:41 |
| pro | 65/150 (43%) | 10:50 | 10:00–12:33 | 11:25 | 11:47 |

Dropped: masters (12 reached (< 20)); dia (10 reached (< 20)); plat (8 reached (< 20)); gold (5 reached (< 20)); silver (10 reached (< 20)).

**200 supply — ZvZ**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 0/26 (0%) | - | -–- | - | dropped |
| gold | 0/36 (0%) | - | -–- | - | dropped |
| plat | 0/59 (0%) | - | -–- | - | dropped |
| dia | 0/112 (0%) | - | -–- | - | dropped |
| masters | 6/63 (10%) | 10:39 | 9:45–13:05 | 11:19 | dropped |
| GM | 7/98 (7%) | 12:30 | 9:02–16:32 | 13:13 | dropped |
| pro | 31/130 (24%) | 9:45 | 8:48–10:28 | 9:57 | dropped |

Dropped: GM (7 reached (< 20)); masters (6 reached (< 20)); dia (0 reached (< 20)); plat (0 reached (< 20)); gold (0 reached (< 20)); silver (0 reached (< 20)).

## B. 44 drones

**44 drones — All**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 68/119 (57%) | 7:08 | 5:57–8:12 | 7:27 | 7:26 |
| gold | 63/103 (61%) | 6:32 | 5:28–8:12 | 6:58 | 6:50 |
| plat | 93/178 (52%) | 5:57 | 5:14–7:55 | 6:42 | 6:15 |
| dia | 171/290 (59%) | 5:21 | 4:45–6:47 | 5:57 | 5:39 |
| masters | 158/214 (74%) | 5:00 | 4:38–5:42 | 5:27 | 5:10 |
| GM | 212/286 (74%) | 4:45 | 4:31–5:28 | 5:10 | 4:52 |
| pro | 383/430 (89%) | 4:38 | 4:24–5:00 | 4:51 | 4:42 |

**44 drones — ZvP**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 24/48 (50%) | 7:40 | 6:34–8:11 | 8:00 | 8:02 |
| gold | 30/36 (83%) | 6:57 | 5:33–8:30 | 7:16 | 7:19 |
| plat | 38/58 (66%) | 5:50 | 5:00–6:56 | 6:27 | 6:23 |
| dia | 58/85 (68%) | 5:14 | 4:43–6:06 | 5:33 | 5:32 |
| masters | 68/82 (83%) | 5:00 | 4:45–5:55 | 5:29 | 5:07 |
| GM | 63/89 (71%) | 4:45 | 4:31–5:07 | 5:07 | 4:52 |
| pro | 146/150 (97%) | 4:38 | 4:29–4:45 | 4:44 | 4:42 |

**44 drones — ZvT**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 32/45 (71%) | 6:29 | 5:57–8:52 | 7:17 | 6:48 |
| gold | 20/31 (65%) | 5:42 | 5:14–6:11 | 5:52 | dropped |
| plat | 47/61 (77%) | 5:50 | 5:14–8:34 | 6:48 | 6:09 |
| dia | 77/93 (83%) | 5:07 | 4:38–6:29 | 5:42 | 5:28 |
| masters | 60/69 (87%) | 5:00 | 4:38–5:21 | 5:06 | 5:03 |
| GM | 89/99 (90%) | 4:38 | 4:31–5:17 | 4:59 | 4:49 |
| pro | 138/150 (92%) | 4:31 | 4:24–4:45 | 4:46 | 4:35 |

Dropped: gold (median 5:42 not later than plat 5:50).

**44 drones — ZvZ**

| league | reached k/n | median | p25–p75 | mean | tier start |
|---|---:|---:|---:|---:|---:|
| silver | 12/26 (46%) | 6:15 | 5:57–7:46 | 6:50 | dropped |
| gold | 13/36 (36%) | 7:58 | 6:15–8:59 | 7:57 | dropped |
| plat | 8/59 (14%) | 6:22 | 5:50–8:48 | 7:23 | dropped |
| dia | 36/112 (32%) | 6:47 | 5:39–8:18 | 7:09 | 7:24 |
| masters | 30/63 (48%) | 5:32 | 4:45–6:59 | 6:03 | 6:09 |
| GM | 60/98 (61%) | 5:17 | 4:38–6:04 | 5:30 | 5:25 |
| pro | 99/130 (76%) | 4:52 | 4:24–5:21 | 5:06 | 5:05 |

Dropped: plat (8 reached (< 20)); gold (13 reached (< 20)); silver (12 reached (< 20)).

## C. How well each milestone follows MMR

Spearman rank correlation of the milestone time with the player's MMR, ladder games that reached it (no pro). More negative = higher MMR reaches it earlier. Pro vs GM: medians of reached games.

| milestone | All rho (k) | ZvP rho | ZvT rho | ZvZ rho | reached, silver / plat / masters / GM / pro (All) | GM median − pro median |
|---|---:|---:|---:|---:|---|---:|
| 100 supply | -0.54 (701) | -0.57 | -0.66 | -0.31 | 50% / 46% / 72% / 69% / 89% | +35.7 s |
| 66 drones | -0.58 (384) | -0.57 | -0.61 | -0.47 | 27% / 19% / 45% / 45% / 69% | +28.6 s |
| 80 drones | -0.36 (158) | -0.26 | -0.42 | nan | 12% / 6% / 21% / 20% / 39% | +35.7 s |
| 200 supply | -0.35 (125) | -0.35 | -0.41 | 0.19 | 13% / 4% / 14% / 16% / 37% | +78.6 s |
| 50 supply (extra) | -0.48 (942) | -0.52 | -0.60 | -0.36 | 71% / 71% / 83% / 89% / 98% | +14.3 s |
| 30 drones (extra) | -0.36 (922) | -0.36 | -0.48 | -0.29 | 71% / 69% / 84% / 86% / 96% | +7.1 s |
| 44 drones (extra) | -0.48 (765) | -0.52 | -0.53 | -0.50 | 57% / 52% / 74% / 74% / 89% | +7.1 s |
| 55 drones (extra) | -0.54 (566) | -0.54 | -0.62 | -0.43 | 45% / 29% / 62% / 63% / 81% | +28.6 s |
| 75 drones (extra) | -0.47 (222) | -0.38 | -0.54 | -0.02 | 16% / 8% / 29% / 27% / 48% | +21.4 s |
| 150 supply (extra) | -0.54 (431) | -0.46 | -0.63 | -0.45 | 35% / 17% / 46% / 48% / 74% | +46.4 s |

## D. Bias checks (All matchups)

### D1. Reached-only median vs Kaplan–Meier median

Reached-only (what the table and these bands use) leaves out games that ended first. Kaplan–Meier counts them as censored at their last sample; it has no value when fewer than half the games get there.

| milestone | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|
| 100 supply | 8:23 / 9:17 | 8:41 / 9:17 | 8:34 / 9:17 | 7:30 / 7:58 | 7:01 / 7:08 | 6:47 / 6:54 | 6:11 / 6:18 |
| 66 drones | 10:00 / 15:50 | 10:57 / 17:08 | 10:50 / 14:52 | 8:12 / 13:55 | 7:15 / 8:55 | 6:47 / 8:27 | 6:18 / 6:54 |
| 80 drones | 11:43 / - | 8:48 / - | 13:27 / - | 9:52 / - | 9:20 / 19:45 | 8:34 / 20:07 | 7:58 / 11:40 |
| 200 supply | 15:00 / 19:45 | 13:34 / 25:14 | 17:37 / 27:44 | 12:30 / - | 12:08 / 27:44 | 12:01 / 17:30 | 10:42 / 12:22 |

### D2. Winners vs losers (reached-only median)

| milestone | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|
| 100 supply | 8:23 / 8:34 | 8:34 / 8:48 | 8:12 / 8:55 | 7:33 / 7:30 | 6:57 / 7:08 | 6:43 / 6:50 | 6:11 / 6:11 |
| 66 drones | 9:10 / 10:07 | 9:45 / 11:15 | 13:20 / 9:45 | 8:20 / 8:09 | 7:01 / 7:22 | 6:47 / 6:54 | 6:18 / 6:25 |
| 80 drones | 8:48 / 13:34 | 8:41 / 9:24 | 15:14 / 11:57 | 10:21 / 8:37 | 10:03 / 8:37 | 8:12 / 8:41 | 7:51 / 8:12 |
| 200 supply | 15:00 / 15:32 | 14:10 / 13:34 | 19:02 / 16:11 | 12:01 / 13:30 | 12:05 / 12:08 | 12:12 / 11:40 | 10:35 / 10:42 |

Cells: winners / losers.

### D3. ZvZ: primary player only vs both Zerg players

Both: the ZvZ opponent added with the league of their own MMR (383 players).

| milestone | silver | gold | plat | dia | masters | GM |
|---|---:|---:|---:|---:|---:|---:|
| 100 supply | 7:44 (9) / 7:51 (20) | 9:10 (12) / 8:55 (19) | 9:17 (9) / 8:05 (28) | 7:22 (36) / 7:22 (70) | 7:37 (31) / 7:30 (51) | 7:05 (54) / 7:01 (103) |
| 66 drones | 11:18 (4) / 10:07 (9) | 11:54 (1) / 10:14 (4) | 11:36 (2) / 10:10 (8) | 7:08 (4) / 7:22 (13) | 9:52 (11) / 9:13 (18) | 7:12 (16) / 6:47 (31) |
| 80 drones | 19:31 (1) / 19:31 (1) | - (0) / 18:41 (1) | 9:24 (1) / 9:24 (1) | 9:42 (2) / 9:42 (4) | 11:04 (2) / 11:04 (2) | 14:17 (1) / 13:30 (2) |
| 200 supply | - (0) / 11:32 (1) | - (0) / - (0) | - (0) / 11:29 (2) | - (0) / 10:00 (5) | 10:39 (6) / 10:39 (6) | 12:30 (7) / 12:58 (15) |

Cells: median (reached) primary only / both players.

### D4. Sampling: milestone loops are PlayerStats samples

2379 milestone loops; 100% are multiples of 160. A median can only move in 160-loop (7.1 s) steps; two leagues 1 sample apart are 7 s apart.

## E. Leagues with a tier, per milestone and matchup

Kept league medians (reached games) after the 20-game and must-improve rules; `n/a` = fewer than two tiers, not rated.

| milestone | All | ZvP | ZvT | ZvZ |
|---|---|---|---|---|
| 100 supply | gold 8:41 plat 8:34 dia 7:30 masters 7:01 GM 6:47 pro 6:11 | gold 8:34 plat 7:33 dia 7:30 masters 7:01 GM 6:40 pro 6:18 | silver 9:20 plat 9:10 dia 7:37 masters 7:01 GM 6:47 pro 6:11 | masters 7:37 GM 7:05 pro 6:07 |
| 66 drones | gold 10:57 plat 10:50 dia 8:12 masters 7:15 GM 6:47 pro 6:18 | dia 7:55 masters 7:01 GM 6:40 pro 6:18 | plat 11:54 dia 8:27 masters 7:30 GM 7:01 pro 6:18 | n/a |
| 80 drones | dia 9:52 masters 9:20 GM 8:34 pro 7:58 | masters 8:52 GM 8:34 pro 7:55 | masters 9:20 GM 8:23 pro 8:05 | n/a |
| 200 supply | masters 12:08 GM 12:01 pro 10:42 | n/a | GM 12:44 pro 10:50 | n/a |
| 50 supply (extra) | silver 5:07 gold 4:52 plat 4:31 dia 4:24 masters 4:10 GM 4:02 pro 3:48 | silver 5:07 gold 4:45 dia 4:24 masters 4:10 GM 4:02 pro 3:48 | silver 5:07 gold 5:00 plat 4:31 dia 4:10 masters 4:02 GM 3:55 pro 3:48 | gold 4:52 dia 4:45 masters 4:31 GM 4:17 pro 3:55 |
| 30 drones (extra) | gold 4:17 plat 3:48 masters 3:41 GM 3:34 pro 3:27 | silver 4:31 gold 3:55 plat 3:48 masters 3:41 GM 3:34 pro 3:27 | silver 4:17 gold 4:10 plat 3:48 GM 3:34 pro 3:27 | gold 4:38 dia 4:13 masters 3:48 GM 3:41 pro 3:30 |
| 44 drones (extra) | silver 7:08 gold 6:32 plat 5:57 dia 5:21 masters 5:00 GM 4:45 pro 4:38 | silver 7:40 gold 6:57 plat 5:50 dia 5:14 masters 5:00 GM 4:45 pro 4:38 | silver 6:29 plat 5:50 dia 5:07 masters 5:00 GM 4:38 pro 4:31 | dia 6:47 masters 5:32 GM 5:17 pro 4:52 |
| 55 drones (extra) | plat 8:34 dia 6:50 masters 6:11 GM 5:50 pro 5:21 | gold 8:27 dia 6:36 masters 6:07 GM 5:39 pro 5:21 | plat 9:10 dia 7:01 masters 6:04 GM 5:42 pro 5:21 | GM 6:18 pro 5:50 |
| 75 drones (extra) | dia 9:17 masters 8:41 GM 7:44 pro 7:22 | masters 8:27 GM 7:44 pro 7:22 | dia 9:17 masters 8:48 GM 7:44 pro 7:15 | n/a |
| 150 supply (extra) | silver 12:01 plat 11:47 dia 9:38 masters 9:02 GM 8:37 pro 7:51 | gold 10:57 dia 9:02 GM 8:48 pro 7:51 | plat 12:22 dia 10:07 masters 9:10 GM 8:41 pro 7:58 | GM 8:34 pro 7:51 |

## F. Mean minus median (All, reached games)

The multi page shows the mean; bands use medians. Seconds the mean is later than the median:

| milestone | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|
| 100 supply | +34 | +14 | +19 | +15 | +18 | +13 | +11 |
| 66 drones | +38 | -4 | +35 | +29 | +34 | +31 | +28 |
| 80 drones | - | - | - | +6 | +30 | +51 | +32 |
| 200 supply | - | - | - | - | +42 | +68 | +23 |
| 44 drones | +19 | +25 | +46 | +36 | +27 | +24 | +13 |

`-`: fewer than 20 reached games.

## G. Tier starts (loops), as a band file would hold them

```
milestone.supply100.All=gold:11760,plat:11600,dia:10800,masters:9760,GM:9280,pro:8720
milestone.supply100.ZvP=gold:12200,plat:10840,dia:10120,masters:9760,GM:9200,pro:8720
milestone.supply100.ZvT=silver:12680,plat:12440,dia:11280,masters:9840,GM:9280,pro:8720
milestone.supply100.ZvZ=masters:10600,GM:9880,pro:8880
milestone.drones66.All=gold:14800,plat:14640,dia:12800,masters:10400,GM:9440,pro:8800
milestone.drones66.ZvP=dia:11240,masters:10040,GM:9200,pro:8720
milestone.drones66.ZvT=plat:18320,dia:13680,masters:10720,GM:9760,pro:8960
milestone.drones66.ZvZ=(not rated)
milestone.drones80.All=dia:13640,masters:12920,GM:12040,pro:11120
milestone.drones80.ZvP=masters:12120,GM:11720,pro:11080
milestone.drones80.ZvT=masters:13200,GM:11920,pro:11080
milestone.drones80.ZvZ=(not rated)
milestone.supply200.All=masters:16400,GM:16240,pro:15280
milestone.supply200.ZvP=(not rated)
milestone.supply200.ZvT=GM:18400,pro:15840
milestone.supply200.ZvZ=(not rated)
milestone.drones44.All=silver:10000,gold:9200,plat:8400,dia:7600,masters:6960,GM:6560,pro:6320
milestone.drones44.ZvP=silver:10800,gold:9840,plat:8600,dia:7440,masters:6880,GM:6560,pro:6320
milestone.drones44.ZvT=silver:9160,plat:8280,dia:7360,masters:6800,GM:6480,pro:6160
milestone.drones44.ZvZ=dia:9960,masters:8280,GM:7280,pro:6840
```

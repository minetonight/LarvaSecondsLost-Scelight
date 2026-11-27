# Epic 20 — Benchmark the milestone table by league and matchup

The multi-replay page shows when a player first reached 100 supply, 66 drones, 80 drones and 200 supply, per bucket (All, ZvP, ZvT, ZvZ). These times cannot be compared with anything yet. The larva tables can: each rate has a ranking row such as "top 30% plat" or "top 15% masters", from league bands built on the benchmark corpus. This epic gives the milestone table the same kind of ranking, built from our replays (overall and per matchup), and fixes what the analysis found in the way.

Written for the maintainer and for the agents that will implement this epic. All numbers come from the module code in the working tree on 2026-11-27 (3.2, after Epic 19), run over the benchmark corpus. The full tables are in [knowhow/epic20-milestone-baseline.md](../knowhow/epic20-milestone-baseline.md).

## 0. Summary

- **What works.** 100 supply and 66 drones separate the leagues from Diamond up: medians get earlier with every league, and the times follow MMR (Spearman ρ −0.54 and −0.58). With the larva band method they get 6 tiers in All, 4 to 6 in ZvP and ZvT.
- **What does not.**
  - **Below Diamond, reached times barely change with skill.** For 100 supply, silver, gold and plat reach it at 8:23, 8:41 and 8:34 (median). What changes is how often they get there at all.
  - **80 drones and 200 supply are rare.** Below GM, 4–21% of ladder games reach them. In All only Diamond (80 drones) or masters (200 supply) and up get a tier; ZvP, ZvT and ZvZ mostly get none.
  - **ZvZ almost never reaches 66 or 80 drones** (≤ 17% below pro, 2% for 80 drones even at pro), so ZvZ can rate only 100 supply, and only from masters up.
- **A better milestone exists: 44 drones** (two-base saturation). It is reached in 52–89% of games, is rated in every league in All and ZvP, in 6 leagues in ZvT and from Diamond up in ZvZ, with clean steps from silver 7:08 to pro 4:38.
- **The table shows the wrong statistic for a ranking.** Cells show the mean over reached games. The mean is mostly 11–46 s later than the median, as much as the 14–36 s between the masters, GM and pro medians of 100 supply and 66 drones. A typical GM player (66 drones: median 6:47, mean 7:17) would be rated masters. Rank, and show, the median.
- **Sampling.** Milestone loops are `PlayerStats` samples, every 160 loops (7.1 s). Neighbouring top tiers are often only 1–5 samples apart.
- **Plan.** New milestones (44 and 55 drones, 150 supply) → repo dump CLI → band tool (`ant MILESTONE_BANDS`) → median cells in loops → shared tier logic and a milestone formatter → ranking rows on the multi page → milestones on the single-replay page → help and release 3.3.
- **Status (2026-11-27):** implemented; 3.3 is built and committed in the Pages clone, the push waits for the maintainer; see section 8.

## 1. How this was measured

- **Probe.** A throwaway `main` class, `MilestoneDumpProbe` (kept as [knowhow/epic20-milestone-spike/MilestoneDumpProbe.java.txt](../knowhow/epic20-milestone-spike/MilestoneDumpProbe.java.txt)), compiled with the module source into a scratch folder. Per replay it runs `LarvaReplayAnalyzer.analyze` and takes the first `PlayerStats` sample at or above 50/100/150/200 supply and 30/44/55/66/75/80 drones (the `LarvaMilestoneCalculator` rule), plus game length, last live sample and result. 1,625 replays took 38 s.
- **Check.** On the `queensMoved` fixture the probe gives exactly the golden's milestones (`test/golden/epic17-multi-queensMoved.txt`: 11:11, 13:55, n/a, 17:08).
- **Corpus.** The larva band corpus (`benchmark_bands.py list`): 1,200 ladder replays with MMR (400 per matchup), league from the Zerg player's MMR and region by MMR-Ranges season 69, and 430 pro replays (Reynor and Serral, tournaments). One player per replay, the primary Zerg, as for the larva bands. **1,620 player-games** (5 replays have no stats for the named player). Check run: the second Zerg of the ladder ZvZ games (383 players, league from their own MMR).
- **Method.** That of `benchmark_bands.py`: median per league over the games that reached the milestone; tier start halfway between a league's median and the median of the league below; the weakest tier half a step below its own median; 20 reached games per league; going down from pro, a league whose median is not later than the next kept league above it is dropped. Lower time is better.
- **Data.** `knowhow/epic20-milestone-spike/milestone-times-probe.csv` (one line per replay) and `milestone-times-zvz-opponents-probe.csv` (the probe output; since S02 the repo CLI writes `benchmark-data/exports/milestone-times.csv`). Analysis script: `knowhow/epic20-milestone-spike/milestone_analysis.py.txt`.

| league | ZvP | ZvT | ZvZ | all | median game length |
|---|---:|---:|---:|---:|---:|
| silver | 48 | 45 | 26 | 119 | 11:14 |
| gold | 36 | 31 | 36 | 103 | 11:26 |
| plat | 58 | 61 | 59 | 178 | 9:15 |
| dia | 85 | 93 | 112 | 290 | 9:29 |
| masters | 82 | 69 | 63 | 214 | 10:08 |
| GM | 89 | 99 | 98 | 286 | 9:32 |
| pro | 150 | 150 | 130 | 430 | 11:25 |

No bronze replays.

## 2. Findings

### F-1 Median time and how often it is reached (All matchups)

Median over the games that reached it, and the share of games that did:

| milestone | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|
| 44 drones *(new)* | 7:08 (57%) | 6:32 (61%) | 5:57 (52%) | 5:21 (59%) | 5:00 (74%) | 4:45 (74%) | 4:38 (89%) |
| 100 supply | 8:23 (50%) | 8:41 (57%) | 8:34 (46%) | 7:30 (51%) | 7:01 (72%) | 6:47 (69%) | 6:11 (89%) |
| 66 drones | 10:00 (27%) | 10:57 (22%) | 10:50 (19%) | 8:12 (24%) | 7:15 (45%) | 6:47 (45%) | 6:18 (69%) |
| 80 drones | 11:43 (12%) | 8:48 (5%) | 13:27 (6%) | 9:52 (9%) | 9:20 (21%) | 8:34 (20%) | 7:58 (39%) |
| 200 supply | 15:00 (13%) | 13:34 (11%) | 17:37 (4%) | 12:30 (6%) | 12:08 (14%) | 12:01 (16%) | 10:42 (37%) |

Per matchup: baseline section B.

### F-2 Which leagues get a tier

League medians kept by the rules (20 reached games, must improve with skill). "n/a" = fewer than two tiers, so the cell would not be rated.

| milestone | All | ZvP | ZvT | ZvZ |
|---|---|---|---|---|
| 44 drones *(new)* | silver to pro (7) | silver to pro (7) | silver, plat to pro (6) | dia to pro (4) |
| 100 supply | gold to pro (6) | gold to pro (6) | silver, plat to pro (6) | masters to pro (3) |
| 66 drones | gold to pro (6) | dia to pro (4) | plat to pro (5) | n/a |
| 80 drones | dia to pro (4) | masters to pro (3) | masters to pro (3) | n/a |
| 200 supply | masters to pro (3) | n/a | GM, pro (2) | n/a |

Band file lines (tier starts in loops) for all of these are in baseline section G. For example: `100 supply, All: gold 11760, plat 11600, dia 10800, masters 9760, GM 9280, pro 8720`.

### F-3 Below Diamond the reached times say little

- For 100 supply and 66 drones, the silver, gold and plat medians lie within 18 s and 57 s of each other and are not in order (silver is dropped for being faster than gold). Gold and plat are kept 7 s apart (one sample), so the gold/plat boundary is noise.
- **Survivorship.** Only the games that got there count. Kaplan–Meier, which counts a game that ended first as censored, gives 66 drones at 15:50 silver, 14:52 plat, 13:55 dia, 8:55 masters, 8:27 GM and 6:54 pro, against 10:00 to 6:18 reached-only. A low-league player who reaches 66 drones does it at a decent time; most low-league games just never get there.
- **Result bias is small.** Winners and losers of the same league reach 100 supply and 66 drones within 0–21 s of each other from masters up (baseline D2).
- **Consequence.** The ranking answers "when you got there, how fast was it". The `k/n` in the cell answers "how often". Both stay visible; the help page must say so.

### F-4 Mean versus median

The multi page shows the mean over reached games; bands are league medians of single games. The time distribution has a long late tail:

| milestone | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|
| 44 drones | +19 s | +25 s | +46 s | +36 s | +27 s | +24 s | +13 s |
| 100 supply | +34 s | +14 s | +19 s | +15 s | +18 s | +13 s | +11 s |
| 66 drones | +38 s | −4 s | +35 s | +29 s | +34 s | +31 s | +28 s |

The table shows mean minus median, over the games that reached the milestone. The tiers between masters, GM and pro are 14–36 s wide, so rating the mean puts a typical player about one tier too low. A median compared with league medians has no such offset. (It still sits nearer the middle than single games do: the same caveat as the larva rankings.)

### F-5 How well each milestone follows skill

Spearman ρ of the reached time with the player's MMR (ladder, reached games), and the GM − pro gap:

| milestone | ρ All (games) | ρ ZvP | ρ ZvT | ρ ZvZ | GM − pro |
|---|---:|---:|---:|---:|---:|
| 44 drones | −0.48 (765) | −0.52 | −0.53 | −0.50 | +7 s |
| 100 supply | −0.54 (701) | −0.57 | −0.66 | −0.31 | +36 s |
| 55 drones | −0.54 (566) | −0.54 | −0.62 | −0.43 | +29 s |
| 66 drones | −0.58 (384) | −0.57 | −0.61 | −0.47 | +29 s |
| 150 supply | −0.54 (431) | −0.46 | −0.63 | −0.45 | +46 s |
| 80 drones | −0.36 (158) | −0.26 | −0.42 | n/a | +36 s |
| 200 supply | −0.35 (125) | −0.35 | −0.41 | +0.19 | +79 s |

- 44 drones is the only drone milestone that tracks skill in ZvZ (−0.50) and that most games reach. It hardly separates GM from pro (one sample).
- 66 drones follows skill best, then 100 supply, 55 drones and 150 supply. 80 drones and 200 supply follow it least, and are rarest.

### F-6 ZvZ with both Zerg players

387 of the 400 ladder ZvZ replays have the opponent's MMR. Adding the opponent (league from their own MMR) about doubles the ZvZ sample:

- 44 drones ZvZ: gold 7:01, plat 6:25, dia 6:11, masters 5:35, GM 5:07, pro 4:52, so 6 tiers instead of 4;
- 100 supply ZvZ: plat 8:05, masters 7:30, GM 7:01, pro 6:07, so 4 tiers instead of 3;
- 66 drones ZvZ: GM 6:47 and pro 6:43, two tiers 4 s apart, which means nothing; 80 drones and 200 supply stay n/a.

The pro ZvZ opponents are not in the manifest, so the pro tier does not grow.

### F-7 Sampling and the pro tier

- Every milestone loop is a multiple of 160 (`PlayerStats` sample). Medians move in 7.1 s steps (3.6 s when two middle values are averaged). Masters → GM → pro gaps are 1 to 6 samples for most milestones (100 supply: 2 and 5; 44 drones: 2 and 1), so "top X%" inside a tier is coarse.
- The pro tier is two players (Reynor, Serral) in tournament games. Same as for the larva bands.

## 3. Design

**Band method.** As the larva bands (`docs/benchmarking/statistical-analysis.md`, "League bands"), per milestone and bucket, lower is better. A cell with one tier is a pace, a cell without a tier is not rated (`n/a`) (E20-S10).

**Band file.** `LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/larva-milestone-bands.properties`, generated, never edited by hand:

```
# <LarvaMilestone>.<LarvaMatchupBucket>=<label>:<tier start loop>,... weakest tier first; lower is better.
SUPPLY_100.ALL=gold:11760,plat:11600,dia:10800,masters:9760,GM:9280,pro:8720
```

Loops only. Times are shown by `LarvaGameClock` (`knowhow/time-management.md`).

**Player value.** Per bucket and milestone: the median loop over the player's games that reached it (OQ-1). Even count: mean of the two middle loops, as Python's `statistics.median` does in the band tool.

**Rating text.** Same as the larva rows: `top <p>% <label>`, `p` from the position inside the tier, in 5% steps. A time later than the weakest tier start reads `aim for m:ss`, the time where that tier starts (E20-S12; it replaced `slower than <label>`, OQ-5: no percentages there). One tier: `<label> pace m:ss` at or before its start (E20-S10). Inside the bands the league label is bold (E20-S12). No band, or `k = 0`: `n/a`.

**One-sample rule (added during S03).** Besides the larva band rules, a league is dropped when its median is less than one `PlayerStats` sample (160 loops) later than the next kept league above it: milestone loops are samples, so a smaller gap cannot be told apart. It removed two meaningless tiers: plat in ZvP 100 supply (7:33 against dia 7:30) and the ZvZ 66 drones band (GM 6:47 against pro 6:43). This is the maintainer's "no ranking where it is impractical". Since E20-S10 a league dropped by this rule is named in the tier it was too close to (`plat/dia`, `GM/pro`), and ZvZ 66 drones is the pace `GM/pro pace 6:47`.

**Table.** Below each milestone row, a ranking row, as in the larva tables. Example with the bands of baseline section G (ZvZ without the opponents; since E20-S11 the rows read `44 drones made by:` and a ditto row `"  ranking`, and the times are bold):

```
Milestone            All            ZvP              ZvT            ZvZ
44 drones alive      4:50 (40/52)   4:59 (18/20)     5:14 (14/18)   5:28 (8/14)
44 drones ranking    top 75% GM     top 45% masters  top 45% dia    top 10% masters
100 supply used      ...
80 drones alive      10:40 (3/52)   10:40 (2/20)     10:31 (1/18)   n/a (0/14)
80 drones ranking    slower than dia  slower than masters  slower than masters  n/a
```

## 4. Open questions (each has a default)

| # | Question | Default | Maintainer answers
|---|---|---|---|
| OQ-1 | Milestone cells: switch from mean to median (F-4)? | Yes. The cell shows the median, and the ranking rates it. Title "Milestones (median time, reached / games)". | yes, median |
| OQ-2 | Add a 44-drone milestone (F-1, F-2, F-5)? | Yes, as the first row (it comes first in the game). | yes, 44 and 55 drones, also 150 supply, add them all and keep the initial milestones too, without ranking where where its impractical |
| OQ-3 | 80 drones and 200 supply are rated only from masters (All) and mostly n/a per matchup. Keep them? | Keep the rows and rate where bands exist. Alternatives for later: 55/75 drones (baseline E), or counts at a fixed time (drones at 6:00), which every game that lasts long enough has. | keep them, as they are common sense and personal benchmarks among players. | 
| OQ-4 | ZvZ bands with both Zerg players (F-6)? | Yes, only in the ZvZ bucket. All keeps one player per replay, so ZvZ does not get twice the weight there. The larva bands are not changed. | yes |
| OQ-5 | A time later than the weakest tier: `top 100% masters` (as the larva rows) or `below masters`? | `below <label>`. For milestones the weakest tier is often masters, and "top 100% masters" reads like praise for a plat time. The larva rows are not changed. | not percents for sure, it will be misread indeed. `somewhere below <label>` is clear, but too long. i cant figure what the text must be, but even more clear than `below <label>` |
| OQ-6 | Refine milestone times between samples (F-7)? | No, not in this epic. Optional E20-S08. | i did not understand that finding. aggreing with the default No. |
| OQ-7 | Show milestones on the single-replay page? | No (AC-13.3 stays). | yes actually, lets add that as it is macro milestone and will help the players (me) focus on faster droning. write a story here about that|

**Decisions (maintainer, 2026-11-27):** OQ-1 median; OQ-2 add 44 and 55 drones and 150 supply, keep the original four, no ranking where it is impractical (cells without a band read `n/a`, plus the one-sample rule in section 3); OQ-3 keep 80 drones and 200 supply; OQ-4 both ZvZ players; OQ-5 no percentage: the text is `slower than <label>`; OQ-6 no; OQ-7 yes: story E20-S09.

## 5. Stories

Rules for all stories:

- Java 7 source level: no lambdas, no streams.
- Compute in loops; show times only through `LarvaGameClock`. `LarvaTimeBasisGuardTest` must stay green.
- New logic goes into small classes with JUnit tests (`ant TEST`).
- Never run a headless tool while Scelight is running, and only one headless JVM at a time.
- Golden changes (`ant GOLDEN_MULTI_REGENERATE`) are reviewed by the maintainer before they are accepted.

Order: S01 → S02 → S03, then S04 → S05 → S06, then S07. S04 and S05 can start in parallel with S02 and S03.

### Phase 1 — Data and bands

#### E20-S01 — New milestones: 44 and 55 drones, 150 supply (OQ-2)

**Do:**

- Add `DRONES_44`, `DRONES_55` and `SUPPLY_150` to `LarvaMilestone`. Declaration order is the display order, the order in which a typical game reaches them (league medians): 44 drones, 55 drones, 100 supply, 66 drones, 150 supply, 80 drones, 200 supply.
- `LarvaAnalysisCheckRow` keeps checking 66 and 80 only; nothing else lists milestones by hand. Check with `grep -rn "DRONES_66\|LarvaMilestone.values" src test`.

**Done when:** `LarvaMilestoneCalculatorTest` covers 44 drones; `ant TEST` passes; the regenerated multi golden has a `44 drones` line (`queensMoved`: loop 13120, 9:45) and nothing else changed.

#### E20-S02 — Milestone dump CLI

**Why:** the band tool needs milestone loops for the whole corpus. The probe copies the calculator's rule; the repo tool must use it.

**Do:**

- New dev-only `LarvaMilestoneDumpCli`, modelled on `LarvaPhaseRateDumpCli`: same list format (`replay<TAB>player<TAB>cohort<TAB>matchup`, `${NAME}` expanded) plus an optional fifth column `role` (`primary` when absent, `opponent` for the S03 ZvZ lines), same bootstrap, `System.exit` at the end.
- CSV on stdout, one line per list line: `file,cohort,matchup,role,player,result,replayLoops,lastLiveLoop,<one column per LarvaMilestone, loop or empty>`. Loops come from `LarvaMilestoneCalculator.compute( report.getResourceSnapshotsByPlayerName().get( player ) )`.
- A replay that fails or has no stats for the player gives a `#` line, like the rate dump.

**Done when:** over `benchmark-data/tmp/bands-replays.tsv` the milestone columns equal `benchmark-data/exports/milestone-times.csv` (supply100, drones44, drones66, drones80, supply200) on all 1,620 lines.

#### E20-S03 — Milestone band tool (`ant MILESTONE_BANDS`)

**Do:**

- New `benchmark-data/tools/milestone_bands.py` (its own file, so `benchmark_bands.py` stays as it is), reusing the league code of `benchmark_bands.py` (`cohort_entries`, `league_of`, `tier_starts`, `MIN_SAMPLES`), with two commands:
  - `list`: the `benchmark_bands.py list` output with a fifth column `role`, plus, for every ladder ZvZ replay with `opponentMmr` and a known region, a line for the opponent with their own league and the role `opponent` (OQ-4). The band step uses those lines only in the ZvZ bucket.
  - `bands <csv>`: per milestone and bucket (ALL, ZVP, ZVT, ZVZ), lower is better, writes `larva-milestone-bands.properties` and `benchmark-data/exports/milestone-bands.md`. The report has the league table, per milestone and bucket the median with `k/n` per league, the tier starts as m:ss and loops, and the dropped leagues with the reason.
- `build.xml`: a target `MILESTONE_BANDS` (depends `COMPILE,REQUIRE_SCELIGHT_FOLDER,BENCHMARK_MMR_RANGES`) running `list`, `LarvaMilestoneDumpCli` into `benchmark-data/exports/milestone-times.csv`, and `bands`. `BENCHMARK_BANDS` stays as it is.

**Done when:**

- Without the opponent lines, the tier starts equal baseline section G for 44 drones, 100 supply, 66 drones, 80 drones and 200 supply.
- With them, the ZvZ medians equal F-6 and the All, ZvP and ZvT bands do not change.
- The command runs without an agent's judgment (no hand edits).

### Phase 2 — Module

#### E20-S04 — Median milestone cells, in loops (OQ-1)

**Do:**

- `LarvaAggregationPlayerRecord`: replace `milestoneDisplayMs` by `milestoneLoops` (`Map< LarvaMilestone, Integer >`). `LarvaAggregationRecordBuilder` stores the calculator's loops unconverted.
- `LarvaAggregator`: per bucket and milestone keep the reached loops (not sums) and compute the median (even count: mean of the two middle loops, half up).
- `LarvaAggregatedMilestoneCell`: `getMedianLoop()` in place of `getMeanMs()`. `k/n` unchanged.
- View model and dump format the loop with `LarvaGameClock`. The title becomes "Milestones (median time, reached / games)".
- `LarvaAnalysisCheckRow` reads the loops.

**Done when:**

- `LarvaAggregatorTest` covers an odd count, an even count, one game, and none reached.
- `ant TEST` passes; the regenerated golden changes only the milestone lines (one game: median = that game).

#### E20-S05 — Shared tier logic and the milestone rating formatter

**Do:**

- Move the tier logic out of `LarvaPhaseBenchmarkRatingFormatter` into a new `LarvaBenchmarkTiers`: parse one `label:start,...` entry with a direction, check order, resolve tier and top percent, format. `LarvaPhaseBenchmarkRatingFormatter` uses it; its output must not change.
- New `LarvaMilestoneBenchmarkRatingFormatter`: reads `larva-milestone-bands.properties` (keys `<LarvaMilestone>.<LarvaMatchupBucket>`, lower is better). `format( milestone, bucket, Integer medianLoop )` → `top p% <label>`, `slower than <label>` later than the weakest tier (OQ-5), `n/a` with no band or no value. A missing or broken file logs once and gives `n/a`, like the larva formatter.
- A fixture band file for tests (`test/resources/larva-milestone-bands-fixture.properties`), copied ahead of the generated one as the larva fixture is.

**Done when:**

- `LarvaPhaseBenchmarkRatingFormatterTest` passes unchanged.
- New tests cover: lower is better, tier boundaries, the best tier capped at top 5%, `slower than`, a missing key, a malformed file, and that the packaged file loads.

#### E20-S06 — Ranking rows in the milestone table

**Do:**

- `LarvaAggregatedMilestoneCell` gets the ranking text (computed in `LarvaAggregator`, as for the phase cells).
- `LarvaMultiAnalysisViewModelBuilder.milestoneTable`: after each milestone row, a row `<n> drones ranking` / `<n> supply ranking` with the ranking text, or `n/a`.
- `HELP_LINE`: "Ranking rows compare your averages (milestones: medians) with per-game benchmark bands; they sit closer to the middle than single games."
- Dump formatter prints the ranking per milestone line. Regenerate the golden; `LarvaMultiViewGoldenCheck` covers the new rows.
- Layout check at 1280 and 1920 px with `multi-page-state`: the table grows from 4 to 14 rows. Nothing may be clipped, and the rows of both players must stay aligned.

**Done when:**

- `ant TEST` and `ant TEST_GOLDEN_MULTI` pass.
- A screenshot of the multi page with two players is in `knowhow/epic20-milestone-rankings.png`.
- On the maintainer's own replays the ZvZ column shows `n/a` for 66 and 80 drones, as F-2 predicts.

### Phase 3 — Documentation and release

#### E20-S07 — Help, docs, release

**Do:**

- Help page (`help/larva-module-help.html`), Milestones bullet:
  - median time, `k/n`;
  - the ranking is per matchup, from league medians of games that reached the milestone;
  - `k/n` shows how often you get there, and the ranking ignores games that never got there;
  - `slower than <league>` means later than that league's tier;
  - All mixes your matchups, so a ZvZ-heavy player rates lower there.
- Help page caveat: fix "every 10 game seconds (about 7 seconds of real time)" to "every 160 game loops (7.1 s)", the game clock since 3.2.
- `docs/benchmarking/statistical-analysis.md`: a "Milestone bands" section (corpus, both ZvZ players, the rules, the files).
- Release 3.3 as in `RELEASING.md` (maintainer asked for it on 2026-11-27). 3.2 was never published, so the Changes line also lists the Epic 19 changes.

**Done when:** the help page and docs describe what the page shows, and the maintainer has approved and published the release.

### Phase 4 — Single-replay page (OQ-7)

#### E20-S09 — Milestones on the single-replay page

**Why:** a milestone is a macro target you can check right after a game ("44 drones by 5:00"). One game is exactly what the bands describe (league medians of single games), so the ranking needs no caveat about averages.

**Do:**

- `ReplaySummary` gets the matchup of each Zerg player of a two-player replay (`ReplaySummaryService`, same opponent rule as the aggregation: `LarvaAggregationRecordBuilder.findOneVsOneOpponentRace`, `LarvaGameKinds.matchupOf`).
- `LarvaTimelineModel` gets `LarvaPlayerMilestones` per player with a phase table (the calculator's loops and the matchup), built by `LarvaTimelineModelBuilder`.
- New `LarvaMilestoneTableViewModels`: one column per milestone, rows `Reached at` (`m:ss` or `n/a`) and `ranking (ZvP)` against the band of the game's matchup (`All` when the matchup is unknown, for example a 2v2).
- `LarvaTimelinePreviewComp` paints it (since E20-S13: under the player name) with the shared table painter, and counts its height in the preferred height.

**Done when:** unit tests cover the table (columns in game order, game clock times, matchup band, unknown matchup rated against All); the phase-table pixel references do not change (their fixture has no milestones); at 1280 px the table fits the page (screenshot `knowhow/epic20-single-replay-milestones.png`).

### Phase 5 — Goals and readability (maintainer, 2026-11-27, after the first 3.3 build)

#### E20-S10 — Leagues as goals: merged labels and one-tier paces

**Why:** the maintainer wants a league that reaches a milestone as early as the league above to get the credit, and every cell to name a goal. Before, a league dropped by the one-sample rule just disappeared (its times counted in the faster tier, unnamed), and a cell with only one league that had enough games read `n/a`.

**Decisions (maintainer):** "goal = league": labels name the merged leagues; a cell with one league is a pace; a time later than the weakest league reads `slower than <label>` without a time; a player who never reached it, and a cell with no league of 20 games (ZvZ 80 drones), stay `n/a`. Leagues dropped for fewer than 20 games get no tier: their times read `slower than` the weakest league that has data, which is the goal they already see.

**Do:**

- `milestone_bands.py`: a league dropped by the one-sample rule is named in the tier above (`plat/dia`, weakest first; tier starts unchanged). One kept tier: the band is that tier's pace, the slowest median of the leagues it names (`GM/pro:9120`, so the GM median counts as at pace).
- `LarvaBenchmarkTiers.parse(…, minTiers)`: milestone bands may have one tier (`hasScale()` false); the larva bands still need two.
- `LarvaMilestoneBenchmarkRatingFormatter`: one tier reads `<label> pace m:ss` (through `LarvaGameClock`) at or before the start, `slower than <label>` after it.

**Done when:** tests cover the pace, the slower text, a merged label and the one-tier parse; the regenerated bands keep every tier start; `ant TEST_GOLDEN_MULTI` changes only labels.

#### E20-S11 — Multi-page milestone table readability

**Do (maintainer's words):** "drones alive" was internal slang: rows read `44 drones made by:` (supply rows: `100 supply used at:`, E20-S12). Only the times are bold (not the `k/n`, not the rankings). The ranking rows are ditto rows, `"  ranking` with the mark under the milestone name, so time and ranking rows alternate.

- `LarvaTableViewModel`: optional bold length per cell (leading characters); `LarvaTablePainter` measures and paints it with the bold variant of the font. The other tables pass none and paint as before (pixel references unchanged).
- `LarvaMultiAnalysisViewModelBuilder`: `MILESTONE_RANKING_ROW_LABEL`, `milestoneRowLabel`, bold length = length of the `m:ss` text.

**Done when:** unit tests cover the labels, the bold lengths and that a bold cell paints more ink; a multi-page screenshot shows the result (`knowhow/epic20-milestone-rankings.png`).

#### E20-S12 — Goal times and bold leagues

**Decisions (maintainer, 2026-11-27):** a time later than every tier gives a time to aim at, `aim for m:ss` (where the weakest tier starts; for a pace cell the pace), with no league label; `slower than <label>` is gone. Pace cells keep `GM/pro pace 6:47`. Inside the bands the league label is bold, `top 50% **plat**`, in every ranking cell (milestones and larva gen / larva missed) on both pages; the larva `top 100% silver` of a value worse than the weakest band is not inside a band and stays plain. Supply rows read `100 supply used at:`.

**Do:**

- `LarvaRanking`: ranking text plus the position of its league label (none outside the bands). `LarvaBenchmarkTiers.rankTopPercent`, `LarvaPhaseBenchmarkRatingFormatter.rankSpawnedLarva` / `rankMissedLarva`, `LarvaMilestoneBenchmarkRatingFormatter.rank` / `rankLoop` return it; the old `format…` methods return its text, so the dumps and goldens read the same texts.
- The aggregated cells keep the `LarvaRanking` (`get…RankingValue()`); `LarvaTableViewModel` takes one bold part per cell (`Bold`, `boldLabel( ranking )`), replacing the S11 bold prefix; the painter paints text before, bold part, text after.
- The single-replay phase table's pixel references are regenerated once (bold labels, and the column widths they widen).

**Done when:** tests cover `aim for`, the marked label inside and its absence outside the bands, a bold part at the end of a cell; `ant TEST_GOLDEN_MULTI` changes only `rank=` texts.

#### E20-S13 — Single-replay layout: milestones first, bold larva summary

**Why (maintainer):** the milestone table was crammed between the phase table and the rails.

**Do:** per player: name, milestone table, the larva summary in bold, the phase table, the rails. The summary reads `x larva generated, y potential larva missed, z inject larva missed` for every player (before: ZvZ only had the generated count; the hatchery rows keep "potential injected larva missed"). Every gap, measured text to text, equals the one between the summary and the phase table: 19 px (name → milestone table, milestone table → summary, summary → phase table, phase table → first rail label; the last was about 1 px before).

- `LarvaTimelineModelBuilder.overviewLabel`; `LarvaTimelinePreviewComp`: order, bold summary, `TEXT_BASELINE_BELOW_TABLE` 27, `PHASE_TABLE_GAP` 24 (was 8), preferred height.
- Pixel references regenerated (bold summary, rails 16 px lower; the fixture has no milestone table).

**Done when:** `ant TEST` passes; on a real replay the four gaps measure the same (screenshot `knowhow/epic20-single-replay-milestones.png`).

### Optional

#### E20-S08 — Milestone time between samples (OQ-6)

Only if the maintainer wants finer rankings. For drones: after the last sample below the threshold, count drone `UnitBorn` events (and known drone losses and building starts) to find the loop where the count first reaches it. Exact when nothing died or morphed in between. For supply: linear interpolation of `foodUsed` between the two samples. Re-run E20-S03 afterwards: every band moves.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Below Diamond the tiers are 1–2 samples apart, so gold vs plat is noise. | The rules keep what the data supports; `k/n` stays next to it; help text (S07). |
| Survivorship: the ranking ignores games that never got there. | `k/n` in the same cell; F-3 in the docs. |
| Per-matchup cells near the 20-game limit: a tier can appear or vanish with a corpus change. | The band report lists `k/n` and the dropped leagues; re-run and review after corpus changes. |
| The pro tier is two players in tournament games. | Same as the larva bands; documented. |
| The corpus mixes seasons and patches, with one MMR table. | Same as the larva bands; documented. |
| Rating the player's median of few games is noisy. | `k/n` is shown; no minimum, as for the larva rows. |

## 7. Definition of done (epic)

- Each milestone cell shows the median time with `k/n`, and a ranking row shows `top p% <league>`, `<league> pace m:ss`, `aim for m:ss` or `n/a`, from bands built per milestone and matchup by `ant MILESTONE_BANDS`.
- The bands come from the corpus by a command, with a report that names every dropped league and why.
- 44 and 55 drones and 150 supply are milestones; the single-replay page shows the milestones of each Zerg player with a ranking.
- `ant TEST` and `ant TEST_GOLDEN_MULTI` pass, the help and docs are updated, and the release is published with the maintainer's OK.

## 8. Status (2026-11-27)

Implemented, S10 to S13 included. **3.3** is built, checked (`CHECK_RELEASE`), installed locally and committed in the Pages clone; the push needs the maintainer's GitHub login (the clone has no stored credential), then `ant SMOKE_CHECK_RELEASE`. `ant TEST` (419 tests), `ant TEST_GOLDEN_MULTI` and `ant TEST_GOLDEN_SCENARIO` pass. Nothing is committed in this repo (the maintainer commits).

| Story | Status | Notes |
|---|---|---|
| S01 new milestones | done | 44, 55 drones, 150 supply. Golden: `44 drones` at loop 13120 (9:45), only milestone lines changed. |
| S02 dump CLI | done | `LarvaMilestoneDumpCli`. Equal to the probe on all 1,620 primary and 383 opponent player-games. |
| S03 band tool | done | `benchmark-data/tools/milestone_bands.py` (separate file), `ant MILESTONE_BANDS` (45 s). All, ZvP and ZvT bands equal baseline section G for the original five; ZvZ medians equal F-6. Added the one-sample rule (section 3). Report: `benchmark-data/exports/milestone-bands.md`. |
| S04 median cells | done | `milestoneLoops`, `getMedianLoop()`, title "Milestones (median time, reached / games)". |
| S05 tiers, formatter | done | `LarvaBenchmarkTiers` shared; `LarvaPhaseBenchmarkRatingFormatterTest` unchanged and green; `LarvaMilestoneBenchmarkRatingFormatter` with `slower than`. |
| S06 ranking rows | done | 14 rows. Layout at 1280 and 1920 px: nothing clipped; with two players every table starts at the same y. Screenshot: `knowhow/epic20-milestone-rankings.png` (Reynor, 84 pro games). |
| S07 help, docs, release | push pending | Help page, `docs/benchmarking/statistical-analysis.md` "Milestone bands", version 3.3, Changes line, manifest texts; release committed in the Pages clone, not pushed yet. |
| S08 between samples | not started | Optional (OQ-6: no). |
| S09 single-replay page | done | Screenshot: `knowhow/epic20-single-replay-milestones.png`. |
| S10 goals: merged labels, paces | done | 13 cells name merged leagues; paces: ZvZ 66 drones `GM/pro` 6:47, ZvP 200 supply `pro` 10:50, ZvZ 200 supply `pro` 9:45. ZvZ 80 drones stays `n/a`. Golden: only labels changed. |
| S11 table readability | done | `made by:`, bold times, ditto ranking rows. Two ladder players at 1920 px: nothing clipped, page 1364 px wide (merged labels widen the All column). Screenshot updated. |
| S12 goal times, bold leagues | done | `aim for m:ss` replaces `slower than`; bold league inside the bands on both pages; `100 supply used at:`. Golden: only `rank=` texts; phase-table pixel references regenerated (bold labels only). |
| S13 single-replay layout | done | Name → milestones → bold summary → phase table → rails, every gap 19 px text to text (measured in Scelight). Summary `x larva generated, y potential larva missed, z inject larva missed` for every player. Pixel references regenerated. |

The probe output behind sections 1–2 is kept in `knowhow/epic20-milestone-spike/` (`*-probe.csv`); `benchmark-data/exports/milestone-times.csv` is now the repo CLI's output (other columns, ZvZ opponents included).

# Phase 5 statistical analysis

This document defines the Phase 5 offline benchmark-analysis contract for Epic 13.

## Goals

Phase 5 must convert the deterministic Phase 4 exports into benchmark-oriented tables.

Required outcomes:

- per-cohort descriptive statistics by matchup and phase,
- matchup-agnostic fallback tables,
- outlier visibility,
- cross-cohort trend checks,
- simple metric-vs-skill correlation review.

## Canonical tool

The canonical Phase 5 tool is:

- [benchmark-data/tools/benchmark_stats_tool.py](../../benchmark-data/tools/benchmark_stats_tool.py)

It reads the Phase 4 long-form CSV export:

- [benchmark-data/exports/phase-metrics.csv](../../benchmark-data/exports/phase-metrics.csv)

## Canonical outputs

Phase 5 writes:

- [benchmark-data/reports/benchmark-summary.json](../../benchmark-data/reports/benchmark-summary.json)
- [benchmark-data/reports/benchmark-summary.md](../../benchmark-data/reports/benchmark-summary.md)
- [benchmark-data/exports/benchmark-percentiles.json](../../benchmark-data/exports/benchmark-percentiles.json)
- [benchmark-data/exports/benchmark-percentiles.csv](../../benchmark-data/exports/benchmark-percentiles.csv)
- [benchmark-data/exports/benchmark-fallback-percentiles.json](../../benchmark-data/exports/benchmark-fallback-percentiles.json)
- [benchmark-data/exports/benchmark-fallback-percentiles.csv](../../benchmark-data/exports/benchmark-fallback-percentiles.csv)

## Statistics currently computed

For each `metricName × matchup × phase × cohortBucket` group:

- `sampleSize`
- `mean`
- `median`
- `p10`
- `p25`
- `p50`
- `p75`
- `p90`
- `stddev`
- `outlierRule`
- `qualityNote`

The default outlier review rule is IQR-based `1.5x` fencing.

## Trend checks

Trend checks currently compare adjacent cohort medians in bucket order:

- `mmr-2000`
- `mmr-3000`
- `mmr-4000`
- `mmr-5000`
- `elite-7000`

Expected direction:

- `larvaMissedPerHatchPerMinute` → lower is better
- `injectMissedLarvaPerHatchPerMinute` → lower is better
- `spawnedLarvaPerHatchPerMinute` → higher is better
- `injectUptimePct` → higher is better

Any median inversion is surfaced as a warning row, not silently ignored.

## Correlation review

The first Phase 5 implementation uses Pearson correlation between:

- cohort rank as an ordinal variable,
- the per-row metric value.

This is intentionally simple. It is sufficient for early validation and can later be replaced or supplemented by stronger statistical methods if needed.

## Productized phase benchmarks

The first in-product player ratings use the matchup-agnostic fallback means from the MMR-bucket corpus, not the per-matchup tables.

Rationale:

- the UI needs one stable reference table per metric and phase,
- the fallback means were less noisy than individual matchup slices,
- exact replay-MMR correlations were not strong enough to justify a finer-grained direct-MMR model,
- bucket-level cohort ordering was still directionally strong enough to ship an initial benchmark ladder.

The compact user-facing tiers are:

- `gold`
- `plat`
- `diamond`
- `masters`
- `pro`

### Selected benchmark anchors

These anchors come from the all-matchup fallback mean tables.

#### `spawnedLarvaPerHatchPerMinute`

Only `MID` and `LATE` are productized.

`EARLY` and `END` are intentionally omitted because those phases were less stable and less interpretable for player-facing feedback.

| phase | gold | plat | diamond | masters | pro 
| ---- | ----: | ----: | ----:  | ----: | ----:
| MID  | 4.119 | 4.638 | 5.555  | 5.906 | 6.023
| LATE | 3.214 | 3.824 | 4.233  | 4.646 | (5.000)

NB: late game is normal to drop the larva spending, as the Zerg is not necessary trading all the time. They sit on larva and bank and wait on battle to remax. 
Reason: waiting for quality trades.

#### `larvaMissedPerHatchPerMinute`

Only `EARLY`, `MID`, and `LATE` are productized.

`END` is intentionally omitted because the end-game sample shape was less stable for player-facing benchmarking.

| phase | gold | plat | diamond | masters | pro
| ----- | ----: | ----: | ----: |  ----: | ----:
| EARLY | 1.094 | 0.819 | 0.598 |  0.452 | 0.242
| MID   | 2.267 | 2.028 | 1.420 |  1.109 | 0.608
| LATE  | 2.572 | 2.255 | 1.807 |  1.595 | 0.891

## Player-facing rating method

The replay page shows two new rows below each player name:

- `larva gen ranking`
- `larva missed ranking`

Each row is rendered across the phase columns `Early`, `Mid`, `Late`, `End`.

Formatting rules:

- if no benchmark exists for that metric-phase pair, show `n/a`
- otherwise show `top X% tier`
- the UI uses the compact label `dia` for the `diamond` tier to keep the table narrow


### Tiers start values:

As the values above represent the averages per tier, the following table represent the the inferred starts of the tiers:

#### `spawnedLarvaPerHatchPerMinute`
| phase | gold | plat | diamond | masters | pro
| ----- | ----: | ----: | ----: |  ----: | ----:
MID		| 3.859 |4.378 | 5.096 | 5.730 | 5.964
LATE	| 2.909	|3.519 | 4.028 | 4.439 | 4.823	

#### `larvaMissedPerHatchPerMinute`
| phase | gold | plat | diamond | masters | pro
| ----- | ----: | ----: | ----: |  ----: | ----:
EARLY  | 1.2315 | 0.9565 | 0.708 | 0.525 | 0.347
MID	   | 2.3865 | 2.1475 | 1.724 | 1.266 | 0.859
LATE   | 2.7305 | 2.4135 | 2.031 | 1.701 | 1.243


Interpolation rules:

1. Treat the selected benchmark numbers as ordered tier anchors.
2. Determine the current tier by the interval containing the player value.
	- for `spawnedLarvaPerHatchPerMinute`, higher is better
	- for `larvaMissedPerHatchPerMinute`, lower is better
3. Interpolate progress inside that tier using the adjacent anchor distance.
4. Convert that progress into a compact percentile-inside-tier string.
5. Round the displayed percentile to the nearest `5%` for readability.

Interpretation examples:

- `top 80% plat` → barely into the plat band
- `top 40% plat` → solidly inside the plat band
- `top 15% plat` → close to the diamond anchor
- `top 5% masters` → better than the masters anchor by roughly one additional tier span
## Epic 19 update (phases on Workers Active)

Epic 19 moved the phase boundaries back to Scelight's Workers Active count (`PlayerStats.workersActiveCount`); the hand-built "drones alive" tally of April–November 2026 never subtracted killed drones. The tier starts in the tables above are history.

## Rate unit (since 2026-11-26)

Rates are per hatchery per game clock minute (60 s, 1344 loops). Before 2026-11-26 they were per 960 loops (a Normal-speed minute, 42.9 s), so every rate in the tables above, in the April 2026 exports and in the spreadsheet is 1/1.4 of today's value. See [knowhow/time-management.md](../../knowhow/time-management.md).

## League bands (since 2026-11-26)

The maintainer's spreadsheet (`benchmark-data/scelight mod - larva efficiency calculations.ods`, sheet "may 2026 v2.1") defines the method; `ant BENCHMARK_BANDS` (in `LarvaSecondsLostExtMod`) runs it as one command, without an agent:

1. **League per replay.** MMR-bucket corpus: the Zerg player's ladder MMR, read from the replay, against the 1v1 league limits of the player's region (toon `1-…` Americas, `2-…` Europe, `3-…` Korea; other regions are skipped) from [MMR-Ranges](https://burnysc2.github.io/MMR-Ranges/). An MMR above Master 1 counts as GrandMaster. Elite corpus (tournament replays of named pros, no MMR): `pro`. The MMR table is a snapshot, `benchmark-data/mmr-ranges/mmr-ranges-1v1.json`, of one season (the site shows only the current one), used for replays of all seasons; `-Dmmr.refresh=true` downloads the current table first.
2. **League median** of each rate per phase (the spreadsheet's "average" column holds medians; means are pulled by short phases with few hatcheries and turned early larva generated upside down).
3. **Tier start halfway** between a league's median and the median of the league below; the weakest tier starts half a step below its own median (spreadsheet formula `B3-(C3-B3)/2`).
4. **Rules instead of judgment:** a league needs 20 replays with a rate in the phase; walking down from pro, a league whose median is not worse than the next kept league above it is dropped for that phase; a phase with fewer than two tiers is not rated (n/a).

Outputs: the band file `LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/larva-benchmark-bands.properties` (read by `LarvaPhaseBenchmarkRatingFormatter`; labels `bronze`, `silver`, `gold`, `plat`, `dia`, `masters`, `GM`, `pro`), the report [benchmark-bands.md](../../benchmark-data/exports/benchmark-bands.md) (league limits, medians with counts, tier starts, dropped leagues) and the per-replay rates [phase-rates.csv](../../benchmark-data/exports/phase-rates.csv). After a run: `ant TEST` (the unit tests use fixed fixture bands, so they do not change), `ant GOLDEN_MULTI_REGENERATE` (the multi golden shows rankings), then review and commit the diff.

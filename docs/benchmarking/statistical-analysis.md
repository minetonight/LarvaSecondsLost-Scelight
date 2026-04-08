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

### Selected benchmark anchors

These anchors come from the all-matchup fallback mean tables.

#### `spawnedLarvaPerHatchPerMinute`

Only `MID` and `LATE` are productized.

`EARLY` and `END` are intentionally omitted because those phases were less stable and less interpretable for player-facing feedback.

| phase | gold | plat | diamond | masters |
| --- | ---: | ---: | ---: | ---: |
| MID | 4.119 | 4.638 | 5.555 | 5.906 |
| LATE | 3.214 | 3.824 | 4.233 | 4.646 |

#### `larvaMissedPerHatchPerMinute`

Only `EARLY`, `MID`, and `LATE` are productized.

`END` is intentionally omitted because the end-game sample shape was less stable for player-facing benchmarking.

| phase | gold | plat | diamond | masters |
| --- | ---: | ---: | ---: | ---: |
| EARLY | 1.094 | 0.819 | 0.598 | 0.452 |
| MID | 2.267 | 2.028 | 1.420 | 1.109 |
| LATE | 2.572 | 2.255 | 1.807 | 1.595 |

## Player-facing rating method

The replay page shows two new rows below each player name:

- `larva gen ranking`
- `larva missed ranking`

Each row is rendered across the phase columns `Early`, `Mid`, `Late`, `End`.

Formatting rules:

- if no benchmark exists for that metric-phase pair, show `n/a`
- otherwise show `top X% tier`
- the UI uses the compact label `dia` for the `diamond` tier to keep the table narrow

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
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
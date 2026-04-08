# Benchmark exports

This folder stores derived Phase 4+ benchmark artifacts.

## Phase 4 outputs

- `replays/` — one resumable sidecar JSON file per accepted replay hash
- `phase-metrics.json` — nested replay-oriented export for auditability
- `phase-metrics.csv` — long-form table for later statistical analysis

## Phase 5 outputs

- `benchmark-percentiles.json` — matchup-specific percentile tables
- `benchmark-percentiles.csv` — matchup-specific percentile tables in flat form
- `benchmark-fallback-percentiles.json` — matchup-agnostic fallback tables
- `benchmark-fallback-percentiles.csv` — matchup-agnostic fallback tables in flat form

These files are derived from the canonical corpus manifest plus the replay-analysis extractor contract documented in [docs/benchmarking/batch-extraction.md](../../docs/benchmarking/batch-extraction.md).
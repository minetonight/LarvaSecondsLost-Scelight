# Benchmark data workspace

This folder holds the offline benchmark-research workspace for Epic 13.

## Layout

- `manifests/` — tracked corpus metadata, schemas, and canonical manifest files
- `tools/` — offline helper scripts for validation, deduplication, and coverage reporting
- `reports/` — tracked or reviewable benchmark summaries and corpus coverage outputs
- `exports/` — derived benchmark tables and intermediate machine-readable outputs
- `raw-replays/` — local raw replay storage, ignored by git

## Ground rules

- raw replay archives stay out of normal version control
- tracked files should remain small, reviewable, and reproducible
- all accepted corpus decisions should flow through the manifest tooling

## Primary manifest

The canonical corpus manifest is:

- [benchmark-data/manifests/replay-corpus.json](manifests/replay-corpus.json)

Its schema is:

- [benchmark-data/manifests/replay-corpus.schema.json](manifests/replay-corpus.schema.json)

## Primary tool

Use the offline corpus tool at:

- [benchmark-data/tools/benchmark_corpus_tool.py](tools/benchmark_corpus_tool.py)

Key commands:

- `validate` — validate manifest structure and semantic consistency
- `dedupe` — normalize lists and collapse duplicate replay hashes deterministically
- `coverage` — generate cohort × matchup coverage summaries in JSON and Markdown
- `import-seeded-downloads` — promote seeded replay downloads into canonical manifest entries

## Phase 4 batch runner

Use the Phase 4 batch runner at:

- [benchmark-data/tools/benchmark_batch_runner.py](tools/benchmark_batch_runner.py)

It is responsible for:

- resumable accepted-replay processing,
- per-replay sidecar exports under `exports/replays/`,
- long-form `phase-metrics.csv`,
- nested `phase-metrics.json`,
- batch summary and failure reports.

For the Java-backed extractor adapter used by the batch runner, see:

- [benchmark-data/tools/benchmark_java_extractor.py](tools/benchmark_java_extractor.py)

## Phase 5 statistical analysis

Use the Phase 5 statistics tool at:

- [benchmark-data/tools/benchmark_stats_tool.py](tools/benchmark_stats_tool.py)

It converts long-form replay-phase exports into benchmark percentile tables, trend checks, and review summaries.

## Internet replay sourcing

Use the internet sourcing tool at:

- [benchmark-data/tools/internet_replay_source_tool.py](tools/internet_replay_source_tool.py)

It supports:

- discovery of public SpawningTool replay downloads,
- discovery of SpawningTool replay-pack links,
- direct replay downloads,
- bulk ZIP downloads and optional replay extraction,
- categorized seeded downloads under source / cohort / player / matchup folders.

Current categorized replay storage uses a layout like:

- `raw-replays/categorized/SpawningTool_filtered_replay_browser/elite-7000/Serral/ZvT/elite-serral-zvt/`

The researched source catalog lives at:

- [benchmark-data/manifests/replay-source-catalog.json](manifests/replay-source-catalog.json)

The initial tracked source seeds live at:

- [benchmark-data/manifests/replay-source-seeds.json](manifests/replay-source-seeds.json)

See [docs/benchmarking/batch-extraction.md](../docs/benchmarking/batch-extraction.md) for the extraction protocol and output contract.

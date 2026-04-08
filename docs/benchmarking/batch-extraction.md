# Phase 4 batch extraction

This document defines the Phase 4 offline batch runner contract for Epic 13.

## Goals

Phase 4 must:

- run accepted replay analysis without aborting the whole corpus on one failure,
- resume from existing successful per-replay outputs,
- regenerate deterministic long-form and nested exports,
- leave enough diagnostics behind to triage failures later.

## Batch runner

The canonical Phase 4 runner is:

- [benchmark-data/tools/benchmark_batch_runner.py](../../benchmark-data/tools/benchmark_batch_runner.py)

Its responsibilities are:

- read and validate [benchmark-data/manifests/replay-corpus.json](../../benchmark-data/manifests/replay-corpus.json),
- iterate accepted replay entries in deterministic order,
- reuse previously generated replay sidecars when `--resume` is enabled,
- optionally invoke an external extractor command for missing sidecars,
- regenerate aggregated CSV and JSON exports from successful replay sidecars,
- write summary and failure reports.

## Output files

Phase 4 produces these canonical outputs:

- [benchmark-data/exports/phase-metrics.json](../../benchmark-data/exports/phase-metrics.json)
- [benchmark-data/exports/phase-metrics.csv](../../benchmark-data/exports/phase-metrics.csv)
- [benchmark-data/reports/batch-run-summary.json](../../benchmark-data/reports/batch-run-summary.json)
- [benchmark-data/reports/batch-run-summary.md](../../benchmark-data/reports/batch-run-summary.md)
- [benchmark-data/reports/failed-replays.jsonl](../../benchmark-data/reports/failed-replays.jsonl)
- one sidecar JSON per replay hash under `benchmark-data/exports/replays/`

## Resume model

Resume behavior is intentionally simple and auditable:

1. one accepted replay maps to one sidecar file named `{replayHash}.json`,
2. if the sidecar exists and validates, the runner reuses it,
3. if the sidecar is missing or invalid, the runner re-extracts only that replay,
4. aggregated CSV / JSON outputs are always regenerated from the currently valid sidecars.

This keeps reruns deterministic while avoiding a large opaque checkpoint file.

## Extractor command protocol

The batch runner can call an external extractor command with `--extract-command`.

Protocol:

- input: one JSON object on stdin per replay
- output: one JSON object on stdout per replay
- failure: non-zero exit code and a helpful stderr message

### Request shape

The runner sends a JSON object with at least:

- `protocolVersion`
- `runId`
- `manifestEntry`
- `outputPath`

`manifestEntry` is the accepted replay entry from the canonical corpus manifest.

### Expected response shape

The extractor must return a JSON object equivalent to the Phase 2 headless replay export model:

- replay-level metadata fields such as `replaySha256`, `mapTitle`, `replayLengthLoops`, and `baseBuild`
- replay-level diagnostics in `diagnosticFlagList`
- `playerRecordList`
- per-player `phaseRecordList`

The field names are intentionally aligned with the Java DTOs introduced in Phase 2:

- `LarvaBenchmarkReplayRecord`
- `LarvaBenchmarkPlayerRecord`
- `LarvaBenchmarkPhaseRecord`

## Phase 4.5 Java extractor adapter

Phase 4.5 adds a standalone extractor path that reuses the same Java analysis engine:

- [LarvaBenchmarkCliExtractor.java](../../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaBenchmarkCliExtractor.java)
- [LarvaBenchmarkJsonSerializer.java](../../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaBenchmarkJsonSerializer.java)
- [benchmark-data/tools/benchmark_java_extractor.py](../../benchmark-data/tools/benchmark_java_extractor.py)

Design notes:

- `ReplaySummaryService` now supports standalone dependency injection,
- the CLI extractor emits the DTO shape as JSON,
- reflection bridges are used so the module still compiles only against the external-module API,
- the Python wrapper handles the batch runner's stdin JSON protocol and forwards replay-path arguments to Java.

Recommended `--extract-command` shape:

- `benchmark-data/tools/benchmark_java_extractor.py --classpath <runtime-classpath>`

The runtime classpath must contain at least:

- `LarvaSecondsLostExtMod/bin`,
- the Scelight external-module API jar,
- Scelight runtime classes that provide `hu.scelight.sc2.rep.factory.RepParserEngine` and `hu.scelight.sc2.rep.repproc.SelectionTracker`.

## Long-form CSV contract

The runner writes one row per replay × analyzed player × gameplay phase.

Stable columns are aligned with [docs/benchmarking/data-dictionary.md](data-dictionary.md#long-form-phase-export-columns).

The Phase 4 runner currently writes at least:

- identity and grouping columns,
- timing and normalization columns,
- four benchmark metric columns,
- `qualityFlags`,
- `analysisVersion`,
- `extractionRunId`.

## Failure logging

Each failed replay is written to `failed-replays.jsonl` with:

- replay hash,
- local file path,
- failure stage,
- structured error type,
- human-readable message,
- extractor command when relevant.

This is enough to retry selectively or inspect broken corpus entries later.
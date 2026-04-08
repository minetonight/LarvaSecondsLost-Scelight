# Epic 13 benchmark data dictionary

## Purpose

This document defines the Phase 1 field contract for Epic 13 manifests and later benchmark exports.

It is the authoritative glossary for:

- corpus-manifest fields,
- cohort enums,
- rejection reasons,
- quality flags,
- long-form phase export columns.

## Enum values

### `matchup`

Allowed values:

- `ZvT`
- `ZvP`
- `ZvZ`

Definition:

- matchup from the analyzed Zerg player's perspective.

### `cohortBucket`

Allowed values:

- `mmr-2000`
- `mmr-3000`
- `mmr-4000`
- `mmr-5000`
- `elite-7000`

### `cohortConfidence`

Allowed values:

- `confirmed`
- `strong-inference`
- `weak-inference`

### `acceptanceState`

Allowed values:

- `discovered`
- `accepted`
- `rejected`

### `rejectionReason`

Allowed values:

- `duplicate-replay`
- `corrupt-replay`
- `unsupported-matchup`
- `missing-zerg-target`
- `missing-provenance`
- `analysis-failed`
- `insufficient-skill-evidence`
- `out-of-cohort-scope`
- `manual-review-rejected`

### `qualityFlags`

Allowed values:

Soft flags:

- `sparse-resource-snapshots`
- `partial-tooltip-context`
- `ambiguous-larva-pressure`
- `limited-inject-evidence`
- `elite-cohort-balance-pressure`

Hard flags:

- `analysis-failed`
- `corrupt-replay`
- `unsupported-matchup`
- `missing-zerg-target`
- `missing-provenance`
- `duplicate-replay`

## Corpus manifest fields

The canonical Phase 1 manifest file shape is defined in [benchmark-data/manifests/replay-corpus.schema.json](../../benchmark-data/manifests/replay-corpus.schema.json).

## Root manifest object

### `schemaVersion`

Type:

- string

Meaning:

- schema identifier for manifest compatibility.

### `generatedAt`

Type:

- string, ISO 8601 datetime

Meaning:

- last materialization time of the manifest document.

### `sourceReplayList`

Type:

- array of replay source entries

Meaning:

- discovered replay candidates before acceptance filtering.

### `acceptedReplayList`

Type:

- array of accepted replay entries

Meaning:

- canonical deduplicated corpus used for batch analysis.

### `rejectedReplayList`

Type:

- array of rejected replay entries

Meaning:

- discovered or reviewed replays that are excluded, with reasons.

## Shared replay identity fields

These fields appear on discovered, accepted, or rejected entries unless stated otherwise.

### `replayHash`

Type:

- string

Meaning:

- SHA-256 of the replay file.

### `filePath`

Type:

- string

Meaning:

- local canonical path used in the research corpus.

### `originalFileName`

Type:

- string

Meaning:

- filename as acquired from the source.

### `sourceUrlList`

Type:

- array of strings

Meaning:

- one or more original public URLs or exact source locations.

### `sourceLabel`

Type:

- string

Meaning:

- human-readable source label such as site name, archive pack name, or `user-supplied`.

### `acquiredAt`

Type:

- string, ISO 8601 datetime

Meaning:

- first acquisition timestamp.

## Replay classification fields

### `primaryZergPlayer`

Type:

- string

Meaning:

- the Zerg player whose larva metrics will be benchmarked.

### `opponentPlayer`

Type:

- string

Meaning:

- opposing player label when available.

### `matchup`

Type:

- enum string

Meaning:

- matchup from the Zerg player's perspective.

### `cohortBucket`

Type:

- enum string

Meaning:

- benchmark bucket assigned to the replay.

### `cohortConfidence`

Type:

- enum string

Meaning:

- confidence in the bucket labeling.

### `cohortEvidence`

Type:

- string

Meaning:

- short explanation of why the cohort label is believed.

### `acceptanceState`

Type:

- enum string

Meaning:

- current decision state.

### `rejectionReason`

Type:

- enum string or null

Meaning:

- explicit exclusion reason for rejected entries.

### `qualityFlags`

Type:

- array of enum strings

Meaning:

- warnings or hard-failure markers attached to the replay record.

### `notes`

Type:

- string

Meaning:

- freeform audit note for manual decisions or unusual conditions.

## Long-form phase export columns

These columns define the recommended Phase 2+ batch export row.

Each row represents one replay × one analyzed player × one gameplay phase.

### Identity and grouping columns

- `replayHash`
- `primaryZergPlayer`
- `matchup`
- `cohortBucket`
- `cohortConfidence`
- `phase`
- `mapName`
- `replayBuild`

### Timing and normalization columns

- `replayDurationLoops`
- `phaseStartLoop`
- `phaseEndLoop`
- `phaseDurationLoops`
- `hatchEligibleLoops`
- `injectEligibleLoops`
- `injectActiveLoops`

### Metric columns

- `larvaMissedCount`
- `injectMissedLarvaCount`
- `spawnedLarvaCount`
- `larvaMissedPerHatchPerMinute`
- `injectMissedLarvaPerHatchPerMinute`
- `spawnedLarvaPerHatchPerMinute`
- `injectUptimePct`

### Diagnostics columns

- `qualityFlags`
- `analysisVersion`
- `extractionRunId`

## Aggregated benchmark table columns

Phase 5 aggregated outputs should include at least:

- `metricName`
- `matchup`
- `phase`
- `cohortBucket`
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

## Notes on nullability

### `injectUptimePct`

May be null when the phase has no inject-eligible loops.

### `rejectionReason`

Must be null for accepted entries.

### `sourceUrlList`

May be empty only if `sourceLabel` and explanatory `notes` exist.

## Phase 1 stability guarantee

Phase 1 considers the following stable enough to build tooling against:

- replay manifest root shape,
- cohort enums,
- rejection reasons,
- quality-flag names,
- long-form phase export column names.

New fields may be added later, but these names should not be changed casually because later scripts and reports will depend on them.

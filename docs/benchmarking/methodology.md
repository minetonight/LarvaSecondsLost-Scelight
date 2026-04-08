# Epic 13 benchmark methodology

## Purpose

This document defines the Phase 1 research contract for Epic 13.

It answers four questions before large-scale replay collection starts:

1. which metrics are benchmarked,
2. how cohorts are defined,
3. how replay quality and provenance are governed,
4. what minimum standards must be met before a benchmark table is published.

The goal is to keep the benchmark pipeline auditable and consistent with the supported `Larva` page.

## Core principles

### One metric engine

Benchmark extraction must reuse the same larva-analysis logic that powers the module-owned `Larva` page.

Rules:

- no second scoring engine for offline statistics,
- no alternate timing model,
- no spreadsheet-only formulas that diverge from module calculations.

### Loop-authoritative timing

Per [knowhow/time-management.md](../../knowhow/time-management.md), replay loops remain the source of truth.

Rules:

- phase boundaries remain loop-derived,
- larva missed remains loop-derived,
- inject uptime remains loop-derived,
- any display-time formatting happens only in exports intended for humans.

### Reproducibility over convenience

Every accepted replay must be traceable to:

- a content hash,
- an acquisition source,
- a cohort assignment method,
- an acceptance or rejection decision.

## Benchmark scope

## Metrics in scope for the first benchmark pass

The benchmark pipeline will compute the following Epic 12 metrics for each analyzed Zerg player and each gameplay phase.

### 1. `larvaMissedPerHatchPerMinute`

Definition:

$$
\text{missed-larva rate} = \frac{\text{missed larva count} \times \text{loops per minute}}{\text{hatch-eligible loops}}
$$

Interpretation:

- lower is better.

### 2. `injectMissedLarvaPerHatchPerMinute`

Definition:

$$
\text{inject-missed rate} = \frac{\text{potential injected larva missed} \times \text{loops per minute}}{\text{hatch-eligible loops}}
$$

Interpretation:

- lower is better.

### 3. `spawnedLarvaPerHatchPerMinute`

Definition:

$$
\text{spawned-larva rate} = \frac{\text{spawned larva count} \times \text{loops per minute}}{\text{hatch-eligible loops}}
$$

Interpretation:

- higher is usually better,
- but it is partly game-state sensitive and must not be treated as a pure skill signal without validation.

### 4. `injectUptimePct`

Definition:

$$
\text{inject uptime} = \frac{\text{inject-active loops}}{\text{inject-eligible loops}} \times 100\%
$$

Interpretation:

- higher is better,
- `n/a` remains valid when inject eligibility never started in that phase.

## Supporting fields collected with every metric row

The benchmark exports must also preserve context fields that are needed for grouping or filtering:

- `phase`
- `matchup`
- `mapName`
- `replayBuild`
- `replayDurationLoops`
- `phaseDurationLoops`
- `hatchEligibleLoops`
- `injectEligibleLoops`
- `qualityFlags`
- `cohortBucket`
- `cohortConfidence`

## Cohort contract

## Matchup segmentation

Every replay is classified first by matchup from the analyzed Zerg player's perspective:

- `ZvT`
- `ZvP`
- `ZvZ`

No benchmark table may merge matchups silently.

If a combined table is later published, it must be labeled explicitly as cross-matchup.

## Skill buckets

Phase 1 locks the following initial cohort buckets:

- `mmr-2000`
- `mmr-3000`
- `mmr-4000`
- `mmr-5000`
- `elite-7000`

These are benchmark labels, not promises of exact hidden MMR precision.

## 7000-cohort decision

Phase 1 adopts this decision:

- the 7000 target is a combined elite cohort containing Serral and Reynor replay samples,
- per matchup bucket, no single named player should contribute more than `60%` of accepted replays when enough supply exists,
- if the elite pool stays unbalanced, reports must show the player composition.

This keeps the first corpus practical while avoiding a silent single-player benchmark.

## Cohort confidence levels

Every accepted replay must carry one of these confidence levels:

- `confirmed` — player or skill bucket is directly evidenced by trusted source metadata,
- `strong-inference` — label is inferred from strong public context with low ambiguity,
- `weak-inference` — label is approximate and should be excluded from final benchmark publication unless sample shortages force review.

Publication rule:

- final benchmark tables should use only `confirmed` and `strong-inference` rows unless explicitly marked otherwise.

## Replay acceptance rules

A replay is accepted into the benchmark corpus only if all of the following are true.

### Technical validity

- file opens successfully,
- replay is not corrupted or truncated,
- replay contains enough detail for the current analysis pipeline,
- the analyzed player is Zerg,
- matchup is one of `ZvT`, `ZvP`, or `ZvZ`.

### Provenance validity

- acquisition source is recorded,
- content hash is recorded,
- local file path or canonical corpus location is recorded,
- cohort bucket and confidence are recorded.

### Analysis validity

- Epic 12 phase metrics can be extracted,
- quality flags do not indicate a hard failure,
- the replay is not a duplicate of an already accepted replay with the same content hash.

## Replay rejection rules

A replay must be rejected if any of the following apply:

- duplicate content hash already accepted,
- corrupt or unreadable file,
- unsupported matchup,
- no analyzable Zerg player for the target cohort,
- missing provenance,
- failed metric extraction,
- analyst-reviewed outlier caused by clearly broken replay data.

Rejections are not deleted. They must be preserved in the rejected manifest with a machine-readable reason.

## Deduplication policy

Deduplication is content-based.

Rules:

- primary key is SHA-256 of the replay file,
- filename alone never decides uniqueness,
- multiple source URLs may point to the same accepted replay,
- if duplicates differ only by filename, keep one canonical entry and attach all known sources to it.

## Quality-flag policy

Accepted replays may still carry warnings.

### Soft quality flags

Soft flags do not automatically reject a replay but must remain visible in exports:

- `sparse-resource-snapshots`
- `partial-tooltip-context`
- `ambiguous-larva-pressure`
- `limited-inject-evidence`
- `elite-cohort-balance-pressure`

### Hard quality flags

Hard flags force rejection unless manually overridden and documented:

- `analysis-failed`
- `corrupt-replay`
- `unsupported-matchup`
- `missing-zerg-target`
- `missing-provenance`
- `duplicate-replay`

## Publication thresholds

Phase 1 defines these minimum publication rules.

### Cohort target

Desired target:

- `100` accepted replays per cohort × matchup bucket.

### Minimum publishable sample size

A benchmark table may be published only if it has at least:

- `50` accepted replay rows for the cohort × matchup slice,
- and at least `20` usable phase rows for the specific phase statistic being shown.

If fewer rows exist:

- the slice remains exploratory only,
- reports must label it as insufficient for benchmark publication.

## Statistical outputs required later

When Phase 5 runs, each benchmark table must include at least:

- sample size,
- mean,
- median,
- `p10`, `p25`, `p50`, `p75`, `p90`,
- outlier policy note,
- cohort confidence note.

## Spending Quotient research rules

Epic 13 includes a methodology review of Spending Quotient.

Phase 1 locks the evaluation questions but not the answer.

The review must answer:

1. what SQ normalizes against,
2. how SQ turns raw values into interpretable ranges,
3. how SQ handles game-state differences,
4. which parts can transfer to larva metrics,
5. which parts must be rejected.

Interim decision:

- Epic 13 will not assume a composite larva score in Phase 1,
- the first benchmark deliverable is percentile tables for the existing metrics.

## Deliverables expected from Phase 1

Phase 1 is complete when these artifacts exist and agree with each other:

- [docs/benchmarking/methodology.md](methodology.md)
- [docs/benchmarking/replay-sourcing.md](replay-sourcing.md)
- [docs/benchmarking/data-dictionary.md](data-dictionary.md)
- [benchmark-data/manifests/replay-corpus.schema.json](../../benchmark-data/manifests/replay-corpus.schema.json)
- [benchmark-data/manifests/corpus-manifest.template.json](../../benchmark-data/manifests/corpus-manifest.template.json)

## Open questions intentionally left for later phases

Phase 1 does not yet decide:

- whether final product UX should use percentiles, named tiers, or both,
- whether a composite score will ever outperform individual metrics,
- whether lower-MMR cohorts need wider bins after real corpus availability is measured.

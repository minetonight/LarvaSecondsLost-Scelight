# Epic 13 — benchmark definition and replay-corpus analysis plan

## Executive summary

Define statistically grounded benchmark ranges for the Larva Seconds Lost metrics by analyzing a large replay corpus across skill cohorts and matchups.

This epic should stay aligned with the current architecture:

- the authoritative larva / inject metrics continue to come from the existing replay-analysis pipeline in the module
- timing remains loop-based per [knowhow/time-management.md](../knowhow/time-management.md)
- batch processing must reuse the same analysis logic that powers the supported `Larva` page instead of re-implementing the metric rules in a second engine
- replay acquisition, provenance, deduplication, and aggregation should live in an offline research workflow, not in the shipped UI path

The goal is not just to compute averages. The goal is to produce benchmark bands that are robust enough to answer questions like:

- what does strong early-game larva management look like at 2000, 3000, 4000, 5000, and 7000 MMR?
- which phase metrics correlate most strongly with player skill?
- should the module surface one headline score, multiple benchmarked sub-scores, or percentile-style ranges?
- can ideas from Spending Quotient be adapted without oversimplifying larva mechanics?

## Goal

Create a repeatable benchmark pipeline that:

1. acquires a large labeled replay corpus,
2. batch-extracts the existing larva-phase metrics,
3. studies skill correlation by matchup and game phase,
4. defines benchmark ranges per skill cohort,
5. produces artifacts that can later be consumed by the module UI or exported reports.

## Non-goals

This epic should **not** try to finish every future UX integration.

Out of scope for the first pass:

- adding a final benchmark UI to the module-owned `Larva` page
- shipping raw replay archives inside this repository
- inventing a brand-new larva metric engine separate from the existing analyzer
- unsupported integration with Scelight internals beyond the already supported external-module path
- irreversible conclusions from tiny samples or poorly sourced ladder data

## Requested corpus targets

The requested corpus shape is:

- 7000 MMR cohort: Serral and Reynor replays
  - `100` ZvT
  - `100` ZvP
  - `100` ZvZ
- 5000 MMR cohort
  - `100` ZvT
  - `100` ZvP
  - `100` ZvZ
- 4000 MMR cohort
  - `100` ZvT
  - `100` ZvP
  - `100` ZvZ
- 3000 MMR cohort
  - `100` ZvT
  - `100` ZvP
  - `100` ZvZ
- 2000 MMR cohort
  - `100` ZvT
  - `100` ZvP
  - `100` ZvZ

That implies a nominal minimum of `1500` accepted replays if the 7000 cohort is treated as one combined pool.

## First clarification to lock down during implementation

The 7000-MMR request is slightly ambiguous:

- interpretation A: `100` per matchup combined across Serral and Reynor
- interpretation B: `100` per matchup for Serral and `100` per matchup for Reynor

Recommended default for planning:

- treat the 7000 cohort as a combined elite cohort with per-player balancing where possible
- cap any single player's share of a matchup bucket so one player does not dominate the benchmark

If enough public replays exist later, the corpus can be expanded and analyzed both ways.

## Why this epic matters

Epic 12 already gives per-phase rates on the supported `Larva` page. Epic 13 turns those raw rates into player-facing meaning.

Without benchmarks, values like `3.2 larva missed / hatch / min` or `58% inject uptime` are informative only to expert users.
With benchmarks, the module can later say whether that number is weak, average, strong, or elite for the same matchup and phase.

## Existing architecture to build on

Relevant current components:

- [LarvaReplayAnalyzer.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java) already produces replay-derived larva, inject, hatchery, and per-phase metrics
- [LarvaAnalysisReport.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java) is the immutable analysis payload
- [LarvaPhaseStats.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaPhaseStats.java) already defines phase-level metric blocks
- [LarvaPlayerPhaseTable.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaPlayerPhaseTable.java) already groups phase metrics per player
- [LarvaTimelineGoldenFormatter.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineGoldenFormatter.java) demonstrates the existing deterministic, diff-friendly validation style
- [README.md](../LarvaSecondsLostExtMod/README.md) documents the supported module scope, packaging flow, and current analysis caveats

These are important because the benchmark system should reuse them instead of creating a disconnected research-only metric definition.

## Key design principle

**One metric engine, two consumption modes.**

The same analyzer should support:

1. interactive single-replay use on the module-owned `Larva` page
2. offline batch replay analysis for benchmark generation

If benchmark calculations diverge from the UI calculations, the resulting ranges will be hard to trust.

## Benchmarking questions to answer

### 1. Which metrics deserve benchmarks?

Minimum candidate set, directly derived from Epic 12 outputs:

- larva missed per hatch per minute
- inject-missed larva per hatch per minute
- total larva spawned per hatch per minute
- inject uptime percentage

Recommended additional candidate fields for offline analysis:

- phase span duration
- hatch-count normalization inputs
- matchup
- map name
- replay duration
- league / MMR bucket confidence
- replay patch / build
- sample-quality flags such as sparse snapshots, ambiguous larva pressure, or incomplete inject evidence

### 2. Should there be a single composite score?

Research task:

- study how Spending Quotient was defined, normalized, and validated
- identify which parts are portable: normalization philosophy, anti-inflation handling, outlier treatment, or matchup segmentation
- explicitly reject anything that depends on economics assumptions that do not transfer to larva mechanics

Recommended outcome:

- do not assume a single composite score up front
- first validate whether one or more individual metrics correlate more cleanly with skill than any forced composite

### 3. How should benchmark ranges be expressed?

Candidates:

- mean and standard deviation bands
- percentile bands such as bottom 25%, middle 50%, top 25%
- named tiers such as weak / average / strong / elite
- matchup-specific percentile cutoffs
- phase-specific reference values

Recommended first deliverable:

- compute percentile tables first
- derive named labels only after the distributions are stable

Percentiles are more robust than early attempts at hard-coded score adjectives.

## Constraints and safeguards

### Replay sourcing

Replay acquisition must use publicly available or user-permitted sources only.

Requirements:

- record the source URL or acquisition source for every replay
- do not commit large replay archives to git
- store only manifests, hashes, and derived statistics in the repository unless the user explicitly wants local fixtures checked in
- preserve provenance so bad samples can be removed later

### Deduplication

The same replay can appear on multiple sites or under renamed files.

Requirements:

- deduplicate by strong content hash, not just filename
- preserve a source manifest listing all discovered origins for traceability
- reject corrupted or truncated replays before analysis

### Skill labeling

MMR / skill labeling will be noisy outside elite named-player cohorts.

Requirements:

- store bucket confidence for each replay
- prefer explicit ladder metadata when available
- otherwise treat skill label as inferred and record the inference method
- separate exact known-player cohorts from approximate ladder buckets in downstream statistics

### Statistical integrity

Requirements:

- track sample size per cohort, matchup, and phase
- refuse to publish benchmark bands below a minimum accepted sample threshold
- record outlier rules instead of silently deleting data
- keep matchup-specific and all-matchup aggregate views separate

## Recommended deliverables

### Research deliverables

- Spending Quotient methodology note
- replay corpus sourcing note
- benchmark methodology note
- benchmark interpretation note

### Data deliverables

- replay source manifest
- accepted replay manifest with hashes and labels
- rejected replay manifest with reasons
- per-replay extracted metrics table
- aggregated cohort summary tables
- percentile / benchmark tables

### Code deliverables

- reusable headless replay-analysis entry point
- corpus manifest reader / validator
- batch analysis runner
- aggregation and statistics generator
- export formatters for CSV and JSON
- optional local plotting notebooks or scripts outside shipped module code

## Recommended implementation architecture

### Phase A — methodology and corpus governance

Before writing batch code, define the research contract.

Required outputs:

- a written methodology note describing each target metric and benchmark question
- a corpus manifest schema describing replay path, hash, source, player, matchup, bucket, and quality flags
- acceptance / rejection rules for replay validity and deduplication
- a specific decision on whether the 7000 cohort is combined or per-player

Recommended files to create during implementation:

- `docs/benchmarking/methodology.md`
- `docs/benchmarking/replay-sourcing.md`
- `docs/benchmarking/data-dictionary.md`

Success criteria:

- a new contributor can tell which replays qualify
- every later table column has a documented definition
- cohort rules are written before collection starts at scale

### Phase B — extract a reusable batch-analysis seam

The current analyzer logic lives in the module codebase and already powers the supported page.
That is the seam to preserve.

Recommended approach:

- expose a replay-analysis service that can run without Swing page construction
- keep all metric calculations in the existing analysis layer
- define one stable export object for offline batch use

Recommended export payload fields per analyzed Zerg player:

- replay identifier and hash
- replay metadata: map, matchup, duration, build, source label
- player identity label used for cohorting
- one row per phase with Epic 12 metrics
- quality / caveat flags from analysis diagnostics
- optional whole-game totals for future correlation work

Possible files to touch:

- [LarvaReplayAnalyzer.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java)
- [LarvaAnalysisReport.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java)
- [ReplaySummaryService.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/ReplaySummaryService.java)
- a new batch-friendly DTO such as `LarvaBenchmarkReplayRecord`
- a new headless service such as `LarvaBenchmarkExtractionService`

Success criteria:

- a single replay can be analyzed headlessly and exported deterministically
- exported benchmark rows match the same phase metrics visible on the module page
- no timing rule regresses away from loop-based authority

### Phase C — build corpus acquisition and manifest tooling

This phase is about making large-scale data collection manageable and auditable.

Recommended workflow:

1. discover public replay sources
2. download into a local ignored corpus directory
3. compute content hashes
4. classify or enrich metadata
5. write manifests
6. deduplicate and mark accepted / rejected entries

Recommended implementation split:

- keep heavyweight download / scraping helpers outside the shipped module path
- keep the corpus directory out of git
- keep only manifests and small derived artifacts in the repository

Recommended local folders:

- `benchmark-data/raw-replays/` ignored by git
- `benchmark-data/manifests/`
- `benchmark-data/exports/`
- `benchmark-data/reports/`

Manifest fields should include at least:

- replay hash
- local file path
- original filename
- source URL or source identifier
- acquisition date
- labeled player(s)
- matchup
- target cohort bucket
- inferred-vs-confirmed skill label
- acceptance state
- rejection reason if any

Success criteria:

- the full corpus can be rebuilt or audited from manifests
- duplicate replays are collapsed deterministically
- the accepted corpus count per bucket is visible at any time

### Phase D — run headless batch extraction

This phase produces the raw analysis table.

Per accepted replay, extract:

- replay metadata
- analyzed Zerg player identity
- phase intervals
- phase metrics from Epic 12
- diagnostic quality flags

Recommended export shapes:

- long-form CSV: one row per replay × player × phase
- JSON summary: one object per replay with nested phases
- optional debug dump for failed analyses

Important rule:

- keep raw extraction separate from statistical aggregation

That separation makes re-runs and auditing easier.

Success criteria:

- the batch runner can resume after failures
- failed replays are logged without aborting the whole corpus run
- export rows are deterministic for the same replay corpus and code revision

### Phase E — statistical analysis and benchmark definition

This is the actual benchmarking phase.

Required analyses:

- per-cohort averages by matchup and phase
- medians and percentile bands by matchup and phase
- variance and outlier review
- cross-cohort trend checks
- correlation between each metric and skill cohort

Recommended analyses beyond simple averages:

- compare ZvT, ZvP, and ZvZ separately before combining
- compare phase-specific signal strength instead of only whole-game averages
- inspect whether inject uptime and larva-missed are redundant or complementary
- test whether spawned-larva rate behaves as a useful performance metric or mostly as a game-state proxy

Recommended benchmark outputs:

- per-metric percentile tables
- one benchmark summary table per matchup
- one matchup-agnostic fallback table if matchup-specific sample sizes are insufficient
- an interpretation memo explaining what should later be shown to players

Success criteria:

- benchmark bands are reproducible from stored exports
- the analysis identifies which metrics best separate skill cohorts
- the result contains enough evidence to decide whether to expose single-metric or multi-metric feedback in the product

### Phase F — validate methodology with known-player sanity checks

Before trusting full benchmark tables, validate the methodology against replays whose expected quality is obvious.

Required sanity checks:

- elite cohort should generally outperform lower cohorts on larva-missed and inject uptime metrics
- the benchmark direction must be sensible phase by phase
- suspicious inversions must be investigated, not hand-waved away
- a few manually inspected replays from each cohort should match their extracted metrics plausibly

Recommended validation aids:

- compare selected benchmark-run outputs against the existing `Larva` page
- retain a small golden replay subset for regression checks
- produce summary anomaly reports such as impossible rates, empty phases, or missing Zerg labels

Success criteria:

- obvious benchmark inversions are explainable or corrected
- spot-checked replay outputs match expectations from manual replay inspection
- the corpus quality report is stable enough for later product use

### Phase G — define product-facing benchmark ranges

Only after the distributions are validated should the epic define product-facing bands.

Recommended output format:

- percentile-based thresholds per metric, per matchup, per phase
- optional compressed named tiers layered on top of those thresholds
- a separate note deciding whether to later surface:
  - raw values only
  - raw value + percentile
  - raw value + named tier
  - a composite benchmark score

Recommended default product stance:

- show benchmark context alongside raw values
- avoid a composite score until the analysis proves it adds value beyond the individual phase metrics

Success criteria:

- a future UI implementer can consume stable threshold tables without redoing the statistics work
- the benchmark definition is documented clearly enough to explain to users

## Spending Quotient research track

This epic explicitly asks whether Spending Quotient can inspire the benchmark design.

Create a short research deliverable that answers:

1. what inputs SQ uses
2. how SQ normalizes economy usage against opportunity
3. how SQ handles matchup and game-state variation
4. what validation evidence made SQ trustworthy
5. which parts map well to larva management
6. which parts do not map well and should be rejected

Recommended stance going in:

- borrow normalization discipline, not branding
- expect larva benchmarks to need phase-aware and hatch-normalized context that differs from economy-spending metrics

## Data model recommendation

Define a stable offline record shape.

### Suggested corpus manifest model

- `replayHash`
- `filePath`
- `sourceUrl`
- `sourceLabel`
- `acquiredAt`
- `originalFileName`
- `primaryZergPlayer`
- `opponentRace`
- `matchup`
- `cohortBucket`
- `cohortConfidence`
- `isAccepted`
- `rejectionReason`
- `notes`

### Suggested per-replay export model

- replay identity fields
- player identity fields
- build / map / duration metadata
- quality flags
- one nested stat block per phase
- optional whole-game derived summary

### Suggested aggregated benchmark model

- `metricName`
- `phase`
- `matchup`
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
- `qualityNotes`

## Recommended technical split

### Inside the shared analysis logic

Keep in or near the existing module analysis package:

- replay parsing
- phase metric extraction
- deterministic export DTOs
- analysis diagnostics needed for quality flags

### Outside the shipped module path

Prefer separate offline tooling for:

- large replay downloads
- corpus manifest maintenance
- bulk batch execution orchestration
- statistical aggregation and plotting
- benchmark report generation

Reason:

- the external module should stay focused on supported in-app functionality
- benchmark research tooling will evolve faster and may need dependencies inappropriate for the shipped module

## Validation strategy

### Technical validation

- deterministic output for the same replay and code revision
- graceful handling of corrupt or unsupported replays
- reproducible batch reruns from manifests
- no metric drift between interactive and batch outputs

### Statistical validation

- sample counts visible for every benchmark table
- outlier policy documented
- matchup segmentation checked before pooling
- cohort ordering inspected for directional sanity

### Product validation

- resulting benchmark tables are understandable by a future UI layer
- raw values remain available even if benchmark tiers are added
- no benchmark claim is made without sample size context

## Risks and mitigations

| Risk | Impact | Probability | Mitigation | Contingency |
|------|--------|-------------|------------|-------------|
| Replay sourcing is uneven across cohorts | High | High | Track corpus gaps early and publish fill-rate dashboards | Start with fewer benchmarked cohorts or wider confidence bands |
| 7000-MMR replays overrepresent one player | Medium | High | Cap per-player contribution and keep player labels | Publish elite cohort as named-player benchmark, not generic 7000 bucket |
| MMR labels for lower cohorts are noisy | High | High | Record confidence and source method | Use broader cohort bins or known-league proxies |
| Batch output diverges from UI output | High | Medium | Reuse one analyzer and cross-check sample replays | Block benchmark publication until parity is restored |
| Metrics correlate weakly with skill | Medium | Medium | Test multiple candidate metrics and composites | Ship only descriptive percentile context, not prescriptive score bands |
| Large corpus processing is slow or flaky | Medium | Medium | Make runs resumable and failure-tolerant | Process by manifest shards and merge results |
| Outliers distort ranges | Medium | Medium | Use percentiles and documented trimming rules | Publish percentile-only tables first |

## Implementation phases and task breakdown

### Phase 1 — benchmark methodology

- [ ] Write the benchmark methodology note
- [ ] Lock down corpus labels, buckets, and acceptance rules
- [ ] Decide the 7000-cohort interpretation
- [ ] Define the long-form export schema
- [ ] Define quality flags and rejection reasons

### Phase 2 — reusable extraction seam

- [ ] Add a headless analysis export path
- [ ] Define replay-level and phase-level export DTOs
- [ ] Validate one replay against existing UI-visible values
- [ ] Add deterministic export tests or golden checks

### Phase 3 — corpus management

- [ ] Create manifest format and validator
- [ ] Create local corpus folder conventions
- [ ] Build deduplication and acceptance tooling
- [ ] Produce a corpus coverage summary by cohort and matchup

### Phase 4 — batch runner

- [ ] Implement resumable batch execution
- [ ] Export per-replay metrics to CSV and JSON
- [ ] Log failures with enough detail for triage
- [ ] Produce a reproducible run summary

### Phase 5 — statistical analysis

- [ ] Compute cohort averages, medians, and percentiles
- [ ] Analyze correlations by phase and matchup
- [ ] Compare candidate composite-score approaches
- [ ] Draft benchmark threshold tables

### Phase 6 — validation and publication

- [ ] Run known-player sanity checks
- [ ] Review suspicious anomalies manually
- [ ] Finalize benchmark tables and interpretation memo
- [ ] Document follow-up UI integration recommendations

## Acceptance criteria

This epic is complete when all of the following are true:

- a replay corpus manifest exists with provenance, deduplication, and acceptance state
- batch extraction reuses the same metric engine as the module page
- at least one reproducible benchmark export can be regenerated from the accepted corpus
- averages, medians, and percentile bands exist per cohort, matchup, and phase where sample sizes are sufficient
- the project has a written conclusion on whether Spending Quotient concepts help the larva benchmark design
- benchmark ranges are documented clearly enough for later UI integration
- raw replay files remain outside normal repository versioning unless explicitly requested otherwise

## Suggested files to create during implementation

### Planning and methodology

- [prompts/epic13.md](epic13.md)
- `docs/benchmarking/methodology.md`
- `docs/benchmarking/spending-quotient-research.md`
- `docs/benchmarking/data-dictionary.md`
- `docs/benchmarking/benchmark-interpretation.md`

### Offline corpus and outputs

- `benchmark-data/manifests/replay-sources.json`
- `benchmark-data/manifests/accepted-replays.json`
- `benchmark-data/manifests/rejected-replays.json`
- `benchmark-data/exports/phase-metrics.csv`
- `benchmark-data/exports/phase-metrics.json`
- `benchmark-data/reports/benchmark-summary.md`

### Code candidates

- [LarvaReplayAnalyzer.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java)
- [LarvaAnalysisReport.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java)
- a new `LarvaBenchmarkExtractionService`
- a new `LarvaBenchmarkReplayRecord`
- a new offline batch runner outside the shipped module path

## Final recommendation

Build Epic 13 as an offline research-and-benchmark pipeline anchored to the existing analyzer, not as a UI-first feature.

That gives the project three important benefits:

1. benchmark definitions stay consistent with the module's real metrics,
2. replay-corpus quality stays auditable,
3. later UI work can consume validated threshold tables instead of guessing what “good” means.

## Next technical question after planning

Once the first benchmark tables exist, the next product question should be:

**Which benchmark context helps users most on the supported `Larva` page: percentile bands, named tiers, or a carefully validated composite score?**

# Epic 13 replay sourcing and corpus governance

## Purpose

This document defines how replays are acquired, stored, labeled, and audited for benchmark research.

It exists to prevent three common benchmark failures:

- unknown replay provenance,
- duplicate-heavy corpora,
- skill labels that look precise but are actually guesses.

## Allowed sources

Only use publicly accessible or user-permitted replay sources.

Allowed examples:

- public replay archives,
- named-player replay packs published for public download,
- public tournament replay releases,
- user-supplied local replay collections when the user explicitly authorizes their use.

Not allowed for the benchmark corpus:

- private collections without permission,
- replays with no traceable source,
- files passed around without any reproducible provenance note.

## Repository storage policy

Raw replay files are not normal repository artifacts.

Rules:

- keep raw replay files in a local ignored directory,
- keep only manifests, hashes, schemas, and derived statistics under version control,
- never rely on ad hoc desktop folders without updating the manifest.

## Local folder layout

Recommended local layout:

- `benchmark-data/raw-replays/` — ignored raw files
- `benchmark-data/manifests/` — tracked manifests and schemas
- `benchmark-data/exports/` — tracked or reviewable derived tabular outputs
- `benchmark-data/reports/` — tracked summary reports

## Acquisition workflow

Every replay enters the corpus through the same workflow.

1. download or copy the replay into the local raw corpus,
2. compute SHA-256,
3. record original filename,
4. record source URL or source label,
5. record acquisition date,
6. add provisional cohort labels,
7. run deduplication,
8. mark accepted or rejected with a reason,

For tracked internet seeds, the current proof-of-concept workflow is:

1. discover replay candidates from `benchmark-data/manifests/replay-source-seeds.json`,
2. download elite or broad replay candidates into categorized local folders under `benchmark-data/raw-replays/categorized/`,
3. convert reviewed elite seeded downloads into canonical manifest entries with `import-seeded-downloads`,
4. keep broader unlabeled replay pools downloaded but outside the accepted manifest until cohort evidence is added.
9. preserve any additional discovered sources on the canonical record.

## Canonical replay identity

The canonical replay identity is the SHA-256 hash of the replay file.

Why:

- filenames are unreliable,
- replay sites may rename archives,
- the same replay may be republished across multiple mirrors.

## Source attribution rules

Every replay record must include at least one of:

- `sourceUrl`
- `sourceLabel`

Recommended practice:

- use both when possible,
- keep `sourceLabel` human-readable,
- keep `sourceUrl` stable and exact when available.

If a replay is user-supplied and not publicly hosted, use:

- `sourceLabel = "user-supplied"`
- and explain the origin in `notes`.

## Cohort labeling workflow

## Named elite cohort

For the elite bucket, Phase 1 uses named-player targeting.

Rules:

- only Serral or Reynor replays qualify for the initial `elite-7000` cohort,
- player identity must be recorded explicitly,
- player composition must remain visible in corpus summaries.

## Lower-MMR cohorts

For the 2000, 3000, 4000, and 5000 buckets:

- record the intended bucket,
- record the labeling method,
- record confidence as `confirmed`, `strong-inference`, or `weak-inference`.

Examples of stronger evidence:

- source metadata explicitly listing MMR or league close to the target bucket,
- curated ladder packs with documented rating bands.

Examples of weaker evidence:

- filename-only claims,
- forum-post assumptions with no supporting metadata.

## Acceptance review checklist

Before a replay becomes accepted, confirm:

- source recorded,
- SHA-256 recorded,
- matchup recorded from the Zerg perspective,
- Zerg target player recorded,
- cohort bucket recorded,
- cohort confidence recorded,
- duplicate check completed,
- parser and extraction path can read it.

## Rejection review checklist

Reject and document the reason if:

- file is unreadable or corrupt,
- replay is duplicate of an accepted replay,
- no suitable Zerg player matches the target cohort,
- provenance is missing,
- matchup is outside `ZvT`, `ZvP`, `ZvZ`,
- batch extraction fails in a way that invalidates the benchmark row.

## Corpus balancing rules

These rules are for sampling balance, not replay validity.

### Elite cohort balancing

- try to keep each of Serral and Reynor under `60%` of any elite matchup bucket,
- if the bucket cannot be balanced, keep the extra replays but report the imbalance.

### General cohort balancing

- do not silently overfill one matchup while another remains under-sampled,
- keep running coverage summaries by cohort and matchup,
- do not publish broad benchmark claims from a slice that is visibly lopsided.

## Auditability requirements

The corpus must remain auditable.

That means:

- accepted and rejected entries remain stored in manifests,
- duplicate source origins are preserved,
- manual overrides are documented in `notes`,
- schema version is recorded in manifest files.

## Manual override policy

Sometimes a replay may deserve manual review.

Allowed manual overrides:

- promote a replay from rejected to accepted after source clarification,
- downgrade confidence after new ambiguity is discovered,
- attach an additional note explaining a cohort exception.

Requirements:

- manual overrides must update the manifest,
- the reason must be recorded in `notes`,
- the underlying hash must stay unchanged.

## Phase 1 deliverable expectation

At the end of Phase 1, a contributor should be able to answer:

- where raw replays are stored,
- what qualifies as an accepted replay,
- how duplicates are detected,
- how skill labels are expressed,
- how elite-player balancing is enforced,
- and where the canonical manifest schema lives.

# Benchmark manifest files

This folder holds tracked benchmark-corpus metadata for Epic 13.

## Files created in Phase 1

- `replay-corpus.schema.json` — JSON Schema for the canonical corpus manifest
- `corpus-manifest.template.json` — example manifest document matching the schema

## Intended future tracked files

- accepted replay manifest snapshots
- rejected replay manifest snapshots
- replay source manifests
- corpus coverage summaries

Tracked internet-source research files may also live here, including:

- public source catalogs
- discovery seed manifests
- replay-pack reference manifests

## Storage policy

Raw replay files do not belong in this folder.

Keep raw replay files under the ignored local path:

- `benchmark-data/raw-replays/`

Keep only:

- schemas,
- manifests,
- small reviewable metadata,
- derived benchmark outputs intended for audit or publication.

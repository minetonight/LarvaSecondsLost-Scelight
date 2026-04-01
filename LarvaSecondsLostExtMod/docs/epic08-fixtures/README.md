# Epic 08 fixture set

This folder implements Epic 08 Story 08.01 with a diff-friendly validation fixture catalog and golden outputs for the hardened larva timeline pipeline.

## What is stored here

Because replay binaries are not committed in this repository, the fixture set is stored as **scenario-based validation fixtures**:

- [fixture-catalog.md](fixture-catalog.md) describes each replay scenario to validate.
- [goldens/simple-single-hatchery.txt](goldens/simple-single-hatchery.txt) and the other files define the expected normalized output shape.
- The expected output format matches the deterministic validation snapshot now written into the development dump file.

## How to use the fixture set

1. Pick or record a local replay that matches one fixture scenario.
2. Run Scelight with development dump enabled.
3. Load the replay on the module-owned `Larva` page.
4. Compare the `Deterministic validation snapshot` section in the dump file against the matching golden file in this folder.
5. If the replay intentionally differs from the scenario, create a new golden file instead of mutating an unrelated one.

## Why this format exists

The Epic 07 feature was already visually working. Epic 08 Story 08.01 adds a stable textual contract that is easy to diff when validating:

- visible hatchery rows
- `3+ larva` windows
- missed-larva markers
- per-player totals
- key analysis counters

This keeps validation lightweight and Java-7-friendly while the module continues to use the supported external-module fallback page.

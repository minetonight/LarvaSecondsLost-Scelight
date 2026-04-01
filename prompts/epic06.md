# Epic 06 — Larva-to-hatchery assignment foundation

## Epic goal

Build the replay-analysis foundation needed to reconstruct per-hatchery larva count timelines reliably enough for later `3+ larva` window rendering.

This epic exists because Epic 04 and Epic 05 already established that a pure external module cannot use Scelight's native chart dropdown or augment the native Base Control chart in a supported way. The supported path now depends on the module-owned `Larva` timeline, which requires replay-derived per-hatchery larva counts.

Epic 06 should stop short of the final red-rectangle visualization. Its job is to produce the replay-analysis primitives and diagnostics that Epic 07 can convert into real `3+ larva` windows, missed-larva summaries, and hover details.

## Context

From the Epic 06 handoff summary in the module README:

- Epic 05 proved that a pure external module cannot augment Scelight's native `Base Control` chart in a supported way.
- Epic 05 proved that the separate module-owned `Larva` timeline remains the supported rendering path.
- Epic 05 left the next major unknown as larva-to-hatchery attribution and replay-derived per-hatchery larva counts.
- The fallback visualization path from earlier epics is already stable enough to host replay-derived rows once those counts exist.

From the project goal and know-how documents:

- a larva unit is assumed to exist in replay data
- a larva should be assigned to a hatchery either by replay relation or by spatial proximity near hatchery coordinates
- Java 7 syntax and Java 7 API compatibility are mandatory
- the final feature will need one line per hatchery, separated by player, with later `3+ larva` windows drawn from replay-derived state

From the infrastructure plan:

- Epic 06 is the replay-analysis foundation for hatchery tracking, larva tracking, Spawn Larva correlation, opening calibration, and heuristic assignment
- the intended output is a normalized `HatcheryLarvaTimeline` model with timestamped larva counts and confidence-aware diagnostics
- the replay-analysis layer should stay decoupled from page painting and later Epic 07 window rendering

## Non-goals

- no final `3+ larva` red-rectangle rendering yet
- no thick black missed-larva threshold markers yet
- no total potential-larva-missed overview text yet
- no hover minerals / gas detail yet
- no attempt to restore unsupported native chart or Base Control integration

## Story 06.01 — Track hatchery lifecycle with morph continuity

**As a** developer  
**I want** hatchery-like buildings tracked across birth, completion, morph, owner change, and death  
**So that** larva can later be attributed to stable hatchery timelines instead of isolated structures.

### Acceptance criteria

- Hatchery-like units such as `Hatchery`, `Lair`, and `Hive` are tracked on the same unit tag timeline.
- Completion, alive/dead state, and owner information are updated as replay events change.
- Morph continuity on the same unit tag is preserved instead of treating each morph as a new hatchery timeline.
- The tracked state is usable by later larva assignment logic.

### Notes

This story creates the hatchery side of the assignment model.

## Story 06.02 — Correlate `SpawnLarva` command targets

**As a** developer  
**I want** `SpawnLarva` game events correlated by hatchery tag  
**So that** larva births can use a stronger replay-native signal before heuristic proximity is attempted.

### Acceptance criteria

- `SpawnLarva` command targets are collected by hatchery tag when game events expose them.
- That signal is available during larva birth assignment.
- The implementation remains decoupled from Swing/UI code.
- Diagnostics can report that inject correlation influenced assignment results.

### Notes

This story provides the strongest supported signal short of a direct larva-parent relation.

## Story 06.03 — Derive and document opening calibration

**As a** developer  
**I want** an opening-based hatchery-to-larva offset calibration  
**So that** spatial fallback assignment uses a documented replay-derived heuristic instead of an unexplained magic rule.

### Acceptance criteria

- The replay opening is inspected for simple Zerg starts with one hatchery and at least three larva.
- Average hatchery-to-larva offset and assignment radius are derived when those samples exist.
- A documented fallback offset/radius is used when replay-based calibration is unavailable.
- Diagnostics report whether calibration came from replay samples or fallback defaults.

### Notes

This story turns the `below the hatchery` assumption into a replay-derived heuristic.

## Story 06.04 — Implement larva-to-hatchery assignment heuristic

**As a** developer  
**I want** larva births assigned through direct creator tags, inject correlation, and calibrated spatial fallback  
**So that** replay-derived larva counts can be reconstructed with useful confidence levels.

### Acceptance criteria

- Direct creator-based assignment is preferred when available.
- Recent `SpawnLarva` correlation strengthens a hatchery match.
- Calibrated spatial matching is used when no direct relation exists.
- Ambiguous or unassignable larva births remain explicitly unassigned instead of guessed silently.
- Assignment confidence is captured for diagnostics.

### Notes

This story should fail safely on ambiguity.

## Story 06.05 — Emit per-hatchery larva count timelines

**As a** developer  
**I want** replay-derived per-hatchery larva count timelines emitted as normalized data  
**So that** Epic 07 can convert those timelines into real `3+ larva` windows.

### Acceptance criteria

- Each tracked hatchery can produce a time-ordered larva count timeline.
- Timeline points update when larva are assigned, removed, or invalidated by type/death changes.
- Timelines are grouped with hatchery identity and player identity.
- The output is separate from UI rendering and reusable by later epics.

### Notes

This story produces the core replay-derived data model for the supported Larva timeline.

## Story 06.06 — Surface replay-analysis diagnostics and prepare Epic 07 handoff

**As a** developer  
**I want** calibration, assignment, and timeline diagnostics surfaced clearly  
**So that** Epic 07 can start from a proven replay-analysis foundation instead of treating assignment as a black box.

### Acceptance criteria

- The module surfaces calibration source, assignment confidence counts, and per-hatchery larva count diagnostics.
- Logs and/or the development diagnostic dump can confirm replay-analysis outcomes.
- A short note states what Epic 06 has proven:
  - hatchery-like buildings are tracked across lifecycle changes,
  - larva births can be correlated or assigned heuristically,
  - calibrated replay-derived larva count timelines can be emitted,
  - the supported rendering path remains the module-owned `Larva` timeline.
- A short note states what is still unresolved:
  - final `3+ larva` windows,
  - hatchery lifetime-bounded rendering,
  - missed-larva accumulation markers,
  - overview totals and hover resource details.
- The next technical question is clearly identified as: **how should the derived per-hatchery larva counts be converted into stable `3+ larva` windows and visualized on the Larva timeline?**

## Definition of done

Epic 06 is done when:

- hatchery-like units are tracked across lifecycle changes on stable timelines
- `SpawnLarva` targets are correlated when replay data exposes them
- a replay-derived calibration or documented fallback heuristic exists
- larva births can be assigned with confidence-aware diagnostics
- per-hatchery larva count timelines are emitted for the supported module-owned timeline path
- the project is ready for Epic 07 real `3+ larva` window rendering

## Risks in this epic

- Replay data may still lack a direct larva-parent relation for many births.
- Calibration may not be available on every replay opening.
- Spatial heuristics may become ambiguous in dense multi-hatchery scenarios.
- It may be tempting to overfit the heuristic to one replay shape instead of documenting a safe fallback.

## Suggested implementation order

1. Story 06.01 — Track hatchery lifecycle with morph continuity
2. Story 06.02 — Correlate `SpawnLarva` command targets
3. Story 06.03 — Derive and document opening calibration
4. Story 06.04 — Implement larva-to-hatchery assignment heuristic
5. Story 06.05 — Emit per-hatchery larva count timelines
6. Story 06.06 — Surface replay-analysis diagnostics and prepare Epic 07 handoff

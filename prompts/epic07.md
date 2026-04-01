# Epic 07 — Larva windows and supported timeline visualization

## Epic goal

Convert the Epic 06 per-hatchery larva count timelines into stable replay-derived `3+ larva` windows and visualize them on the supported module-owned `Larva` timeline.

This epic exists because Epic 04 and Epic 05 already established that a pure external module cannot add a native `larva` chart entry or augment Scelight's native `Base Control` chart in a supported way. The supported production path is therefore the module-owned `Larva` page and its timeline component.

Epic 07 should deliver the first real larva-window visualization on that supported path: one row per replay-derived hatchery, separated by player, with red rectangles during periods where the hatchery has at least three larva. It should also start the missed-larva accounting flow that later stories can expand with threshold markers, totals, and hover details.

## Context

From the Epic 05 handoff summary in the module README:

- Epic 05 proved that the public external module API does not expose supported augmentation of the native `Base Control` chart.
- Epic 05 proved that the separate module-owned `Larva` timeline remains the supported visualization path once native augmentation is ruled out.
- Epic 05 left larva-to-hatchery attribution and stable per-hatchery larva counts as the next prerequisite for real visualization.

From the Epic 06 handoff summary in the module README:

- Epic 06 now parses hatchery-like tracker events for `Hatchery`, `Lair`, and `Hive`, preserving morph continuity on the same unit tag.
- Epic 06 now correlates `SpawnLarva` command targets and uses them as a stronger assignment signal before calibrated spatial fallback.
- Epic 06 now derives a replay-specific hatchery-to-larva offset and assignment radius when the replay opening supports calibration, and otherwise uses documented defaults.
- Epic 06 now emits per-hatchery larva count timelines with calibration and assignment diagnostics.
- Epic 06 left the next major technical question as: how should the derived per-hatchery larva count timelines be converted into stable `3+ larva` windows and visualized on the Larva timeline?

From the project goal and know-how documents:

- the desired visualization is similar to Base Control, but it must live on the supported module-owned path
- a red rectangle starts when a hatchery has `3+` larva and ends when it drops below three larva
- there must be a different line for each hatchery created in the game, separated by player
- the hatchery line should start when the building is completed and has larva, not at match time `0:00`
- the hatchery line should end when the building is destroyed, or otherwise at replay end
- hatcheries that were never completed or that created zero larva must not be shown
- Java 7 syntax and Java 7 API compatibility remain mandatory

From the infrastructure plan:

- Epic 07 is the first real conversion from replay-derived larva counts to `LarvaWindow`-style segments
- rendering must stay decoupled from replay parser objects by using normalized presentation models
- the supported output path is the module-owned chart-like Larva timeline, not unsupported native chart registration or Base Control patching
- later stories should layer on missed-larva markers, per-hatchery totals, match totals, and hover-time resource details

## Non-goals

- no unsupported native chart dropdown registration
- no unsupported Base Control augmentation
- no reflection-based patching of replay-analyzer internals
- no non-Java-7 language or API usage
- no final hover minerals / gas detail in the first story unless its data model hook is naturally required

## Story 07.01 — Convert count timelines into stable `3+ larva` windows

**As a** user  
**I want** the supported `Larva` timeline to show real replay-derived `3+ larva` windows per hatchery  
**So that** the placeholder preview becomes a real larva-saturation chart on the supported external-module path.

### Acceptance criteria

- Per-hatchery larva count timelines are converted into stable `3+ larva` windows.
- Consecutive count points that remain at `3+` larva are merged into continuous red-window spans instead of fragmented micro-segments.
- Rows are rendered only for hatcheries that completed and that created at least one larva.
- A hatchery row starts when the hatchery is completed and has larva, not at replay start.
- A hatchery row ends when the hatchery is destroyed, or at replay end if it survives.
- The conversion stays in replay-analysis / presentation-model code, not in Swing painting code.
- The module-owned timeline renders these real windows instead of only replay-length-derived placeholder spans.

### Notes

This is the minimum user-visible Epic 07 slice. It should establish the real larva-window model that later stories can decorate with missed-larva markers and totals.

## Story 07.02 — Show per-hatchery missed-larva thresholds

**As a** user  
**I want** one thick black marker every time a hatchery accumulates 11 seconds with `3+` larva  
**So that** I can see each potential larva missed on the row where it occurred.

### Acceptance criteria

- Each hatchery accumulates only time spent inside `3+` larva windows.
- Every accumulated 11 seconds produces one thick black marker on that hatchery row.
- Markers continue across multiple windows when accumulated saturation time is intermittent.
- Marker generation is derived from the same normalized timeline model, not recomputed in paint code.

## Story 07.03 — Show per-hatchery and match totals

**As a** user  
**I want** missed-larva totals shown per hatchery and for the whole replay  
**So that** the chart communicates both local and overall impact.

### Acceptance criteria

- Each visible hatchery row shows text in the format `[x] potential larva missed`.
- The top of the chart shows `[y] total potential larva missed in this match`.
- Totals match the number of 11-second threshold markers.
- Hatcheries with zero missed larva still render correctly if they otherwise qualify for display.

## Story 07.04 — Prepare hover-time resource context for later detail

**As a** user  
**I want** missed-larva markers to be ready for minerals / gas hover details  
**So that** later work can attach economy context without restructuring the chart model.

### Acceptance criteria

- The normalized model can attach per-marker hover metadata without leaking replay parser objects into Swing code.
- Marker-level context can carry minerals and gas values separately.
- The initial implementation may leave the hover text unrendered if the data pipeline hook is all that is required at this stage.

## Story 07.05 — Prepare Epic 08 handoff conditions

**As a** developer  
**I want** Epic 07 outcomes summarized clearly  
**So that** later hardening work can focus on validation, replay fixtures, and documentation instead of rediscovering what is already proven.

### Acceptance criteria

- A short note states what Epic 07 has proven:
  - replay-derived per-hatchery count timelines can be converted into stable `3+ larva` windows,
  - the supported module-owned `Larva` timeline can render one row per qualifying hatchery separated by player,
  - hatchery lifetime bounds can control row visibility,
  - the project remains on the supported fallback rendering path.
- A short note states what is still unresolved:
  - any missing hover resource detail,
  - hardening against replay edge cases,
  - replay-fixture validation coverage,
  - final documentation updates.
- The next technical question is clearly identified as: **how should Epic 07's larva-window and missed-larva visualization be hardened and validated across replay fixtures and edge cases?**

## Definition of done

Epic 07 is done when:

- real replay-derived `3+ larva` windows are rendered on the supported module-owned `Larva` timeline
- one row is shown per qualifying hatchery and rows are grouped by player
- hatchery rows respect lifetime bounds instead of starting at match `0:00`
- hatcheries that never completed or created zero larva are excluded
- missed-larva threshold markers and totals are derived from normalized model data
- the project is ready for hardening and validation work

## Risks in this epic

- Derived count timelines may still have ambiguous edge cases around hatchery death, morph timing, or replay truncation.
- A direct port of placeholder rendering could accidentally keep replay-wide rails instead of hatchery-lifetime-bounded rails.
- Missed-larva accumulation could be implemented incorrectly if intermittent `3+` windows are not merged or accumulated consistently.
- Hover-data ambitions could overcomplicate the first real visualization slice if they are not kept behind a clean model boundary.

## Suggested implementation order

1. Story 07.01 — Convert count timelines into stable `3+ larva` windows
2. Story 07.02 — Show per-hatchery missed-larva thresholds
3. Story 07.03 — Show per-hatchery and match totals
4. Story 07.04 — Prepare hover-time resource context for later detail
5. Story 07.05 — Prepare Epic 08 handoff conditions

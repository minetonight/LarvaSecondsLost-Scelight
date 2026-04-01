# Epic 08 fixture catalog

The fixture catalog defines the minimum validation scenarios for Epic 08 Stories 08.01 and 08.02.

## Fixture 01 — simple single hatchery opening

**Goal:** verify the base happy path.

Expected characteristics:
- one visible hatchery row
- at least one stable `3+ larva` window
- missed-larva markers increase monotonically
- no ambiguous or no-eligible larva assignments

Golden file: [goldens/simple-single-hatchery.txt](goldens/simple-single-hatchery.txt)

## Fixture 02 — intermittent saturation across multiple windows

**Goal:** verify that `11` accumulated seconds are preserved across separate windows.

Expected characteristics:
- one hatchery row with multiple windows
- missed-larva markers continue across separate red windows
- no double counting when windows touch or partially overlap after normalization

Golden file: [goldens/intermittent-saturation.txt](goldens/intermittent-saturation.txt)

## Fixture 03 — hatchery destroyed before replay end

**Goal:** verify visible row lifetime bounds.

Expected characteristics:
- row ends at hatchery destruction, not replay end
- windows and markers never extend past the destroyed boundary
- later replay events do not reopen the row

Golden file: [goldens/destroyed-hatchery-bounds.txt](goldens/destroyed-hatchery-bounds.txt)

## Fixture 04 — morph continuity and sparse resource snapshots

**Goal:** verify that `Hatchery` → `Lair` / `Hive` continuity does not split the row and sparse stats do not break output.

Expected characteristics:
- one continuous row across morph changes on the same tag
- row label reflects the final hatchery lineage type
- hover/resource context may fall back to `unknown`, but row/window/marker output remains stable

Golden file: [goldens/morph-continuity-sparse-resources.txt](goldens/morph-continuity-sparse-resources.txt)

## Fixture 05 — ambiguous multi-hatchery assignment pressure

**Goal:** verify fail-safe behavior under ambiguous larva attribution.

Expected characteristics:
- ambiguous larva remain counted as ambiguous/unassigned
- visualization stays stable instead of fabricating extra windows
- analysis counters expose the ambiguity explicitly

Golden file: [goldens/ambiguous-assignment.txt](goldens/ambiguous-assignment.txt)

## Fixture 06 — out-of-order or duplicate transitions

**Goal:** verify Story 08.02 hardening around duplicate loops, out-of-order points, and zero-duration segments.

Expected characteristics:
- normalized windows remain chronological
- zero-length windows are absent
- marker order is monotonic
- duplicate loop transitions collapse to one effective state per loop

Golden file: [goldens/normalized-transitions.txt](goldens/normalized-transitions.txt)

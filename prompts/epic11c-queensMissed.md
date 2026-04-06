# Epic 11C — queen discovery hardening for missed injects

## Goal

Fix replays where dark-red missed-inject windows stay completely empty because no queens ever enter the idle-inject pipeline.

The failing dump in [knowhow/LarvaSecondsLost-dev-dump-no-misses-bug.txt](knowhow/LarvaSecondsLost-dev-dump-no-misses-bug.txt) already proves that hatch-side inject inference is healthy:

- `analysis.injectWindows=60`
- `analysis.idleInjectWindows=0`
- every idle-inject hatchery reports `attributedQueenCommands=0`
- every idle-inject hatchery reports `idle diagnostics: none`

That pattern means the hatcheries are present, but queen evidence never gets admitted.

## Root-cause hypothesis

The current queen tracker gate in [LarvaReplayAnalyzer.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java) is too strict:

- tracker events only create or update `QueenState` when `unitTypeName == "Queen"`
- command evidence is also dropped if the selected queen tag is not already present in `queenByTag`

So one replay can fail in two ways:

1. tracker `unitTypeName` is a queen variant or alias and never matches the exact string
2. singleton-selected queen commands exist, but they cannot seed a `QueenState` because tracker discovery already failed

## Scope

This story changes only queen discovery and diagnostics for Story 11.03 missed injects.

Keep unchanged:

- green inferred inject windows
- larva-to-hatchery assignment rules
- current hybrid / deterministic idle-inject allocation rules
- row ordering by player then hatchery completion time

## Implementation plan

### Phase A — broaden tracker-side queen recognition

Replace exact queen-name checks with a normalized queen-type predicate.

Requirements:

- accept replay unit names that still clearly identify a queen
- keep tag-based updates working for already known queens
- allow `UnitTypeChange` events to create a queen state when the type change itself is the first queen proof

## Phase B — allow command-side queen seeding

When singleton selection proves a queen caster, command collection should be able to create a minimal `QueenState` even if tracker discovery missed it.

Requirements:

- seed the queen tag
- resolve player ownership from command user data when possible
- mark the queen as alive/completed at the command loop conservatively
- keep later tracker observations free to add better position/lifecycle detail

## Phase C — expose queen-discovery diagnostics

Add high-level counters so dumps immediately reveal whether the failure is:

- no queens tracked at all
- queens tracked but never position-observed
- queens discovered only from commands
- no queen command evidence collected

Recommended counters:

- tracked queens
- tracker-observed queens
- command-seeded queens
- queen command evidence count

## Phase D — validate against the failing replay shape

Success criteria for the no-misses replay family:

- at least some queens appear in diagnostics
- idle-inject analysis no longer stays globally blind
- dark-red windows appear whenever the hybrid rules can justify them
- if a replay is still uncertain, the dump must now explain why

## Acceptance criteria

- queen tracker admission no longer depends on one exact string form
- singleton-selected queen commands can seed missing queen state
- diagnostic dumps expose queen discovery counts
- `analysis.idleInjectWindows=0` is no longer a silent queen-blindness failure mode
- project still compiles with `ant -noinput COMPILE`

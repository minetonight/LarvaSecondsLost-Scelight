# Epic 12 — stats per game phase implementation plan

## Executive summary

Add a per-player phase table to the supported module-owned `Larva` page.

The table should sit directly under each player header, span the full available chart width, and report four per-phase metrics:

- larva missed, as larva per hatch per minute
- injects missed, as larva per hatch per minute
- total larva spawned, as larva per hatch per minute
- injection uptime, as a percentage

This feature should stay aligned with the existing module architecture:

- replay analysis remains authoritative in the analyzer/model layer
- the preview component renders only normalized immutable presentation models
- all timing calculations stay in replay loops until the presentation boundary

That last rule is mandatory. Follow [knowhow/time-management.md](../knowhow/time-management.md): loops stay authoritative, and visible gameplay-time formatting happens only when labels are rendered.

## Goal

Show phase-aware larva and inject efficiency for each Zerg player without leaving the supported external-module integration path.

The output should help answer questions like:

- did the player miss more larva in early game or late game?
- how much inject value was lost in each phase?
- how much larva was actually spawned per hatch per minute in each phase?
- how much of the eligible hatchery time was spent injected?

## Requested phase definitions

Phase definitions should be interpreted as monotonic promotions, not reversible buckets:

- `EARLY` starts at replay start
- `MID` starts after the player has been above `36` simultaneously alive drones for more than `30` seconds
- `LATE` starts after the player has been above `66` simultaneously alive drones for more than `30` seconds
- `END` starts after the player has been above `89` simultaneously alive drones for more than `30` seconds

Recommended clarification for implementation:

- use `> 36`, `> 66`, and `> 89` worker-count thresholds because the spec says “up to 36”, “up to 66”, and “90+”
- once promoted, do **not** downgrade if worker count later falls
- if worker count dips below a promotion threshold before the full `30` seconds completes, reset that promotion candidate window

This keeps the table deterministic and avoids noisy phase bouncing.

## Metric contract

### 1. Larva missed per phase

Source of truth:

- existing missed-larva accumulation already derived from `3+` larva saturation windows and `11` second thresholds

Numerator:

- count missed-larva thresholds whose underlying loops fall inside the phase interval

Denominator:

- eligible hatchery lifetime inside the phase, measured in loops and converted only at the final rate calculation

Rendered value:

$$
\\text{larva missed rate} = \\frac{\\text{missed larva count} \\times \\text{loops per minute}}{\\text{eligible hatch loops}}
$$

### 2. Injects missed per phase

Source of truth:

- existing dark-red idle-inject opportunity windows and `29` second missed-inject threshold markers

Numerator:

- count potential injected larva missed inside the phase
- since one missed-inject threshold equals `3` larva, accumulate as larva rather than raw threshold count

Denominator:

- same hatch-eligible loop denominator used for the larva-per-hatch-per-minute rate

Rendered value:

$$
\\text{inject-missed rate} = \\frac{\\text{missed inject larva} \\times \\text{loops per minute}}{\\text{eligible hatch loops}}
$$

### 3. Total larva spawned per phase

Source of truth:

- assigned larva births already reconstructed during replay analysis
- include both natural spawn and inferred inject spawns

Numerator:

- count assigned larva birth events inside the phase interval

Denominator:

- same eligible hatch loops inside the phase

Rendered value:

$$
\\text{spawned rate} = \\frac{\\text{spawned larva count} \\times \\text{loops per minute}}{\\text{eligible hatch loops}}
$$

### 4. Injection uptime %

Source of truth:

- existing green inject-active windows

Eligibility start:

- global player start is `36` gameplay seconds after Spawning Pool completion
- per hatchery start is `max(global inject start, hatch completion)`

Eligibility end:

- hatchery destruction loop, otherwise replay end loop

Numerator:

- sum of inject-active loops inside the phase and inside the inject-eligible hatchery interval

Denominator:

- total inject-eligible loops inside the phase

Rendered value:

$$
\\text{inject uptime} = \\frac{\\text{inject-active loops}}{\\text{inject-eligible loops}} \\times 100\\%
$$

If there are no inject-eligible loops in a phase, show `n/a` rather than `0%`.

## Timing rules

This epic touches timing heavily, so the rules from [knowhow/time-management.md](../knowhow/time-management.md) must be restated here:

- phase boundaries are stored in loops
- hatchery eligibility windows are stored in loops
- inject eligibility windows are stored in loops
- missed-larva thresholds are counted from loop positions
- missed-inject thresholds are counted from loop positions
- total spawned larva uses event loops directly
- replay end comes from replay/header elapsed loops
- displayed gameplay-time labels are derived from loops only when formatting visible text

Do **not** reverse-calculate loops from displayed milliseconds.

## Current architecture to build on

Relevant existing components:

- [LarvaReplayAnalyzer.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java) already owns replay-derived analysis, larva assignment, inject inference, idle-inject detection, and resource snapshots
- [LarvaAnalysisReport.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java) is the immutable analysis payload carried to the UI
- [LarvaTimelineModelBuilder.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModelBuilder.java) converts analysis results into immutable presentation data
- [LarvaTimelineModel.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModel.java) currently carries per-player overview labels and row models
- [LarvaTimelinePreviewComp.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelinePreviewComp.java) paints the player header, current overview line, and hatchery rows
- [LarvaReplayPageComp.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayPageComp.java) owns the page surface and summary text

There is already one important enabling fact in the analyzer:

- `IPlayerStatsEvent` is already collected and the API exposes `getWorkersActiveCount()`

That makes player-phase detection feasible without leaving the supported replay surface.

## Recommended implementation architecture

### Phase A — define the new immutable analysis models

Add new immutable models in the module package:

- `LarvaGamePhase`
- `LarvaPhaseInterval`
- `LarvaPhaseStats`
- `LarvaPlayerPhaseTable`

Recommended responsibilities:

- `LarvaGamePhase`: enum for `EARLY`, `MID`, `LATE`, `END`
- `LarvaPhaseInterval`: one resolved per-player phase interval in loops
- `LarvaPhaseStats`: numerators, denominators, and rendered-support fields for one player/phase pair
- `LarvaPlayerPhaseTable`: one player's full 4x4 table model

Also add a worker-count snapshot model if extending the existing resource snapshot becomes too awkward:

- either extend [LarvaPlayerResourceSnapshot.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaPlayerResourceSnapshot.java)
- or introduce a dedicated `LarvaPlayerPhaseSnapshot`

Recommendation: keep phase logic separate from tooltip resource snapshots unless a combined model stays clean.

### Phase B — extend analyzer input collection

In [LarvaReplayAnalyzer.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java):

1. record per-player worker-count snapshots from `IPlayerStatsEvent.getWorkersActiveCount()`
2. record Spawning Pool completion loops per player
3. preserve assigned larva birth loops by hatchery and player if the current state objects do not already expose them in an analysis-friendly way

New analyzer responsibilities for this epic:

- player-level worker-count timeline
- player-level queen-unlock baseline from Spawning Pool completion + 36 seconds
- player × phase interval resolution
- player × phase metric aggregation

### Phase C — build a dedicated phase segmentation engine

Implement phase resolution inside the analyzer layer, not in Swing.

Recommended algorithm:

1. sort each player's worker-count snapshots by loop
2. track candidate promotion windows for `MID`, `LATE`, and `END`
3. when worker count stays above the threshold continuously for more than `30` gameplay seconds, confirm promotion
4. close the prior phase interval at the confirmed promotion loop
5. create the next phase interval starting at that loop
6. never downgrade later

Important detail:

- dwell measurement must use loop deltas between successive snapshots
- sparse tracker snapshots must be handled by interval logic, not by formatted timestamp strings

### Phase D — aggregate per-phase numerators and denominators

Use the existing replay-derived models as the source of truth.

Recommended metric sources:

- larva missed: existing missed-larva marker loops
- inject missed: existing missed-inject marker loops
- total larva spawned: assigned larva birth loops
- inject uptime: green inject-window loop ranges
- hatch eligibility: hatchery lifetime intersection with the phase interval
- inject eligibility: intersection of phase interval with `[max(pool unlock + 36s, hatch completion), hatch destruction/replay end]`

Recommended aggregation strategy:

1. iterate player phase intervals
2. collect the player's hatcheries
3. intersect each hatchery lifetime with the phase interval
4. sum hatch-eligible loops
5. intersect inject-eligible windows with the same phase interval
6. sum inject-eligible loops and inject-active loops
7. count marker/birth events whose loops fall inside the phase interval
8. convert only the final display values, not the internal windows

### Phase E — carry a structured table into the presentation model

The current per-player summary is only a string built from `groupOverviewLabelMap` in [LarvaTimelineModelBuilder.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModelBuilder.java).

That is too weak for a 4x4 table.

Recommended change:

- extend [LarvaTimelineModel.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModel.java) with a structured map such as `Map<String, LarvaPlayerPhaseTable>`
- keep the old `groupOverviewLabelMap` only as a temporary fallback or summary line if desired
- let [LarvaTimelineModelBuilder.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModelBuilder.java) translate raw phase stats into render-ready cell strings

### Phase F — render the table under each player header

In [LarvaTimelinePreviewComp.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelinePreviewComp.java):

- keep the player name where it currently renders
- replace the single overview label line with a full-width table block
- place the table immediately below the player header
- start the hatchery rows below the table

Recommended table shape:

- header columns: `Early`, `Mid`, `Late`, `End`
- row labels:
	- `Larva missed`
	- `Inject missed`
	- `Total spawned`
	- `Inject uptime`

Recommended rendering behavior:

- first column reserved for metric labels
- four equal-width value columns spanning the rest of the preview width
- value strings centered in each phase column
- consistent row height and borders so the table stays readable on resize

### Phase G — update diagnostics and golden outputs

Add deterministic phase output to:

- [LarvaTimelineGoldenFormatter.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineGoldenFormatter.java)
- [LarvaVerificationReportFormatter.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaVerificationReportFormatter.java)

Recommended additions:

- per-player phase intervals with loop ranges
- phase promotion trigger loops
- per-phase numerators and denominators
- final rendered cell values
- explicit `n/a` cases for inject uptime where no eligible denominator exists

Also refresh documentation in:

- [LarvaSecondsLostExtMod/README.md](../LarvaSecondsLostExtMod/README.md)
- [LarvaSecondsLostExtMod/docs/validation-checklist.md](../LarvaSecondsLostExtMod/docs/validation-checklist.md)

## Likely files to touch

### Analysis layer

- [LarvaReplayAnalyzer.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java)
- [LarvaAnalysisReport.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java)
- [LarvaPlayerResourceSnapshot.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaPlayerResourceSnapshot.java) or a new dedicated phase-snapshot class
- possibly [HatcheryLarvaTimeline.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/HatcheryLarvaTimeline.java) if assigned larva birth loops need to be exposed more explicitly
- [HatcheryInjectTimeline.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/HatcheryInjectTimeline.java)
- [HatcheryIdleInjectTimeline.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/HatcheryIdleInjectTimeline.java)

### Presentation layer

- [LarvaTimelineModelBuilder.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModelBuilder.java)
- [LarvaTimelineModel.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModel.java)
- [LarvaTimelinePreviewComp.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelinePreviewComp.java)
- [LarvaReplayPageComp.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayPageComp.java)

### Validation and docs

- [LarvaTimelineGoldenFormatter.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineGoldenFormatter.java)
- [LarvaVerificationReportFormatter.java](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaVerificationReportFormatter.java)
- [LarvaSecondsLostExtMod/docs/validation-checklist.md](../LarvaSecondsLostExtMod/docs/validation-checklist.md)
- [LarvaSecondsLostExtMod/README.md](../LarvaSecondsLostExtMod/README.md)

## Key risks and mitigations

### Risk 1 — worker count may not perfectly equal “simultaneously alive drones”

`IPlayerStatsEvent.getWorkersActiveCount()` is the best currently available supported signal, but it must be validated against actual Zerg worker state in replay fixtures.

Mitigation:

- add worker-count snapshots and promotion loops to deterministic diagnostics
- validate several known Zerg replays manually at `36`, `66`, and `90` thresholds

### Risk 2 — tracker snapshots are sparse

The spec requires “more than 30 seconds”, but player-stats snapshots are not continuous.

Mitigation:

- compute dwell over snapshot-to-snapshot loop intervals
- log candidate threshold start loops and reset points in diagnostics
- avoid point-sample assumptions

### Risk 3 — Spawning Pool completion may be missing or ambiguous in some replays

Inject uptime depends on a global player unlock baseline.

Mitigation:

- explicitly track Spawning Pool completion in the analyzer
- if no valid pool completion exists, show inject uptime as `n/a` and log why

### Risk 4 — total larva spawned may exclude unassigned larva

The current analyzer intentionally leaves ambiguous larva unassigned.

Mitigation:

- phase stats should only count assigned larva births
- diagnostics should report excluded ambiguous/unassigned births per player if possible

### Risk 5 — the current overview area is string-based

Encoding a table into a single summary string would become brittle quickly.

Mitigation:

- add a structured table model now
- keep strings only for fallback text or dump summaries

### Risk 6 — UI height and readability regressions

The preview component currently assumes one small overview block under the player name.

Mitigation:

- make table height explicit in preferred-size calculations
- validate resize behavior and multi-player replays with many hatchery rows

## Validation strategy

### Phase boundary validation

Prepare or reuse fixture replays that cover:

- player never reaches `MID`
- player reaches `MID` only
- player reaches `MID` and `LATE`
- player reaches `END`
- player hovers around `36`, `66`, or `90` before stabilizing

For each replay verify:

- candidate promotion threshold starts
- reset when the worker count dips before 30 seconds completes
- confirmed promotion loop after full dwell
- no downgrade after later worker losses

### Timing validation

Confirm that all new calculations remain loop-based:

- phase intervals stored in loops
- hatchery denominators accumulated in loops
- inject denominators accumulated in loops
- final formatting only at the UI boundary

This must match [knowhow/time-management.md](../knowhow/time-management.md).

### Metric validation

For at least one replay per category, manually verify:

- missed-larva thresholds counted in the correct phase
- missed-inject thresholds counted in the correct phase
- total larva births counted in the correct phase
- inject uptime numerator and denominator on one known hatchery after pool unlock

### Output validation

Extend golden output to include:

- phase loop ranges per player
- per-phase hatch loops
- per-phase inject-eligible loops
- per-phase inject-active loops
- per-phase larva missed count
- per-phase inject-missed larva count
- per-phase spawned larva count
- rendered table values

### Manual UI validation

Verify on the `Larva` page that:

- the table appears under each player name
- the table spans the full available width
- hatchery rows remain readable below it
- table values remain stable across resize and replay reload
- tooltips and legend still behave normally

## Recommended implementation order

1. define immutable phase models
2. add worker-count and Spawning Pool tracking to the analyzer
3. implement player phase interval resolution
4. implement per-phase metric aggregation
5. extend `LarvaAnalysisReport` to carry the new phase data
6. extend `LarvaTimelineModel` and `LarvaTimelineModelBuilder` with a structured table model
7. render the table in `LarvaTimelinePreviewComp`
8. update summary text and help text if needed
9. extend golden output and verification reports
10. validate with fixture replays and run `ant BUILD_RELEASE`

## Acceptance criteria

- each player header on the `Larva` page shows a `4 x 4` phase table under the player name
- phase transitions are derived from worker-count dwell in replay loops, not milliseconds
- `EARLY`, `MID`, `LATE`, and `END` are monotonic and deterministic
- larva missed per phase is shown as larva per hatch per minute
- injects missed per phase is shown as larva per hatch per minute
- total larva spawned per phase is shown as larva per hatch per minute
- inject uptime is shown as a percentage or `n/a` when no eligible denominator exists
- all denominators use phase-limited hatchery eligibility, not full replay length
- inject uptime starts at `max(pool completion + 36 gameplay seconds, hatch completion)`
- all calculations remain loop-authoritative and only render gameplay-time formatting at the UI edge
- deterministic golden output includes phase ranges and phase metrics for replay-fixture validation

## Non-goals for this epic

Keep these out of scope unless explicitly requested later:

- cross-replay history pages or player trends across many matches
- native Scelight chart registration
- replacing the supported module-owned Larva page with internal Scelight UI hooks
- new chart lanes for phase visualization inside hatchery rows
- reversible live phase bucketing that changes retroactively when worker counts fall

## Final recommendation

Treat this as an analyzer-first feature.

The safest path is:

- resolve player phase intervals in the replay analyzer
- aggregate phase metrics into immutable report models
- build a structured per-player table in the presentation model
- render that table under the existing player header area

That preserves the current architecture, keeps timing correct, and gives Epic 12 a deterministic validation surface.

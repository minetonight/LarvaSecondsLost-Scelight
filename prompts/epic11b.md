# Epic 11B — inferred inject reconstruction refactor plan

## Goal

Refactor the partially implemented inject analysis so the green inject lane no longer depends on replay-exposed `SpawnLarva` command visibility.

Instead, infer inject completion from replay-derived larva births:

- do not track inject events directly for the green lane
- detect moments where `3` larva spawn to the same hatchery within `8` game loops
- retroactively create a `29` second inject-active window from that burst
- keep the current dark-red missed-inject logic unchanged

This is a behavior change plus a cleanup pass. The current code still reflects the older command-based approach and should be generalized before swapping the implementation.

## Scope boundary

This change applies only to green inject reconstruction.

Keep these intact:

- red `3+ larva` windows
- black `11` second missed-larva ticks on the main rail
- dark-red idle-inject windows
- black `29` second missed-inject ticks on the dark-red lane
- `SpawnLarva` use as an optional larva-to-hatchery assignment heuristic

## Current implementation to replace

The current green inject lane is command-based:

- [inject timeline reconstruction](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java#L1091-L1247)
- [inject window model](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/HatcheryInjectWindow.java)
- [inject timeline model](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/HatcheryInjectTimeline.java)
- [timeline segment assembly](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModelBuilder.java#L347-L358)
- [page copy and legend](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayPageComp.java#L229-L235)
- [inject diagnostics text](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java#L484-L497)

The command collector in [SpawnLarvaCorrelator.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/SpawnLarvaCorrelator.java) should no longer be the source of truth for green inject windows.

## Target behavior

### Green inject lane

For each hatchery:

1. collect replay-derived larva births already assigned to that hatchery
2. sort them chronologically
3. detect each burst where `3` births occur within `8` game loops
4. treat that burst as inferred inject completion evidence
5. create a `29` second inject-active window retroactively from that burst
6. clip the window to hatchery completion, destruction, and replay end
7. merge or deduplicate overlapping evidence so one inject does not create duplicate green windows

### Dark-red idle inject lane

Leave unchanged.

The current queen-evidence-based logic remains the supported implementation path for missed inject potential.

## Refactor plan

### Phase A — generalize the inject data model

The current inject model still assumes direct command evidence.

Refactor `HatcheryInjectWindow` so it represents inject evidence, not specifically command events.

Recommended changes:

- rename `commandLoop` to `evidenceLoop`
- rename `commandTimeLabel` to `evidenceTimeLabel`
- add an evidence descriptor such as `evidenceKind`
- update diagnostics and tooltip wording from `Command:` to `Inference:` or `Detected burst:`

This allows the UI and diagnostics to survive the behavior change cleanly.

### Phase B — preserve assigned larva birth evidence per hatchery

The analyzer already assigns larva births during replay analysis in [LarvaReplayAnalyzer.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java#L221-L293).

Extend hatchery analysis state so each hatchery retains a deterministic list of assigned larva birth loops.

Do not rely only on `countPointList`; it is useful for state visualization but not ideal for exact burst detection.

Preferred shape:

- one per-hatchery chronological list of assigned larva birth loops
- optional formatted time labels only when needed for diagnostics

### Phase C — add an inferred inject detector

Create a dedicated component, for example:

- `InferredInjectDetector`, or
- `LarvaInjectBurstDetector`

Input:

- hatchery lifetime bounds
- assigned larva birth loops for one hatchery
- replay timing conversion

Output:

- normalized inferred inject windows
- dedupe or overlap diagnostics
- counts for kept, merged, and discarded evidence

Detection rule:

- if three births land within an `8` loop span, emit one inject evidence point at the burst completion loop

Window rule:

- convert that evidence into a `29` second green window ending at the burst completion event and extending backward by the inject-active duration

Normalization rule:

- trim to hatchery completion
- trim to hatchery destruction
- trim to replay end
- merge or discard overlapping windows deterministically

### Phase D — replace command-based inject reconstruction

Replace the current `buildInjectTimelines()` flow in [LarvaReplayAnalyzer.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayAnalyzer.java#L1091-L1247).

The new source for green windows should be inferred burst evidence, not `SpawnLarva` command targets.

The old command-based builder should either:

- be removed, or
- be reduced to a helper only used elsewhere for larva assignment heuristics

### Phase E — keep `SpawnLarva` only where still useful

The correlator in [SpawnLarvaCorrelator.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/SpawnLarvaCorrelator.java) is still useful for larva-to-hatchery assignment.

Recommended default:

- keep it for assignment confidence and birth attribution
- remove it from green inject-window reconstruction

### Phase F — update UI text and diagnostics

Update all wording that still claims green windows come from `SpawnLarva` commands.

Required updates:

- [LarvaReplayPageComp.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayPageComp.java#L229-L235)
- [LarvaReplayPageComp.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaReplayPageComp.java#L299-L302)
- [LarvaAnalysisReport.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaAnalysisReport.java#L484-L497)
- [ReplaySummaryService.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/ReplaySummaryService.java#L88-L96)
- tooltip text in [LarvaTimelineModelBuilder.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineModelBuilder.java#L347-L358)
- deterministic fixture output in [LarvaTimelineGoldenFormatter.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineGoldenFormatter.java)

### Phase G — validation and golden refresh

Use the existing deterministic validation path:

- [DevDiagnosticDumpWriter.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/DevDiagnosticDumpWriter.java)
- [LarvaTimelineGoldenFormatter.java](LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaTimelineGoldenFormatter.java)
- fixture references under [LarvaSecondsLostExtMod/docs/epic08-fixtures](LarvaSecondsLostExtMod/docs/epic08-fixtures)

Refresh or add scenarios for:

- one clean inferred inject burst
- two nearby bursts that should collapse to one effective inject window
- burst near hatchery completion
- burst near destruction
- burst near replay end
- ambiguous larva assignment that must not fabricate a green window
- replays where public command exposure misses queued or update target events, but larva births still prove the inject

## Acceptance criteria

- green inject lanes no longer depend on replay `SpawnLarva` command visibility
- `3` births within `8` loops on one hatchery produce one inferred inject evidence point
- that evidence creates one retroactive `29` second green inject window
- hatchery lifetime and replay bounds clip the window deterministically
- overlapping inferred windows are normalized deterministically
- dark-red idle inject windows remain unchanged
- missed-inject accumulation remains unchanged
- diagnostics, hover text, and golden outputs describe inferred injects instead of command-target reconstruction

## Recommended implementation order

1. generalize inject model names
2. record assigned larva birth evidence per hatchery
3. build the inferred burst detector
4. replace green inject reconstruction with inferred windows
5. update tooltips, legend text, and diagnostics
6. refresh golden outputs and validate with fixture dumps
7. run `ant BUILD_RELEASE`

## Important assumption

This plan removes command-based tracking only for the green inject lane.

It does **not** remove `SpawnLarva` from larva-assignment heuristics unless a wider replay-analysis cleanup is explicitly requested later.

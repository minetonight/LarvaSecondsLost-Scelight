# Time management for Larva Seconds Lost

## Core rule

Use raw replay loops as the source of truth for all larva analysis.

That means:
- hatchery timelines stay in loops
- 3+ larva windows stay in loops
- missed-larva thresholds stay in loops
- replay end should come from replay/header elapsed loops

Do **not** derive gameplay loops back from displayed milliseconds.

## Why the old approach was wrong

Scelight can display replay time in game time or real time, and that conversion depends on replay speed.

A bad flow looked like this:
1. take replay length from `RepProcessor.getLengthMs()`
2. treat that value like raw game-time milliseconds
3. convert it back to loops with a fixed `16 loops/sec`

That truncates Faster replays because the displayed milliseconds are already speed-adjusted.

Practical symptom:
- charts end about 20-30% early
- the cutoff closely matches Faster speed scaling

## Correct conversion model

### Calculation layer

Keep calculations in loops.

Examples:
- saturation windows are built from count-point loops
- missed-larva markers are created from loop distances
- replay end visibility uses `replayLengthLoops`

### Display layer

Convert loops to visible time only when rendering labels, tooltips, or dumps.

For the Larva module, visible chart time should use **gameplay time**, not the replay page's optional
real-time mode. The larva mechanic itself is gameplay-timed, so the chart must show the same elapsed
duration that the 11-second threshold uses.

Use this conversion:
- base loop to gameplay milliseconds: `(gameloop * 125L) / 2`

Do not compress those labels for Faster real-time display, or the visible missed-larva spacing will
shrink to about 7.9 seconds instead of 11 seconds.

In this module that conversion is centralized in `LarvaAnalysisReport`.

## Rules for each UI element

### Chart ranges

- row start/end logic uses loop values
- window start/end logic uses loop values
- visible replay end uses replay header elapsed loops

### Marker thresholds

The missed-larva rule is tied to the visible in-game timer pace, and Blizzard keeps that timer pace
linked to game speed.

Implementation rule:
- start from the base `11` visible in-game seconds
- convert that threshold into loops using replay speed
- Scelight relative speeds are `36` for Normal and `26` for Faster
- threshold formula: `11 * 16 * (36 / gameSpeedRelative)`
- compare loop deltas against that threshold
- only format human-readable time at render time

Examples:
- Faster: `11 * 16 * (36 / 26) ≈ 243` loops
- Normal: `11 * 16 * (36 / 36) = 176` loops
- Slower speeds use fewer loops per visible timer second, so the visible time between missed-larva
	markers stretches accordingly

### Tooltips

Tooltips should use higher precision than row labels.

Rule:
- normal labels can stay at whole seconds for readability
- tooltips should format loop-derived time to tenths of a second

### Resource snapshots

Resource snapshots come from replay tracker `PlayerStats` events, so they are sparse by nature.

Rule:
- if the latest past snapshot is older than 10 seconds, show `Unknown at this timestamp`
- if an upcoming snapshot is within 10 seconds, prefer it and mark it as near-future
- snapshot timestamps must be formatted with the same gameplay-time conversion as windows and markers

## Implementation checklist

When touching timing code, verify these questions:

1. Is this value a raw replay loop or a displayed time?
2. Am I doing calculations only in loops?
3. Am I formatting time only at the presentation boundary?
4. Am I formatting larva mechanic timings in gameplay time?
5. Am I avoiding fixed `16 loops/sec` reverse conversion from displayed milliseconds?

## Files involved

Main timing owners:
- `LarvaReplayAnalyzer.java`
- `LarvaAnalysisReport.java`
- `LarvaSaturationWindowCalculator.java`
- `LarvaMissedLarvaMarkerCalculator.java`
- `LarvaTimelineModelBuilder.java`

## Short version

- loops are authoritative
- display time is derived from loops
- larva chart labels use gameplay time so 11 gameplay seconds still look like 11 seconds
- replay end comes from replay loops, not converted display milliseconds
- thresholds stay in loops
- tooltips may show tenths, but scoring still uses loop precision

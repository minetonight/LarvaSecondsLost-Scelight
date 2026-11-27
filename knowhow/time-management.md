# Time in Larva Seconds Lost

## The rule (since 2026-11-26)

- **Compute in loops.** Every replay event has a game loop; windows, thresholds, phases and durations stay in loops.
- **Show with the game clock.** Every time and every "per minute" the module shows goes through `LarvaGameClock` (loops / 22.4). A minute is 60 seconds of the game clock, 1344 loops.
- **Nothing else converts.** No `* 16`, `/ 22.4`, 960 or 1344 in other code, and no Scelight time API (`formatLoopTime`, `loopToTime`, `getLengthMs`, `convertToRealTime`, `isRealTime`, `getConverterGameSpeed`). `LarvaTimeBasisGuardTest` scans `src` and fails `ant TEST` when code breaks this.
- **Game data in loops.** A duration from the game data is a loop constant in `LarvaGameClock`, measured on replays; never "seconds × factor".

## The time bases

| base | loops per second | where it comes from | use in the module |
|---|---:|---|---|
| game loop | – | replays record every event in loops | all calculations |
| game clock second | 22.4 | the in-game timer of LotV replays (real time at Faster); Liquipedia's LotV values (hatchery 71 s) | every time and rate shown |
| Normal-speed second | 16 | Blizzard's game data (editor values: hatchery 100, inject 40, energy 0.5625 per second) | only to turn data values into loops (`LOOPS_PER_GAME_DATA_SECOND`) |
| Scelight time | 22.15 (16 × 36/26), or 16 with "use real time" off | Scelight's own pages and `IRepProcessor` time methods | never |

Scelight's factor for Faster is 36/26 = 1.385 where the game uses 1.4, so Scelight shows times about 1.1 % too high (game 7:51 is Scelight 7:56). The other speeds are exact. Scelight's own charts keep their time; the module's differ from them by that 1.1 %.

## Game data in loops (measured on replays)

| what | loops | game clock | Normal-speed seconds |
|---|---:|---:|---:|
| natural larva spawn | 240 | 10.7 s | 15 |
| back-to-back inject pops | 641 | 28.6 s | 40 (+1 loop) |
| queen build | 800 | 35.7 s | 50 |
| hatchery build | 1600 | 71.4 s | 100 |
| spawning pool build | 1040 | 46.4 s | 65 |
| evolution chamber build | 560 | 25.0 s | 35 |
| extractor build | 480 | 21.4 s | 30 |
| tracker PlayerStats interval | 160 | 7.1 s | 10 |
| queen energy | 0.03515625 per loop | 0.7875 per s | 0.5625 per s |

Build times: `UnitInit` to `UnitDone` of the same tag. Inject: gaps between 3-larva bursts of one hatchery (queued injects). Natural larva: gaps between single births.

Hand-picked limits (bridge limit 90 s, command lookback 40 s, tumor windows 30 s and 60 s, minimum game length 4 min) are game clock seconds, converted with `LarvaGameClock.secondsToLoops`.

## What went wrong before

Until 2026-11-26 the module mixed three bases:

- Chart labels used "gameplay time" (loops / 16, Normal-speed seconds) while other labels used Scelight's time, which followed its real-time setting.
- Thresholds were built as "11 visible seconds × 16 × 36/26" (243 loops; the real larva interval is 240) and the inject as 29 × 16 × 36/26 (642; real 641). The game speed factor was applied to durations that do not depend on game speed.
- "Per minute" rates were per 960 loops (60 Normal-speed seconds = 42.9 s of game clock), so they were 1/1.4 of per-minute rates. Old exports (April 2026 percentiles, the spreadsheet `benchmark-data/scelight mod - larva efficiency calculations.ods`, the E19 band notes) use that unit: multiply by 1.4 to compare.
- An even older bug: replay length came from Scelight's displayed milliseconds and was turned back into loops with 16 loops per second, so charts ended 20–30 % early. Replay end comes from the header's elapsed loops.

## Rules for each element

- Chart rows, windows and the replay end use loops (`replayLengthLoops` from the header).
- Labels are `m:ss` (`LarvaGameClock.format`); tooltips may show tenths (`formatTenths`). Scoring keeps loop precision.
- Resource snapshots (tracker `PlayerStats`) are sparse: a snapshot counts for a point within one PlayerStats interval (160 loops).
- Python tools never convert loops: they take rates and times from the Java CLIs.

## Checklist

1. Is this value a loop or a shown time?
2. Is the calculation in loops?
3. Does the conversion go through `LarvaGameClock`?
4. Is a game-data duration a measured loop constant, not "seconds × factor"?
5. Does `ant TEST` (with `LarvaTimeBasisGuardTest`) pass?

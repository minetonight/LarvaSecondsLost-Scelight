# Epic 17 / E17-S02 spike: batch parse cost, cheap metadata, threading model

Measured on 2026-11-24 on this machine (Ubuntu, 20 logical cores, 32 GB, NVMe), Java 8 (`/usr/lib/jvm/java-8-openjdk-amd64`), Scelight 6.5.4, module built against SDK 1.5.2. Replays: 460 files for the headless runs (120 stratified from `benchmark-data/raw-replays/bucketed-mmr`, 300 random plus the 40 largest files from the user's `Accounts` folder, read only), 360 files and then the whole Accounts library (8 352 files) inside the real Scelight.

## Result: GO. Single worker, pre-filter yes, gate the debug side effects

| Question | Answer in one line |
|---|---|
| Full path per replay (Q1) | mean 17 ms, median 10 ms, p90 40 ms, max 190 ms. **About 90 times faster than the 1.5 s budget** |
| Cheap metadata (Q2) | **Available and identical** to the full parse for every field, but only through the cached `getRepProc` (inside Scelight), and only when the replay is in Scelight's cache |
| Cheap versus full cost (Q3) | cache hit: 0.2 to 0.9 ms. Full parse: 8 to 12 ms. Whole analysis: 17 ms |
| Memory (Q4) | One page summary retains about 0.27 MB. Compact records are about 1 KB each. Live heap after 460 sequential analyses: 11 to 15 MB |
| Reuse and threads (Q5) | Sequential reuse is safe. 2 and 4 threads are safe and byte-identical, in the headless engine and in the real Scelight engine |
| Debug side effects (Q6) | Not measurable (< 1 %). Gate them anyway (R-6): file growth and log noise |
| **Threading decision** | **1 worker** (mean is far below 1.5 s). 2 workers are verified safe and are a one-constant upgrade |
| `ESTIMATED_MS_PER_REPLAY` | **50** |

## Method and exact command lines

Harness: [epic17-batch-spike/BatchSpike.java.txt](epic17-batch-spike/BatchSpike.java.txt) (throwaway, kept as `.txt` so Ant does not compile it; copy it to a scratch folder as `BatchSpike.java`). It is a small `main` that uses the existing public classes (`StandaloneScelightBootstrap`, `ReflectiveScelightFactory`, `ReflectiveScelightRepParserEngine`, `ReplaySummaryService`, `LarvaBenchmarkJsonSerializer`) exactly like `LarvaBenchmarkCliExtractor` does, and runs N worker threads with **one `ReplaySummaryService` per worker**. Modes: `full` (`analyzeBenchmarkReplay`), `page` (`analyze`, builds the timeline model too), `parseonly` (engine `getRepProc` only), `cheap` (metadata from a Scelight `RepProcessor` built with an empty content set). Classpath helper: [epic17-batch-spike/env.sh.txt](epic17-batch-spike/env.sh.txt).

```bash
# classpath: module bin + SDK + all Scelight .sldat jars (app, app-libs, launcher, sc2-t, sc2-bd) + module lib/
source env.sh        # defines $M $S $J8 $CPLIBS $SP, see env.sh.txt
javac -nowarn -source 1.7 -target 1.7 -cp "$M/bin:$CPLIBS" -d classes BatchSpike.java

# lists (fixed seed, so the runs are repeatable)
find "/home/aleks/Documents/StarCraft II/Accounts" -name '*.SC2Replay' | sort | shuf --random-source=<(yes) > all-accounts-shuffled.txt
head -300 all-accounts-shuffled.txt > list-accounts300.txt
# 120 = every 10th (offset 3) file of each of the 12 bucketed-mmr/mmr-*/Zv* folders, first 10 per folder
# 40 largest Accounts files: xargs -d '\n' -a all-accounts-shuffled.txt stat -c '%s %n' | sort -rn | head -40
# list-all.txt = bucketed120 + accounts300 + largest40, de-duplicated, 460 files

# headless runs (stop Scelight first, see "Surprises")
$J8/java -Xmx1024m -Dscelight.dir="$S" -cp "classes:$M/bin:$CPLIBS" BatchSpike full list-all.txt <threads> out/dir
$J8/java -Xmx1024m -Dscelight.dir="$S" -cp "classes:$M/bin:$CPLIBS" BatchSpike cheap list-accounts300.txt 1 out/dir
```

Inside Scelight (needs the cache-only path), via the dev command channel (see [running-scelight.md](running-scelight.md)); the commands live in `LarvaSpikeBatchCommands.java` (throwaway, registered in `LarvaDevBuiltinCommands`):

```
<id> spike-batch-meta <listFile> <outTsv>                          # getRepProc (cache) versus parseAndWrapReplay, same fields, with cache hit flag
<id> spike-batch-full <listFile> <threads> <outJsonl> [prefilter]   # analyzeBenchmarkReplay, one ReplaySummaryService per worker
```

Scelight was started and stopped with the documented commands (`ant INSTALL_DEPLOYMENT`, start command, `exit` command) and is **stopped** now.

## Q1: full per-replay time (`getRepProc` + reparse + `LarvaReplayAnalyzer.analyze`)

All times are wall milliseconds per replay on one thread. "Excl. first" removes the very first replay of the JVM (class loading and JIT).

| Run | Files | OK | Mean | Median | p90 | p99 | Max | Notes |
|---|---|---|---|---|---|---|---|---|
| Headless, `full`, 460 files, excl. first | 450 | 450 | 16.8 | 10.6 | 38.7 | 90.4 | 188.7 | CPU time per replay is 16.5 ms: single threaded, no waiting |
| Headless, same, including first | 460 | 451 | 19 to 20 | 10 to 11 | 35 to 42 | | 910 to 1 127 | The first replay costs about 0.9 to 1.1 s once per JVM |
| Headless, `page` (adds timeline model) | 450 | 450 | 16.8 | 10.7 | 37.9 | | 175.7 | Building the timeline model is free |
| Headless, `parseonly` | 450 | 450 | 12.6 | 7.9 | 29.2 | | 153.6 | **Parsing is 75 % of the cost**, analysis about 4 ms |
| In Scelight, 360 files (cache hits, so cheap call + reparse + analyze) | 351 | 351 | 16.1 to 17.4 | 9.6 to 10.6 | 39.5 to 41.7 | | 181 to 188 | Warm JVM: no first-replay penalty. Two runs |
| In Scelight, whole library, 8 352 files | 8 281 | 8 281 | 10.2 | 8.5 | 18.9 | | 174.6 | Library average is smaller because most of it is short games |
| In Scelight, 100 files never seen by Scelight (cache **miss**) | 86 | 86 | 46 to 50 | 32 to 36 | 82 to 88 | | 320 to 426 | Larger pro replays (categorized/internet-sample). Miss = full parse + cache write |

Answer: **yes, far within 1.5 s**. The mean is about 17 ms, the worst replay in 8 352 took 175 ms. The 1 s cost of the first analysis in a fresh JVM does not apply inside Scelight (already warm) but a cold first batch might pay a few hundred ms for the analyzer classes.

Data volume that matters: whole library without pre-filter 85.4 s (8 352 files, 71 unparseable), with pre-filter 41.8 s (see Q3).

Failures: 9 of 460 files (all large custom games: 3 to 6 players, Archon) throw inside Scelight's `RepProcessor` (`ArrayIndexOutOfBoundsException: 3` in `trackPlayerLeave`, or `NullPointerException`). Inside Scelight `getRepProc` catches this and returns `null`, so `ReplaySummaryService` throws `IllegalStateException("Scelight could not parse the replay file.")`. In the whole library 71 of 8 352 files (0.85 %) fail this way. S12 must treat that as a per-file failure (OQ-15) and keep going. The pre-filter removes them without an error because `getRepProc` returns `null`, which the filter treats as "not eligible"; **decide in S12 whether a `null` processor is listed as a parse failure or silently excluded** (my measurement counted it as excluded).

## Q2: metadata availability matrix (cheap cache-only processor versus full parse)

"Cheap" is `repParserEngine.getRepProc(path)` for a replay that is in Scelight's replay-processor cache. That processor is built from the cache entry (header, details, init data, attributes, message events plus per-user derived data) and has **no** tracker events and **no** game events (`getReplay().getTrackerEvents() == null`). "Full" is `parseAndWrapReplay(path)`.

Compared fields, string-for-string, across the cheap and the full processor: 8 281 cache hits in the whole library, 351 in the 360-file set, 80 in the large-replay set. **Every field was identical in all 8 712 comparisons.**

| Field | Getter | Cheap (cache hit) | Evidence |
|---|---|---|---|
| Format | `IRepProcessor.getFormat()` | **available, identical** | 1v1 / 2v2 / FFA / Archon / etc. Archon detection works from lobby slots, no events needed |
| Game mode | `getReplay().getAttributesEvents().getGameMode()` | **available, identical** | `IGameMode.AMM` (`toString()` "AutoMM") is the ladder mode. Others: Private, Public, Single Player, Unknown |
| Date | `getReplay().getDetails().getTime()` | **available, identical** | Never null in 8 352 replays. It is the replay end time as `java.util.Date` (compared as epoch ms) |
| Players, races | `getReplay().getDetails().getPlayerList()[i].getRace()` | **available, identical** | Compare to `IRace.ZERG` (identity) |
| Players, toons | `getPlayerList()[i].getToon()` | **available, identical** | Build the identity key from `getRegionId()`, `getProgramId()`, `getRealmId()`, `getId()` (C-10). Computer players have zero toons. Human toon is always non-null in the sample |
| Length in loops | `getReplay().getHeader().getElapsedGameLoops()` (`Integer`) | **available, identical** | This is the value to compare to 5 376. `IRepProcessor` itself only has `getLengthMs()` (also identical, but it is converted to ms with the real-time setting, so do not use it for the 5 376 test) |
| Length in ms | `getLengthMs()` | available, identical | Setting dependent |
| Result / winners | `getPlayers…`, `getWinnersString()` | available, identical | Not needed for the filter |
| Tracker and game events | `getReplay().getTrackerEvents()/getGameEvents()` | **missing by design** | `null` on a cache hit: this is how S12 and S07 detect a cache-only processor (`ReplaySummaryService` already does) |

Important behaviors of the cheap call:

1. **Cache hit** (the replay was seen by Scelight's crawler before, which is the normal case for any replay in a monitored folder): cheap processor, no events, 0.2 to 0.9 ms.
2. **Cache miss** (replay never seen by Scelight, e.g. a freshly copied file in an unmonitored folder, or a brand new replay the crawler has not reached yet): `getRepProc` does a **full parse**, writes the cache entry and returns a processor **with events**. Cost 32 to 36 ms median (46 to 50 ms mean). The metadata is still correct, but there is no saving from the pre-filter, and the module then does not need `parseAndWrapReplay` (events already present).
3. `getRepProc` returns `null` for unparseable replays (71 of 8 352).
4. In the **headless** engine (`ReflectiveScelightRepParserEngine`) `getRepProc` always builds a full processor and bypasses the cache, so the cheap path cannot be measured headless. A header-only `new RepProcessor(path, RepParserEngine.EMPTY_CONTENT_SET)` (needs Scelight classes, not in the SDK) is the headless stand-in: 0.9 ms mean, 0.8 ms median, versus 9.1 ms for the full parse in the same process, same field values (299 of 299 identical).

**Verdict: cheap metadata available.** (Outcome row 1.)

## Q3: cheap versus full call time, and the pre-filter saving

Predicate measured (the S12 pre-filter): `getFormat() == ONE_VS_ONE && gameMode == AMM && headerElapsedGameLoops >= 5376 && at least one Zerg player`, and `getRepProc != null`.

| Metric (whole library 8 352 files, in Scelight, 1 worker) | Value |
|---|---|
| Cheap call, cache hit: mean / median / p90 / max | 0.86 / 0.70 / 1.7 / 23 ms (first pass over the folder, files not yet in the OS cache). Warm: 0.19 ms mean |
| Full parse (`parseAndWrapReplay`): mean / median / p90 / max | 7.9 / 6.5 / 14.7 / 125 ms |
| Not AutoMM | 4 004 (48 %) |
| AutoMM, not 1v1 | 159 |
| AutoMM 1v1 shorter than 5 376 loops | 436 |
| AutoMM 1v1 long, no Zerg | 58 |
| Unparseable (`getRepProc` null) | 71 |
| **Pass the filter** | **3 624 (43 %)** |
| Total time **without** pre-filter | 85.4 s |
| Total time **with** pre-filter | 41.8 s (skipped 4 728 files at a median of 0.19 ms each) |
| **Saving** | **51 % of the time** and all 71 parse errors |

Cost for a replay that passes: the cheap call is extra work (0.2 to 0.9 ms, about 2 to 5 % of the 17 ms analysis), so the filter pays off as soon as more than a few percent of the files are rejected. Mixed selections (the user's own folder) reject more than half.

Caveat for S12: the user-selected files in the chooser may include replays Scelight has not cached yet. They cost a full parse in the cheap call (about 40 ms) and are then analyzed without a second parse, so the total is still the same as the no-filter path.

## Q4: memory

| Measurement | Result |
|---|---|
| Heap retained per `LarvaReplayPageSummary` (30 retained, `System.gc()` before and after) | about **0.27 MB** each (8 MB for 30). It holds the `LarvaAnalysisReport` and the timeline model |
| Compact record, `LarvaBenchmarkReplayRecord` as JSON | about 0.8 KB per replay (356 KB for 120 replays, 1.4 MB for 451). Object form is a few KB, so 500 records are far below 10 MB |
| Live heap after 460 sequential analyses with all JSON strings retained, after `System.gc()` | 11 to 15 MB |
| Largest heap seen after any young or full GC (`-Xloggc`, `-Xmx1024m`, 460 files) | 77 MB (floating garbage, not live data) |
| Minimum `-Xmx` that completes the 460-file run without extra failures (headless, includes the Scelight statics) | 1 worker: **128 MB** (GC 3.9 s, slow). 2 workers: 256 MB. 4 workers: 256 MB. At 128 MB with 2 workers 2 extra files failed (GC thrashing, 16 s of GC) |
| Scelight's `-Xmx1024m` | fine for 4 workers (GC time 0.7 s of 4.5 s) |

Answer: 100 sequential analyses with compact records only: peak heap use is dominated by garbage, live data stays under 20 MB. The 500-replay peak growth criterion of S16 (≤ 200 MB) is met by a very large margin. R-4 (switch to streaming accumulation) is **not needed**.

## Q5: reuse and thread safety

Code review (`ReplaySummaryService`, `LarvaReplayAnalyzer`):
- `LarvaReplayAnalyzer` has only final fields set once (`factory`, `SpawnLarvaCorrelator`, `InferredInjectDetector`, `LarvaSaturationWindowCalculator`, `LarvaMissedLarvaMarkerCalculator`); all analysis state is local.
- `ReplaySummaryService` has a `SimpleDateFormat` field (`dateFormat`), which is **not thread-safe**, so one service per thread is required (as the stories already say).

Measured:
- **Sequential reuse:** one service instance over 460 files in forward order and in reversed order gives byte-identical JSON per file (451 of 451, compared as sets). No state leaks between replays.
- **Headless, 2 and 4 threads** (one service per worker, shared stateless engine) versus 1 thread on the 460 files, 3 repetitions: the 451 successful records are **byte-identical** in every run. The only differences are the exception text of the 3 failing replays (JIT fast-throw drops the message `: 3`), which is not data.
- **In Scelight** (real static `RepParserEngine`, real `RepProcCache`, cache hits): `spike-batch-full` with 1, 2 and 4 threads over 360 files, JSON **byte-identical** (`cmp`).
- **In Scelight with cache misses** (concurrent `RepProcCache.putRepProc` from 2 workers, 86 never-seen files): no errors, and all records equal a later sequential run on the now-cached files, except the two fields that report the path taken (`fullReplayParseUsed`, and the `full-replay-reparse` flag).
- Headless and in-Scelight records differ only in `players` and `winners` strings (125 of 351 files; Scelight's favored-player settings affect the text) and in the path flags. All metric fields are identical, so the headless CLI remains a valid test oracle for the numbers.

Answer: `LarvaReplayAnalyzer` can be reused sequentially. `IRepParserEngine` / the Scelight parser is **safe with 2 threads** (and 4), provided there is one `ReplaySummaryService` per worker.

Scaling (460 files, headless, 3 repetitions each, 20-core machine): 1 thread 8.7 to 9.1 s (51 files/s); 2 threads 5.6 s (82 files/s, x1.6); 4 threads 4.5 s (102 files/s, x2.0, per-replay latency doubles and GC time triples). In Scelight (360 files): 1 thread 5.8 to 6.3 s, 2 threads 3.1 s, 4 threads 2.1 s. For 100 cache-missing large replays 2 threads: 2.2 s versus 3.9 s.

## Q6: debug side effects

| Side effect | Measurement | Verdict |
|---|---|---|
| `/tmp/larva-pos2jl.log` (`LarvaReplayAnalyzer.writePos2jlLog`, opens and closes the file per line, only for the hatchery tagged "2jl") | 4 runs on, 4 runs with the path symlinked to `/dev/null`, 4 runs with the open failing: totals 8.8 to 9.1 s in all three variants. **No measurable difference (< 1 %)** | Cost is nil, but the file grows without limit: 1 056 lines, 56 KB per 460 replays (about 2.3 lines per replay). It is pure debug output and appends forever |
| `logSpawnLarvaCommands` and the analysis summary `logger.debug` | `StdErrLogger` debug off versus on (stderr to a file): 8.8 to 8.9 s versus 8.9 s. 21.8 debug lines per replay (9 823 lines for 451 replays, 940 KB). In Scelight each line goes to the Scelight log file | **No measurable CPU cost** (< 2 %). Measured only the logger being on or off; the string building stays in the "off" case, which is the case in production code today |

Both are far below the 5 % threshold. **Decision: gate them anyway (R-6)**, because a 500-file batch would append about 1 000 lines to `/tmp` and write about 10 000 debug lines (about 1 MB) into the Scelight log for no benefit, and `/tmp/larva-pos2jl.log` is a hard-coded Unix path. S12 keeps the gating as written (behind the dev flag), single-page outputs stay unchanged.

## Decision: threading model

**1 worker.** Rationale:
- The Mean (17 ms; 5 ms per file with the pre-filter on a mixed library) is about 90 times under the 1.5 s trigger for a pool. 500 replays take about 8 s warm, about 25 s in the all-cache-miss worst case.
- A pool of 2 is verified safe (identical output, also with concurrent cache writes) and gives x1.6 to x1.8, but costs more memory (256 MB minimum versus 128 MB headless), doubles the GC pressure inside a Scelight JVM that also runs the UI, and requires merging results in selection order. Not worth it at these speeds.
- Make the worker count a named constant (`BATCH_WORKER_COUNT = 1`) in the runner so that raising it to 2 later is a one-line change. Rules that stay valid for any count: one `ReplaySummaryService` per worker, results merged in selection order.
- S07 shares the S06 FIFO worker, so the auto-open pre-check and analysis also never run concurrently with themselves.
- Run the worker below normal priority (`Thread.NORM_PRIORITY - 1`) so the EDT stays responsive. UI responsiveness during a run was **not** measured (the commands run on the channel thread, not next to a user driving the UI).

## `ESTIMATED_MS_PER_REPLAY` (OQ-14 confirm dialog)

**Recommended constant: `ESTIMATED_MS_PER_REPLAY = 50`** (named constant, single worker, no division).

| Basis | ms per replay |
|---|---|
| Median analysis, cache hit | 10 |
| Mean analysis, cache hit, Zerg ladder games | 17 |
| Mean with pre-filter on the whole user library | 5 |
| Mean, cache miss, large pro replays | 46 to 50 |
| p99 / max | 90 / 190 (up to 426 for cold large replays) |

50 covers the miss case and leaves margin. Example dialogs: 501 files: "about 25 seconds"; 5 000 files: "about 4 minutes" (real: about 25 to 50 s). Round the estimate to whole seconds below 2 minutes and to minutes above, and say "at most" if wording allows. OQ-14's 500-file confirm threshold is still decided by the user; at these speeds 500 files take well under 30 s even in the worst case, so the dialog is a courtesy, not protection against long hangs.

## Effect on later stories (outcome rows applied)

| Story | Outcome row | Change |
|---|---|---|
| **S07** auto-open | Cheap metadata available | The Zerg pre-check uses `repParserEngine.getRepProc(path)` on the queue worker: `details.getPlayerList()[i].getRace() == IRace.ZERG`. A brand-new replay is usually **not** in the cache yet (cache miss), so that call is a full parse (about 40 ms) and returns events; follow with `analyze` without a second parse (it does not reparse when events are present). No separate "cheap or full" branch is needed for correctness. Do not use the phase-table map for the Zerg check |
| **S09** `buildMetadataOnly(IRepProcessor)` | Cheap metadata available | Fields as in the Q2 matrix: `getFormat()`, `getGameMode()`, `getDetails().getTime()`, player races and toon key parts, `getHeader().getElapsedGameLoops()`. The record builder may rely on all of them being present on a cache-only processor |
| **S12** runner | Cheap available; mean ≤ 1.5 s and 2 threads safe | Pre-filter **yes**: skip files that fail the predicate (not AutoMM when ladder-only is on, not 1v1, below 5 376 loops, no Zerg, or `null` processor) before `analyzeForAggregation`. Expected saving about 50 % on a typical mixed library. **1 worker** (`BATCH_WORKER_COUNT = 1`), one service per worker, selection order. **Progress granularity:** per file, but throttled (the S12 rule of at least 100 ms between posts is right: at 5 to 17 ms per file there can be 60 to 200 files per second, so a post per file would flood the EDT). Final post always. Cancel latency: checked before each file, worst case one replay (typically 10 to 40 ms, at most about 0.4 s). **Gate `writePos2jlLog` and `logSpawnLarvaCommands`** (no measurable cost, but unbounded `/tmp` growth and 10 000 log lines per 500 files). A `null` processor from `getRepProc` is a parse failure to be listed (OQ-15), decide whether the pre-filter reports it as such |
| **S14** | OQ-14 | Estimate = N × 50 ms. No worker division with one worker |
| **S16** performance run | §9 re-baselined | The §9 target (100 replays ≤ 2.5 min) is trivially met: expect 100 replays in about 1 to 5 s (about 10 s worst case with cache misses). Suggested re-baselined target for the validation run: **100 replays ≤ 15 s, 500 replays ≤ 60 s, peak heap growth ≤ 200 MB** (measured: under 20 MB live). Keep the 200 MB criterion. A manual check of UI responsiveness during a 500-file run is still needed |
| **R-3** (slow batch) | closed | Not a risk at these speeds. The partial-results contingency is not needed |
| **R-4** (memory) | closed | Compact records are about 1 KB, the streaming contingency is not needed |
| **R-6** (debug logging) | confirmed | Gate as planned. Measured cost is nil |

## Surprises and gotchas

1. **The standalone engine has no cache**: `ReflectiveScelightRepParserEngine.getRepProc` always builds a full `RepProcessor`, so the headless CLI cannot show the cache-only behavior. That is why the in-Scelight spike commands exist.
2. **Do not run the headless harness (or the CLI) while Scelight is running.** The Scelight `Env` class initializer starts an `InstanceMonitor` thread (single-instance socket). With Scelight running the harness silently produced no output. Conversely, those non-daemon threads keep the headless JVM alive after `main` returns or throws: the harness (like `LarvaBenchmarkCliExtractor`) must call `System.exit`. My first run hung for 7 minutes for that reason.
3. **Parsing, not the larva analysis, is the cost** (75 %). The analyzer itself needs about 4 ms.
4. The 9 (library: 71) unparseable files are all Scelight parser limits (Archon or 3+ player custom games), not module problems. In Scelight they come back as `getRepProc == null`.
5. **A cache miss makes the "cheap" call the expensive one** (full parse plus cache write), but it hands back events, so nothing is parsed twice.
6. `getElapsedGameLoops()` is the header value; `getLengthMs()` depends on the real-time setting. Use the header loops for the 5 376 threshold.
7. The pre-filter, applied to the user's whole library, selects 43 % of the files (3 624 of 8 352): 48 % are not AutoMM games.
8. Running the spike inside Scelight put about 160 benchmark replays (the 100 and 80 "miss" lists) into Scelight's replay-processor cache. Harmless (the earlier convention accepts it). Nothing in `Accounts` was modified.

## Files

- `LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/LarvaSpikeBatchCommands.java` (new, throwaway, Java 7 level) and its one-line registration in `LarvaDevBuiltinCommands.java`. Delete both after S12 is built.
- `knowhow/epic17-batch-spike/BatchSpike.java.txt`, `knowhow/epic17-batch-spike/env.sh.txt` (headless harness).
- No existing metric logic, Epic 13 CLI or benchmark output was changed.

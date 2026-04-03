# Epic 11 — Injection status tracking

## Epic goal

Add replay-derived injection status tracking to the supported module-owned `Larva` timeline so each qualifying hatchery can show:

- a green inject-status lane for periods when the hatchery is actively injected,
- a red idle-inject-queen lane for periods when one or more nearby queens could inject but the hatchery is not injected,
- and separate missed-inject larva accounting derived from 29-second inject windows worth 3 larva each.

This epic exists because the current supported path already proved the module can render replay-derived per-hatchery larva pressure on the separate `Larva` page without unsupported native chart dropdown or Base Control augmentation. The next step is to make inject execution and missed inject opportunities visible on that same supported path.

Epic 11 is intentionally limited to injection-status tracking and missed-inject accounting. It does **not** include later `v3` items such as per-phase stats, player-history tables, or opening replays in separate tree windows.

## Working assumptions confirmed for this epic

- Treat the README handoff summaries as the last formal baseline, but also account for the current unpublished Epic 09 / Epic 10 state reflected in the module text and bugfix notes.
- Use a **new queen-to-hatchery radius** for inject eligibility instead of reusing the larva-assignment radius. Current implementation baseline: `12.0` tracker-map units center-to-center.
- Missed inject pressure should use **accumulated qualifying time across gaps** until each 29-second threshold is reached.
- Epic 11 scope is **injection tracking only**.

## Context

From the Epic 08 handoff summary in the module README:

- Epic 08 now documents replay fixtures, golden outputs, deterministic diagnostics, and reproducible packaging for the supported module-owned `Larva` page.
- Epic 08 keeps the supported rendering path explicit: the module-owned `Larva` page remains supported, while native chart dropdown registration and native Base Control augmentation remain unsupported for a pure external module.
- Epic 08 leaves later work as optional UX / feature expansion rather than blocked infrastructure.

From the current unpublished Epic 09 / Epic 10 state visible in the workspace:

- timing bugs around loop-to-time conversion and accumulation precision were investigated, so any new inject accounting must stay loop-precise and Scelight-time-consistent
- the active `v3` request now shifts focus from only larva-pressure output to explicit inject execution, idle inject opportunities, and separate missed-inject larva accounting

From the project goal and know-how documents:

- the overall visualization goal remains similar in spirit to Scelight's Base Control view, but the production implementation must stay on the supported external-module-owned `Larva` page
- Java 7 syntax and Java 7 API compatibility remain strict requirements
- the Scelight SDK-style Ant build and deployment flow remains mandatory

From the current replay-analysis foundation:

- Epic 06 already correlates `SpawnLarva` command targets by hatchery tag
- Epic 07 / Epic 08 already normalize replay-derived hatchery rows, stable time windows, markers, and diagnostics
- the current model pipeline already supports per-row segments, markers, hover text, and per-player summaries on the supported `Larva` page
- the existing black timeline ticks already mean the old larva-pressure metric: one marker every 11 seconds of `3+ larva` saturation, not inject status and not missed-inject accounting

From Scelight source used for implementation inspiration:

- `BaseControlChartFactory` groups `Spawn Larva` commands by target hatchery tag
- Scelight's Base Control logic removes overlapping inject commands because `Spawn Larva` cannot stack on the same hatchery
- Base Control computes hatch spawning coverage and injection gaps from inject command loops plus the larva-spawning duration

From the Epic 11 feature request:

- show a green line below the hatchery rail for the time a hatch is injected
- determine whether replay data exposes a direct injected-building state or whether active inject time must be reconstructed from replay events
- show red idle-inject-queen periods when nearby queens have 25+ energy and the hatchery is not injected
- count missed inject opportunities in separate accumulated 29-second windows, worth 3 larva each per hatchery
- keep missed-inject larva in a separate variable from the existing potential-larva-missed metric
- show those missed-inject larva as separate summary lines after the existing potential-larva-missed line

## Non-goals

- no unsupported native chart dropdown registration
- no unsupported Base Control augmentation or patching of Scelight internals
- no inclusion of per-phase stats, player-history tables, or replay-window-opening UX in Epic 11
- no migration away from Java 7 syntax, Java 7 APIs, or the SDK-style Ant workflow
- no silent guessing if replay data cannot prove queen eligibility or inject status confidently enough

## Story 11.01 — Prove the replay-native inject signal and build normalized inject windows

**As a** developer  
**I want** inject-active periods derived from replay-native evidence  
**So that** the timeline can show green inject windows based on trustworthy replay signals instead of UI-only heuristics.

### Acceptance criteria

- Replay analysis explicitly answers the technical question: does replay data expose a direct injected-building status, or must inject-active time be reconstructed from `SpawnLarva` command targets and duration rules?
- The answer is documented in code-facing diagnostics or developer notes, not left as tribal knowledge.
- A normalized per-hatchery inject-window model exists and is separate from Swing painting.
- Inject windows are keyed by hatchery identity with morph continuity across `Hatchery` / `Lair` / `Hive` on the same tag.
- Inject windows use a 29-second active duration per successful inject window.
- Overlapping inject commands for the same hatchery are normalized safely, following the same non-stacking principle seen in Scelight Base Control logic.
- Inject windows never extend before hatchery completion, after hatchery destruction, or past replay end.
- Diagnostics can explain why an inject window exists, was trimmed, or was discarded.

### Notes

This story resolves the core uncertainty first. If no direct replay-side injected-state exists, Epic 11 should formalize command-target reconstruction as the supported approach.

## Story 11.02 — Render a green inject-status lane below each hatchery rail

**As a** user  
**I want** a visible green lane showing when a hatchery is injected  
**So that** I can compare larva pressure against actual inject uptime on the same row.

### Acceptance criteria

- Each qualifying hatchery row can render a dedicated green inject-status lane below the main hatchery rail.
- The green lane aligns with the hatchery lifetime-bounded row already used by the supported `Larva` timeline.
- The existing `3+ larva` windows remain visible and are not replaced by the inject lane.
- Inject-status rendering comes from normalized model data, not from ad-hoc paint-time replay parsing.
- Hovering an inject-status segment shows at least the inject start and end times.
- The legend / summary text explains what the green inject lane means.
- Replays with no injects still render correctly without placeholder green noise.

### Notes

The inject lane is a subordinate lane under the existing hatchery rail, not a replacement for the main larva-pressure row.
The existing black ticks remain the separate `3+ larva` missed-potential-larva markers unless a later story explicitly changes that behavior.

## Story 11.03 — Detect idle inject queens with a dedicated queen radius

**As a** user  
**I want** red idle-queen periods shown when a hatchery could have been injected but was not  
**So that** I can see missed inject opportunities, not just successful injects.

### Acceptance criteria

- Epic 11 defines and documents a **new queen-to-hatchery proximity radius** specifically for inject eligibility.
- A hatchery qualifies for idle-inject pressure only when all of these are true:
  - the hatchery is complete and alive,
  - the hatchery is not currently inside an inject-active window,
  - at least one friendly queen is within the inject radius,
  - that queen has at least 25 energy.
  - that queen's 25 energy was not assigned to another hatchery
   + an edge case with M queens and N macro hatcheries around it - every 25 energy of every queen must be counted for only one hatch.
- Idle-inject windows are normalized as their own model data and rendered as a separate red lane below the hatchery rail / inject lane.
- If replay data cannot prove queen energy or proximity confidently for a period, the implementation fails safely and diagnostics explain the uncertainty instead of fabricating a stable-looking idle window.
- Diagnostics can explain which queens qualified a window and why a window stopped.
- The implementation supports multiple nearby queens without double-counting the same hatchery time.

### Notes

This story is intentionally strict. The output should prefer trustworthy missed-inject opportunity windows over aggressive speculative detection.

## Story 11.03A — Track queen non-inject energy spending conservatively

**As a** user  
**I want** missed inject detection to account for queen energy spent on other spells  
**So that** a queen that used creep tumor or transfuse does not create false missed-inject windows.

### Acceptance criteria

- Singleton-attributed queen commands recognize at least these energy spends:
  - `SpawnLarva` = 25 energy,
  - `Creep Tumor` = 25 energy,
  - `Transfuse` = 50 energy.
- When one of those known queen energy-spend commands is seen, idle-inject readiness is delayed by the corresponding replay-loop regeneration time.
- A known non-inject queen energy spend does **not** produce a false red idle-inject window immediately afterward.
- If replay evidence cannot safely prove that the queen remained bound to the same hatchery after spending energy elsewhere, the implementation fails safe and suppresses later red windows until trustworthy proof resumes.
- Diagnostics and code-facing notes explain that this conservative handling exists specifically to avoid false positives from tumor / transfuse energy usage.

### Notes

This story is intentionally conservative. It is better to miss a borderline red window than to report a missed inject for a queen that actually spent its energy on another legitimate spell.

## Story 11.04 — Accumulate missed-inject larva separately from existing larva-pressure loss

**As a** user  
**I want** missed-inject larva counted separately from `3+ larva` pressure loss  
**So that** I can distinguish spending/larva-bank issues from inject-execution issues.

### Acceptance criteria

- Missed-inject accumulation uses loop-precise qualifying time from idle-inject windows.
- Qualifying time accumulates **across gaps** until each 29-second threshold is reached.
- Each completed 29-second threshold contributes **3 missed inject larva** for that hatchery.
- Missed-inject larva totals are stored in separate variables / model fields from the existing `potential larva missed` metric.
- Each qualifying hatchery row shows a separate missed-inject summary line after the existing potential-larva-missed line.
- The missed-inject summary line is visually distinguished in red.
- Player-level summaries can aggregate missed-inject larva separately from existing larva-pressure totals.
- Tooltip or diagnostics time labels use the same Scelight-consistent time basis already hardened in recent timing fixes.

### Notes

This story must not silently merge two different concepts: larva lost due to spending pressure and larva missed due to inject neglect.

## Story 11.05 — Add validation coverage and Epic 11 handoff documentation

**As a** developer  
**I want** inject-status tracking validated and summarized clearly  
**So that** later work can safely build stats and UX on top of a trustworthy injection model.

### Acceptance criteria

- Replay fixtures or validation scenarios cover at least:
  - normal inject uptime,
  - overlapping / reissued inject commands,
  - destroyed or morphed hatcheries during an inject window,
  - nearby queen present but below 25 energy,
  - nearby queen with 25+ energy while hatchery is not injected,
  - multiple nearby queens that should not double-count the same idle window.
- Diagnostics can report per hatchery:
  - inject-active windows,
  - idle-inject windows,
  - missed-inject accumulated time,
  - missed-inject larva totals,
  - and uncertainty reasons when detection is inconclusive.
- README handoff sections and developer notes are updated to reflect what Epic 11 proves.
- A short handoff note states what Epic 11 has proven:
  - inject-active windows can be reconstructed reliably enough for the supported timeline,
  - idle inject opportunity windows can be derived with a dedicated queen radius and energy threshold,
  - missed-inject larva is tracked separately from existing larva-pressure loss,
  - the feature remains on the supported module-owned `Larva` page.
- A short note states what remains for later work:
  - per-phase stats,
  - player-history / multi-replay stats,
  - replay-opening UX improvements,
  - and any unresolved replay-data limits around queen-energy certainty.

## Definition of done

Epic 11 is done when:

- a normalized inject-window model exists per hatchery
- the supported `Larva` timeline renders green inject-active lanes below hatchery rows
- idle-inject opportunity windows are rendered from a dedicated queen-radius + 25-energy rule
- missed-inject accumulation uses 29-second thresholds worth 3 larva each
- missed-inject larva totals remain separate from existing potential-larva-missed totals
- diagnostics and validation scenarios cover inject overlap, morph continuity, destruction, queen eligibility, and safe failure cases
- the supported rendering path remains the module-owned `Larva` page

## Risks in this epic

- Replay data may expose `SpawnLarva` command targets without exposing a separate direct injected-state flag, requiring duration reconstruction instead of a simpler building property.
- Queen energy may be sparse or indirect in replay data, making exact idle-queen windows harder than inject-active windows.
- A poorly chosen queen radius could create noisy false-positive idle windows around clustered hatcheries.
- Overlapping inject commands, hatchery morph continuity, or destruction during the 29-second inject duration could create misleading windows if not normalized carefully.
- The existing timing-basis bugs could reappear if inject accounting mixes replay loops with inconsistent real-time conversions.

## Suggested implementation order

1. Story 11.01 — Prove the replay-native inject signal and build normalized inject windows
2. Story 11.02 — Render a green inject-status lane below each hatchery rail
3. Story 11.03 — Detect idle inject queens with a dedicated queen radius
4. Story 11.03A — Track queen non-inject energy spending conservatively
5. Story 11.04 — Accumulate missed-inject larva separately from existing larva-pressure loss
6. Story 11.05 — Add validation coverage and Epic 11 handoff documentation

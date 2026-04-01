# Epic 05 — Probe Base Control chart augmentation

## Epic goal

Determine whether a pure Scelight external module can augment Scelight's built-in `Base Control` chart with larva rectangles instead of owning a separate larva visualization.

This epic exists because the final project goal asks for a visualization similar to Base Control, but Epic 04 already established that native `larva` chart-dropdown registration is unsupported for a pure external module. Epic 05 must produce a definitive yes/no answer for Base Control augmentation and keep the supported rendering path clear.

If Base Control augmentation is supported through the public external module API, this epic should implement it safely.
If it is not supported, this epic should document that limitation clearly and keep the module-owned `Larva` timeline as the supported solution.

## Context

From the Epic 05 handoff summary in the module README:

- Epic 04 proved that a pure external module cannot register a native `larva` chart entry in Scelight's built-in chart dropdown through the public external module API.
- Epic 04 proved that the module-owned `Larva` page remains the supported visualization path when native chart registration is unavailable.
- Epic 04 proved that capability-check-based reporting can communicate unsupported native integration without breaking the fallback page.
- Epic 04 did not yet answer whether the native `Base Control` chart itself could be augmented with larva rectangles.

From the project goal and know-how documents:

- the desired visualization is similar to the Base Control view that shows injected hatcheries
- Java 7 syntax and Java 7 API compatibility are mandatory
- the supported implementation path should stay within the public Scelight external module API
- replay-page fallback infrastructure must remain usable if Base Control augmentation is impossible

From the infrastructure plan:

- Epic 05 is the explicit feasibility test for Base Control chart augmentation
- this is an experiment, not a guaranteed implementation path
- unsupported internal coupling to `ChartType`, `ChartsComp`, `BaseControlChartFactory`, or `BaseControlChartDataSet` is not the supported production path

## Non-goals

- no final 3+ larva red-rectangle rendering yet
- no final missed-larva threshold markers or overview totals yet
- no hover minerals / gas detail yet
- no replacement of the already working module-owned `Larva` timeline
- no production dependency on reflection-based patching of Scelight internals

## Story 05.01 — Review the public Base Control extension surface

**As a** developer  
**I want** to inspect the public external module API for any Base Control augmentation hook  
**So that** Epic 05 starts from supported API evidence instead of assumptions.

### Acceptance criteria

- The public external module API is reviewed for any callback, registry, or adapter that could add data to the native Base Control chart.
- The review explicitly answers whether a pure external module can augment Base Control through supported API.
- The result is written down in project-facing documentation or diagnostics.
- The conclusion remains compatible with Java 7 and SDK-style external module deployment.

### Notes

This story should prefer SDK and API evidence over inferred behavior.

## Story 05.02 — Inspect native Base Control wiring boundaries without coupling to them

**As a** developer  
**I want** to understand how Scelight constructs the native Base Control chart internally  
**So that** I can determine whether unsupported augmentation would require internal-only coupling.

### Acceptance criteria

- The internal Base Control wiring is inspected at source level only as a feasibility study.
- The investigation identifies the relevant internal classes involved in Base Control construction.
- The project records whether those boundaries are external-module-extensible or internal-only.
- No production implementation is committed that depends on unsupported internal runtime coupling.

### Notes

This story is evidence gathering only. It must not convert the external module into an internal fork.

## Story 05.03 — Add a capability-check abstraction for Base Control augmentation

**As a** developer  
**I want** Base Control augmentation logic isolated behind a capability-check abstraction  
**So that** the module-owned `Larva` timeline remains intact whether the answer is yes or no.

### Acceptance criteria

- Base Control augmentation feasibility is isolated behind a small integration boundary.
- The module can report whether Base Control augmentation is supported, unsupported, or unavailable.
- The existing module-owned `Larva` page keeps working regardless of the capability result.
- Later larva-window rendering code does not need to know unsupported native augmentation details.

### Notes

Keep this layer small. The main goal is clean separation between feasibility logic and the supported fallback path.

## Story 05.04 — Augment Base Control only if the public API supports it

**As a** user  
**I want** larva rectangles added to the native `Base Control` chart when supported  
**So that** the final visualization can align with Scelight's existing replay chart workflow without unsafe hacks.

### Acceptance criteria

- If a supported external-module augmentation hook exists, the module augments the native Base Control chart safely.
- The augmentation integrates without breaking startup, replay loading, or the module-owned fallback page.
- The implementation uses only supported public APIs.
- If no supported augmentation hook exists, no unsafe workaround is shipped under this story.

### Notes

A clean and documented `no` is a valid outcome for this story.

## Story 05.05 — Surface unsupported-native-augmentation outcome clearly

**As a** user  
**I want** the module to state clearly when Base Control augmentation is unsupported  
**So that** I understand why the separate `Larva` timeline remains the supported rendering path.

### Acceptance criteria

- If Base Control augmentation is unsupported, the module communicates that result clearly in diagnostics, documentation, or page messaging.
- The wording explains that the module-owned `Larva` timeline remains the supported rendering path.
- Logs and/or the development diagnostic dump can confirm which augmentation mode is active.
- The messaging does not imply that unsupported internal patching is part of the supported implementation.

### Notes

This story matters because the final visualization goal is explicitly similar to Base Control even if the platform cannot expose that chart to external modules.

## Story 05.06 — Prepare Epic 06 handoff conditions

**As a** developer  
**I want** a clean handoff from Base Control feasibility to larva-assignment foundation work  
**So that** the next epic can focus on reconstructing replay-derived per-hatchery larva counts.

### Acceptance criteria

- A short note states what Epic 05 has proven:
  - whether a pure external module can augment the native Base Control chart,
  - whether the answer is based on supported public API evidence,
  - whether the module-owned `Larva` timeline remains the supported fallback,
  - whether native Base Control augmentation is active or unsupported.
- A short note states what is still unresolved:
  - larva-to-hatchery attribution,
  - stable per-hatchery larva count timelines,
  - final 3+ larva windows,
  - missed-larva summaries and hover resource details.
- The next technical question is clearly identified as: **how can larva births be assigned to hatcheries reliably enough to build per-hatchery larva timelines?**

## Definition of done

Epic 05 is done when:

- there is a definitive yes/no answer for native Base Control chart augmentation
- the answer is based on supported API and code-level evidence rather than assumption
- any Base Control augmentation logic is isolated behind a capability boundary
- unsupported native augmentation is documented clearly if that is the outcome
- the module-owned `Larva` timeline remains the supported visualization path when native augmentation is unavailable
- the project is ready for Epic 06 larva-assignment foundation work

## Risks in this epic

- The final visualization goal may encourage unsupported coupling to internal Base Control classes.
- Internal Scelight chart wiring may appear technically reachable while still being unsupported and brittle.
- A partial investigation could leave the project uncertain about whether Base Control is a real target or a dead end.
- Mixing Base Control feasibility logic into larva-window rendering work would slow later epics.

## Suggested implementation order

1. Story 05.01 — Review the public Base Control extension surface
2. Story 05.02 — Inspect native Base Control wiring boundaries without coupling to them
3. Story 05.03 — Add a capability-check abstraction for Base Control augmentation
4. Story 05.04 — Augment Base Control only if the public API supports it
5. Story 05.05 — Surface unsupported-native-augmentation outcome clearly
6. Story 05.06 — Prepare Epic 06 handoff conditions

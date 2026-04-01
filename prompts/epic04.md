# Epic 04 — Attempt native chart dropdown integration

## Epic goal

Determine whether a pure Scelight external module can register a native `larva` entry in Scelight's built-in chart dropdown.

This epic exists because the final project goal still calls for a chart selectable as `larva`, but the supported implementation path so far is only the module-owned `Larva` page. Epic 04 must produce a definitive yes/no answer and then keep the project on the supported path.

If native registration is supported through the public external module API, this epic should implement it safely.
If native registration is not supported, this epic should document that limitation clearly and keep the fallback module-owned visualization as the supported solution.

## Context

From the Epic 04 handoff summary in the module README:

- Epic 03 proved that the supported module-owned `Larva` page can host a chart-like timeline without depending on Scelight internal chart classes.
- Epic 03 proved that replay-derived timing can drive visible placeholder output on that timeline.
- Epic 03 proved that the fallback visualization can render one row per derived hatchery timeline and group those rows by player when such timelines are available.
- Epic 03 proved that resize, redraw, replay switching, and page state transitions can update the timeline without reopening the page.
- Epic 03 proved that the visualization path can stay decoupled from replay parser objects by using a normalized presentation model between replay analysis and painting.

From the project goal and know-how documents:

- the desired end-user target is a chart choice named `larva`
- Java 7 syntax and Java 7 API compatibility are mandatory
- the module should stay within the public Scelight external module API for the supported production path
- Scelight external modules are identified by manifest + module folder structure and should remain independently deployable
- replay-page fallback infrastructure already exists and must remain usable if native chart registration is impossible

From the infrastructure plan:

- Epic 04 is specifically the feasibility check for native chart dropdown integration
- the expected implementation boundary is `ScelightChartIntegration.java` or equivalent capability-focused wiring
- unsupported reflection-based coupling to internal chart enums, factories, or replay-analyzer classes is not the supported production path

## Non-goals

- no final 3+ larva red-rectangle rendering yet
- no final missed-larva threshold markers or overview totals yet
- no hover minerals / gas detail yet
- no Base Control chart augmentation yet
- no production dependency on unsupported reflection-based hacks into Scelight internals
- no abandonment of the already working module-owned `Larva` page fallback

## Story 04.01 — Review the public chart-extension surface

**As a** developer  
**I want** to inspect the public external module API for chart registration capability  
**So that** Epic 04 starts from what is actually supported instead of what the final goal hopes for.

### Acceptance criteria

- The public external module API is reviewed for any chart registration or replay-chart contribution hook.
- The review explicitly answers whether a pure external module can add entries to Scelight's built-in chart selector through supported API.
- The result is written down in project-facing documentation or diagnostics.
- The conclusion remains compatible with Java 7 and SDK-style external module deployment.

### Notes

This story should prefer evidence from the SDK, API Javadoc, and current Scelight source layout over assumptions.

## Story 04.02 — Inspect native chart wiring boundaries without coupling to them

**As a** developer  
**I want** to understand how Scelight's built-in chart dropdown is wired internally  
**So that** I can determine whether lack of public API support is a true limitation or only a documentation gap.

### Acceptance criteria

- The internal chart wiring is inspected at source level only as a feasibility study.
- The investigation identifies the relevant internal chart selector and factory boundaries.
- The project records whether those boundaries are external-module-extensible or internal-only.
- No production implementation is committed that depends on unsupported internal runtime coupling.

### Notes

This story is about evidence gathering. It must not quietly turn the module into an internal Scelight fork.

## Story 04.03 — Add a capability-check abstraction for native chart integration

**As a** developer  
**I want** native chart registration logic isolated behind a capability-check abstraction  
**So that** the supported fallback path remains intact whether the answer is yes or no.

### Acceptance criteria

- Chart registration logic is isolated behind a small integration boundary.
- The module can report whether native chart registration is supported, unsupported, or unavailable.
- The existing module-owned `Larva` page keeps working regardless of the capability result.
- The abstraction does not force later larva-window rendering code to know about unsupported native integration details.

### Notes

Keep this layer small. The main goal is clean separation between feasibility logic and the replay-page fallback.

## Story 04.04 — Register `larva` natively if and only if the public API supports it

**As a** user  
**I want** `larva` to appear in Scelight's built-in chart dropdown when supported  
**So that** the final feature can align with the native chart workflow without unsafe hacks.

### Acceptance criteria

- If a supported external-module chart registration hook exists, the module registers a chart entry named `larva`.
- The registration integrates without breaking startup, replay loading, or the module-owned fallback page.
- The implementation uses only supported public APIs.
- If no supported registration hook exists, no unsafe workaround is shipped under this story.

### Notes

A clean and documented `no` is a valid outcome for this story.

## Story 04.05 — Surface unsupported-native-integration outcome clearly

**As a** user  
**I want** the module to state clearly when native chart dropdown integration is unsupported  
**So that** I understand why the `Larva` page remains the supported visualization path.

### Acceptance criteria

- If native chart registration is unsupported, the module communicates that result clearly in diagnostics, documentation, or page messaging.
- The wording explains that the fallback module-owned `Larva` page remains the supported rendering path.
- Logs and/or the development diagnostic dump can confirm which integration mode is active.
- The messaging does not imply that unsupported reflection-based hacks are part of the supported implementation.

### Notes

This story is important because the project goal asks for a native chart entry, but the platform may not expose that capability to external modules.

## Story 04.06 — Prepare Epic 05 handoff conditions

**As a** developer  
**I want** a clean handoff from chart-registration feasibility to Base Control augmentation feasibility  
**So that** the next epic can focus on the next unresolved native integration question.

### Acceptance criteria

- A short note states what Epic 04 has proven:
  - whether a pure external module can register a native `larva` chart entry,
  - whether the answer is based on supported public API evidence,
  - whether the module-owned `Larva` page remains the supported fallback,
  - whether native chart integration is active or unsupported.
- A short note states what is still unresolved:
  - Base Control chart augmentation,
  - final 3+ larva window calculation,
  - missed-larva summaries,
  - hover resource details.
- The next technical question is clearly identified as: **can a pure external module augment Scelight's built-in Base Control chart with larva rectangles?**

## Definition of done

Epic 04 is done when:

- there is a definitive yes/no answer for native `larva` chart dropdown registration
- the answer is based on supported API and code-level evidence rather than assumption
- any native integration code is isolated behind a capability boundary
- unsupported native registration is documented clearly if that is the outcome
- the module-owned `Larva` page remains the supported visualization path when native registration is unavailable
- the project is ready for Epic 05 investigation of Base Control chart augmentation

## Risks in this epic

- The final project goal may imply a supported native chart entry even though the external module API may not expose one.
- Internal Scelight chart wiring may tempt unsafe reflection-based workarounds that are difficult to maintain.
- A partial or ambiguous investigation could leave the project uncertain about whether fallback UI is temporary or permanent.
- Over-coupling capability checks to unfinished larva rendering logic would make later epics harder.

## Suggested implementation order

1. Story 04.01 — Review the public chart-extension surface
2. Story 04.02 — Inspect native chart wiring boundaries without coupling to them
3. Story 04.03 — Add a capability-check abstraction for native chart integration
4. Story 04.04 — Register `larva` natively if and only if the public API supports it
5. Story 04.05 — Surface unsupported-native-integration outcome clearly
6. Story 04.06 — Prepare Epic 05 handoff conditions

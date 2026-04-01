# Epic 03 — First chart somewhere accessible

## Epic goal

Render a first time-based chart or chart-like visualization on the module-owned `Larva` page so Epic 02's replay-scoped fallback surface becomes a usable visualization host.

This epic is about proving the rendering path, not about completing the final larva feature yet.

The final project goal remains the same:

- analyze hatchery larva counts from replay data
- visualize windows where a hatchery has 3+ larva
- eventually show one line per hatchery, separated by player
- eventually support missed-larva summaries and related overlays

Epic 03 should create the stable visualization foundation that later epics can replace with real larva windows and red rectangles.

## Context

From the Epic 03 handoff summary in the module README:

- Epic 02 proved that a replay-scoped module-owned surface exists and is reachable from Scelight navigation.
- Epic 02 proved that replay selection, replay loading, and replay lifecycle feedback are working on the supported fallback path.
- Epic 02 did not prove native chart dropdown integration or Base Control augmentation; both remain unsupported through the public external module API.
- Epic 02 did not yet implement the final larva-window rendering.
- Epic 03 and later work should focus on visualization on the supported module-owned `Larva` page.

From the infrastructure plan and know-how documents:

- the supported production path is a pure external module using the public Scelight external module API
- Java 7 syntax and Java 7 API compatibility are mandatory
- the module should avoid coupling to Scelight internal chart enums, factories, or replay-analyzer internals
- the first visualization may be a module-owned chart-like timeline instead of a native chart-dropdown entry
- the rendering path should survive replay switching, resize, redraw, and cleanup events

## Non-goals

- no native chart dropdown registration yet
- no Base Control chart augmentation yet
- no final per-hatchery 3+ larva red-rectangle rendering yet
- no final missed-larva accumulation logic yet
- no overview text such as total potential larva missed yet
- no hover minerals / gas detail yet
- no unsupported reflection-based chart injection into Scelight internals

## Story 03.01 — Define the module-owned timeline rendering contract

**As a** developer  
**I want** a small visualization contract for time-based replay rendering  
**So that** the `Larva` page can host a chart-like component without depending on final larva logic.

### Acceptance criteria

- A small presentation model exists for timeline intervals or markers.
- The model is decoupled from Swing painting details.
- The model is simple enough to be reused later for larva windows.
- The API remains Java 7 compatible.

### Notes

This is the abstraction layer that later epics should be able to feed with real hatchery data.

## Story 03.02 — Add a chart-like timeline component to the `Larva` page

**As a** user  
**I want** to see a visual timeline area on the `Larva` page  
**So that** the module-owned replay surface becomes more than text diagnostics.

### Acceptance criteria

- The `Larva` page contains a visible timeline or chart-like region above or beside diagnostics.
- The component is clearly owned by the module and not presented as native Scelight chart integration.
- The page layout remains readable when no replay is loaded.
- The component can be created and disposed safely with the page lifecycle.

### Notes

This story proves the page can host visualization, even if the first dataset is trivial.

## Story 03.03 — Render a first replay-derived placeholder interval

**As a** developer  
**I want** the timeline to render at least one replay-derived interval or marker  
**So that** the visualization path is exercised with real replay timing instead of static UI only.

### Acceptance criteria

- The chart-like component renders at least one visible interval or marker.
- The rendered timing comes from replay-derived data, even if it is only a placeholder approximation.
- The visualization updates when a different replay is loaded.
- The user can visually confirm that rendering depends on replay context.

### Notes

This story is intentionally modest. The first rendered interval does not need to be a real 3+ larva window yet.

## Story 03.04 — Make rendering stable across resize, redraw, and replay switching

**As a** user  
**I want** the timeline to behave predictably while interacting with Scelight  
**So that** the fallback visualization path is robust enough for later real data.

### Acceptance criteria

- Resizing the page repaints the chart-like component correctly.
- Replay switching replaces the old visualization state with the new one.
- Empty, loading, success, and error page states do not leave stale chart artifacts behind.
- The rendering path does not require reopening the page to refresh.

### Notes

This story is more important than visual polish. Stability matters more than appearance at this stage.

## Story 03.05 — Keep visualization separate from final larva analysis logic

**As a** developer  
**I want** the first chart implementation isolated from unfinished larva rules  
**So that** later epics can replace placeholder data with real hatchery windows without rewriting the page structure.

### Acceptance criteria

- The rendering component consumes a normalized presentation model instead of replay parser objects directly.
- Replay loading, replay summary diagnostics, and painting logic remain separated.
- Placeholder chart data can be replaced later without changing the overall page wiring.
- The design leaves room for one row per hatchery and per-player grouping later.

### Notes

This story prevents premature coupling between UI experimentation and the final larva algorithm.

## Story 03.06 — Make the fallback visualization mode explicit

**As a** user  
**I want** the page to communicate that this is a module-owned fallback visualization  
**So that** I do not confuse it with native Scelight chart registration.

### Acceptance criteria

- The `Larva` page messaging or diagnostics state that the visualization is module-owned.
- Logs and/or the development diagnostic dump can confirm that the chart-like component was populated.
- The wording does not imply that the built-in chart dropdown has been extended.
- The module remains truthful about current integration limitations.

### Notes

Clear messaging reduces confusion before Epic 04 investigates native chart registration directly.

## Story 03.07 — Prepare Epic 04 handoff conditions

**As a** developer  
**I want** a clean handoff from first visualization to native chart integration investigation  
**So that** the next epic can focus on the chart registration question instead of replay-page rendering basics.

### Acceptance criteria

- A short note states what Epic 03 has proven:
  - a chart-like timeline can be rendered on the supported module-owned `Larva` page,
  - replay-derived timing can drive visible output,
  - resize, redraw, and replay switching are handled safely,
  - the visualization path is decoupled enough for later real larva windows.
- A short note states what is still unresolved:
  - native chart dropdown registration,
  - Base Control augmentation,
  - final 3+ larva window calculation,
  - missed-larva overview and hover details.
- The next technical question is clearly identified as: **can a pure external module register a native `larva` chart entry in Scelight's chart dropdown?**

## Definition of done

Epic 03 is done when:

- the module-owned `Larva` page contains a visible chart-like timeline area
- the timeline renders replay-derived timing data, even if still placeholder-level
- redraw, resize, and replay switching work reliably
- visualization is clearly separated from replay diagnostics and unfinished larva logic
- the fallback nature of the visualization is communicated clearly
- the project is ready for Epic 04 investigation of native chart dropdown integration

## Risks in this epic

- The first chart implementation may accidentally couple too tightly to placeholder replay logic.
- Rendering may appear correct for one replay but fail on replay switching or empty states.
- It may be tempting to skip abstraction and paint directly from parser results, which would slow later epics.
- Users may mistake a module-owned timeline for successful native chart integration unless messaging is explicit.

## Suggested implementation order

1. Story 03.01 — Define the module-owned timeline rendering contract
2. Story 03.02 — Add a chart-like timeline component to the `Larva` page
3. Story 03.03 — Render a first replay-derived placeholder interval
4. Story 03.04 — Make rendering stable across resize, redraw, and replay switching
5. Story 03.05 — Keep visualization separate from final larva analysis logic
6. Story 03.06 — Make the fallback visualization mode explicit
7. Story 03.07 — Prepare Epic 04 handoff conditions

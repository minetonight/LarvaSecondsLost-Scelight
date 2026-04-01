# Epic 02 — Reachable replay-view presence

## Epic goal

Make the larva module reachable from the replay-analysis workflow by providing a replay-scoped fallback surface that an external Scelight module can support through the public API.

This epic should answer the Epic 01 handoff question: **where can a mod surface something during replay analysis?**

The expected answer for the supported implementation path is a **module-owned `Larva` page** that stays adjacent to replay work, loads replay data through the public replay parser API, and becomes the stable base for later larva visualization.

## Context

From Epic 01, the following is already proven:

- the module builds, packages, installs, and starts successfully
- Java 7 syntax and Java 7 API compatibility remain mandatory
- lifecycle logging and a development diagnostic dump already exist
- the next uncertainty is replay-view accessibility, not packaging

From the infrastructure plan and current Scelight know-how:

- the public external module API exposes page-level UI hooks and replay parsing services
- the public external module API does **not** promise direct access to Scelight's internal replay analyzer tab lifecycle
- the supported path should avoid app-internal hacks and stay within the external module API
- this epic should keep replay integration separate from later chart rendering logic

## Non-goals

- no native chart dropdown registration yet
- no Base Control chart augmentation yet
- no final larva red-rectangle visualization yet
- no full larva-to-hatchery assignment implementation yet
- no unsupported reflection-based integration into Scelight internals

## Story 02.01 — Identify the supported replay integration path

**As a** developer  
**I want** to determine which replay-related UI hooks are actually available to a pure external module  
**So that** Epic 02 can be built on a supported integration point instead of assumptions.

### Acceptance criteria

- The replay-related extension points exposed by the public external module API are reviewed.
- A short conclusion is written down stating whether the internal replay analyzer can be extended directly.
- If direct replay-analyzer injection is not supported, the fallback path is explicitly chosen.
- The chosen supported path is a module-owned replay page reachable from Scelight UI.

### Notes

This story is about removing uncertainty early. The goal is not to force native replay-analyzer injection if the API does not expose it.

## Story 02.02 — Add a module-owned `Larva` replay page

**As a** user  
**I want** a dedicated `Larva` page inside Scelight  
**So that** I can reach module-owned replay diagnostics while working with replays.

### Acceptance criteria

- The module adds a visible page named `Larva` to Scelight.
- The page is reachable from the normal page tree/navigation.
- The page can be created safely during module startup.
- The page remains independent from Scelight internal replay-analyzer classes.

### Notes

This page is the Epic 02 fallback replay-view surface and should remain useful even if later native chart integration stays unsupported.

## Story 02.03 — Add replay loading actions to the page

**As a** user  
**I want** practical ways to select a replay from the `Larva` page  
**So that** the fallback replay-view surface is usable during real replay analysis.

### Acceptance criteria

- The page provides an action to open a replay file manually.
- The page provides an action to analyze the latest replay when available.
- The page provides an action to refresh the current replay analysis.
- Replay selection failures show a safe, user-friendly message.

### Notes

Keep the replay actions simple and Java 7 compatible. Do not depend on unsupported internal Scelight replay page controls.

## Story 02.04 — Show replay-scoped summary information

**As a** user  
**I want** replay metadata shown on the `Larva` page  
**So that** I can confirm the module is analyzing the expected replay.

### Acceptance criteria

- The page shows the replay file path or file name.
- The page shows basic replay metadata such as map, players, winners, replay length, and replay version when available.
- Replay parsing uses the public Scelight replay parser / processor API.
- The replay summary is clearly separated from future larva-specific visualization logic.

### Notes

This story intentionally starts with lightweight replay diagnostics. It is a bridge between hello-world startup proof and later larva analysis.

## Story 02.05 — Connect replay monitoring and latest-replay fallback

**As a** user  
**I want** the module to notice current replay context without always browsing manually  
**So that** the fallback page feels connected to replay analysis instead of isolated.

### Acceptance criteria

- The module listens for newly detected replay files through the replay-folder monitor if the API exposes it.
- The module remembers the latest replay candidate for later analysis.
- If the monitor event is unavailable or missed, the module can still resolve a best-effort latest replay candidate from the monitored replay folders.
- On click of the module in the main navigation the latest replay is loaded automatically.
- The replay source is identified in the diagnostics output.

### Notes

This story is important because the fallback page should stay adjacent to replay workflow, not become a disconnected utility page.

## Story 02.06 — Add loading, success, and error states

**As a** user  
**I want** clear page states while replay analysis runs  
**So that** I can understand whether the module is working, waiting, or failed.

### Acceptance criteria

- The page shows an idle state before any replay is analyzed.
- The page shows a busy/loading state while replay parsing is running.
- The page shows a success state with replay diagnostics after analysis finishes.
- The page shows an error state with a useful failure message if analysis fails.
- UI refreshes occur safely without blocking the Scelight UI unnecessarily.

### Notes

Keep replay work off the UI thread when possible, but keep the implementation small and robust.

## Story 02.07 — Surface the fallback integration mode explicitly

**As a** developer  
**I want** the page to state that it is using a fallback replay-view integration path  
**So that** later epics do not confuse supported fallback UI with native replay-analyzer integration.

### Acceptance criteria

- The `Larva` page explains that it is a module-owned replay-view fallback surface.
- The diagnostics indicate how the replay was selected, for example manual selection or replay monitor.
- The module records enough information in logs and/or the dev diagnostic dump to confirm replay-page activity.
- The wording makes it clear that native replay-analyzer injection is not yet assumed.

### Notes

This story reduces future confusion when chart integration and Base Control feasibility are evaluated.

## Story 02.08 — Prepare Epic 03 handoff conditions

**As a** developer  
**I want** a clean handoff from replay presence to first-visualization work  
**So that** the next epic can focus on rendering rather than replay-page wiring.

### Acceptance criteria

- A short note states what Epic 02 has proven:
  - a replay-scoped module-owned surface exists,
  - replays can be selected and analyzed there,
  - replay metadata can be displayed,
  - replay lifecycle states are visible.
- A short note states what is still unresolved:
  - native chart dropdown integration,
  - Base Control augmentation,
  - real larva windows.
- The next technical question is clearly identified as: **can the module render a first chart-like timeline on the supported `Larva` page?**

## Definition of done

Epic 02 is done when:

- the module exposes a visible `Larva` page in Scelight
- the page is reachable during replay work
- the page can load a replay manually and through latest-replay flow
- replay summary information is shown from the public replay parser API
- idle, loading, success, and error states are visible
- replay monitor integration or best-effort latest-replay fallback is in place
- logs and dev diagnostics make replay-page behavior observable
- the project is ready for Epic 03 chart-like rendering on the module-owned page

## Risks in this epic

- The public external module API may not expose any direct hook into the internal replay analyzer workflow.
- Replay-folder monitor events may be timing-sensitive, so a latest-replay fallback may still be required.
- Replay parsing may return partial data unless the correct parser/processor path is used.
- It may be tempting to couple page wiring to future chart logic too early.

## Suggested implementation order

1. Story 02.01 — Identify the supported replay integration path
2. Story 02.02 — Add a module-owned `Larva` replay page
3. Story 02.03 — Add replay loading actions to the page
4. Story 02.04 — Show replay-scoped summary information
5. Story 02.06 — Add loading, success, and error states
6. Story 02.05 — Connect replay monitoring and latest-replay fallback
7. Story 02.07 — Surface the fallback integration mode explicitly
8. Story 02.08 — Prepare Epic 03 handoff conditions

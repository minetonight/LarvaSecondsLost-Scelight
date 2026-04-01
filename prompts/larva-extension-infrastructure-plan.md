# Strategic Implementation Plan: Scelight External Module Infrastructure for `larva` Chart

## Overview

This plan covers only the infrastructure required to build, package, load, and validate a Scelight external module that adds a `larva` chart-oriented extension for Zerg hatchery larva saturation windows. The target outcome is a deployable Scelight external module, built with the Scelight external module SDK, using Java 7 syntax only, and structured so the later feature implementation can focus on replay analysis and visualization logic rather than module wiring.

The intended functional direction is to calculate time windows when a hatchery has at least 3 larva and expose those windows through Scelight-compatible UI and chart infrastructure. A key architectural constraint is that Scelight tracker data appears to expose larva entities, owners, and coordinates, but not a direct larva-to-hatchery parent relation. Another critical constraint is that the desired chart dropdown entry named `larva` may depend on app-internal chart registration behavior that an external module might not be able to extend directly.

An additional working assumption for the first implementation is that a larva is visually and positionally "below" the hatchery it belongs to. This should not be hard-coded as an unexplained magic rule. Instead, the module should derive and document a repeatable "below the hatchery" offset/range from the start of the Zerg replay that start with one hatchery and three larva, and then use that as part of the hatchery assignment heuristic.

## Goals

- Establish a standalone external module project rooted in the SDK model and compatible with Scelight deployment packaging.
- Create the minimum class, config, and asset structure needed for module lifecycle, replay analysis, visualization, and validation.
- Keep the module isolated from Scelight app internals except through the published external module API and runtime discovery mechanisms.
- Preserve Java 7 compatibility across source layout, dependencies, build settings, and helper utilities.
- Prepare fallback infrastructure if direct chart dropdown integration cannot be achieved from a pure external module.
- Deliver work in epics that start with the smallest possible working module and only then move into replay-view visualization experiments.

## Epic roadmap

### Epic 1: Hello World module baseline

**Objective:** prove that a custom external module can be built, packaged, installed, enabled, and observed at runtime.

**Scope:**

- Create a dedicated module project from the Scelight external module SDK structure.
- Configure `release.properties`, manifest generation, icon, folder id, deployment naming, and local install path.
- Implement a minimal `LarvaChartModule.java` entrypoint.
- Add very small visible behavior such as startup logging, a simple menu/action/hook if the external API allows it, or a diagnostic confirmation panel.
- Add an optional development-only diagnostic dump file so basic runtime verification can be performed with fewer manual UI steps.

**Exit criteria:**

- The deployment zip builds cleanly.
- The module installs into a local Scelight instance.
- Enabling the module produces a visible or logged "hello world" confirmation.
- A predictable diagnostic dump path can be enabled for development-time verification.
- The project is confirmed to compile with Java 7 only.

### Epic 2: Reachable replay-view presence

**Objective:** make the module visible somewhere accessible while analyzing a replay, even if native chart injection is not yet possible.

**Scope:**

- Inspect and validate which replay-view extension points are exposed to external modules.
- Build a replay-scoped integration path that can surface module output from within or adjacent to the replay analyzer experience.
- Prefer a lightweight panel, toolbar action, side view, or overlay tied to the replay page.
- Keep this layer separate from larva analysis so it can be reused for diagnostics.

**Exit criteria:**

- A user can open a replay and reach a module-owned visualization or diagnostic surface from that workflow.
- The integration path is documented as either native replay-view extension or fallback UI.

### Epic 3: First chart somewhere accessible

**Objective:** render any time-based chart or chart-like visualization owned by the module before attempting the final larva feature.

**Scope:**

- Start with a trivial dataset, for example one fixed interval or one replay-derived marker.
- Reuse the replay-view surface from Epic 2 if native chart registration is unavailable.
- Validate rendering lifecycle, resizing, redraw, replay switching, and cleanup.
- Build the abstraction that later consumes real larva windows.

**Exit criteria:**

- A chart or chart-like timeline is visible from the replay workflow.
- The rendering path is stable enough to host real hatchery intervals.

### Epic 4: Attempt native chart dropdown integration

**Objective:** determine whether an external module can add a new chart entry named `larva` to the existing chart selector.

**Scope:**

- Investigate the external API and runtime for chart registration capability.
- Implement `ScelightChartIntegration.java` behind a capability check.
- If supported, register `larva` as a chart entry.
- If not supported, record the limitation and keep the fallback replay-view chart path as the supported solution.

**Exit criteria:**

- There is a definitive yes/no answer for native chart dropdown integration.
- The module either exposes `larva` in the chart dropdown or documents the unsupported limitation and uses fallback UI.

### Epic 5: Probe Base Control chart augmentation

**Objective:** test whether the module can add new red rectangles to the existing Base Control chart instead of owning a separate chart.

**Scope:**

- Treat this as an experiment, not as the base assumption.
- Check whether Base Control chart internals are replaceable, interceptable, or extensible from the external module API.
- If augmentation is possible, define a minimal adapter that adds one extra rectangle series without rewriting the whole chart.
- If augmentation is not possible, explicitly stop and retain a separate larva visualization path.

**Exit criteria:**

- Feasibility is proven or disproven with code-level evidence.
- The plan clearly states whether Base Control augmentation remains in scope.

### Epic 6: Larva-to-hatchery assignment foundation

**Objective:** build the replay analysis primitives needed to assign larva to hatcheries with enough confidence for visualization.

**Scope:**

- Parse hatchery and larva tracker events.
- Correlate `SpawnLarva` command targets where available.
- Define a calibration routine using simple Zerg replays with one hatchery and three larva at game start.
- Derive a documented "below the hatchery" directional/offset heuristic from those replays.
- Combine proximity, ownership, timing, and the "below" heuristic into `LarvaAssignmentHeuristic.java`.

**Exit criteria:**

- The analyzer can emit per-hatchery larva counts over time.
- The "below" heuristic is documented and configurable rather than implicit.

### Epic 7: Larva windows and red-rectangle visualization

**Objective:** generate the actual hatchery windows where larva count is at least 3 and show them as red rectangles.

**Scope:**

- Build `HatcheryLarvaTimeline` and `LarvaWindow` models.
- Convert larva-count intervals into red rectangle segments.
- Render one row per hatchery and separate by player.
- If native Base Control augmentation is possible, plug rectangles there.
- Otherwise render them in the fallback or module-owned chart surface.
- the line of a hatchery starts from the time the building is completed and has a larva, not from time 0:00 of the match. same for the end of of the line - when the building is destroyed.
- when one hatchery accumulates 11 seconds over time with 3+ larva, one thick black line is shown on the line chart. that happens every 11 accumulated seconds. 
- below the line of the hatchery there is a text with message "[x] potential larva missed".
- on top of the charts there is overview message saying "[y] total potential larva missed in this match".
- do not show charts for hatcheries that we not completed during building, that created zero larva. 
- in a hover over a missed larva, show current minerals and gas with blue and green numbers.

**Exit criteria:**

- Red rectangles are rendered from real replay-derived windows.
- The visualization is organized by player and hatchery.

### Epic 8: Hardening and validation

**Objective:** turn the working prototype into a reliable module.

**Scope:**

- Add replay fixtures and golden outputs.
- Validate replay switching, disabled/enabled module state, and packaging reproducibility.
- Add diagnostics for ambiguous larva assignments.
- Document supported and unsupported integration modes.

**Exit criteria:**

- The module is reproducible to build and verify.
- Known limitations are documented.

## Required infrastructure to create

- A dedicated module source tree, preferably under a new workspace folder such as `LarvaChartExtMod/`, based on the SDK conventions rather than modifying app code.
- Ant-based release packaging driven by a copied/adapted SDK [ScelightExtModSDK/build.xml](../ScelightExtModSDK/build.xml).
- Module metadata in a copied/adapted [ScelightExtModSDK/release/release.properties](../ScelightExtModSDK/release/release.properties).
- External module manifest generation inputs, including module name, folder identifier, version, icon, and deployment naming.
- A minimal runtime entrypoint class implementing the required Scelight external module API registration hooks.
- A replay-analysis service layer that is decoupled from UI classes.
- A visualization adapter layer that can either:
  - integrate with a supported chart extension interface if the API allows it, or
  - publish an alternate visualization surface if direct chart dropdown injection is not exposed.
- Test replay fixtures and deterministic validation outputs.
- Build-time and runtime documentation describing how to install and verify the module.

## Proposed project/module layout

Recommended top-level structure for the new module project:

- `LarvaChartExtMod/`
  - `src/`
    - `hu/.../larvachart/`
      - `LarvaChartModule.java` — module entrypoint and registration bootstrap.
      - `ModuleServices.java` — simple service wiring for Java 7.
      - `api/`
        - `LarvaWindowProvider.java` — internal abstraction for computed windows.
      - `replay/`
        - `LarvaReplayAnalyzer.java`
        - `LarvaAssignmentHeuristic.java`
        - `SpawnLarvaCorrelator.java`
        - `HatcheryLarvaTimeline.java`
        - `LarvaWindow.java`
      - `visual/`
        - `LarvaChartAdapter.java`
        - `LarvaOverlayAdapter.java`
        - `LarvaTableViewAdapter.java`
      - `integration/`
        - `ScelightChartIntegration.java`
        - `ScelightFallbackIntegration.java`
      - `validation/`
        - `AnalysisSanityChecks.java`
  - `lib/`
    - third-party jars only if absolutely necessary; prefer none.
  - `release/`
    - `release.properties`
    - `resources/`
      - `module-icon.png`
      - optional help/readme assets
    - `deployment/`
      - generated packaging output
  - `bin/`
    - compiled classes output for Ant packaging
  - `test-replays/`
    - small curated replay corpus for manual and scripted validation
  - `docs/`
    - `module-design.md`
    - `validation-checklist.md`

This layout keeps replay parsing, UI integration, and packaging separate. It also avoids tight coupling that would make later fallback modes difficult.

## Build and packaging infrastructure

The build should follow the SDK packaging model instead of inventing a separate system. Required assets:

- A module-local Ant build file derived from [ScelightExtModSDK/build.xml](../ScelightExtModSDK/build.xml).
- A module-local `release.properties` derived from [ScelightExtModSDK/release/release.properties](../ScelightExtModSDK/release/release.properties).
- Build properties configured for:
  - module `name`
  - module `version`
  - module `folder`
  - `iconFile`
  - `deploymentName`
  - `archiveBaseUrl`
  - local `scelightFolder`
  - local `backupFolder`
- A packaging rule that compiles `src/` to `bin/`, jars `bin/` into the module jar, copies any `lib/` dependencies, generates `Scelight-mod-x-manifest.xml`, and creates the deployment zip.
- Optional `module.xml` generation only if required by the runtime discovery path.
- Explicit Java 7 source/target settings in the Ant compilation task.
- Dependency hygiene rules:
  - no Java 8+ bytecode
  - no lambda-capable libraries
  - no dependencies requiring newer JRE APIs
- A repeatable install target that deploys directly into a local Scelight test installation.

If the workspace later needs Gradle or Maven convenience wrappers, they should only orchestrate the SDK-compatible Ant packaging, not replace it.

## Runtime integration infrastructure

The runtime layer must answer one question early: can a pure external module register a new chart dropdown entry? Infrastructure should support both outcomes.

Primary integration infrastructure:

- `LarvaChartModule.java` as the module entrypoint.
- `ScelightChartIntegration.java` to encapsulate all use of chart-related API extension points.
- A registration contract for:
  - chart descriptor or chart provider
  - label/id mapping using the desired dropdown name `larva`
  - lifecycle-safe initialization and cleanup
- Defensive feature detection so the module can determine at startup whether the target chart extension point is actually available through the external API.

Fallback integration infrastructure if direct dropdown registration is not possible:

- `ScelightFallbackIntegration.java` that can register:
  - a custom analysis panel
  - a replay overlay
  - a dedicated module view
  - an export action producing a time-series artifact consumable outside the standard chart dropdown
- A naming strategy that preserves the user-facing concept `larva` even if it appears outside the native chart selector.
- A capability-reporting mechanism shown in logs or module diagnostics so users know whether native chart integration is active or fallback mode is in use.

## Replay data infrastructure

Because no direct larva-to-hatchery parent relation is known, replay analysis infrastructure must be designed around reconstruction rather than direct lookup.

Required components:

- A tracker-event reader abstraction over Scelight-exposed replay data.
- A hatchery registry keyed by unit identity and timeline state.
- A larva state registry keyed by larva identity, owner, and coordinates over time.
- A `SpawnLarvaCorrelator` that uses Spawn Larva command target hatchery tags where available as a strong signal.
- A `LarvaHeuristicCalibration` component that derives the expected larva placement band under a hatchery from the start of the Zerg replay with one hatchery and three larva.
- A `LarvaAssignmentHeuristic` for non-command-based inference, likely combining:
  - nearest eligible hatchery by position
  - "below the hatchery" directional bias calibrated from the start of the replay itself
  - ownership consistency
  - timing windows
  - hatchery life-state filtering
- A normalized `HatcheryLarvaTimeline` output model producing:
  - timestamped larva counts
  - derived windows where count is at least 3
  - optional confidence metadata when assignments are heuristic
- Replay cache infrastructure so repeated UI redraws do not re-run expensive reconstruction.
- Clear invalidation rules tied to replay/session lifecycle.

## Visualization infrastructure

The visualization layer should consume only normalized output from replay analysis.

Required components:

- A `LarvaWindowProvider` interface returning hatchery windows in a chart-friendly structure.
- `LarvaChartAdapter.java` for native chart integration if supported.
- `BaseControlAugmentationAdapter.java` for the experimental path where extra red rectangles can be injected into Base Control.
- `LarvaOverlayAdapter.java` to draw time windows or saturation markers on an alternate timeline/overlay surface.
- `LarvaTableViewAdapter.java` to present computed hatchery intervals in a non-chart fallback UI.
- A common presentation model describing:
  - hatchery identifier
  - start/end frame or time
  - window duration
  - optional confidence level
- A color/style configuration class so visuals are not hard-coded into analysis logic.
- Localization-ready labels if the external API supports text resources.

## Validation and testing infrastructure

Testing infrastructure should verify module packaging, runtime discovery, replay analysis correctness, and UI survivability.

Required assets:

- A curated replay set in `test-replays/` including:
  - early-game hatchery inject cycles
  - natural/third hatchery scenarios
  - replays with missed injects
  - edge cases with destroyed or lifted state changes if relevant
- Golden-output fixtures, such as expected larva windows per replay, stored in simple text or CSV form.
- A manual validation checklist in `docs/validation-checklist.md`.
- A lightweight verification harness or diagnostic mode that logs:
  - discovered hatcheries
  - larva assignment decisions
  - generated `>=3` windows
  - whether native chart integration or fallback integration was activated
- An optional structured diagnostic dump file for dev-mode runs so validation can be inspected without depending entirely on manual UI navigation.
- Packaging verification steps to confirm the deployment zip and installed module match expected manifest contents.
- A compatibility check against the target Scelight version matching the external module API jar version.

## Risks and unknowns

- The largest risk is that adding a new chart dropdown entry named `larva` may not be exposed through the external module API and may require app-internal chart registration.
- Adding extra rectangles to the built-in Base Control chart may also be impossible from a pure external module and should be treated as a later feasibility test, not as a guaranteed path.
- Larva attribution to hatcheries may remain probabilistic for some replay periods because tracker data lacks a direct parent relation.
- The "larva is below its hatchery" rule may vary by map orientation, coordinate system interpretation, or replay version if not carefully calibrated from tracker coordinates.
- Spawn Larva command target correlation may cover only injected larva creation, not all states needed for a continuous hatchery larva count model.
- External API version drift may limit which integration points are actually stable.
- Java 7 restrictions reduce available helper libraries and require careful avoidance of modern language features.

## Recommended implementation order

1. Complete Epic 1 with a pure hello-world external module.
  - Include the optional zero-click diagnostic dump path during development so external-app verification is easier to automate.
2. Complete Epic 2 by exposing some module-owned replay-view entry point.
3. Complete Epic 3 by rendering a trivial chart or timeline somewhere reachable.
4. Attempt Epic 4 to determine whether native `larva` chart dropdown integration is possible.
5. Attempt Epic 5 to determine whether Base Control chart augmentation is possible.
6. Build Epic 6 replay-analysis and larva assignment calibration.
7. Implement Epic 7 real larva windows and red rectangles on the best available rendering path.
8. Finish with Epic 8 hardening, fixtures, diagnostics, and documentation.

## Definition of ready

The infrastructure is ready when:

- a module project exists with SDK-compatible source, release, and packaging layout;
- Java 7 compilation is enforced;
- deployment zip generation works reproducibly;
- the module installs into a local Scelight instance;
- the module has already passed a hello-world milestone before complex replay analysis begins;
- a replay-view-accessible visualization path exists, even if it is not the final chart-dropdown path;
- runtime startup can detect and report whether native chart dropdown integration is available;
- runtime startup can detect and report whether Base Control augmentation is available;
- replay analysis scaffolding can load a replay and emit normalized hatchery larva windows;
- at least one visualization path exists, even if it is a fallback rather than a native chart dropdown entry;
- validation fixtures and a manual verification checklist exist;
- unresolved pure-external-module limitations are explicitly documented, with fallback infrastructure already in place.

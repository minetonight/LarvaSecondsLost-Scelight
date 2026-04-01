# Epic 08 — Hardening and validation

## Epic goal

Harden the Epic 07 larva-window and missed-larva visualization so it becomes a reliable, repeatable, and well-validated Scelight external module feature.

This epic exists because Epic 07 already proved the supported rendering path: replay-derived per-hatchery larva count timelines can be converted into stable red `3+ larva` windows, rendered per hatchery and grouped by player on the module-owned `Larva` page. Epic 08 should now focus on trustworthiness rather than new visualization concepts: replay fixtures, golden outputs, replay edge cases, deterministic diagnostics, packaging reproducibility, and final documentation.

The final supported product is still the external-module-owned fallback page, not unsupported native chart dropdown registration or unsupported Base Control augmentation. All implementation must remain Java 7 compatible.

## Context

From the Epic 07 handoff summary in the module README:

- Epic 07 now converts replay-derived per-hatchery larva count timelines into stable red `3+ larva` windows on the supported module-owned `Larva` timeline.
- Epic 07 now renders one row per qualifying hatchery, grouped by player, with row visibility bounded by hatchery lifetime instead of replay start.
- Epic 07 now accumulates one missed-larva marker every 11 seconds of `3+ larva` saturation and surfaces per-hatchery plus per-player totals from the same normalized model.
- Epic 07 now attaches replay-derived minerals, gas, and supply context to red windows and missed-larva markers and renders that context in hover tooltips.
- Epic 07 proved the project can remain on the supported fallback rendering path without unsupported native chart registration or Base Control augmentation.
- Epic 07 left three unresolved areas: replay edge cases, replay-fixture and golden-output coverage, and final documentation cleanup.

From the project goal and know-how documents:

- the feature goal is to calculate and visualize the time windows when a hatchery has `3+` larva
- the supported visualization remains chart-like and separated by player and hatchery
- one missed larva is counted every 11 accumulated seconds of `3+` larva saturation
- hover detail should expose minerals and gas context, and the current implementation already has a model path for that data
- Java 7 syntax and Java 7 API compatibility remain strict requirements for source and dependencies
- Scelight external modules should keep using the SDK-style Ant packaging flow and install into Scelight's `mod-x` structure

From the infrastructure plan:

- Epic 08 is the hardening and validation phase for the working prototype
- the expected outputs are replay fixtures, golden outputs, diagnostics for ambiguous larva assignments, and documented supported / unsupported integration modes
- packaging reproducibility and validation workflow should be part of the finished epic, not left implicit

From the current module structure:

- replay analysis and timeline building already exist in classes such as `LarvaReplayAnalyzer`, `LarvaSaturationWindowCalculator`, `LarvaMissedLarvaMarkerCalculator`, and `LarvaTimelineModelBuilder`
- the module already has a development diagnostic path through `DevDiagnosticDumpWriter`
- the build and install workflow already goes through `ant BUILD_RELEASE` and `ant INSTALL_DEPLOYMENT`

## Non-goals

- no attempt to add a native `larva` entry to Scelight's built-in chart dropdown
- no attempt to augment Scelight's native `Base Control` chart through unsupported internals
- no switch away from the SDK-style Ant build and deployment flow
- no migration away from Java 7 syntax or Java 7-compatible APIs
- no large new feature beyond hardening, validation, diagnostics, and documentation of the existing Epic 07 pipeline

## Story 08.01 — Create replay fixtures and golden outputs

**As a** developer  
**I want** a curated replay-fixture corpus with expected outputs  
**So that** larva windows, missed-larva markers, totals, and hover-context generation can be verified repeatedly instead of only by manual inspection.

### Acceptance criteria

- A dedicated replay-fixture set exists for Epic 08 validation.
- The fixture set covers at least:
  - a simple single-hatchery Zerg opening,
  - multi-hatchery growth,
  - hatchery destruction before replay end,
  - replay(s) with clear missed inject periods,
  - replay(s) with sparse or imperfect stats sampling,
  - replay(s) that stress morph continuity across `Hatchery` / `Lair` / `Hive`.
- Each fixture has a deterministic golden output in a simple Java-7-friendly format such as text, CSV, or properties-style data.
- Golden outputs include, at minimum:
  - visible hatchery rows,
  - `3+ larva` window ranges,
  - missed-larva marker times,
  - per-hatchery totals,
  - per-player totals.
- Golden outputs are stored in a location that is easy to diff and review.
- The fixture set and golden-output format are documented so later epics can extend them without reverse-engineering the validation scheme.

### Notes

This story is the backbone of Epic 08. Without fixture coverage, later hardening changes will not be trustworthy.

## Story 08.02 — Harden replay-analysis and timeline edge cases

**As a** developer  
**I want** the Epic 07 pipeline hardened against replay edge cases  
**So that** the module behaves predictably across real-world replays instead of only clean happy paths.

### Acceptance criteria

- The larva pipeline is reviewed end-to-end across `LarvaReplayAnalyzer`, `LarvaSaturationWindowCalculator`, `LarvaMissedLarvaMarkerCalculator`, and `LarvaTimelineModelBuilder`.
- The implementation handles or explicitly documents edge cases including:
  - unusual hatchery morph timing,
  - hatchery death corner cases,
  - replay truncation or incomplete event tails,
  - sparse player-resource sampling,
  - ambiguous larva assignment periods,
  - timelines with out-of-order or zero-duration transitions.
- Saturation windows never produce negative or zero-length visible segments.
- Missed-larva markers remain monotonic and deterministic even when `3+ larva` saturation is intermittent across multiple windows.
- Hatchery rows remain bounded by valid lifetime rules and never extend beyond replay end.
- If the module cannot resolve an edge case confidently, diagnostics explain the limitation instead of silently fabricating stable-looking output.

### Notes

Hardening should prefer explicit failure-safe behavior over clever guessing. The goal is a trustworthy visualization, not maximal speculative assignment.

## Story 08.03 — Strengthen diagnostics and verification output

**As a** developer  
**I want** deterministic diagnostics for larva-window validation  
**So that** I can distinguish analysis bugs, visualization bugs, and packaging/runtime issues quickly.

### Acceptance criteria

- Development diagnostics can report the final replay-derived hatchery rows, `3+ larva` windows, missed-larva markers, and totals in a stable textual form.
- Diagnostics can also report ambiguous or unassigned larva cases with reason categories.
- The existing development dump path is extended only in a way that remains safe for Java 7 and easy to compare against golden outputs.
- Validation output distinguishes at least these concerns:
  - replay-analysis result,
  - timeline-model result,
  - integration-mode / supported-path result,
  - packaging/runtime lifecycle result.
- A developer can run the module, inspect one predictable diagnostic output, and understand whether a failure comes from replay parsing, saturation-window conversion, marker accumulation, or UI-model assembly.
- The diagnostic format is documented in a short developer-facing note.

### Notes

The current dev dump already proves lifecycle and replay-analysis basics. Epic 08 should make it useful for validating the full Epic 07 output model.

## Story 08.04 — Validate packaging, installation, and reproducibility

**As a** developer  
**I want** the module build and deployment flow validated as reproducible  
**So that** hardening changes do not break installation or drift away from Scelight external-module conventions.

### Acceptance criteria

- `ant BUILD_RELEASE` still produces the expected release and deployment artifacts.
- `ant INSTALL_DEPLOYMENT` still installs the module cleanly into a local Scelight folder and removes prior installed copies of the same module.
- Validation confirms that deployment contents still match the expected SDK-style external-module structure under Scelight's `mod-x` folder.
- Validation confirms that the module continues to compile with Java `source=1.7` and `target=1.7`.
- The reproducibility checklist includes at least:
  - build,
  - install,
  - enable module in Scelight,
  - open the `Larva` page,
  - load a validation replay,
  - verify visible output and diagnostic output.
- The workflow remains aligned with Scelight external-module know-how rather than introducing unsupported packaging shortcuts.

### Notes

This story closes the gap between "the code works on one machine" and "the module can be rebuilt and revalidated predictably."

## Story 08.05 — Finalize documentation and handoff summary

**As a** developer  
**I want** Epic 08 outcomes and limitations documented clearly  
**So that** future work can start from a validated baseline instead of rediscovering project constraints.

### Acceptance criteria

- README handoff sections are updated to reflect what Epic 08 has proven.
- The supported rendering path and unsupported native integration paths remain documented clearly.
- Replay-fixture usage and validation steps are documented in a concise developer-facing checklist.
- Any known edge cases that remain unresolved are documented explicitly.
- A short note states what Epic 08 has proven:
  - replay-derived larva windows and missed-larva markers are validated against fixture-based golden outputs,
  - diagnostics can explain ambiguous or edge-case outcomes,
  - packaging and installation remain reproducible through the SDK-style flow,
  - the module remains on the supported external-module rendering path.
- A short note states what is still unresolved, if anything remains after hardening.
- The next technical question for later work is identified explicitly instead of being left implicit.

### Notes

If Epic 08 closes the current roadmap, the final note may state that further work is optional polish rather than a required prerequisite.

## Definition of done

Epic 08 is done when:

- replay fixtures exist and are documented
- golden outputs cover larva windows, missed-larva markers, and totals
- key edge cases are either handled safely or documented explicitly
- deterministic diagnostics can explain replay-analysis and timeline-model outcomes
- build and install flow remain reproducible with the SDK-style Ant workflow
- Java 7 compatibility remains intact
- README and developer validation notes reflect the true supported solution and current limitations

## Risks in this epic

- Replay fixtures may reveal that some Epic 07 assumptions are unstable across real matches.
- Player-resource snapshots may be too sparse on some replays to support perfect hover context at every desired instant.
- Hardening may expose that certain ambiguous larva assignments should remain unresolved instead of forced into a misleading visualization.
- Diagnostic output could become noisy unless it is structured for comparison and triage.
- It may be tempting to add new feature scope while fixing edge cases; Epic 08 should stay focused on reliability and validation.

## Suggested implementation order

1. Story 08.01 — Create replay fixtures and golden outputs
2. Story 08.02 — Harden replay-analysis and timeline edge cases
3. Story 08.03 — Strengthen diagnostics and verification output
4. Story 08.04 — Validate packaging, installation, and reproducibility
5. Story 08.05 — Finalize documentation and handoff summary

# Larva Seconds Lost validation checklist

This note documents the Epic 08 Story 08.03 and Story 08.04 validation flow.

## Diagnostic dump structure

When development dump mode is enabled, the dump file is expected to contain these sections:

- `[Lifecycle]` — module startup / shutdown state
- `[Analysis]` — replay summary, larva analysis, and deterministic validation snapshot
- `[Capabilities]` — supported vs unsupported native integration modes
- `[Verification]` — a structured triage summary separating:
  - replay-analysis result,
  - timeline-model result,
  - integration-mode result,
  - packaging/runtime result.

Use the dump to identify the failing stage quickly:

- if `Replay-analysis result` failed, focus on replay parsing or larva assignment
- if `Timeline-model result` is unavailable, focus on timeline-model assembly
- if `marker accumulation` or `saturation-window conversion` counters look wrong, compare against the golden files in [docs/epic08-fixtures/goldens](epic08-fixtures/goldens)
- if `Packaging/runtime result` is not `started`, focus on installation, module enablement, or JVM launch configuration

## Reproducibility checklist

Run this checklist after changes that affect analysis, rendering, or packaging.

1. **Build**
   - Run `ant BUILD_RELEASE` from the module project root.
   - Confirm the build succeeds with Java `source=1.7` and `target=1.7`.

2. **Inspect release artifacts**
   - Confirm [release/Scelight](../release/Scelight) exists.
   - Confirm the module payload exists under `release/Scelight/mod-x/larva-seconds-lost/<version>/`.
   - Confirm [release/deployment/module.xml](../release/deployment/module.xml) exists.
   - Confirm the deployment zip exists under `release/deployment/`.

3. **Install**
   - Run `ant INSTALL_DEPLOYMENT`.
   - Confirm the target Scelight folder contains `mod-x/larva-seconds-lost/<version>/`.
   - Confirm older installed copies were replaced.

4. **Run in Scelight**
   - Start Scelight.
   - Enable the module if needed.
   - Open the `Larva` page.
   - Load a validation replay.

5. **Verify visible output**
   - Confirm each player header shows the Epic 12 phase table.
   - Confirm the phase table contains Early / Mid / Late / End columns.
   - Confirm the four phase metrics render: missed larva rate, missed inject rate, spawned larva rate, and inject uptime.
   - Confirm hatchery rows render.
   - Confirm red `3+ larva` windows render.
   - Confirm missed-larva markers and totals render.

6. **Verify diagnostic output**
   - Confirm the dump file contains `Deterministic validation snapshot`.
   - Confirm the dump file contains the `[Verification]` section.
   - Confirm the deterministic snapshot now includes `timeline.phaseTableCount` and per-player phase-table entries.
   - Compare the deterministic snapshot against the expected fixture golden file when applicable.

## Expected packaging convention

The supported packaging flow remains the SDK-style Ant flow only:

- source in `src/`
- compiled output in `bin/`
- staged module tree in `release/Scelight/mod-x/larva-seconds-lost/<version>/`
- deployment artifacts in `release/deployment/`

Do not replace this with unsupported custom packaging shortcuts.
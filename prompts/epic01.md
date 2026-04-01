# Epic 01 — Hello World module baseline

## Epic goal

Prove that a Scelight external module for the larva feature can be built, packaged, installed, enabled, and observed at runtime with the smallest possible implementation.

This epic does **not** implement larva analysis yet. Its purpose is to remove build and integration uncertainty first.

## Context

The module must:

- use the Scelight external module SDK structure
- compile with Java 7 syntax only
- produce a deployable module package
- load into a local Scelight installation
- expose some visible or logged proof that the module is alive

This epic is the foundation for all later epics.

## Non-goals

- no chart rendering yet
- no replay-view integration yet
- no larva-to-hatchery assignment yet
- no Base Control chart augmentation yet

## Story 01.01 — Create the standalone module project

**As a** developer  
**I want** a dedicated external module project based on the Scelight SDK  
**So that** the larva work is isolated from app internals and can be packaged correctly.

### Acceptance criteria

- A dedicated module project/folder exists for the larva extension.
- The project contains SDK-style folders: `src`, `release`, `lib`, `bin`.
- The Scelight external module API jar is referenced for compilation.
- The project layout is documented briefly for future work.

### Notes

This story should prefer copying/adapting the SDK structure instead of inventing a custom project shape.

## Story 01.02 — Configure module identity and packaging metadata

**As a** developer  
**I want** release metadata configured for the new module  
**So that** Scelight can recognize, load, and display the module correctly.

### Acceptance criteria

- `release.properties` is customized for the larva module.
- The module has a stable folder id, display name, version, and deployment name.
- An icon asset exists or a temporary placeholder icon is configured.
- Manifest generation is wired to produce a valid `Scelight-mod-x-manifest.xml` during build.

### Notes

Use temporary values if needed, but choose names that will not need a large rename later.

## Story 01.03 — Create the minimal module entrypoint

**As a** developer  
**I want** a minimal external module main class  
**So that** the module can initialize successfully inside Scelight.

### Acceptance criteria

- A main module class exists and implements the required external module API contract.
- The class has a no-arg constructor if required by the manifest/API.
- Initialization code is minimal and safe.
- Startup and shutdown behavior are separated clearly enough for later expansion.

### Notes

Keep this class small. It should bootstrap only what Epic 01 needs.

## Story 01.04 — Add visible hello-world proof

**As a** developer  
**I want** a simple visible or logged hello-world behavior  
**So that** I can confirm the module is enabled and alive at runtime.

### Acceptance criteria

- Enabling the module results in a clear observable signal.
- The signal is either:
  - a log message,
  - a simple dialog,
  - a tiny visible UI contribution,
  - or another safe confirmation supported by the external API.
- The confirmation does not require replay parsing.
- The behavior can be disabled or replaced cleanly in later epics.

### Notes

Prefer the least invasive runtime proof first. Logging is acceptable if it is easy to confirm.

## Story 01.05 — Make the module buildable from the workspace

**As a** developer  
**I want** a repeatable local build flow  
**So that** I can iterate on the module without manual packaging mistakes.

### Acceptance criteria

- The module builds from the workspace using the SDK-compatible Ant flow.
- The build produces the module jar and deployment zip.
- The build does not require Java features newer than Java 7.
- The build instructions are written down in a short developer note.

### Notes

If additional compile configuration is needed to enforce Java 7 source/target, do it now.

## Story 01.06 — Install the module into a local Scelight instance

**As a** developer  
**I want** the built deployment installed locally  
**So that** I can validate the end-to-end packaging and runtime path.

### Acceptance criteria

- The deployment can be installed into a local Scelight folder.
- Old copies of the same module are removed or replaced predictably.
- The installed module appears in Scelight’s module management flow.
- The module can be enabled without breaking startup.

### Notes

This story proves deployment correctness, not feature completeness.

## Story 01.07 — Verify runtime lifecycle and diagnostics

**As a** developer  
**I want** basic lifecycle diagnostics  
**So that** later epics can distinguish packaging failures from runtime integration failures.

### Acceptance criteria

- Module startup is logged.
- Module shutdown or disable behavior is also logged if the API allows it.
- Any initialization failure is surfaced with a useful message.
- A short troubleshooting note is added for common failure points.

### Notes

This is especially useful before replay-view work starts.

## Story 01.08 — Establish Epic 02 handoff conditions

**As a** developer  
**I want** a clean handoff from hello-world to replay-view work  
**So that** the next epic starts from a proven baseline.

### Acceptance criteria

- There is a short note stating what runtime extension points are still unknown.
- There is a short note stating what is now proven:
  - build works,
  - packaging works,
  - installation works,
  - module initialization works.
- The next technical question is clearly identified as: "where can a mod surface something during replay analysis?"

## Definition of done

Epic 01 is done when:

- the module project exists in SDK-compatible structure
- the module compiles with Java 7 only
- a deployment zip is produced reproducibly
- the module installs into a local Scelight instance
- the module can be enabled successfully
- a visible or logged hello-world confirmation is observed
- basic lifecycle diagnostics exist
- the project is ready to begin replay-view accessibility work in Epic 02

## Risks in this epic

- The external module API may expose fewer UI hooks than expected.
- Packaging may succeed while runtime loading still fails because of manifest or classpath issues.
- The local Scelight install path may need careful configuration for repeatable deployment.

## Suggested implementation order

1. Story 01.01 — Create the standalone module project
2. Story 01.02 — Configure module identity and packaging metadata
3. Story 01.03 — Create the minimal module entrypoint
4. Story 01.05 — Make the module buildable from the workspace
5. Story 01.06 — Install the module into a local Scelight instance
6. Story 01.04 — Add visible hello-world proof
7. Story 01.07 — Verify runtime lifecycle and diagnostics
8. Story 01.08 — Establish Epic 02 handoff conditions

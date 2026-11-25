# Epic 18 — Online release and updates

| | |
|---|---|
| **Goal** | Every Scelight install that uses Larva Seconds Lost (the user runs at least 3) gets new versions from the web, with no manual zip copying after a one-time install. Any SC2 player can install it from a public page. |
| **Source** | `/plan` request 2026-11-25, plus `prompts/v2-idea.md` "Release prep" |
| **Created** | 2026-11-25 |
| **Status** | In progress. Code and docs done 2026-11-25 (S01–S10, S12, S13; `ant TEST` 300 passing). Open: S00 spike, S06 GitHub repo (user), S14 first release and rollout. See §11. |
| **Base URL** | `https://minetonight.github.io/` (user decision) |

---

## 0. Read this first: how Scelight updates external modules

I checked this in the Scelight source (`scelight/src-launcher/hu/sllauncher/service/updater/Updater.java`). It decides the shape of this epic.

1. **Scelight only auto-updates "Official External Modules".** At startup the launcher downloads `https://scelightop.appspot.com/bean/modules.xml.gz`. It has an `<extModRefList>`, and each entry holds a `moduleBeanUrl`. For each official module the user has set to auto-update, the launcher downloads that `module.xml`, then downloads the zip from `<urlList>`, checks the SHA-256 and the hash of every file, and installs it into `Scelight/mod-x/<folder>/<version>/`.
2. **The official list is controlled by Scelight's author, not by us, and he is no longer active on Scelight.** It was fetched on 2026-11-25 and has exactly one entry: `SC2ReplayStatsUploader`. You cannot add your own `moduleBeanUrl` in Scelight's settings. A module that is not on the list is a "manual install" module, and the launcher never updates it.
3. **Scelight loads the highest version folder.** `Updater.getLatestVersionSubfolder()` picks the newest valid version subfolder under `mod-x/larva-seconds-lost/`. So if a new folder `3.2/` appears next to `3.1/`, Scelight uses `3.2` on the next start.
4. **A module can write to its own folder.** `IModEnv.getRootFolder()` returns `Scelight/mod-x/larva-seconds-lost/`, and `getCacheFolder()` is also writable.

**Result: we are on our own. The module updates itself.**

The module checks the hosted `module.xml`, shows "version X is available", downloads and verifies the zip, unpacks it into a new version folder and asks the user to restart. Fact 3 makes this work, and it depends on nobody.

> **User decision (2026-11-25): never contact the Scelight author or ask for an official listing.** No story, doc or agent may suggest it.

We still use the SDK's `module.xml` format, because the Ant build already generates it with all hashes, and it is a well-defined format.

**Bootstrap:** the three existing installs run 3.0, which has no updater. Each machine needs **one last manual install** of the first online version (3.1). After that, updates come through the app.

---

## 1. Story index

| ID | Story | Phase | Size | Depends on |
|---|---|---|---|---|
| E18-S00 | Spike: Scelight's JVM can fetch from github.io over HTTPS — **open** (folded into S14: `ant SMOKE_CHECK_RELEASE` + Check now on each machine) | 0 | S | — |
| E18-S01 | Remove personal laptop paths (build and docs) — **done** | 1 | S | — |
| E18-S02 | Scrub personal paths in benchmark data — **done** | 1 | M | — |
| E18-S03 | Update the manifest: description, short description, home page — **done** (check in Scelight at S14) | 1 | S | OQ-1 |
| E18-S04 | Set `archiveBaseUrl` and the hosted folder layout — **done** | 1 | S | OQ-1 |
| E18-S05 | Release lint: Ant `CHECK_RELEASE` — **done** | 1 | S | S01, S04 |
| E18-S06 | Create the GitHub Pages host and landing page — **done** (live) | 2 | S | OQ-1 |
| E18-S07 | Publish script: Ant `PUBLISH_RELEASE` — **done** (dry-run against a scratch repo) | 2 | M | S05, S06 |
| E18-S08 | Update-check core (pure logic, JUnit) — **done** | 3 | M | S04 |
| E18-S09 | Update notice on the Larva hub, plus a setting — **done** (not yet seen in a running Scelight) | 3 | M | S08 |
| E18-S10 | One-click install of a new version and cleanup of old versions — **done** (unit-tested; live test at S14) | 3 | L | S08, S09 |
| ~~E18-S11~~ | Removed (no official listing, user decision) | — | — | — |
| E18-S12 | `README.md` at the project root: install guide — **done** | 4 | S | S06 |
| E18-S13 | `RELEASING.md`: how to publish a release (for agents and the user) — **done** | 4 | S | S07 |
| E18-S14 | First online release 3.1 and rollout to the 3 machines | 5 | M | all above |

Order: S00 and Phase 1 can run in parallel. Phase 2 needs OQ-1. Phase 3 needs the hosted URL to be fixed (S04), but not live. S14 is last.

```
S00 ─────────────────────────────────────────────┐
S01 ─┬─ S05 ─┐                                   │
S04 ─┘       ├─ S07 ─── S13                       ├─ S14
S06 ─────────┘                                    │
S03 ─────────────────────────────────────────────┤
S08 ─ S09 ─ S10 ─────────────────────────────────┤
S06 ─ S12 ───────────────────────────────────────┘
```

---

## 2. Open questions (each has a default, so work is not blocked)

| OQ | Question | Default (used unless the user overrides it) |
|---|---|---|
| OQ-1 | Which repo serves `minetonight.github.io`? (a) the user-site repo `minetonight/minetonight.github.io`, path `/scelight/larva-seconds-lost/`; (b) GitHub Pages of this repo, path `/LarvaSecondsLost-Scelight/`. | **(a)**. The URL does not depend on this repo's name. Zips stay out of the dev repo history (this repo already has a lot of benchmark data). Later mods can sit next to it under `/scelight/`. Neither repo exists yet (`https://minetonight.github.io/` returns 404). |
| OQ-2 | Updater scope: notify only (open the landing page in a browser), or full one-click install? | **One-click install (S10)**, with the browser link as a fallback. With 3 machines, "download, unzip, restart" by hand each time is exactly the problem. |
| OQ-3 | Should the dev repo keep tracking `release/deployment/*.zip`? `.gitignore` currently has `*.zip` commented out. | **No.** Ignore `LarvaSecondsLostExtMod/release/deployment/*.zip` again. The Pages repo is the archive of record. `module.xml` in the dev repo is fine to keep. |
| OQ-4 | About 1,660 tracked files under `benchmark-data/exports` and `benchmark-data/reports` contain the developer's home path (generated extractor JSON). Scrub them? | **Yes, but as a separate commit (S02)** that replaces the prefixes with `<REPO>` and `<SCELIGHT_HOME>`. Also fix the extractor so new output has no absolute paths. Old commits keep the paths: rewriting git history is out of scope unless the user asks for it. |
| OQ-5 | Version number of the first online release. | **3.1.0.** It must be higher than the installed 3.0 so updates work. The manifest and description change, and it ships the updater. |
| OQ-6 | Minimum Java for users. | **Java 8.** GitHub Pages needs TLS 1.2, and Java 7 does not turn it on by default. The code stays Java 7 source level (project rule). Only the README states Java 8+. S00 confirms this. |

---

## 3. Phase 0 — Spike

### E18-S00 — Spike: Scelight's JVM can fetch from github.io over HTTPS

**Why:** The updater fails if the JVM that runs Scelight cannot open `https://*.github.io` (TLS version or missing root certificates on an old JRE).

**Do:**
- Find out which `java` binary runs Scelight on each of the 3 machines (`Scelight-linux.sh` / `Scelight.exe` / the OS X launcher). Write down the version.
- In the scratchpad, write a throwaway Java 7-compatible `main` that opens `https://minetonight.github.io/` (any reachable github.io URL is fine before S06), reads 1 KB, and prints the HTTP status, TLS protocol and certificate chain. Run it with that binary on each machine.
- Also test one redirecting URL (`https://github.com/<user>/<repo>/releases/download/...`), to decide if Release assets could be a second mirror in `<urlList>`.

**Done when:** a short note in `knowhow/epic18-https-spike.md` records the Java version for each machine and whether it passed. If a machine fails, OQ-6 becomes a README requirement ("update Java").

---

## 4. Phase 1 — Release preparation

### E18-S01 — Remove personal laptop paths (build and docs)

Tracked files with the developer's home path outside `benchmark-data/`:

| File | Now | Change |
|---|---|---|
| `LarvaSecondsLostExtMod/release/release.properties` | `scelightFolder=<home>/StarCraft II/Scelight/` | Remove the key. Move it to a new **gitignored** `release/local.properties`. Add `release/local.properties.example` with a placeholder and a comment. |
| `LarvaSecondsLostExtMod/build.xml` `INIT_PROPERTIES` | loads only `release.properties` | Load `${releaseFolder}/local.properties` **first** (Ant keeps the first value set, so a local value wins). `INSTALL_DEPLOYMENT`, `GOLDEN_MULTI_RUN` and `GOLDEN_MULTI_VIEW_RUN` fail with a clear message if `scelightFolder` is not set: "copy release/local.properties.example to release/local.properties". Also accept `-DscelightFolder=...`. |
| `LarvaSecondsLostExtMod/.classpath` | absolute path to `scelight-ext-mod-api-1.5.2.jar` | Make it relative to the project (`../ScelightExtModSDK/...`), or use a classpath variable. Check that VS Code Java / Eclipse still resolves it. |
| `knowhow/running-scelight.md`, `knowhow/epic17-batch-spike.md` | absolute paths in examples | Replace with `<SCELIGHT_HOME>` / `<REPO>` placeholders, and explain them once at the top. |

**Not affected:** the shipped zip. I unpacked `LarvaSecondsLost-3.0.zip` and its jar: no `/home/` string. S05 keeps it that way.

**Done when:** `git grep -n "$HOME" -- ':!benchmark-data'` returns nothing. `ant BUILD_RELEASE` works without `local.properties`. `ant INSTALL_DEPLOYMENT` works with it.

### E18-S02 — Scrub personal paths in benchmark data (optional, OQ-4)

- One-off script (keep it in the scratchpad, or commit it under `benchmark-data/tools/`) replaces the repo root with `<REPO>` and any other home-folder prefix with `~/` in `benchmark-data/exports/**` and `benchmark-data/reports/**`.
- `git rm --cached benchmark-data/tools/__pycache__/*.pyc` and add `__pycache__/` to `.gitignore`.
- Change `benchmark_java_extractor.py` (and the batch tools) so they write repo-relative paths, or the placeholders, in `extractorCommand` and `replayFilePath`.
- Check that no tool reads these fields back as real paths. If one does, resolve the placeholders at read time.

**Done when:** `git grep -c "$HOME"` returns nothing, and the benchmark reports regenerate the same (only the paths differ).

### E18-S03 — Update the manifest: description, short description, home page

File: `LarvaSecondsLostExtMod/release/resources/Scelight-mod-x-manifest-template.xml`. Scelight shows this text on its Installed Modules page.

- `<homePage>`: `https://minetonight.github.io/scelight/larva-seconds-lost/` (the landing page from S06). It is `example.invalid` now.
- `<shortDesc>` (one line, plain text). Draft: *"Zerg larva and inject analysis: missed larva, inject uptime, phase tables, benchmarks and multi-replay averages."*
- `<description>` (HTML 3.2, no scripts). Rewrite it to cover what exists after Epic 17: the Larva hub with Open Replay / Analyze Latest / Multi analysis; replay sub-pages that open automatically for new Zerg replays; the larva timeline; Early/Mid/Late/End phase tables; benchmark rankings; multi-replay tables per matchup with milestones (100/200 supply, 66/80 drones); and auto-update (after S10). Add one line with the home page and the issue link `https://github.com/minetonight/LarvaSecondsLost-Scelight/issues`.
- Leave `<iconImgData>` alone. It already matches `release/resources/larva-module-icon.png`.
- Add `<contact>` to `<personName>` only if the user wants a public contact. User decision: add discord nickname scorpion_rbp as contact.

**Done when:** after `ant BUILD_RELEASE`, the Installed Modules page in a local Scelight shows the new texts and a working home page link.

### E18-S04 — Set `archiveBaseUrl` and the hosted folder layout

- In `release.properties`: `archiveBaseUrl=https\://minetonight.github.io/scelight/larva-seconds-lost/`.
- The hosted layout is a **contract**. Once published, these URLs never change:

```
https://minetonight.github.io/scelight/larva-seconds-lost/
├── index.html                    landing page (S06), = <homePage>
├── module.xml                    ALWAYS the latest release; this URL never changes
├── LarvaSecondsLost-3.1.zip      immutable, never overwritten or deleted
├── LarvaSecondsLost-3.2.zip      ...
└── releases.md (or in index)     changelog, one entry per version
```

- Rules: every zip name contains its version and is never replaced (old `module.xml` files point to it by SHA-256). Only `module.xml` and `index.html` change for each release. To undo a bad release, **publish a newer version** built from the good commit. Never point `module.xml` at a lower version: no updater downgrades, so a lower version only confuses things.
- Keep `<urlList>` with one entry. Add a GitHub Release asset mirror only if S00 showed that redirects work.

### E18-S05 — Release lint: Ant `CHECK_RELEASE`

A new target that runs at the end of `BUILD_RELEASE`. The build fails if any of these is true:
- `module.xml` or the manifest contains `example.invalid`.
- `archiveBaseUrl` does not start with `https://minetonight.github.io/`.
- Any file in the deployment zip, including the unpacked jar, contains `/home/` or `C:\Users\` (use `<unzip>` to a temp folder, then `<fileset>` with `<contains>`).
- The version in `module.xml` is not higher than the last published version. Read it from the Pages checkout if `pagesRepoFolder` is set in `local.properties`, otherwise skip with a warning.
- Optional: the jar contains test classes or `*.log` files.

**Done when:** a broken property makes `ant BUILD_RELEASE` fail with a clear message, and the clean tree passes.

---

## 5. Phase 2 — Hosting

### E18-S06 — Create the GitHub Pages host and landing page

**The user does the GitHub parts (account actions).** An agent prepares the files and the exact commands.

- Create the public repo `minetonight/minetonight.github.io` (OQ-1 a). Turn on Pages from `main`, root folder. Add an empty `.nojekyll` so files are served as they are.
- `scelight/larva-seconds-lost/index.html`: a plain static page with what the module does, 2–3 screenshots (reuse `knowhow/epic17-multi-2players.png` or `knowhow/epic13-benchmarks.png`), the latest version and download link, the 5-step install from S12, and a changelog.
- Check: `curl -I` on `module.xml` and on the zip returns 200 and a sensible `Content-Type` (`application/zip`). The launcher adds a timestamp query to `module.xml` requests (`UrlBuilder.addTimestamp()`), so Pages caching (`max-age=600`) does not hide a new release from the launcher. The in-module checker (S08) must add a timestamp too.

### E18-S07 — Publish script: Ant `PUBLISH_RELEASE`

- New property `pagesRepoFolder` in `local.properties` (path to a local clone of the Pages repo).
- The target depends on `BUILD_RELEASE` + `CHECK_RELEASE`. It copies `LarvaSecondsLost-<ver>.zip` and `module.xml` into `${pagesRepoFolder}/scelight/larva-seconds-lost/`, **refuses to overwrite an existing zip with the same name**, updates the version and the link in `index.html` (fixed tokens or a small template), and prints the exact `git add / commit / push` commands.
- **It does not push.** Pushing publishes to every user, so a person runs it (agents: ask the user first, see S13).
- After the push: a smoke check (a small Ant `<get>` or a script) downloads the live `module.xml`, then the zip from the URL inside it, and checks the SHA-256 and size against `module.xml`.

---

## 6. Phase 3 — Update path

### E18-S08 — Update-check core (pure logic, JUnit)

Package `hu.aleks.larvasecondslostextmod.update` (business layer, no Swing):
- `RemoteRelease` value object: version, archive URL list, sha256, size, list of (path, sha256).
- `ModuleXmlParser`: parses `module.xml` with `javax.xml.parsers` (built into Java 7). It does not use launcher classes, because those are not part of the ext-mod API.
- `VersionComparator`: compares `major.minor.revision`, so `3.1` equals `3.1.0`, which is how the Ant script names folders.
- `UpdateChecker`: fetches `module.xml` (timestamp query, connect/read timeout 5 s, a clear `User-Agent` such as `LarvaSecondsLost/<ver>` with no personal data). It compares the result with the running version (from `IExtModManifestBean` given to `init`). It returns `UpToDate | Available(RemoteRelease) | Failed(reason)`. Failures are logged once, with the reason, and never shown as a dialog.
- The URL is a constant in one place (`UpdateConfig.MODULE_XML_URL`). It is also stored as a hidden property so it can be tested.

Tests (`ant TEST`, offline fixtures in `test/`): parse the real `release/deployment/module.xml`; version compare edge cases; missing or invalid fields → `Failed`; name or folder mismatch (`Larva Seconds Lost` / `larva-seconds-lost`) → `Failed`.

### E18-S09 — Update notice on the Larva hub, plus a setting

- At module start, run the check on a background thread at most once every 24 h (keep the last check time in the module's settings or cache folder). Never on the EDT, and never in the way of replay parsing.
- If an update is available, show a slim banner at the top of the Larva hub: `Version 3.2 is available — [Install] [What's new] [Hide]`. "What's new" opens `<homePage>` with `IUtils.showURLInBrowser`. "Hide" hides that version only.
- Settings, using the module's existing settings mechanism: `Check for updates automatically` (default **on**) and a `Check now` button that shows the result in place.
- Privacy: the check is a plain GET to GitHub Pages and sends no data. GitHub sees the IP address. Say this in one line in the module help and in the README.

### E18-S10 — One-click install of a new version and cleanup of old versions

On **Install**, all off the EDT, with a progress label on the banner:
1. Download the zip into `getCacheFolder()/update/`. Check that the size and SHA-256 match `RemoteRelease`.
2. Open the zip. **Reject** every entry that is not under `Scelight/mod-x/larva-seconds-lost/<newVersion>/`, and any entry with `..` or an absolute path (zip-slip). Check each file's SHA-256 against the file list.
3. Extract into `getRootFolder()/<newVersion>.part/`, then rename it to `<newVersion>/` (an atomic move if the OS supports it, otherwise copy and delete). If `<newVersion>/` already exists and is complete, skip this step.
4. Banner: `Version 3.2 installed — restart Scelight to use it`. Never restart automatically.
5. Cleanup at the next start: the running module (new version) deletes sibling version folders **lower** than itself. It never deletes its own `getVersionFolder()`. If the delete fails (for example a locked file on Windows), log it and try again next start.

Safety: if anything fails, delete `.part` and the cached zip, log the reason, and show `Update failed: <reason> — [Open download page]`. The installed version is never touched before the new folder is complete.

Tests: zip-slip rejection, hash mismatch, interrupted extract (`.part` left over → cleaned up), the cleanup selection never picks the running version. Use small synthetic zips in the tests.

### E18-S11 — Removed

There is no official listing (user decision, §0). Nobody contacts the Scelight author.

---

## 7. Phase 4 — Documentation

### E18-S12 — `README.md` at the project root: install guide

There is no root README yet. It is written for SC2 players, not developers:
1. What it is (2 sentences) and one screenshot.
2. Requirements: Scelight (link), Java 8+ (OQ-6), Windows / Linux / macOS.
3. **Install (one time):** download the latest zip from the landing page. Close Scelight. Extract the zip into the folder that **contains** the `Scelight` folder, so you get `Scelight/mod-x/larva-seconds-lost/<version>/`. Start Scelight. Go to *Installed Modules* → enable *Larva Seconds Lost* → restart.
4. **Updates:** the Larva page tells you when a new version is out → Install → restart. A manual update (extracting a newer zip the same way) also works.
5. Uninstall: delete `Scelight/mod-x/larva-seconds-lost/`.
6. Troubleshooting: where Scelight's log is, "module not shown" (wrong extract level), "update failed" (firewall, old Java).
7. For developers: link to `LarvaSecondsLostExtMod/README.md` and `RELEASING.md`.

### E18-S13 — `RELEASING.md`: how to publish a release (for agents and the user)

At the project root. Add it to the documentation list in `.claude/CLAUDE.md`. Replace the long "publish" part of `LarvaSecondsLostExtMod/docs/publishing-and-release.md` with a link, so there is only one source of truth.

Contents:
- One diagram of where things are: dev repo → `ant PUBLISH_RELEASE` → Pages repo clone → push → `minetonight.github.io/scelight/larva-seconds-lost/` → in-module updater.
- One-time setup: `release/local.properties` keys (`scelightFolder`, `pagesRepoFolder`) and cloning the Pages repo.
- **Release checklist**, copy-paste ready: bump the version in `release.properties` → update the changelog and manifest description → `ant TEST` → `ant BUILD_RELEASE` (includes CHECK_RELEASE) → `ant INSTALL_DEPLOYMENT` and check one replay locally → `ant PUBLISH_RELEASE` → review the diff in the Pages repo → commit and push → smoke check → confirm that one other machine shows the update notice.
- Invariants: the `module.xml` URL never changes. Zips are immutable. Versions only go up. Undo a release by publishing a newer one. `name` and `folder` never change (the launcher and the updater both check them).
- **Agent rules:** never push to the Pages repo or create GitHub releases without the user's explicit OK in that session. Never delete a published zip. Never put personal paths or emails into released files.
- Troubleshooting: Pages not updated yet (wait about 1–10 min, check the Actions tab), hash mismatch (zip rebuilt after `module.xml` was generated: run `BUILD_RELEASE` again and do not edit the zip by hand), the version check says "not higher".

---

## 8. Phase 5 — Release and rollout

### E18-S14 — First online release 3.1 and rollout to the 3 machines

1. Bump to 3.1.0 (OQ-5). Follow `RELEASING.md` from start to end. This also tests the document. Fix it wherever it was wrong.
2. Manual bootstrap install of 3.1 on machines 2 and 3, following the root README, as a new user would. Machine 1 (the dev laptop) uses `INSTALL_DEPLOYMENT`.
3. **End-to-end update test:** publish 3.1.1 (a tiny change, such as a changelog line in the help). On all 3 machines: the banner appears → Install → restart → Installed Modules shows 3.1.1 → the `3.1/` folder is gone after the next start.
4. Record the results in `knowhow/epic18-rollout.md` (OS, Java version, pass or fail).

---

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Scelight never updates the module itself | No native auto-update | The in-module updater does not depend on Scelight's operator (§0) |
| Old JRE cannot do TLS 1.2 or lacks the CA | The update check fails on that machine | S00 spike, Java 8+ in the README, a clear "update failed" message with a download link |
| A bad release reaches all users | Broken module everywhere | CHECK_RELEASE, a local check before publish, roll forward with a fixed higher version, and the old version folder is kept until the new one starts |
| Overwriting a published zip | Hash mismatch, installs fail | PUBLISH_RELEASE refuses to overwrite, and the invariant is in RELEASING.md |
| Updater bug damages the install | The module is gone until a manual reinstall | The new version goes into a separate folder first, the running version is never touched, and the old one is deleted only after the new one has started |
| Personal data in public artifacts | Privacy | S01, S02, S05 lint, and agent rules in RELEASING.md |
| Repo `minetonight.github.io` gets other content later | Clashing paths | Everything for this mod stays under `/scelight/larva-seconds-lost/` |

## 10. Definition of done (epic)

- [ ] `https://minetonight.github.io/scelight/larva-seconds-lost/module.xml` serves 3.1.x. Its zip URL, size and SHA-256 are correct.
- [x] No home-folder paths in tracked files (except old git history), and none in any released file.
- [ ] Installed Modules shows the new description and a working home page link.
- [ ] All 3 machines updated 3.1 → 3.1.1 from inside Scelight (S14 step 3).
- [ ] The root `README.md` was enough for a fresh install on machine 2 or 3.
- [ ] `RELEASING.md` was followed word for word for 3.1.1, with no changes needed afterwards.
- [x] `ant TEST` passes, including the new updater tests (300 tests, 2026-11-25).

---

## 11. Implementation notes (2026-11-25)

What was built, and where it differs from the stories above. Binding for later work.

**S01 personal paths.** `scelightFolder` (and later `pagesRepoFolder`) live in gitignored `LarvaSecondsLostExtMod/release/local.properties` (template `local.properties.example`), loaded before `release.properties`. Target `REQUIRE_SCELIGHT_FOLDER` guards `INSTALL_DEPLOYMENT` and the golden runs. `.classpath` uses `../ScelightExtModSDK/...`. The knowhow docs use shell variables (`$SCELIGHT_HOME`, `$SC2_ACCOUNTS`, `$SC2_REPLAY_DEST`, `$REPLAY_BACKUP`, `$REPO`); real values are in gitignored `knowhow/local-paths.md` (template `knowhow/local-paths.example.md`).

**S02 benchmark data.** Scrub rule: repo root → `<REPO>`, any other home path → `~/` (simpler than the `<SCELIGHT_HOME>` idea, same effect). Done byte-exact (CSV line endings kept), 1,658 files. New output goes through `benchmark-data/tools/portable_paths.py` (batch runner and stats tool). `__pycache__` untracked and ignored.

**S03 manifest.** Contact is `Discord: scorpion_rbp` (user decision) in `<contact><other>`.

**S05 CHECK_RELEASE.** `BUILD_RELEASE` = `PACKAGE_RELEASE` + `CHECK_RELEASE`. Checks the base URL, scans module.xml, the unpacked zip and the unpacked jar for `example.invalid`, `/home/`, `/Users/`, `C:\Users\`, and fails on logs, test classes, replays and dumps. The version-higher check moved to `PUBLISH_RELEASE` (it needs the published module.xml).

**S06 landing page.** Template `LarvaSecondsLostExtMod/release/site/index-template.html` (tokens `@version@ @archiveFile@ @archiveSizeKb@ @releaseDate@`, unknown tokens fail the publish). The changelog is the "Changes" list in that template. **Screenshots (2026-11-25, user request):** single replay (`knowhow/epic13.5-pro-benchmarks.png`, a pro game, with the title bar and its real name cropped off) and multi analysis (`knowhow/epic17-multi-2players.png`, with the user's account names, as the user asked) are in `release/site/img/` with 720 px thumbnails. The page shows them side by side, and they open full-page on click. `ant PUBLISH_SITE` updates only the page and images of the published version.

**S07 publish.** `PUBLISH_RELEASE` (needs `pagesRepoFolder`) refuses an existing zip and a not-higher version (`ReleaseTool check-newer`), copies zip + module.xml, renders index.html (`ReleaseTool page`), prints the git commands, never pushes. `SMOKE_CHECK_RELEASE` (`ReleaseTool smoke`) downloads the live module.xml and zip and installs them into a temp folder with the real updater code.

**S08–S10 updater.** Package `hu.aleks.larvasecondslostextmod.update`: `ModuleVersion`, `ModuleXmlParser` (DOCTYPE refused), `RemoteRelease`, `UpdateChecker` (name and folder must match), `HttpReleaseDownloader` (5 s connect / 15 s read, timestamp query, User-Agent `LarvaSecondsLost/<ver>`), `UpdateInstaller` (size + archive SHA-256, every entry must be listed in module.xml and under `Scelight/mod-x/larva-seconds-lost/<ver>/`, per-file SHA-256, staging folder `.incoming-<ver>` then move), `OldVersionCleaner` (at start: lower versions and `.incoming-*`), `UpdatePreferences` (`update.properties` in the module cache folder: autoCheck, lastCheckMillis, hiddenVersion), `UpdateService` (one daemon thread, automatic check 30 s after start at most once per 24 h, automatic failures only logged), `UpdatePanel` (bottom row of the Larva hub: status, Install / Try again, What's new / Open download page, Hide, Check for updates automatically, Check now). The module creates it in `startUpdateService()`; a failure there is logged and the module runs without updates. Override for tests: `-Dlarva.update.moduleXmlUrl=<url>`. Module help has an "Updates" section.

**Live test in Scelight (2026-11-25, dev laptop).** 3.0 with the updater, pointed at a local `module.xml` for a fake 3.0.1: the hub row showed "Version 3.0.1 is available" with Install / What's new / Hide. Install put a complete `3.0.1/` next to `3.0/` and the row said to restart. After the restart Scelight ran 3.0.1 (`ping`) and the module removed `3.0/`. With the real URL, Scelight's Java 8 reached github.io over HTTPS (HTTP 404, since nothing is published yet), and the failed automatic check was only logged. That is S00 for this machine. Fix found during the test: an automatic check queued at startup no longer replaces a status the user is acting on. Dev commands `update-state`, `update-check` and `update-install` were added; the procedure is in `knowhow/running-scelight.md`.

**Still open.**
1. ~~Pages repo~~ **Done 2026-11-25.** Clone at `pagesRepoFolder` (in `local.properties`). **3.1 is published** (Pages commit `4eabf06`). `ant SMOKE_CHECK_RELEASE` passed against the live site.
2. S14: version is **3.1.0** in `release.properties` (bumped 2026-11-25; tests pass; `BUILD_RELEASE` passes; 3.1 is installed and running on the dev laptop = machine 1). Next: bootstrap 3.1 by hand on machines 2 and 3, then publish 3.1.1 and update all three from inside Scelight. Watch the hub row in a real Scelight (not yet seen) and record Java versions (S00) in `knowhow/epic18-rollout.md`.
3. The old tracked `release/deployment/LarvaSecondsLost-3.0.zip` was deleted from the working tree by the 3.1 build; commit that deletion (OQ-3). New zips are gitignored.


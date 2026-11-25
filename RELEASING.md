# Releasing Larva Seconds Lost online

How to publish a new version so that every install updates itself. Written for the maintainer and for AI agents.
Background and design decisions: [prompts/epic18-release-stories.md](prompts/epic18-release-stories.md).

## How it works

```
this repo (LarvaSecondsLostExtMod/)
   ant BUILD_RELEASE        builds release/deployment/LarvaSecondsLost-<ver>.zip + module.xml, then CHECK_RELEASE
   ant PUBLISH_RELEASE      copies zip + module.xml + index.html into the Pages clone (never pushes)
        │
        ▼
local clone of github.com/minetonight/minetonight.github.io
   scelight/larva-seconds-lost/   ← you review, commit and push
        │  GitHub Pages (about 1 min)
        ▼
https://minetonight.github.io/scelight/larva-seconds-lost/
   index.html                     landing page (= manifest homePage)
   module.xml                     ALWAYS the latest release
   LarvaSecondsLost-<ver>.zip     one per version, never changed
        │  once a day, or "Check now"
        ▼
the module inside each Scelight (package hu.aleks.larvasecondslostextmod.update)
   reads module.xml → "Version X is available" → Install → downloads zip, checks size, SHA-256 and every file hash
   → extracts into Scelight/mod-x/larva-seconds-lost/<X>/ → user restarts → Scelight loads the highest version folder
   → the new version deletes lower version folders
```

Scelight itself never updates this module (only modules on Scelight's official list, which we do not use). **Never contact the Scelight author or ask for an official listing.**

## One-time setup (per machine you release from)

1. Clone the Pages repo, for example next to this repo:
   `git clone https://github.com/minetonight/minetonight.github.io.git`
2. `cp LarvaSecondsLostExtMod/release/local.properties.example LarvaSecondsLostExtMod/release/local.properties` and set:
   - `scelightFolder`: your Scelight install (for `INSTALL_DEPLOYMENT`)
   - `pagesRepoFolder`: the clone from step 1
   `local.properties` is gitignored. Never commit machine paths.

## Release checklist

Run Ant from `LarvaSecondsLostExtMod/`.

1. **Version.** In `release/release.properties`, raise `version.major`, `version.minor` or `version.revision`. Versions only go up.
2. **Texts.**
   - Add one line for the new version at the top of the "Changes" list in `release/site/index-template.html`.
   - If features changed, update `<shortDesc>` and `<description>` in `release/resources/Scelight-mod-x-manifest-template.xml`.
3. **Tests.** `ant TEST` must pass.
4. **Build.** `ant BUILD_RELEASE`. It ends with `CHECK_RELEASE`, which stops on placeholder URLs, personal paths (`/home/`, `/Users/`, `C:\Users\`) and dev leftovers.
5. **Local check.** Stop Scelight, run `ant INSTALL_DEPLOYMENT`, start Scelight, and open one replay on the Larva page.
6. **Publish to the clone.** `ant PUBLISH_RELEASE`. This refuses a version that is not higher than the published one, and a zip that is already published.
7. **Review and push.** In the Pages clone: `git status`, `git diff` (only `module.xml` and `index.html` change, plus one new zip), then commit and push.
   **Agents: ask the user before pushing, every time.** A push reaches every user.
8. **Smoke check.** Wait until the repo's Actions tab shows "pages build and deployment" as green. Then run `ant SMOKE_CHECK_RELEASE`. It downloads the live `module.xml` and zip and installs them into a temp folder, exactly like the module does.
9. **Real update.** On another machine with the previous version: Larva page → **Check now** → **Install** → restart Scelight. Help → About → Installed Modules must show the new version.
10. Commit the source changes in this repo (version bump, changelog, manifest).

## Change only the landing page

To change the page text or screenshots without a new version: edit `LarvaSecondsLostExtMod/release/site/index-template.html` or the images in `release/site/img/`, then run `ant PUBLISH_SITE`. It renders `index.html` for the version that is already published, keeps that version's release date, copies `img/`, and never touches the zip or `module.xml`. Review, commit and push in the Pages clone as in step 7. `PUBLISH_RELEASE` copies `img/` too.

Screenshots must not show private data. Crop away Scelight's title bar (it shows the registered name), and strip the metadata, for example with `convert in.png -crop <w>x<h>+0+40 +repage -strip out.png`. Thumbnails are 720 px wide: `convert full.png -resize 720x -colors 256 -strip name-thumb.png`.

## Rules that must never be broken

- The `module.xml` URL never changes: `https://minetonight.github.io/scelight/larva-seconds-lost/module.xml`. It is compiled into every installed version (`UpdateConfig`).
- A published zip is never replaced or deleted. Old `module.xml` files and users refer to it by SHA-256.
- Versions only go up. **To undo a bad release, publish a newer version** built from the good code. Never point `module.xml` at a lower version: the updater only installs higher versions.
- `name` (`Larva Seconds Lost`) and `folder` (`larva-seconds-lost`) never change. The updater refuses a `module.xml` with another name or folder.
- No personal paths, emails or private names in anything published (zip, `module.xml`, `index.html`, screenshots). The only public contact is Discord `scorpion_rbp`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `CHECK_RELEASE` fails on a path or URL | The message lists the files (unpacked under `release/lint-tmp/`). Remove the string from the source or resource and build again. |
| `PUBLISH_RELEASE`: "already published" or "not higher" | Raise the version in `release/release.properties`. |
| `PUBLISH_RELEASE`: "pagesRepoFolder is not set" | One-time setup, step 2. |
| Smoke check: HTTP 404 | Pages has not deployed yet. Wait 1–10 minutes and check the Actions tab of the Pages repo. |
| Smoke check: checksum mismatch | The zip was changed after `module.xml` was generated. Never edit the zip by hand. Delete the unpushed files from the clone, then run `BUILD_RELEASE` and `PUBLISH_RELEASE` again. |
| A user's update check fails | Usually an old Java without TLS 1.2. They can update by hand from the landing page. |

## Testing the updater without publishing

- Start Scelight with `-Dlarva.update.moduleXmlUrl=<url>` to point the module at another `module.xml`, for example a local `python3 -m http.server` folder with a copy of `release/deployment/`. Edit `<urlList>` in that copy to the local zip URL.
- Unit tests: `LarvaSecondsLostExtMod/test/hu/aleks/larvasecondslostextmod/update/`.
- The update settings are in `update.properties` in the module cache folder (Scelight workspace `mod-x-cache/larva-seconds-lost/`). Delete `lastCheckMillis` to force an automatic check on the next start.

# Larva Seconds Lost publishing and release guide

This note documents how to export the module to another computer and how to publish a hosted release for other users.

## Release artifacts produced by the Ant build

Running `ant BUILD_RELEASE` from the module root produces these outputs:

- [release/Scelight/mod-x/larva-seconds-lost](../release/Scelight/mod-x/larva-seconds-lost) — unpacked module tree ready to copy into a Scelight installation
- [release/deployment](../release/deployment) — deployment outputs for sharing or hosting
- [release/deployment/module.xml](../release/deployment/module.xml) — release descriptor with version, download URL, archive SHA-256, archive size, and per-file hashes

The deployment zip is named `LarvaSecondsLost-<version>.zip`.

## Before publishing a new version

1. Update [release/release.properties](../release/release.properties):
   - increment `version.major`, `version.minor`, or `version.revision`
   - verify `folder=larva-seconds-lost` stays unchanged
   - set `archiveBaseUrl` to the public directory URL where the deployment zip will be hosted
2. Update [release/resources/Scelight-mod-x-manifest-template.xml](../release/resources/Scelight-mod-x-manifest-template.xml) if the release description, home page, or short description changed.
3. Run the validation flow from [validation-checklist.md](validation-checklist.md).
4. Run `ant BUILD_RELEASE`.

## Export to another local computer

Use this flow when the module is shared directly instead of through a hosted module feed.

### Option A — copy the deployment zip

1. Build the release.
2. Copy `release/deployment/LarvaSecondsLost-<version>.zip` to the target machine.
3. On the target machine, extract the zip into the folder that contains the target `Scelight` folder.
   - Example result: `.../Scelight/mod-x/larva-seconds-lost/<version>/...`
4. Start Scelight.
5. Enable the module on the Installed Modules page if it is not already enabled.

### Option B — copy the unpacked module folder

1. Build the release.
2. Copy [release/Scelight/mod-x/larva-seconds-lost](../release/Scelight/mod-x/larva-seconds-lost) to the target machine.
3. Merge it into the target `Scelight/mod-x/` folder.
4. Start Scelight and enable the module.

## Publish for other users through a hosted URL

Use this flow when users should download the release from a stable web location.

1. Build the release after setting `archiveBaseUrl` correctly.
2. Upload these files from [release/deployment](../release/deployment):
   - `LarvaSecondsLost-<version>.zip`
   - `module.xml`
3. Ensure the generated archive URL inside `module.xml` is reachable over HTTP or HTTPS.
4. Keep the `module.xml` URL stable across releases.
5. For a new version, upload the new zip and replace the hosted `module.xml` with the newly generated one.

Why both files matter:

- the zip contains the actual module payload
- `module.xml` tells Scelight where the zip is, what version it represents, and what file hashes are expected

## Official-module style distribution

The Scelight know-how distinguishes between manual external-module installs and official hosted modules.

- Manual distribution: users download the zip or folder and place it under `Scelight/mod-x/`.
- Official-style distribution: users install from a hosted module descriptor and Scelight can check for updates.

If the module is ever listed through Scelight's official external-module flow, provide the public `module.xml` URL to the Scelight operator. The generated descriptor already contains the archive metadata required by that flow.

## Quick release checklist

1. Update version and release metadata.
2. Run `ant BUILD_RELEASE`.
3. Confirm the zip and `module.xml` were generated.
4. Install locally with `ant INSTALL_DEPLOYMENT`.
5. Open the `Larva` page and validate one replay.
6. If sharing manually, send the zip or module folder.
7. If publishing on the web, upload the zip and `module.xml`.
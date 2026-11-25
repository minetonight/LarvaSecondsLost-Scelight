# Larva Seconds Lost publishing and release guide

**Moved.** The release process (build, checks, GitHub Pages hosting and in-app updates) is described in one place: [RELEASING.md](../../RELEASING.md) at the repository root. The player install guide is the root [README.md](../../README.md).

## Release artifacts produced by the Ant build

`ant BUILD_RELEASE` (run from `LarvaSecondsLostExtMod/`) produces:

- `release/Scelight/mod-x/larva-seconds-lost/<version>/`: unpacked module tree
- `release/deployment/LarvaSecondsLost-<version>.zip`: the archive users download (gitignored; the published copy lives in the GitHub Pages repo)
- `release/deployment/module.xml`: version, download URL, archive SHA-256 and size, and per-file hashes; the in-app updater reads the published copy

## Copy to another computer without the web

1. Build the release.
2. Copy `release/deployment/LarvaSecondsLost-<version>.zip` to the other machine.
3. Close Scelight there and extract the zip into the folder that contains the `Scelight` folder.
4. Start Scelight and enable the module on the Installed Modules page if needed.

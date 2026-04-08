# Internet replay sourcing research

This document captures the first Epic 13 research pass on public internet replay sources.

## Research summary

### 1. SpawningTool replay browser

Page:

- https://lotv.spawningtool.com/replays/

Observed properties:

- public replay browser with filters for matchup, player, event, map, tags, and more,
- replay detail URLs follow `/<id>/`,
- direct replay downloads follow `/<id>/download/`,
- listing pages expose a bulk ZIP endpoint such as `/zip/?&p=1`,
- public pages are fetchable without account authentication.

Why this source is strong:

- direct replay files are reachable,
- listing pages are reproducible,
- filters can be saved as URLs,
- bulk ZIP download makes first-pass corpus acquisition efficient.

Main limitation:

- listing-page discovery alone does not provide reliable MMR metadata, so cohort labeling still needs manual or secondary-source enrichment.

### 2. SpawningTool replay packs

Page:

- https://lotv.spawningtool.com/replaypacks/

Observed properties:

- public curated replay-pack page,
- contains Google Drive file links and folder links,
- individual Google Drive file links can be transformed into direct downloads,
- folder links are useful for discovery but often need manual review or external tooling.

Why this source is strong:

- good fit for elite or tournament replay collection,
- curated packs tend to preserve event context,
- useful for known-player sanity checks and benchmark validation subsets.

Main limitation:

- Google Drive folders are not as automation-friendly as direct files,
- pack contents may include non-target matchups and need filtering after download.

### 3. SC2ReplayStats public site

Page:

- https://sc2replaystats.com/

Observed properties:

- public site is reachable,
- ladder and event pages are public,
- direct public replay-download workflow was not confirmed in this pass.

Current stance:

- use as a metadata/reference candidate only,
- do not automate downloads from it until a stable public replay-download path is verified.

## Recommended acquisition strategy

Start with a two-lane strategy:

1. use SpawningTool replay-browser URLs for broad public replay collection,
2. use SpawningTool replay-pack links for curated elite and tournament collections.

This balances:

- scale,
- reproducibility,
- auditability,
- and realistic download automation effort.

## Tooling

The canonical internet sourcing tool is:

- [benchmark-data/tools/internet_replay_source_tool.py](../../benchmark-data/tools/internet_replay_source_tool.py)

Supported workflows:

- discover replay download links from SpawningTool listing pages,
- discover replay-pack links from SpawningTool replay packs,
- run discovery from a tracked source-seed manifest,
- download direct replay files,
- download bulk ZIP archives,
- optionally extract `.SC2Replay` files from ZIP archives,
- download a small bounded sample per seed for initial corpus bootstrapping.

## Initial source seeds

The first tracked source-seed manifest is:

- [benchmark-data/manifests/replay-source-seeds.json](../../benchmark-data/manifests/replay-source-seeds.json)

It currently includes:

- elite Serral and Reynor seeds for `ZvT`, `ZvP`, and `ZvZ`,
- broad public pro-only matchup seeds for `ZvT`, `ZvP`, and `ZvZ`,
- the SpawningTool replay-pack index.

Important tag findings from the research pass:

- Serral player tag id: `644`
- Reynor player tag id: `546`
- `TvZ` tag id: `3` and this is the practical filter for `ZvT`
- `PvZ` tag id: `11` and this is the practical filter for `ZvP`
- `ZvZ` tag id: `13`

## Guardrails

- only use publicly accessible sources,
- keep raw replay files under `benchmark-data/raw-replays/`,
- keep source discovery JSON and summaries reviewable,
- preserve source URLs so the corpus remains auditable later.
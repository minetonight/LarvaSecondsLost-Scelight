# Benchmark bands

Written by `benchmark-data/tools/benchmark_bands.py` (`ant BENCHMARK_BANDS`) on 2026-11-26. Do not edit by hand.

- Rates per replay and phase: [phase-rates.csv](phase-rates.csv) (primary Zerg player; per game clock minute, 1344 loops, as in `LarvaPhaseStats` since 2026-11-26).
- Leagues: [MMR-Ranges](https://burnysc2.github.io/MMR-Ranges/) 1v1, season 69 (start 2026-11-10, site updated 2026-11-26 03:43, snapshot `benchmark-data/mmr-ranges/mmr-ranges-1v1.json`). The replays come from many seasons; one season's limits are used for all of them.
- Method: median per league; a tier starts halfway between its median and the median of the league below; the lowest tier half a step below its own median (the maintainer's spreadsheet, sheet "may 2026 v2.1"). A league needs 20 replays in the phase, and medians must improve with skill, else the league is dropped for that phase.
- Band file used by the module: `LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/larva-benchmark-bands.properties`.

## Leagues

| league | label | MMR Europe | MMR Americas | MMR Korea | replays |
|---|---|---|---|---|---:|
| Bronze | bronze | < 1800 | < 1680 | < 2000 | 0 |
| Silver | silver | 1800-2440 | 1680-2320 | 2000-2680 | 119 |
| Gold | gold | 2440-2840 | 2320-2720 | 2680-3080 | 103 |
| Platinum | plat | 2840-3280 | 2720-3120 | 3080-3600 | 177 |
| Diamond | dia | 3280-4280 | 3120-4240 | 3600-4760 | 288 |
| Master | masters | 4280-5005 | 4240-4954 | 4760-5577 | 214 |
| GrandMaster | GM | 5005+ | 4954+ | 5577+ | 286 |
| pro | pro | elite corpus (tournaments) | | | 430 |

Skipped: 5 replays (region 5 is not in MMR-Ranges).

## spawnedLarvaPerHatchPerMinute (higher is better)

Median (replays) per league:

| phase | bronze | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| EARLY | - | 6.4851 (119) | 6.6875 (103) | 6.9426 (177) | 6.9925 (288) | 7.1129 (214) | 7.2124 (286) | 7.5186 (423) |
| MID | - | 5.8510 (75) | 5.7444 (74) | 6.4051 (111) | 7.0470 (196) | 7.9295 (169) | 8.4015 (231) | 9.2842 (391) |
| LATE | - | 4.5898 (30) | 4.5154 (20) | 4.6298 (28) | 5.9178 (66) | 6.4947 (90) | 7.2593 (121) | 8.1533 (275) |

Tier starts:

- EARLY: silver 6.3839, gold 6.5863, plat 6.8151, dia 6.9676, masters 7.0527, GM 7.1627, pro 7.3655.
- MID: gold 5.4141, plat 6.0748, dia 6.7260, masters 7.4882, GM 8.1655, pro 8.8428. Dropped: silver, its median 5.8510 is not below the gold median 5.7444.
- LATE: gold 4.4582, plat 4.5726, dia 5.2738, masters 6.2063, GM 6.8770, pro 7.7063. Dropped: silver, its median 4.5898 is not below the gold median 4.5154.

## larvaMissedPerHatchPerMinute (lower is better)

Median (replays) per league:

| phase | bronze | silver | gold | plat | dia | masters | GM | pro |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| EARLY | - | 1.5322 (119) | 1.4787 (103) | 0.8935 (177) | 0.7934 (288) | 0.5076 (214) | 0.3768 (286) | 0.1649 (423) |
| MID | - | 3.3532 (75) | 3.1562 (74) | 2.6682 (111) | 2.4644 (196) | 1.8711 (169) | 1.4045 (231) | 0.7073 (391) |
| LATE | - | 4.3092 (30) | 3.8894 (20) | 3.3135 (28) | 2.9714 (66) | 2.2748 (90) | 2.0594 (121) | 1.5268 (275) |

Tier starts:

- EARLY: silver 1.5590, gold 1.5055, plat 1.1861, dia 0.8434, masters 0.6505, GM 0.4422, pro 0.2709.
- MID: silver 3.4517, gold 3.2547, plat 2.9122, dia 2.5663, masters 2.1678, GM 1.6378, pro 1.0559.
- LATE: silver 4.5192, gold 4.0993, plat 3.6014, dia 3.1425, masters 2.6231, GM 2.1671, pro 1.7931.

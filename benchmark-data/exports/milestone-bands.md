# Milestone bands

Written by `benchmark-data/tools/milestone_bands.py` (`ant MILESTONE_BANDS`) on 2026-11-27. Do not edit by hand.

- Times per player-game: [milestone-times.csv](milestone-times.csv), the first `PlayerStats` loop at or above each milestone (`LarvaMilestoneCalculator`). Times are game clock m:ss (22.4 loops per second).
- Leagues: [MMR-Ranges](https://burnysc2.github.io/MMR-Ranges/) 1v1, season 69 (start 2026-11-10), as the larva bands (`benchmark-bands.md`). Pro: the elite corpus.
- Players: the primary Zerg of each replay in All, ZvP, ZvT and ZvZ; the ZvZ bucket also has the second Zerg of each ladder ZvZ, in the league of their own MMR.
- Method: median per league over the games that reached the milestone (`k/n` below); a tier starts halfway between its median and the median of the league below; the weakest tier half a step below its own median. A league needs 20 games that reached it, and its median must be at least one `PlayerStats` sample (160 loops, 7.1 s) later than the next kept league above it, else the league is dropped and named in that league's tier (`plat/dia`). One tier left: a pace, the slowest median of the leagues it names (`GM/pro pace 6:47`). No tier left: not rated (n/a).
- Band file used by the module: `LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/larva-milestone-bands.properties`.

## Player-games per league

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 119 | 48 | 45 | 54 |
| gold | 103 | 36 | 31 | 56 |
| plat | 178 | 58 | 61 | 153 |
| dia | 290 | 85 | 93 | 215 |
| masters | 214 | 82 | 69 | 118 |
| GM | 286 | 89 | 99 | 181 |
| pro | 430 | 150 | 150 | 130 |

## 44 drones

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 7:08 (68/119) | 7:40 (24/48) | 6:29 (32/45) | 6:15 (24/54) |
| gold | 6:32 (63/103) | 6:57 (30/36) | 5:42 (20/31) | 7:01 (21/56) |
| plat | 5:57 (93/178) | 5:50 (38/58) | 5:50 (47/61) | 6:25 (23/153) |
| dia | 5:21 (171/290) | 5:14 (58/85) | 5:07 (77/93) | 6:11 (76/215) |
| masters | 5:00 (158/214) | 5:00 (68/82) | 5:00 (60/69) | 5:35 (53/118) |
| GM | 4:45 (212/286) | 4:45 (63/89) | 4:38 (89/99) | 5:07 (109/181) |
| pro | 4:38 (383/430) | 4:38 (146/150) | 4:31 (138/150) | 4:52 (99/130) |

Tier starts:

- All: silver 7:26 (10000), gold 6:50 (9200), plat 6:15 (8400), dia 5:39 (7600), masters 5:10 (6960), GM 4:52 (6560), pro 4:42 (6320).
- ZvP: silver 8:02 (10800), gold 7:19 (9840), plat 6:23 (8600), dia 5:32 (7440), masters 5:07 (6880), GM 4:52 (6560), pro 4:42 (6320).
- ZvT: silver 6:48 (9160), gold/plat 6:09 (8280), dia 5:28 (7360), masters 5:03 (6800), GM 4:49 (6480), pro 4:35 (6160). Dropped: gold, its median 5:42 (7680) is not one sample (160 loops) later than plat 5:50 (7840), named in that tier.
- ZvZ: silver/gold 7:19 (9840), plat 6:43 (9040), dia 6:18 (8480), masters 5:53 (7920), GM 5:21 (7200), pro 5:00 (6720). Dropped: silver, its median 6:15 (8400) is not one sample (160 loops) later than gold 7:01 (9440), named in that tier.

## 55 drones

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 8:12 (53/119) | 7:44 (14/48) | 8:41 (31/45) | 8:41 (17/54) |
| gold | 7:58 (38/103) | 8:27 (20/36) | 6:54 (16/31) | 8:41 (6/56) |
| plat | 8:34 (51/178) | 6:47 (14/58) | 9:10 (35/61) | 9:24 (11/153) |
| dia | 6:50 (110/290) | 6:36 (34/85) | 7:01 (64/93) | 7:15 (29/215) |
| masters | 6:11 (133/214) | 6:07 (60/82) | 6:04 (55/69) | 7:19 (34/118) |
| GM | 5:50 (181/286) | 5:39 (58/89) | 5:42 (82/99) | 6:18 (75/181) |
| pro | 5:21 (349/430) | 5:21 (143/150) | 5:21 (130/150) | 5:50 (76/130) |

Tier starts:

- All: silver/gold/plat 9:26 (12680), dia 7:42 (10360), masters 6:31 (8760), GM 6:00 (8080), pro 5:35 (7520). Dropped: gold, its median 7:58 (10720) is not one sample (160 loops) later than plat 8:34 (11520), named in that tier; silver, its median 8:12 (11040) is not one sample (160 loops) later than plat 8:34 (11520), named in that tier.
- ZvP: gold 9:22 (12600), dia 7:31 (10120), masters 6:22 (8560), GM 5:53 (7920), pro 5:30 (7400). Dropped: plat, 14 games reached it (fewer than 20); silver, 14 games reached it (fewer than 20).
- ZvT: silver/plat 10:14 (13760), dia 8:05 (10880), masters 6:32 (8800), GM 5:53 (7920), pro 5:32 (7440). Dropped: gold, 16 games reached it (fewer than 20); silver, its median 8:41 (11680) is not one sample (160 loops) later than plat 9:10 (12320), named in that tier.
- ZvZ: dia/masters 7:49 (10522.5), GM 6:48 (9157.5), pro 6:04 (8157.5). Dropped: dia, its median 7:15 (9760) is not one sample (160 loops) later than masters 7:19 (9840), named in that tier; plat, 11 games reached it (fewer than 20); gold, 6 games reached it (fewer than 20); silver, 17 games reached it (fewer than 20).

## 100 supply

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 8:23 (60/119) | 8:20 (19/48) | 9:20 (32/45) | 7:51 (20/54) |
| gold | 8:41 (59/103) | 8:34 (30/36) | 8:48 (17/31) | 8:55 (19/56) |
| plat | 8:34 (81/178) | 7:33 (30/58) | 9:10 (42/61) | 8:05 (28/153) |
| dia | 7:30 (149/290) | 7:30 (43/85) | 7:37 (70/93) | 7:22 (70/215) |
| masters | 7:01 (154/214) | 7:01 (67/82) | 7:01 (56/69) | 7:30 (51/118) |
| GM | 6:47 (198/286) | 6:40 (62/89) | 6:47 (82/99) | 7:01 (103/181) |
| pro | 6:11 (384/430) | 6:18 (147/150) | 6:11 (137/150) | 6:07 (100/130) |

Tier starts:

- All: silver/gold 8:45 (11760), plat 8:37 (11600), dia 8:02 (10800), masters 7:15 (9760), GM 6:54 (9280), pro 6:29 (8720). Dropped: silver, its median 8:23 (11280) is not one sample (160 loops) later than gold 8:41 (11680), named in that tier.
- ZvP: gold 9:06 (12240), plat/dia 8:02 (10800), masters 7:15 (9760), GM 6:50 (9200), pro 6:29 (8720). Dropped: plat, its median 7:33 (10160) is not one sample (160 loops) later than dia 7:30 (10080), named in that tier; silver, 19 games reached it (fewer than 20).
- ZvT: silver 9:26 (12680), plat 9:15 (12440), dia 8:23 (11280), masters 7:19 (9840), GM 6:54 (9280), pro 6:29 (8720). Dropped: gold, 17 games reached it (fewer than 20).
- ZvZ: silver/plat 8:23 (11280), dia/masters 7:47 (10480), GM 7:15 (9760), pro 6:34 (8840). Dropped: dia, its median 7:22 (9920) is not one sample (160 loops) later than masters 7:30 (10080), named in that tier; gold, 19 games reached it (fewer than 20); silver, its median 7:51 (10560) is not one sample (160 loops) later than plat 8:05 (10880), named in that tier.

## 66 drones

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 10:00 (32/119) | 9:06 (10/48) | 10:21 (18/45) | 10:07 (9/54) |
| gold | 10:57 (23/103) | 10:28 (13/36) | 10:57 (9/31) | 10:14 (4/56) |
| plat | 10:50 (33/178) | 9:31 (9/58) | 11:54 (22/61) | 10:10 (8/153) |
| dia | 8:12 (71/290) | 7:55 (22/85) | 8:27 (45/93) | 7:22 (13/215) |
| masters | 7:15 (97/214) | 7:01 (41/82) | 7:30 (45/69) | 9:13 (18/118) |
| GM | 6:47 (128/286) | 6:40 (43/89) | 7:01 (69/99) | 6:47 (31/181) |
| pro | 6:18 (297/430) | 6:18 (126/150) | 6:18 (123/150) | 6:43 (48/130) |

Tier starts:

- All: silver/gold 11:00 (14800), plat 10:53 (14640), dia 9:31 (12800), masters 7:44 (10400), GM 7:01 (9440), pro 6:32 (8800). Dropped: silver, its median 10:00 (13440) is not one sample (160 loops) later than gold 10:57 (14720), named in that tier.
- ZvP: dia 8:21 (11240), masters 7:28 (10040), GM 6:50 (9200), pro 6:29 (8720). Dropped: plat, 9 games reached it (fewer than 20); gold, 13 games reached it (fewer than 20); silver, 10 games reached it (fewer than 20).
- ZvT: plat 13:37 (18320), dia 10:10 (13680), masters 7:58 (10720), GM 7:15 (9760), pro 6:40 (8960). Dropped: gold, 9 games reached it (fewer than 20); silver, 18 games reached it (fewer than 20).
- ZvZ: one tier, GM/pro pace 6:47 (9120). Dropped: GM, its median 6:47 (9120) is not one sample (160 loops) later than pro 6:43 (9040), named in that tier; masters, 18 games reached it (fewer than 20); dia, 13 games reached it (fewer than 20); plat, 8 games reached it (fewer than 20); gold, 4 games reached it (fewer than 20); silver, 9 games reached it (fewer than 20).

## 150 supply

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 12:01 (42/119) | 11:47 (10/48) | 12:22 (25/45) | 10:00 (12/54) |
| gold | 11:18 (39/103) | 10:57 (22/36) | 11:15 (14/31) | 13:41 (6/56) |
| plat | 11:47 (31/178) | 9:31 (8/58) | 12:22 (21/61) | 9:49 (6/153) |
| dia | 9:38 (83/290) | 9:02 (23/85) | 10:07 (43/93) | 9:45 (36/215) |
| masters | 9:02 (98/214) | 8:48 (42/82) | 9:10 (40/69) | 9:10 (31/118) |
| GM | 8:37 (138/286) | 8:48 (45/89) | 8:41 (57/99) | 8:20 (67/181) |
| pro | 7:51 (319/430) | 7:51 (127/150) | 7:58 (122/150) | 7:51 (70/130) |

Tier starts:

- All: silver 12:08 (16320), gold/plat 11:54 (16000), dia 10:42 (14400), masters 9:20 (12560), GM 8:50 (11880), pro 8:14 (11080). Dropped: gold, its median 11:18 (15200) is not one sample (160 loops) later than plat 11:47 (15840), named in that tier.
- ZvP: gold 11:54 (16000), dia 10:00 (13440), masters/GM 8:55 (12000), pro 8:20 (11200). Dropped: masters, its median 8:48 (11840) is not one sample (160 loops) later than GM 8:48 (11840), named in that tier; plat, 8 games reached it (fewer than 20); silver, 10 games reached it (fewer than 20).
- ZvT: silver/plat 13:30 (18160), dia 11:15 (15120), masters 9:38 (12960), GM 8:55 (12000), pro 8:20 (11200). Dropped: gold, 14 games reached it (fewer than 20); silver, its median 12:22 (16640) is not one sample (160 loops) later than plat 12:22 (16640), named in that tier.
- ZvZ: dia 10:03 (13520), masters 9:27 (12720), GM 8:45 (11760), pro 8:05 (10880). Dropped: plat, 6 games reached it (fewer than 20); gold, 6 games reached it (fewer than 20); silver, 12 games reached it (fewer than 20).

## 80 drones

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 11:43 (14/119) | 10:03 (6/48) | 13:34 (7/45) | 19:31 (1/54) |
| gold | 8:48 (5/103) | 8:48 (1/36) | 9:20 (4/31) | 18:41 (1/56) |
| plat | 13:27 (10/178) | 10:50 (2/58) | 15:00 (7/61) | 9:24 (1/153) |
| dia | 9:52 (27/290) | 10:21 (7/85) | 9:35 (18/93) | 9:42 (4/215) |
| masters | 9:20 (46/214) | 8:52 (20/82) | 9:20 (24/69) | 11:04 (2/118) |
| GM | 8:34 (56/286) | 8:34 (25/89) | 8:23 (30/99) | 13:30 (2/181) |
| pro | 7:58 (169/430) | 7:55 (80/150) | 8:05 (87/150) | 7:55 (2/130) |

Tier starts:

- All: dia 10:08 (13640), masters 9:36 (12920), GM 8:57 (12040), pro 8:16 (11120). Dropped: plat, 10 games reached it (fewer than 20); gold, 5 games reached it (fewer than 20); silver, 14 games reached it (fewer than 20).
- ZvP: masters 9:01 (12120), GM 8:43 (11720), pro 8:14 (11080). Dropped: dia, 7 games reached it (fewer than 20); plat, 2 games reached it (fewer than 20); gold, 1 games reached it (fewer than 20); silver, 6 games reached it (fewer than 20).
- ZvT: masters 9:49 (13200), GM 8:52 (11920), pro 8:14 (11080). Dropped: dia, 18 games reached it (fewer than 20); plat, 7 games reached it (fewer than 20); gold, 4 games reached it (fewer than 20); silver, 7 games reached it (fewer than 20).
- ZvZ: not rated (no tier). Dropped: pro, 2 games reached it (fewer than 20); GM, 2 games reached it (fewer than 20); masters, 2 games reached it (fewer than 20); dia, 4 games reached it (fewer than 20); plat, 1 games reached it (fewer than 20); gold, 1 games reached it (fewer than 20); silver, 1 games reached it (fewer than 20).

## 200 supply

Median of the games that reached it (k/n):

| league | All | ZvP | ZvT | ZvZ |
|---|---:|---:|---:|---:|
| silver | 15:00 (15/119) | 14:52 (5/48) | 15:32 (10/45) | 11:32 (1/54) |
| gold | 13:34 (11/103) | 13:52 (6/36) | 13:34 (5/31) | - (0/56) |
| plat | 17:37 (8/178) | - (0/58) | 17:37 (8/61) | 11:29 (2/153) |
| dia | 12:30 (16/290) | 12:15 (6/85) | 12:44 (10/93) | 10:00 (5/215) |
| masters | 12:08 (29/214) | 12:08 (11/82) | 12:15 (12/69) | 10:39 (6/118) |
| GM | 12:01 (46/286) | 11:18 (18/89) | 12:44 (21/99) | 12:58 (15/181) |
| pro | 10:42 (161/430) | 10:50 (65/150) | 10:50 (65/150) | 9:45 (31/130) |

Tier starts:

- All: masters 12:12 (16400), GM 12:05 (16240), pro 11:22 (15280). Dropped: dia, 16 games reached it (fewer than 20); plat, 8 games reached it (fewer than 20); gold, 11 games reached it (fewer than 20); silver, 15 games reached it (fewer than 20).
- ZvP: one tier, pro pace 10:50 (14560). Dropped: GM, 18 games reached it (fewer than 20); masters, 11 games reached it (fewer than 20); dia, 6 games reached it (fewer than 20); gold, 6 games reached it (fewer than 20); silver, 5 games reached it (fewer than 20).
- ZvT: GM 13:41 (18400), pro 11:47 (15840). Dropped: masters, 12 games reached it (fewer than 20); dia, 10 games reached it (fewer than 20); plat, 8 games reached it (fewer than 20); gold, 5 games reached it (fewer than 20); silver, 10 games reached it (fewer than 20).
- ZvZ: one tier, pro pace 9:45 (13120). Dropped: GM, 15 games reached it (fewer than 20); masters, 6 games reached it (fewer than 20); dia, 5 games reached it (fewer than 20); plat, 2 games reached it (fewer than 20); silver, 1 games reached it (fewer than 20).

# Replay facts for drones and queens

What SC2 replays (as Scelight parses them) record and do not record about drones and queens. Checked event by event in Epic 19 ([prompts/epic19-drones-and-queen-injects.md](../prompts/epic19-drones-and-queen-injects.md)). Read this before you change drone counting or missed injects.

## Time

- Replays count in game loops. The in-game clock of LotV replays is loops / 22.4; Blizzard's game data counts Normal-speed seconds of 16 loops; Scelight shows loops / 16 × 26/36 (1.1 % high, game 7:51 is Scelight 7:56), or loops / 16 with its "use real time" setting off.
- The module computes in loops and shows the game clock through `LarvaGameClock` only; game-data durations are measured loop constants (larva 240, inject 641, queen 800, hatchery 1600). Rules, measured values and history: [time-management.md](time-management.md).

## Tags and coordinates

- Unit tag = `unitTagIndex << 18 | unitTagRecycle`. Command target tags and Scelight's selection tracker use the same value. As an `int` it can be negative; use `tag >>> 18` for the index.
- Tracker `x`/`y` (UnitBorn, UnitInit, UnitDied, UnitPositions) share one scale.
- Command target points: use `getX() / 4096.0` to compare with tracker x/y. `ITargetPoint.getXFloat()` is half that scale. Example: tumor `UnitInit` at (149, 76), command `getXFloat()` (74.75, 38.25).
- Target unit snapshot points (`ITargetUnit.getSnapshotPoint()`, map with `x`, `y`): same raw scale, divide by 4096.

## Drones

- The ground truth is `PlayerStats.workersActiveCount`, the value of Scelight's **Workers Active** chart (`PlayerStatsChartFactory`, `F_WORKERS_ACTIVE_COUNT`). It is sampled every 160 loops (10 game seconds, about 7 s real time). It drops when a drone starts a building and comes back on cancel, does not count drones in eggs, and is 0 after a player leaves.
- Drones from eggs: `UnitBorn Drone` (creator ability `LarvaTrain`). The 12 starting drones are `UnitBorn` at loop 0.
- Larva: every `UnitBorn Larva` names its hatchery as creator (`creatorUnitTagIndex`/`creatorUnitTagRecycle`), and `creatorAbilityName` is empty, natural and inject larva alike (checked on 16 replays, builds 72282 to 95841). Assign larva by the creator; a spatial guess splits inject pops between a main and a nearby macro hatchery (E19-S02).
- Drone becomes a building: the building gets `UnitInit`; the drone gets no event then. Its `UnitDied` (no killer) comes one loop after the building's `UnitDone`, often 20+ s later.
- Cancelled building (extractor trick): the building's `UnitDied` has the own player as killer; the drone comes back without any event.
- `UnitDied` has **no** `unitTypeName`. `UnitTypeChange` has **no** player id. A hand-built drone tally from these events misses deaths and credits morphs to "Unknown player" (the bug Epic 19 removed). Do not rebuild one.

## Queens

- Queens: `UnitBorn Queen` (position = next to the producing hatchery), `UnitDied` with position. Burrow is a `UnitTypeChange` to `QueenBurrowed`.
- **No positions while walking.** Tracker events give a queen's position only at birth, death and in `UnitPositions`.
- `UnitPositions` (every 15 s, damaged units only): `firstUnitIndex`, then `items` as triples (index delta, x, y). The first delta is added to `firstUnitIndex`, each later one to the previous index. Match the index with `tag >>> 18` of a living queen.
- Queen creep tumor: `UnitInit CreepTumorQueen` with position, always present. It has no creator tag. The queen stood there and spent 25 energy.
- Tumor command: ability id contains `CreepTumorQueen` (Scelight: abil `Queen`, cmd `CreepTumorQueen`). `CreepTumorBurrowed/CreepTumor` is a tumor spreading itself; `Hatchery/Queen` is training a queen. About two thirds of tumors match a command within 1.5 cells and 60 s.
- Inject: no tracker event on the hatchery side. The module infers it from 3 larva born within 8 loops (`InferredInjectDetector`). Queued injects make back-to-back windows; each window is one inject and 25 energy.
- With the module's own selection tracking, 40 of 42 inferred injects of the scenario replay have a Spawn Larva command with queens selected (Scelight's tracker: 15 of 42), and at least half of the injects have one for 101 of 104 players of the check corpus and 185 of 216 of the maintainer's games.
- Transfusion: only the command (`Transfusion`, 50 energy, target unit with snapshot point). A command may not be cast.

## Selections and commands (E19-S11)

Scelight's `SelectionTracker` (app 6.5.4, 2020) loses most queen selections in current replays. The module keeps its own (`queens/SelectionModel`, `queens/ReplayCommandStream`):

- **Control group updates:** 0 set, 1 append, 2 recall, 3 clear, 4 set and steal, 5 append and steal. Scelight handles only 0 to 2. Steal (the unit leaves every other group) is how many players bind hotkeys now, so their queen hotkey stayed empty. After a steal the client recalls the other groups and restores the selection with a delta; those pairs change nothing.
- **Order:** a selection is sorted by unit tag (unsigned). Removals are by index into that order. Scelight appends added units at the end, so its later removals hit the wrong units. A morphing unit (larva to egg) is removed and added back under the same tag: that is the check for the order (98 % consistent with tag order, 33 % with Scelight's).
- **Remove masks** longer than 8 bits are stored in another layout than Scelight's `getBit` assumes; see `SelectionMaskDecoding` (fits 9 to 16 bits; index n-1 is always set because masks are cut after the last removed index).
- **Deaths:** a dead unit leaves control groups without a delta; the active selection gets an explicit delta.
- **Repeated commands:** current replays send the same ability on a new target as `CmdUpdateTargetUnit` / `CmdUpdateTargetPoint` (with a `CommandManagerState`), reusing the ability and flags of the user's last `Cmd`. Shift-injecting four hatcheries is one `Cmd` and three updates. Spawn Larva is `abilLink` 113; a right click has no ability (flags `0x108` = user + smart click, `0x2` = queued).
- **Decoding stops:** Scelight stops decoding some replays partway (one ZvT pro game: all players' game events end at 11:05 of 20:15; only a `GameUserLeave` follows). After the last command, selection, control group or camera event, missing orders prove nothing.
- Commands with several casters selected: one queen casts each spell; a move applies to all selected queens.

## Energy (game data)

- 25 at birth, cap 200, regeneration 0.5625 per game second = 0.03515625 per loop (0.7875 per real second at Faster). 25 energy take 712 loops, about 31.7 s real time. Game speed does not matter in loops.
- Inject, tumor: 25. Transfusion: 50.
- One inject lasts 641 loops, about 28.6 s (before 2026-11-26 the module used 29 × 16 × 36 / 26 = 642 loops). Since a queen needs 31.7 s for the next 25 energy, counting one missed inject per 29 s of red can exceed a player's energy budget by up to 11 % over long red stretches.

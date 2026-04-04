# Parser research

Checked out repositories:
- `Blizzard/s2protocol`
- `icza/s2prot`
- `ggtracker/sc2reader`
- `sebosp/s2protocol-rs`
- `ascendedguard/sc2replay-csharp`
- `tec27/comsat`

## Queen inject related findings

### Scelight / built-in Java protocol
Scelight already contains a Java port of Blizzard `s2protocol` under `scelight/src-app/hu/scelight/sc2/rep/s2prot`.

### Blizzard `s2protocol`
- Raw protocol definitions expose command ability fields like `m_abilLink`, `m_abilCmdIndex`, and target fields like `m_targetUnitTag`.
- It does **not** ship high-level balance or ability-name mapping, so it cannot directly tell us `SpawnLarva` without an external mapping layer.

### `icza/s2prot`
- Similar to Blizzard `s2protocol`, it exposes raw protocol structures.
- The generated protocol files include `m_abilLink`, `m_abilCmdIndex`, `m_targetUnitTag`, and `SCmdUpdateTargetUnitEvent`.
- Good for verifying raw event presence, but not for ability-name lookup by itself.

### `ggtracker/sc2reader`
- Has higher-level command event classes including `TargetUnitCommandEvent` and `UpdateTargetUnitCommandEvent`.
- Tests explicitly check `event.ability.name == "SpawnLarva"`.
- Source comments mention queued injects showing up as `UpdateTargetUnitCommandEvent`.

### `sebosp/s2protocol-rs`
- Includes bundled balance data with `Queen -> SpawnLarva -> Execute`.
- Its command state keeps `abil_link`, `ability`, `abil_cmd_index`, and target unit `tag`.
- README examples show aggregated `SpawnLarva.Execute` command counts.

### `ascendedguard/sc2replay-csharp`
- Has explicit `AbilityType.QueenSpawnLarva` enum support.
- Ability events also decode unit-targeted commands and assign `TargetUnit`.

### `tec27/comsat`
- Exposes raw `AbilityEvent` with `abilityCode`.
- It looks lower level in this area and does not appear to provide a ready `SpawnLarva` name mapping in the checked files.

## Practical conclusion
For inject debugging:
1. `sc2reader`, `s2protocol-rs`, and `sc2replay-csharp` are the most promising comparison targets because they already map raw commands to `SpawnLarva`-style names.
2. `Blizzard/s2protocol` and `icza/s2prot` are still useful to confirm whether raw command / target-unit events exist in the replay stream.

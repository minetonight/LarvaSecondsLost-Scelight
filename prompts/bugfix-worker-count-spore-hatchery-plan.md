0) bug:
"after the latest worker-count changes, inject efficiency disappeared, spore crawlers now appear as hatcheries, and drone count is too low compared to live game."

problem analysis:
- The replay analyzer currently treats any Zerg "building-like" tracker event as a hatchery state when it sees a unit-init/born event.
- `isBuildingConstructionLike()` is too broad: it matches unit types containing "crawler" and other generic building terms.
- That means spore crawlers (and other non-hatchery Zerg buildings) can be recorded in `hatcheryByTag` as if they were Hatcheries/Lairs/Hives.
- Those bogus hatchery states pollute larva assignment snapshots, hatchery timelines, and inject inference, which explains the lost inject efficiency metric.
- The same root cause also affects drone accounting, because building construction/cancellation events are being tracked through mixed hatchery and generic building state paths.

primary root cause:
- `LarvaReplayAnalyzer.analyze()` conflates "building construction-like" detection with actual hatchery state creation.
- `getOrCreateHatcheryState()` is invoked for any `isBuildingConstructionLike` unit type, not only `isHatcheryLike` units.

proposed fix:
1) separate hatchery state tracking from generic building construction bookkeeping.
2) only create `HatcheryState` for actual hatchery-like units: `Hatchery`, `Lair`, `Hive`.
3) continue to track drone decrement/increment for all building starts/cancellations, but do not store non-hatchery building tags in `hatcheryByTag`.
4) preserve completion tracking for non-hatchery building construction so cancellation recovery still works.

verification targets:
- there should be no spore crawler entries in the hatchery timeline list.
- inject efficiency should reappear once larva births are assigned only to real hatchery states.
- worker counts should stop using stale/incorrect drone tallies caused by spore crawler/hatchery misclassification.

next steps:
- review `LarvaReplayAnalyzer` around tracker event unit init/born handling.
- patch the `buildingConstructionLike` branch to decouple it from `hatcheryByTag` creation.
- add a regression note for `isBuildingConstructionLike()` and `isHatcheryLike()` separation.
- manually compare `resourceSnapshotsByPlayerName` output against raw player stats for Zerg games after the fix.

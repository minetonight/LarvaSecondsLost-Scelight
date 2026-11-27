#!/usr/bin/env python3
"""Milestone bands for the milestone rankings (Epic 20): league medians of the loop at which a milestone was first reached, tier
starts halfway between medians, per milestone and matchup bucket. An earlier loop is better.

Same method and rules as benchmark_bands.py (league per replay from the Zerg player's MMR and MMR-Ranges, pro = elite corpus;
MIN_SAMPLES games per league; medians must improve with skill, else the league is dropped). The differences:

- The value is the first PlayerStats loop at or above the milestone (LarvaMilestoneCalculator, through LarvaMilestoneDumpCli). Games
  that never reached it are left out, so MIN_SAMPLES counts games that reached it. The module shows k/n next to every ranking.
- Medians must get earlier with skill by at least one PlayerStats sample (MIN_GAP_LOOPS, 160 loops, 7.1 s): milestone loops are
  samples, so two leagues closer than that cannot be told apart. Walking down from pro, such a league is dropped from the tiers but
  named in the tier it was too close to ("plat/dia": plat reaches it as early as dia, E20-S10).
- One tier left: the band is a pace (E20-S10), the slowest median of the leagues named in the tier, so they all count as at pace
  ("GM/pro pace 6:47"). No tier left: not rated (n/a).
- Buckets ALL, ZVP, ZVT, ZVZ. ALL and the matchup buckets use the primary Zerg player of each replay, as the larva bands do. The ZVZ
  bucket also uses the second Zerg player of each ladder ZvZ, in the league of their own MMR (E20 OQ-4), only there, so ZvZ games do
  not weigh twice in ALL.

Commands (from the repository root; `ant MILESTONE_BANDS` in LarvaSecondsLostExtMod runs all three):

    python3 benchmark-data/tools/milestone_bands.py list --output benchmark-data/tmp/milestone-replays.tsv
    java ... LarvaMilestoneDumpCli --list benchmark-data/tmp/milestone-replays.tsv > benchmark-data/exports/milestone-times.csv
    python3 benchmark-data/tools/milestone_bands.py bands benchmark-data/exports/milestone-times.csv
"""

from __future__ import annotations

import argparse
import csv
import datetime
import statistics
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import benchmark_bands as bb  # noqa: E402  (the league code and rules are shared)

ROOT = bb.ROOT
BANDS_FILE = ROOT / "LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/larva-milestone-bands.properties"
REPORT_FILE = ROOT / "benchmark-data/exports/milestone-bands.md"
BUCKETS = ["ALL", "ZVP", "ZVT", "ZVZ"]
BUCKET_LABELS = {"ALL": "All", "ZVP": "ZvP", "ZVT": "ZvT", "ZVZ": "ZvZ"}
MATCHUP_BUCKETS = {"ZvP": "ZVP", "ZvT": "ZVT", "ZvZ": "ZVZ"}
# Columns of LarvaMilestoneDumpCli before the milestone columns (one per LarvaMilestone, its name).
FIXED_COLUMNS = ["file", "cohort", "matchup", "role", "player", "result", "replayLoops", "lastLiveLoop"]
PRIMARY, OPPONENT = "primary", "opponent"
# One PlayerStats sample (LarvaGameClock.PLAYER_STATS_INTERVAL_LOOPS): the smallest step between two league medians that means something.
MIN_GAP_LOOPS = 160


def game_clock(loop: float) -> str:
    """m:ss on the game clock (22.4 loops per second, rounded down), as LarvaGameClock.format."""
    seconds = int(loop) * 625 // 14 // 1000
    return "%d:%02d" % (seconds // 60, seconds % 60)


def milestone_label(name: str) -> str:
    kind, threshold = name.split("_")
    return "%s %s" % (threshold, "drones" if kind == "DRONES" else "supply")


# ---------------------------------------------------------------------------------------------------------------- replay list


def opponent_lines(snapshot: dict) -> list:
    """The second Zerg of every ladder ZvZ with the opponent's MMR and a region in MMR-Ranges, in the league of their own MMR."""
    lines = []
    for entry in bb.manifest_entries(bb.MMR_MANIFEST):
        region = bb.REGIONS.get(str(entry.get("opponentToon", "")).split("-")[0])
        if entry.get("matchup") != "ZvZ" or entry.get("opponentMmr") is None or region is None or not entry.get("opponentPlayer"):
            continue
        league = bb.league_of(int(entry["opponentMmr"]), region, snapshot["leagues"])
        lines.append("${REPO}/%s\t%s\t%s\tZvZ\t%s" % (entry["filePath"], entry["opponentPlayer"], league, OPPONENT))
    return lines


def write_list(output: Path) -> int:
    snapshot = bb.load_snapshot()
    entries, skipped = bb.cohort_entries(snapshot)
    opponents = opponent_lines(snapshot)
    lines = ["# replay<TAB>player<TAB>cohort<TAB>matchup<TAB>role; leagues from MMR-Ranges 1v1 season %s, written by milestone_bands.py"
             % snapshot.get("season")]
    lines += ["# skipped %s: %s" % (entry["filePath"], reason) for entry, reason in skipped]
    lines += ["${REPO}/%s\t%s\t%s\t%s\t%s" % (entry["filePath"], entry["primaryZergPlayer"], cohort, entry["matchup"], PRIMARY)
              for entry, cohort, _ in entries]
    lines += opponents
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("Wrote %s: %d primary players, %d ZvZ opponents" % (bb.rel(output), len(entries), len(opponents)))
    return 0


# ---------------------------------------------------------------------------------------------------------------- bands


def read_times(times_csv: Path) -> tuple:
    """(milestone names, reached loops per (milestone, bucket, cohort), games per (bucket, cohort))."""
    reached = defaultdict(list)
    games = defaultdict(int)
    with times_csv.open(encoding="utf-8") as handle:
        reader = csv.DictReader(line for line in handle if not line.startswith("#"))
        if reader.fieldnames[:len(FIXED_COLUMNS)] != FIXED_COLUMNS:
            raise SystemExit("%s: header %s is not the one of LarvaMilestoneDumpCli" % (bb.rel(times_csv), reader.fieldnames))
        milestones = reader.fieldnames[len(FIXED_COLUMNS):]
        for row in reader:
            bucket = MATCHUP_BUCKETS.get(row["matchup"])
            if row["role"] == PRIMARY:
                buckets = ["ALL"] + ([bucket] if bucket else [])
            elif row["role"] == OPPONENT and bucket == "ZVZ":
                buckets = ["ZVZ"]
            else:
                raise SystemExit("%s: unexpected role %r for matchup %r" % (bb.rel(times_csv), row["role"], row["matchup"]))
            for target in buckets:
                games[(target, row["cohort"])] += 1
                for milestone in milestones:
                    if row[milestone]:
                        reached[(milestone, target, row["cohort"])].append(int(row[milestone]))
    return milestones, reached, games


def choose_tiers(stats: dict) -> tuple:
    """stats: cohort -> (median loop, reached games). As benchmark_bands.choose_tiers for lower is better, plus the one-sample gap.

    Returns (kept [(cohort, median, n)] weakest first, dropped [(cohort, reason)], merged {kept cohort: [(cohort, median)] of the
    leagues dropped for the gap into that tier, strongest first})."""
    kept, dropped, merged = [], [], defaultdict(list)
    for cohort, _ in reversed(bb.COHORTS):
        if cohort not in stats:
            continue
        median, count = stats[cohort]
        if count < bb.MIN_SAMPLES:
            dropped.append((cohort, "%d games reached it (fewer than %d)" % (count, bb.MIN_SAMPLES)))
        elif kept and median - kept[-1][1] < MIN_GAP_LOOPS:
            dropped.append((cohort, "its median %s (%s) is not one sample (%d loops) later than %s %s (%s), named in that tier" % (
                game_clock(median), loop_text(median), MIN_GAP_LOOPS, bb.label(kept[-1][0]), game_clock(kept[-1][1]),
                loop_text(kept[-1][1]))))
            merged[kept[-1][0]].append((cohort, median))
        else:
            kept.append((cohort, median, count))
    kept.reverse()
    return kept, dropped, dict(merged)


def tier_labels(kept: list, merged: dict) -> list:
    """One label per kept tier, naming the leagues merged into it weakest first: plat/dia."""
    return ["/".join([bb.label(c) for c, _ in reversed(merged.get(cohort, []))] + [bb.label(cohort)]) for cohort, _, _ in kept]


def tier_starts(kept: list, merged: dict) -> list:
    """Two tiers or more: benchmark_bands.tier_starts. One tier: its pace, the slowest median of the leagues it names."""
    if len(kept) >= 2:
        return bb.tier_starts([median for _, median, _ in kept])
    if kept:
        cohort, median, _ = kept[0]
        return [max([median] + [m for _, m in merged.get(cohort, [])])]
    return []


def compute_bands(milestones: list, reached: dict) -> dict:
    """(milestone, bucket) -> {stats, kept, dropped, labels, starts}; lower is better."""
    result = {}
    for milestone in milestones:
        for bucket in BUCKETS:
            stats = {cohort: (statistics.median(reached[(milestone, bucket, cohort)]), len(reached[(milestone, bucket, cohort)]))
                     for cohort, _ in bb.COHORTS if reached.get((milestone, bucket, cohort))}
            kept, dropped, merged = choose_tiers(stats)
            result[(milestone, bucket)] = {"stats": stats, "kept": kept, "dropped": dropped, "labels": tier_labels(kept, merged),
                                           "starts": tier_starts(kept, merged)}
    return result


def loop_text(value: float) -> str:
    return str(int(value)) if value == int(value) else ("%.2f" % value).rstrip("0").rstrip(".")


def write_properties(bands: dict, milestones: list, snapshot: dict, times_csv: Path, output: Path) -> None:
    lines = [
        "# Milestone bands of the milestone rankings. Written by benchmark-data/tools/milestone_bands.py; do not edit by hand.",
        "# Times: %s. Leagues: MMR-Ranges 1v1 season %s (start %s), by the player's region; pro: elite corpus." % (
            bb.rel(times_csv), snapshot.get("season"), snapshot.get("seasonStart")),
        "# Value: first PlayerStats loop at or above the milestone, over the games that reached it. ZVZ also counts the second Zerg.",
        "# A tier starts halfway between its league median and the median of the league below; the weakest tier half a step below its own.",
        "# A label names the leagues of its tier (plat/dia: plat is not one sample later than dia). One tier: its pace, read as <label> pace m:ss.",
        "# <LarvaMilestone>.<LarvaMatchupBucket>=<label>:<tier start loop>,... weakest tier first; lower is better.",
    ]
    for milestone in milestones:
        for bucket in BUCKETS:
            band = bands[(milestone, bucket)]
            if band["starts"]:
                lines.append("%s.%s=%s" % (milestone, bucket, ",".join(
                    "%s:%s" % (label, loop_text(start)) for label, start in zip(band["labels"], band["starts"]))))
    output.write_text("\n".join(lines) + "\n", encoding="ascii")


def write_report(bands: dict, milestones: list, games: dict, snapshot: dict, times_csv: Path, output: Path) -> None:
    out = ["# Milestone bands", "",
           "Written by `benchmark-data/tools/milestone_bands.py` (`ant MILESTONE_BANDS`) on %s. Do not edit by hand." %
           datetime.date.today().isoformat(), "",
           "- Times per player-game: [%s](%s), the first `PlayerStats` loop at or above each milestone (`LarvaMilestoneCalculator`). "
           "Times are game clock m:ss (22.4 loops per second)." % (Path(bb.rel(times_csv)).name, Path(bb.rel(times_csv)).name),
           "- Leagues: [MMR-Ranges](%s) 1v1, season %s (start %s), as the larva bands (`benchmark-bands.md`). Pro: the elite corpus." % (
               snapshot["source"], snapshot.get("season"), snapshot.get("seasonStart")),
           "- Players: the primary Zerg of each replay in All, ZvP, ZvT and ZvZ; the ZvZ bucket also has the second Zerg of each ladder "
           "ZvZ, in the league of their own MMR.",
           "- Method: median per league over the games that reached the milestone (`k/n` below); a tier starts halfway between its "
           "median and the median of the league below; the weakest tier half a step below its own median. A league needs %d games "
           "that reached it, and its median must be at least one `PlayerStats` sample (%d loops, 7.1 s) later than the next kept "
           "league above it, else the league is dropped and named in that league's tier (`plat/dia`). One tier left: a pace, the "
           "slowest median of the leagues it names (`GM/pro pace 6:47`). No tier left: not rated (n/a)." % (bb.MIN_SAMPLES, MIN_GAP_LOOPS),
           "- Band file used by the module: `%s`." % bb.rel(BANDS_FILE), "",
           "## Player-games per league", "",
           "| league | " + " | ".join(BUCKET_LABELS[b] for b in BUCKETS) + " |", "|---|" + "---:|" * len(BUCKETS)]
    for cohort, short in bb.COHORTS:
        if any(games.get((bucket, cohort)) for bucket in BUCKETS):
            out.append("| %s | %s |" % (short, " | ".join(str(games.get((bucket, cohort), 0)) for bucket in BUCKETS)))
    for milestone in milestones:
        out += ["", "## %s" % milestone_label(milestone), "", "Median of the games that reached it (k/n):", "",
                "| league | " + " | ".join(BUCKET_LABELS[b] for b in BUCKETS) + " |", "|---|" + "---:|" * len(BUCKETS)]
        for cohort, short in bb.COHORTS:
            cells = []
            for bucket in BUCKETS:
                n = games.get((bucket, cohort), 0)
                stats = bands[(milestone, bucket)]["stats"]
                cells.append("%s (%d/%d)" % (game_clock(stats[cohort][0]), stats[cohort][1], n) if cohort in stats else
                             ("- (0/%d)" % n if n else "-"))
            if any(games.get((bucket, cohort)) for bucket in BUCKETS):
                out.append("| %s | %s |" % (short, " | ".join(cells)))
        out += ["", "Tier starts:", ""]
        for bucket in BUCKETS:
            band = bands[(milestone, bucket)]
            tiers = ", ".join("%s %s (%s)" % (label, game_clock(start), loop_text(start))
                              for label, start in zip(band["labels"], band["starts"]))
            if len(band["starts"]) == 1:
                tiers = "one tier, %s pace %s (%s)" % (band["labels"][0], game_clock(band["starts"][0]), loop_text(band["starts"][0]))
            line = "- %s: %s" % (BUCKET_LABELS[bucket], tiers if tiers else "not rated (no tier)")
            if band["dropped"]:
                line += ". Dropped: " + "; ".join("%s, %s" % (bb.label(cohort), reason) for cohort, reason in band["dropped"])
            out.append(line + ".")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(out) + "\n", encoding="utf-8")


def write_bands(times_csv: Path, properties: Path, report: Path) -> int:
    snapshot = bb.load_snapshot()
    milestones, reached, games = read_times(times_csv)
    bands = compute_bands(milestones, reached)
    write_properties(bands, milestones, snapshot, times_csv, properties)
    write_report(bands, milestones, games, snapshot, times_csv, report)
    for (milestone, bucket), band in bands.items():
        tiers = " ".join("%s=%s" % (label, game_clock(start)) for label, start in zip(band["labels"], band["starts"]))
        print("%-11s %-4s %s%s" % (milestone, bucket, tiers or "not rated",
                                   "  (dropped: %s)" % ", ".join(bb.label(c) for c, _ in band["dropped"]) if band["dropped"] else ""))
    print("Wrote %s and %s" % (bb.rel(properties), bb.rel(report)))
    return 0


def main(argv: list) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = parser.add_subparsers(dest="command", required=True)
    listing = sub.add_parser("list", help="write the player list for LarvaMilestoneDumpCli: league, matchup and role per player-game")
    listing.add_argument("--output", type=Path, required=True)
    bands = sub.add_parser("bands", help="league medians and tier starts from the milestone CSV")
    bands.add_argument("times_csv", type=Path)
    bands.add_argument("--properties", type=Path, default=BANDS_FILE)
    bands.add_argument("--report", type=Path, default=REPORT_FILE)
    args = parser.parse_args(argv)
    if args.command == "list":
        return write_list(args.output)
    return write_bands(args.times_csv, args.properties, args.report)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

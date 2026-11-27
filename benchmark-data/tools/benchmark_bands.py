#!/usr/bin/env python3
"""Benchmark bands for the phase table rankings: league buckets, league medians, tier starts halfway between medians.

The method is the one of the maintainer's spreadsheet (benchmark-data/scelight mod - larva efficiency calculations.ods, sheet
"may 2026 v2.1"):

1. Put every replay into a league. MMR-bucket corpus: the Zerg player's ladder MMR (read from the replay) against the 1v1 league
   limits of the player's region from MMR-Ranges (https://burnysc2.github.io/MMR-Ranges/). An MMR above Master 1 counts as
   GrandMaster. Elite corpus (tournament replays of named pros, no MMR): the pro tier.
2. Take the median rate of each league per metric and phase.
3. A tier starts halfway between its league median and the median of the league below. The lowest tier starts half a step below
   its own median.

Rules that need no judgment, so the command can be repeated without an agent:

- A league needs at least MIN_SAMPLES replays with a rate in that phase.
- Medians must improve with skill. Walking down from pro, a league whose median is not worse than the next kept league above it is
  dropped for that phase (the report names it and why).
- A phase needs two tiers or more; otherwise it is not rated (the phase table shows n/a).

Commands (from the repository root; `ant BENCHMARK_BANDS` in LarvaSecondsLostExtMod runs list, the rate dump and bands in one go):

    python3 benchmark-data/tools/benchmark_bands.py fetch-mmr-ranges
    python3 benchmark-data/tools/benchmark_bands.py list --output benchmark-data/tmp/bands-replays.tsv
    java ... LarvaPhaseRateDumpCli --list benchmark-data/tmp/bands-replays.tsv > benchmark-data/exports/phase-rates.csv
    python3 benchmark-data/tools/benchmark_bands.py bands benchmark-data/exports/phase-rates.csv

fetch-mmr-ranges needs the network and replaces benchmark-data/mmr-ranges/mmr-ranges-1v1.json; the other commands work offline
from that snapshot.
"""

from __future__ import annotations

import argparse
import csv
import datetime
import json
import re
import statistics
import sys
import urllib.request
from collections import defaultdict
from pathlib import Path
from urllib.parse import urljoin

ROOT = Path(__file__).resolve().parents[2]
MMR_SNAPSHOT = ROOT / "benchmark-data/mmr-ranges/mmr-ranges-1v1.json"
MMR_MANIFEST = ROOT / "benchmark-data/manifests/replay-corpus-mmr-buckets-canonical.json"
PRO_MANIFEST = ROOT / "benchmark-data/manifests/replay-corpus-elite-7000-full.json"
BANDS_FILE = ROOT / "LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/larva-benchmark-bands.properties"
REPORT_FILE = ROOT / "benchmark-data/exports/benchmark-bands.md"
SITE_URL = "https://burnysc2.github.io/MMR-Ranges/"
USER_AGENT = "larva-benchmark-bands/1"

# Toon handles are "<region>-S2-<realm>-<id>"; MMR-Ranges has the three main regions.
REGIONS = {"1": "Americas", "2": "Europe", "3": "Korea"}
TABLE_ROWS = ["GrandMaster"] + ["%s %d" % (league, tier) for league in ("Master", "Diamond", "Platinum", "Gold", "Silver", "Bronze")
                                for tier in (1, 2, 3)]
# Cohorts from the weakest up, with the phase table's compact labels.
COHORTS = [("Bronze", "bronze"), ("Silver", "silver"), ("Gold", "gold"), ("Platinum", "plat"), ("Diamond", "dia"),
           ("Master", "masters"), ("GrandMaster", "GM"), ("pro", "pro")]
PHASES = ["EARLY", "MID", "LATE"]
# CSV column -> (band key prefix, higher is better)
METRICS = {
    "spawnedLarvaPerHatchPerMinute": ("spawned", True),
    "larvaMissedPerHatchPerMinute": ("missed", False),
}
MIN_SAMPLES = 20


# ---------------------------------------------------------------------------------------------------------------- MMR-Ranges


def http_get(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def json_blobs(script: str) -> list:
    """The site embeds its tables as JSON.parse('...') string literals in the bundled script."""
    blobs = []
    for literal in re.findall(r"JSON\.parse\('((?:[^'\\]|\\.)*)'\)", script):
        try:
            blobs.append(json.loads(literal.replace("\\'", "'")))
        except ValueError:
            continue
    return blobs


def find_1v1_table(blobs: list) -> list:
    """The 1v1 table is the only one with Grandmaster ranges (team modes show '-')."""
    for blob in blobs:
        if not isinstance(blob, dict):
            continue
        for table in blob.values():
            if (isinstance(table, list) and len(table) == len(TABLE_ROWS) + 1 and table[0] == ["Server", "Americas", "Europe", "Korea"]
                    and table[1][0] == "GrandMaster" and table[1][1] != "-"):
                return table
    raise SystemExit("MMR-Ranges: no 1v1 table found in the site script (the site layout changed?)")


def find_season(blobs: list) -> dict:
    """The header blob holds the season number (int), the season start (date) and the last update (date and time)."""
    for blob in blobs:
        if not isinstance(blob, dict):
            continue
        numbers = [v for v in blob.values() if isinstance(v, int) and 0 < v < 1000]
        dates = [v for v in blob.values() if isinstance(v, str) and re.search(r"\d{4}-\d{2}-\d{2}", v)]
        if len(numbers) == 1 and len(dates) >= 2:
            updated = [d for d in dates if re.search(r"\d{2}:\d{2}", d)]
            start = [d for d in dates if not re.search(r"\d{2}:\d{2}", d)]
            return {
                "season": numbers[0],
                "seasonStart": re.search(r"\d{4}-\d{2}-\d{2}", start[0]).group(0) if start else None,
                "siteUpdated": re.search(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}", updated[0]).group(0) if updated else None,
            }
    return {"season": None, "seasonStart": None, "siteUpdated": None}


def parse_range(text: str) -> list:
    match = re.fullmatch(r"\s*(\d+)\s*-\s*(\d+)\s*", text)
    if not match:
        raise SystemExit("MMR-Ranges: cannot read range %r" % text)
    return [int(match.group(1)), int(match.group(2))]


def validate_leagues(leagues: dict) -> None:
    if list(leagues) != TABLE_ROWS:
        raise SystemExit("MMR-Ranges: unexpected league rows %s" % list(leagues))
    for region in REGIONS.values():
        previous = None
        for name in TABLE_ROWS[1:]:
            low, high = leagues[name][region]
            if low >= high:
                raise SystemExit("MMR-Ranges: %s %s range %d-%d is empty" % (region, name, low, high))
            if previous is not None and high > previous:
                raise SystemExit("MMR-Ranges: %s %s overlaps the league above" % (region, name))
            previous = low


def fetch_mmr_ranges(output: Path) -> int:
    page = http_get(SITE_URL)
    match = re.search(r'<script[^>]+src="([^"]*main\.[0-9a-f]+\.js)"', page)
    if not match:
        raise SystemExit("MMR-Ranges: no main script on %s (the site layout changed?)" % SITE_URL)
    script_url = urljoin(SITE_URL, match.group(1))
    blobs = json_blobs(http_get(script_url))
    table = find_1v1_table(blobs)
    regions = table[0][1:]
    leagues = {row[0]: {region: parse_range(value) for region, value in zip(regions, row[1:])} for row in table[1:]}
    validate_leagues(leagues)
    snapshot = {"source": SITE_URL, "script": script_url, "mode": "1v1",
                "fetchedAt": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")}
    snapshot.update(find_season(blobs))
    snapshot["leagues"] = leagues
    output.parent.mkdir(parents=True, exist_ok=True)
    text = re.sub(r"\[\s*(\d+),\s*(\d+)\s*\]", r"[\1, \2]", json.dumps(snapshot, indent=1))
    output.write_text(text + "\n", encoding="utf-8")
    print("MMR-Ranges season %s (start %s, site updated %s) written to %s" % (
        snapshot["season"], snapshot["seasonStart"], snapshot["siteUpdated"], output.relative_to(ROOT)))
    return 0


def load_snapshot() -> dict:
    snapshot = json.loads(MMR_SNAPSHOT.read_text(encoding="utf-8"))
    validate_leagues(snapshot["leagues"])
    return snapshot


def league_of(mmr: int, region: str, leagues: dict) -> str:
    """League name for an MMR. A gap between two leagues counts as the lower one; above Master 1 is GrandMaster."""
    for name in TABLE_ROWS[1:]:
        low, high = leagues[name][region]
        if mmr >= low:
            return "GrandMaster" if name == "Master 1" and mmr >= high else name.split()[0]
    return "Bronze"


# ---------------------------------------------------------------------------------------------------------------- replay list


def manifest_entries(path: Path) -> list:
    return json.loads(path.read_text(encoding="utf-8"))["acceptedReplayList"]


def cohort_entries(snapshot: dict) -> tuple:
    """(entries with their cohort, skipped entries with the reason)."""
    entries, skipped = [], []
    for entry in manifest_entries(MMR_MANIFEST):
        region = REGIONS.get(str(entry.get("primaryZergToon", "")).split("-")[0])
        if region is None or entry.get("primaryZergMmr") is None:
            skipped.append((entry, "region %s is not in MMR-Ranges" % str(entry.get("primaryZergToon", "?")).split("-")[0]))
            continue
        entries.append((entry, league_of(int(entry["primaryZergMmr"]), region, snapshot["leagues"]), region))
    for entry in manifest_entries(PRO_MANIFEST):
        entries.append((entry, "pro", None))
    return entries, skipped


def write_list(output: Path) -> int:
    snapshot = load_snapshot()
    entries, skipped = cohort_entries(snapshot)
    lines = ["# replay<TAB>player<TAB>cohort<TAB>matchup; leagues from MMR-Ranges 1v1 season %s, written by benchmark_bands.py"
             % snapshot.get("season")]
    lines += ["# skipped %s: %s" % (entry["filePath"], reason) for entry, reason in skipped]
    lines += ["${REPO}/%s\t%s\t%s\t%s" % (entry["filePath"], entry["primaryZergPlayer"], cohort, entry["matchup"])
              for entry, cohort, _ in entries]
    text = "\n".join(lines) + "\n"
    if output is None:
        sys.stdout.write(text)
    else:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text, encoding="utf-8")
    return 0


# ---------------------------------------------------------------------------------------------------------------- bands


def read_rates(rate_csv: Path) -> tuple:
    """(values per (metric, phase, cohort), replay files per cohort)."""
    values = defaultdict(list)
    replays = defaultdict(set)
    with rate_csv.open(encoding="utf-8") as handle:
        for row in csv.DictReader(line for line in handle if not line.startswith("#")):
            replays[row["cohort"]].add(row["file"])
            for metric in METRICS:
                if row[metric]:
                    values[(metric, row["phase"], row["cohort"])].append(float(row[metric]))
    return values, replays


def is_worse(value: float, than: float, higher_is_better: bool) -> bool:
    return value < than if higher_is_better else value > than


def choose_tiers(stats: dict, higher_is_better: bool) -> tuple:
    """stats: cohort -> (median, n). Returns (kept [(cohort, median, n)] weakest first, dropped [(cohort, reason)])."""
    kept, dropped = [], []
    for cohort, _ in reversed(COHORTS):
        if cohort not in stats:
            continue
        median, count = stats[cohort]
        if count < MIN_SAMPLES:
            dropped.append((cohort, "%d replays (fewer than %d)" % (count, MIN_SAMPLES)))
        elif kept and not is_worse(median, kept[-1][1], higher_is_better):
            dropped.append((cohort, "its median %.4f is not %s the %s median %.4f" % (
                median, "below" if higher_is_better else "above", label(kept[-1][0]), kept[-1][1])))
        else:
            kept.append((cohort, median, count))
    kept.reverse()
    return kept, dropped


def tier_starts(medians: list) -> list:
    starts = [medians[0] - (medians[1] - medians[0]) / 2.0]
    starts += [(medians[i - 1] + medians[i]) / 2.0 for i in range(1, len(medians))]
    return starts


def label(cohort: str) -> str:
    return dict(COHORTS)[cohort]


def compute_bands(values: dict) -> dict:
    """(metric, phase) -> {stats, kept, dropped, starts}."""
    result = {}
    for metric, (_, higher_is_better) in METRICS.items():
        for phase in PHASES:
            stats = {cohort: (statistics.median(values[(metric, phase, cohort)]), len(values[(metric, phase, cohort)]))
                     for cohort, _ in COHORTS if values.get((metric, phase, cohort))}
            kept, dropped = choose_tiers(stats, higher_is_better)
            starts = tier_starts([median for _, median, _ in kept]) if len(kept) >= 2 else []
            result[(metric, phase)] = {"stats": stats, "kept": kept, "dropped": dropped, "starts": starts}
    return result


def write_properties(bands: dict, snapshot: dict, rate_csv: Path, output: Path) -> None:
    lines = [
        "# Benchmark bands of the phase table rankings. Written by benchmark-data/tools/benchmark_bands.py; do not edit by hand.",
        "# Rates: %s. Leagues: MMR-Ranges 1v1 season %s (start %s), by the player's region; pro: elite corpus." % (
            rel(rate_csv), snapshot.get("season"), snapshot.get("seasonStart")),
        "# A tier starts halfway between its league median and the median of the league below; the lowest tier half a step below its own.",
        "# <metric>.<phase>=<label>:<tier start>,... weakest tier first. spawned: higher is better. missed: lower is better.",
    ]
    for metric, (prefix, _) in METRICS.items():
        for phase in PHASES:
            band = bands[(metric, phase)]
            if band["starts"]:
                lines.append("%s.%s=%s" % (prefix, phase, ",".join(
                    "%s:%.4f" % (label(cohort), start) for (cohort, _, _), start in zip(band["kept"], band["starts"]))))
    output.write_text("\n".join(lines) + "\n", encoding="ascii")


def rel(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(ROOT))
    except ValueError:
        return str(path)


def mmr_cell(snapshot: dict, cohort: str, region: str) -> str:
    leagues = snapshot["leagues"]
    rows = [name for name in TABLE_ROWS[1:] if name.split()[0] == cohort]
    if cohort == "GrandMaster":
        return "%d+" % leagues["Master 1"][region][1]
    if cohort == "Bronze":
        return "< %d" % leagues["Bronze 1"][region][1]
    if not rows:
        return "-"
    return "%d-%d" % (leagues[rows[-1]][region][0], leagues[rows[0]][region][1])


def write_report(bands: dict, snapshot: dict, replays: dict, skipped: list, rate_csv: Path, output: Path) -> None:
    out = ["# Benchmark bands", "",
           "Written by `benchmark-data/tools/benchmark_bands.py` (`ant BENCHMARK_BANDS`) on %s. Do not edit by hand." %
           datetime.date.today().isoformat(), "",
           "- Rates per replay and phase: [%s](%s) (primary Zerg player; per game clock minute, 1344 loops, as in `LarvaPhaseStats` since 2026-11-26)." % (
               Path(rel(rate_csv)).name, Path(rel(rate_csv)).name),
           "- Leagues: [MMR-Ranges](%s) 1v1, season %s (start %s, site updated %s, snapshot `%s`). The replays come from many "
           "seasons; one season's limits are used for all of them." % (
               snapshot["source"], snapshot.get("season"), snapshot.get("seasonStart"), snapshot.get("siteUpdated"), rel(MMR_SNAPSHOT)),
           "- Method: median per league; a tier starts halfway between its median and the median of the league below; the lowest tier "
           "half a step below its own median (the maintainer's spreadsheet, sheet \"may 2026 v2.1\"). A league needs %d replays in "
           "the phase, and medians must improve with skill, else the league is dropped for that phase." % MIN_SAMPLES,
           "- Band file used by the module: `%s`." % rel(BANDS_FILE), "",
           "## Leagues", "",
           "| league | label | MMR Europe | MMR Americas | MMR Korea | replays |", "|---|---|---|---|---|---:|"]
    for cohort, short in COHORTS:
        if cohort == "pro":
            out.append("| pro | pro | elite corpus (tournaments) | | | %d |" % len(replays.get(cohort, ())))
        else:
            out.append("| %s | %s | %s | %s | %s | %d |" % (cohort, short, mmr_cell(snapshot, cohort, "Europe"),
                                                       mmr_cell(snapshot, cohort, "Americas"), mmr_cell(snapshot, cohort, "Korea"),
                                                       len(replays.get(cohort, ()))))
    if skipped:
        out.append("")
        out.append("Skipped: %d replays (%s)." % (len(skipped), "; ".join(sorted({reason for _, reason in skipped}))))
    for metric, (prefix, higher_is_better) in METRICS.items():
        out += ["", "## %s (%s is better)" % (metric, "higher" if higher_is_better else "lower"), "",
                "Median (replays) per league:", "",
                "| phase | " + " | ".join(short for _, short in COHORTS) + " |", "|---|" + "---:|" * len(COHORTS)]
        for phase in PHASES:
            stats = bands[(metric, phase)]["stats"]
            out.append("| %s | %s |" % (phase, " | ".join(
                "%.4f (%d)" % stats[cohort] if cohort in stats else "-" for cohort, _ in COHORTS)))
        out += ["", "Tier starts:", ""]
        for phase in PHASES:
            band = bands[(metric, phase)]
            tiers = ", ".join("%s %.4f" % (label(cohort), start) for (cohort, _, _), start in zip(band["kept"], band["starts"]))
            line = "- %s: %s" % (phase, tiers if tiers else "not rated (fewer than two tiers)")
            if band["dropped"]:
                line += ". Dropped: " + "; ".join("%s, %s" % (label(cohort), reason) for cohort, reason in band["dropped"])
            out.append(line + ".")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(out) + "\n", encoding="utf-8")


def write_bands(rate_csv: Path, properties: Path, report: Path) -> int:
    snapshot = load_snapshot()
    values, replays = read_rates(rate_csv)
    _, skipped = cohort_entries(snapshot)
    bands = compute_bands(values)
    write_properties(bands, snapshot, rate_csv, properties)
    write_report(bands, snapshot, replays, skipped, rate_csv, report)
    for (metric, phase), band in bands.items():
        tiers = " ".join("%s=%.4f" % (label(cohort), start) for (cohort, _, _), start in zip(band["kept"], band["starts"]))
        print("%-31s %-5s %s%s" % (metric, phase, tiers or "not rated",
                                   "  (dropped: %s)" % ", ".join(label(c) for c, _ in band["dropped"]) if band["dropped"] else ""))
    print("Wrote %s and %s" % (rel(properties), rel(report)))
    return 0


def main(argv: list) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = parser.add_subparsers(dest="command", required=True)
    fetch = sub.add_parser("fetch-mmr-ranges", help="download the current MMR-Ranges 1v1 table (network)")
    fetch.add_argument("--output", type=Path, default=MMR_SNAPSHOT)
    listing = sub.add_parser("list", help="write the replay list for LarvaPhaseRateDumpCli, cohort = league")
    listing.add_argument("--output", type=Path)
    bands = sub.add_parser("bands", help="league medians and tier starts from the rate CSV")
    bands.add_argument("rate_csv", type=Path)
    bands.add_argument("--properties", type=Path, default=BANDS_FILE)
    bands.add_argument("--report", type=Path, default=REPORT_FILE)
    args = parser.parse_args(argv)
    if args.command == "fetch-mmr-ranges":
        return fetch_mmr_ranges(args.output)
    if args.command == "list":
        return write_list(args.output)
    return write_bands(args.rate_csv, args.properties, args.report)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

#!/usr/bin/env python3
"""Build a low-MMR benchmark replay corpus from public SpawningTool listings.

This tool downloads candidate replay files page by page, extracts exact ladder MMR
from replay init-data when present, and retains only replays that fill the target
Epic 13 2000 / 3000 / 4000 / 5000 cohort buckets.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import zipfile
from collections import defaultdict
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

from benchmark_corpus_tool import write_json
from internet_replay_source_tool import download_file
from internet_replay_source_tool import parse_spawningtool_replays
from internet_replay_source_tool import sanitize_filename
from internet_replay_source_tool import sha256_hex


REPO_ROOT = Path(__file__).resolve().parents[2]
SC2READER_ROOT = REPO_ROOT / "parser-research/sc2reader"
if str(SC2READER_ROOT) not in sys.path:
    sys.path.insert(0, str(SC2READER_ROOT))

import sc2reader  # type: ignore


SCHEMA_VERSION = "epic13-mmr-bucket-builder-v1"
LEAGUE_NAMES = {
    0: "Unknown",
    1: "Bronze",
    2: "Silver",
    3: "Gold",
    4: "Platinum",
    5: "Diamond",
    6: "Master",
    7: "Grandmaster",
    8: "Unranked",
}
MATCHUP_SPECS = {
    "ZvT": {
        "url": "https://lotv.spawningtool.com/replays/?tag=3",
        "opponentRace": "Terran",
    },
    "ZvP": {
        "url": "https://lotv.spawningtool.com/replays/?tag=11",
        "opponentRace": "Protoss",
    },
    "ZvZ": {
        "url": "https://lotv.spawningtool.com/replays/?tag=13",
        "opponentRace": "Zerg",
    },
}
BUCKET_RANGES = {
    "mmr-2000": (2000, 3000),
    "mmr-3000": (3000, 4000),
    "mmr-4000": (4000, 5000),
    "mmr-5000": (5000, 10_000_000),
}
BUCKET_ORDER = ("mmr-2000", "mmr-3000", "mmr-4000", "mmr-5000")


class CorpusBuildError(Exception):
    """Raised when the MMR corpus builder cannot continue."""


def iso_now() -> str:
    from datetime import datetime, timezone

    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def league_name(league_id: Optional[int]) -> str:
    if league_id is None:
        return "Unknown"
    return LEAGUE_NAMES.get(int(league_id), f"League-{league_id}")


def bucket_for_mmr(mmr: Optional[int]) -> Optional[str]:
    if mmr is None:
        return None
    for bucket_name, (lower, upper) in BUCKET_RANGES.items():
        if lower <= mmr < upper:
            return bucket_name
    return None


def bucket_midpoint(bucket_name: str) -> float:
    lower, upper = BUCKET_RANGES[bucket_name]
    return (lower + upper) / 2.0


def parse_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        return None
    return parsed if parsed > 0 else None


def load_replay_candidates(replay_path: Path, expected_matchup: str) -> List[Dict[str, Any]]:
    replay = sc2reader.load_replay(str(replay_path), load_map=False)
    players = list(replay.players)
    if len(players) != 2:
        return []

    expected_opponent_race = MATCHUP_SPECS[expected_matchup]["opponentRace"]
    candidates: List[Dict[str, Any]] = []

    for player in players:
        play_race = getattr(player, "play_race", None)
        if play_race != "Zerg":
            continue

        opponents = [opponent for opponent in players if opponent is not player]
        if len(opponents) != 1:
            continue
        opponent = opponents[0]
        opponent_race = getattr(opponent, "play_race", None)
        if expected_matchup != "ZvZ" and opponent_race != expected_opponent_race:
            continue
        if expected_matchup == "ZvZ" and opponent_race != "Zerg":
            continue

        init_data = getattr(player, "init_data", {}) or {}
        opponent_init_data = getattr(opponent, "init_data", {}) or {}
        player_mmr = parse_int(init_data.get("scaled_rating"))
        opponent_mmr = parse_int(opponent_init_data.get("scaled_rating"))
        player_league_id = parse_int(init_data.get("highest_league"))
        opponent_league_id = parse_int(opponent_init_data.get("highest_league"))

        candidates.append(
            {
                "primaryZergPlayer": getattr(player, "name", None),
                "primaryZergToon": getattr(player, "toon_handle", None),
                "primaryZergRace": play_race,
                "primaryZergMmr": player_mmr,
                "primaryZergLeagueId": player_league_id,
                "primaryZergLeague": league_name(player_league_id),
                "opponentPlayer": getattr(opponent, "name", None),
                "opponentToon": getattr(opponent, "toon_handle", None),
                "opponentRace": opponent_race,
                "opponentMmr": opponent_mmr,
                "opponentLeagueId": opponent_league_id,
                "opponentLeague": league_name(opponent_league_id),
            }
        )

    return candidates


def choose_candidate(
    candidates: Sequence[Dict[str, Any]],
    counts: Dict[str, int],
    target_per_bucket: int,
) -> Optional[Tuple[str, Dict[str, Any]]]:
    eligible: List[Tuple[str, Dict[str, Any]]] = []
    for candidate in candidates:
        bucket_name = bucket_for_mmr(candidate.get("primaryZergMmr"))
        if bucket_name is None:
            continue
        if counts[bucket_name] >= target_per_bucket:
            continue
        eligible.append((bucket_name, candidate))

    if not eligible:
        return None

    def sort_key(item: Tuple[str, Dict[str, Any]]) -> Tuple[int, float, str, str]:
        bucket_name, candidate = item
        mmr = int(candidate["primaryZergMmr"])
        remaining = target_per_bucket - counts[bucket_name]
        return (
            -remaining,
            abs(mmr - bucket_midpoint(bucket_name)),
            str(candidate.get("primaryZergPlayer") or ""),
            bucket_name,
        )

    return sorted(eligible, key=sort_key)[0]


def build_summary_markdown(report: Dict[str, Any]) -> str:
    matchups: Sequence[str] = report.get("matchups") or tuple(MATCHUP_SPECS)
    lines: List[str] = []
    lines.append("# Exact-MMR replay bucket build summary")
    lines.append("")
    lines.append(f"Generated: {report['generatedAt']}")
    lines.append("")
    lines.append(f"- Target per cohort × matchup: {report['targetPerBucket']}")
    lines.append(f"- Selected replay count: {report['selectedReplayCount']}")
    if report.get("preexistingReplayCount"):
        lines.append(f"- Preexisting replay count: {report['preexistingReplayCount']}")
    lines.append(f"- Downloaded page ZIP count: {report['downloadedArchiveCount']}")
    lines.append(f"- Extracted replay count: {report['downloadedReplayCount']}")
    lines.append(f"- Exact-MMR candidate count: {report['exactMmrCandidateCount']}")
    lines.append(f"- Missing-MMR replay count: {report['missingMmrReplayCount']}")
    lines.append(f"- Parse failure count: {report['parseFailureCount']}")
    lines.append("")
    lines.append("## Final counts")
    lines.append("")
    matchup_header = " | ".join(matchups)
    lines.append(f"| Cohort | {matchup_header} | Total |")
    lines.append("|--------|" + "|".join(["-----"] * len(matchups)) + "|-------|")
    for bucket_name in BUCKET_ORDER[::-1]:
        row = report["bucketSummary"][bucket_name]
        total = sum(row[matchup] for matchup in matchups)
        matchup_values = " | ".join(str(row[matchup]) for matchup in matchups)
        lines.append(f"| {bucket_name} | {matchup_values} | {total} |")
    lines.append("")
    lines.append("## Page scan summary")
    lines.append("")
    lines.append("| Matchup | Pages scanned | Replays seen | Selected |")
    lines.append("|---------|---------------|--------------|----------|")
    for matchup in matchups:
        summary = report["matchupScanSummary"][matchup]
        lines.append(
            f"| {matchup} | {summary['pagesScanned']} | {summary['replaysSeen']} | {summary['selected']} |"
        )
    lines.append("")
    if report.get("missingTargets"):
        lines.append("## Missing targets")
        lines.append("")
        for item in report["missingTargets"]:
            lines.append(f"- {item['cohortBucket']} {item['matchup']}: missing {item['missingCount']}")
        lines.append("")
    return "\n".join(lines) + "\n"


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build a bucketed exact-MMR replay corpus from SpawningTool.")
    parser.add_argument("--target-per-bucket", type=int, default=100, help="Target replay count per cohort × matchup.")
    parser.add_argument("--start-page", type=int, default=1, help="First listing page to scan per matchup.")
    parser.add_argument("--max-pages", type=int, default=250, help="Maximum listing pages to scan per matchup.")
    parser.add_argument(
        "--matchups",
        nargs="+",
        choices=tuple(MATCHUP_SPECS),
        default=tuple(MATCHUP_SPECS),
        help="Subset of matchups to build.",
    )
    parser.add_argument(
        "--final-root",
        type=Path,
        default=REPO_ROOT / "benchmark-data/raw-replays/bucketed-mmr",
        help="Final bucketed replay directory.",
    )
    parser.add_argument(
        "--staging-dir",
        type=Path,
        default=REPO_ROOT / "benchmark-data/raw-replays/mmr-staging",
        help="Temporary download directory used while classifying replays.",
    )
    parser.add_argument(
        "--report-json-out",
        type=Path,
        default=REPO_ROOT / "benchmark-data/reports/mmr-bucket-selection.json",
        help="JSON report path.",
    )
    parser.add_argument(
        "--report-md-out",
        type=Path,
        default=REPO_ROOT / "benchmark-data/reports/mmr-bucket-selection.md",
        help="Markdown report path.",
    )
    parser.add_argument(
        "--sleep-seconds",
        type=float,
        default=0.0,
        help="Optional delay between replay downloads.",
    )
    return parser.parse_args(argv)


def extract_page_zip(archive_path: Path, target_dir: Path) -> List[Path]:
    extracted_paths: List[Path] = []
    with zipfile.ZipFile(archive_path) as archive:
        for member in archive.infolist():
            if member.is_dir() or not member.filename.lower().endswith(".sc2replay"):
                continue
            safe_name = Path(member.filename).name
            extracted_path = target_dir / safe_name
            with archive.open(member) as source, extracted_path.open("wb") as handle:
                handle.write(source.read())
            extracted_paths.append(extracted_path)
    return extracted_paths


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    active_matchups = list(dict.fromkeys(args.matchups))
    args.final_root = args.final_root.resolve()
    args.staging_dir = args.staging_dir.resolve()
    args.report_json_out = args.report_json_out.resolve()
    args.report_md_out = args.report_md_out.resolve()
    args.final_root.mkdir(parents=True, exist_ok=True)
    args.staging_dir.mkdir(parents=True, exist_ok=True)

    counts: Dict[str, Dict[str, int]] = {
        bucket_name: {matchup: 0 for matchup in active_matchups} for bucket_name in BUCKET_ORDER
    }
    selected_replay_list: List[Dict[str, Any]] = []
    selected_hashes: set[str] = set()
    seen_replay_ids: set[str] = set()
    preexisting_replay_count = 0
    downloaded_archive_count = 0
    downloaded_replay_count = 0
    exact_mmr_candidate_count = 0
    missing_mmr_replay_count = 0
    parse_failure_count = 0
    matchup_scan_summary: Dict[str, Dict[str, int]] = {
        matchup: {"pagesScanned": 0, "replaysSeen": 0, "selected": 0} for matchup in active_matchups
    }

    for bucket_name in BUCKET_ORDER:
        for matchup in active_matchups:
            existing_dir = args.final_root / bucket_name / matchup
            if not existing_dir.is_dir():
                continue
            existing_paths = sorted(existing_dir.glob("*.SC2Replay"))
            counts[bucket_name][matchup] += len(existing_paths)
            preexisting_replay_count += len(existing_paths)
            for existing_path in existing_paths:
                selected_hashes.add(existing_path.stem)

    def quotas_complete(matchup: str) -> bool:
        return all(counts[bucket_name][matchup] >= args.target_per_bucket for bucket_name in BUCKET_ORDER)

    for matchup in active_matchups:
        spec = MATCHUP_SPECS[matchup]
        for page_number in range(args.start_page, args.max_pages + 1):
            if quotas_complete(matchup):
                break

            page_url = spec["url"] if page_number == 1 else f"{spec['url']}&p={page_number}"
            page_payload: Optional[Dict[str, Any]] = None
            last_error: Optional[Exception] = None
            for _attempt in range(3):
                try:
                    page_payload = parse_spawningtool_replays(page_url, 1)
                    break
                except Exception as exc:  # pragma: no cover - network resilience
                    last_error = exc
                    time.sleep(2.0)
            if page_payload is None:
                raise CorpusBuildError(f"Failed to load {page_url}: {last_error}")

            replay_list = page_payload.get("replayList") or []
            matchup_scan_summary[matchup]["pagesScanned"] += 1
            matchup_scan_summary[matchup]["replaysSeen"] += len(replay_list)
            zip_url_list = ((page_payload.get("pageList") or [{}])[0]).get("zipUrlList") or []
            if not zip_url_list:
                raise CorpusBuildError(f"No ZIP URL found for {page_url}")

            page_stage_dir = args.staging_dir / matchup / f"page-{page_number:04d}"
            page_stage_dir.mkdir(parents=True, exist_ok=True)
            zip_result = download_file(zip_url_list[0], page_stage_dir, f"{matchup}_page_{page_number:04d}.zip", retries=2)
            downloaded_archive_count += 1
            archive_path = Path(zip_result["filePath"])
            extracted_paths = extract_page_zip(archive_path, page_stage_dir)
            downloaded_replay_count += len(extracted_paths)
            archive_path.unlink(missing_ok=True)

            replay_entries_by_title: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
            for replay_entry in replay_list:
                replay_entries_by_title[str(replay_entry.get("title") or "")].append(replay_entry)

            for replay_path in extracted_paths:
                title = replay_path.name[: -len(".SC2Replay")]
                replay_entry_list = replay_entries_by_title.get(title) or []
                replay_entry = replay_entry_list.pop(0) if replay_entry_list else None
                replay_id = str((replay_entry or {}).get("replayId") or title)
                if replay_id in seen_replay_ids:
                    replay_path.unlink(missing_ok=True)
                    continue
                seen_replay_ids.add(replay_id)

                try:
                    replay_hash = sha256_hex(replay_path)
                    if replay_hash in selected_hashes:
                        replay_path.unlink(missing_ok=True)
                        continue

                    candidates = load_replay_candidates(replay_path, matchup)
                    exact_candidates = [candidate for candidate in candidates if candidate.get("primaryZergMmr") is not None]
                    if exact_candidates:
                        exact_mmr_candidate_count += 1
                    else:
                        missing_mmr_replay_count += 1
                        replay_path.unlink(missing_ok=True)
                        continue

                    selection = choose_candidate(
                        exact_candidates,
                        counts={bucket: counts[bucket][matchup] for bucket in BUCKET_ORDER},
                        target_per_bucket=args.target_per_bucket,
                    )
                    if selection is None:
                        replay_path.unlink(missing_ok=True)
                        continue

                    bucket_name, candidate = selection
                    final_dir = args.final_root / bucket_name / matchup
                    final_dir.mkdir(parents=True, exist_ok=True)
                    final_path = final_dir / f"{replay_hash}.SC2Replay"
                    replay_path.replace(final_path)

                    selected_replay_list.append(
                        {
                            "replayHash": replay_hash,
                            "filePath": str(final_path.relative_to(REPO_ROOT)),
                            "originalFileName": replay_path.name,
                            "sizeBytes": final_path.stat().st_size,
                            "sourceLabel": "SpawningTool replay browser",
                            "sourceUrlList": [
                                (replay_entry or {}).get("detailUrl"),
                                (replay_entry or {}).get("downloadUrl"),
                                zip_url_list[0],
                            ],
                            "acquiredAt": iso_now(),
                            "cohortBucket": bucket_name,
                            "matchup": matchup,
                            "primaryZergPlayer": candidate.get("primaryZergPlayer"),
                            "primaryZergToon": candidate.get("primaryZergToon"),
                            "primaryZergMmr": candidate.get("primaryZergMmr"),
                            "primaryZergLeague": candidate.get("primaryZergLeague"),
                            "opponentPlayer": candidate.get("opponentPlayer"),
                            "opponentToon": candidate.get("opponentToon"),
                            "opponentRace": candidate.get("opponentRace"),
                            "opponentMmr": candidate.get("opponentMmr"),
                            "opponentLeague": candidate.get("opponentLeague"),
                            "replayId": replay_id,
                            "title": title,
                            "detailUrl": (replay_entry or {}).get("detailUrl"),
                            "downloadUrl": (replay_entry or {}).get("downloadUrl"),
                            "cohortConfidence": "confirmed",
                            "cohortEvidence": "Exact ladder MMR read from replay init_data.scaled_rating.",
                        }
                    )
                    counts[bucket_name][matchup] += 1
                    matchup_scan_summary[matchup]["selected"] += 1
                    selected_hashes.add(replay_hash)
                except Exception:  # pragma: no cover - operational loop
                    parse_failure_count += 1
                    replay_path.unlink(missing_ok=True)
                    continue
                finally:
                    if args.sleep_seconds > 0:
                        time.sleep(args.sleep_seconds)

                if quotas_complete(matchup):
                    break

            for leftover_path in page_stage_dir.glob("*.SC2Replay"):
                leftover_path.unlink(missing_ok=True)
            page_stage_dir.rmdir()

    bucket_summary = {
        bucket_name: {matchup: counts[bucket_name][matchup] for matchup in active_matchups}
        for bucket_name in BUCKET_ORDER
    }
    missing_targets: List[Dict[str, Any]] = []
    for bucket_name in BUCKET_ORDER:
        for matchup in active_matchups:
            missing_count = args.target_per_bucket - counts[bucket_name][matchup]
            if missing_count > 0:
                missing_targets.append(
                    {
                        "cohortBucket": bucket_name,
                        "matchup": matchup,
                        "missingCount": missing_count,
                    }
                )

    report = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "matchups": active_matchups,
        "targetPerBucket": args.target_per_bucket,
        "startPage": args.start_page,
        "maxPages": args.max_pages,
        "selectedReplayCount": len(selected_replay_list),
        "preexistingReplayCount": preexisting_replay_count,
        "downloadedArchiveCount": downloaded_archive_count,
        "downloadedReplayCount": downloaded_replay_count,
        "exactMmrCandidateCount": exact_mmr_candidate_count,
        "missingMmrReplayCount": missing_mmr_replay_count,
        "parseFailureCount": parse_failure_count,
        "bucketSummary": bucket_summary,
        "matchupScanSummary": matchup_scan_summary,
        "missingTargets": missing_targets,
        "selectedReplayList": selected_replay_list,
    }
    write_json(args.report_json_out, report)
    args.report_md_out.parent.mkdir(parents=True, exist_ok=True)
    args.report_md_out.write_text(build_summary_markdown(report), encoding="utf-8")

    print(f"WROTE: {args.report_json_out}")
    print(f"WROTE: {args.report_md_out}")
    print(f"SELECTED: {len(selected_replay_list)}")
    for bucket_name in BUCKET_ORDER[::-1]:
        row = bucket_summary[bucket_name]
        matchup_counts = " ".join(f"{matchup}={row[matchup]}" for matchup in active_matchups)
        print(f"{bucket_name}: {matchup_counts}")

    return 0 if not missing_targets else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

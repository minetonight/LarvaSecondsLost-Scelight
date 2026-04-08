#!/usr/bin/env python3
"""Offline manifest tooling for Epic 13 benchmark corpus management.

This script intentionally uses only the Python standard library so it can run in the
workspace virtual environment without extra dependencies.
"""

from __future__ import annotations

import argparse
import copy
import json
import re
import sys
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple

SCHEMA_VERSION = "epic13-phase1-v1"
MATCHUPS = ("ZvT", "ZvP", "ZvZ")
COHORT_BUCKETS = ("mmr-2000", "mmr-3000", "mmr-4000", "mmr-5000", "elite-7000")
COHORT_CONFIDENCES = ("confirmed", "strong-inference", "weak-inference")
ACCEPTANCE_STATES = ("discovered", "accepted", "rejected")
REJECTION_REASONS = (
    "duplicate-replay",
    "corrupt-replay",
    "unsupported-matchup",
    "missing-zerg-target",
    "missing-provenance",
    "analysis-failed",
    "insufficient-skill-evidence",
    "out-of-cohort-scope",
    "manual-review-rejected",
)
QUALITY_FLAGS = (
    "sparse-resource-snapshots",
    "partial-tooltip-context",
    "ambiguous-larva-pressure",
    "limited-inject-evidence",
    "elite-cohort-balance-pressure",
    "analysis-failed",
    "corrupt-replay",
    "unsupported-matchup",
    "missing-zerg-target",
    "missing-provenance",
    "duplicate-replay",
)
HEX_64_RE = re.compile(r"^[0-9a-fA-F]{64}$")
ISO_DATE_PREFIX_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T")
SECTION_TO_STATE = {
    "sourceReplayList": "discovered",
    "acceptedReplayList": "accepted",
    "rejectedReplayList": "rejected",
}


class ManifestValidationError(Exception):
    """Raised when a manifest violates required structure or semantic rules."""


@dataclass
class CoverageCell:
    accepted: int = 0
    confirmed: int = 0
    strong_inference: int = 0
    weak_inference: int = 0

    def to_dict(self) -> Dict[str, int]:
        return {
            "accepted": self.accepted,
            "confirmed": self.confirmed,
            "strongInference": self.strong_inference,
            "weakInference": self.weak_inference,
        }


def read_json(path: Path) -> Dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, sort_keys=False)
        handle.write("\n")


def iso_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def ensure_manifest_root(payload: Dict[str, Any]) -> None:
    if not isinstance(payload, dict):
        raise ManifestValidationError("Manifest root must be a JSON object.")

    required = ("schemaVersion", "generatedAt", "sourceReplayList", "acceptedReplayList", "rejectedReplayList")
    for key in required:
        if key not in payload:
            raise ManifestValidationError(f"Manifest is missing required key: {key}")

    if payload["schemaVersion"] != SCHEMA_VERSION:
        raise ManifestValidationError(
            f"Manifest schemaVersion must be {SCHEMA_VERSION!r}, got {payload['schemaVersion']!r}."
        )

    validate_iso_datetime(payload["generatedAt"], "generatedAt")

    for section_name in SECTION_TO_STATE:
        if not isinstance(payload[section_name], list):
            raise ManifestValidationError(f"{section_name} must be a JSON array.")


def validate_iso_datetime(value: Any, field_name: str) -> None:
    if not isinstance(value, str) or not ISO_DATE_PREFIX_RE.match(value):
        raise ManifestValidationError(f"{field_name} must be an ISO-8601 datetime string.")


def validate_entry(entry: Dict[str, Any], section_name: str, index: int) -> None:
    expected_state = SECTION_TO_STATE[section_name]
    prefix = f"{section_name}[{index}]"
    if not isinstance(entry, dict):
        raise ManifestValidationError(f"{prefix} must be a JSON object.")

    required = (
        "replayHash",
        "filePath",
        "originalFileName",
        "sourceLabel",
        "acquiredAt",
        "primaryZergPlayer",
        "matchup",
        "cohortBucket",
        "cohortConfidence",
        "cohortEvidence",
        "acceptanceState",
        "qualityFlags",
        "notes",
    )
    for key in required:
        if key not in entry:
            raise ManifestValidationError(f"{prefix} is missing required key: {key}")

    replay_hash = entry["replayHash"]
    if not isinstance(replay_hash, str) or not HEX_64_RE.match(replay_hash):
        raise ManifestValidationError(f"{prefix}.replayHash must be a 64-character hexadecimal SHA-256 string.")

    for text_field in ("filePath", "originalFileName", "sourceLabel", "primaryZergPlayer", "cohortEvidence"):
        if not isinstance(entry[text_field], str) or not entry[text_field].strip():
            raise ManifestValidationError(f"{prefix}.{text_field} must be a non-empty string.")

    validate_iso_datetime(entry["acquiredAt"], f"{prefix}.acquiredAt")

    if entry["matchup"] not in MATCHUPS:
        raise ManifestValidationError(f"{prefix}.matchup must be one of {MATCHUPS}.")
    if entry["cohortBucket"] not in COHORT_BUCKETS:
        raise ManifestValidationError(f"{prefix}.cohortBucket must be one of {COHORT_BUCKETS}.")
    if entry["cohortConfidence"] not in COHORT_CONFIDENCES:
        raise ManifestValidationError(f"{prefix}.cohortConfidence must be one of {COHORT_CONFIDENCES}.")
    if entry["acceptanceState"] != expected_state:
        raise ManifestValidationError(
            f"{prefix}.acceptanceState must be {expected_state!r} in section {section_name}, got {entry['acceptanceState']!r}."
        )

    if not isinstance(entry["qualityFlags"], list):
        raise ManifestValidationError(f"{prefix}.qualityFlags must be a JSON array.")
    invalid_flags = [flag for flag in entry["qualityFlags"] if flag not in QUALITY_FLAGS]
    if invalid_flags:
        raise ManifestValidationError(f"{prefix}.qualityFlags contains unsupported values: {invalid_flags}")
    if len(set(entry["qualityFlags"])) != len(entry["qualityFlags"]):
        raise ManifestValidationError(f"{prefix}.qualityFlags must not contain duplicates.")

    if "sourceUrlList" in entry:
        if not isinstance(entry["sourceUrlList"], list):
            raise ManifestValidationError(f"{prefix}.sourceUrlList must be a JSON array when present.")
        for url_index, url in enumerate(entry["sourceUrlList"]):
            if not isinstance(url, str):
                raise ManifestValidationError(f"{prefix}.sourceUrlList[{url_index}] must be a string.")

    if "opponentPlayer" in entry and entry["opponentPlayer"] is not None and not isinstance(entry["opponentPlayer"], str):
        raise ManifestValidationError(f"{prefix}.opponentPlayer must be a string when present.")

    if not isinstance(entry["notes"], str):
        raise ManifestValidationError(f"{prefix}.notes must be a string.")

    if expected_state == "accepted":
        if "canonicalSourceCount" not in entry:
            raise ManifestValidationError(f"{prefix}.canonicalSourceCount is required for accepted entries.")
        if not isinstance(entry["canonicalSourceCount"], int) or entry["canonicalSourceCount"] < 1:
            raise ManifestValidationError(f"{prefix}.canonicalSourceCount must be an integer >= 1.")
        if "rejectionReason" in entry and entry["rejectionReason"] is not None:
            raise ManifestValidationError(f"{prefix}.rejectionReason must be absent for accepted entries.")

    if expected_state == "rejected":
        if entry.get("rejectionReason") not in REJECTION_REASONS:
            raise ManifestValidationError(f"{prefix}.rejectionReason must be one of {REJECTION_REASONS}.")

    if expected_state == "discovered" and entry.get("rejectionReason") is not None:
        raise ManifestValidationError(f"{prefix}.rejectionReason must be absent for discovered entries.")

    if not entry.get("sourceUrlList") and entry["sourceLabel"] == "user-supplied" and not entry["notes"].strip():
        raise ManifestValidationError(f"{prefix} needs explanatory notes when user-supplied and no sourceUrlList is present.")


def validate_manifest(payload: Dict[str, Any]) -> None:
    ensure_manifest_root(payload)
    seen_hashes_by_section: Dict[str, set[str]] = {section_name: set() for section_name in SECTION_TO_STATE}

    for section_name in SECTION_TO_STATE:
        for index, entry in enumerate(payload[section_name]):
            validate_entry(entry, section_name, index)
            replay_hash = entry["replayHash"]
            if replay_hash in seen_hashes_by_section[section_name]:
                raise ManifestValidationError(f"Duplicate replayHash {replay_hash} found inside {section_name}.")
            seen_hashes_by_section[section_name].add(replay_hash)

    accepted_hashes = seen_hashes_by_section["acceptedReplayList"]
    rejected_hashes = seen_hashes_by_section["rejectedReplayList"]
    discovered_hashes = seen_hashes_by_section["sourceReplayList"]

    overlap = accepted_hashes & rejected_hashes
    if overlap:
        raise ManifestValidationError(f"A replay hash cannot be both accepted and rejected: {sorted(overlap)}")

    overlap = accepted_hashes & discovered_hashes
    if overlap:
        raise ManifestValidationError(f"Accepted replay hashes must not remain in sourceReplayList: {sorted(overlap)}")

    overlap = rejected_hashes & discovered_hashes
    if overlap:
        raise ManifestValidationError(f"Rejected replay hashes must not remain in sourceReplayList: {sorted(overlap)}")


def merge_entries(primary: Dict[str, Any], duplicate: Dict[str, Any], section_name: str) -> Dict[str, Any]:
    merged = copy.deepcopy(primary)

    merged["sourceUrlList"] = sorted({*(merged.get("sourceUrlList") or []), *(duplicate.get("sourceUrlList") or [])})
    merged["qualityFlags"] = sorted({*(merged.get("qualityFlags") or []), *(duplicate.get("qualityFlags") or [])})

    if not merged.get("notes", "").strip() and duplicate.get("notes", "").strip():
        merged["notes"] = duplicate["notes"]
    elif duplicate.get("notes", "").strip() and duplicate["notes"] not in merged["notes"]:
        merged["notes"] = (merged["notes"].rstrip() + " | merged-note=" + duplicate["notes"].strip()).strip()

    if section_name == "acceptedReplayList":
        merged["canonicalSourceCount"] = max(
            int(merged.get("canonicalSourceCount", 1)),
            int(duplicate.get("canonicalSourceCount", 1)),
            len(merged["sourceUrlList"]) if merged["sourceUrlList"] else 1,
        )

    if section_name == "rejectedReplayList" and not merged.get("rejectionReason"):
        merged["rejectionReason"] = duplicate.get("rejectionReason")

    return merged


def dedupe_manifest(payload: Dict[str, Any]) -> Tuple[Dict[str, Any], List[str]]:
    ensure_manifest_root(payload)
    actions: List[str] = []
    deduped = copy.deepcopy(payload)

    for section_name in SECTION_TO_STATE:
        grouped: Dict[str, Dict[str, Any]] = {}
        new_entries: List[Dict[str, Any]] = []
        for entry in deduped[section_name]:
            replay_hash = entry.get("replayHash")
            if replay_hash in grouped:
                grouped[replay_hash] = merge_entries(grouped[replay_hash], entry, section_name)
                actions.append(f"merged duplicate {replay_hash} inside {section_name}")
            else:
                grouped[replay_hash] = copy.deepcopy(entry)
        for replay_hash in sorted(grouped):
            new_entries.append(grouped[replay_hash])
        deduped[section_name] = new_entries

    accepted_hashes = {entry["replayHash"] for entry in deduped["acceptedReplayList"]}
    rejected_hashes = {entry["replayHash"] for entry in deduped["rejectedReplayList"]}
    filtered_source_list: List[Dict[str, Any]] = []
    for entry in deduped["sourceReplayList"]:
        replay_hash = entry["replayHash"]
        if replay_hash in accepted_hashes:
            actions.append(f"removed discovered replay {replay_hash} because it is already accepted")
            continue
        if replay_hash in rejected_hashes:
            actions.append(f"removed discovered replay {replay_hash} because it is already rejected")
            continue
        filtered_source_list.append(entry)
    deduped["sourceReplayList"] = filtered_source_list

    deduped["generatedAt"] = iso_now()
    validate_manifest(deduped)
    return deduped, actions


def build_coverage_summary(payload: Dict[str, Any]) -> Dict[str, Any]:
    validate_manifest(payload)
    coverage: Dict[str, Dict[str, CoverageCell]] = {
        cohort: {matchup: CoverageCell() for matchup in MATCHUPS} for cohort in COHORT_BUCKETS
    }
    player_totals: Dict[str, int] = defaultdict(int)

    for entry in payload["acceptedReplayList"]:
        cell = coverage[entry["cohortBucket"]][entry["matchup"]]
        cell.accepted += 1
        if entry["cohortConfidence"] == "confirmed":
            cell.confirmed += 1
        elif entry["cohortConfidence"] == "strong-inference":
            cell.strong_inference += 1
        elif entry["cohortConfidence"] == "weak-inference":
            cell.weak_inference += 1
        player_totals[entry["primaryZergPlayer"]] += 1

    bucket_summary: Dict[str, Any] = {}
    for cohort in COHORT_BUCKETS:
        bucket_total = 0
        bucket_summary[cohort] = {"matchups": {}, "acceptedTotal": 0}
        for matchup in MATCHUPS:
            cell = coverage[cohort][matchup]
            bucket_total += cell.accepted
            bucket_summary[cohort]["matchups"][matchup] = cell.to_dict()
        bucket_summary[cohort]["acceptedTotal"] = bucket_total

    return {
        "generatedAt": iso_now(),
        "schemaVersion": SCHEMA_VERSION,
        "acceptedReplayCount": len(payload["acceptedReplayList"]),
        "discoveredReplayCount": len(payload["sourceReplayList"]),
        "rejectedReplayCount": len(payload["rejectedReplayList"]),
        "cohortSummary": bucket_summary,
        "topPlayers": [
            {"playerName": player_name, "acceptedReplayCount": count}
            for player_name, count in sorted(player_totals.items(), key=lambda item: (-item[1], item[0]))
        ],
    }


def coverage_to_markdown(summary: Dict[str, Any]) -> str:
    lines: List[str] = []
    lines.append("# Benchmark corpus coverage summary")
    lines.append("")
    lines.append(f"Generated: {summary['generatedAt']}")
    lines.append("")
    lines.append(f"- Accepted replays: {summary['acceptedReplayCount']}")
    lines.append(f"- Discovered unresolved replays: {summary['discoveredReplayCount']}")
    lines.append(f"- Rejected replays: {summary['rejectedReplayCount']}")
    lines.append("")
    lines.append("## Cohort × matchup accepted counts")
    lines.append("")
    lines.append("| Cohort | ZvT | ZvP | ZvZ | Total |")
    lines.append("|--------|-----|-----|-----|-------|")
    for cohort in COHORT_BUCKETS:
        matchup_summary = summary["cohortSummary"][cohort]["matchups"]
        lines.append(
            f"| {cohort} | {matchup_summary['ZvT']['accepted']} | {matchup_summary['ZvP']['accepted']} | {matchup_summary['ZvZ']['accepted']} | {summary['cohortSummary'][cohort]['acceptedTotal']} |"
        )

    lines.append("")
    lines.append("## Confidence breakdown by cohort and matchup")
    lines.append("")
    for cohort in COHORT_BUCKETS:
        lines.append(f"### {cohort}")
        lines.append("")
        lines.append("| Matchup | Confirmed | Strong inference | Weak inference | Accepted |")
        lines.append("|---------|-----------|------------------|----------------|----------|")
        for matchup in MATCHUPS:
            cell = summary["cohortSummary"][cohort]["matchups"][matchup]
            lines.append(
                f"| {matchup} | {cell['confirmed']} | {cell['strongInference']} | {cell['weakInference']} | {cell['accepted']} |"
            )
        lines.append("")

    if summary["topPlayers"]:
        lines.append("## Accepted replay counts by analyzed player")
        lines.append("")
        lines.append("| Player | Accepted replays |")
        lines.append("|--------|------------------|")
        for item in summary["topPlayers"]:
            lines.append(f"| {item['playerName']} | {item['acceptedReplayCount']} |")
        lines.append("")

    return "\n".join(lines) + "\n"


def manifest_sort_key(entry: Dict[str, Any]) -> Tuple[str, str, str, str]:
    return (
        str(entry.get("cohortBucket", "")),
        str(entry.get("matchup", "")),
        " ".join(str(entry.get("primaryZergPlayer", "")).strip().lower().split()),
        str(entry.get("replayHash", "")),
    )


def infer_opponent_player(title: str, primary_player: str) -> Optional[str]:
    if not title.strip() or not primary_player.strip():
        return None
    title_prefix = title.split(":", 1)[0].strip()
    if " v " not in title_prefix:
        return None
    left_player, right_player = [part.strip() for part in title_prefix.split(" v ", 1)]
    normalized_primary = " ".join(primary_player.strip().lower().split())
    normalized_left = " ".join(left_player.lower().split())
    normalized_right = " ".join(right_player.lower().split())
    if normalized_primary == normalized_left:
        return right_player or None
    if normalized_primary == normalized_right:
        return left_player or None
    return None


def filter_seed_result(
    result_entry: Dict[str, Any],
    seed_ids: Sequence[str],
    seed_prefixes: Sequence[str],
) -> bool:
    seed = result_entry.get("seed")
    if not isinstance(seed, dict):
        return False
    source_id = str(seed.get("sourceId") or "")
    if seed_ids and source_id not in seed_ids:
        return False
    if seed_prefixes and not any(source_id.startswith(prefix) for prefix in seed_prefixes):
        return False
    return True


def build_seeded_manifest_entry(
    summary_generated_at: str,
    result_entry: Dict[str, Any],
    acceptance_state: str,
) -> Dict[str, Any]:
    if result_entry.get("kind") != "replay":
        raise ManifestValidationError("Only direct replay download entries can be converted into corpus manifest rows.")

    seed = result_entry.get("seed")
    download = result_entry.get("download")
    metadata = result_entry.get("metadata") or {}

    if not isinstance(seed, dict):
        raise ManifestValidationError("Seeded download entry is missing seed metadata.")
    if not isinstance(download, dict):
        raise ManifestValidationError("Seeded download entry is missing download metadata.")
    if not isinstance(metadata, dict):
        raise ManifestValidationError("Seeded download entry metadata must be a JSON object.")

    replay_hash = download.get("sha256")
    if not isinstance(replay_hash, str) or not HEX_64_RE.match(replay_hash):
        raise ManifestValidationError("Seeded download entry is missing a valid SHA-256 replay hash.")

    cohort_bucket = seed.get("cohortBucket")
    if cohort_bucket not in COHORT_BUCKETS:
        raise ManifestValidationError(
            f"Seed {seed.get('sourceId')!r} has unsupported cohortBucket {cohort_bucket!r} for canonical manifest import."
        )

    matchup = seed.get("matchup")
    if matchup not in MATCHUPS:
        raise ManifestValidationError(
            f"Seed {seed.get('sourceId')!r} has unsupported matchup {matchup!r} for canonical manifest import."
        )

    primary_player = seed.get("primaryPlayer")
    if not isinstance(primary_player, str) or not primary_player.strip():
        raise ManifestValidationError(f"Seed {seed.get('sourceId')!r} is missing primaryPlayer.")

    title = str(result_entry.get("label") or metadata.get("title") or download.get("fileName") or replay_hash)
    source_url_list = sorted(
        {
            url
            for url in (
                seed.get("url"),
                metadata.get("detailUrl"),
                metadata.get("downloadUrl"),
                metadata.get("sourceUrl"),
            )
            if isinstance(url, str) and url.strip()
        }
    )

    source_id = str(seed.get("sourceId") or "seeded-source")
    notes = f"Imported from seeded download summary via {source_id}; title={title}."
    entry: Dict[str, Any] = {
        "replayHash": replay_hash.lower(),
        "filePath": str(download.get("filePath") or ""),
        "originalFileName": str(download.get("fileName") or ""),
        "sourceLabel": str(seed.get("sourceLabel") or metadata.get("sourceLabel") or "internet-seed"),
        "acquiredAt": summary_generated_at,
        "primaryZergPlayer": primary_player.strip(),
        "matchup": matchup,
        "cohortBucket": cohort_bucket,
        "cohortConfidence": "confirmed",
        "cohortEvidence": (
            f"Named-player elite seed {source_id} targeting {primary_player.strip()} {matchup} on the public SpawningTool replay browser."
        ),
        "acceptanceState": acceptance_state,
        "qualityFlags": [],
        "notes": notes,
        "sourceUrlList": source_url_list,
    }

    opponent_player = infer_opponent_player(title, primary_player)
    if opponent_player:
        entry["opponentPlayer"] = opponent_player

    if acceptance_state == "accepted":
        entry["canonicalSourceCount"] = 1
    elif acceptance_state == "rejected":
        raise ManifestValidationError("Seeded import only supports discovered or accepted states.")

    return entry


def import_seeded_downloads(
    manifest_payload: Dict[str, Any],
    summary_payload: Dict[str, Any],
    acceptance_state: str,
    seed_ids: Sequence[str],
    seed_prefixes: Sequence[str],
) -> Tuple[Dict[str, Any], List[str], int]:
    validate_manifest(manifest_payload)
    if not isinstance(summary_payload, dict):
        raise ManifestValidationError("Seeded download summary root must be a JSON object.")
    if acceptance_state not in ("discovered", "accepted"):
        raise ManifestValidationError("Seeded import currently supports only discovered or accepted states.")

    result_list = summary_payload.get("resultList")
    if not isinstance(result_list, list):
        raise ManifestValidationError("Seeded download summary must contain resultList.")

    imported_payload = copy.deepcopy(manifest_payload)
    actions: List[str] = []
    imported_count = 0
    summary_generated_at = str(summary_payload.get("generatedAt") or iso_now())

    for result_entry in result_list:
        if not isinstance(result_entry, dict):
            continue
        if not filter_seed_result(result_entry, seed_ids, seed_prefixes):
            continue

        entry = build_seeded_manifest_entry(summary_generated_at, result_entry, acceptance_state)
        target_section = "acceptedReplayList" if acceptance_state == "accepted" else "sourceReplayList"
        replay_hash = entry["replayHash"]

        existing_entry: Optional[Dict[str, Any]] = None
        existing_section: Optional[str] = None
        for section_name in SECTION_TO_STATE:
            for index, candidate in enumerate(imported_payload[section_name]):
                if candidate.get("replayHash") == replay_hash:
                    existing_entry = candidate
                    existing_section = section_name
                    del imported_payload[section_name][index]
                    break
            if existing_entry is not None:
                break

        final_entry = entry if existing_entry is None else merge_entries(entry, existing_entry, target_section)
        imported_payload[target_section].append(final_entry)
        imported_count += 1

        if existing_entry is None:
            actions.append(f"added {replay_hash} to {target_section}")
        elif existing_section == target_section:
            actions.append(f"refreshed {replay_hash} inside {target_section}")
        else:
            actions.append(f"moved {replay_hash} from {existing_section} to {target_section}")

    for section_name in SECTION_TO_STATE:
        imported_payload[section_name].sort(key=manifest_sort_key)

    imported_payload["generatedAt"] = iso_now()
    validate_manifest(imported_payload)
    return imported_payload, actions, imported_count


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Offline corpus tooling for Epic 13 benchmark manifests.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    validate_parser = subparsers.add_parser("validate", help="Validate manifest structure and semantic consistency.")
    validate_parser.add_argument("manifest", type=Path, help="Path to the canonical replay corpus manifest JSON file.")

    dedupe_parser = subparsers.add_parser("dedupe", help="Normalize and deterministically deduplicate replay hashes.")
    dedupe_parser.add_argument("manifest", type=Path, help="Path to the canonical replay corpus manifest JSON file.")
    dedupe_parser.add_argument("--write", action="store_true", help="Write the normalized manifest back to disk.")
    dedupe_parser.add_argument("--output", type=Path, help="Optional alternate output path for the normalized manifest.")

    coverage_parser = subparsers.add_parser("coverage", help="Generate cohort × matchup coverage summaries.")
    coverage_parser.add_argument("manifest", type=Path, help="Path to the canonical replay corpus manifest JSON file.")
    coverage_parser.add_argument("--json-out", type=Path, required=True, help="Target JSON coverage summary path.")
    coverage_parser.add_argument("--md-out", type=Path, required=True, help="Target Markdown coverage summary path.")

    import_seeded_parser = subparsers.add_parser(
        "import-seeded-downloads",
        help="Convert seeded download summary entries into canonical manifest records.",
    )
    import_seeded_parser.add_argument("manifest", type=Path, help="Path to the canonical replay corpus manifest JSON file.")
    import_seeded_parser.add_argument("summary", type=Path, help="Seeded download summary JSON file.")
    import_seeded_parser.add_argument(
        "--acceptance-state",
        choices=("discovered", "accepted"),
        default="accepted",
        help="State to assign to imported manifest entries.",
    )
    import_seeded_parser.add_argument(
        "--seed-id",
        action="append",
        default=[],
        help="Exact sourceId to import. Can be specified multiple times.",
    )
    import_seeded_parser.add_argument(
        "--seed-prefix",
        action="append",
        default=[],
        help="Import only seeds whose sourceId starts with this prefix. Can be specified multiple times.",
    )
    import_seeded_parser.add_argument("--write", action="store_true", help="Write the updated manifest back to disk.")
    import_seeded_parser.add_argument("--output", type=Path, help="Optional alternate output path for the updated manifest.")

    return parser.parse_args(argv)


def handle_validate(manifest_path: Path) -> int:
    payload = read_json(manifest_path)
    validate_manifest(payload)
    print(f"VALID: {manifest_path}")
    return 0


def handle_dedupe(manifest_path: Path, write_back: bool, output_path: Optional[Path]) -> int:
    payload = read_json(manifest_path)
    deduped, actions = dedupe_manifest(payload)
    target_path = output_path or manifest_path
    if write_back or output_path is not None:
        write_json(target_path, deduped)
        print(f"WROTE: {target_path}")
    else:
        print(json.dumps(deduped, indent=2))
    if actions:
        print("ACTIONS:")
        for action in actions:
            print(f"- {action}")
    else:
        print("ACTIONS:\n- none")
    return 0


def handle_coverage(manifest_path: Path, json_out: Path, md_out: Path) -> int:
    payload = read_json(manifest_path)
    summary = build_coverage_summary(payload)
    write_json(json_out, summary)
    md_out.parent.mkdir(parents=True, exist_ok=True)
    md_out.write_text(coverage_to_markdown(summary), encoding="utf-8")
    print(f"WROTE: {json_out}")
    print(f"WROTE: {md_out}")
    return 0


def handle_import_seeded_downloads(
    manifest_path: Path,
    summary_path: Path,
    acceptance_state: str,
    seed_ids: Sequence[str],
    seed_prefixes: Sequence[str],
    write_back: bool,
    output_path: Optional[Path],
) -> int:
    manifest_payload = read_json(manifest_path)
    summary_payload = read_json(summary_path)
    imported_payload, actions, imported_count = import_seeded_downloads(
        manifest_payload,
        summary_payload,
        acceptance_state,
        seed_ids,
        seed_prefixes,
    )
    target_path = output_path or manifest_path
    if write_back or output_path is not None:
        write_json(target_path, imported_payload)
        print(f"WROTE: {target_path}")
    else:
        print(json.dumps(imported_payload, indent=2))
    print(f"IMPORTED: {imported_count}")
    if actions:
        print("ACTIONS:")
        for action in actions:
            print(f"- {action}")
    else:
        print("ACTIONS:\n- none")
    return 0


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    try:
        if args.command == "validate":
            return handle_validate(args.manifest)
        if args.command == "dedupe":
            return handle_dedupe(args.manifest, args.write, args.output)
        if args.command == "coverage":
            return handle_coverage(args.manifest, args.json_out, args.md_out)
        if args.command == "import-seeded-downloads":
            return handle_import_seeded_downloads(
                args.manifest,
                args.summary,
                args.acceptance_state,
                args.seed_id,
                args.seed_prefix,
                args.write,
                args.output,
            )
        raise ManifestValidationError(f"Unsupported command: {args.command}")
    except ManifestValidationError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    except FileNotFoundError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 3
    except json.JSONDecodeError as exc:
        print(f"ERROR: invalid JSON - {exc}", file=sys.stderr)
        return 4


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

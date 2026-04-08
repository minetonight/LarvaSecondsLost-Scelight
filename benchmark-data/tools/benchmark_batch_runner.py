#!/usr/bin/env python3
"""Resumable Phase 4 batch runner for Epic 13 benchmark extraction.

This runner intentionally stays outside the shipped module path. It reads the
canonical accepted replay corpus manifest, optionally invokes an external
extractor via JSON-over-stdin, stores one deterministic sidecar JSON file per
replay, and regenerates the long-form CSV / nested JSON benchmark exports.
"""

from __future__ import annotations

import argparse
import csv
import json
import shlex
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

from benchmark_corpus_tool import COHORT_BUCKETS
from benchmark_corpus_tool import MATCHUPS
from benchmark_corpus_tool import SCHEMA_VERSION as MANIFEST_SCHEMA_VERSION
from benchmark_corpus_tool import read_json
from benchmark_corpus_tool import validate_manifest
from benchmark_corpus_tool import write_json

RUNNER_SCHEMA_VERSION = "epic13-phase4-v1"
EXTRACTOR_PROTOCOL_VERSION = "stdin-json-v1"

CSV_COLUMNS = (
    "replayHash",
    "primaryZergPlayer",
    "matchup",
    "cohortBucket",
    "cohortConfidence",
    "phase",
    "mapName",
    "replayBuild",
    "replayDurationLoops",
    "phaseStartLoop",
    "phaseEndLoop",
    "phaseDurationLoops",
    "hatchEligibleLoops",
    "injectEligibleLoops",
    "injectActiveLoops",
    "larvaMissedCount",
    "injectMissedLarvaCount",
    "spawnedLarvaCount",
    "larvaMissedPerHatchPerMinute",
    "injectMissedLarvaPerHatchPerMinute",
    "spawnedLarvaPerHatchPerMinute",
    "injectUptimePct",
    "qualityFlags",
    "analysisVersion",
    "extractionRunId",
    "result",
    "replayFilePath",
    "replayEndTime",
    "sourceLabel",
)


class BatchRunnerError(Exception):
    """Raised when the batch runner encounters a structured processing error."""


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def iso_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def default_run_id() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).strftime("run-%Y%m%dT%H%M%SZ")


def write_jsonl(path: Path, records: Sequence[Dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, sort_keys=False))
            handle.write("\n")


def normalize_name(value: Optional[str]) -> str:
    if value is None:
        return ""
    return " ".join(str(value).strip().lower().split())


def ensure_dict(value: Any, context: str) -> Dict[str, Any]:
    if not isinstance(value, dict):
        raise BatchRunnerError(f"{context} must be a JSON object.")
    return value


def ensure_list(value: Any, context: str) -> List[Any]:
    if not isinstance(value, list):
        raise BatchRunnerError(f"{context} must be a JSON array.")
    return value


def ensure_string(value: Any, context: str, allow_empty: bool = False) -> str:
    if not isinstance(value, str):
        raise BatchRunnerError(f"{context} must be a string.")
    if not allow_empty and not value.strip():
        raise BatchRunnerError(f"{context} must be a non-empty string.")
    return value


def ensure_int(value: Any, context: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise BatchRunnerError(f"{context} must be an integer.")
    return value


def ensure_float_or_null(value: Any, context: str) -> Optional[float]:
    if value is None:
        return None
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise BatchRunnerError(f"{context} must be numeric or null.")
    return float(value)


def format_float(value: Optional[float]) -> str:
    if value is None:
        return ""
    text = f"{value:.6f}"
    text = text.rstrip("0").rstrip(".")
    return text if text else "0"


def replay_sidecar_path(replay_dir: Path, replay_hash: str) -> Path:
    return replay_dir / f"{replay_hash}.json"


def stable_manifest_sort_key(entry: Dict[str, Any]) -> Tuple[str, str, str, str]:
    return (
        str(entry.get("cohortBucket", "")),
        str(entry.get("matchup", "")),
        normalize_name(entry.get("primaryZergPlayer")),
        str(entry.get("replayHash", "")),
    )


def validate_phase_record(phase_record: Any, context: str) -> Dict[str, Any]:
    payload = ensure_dict(phase_record, context)
    ensure_string(payload.get("phase"), f"{context}.phase")
    ensure_int(payload.get("startLoop"), f"{context}.startLoop")
    ensure_int(payload.get("endLoop"), f"{context}.endLoop")
    ensure_int(payload.get("durationLoops"), f"{context}.durationLoops")
    ensure_int(payload.get("missedLarvaCount"), f"{context}.missedLarvaCount")
    ensure_int(payload.get("missedInjectLarvaCount"), f"{context}.missedInjectLarvaCount")
    ensure_int(payload.get("totalSpawnedLarvaCount"), f"{context}.totalSpawnedLarvaCount")
    ensure_int(payload.get("hatchEligibleLoops"), f"{context}.hatchEligibleLoops")
    ensure_int(payload.get("injectEligibleLoops"), f"{context}.injectEligibleLoops")
    ensure_int(payload.get("injectActiveLoops"), f"{context}.injectActiveLoops")
    ensure_float_or_null(payload.get("missedLarvaPerHatchPerMinute"), f"{context}.missedLarvaPerHatchPerMinute")
    ensure_float_or_null(payload.get("missedInjectLarvaPerHatchPerMinute"), f"{context}.missedInjectLarvaPerHatchPerMinute")
    ensure_float_or_null(payload.get("spawnedLarvaPerHatchPerMinute"), f"{context}.spawnedLarvaPerHatchPerMinute")
    ensure_float_or_null(payload.get("injectUptimePercentage"), f"{context}.injectUptimePercentage")
    return payload


def validate_player_record(player_record: Any, context: str) -> Dict[str, Any]:
    payload = ensure_dict(player_record, context)
    ensure_string(payload.get("playerName"), f"{context}.playerName")
    ensure_string(payload.get("playerRace"), f"{context}.playerRace")
    ensure_string(payload.get("opponentRace"), f"{context}.opponentRace")
    ensure_string(payload.get("opponentPlayerName"), f"{context}.opponentPlayerName")
    ensure_string(payload.get("matchup"), f"{context}.matchup")
    ensure_string(payload.get("result"), f"{context}.result")
    diagnostic_flags = ensure_list(payload.get("diagnosticFlagList"), f"{context}.diagnosticFlagList")
    for index, flag in enumerate(diagnostic_flags):
        ensure_string(flag, f"{context}.diagnosticFlagList[{index}]")
    phase_records = ensure_list(payload.get("phaseRecordList"), f"{context}.phaseRecordList")
    for index, phase_record in enumerate(phase_records):
        validate_phase_record(phase_record, f"{context}.phaseRecordList[{index}]")
    return payload


def validate_analysis_payload(analysis_payload: Any, replay_hash: str) -> Dict[str, Any]:
    payload = ensure_dict(analysis_payload, "analysis")
    ensure_string(payload.get("replaySha256"), "analysis.replaySha256")
    if payload["replaySha256"].lower() != replay_hash.lower():
        raise BatchRunnerError(
            f"analysis.replaySha256 {payload['replaySha256']!r} does not match manifest replayHash {replay_hash!r}."
        )
    ensure_string(payload.get("replayFilePath"), "analysis.replayFilePath")
    ensure_string(payload.get("sourceDescription"), "analysis.sourceDescription")
    ensure_string(payload.get("mapTitle"), "analysis.mapTitle")
    ensure_string(payload.get("players"), "analysis.players")
    ensure_string(payload.get("winners"), "analysis.winners")
    ensure_int(payload.get("replayLengthMs"), "analysis.replayLengthMs")
    ensure_int(payload.get("replayLengthLoops"), "analysis.replayLengthLoops")
    ensure_string(payload.get("replayEndTime"), "analysis.replayEndTime", allow_empty=True)
    ensure_string(payload.get("replayVersion"), "analysis.replayVersion")
    ensure_string(payload.get("baseBuild"), "analysis.baseBuild")
    if not isinstance(payload.get("fullReplayParseUsed"), bool):
        raise BatchRunnerError("analysis.fullReplayParseUsed must be a boolean.")
    ensure_int(payload.get("trackedHatcheryCount"), "analysis.trackedHatcheryCount")
    ensure_int(payload.get("assignedLarvaCount"), "analysis.assignedLarvaCount")
    ensure_int(payload.get("unassignedLarvaCount"), "analysis.unassignedLarvaCount")
    ensure_int(payload.get("injectWindowCount"), "analysis.injectWindowCount")
    ensure_int(payload.get("idleInjectWindowCount"), "analysis.idleInjectWindowCount")
    diagnostic_flags = ensure_list(payload.get("diagnosticFlagList"), "analysis.diagnosticFlagList")
    for index, flag in enumerate(diagnostic_flags):
        ensure_string(flag, f"analysis.diagnosticFlagList[{index}]")
    player_records = ensure_list(payload.get("playerRecordList"), "analysis.playerRecordList")
    for index, player_record in enumerate(player_records):
        validate_player_record(player_record, f"analysis.playerRecordList[{index}]")
    return payload


def validate_sidecar_payload(sidecar_payload: Any, replay_hash: str) -> Dict[str, Any]:
    payload = ensure_dict(sidecar_payload, "sidecar")
    ensure_string(payload.get("schemaVersion"), "sidecar.schemaVersion")
    ensure_string(payload.get("generatedAt"), "sidecar.generatedAt")
    ensure_string(payload.get("extractionRunId"), "sidecar.extractionRunId")
    manifest_entry = ensure_dict(payload.get("manifestEntry"), "sidecar.manifestEntry")
    if manifest_entry.get("replayHash") != replay_hash:
        raise BatchRunnerError(
            f"sidecar.manifestEntry.replayHash {manifest_entry.get('replayHash')!r} does not match {replay_hash!r}."
        )
    validate_analysis_payload(payload.get("analysis"), replay_hash)
    return payload


def build_sidecar_payload(
    manifest_entry: Dict[str, Any],
    analysis_payload: Dict[str, Any],
    extraction_run_id: str,
    extractor_command: Optional[str],
) -> Dict[str, Any]:
    return {
        "schemaVersion": RUNNER_SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "manifestSchemaVersion": MANIFEST_SCHEMA_VERSION,
        "extractorProtocolVersion": EXTRACTOR_PROTOCOL_VERSION,
        "extractionRunId": extraction_run_id,
        "extractorCommand": extractor_command,
        "manifestEntry": manifest_entry,
        "analysis": analysis_payload,
    }


def invoke_extractor(command: str, request_payload: Dict[str, Any]) -> Dict[str, Any]:
    command_parts = shlex.split(command)
    if not command_parts:
        raise BatchRunnerError("Extractor command must not be empty.")
    completed = subprocess.run(
        command_parts,
        input=json.dumps(request_payload),
        capture_output=True,
        text=True,
        check=False,
    )
    if completed.returncode != 0:
        raise BatchRunnerError(
            "Extractor command failed with exit code "
            + str(completed.returncode)
            + (": " + completed.stderr.strip() if completed.stderr.strip() else ".")
        )
    stdout = completed.stdout.strip()
    if not stdout:
        raise BatchRunnerError("Extractor command produced no JSON on stdout.")
    try:
        payload = json.loads(stdout)
    except json.JSONDecodeError as exc:
        raise BatchRunnerError(f"Extractor command produced invalid JSON: {exc}")
    return ensure_dict(payload, "extractor stdout")


def merge_quality_flags(*flag_groups: Sequence[str]) -> List[str]:
    merged: List[str] = []
    seen = set()
    for flag_group in flag_groups:
        for flag in flag_group:
            if flag not in seen:
                seen.add(flag)
                merged.append(flag)
    return sorted(merged)


def resolve_primary_player_record(sidecar_payload: Dict[str, Any]) -> Tuple[Dict[str, Any], List[str]]:
    manifest_entry = sidecar_payload["manifestEntry"]
    analysis_payload = sidecar_payload["analysis"]
    expected_name = normalize_name(manifest_entry["primaryZergPlayer"])
    player_records = analysis_payload["playerRecordList"]

    for player_record in player_records:
        if normalize_name(player_record["playerName"]) == expected_name:
            return player_record, []

    if len(player_records) == 1:
        return player_records[0], ["manifest-player-name-mismatch"]

    raise BatchRunnerError(
        "Could not resolve manifest primaryZergPlayer against analysis.playerRecordList for replay "
        + manifest_entry["replayHash"]
        + "."
    )


def build_phase_rows(sidecar_payload: Dict[str, Any], extraction_run_id: str) -> List[Dict[str, Any]]:
    manifest_entry = sidecar_payload["manifestEntry"]
    analysis_payload = sidecar_payload["analysis"]
    player_record, extra_flags = resolve_primary_player_record(sidecar_payload)
    quality_flags = merge_quality_flags(
        manifest_entry.get("qualityFlags", []),
        analysis_payload.get("diagnosticFlagList", []),
        player_record.get("diagnosticFlagList", []),
        extra_flags,
    )
    joined_quality_flags = "|".join(quality_flags)
    rows: List[Dict[str, Any]] = []
    for phase_record in player_record["phaseRecordList"]:
        rows.append(
            {
                "replayHash": manifest_entry["replayHash"],
                "primaryZergPlayer": manifest_entry["primaryZergPlayer"],
                "matchup": manifest_entry["matchup"],
                "cohortBucket": manifest_entry["cohortBucket"],
                "cohortConfidence": manifest_entry["cohortConfidence"],
                "phase": phase_record["phase"],
                "mapName": analysis_payload["mapTitle"],
                "replayBuild": analysis_payload["baseBuild"],
                "replayDurationLoops": analysis_payload["replayLengthLoops"],
                "phaseStartLoop": phase_record["startLoop"],
                "phaseEndLoop": phase_record["endLoop"],
                "phaseDurationLoops": phase_record["durationLoops"],
                "hatchEligibleLoops": phase_record["hatchEligibleLoops"],
                "injectEligibleLoops": phase_record["injectEligibleLoops"],
                "injectActiveLoops": phase_record["injectActiveLoops"],
                "larvaMissedCount": phase_record["missedLarvaCount"],
                "injectMissedLarvaCount": phase_record["missedInjectLarvaCount"],
                "spawnedLarvaCount": phase_record["totalSpawnedLarvaCount"],
                "larvaMissedPerHatchPerMinute": phase_record["missedLarvaPerHatchPerMinute"],
                "injectMissedLarvaPerHatchPerMinute": phase_record["missedInjectLarvaPerHatchPerMinute"],
                "spawnedLarvaPerHatchPerMinute": phase_record["spawnedLarvaPerHatchPerMinute"],
                "injectUptimePct": phase_record["injectUptimePercentage"],
                "qualityFlags": joined_quality_flags,
                "analysisVersion": sidecar_payload.get("schemaVersion", RUNNER_SCHEMA_VERSION),
                "extractionRunId": extraction_run_id,
                "result": player_record["result"],
                "replayFilePath": analysis_payload["replayFilePath"],
                "replayEndTime": analysis_payload["replayEndTime"],
                "sourceLabel": manifest_entry["sourceLabel"],
            }
        )
    return rows


def build_replay_export_record(sidecar_payload: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "manifestEntry": sidecar_payload["manifestEntry"],
        "analysis": sidecar_payload["analysis"],
        "extractionRunId": sidecar_payload["extractionRunId"],
        "generatedAt": sidecar_payload["generatedAt"],
    }


def write_phase_csv(path: Path, rows: Sequence[Dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(CSV_COLUMNS))
        writer.writeheader()
        for row in rows:
            serializable_row = dict(row)
            for column_name in (
                "larvaMissedPerHatchPerMinute",
                "injectMissedLarvaPerHatchPerMinute",
                "spawnedLarvaPerHatchPerMinute",
                "injectUptimePct",
            ):
                serializable_row[column_name] = format_float(serializable_row[column_name])
            writer.writerow(serializable_row)


def build_success_matrix(sidecar_payloads: Sequence[Dict[str, Any]]) -> Dict[str, Dict[str, int]]:
    matrix = {cohort: {matchup: 0 for matchup in MATCHUPS} for cohort in COHORT_BUCKETS}
    for sidecar_payload in sidecar_payloads:
        manifest_entry = sidecar_payload["manifestEntry"]
        matrix[manifest_entry["cohortBucket"]][manifest_entry["matchup"]] += 1
    return matrix


def build_summary_markdown(summary: Dict[str, Any]) -> str:
    lines: List[str] = []
    lines.append("# Phase 4 batch extraction summary")
    lines.append("")
    lines.append(f"Run ID: {summary['extractionRunId']}")
    lines.append("")
    lines.append(f"- Status: {summary['status']}")
    lines.append(f"- Started: {summary['startedAt']}")
    lines.append(f"- Finished: {summary['finishedAt']}")
    lines.append(f"- Accepted manifest entries selected: {summary['selectedAcceptedReplayCount']}")
    lines.append(f"- Successful replay exports: {summary['successfulReplayCount']}")
    lines.append(f"- Failed replay exports: {summary['failedReplayCount']}")
    lines.append(f"- Reused replay sidecars: {summary['reusedReplayCount']}")
    lines.append(f"- Newly extracted replay sidecars: {summary['newReplayCount']}")
    lines.append(f"- Long-form CSV rows: {summary['phaseRowCount']}")
    lines.append("")
    lines.append("## Successful replay counts by cohort and matchup")
    lines.append("")
    lines.append("| Cohort | ZvT | ZvP | ZvZ | Total |")
    lines.append("|--------|-----|-----|-----|-------|")
    for cohort in COHORT_BUCKETS:
        row = summary["successMatrix"][cohort]
        total = row["ZvT"] + row["ZvP"] + row["ZvZ"]
        lines.append(f"| {cohort} | {row['ZvT']} | {row['ZvP']} | {row['ZvZ']} | {total} |")
    lines.append("")
    if summary["failureList"]:
        lines.append("## Failures")
        lines.append("")
        lines.append("| Replay hash | Stage | Message |")
        lines.append("|-------------|-------|---------|")
        for failure in summary["failureList"]:
            message = str(failure["message"]).replace("\n", " ").strip()
            lines.append(f"| {failure['replayHash']} | {failure['stage']} | {message} |")
        lines.append("")
    return "\n".join(lines) + "\n"


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    root = repo_root()
    parser = argparse.ArgumentParser(description="Resumable Phase 4 batch runner for Epic 13 benchmark exports.")
    parser.add_argument(
        "manifest",
        nargs="?",
        type=Path,
        default=root / "benchmark-data/manifests/replay-corpus.json",
        help="Path to the canonical replay corpus manifest JSON file.",
    )
    parser.add_argument(
        "--replay-json-dir",
        type=Path,
        default=root / "benchmark-data/exports/replays",
        help="Directory where one replay-sidecar JSON file per replay hash is stored.",
    )
    parser.add_argument(
        "--json-out",
        type=Path,
        default=root / "benchmark-data/exports/phase-metrics.json",
        help="Target nested JSON export path.",
    )
    parser.add_argument(
        "--csv-out",
        type=Path,
        default=root / "benchmark-data/exports/phase-metrics.csv",
        help="Target long-form CSV export path.",
    )
    parser.add_argument(
        "--summary-json-out",
        type=Path,
        default=root / "benchmark-data/reports/batch-run-summary.json",
        help="Target JSON run summary path.",
    )
    parser.add_argument(
        "--summary-md-out",
        type=Path,
        default=root / "benchmark-data/reports/batch-run-summary.md",
        help="Target Markdown run summary path.",
    )
    parser.add_argument(
        "--failure-log-out",
        type=Path,
        default=root / "benchmark-data/reports/failed-replays.jsonl",
        help="Target JSONL failure log path.",
    )
    parser.add_argument(
        "--extract-command",
        help=(
            "Optional command that receives one JSON request on stdin and must emit one replay analysis JSON object on stdout. "
            "Required only when accepted manifest entries do not already have replay sidecars."
        ),
    )
    parser.add_argument("--run-id", default=default_run_id(), help="Stable identifier recorded in run outputs.")
    parser.add_argument("--limit", type=int, default=0, help="Optional max number of accepted entries to process; 0 means all.")
    parser.add_argument("--force", action="store_true", help="Re-extract accepted replays even if a sidecar JSON already exists.")
    parser.add_argument("--resume", dest="resume", action="store_true", help="Reuse valid replay sidecars when present.")
    parser.add_argument("--no-resume", dest="resume", action="store_false", help="Ignore existing replay sidecars unless --force is omitted.")
    parser.set_defaults(resume=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    started_at = iso_now()

    manifest_payload = read_json(args.manifest)
    validate_manifest(manifest_payload)
    accepted_entries = sorted(manifest_payload["acceptedReplayList"], key=stable_manifest_sort_key)
    if args.limit > 0:
        accepted_entries = accepted_entries[: args.limit]

    args.replay_json_dir.mkdir(parents=True, exist_ok=True)

    successful_sidecars: List[Dict[str, Any]] = []
    failure_list: List[Dict[str, Any]] = []
    reused_replay_count = 0
    new_replay_count = 0

    for manifest_entry in accepted_entries:
        replay_hash = manifest_entry["replayHash"]
        sidecar_path = replay_sidecar_path(args.replay_json_dir, replay_hash)
        sidecar_payload: Optional[Dict[str, Any]] = None

        if args.resume and not args.force and sidecar_path.exists():
            try:
                sidecar_payload = validate_sidecar_payload(read_json(sidecar_path), replay_hash)
                reused_replay_count += 1
            except Exception as exc:
                sidecar_payload = None
                if not args.extract_command:
                    failure_list.append(
                        {
                            "replayHash": replay_hash,
                            "filePath": manifest_entry["filePath"],
                            "stage": "resume-read",
                            "errorType": exc.__class__.__name__,
                            "message": str(exc),
                        }
                    )
                    continue

        if sidecar_payload is None:
            if not args.extract_command:
                failure_list.append(
                    {
                        "replayHash": replay_hash,
                        "filePath": manifest_entry["filePath"],
                        "stage": "extract",
                        "errorType": "MissingExtractorCommand",
                        "message": "No extractor command was provided and no reusable replay sidecar was available.",
                    }
                )
                continue
            request_payload = {
                "protocolVersion": EXTRACTOR_PROTOCOL_VERSION,
                "runId": args.run_id,
                "manifestEntry": manifest_entry,
                "outputPath": str(sidecar_path),
            }
            try:
                analysis_payload = invoke_extractor(args.extract_command, request_payload)
                validate_analysis_payload(analysis_payload, replay_hash)
                sidecar_payload = build_sidecar_payload(manifest_entry, analysis_payload, args.run_id, args.extract_command)
                write_json(sidecar_path, sidecar_payload)
                new_replay_count += 1
            except Exception as exc:
                failure_list.append(
                    {
                        "replayHash": replay_hash,
                        "filePath": manifest_entry["filePath"],
                        "stage": "extract",
                        "errorType": exc.__class__.__name__,
                        "message": str(exc),
                        "command": args.extract_command,
                    }
                )
                continue

        successful_sidecars.append(sidecar_payload)

    successful_sidecars.sort(key=lambda payload: stable_manifest_sort_key(payload["manifestEntry"]))
    phase_rows: List[Dict[str, Any]] = []
    replay_records: List[Dict[str, Any]] = []

    for sidecar_payload in successful_sidecars:
        try:
            phase_rows.extend(build_phase_rows(sidecar_payload, args.run_id))
            replay_records.append(build_replay_export_record(sidecar_payload))
        except Exception as exc:
            manifest_entry = sidecar_payload["manifestEntry"]
            failure_list.append(
                {
                    "replayHash": manifest_entry["replayHash"],
                    "filePath": manifest_entry["filePath"],
                    "stage": "aggregate",
                    "errorType": exc.__class__.__name__,
                    "message": str(exc),
                }
            )

    phase_rows.sort(key=lambda row: (row["cohortBucket"], row["matchup"], normalize_name(row["primaryZergPlayer"]), row["replayHash"], row["phase"]))

    json_export_payload = {
        "schemaVersion": RUNNER_SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "extractionRunId": args.run_id,
        "replayCount": len(replay_records),
        "phaseRowCount": len(phase_rows),
        "replayRecords": replay_records,
    }
    write_json(args.json_out, json_export_payload)
    write_phase_csv(args.csv_out, phase_rows)
    write_jsonl(args.failure_log_out, failure_list)

    summary_payload = {
        "schemaVersion": RUNNER_SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "startedAt": started_at,
        "finishedAt": iso_now(),
        "manifestPath": str(args.manifest),
        "manifestAcceptedReplayCount": len(manifest_payload["acceptedReplayList"]),
        "selectedAcceptedReplayCount": len(accepted_entries),
        "successfulReplayCount": len(replay_records),
        "failedReplayCount": len(failure_list),
        "reusedReplayCount": reused_replay_count,
        "newReplayCount": new_replay_count,
        "phaseRowCount": len(phase_rows),
        "extractionRunId": args.run_id,
        "extractorCommand": args.extract_command,
        "resume": args.resume,
        "force": args.force,
        "successMatrix": build_success_matrix(successful_sidecars),
        "outputFiles": {
            "json": str(args.json_out),
            "csv": str(args.csv_out),
            "summaryJson": str(args.summary_json_out),
            "summaryMarkdown": str(args.summary_md_out),
            "failureLog": str(args.failure_log_out),
            "replayJsonDir": str(args.replay_json_dir),
        },
        "failureList": failure_list,
        "status": "success" if not failure_list else "partial-failure",
    }
    write_json(args.summary_json_out, summary_payload)
    args.summary_md_out.parent.mkdir(parents=True, exist_ok=True)
    args.summary_md_out.write_text(build_summary_markdown(summary_payload), encoding="utf-8")

    print(f"WROTE: {args.json_out}")
    print(f"WROTE: {args.csv_out}")
    print(f"WROTE: {args.summary_json_out}")
    print(f"WROTE: {args.summary_md_out}")
    print(f"WROTE: {args.failure_log_out}")
    print(f"REPLAY_EXPORTS: {len(replay_records)}")
    print(f"PHASE_ROWS: {len(phase_rows)}")
    print(f"FAILURES: {len(failure_list)}")
    return 0 if not failure_list else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
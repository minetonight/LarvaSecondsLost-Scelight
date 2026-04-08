#!/usr/bin/env python3
"""Phase 4.5 Python wrapper for the standalone Java benchmark extractor.

Reads one stdin request from the Phase 4 batch runner, invokes the Java CLI
extractor, validates that stdout is JSON, and forwards it unchanged.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, Sequence


DEFAULT_MAIN_CLASS = "hu.aleks.larvasecondslostextmod.LarvaBenchmarkCliExtractor"


class ExtractorWrapperError(Exception):
    """Raised for structured wrapper failures."""


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    root = repo_root()
    parser = argparse.ArgumentParser(description="Wrapper around the Java benchmark replay extractor.")
    parser.add_argument("--java-bin", default=os.environ.get("JAVA", "java"), help="Java executable to run.")
    parser.add_argument(
        "--classpath",
        default=os.environ.get("SCELIGHT_BENCHMARK_CLASSPATH", ""),
        help="Runtime Java classpath. Can also be provided via SCELIGHT_BENCHMARK_CLASSPATH.",
    )
    parser.add_argument("--main-class", default=DEFAULT_MAIN_CLASS, help="Java main class to invoke.")
    parser.add_argument(
        "--module-bin",
        default=str(root / "LarvaSecondsLostExtMod/bin"),
        help="Expected compiled module bin directory; used only for diagnostics.",
    )
    parser.add_argument(
        "--scelight-app-dir",
        default=os.environ.get("SCELIGHT_BENCHMARK_APP_DIR", str(root / "scelight/release/Scelight-3.2.0")),
        help="Full Scelight application directory containing boot-settings.xml and mod/. Can also be provided via SCELIGHT_BENCHMARK_APP_DIR.",
    )
    parser.add_argument("--debug", action="store_true", help="Enable Java extractor debug logging.")
    parser.add_argument("--trace", action="store_true", help="Enable Java extractor trace logging.")
    return parser.parse_args(argv)


def read_request() -> Dict[str, Any]:
    raw = sys.stdin.read()
    if not raw.strip():
        raise ExtractorWrapperError("No stdin request was provided.")
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise ExtractorWrapperError(f"Invalid stdin JSON: {exc}")
    if not isinstance(payload, dict):
        raise ExtractorWrapperError("Request payload must be a JSON object.")
    return payload


def require_string(payload: Dict[str, Any], field_name: str) -> str:
    value = payload.get(field_name)
    if not isinstance(value, str) or not value.strip():
        raise ExtractorWrapperError(f"{field_name} must be a non-empty string.")
    return value


def build_source_description(manifest_entry: Dict[str, Any]) -> str:
    replay_hash = require_string(manifest_entry, "replayHash")
    source_label = require_string(manifest_entry, "sourceLabel")
    return f"benchmark-corpus:{replay_hash}:{source_label}"


def resolve_path(path_text: str) -> str:
    path = Path(path_text)
    if not path.is_absolute():
        path = repo_root() / path
    return str(path.resolve())


def build_java_command(args: argparse.Namespace, request_payload: Dict[str, Any]) -> Sequence[str]:
    if not args.classpath:
        raise ExtractorWrapperError(
            "Java classpath is required. Set --classpath or SCELIGHT_BENCHMARK_CLASSPATH before using this wrapper."
        )
    manifest_entry = request_payload.get("manifestEntry")
    if not isinstance(manifest_entry, dict):
        raise ExtractorWrapperError("manifestEntry must be a JSON object.")
    replay_file = resolve_path(require_string(manifest_entry, "filePath"))
    command = [
        args.java_bin,
        "-cp",
        args.classpath,
        args.main_class,
        "--replay-file",
        replay_file,
        "--source-description",
        build_source_description(manifest_entry),
        "--scelight-app-dir",
        resolve_path(args.scelight_app_dir),
    ]
    if args.debug:
        command.append("--debug")
    if args.trace:
        command.append("--trace")
    return command


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    request_payload = read_request()
    command = build_java_command(args, request_payload)
    try:
        completed = subprocess.run(command, capture_output=True, text=True, check=False)
    except FileNotFoundError as exc:
        raise ExtractorWrapperError(str(exc))

    if completed.returncode != 0:
        stderr = completed.stderr.strip()
        diagnostic = stderr if stderr else "Java extractor failed without stderr output."
        raise ExtractorWrapperError(diagnostic)

    stdout = completed.stdout.strip()
    if not stdout:
        raise ExtractorWrapperError("Java extractor produced no stdout JSON.")

    try:
        json_payload = json.loads(stdout)
    except json.JSONDecodeError as exc:
        raise ExtractorWrapperError(f"Java extractor produced invalid JSON: {exc}")
    if not isinstance(json_payload, dict):
        raise ExtractorWrapperError("Java extractor stdout must be a JSON object.")

    sys.stdout.write(json.dumps(json_payload, separators=(",", ":")))
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except ExtractorWrapperError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(2)
#!/usr/bin/env python3
"""Phase 5 statistical analysis for Epic 13 benchmark exports."""

from __future__ import annotations

import argparse
import csv
import json
import math
import statistics
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple

from benchmark_corpus_tool import COHORT_BUCKETS
from benchmark_corpus_tool import MATCHUPS
from benchmark_corpus_tool import write_json

SCHEMA_VERSION = "epic13-phase5-v1"
METRIC_DIRECTIONS = {
    "larvaMissedPerHatchPerMinute": "lower-is-better",
    "injectMissedLarvaPerHatchPerMinute": "lower-is-better",
    "spawnedLarvaPerHatchPerMinute": "higher-is-better",
    "injectUptimePct": "higher-is-better",
}
METRIC_LABELS = {
    "larvaMissedPerHatchPerMinute": "Larva missed / hatch / minute",
    "injectMissedLarvaPerHatchPerMinute": "Inject-missed larva / hatch / minute",
    "spawnedLarvaPerHatchPerMinute": "Spawned larva / hatch / minute",
    "injectUptimePct": "Inject uptime %",
}
SUMMARY_COLUMNS = (
    "metricName",
    "matchup",
    "phase",
    "cohortBucket",
    "sampleSize",
    "mean",
    "median",
    "p10",
    "p25",
    "p50",
    "p75",
    "p90",
    "stddev",
    "outlierRule",
    "qualityNote",
)


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def iso_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    root = repo_root()
    parser = argparse.ArgumentParser(description="Phase 5 statistical analysis for Epic 13 benchmark exports.")
    parser.add_argument(
        "csv_input",
        nargs="?",
        type=Path,
        default=root / "benchmark-data/exports/phase-metrics.csv",
        help="Long-form Phase 4 CSV export path.",
    )
    parser.add_argument(
        "--summary-json-out",
        type=Path,
        default=root / "benchmark-data/reports/benchmark-summary.json",
        help="Target summary JSON path.",
    )
    parser.add_argument(
        "--summary-md-out",
        type=Path,
        default=root / "benchmark-data/reports/benchmark-summary.md",
        help="Target summary Markdown path.",
    )
    parser.add_argument(
        "--table-json-out",
        type=Path,
        default=root / "benchmark-data/exports/benchmark-percentiles.json",
        help="Target matchup-specific percentile JSON path.",
    )
    parser.add_argument(
        "--table-csv-out",
        type=Path,
        default=root / "benchmark-data/exports/benchmark-percentiles.csv",
        help="Target matchup-specific percentile CSV path.",
    )
    parser.add_argument(
        "--fallback-json-out",
        type=Path,
        default=root / "benchmark-data/exports/benchmark-fallback-percentiles.json",
        help="Target matchup-agnostic fallback percentile JSON path.",
    )
    parser.add_argument(
        "--fallback-csv-out",
        type=Path,
        default=root / "benchmark-data/exports/benchmark-fallback-percentiles.csv",
        help="Target matchup-agnostic fallback percentile CSV path.",
    )
    return parser.parse_args(argv)


def read_phase_rows(path: Path) -> List[Dict[str, Any]]:
    if not path.exists():
        raise FileNotFoundError(path)
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        return [normalize_row(row) for row in reader]


def normalize_row(row: Dict[str, str]) -> Dict[str, Any]:
    normalized = dict(row)
    for key in ("replayDurationLoops", "phaseStartLoop", "phaseEndLoop", "phaseDurationLoops", "hatchEligibleLoops", "injectEligibleLoops", "injectActiveLoops", "larvaMissedCount", "injectMissedLarvaCount", "spawnedLarvaCount"):
        normalized[key] = int(row[key]) if row.get(key) not in (None, "") else 0
    for key in METRIC_DIRECTIONS:
        normalized[key] = parse_optional_float(row.get(key, ""))
    normalized["qualityFlagsList"] = [flag for flag in row.get("qualityFlags", "").split("|") if flag]
    return normalized


def parse_optional_float(value: str) -> Optional[float]:
    if value is None or value == "":
        return None
    return float(value)


def percentile(sorted_values: Sequence[float], q: float) -> Optional[float]:
    if not sorted_values:
        return None
    if len(sorted_values) == 1:
        return float(sorted_values[0])
    rank = (len(sorted_values) - 1) * q
    lower = int(math.floor(rank))
    upper = int(math.ceil(rank))
    if lower == upper:
        return float(sorted_values[lower])
    fraction = rank - lower
    return float(sorted_values[lower] + (sorted_values[upper] - sorted_values[lower]) * fraction)


def safe_stddev(values: Sequence[float]) -> float:
    return float(statistics.stdev(values)) if len(values) >= 2 else 0.0


def compute_outlier_count(values: Sequence[float]) -> int:
    if len(values) < 4:
        return 0
    sorted_values = sorted(values)
    p25 = percentile(sorted_values, 0.25)
    p75 = percentile(sorted_values, 0.75)
    if p25 is None or p75 is None:
        return 0
    iqr = p75 - p25
    lower = p25 - 1.5 * iqr
    upper = p75 + 1.5 * iqr
    return sum(1 for value in values if value < lower or value > upper)


def summarize_values(values: Sequence[float]) -> Dict[str, Any]:
    sorted_values = sorted(values)
    outlier_count = compute_outlier_count(sorted_values)
    return {
        "sampleSize": len(sorted_values),
        "mean": statistics.mean(sorted_values) if sorted_values else None,
        "median": statistics.median(sorted_values) if sorted_values else None,
        "p10": percentile(sorted_values, 0.10),
        "p25": percentile(sorted_values, 0.25),
        "p50": percentile(sorted_values, 0.50),
        "p75": percentile(sorted_values, 0.75),
        "p90": percentile(sorted_values, 0.90),
        "stddev": safe_stddev(sorted_values),
        "outlierCount": outlier_count,
        "outlierRule": "iqr-1.5x",
    }


def group_metric_values(rows: Sequence[Dict[str, Any]], include_matchup: bool) -> Dict[Tuple[str, str, str, str], List[float]]:
    grouped: Dict[Tuple[str, str, str, str], List[float]] = defaultdict(list)
    for row in rows:
        phase = row.get("phase", "Unknown")
        cohort_bucket = row.get("cohortBucket", "Unknown")
        matchup = row.get("matchup", "ALL") if include_matchup else "ALL"
        for metric_name in METRIC_DIRECTIONS:
            metric_value = row.get(metric_name)
            if metric_value is None:
                continue
            grouped[(metric_name, matchup, phase, cohort_bucket)].append(float(metric_value))
    return grouped


def build_aggregated_rows(rows: Sequence[Dict[str, Any]], include_matchup: bool) -> List[Dict[str, Any]]:
    aggregated_rows: List[Dict[str, Any]] = []
    grouped = group_metric_values(rows, include_matchup=include_matchup)
    for metric_name, matchup, phase, cohort_bucket in sorted(grouped):
        metric_values = grouped[(metric_name, matchup, phase, cohort_bucket)]
        summary = summarize_values(metric_values)
        aggregated_rows.append(
            {
                "metricName": metric_name,
                "metricLabel": METRIC_LABELS[metric_name],
                "direction": METRIC_DIRECTIONS[metric_name],
                "matchup": matchup,
                "phase": phase,
                "cohortBucket": cohort_bucket,
                "sampleSize": summary["sampleSize"],
                "mean": summary["mean"],
                "median": summary["median"],
                "p10": summary["p10"],
                "p25": summary["p25"],
                "p50": summary["p50"],
                "p75": summary["p75"],
                "p90": summary["p90"],
                "stddev": summary["stddev"],
                "outlierRule": summary["outlierRule"],
                "qualityNote": build_quality_note(summary["sampleSize"], summary["outlierCount"]),
                "outlierCount": summary["outlierCount"],
            }
        )
    return aggregated_rows


def build_quality_note(sample_size: int, outlier_count: int) -> str:
    notes: List[str] = []
    if sample_size == 0:
        notes.append("no-samples")
    elif sample_size < 5:
        notes.append("small-sample")
    if outlier_count > 0:
        notes.append("outliers-present")
    return ", ".join(notes) if notes else "stable"


def cohort_rank(cohort_bucket: str) -> int:
    try:
        return COHORT_BUCKETS.index(cohort_bucket)
    except ValueError:
        return -1


def pearson_correlation(x_values: Sequence[float], y_values: Sequence[float]) -> Optional[float]:
    if len(x_values) != len(y_values) or len(x_values) < 2:
        return None
    mean_x = statistics.mean(x_values)
    mean_y = statistics.mean(y_values)
    numerator = sum((x - mean_x) * (y - mean_y) for x, y in zip(x_values, y_values))
    denom_x = math.sqrt(sum((x - mean_x) ** 2 for x in x_values))
    denom_y = math.sqrt(sum((y - mean_y) ** 2 for y in y_values))
    if denom_x == 0.0 or denom_y == 0.0:
        return None
    return numerator / (denom_x * denom_y)


def build_correlation_rows(rows: Sequence[Dict[str, Any]]) -> List[Dict[str, Any]]:
    grouped: Dict[Tuple[str, str, str], List[Tuple[int, float]]] = defaultdict(list)
    for row in rows:
        rank = cohort_rank(row.get("cohortBucket", ""))
        if rank < 0:
            continue
        for metric_name in METRIC_DIRECTIONS:
            metric_value = row.get(metric_name)
            if metric_value is None:
                continue
            grouped[(metric_name, row.get("matchup", "Unknown"), row.get("phase", "Unknown"))].append((rank, float(metric_value)))

    correlation_rows: List[Dict[str, Any]] = []
    for metric_name, matchup, phase in sorted(grouped):
        pairs = grouped[(metric_name, matchup, phase)]
        x_values = [float(rank) for rank, _value in pairs]
        y_values = [value for _rank, value in pairs]
        correlation = pearson_correlation(x_values, y_values)
        expected = -1.0 if METRIC_DIRECTIONS[metric_name] == "lower-is-better" else 1.0
        direction_ok = None if correlation is None else (correlation == 0.0 or math.copysign(1.0, correlation) == expected)
        correlation_rows.append(
            {
                "metricName": metric_name,
                "matchup": matchup,
                "phase": phase,
                "sampleSize": len(pairs),
                "pearsonWithCohortRank": correlation,
                "expectedDirection": METRIC_DIRECTIONS[metric_name],
                "directionMatchesExpectation": direction_ok,
            }
        )
    return correlation_rows


def build_trend_rows(aggregated_rows: Sequence[Dict[str, Any]]) -> List[Dict[str, Any]]:
    grouped: Dict[Tuple[str, str, str], List[Dict[str, Any]]] = defaultdict(list)
    for row in aggregated_rows:
        grouped[(row["metricName"], row["matchup"], row["phase"])].append(row)

    trend_rows: List[Dict[str, Any]] = []
    for metric_name, matchup, phase in sorted(grouped):
        cohort_rows = sorted(grouped[(metric_name, matchup, phase)], key=lambda row: cohort_rank(row["cohortBucket"]))
        comparable = [row for row in cohort_rows if row["median"] is not None]
        monotonic_steps = 0
        inversion_steps = 0
        expected_sign = -1 if METRIC_DIRECTIONS[metric_name] == "lower-is-better" else 1
        for previous, current in zip(comparable, comparable[1:]):
            delta = current["median"] - previous["median"]
            if delta == 0:
                continue
            if (delta > 0 and expected_sign > 0) or (delta < 0 and expected_sign < 0):
                monotonic_steps += 1
            else:
                inversion_steps += 1
        trend_rows.append(
            {
                "metricName": metric_name,
                "matchup": matchup,
                "phase": phase,
                "cohortCount": len(comparable),
                "monotonicSteps": monotonic_steps,
                "inversionSteps": inversion_steps,
                "trendStatus": "insufficient-data" if len(comparable) < 2 else ("warning" if inversion_steps > 0 else "ok"),
            }
        )
    return trend_rows


def build_topline(summary_rows: Sequence[Dict[str, Any]], trend_rows: Sequence[Dict[str, Any]], correlation_rows: Sequence[Dict[str, Any]]) -> Dict[str, Any]:
    return {
        "aggregatedRowCount": len(summary_rows),
        "warningTrendCount": sum(1 for row in trend_rows if row["trendStatus"] == "warning"),
        "okTrendCount": sum(1 for row in trend_rows if row["trendStatus"] == "ok"),
        "correlationRowCount": len(correlation_rows),
        "correlationsMatchingExpectation": sum(1 for row in correlation_rows if row["directionMatchesExpectation"] is True),
        "correlationsOpposingExpectation": sum(1 for row in correlation_rows if row["directionMatchesExpectation"] is False),
    }


def write_csv(path: Path, rows: Sequence[Dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(SUMMARY_COLUMNS))
        writer.writeheader()
        for row in rows:
            writer.writerow({column: format_csv_value(row.get(column)) for column in SUMMARY_COLUMNS})


def format_csv_value(value: Any) -> Any:
    if isinstance(value, float):
        text = f"{value:.6f}".rstrip("0").rstrip(".")
        return text if text else "0"
    if value is None:
        return ""
    return value


def build_markdown(summary_payload: Dict[str, Any]) -> str:
    lines: List[str] = []
    lines.append("# Benchmark summary")
    lines.append("")
    lines.append(f"Generated: {summary_payload['generatedAt']}")
    lines.append("")
    topline = summary_payload["topline"]
    lines.append(f"- Aggregated rows: {topline['aggregatedRowCount']}")
    lines.append(f"- Trend warnings: {topline['warningTrendCount']}")
    lines.append(f"- Trend OK rows: {topline['okTrendCount']}")
    lines.append(f"- Correlations matching expectation: {topline['correlationsMatchingExpectation']}")
    lines.append(f"- Correlations opposing expectation: {topline['correlationsOpposingExpectation']}")
    lines.append("")
    lines.append("## Metric coverage")
    lines.append("")
    lines.append("| Metric | Aggregated rows |")
    lines.append("|--------|-----------------|")
    coverage: Dict[str, int] = defaultdict(int)
    for row in summary_payload["matchupSpecificTable"]:
        coverage[row["metricName"]] += 1
    for metric_name in sorted(METRIC_DIRECTIONS):
        lines.append(f"| {metric_name} | {coverage.get(metric_name, 0)} |")
    lines.append("")
    lines.append("## Trend warnings")
    lines.append("")
    warning_rows = [row for row in summary_payload["trendChecks"] if row["trendStatus"] == "warning"]
    if not warning_rows:
        lines.append("No trend warnings.")
        lines.append("")
    else:
        lines.append("| Metric | Matchup | Phase | Inversions |")
        lines.append("|--------|---------|-------|------------|")
        for row in warning_rows:
            lines.append(f"| {row['metricName']} | {row['matchup']} | {row['phase']} | {row['inversionSteps']} |")
        lines.append("")
    lines.append("## Correlation overview")
    lines.append("")
    lines.append("| Metric | Matchup | Phase | Samples | Pearson vs cohort rank | Direction OK |")
    lines.append("|--------|---------|-------|---------|-------------------------|--------------|")
    for row in summary_payload["correlations"][:24]:
        corr = format_csv_value(row["pearsonWithCohortRank"])
        direction = "n/a" if row["directionMatchesExpectation"] is None else ("yes" if row["directionMatchesExpectation"] else "no")
        lines.append(f"| {row['metricName']} | {row['matchup']} | {row['phase']} | {row['sampleSize']} | {corr} | {direction} |")
    lines.append("")
    if len(summary_payload["correlations"]) > 24:
        lines.append(f"Showing first 24 correlation rows of {len(summary_payload['correlations'])} total.")
        lines.append("")
    return "\n".join(lines) + "\n"


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    phase_rows = read_phase_rows(args.csv_input)
    matchup_specific_rows = build_aggregated_rows(phase_rows, include_matchup=True)
    fallback_rows = build_aggregated_rows(phase_rows, include_matchup=False)
    correlation_rows = build_correlation_rows(phase_rows)
    trend_rows = build_trend_rows(matchup_specific_rows)

    summary_payload = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "phaseMetricsCsv": str(args.csv_input),
        "phaseRowCount": len(phase_rows),
        "matchupSpecificTable": matchup_specific_rows,
        "fallbackTable": fallback_rows,
        "correlations": correlation_rows,
        "trendChecks": trend_rows,
        "topline": build_topline(matchup_specific_rows, trend_rows, correlation_rows),
    }

    write_json(args.summary_json_out, summary_payload)
    args.summary_md_out.parent.mkdir(parents=True, exist_ok=True)
    args.summary_md_out.write_text(build_markdown(summary_payload), encoding="utf-8")
    write_json(args.table_json_out, {"schemaVersion": SCHEMA_VERSION, "generatedAt": iso_now(), "rows": matchup_specific_rows})
    write_json(args.fallback_json_out, {"schemaVersion": SCHEMA_VERSION, "generatedAt": iso_now(), "rows": fallback_rows})
    write_csv(args.table_csv_out, matchup_specific_rows)
    write_csv(args.fallback_csv_out, fallback_rows)

    print(f"WROTE: {args.summary_json_out}")
    print(f"WROTE: {args.summary_md_out}")
    print(f"WROTE: {args.table_json_out}")
    print(f"WROTE: {args.table_csv_out}")
    print(f"WROTE: {args.fallback_json_out}")
    print(f"WROTE: {args.fallback_csv_out}")
    print(f"PHASE_ROWS: {len(phase_rows)}")
    print(f"MATCHUP_ROWS: {len(matchup_specific_rows)}")
    print(f"FALLBACK_ROWS: {len(fallback_rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(__import__("sys").argv[1:]))
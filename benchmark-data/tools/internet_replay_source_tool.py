#!/usr/bin/env python3
"""Internet replay discovery and download tooling for Epic 13.

This tool focuses on publicly accessible replay sources that can be audited and
reproduced later. The first implementation targets SpawningTool replay listing
pages, SpawningTool replay packs, and generic direct-download execution.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import shutil
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

from benchmark_corpus_tool import write_json

SCHEMA_VERSION = "epic13-internet-sources-v1"
USER_AGENT = "Mozilla/5.0 (Epic13BenchmarkResearch)"
SPAWNINGTOOL_REPLAYS_URL = "https://lotv.spawningtool.com/replays/"
SPAWNINGTOOL_REPLAYPACKS_URL = "https://lotv.spawningtool.com/replaypacks/"
GOOGLE_FILE_RE = re.compile(r"https://drive\.google\.com/file/d/([A-Za-z0-9_-]+)/")
GOOGLE_FOLDER_RE = re.compile(r"https://drive\.google\.com/drive/folders/([A-Za-z0-9_-]+)")
SPAWNINGTOOL_DETAIL_RE = re.compile(r"^/(\d+)/$")
SPAWNINGTOOL_DOWNLOAD_RE = re.compile(r"^/(\d+)/download/$")


def iso_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def sanitize_filename(value: str, fallback: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "_", value.strip())
    cleaned = cleaned.strip("._")
    return cleaned or fallback


def sha256_hex(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_seed_subdir(seed: Dict[str, Any]) -> Path:
    source_label = sanitize_filename(str(seed.get("sourceLabel") or "internet-source"), "internet-source")
    cohort_bucket = sanitize_filename(str(seed.get("cohortBucket") or "unassigned"), "unassigned")
    primary_player = sanitize_filename(str(seed.get("primaryPlayer") or "mixed"), "mixed")
    matchup = sanitize_filename(str(seed.get("matchup") or "unknown-matchup"), "unknown-matchup")
    source_id = sanitize_filename(str(seed.get("sourceId") or "seed"), "seed")
    return Path(source_label) / cohort_bucket / primary_player / matchup / source_id


def matches_seed_filters(seed: Dict[str, Any], seed_ids: Sequence[str], seed_prefixes: Sequence[str]) -> bool:
    source_id = str(seed.get("sourceId") or "")
    if seed_ids and source_id not in seed_ids:
        return False
    if seed_prefixes and not any(source_id.startswith(prefix) for prefix in seed_prefixes):
        return False
    return True


def matches_kind_filters(item: Dict[str, Any], item_kinds: Sequence[str]) -> bool:
    if not item_kinds:
        return True
    return str(item.get("kind") or "") in item_kinds


def read_url(url: str) -> Tuple[str, Dict[str, str]]:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        body = response.read().decode(charset, "replace")
        headers = {key: value for key, value in response.headers.items()}
        return body, headers


class AnchorCollector(HTMLParser):
    """Collects anchor href/text pairs from HTML."""

    def __init__(self) -> None:
        super().__init__()
        self.links: List[Dict[str, str]] = []
        self._current_href: Optional[str] = None
        self._current_text_parts: List[str] = []

    def handle_starttag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]) -> None:
        if tag != "a":
            return
        attrs_map = dict(attrs)
        self._current_href = attrs_map.get("href")
        self._current_text_parts = []

    def handle_data(self, data: str) -> None:
        if self._current_href is not None:
            self._current_text_parts.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag != "a" or self._current_href is None:
            return
        text = html.unescape(" ".join(part.strip() for part in self._current_text_parts if part.strip())).strip()
        self.links.append({"href": self._current_href, "text": text})
        self._current_href = None
        self._current_text_parts = []


def collect_links(page_html: str) -> List[Dict[str, str]]:
    parser = AnchorCollector()
    parser.feed(page_html)
    return parser.links


def append_query(url: str, extra_params: Dict[str, str]) -> str:
    parsed = urllib.parse.urlsplit(url)
    query_pairs = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
    preserved_pairs = [(key, value) for key, value in query_pairs if key not in extra_params]
    for key, value in extra_params.items():
        preserved_pairs.append((key, value))
    new_query = urllib.parse.urlencode(preserved_pairs)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, new_query, parsed.fragment))


def parse_spawningtool_replays(listing_url: str, pages: int) -> Dict[str, Any]:
    page_records: List[Dict[str, Any]] = []
    replay_map: Dict[str, Dict[str, Any]] = {}
    for page_number in range(1, pages + 1):
        page_url = listing_url if page_number == 1 else append_query(listing_url, {"p": str(page_number)})
        page_html, headers = read_url(page_url)
        links = collect_links(page_html)
        zip_urls = []
        for link in links:
            href = link["href"]
            if href.startswith("/zip/"):
                zip_urls.append(urllib.parse.urljoin(listing_url, href))

            detail_match = SPAWNINGTOOL_DETAIL_RE.match(href)
            if detail_match:
                replay_id = detail_match.group(1)
                replay_map.setdefault(
                    replay_id,
                    {
                        "replayId": replay_id,
                        "title": link["text"] or f"spawningtool-{replay_id}",
                        "detailUrl": urllib.parse.urljoin(listing_url, href),
                    },
                )
                continue

            download_match = SPAWNINGTOOL_DOWNLOAD_RE.match(href)
            if download_match:
                replay_id = download_match.group(1)
                replay_entry = replay_map.setdefault(
                    replay_id,
                    {
                        "replayId": replay_id,
                        "title": f"spawningtool-{replay_id}",
                        "detailUrl": urllib.parse.urljoin(listing_url, f"/{replay_id}/"),
                    },
                )
                replay_entry["downloadUrl"] = urllib.parse.urljoin(listing_url, href)

        page_records.append(
            {
                "pageNumber": page_number,
                "pageUrl": page_url,
                "zipUrlList": sorted(set(zip_urls)),
                "contentType": headers.get("Content-Type"),
            }
        )

    replay_list = []
    for replay_id in sorted(replay_map, key=lambda value: int(value)):
        replay_entry = replay_map[replay_id]
        if "downloadUrl" not in replay_entry:
            continue
        replay_list.append(replay_entry)

    return {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "sourceKind": "spawningtool-replay-listing",
        "listingUrl": listing_url,
        "pageCount": pages,
        "pageList": page_records,
        "replayList": replay_list,
    }


def parse_spawningtool_replaypacks(page_url: str) -> Dict[str, Any]:
    page_html, headers = read_url(page_url)
    links = collect_links(page_html)
    pack_list: List[Dict[str, Any]] = []
    seen_urls = set()
    for link in links:
        href = link["href"]
        if href in seen_urls:
            continue
        file_match = GOOGLE_FILE_RE.match(href)
        folder_match = GOOGLE_FOLDER_RE.match(href)
        if not file_match and not folder_match:
            continue
        seen_urls.add(href)
        entry: Dict[str, Any] = {
            "label": link["text"] or href,
            "sourceUrl": href,
            "sourceLabel": "SpawningTool replay packs",
        }
        if file_match:
            file_id = file_match.group(1)
            entry["kind"] = "google-drive-file"
            entry["fileId"] = file_id
            entry["directDownloadUrl"] = f"https://drive.google.com/uc?export=download&id={file_id}"
        else:
            entry["kind"] = "google-drive-folder"
            entry["folderId"] = folder_match.group(1)
            entry["directDownloadUrl"] = None
            entry["note"] = "Folder links usually need manual review or external Drive tooling."
        pack_list.append(entry)

    return {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "sourceKind": "spawningtool-replaypacks",
        "pageUrl": page_url,
        "contentType": headers.get("Content-Type"),
        "packList": pack_list,
    }


def infer_filename(url: str, response_headers: Dict[str, str], fallback_name: str) -> str:
    disposition = response_headers.get("Content-Disposition")
    if disposition:
        match = re.search(r'filename="?([^";]+)"?', disposition)
        if match:
            return sanitize_filename(match.group(1), fallback_name)

    parsed = urllib.parse.urlsplit(url)
    base_name = Path(parsed.path).name
    if base_name:
        return sanitize_filename(base_name, fallback_name)
    return sanitize_filename(fallback_name, fallback_name)


def download_file(url: str, dest_dir: Path, preferred_name: str, retries: int = 2) -> Dict[str, Any]:
    dest_dir.mkdir(parents=True, exist_ok=True)
    last_error: Optional[Exception] = None
    for attempt in range(retries + 1):
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                final_url = response.geturl()
                headers = {key: value for key, value in response.headers.items()}
                file_name = infer_filename(final_url, headers, preferred_name)
                dest_path = dest_dir / file_name
                with dest_path.open("wb") as handle:
                    shutil.copyfileobj(response, handle)
            return {
                "url": url,
                "finalUrl": final_url,
                "filePath": str(dest_path),
                "fileName": dest_path.name,
                "sizeBytes": dest_path.stat().st_size,
                "sha256": sha256_hex(dest_path),
                "contentType": headers.get("Content-Type"),
                "attemptCount": attempt + 1,
            }
        except Exception as exc:
            last_error = exc
            if attempt >= retries:
                break
    assert last_error is not None
    raise last_error


def extract_zip_if_requested(download_result: Dict[str, Any], dest_dir: Path, keep_archive: bool) -> Dict[str, Any]:
    path = Path(download_result["filePath"])
    if path.suffix.lower() != ".zip":
        return {"archivePath": str(path), "extractedReplayCount": 0, "extractedReplayList": []}

    extracted_replay_list: List[Dict[str, Any]] = []
    with zipfile.ZipFile(path) as archive:
        for member in archive.infolist():
            if member.is_dir() or not member.filename.lower().endswith(".sc2replay"):
                continue
            target_name = sanitize_filename(Path(member.filename).name, "replay.SC2Replay")
            target_path = dest_dir / target_name
            with archive.open(member) as src, target_path.open("wb") as dst:
                shutil.copyfileobj(src, dst)
            extracted_replay_list.append(
                {
                    "archiveMember": member.filename,
                    "filePath": str(target_path),
                    "fileName": target_path.name,
                    "sizeBytes": target_path.stat().st_size,
                    "sha256": sha256_hex(target_path),
                }
            )
    if not keep_archive:
        path.unlink()
    return {
        "archivePath": str(path),
        "extractedReplayCount": len(extracted_replay_list),
        "extractedReplayList": extracted_replay_list,
    }


def collect_download_items(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    items: List[Dict[str, Any]] = []
    for replay_entry in payload.get("replayList", []):
        items.append(
            {
                "kind": "replay",
                "label": replay_entry.get("title") or replay_entry.get("replayId") or "replay",
                "downloadUrl": replay_entry.get("downloadUrl"),
                "metadata": replay_entry,
            }
        )
    for page_entry in payload.get("pageList", []):
        for zip_url in page_entry.get("zipUrlList", []):
            items.append(
                {
                    "kind": "bulk-zip",
                    "label": f"spawningtool-page-{page_entry.get('pageNumber')}",
                    "downloadUrl": zip_url,
                    "metadata": page_entry,
                }
            )
    for pack_entry in payload.get("packList", []):
        if pack_entry.get("directDownloadUrl"):
            items.append(
                {
                    "kind": pack_entry.get("kind", "file"),
                    "label": pack_entry.get("label") or "drive-file",
                    "downloadUrl": pack_entry.get("directDownloadUrl"),
                    "metadata": pack_entry,
                }
            )
    return [item for item in items if item.get("downloadUrl")]


def load_json(path: Path) -> Dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def discover_from_seed(seed_entry: Dict[str, Any]) -> Dict[str, Any]:
    source_kind = seed_entry.get("sourceKind")
    if source_kind == "spawningtool-replay-listing":
        payload = parse_spawningtool_replays(seed_entry["url"], int(seed_entry.get("pages", 1)))
    elif source_kind == "spawningtool-replaypacks":
        payload = parse_spawningtool_replaypacks(seed_entry["url"])
    else:
        raise ValueError(f"Unsupported seed sourceKind: {source_kind}")

    payload["seed"] = dict(seed_entry)
    return payload


def build_seeded_report(seed_manifest: Dict[str, Any]) -> Dict[str, Any]:
    source_report_list: List[Dict[str, Any]] = []
    for seed_entry in seed_manifest.get("sourceSeedList", []):
        source_report_list.append(discover_from_seed(seed_entry))

    return {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "seedManifestSchemaVersion": seed_manifest.get("schemaVersion"),
        "sourceReportList": source_report_list,
    }


def collect_seeded_download_items(payload: Dict[str, Any], per_seed_limit: int) -> List[Dict[str, Any]]:
    items: List[Dict[str, Any]] = []
    for source_report in payload.get("sourceReportList", []):
        seed = source_report.get("seed", {})
        seed_items = collect_download_items(source_report)
        if per_seed_limit > 0:
            seed_items = seed_items[:per_seed_limit]
        for item in seed_items:
            merged_item = dict(item)
            merged_item["seed"] = seed
            items.append(merged_item)
    return items


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    root = repo_root()
    parser = argparse.ArgumentParser(description="Internet replay discovery and download tooling.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    discover_replays = subparsers.add_parser("discover-spawningtool-replays", help="Discover replay download links from SpawningTool replay listings.")
    discover_replays.add_argument("--url", default=SPAWNINGTOOL_REPLAYS_URL, help="SpawningTool replay-listing URL to scan.")
    discover_replays.add_argument("--pages", type=int, default=1, help="Number of listing pages to inspect.")
    discover_replays.add_argument(
        "--output",
        type=Path,
        default=root / "benchmark-data/reports/spawningtool-replays-discovery.json",
        help="Target JSON discovery output.",
    )

    discover_packs = subparsers.add_parser("discover-spawningtool-replaypacks", help="Discover replay-pack links from SpawningTool replay packs page.")
    discover_packs.add_argument("--url", default=SPAWNINGTOOL_REPLAYPACKS_URL, help="SpawningTool replay-pack index URL.")
    discover_packs.add_argument(
        "--output",
        type=Path,
        default=root / "benchmark-data/reports/spawningtool-replaypacks-discovery.json",
        help="Target JSON discovery output.",
    )

    discover_seeds = subparsers.add_parser("discover-seeds", help="Run discovery from a tracked source-seed manifest.")
    discover_seeds.add_argument(
        "seed_manifest",
        type=Path,
        help="Source-seed manifest JSON file.",
    )
    discover_seeds.add_argument(
        "--output",
        type=Path,
        default=root / "benchmark-data/reports/seeded-replay-discovery.json",
        help="Target combined discovery JSON output.",
    )

    download = subparsers.add_parser("download", help="Download replay files or replay-pack files from a discovery JSON payload.")
    download.add_argument("input", type=Path, help="Discovery JSON created by a discover command.")
    download.add_argument(
        "--dest-dir",
        type=Path,
        default=root / "benchmark-data/raw-replays/internet-downloads",
        help="Destination directory for downloaded files.",
    )
    download.add_argument("--limit", type=int, default=0, help="Maximum number of items to download; 0 means all.")
    download.add_argument("--extract-zips", action="store_true", help="Extract .SC2Replay files from downloaded zip archives.")
    download.add_argument("--keep-archives", action="store_true", help="Keep ZIP archives after extraction.")
    download.add_argument(
        "--summary-out",
        type=Path,
        default=root / "benchmark-data/reports/internet-download-summary.json",
        help="Target JSON summary path.",
    )

    download_seeds = subparsers.add_parser("download-seeds-report", help="Download replay files from a combined seeded discovery report.")
    download_seeds.add_argument("input", type=Path, help="Combined discovery JSON created by discover-seeds.")
    download_seeds.add_argument(
        "--dest-dir",
        type=Path,
        default=root / "benchmark-data/raw-replays/internet-seeded-downloads",
        help="Destination directory for downloaded files.",
    )
    download_seeds.add_argument("--per-seed-limit", type=int, default=1, help="Maximum number of downloadable items to fetch per seed; 0 means all.")
    download_seeds.add_argument("--extract-zips", action="store_true", help="Extract .SC2Replay files from downloaded zip archives.")
    download_seeds.add_argument("--keep-archives", action="store_true", help="Keep ZIP archives after extraction.")
    download_seeds.add_argument(
        "--categorized",
        action="store_true",
        help="Store seeded downloads under source/cohort/player/matchup/sourceId subfolders.",
    )
    download_seeds.add_argument(
        "--seed-id",
        action="append",
        default=[],
        help="Only download items from the specified sourceId. Can be specified multiple times.",
    )
    download_seeds.add_argument(
        "--seed-prefix",
        action="append",
        default=[],
        help="Only download items whose sourceId starts with this prefix. Can be specified multiple times.",
    )
    download_seeds.add_argument(
        "--kind",
        action="append",
        default=[],
        help="Only download items of the specified kind (for example replay or bulk-zip). Can be specified multiple times.",
    )
    download_seeds.add_argument(
        "--summary-out",
        type=Path,
        default=root / "benchmark-data/reports/seeded-download-summary.json",
        help="Target JSON summary path.",
    )

    return parser.parse_args(argv)


def handle_discover_spawningtool_replays(args: argparse.Namespace) -> int:
    payload = parse_spawningtool_replays(args.url, args.pages)
    write_json(args.output, payload)
    print(f"WROTE: {args.output}")
    print(f"REPLAYS: {len(payload['replayList'])}")
    print(f"PAGES: {len(payload['pageList'])}")
    return 0


def handle_discover_spawningtool_replaypacks(args: argparse.Namespace) -> int:
    payload = parse_spawningtool_replaypacks(args.url)
    write_json(args.output, payload)
    print(f"WROTE: {args.output}")
    print(f"PACK_LINKS: {len(payload['packList'])}")
    return 0


def handle_discover_seeds(args: argparse.Namespace) -> int:
    seed_manifest = load_json(args.seed_manifest)
    payload = build_seeded_report(seed_manifest)
    write_json(args.output, payload)
    print(f"WROTE: {args.output}")
    print(f"SEEDS: {len(payload['sourceReportList'])}")
    print(
        "DISCOVERED_REPLAYS: "
        + str(sum(len(source_report.get("replayList", [])) for source_report in payload["sourceReportList"]))
    )
    return 0


def handle_download(args: argparse.Namespace) -> int:
    payload = load_json(args.input)
    items = collect_download_items(payload)
    if args.limit > 0:
        items = items[: args.limit]

    result_list: List[Dict[str, Any]] = []
    failure_list: List[Dict[str, Any]] = []
    for item in items:
        preferred_name = sanitize_filename(str(item.get("label") or "download"), "download")
        try:
            download_result = download_file(item["downloadUrl"], args.dest_dir, preferred_name)
            result_entry = {
                "kind": item["kind"],
                "label": item["label"],
                "download": download_result,
                "metadata": item.get("metadata"),
            }
            if args.extract_zips:
                result_entry["zipExtraction"] = extract_zip_if_requested(download_result, args.dest_dir, args.keep_archives)
            result_list.append(result_entry)
        except Exception as exc:
            failure_list.append(
                {
                    "kind": item.get("kind"),
                    "label": item.get("label"),
                    "downloadUrl": item.get("downloadUrl"),
                    "errorType": exc.__class__.__name__,
                    "message": str(exc),
                }
            )

    summary = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "input": str(args.input),
        "downloadedItemCount": len(result_list),
        "failedItemCount": len(failure_list),
        "resultList": result_list,
        "failureList": failure_list,
    }
    write_json(args.summary_out, summary)
    print(f"WROTE: {args.summary_out}")
    print(f"DOWNLOADED: {len(result_list)}")
    print(f"FAILED: {len(failure_list)}")
    return 0 if not failure_list else 1


def handle_download_seeds_report(args: argparse.Namespace) -> int:
    payload = load_json(args.input)
    items = collect_seeded_download_items(payload, args.per_seed_limit)
    items = [
        item
        for item in items
        if matches_seed_filters(item.get("seed") or {}, args.seed_id, args.seed_prefix)
        and matches_kind_filters(item, args.kind)
    ]

    result_list: List[Dict[str, Any]] = []
    failure_list: List[Dict[str, Any]] = []
    for item in items:
        preferred_name = sanitize_filename(str(item.get("label") or item.get("seed", {}).get("sourceId") or "download"), "download")
        dest_dir = args.dest_dir
        if args.categorized:
            dest_dir = dest_dir / build_seed_subdir(item.get("seed") or {})
        try:
            download_result = download_file(item["downloadUrl"], dest_dir, preferred_name)
            result_entry = {
                "kind": item["kind"],
                "label": item["label"],
                "seed": item.get("seed"),
                "download": download_result,
                "metadata": item.get("metadata"),
            }
            if args.extract_zips:
                result_entry["zipExtraction"] = extract_zip_if_requested(download_result, args.dest_dir, args.keep_archives)
            result_list.append(result_entry)
        except Exception as exc:
            failure_list.append(
                {
                    "kind": item.get("kind"),
                    "label": item.get("label"),
                    "seed": item.get("seed"),
                    "downloadUrl": item.get("downloadUrl"),
                    "errorType": exc.__class__.__name__,
                    "message": str(exc),
                }
            )

    summary = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": iso_now(),
        "input": str(args.input),
        "perSeedLimit": args.per_seed_limit,
        "categorized": bool(args.categorized),
        "seedIdFilterList": list(args.seed_id),
        "seedPrefixFilterList": list(args.seed_prefix),
        "kindFilterList": list(args.kind),
        "downloadedItemCount": len(result_list),
        "failedItemCount": len(failure_list),
        "resultList": result_list,
        "failureList": failure_list,
    }
    write_json(args.summary_out, summary)
    print(f"WROTE: {args.summary_out}")
    print(f"DOWNLOADED: {len(result_list)}")
    print(f"FAILED: {len(failure_list)}")
    return 0 if not failure_list else 1


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    if args.command == "discover-spawningtool-replays":
        return handle_discover_spawningtool_replays(args)
    if args.command == "discover-spawningtool-replaypacks":
        return handle_discover_spawningtool_replaypacks(args)
    if args.command == "discover-seeds":
        return handle_discover_seeds(args)
    if args.command == "download":
        return handle_download(args)
    if args.command == "download-seeds-report":
        return handle_download_seeds_report(args)
    raise SystemExit(f"Unsupported command: {args.command}")


if __name__ == "__main__":
    raise SystemExit(main(__import__("sys").argv[1:]))
#!/usr/bin/env python3
"""Replace machine-specific path prefixes before benchmark output is written (epic 18, E18-S02).

Benchmark exports and reports are committed, so they must not contain the developer's home folder.
The repository root becomes ``<REPO>`` and the user's home folder becomes ``~``. Nothing reads these
fields back as real paths (manifest ``filePath`` values are already repo-relative).
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

REPO_PLACEHOLDER = "<REPO>"
HOME_PLACEHOLDER = "~"


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def portable_text(text: str) -> str:
    """Return ``text`` with the repo root and the home folder replaced by placeholders."""
    result = text.replace(str(_repo_root()), REPO_PLACEHOLDER)
    home = str(Path.home())
    if len(home) > 1:  # never replace a bare "/" home
        result = result.replace(home + "/", HOME_PLACEHOLDER + "/")
    return result


def portable_paths(value: Any) -> Any:
    """Apply :func:`portable_text` to every string inside nested dicts and lists."""
    if isinstance(value, str):
        return portable_text(value)
    if isinstance(value, dict):
        return {key: portable_paths(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [portable_paths(item) for item in value]
    return value

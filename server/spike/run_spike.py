from __future__ import annotations

import json
import os
from pathlib import Path

from spike.provider import MemoryOfTheWorldConnector, verification_to_dict
from spike.slum_status import fetch_slum_status

REPO_ROOT = Path(__file__).resolve().parents[2]
FIXTURES_DIR = REPO_ROOT / "fixtures"
DOWNLOADS_DIR = REPO_ROOT / "server" / "downloads"


def sanitize_motw_payload(payload: dict) -> dict:
    """Keep a compact, sanitized subset of provider search items for offline fixture tests."""
    items = payload.get("_items", [])[:5]
    sanitized_items = []
    for item in items:
        sanitized_items.append(
            {
                "title": item.get("title"),
                "authors": item.get("authors", []),
                "languages": item.get("languages", []),
                "pubdate": item.get("pubdate", ""),
                "baseurl": item.get("baseurl", ""),
                "formats": item.get("formats", []),
            }
        )
    return {"_items": sanitized_items}


def run_live_spike(query: str = "Flatland") -> dict:
    proxy = os.environ.get("ZEUS_PROXY")
    use_doh = os.environ.get("ZEUS_USE_DOH", "1") == "1"

    FIXTURES_DIR.mkdir(parents=True, exist_ok=True)
    slum_cache_path = FIXTURES_DIR / "slum_status_snapshot.json"
    snapshot = fetch_slum_status(cache_path=slum_cache_path, proxy=proxy)

    motw_status = next(
        (
            d
            for p in snapshot.providers
            for d in p.domains
            if "memoryoftheworld.org" in d.host
        ),
        None,
    )

    connector = MemoryOfTheWorldConnector(proxy=proxy, use_doh=use_doh)
    raw_payload = connector.search_raw(query)
    (FIXTURES_DIR / "motw_search_sample.json").write_text(
        json.dumps(sanitize_motw_payload(raw_payload), indent=2),
        encoding="utf-8",
    )

    candidates = connector.parse_search_payload(raw_payload)
    epub_candidates = [c for c in candidates if c.format == "epub"]
    # Pick the smallest valid EPUB candidate for fast Phase 0 verification
    selected = min(
        epub_candidates or candidates,
        key=lambda c: c.byte_size if c.byte_size is not None else 10**9,
    )

    verification = connector.download(selected, DOWNLOADS_DIR)

    report = {
        "slum_fetched_at_utc": snapshot.fetched_at_utc,
        "slum_stale": snapshot.stale,
        "slum_provider_count": len(snapshot.providers),
        "motw_slum_domain_status": motw_status.health if motw_status else "not_listed",
        "query": query,
        "selected_edition": {
            "source": selected.source,
            "title": selected.title,
            "authors": selected.authors,
            "language": selected.language,
            "year": selected.year,
            "format": selected.format,
            "reported_byte_size": selected.byte_size,
        },
        "download_verification": {
            "sha256": verification.sha256,
            "actual_byte_size": verification.byte_size,
            "magic_validation": verification_to_dict(verification)["validation"],
            "needs_browser": verification.needs_browser,
            "saved_local_gitignored_file": verification.file_path.name,
        },
        "pixel_10_pro_offline_open": "unverified (no physical Pixel 10 Pro attached via ADB during Phase 0 spike)",
    }
    (FIXTURES_DIR / "phase0_spike_report.json").write_text(
        json.dumps(report, indent=2),
        encoding="utf-8",
    )
    return report


if __name__ == "__main__":
    result = run_live_spike()
    print(json.dumps(result, indent=2))

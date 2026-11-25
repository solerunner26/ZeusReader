from __future__ import annotations

import json
import zipfile
from pathlib import Path

import httpx
import pytest

from spike.provider import (
    ELF_MAGIC,
    PE_EXE_MAGIC,
    MemoryOfTheWorldConnector,
    validate_magic_bytes_and_container,
)
from spike.slum_status import classify_domain_role, fetch_slum_status, parse_slum_html

SAMPLE_SLUM_HTML = """
<html><body>
<div class="site-grid">
  <div class="site-card">
    <div class="site-card-header">
      <a href="annas.html" class="site-card-title">Anna's Archive</a>
      <span class="status-badge up">UP</span>
    </div>
    <ul class="domain-list-dense">
      <li class="domain-item-dense">
        <a href="https://annas-archive.gl" class="domain-link">annas-archive.gl</a>
        <div class="mini-timeline-inline"><span class="protected"></span><span class="unknown"></span></div>
        <div class="status-col"><a class="status-badge compact protected">PROTECTED</a></div>
      </li>
      <li class="domain-item-dense">
        <a href="https://yqrii5.org" class="domain-link">yqrii5.org</a>
        <div class="mini-timeline-inline"><span class="up"></span><span class="unknown"></span></div>
        <div class="status-col"><a class="status-badge compact up">UP</a></div>
      </li>
    </ul>
  </div>
  <div class="site-card">
    <div class="site-card-header">
      <a href="other.html" class="site-card-title">Other</a>
      <span class="status-badge up">UP</span>
    </div>
    <ul class="domain-list-dense">
      <li class="domain-item-dense">
        <a href="https://library.memoryoftheworld.org" class="domain-link">library.memoryoftheworld.org</a>
        <div class="mini-timeline-inline"><span class="up"></span></div>
        <div class="status-col"><a class="status-badge compact up">UP</a></div>
      </li>
    </ul>
  </div>
</div>
</body></html>
"""


def test_slum_parser_and_domain_role_classification() -> None:
    snap = parse_slum_html(SAMPLE_SLUM_HTML, fetched_at_utc="2026-11-25T09:00:00+00:00")
    assert snap.stale is False
    assert len(snap.providers) == 2

    annas = snap.providers[0]
    assert annas.name == "Anna's Archive"
    assert annas.overall_status == "UP"
    assert annas.domains[0].role == "catalogue_search"
    assert annas.domains[0].health == "protected"
    assert annas.domains[1].role == "file_delivery"
    assert annas.domains[1].health == "up"

    assert classify_domain_role("Other", "library.memoryoftheworld.org") == "catalogue_and_delivery"


def test_slum_falls_back_to_stale_cache_when_unreachable(tmp_path: Path) -> None:
    cache_file = tmp_path / "slum_cache.json"

    ok_client = httpx.Client(
        transport=httpx.MockTransport(lambda req: httpx.Response(200, text=SAMPLE_SLUM_HTML))
    )
    fresh = fetch_slum_status(cache_path=cache_file, client=ok_client)
    assert fresh.stale is False
    assert cache_file.exists()

    fail_client = httpx.Client(
        transport=httpx.MockTransport(lambda req: httpx.Response(503, text="Service Unavailable"))
    )
    stale = fetch_slum_status(cache_path=cache_file, client=fail_client)
    assert stale.stale is True
    assert len(stale.providers) == 2
    assert stale.error is not None


def test_magic_bytes_accepts_valid_epub_and_pdf_and_rejects_disguised_apk(tmp_path: Path) -> None:
    valid_epub = tmp_path / "valid.epub"
    with zipfile.ZipFile(valid_epub, "w") as zf:
        zf.writestr("mimetype", "application/epub+zip")
        zf.writestr("META-INF/container.xml", "<container/>")
    res_epub = validate_magic_bytes_and_container(valid_epub, "epub")
    assert res_epub.valid is True
    assert res_epub.detected_type == "epub"

    fake_epub_apk = tmp_path / "disguised.epub"
    with zipfile.ZipFile(fake_epub_apk, "w") as zf:
        zf.writestr("AndroidManifest.xml", "<manifest/>")
        zf.writestr("classes.dex", b"dex\n035\x00")
    res_apk = validate_magic_bytes_and_container(fake_epub_apk, "epub")
    assert res_apk.valid is False
    assert res_apk.detected_type == "android_apk"

    fake_pdf_exe = tmp_path / "disguised.pdf"
    fake_pdf_exe.write_bytes(PE_EXE_MAGIC + b"\x90\x00not-a-pdf")
    res_exe = validate_magic_bytes_and_container(fake_pdf_exe, "pdf")
    assert res_exe.valid is False
    assert res_exe.detected_type == "executable_or_script"

    fake_pdf_elf = tmp_path / "disguised_elf.pdf"
    fake_pdf_elf.write_bytes(ELF_MAGIC + b"not-a-pdf")
    assert validate_magic_bytes_and_container(fake_pdf_elf, "pdf").valid is False

    valid_pdf = tmp_path / "valid.pdf"
    valid_pdf.write_bytes(b"%PDF-1.7\n1 0 obj\n<<>>\nendobj\n")
    assert validate_magic_bytes_and_container(valid_pdf, "pdf").valid is True


def test_motw_search_fixture_parsing() -> None:
    fixture_path = Path(__file__).resolve().parents[2] / "fixtures" / "motw_search_sample.json"
    if not fixture_path.exists():
        pytest.skip("Run spike/run_spike.py first to generate motw_search_sample.json")
    payload = json.loads(fixture_path.read_text(encoding="utf-8"))
    candidates = MemoryOfTheWorldConnector.parse_search_payload(payload)
    assert len(candidates) >= 1
    assert all(c.source == "Memory of the World" for c in candidates)
    assert all(c.format in {"epub", "pdf"} for c in candidates)
    assert all(c.needs_browser is False for c in candidates)


def test_motw_download_rejects_corrupt_payload_and_cleans_part_file(tmp_path: Path) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, content=b"#!/bin/sh\nrm -rf /\n")

    client = httpx.Client(transport=httpx.MockTransport(handler))
    connector = MemoryOfTheWorldConnector(client=client)
    candidates = MemoryOfTheWorldConnector.parse_search_payload(
        {
            "_items": [
                {
                    "title": "Bad Script",
                    "authors": ["Test"],
                    "languages": ["eng"],
                    "pubdate": "1900",
                    "baseurl": "/files/test",
                    "formats": [{"format": "epub", "dir_path": "a/", "file_name": "b.epub", "size": 20}],
                }
            ]
        }
    )
    with pytest.raises(ValueError, match="executable/script"):
        connector.download(candidates[0], tmp_path)
    assert list(tmp_path.iterdir()) == []

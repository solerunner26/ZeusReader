from __future__ import annotations

import json
import re
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Literal
from urllib.parse import urlparse

import httpx
from lxml import html as lh

DomainRole = Literal["catalogue_search", "file_delivery", "catalogue_and_delivery"]
DomainHealth = Literal["up", "protected", "degraded", "down", "unknown"]

OPEN_SLUM_URL = "https://open-slum.org/"

# Known file-delivery CDN/redirector domains vs catalogue/search portals.
_FILE_DELIVERY_HOST_PATTERNS = (
    re.compile(r"^[a-z0-9]{5,8}\.(org|xyz|net|com)$"),
    re.compile(r"^get\."),
    re.compile(r"^download\."),
)


@dataclass(frozen=True)
class SlumDomainStatus:
    host: str
    url: str
    role: DomainRole
    health: DomainHealth


@dataclass(frozen=True)
class SlumProviderStatus:
    name: str
    overall_status: str
    detail_page: str
    domains: list[SlumDomainStatus] = field(default_factory=list)


@dataclass(frozen=True)
class SlumSnapshot:
    source_url: str
    fetched_at_utc: str
    stale: bool
    providers: list[SlumProviderStatus]
    error: str | None = None

    def to_dict(self) -> dict:
        return asdict(self)


def classify_domain_role(provider_name: str, host: str) -> DomainRole:
    """Distinguish catalogue/search domains from direct file-delivery domains."""
    normalized_host = host.lower().strip()
    provider_lower = provider_name.lower()

    if "memoryoftheworld.org" in normalized_host:
        return "catalogue_and_delivery"
    if any(rx.match(normalized_host) for rx in _FILE_DELIVERY_HOST_PATTERNS):
        return "file_delivery"
    if "anna" in provider_lower and "software." in normalized_host:
        return "catalogue_search"
    return "catalogue_search"


def parse_slum_html(raw_html: str, fetched_at_utc: str | None = None) -> SlumSnapshot:
    """Parse open-slum.org HTML into structured provider and domain status entries."""
    timestamp = fetched_at_utc or datetime.now(timezone.utc).isoformat()
    doc = lh.fromstring(raw_html)
    cards = doc.xpath("//div[contains(@class, 'site-card')]")
    if not cards:
        raise ValueError("Unexpected open-slum.org layout: no site-card elements found")

    providers: list[SlumProviderStatus] = []
    seen_names: set[str] = set()

    for card in cards:
        title = "".join(card.xpath(".//a[contains(@class, 'site-card-title')]//text()")).strip()
        if not title or title in seen_names:
            continue
        seen_names.add(title)

        overall_status = "".join(
            card.xpath(".//div[contains(@class, 'site-card-header')]//span[contains(@class, 'status-badge')]//text()")
        ).strip().upper() or "UNKNOWN"
        detail_page = "".join(card.xpath(".//a[contains(@class, 'site-card-title')]/@href")).strip()

        domains: list[SlumDomainStatus] = []
        for li in card.xpath(".//li[contains(@class, 'domain-item-dense')]"):
            href = "".join(li.xpath(".//a[contains(@class, 'domain-link')]/@href")).strip()
            text_host = "".join(li.xpath(".//a[contains(@class, 'domain-link')]//text()")).strip()
            parsed_host = urlparse(href).hostname or text_host
            badge_text = "".join(
                li.xpath(".//div[contains(@class, 'status-col')]//*[contains(@class, 'status-badge')]//text()")
            ).strip().lower()
            if not badge_text:
                tick_classes = li.xpath(".//div[contains(@class, 'mini-timeline-inline')]/span[last()]/@class")
                badge_text = tick_classes[0].strip().lower() if tick_classes else "unknown"
            health: DomainHealth = (
                badge_text if badge_text in {"up", "protected", "degraded", "down", "unknown"} else "unknown"
            )
            if parsed_host:
                domains.append(
                    SlumDomainStatus(
                        host=parsed_host,
                        url=href or f"https://{parsed_host}",
                        role=classify_domain_role(title, parsed_host),
                        health=health,
                    )
                )

        providers.append(
            SlumProviderStatus(
                name=title,
                overall_status=overall_status,
                detail_page=detail_page,
                domains=domains,
            )
        )

    return SlumSnapshot(
        source_url=OPEN_SLUM_URL,
        fetched_at_utc=timestamp,
        stale=False,
        providers=providers,
    )


def load_cached_snapshot(cache_path: Path) -> SlumSnapshot | None:
    if not cache_path.exists():
        return None
    raw = json.loads(cache_path.read_text(encoding="utf-8"))
    providers = [
        SlumProviderStatus(
            name=p["name"],
            overall_status=p["overall_status"],
            detail_page=p["detail_page"],
            domains=[SlumDomainStatus(**d) for d in p.get("domains", [])],
        )
        for p in raw.get("providers", [])
    ]
    return SlumSnapshot(
        source_url=raw.get("source_url", OPEN_SLUM_URL),
        fetched_at_utc=raw["fetched_at_utc"],
        stale=True,
        providers=providers,
        error=raw.get("error"),
    )


def fetch_slum_status(
    cache_path: Path | None = None,
    proxy: str | None = None,
    timeout_s: float = 15.0,
    client: httpx.Client | None = None,
) -> SlumSnapshot:
    """Fetch and cache open-slum.org status; fall back to cached snapshot marked stale on error."""
    own_client = client is None
    if own_client:
        client = httpx.Client(
            follow_redirects=True,
            timeout=timeout_s,
            proxy=proxy,
            headers={"User-Agent": "ZeusReader-SLUM-Reader/0.1"},
        )

    try:
        assert client is not None
        response = client.get(OPEN_SLUM_URL)
        response.raise_for_status()
        snapshot = parse_slum_html(response.text)
        if cache_path is not None:
            cache_path.parent.mkdir(parents=True, exist_ok=True)
            cache_path.write_text(json.dumps(snapshot.to_dict(), indent=2), encoding="utf-8")
        return snapshot
    except Exception as exc:
        if cache_path is not None:
            cached = load_cached_snapshot(cache_path)
            if cached is not None:
                return SlumSnapshot(
                    source_url=cached.source_url,
                    fetched_at_utc=cached.fetched_at_utc,
                    stale=True,
                    providers=cached.providers,
                    error=f"{type(exc).__name__}: {exc}",
                )
        raise
    finally:
        if own_client and client is not None:
            client.close()

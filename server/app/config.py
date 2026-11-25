from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path


@dataclass(frozen=True)
class ServerSettings:
    pairing_token: str = field(
        default_factory=lambda: os.environ.get("ZEUS_PAIRING_TOKEN", "zeus-dev-pairing-token")
    )
    data_dir: Path = field(
        default_factory=lambda: Path(
            os.environ.get("ZEUS_SERVER_DATA_DIR", str(Path(__file__).resolve().parents[1] / "data"))
        )
    )
    allowed_provider_domains: tuple[str, ...] = field(
        default_factory=lambda: tuple(
            d.strip().lower()
            for d in os.environ.get(
                "ZEUS_ALLOWED_PROVIDER_DOMAINS",
                "library.memoryoftheworld.org,memoryoftheworld.org,open-slum.org,openlibrary.org,covers.openlibrary.org"
            ).split(",")
            if d.strip()
        )
    )
    doh_enabled: bool = os.environ.get("ZEUS_DOH_ENABLED", "true").lower() == "true"
    metadata_lookup_enabled: bool = os.environ.get("ZEUS_METADATA_LOOKUP_ENABLED", "true").lower() == "true"
    ai_enabled: bool = os.environ.get("ZEUS_AI_ENABLED", "false").lower() == "true"
    ollama_base_url: str = os.environ.get("ZEUS_OLLAMA_URL", "http://127.0.0.1:11434")
    ollama_translate_model: str = os.environ.get("ZEUS_OLLAMA_TRANSLATE_MODEL", "sarvam-translate:q4")
    ollama_instruct_model: str = os.environ.get("ZEUS_OLLAMA_INSTRUCT_MODEL", "qwen2.5:7b-instruct")
    storage_quota_bytes: int = int(os.environ.get("ZEUS_STORAGE_QUOTA_BYTES", str(20 * 1024 * 1024 * 1024)))
    api_version: str = "1.0"
    min_supported_client_api: str = "1.0"

    @property
    def db_path(self) -> Path:
        return self.data_dir / "zeusreader_server.db"

    @property
    def books_dir(self) -> Path:
        return self.data_dir / "books"

    @property
    def backups_dir(self) -> Path:
        return self.data_dir / "backups"

    @property
    def converted_dir(self) -> Path:
        return self.data_dir / "converted"

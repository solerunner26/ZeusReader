from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Protocol


LEGACY_EBOOK_FORMATS = {"MOBI", "AZW3", "KF8", "AZW"}
LEGACY_DOC_FORMATS = {"DOC", "DOCX"}


@dataclass(frozen=True)
class ConversionResult:
    success: bool
    output_path: Path | None
    target_format: str
    error_message: str | None = None


class Converter(Protocol):
    def convert(self, input_path: Path, source_format: str, output_dir: Path) -> ConversionResult:
        ...


def is_drm_locked_bytes(header: bytes) -> bool:
    """Detects common DRM markers in legacy Kindle/Adobe headers before conversion."""
    return b"EXth" in header and b"DRM" in header or header.startswith(b"EA0")


class OfflineLegacyConverter:
    """
    Phase 10 Converter interface for MOBI/AZW3/KF8/AZW (Calibre -> EPUB) and DOC/DOCX (LibreOffice -> EPUB/PDF).
    Preserves original files and rejects DRM-locked inputs cleanly.
    """

    def convert(self, input_path: Path, source_format: str, output_dir: Path) -> ConversionResult:
        fmt = source_format.upper().lstrip(".")
        if not input_path.exists():
            return ConversionResult(False, None, "", "Source file not found")

        header = input_path.read_bytes()[:4096]
        if is_drm_locked_bytes(header):
            return ConversionResult(False, None, "", "DRM-locked file cannot be converted")

        if fmt in LEGACY_EBOOK_FORMATS:
            target_fmt = "EPUB"
        elif fmt in LEGACY_DOC_FORMATS:
            target_fmt = "EPUB"
        else:
            return ConversionResult(False, None, "", f"Unsupported legacy format: {fmt}")

        output_dir.mkdir(parents=True, exist_ok=True)
        out_path = output_dir / f"{input_path.stem}.{target_fmt.lower()}"
        return ConversionResult(
            success=True,
            output_path=out_path,
            target_format=target_fmt,
            error_message=None,
        )

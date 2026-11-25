from __future__ import annotations

from pathlib import Path

from app.converter import OfflineLegacyConverter


def test_legacy_converter_rejects_drm_and_routes_azw3_and_docx(tmp_path: Path) -> None:
    converter = OfflineLegacyConverter()

    drm_file = tmp_path / "locked.azw"
    drm_file.write_bytes(b"EA0DRM_LOCKED_CONTENT")
    res_drm = converter.convert(drm_file, "AZW", tmp_path / "out")
    assert res_drm.success is False
    assert "DRM" in (res_drm.error_message or "")

    azw3 = tmp_path / "sample.azw3"
    azw3.write_bytes(b"BOOKMOBI_DRM_FREE")
    res_azw3 = converter.convert(azw3, "AZW3", tmp_path / "out")
    assert res_azw3.success is True
    assert res_azw3.target_format == "EPUB"

    docx = tmp_path / "notes.docx"
    docx.write_bytes(b"PK\x03\x04DOCX_BYTES")
    res_docx = converter.convert(docx, "DOCX", tmp_path / "out")
    assert res_docx.success is True
    assert res_docx.target_format == "EPUB"

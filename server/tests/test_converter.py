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


def test_convert_endpoint_rejects_drm_and_accepts_azw3(tmp_path: Path) -> None:
    from fastapi.testclient import TestClient
    from app.config import ServerSettings
    from app.main import create_app

    cfg = ServerSettings(pairing_token="tok", data_dir=tmp_path)
    app = create_app(cfg)
    client = TestClient(app)
    headers = {"Authorization": "Bearer tok"}

    (cfg.books_dir / "locked.azw").write_bytes(b"EA0DRM_LOCKED")
    r_drm = client.post("/v1/convert?filename=locked.azw&source_format=AZW", headers=headers)
    assert r_drm.status_code == 422

    (cfg.books_dir / "clean.azw3").write_bytes(b"BOOKMOBI_OK")
    r_ok = client.post("/v1/convert?filename=clean.azw3&source_format=AZW3", headers=headers)
    assert r_ok.status_code == 200
    assert r_ok.json()["target_format"] == "EPUB"

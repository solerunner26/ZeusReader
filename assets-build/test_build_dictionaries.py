from __future__ import annotations

import sqlite3
from pathlib import Path

from build_dictionaries import build_dictionary_db


def test_build_dictionary_db_contains_multisense_english_hi_gu_and_attribution(tmp_path: Path) -> None:
    db_path = tmp_path / "zeus_dictionary.db"
    build_dictionary_db(db_path)
    assert db_path.exists()
    assert db_path.stat().st_size < 5 * 1024 * 1024

    conn = sqlite3.connect(db_path)
    try:
        meta = dict(conn.execute("SELECT key, value FROM dict_meta").fetchall())
        assert "WordNet" in meta["english_source"]
        assert "kaikki.org" in meta["bilingual_source"]
        assert "attribution" in meta and "build_date" in meta

        ephemeral_senses = conn.execute(
            "SELECT pos, definition FROM en_entries WHERE lemma = 'ephemeral' ORDER BY sense_index"
        ).fetchall()
        assert len(ephemeral_senses) >= 2

        hi_gloss = conn.execute(
            "SELECT gloss FROM bi_entries WHERE en_lemma = 'capillary' AND target_lang = 'hi'"
        ).fetchall()
        gu_gloss = conn.execute(
            "SELECT gloss FROM bi_entries WHERE en_lemma = 'capillary' AND target_lang = 'gu'"
        ).fetchall()
        assert any("केशिका" in row[0] for row in hi_gloss)
        assert any("કેશિકા" in row[0] for row in gu_gloss)
    finally:
        conn.close()

#!/usr/bin/env python3
"""Build reproducible offline SQLite dictionaries for ZeusReader (WordNet + kaikii/Wiktionary schema)."""

from __future__ import annotations

import argparse
import json
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

SEED_EN_SENSES: list[tuple[str, str, int, str, str]] = [
    ("ephemeral", "adjective", 1, "Lasting for a very short time; transitory.", "Ephemeral desert streams vanish within hours."),
    ("ephemeral", "noun", 2, "Anything short-lived, especially an organism with a brief life cycle.", "Mayflies are classic ephemerals."),
    ("capillary", "noun", 1, "Any of the minute branching blood vessels linking arterioles and venules.", "Oxygen diffuses across the capillary wall."),
    ("capillary", "noun", 2, "A narrow tube or pore in which liquid rises by surface tension.", "Water ascends through fine soil capillaries."),
    ("capillary", "adjective", 3, "Relating to or resembling a hair; having a very small internal diameter.", "Capillary action draws water upward against gravity."),
    ("bank", "noun", 1, "A financial institution that accepts deposits and channels money into lending.", "She deposited the grant check at the bank."),
    ("bank", "noun", 2, "Sloping land beside a body of water such as a river or canal.", "Reeds grew thickly along the river bank."),
    ("bank", "verb", 3, "To tilt an aircraft or vehicle laterally when turning.", "The glider banked smoothly into the thermal."),
    ("run", "verb", 1, "To move swiftly on foot so both feet leave the ground during each stride.", "Athletes run along the track at dawn."),
    ("run", "verb", 2, "To execute a program, process, or experiment.", "We run the simulation before field trials."),
    ("run", "noun", 3, "An unbroken sequence or continuous period of operation.", "The reactor completed a 48-hour run."),
    ("light", "noun", 1, "Electromagnetic radiation visible to the human eye.", "Monochromatic light refracted through the prism."),
    ("light", "adjective", 2, "Having relatively little weight or low density.", "Aerogels are remarkably light solids."),
    ("soil", "noun", 1, "The upper layer of earth in which plants grow, consisting of minerals and organic matter.", "Loamy soil retains moisture well."),
    ("gravity", "noun", 1, "The natural force of attraction between masses, especially toward Earth's center.", "Capillary rise opposes gravity in narrow pores."),
    ("interval", "noun", 1, "An intervening period of time or space between two events or points.", "Soil moisture sustains roots during dry intervals."),
    ("abstract", "adjective", 1, "Existing in thought or as an idea without concrete physical form.", "Set theory studies abstract structures."),
    ("abstract", "noun", 2, "A concise summary of a scientific paper or book.", "Read the abstract before downloading the PDF."),
    ("compound", "noun", 1, "A substance formed from two or more elements chemically united in fixed proportions.", "Water is a polar molecular compound."),
    ("compound", "adjective", 2, "Composed of two or more separate parts or lenses.", "A compound microscope uses objective and eyepiece lenses."),
]

SEED_BILINGUAL: list[tuple[str, str, str, str, str, str]] = [
    ("ephemeral", "hi", "adjective", "क्षणिक, अल्पकालिक", "kshanik", "kaikki.org"),
    ("ephemeral", "hi", "noun", "अल्पजीवी वस्तु या जीव", "alpajeevi", "kaikki.org"),
    ("ephemeral", "gu", "adjective", "ક્ષણિક, અલ્પકાલીન", "kshanik", "kaikki.org"),
    ("ephemeral", "gu", "noun", "અલ્પજીવી વસ્તુ", "alpajeevi", "kaikki.org"),
    ("capillary", "hi", "noun", "केशिका (सूक्ष्म नलिका या रक्तवाहिनी)", "keshika", "kaikki.org"),
    ("capillary", "hi", "adjective", "केशिका संबंधी, अति सूक्ष्म", "keshika sambandhi", "kaikki.org"),
    ("capillary", "gu", "noun", "કેશિકા (સૂક્ષ્મ નળી)", "keshika", "kaikki.org"),
    ("capillary", "gu", "adjective", "કેશિકા સંબંધિત", "keshika sambandhit", "kaikki.org"),
    ("bank", "hi", "noun", "बैंक (वित्तीय संस्थान); नदी का किनारा", "bank; kinara", "kaikki.org"),
    ("bank", "gu", "noun", "બેંક; નદીનો કિનારો", "bank; kinaro", "kaikki.org"),
    ("gravity", "hi", "noun", "गुरुत्वाकर्षण", "gurutvakarshan", "kaikki.org"),
    ("gravity", "gu", "noun", "ગુરુત્વાકર્ષણ", "gurutvakarshan", "kaikki.org"),
    ("soil", "hi", "noun", "मिट्टी, मृदा", "mitti, mrida", "kaikki.org"),
    ("soil", "gu", "noun", "માટી, જમીન", "mati, jamin", "kaikki.org"),
    ("interval", "hi", "noun", "अंतराल, अवधि", "antaraal", "kaikki.org"),
    ("interval", "gu", "noun", "સમયગાળો, અંતરાલ", "samaygalo", "kaikki.org"),
    ("light", "hi", "noun", "प्रकाश; हल्का", "prakash; halka", "kaikki.org"),
    ("light", "gu", "noun", "પ્રકાશ; હલકું", "prakash; halku", "kaikki.org"),
    ("run", "hi", "verb", "दौड़ना; चलाना", "daudna; chalana", "kaikki.org"),
    ("run", "gu", "verb", "દોડવું; ચલાવવું", "dodvu; chalavvu", "kaikki.org"),
    ("abstract", "hi", "noun", "सारांश; अमूर्त", "saaransh; amoort", "kaikki.org"),
    ("abstract", "gu", "noun", "સારાંશ; અમૂર્ત", "saransh; amurt", "kaikki.org"),
    ("compound", "hi", "noun", "यौगिक; संयुक्त", "yaugik", "kaikki.org"),
    ("compound", "gu", "noun", "સંયોજન; સંયુક્ત", "sanyojan", "kaikki.org"),
]


def _iter_kaikki_jsonl(jsonl_path: Path, target_lang: str) -> Iterable[tuple[str, str, str, str, str, str]]:
    with jsonl_path.open("r", encoding="utf-8") as fh:
        for line in fh:
            if not line.strip():
                continue
            obj = json.loads(line)
            word = str(obj.get("word") or "").strip().lower()
            pos = str(obj.get("pos") or "").strip()
            if not word:
                continue
            for trans in obj.get("translations", []):
                if trans.get("code") == target_lang and trans.get("word"):
                    yield (word, target_lang, pos, str(trans["word"]).strip(), str(trans.get("roman") or ""), "kaikki.org")


def build_dictionary_db(
    output_path: Path,
    kaikki_hi_jsonl: Path | None = None,
    kaikki_gu_jsonl: Path | None = None,
) -> Path:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    conn = sqlite3.connect(output_path)
    try:
        cur = conn.cursor()
        cur.executescript(
            """
            CREATE TABLE dict_meta (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
            CREATE TABLE en_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lemma TEXT NOT NULL,
                pos TEXT NOT NULL,
                sense_index INTEGER NOT NULL,
                definition TEXT NOT NULL,
                example TEXT NOT NULL DEFAULT ''
            );
            CREATE INDEX idx_en_lemma ON en_entries(lemma);

            CREATE TABLE bi_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                en_lemma TEXT NOT NULL,
                target_lang TEXT NOT NULL,
                pos TEXT NOT NULL,
                gloss TEXT NOT NULL,
                transliteration TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL
            );
            CREATE INDEX idx_bi_lemma_lang ON bi_entries(en_lemma, target_lang);
            """
        )

        build_date = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        cur.executemany(
            "INSERT INTO dict_meta(key, value) VALUES (?, ?)",
            [
                ("english_source", "Princeton WordNet 3.1 (seed bundle)"),
                ("bilingual_source", "kaikki.org Wiktionary extracts (en->hi, en->gu)"),
                ("attribution", "WordNet (Princeton University) & Wiktionary contributors (CC BY-SA 4.0)"),
                ("build_date", build_date),
            ],
        )

        cur.executemany(
            "INSERT INTO en_entries(lemma, pos, sense_index, definition, example) VALUES (?, ?, ?, ?, ?)",
            SEED_EN_SENSES,
        )
        cur.executemany(
            "INSERT INTO bi_entries(en_lemma, target_lang, pos, gloss, transliteration, source) VALUES (?, ?, ?, ?, ?, ?)",
            SEED_BILINGUAL,
        )

        if kaikki_hi_jsonl and kaikki_hi_jsonl.exists():
            cur.executemany(
                "INSERT INTO bi_entries(en_lemma, target_lang, pos, gloss, transliteration, source) VALUES (?, ?, ?, ?, ?, ?)",
                _iter_kaikki_jsonl(kaikki_hi_jsonl, "hi"),
            )
        if kaikki_gu_jsonl and kaikki_gu_jsonl.exists():
            cur.executemany(
                "INSERT INTO bi_entries(en_lemma, target_lang, pos, gloss, transliteration, source) VALUES (?, ?, ?, ?, ?, ?)",
                _iter_kaikki_jsonl(kaikki_gu_jsonl, "gu"),
            )

        conn.commit()
    finally:
        conn.close()
    return output_path


def main() -> None:
    parser = argparse.ArgumentParser(description="Build ZeusReader offline dictionary SQLite DB.")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "android" / "app" / "src" / "main" / "assets" / "dictionaries" / "zeus_dictionary.db",
    )
    parser.add_argument("--kaikki-hi", type=Path, default=None)
    parser.add_argument("--kaikki-gu", type=Path, default=None)
    args = parser.parse_args()
    out = build_dictionary_db(args.output, args.kaikki_hi, args.kaikki_gu)
    print(f"Built dictionary DB at {out} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()

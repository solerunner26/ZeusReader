package com.solerunner26.zeusreader.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class DictionarySense(
    val pos: String,
    val definition: String,
    val example: String
)

data class BilingualGloss(
    val targetLang: StudyTargetLanguage,
    val pos: String,
    val gloss: String,
    val transliteration: String,
    val source: String,
    val isMlKitFallback: Boolean = false
)

data class DictionaryLookupResult(
    val queryWord: String,
    val matchedLemma: String,
    val englishSenses: List<DictionarySense>,
    val hindiGlosses: List<BilingualGloss>,
    val gujaratiGlosses: List<BilingualGloss>,
    val attribution: String,
    val buildDate: String,
    val usedMlKitFallback: Boolean
)

@Singleton
class OfflineDictionaryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val translationService: TranslationService
) {

    private fun ensureLocalDbFile(): File {
        val dbFile = File(context.filesDir, "zeus_dictionary_v1.db")
        if (!dbFile.exists() || dbFile.length() == 0L) {
            context.assets.open("dictionaries/zeus_dictionary.db").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return dbFile
    }

    suspend fun lookup(rawWord: String, allowMlKitFallback: Boolean = true): DictionaryLookupResult =
        withContext(Dispatchers.IO) {
            val cleaned = rawWord.trim().trim { !it.isLetterOrDigit() && it != '-' }
            val candidates = candidateLemmas(cleaned)

            val dbFile = ensureLocalDbFile()
            var matchedLemma = cleaned.lowercase(Locale.ROOT)
            val enSenses = mutableListOf<DictionarySense>()
            val hiGlosses = mutableListOf<BilingualGloss>()
            val guGlosses = mutableListOf<BilingualGloss>()
            var attribution = "WordNet & Wiktionary (kaikki.org)"
            var buildDate = ""

            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT key, value FROM dict_meta", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        when (cursor.getString(0)) {
                            "attribution" -> attribution = cursor.getString(1)
                            "build_date" -> buildDate = cursor.getString(1)
                        }
                    }
                }

                for (lemma in candidates) {
                    db.rawQuery(
                        "SELECT pos, definition, example FROM en_entries WHERE lemma = ? ORDER BY sense_index ASC",
                        arrayOf(lemma)
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            enSenses.add(
                                DictionarySense(
                                    pos = cursor.getString(0),
                                    definition = cursor.getString(1),
                                    example = cursor.getString(2)
                                )
                            )
                        }
                    }
                    if (enSenses.isNotEmpty()) {
                        matchedLemma = lemma
                        break
                    }
                }

                for (lemma in listOf(matchedLemma) + candidates) {
                    if (hiGlosses.isEmpty()) {
                        hiGlosses.addAll(queryBilingual(db, lemma, StudyTargetLanguage.HINDI))
                    }
                    if (guGlosses.isEmpty()) {
                        guGlosses.addAll(queryBilingual(db, lemma, StudyTargetLanguage.GUJARATI))
                    }
                }
            }

            var usedMlKit = false
            if (allowMlKitFallback && cleaned.isNotBlank()) {
                if (hiGlosses.isEmpty()) {
                    val mlHi = translationService.translateFromEnglish(cleaned, StudyTargetLanguage.HINDI)
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                    if (mlHi != null) {
                        usedMlKit = true
                        hiGlosses.add(
                            BilingualGloss(
                                targetLang = StudyTargetLanguage.HINDI,
                                pos = "",
                                gloss = mlHi,
                                transliteration = "",
                                source = "ML Kit fallback",
                                isMlKitFallback = true
                            )
                        )
                    }
                }
                if (guGlosses.isEmpty()) {
                    val mlGu = translationService.translateFromEnglish(cleaned, StudyTargetLanguage.GUJARATI)
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                    if (mlGu != null) {
                        usedMlKit = true
                        guGlosses.add(
                            BilingualGloss(
                                targetLang = StudyTargetLanguage.GUJARATI,
                                pos = "",
                                gloss = mlGu,
                                transliteration = "",
                                source = "ML Kit fallback",
                                isMlKitFallback = true
                            )
                        )
                    }
                }
            }

            DictionaryLookupResult(
                queryWord = cleaned,
                matchedLemma = matchedLemma,
                englishSenses = enSenses,
                hindiGlosses = hiGlosses,
                gujaratiGlosses = guGlosses,
                attribution = attribution,
                buildDate = buildDate,
                usedMlKitFallback = usedMlKit
            )
        }

    private fun queryBilingual(
        db: SQLiteDatabase,
        lemma: String,
        target: StudyTargetLanguage
    ): List<BilingualGloss> {
        val out = mutableListOf<BilingualGloss>()
        db.rawQuery(
            "SELECT pos, gloss, transliteration, source FROM bi_entries WHERE en_lemma = ? AND target_lang = ?",
            arrayOf(lemma, target.code)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                out.add(
                    BilingualGloss(
                        targetLang = target,
                        pos = cursor.getString(0),
                        gloss = cursor.getString(1),
                        transliteration = cursor.getString(2),
                        source = cursor.getString(3),
                        isMlKitFallback = false
                    )
                )
            }
        }
        return out
    }

    companion object {
        fun candidateLemmas(word: String): List<String> {
            val lower = word.lowercase(Locale.ROOT).trim()
            if (lower.isBlank()) return emptyList()
            val out = linkedSetOf(lower)
            if (lower.endsWith("ies") && lower.length > 4) {
                out.add(lower.dropLast(3) + "y")
            }
            if (lower.endsWith("es") && lower.length > 3) {
                out.add(lower.dropLast(2))
            }
            if (lower.endsWith("s") && lower.length > 3) {
                out.add(lower.dropLast(1))
            }
            if (lower.endsWith("ing") && lower.length > 5) {
                out.add(lower.dropLast(3))
                out.add(lower.dropLast(3) + "e")
            }
            if (lower.endsWith("ed") && lower.length > 4) {
                out.add(lower.dropLast(2))
                out.add(lower.dropLast(1))
            }
            return out.toList()
        }
    }
}

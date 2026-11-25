package com.solerunner26.zeusreader.translation

import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.TranslationCacheEntity
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class TranslatedDisplayMode(val label: String) {
    INTERLINEAR("Interlinear"),
    TRANSLATION_ONLY("Translation only"),
    SIDE_BY_SIDE("Side-by-side")
}

enum class TranslationEnginePreference(val label: String, val engineIdVersion: String) {
    ON_DEVICE_MLKIT("On-device (ML Kit)", "mlkit_v1"),
    PREFER_PRIVATE_SERVER("Prefer private server", "server_ai_v1")
}

data class AlignedSentencePair(
    val sentenceIndex: Int,
    val sourceEnglish: String,
    val translatedText: String,
    val keptUntranslated: Boolean = false
)

data class TranslatedPassageResult(
    val passageHash: String,
    val glossaryRevision: Int,
    val engineIdVersion: String,
    val fromCache: Boolean,
    val pairs: List<AlignedSentencePair>
)

interface ServerTranslationDelegate {
    suspend fun translateBatch(sentences: List<String>, target: StudyTargetLanguage): Result<List<String>>
}

@Singleton
class TranslatedReadingEngine @Inject constructor(
    private val libraryDao: LibraryDao,
    private val mlKitTranslationService: TranslationService
) {

    @Volatile
    var serverDelegate: ServerTranslationDelegate? = null

    /**
     * Translates [passageText] sentence-by-sentence, honoring the exact cache key from GEMINI.md §3.2:
     * `(fileVersionSha256, chapterOrSectionId, sourcePassageHash, sentenceIndex, targetLang, engineIdVersion, glossaryRevision)`.
     * Never auto-swaps visible text unless [forceRetranslate] is explicitly requested.
     */
    suspend fun translatePassage(
        editionId: String,
        fileVersionSha256: String,
        chapterOrSectionId: String,
        passageText: String,
        targetLang: StudyTargetLanguage,
        enginePreference: TranslationEnginePreference,
        forceRetranslate: Boolean = false
    ): TranslatedPassageResult {
        val passageHash = SentenceSegmenter.sha256(passageText.trim())
        val glossaryEntries = libraryDao.getBookGlossary(editionId)
        val glossaryRevision = glossaryEntries.maxOfOrNull { it.glossaryRevision } ?: 0
        val glossaryTerms = glossaryEntries.map { it.term }.filter { it.isNotBlank() }

        val effectiveEngineId = resolveEffectiveEngineId(enginePreference)

        if (!forceRetranslate) {
            val cached = libraryDao.getCachedPassageTranslations(
                fileSha256 = fileVersionSha256,
                sectionId = chapterOrSectionId,
                passageHash = passageHash,
                targetLang = targetLang.code,
                engineVersion = effectiveEngineId,
                glossaryRevision = glossaryRevision
            )
            if (cached.isNotEmpty()) {
                return TranslatedPassageResult(
                    passageHash = passageHash,
                    glossaryRevision = glossaryRevision,
                    engineIdVersion = effectiveEngineId,
                    fromCache = true,
                    pairs = cached.map {
                        AlignedSentencePair(
                            sentenceIndex = it.sentenceIndex,
                            sourceEnglish = it.sourceSentence,
                            translatedText = it.translatedSentence,
                            keptUntranslated = it.sourceSentence == it.translatedSentence
                        )
                    }
                )
            }
        }

        val sentences = SentenceSegmenter.segmentSentences(passageText)
        val now = System.currentTimeMillis()
        val pairs = mutableListOf<AlignedSentencePair>()
        val cacheEntities = mutableListOf<TranslationCacheEntity>()

        sentences.forEachIndexed { idx, sentence ->
            val translated = if (SentenceSegmenter.looksNonTranslatable(sentence)) {
                sentence
            } else {
                translateWithGlossaryProtection(
                    sentence = sentence,
                    glossaryTerms = glossaryTerms,
                    targetLang = targetLang,
                    enginePreference = enginePreference
                )
            }
            pairs.add(
                AlignedSentencePair(
                    sentenceIndex = idx,
                    sourceEnglish = sentence,
                    translatedText = translated,
                    keptUntranslated = translated == sentence
                )
            )
            cacheEntities.add(
                TranslationCacheEntity(
                    fileVersionSha256 = fileVersionSha256,
                    chapterOrSectionId = chapterOrSectionId,
                    sourcePassageHash = passageHash,
                    sentenceIndex = idx,
                    targetLang = targetLang.code,
                    engineIdVersion = effectiveEngineId,
                    glossaryRevision = glossaryRevision,
                    sourceSentence = sentence,
                    translatedSentence = translated,
                    createdAtEpochMs = now
                )
            )
        }

        if (cacheEntities.isNotEmpty()) {
            libraryDao.upsertTranslationCacheEntries(cacheEntities)
        }

        return TranslatedPassageResult(
            passageHash = passageHash,
            glossaryRevision = glossaryRevision,
            engineIdVersion = effectiveEngineId,
            fromCache = false,
            pairs = pairs
        )
    }

    /** Pre-translates upcoming 2–3 pages asynchronously so page turns feel instantaneous. */
    fun pretranslateUpcomingPages(
        scope: CoroutineScope,
        editionId: String,
        fileVersionSha256: String,
        upcomingSections: List<Pair<String, String>>,
        targetLang: StudyTargetLanguage,
        enginePreference: TranslationEnginePreference
    ) {
        scope.launch(Dispatchers.IO) {
            upcomingSections.take(3).forEach { (sectionId, text) ->
                if (text.isNotBlank()) {
                    runCatching {
                        translatePassage(
                            editionId = editionId,
                            fileVersionSha256 = fileVersionSha256,
                            chapterOrSectionId = sectionId,
                            passageText = text,
                            targetLang = targetLang,
                            enginePreference = enginePreference,
                            forceRetranslate = false
                        )
                    }
                }
            }
        }
    }

    private suspend fun resolveEffectiveEngineId(pref: TranslationEnginePreference): String {
        return when (pref) {
            TranslationEnginePreference.ON_DEVICE_MLKIT -> pref.engineIdVersion
            TranslationEnginePreference.PREFER_PRIVATE_SERVER -> {
                if (serverDelegate != null) pref.engineIdVersion else TranslationEnginePreference.ON_DEVICE_MLKIT.engineIdVersion
            }
        }
    }

    private suspend fun translateWithGlossaryProtection(
        sentence: String,
        glossaryTerms: List<String>,
        targetLang: StudyTargetLanguage,
        enginePreference: TranslationEnginePreference
    ): String {
        val (masked, tokens) = protectGlossaryTerms(sentence, glossaryTerms)

        val serverResult = if (enginePreference == TranslationEnginePreference.PREFER_PRIVATE_SERVER) {
            serverDelegate?.translateBatch(listOf(masked), targetLang)?.getOrNull()?.firstOrNull()
        } else {
            null
        }
        val rawTranslation = serverResult
            ?: mlKitTranslationService.translateFromEnglish(masked, targetLang).getOrNull()
            ?: sentence

        return restoreGlossaryTerms(rawTranslation, tokens)
    }

    companion object {
        fun protectGlossaryTerms(
            sentence: String,
            glossaryTerms: List<String>
        ): Pair<String, List<Pair<String, String>>> {
            if (glossaryTerms.isEmpty()) return sentence to emptyList()
            var masked = sentence
            val tokens = mutableListOf<Pair<String, String>>()
            glossaryTerms
                .sortedByDescending { it.length }
                .forEachIndexed { idx, term ->
                    val token = "ZXG$idx"
                    val rx = Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE)
                    if (rx.containsMatchIn(masked)) {
                        masked = rx.replace(masked, token)
                        tokens.add(token to term)
                    }
                }
            return masked to tokens
        }

        fun restoreGlossaryTerms(
            translated: String,
            tokens: List<Pair<String, String>>
        ): String {
            var restored = translated
            for ((token, original) in tokens) {
                restored = restored.replace(token, original, ignoreCase = true)
            }
            return restored
        }
    }
}

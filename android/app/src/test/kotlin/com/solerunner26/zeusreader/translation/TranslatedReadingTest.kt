package com.solerunner26.zeusreader.translation

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.local.EditionEntity
import com.solerunner26.zeusreader.data.local.WorkEntity
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TranslatedReadingTest {

    private lateinit var context: Context
    private lateinit var database: ZeusReaderDatabase
    private lateinit var repository: RoomLibraryRepository

    private var translationCallCount = 0

    private val countingMlKitService = object : TranslationService {
        override suspend fun getOfflinePackStatus(): OfflinePackStatus =
            OfflinePackStatus(englishDictionaryReady = true, hindiPackDownloaded = true, gujaratiPackDownloaded = true)

        override suspend fun downloadLanguagePack(target: StudyTargetLanguage): Result<Unit> = Result.success(Unit)

        override suspend fun translateFromEnglish(text: String, target: StudyTargetLanguage): Result<String> {
            translationCallCount++
            return when (target) {
                StudyTargetLanguage.HINDI -> Result.success("HI[$text]")
                StudyTargetLanguage.GUJARATI -> Result.success("GU[$text]")
            }
        }

        override suspend fun recognizeLatinText(bitmap: Bitmap): Result<String> =
            Result.success("Scanned paragraph extracted by OCR. Light moves rapidly.")

        override suspend fun detectLanguageCode(text: String): String = "en"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLibraryRepository(database.libraryDao())
        translationCallCount = 0
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sentenceSegmenter_handlesIcuBoundariesHeadersAndEquations() {
        val pages = listOf(
            "Physics Textbook\nDr. Euler studied energy. E = m * c^2.\nPage 1",
            "Physics Textbook\nAnother chapter begins. Waves carry energy.\n2"
        )
        val repeated = SentenceSegmenter.findRepeatedEdgeLines(pages)
        assertTrue("Physics Textbook" in repeated)

        val cleaned = SentenceSegmenter.stripHeadersAndFooters(pages[0], repeated)
        assertFalse(cleaned.contains("Physics Textbook"))
        assertFalse(cleaned.contains("Page 1"))

        val sentences = SentenceSegmenter.segmentSentences(cleaned)
        assertEquals(2, sentences.size)
        assertTrue(sentences[0].contains("Dr. Euler"))

        assertTrue(SentenceSegmenter.looksNonTranslatable("x^2 + y^2 = r^2"))
        assertTrue(SentenceSegmenter.looksNonTranslatable("def compute(x):"))
        assertFalse(SentenceSegmenter.looksNonTranslatable("Dr. Euler studied energy."))
    }

    @Test
    fun translatedReadingEngine_cachesByKeyProtectsGlossaryAndInvalidatesOnRevisionChange() = runTest {
        val dao = database.libraryDao()
        val now = 1_700_000_000_000L
        dao.upsertWork(
            WorkEntity(
                id = "work-1",
                title = "Optics Primer",
                authors = "Euler",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        dao.upsertEdition(
            EditionEntity(
                id = "ed-1",
                workId = "work-1"
            )
        )

        val engine = TranslatedReadingEngine(dao, countingMlKitService)
        val passage = "Euler proved the theorem.\na + b = c + d."

        val first = engine.translatePassage(
            editionId = "ed-1",
            fileVersionSha256 = "sha-v1",
            chapterOrSectionId = "ch-1",
            passageText = passage,
            targetLang = StudyTargetLanguage.HINDI,
            enginePreference = TranslationEnginePreference.ON_DEVICE_MLKIT
        )
        assertFalse(first.fromCache)
        assertEquals(2, first.pairs.size)
        assertTrue(first.pairs[1].keptUntranslated)
        assertEquals(1, translationCallCount)

        // Repeat lookup with same key must hit Room translation cache with zero extra translator calls
        val cached = engine.translatePassage(
            editionId = "ed-1",
            fileVersionSha256 = "sha-v1",
            chapterOrSectionId = "ch-1",
            passageText = passage,
            targetLang = StudyTargetLanguage.HINDI,
            enginePreference = TranslationEnginePreference.ON_DEVICE_MLKIT
        )
        assertTrue(cached.fromCache)
        assertEquals(1, translationCallCount)

        // Adding a per-book glossary term bumps glossaryRevision -> invalidates cache & keeps term in English
        repository.addGlossaryTerm("ed-1", "Euler")
        val afterGlossary = engine.translatePassage(
            editionId = "ed-1",
            fileVersionSha256 = "sha-v1",
            chapterOrSectionId = "ch-1",
            passageText = passage,
            targetLang = StudyTargetLanguage.HINDI,
            enginePreference = TranslationEnginePreference.ON_DEVICE_MLKIT
        )
        assertFalse(afterGlossary.fromCache)
        assertEquals(1, afterGlossary.glossaryRevision)
        assertTrue(afterGlossary.pairs[0].translatedText.contains("Euler"))
        assertEquals(2, translationCallCount)

        // Gujarati uses separate targetLang key
        val gujarati = engine.translatePassage(
            editionId = "ed-1",
            fileVersionSha256 = "sha-v1",
            chapterOrSectionId = "ch-1",
            passageText = passage,
            targetLang = StudyTargetLanguage.GUJARATI,
            enginePreference = TranslationEnginePreference.ON_DEVICE_MLKIT
        )
        assertFalse(gujarati.fromCache)
        assertTrue(gujarati.pairs[0].translatedText.startsWith("GU["))
    }

    @Test
    fun serverPreferenceFallsBackGracefullyWhenServerUnavailable() = runTest {
        val dao = database.libraryDao()
        val engine = TranslatedReadingEngine(dao, countingMlKitService)

        val fallback = engine.translatePassage(
            editionId = "ed-none",
            fileVersionSha256 = "sha-x",
            chapterOrSectionId = "p0",
            passageText = "Books build enduring habits.",
            targetLang = StudyTargetLanguage.HINDI,
            enginePreference = TranslationEnginePreference.PREFER_PRIVATE_SERVER
        )
        assertEquals(TranslationEnginePreference.ON_DEVICE_MLKIT.engineIdVersion, fallback.engineIdVersion)
        assertTrue(fallback.pairs.single().translatedText.startsWith("HI["))

        // When a server delegate becomes active, explicit PREFER_PRIVATE_SERVER uses server_ai_v1
        engine.serverDelegate = object : ServerTranslationDelegate {
            override suspend fun translateBatch(
                sentences: List<String>,
                target: StudyTargetLanguage
            ): Result<List<String>> = Result.success(sentences.map { "SARVAM[$it]" })
        }

        val serverResult = engine.translatePassage(
            editionId = "ed-none",
            fileVersionSha256 = "sha-x",
            chapterOrSectionId = "p0",
            passageText = "Books build enduring habits.",
            targetLang = StudyTargetLanguage.HINDI,
            enginePreference = TranslationEnginePreference.PREFER_PRIVATE_SERVER
        )
        assertEquals(TranslationEnginePreference.PREFER_PRIVATE_SERVER.engineIdVersion, serverResult.engineIdVersion)
        assertTrue(serverResult.pairs.single().translatedText.startsWith("SARVAM["))
    }
}

package com.solerunner26.zeusreader.dictionary

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import com.solerunner26.zeusreader.vocab.ReviewGrade
import com.solerunner26.zeusreader.vocab.SpacedRepetitionScheduler
import kotlinx.coroutines.flow.first
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
class DictionaryAndVocabTest {

    private lateinit var context: Context
    private lateinit var database: ZeusReaderDatabase
    private lateinit var repository: RoomLibraryRepository

    private val fakeTranslation = object : TranslationService {
        override suspend fun getOfflinePackStatus(): OfflinePackStatus =
            OfflinePackStatus(englishDictionaryReady = true, hindiPackDownloaded = true, gujaratiPackDownloaded = false)

        override suspend fun downloadLanguagePack(target: StudyTargetLanguage): Result<Unit> = Result.success(Unit)

        override suspend fun translateFromEnglish(text: String, target: StudyTargetLanguage): Result<String> =
            when (target) {
                StudyTargetLanguage.HINDI -> Result.success("अनुवादित: $text")
                StudyTargetLanguage.GUJARATI -> Result.success("અનુવાદિત: $text")
            }

        override suspend fun recognizeLatinText(bitmap: Bitmap): Result<String> = Result.success("")
        override suspend fun detectLanguageCode(text: String): String = "en"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLibraryRepository(database.libraryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun offlineDictionary_returnsMultiSenseHiGuAndMlKitFallbackForMissingWord() = runTest {
        val dict = OfflineDictionaryRepository(context, fakeTranslation)

        val ephemeral = dict.lookup("ephemeral")
        assertTrue(ephemeral.englishSenses.size >= 2)
        assertTrue(ephemeral.hindiGlosses.any { it.gloss.contains("क्षणिक") })
        assertTrue(ephemeral.gujaratiGlosses.any { it.gloss.contains("ક્ષણિક") })
        assertFalse(ephemeral.usedMlKitFallback)
        assertTrue(ephemeral.attribution.contains("WordNet"))

        val inflected = dict.lookup("capillaries")
        assertEquals("capillary", inflected.matchedLemma)
        assertTrue(inflected.englishSenses.size >= 2)

        val missing = dict.lookup("supercalifragilistic")
        assertTrue(missing.englishSenses.isEmpty())
        assertTrue(missing.usedMlKitFallback)
        assertTrue(missing.hindiGlosses.first().isMlKitFallback)
    }

    @Test
    fun vocabularyPersistenceAndSm2SpacedRepetition_preserveContextAndUpdateIntervals() = runTest {
        val now = 1_700_000_000_000L
        val word = VocabWordEntity(
            id = "v1",
            word = "capillary",
            lemma = "capillary",
            targetLang = "hi",
            englishMeaning = "A narrow tube or pore in which liquid rises by surface tension.",
            translatedMeaning = "केशिका",
            bookTitle = "Soil Physics",
            sentenceContext = "Capillary action draws water upward through narrow soil pores.",
            nextReviewEpochMs = now,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        repository.saveVocabularyWord(word)

        val saved = repository.observeVocabularyWords().first()
        assertEquals(1, saved.size)
        assertEquals("Soil Physics", saved.first().bookTitle)
        assertTrue(saved.first().sentenceContext.contains("Capillary action"))

        val step1 = SpacedRepetitionScheduler.scheduleNext(saved.first(), ReviewGrade.GOOD, now)
        assertEquals(1, step1.repetitions)
        assertEquals(1, step1.intervalDays)

        val step2 = SpacedRepetitionScheduler.scheduleNext(step1, ReviewGrade.GOOD, now)
        assertEquals(2, step2.repetitions)
        assertEquals(6, step2.intervalDays)

        val step3 = SpacedRepetitionScheduler.scheduleNext(step2, ReviewGrade.EASY, now)
        assertEquals(3, step3.repetitions)
        assertTrue(step3.intervalDays > 6)
    }
}

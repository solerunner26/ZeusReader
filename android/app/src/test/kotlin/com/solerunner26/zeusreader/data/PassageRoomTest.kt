package com.solerunner26.zeusreader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PassageRoomTest {

    private lateinit var db: ZeusReaderDatabase
    private lateinit var repo: PassageRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PassageRepository(db.passageDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `create folder, save passage, edit, move, FTS search, soft delete and undo`() = runTest {
        val folderA = repo.createFolder("Exam quotes")
        val folderB = repo.createFolder("Philosophy")

        val saved = repo.savePassage(
            bookId = "work-1",
            editionId = "edition-1",
            folderId = folderA.id,
            text = "Knowledge is power when shared.",
            originalText = "Knowledge is power when shared.",
            locatorJson = """{"href":"ch1.xhtml","progression":0.25}""",
            pageLabel = "p. 14",
            chapterTitle = "Chapter 1",
            language = "en",
            note = "Key thesis",
            tags = listOf("epistemology", "exam")
        )

        var list = repo.activePassagesWithTags.first()
        assertEquals(1, list.size)
        assertEquals("Exam quotes", repo.activeFolders.first().first().name)
        assertEquals(listOf("epistemology", "exam"), list.first().tags)

        val ftsHits = repo.searchPassages("epistemology")
        assertEquals(1, ftsHits.size)

        repo.updatePassage(
            passage = saved,
            newText = "Knowledge is power.",
            newNote = "Trimmed quote",
            newFolderId = folderB.id,
            newTags = listOf("philosophy")
        )
        list = repo.activePassagesWithTags.first()
        assertEquals("Knowledge is power.", list.first().passage.text)
        assertEquals(folderB.id, list.first().passage.folderId)
        assertEquals(saved.originalText, list.first().passage.originalText)

        repo.softDeletePassage(saved.id)
        assertTrue(repo.activePassagesWithTags.first().isEmpty())

        repo.restorePassage(saved.id)
        assertEquals(1, repo.activePassagesWithTags.first().size)
    }
}

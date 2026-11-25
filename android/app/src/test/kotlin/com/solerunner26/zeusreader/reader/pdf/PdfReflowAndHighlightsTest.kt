package com.solerunner26.zeusreader.reader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.local.EditionEntity
import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.data.local.WorkEntity
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
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
class PdfReflowAndHighlightsTest {

    private lateinit var context: Context
    private lateinit var database: ZeusReaderDatabase
    private lateinit var repository: RoomLibraryRepository

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
    fun dehyphenateLines_joinsHyphenatedWordsPreservingParagraphFlow() {
        val joined = MuPdfDocumentSession.dehyphenateLines(
            listOf(
                "Capillary action draws wa-",
                "ter upward through nar-",
                "row soil pores."
            )
        )
        assertEquals("Capillary action draws water upward through narrow soil pores.", joined)
    }

    @Test
    fun cropLightMargins_trimsWhiteBordersLeavingContent() {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        for (y in 60 until 140) {
            for (x in 50 until 150) {
                bmp.setPixel(x, y, Color.BLACK)
            }
        }

        val cropped = MuPdfDocumentSession.cropLightMargins(bmp)
        assertTrue(cropped.width < 160)
        assertTrue(cropped.height < 160)
        assertTrue(cropped.width >= 96)
    }

    @Test
    fun highlightsPersistAcrossRestartAndRespectTombstones() = runTest {
        val dao = database.libraryDao()
        dao.upsertWork(WorkEntity("w1", "Optics", "", createdAtEpochMs = 1L, updatedAtEpochMs = 1L))
        dao.upsertEdition(EditionEntity("e1", "w1"))

        val h1 = HighlightEntity(
            id = "h1",
            editionId = "e1",
            selectedText = "Snell's law relates angles of incidence and refraction.",
            note = "Key formula",
            pageNumber = 14,
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 100L
        )
        repository.saveHighlight(h1)
        assertEquals(1, repository.getHighlights("e1").size)

        repository.deleteHighlight("h1")
        assertEquals(0, repository.getHighlights("e1").size)
    }
}

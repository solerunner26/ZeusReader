package com.solerunner26.zeusreader.data.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.local.LibraryDao
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LibraryImporterRoomTest {

    private lateinit var context: Context
    private lateinit var database: ZeusReaderDatabase
    private lateinit var dao: LibraryDao
    private lateinit var repository: RoomLibraryRepository
    private lateinit var importer: LibraryImporter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "library_originals").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.libraryDao()
        repository = RoomLibraryRepository(dao)
        importer = LibraryImporter(context, dao)
    }

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "library_originals").deleteRecursively()
    }

    @Test
    fun phase1ExitCriteria_pdfScannedPdfGroupedReorderedImagesRejectedApkAndDedup() = runTest {
        val textPdfBytes = "%PDF-1.7\n% Text PDF fixture\n1 0 obj << /Type /Catalog >> endobj\n".toByteArray()
        val scannedTwoColPdfBytes = "%PDF-1.6\n% Scanned 2-column textbook fixture\n1 0 obj << /MediaBox [0 0 1200 1600] >> endobj\n".toByteArray()

        val jpgPages = (1..5).map { idx ->
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) +
                "PAGE_$idx".toByteArray()
        }

        val fakeApkAsPdfBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write("<manifest package=\"bad\"/>".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write(byteArrayOf(0x64, 0x65, 0x78, 0x0A))
                zip.closeEntry()
            }
            baos.toByteArray()
        }

        // 1. Import 1 text PDF + 1 scanned 2-col PDF
        val pdfReport = importer.importSources(
            listOf(
                ImportSource("Linear_Algebra_Notes.pdf") { ByteArrayInputStream(textPdfBytes) },
                ImportSource("Scanned_Optics_2Col.pdf") { ByteArrayInputStream(scannedTwoColPdfBytes) }
            )
        )
        assertEquals(2, pdfReport.importedBooks.size)
        assertTrue(pdfReport.rejected.isEmpty())
        assertEquals(2, dao.countWorks())
        assertEquals(2, dao.countEditions())
        assertEquals(2, dao.countFileAssets())

        // Verify FTS4 search finds the text PDF
        val ftsMatches = repository.searchLibrarySummaries("Linear").first()
        assertEquals(1, ftsMatches.size)
        assertEquals("Linear Algebra Notes", ftsMatches.first().title)
        assertEquals("", ftsMatches.first().authors) // unknown metadata stays blank

        // 2. Import 5 JPGs -> grouped into 1 Image Book & reordered
        val imageSources = jpgPages.mapIndexed { idx, bytes ->
            ImportSource("scan_${idx + 1}.jpg") { ByteArrayInputStream(bytes) }
        }
        val imageReport = importer.importSources(
            sources = imageSources,
            imageBookTitleOverride = "Botany Lab Plates"
        )
        assertEquals(1, imageReport.importedBooks.size)
        val imageEditionId = imageReport.importedBooks.first().editionId
        val initialPages = repository.observeImageBookPages(imageEditionId).first()
        assertEquals(5, initialPages.size)
        assertEquals(listOf("scan_1.jpg", "scan_2.jpg", "scan_3.jpg", "scan_4.jpg", "scan_5.jpg"), initialPages.map { it.displayName })

        // Reorder pages: move page 5 to front and page 1 to index 3
        val reorderedIds = listOf(
            initialPages[4].id,
            initialPages[1].id,
            initialPages[2].id,
            initialPages[0].id,
            initialPages[3].id
        )
        repository.reorderImageBookPages(imageEditionId, reorderedIds)
        val persistedOrder = repository.observeImageBookPages(imageEditionId).first()
        assertEquals(
            listOf("scan_5.jpg", "scan_2.jpg", "scan_3.jpg", "scan_1.jpg", "scan_4.jpg"),
            persistedOrder.map { it.displayName }
        )

        // 3. Disguised .apk as .pdf rejected without polluting DB or originalsDir
        val rejectedReport = importer.importSources(
            listOf(ImportSource("trojan_book.pdf") { ByteArrayInputStream(fakeApkAsPdfBytes) })
        )
        assertEquals(0, rejectedReport.importedBooks.size)
        assertEquals(1, rejectedReport.rejected.size)
        assertTrue(rejectedReport.rejected.first().reason.contains("APK", ignoreCase = true))

        // 4. Re-import exact same text PDF -> SHA-256 deduplicated, zero duplicates created
        val duplicateReport = importer.importSources(
            listOf(ImportSource("Linear_Algebra_Copy.pdf") { ByteArrayInputStream(textPdfBytes) })
        )
        assertEquals(0, duplicateReport.importedBooks.size)
        assertEquals(1, duplicateReport.duplicates.size)

        // Total counts: 2 PDFs + 1 Image Book = 3 Works, 3 Editions; 2 PDFs + 5 JPGs = 7 FileAssets
        assertEquals(3, dao.countWorks())
        assertEquals(3, dao.countEditions())
        assertEquals(7, dao.countFileAssets())
    }
}

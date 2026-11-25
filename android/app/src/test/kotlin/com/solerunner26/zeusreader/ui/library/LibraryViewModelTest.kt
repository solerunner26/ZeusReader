package com.solerunner26.zeusreader.ui.library

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.RoomLibraryRepository
import com.solerunner26.zeusreader.data.importer.ImportSource
import com.solerunner26.zeusreader.data.importer.LibraryImporter
import com.solerunner26.zeusreader.data.importer.WatchedFolderManager
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LibraryViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: ZeusReaderDatabase
    private lateinit var importer: LibraryImporter
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "library_originals").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = database.libraryDao()
        val repo = RoomLibraryRepository(dao)
        importer = LibraryImporter(context, dao)
        val watchedFolderManager = WatchedFolderManager(context, dao, importer)
        viewModel = LibraryViewModel(repo, importer, watchedFolderManager)
    }

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
        database.close()
        File(context.filesDir, "library_originals").deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_transitionsFromEmptyToPopulatedAndFiltersByFormat() = runTest {
        val collector = backgroundScope.launch(mainDispatcher) { viewModel.uiState.collect {} }

        val emptyState = viewModel.uiState.first { it !is LibraryUiState.Loading }
        assertEquals(LibraryUiState.Empty, emptyState)

        importer.importSources(
            listOf(
                ImportSource("Thermodynamics.pdf") {
                    ByteArrayInputStream("%PDF-1.7\n1 0 obj<<>>endobj\n".toByteArray())
                },
                ImportSource("diagram_1.jpg") {
                    ByteArrayInputStream(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
                }
            ),
            imageBookTitleOverride = "Optics Diagrams"
        )

        val populated = viewModel.uiState.first {
            it is LibraryUiState.Populated && it.books.size == 2
        } as LibraryUiState.Populated
        assertEquals(2, populated.books.size)

        viewModel.onFilterSelected(LibraryFormatFilter.PDF)
        val pdfOnly = viewModel.uiState.first {
            it is LibraryUiState.Populated && it.selectedFilter == LibraryFormatFilter.PDF
        } as LibraryUiState.Populated
        assertEquals(1, pdfOnly.books.size)
        assertTrue(pdfOnly.books.first().primaryFormat.equals("PDF", ignoreCase = true))

        viewModel.onFilterSelected(LibraryFormatFilter.IMAGE_BOOK)
        val imagesOnly = viewModel.uiState.first {
            it is LibraryUiState.Populated && it.selectedFilter == LibraryFormatFilter.IMAGE_BOOK
        } as LibraryUiState.Populated
        assertEquals(1, imagesOnly.books.size)
        assertTrue(imagesOnly.books.first().isImageBook)

        collector.cancel()
    }
}

package com.solerunner26.zeusreader.ui.library

import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.WorkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_whenRepositoryEmpty_emitsEmpty() = runTest(testDispatcher) {
        val repository = FakeLibraryRepository(emptyList())
        val viewModel = LibraryViewModel(repository)

        assertEquals(LibraryUiState.Loading, viewModel.uiState.value)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(LibraryUiState.Empty, viewModel.uiState.value)
        collectJob.cancel()
    }

    @Test
    fun uiState_whenWorksPresent_emitsPopulated() = runTest(testDispatcher) {
        val sample = WorkEntity(
            id = "work-1",
            title = "Flatland",
            authors = "Edwin A. Abbott",
            language = "en",
            createdAtEpochMs = 1_700_000_000_000L,
            updatedAtEpochMs = 1_700_000_000_000L
        )
        val repository = FakeLibraryRepository(listOf(sample))
        val viewModel = LibraryViewModel(repository)

        val state = viewModel.uiState.first { it !is LibraryUiState.Loading }
        assertTrue(state is LibraryUiState.Populated)
        assertEquals(listOf(sample), (state as LibraryUiState.Populated).works)
    }

    @Test
    fun uiState_whenRepositoryFails_emitsError() = runTest(testDispatcher) {
        val failingRepository = object : LibraryRepository {
            override fun observeWorks(): Flow<List<WorkEntity>> = flow {
                error("Database unavailable")
            }

            override fun searchWorks(ftsQuery: String): Flow<List<WorkEntity>> = flow {
                emit(emptyList())
            }
        }
        val viewModel = LibraryViewModel(failingRepository)

        val state = viewModel.uiState.first { it !is LibraryUiState.Loading }
        assertTrue(state is LibraryUiState.Error)
        assertEquals("Database unavailable", (state as LibraryUiState.Error).message)
    }
}

private class FakeLibraryRepository(
    initialWorks: List<WorkEntity>
) : LibraryRepository {
    private val works = MutableStateFlow(initialWorks)

    override fun observeWorks(): Flow<List<WorkEntity>> = works

    override fun searchWorks(ftsQuery: String): Flow<List<WorkEntity>> = works
}

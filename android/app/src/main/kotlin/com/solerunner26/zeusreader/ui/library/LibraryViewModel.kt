package com.solerunner26.zeusreader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.WorkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    libraryRepository: LibraryRepository
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> =
        libraryRepository.observeWorks()
            .map { works ->
                if (works.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Populated(works)
                }
            }
            .catch { throwable ->
                emit(LibraryUiState.Error(throwable.message ?: "Failed to load library"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState.Loading
            )
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Populated(val works: List<WorkEntity>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

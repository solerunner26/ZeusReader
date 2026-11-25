package com.solerunner26.zeusreader.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.PassageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ReadingStatsUiState(
    val totalBooks: Int = 0,
    val booksInProgress: Int = 0,
    val totalPassages: Int = 0,
    val totalVocabWords: Int = 0,
    val dueVocabWords: Int = 0,
    val currentStreakDays: Int = 0,
    val activeReadingDays: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val passageDao: PassageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingStatsUiState())
    val uiState: StateFlow<ReadingStatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                libraryDao.observeLibrarySummaries(),
                passageDao.observeActivePassages(),
                libraryDao.observeVocabularyWords()
            ) { books, passages, vocab ->
                val now = System.currentTimeMillis()
                val zone = ZoneId.systemDefault()
                val activeDates = buildSet<LocalDate> {
                    books.mapNotNull { it.lastPageIndex?.let { _ -> it.updatedAtEpochMs } }
                        .forEach { add(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) }
                    passages.forEach { add(Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()) }
                    vocab.forEach { add(Instant.ofEpochMilli(it.createdAtEpochMs).atZone(zone).toLocalDate()) }
                }
                ReadingStatsUiState(
                    totalBooks = books.size,
                    booksInProgress = books.count { (it.lastPageIndex ?: 0) > 0 },
                    totalPassages = passages.size,
                    totalVocabWords = vocab.size,
                    dueVocabWords = vocab.count { it.nextReviewEpochMs <= now },
                    currentStreakDays = computeConsecutiveStreak(activeDates, LocalDate.now(zone)),
                    activeReadingDays = activeDates.size
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    companion object {
        internal fun computeConsecutiveStreak(activeDates: Set<LocalDate>, today: LocalDate): Int {
            if (activeDates.isEmpty()) return 0
            var cursor = if (today in activeDates) today else today.minusDays(1)
            if (cursor !in activeDates) return 0
            var streak = 0
            while (cursor in activeDates) {
                streak += 1
                cursor = cursor.minusDays(1)
            }
            return streak
        }
    }
}

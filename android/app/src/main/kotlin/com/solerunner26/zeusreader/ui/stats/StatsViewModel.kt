package com.solerunner26.zeusreader.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.PassageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class ReadingStatsUiState(
    val totalBooks: Int = 0,
    val booksInProgress: Int = 0,
    val totalPassages: Int = 0,
    val totalVocabWords: Int = 0,
    val dueVocabWords: Int = 0,
    val currentStreakDays: Int = 0,
    val activeReadingDays: Int = 0,
    val bestStreakDays: Int = 0,
    val todayMinutes: Int = 0,
    val dailyGoalMinutes: Int = 20,
    val monthMinutes: Int = 0,
    val totalMinutes: Int = 0,
    /** Monday..Sunday of the current week: true when the reader was active that day. */
    val weekActive: List<Boolean> = List(7) { false },
    val todayIndex: Int = 0,
    val displayName: String = ""
) {
    val goalProgress: Float get() = if (dailyGoalMinutes <= 0) 0f else (todayMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
    val goalReached: Boolean get() = todayMinutes >= dailyGoalMinutes && dailyGoalMinutes > 0
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val passageDao: PassageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingStatsUiState())
    val uiState: StateFlow<ReadingStatsUiState> = _uiState.asStateFlow()

    private val readingTime = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    private val profileTick = MutableStateFlow(0)
    private val profile = ReaderProfile(context)

    init {
        refresh()
        viewModelScope.launch {
            combine(
                libraryDao.observeLibrarySummaries(),
                passageDao.observeActivePassages(),
                libraryDao.observeVocabularyWords(),
                readingTime,
                profileTick
            ) { books, passages, vocab, time, _ ->
                val now = System.currentTimeMillis()
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val activeDates = buildSet<LocalDate> {
                    books.mapNotNull { it.lastPageIndex?.let { _ -> it.updatedAtEpochMs } }
                        .forEach { add(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) }
                    passages.forEach { add(Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()) }
                    vocab.forEach { add(Instant.ofEpochMilli(it.createdAtEpochMs).atZone(zone).toLocalDate()) }
                    time.filterValues { it >= 60L }.keys.forEach { add(it) }
                }
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReadingStatsUiState(
                    totalBooks = books.size,
                    booksInProgress = books.count { (it.lastPageIndex ?: 0) > 0 },
                    totalPassages = passages.size,
                    totalVocabWords = vocab.size,
                    dueVocabWords = vocab.count { it.nextReviewEpochMs <= now },
                    currentStreakDays = computeConsecutiveStreak(activeDates, today),
                    activeReadingDays = activeDates.size,
                    bestStreakDays = computeBestStreak(activeDates),
                    todayMinutes = ((time[today] ?: 0L) / 60L).toInt(),
                    dailyGoalMinutes = profile.dailyGoalMinutes,
                    monthMinutes = (time.filterKeys { it.year == today.year && it.month == today.month }.values.sum() / 60L).toInt(),
                    totalMinutes = (time.values.sum() / 60L).toInt(),
                    weekActive = (0 until 7).map { monday.plusDays(it.toLong()) in activeDates },
                    todayIndex = today.dayOfWeek.value - 1,
                    displayName = profile.displayName
                )
            }.collect { _uiState.value = it }
        }
    }

    /** Re-reads reading time written by the reader process and profile changes. */
    fun refresh() {
        viewModelScope.launch {
            readingTime.value = withContext(Dispatchers.IO) { ReadingTimeStore.load(context) }
            profileTick.value = profileTick.value + 1
        }
    }

    fun setDailyGoal(minutes: Int) {
        profile.dailyGoalMinutes = minutes
        refresh()
    }

    fun setDisplayName(name: String) {
        profile.displayName = name
        refresh()
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

        internal fun computeBestStreak(activeDates: Set<LocalDate>): Int {
            var best = 0
            activeDates.forEach { d ->
                if (d.minusDays(1) !in activeDates) {
                    var len = 0
                    var cur = d
                    while (cur in activeDates) {
                        len++
                        cur = cur.plusDays(1)
                    }
                    best = maxOf(best, len)
                }
            }
            return best
        }
    }
}

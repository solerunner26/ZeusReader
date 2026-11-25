package com.solerunner26.zeusreader.vocab

import com.solerunner26.zeusreader.data.local.VocabWordEntity
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewGrade(val quality: Int, val label: String) {
    AGAIN(1, "Again"),
    GOOD(4, "Good"),
    EASY(5, "Easy")
}

/** SM-2 spaced-repetition scheduler for offline vocabulary flashcards. */
object SpacedRepetitionScheduler {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun scheduleNext(
        word: VocabWordEntity,
        grade: ReviewGrade,
        nowEpochMs: Long = System.currentTimeMillis()
    ): VocabWordEntity {
        val q = grade.quality.coerceIn(0, 5)
        val (nextReps, nextInterval) = if (q < 3) {
            0 to 1
        } else {
            when (word.repetitions) {
                0 -> 1 to 1
                1 -> 2 to 6
                else -> (word.repetitions + 1) to max(1, (word.intervalDays * word.easeFactor).roundToInt())
            }
        }

        val delta = 0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)
        val nextEase = max(1.3f, word.easeFactor + delta)

        return word.copy(
            repetitions = nextReps,
            intervalDays = nextInterval,
            easeFactor = nextEase,
            nextReviewEpochMs = nowEpochMs + nextInterval * DAY_MS,
            updatedAtEpochMs = nowEpochMs,
            version = word.version + 1
        )
    }
}

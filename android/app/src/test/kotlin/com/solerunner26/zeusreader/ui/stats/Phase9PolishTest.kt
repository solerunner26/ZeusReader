package com.solerunner26.zeusreader.ui.stats

import com.solerunner26.zeusreader.ui.reader.TtsSleepTimerController
import com.solerunner26.zeusreader.ui.reader.TtsSleepTimerOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class Phase9PolishTest {

    @Test
    fun `computeConsecutiveStreak counts today and consecutive prior days`() {
        val today = LocalDate.of(2026, 11, 25)
        val dates = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, StatsViewModel.computeConsecutiveStreak(dates, today))
    }

    @Test
    fun `computeConsecutiveStreak returns zero when neither today nor yesterday active`() {
        val today = LocalDate.of(2026, 11, 25)
        val dates = setOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, StatsViewModel.computeConsecutiveStreak(dates, today))
    }

    @Test
    fun `TtsSleepTimerController invokes onExpired after selected duration`() = runTest {
        var expired = false
        val controller = TtsSleepTimerController(
            scope = this,
            onExpired = { expired = true },
            tickIntervalMs = 1L
        )
        controller.select(TtsSleepTimerOption.MIN_15)
        assertTrue(controller.state.value.isActive)
        assertEquals(15 * 60, controller.state.value.remainingSeconds)

        advanceTimeBy(15 * 60L + 5L)
        assertTrue(expired)
        assertFalse(controller.state.value.isActive)
        assertEquals(TtsSleepTimerOption.OFF, controller.state.value.selectedOption)
    }
}

package com.solerunner26.zeusreader.ui.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TtsSleepTimerOption(val label: String, val durationMinutes: Int) {
    OFF("Sleep: Off", 0),
    MIN_15("Sleep: 15m", 15),
    MIN_30("Sleep: 30m", 30),
    MIN_45("Sleep: 45m", 45),
    MIN_60("Sleep: 60m", 60)
}

data class TtsSleepTimerState(
    val selectedOption: TtsSleepTimerOption = TtsSleepTimerOption.OFF,
    val remainingSeconds: Int = 0
) {
    val isActive: Boolean get() = selectedOption != TtsSleepTimerOption.OFF && remainingSeconds > 0
}

/**
 * Lightweight countdown timer for TTS playback that triggers [onExpired]
 * when the selected duration elapses.
 */
class TtsSleepTimerController(
    private val scope: CoroutineScope,
    private val onExpired: () -> Unit,
    private val tickIntervalMs: Long = 1_000L
) {
    private val _state = MutableStateFlow(TtsSleepTimerState())
    val state: StateFlow<TtsSleepTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun select(option: TtsSleepTimerOption) {
        timerJob?.cancel()
        if (option == TtsSleepTimerOption.OFF) {
            _state.value = TtsSleepTimerState(TtsSleepTimerOption.OFF, 0)
            return
        }
        val totalSeconds = option.durationMinutes * 60
        _state.value = TtsSleepTimerState(option, totalSeconds)
        timerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(tickIntervalMs)
                remaining -= 1
                _state.value = TtsSleepTimerState(option, remaining)
            }
            _state.value = TtsSleepTimerState(TtsSleepTimerOption.OFF, 0)
            onExpired()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        _state.value = TtsSleepTimerState(TtsSleepTimerOption.OFF, 0)
    }
}

package com.solerunner26.zeusreader.ui.stats

import android.content.Context
import java.io.File
import java.time.LocalDate

/**
 * Records real reading time per day (seconds), written by the reader process and read by the
 * main process. A tiny line file ("2026-11-28=1234") is used instead of SharedPreferences because
 * the reader runs in its own `:reader` process and SharedPreferences are cached per process.
 */
object ReadingTimeStore {
    private const val FILE = "reading_time_v1.txt"
    private const val KEEP_DAYS = 400L

    private fun file(context: Context) = File(context.filesDir, FILE)

    @Synchronized
    fun load(context: Context): Map<LocalDate, Long> = runCatching {
        val f = file(context)
        if (!f.exists()) emptyMap() else f.readLines().mapNotNull { line ->
            val parts = line.split('=')
            if (parts.size != 2) null else runCatching { LocalDate.parse(parts[0]) to parts[1].toLong() }.getOrNull()
        }.toMap()
    }.getOrDefault(emptyMap())

    @Synchronized
    fun addSeconds(context: Context, seconds: Long, day: LocalDate = LocalDate.now()) {
        if (seconds <= 0) return
        runCatching {
            val map = load(context).toMutableMap()
            map[day] = (map[day] ?: 0L) + seconds.coerceAtMost(6 * 3600L)
            val cutoff = day.minusDays(KEEP_DAYS)
            val text = map.filterKeys { it.isAfter(cutoff) }.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }
            val tmp = File(context.filesDir, "$FILE.tmp")
            tmp.writeText(text)
            tmp.renameTo(file(context))
        }
    }

    fun minutesOn(context: Context, day: LocalDate = LocalDate.now()): Int = ((load(context)[day] ?: 0L) / 60L).toInt()
}

/** Small per-user profile & goal preferences (main process only). */
class ReaderProfile(context: Context) {
    private val prefs = context.getSharedPreferences("zeus_reader_profile", Context.MODE_PRIVATE)

    var displayName: String
        get() = prefs.getString("display_name", "").orEmpty()
        set(value) = prefs.edit().putString("display_name", value.trim().take(40)).apply()

    var dailyGoalMinutes: Int
        get() = prefs.getInt("daily_goal_minutes", 20)
        set(value) = prefs.edit().putInt("daily_goal_minutes", value.coerceIn(5, 180)).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()

    var lastSeenActivityMs: Long
        get() = prefs.getLong("last_seen_activity_ms", 0L)
        set(value) = prefs.edit().putLong("last_seen_activity_ms", value).apply()
}

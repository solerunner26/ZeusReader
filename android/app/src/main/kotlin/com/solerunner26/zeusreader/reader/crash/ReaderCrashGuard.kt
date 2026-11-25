package com.solerunner26.zeusreader.reader.crash

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ReaderCrashReport(
    val bookTitle: String,
    val format: String,
    val errorSummary: String,
    val timestampEpochMs: Long
)

/**
 * Cross-process crash marker stored in app-private storage so native/parser crashes in `:reader`
 * surface cleanly in `:app` Library without terminating Library.
 */
object ReaderCrashGuard {
    private const val CRASH_FILE_NAME = "last_reader_crash.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun recordCrash(context: Context, bookTitle: String, format: String, throwable: Throwable) {
        val report = ReaderCrashReport(
            bookTitle = bookTitle,
            format = format,
            errorSummary = throwable.message?.take(240) ?: throwable::class.java.simpleName,
            timestampEpochMs = System.currentTimeMillis()
        )
        runCatching {
            File(context.filesDir, CRASH_FILE_NAME).writeText(json.encodeToString(report))
        }
    }

    fun consumeCrashReport(context: Context): ReaderCrashReport? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (!file.exists()) return null
        return runCatching {
            val report = json.decodeFromString<ReaderCrashReport>(file.readText())
            file.delete()
            report
        }.getOrElse {
            file.delete()
            null
        }
    }
}

package com.solerunner26.zeusreader.reader.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Declared feature matrix per GEMINI.md §3.2. */
data class ReaderCapabilities(
    val textSelection: Boolean,
    val search: Boolean,
    val translate: Boolean,
    val highlight: Boolean,
    val tts: Boolean
) {
    companion object {
        val EPUB = ReaderCapabilities(
            textSelection = true,
            search = true,
            translate = true,
            highlight = true,
            tts = true
        )
        val PDF = ReaderCapabilities(
            textSelection = true,
            search = true,
            translate = true,
            highlight = true,
            tts = true
        )
        val IMAGE_BOOK_BEFORE_OCR = ReaderCapabilities(
            textSelection = false,
            search = false,
            translate = false,
            highlight = false,
            tts = false
        )
        val HTML_SANDBOXED = ReaderCapabilities(
            textSelection = true,
            search = true,
            translate = true,
            highlight = true,
            tts = true
        )
    }
}

enum class ReaderEngineType {
    EPUB_READIUM,
    PDF_MUPDF,
    IMAGE_BOOK,
    MUPDF_EXPANDED,
    HTML_SANDBOXED
}

interface ReaderEngine {
    val engineType: ReaderEngineType
    val capabilities: ReaderCapabilities
}

/**
 * Unified cross-format resume anchor persisted inside [com.solerunner26.zeusreader.data.local.ReadingProgressEntity].
 */
@Serializable
sealed interface UnifiedLocator {

    @Serializable
    data class EpubLocator(
        val readiumLocatorJson: String,
        val href: String,
        val title: String? = null,
        val progression: Double = 0.0,
        val totalProgression: Double? = null
    ) : UnifiedLocator

    @Serializable
    data class PdfLocator(
        val pageIndex: Int,
        val viewportScale: Float = 1f,
        val scrollX: Float = 0f,
        val scrollY: Float = 0f
    ) : UnifiedLocator

    @Serializable
    data class ImageBookLocator(
        val pageIndex: Int,
        val viewportScale: Float = 1f
    ) : UnifiedLocator

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(locator: UnifiedLocator): String = json.encodeToString(locator)

        fun decode(raw: String?): UnifiedLocator? {
            if (raw.isNullOrBlank()) return null
            return runCatching { json.decodeFromString<UnifiedLocator>(raw) }.getOrNull()
        }
    }
}

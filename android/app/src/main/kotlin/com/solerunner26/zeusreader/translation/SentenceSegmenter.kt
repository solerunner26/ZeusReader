package com.solerunner26.zeusreader.translation

import android.icu.text.BreakIterator
import java.security.MessageDigest
import java.util.Locale

/**
 * Sentence segmentation, header/footer stripping, and code/equation detection
 * for Translated Reading Mode (`GEMINI.md §4.2`).
 */
object SentenceSegmenter {

    private val EQUATION_OR_CODE_REGEX = Regex(
        """^[\s0-9+\-*/=^()\[\]{}<>|\\.,:;_%]+$|^(def |class |val |var |fun |import |return |if\s*\(|for\s*\()"""
    )
    private val PAGE_NUMBER_LINE_REGEX = Regex("""^\s*(page\s+)?\d{1,4}\s*$""", RegexOption.IGNORE_CASE)

    private val ABBREVIATION_ENDINGS = setOf(
        "dr.", "mr.", "mrs.", "ms.", "prof.", "sr.", "jr.",
        "vs.", "e.g.", "i.e.", "fig.", "eq.", "no.", "ch.", "sec."
    )

    fun segmentSentences(text: String, locale: Locale = Locale.ENGLISH): List<String> {
        val rawBlocks = text
            .split(Regex("\\r?\\n"))
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
        if (rawBlocks.isEmpty()) return emptyList()

        val out = mutableListOf<String>()
        for (block in rawBlocks) {
            val iterator = BreakIterator.getSentenceInstance(locale)
            iterator.setText(block)
            var start = iterator.first()
            var end = iterator.next()
            var isFirstInBlock = true
            while (end != BreakIterator.DONE) {
                val sentence = block.substring(start, end).trim()
                if (sentence.isNotBlank()) {
                    val prev = if (!isFirstInBlock) out.lastOrNull() else null
                    val lastWord = prev?.substringAfterLast(' ')?.lowercase()
                    if (prev != null && lastWord in ABBREVIATION_ENDINGS) {
                        out[out.lastIndex] = "$prev $sentence"
                    } else {
                        out.add(sentence)
                    }
                    isFirstInBlock = false
                }
                start = end
                end = iterator.next()
            }
        }
        return out
    }

    /** Identifies equations, numeric tables, or code snippets that should remain untranslated. */
    fun looksNonTranslatable(sentence: String): Boolean {
        val trimmed = sentence.trim()
        if (trimmed.isEmpty()) return true
        if (EQUATION_OR_CODE_REGEX.containsMatchIn(trimmed)) return true
        val letters = trimmed.count { it.isLetter() }
        val symbols = trimmed.count { it in "=<>+*/^[]{}|" }
        return letters == 0 || (symbols >= 3 && symbols > letters / 3)
    }

    /** Removes common running headers/footers and isolated page-number lines across extracted pages. */
    fun stripHeadersAndFooters(pageText: String, repeatedLines: Set<String> = emptySet()): String {
        val lines = pageText.lines()
        if (lines.isEmpty()) return ""
        val filtered = lines.filterIndexed { index, raw ->
            val trimmed = raw.trim()
            val isEdgeLine = index == 0 || index == lines.lastIndex
            when {
                trimmed.isEmpty() -> true
                PAGE_NUMBER_LINE_REGEX.matches(trimmed) -> false
                isEdgeLine && trimmed in repeatedLines -> false
                else -> true
            }
        }
        return filtered.joinToString("\n").trim()
    }

    fun findRepeatedEdgeLines(pages: List<String>): Set<String> {
        if (pages.size < 2) return emptySet()
        val counts = hashMapOf<String, Int>()
        for (page in pages) {
            val nonEmpty = page.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (nonEmpty.isEmpty()) continue
            listOfNotNull(nonEmpty.firstOrNull(), nonEmpty.lastOrNull()).distinct().forEach { edge ->
                counts[edge] = (counts[edge] ?: 0) + 1
            }
        }
        return counts.filterValues { it >= 2 }.keys
    }

    fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

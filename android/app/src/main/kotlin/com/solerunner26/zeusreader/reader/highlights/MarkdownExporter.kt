package com.solerunner26.zeusreader.reader.highlights

import com.solerunner26.zeusreader.data.local.HighlightEntity

/** Formats book highlights and notes into clean Markdown for offline backup or sharing. */
object MarkdownExporter {

    fun exportHighlights(
        bookTitle: String,
        authors: String = "",
        highlights: List<HighlightEntity>
    ): String = buildString {
        appendLine("# $bookTitle")
        if (authors.isNotBlank()) {
            appendLine("*By $authors*")
        }
        appendLine()
        if (highlights.isEmpty()) {
            appendLine("_No highlights saved yet._")
            return@buildString
        }
        highlights.forEach { item ->
            val location = buildList {
                if (item.chapterTitle.isNotBlank()) add(item.chapterTitle)
                if (item.pageNumber != null) add("Page ${item.pageNumber}")
            }.joinToString(" • ")

            if (location.isNotBlank()) {
                appendLine("### $location")
            }
            appendLine("> ${item.selectedText.trim().replace("\n", "\n> ")}")
            if (item.note.isNotBlank()) {
                appendLine()
                appendLine("**Note:** ${item.note.trim()}")
            }
            appendLine()
        }
    }
}

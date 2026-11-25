package com.solerunner26.zeusreader.reader.highlights

import com.solerunner26.zeusreader.data.local.HighlightEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExporterTest {

    @Test
    fun exportHighlights_formatsTitleQuotesAndNotes() {
        val highlights = listOf(
            HighlightEntity(
                id = "h1",
                editionId = "e1",
                selectedText = "Capillary action draws water upward through narrow soil pores.",
                note = "Compare against Poiseuille equation",
                chapterTitle = "Chapter 4",
                pageNumber = 42,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L
            )
        )

        val md = MarkdownExporter.exportHighlights(
            bookTitle = "Soil Physics",
            authors = "Hillel",
            highlights = highlights
        )

        assertTrue(md.contains("# Soil Physics"))
        assertTrue(md.contains("*By Hillel*"))
        assertTrue(md.contains("### Chapter 4 • Page 42"))
        assertTrue(md.contains("> Capillary action draws water upward"))
        assertTrue(md.contains("**Note:** Compare against Poiseuille equation"))
    }
}

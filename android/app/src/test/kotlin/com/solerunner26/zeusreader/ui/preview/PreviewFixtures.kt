package com.solerunner26.zeusreader.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.theme.StormlightSky
import com.solerunner26.zeusreader.ui.theme.ZeusPaper
import com.solerunner26.zeusreader.ui.theme.ZeusSky
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

object PreviewFixtures {
    fun book(i: Int, title: String, author: String, format: String = "EPUB", progress: Int? = null) = LibraryBookSummary(
        workId = "w$i",
        editionId = "e$i",
        title = title,
        authors = author,
        language = "en",
        publicationYear = 2020,
        primaryFormat = format,
        totalByteSize = 1_000_000L,
        assetCount = 1,
        imagePageCount = 0,
        primaryLocalPath = null,
        lastPageIndex = progress,
        updatedAtEpochMs = 1_700_000_000_000L + i,
        category = "Science"
    )

    val books = listOf(
        book(1, "Soil Science for Beginners", "R. Mehta", progress = 370),
        book(2, "Organic Chemistry Notes", "A. Shah", format = "PDF", progress = 12),
        book(3, "Physics, Volume Two", "H. Verma", format = "PDF"),
        book(4, "The Nicomachean Ethics", "Aristotle"),
        book(5, "Life Underground", "K. Rao")
    )
}

/** Theme + living sky (no animation) for snapshot tests. */
@Composable
fun PreviewSky(
    sky: ZeusSky = ZeusSky.STORM,
    paper: ZeusPaper = ZeusPaper.NIGHT,
    daylight: Boolean = false,
    content: @Composable () -> Unit
) {
    ZeusTheme(sky = sky, paper = paper, reduceMotion = true, daylight = daylight) {
        StormlightSky(animate = false, modifier = Modifier) { content() }
    }
}

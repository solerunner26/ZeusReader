package com.solerunner26.zeusreader.ui.preview

import androidx.compose.runtime.Composable
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.theme.ZeusBackground
import com.solerunner26.zeusreader.ui.theme.ZeusMode
import com.solerunner26.zeusreader.ui.theme.ZeusPack
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
}

/** Sunset theme + canvas (no motion) for snapshot tests. */
@Composable
fun PreviewTheme(pack: ZeusPack = ZeusPack.SUNSET, mode: ZeusMode = ZeusMode.DAY, content: @Composable () -> Unit) {
    ZeusTheme(pack = pack, mode = mode, reduceMotion = true) {
        ZeusBackground { content() }
    }
}

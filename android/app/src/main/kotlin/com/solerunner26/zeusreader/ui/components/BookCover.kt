package com.solerunner26.zeusreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.solerunner26.zeusreader.ui.theme.ZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusPack
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import java.io.File
import kotlin.math.abs

/**
 * Book cover: the real cover cropped to fill (never stretched), or a generated two-tone
 * "weather" cover (title + author on a sky gradient chosen from the title) when none exists.
 */
@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    authors: String = "",
    format: String = "",
    coverPath: String? = null,
    shape: Shape = ZeusShapes.cover,
    compact: Boolean = false
) {
    val hasCover = remember(coverPath) { !coverPath.isNullOrBlank() && File(coverPath).exists() }
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Box(
        modifier = modifier
            .clip(shape)
            .border(ZeusSpacing.hairline, colors.line, shape),
        contentAlignment = Alignment.BottomStart
    ) {
        if (hasCover) {
            AsyncImage(
                model = File(coverPath!!),
                contentDescription = "$title cover",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CappedFontScale(1f) {
            val sky = remember(title) { coverPalettes[abs(title.hashCode()) % coverPalettes.size] }
            val eink = ZeusTheme.colors.isEink
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (eink) {
                            Brush.linearGradient(listOf(ZeusPalette.einkHighlight, ZeusPalette.einkHighlight))
                        } else {
                            Brush.linearGradient(listOf(sky.first, sky.second))
                        }
                    )
            ) {
                if (!eink) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)))
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (compact) ZeusSpacing.sm else ZeusSpacing.md),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (format.isNotBlank()) {
                        Text(
                            text = format.uppercase(),
                            style = type.tiny,
                            color = if (eink) ZeusPaper_ink() else ZeusPalette.white.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
                        Text(
                            text = title,
                            style = if (compact) type.tiny else type.headline,
                            color = if (eink) ZeusPaper_ink() else ZeusPalette.starlight,
                            maxLines = if (compact) 3 else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (authors.isNotBlank() && !compact) {
                            Text(
                                text = authors.uppercase(),
                                style = type.tiny,
                                color = if (eink) ZeusPaper_ink() else ZeusPalette.starlight.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Suppress("FunctionName")
private fun ZeusPaper_ink(): Color = ZeusPalette.black

/** Rich two-tone cover palettes for books without artwork (deep, readable with white type). */
private val coverPalettes = listOf(
    ZeusPalette.coverMoss to ZeusPalette.coverMossDeep,
    ZeusPalette.coverBrick to ZeusPalette.coverBrickDeep,
    ZeusPalette.coverNavy to ZeusPalette.coverNavyDeep,
    ZeusPalette.coverOchre to ZeusPalette.coverOchreDeep,
    ZeusPalette.coverPlum to ZeusPalette.coverPlumDeep,
    ZeusPalette.coverTeal to ZeusPalette.coverTealDeep
)

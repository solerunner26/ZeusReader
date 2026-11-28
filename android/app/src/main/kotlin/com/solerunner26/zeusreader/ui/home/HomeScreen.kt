package com.solerunner26.zeusreader.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.library.LibraryUiState
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.passages.PassagesViewModel
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
import com.solerunner26.zeusreader.ui.theme.LiquidGlassCard
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.liquidGlass
import java.io.File

@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    passagesViewModel: PassagesViewModel,
    statsViewModel: StatsViewModel,
    glassEnabled: Boolean,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPrepareOffline: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val libState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val passagesState by passagesViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()

    val books = (libState as? LibraryUiState.Populated)?.books.orEmpty()
    val continueBook = books.firstOrNull { (it.lastPageIndex ?: 0) > 0 } ?: books.firstOrNull()
    val featuredSpark = passagesState.passages.firstOrNull()

    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val reduceMotion = ZeusTheme.reduceMotion

    val bobOffsetY = if (!reduceMotion) {
        val transition = rememberInfiniteTransition(label = "home_cover_bob")
        val y by transition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ZeusTheme.motion.COVER_BOB_MS),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cover_bob_y"
        )
        y
    } else {
        0f
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("home_screen"),
        contentPadding = PaddingValues(
            start = ZeusSpacing.lg,
            end = ZeusSpacing.lg,
            top = ZeusSpacing.md,
            bottom = ZeusSpacing.dockBottomClearance
        ),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
    ) {
        // Top Header: Brand Banner + Streak Sticker + Settings/Profile Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zeus_home_banner),
                    contentDescription = "ZeusReader",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(ZeusShapes.pill)
                )

                Surface(
                    onClick = onOpenStats,
                    shape = ZeusShapes.sticker,
                    color = colors.stickerLightBg,
                    contentColor = colors.stickerInk,
                    modifier = Modifier
                        .heightIn(min = ZeusSpacing.touchMin)
                        .rotate(2.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = colors.stickerInk,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${statsState.currentStreakDays}d streak",
                            style = type.sticker,
                            color = colors.stickerInk,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(ZeusSpacing.touchMin)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(sky.accent, sky.glow2)))
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Profile and settings",
                        tint = sky.baseA,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Hero Floating Book Cover + Charge Ring
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(sky.accent.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
                }

                if (continueBook != null) {
                    BookCoverThumbnail(
                        title = continueBook.title,
                        format = continueBook.primaryFormat,
                        coverPath = continueBook.coverPath,
                        modifier = Modifier
                            .size(width = 144.dp, height = 210.dp)
                            .graphicsLayer {
                                translationY = bobOffsetY
                                rotationY = -12f
                                rotationX = 4f
                                cameraDistance = 12f * density
                            }
                            .shadow(20.dp, ZeusShapes.cover, clip = false)
                            .clickable { onOpenBook(continueBook) }
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(width = 144.dp, height = 210.dp)
                            .graphicsLayer {
                                translationY = bobOffsetY
                                rotationY = -10f
                                cameraDistance = 12f * density
                            }
                            .clickable(onClick = onOpenLibrary),
                        shape = ZeusShapes.cover,
                        color = sky.baseB.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, colors.glassBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(ZeusSpacing.lg),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = null,
                                tint = sky.accent,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Your Reading Sky",
                                style = type.headline,
                                color = colors.textOnSky
                            )
                        }
                    }
                }

                // Charge Ring (bottom-right)
                val progressFraction = if (statsState.totalBooks > 0) {
                    (statsState.booksInProgress.toFloat() / statsState.totalBooks.toFloat()).coerceIn(0.15f, 1f)
                } else {
                    0.15f
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = ZeusSpacing.xs, bottom = ZeusSpacing.xs)
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(sky.baseA.copy(alpha = 0.88f))
                        .clickable(onClick = onOpenStats),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.16f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = sky.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${statsState.totalBooks}",
                            style = type.title.copy(fontFamily = type.Unbounded, fontWeight = FontWeight.ExtraBold),
                            color = colors.textOnSky
                        )
                        Text(
                            text = "books",
                            style = type.meta.copy(fontSize = 10.sp),
                            color = colors.textOnSkySecondary
                        )
                    }
                }
            }
        }

        // Glass Resume Card + Gradient Pill CTA
        item {
            LiquidGlassCard(glassEnabled = glassEnabled, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ZeusSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                ) {
                    if (continueBook != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = continueBook.primaryFormat.ifBlank { "BOOK" }.uppercase(),
                                style = type.meta,
                                color = sky.accent
                            )
                            Text(
                                text = "p.${(continueBook.lastPageIndex ?: 0) + 1}",
                                style = type.meta,
                                color = colors.textOnSkySecondary
                            )
                        }
                        Text(
                            text = continueBook.title,
                            style = type.headline,
                            color = colors.textOnSky,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (continueBook.authors.isNotBlank()) {
                            Text(
                                text = continueBook.authors,
                                style = type.body.copy(fontStyle = FontStyle.Italic),
                                color = colors.textOnSkySecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        StormlightGradientPillButton(
                            label = "Jump back in",
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            onClick = { onOpenBook(continueBook) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Start your sanctuary",
                            style = type.headline,
                            color = colors.textOnSky
                        )
                        Text(
                            text = "Import EPUB, PDF, CBZ, or XPS books in Olympus, or search online sources.",
                            style = type.body,
                            color = colors.textOnSkySecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
                        ) {
                            StormlightGradientPillButton(
                                label = "Open Olympus",
                                icon = Icons.Rounded.FileOpen,
                                onClick = onOpenLibrary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Bento Row: Resurfaced Spark (1.4f) + Words Sticker (1f)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                LiquidGlassCard(
                    glassEnabled = glassEnabled,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 112.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenPassages)
                            .padding(ZeusSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
                    ) {
                        Text(
                            text = if (featuredSpark != null) "RESURFACED SPARK" else "SPARKS",
                            style = type.meta.copy(fontSize = 10.sp),
                            color = sky.accent,
                            maxLines = 1
                        )
                        Text(
                            text = featuredSpark?.passage?.text?.let { "“$it”" }
                                ?: "Save memorable lines while reading to resurface them here.",
                            style = type.bodyReading.copy(fontSize = 14.sp, lineHeight = 20.sp, fontStyle = FontStyle.Italic),
                            color = colors.textOnSky,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = onOpenVocab,
                    shape = ZeusShapes.cardCompact,
                    color = colors.stickerLightBg,
                    contentColor = colors.stickerInk,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 112.dp)
                        .rotate(-2f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(ZeusSpacing.md),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${statsState.dueVocabWords}",
                            style = type.displayNumber,
                            color = colors.stickerInk,
                            maxLines = 1
                        )
                        Text(
                            text = "words waiting",
                            style = type.sticker,
                            color = colors.stickerInk,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Compact 2-pill utility bar: Online Books + Offline Packs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                GlassUtilityPill(
                    label = "Online Books",
                    icon = Icons.Rounded.CloudDownload,
                    glassEnabled = glassEnabled,
                    onClick = onOpenOnline,
                    modifier = Modifier.weight(1f)
                )
                GlassUtilityPill(
                    label = "Offline Packs",
                    icon = Icons.Rounded.Translate,
                    glassEnabled = glassEnabled,
                    onClick = onOpenPrepareOffline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StormlightGradientPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sky = ZeusTheme.sky
    val type = ZeusTheme.type
    Box(
        modifier = modifier
            .heightIn(min = ZeusSpacing.primaryButtonHeight)
            .clip(ZeusShapes.pill)
            .background(Brush.linearGradient(listOf(sky.accent, sky.glow2)))
            .clickable(onClick = onClick)
            .padding(horizontal = ZeusSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
        ) {
            Text(
                text = label,
                style = type.button,
                color = sky.baseA,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = sky.baseA,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GlassUtilityPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glassEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Row(
        modifier = modifier
            .heightIn(min = ZeusSpacing.touchMin)
            .liquidGlass(enabled = glassEnabled, shape = ZeusShapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = sky.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(ZeusSpacing.xs))
        Text(
            text = label,
            style = type.sticker,
            color = colors.textOnSky,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BookCoverThumbnail(
    title: String,
    format: String,
    coverPath: String?,
    modifier: Modifier = Modifier
) {
    val hasCover = !coverPath.isNullOrBlank() && File(coverPath).exists()
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Box(
        modifier = modifier.clip(ZeusShapes.cover),
        contentAlignment = Alignment.Center
    ) {
        if (hasCover) {
            AsyncImage(
                model = File(coverPath!!),
                contentDescription = "$title cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = ZeusShapes.cover,
                color = sky.baseB.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, colors.glassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ZeusSpacing.md),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = format.ifBlank { "BOOK" }.uppercase(),
                        style = type.meta,
                        color = sky.accent
                    )
                    Text(
                        text = title,
                        style = type.title,
                        color = colors.textOnSky,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

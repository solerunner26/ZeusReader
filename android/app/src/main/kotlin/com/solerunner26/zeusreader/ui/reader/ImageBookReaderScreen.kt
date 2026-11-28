package com.solerunner26.zeusreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.local.ImageBookPageEntity
import java.io.File

@Composable
fun ImageBookReaderScreen(
    editionId: String,
    title: String,
    viewModel: ImageBookReaderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
    var showReorderSheet by remember { mutableStateOf(false) }

    LaunchedEffect(editionId) {
        viewModel.loadEdition(editionId)
    }

    val safeIndex = if (pages.isEmpty()) 0 else currentIndex.coerceIn(0, pages.lastIndex)
    val currentPage = pages.getOrNull(safeIndex)

    ReaderFrame(
        title = title,
        meta = currentPage?.displayName.orEmpty(),
        paperColor = ZeusTheme.colors.card,
        onBack = onBack,
        progressFraction = if (pages.size > 1) safeIndex.toFloat() / (pages.size - 1) else null,
        progressLabel = if (pages.isEmpty()) "No pages" else "Page ${safeIndex + 1} / ${pages.size}",
        tools = listOf(
            ReaderTool(stringResource(R.string.reader_reorder_pages), Icons.Rounded.Reorder, enabled = pages.size > 1) { showReorderSheet = true }
        ),
        jump = if (pages.size > 1) JumpModel(
            count = pages.size,
            current = safeIndex,
            unitLabel = "pages",
            labelFor = { i -> pages.getOrNull(i)?.displayName ?: "Page ${i + 1}" },
            onJump = { i -> viewModel.selectPage(i) }
        ) else null,
        onPrevious = viewModel::previousPage,
        onNext = viewModel::nextPage,
        canGoPrevious = safeIndex > 0,
        canGoNext = safeIndex + 1 < pages.size,
        modifier = modifier
    ) {
        if (currentPage == null) {
            Text(
                text = "No image pages available.",
                color = ZeusTheme.colors.ink,
                style = ZeusTheme.type.body,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            var scale by remember(currentPage.id) { mutableFloatStateOf(1f) }
            var offsetX by remember(currentPage.id) { mutableFloatStateOf(0f) }
            var offsetY by remember(currentPage.id) { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentPage.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = nextScale
                            if (nextScale > 1f) {
                                val maxX = size.width * (nextScale - 1f) / 2f
                                val maxY = size.height * (nextScale - 1f) / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(currentPage.localPath),
                    contentDescription = currentPage.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                )
                if (scale <= 1.05f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(ZeusSizes.edgeZoneWidth)
                            .fillMaxHeight()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = viewModel::previousPage)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(ZeusSizes.edgeZoneWidth)
                            .fillMaxHeight()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = viewModel::nextPage)
                    )
                }
            }
        }
    }

    if (showReorderSheet) {
        ReorderImagePagesBottomSheet(
            initialPages = pages,
            onDismiss = { showReorderSheet = false },
            onSave = { reordered ->
                viewModel.saveReorderedPages(reordered.map { it.id })
                showReorderSheet = false
            }
        )
    }
}

@Composable
private fun ReorderImagePagesBottomSheet(
    initialPages: List<ImageBookPageEntity>,
    onDismiss: () -> Unit,
    onSave: (List<ImageBookPageEntity>) -> Unit
) {
    var draftPages by remember(initialPages) { mutableStateOf(initialPages) }

    ZeusBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.reader_reorder_pages)) {
        draftPages.forEachIndexed { index, page ->
            MenuRow(title = "${index + 1}. ${page.displayName}", titleMaxLines = 1) {
                CircleIconButton(Icons.Rounded.ArrowUpward, stringResource(R.string.reader_move_up), {
                    if (index > 0) {
                        draftPages = draftPages.toMutableList().apply { add(index - 1, removeAt(index)) }
                    }
                }, enabled = index > 0)
                CircleIconButton(Icons.Rounded.ArrowDownward, stringResource(R.string.reader_move_down), {
                    if (index < draftPages.lastIndex) {
                        draftPages = draftPages.toMutableList().apply { add(index + 1, removeAt(index)) }
                    }
                }, enabled = index < draftPages.lastIndex)
            }
        }
        PrimaryButton(label = stringResource(R.string.reader_save_order), onClick = { onSave(draftPages) }, modifier = Modifier.fillMaxWidth())
    }
}

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.local.ImageBookPageEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showReorderSheet = true },
                        enabled = pages.size > 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Reorder,
                            contentDescription = stringResource(R.string.reader_reorder_pages)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (pages.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::previousPage,
                            enabled = safeIndex > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.reader_prev_page)
                            )
                        }
                        Text(
                            text = "Page ${safeIndex + 1} / ${pages.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = viewModel::nextPage,
                            enabled = safeIndex + 1 < pages.size,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.reader_next_page)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF141414)),
            contentAlignment = Alignment.Center
        ) {
            if (currentPage == null) {
                Text(
                    text = "No image pages available.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
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
                                    offsetX += pan.x
                                    offsetY += pan.y
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
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )

                    if (scale <= 1.05f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(64.dp)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = viewModel::previousPage
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(64.dp)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = viewModel::nextPage
                                )
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderImagePagesBottomSheet(
    initialPages: List<ImageBookPageEntity>,
    onDismiss: () -> Unit,
    onSave: (List<ImageBookPageEntity>) -> Unit
) {
    var draftPages by remember(initialPages) { mutableStateOf(initialPages) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.reader_reorder_pages),
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(draftPages, key = { _, p -> p.id }) { index, page ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${page.displayName}",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        draftPages = draftPages.toMutableList().apply {
                                            val item = removeAt(index)
                                            add(index - 1, item)
                                        }
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = stringResource(R.string.reader_move_up)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (index < draftPages.lastIndex) {
                                        draftPages = draftPages.toMutableList().apply {
                                            val item = removeAt(index)
                                            add(index + 1, item)
                                        }
                                    }
                                },
                                enabled = index < draftPages.lastIndex,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = stringResource(R.string.reader_move_down)
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onSave(draftPages) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.reader_save_order))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

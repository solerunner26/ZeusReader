package com.solerunner26.zeusreader.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    editionId: String,
    title: String,
    localPath: String,
    viewModel: PdfReaderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val widthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx().toInt()
    }

    LaunchedEffect(editionId, localPath) {
        viewModel.openDocument(
            editionId = editionId,
            title = title,
            localPath = localPath,
            viewportWidthPx = widthPx
        )
    }

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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            val ready = uiState as? PdfReaderUiState.Ready
            if (ready != null) {
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
                            enabled = ready.currentPage > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.reader_prev_page)
                            )
                        }
                        Text(
                            text = "Page ${ready.currentPage + 1} / ${ready.pageCount}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = viewModel::nextPage,
                            enabled = ready.currentPage + 1 < ready.pageCount,
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
            when (val state = uiState) {
                PdfReaderUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is PdfReaderUiState.Error -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is PdfReaderUiState.Ready -> {
                    var scale by remember(state.currentPage) { mutableFloatStateOf(1f) }
                    var offsetX by remember(state.currentPage) { mutableFloatStateOf(0f) }
                    var offsetY by remember(state.currentPage) { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.currentPage) {
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
                        state.bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF page ${state.currentPage + 1}",
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
                        }

                        if (state.isRenderingPage) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        // Thumb-safe one-handed edge page-turn zones when unzoomed
                        if (scale <= 1.05f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(64.dp)
                                    .fillMaxHeight()
                                    .heightIn(min = 48.dp)
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
                                    .heightIn(min = 48.dp)
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
    }
}

package com.solerunner26.zeusreader.ui.reader

import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.reader.epub.EpubSearchResultItem
import com.solerunner26.zeusreader.reader.epub.EpubTocItem
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    editionId: String,
    title: String,
    localPath: String,
    activity: FragmentActivity,
    viewModel: EpubReaderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    var navigatorRef by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(editionId, localPath) {
        viewModel.openBook(editionId = editionId, title = title, localPath = localPath)
    }

    LaunchedEffect(navigatorRef) {
        val nav = navigatorRef ?: return@LaunchedEffect
        nav.currentLocator.collect { locator ->
            viewModel.onLocatorChanged(locator)
        }
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
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                },
                actions = {
                    val ready = uiState as? EpubReaderUiState.Ready
                    IconButton(
                        onClick = { showTocSheet = true },
                        enabled = ready != null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            contentDescription = "Table of contents"
                        )
                    }
                    IconButton(
                        onClick = { showSearchSheet = true },
                        enabled = ready?.capabilities?.search == true,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search in book"
                        )
                    }
                }
            )
        },
        bottomBar = {
            val ready = uiState as? EpubReaderUiState.Ready
            if (ready != null) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EPUB • Readium",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${ready.currentProgressionPercent}%",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                EpubReaderUiState.Loading -> CircularProgressIndicator()

                is EpubReaderUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                is EpubReaderUiState.Ready -> {
                    val containerId = remember { View.generateViewId() }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            FragmentContainerView(ctx).apply {
                                id = containerId
                                val fm = activity.supportFragmentManager
                                val existing = fm.findFragmentById(containerId) as? EpubNavigatorFragment
                                if (existing == null) {
                                    val factory = EpubNavigatorFactory(state.publication)
                                    fm.fragmentFactory = factory.createFragmentFactory(
                                        initialLocator = state.initialLocator
                                    )
                                    val fragment = fm.fragmentFactory.instantiate(
                                        ctx.classLoader,
                                        EpubNavigatorFragment::class.java.name
                                    ) as EpubNavigatorFragment
                                    fm.beginTransaction()
                                        .replace(containerId, fragment)
                                        .commitNowAllowingStateLoss()
                                    navigatorRef = fragment
                                } else {
                                    navigatorRef = existing
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    val readyState = uiState as? EpubReaderUiState.Ready
    if (showTocSheet && readyState != null) {
        EpubTocBottomSheet(
            items = readyState.tocItems,
            onDismiss = { showTocSheet = false },
            onSelect = { item ->
                navigatorRef?.go(item.link, animated = true)
                showTocSheet = false
            }
        )
    }

    if (showSearchSheet && readyState != null) {
        EpubSearchBottomSheet(
            results = searchResults,
            isSearching = isSearching,
            onSearch = viewModel::search,
            onDismiss = { showSearchSheet = false },
            onSelect = { match ->
                navigatorRef?.go(match.locator, animated = true)
                showSearchSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpubTocBottomSheet(
    items: List<EpubTocItem>,
    onDismiss: () -> Unit,
    onSelect: (EpubTocItem) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Table of Contents", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(
                    text = "No table of contents entries in this EPUB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (item.depth * 12).dp)
                                .heightIn(min = 48.dp)
                                .clickable { onSelect(item) }
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpubSearchBottomSheet(
    results: List<EpubSearchResultItem>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (EpubSearchResultItem) -> Unit
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Search in Book", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search passage…") }
                )
                Button(
                    onClick = { onSearch(query) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Find")
                }
            }
            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results) { match ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onSelect(match) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = match.chapterTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = match.snippet,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

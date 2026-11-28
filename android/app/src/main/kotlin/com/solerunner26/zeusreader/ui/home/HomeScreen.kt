package com.solerunner26.zeusreader.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.library.LibraryUiState
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.passages.PassagesViewModel
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
import com.solerunner26.zeusreader.ui.theme.LiquidGlassCard
import com.solerunner26.zeusreader.ui.theme.ZeusFonts
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
    modifier: Modifier = Modifier
) {
    val libState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val passagesState by passagesViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()

    val books = (libState as? LibraryUiState.Populated)?.books.orEmpty()
    val continueBook = books.firstOrNull { (it.lastPageIndex ?: 0) > 0 } ?: books.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(
                            text = "ZeusReader",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${statsState.currentStreakDays}d streak • ${statsState.totalBooks} books",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("home_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (continueBook != null) {
                item {
                    LiquidGlassCard(glassEnabled = glassEnabled, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBook(continueBook) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookCoverThumbnail(
                                title = continueBook.title,
                                format = continueBook.primaryFormat,
                                coverPath = continueBook.coverPath,
                                modifier = Modifier.size(width = 72.dp, height = 104.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CONTINUE READING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = continueBook.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (continueBook.authors.isNotBlank()) {
                                    Text(
                                        text = continueBook.authors,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { onOpenBook(continueBook) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoStories, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Resume")
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    LiquidGlassCard(glassEnabled = glassEnabled, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Your Personal Reading Sanctuary", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Import EPUB, PDF, CBZ, XPS, or HTML books in your Library, or acquire books from your private server.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onOpenLibrary, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Icon(Icons.Rounded.FileOpen, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open Library")
                                }
                                OutlinedButton(onClick = onOpenOnline, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Online")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        QuickActionPill("Library", Icons.Rounded.AutoStories, glassEnabled, onOpenLibrary)
                    }
                    item {
                        QuickActionPill("Online Books", Icons.Rounded.CloudDownload, glassEnabled, onOpenOnline)
                    }
                    item {
                        QuickActionPill("Offline Packs", Icons.Rounded.Translate, glassEnabled, onOpenPrepareOffline)
                    }
                    item {
                        QuickActionPill("Flashcards (${statsState.dueVocabWords})", Icons.Rounded.School, glassEnabled, onOpenVocab)
                    }
                    item {
                        QuickActionPill("Stats", Icons.Rounded.BarChart, glassEnabled, onOpenStats)
                    }
                }
            }

            if (passagesState.passages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Passages", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = onOpenPassages, modifier = Modifier.heightIn(min = 40.dp)) {
                            Text("View all")
                        }
                    }
                }
                items(passagesState.passages.take(3), key = { it.passage.id }) { item ->
                    LiquidGlassCard(glassEnabled = glassEnabled, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPassages() }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = item.passage.chapterTitle ?: "Saved passage",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = item.passage.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = ZeusFonts.Fraunces,
                                    fontStyle = FontStyle.Italic
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glassEnabled: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassCard(glassEnabled = glassEnabled, cornerRadius = 18.dp) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
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
            LiquidGlassCard(glassEnabled = true, cornerRadius = 12.dp, modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = format.ifBlank { "BOOK" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

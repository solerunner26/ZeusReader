package com.solerunner26.zeusreader.ui.online

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.theme.HueAuraBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineAcquisitionScreen(
    viewModel: OnlineAcquisitionViewModel,
    onBack: () -> Unit,
    uiConfig: AppUiConfig = AppUiConfig(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPairingDialog by remember { mutableStateOf(false) }
    var pairingJsonInput by remember { mutableStateOf("") }

    HueAuraBackground(
        config = uiConfig,
        darkTheme = darkTheme,
        modifier = modifier
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            topBar = {
                TopAppBar(
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
                    title = { Text("Private Server & Online") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = viewModel::syncPassagesNow,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = "Sync passages")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .testTag("online_acquisition_screen"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Server Pairing (Tailscale / LAN)", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = state.serverUrl,
                                onValueChange = { viewModel.updateServerConfig(it, state.pairingToken) },
                                label = { Text("Server URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.pairingToken,
                                onValueChange = { viewModel.updateServerConfig(state.serverUrl, it) },
                                label = { Text("Pairing Token") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = viewModel::checkServerAndLoad,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Connect & Refresh")
                                }
                                OutlinedButton(
                                    onClick = viewModel::syncPassagesNow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Sync Now")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPairingDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Paste QR JSON")
                                }
                                OutlinedButton(
                                    onClick = viewModel::backupToServer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Backup Data")
                                }
                            }
                            state.slumStatus?.let { slum ->
                                val staleText = if (slum.stale) " (cached/stale)" else ""
                                Text(
                                    text = "Open SLUM: ${slum.providers.size} providers tracked$staleText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            state.statusMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::updateQuery,
                            label = { Text("Search title, author, or ISBN") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = viewModel::searchOnline,
                            enabled = !state.isSearching && state.query.isNotBlank(),
                            modifier = Modifier.heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search online")
                        }
                    }
                    if (state.isSearching) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (state.searchResults.isNotEmpty()) {
                    item {
                        Text("Editions Found (${state.searchResults.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(state.searchResults, key = { it.idempotency_key }) { item ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.authors.joinToString()} • ${item.format.uppercase()} • ${item.source}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { viewModel.queueDownload(item) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Text("  Queue to Server")
                                }
                            }
                        }
                    }
                }

                if (state.transfers.isNotEmpty()) {
                    item {
                        Text("Transfers (${state.transfers.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(state.transfers, key = { it.job_id }) { job ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(job.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${job.state} • ${job.progress_percent}% • ${job.format.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (job.state == "NeedsBrowser" && !job.browser_url.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, job.browser_url.toUri())
                                                )
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Continue in browser")
                                        }
                                    }
                                    if (job.state == "ReadyOnServer") {
                                        Button(
                                            onClick = { viewModel.downloadToPhone(job) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(Icons.Rounded.Download, contentDescription = null)
                                            Text("  Download to Phone")
                                        }
                                    } else if (job.state == "Failed" || job.state == "Cancelled") {
                                        OutlinedButton(
                                            onClick = { viewModel.retryTransfer(job.job_id) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Retry")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.cancelTransfer(job.job_id) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = { showPairingDialog = false },
            title = { Text("Paste Pairing QR Payload") },
            text = {
                OutlinedTextField(
                    value = pairingJsonInput,
                    onValueChange = { pairingJsonInput = it },
                    label = { Text("{\"url\":\"http://...\",\"token\":\"...\"}") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.applyPairingPayload(pairingJsonInput)
                        showPairingDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

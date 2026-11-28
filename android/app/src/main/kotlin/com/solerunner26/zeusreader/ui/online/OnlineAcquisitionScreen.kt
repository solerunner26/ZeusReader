package com.solerunner26.zeusreader.ui.online

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.GlassSection
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusListRow
import com.solerunner26.zeusreader.ui.components.ZeusSectionHeader
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

/**
 * Online Books (SLUM connector UI). Layout only — all behaviour stays in [OnlineAcquisitionViewModel].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnlineAcquisitionScreen(
    viewModel: OnlineAcquisitionViewModel,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") uiConfig: AppUiConfig = AppUiConfig(),
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPairingDialog by remember { mutableStateOf(false) }
    var showServerSettings by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var pairingJsonInput by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }

    val colors = ZeusTheme.colors
    val type = ZeusTheme.type

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ZeusTopBar(
            title = "Online books",
            subtitle = when (state.isConnected) {
                true -> "Connected to your relay"
                false -> "Relay offline — check server settings"
                null -> "Search open catalogues"
            },
            onBack = onBack
        ) {
            GlassIconButton(Icons.Rounded.History, "Download history", { showHistorySheet = true })
            GlassIconButton(Icons.Rounded.Tune, "Server settings", { showServerSettings = !showServerSettings }, highlighted = showServerSettings)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("online_acquisition_screen"),
            contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, bottom = rememberNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            item(key = "search") {
                GlassSection(neon = true) {
                    ZeusTextField(
                        value = state.query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = "Title, author or ISBN",
                        leadingIcon = Icons.Rounded.Search,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (!state.isSearching && state.query.isNotBlank()) viewModel.searchOnline()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryPillButton(
                        label = if (state.isSearching) "Searching…" else "Search online",
                        icon = Icons.Rounded.Search,
                        leadingIcon = true,
                        onClick = viewModel::searchOnline,
                        enabled = !state.isSearching && state.query.isNotBlank(),
                        height = ZeusSpacing.secondaryButtonHeight + ZeusSpacing.xs,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.isSearching) ZeusLinearProgress()
                }
            }

            if (showServerSettings) {
                item(key = "server") {
                    GlassSection(title = "Server settings · saved automatically") {
                        ZeusTextField(
                            value = state.serverUrl,
                            onValueChange = { viewModel.updateServerConfig(it, state.pairingToken) },
                            label = "Server URL",
                            leadingIcon = Icons.Rounded.Link,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ZeusTextField(
                            value = state.pairingToken,
                            onValueChange = { viewModel.updateServerConfig(state.serverUrl, it) },
                            label = "Pairing token",
                            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailing = {
                                GlassIconButton(
                                    icon = if (tokenVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (tokenVisible) "Hide token" else "Show token",
                                    onClick = { tokenVisible = !tokenVisible },
                                    size = ZeusSizes.avatar - ZeusSpacing.xs
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
                        ) {
                            GlassPillButton("Reconnect", viewModel::checkServerAndLoad, icon = Icons.Rounded.Refresh)
                            GlassPillButton("Sync now", viewModel::syncPassagesNow, icon = Icons.Rounded.Sync)
                            GlassPillButton("Paste QR JSON", { showPairingDialog = true }, icon = Icons.Rounded.QrCode2)
                            GlassPillButton("Backup data", viewModel::backupToServer, icon = Icons.Rounded.Backup)
                        }
                        state.slumStatus?.let { slum ->
                            MetaLabel(
                                text = "Catalogue: ${slum.providers.size} sources tracked" + if (slum.stale) " · cached" else "",
                                accent = true
                            )
                        }
                    }
                }
            }

            state.activeActionLabel?.let { label ->
                item(key = "active") {
                    GlassSection {
                        val pct = state.activeProgressPercent
                        Text(
                            text = if (pct != null) "$label · $pct% · ${state.activeElapsedSeconds}s" else "$label · ${state.activeElapsedSeconds}s",
                            style = type.label,
                            color = colors.accentOnSky
                        )
                        ZeusLinearProgress(progress = pct?.let { it / 100f })
                    }
                }
            }

            state.statusMessage?.let { msg ->
                item(key = "status") {
                    Text(msg, style = type.caption, color = colors.textOnSkySecondary)
                }
            }

            if (state.searchResults.isNotEmpty()) {
                item(key = "results_header") { ZeusSectionHeader(title = "Editions found · ${state.searchResults.size}") }
                itemsIndexed(state.searchResults, key = { idx, item -> "${idx}_${item.idempotency_key}" }) { _, item ->
                    val isQueuingThis = state.queuingKey == item.idempotency_key
                    GlassSection {
                        ZeusListRow(
                            title = item.title,
                            subtitle = "${item.authors.joinToString()} · ${item.source}",
                            icon = Icons.Rounded.MenuBook
                        ) {
                            MetaLabel(text = item.format, accent = true)
                        }
                        PrimaryPillButton(
                            label = if (isQueuingThis) "Queuing… ${state.activeElapsedSeconds}s" else "Queue to server",
                            icon = Icons.Rounded.Download,
                            leadingIcon = true,
                            onClick = { viewModel.queueDownload(item) },
                            enabled = state.queuingKey == null && state.activeProgressPercent == null,
                            height = ZeusSpacing.secondaryButtonHeight,
                            shimmer = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.transfers.isNotEmpty()) {
                item(key = "transfers_header") { ZeusSectionHeader(title = "Transfers · ${state.transfers.size}") }
                itemsIndexed(state.transfers, key = { idx, job -> "${idx}_${job.job_id}" }) { _, job ->
                    GlassSection {
                        ZeusListRow(
                            title = job.title,
                            subtitle = "${job.state} · ${job.progress_percent}% · ${job.format.uppercase()}",
                            icon = Icons.Rounded.Download
                        )
                        ZeusLinearProgress(progress = job.progress_percent / 100f)
                        Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                            if (job.state == "NeedsBrowser" && !job.browser_url.isNullOrBlank()) {
                                GlassPillButton(
                                    label = "Continue in browser",
                                    icon = Icons.Rounded.OpenInBrowser,
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, job.browser_url.toUri())) }
                                )
                            }
                            when {
                                job.state == "ReadyOnServer" -> {
                                    val isDownloading = state.activeProgressPercent != null
                                    PrimaryPillButton(
                                        label = if (isDownloading) "Downloading ${state.activeProgressPercent}%" else "Download to phone",
                                        icon = Icons.Rounded.Download,
                                        leadingIcon = true,
                                        onClick = { viewModel.downloadToPhone(job) },
                                        enabled = !isDownloading,
                                        height = ZeusSpacing.secondaryButtonHeight,
                                        shimmer = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                job.state == "Failed" || job.state == "Cancelled" ->
                                    GlassPillButton("Retry", { viewModel.retryTransfer(job.job_id) }, icon = Icons.Rounded.Refresh)
                                else ->
                                    GlassPillButton("Cancel", { viewModel.cancelTransfer(job.job_id) }, icon = Icons.Rounded.Cancel, danger = true)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ZeusBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            title = "Download history",
            subtitle = "${state.downloadHistory.size} this session"
        ) {
            if (state.downloadHistory.isEmpty()) {
                Text(
                    "No books downloaded in this session yet. Downloaded books appear directly in Olympus.",
                    style = type.body,
                    color = colors.textOnSkySecondary
                )
            } else {
                state.downloadHistory.forEach { h ->
                    ZeusListRow(title = h.title, subtitle = "${h.format.uppercase()} · ${h.source} · ${h.state}", icon = Icons.Rounded.MenuBook)
                }
            }
        }
    }

    if (showPairingDialog) {
        ZeusDialog(
            onDismissRequest = { showPairingDialog = false },
            title = "Paste pairing QR payload",
            confirmLabel = "Apply",
            onConfirm = {
                viewModel.applyPairingPayload(pairingJsonInput)
                showPairingDialog = false
            }
        ) {
            ZeusTextField(
                value = pairingJsonInput,
                onValueChange = { pairingJsonInput = it },
                label = "{\"url\":\"https://…\",\"token\":\"…\"}",
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

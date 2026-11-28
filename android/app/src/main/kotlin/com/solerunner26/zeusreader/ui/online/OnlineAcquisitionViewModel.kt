package com.solerunner26.zeusreader.ui.online

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.importer.LibraryImporter
import com.solerunner26.zeusreader.sync.OnlineSearchResultDto
import com.solerunner26.zeusreader.sync.PrivateServerClient
import com.solerunner26.zeusreader.sync.SlumStatusDto
import com.solerunner26.zeusreader.sync.SyncAndBackupRepository
import com.solerunner26.zeusreader.sync.TransferCreateDto
import com.solerunner26.zeusreader.sync.TransferJobDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class OnlineAcquisitionUiState(
    val serverUrl: String = "https://zeusreader-relay.jaldip-sac.workers.dev",
    val pairingToken: String = "zeus-dev-pairing-token",
    val isConnected: Boolean? = null,
    val slumStatus: SlumStatusDto? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<OnlineSearchResultDto> = emptyList(),
    val transfers: List<TransferJobDto> = emptyList(),
    val downloadHistory: List<TransferJobDto> = emptyList(),
    val activeActionLabel: String? = null,
    val activeProgressPercent: Int? = null,
    val activeElapsedSeconds: Int = 0,
    val queuingKey: String? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class OnlineAcquisitionViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryImporter: LibraryImporter,
    private val serverClient: PrivateServerClient,
    private val syncRepository: SyncAndBackupRepository
) : ViewModel() {

    private val prefs by lazy {
        context.getSharedPreferences("zeus_server_config", Context.MODE_PRIVATE)
    }

    private val _uiState = MutableStateFlow(
        OnlineAcquisitionUiState(
            serverUrl = prefs.getString("server_url", "https://zeusreader-relay.jaldip-sac.workers.dev")
                ?: "https://zeusreader-relay.jaldip-sac.workers.dev",
            pairingToken = prefs.getString("pairing_token", "zeus-dev-pairing-token")
                ?: "zeus-dev-pairing-token"
        )
    )
    val uiState: StateFlow<OnlineAcquisitionUiState> = _uiState.asStateFlow()

    init {
        checkServerAndLoad()
    }

    fun updateServerConfig(url: String, token: String) {
        prefs.edit()
            .putString("server_url", url)
            .putString("pairing_token", token)
            .apply()
        _uiState.update { it.copy(serverUrl = url, pairingToken = token) }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun checkServerAndLoad() {
        val state = _uiState.value
        viewModelScope.launch {
            val healthy = serverClient.checkHealth(state.serverUrl, state.pairingToken).getOrDefault(false)
            _uiState.update { it.copy(isConnected = healthy) }
            if (healthy) {
                serverClient.getSlumStatus(state.serverUrl, state.pairingToken)
                    .onSuccess { slum -> _uiState.update { it.copy(slumStatus = slum) } }
                serverClient.listTransfers(state.serverUrl, state.pairingToken)
                    .onSuccess { jobs -> _uiState.update { it.copy(transfers = jobs) } }
            } else {
                _uiState.update { it.copy(statusMessage = "Private server unreachable. Offline reading unaffected.") }
            }
        }
    }

    fun searchOnline() {
        val state = _uiState.value
        val q = state.query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, statusMessage = null) }
            serverClient.searchOnline(state.serverUrl, state.pairingToken, q)
                .onSuccess { resp ->
                    val formatRank = mapOf("pdf" to 0, "epub" to 1, "mobi" to 2, "azw3" to 3)
                    val sorted = resp.results.sortedBy { formatRank[it.format.lowercase()] ?: 99 }
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = sorted,
                            statusMessage = resp.warnings.firstOrNull()
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isSearching = false, statusMessage = err.message ?: "Search failed")
                    }
                }
        }
    }

    fun queueDownload(result: OnlineSearchResultDto) {
        val state = _uiState.value
        viewModelScope.launch {
            val timerJob = startElapsedTimer("Fetching '${result.title}' (${result.format.uppercase()}) on server…")
            _uiState.update { it.copy(queuingKey = result.idempotency_key, activeProgressPercent = null) }
            serverClient.enqueueTransfer(
                state.serverUrl,
                state.pairingToken,
                TransferCreateDto(
                    idempotency_key = result.idempotency_key,
                    source = result.source,
                    title = result.title,
                    authors = result.authors,
                    language = result.language,
                    year = result.year,
                    format = result.format,
                    download_url = result.download_url
                )
            ).onSuccess { job ->
                timerJob.cancel()
                _uiState.update { s ->
                    val updated = (listOf(job) + s.transfers.filterNot { it.job_id == job.job_id })
                    s.copy(
                        transfers = updated,
                        queuingKey = null,
                        activeActionLabel = null,
                        activeProgressPercent = null,
                        activeElapsedSeconds = 0,
                        statusMessage = if (job.state == "ReadyOnServer") {
                            "Ready on server: ${job.title}. Tap Download to Phone."
                        } else {
                            "Queued: ${job.title} (${job.state})"
                        }
                    )
                }
            }.onFailure { err ->
                timerJob.cancel()
                _uiState.update {
                    it.copy(
                        queuingKey = null,
                        activeActionLabel = null,
                        activeProgressPercent = null,
                        activeElapsedSeconds = 0,
                        statusMessage = err.message ?: "Queue failed"
                    )
                }
            }
        }
    }

    fun cancelTransfer(jobId: String) {
        val state = _uiState.value
        viewModelScope.launch {
            serverClient.cancelTransfer(state.serverUrl, state.pairingToken, jobId)
                .onSuccess { job -> replaceTransfer(job) }
        }
    }

    fun retryTransfer(jobId: String) {
        val state = _uiState.value
        viewModelScope.launch {
            serverClient.retryTransfer(state.serverUrl, state.pairingToken, jobId)
                .onSuccess { job -> replaceTransfer(job) }
        }
    }

    fun syncPassagesNow() {
        val state = _uiState.value
        viewModelScope.launch {
            syncRepository.syncWithServer(state.serverUrl, state.pairingToken)
                .onSuccess { count ->
                    _uiState.update { it.copy(statusMessage = "Synced $count passage records") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Sync failed") }
                }
        }
    }

    fun applyPairingPayload(rawJson: String) {
        runCatching {
            val obj = JSONObject(rawJson.trim())
            val url = obj.optString("url").ifBlank { obj.optString("host") }
            val token = obj.optString("token")
            require(url.isNotBlank() && token.isNotBlank()) { "Missing url or token" }
            val cleanUrl = url.trimEnd('/')
            prefs.edit()
                .putString("server_url", cleanUrl)
                .putString("pairing_token", token)
                .apply()
            _uiState.update {
                it.copy(
                    serverUrl = cleanUrl,
                    pairingToken = token,
                    statusMessage = "Pairing config saved."
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(statusMessage = "Invalid pairing payload: ${err.message}") }
        }
    }

    fun backupToServer() {
        val state = _uiState.value
        viewModelScope.launch {
            syncRepository.uploadBackupToServer(state.serverUrl, state.pairingToken, "data_only")
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(statusMessage = "Backup uploaded (${summary.byte_size} bytes, sha256=${summary.sha256.take(10)}…)")
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Backup failed") }
                }
        }
    }

    fun downloadToPhone(job: TransferJobDto) {
        val state = _uiState.value
        viewModelScope.launch {
            val safeTitle = job.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(48).ifBlank { job.job_id }
            val ext = job.format.lowercase().ifBlank { "epub" }
            val destFile = File(File(context.filesDir, "downloads"), "$safeTitle.$ext")
            val timerJob = startElapsedTimer("Downloading '${job.title}' to phone…")
            _uiState.update { it.copy(activeProgressPercent = 0, statusMessage = null) }
            serverClient.downloadTransferFile(
                baseUrl = state.serverUrl,
                token = state.pairingToken,
                jobId = job.job_id,
                destFile = destFile,
                expectedSha256 = job.file_sha256,
                onProgressPercent = { pct ->
                    _uiState.update { it.copy(activeProgressPercent = pct) }
                }
            ).onSuccess { file ->
                timerJob.cancel()
                val report = libraryImporter.importUris(listOf(Uri.fromFile(file)), origin = "server_transfer")
                _uiState.update { s ->
                    s.copy(
                        query = "",
                        searchResults = emptyList(),
                        transfers = s.transfers.filterNot { it.job_id == job.job_id },
                        downloadHistory = listOf(job.copy(state = "AvailableOffline", progress_percent = 100)) +
                            s.downloadHistory.filterNot { it.job_id == job.job_id },
                        activeActionLabel = null,
                        activeProgressPercent = null,
                        activeElapsedSeconds = 0,
                        statusMessage = "Downloaded & added to Library: ${job.title} (imported=${report.importedFileCount}, skipped=${report.duplicates.size})"
                    )
                }
            }.onFailure { err ->
                timerJob.cancel()
                _uiState.update {
                    it.copy(
                        activeActionLabel = null,
                        activeProgressPercent = null,
                        activeElapsedSeconds = 0,
                        statusMessage = "Phone transfer failed: ${err.message}"
                    )
                }
            }
        }
    }

    private fun startElapsedTimer(label: String): kotlinx.coroutines.Job {
        _uiState.update { it.copy(activeActionLabel = label, activeElapsedSeconds = 0) }
        return viewModelScope.launch {
            var secs = 0
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                secs += 1
                _uiState.update { it.copy(activeElapsedSeconds = secs) }
            }
        }
    }

    private fun replaceTransfer(job: TransferJobDto) {
        _uiState.update { s ->
            s.copy(transfers = s.transfers.map { if (it.job_id == job.job_id) job else it })
        }
    }
}

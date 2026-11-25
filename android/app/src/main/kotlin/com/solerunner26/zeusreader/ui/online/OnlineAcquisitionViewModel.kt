package com.solerunner26.zeusreader.ui.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.sync.OnlineSearchResultDto
import com.solerunner26.zeusreader.sync.PrivateServerClient
import com.solerunner26.zeusreader.sync.SlumStatusDto
import com.solerunner26.zeusreader.sync.SyncAndBackupRepository
import com.solerunner26.zeusreader.sync.TransferCreateDto
import com.solerunner26.zeusreader.sync.TransferJobDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineAcquisitionUiState(
    val serverUrl: String = "http://100.64.0.1:8765",
    val pairingToken: String = "zeus-dev-pairing-token",
    val isConnected: Boolean? = null,
    val slumStatus: SlumStatusDto? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<OnlineSearchResultDto> = emptyList(),
    val transfers: List<TransferJobDto> = emptyList(),
    val statusMessage: String? = null
)

@HiltViewModel
class OnlineAcquisitionViewModel @Inject constructor(
    private val serverClient: PrivateServerClient,
    private val syncRepository: SyncAndBackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineAcquisitionUiState())
    val uiState: StateFlow<OnlineAcquisitionUiState> = _uiState.asStateFlow()

    fun updateServerConfig(url: String, token: String) {
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
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = resp.results,
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
                _uiState.update { s ->
                    val updated = (listOf(job) + s.transfers.filterNot { it.job_id == job.job_id })
                    s.copy(transfers = updated, statusMessage = "Queued: ${job.title} (${job.state})")
                }
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = err.message ?: "Queue failed") }
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

    private fun replaceTransfer(job: TransferJobDto) {
        _uiState.update { s ->
            s.copy(transfers = s.transfers.map { if (it.job_id == job.job_id) job else it })
        }
    }
}
